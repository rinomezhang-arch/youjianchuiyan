package com.youjian.banquet.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youjian.banquet.dto.NotifyEvent;
import com.youjian.banquet.entity.BookingDishDetail;
import com.youjian.banquet.repository.BookingDishDetailRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;

/** A transaction-owned submission, not an authorization issuer or a public receipt lookup. */
public class IpadBatchSubmissionService {
    private final JdbcTemplate jdbc;
    private final BookingDishDetailRepository details;
    private final NotifyPublisher notifications;
    private static final ObjectMapper JSON = new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    public IpadBatchSubmissionService(JdbcTemplate jdbc, BookingDishDetailRepository details, NotifyPublisher notifications) {
        this.jdbc=jdbc; this.details=details; this.notifications=notifications;
    }
    public record Item(String dishId,int quantity) {}
    public record Submission(String bookingId,String clientRequestId,List<Item> items,String fingerprint) {}
    public static class Rejected extends RuntimeException {
        private final int code;
        private final String reason;
        public Rejected(int code,String message) { this(code,"invalid_request",message); }
        public Rejected(int code,String reason,String message) { super(message); this.code=code; this.reason=reason; }
        public int code() { return code; }
        public String reason() { return reason; }
    }
    public static Submission normalize(Map<String,Object> body) {
        if(body==null) throw new Rejected(400,"缺少提交参数");
        if(body.containsKey("staff_id")) throw new Rejected(400,"请使用本次授权凭据，不接受自报员工身份");
        if(!(body.get("client_request_id") instanceof String key) || !key.matches("[A-Za-z0-9_-]{16,100}"))
            throw new Rejected(400,"缺少或无效client_request_id，请更新页面并保留本次提交编号");
        if(!(body.get("booking_id") instanceof String booking) || booking.isBlank() || booking.length()>255)
            throw new Rejected(400,"缺少或无效booking_id");
        if(!(body.get("dishes") instanceof List<?> input) || input.isEmpty() || input.size()>200)
            throw new Rejected(400,"菜品列表须为1到200项");
        TreeMap<String,Integer> quantities=new TreeMap<>();
        for(Object item:input) {
            if(!(item instanceof Map<?,?> row) || !(row.get("dish_id") instanceof String raw))
                throw new Rejected(400,"菜品编号无效");
            String id=Normalizer.normalize(raw.trim(),Normalizer.Form.NFC);
            if(id.isEmpty() || id.length()>255 || id.codePoints().anyMatch(Character::isISOControl)) throw new Rejected(400,"菜品编号无效");
            int qty;
            try {
                Object q=row.get("dish_quantity");
                if(!(q instanceof Number) && !(q instanceof String)) throw new NumberFormatException();
                qty=new BigDecimal(q.toString()).intValueExact();
            } catch(Exception e) { throw new Rejected(400,"菜品数量必须是1到99的整数"); }
            if(qty<1 || qty>99) throw new Rejected(400,"菜品数量必须是1到99的整数");
            int total=quantities.getOrDefault(id,0)+qty;
            if(total>99) throw new Rejected(400,"同菜合计数量不能超过99");
            quantities.put(id,total);
        }
        List<Item> items=quantities.entrySet().stream().map(e->new Item(e.getKey(),e.getValue())).toList();
        try {
            byte[] canonical=JSON.writeValueAsBytes(items);
            String fingerprint=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical));
            return new Submission(booking,key,List.copyOf(items),fingerprint);
        } catch(Exception e) { throw new IllegalStateException("提交摘要生成失败",e); }
    }
    public Map<String,Object> submit(Submission submission,long store,int authorizedStaff) {
        if(!TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("提交必须在事务中执行");
        if(store<=0 || authorizedStaff<=0) throw new Rejected(403,"授权身份无效");
        // Current reads after the canonical booking lock: no pre-lock RR snapshot can replay stale receipts.
        List<Map<String,Object>> parents=jdbc.queryForList("SELECT id,booking_id,booking_status,payment_status FROM booking_master WHERE store_id=? AND booking_id=? FOR UPDATE",store,submission.bookingId());
        if(parents.isEmpty()) throw new Rejected(404,"预订不存在或不属于当前门店");
        Map<String,Object> parent=parents.get(0);long parentId=((Number)parent.get("id")).longValue();
        List<Map<String,Object>> receipts=jdbc.queryForList("SELECT payload_sha256,result_json FROM ipad_batch_request WHERE store_id=? AND booking_master_id=? AND client_request_id=? FOR UPDATE",store,parentId,submission.clientRequestId());
        if(!receipts.isEmpty()) {
            Map<String,Object> previous=receipts.get(0);
            if(!submission.fingerprint().equals(previous.get("payload_sha256"))) throw new Rejected(409,"request_conflict","本次提交编号已用于不同菜品或数量，请先核对原提交");
            return decode(previous.get("result_json").toString());
        }
        if(List.of("cancelled","completed").contains(Objects.toString(parent.get("booking_status"),"")) || "paid".equals(parent.get("payment_status")))
            throw new Rejected(409,"booking_closed","订单已取消或已结账，不能加菜");
        String bookingId=parent.get("booking_id").toString();
        List<Long> ids=new ArrayList<>();List<String> names=new ArrayList<>();BigDecimal amount=BigDecimal.ZERO;int quantity=0;
        for(Item item:submission.items()) {
            // Locking current read: authorization's earlier staff query must not pin an old RR menu snapshot.
            var menu=jdbc.queryForList("SELECT dish_id,dish_name,sale_price,is_active FROM dish_master WHERE dish_id=? AND store_id=? FOR SHARE",item.dishId(),store);
            if(menu.isEmpty()) throw new Rejected(400,"购物车中有不存在的菜品，请刷新菜单");
            var dish=menu.get(0);Object active=dish.get("is_active");
            if(!(Boolean.TRUE.equals(active) || active instanceof Number n && n.intValue()==1)) throw new Rejected(409,"dish_unavailable","购物车中有已下架菜品，请刷新菜单");
            if(!(dish.get("sale_price") instanceof BigDecimal price) || price.signum()<0) throw new Rejected(409,"dish_price_invalid","菜品价格无效，请联系店员核对");
            BigDecimal subtotal=price.multiply(BigDecimal.valueOf(item.quantity()));
            var detail=new BookingDishDetail();detail.setStoreId(store);detail.setBookingId(bookingId);detail.setDishId(dish.get("dish_id").toString());
            detail.setDishName(Objects.toString(dish.get("dish_name"),""));detail.setDishQuantity(item.quantity());detail.setUnitPrice(price);detail.setSubtotal(subtotal);
            detail.setKitchenStatus("pending");detail.setCreatedAt(LocalDateTime.now());
            detail=details.saveAndFlush(detail);ids.add(detail.getDishBookingId());names.add(detail.getDishName());amount=amount.add(subtotal);quantity+=item.quantity();
        }
        Map<String,Object> result=new LinkedHashMap<>();result.put("client_request_id",submission.clientRequestId());result.put("booking_id",bookingId);
        result.put("status","committed");result.put("added_dishes",ids.size());result.put("added_quantity",quantity);result.put("added_amount",amount);result.put("dish_booking_ids",ids);
        String encoded;
        try { encoded=JSON.writeValueAsString(result); } catch(Exception e) { throw new IllegalStateException("提交收据保存失败",e); }
        jdbc.update("INSERT INTO ipad_batch_request(store_id,booking_master_id,booking_id,client_request_id,payload_sha256,operator_id,result_json) VALUES(?,?,?,?,?,?,?)",store,parentId,bookingId,submission.clientRequestId(),submission.fingerprint(),authorizedStaff,encoded);
        BigDecimal committedAmount=amount;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try { notifications.publish(NotifyEvent.builder().eventType(NotifyEvent.NotifyType.ORDER_CREATED).storeId(store)
                    .title("客人自助加菜："+String.join("、",names)).content(bookingId+"单 · 客人自助加菜 "+ids.size()+" 道 · 合计¥"+committedAmount)
                    .priority(NotifyEvent.Priority.HIGH).senderId(authorizedStaff).senderName("客人自助点餐")
                    .receiverType(NotifyEvent.ReceiverType.ALL).relatedType("order").triggerTime(LocalDateTime.now()).build()); }
                catch(Exception ignored) { org.slf4j.LoggerFactory.getLogger(IpadBatchSubmissionService.class).warn("加菜提交已完成，通知发送失败"); }
            }
        });
        return decode(encoded);
    }
    private static Map<String,Object> decode(String json) {
        try { return JSON.readValue(json,new TypeReference<Map<String,Object>>(){}); }
        catch(Exception e) { throw new IllegalStateException("提交收据读取失败",e); }
    }
}

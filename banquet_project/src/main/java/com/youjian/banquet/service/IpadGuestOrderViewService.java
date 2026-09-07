package com.youjian.banquet.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.security.SecureRandom;
import java.time.Clock;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/** Dedicated process-local read capability; never a login token or a write authorization. */
@Service
public class IpadGuestOrderViewService {
    public static class OrderDataException extends RuntimeException { public OrderDataException(){super("订单数据不完整");} }
    private static final String PURPOSE="ipad:order-view";
    private static final long TTL=1800000;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final BCryptPasswordEncoder passwords=new BCryptPasswordEncoder();
    private final SecureRandom random=new SecureRandom();
    private final Map<String,Grant> grants=new HashMap<>();
    private record Grant(long store,String device,String booking,long staff,long expires,String purpose) {}
    @Autowired public IpadGuestOrderViewService(JdbcTemplate jdbc) {this(jdbc,Clock.systemUTC());}
    public IpadGuestOrderViewService(JdbcTemplate jdbc,Clock clock) {this.jdbc=jdbc;this.clock=clock;}

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Map<String,Object> authorize(long store,String device,String username,String password,String booking) {
        scope(store,device,booking);
        if(username==null||username.isBlank()||username.length()>100||password==null||password.isEmpty()||password.length()>256)
            throw new IllegalArgumentException("请填写有效员工账号、密码和订单");
        var staff=jdbc.queryForList("SELECT staff_id,staff_password FROM staff_master WHERE store_id=? AND (staff_account=? OR staff_phone=?) AND employment_status IN ('active','在职') LIMIT 2",store,username,username);
        if(staff.size()!=1)throw denied();
        Object encoded=staff.get(0).get("staff_password");boolean matches=false;
        if(encoded instanceof String hash && hash.matches("\\$2[aby]\\$[0-9]{2}\\$.*")) {
            try {matches=passwords.matches(password,hash);} catch(IllegalArgumentException ignored) {}
        }
        if(!matches)throw denied();
        booking(store,booking);
        long staffId=positive(staff.get(0).get("staff_id"));
        String token;
        synchronized(grants) {
            long now=clock.millis();grants.values().removeIf(g->g.expires<=now);
            if(grants.size()>=10000)throw new IllegalStateException("查看授权繁忙");
            byte[] bytes=new byte[32];random.nextBytes(bytes);token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            grants.put(token,new Grant(store,device,booking,staffId,now+TTL,PURPOSE));
        }
        return Map.of("order_view_token",token,"expires_in",1800,"booking_id",booking,"purpose",PURPOSE);
    }

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Map<String,Object> detail(long store,String device,String booking,String token) {
        scope(store,device,booking);Grant grant;
        if(token==null||!token.matches("[A-Za-z0-9_-]{43}"))throw denied();
        synchronized(grants) {
            long now=clock.millis();grants.values().removeIf(g->g.expires<=now);grant=grants.get(token);
        }
        if(grant==null||grant.store!=store||!grant.device.equals(device)||!grant.booking.equals(booking)||!PURPOSE.equals(grant.purpose))throw denied();
        Integer valid=jdbc.queryForObject("SELECT COUNT(*) FROM staff_master WHERE staff_id=? AND store_id=? AND employment_status IN ('active','在职')",Integer.class,grant.staff,store);
        if(valid==null||valid!=1) {synchronized(grants){grants.remove(token);}throw denied();}
        Map<String,Object> order=booking(store,booking);
        var links=jdbc.queryForList("SELECT bt.table_booking_id,bt.table_id,COALESCE(bt.table_name,tm.table_name) AS table_name,tm.table_id AS verified_table_id FROM booking_table bt LEFT JOIN table_master tm ON tm.table_id=bt.table_id AND tm.store_id=bt.store_id WHERE bt.booking_id=? AND bt.store_id=? ORDER BY bt.table_booking_id",booking,store);
        List<Map<String,Object>> tables=new ArrayList<>();
        for(var row:links) {
            positive(row.get("verified_table_id"));var table=new LinkedHashMap<String,Object>();
            table.put("table_booking_id",positive(row.get("table_booking_id")));table.put("table_id",positive(row.get("table_id")));table.put("table_name",row.get("table_name"));tables.add(table);
        }
        var lines=jdbc.queryForList("SELECT dish_booking_id,dish_id,dish_name,dish_quantity,unit_price,subtotal,kitchen_status FROM booking_dish_detail WHERE booking_id=? AND store_id=? AND (kitchen_status IS NULL OR kitchen_status NOT IN ('cancelled','refunded')) ORDER BY dish_booking_id",booking,store);
        List<Map<String,Object>> dishes=new ArrayList<>();BigDecimal total=BigDecimal.ZERO;
        for(var row:lines) {
            var dish=new LinkedHashMap<String,Object>();dish.put("dish_booking_id",positive(row.get("dish_booking_id")));
            dish.put("dish_id",row.get("dish_id"));dish.put("dish_name",row.get("dish_name"));long quantity=positive(row.get("dish_quantity"));dish.put("dish_quantity",quantity);
            BigDecimal price=money(row.get("unit_price")),subtotal=money(row.get("subtotal"));
            if(price.multiply(BigDecimal.valueOf(quantity)).compareTo(subtotal)!=0)throw new OrderDataException();
            dish.put("unit_price",price.toPlainString());dish.put("subtotal",subtotal.toPlainString());dish.put("kitchen_status",row.get("kitchen_status"));
            total=total.add(subtotal);dishes.add(dish);
        }
        var result=new LinkedHashMap<String,Object>();result.put("booking_id",booking);result.put("store_id",store);result.put("booking_status",order.get("booking_status"));result.put("payment_status",order.get("payment_status"));
        result.put("read_only",true);result.put("tables",tables);result.put("dishes",dishes);result.put("total_amount",total.setScale(2).toPlainString());result.put("amount_basis","active_dish_subtotal");return result;
    }
    private Map<String,Object> booking(long store,String id) {
        var rows=jdbc.queryForList("SELECT booking_id,booking_status,payment_status FROM booking_master WHERE booking_id=? AND store_id=? LIMIT 2",id,store);
        if(rows.size()!=1)throw denied();return rows.get(0);
    }
    private static void scope(long store,String device,String booking) {
        if(store<=0||device==null||device.isBlank()||booking==null||booking.isBlank()||booking.length()>255)throw new IllegalArgumentException("订单查看范围无效");
    }
    private static long positive(Object value) {
        if(!(value instanceof Number))throw new OrderDataException();
        long number=new BigDecimal(value.toString()).longValueExact();if(number<=0)throw new OrderDataException();return number;
    }
    private static BigDecimal money(Object value) {
        if(!(value instanceof BigDecimal number)||number.signum()<0)throw new OrderDataException();
        return number.setScale(2,RoundingMode.UNNECESSARY);
    }
    private static SecurityException denied(){return new SecurityException("查看授权无效，请由本店员工重新授权");}
}

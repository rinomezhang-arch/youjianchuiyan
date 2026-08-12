package com.youjian.banquet.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CostProcurementLoopService {
    private final JdbcTemplate jdbc;

    public CostProcurementLoopService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Map<String, Object> dashboard(long storeId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dishes", scalar("SELECT COUNT(*) FROM dish_master WHERE store_id=?", storeId));
        result.put("costReadyDishes", scalar("SELECT COUNT(DISTINCT dish_id) FROM dish_cost_card WHERE store_id=? AND approval_status='APPROVED' AND effective_from<=NOW() AND (effective_to IS NULL OR effective_to>NOW())", storeId));
        result.put("draftRecipes", scalar("SELECT COUNT(*) FROM recipe_draft WHERE store_id=? AND status IN ('DRAFT','UNRESOLVED')", storeId));
        result.put("pendingManagerApproval", scalar("SELECT COUNT(*) FROM procurement_request WHERE store_id=? AND status='PENDING_MANAGER'", storeId));
        result.put("pendingGmApproval", scalar("SELECT COUNT(*) FROM procurement_request WHERE store_id=? AND status='PENDING_GM'", storeId));
        result.put("pendingReceipt", scalar("SELECT COUNT(*) FROM purchase_order WHERE store_id=? AND status IN ('APPROVED','PARTIALLY_RECEIVED')", storeId));
        result.put("pendingRequisition", scalar("SELECT COUNT(*) FROM requisition_order WHERE store_id=? AND approval_status IN ('SUBMITTED','APPROVED','PARTIALLY_ISSUED')", storeId));
        return result;
    }

    public Map<String, Object> validatePackage(long storeId, String packageId) {
        List<Map<String, Object>> blockers = jdbc.queryForList("""
            SELECT pd.dish_id, COALESCE(dm.dish_name,pd.dish_id) dish_name,
              CASE WHEN cc.cost_card_id IS NULL THEN 'MISSING_APPROVED_COST_CARD' ELSE NULL END blocker
            FROM package_dish_detail pd
            LEFT JOIN dish_master dm ON dm.store_id=pd.store_id AND dm.dish_id=pd.dish_id
            LEFT JOIN dish_cost_card cc ON cc.store_id=pd.store_id AND cc.dish_id=pd.dish_id
              AND cc.approval_status='APPROVED' AND cc.effective_from<=NOW() AND (cc.effective_to IS NULL OR cc.effective_to>NOW())
            WHERE pd.store_id=? AND pd.package_id=? AND cc.cost_card_id IS NULL
            """, storeId, packageId);
        return Map.of("packageId", packageId, "ready", blockers.isEmpty(), "blockers", blockers);
    }

    @Transactional
    public Map<String, Object> approveCostCard(long cardId, String approver) {
        int details = scalar("SELECT COUNT(*) FROM dish_cost_card_detail WHERE cost_card_id=? AND ingredient_id IS NOT NULL AND gross_quantity>0 AND yield_rate>0", cardId);
        if (details == 0) throw new IllegalStateException("成本卡缺少有效原料、毛料数量或出成率");
        int invalid = scalar("SELECT COUNT(*) FROM dish_cost_card_detail WHERE cost_card_id=? AND (gross_quantity<=0 OR yield_rate<=0 OR yield_rate>100 OR price_snapshot IS NULL)", cardId);
        if (invalid > 0) throw new IllegalStateException("成本卡存在无效数量、出成率或价格快照");
        jdbc.update("UPDATE dish_cost_card SET approval_status='APPROVED',status='active',approved_by=?,approved_at=NOW(),effective_from=COALESCE(effective_from,NOW()) WHERE cost_card_id=?", approver, cardId);
        jdbc.update("UPDATE dish_master d JOIN dish_cost_card c ON c.store_id=d.store_id AND c.dish_id=d.dish_id SET d.cost_price=c.standard_cost,d.is_active=1,d.usage_type='formal' WHERE c.cost_card_id=?", cardId);
        return jdbc.queryForMap("SELECT * FROM dish_cost_card WHERE cost_card_id=?", cardId);
    }

    @Transactional
    public Map<String, Object> expandPackage(long storeId, String packageId, BigDecimal tables, String operator) {
        Map<String, Object> validation = validatePackage(storeId, packageId);
        if (!Boolean.TRUE.equals(validation.get("ready"))) throw new IllegalStateException("菜单含未审批成本卡菜品，不能展开需求");
        if (tables == null || tables.signum() <= 0) throw new IllegalArgumentException("桌数必须大于 0");
        String no = "MR" + System.currentTimeMillis();
        jdbc.update("INSERT INTO material_requirement_snapshot(store_id,requirement_no,source_type,source_id,menu_id,serving_count,status,calculated_by,calculated_at) VALUES(?,?,'PACKAGE',?,?,?,'CALCULATED',?,NOW())", storeId, no, packageId, packageId, tables, operator);
        long requirementId = lastId();
        jdbc.update("""
            INSERT INTO material_requirement_detail(requirement_id,dish_id,cost_card_id,ingredient_id,unit,net_quantity,yield_rate,gross_quantity,available_quantity,in_transit_quantity,suggested_purchase_quantity)
            SELECT ?,pd.dish_id,cc.cost_card_id,cd.ingredient_id,cd.unit,
              SUM(COALESCE(cd.net_quantity,cd.standard_quantity)*pd.dish_quantity*?),
              cd.yield_rate/100,
              SUM(cd.gross_quantity*pd.dish_quantity*?),
              COALESCE(MAX(im.current_stock),0),0,
              GREATEST(SUM(cd.gross_quantity*pd.dish_quantity*?)-COALESCE(MAX(im.current_stock),0),0)
            FROM package_dish_detail pd
            JOIN dish_cost_card cc ON cc.store_id=pd.store_id AND cc.dish_id=pd.dish_id AND cc.approval_status='APPROVED'
            JOIN dish_cost_card_detail cd ON cd.cost_card_id=cc.cost_card_id
            LEFT JOIN ingredient_master im ON im.store_id=pd.store_id AND im.ingredient_id=cd.ingredient_id
            WHERE pd.store_id=? AND pd.package_id=?
            GROUP BY pd.dish_id,cc.cost_card_id,cd.ingredient_id,cd.unit,cd.yield_rate
            """, requirementId, tables, tables, tables, storeId, packageId);
        return requirement(requirementId);
    }

    public Map<String, Object> requirement(long id) {
        Map<String, Object> result = new LinkedHashMap<>(jdbc.queryForMap("SELECT * FROM material_requirement_snapshot WHERE requirement_id=?", id));
        result.put("details", jdbc.queryForList("SELECT d.*,im.ingredient_name FROM material_requirement_detail d LEFT JOIN ingredient_master im ON im.store_id=(SELECT store_id FROM material_requirement_snapshot WHERE requirement_id=d.requirement_id) AND im.ingredient_id=d.ingredient_id WHERE d.requirement_id=? ORDER BY d.ingredient_id", id));
        return result;
    }

    @Transactional
    public Map<String, Object> createProcurement(long requirementId, String requester) {
        Map<String,Object> req = jdbc.queryForMap("SELECT * FROM material_requirement_snapshot WHERE requirement_id=?", requirementId);
        long storeId = ((Number)req.get("store_id")).longValue();
        int count = scalar("SELECT COUNT(*) FROM material_requirement_detail WHERE requirement_id=? AND suggested_purchase_quantity>0", requirementId);
        if (count == 0) throw new IllegalStateException("没有需要采购的缺口原料");
        String no = "PR" + System.currentTimeMillis();
        jdbc.update("INSERT INTO procurement_request(store_id,request_no,department_name,requester_name,request_date,status,reason,urgency) VALUES(?,?, '厨房',?,?,'PENDING_MANAGER',?,'normal')", storeId, no, requester, LocalDate.now(), "需求单"+req.get("requirement_no"));
        long requestId = lastId();
        jdbc.update("""
          INSERT INTO procurement_request_detail(request_id,requirement_detail_id,ingredient_id,ingredient_name,unit,requested_quantity,estimated_unit_price)
          SELECT ?,d.requirement_detail_id,d.ingredient_id,im.ingredient_name,d.unit,d.suggested_purchase_quantity,COALESCE(im.avg_price,0)
          FROM material_requirement_detail d JOIN ingredient_master im ON im.ingredient_id=d.ingredient_id AND im.store_id=?
          WHERE d.requirement_id=? AND d.suggested_purchase_quantity>0
          """, requestId, storeId, requirementId);
        jdbc.update("UPDATE procurement_request SET total_amount=(SELECT SUM(requested_quantity*estimated_unit_price) FROM procurement_request_detail WHERE request_id=?) WHERE request_id=?", requestId, requestId);
        jdbc.update("UPDATE material_requirement_snapshot SET status='PROCUREMENT_CREATED' WHERE requirement_id=?", requirementId);
        return procurement(requestId);
    }

    @Transactional
    public Map<String, Object> approveProcurement(long requestId, int level, String approver, String comment) {
        Map<String,Object> request = jdbc.queryForMap("SELECT * FROM procurement_request WHERE request_id=? FOR UPDATE", requestId);
        String current = String.valueOf(request.get("status"));
        String expected = level == 1 ? "PENDING_MANAGER" : "PENDING_GM";
        if (!expected.equals(current)) throw new IllegalStateException("当前状态不允许该级审批：" + current);
        String next = level == 1 ? "PENDING_GM" : "APPROVED";
        jdbc.update("UPDATE procurement_request SET status=?,approver_name=?,approve_time=NOW(),approve_comment=? WHERE request_id=?", next, approver, comment, requestId);
        jdbc.update("UPDATE procurement_request_detail SET approved_quantity=requested_quantity WHERE request_id=?", requestId);
        jdbc.update("INSERT INTO procurement_approval_record(store_id,business_type,business_id,approval_level,from_status,to_status,action,approver_name,comment) VALUES(?,'PROCUREMENT_REQUEST',?,?,?,?, 'APPROVE',?,?)", request.get("store_id"), requestId, level, current, next, approver, comment);
        if (level == 2) createPurchaseOrder(requestId, approver);
        return procurement(requestId);
    }

    public Map<String,Object> procurement(long requestId) {
        Map<String,Object> result = new LinkedHashMap<>(jdbc.queryForMap("SELECT * FROM procurement_request WHERE request_id=?", requestId));
        result.put("details", jdbc.queryForList("SELECT * FROM procurement_request_detail WHERE request_id=?", requestId));
        result.put("approvals", jdbc.queryForList("SELECT * FROM procurement_approval_record WHERE business_type='PROCUREMENT_REQUEST' AND business_id=? ORDER BY approval_level", requestId));
        result.put("orders", jdbc.queryForList("SELECT order_id,order_no,status,total_quantity,total_amount,received_quantity FROM purchase_order WHERE request_id=?", requestId));
        return result;
    }

    private long createPurchaseOrder(long requestId, String approver) {
        Map<String,Object> request = jdbc.queryForMap("SELECT * FROM procurement_request WHERE request_id=?", requestId);
        String orderNo = "PO" + System.currentTimeMillis();
        jdbc.update("INSERT INTO purchase_order(store_id,request_id,order_no,order_date,total_quantity,total_amount,status,approver_name,approve_time) SELECT store_id,request_id,?,CURDATE(),COALESCE(SUM(approved_quantity),0),COALESCE(SUM(approved_quantity*estimated_unit_price),0),'APPROVED',?,NOW() FROM procurement_request r JOIN procurement_request_detail d USING(request_id) WHERE r.request_id=? GROUP BY r.store_id,r.request_id", orderNo, approver, requestId);
        long orderId = lastId();
        jdbc.update("INSERT INTO purchase_order_detail(order_id,store_id,line_no,ingredient_id,ingredient_name,unit,quantity,unit_price,amount) SELECT ?,?,ROW_NUMBER() OVER(ORDER BY request_detail_id),ingredient_id,ingredient_name,unit,approved_quantity,estimated_unit_price,approved_quantity*estimated_unit_price FROM procurement_request_detail WHERE request_id=? AND approved_quantity>0", orderId, request.get("store_id"), requestId);
        jdbc.update("UPDATE procurement_request_detail d JOIN purchase_order_detail p ON p.order_id=? AND p.ingredient_id=d.ingredient_id SET d.ordered_quantity=d.approved_quantity WHERE d.request_id=?", orderId, requestId);
        return orderId;
    }

    @Transactional
    public Map<String,Object> receive(long orderId, String receiptNo, String inspector, List<Map<String,Object>> lines) {
        Map<String,Object> order = jdbc.queryForMap("SELECT * FROM purchase_order WHERE order_id=? FOR UPDATE", orderId);
        String status = String.valueOf(order.get("status"));
        if (!List.of("APPROVED","PARTIALLY_RECEIVED","ordered").contains(status)) throw new IllegalStateException("采购单尚未批准或已完成");
        jdbc.update("INSERT INTO purchase_receipt(store_id,receipt_no,receipt_date,order_id,order_no,status,warehouse_keeper_name) VALUES(?,?,?, ?,?,'confirmed',?)", order.get("store_id"), receiptNo, LocalDate.now(), orderId, order.get("order_no"), inspector);
        long receiptId = lastId();
        BigDecimal totalAccepted = BigDecimal.ZERO;
        for (Map<String,Object> line : lines) {
            long detailId = number(line.get("orderDetailId")).longValue();
            BigDecimal delivered = decimal(line.get("deliveredQuantity"));
            BigDecimal accepted = decimal(line.get("acceptedQuantity"));
            BigDecimal rejected = decimal(line.get("rejectedQuantity"));
            if (delivered.signum()<=0 || accepted.signum()<0 || rejected.signum()<0 || accepted.add(rejected).compareTo(delivered)!=0) throw new IllegalArgumentException("实收数量必须等于合格加拒收数量");
            Map<String,Object> detail = jdbc.queryForMap("SELECT * FROM purchase_order_detail WHERE detail_id=? AND order_id=? FOR UPDATE", detailId, orderId);
            BigDecimal remaining = decimal(detail.get("quantity")).subtract(decimal(detail.get("received_quantity")));
            if (delivered.compareTo(remaining)>0) throw new IllegalArgumentException("验收数量超过未收数量");
            String ingredientId = String.valueOf(detail.get("ingredient_id"));
            String batchNo = String.valueOf(line.getOrDefault("batchNo", receiptNo+"-"+detailId));
            String idem = receiptNo+":"+detailId;
            String qualityResult = rejected.signum()>0 ? (accepted.signum()>0?"PARTIAL":"REJECTED") : "QUALIFIED";
            jdbc.update("INSERT INTO receipt_quality_detail(store_id,receipt_id,order_detail_id,ingredient_id,delivered_quantity,accepted_quantity,rejected_quantity,rejection_reason,batch_no,production_date,expiry_date,actual_unit_price,quality_result,inspector_name,idempotency_key,confirmed_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())", order.get("store_id"), receiptId, detailId, ingredientId, delivered, accepted, rejected, line.get("rejectionReason"), batchNo, line.get("productionDate"), line.get("expiryDate"), detail.get("unit_price"), qualityResult, inspector, idem);
            jdbc.update("INSERT INTO purchase_receipt_detail(receipt_id,store_id,line_no,order_detail_id,ingredient_id,ingredient_name,unit,order_quantity,actual_quantity,unit_price,amount,quality_status,remark) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)", receiptId, order.get("store_id"), detailId, detailId, ingredientId, detail.get("ingredient_name"), detail.get("unit"), detail.get("quantity"), accepted, detail.get("unit_price"), accepted.multiply(decimal(detail.get("unit_price"))), qualityResult.toLowerCase(), line.get("rejectionReason"));
            jdbc.update("UPDATE purchase_order_detail SET received_quantity=received_quantity+?,returned_quantity=returned_quantity+? WHERE detail_id=?", accepted, rejected, detailId);
            if (accepted.signum()>0) {
                Long qualityDetailId = jdbc.queryForObject("SELECT quality_detail_id FROM receipt_quality_detail WHERE idempotency_key=?", Long.class, idem);
                jdbc.update("INSERT INTO inventory_batch_ledger(store_id,ingredient_id,batch_no,received_quantity,available_quantity,unit,unit_cost,production_date,expiry_date,receipt_quality_detail_id,status) VALUES(?,?,?,?,?,?,?,?,?,?,'AVAILABLE') ON DUPLICATE KEY UPDATE received_quantity=received_quantity+VALUES(received_quantity),available_quantity=available_quantity+VALUES(available_quantity)", order.get("store_id"), ingredientId, batchNo, accepted, accepted, detail.get("unit"), detail.get("unit_price"), line.get("productionDate"), line.get("expiryDate"), qualityDetailId);
                long batchId = Objects.requireNonNull(jdbc.queryForObject("SELECT inventory_batch_id FROM inventory_batch_ledger WHERE store_id=? AND ingredient_id=? AND batch_no=?", Long.class, order.get("store_id"), ingredientId, batchNo));
                BigDecimal amount = accepted.multiply(decimal(detail.get("unit_price")));
                jdbc.update("INSERT INTO inventory_movement_ledger(store_id,ingredient_id,inventory_batch_id,movement_type,business_type,business_id,quantity,unit,unit_cost,amount,idempotency_key,operator_name,occurred_at) VALUES(?,?,?,'RECEIPT','PURCHASE_RECEIPT',?,?,?,?,?,?,?,NOW())", order.get("store_id"), ingredientId, batchId, receiptId, accepted, detail.get("unit"), detail.get("unit_price"), amount, idem, inspector);
                jdbc.update("UPDATE ingredient_master SET current_stock=COALESCE(current_stock,0)+?,avg_price=? WHERE store_id=? AND ingredient_id=?", accepted, detail.get("unit_price"), order.get("store_id"), ingredientId);
                totalAccepted = totalAccepted.add(accepted);
            }
        }
        int remainingLines = scalar("SELECT COUNT(*) FROM purchase_order_detail WHERE order_id=? AND received_quantity<quantity", orderId);
        jdbc.update("UPDATE purchase_order SET status=?,received_quantity=COALESCE(received_quantity,0)+? WHERE order_id=?", remainingLines==0?"RECEIVED":"PARTIALLY_RECEIVED", totalAccepted, orderId);
        return Map.of("receiptId",receiptId,"acceptedQuantity",totalAccepted,"orderStatus",remainingLines==0?"RECEIVED":"PARTIALLY_RECEIVED");
    }

    @Transactional
    public Map<String,Object> createRequisition(long requirementId, String requester) {
        Map<String,Object> requirement = jdbc.queryForMap("SELECT * FROM material_requirement_snapshot WHERE requirement_id=?", requirementId);
        String no = "RQ" + System.currentTimeMillis();
        jdbc.update("INSERT INTO requisition_order(store_id,source_requirement_id,requisition_no,department_name,requester_name,requisition_date,status,approval_status,reason) VALUES(?,?,?,'厨房',?,CURDATE(),'submitted','SUBMITTED',?)", requirement.get("store_id"), requirementId, no, requester, "来源需求单" + requirement.get("requirement_no"));
        long requisitionId = lastId();
        jdbc.update("INSERT INTO requisition_detail(store_id,requisition_id,line_no,ingredient_id,ingredient_name,unit,request_quantity,issue_quantity,unit_price,amount) SELECT s.store_id,?,ROW_NUMBER() OVER(ORDER BY d.requirement_detail_id),d.ingredient_id,im.ingredient_name,d.unit,d.gross_quantity,0,COALESCE(im.avg_price,0),d.gross_quantity*COALESCE(im.avg_price,0) FROM material_requirement_detail d JOIN material_requirement_snapshot s ON s.requirement_id=d.requirement_id JOIN ingredient_master im ON im.store_id=s.store_id AND im.ingredient_id=d.ingredient_id WHERE d.requirement_id=?", requisitionId, requirementId);
        jdbc.update("INSERT INTO material_issue_detail(store_id,requisition_id,ingredient_id,requested_quantity,unit,source_requirement_detail_id) SELECT s.store_id,?,d.ingredient_id,d.gross_quantity,d.unit,d.requirement_detail_id FROM material_requirement_detail d JOIN material_requirement_snapshot s ON s.requirement_id=d.requirement_id WHERE d.requirement_id=?", requisitionId, requirementId);
        jdbc.update("UPDATE requisition_order SET total_amount=(SELECT SUM(amount) FROM requisition_detail WHERE requisition_id=?) WHERE requisition_id=?", requisitionId, requisitionId);
        return requisition(requisitionId);
    }

    public Map<String,Object> requisition(long id) {
        Map<String,Object> result = new LinkedHashMap<>(jdbc.queryForMap("SELECT * FROM requisition_order WHERE requisition_id=?", id));
        result.put("details", jdbc.queryForList("SELECT * FROM material_issue_detail WHERE requisition_id=? ORDER BY issue_detail_id", id));
        result.put("movements", jdbc.queryForList("SELECT * FROM inventory_movement_ledger WHERE business_type='REQUISITION' AND business_id=? ORDER BY movement_id", id));
        return result;
    }

    @Transactional
    public Map<String,Object> approveRequisition(long id, String approver) {
        int updated = jdbc.update("UPDATE requisition_order SET approval_status='APPROVED',status='approved',approved_by=?,approved_at=NOW(),approver_name=?,approve_time=NOW() WHERE requisition_id=? AND approval_status IN ('DRAFT','SUBMITTED')", approver, approver, id);
        if (updated==0) throw new IllegalStateException("领料单状态不允许审批");
        jdbc.update("UPDATE material_issue_detail SET approved_quantity=requested_quantity WHERE requisition_id=?", id);
        return requisition(id);
    }

    @Transactional
    public Map<String,Object> issue(long requisitionId, String operator, List<Map<String,Object>> lines) {
        Map<String,Object> req = jdbc.queryForMap("SELECT * FROM requisition_order WHERE requisition_id=? FOR UPDATE", requisitionId);
        if (!List.of("APPROVED","PARTIALLY_ISSUED").contains(String.valueOf(req.get("approval_status")))) throw new IllegalStateException("领料单未审批");
        for (Map<String,Object> line: lines) {
            String ingredientId=String.valueOf(line.get("ingredientId")); BigDecimal quantity=decimal(line.get("quantity"));
            if(quantity.signum()<=0) throw new IllegalArgumentException("发料数量必须大于 0");
            Map<String,Object> issueDetail = jdbc.queryForMap("SELECT * FROM material_issue_detail WHERE requisition_id=? AND ingredient_id=? FOR UPDATE", requisitionId, ingredientId);
            BigDecimal approved = decimal(issueDetail.get("approved_quantity"));
            BigDecimal issued = decimal(issueDetail.get("issued_quantity"));
            if (approved.signum() <= 0 || issued.add(quantity).compareTo(approved) > 0) throw new IllegalArgumentException("发料数量超过已审批数量：" + ingredientId);
            BigDecimal remain=quantity;
            List<Map<String,Object>> batches=jdbc.queryForList("SELECT * FROM inventory_batch_ledger WHERE store_id=? AND ingredient_id=? AND status='AVAILABLE' AND available_quantity>0 ORDER BY expiry_date IS NULL,expiry_date,created_at FOR UPDATE",req.get("store_id"),ingredientId);
            for(Map<String,Object> batch:batches){ if(remain.signum()<=0) break; BigDecimal available=decimal(batch.get("available_quantity")); BigDecimal take=available.min(remain); String idem="ISSUE:"+requisitionId+":"+batch.get("inventory_batch_id")+":"+UUID.randomUUID(); BigDecimal cost=decimal(batch.get("unit_cost")); jdbc.update("UPDATE inventory_batch_ledger SET available_quantity=available_quantity-?,status=IF(available_quantity-?=0,'DEPLETED','AVAILABLE') WHERE inventory_batch_id=?",take,take,batch.get("inventory_batch_id")); jdbc.update("INSERT INTO inventory_movement_ledger(store_id,ingredient_id,inventory_batch_id,movement_type,business_type,business_id,quantity,unit,unit_cost,amount,idempotency_key,operator_name,occurred_at) VALUES(?,?,?,'ISSUE','REQUISITION',?,?,?,?,?,?,?,NOW())",req.get("store_id"),ingredientId,batch.get("inventory_batch_id"),requisitionId,take.negate(),batch.get("unit"),cost,take.multiply(cost).negate(),idem,operator); remain=remain.subtract(take); }
            if(remain.signum()>0) throw new IllegalStateException("原料库存不足："+ingredientId);
            jdbc.update("UPDATE ingredient_master SET current_stock=current_stock-? WHERE store_id=? AND ingredient_id=?",quantity,req.get("store_id"),ingredientId);
            jdbc.update("UPDATE material_issue_detail SET issued_quantity=issued_quantity+? WHERE requisition_id=? AND ingredient_id=?", quantity, requisitionId, ingredientId);
            jdbc.update("UPDATE requisition_detail SET issue_quantity=COALESCE(issue_quantity,0)+? WHERE requisition_id=? AND ingredient_id=?", quantity, requisitionId, ingredientId);
        }
        int pending = scalar("SELECT COUNT(*) FROM material_issue_detail WHERE requisition_id=? AND issued_quantity<approved_quantity", requisitionId);
        String approvalStatus = pending == 0 ? "ISSUED" : "PARTIALLY_ISSUED";
        jdbc.update("UPDATE requisition_order SET approval_status=?,status=?,warehouse_keeper_name=?,issue_time=NOW() WHERE requisition_id=?",approvalStatus,pending==0?"issued":"partial_issued",operator,requisitionId);
        return requisition(requisitionId);
    }

    private int scalar(String sql,Object... args){ Integer value=jdbc.queryForObject(sql,Integer.class,args); return value==null?0:value; }
    private long lastId(){ return Objects.requireNonNull(jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class)); }
    private BigDecimal decimal(Object value){ if(value==null)return BigDecimal.ZERO; return new BigDecimal(String.valueOf(value)).setScale(4,RoundingMode.HALF_UP); }
    private Number number(Object value){ if(value instanceof Number n)return n; return Long.parseLong(String.valueOf(value)); }
}

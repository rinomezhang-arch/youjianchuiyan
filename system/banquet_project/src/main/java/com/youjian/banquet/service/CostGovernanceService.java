package com.youjian.banquet.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CostGovernanceService {
    private final JdbcTemplate jdbc;

    public CostGovernanceService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Map<String, Object> validateDish(long storeId, String dishId) {
        List<String> blockers = new ArrayList<>();
        Integer drafts = jdbc.queryForObject("SELECT COUNT(*) FROM recipe_draft WHERE store_id=? AND dish_id=? AND status<>'APPROVED'", Integer.class, storeId, dishId);
        if (drafts != null && drafts > 0) blockers.add("配方草稿尚未全部审核");
        Integer cards = jdbc.queryForObject("SELECT COUNT(*) FROM dish_cost_card c WHERE c.store_id=? AND c.dish_id=? AND c.approval_status='APPROVED' AND c.status='active' AND c.yield_rate>0 AND EXISTS(SELECT 1 FROM dish_cost_card_detail d WHERE d.cost_card_id=c.cost_card_id AND d.gross_quantity>0 AND d.yield_rate>0 AND d.price_snapshot>=0)", Integer.class, storeId, dishId);
        if (cards == null || cards == 0) blockers.add("缺少已审批且包含原料、出成率、价格快照的有效成本卡");
        return Map.of("dishId", dishId, "eligible", blockers.isEmpty(), "blockers", blockers);
    }

    @Transactional
    public Map<String, Object> activateDish(long storeId, String dishId) {
        Map<String, Object> validation = validateDish(storeId, dishId);
        if (!Boolean.TRUE.equals(validation.get("eligible"))) throw new IllegalArgumentException("菜品不能启用：" + validation.get("blockers"));
        jdbc.update("UPDATE dish_master SET is_active=1,usage_type='used' WHERE store_id=? AND dish_id=?", storeId, dishId);
        return validation;
    }

    public Map<String, Object> validateMenu(long storeId, String menuId) {
        List<Map<String, Object>> invalid = jdbc.queryForList("SELECT md.dish_id,dm.dish_name FROM menu_dish md JOIN dish_master dm ON dm.dish_id=md.dish_id AND dm.store_id=? WHERE md.menu_type=? AND (dm.is_active<>1 OR NOT EXISTS(SELECT 1 FROM dish_cost_card c WHERE c.store_id=? AND c.dish_id=md.dish_id AND c.approval_status='APPROVED' AND c.status='active'))", storeId, menuId, storeId);
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM menu_dish WHERE menu_type=?", Integer.class, menuId);
        List<String> blockers = new ArrayList<>();
        if (count == null || count == 0) blockers.add("菜单未配置菜品");
        if (!invalid.isEmpty()) blockers.add("存在未启用或无有效成本卡的菜品");
        Map<String,Object> result = new LinkedHashMap<>(); result.put("menuId",menuId); result.put("eligible",blockers.isEmpty()); result.put("blockers",blockers); result.put("invalidDishes",invalid); return result;
    }

    @Transactional
    public Map<String, Object> expandRequirement(long storeId, String sourceType, String sourceId, List<Map<String,Object>> dishes, BigDecimal servingCount, String operator) {
        if (dishes == null || dishes.isEmpty()) throw new IllegalArgumentException("需求展开至少需要一道菜");
        String no = "MR" + System.currentTimeMillis();
        jdbc.update("INSERT INTO material_requirement_snapshot(store_id,requirement_no,source_type,source_id,serving_count,status,calculated_by,calculated_at) VALUES(?,?,?,?,?,'CALCULATED',?,?)", storeId,no,sourceType,sourceId,servingCount,operator,LocalDateTime.now());
        long requirementId = Objects.requireNonNull(jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class));
        int lines=0;
        for(Map<String,Object> dish:dishes){
            String dishId=String.valueOf(dish.get("dishId")); BigDecimal dishQty=decimal(dish.getOrDefault("quantity",1));
            Map<String,Object> valid=validateDish(storeId,dishId); if(!Boolean.TRUE.equals(valid.get("eligible"))) throw new IllegalArgumentException("菜品 "+dishId+" 成本数据不完整");
            Long cardId=jdbc.queryForObject("SELECT cost_card_id FROM dish_cost_card WHERE store_id=? AND dish_id=? AND approval_status='APPROVED' AND status='active' ORDER BY version_no DESC LIMIT 1",Long.class,storeId,dishId);
            List<Map<String,Object>> details=jdbc.queryForList("SELECT ingredient_id,unit,net_quantity,yield_rate,gross_quantity FROM dish_cost_card_detail WHERE cost_card_id=?",cardId);
            for(Map<String,Object> detail:details){
                String ingredient=String.valueOf(detail.get("ingredient_id")); BigDecimal multiplier=servingCount.multiply(dishQty); BigDecimal net=decimal(detail.get("net_quantity")).multiply(multiplier); BigDecimal gross=decimal(detail.get("gross_quantity")).multiply(multiplier); BigDecimal yield=decimal(detail.get("yield_rate"));
                BigDecimal stock=Optional.ofNullable(jdbc.queryForObject("SELECT COALESCE(current_stock,0) FROM ingredient_master WHERE store_id=? AND ingredient_id=?",BigDecimal.class,storeId,ingredient)).orElse(BigDecimal.ZERO);
                BigDecimal transit=Optional.ofNullable(jdbc.queryForObject("SELECT COALESCE(SUM(d.quantity-d.received_quantity),0) FROM purchase_order_detail d JOIN purchase_order o ON o.order_id=d.order_id WHERE o.store_id=? AND d.ingredient_id=? AND o.status IN ('approved','ordered','partial_received')",BigDecimal.class,storeId,ingredient)).orElse(BigDecimal.ZERO);
                BigDecimal purchase=gross.subtract(stock).subtract(transit).max(BigDecimal.ZERO).setScale(4,RoundingMode.HALF_UP);
                jdbc.update("INSERT INTO material_requirement_detail(requirement_id,dish_id,cost_card_id,ingredient_id,unit,net_quantity,yield_rate,gross_quantity,available_quantity,in_transit_quantity,suggested_purchase_quantity) VALUES(?,?,?,?,?,?,?,?,?,?,?)",requirementId,dishId,cardId,ingredient,String.valueOf(detail.get("unit")),net,yield,gross,stock,transit,purchase); lines++;
            }
        }
        return Map.of("requirementId",requirementId,"requirementNo",no,"lineCount",lines,"status","CALCULATED");
    }
    private BigDecimal decimal(Object value){try{return new BigDecimal(String.valueOf(value));}catch(Exception e){return BigDecimal.ZERO;}}
}

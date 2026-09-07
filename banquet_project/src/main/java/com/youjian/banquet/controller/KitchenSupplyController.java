package com.youjian.banquet.controller;

import com.youjian.banquet.entity.*;
import com.youjian.banquet.service.KitchenSupplyService;
import com.youjian.banquet.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kitchen-supply")
@RequiredArgsConstructor
public class KitchenSupplyController {

    private final KitchenSupplyService kitchenSupplyService;

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", data);
        return result;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 400);
        result.put("message", message);
        return result;
    }

    /**
     * 请求体兼容两种格式：
     * 1. 旧版扁平格式（现有前端在用）：直接是 PurchaseRequest 实体字段，比如 {storeId, requesterName, ...}
     * 2. 新版带明细格式：{request: {...}, items: [{ingredientId,ingredientName,category,quantity,unit,estimatedPrice,notes}]}
     * procurement_request_item 这张表原来在数据库里存在但从没被任何代码写过（没有Repository），
     * 明细数据一直没地方存——这里把它接上，不是新造功能，是补齐一个本来就该有但漏掉的能力。
     */
    @PostMapping("/purchase-requests")
    public ResponseEntity<Map<String, Object>> createPurchaseRequest(@RequestBody Map<String, Object> body) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

            if (body.containsKey("items")) {
                PurchaseRequest request = mapper.convertValue(body.get("request"), PurchaseRequest.class);
                List<PurchaseRequestItem> items = mapper.convertValue(body.get("items"),
                        mapper.getTypeFactory().constructCollectionType(List.class, PurchaseRequestItem.class));
                return ResponseEntity.ok(success(kitchenSupplyService.createPurchaseRequestWithItems(request, items)));
            }

            PurchaseRequest request = mapper.convertValue(body, PurchaseRequest.class);
            return ResponseEntity.ok(success(kitchenSupplyService.createPurchaseRequest(request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @PutMapping("/purchase-requests/{id}/approve")
    public ResponseEntity<Map<String, Object>> approvePurchaseRequest(
            @PathVariable Long id,
            @RequestParam(required = false) String approver,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String approverName = approver;
            if (approverName == null && body != null) {
                approverName = body.get("approver");
            }
            return ResponseEntity.ok(success(
                    kitchenSupplyService.approvePurchaseRequest(id, approverName)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @GetMapping("/purchase-requests")
    public ResponseEntity<Map<String, Object>> getPurchaseRequests(
            @RequestParam(defaultValue = "1") Long storeId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(success(kitchenSupplyService.getPurchaseRequests(storeId, status)));
    }

    /** 请求体: {receipt: {...}, items: [{ingredientId,ingredientName,unit,orderQuantity,actualQuantity,unitPrice,qualityStatus,remark}]} */
    @PostMapping("/goods-receipts")
    public ResponseEntity<Map<String, Object>> createGoodsReceipt(@RequestBody Map<String, Object> body) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            GoodsReceipt receipt = mapper.convertValue(body.get("receipt"), GoodsReceipt.class);
            List<GoodsReceiptItem> items = body.get("items") == null ? java.util.Collections.emptyList()
                    : mapper.convertValue(body.get("items"),
                        mapper.getTypeFactory().constructCollectionType(List.class, GoodsReceiptItem.class));

            // 门店归属校验（原来完全没做，任何登录员工都能把收货单挂到任意门店）：
            // 有 storeId 就必须是自己门店（总经理不受限）；没传就用当前登录者自己的门店兜底，
            // 非总经理且拿不到当前门店时直接拒绝，不再像 Service 层那样悄悄默认成 storeId=1。
            if (receipt.getStoreId() != null) {
                UserContext.assertStoreAccess(receipt.getStoreId());
            } else if (!UserContext.isGeneralManager()) {
                Long current = UserContext.currentStoreId();
                if (current == null || current == 0L) {
                    throw new IllegalArgumentException("缺少 storeId 参数");
                }
                receipt.setStoreId(current);
            }

            return ResponseEntity.ok(success(kitchenSupplyService.createGoodsReceipt(receipt, items)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(error("入库未完成，请核对关联数据后重试"));
        }
    }

    @PutMapping("/goods-receipts/{receiptId}/accept")
    public ResponseEntity<Map<String,Object>> acceptGoodsReceipt(@PathVariable Long receiptId,
            @RequestBody(required=false) Map<String,String> body) {
        try {
            return ResponseEntity.ok(success(kitchenSupplyService.acceptGoodsReceipt(receiptId,body==null?null:body.get("warehouseKeeperName"))));
        } catch(IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch(Exception e) {
            return ResponseEntity.internalServerError().body(error("验收入库未完成，数据已回滚，请刷新后重试"));
        }
    }

    @GetMapping("/goods-receipts")
    public ResponseEntity<Map<String, Object>> getGoodsReceipts(
            @RequestParam(defaultValue = "1") Long storeId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(success(kitchenSupplyService.getGoodsReceipts(storeId, status)));
    }

    @GetMapping("/goods-receipts/{receiptId}/items")
    public ResponseEntity<Map<String, Object>> getGoodsReceiptItems(@PathVariable Long receiptId) {
        return ResponseEntity.ok(success(kitchenSupplyService.getGoodsReceiptItems(receiptId)));
    }

    @PostMapping("/requisitions")
    public ResponseEntity<Map<String, Object>> createRequisition(
            @RequestBody Map<String,Object> body) {
        try {
            var mapper=new com.fasterxml.jackson.databind.ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            MaterialRequisition requisition=mapper.convertValue(body.get("requisition"),MaterialRequisition.class);
            List<MaterialRequisitionItem> items=mapper.convertValue(body.get("items"),mapper.getTypeFactory().constructCollectionType(List.class,MaterialRequisitionItem.class));
            return ResponseEntity.ok(success(kitchenSupplyService.createRequisitionWithItems(requisition,items)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @GetMapping("/requisitions/{id}/items")
    public ResponseEntity<Map<String,Object>> getRequisitionItems(@PathVariable Long id) {
        return ResponseEntity.ok(success(kitchenSupplyService.getRequisitionItems(id)));
    }

    @PutMapping("/requisitions/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveRequisition(
            @PathVariable Long id,
            @RequestParam(required = false) String approver,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String approverName = approver;
            if (approverName == null && body != null) {
                approverName = body.get("approver");
            }
            return ResponseEntity.ok(success(
                    kitchenSupplyService.approveRequisition(id, approverName)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @GetMapping("/requisitions")
    public ResponseEntity<Map<String, Object>> getRequisitions(
            @RequestParam(defaultValue = "1") Long storeId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(success(kitchenSupplyService.getRequisitions(storeId, status)));
    }

    @PostMapping("/preprocessing")
    public ResponseEntity<Map<String, Object>> createPreprocessingRecord(
            @RequestBody PreprocessingRecord record) {
        try {
            return ResponseEntity.ok(success(kitchenSupplyService.createPreprocessingRecord(record)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @GetMapping("/preprocessing")
    public ResponseEntity<Map<String, Object>> getPreprocessingRecords(
            @RequestParam(defaultValue = "1") Long storeId,
            @RequestParam(required = false) String ingredientId) {
        return ResponseEntity.ok(success(
                kitchenSupplyService.getPreprocessingRecords(storeId, ingredientId)));
    }

    @PostMapping("/cost-cards/calculate")
    public ResponseEntity<Map<String, Object>> calculateCostCard(
            @RequestBody Map<String, Object> body) {
        try {
            String dishId = (String) body.get("dishId");
            Long storeId = body.get("storeId") != null ?
                    Long.valueOf(body.get("storeId").toString()) : 1L;
            return ResponseEntity.ok(success(kitchenSupplyService.calculateAndSaveCostCard(dishId, storeId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @GetMapping("/cost-cards")
    public ResponseEntity<Map<String, Object>> getCostCards(
            @RequestParam(defaultValue = "1") Long storeId) {
        return ResponseEntity.ok(success(kitchenSupplyService.getCostCards(storeId)));
    }

    @GetMapping("/cost-cards/{dishId}")
    public ResponseEntity<Map<String, Object>> getCostCard(
            @PathVariable String dishId,
            @RequestParam(defaultValue = "1") Long storeId) {
        return ResponseEntity.ok(success(
                kitchenSupplyService.getCostCard(dishId, storeId).orElse(null)));
    }

    @PostMapping("/unit-conversions")
    public ResponseEntity<Map<String, Object>> addUnitConversion(@RequestBody UnitConversion conversion) {
        try {
            return ResponseEntity.ok(success(kitchenSupplyService.addUnitConversion(conversion)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @GetMapping("/unit-conversions")
    public ResponseEntity<Map<String, Object>> getUnitConversions(
            @RequestParam(defaultValue = "1") Long storeId) {
        return ResponseEntity.ok(success(kitchenSupplyService.getUnitConversions(storeId)));
    }
}

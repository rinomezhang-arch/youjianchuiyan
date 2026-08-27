package com.youjian.banquet.controller;

import com.youjian.banquet.entity.*;
import com.youjian.banquet.service.KitchenSupplyService;
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

    @PostMapping("/purchase-requests")
    public ResponseEntity<Map<String, Object>> createPurchaseRequest(@RequestBody PurchaseRequest request) {
        try {
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
            return ResponseEntity.ok(success(kitchenSupplyService.createGoodsReceipt(receipt, items)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
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
            @RequestBody MaterialRequisition requisition) {
        try {
            return ResponseEntity.ok(success(kitchenSupplyService.createRequisition(requisition)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
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

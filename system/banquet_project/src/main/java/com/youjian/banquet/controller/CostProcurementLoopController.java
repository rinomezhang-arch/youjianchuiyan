package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.service.CostProcurementLoopService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cost-procurement-loop")
public class CostProcurementLoopController {
    private final CostProcurementLoopService service;

    public CostProcurementLoopController(CostProcurementLoopService service) { this.service = service; }

    @GetMapping("/dashboard")
    public Result<Map<String,Object>> dashboard(@RequestParam(defaultValue="1") long storeId){ return Result.success(service.dashboard(storeId)); }

    @GetMapping("/packages/{packageId}/validate")
    public Result<Map<String,Object>> validate(@PathVariable String packageId,@RequestParam(defaultValue="1") long storeId){ return Result.success(service.validatePackage(storeId,packageId)); }

    @PostMapping("/cost-cards/{cardId}/approve")
    public Result<Map<String,Object>> approveCost(@PathVariable long cardId,@RequestBody Map<String,Object> body){ return Result.success(service.approveCostCard(cardId,text(body,"approver","system"))); }

    @PostMapping("/packages/{packageId}/requirements")
    public Result<Map<String,Object>> expand(@PathVariable String packageId,@RequestBody Map<String,Object> body){ return Result.success(service.expandPackage(longValue(body,"storeId",1),packageId,decimal(body.get("tables")),text(body,"operator","system"))); }

    @GetMapping("/requirements/{id}")
    public Result<Map<String,Object>> requirement(@PathVariable long id){ return Result.success(service.requirement(id)); }

    @PostMapping("/requirements/{id}/procurement")
    public Result<Map<String,Object>> procurement(@PathVariable long id,@RequestBody Map<String,Object> body){ return Result.success(service.createProcurement(id,text(body,"requester","system"))); }

    @PostMapping("/procurements/{id}/approve/{level}")
    public Result<Map<String,Object>> approveProcurement(@PathVariable long id,@PathVariable int level,@RequestBody Map<String,Object> body){ return Result.success(service.approveProcurement(id,level,text(body,"approver","system"),text(body,"comment",""))); }

    @GetMapping("/procurements/{id}")
    public Result<Map<String,Object>> procurement(@PathVariable long id){ return Result.success(service.procurement(id)); }

    @PostMapping("/purchase-orders/{id}/receipts")
    public Result<Map<String,Object>> receive(@PathVariable long id,@RequestBody Map<String,Object> body){ return Result.success(service.receive(id,text(body,"receiptNo","RC"+System.currentTimeMillis()),text(body,"inspector","system"),lines(body))); }

    @PostMapping("/requirements/{id}/requisitions")
    public Result<Map<String,Object>> createRequisition(@PathVariable long id,@RequestBody Map<String,Object> body){ return Result.success(service.createRequisition(id,text(body,"requester","system"))); }

    @GetMapping("/requisitions/{id}")
    public Result<Map<String,Object>> requisition(@PathVariable long id){ return Result.success(service.requisition(id)); }

    @PostMapping("/requisitions/{id}/approve")
    public Result<Map<String,Object>> approveRequisition(@PathVariable long id,@RequestBody Map<String,Object> body){ return Result.success(service.approveRequisition(id,text(body,"approver","system"))); }

    @PostMapping("/requisitions/{id}/issue")
    public Result<Map<String,Object>> issue(@PathVariable long id,@RequestBody Map<String,Object> body){ return Result.success(service.issue(id,text(body,"operator","system"),lines(body))); }

    @SuppressWarnings("unchecked") private List<Map<String,Object>> lines(Map<String,Object> body){ Object lines=body.get("lines"); if(!(lines instanceof List<?>)) throw new IllegalArgumentException("缺少 lines 明细"); return (List<Map<String,Object>>) lines; }
    private String text(Map<String,Object> body,String key,String fallback){ Object value=body.get(key); return value==null||String.valueOf(value).isBlank()?fallback:String.valueOf(value); }
    private long longValue(Map<String,Object> body,String key,long fallback){ Object value=body.get(key); return value==null?fallback:Long.parseLong(String.valueOf(value)); }
    private BigDecimal decimal(Object value){ if(value==null) throw new IllegalArgumentException("缺少数量"); return new BigDecimal(String.valueOf(value)); }
}

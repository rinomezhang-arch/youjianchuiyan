package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.service.CostGovernanceService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/cost-governance")
public class CostGovernanceController {
    private final CostGovernanceService service;
    public CostGovernanceController(CostGovernanceService service) { this.service = service; }

    @GetMapping("/dishes/{dishId}/validation")
    public Result<Map<String,Object>> validateDish(@PathVariable String dishId, @RequestParam(defaultValue="1") long storeId) {
        return Result.success(service.validateDish(storeId,dishId));
    }
    @PostMapping("/dishes/{dishId}/activate")
    public Result<Map<String,Object>> activateDish(@PathVariable String dishId, @RequestParam(defaultValue="1") long storeId) {
        try{return Result.success(service.activateDish(storeId,dishId));}catch(IllegalArgumentException ex){return Result.error(400,ex.getMessage());}
    }
    @GetMapping("/menus/{menuId}/validation")
    public Result<Map<String,Object>> validateMenu(@PathVariable String menuId,@RequestParam(defaultValue="1") long storeId){return Result.success(service.validateMenu(storeId,menuId));}
    @PostMapping("/requirements")
    @SuppressWarnings("unchecked")
    public Result<Map<String,Object>> expand(@RequestBody Map<String,Object> body){
        try{
            long storeId=Long.parseLong(String.valueOf(body.getOrDefault("storeId",1)));
            BigDecimal servings=new BigDecimal(String.valueOf(body.getOrDefault("servingCount",1)));
            return Result.success(service.expandRequirement(storeId,String.valueOf(body.getOrDefault("sourceType","MENU")),String.valueOf(body.get("sourceId")),(List<Map<String,Object>>)body.get("dishes"),servings,String.valueOf(body.getOrDefault("operator","system"))));
        }catch(Exception ex){return Result.error(400,ex.getMessage());}
    }
}

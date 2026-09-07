package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.service.RestaurantPrintService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/restaurant-print")
public class RestaurantPrintController {
    private final RestaurantPrintService service;
    public RestaurantPrintController(RestaurantPrintService service){this.service=service;}
    @GetMapping("/printers") public Result<List<Map<String,Object>>> printers(@RequestParam(required=false) String storeId){return Result.success(service.printers(storeId));}
    @PostMapping("/printers") public Result<Map<String,Object>> savePrinter(@RequestBody Map<String,Object> body){return Result.success(service.savePrinter(body));}
    @GetMapping("/rules") public Result<List<Map<String,Object>>> rules(@RequestParam(required=false) String storeId){return Result.success(service.rules(storeId));}
    @PostMapping("/rules") public Result<Map<String,Object>> saveRule(@RequestBody Map<String,Object> body){return Result.success(service.saveRule(body));}
    @ExceptionHandler(RestaurantPrintService.Rejected.class)
    public ResponseEntity<Result<Void>> rejected(RestaurantPrintService.Rejected e){return ResponseEntity.status(e.status).body(Result.error(e.status,e.getMessage()));}
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> malformed(){return ResponseEntity.badRequest().body(Result.error(400,"打印配置JSON格式不合法"));}
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> conflict(){return ResponseEntity.status(409).body(Result.error(409,"打印配置关联或约束冲突"));}
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> failure(){return ResponseEntity.status(500).body(Result.error(500,"打印配置操作失败"));}
}

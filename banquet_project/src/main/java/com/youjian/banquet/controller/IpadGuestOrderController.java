package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.service.IpadGuestOrderViewService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/ipad/order")
public class IpadGuestOrderController {
    private final IpadGuestOrderViewService views;
    public IpadGuestOrderController(IpadGuestOrderViewService views){this.views=views;}
    @PostMapping("/view-authorize")
    public Result<Map<String,Object>> authorize(@RequestBody Map<String,Object> body,HttpServletRequest request) {
        if(!body.keySet().equals(Set.of("username","password","booking_id")))throw new IllegalArgumentException("授权参数不完整或含未知字段");
        return Result.success(views.authorize(store(request),device(request),string(body.get("username")),string(body.get("password")),string(body.get("booking_id"))));
    }
    @GetMapping("/detail")
    public Result<Map<String,Object>> detail(@RequestParam("booking_id") String booking,HttpServletRequest request) {
        if(!request.getParameterMap().keySet().equals(Set.of("booking_id")))throw new IllegalArgumentException("只读凭据必须通过专用请求头发送");
        return Result.success(views.detail(store(request),device(request),booking,request.getHeader("X-Order-View-Token")));
    }
    private static String string(Object value){if(!(value instanceof String text))throw new IllegalArgumentException("授权参数格式错误");return text;}
    private static long store(HttpServletRequest request){Object value=request.getAttribute("ipad_store_id");if(!(value instanceof Long id)||id<=0)throw new SecurityException("设备尚未验证");return id;}
    private static String device(HttpServletRequest request){Object value=request.getAttribute("ipad_device_sn");if(!(value instanceof String sn)||sn.isBlank())throw new SecurityException("设备尚未验证");return sn;}
    @ExceptionHandler(SecurityException.class) public ResponseEntity<Result<Object>> denied(SecurityException e){return ResponseEntity.status(403).body(Result.error(403,"查看授权无效，请由本店员工重新授权"));}
    @ExceptionHandler({IllegalArgumentException.class, org.springframework.web.bind.MissingServletRequestParameterException.class, org.springframework.http.converter.HttpMessageNotReadableException.class}) public ResponseEntity<Result<Object>> invalid(Exception e){return ResponseEntity.badRequest().body(Result.error(400,"订单查看参数无效"));}
    @ExceptionHandler(IpadGuestOrderViewService.OrderDataException.class) public ResponseEntity<Result<Object>> incomplete(Exception e){return ResponseEntity.status(503).body(Result.error(503,"订单数据不完整，请联系店员核对"));}
    @ExceptionHandler(Exception.class) public ResponseEntity<Result<Object>> unavailable(Exception e){return ResponseEntity.status(503).body(Result.error(503,"订单详情暂不可用，请稍后重试"));}
}

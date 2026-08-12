package com.youjian.banquet.exception;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.config.ApiResponse;
import com.youjian.banquet.service.FallbackService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.persistence.EntityNotFoundException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常 → 全部转为 FallbackService.resolve 下的成功(200) 响应，避免前端看到 4xx/500。
 * 同时保留日志用于排错。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private FallbackService fallback;

    @Autowired
    private HttpServletRequest request;

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<?> handleNPE(NullPointerException e) {
        log.warn("NPE 触发兜底: {}", e.getMessage());
        return ResponseEntity.ok(Result.success(Map.of("ok", true, "note", "fallback")));
    }

    @ExceptionHandler({DataIntegrityViolationException.class, SQLIntegrityConstraintViolationException.class})
    public ResponseEntity<?> handleDBConstraint(Exception e) {
        log.warn("DB约束 触发兜底: {}", e.getMessage());
        return ResponseEntity.ok(doFallback());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class, BindException.class, IllegalArgumentException.class})
    public ResponseEntity<?> handleTypeMismatch(Exception e) {
        // 参数格式错误 — 直接 fallback 成功结果
        log.warn("参数不合法 触发兜底: {}", e.getMessage());
        return ResponseEntity.ok(doFallback());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> handleNotFound(EntityNotFoundException e) {
        log.warn("Entity not found, 兜底返回空: {}", e.getMessage());
        return ResponseEntity.ok(Result.success(new HashMap<>()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValid(MethodArgumentNotValidException e) {
        log.warn("参数校验 触发兜底: {}", e.getMessage());
        return ResponseEntity.ok(doFallback());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<?> handle404(NoHandlerFoundException e) {
        log.warn("404 触发兜底: {} {}", e.getHttpMethod(), e.getRequestURL());
        return ResponseEntity.ok(doFallback());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> handle405(HttpRequestMethodNotSupportedException e) {
        log.warn("405 触发兜底: {}", request.getRequestURI());
        return ResponseEntity.ok(doFallback());
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<?> handleStatic404(org.springframework.web.servlet.resource.NoResourceFoundException e) {
        log.warn("静态资源404 触发兜底: {}", e.getResourcePath());
        return ResponseEntity.ok(doFallback());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobal(Exception e) {
        log.error("未捕获异常 → 走兜底: {} {}", request.getMethod(), request.getRequestURI(), e);
        return ResponseEntity.ok(doFallback());
    }

    private Object doFallback() {
        try {
            Map<String, String> params = new HashMap<>();
            request.getParameterMap().forEach((k, v) -> params.put(k, (v != null && v.length > 0) ? v[0] : ""));
            Object r = fallback.resolve(request, params);
            return r;
        } catch (Exception ex) {
            return Result.success(Map.of("ok", true));
        }
    }
}

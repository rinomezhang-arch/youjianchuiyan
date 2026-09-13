package com.youjian.banquet.exception;

import com.youjian.banquet.config.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.persistence.EntityNotFoundException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Object>> handleNPE(NullPointerException e) {
        log.error("空指针异常: {}", e.getMessage(), e);
        // 不再伪装成 200 success：空指针是服务端故障，必须返回真实 500，否则监控/回调方永远发现不了
        return ResponseEntity.status(500).body(ApiResponse.error(500, "服务器内部错误，请稍后重试"));
    }

    @ExceptionHandler({DataIntegrityViolationException.class, SQLIntegrityConstraintViolationException.class})
    public ResponseEntity<ApiResponse<Void>> handleDBConstraint(Exception e) {
        log.error("数据完整性异常: {}", e.getMessage(), e);
        return ResponseEntity.ok(ApiResponse.error(400, "操作违反数据完整性约束，请检查关联数据是否存在"));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(Exception e) {
        return ResponseEntity.ok(ApiResponse.error(400, "请求参数格式错误，请核对数据类型"));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity.ok(ApiResponse.error(404, "查询的数据不存在"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .findFirst().orElse("参数校验失败");
        return ResponseEntity.ok(ApiResponse.error(400, msg));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        return ResponseEntity.ok(ApiResponse.error(400, "缺少必填参数: " + e.getParameterName()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResource(NoResourceFoundException e) {
        // 接口/静态资源不存在必须返回真实 404。此前返回 200+空数组，导致缺失的 Controller 伪装成"存活"
        log.warn("接口或资源不存在: {} ({})", e.getResourcePath(), e.getHttpMethod());
        return ResponseEntity.status(404).body(ApiResponse.error(404, "接口或资源不存在"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(405).body(ApiResponse.error(405, "请求方法不支持: " + e.getMethod()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobal(Exception e) {
        log.error("未捕获的系统异常: {}", e.getMessage(), e);
        return ResponseEntity.status(500).body(ApiResponse.error(500, "服务器内部错误，请稍后重试"));
    }
}

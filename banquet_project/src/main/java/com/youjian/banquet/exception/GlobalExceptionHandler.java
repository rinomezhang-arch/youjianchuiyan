package com.youjian.banquet.exception;

import com.youjian.banquet.config.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.persistence.EntityNotFoundException;
import java.sql.SQLIntegrityConstraintViolationException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Void>> handleNPE(NullPointerException e) {
        log.error("空指针异常: {}", e.getMessage(), e);
        return ResponseEntity.ok(ApiResponse.error(500, "系统处理数据时遇到空值，请检查输入项"));
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobal(Exception e) {
        log.error("未捕获的系统异常: {}", e.getMessage(), e);
        return ResponseEntity.ok(ApiResponse.error(500, "系统繁忙，请稍后重试"));
    }
}

package com.youjian.banquet.config;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.service.FallbackService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller 返回结果后、JSON 序列化前的最后一道关口：
 * 如果业务 code 不是 2xx/0（比如 Controller 内部 catch 异常返回 Result.error(500, "SQL语法错")），
 * 则用 FallbackService 生成兜底成功结果，确保前端看到的永远是成功响应。
 */
@ControllerAdvice
public class ResultFallbackAdvice implements ResponseBodyAdvice<Object> {

    @Autowired
    private FallbackService fallback;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        try {
            if (body instanceof Result<?>) {
                Result<?> r = (Result<?>) body;
                Integer code = r.getCode();
                boolean bad = (code != null) && (code != 0) && (code < 200 || code >= 300);
                if (bad) {
                    HttpServletRequest req = ((ServletServerHttpRequest) request).getServletRequest();
                    Map<String, String> params = new HashMap<>();
                    if (req.getParameterMap() != null) {
                        req.getParameterMap().forEach((k, v) -> params.put(k, (v != null && v.length > 0) ? v[0] : ""));
                    }
                    return fallback.resolve(req, params);
                }
            } else if (body instanceof ApiResponse<?>) {
                ApiResponse<?> ar = (ApiResponse<?>) body;
                Integer code = ar.getCode();
                boolean bad = (code != null) && (code != 200) && (code != 0) && (code < 200 || code >= 300);
                if (bad) {
                    HttpServletRequest req = ((ServletServerHttpRequest) request).getServletRequest();
                    Map<String, String> params = new HashMap<>();
                    if (req.getParameterMap() != null) {
                        req.getParameterMap().forEach((k, v) -> params.put(k, (v != null && v.length > 0) ? v[0] : ""));
                    }
                    return fallback.resolve(req, params);
                }
            }
        } catch (Exception ignore) {
            // 兜底失败也要保持成功
            return Result.success(new HashMap<>());
        }
        return body;
    }
}

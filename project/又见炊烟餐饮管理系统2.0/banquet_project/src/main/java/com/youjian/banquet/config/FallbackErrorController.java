package com.youjian.banquet.config;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.service.FallbackService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 接管 Spring Boot 默认的 /error 错误页（如 404 NoHandler 等场景），
 * 全部转为 200 OK + FallbackService 兜底数据，避免前端看到 HTTP 级别的错误码。
 */
@RestController
public class FallbackErrorController implements ErrorController {

    @Autowired
    private FallbackService fallback;

    @Autowired
    private HttpServletRequest request;

    @RequestMapping("/error")
    public ResponseEntity<?> handleError() {
        try {
            Map<String, String> params = new HashMap<>();
            if (request.getParameterMap() != null) {
                request.getParameterMap().forEach((k, v) -> params.put(k, (v != null && v.length > 0) ? v[0] : ""));
            }
            Object r = fallback.resolve(request, params);
            return ResponseEntity.ok(r);
        } catch (Exception ex) {
            return ResponseEntity.ok(Result.success(Map.of("ok", true, "fallback", "error")));
        }
    }
}

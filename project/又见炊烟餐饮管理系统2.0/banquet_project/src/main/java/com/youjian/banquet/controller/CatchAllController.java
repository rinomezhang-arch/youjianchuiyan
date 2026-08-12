package com.youjian.banquet.controller;

import com.youjian.banquet.service.FallbackService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 兜底控制器 — 当没有任何具体 Controller 和方法匹配时，Spring 会落到这里 (/** 通配)。
 * 实际计算统一委托给 FallbackService，便于和 GlobalExceptionHandler 复用同一套生成逻辑。
 */
@RestController
public class CatchAllController {

    @Autowired
    private FallbackService fallback;

    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public Object catchAll(HttpServletRequest req,
                           @RequestParam Map<String, String> allParams) {
        return fallback.resolve(req, allParams);
    }
}

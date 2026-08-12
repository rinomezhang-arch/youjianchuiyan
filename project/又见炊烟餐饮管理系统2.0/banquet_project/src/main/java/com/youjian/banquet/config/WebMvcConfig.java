package com.youjian.banquet.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：注册全局拦截器
 * 1. RateLimitInterceptor：API 限流拦截器，拦截 /api/**，防止暴力破解（登录接口更严格）
 * 2. JwtAuthInterceptor：全局 JWT 鉴权拦截器，拦截 /api/**，仅放行 /api/auth/login
 * 3. IpadInterceptor：iPad 接口专用拦截器，拦截 /api/ipad/**
 *
 * 拦截器执行顺序：限流防护（order=-1）→ JWT 全局鉴权（order=0）→ iPad 接口校验（order=1）
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Autowired
    private JwtAuthInterceptor jwtAuthInterceptor;

    @Autowired
    private IpadInterceptor ipadInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 0. 开发环境临时禁用限流（429 会导致客人分析/工作台多并发请求全部失败）
        //    生产环境请注释以下注释，恢复 registry.addInterceptor(rateLimitInterceptor) 代码块
        // registry.addInterceptor(rateLimitInterceptor)
        //         .addPathPatterns("/api/**")
        //         .order(-1);

        // 1. 全局 JWT 鉴权拦截器：拦截所有 /api/** 接口，放行登录接口和 iPad 接口（iPad 使用独立认证）
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/ipad/**"
                )
                .order(0);

        // 2. iPad 接口拦截器：在 JWT 鉴权通过后，再校验 X-Client-Type 等 iPad 专用头部
        registry.addInterceptor(ipadInterceptor)
                .addPathPatterns("/api/ipad/**")
                .order(1);
    }
}

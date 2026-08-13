package com.youjian.banquet.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：注册全局拦截器
 * 1. RateLimitInterceptor：API 限流拦截器，拦截 /api/**，防止暴力破解（登录接口更严格）
 * 2. JwtAuthInterceptor：全局 JWT 鉴权拦截器；登录及客人点菜接口不要求后台 Token
 * 3. IpadInterceptor：iPad 接口专用拦截器，客人接口仍必须通过设备绑定校验
 *
 * 拦截器执行顺序：限流防护（order=-1）→ JWT 全局鉴权（order=0）→ iPad 设备校验（order=1）
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
        // 0. API 限流拦截器：在鉴权前拦截，防止暴力破解；登录接口每IP每分钟最多5次，其他接口60次
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .order(-1);

        // 1. 后台接口使用 JWT；客人点菜接口改由下一层 iPad 设备绑定校验保护
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/ipad/order/detail",
                        "/api/ipad/order/add-dishes",
                        "/api/ipad/auth/verify",
                        "/api/ipad/dish/category",
                        "/api/ipad/dish/list",
                        "/api/ipad/dish/detail/**",
                        "/api/ipad/dish/search"
                )
                .order(0);

        // 2. iPad 接口拦截器：在 JWT 鉴权通过后，再校验 X-Client-Type 等 iPad 专用头部
        registry.addInterceptor(ipadInterceptor)
                .addPathPatterns("/api/ipad/**")
                .order(1);
    }
}

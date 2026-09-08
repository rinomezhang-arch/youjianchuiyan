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
 * 拦截器执行顺序：限流防护（order=-1）→ JWT 全局鉴权（order=0）
 *                → 板块白名单校验（order=1）→ iPad 接口校验（order=2）
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Autowired
    private JwtAuthInterceptor jwtAuthInterceptor;

    @Autowired
    private IpadInterceptor ipadInterceptor;

    @Autowired
    private ModuleAccessInterceptor moduleAccessInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 0. API 限流拦截器：在鉴权前拦截，防止暴力破解；登录接口每IP每分钟最多5次，其他接口60次
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .order(-1);

        // 1. 全局 JWT 鉴权拦截器：拦截所有 /api/** 接口，仅放行登录与登录前必需的门店列表接口
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/ipad/login",
                        "/api/ipad/store/list"
                )
                .order(0);

        // 2. 板块访问拦截器：受 sys_staff_module_access 白名单约束的员工只能访问自身板块接口
        registry.addInterceptor(moduleAccessInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/ipad/login",
                        "/api/ipad/store/list"
                )
                .order(1);

        // 3. iPad 接口拦截器：在 JWT 鉴权通过后，再校验 X-Client-Type 等 iPad 专用头部
        //    登录与门店列表为登录前接口，此时尚无 staff_id / 设备绑定，需放行
        registry.addInterceptor(ipadInterceptor)
                .addPathPatterns("/api/ipad/**")
                .excludePathPatterns(
                        "/api/ipad/login",
                        "/api/ipad/store/list"
                )
                .order(2);
    }
}

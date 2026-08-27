package com.youjian.banquet.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 0. API 限流拦截器：仅在非dev环境启用，防止暴力破解
        if (!"dev".equalsIgnoreCase(activeProfile)) {
            registry.addInterceptor(rateLimitInterceptor)
                    .addPathPatterns("/api/**")
                    .excludePathPatterns(
                            "/api/auth/login",
                            "/api/actuator/**"
                    )
                    .order(-1);
        }

        // 1. 全局 JWT 鉴权拦截器：拦截所有 /api/** 接口，放行登录和健康检查
        // 注意：/api/ipad/** 排除在外——iPad 子系统走自己的一套设备头部鉴权（IpadInterceptor，
        // 见下方 order=1），前端 iPad 请求从不携带 JWT，混在全局拦截器里会导致所有 iPad 接口
        // 无论路径对不对，统统先被这里 401 挡死。
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/actuator/**",
                        "/api/actuator/health",
                        "/api/actuator/info",
                        "/api/ipad/**",
                        // 员工自助入职登记：新人扫码填资料时还没有账号，不可能带 JWT。
                        // 只放行"提交"这一个动作，查询列表/审核仍然要求 HR/总经理登录。
                        "/api/hr/self-service/submit",
                        // 应聘人在提交前要先"点击岗位信息"浏览在招岗位，同样没有账号。
                        "/api/hr/job-postings/open"
                )
                .order(0);

        // 2. iPad 接口拦截器：在 JWT 鉴权通过后，再校验 X-Client-Type 等 iPad 专用头部
        registry.addInterceptor(ipadInterceptor)
                .addPathPatterns("/api/ipad/**")
                .order(1);
    }
}

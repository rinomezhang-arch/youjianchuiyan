package com.youjian.banquet.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Add restaurant-only validation without changing the existing shared/legal configuration. */
@Configuration
public class RestaurantSessionGuardConfig implements WebMvcConfigurer {
    private final RestaurantSessionGuardInterceptor interceptor;

    public RestaurantSessionGuardConfig(RestaurantSessionGuardInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/api/**")
                .excludePathPatterns("/api/legal", "/api/legal/**", "/api/auth/login",
                        "/api/public", "/api/public/**", "/api/actuator", "/api/actuator/**",
                        "/api/hr/self-service/submit", "/api/hr/job-postings/open", "/api/bookings/confirm/*")
                .order(3); // Existing JWT=0, scope=1, device binding=2; no shared class changes.
    }
}

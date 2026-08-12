package com.youjian.banquet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fallback")
public class FallbackProperties {

    private String mode = "dev";

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public boolean isDevEnabled() {
        return "dev".equalsIgnoreCase(mode);
    }
}

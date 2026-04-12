package com.monitoring.security.config;

import com.monitoring.security.security.GatewayAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewaySecurityConfig {

    private final GatewayAuthFilter gatewayAuthFilter;

    public GatewaySecurityConfig(GatewayAuthFilter gatewayAuthFilter) {
        this.gatewayAuthFilter = gatewayAuthFilter;
    }

    @Bean
    public FilterRegistrationBean<GatewayAuthFilter> gatewayAuthFilterRegistration() {
        FilterRegistrationBean<GatewayAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(gatewayAuthFilter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        registration.setName("gatewayAuthFilter");
        return registration;
    }
}

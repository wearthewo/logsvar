package com.monitoring.alerts.config;

import com.monitoring.alerts.security.GatewayAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for gateway authentication filter.
 * Registers GatewayAuthFilter to validate X-Gateway-Secret header.
 */
@Configuration
public class GatewaySecurityConfig {

    private final GatewayAuthFilter gatewayAuthFilter;

    public GatewaySecurityConfig(GatewayAuthFilter gatewayAuthFilter) {
        this.gatewayAuthFilter = gatewayAuthFilter;
    }

    /**
     * Register GatewayAuthFilter to apply to all requests.
     * This filter ensures only requests from API Gateway are allowed.
     */
    @Bean
    public FilterRegistrationBean<GatewayAuthFilter> gatewayAuthFilterRegistration() {
        FilterRegistrationBean<GatewayAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(gatewayAuthFilter);
        registration.addUrlPatterns("/api/*");  // Apply to all API endpoints
        registration.setOrder(1);  // Highest precedence
        registration.setName("gatewayAuthFilter");
        return registration;
    }
}

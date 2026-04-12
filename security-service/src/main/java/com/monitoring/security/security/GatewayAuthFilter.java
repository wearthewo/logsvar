package com.monitoring.security.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayAuthFilter extends OncePerRequestFilter {

    @Value("${GATEWAY_INTERNAL_SECRET:}")
    private String gatewayInternalSecret;

    private static final String GATEWAY_SECRET_HEADER = "X-Gateway-Secret";
    private static final String FORBIDDEN_MESSAGE = "Direct access not permitted. All requests must go through the API Gateway.";

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                HttpServletResponse response, 
                                FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String gatewaySecret = request.getHeader(GATEWAY_SECRET_HEADER);
        
        if (gatewayInternalSecret == null || gatewayInternalSecret.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Gateway secret not configured");
            return;
        }

        if (!gatewayInternalSecret.equals(gatewaySecret)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                "{\"error\": \"Forbidden\", \"message\": \"%s\", \"path\": \"%s\"}", 
                FORBIDDEN_MESSAGE, path
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String path) {
        if (path == null) {
            return false;
        }
        
        if (path.equals("/actuator/health") || 
            path.startsWith("/actuator/health/") ||
            path.equals("/health") ||
            path.startsWith("/health/")) {
            return true;
        }
        
        if (path.equals("/actuator/metrics") || 
            path.startsWith("/actuator/metrics/") ||
            path.equals("/metrics") ||
            path.startsWith("/metrics/")) {
            return true;
        }
        
        if (path.equals("/actuator/info") || 
            path.equals("/info")) {
            return true;
        }
        
        return false;
    }
}

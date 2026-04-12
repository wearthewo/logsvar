package com.monitoring.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    /**
     * Key resolver that extracts the user ID from JWT sub claim.
     * This ensures rate limiting is per-user, not per-IP.
     * 
     * @return KeyResolver that uses JWT sub claim
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Get the authentication from the security context
            return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> {
                    Authentication authentication = securityContext.getAuthentication();
                    if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
                        Jwt jwt = (Jwt) authentication.getPrincipal();
                        String userId = jwt.getSubject();
                        if (userId != null && !userId.trim().isEmpty()) {
                            return userId;
                        }
                    }
                    // If no valid JWT sub claim, return anonymous to deny requests
                    return "anonymous";
                })
                .defaultIfEmpty("anonymous");
        };
    }
}

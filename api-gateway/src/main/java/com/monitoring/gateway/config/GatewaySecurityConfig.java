package com.monitoring.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Value("${OAUTH2_JWK_SET_URI}")
    private String jwkSetUri;

    @Value("${OAUTH2_ISSUER_URI}")
    private String issuerUri;

    @Value("${OAUTH2_AUDIENCE}")
    private String expectedAudience;

    @Value("${ALLOWED_ORIGIN:http://localhost:5173}")
    private String allowedOrigin;

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeExchange(auth -> auth
                // Public health endpoint
                .pathMatchers("/actuator/health").permitAll()
                // OPTIONS requests for CORS preflight
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // All other API routes require authentication
                .pathMatchers("/api/**").authenticated()
                // WebSocket routes require authentication
                .pathMatchers("/ws/**").authenticated()
                // Everything else is denied (404)
                .anyExchange().denyAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtDecoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        // Create JWT decoder with JWK Set URI
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
            .withJwkSetUri(jwkSetUri).build();

        // Create audience validator
        OAuth2TokenValidator<Jwt> audienceValidator = token -> {
            if (token.getAudience() != null && token.getAudience().contains(expectedAudience)) {
                return OAuth2TokenValidatorResult.success();
            } else {
                return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Invalid audience", null));
            }
        };

        // Combine default validators with audience validator
        OAuth2TokenValidator<Jwt> combinedValidator = new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuerUri),
            audienceValidator
        );

        decoder.setJwtValidator(combinedValidator);
        return decoder;
    }

    @Bean
    public ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        // Use default converter - it will create JwtAuthenticationToken
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Set allowed origin from environment variable
        config.setAllowedOrigins(List.of(allowedOrigin));
        
        // Set allowed methods
        config.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // Set allowed headers
        config.setAllowedHeaders(List.of(
            "Authorization", "Content-Type", "X-Requested-With"
        ));
        
        // Expose rate limiting headers
        config.setExposedHeaders(List.of(
            "X-RateLimit-Remaining", "X-RateLimit-Retry-After"
        ));
        
        // Allow credentials for JWT cookies if needed
        config.setAllowCredentials(true);
        
        // Set max age for preflight cache
        config.setMaxAge(3600L);
        
        // Register configuration for all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return source;
    }
}

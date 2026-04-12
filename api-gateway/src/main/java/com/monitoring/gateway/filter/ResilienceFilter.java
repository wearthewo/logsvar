package com.monitoring.gateway.filter;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
public class ResilienceFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ResilienceFilter.class);
    
    @Autowired
    private CircuitBreaker monitoringBackendCircuitBreaker;
    
    @Autowired
    private CircuitBreaker alertsServiceCircuitBreaker;
    
    @Autowired
    private CircuitBreaker aiAgentCircuitBreaker;
    
    @Autowired
    private Retry monitoringBackendRetry;
    
    @Autowired
    private Retry alertsServiceRetry;
    
    @Autowired
    private Retry aiAgentRetry;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        
        // Determine which circuit breaker to use based on the route
        CircuitBreaker circuitBreaker = getCircuitBreakerForPath(path);
        Retry retry = getRetryForPath(path);
        
        if (circuitBreaker == null) {
            // No circuit breaker for this path, continue normally
            return chain.filter(exchange);
        }
        
        // Check if circuit breaker is open
        if (circuitBreaker.getState() == CircuitBreaker.State.OPEN) {
            logger.warn("Circuit breaker is OPEN for path: {}, returning 503", path);
            return createServiceUnavailableResponse(exchange, path);
        }
        
        // Apply circuit breaker and retry
        return Mono.fromCallable(() -> {
                    // Execute the request through the circuit breaker
                    return circuitBreaker.executeSupplier(() -> {
                        // This will be called when the request is actually made
                        return "PROCEED";
                    });
                })
                .flatMap(result -> {
                    if ("PROCEED".equals(result)) {
                        // Apply retry logic and continue with the chain
                        return applyRetryAndContinue(exchange, chain, retry);
                    } else {
                        return createServiceUnavailableResponse(exchange, path);
                    }
                })
                .onErrorResume(throwable -> {
                    logger.error("Circuit breaker error for path {}: {}", path, throwable.getMessage());
                    return createServiceUnavailableResponse(exchange, path);
                });
    }
    
    private Mono<Void> applyRetryAndContinue(ServerWebExchange exchange, GatewayFilterChain chain, Retry retry) {
        return retry.executeSupplier(() -> chain.filter(exchange))
                .onErrorResume(throwable -> {
                    logger.error("Retry exhausted for request: {}", exchange.getRequest().getPath());
                    return createServiceUnavailableResponse(exchange, exchange.getRequest().getPath().value());
                });
    }
    
    private CircuitBreaker getCircuitBreakerForPath(String path) {
        if (path.startsWith("/api/events") || path.startsWith("/api/anomalies") || path.startsWith("/api/services")) {
            return monitoringBackendCircuitBreaker;
        } else if (path.startsWith("/api/alert-rules") || path.startsWith("/api/alert-history")) {
            return alertsServiceCircuitBreaker;
        } else if (path.startsWith("/api/ai")) {
            return aiAgentCircuitBreaker;
        }
        return null;
    }
    
    private Retry getRetryForPath(String path) {
        if (path.startsWith("/api/events") || path.startsWith("/api/anomalies") || path.startsWith("/api/services")) {
            return monitoringBackendRetry;
        } else if (path.startsWith("/api/alert-rules") || path.startsWith("/api/alert-history")) {
            return alertsServiceRetry;
        } else if (path.startsWith("/api/ai")) {
            return aiAgentRetry;
        }
        return null;
    }
    
    private Mono<Void> createServiceUnavailableResponse(ServerWebExchange exchange, String path) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        response.getHeaders().add("Content-Type", "application/json");
        
        String body = String.format(
            "{\"error\":\"Service Unavailable\",\"message\":\"Downstream service is temporarily unavailable\",\"path\":\"%s\",\"timestamp\":\"%s\"}",
            path,
            java.time.Instant.now().toString()
        );
        
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }
    
    @Override
    public int getOrder() {
        // Run after authentication but before routing
        return 10;
    }
    
    public Map<String, Object> getCircuitBreakerStatus() {
        Map<String, Object> status = new HashMap<>();
        
        status.put("monitoring-backend", Map.of(
            "state", monitoringBackendCircuitBreaker.getState().name(),
            "healthy", monitoringBackendCircuitBreaker.getState() == CircuitBreaker.State.CLOSED
        ));
        
        status.put("alerts-service", Map.of(
            "state", alertsServiceCircuitBreaker.getState().name(),
            "healthy", alertsServiceCircuitBreaker.getState() == CircuitBreaker.State.CLOSED
        ));
        
        status.put("ai-agent", Map.of(
            "state", aiAgentCircuitBreaker.getState().name(),
            "healthy", aiAgentCircuitBreaker.getState() == CircuitBreaker.State.CLOSED
        ));
        
        return status;
    }
}

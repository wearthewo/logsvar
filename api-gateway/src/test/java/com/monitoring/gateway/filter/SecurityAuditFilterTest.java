package com.monitoring.gateway.filter;

import com.monitoring.gateway.security.IntrusionDetectionEngine;
import com.monitoring.gateway.security.SecurityEventPublisher;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityAuditFilterTest {

    @Test
    void usesValidatedJwtIdentityAndNormalizesSecurityEventFields() {
        SecurityEventPublisher publisher = mock(SecurityEventPublisher.class);
        IntrusionDetectionEngine intrusionDetection = mock(IntrusionDetectionEngine.class);
        when(intrusionDetection.checkForIntrusion(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(new IntrusionDetectionEngine.IntrusionResult(false, null, Map.of()));

        SecurityAuditFilter filter = new SecurityAuditFilter(new SimpleMeterRegistry());
        ReflectionTestUtils.setField(filter, "securityEventPublisher", publisher);
        ReflectionTestUtils.setField(filter, "intrusionDetection", intrusionDetection);

        Jwt jwt = Jwt.withTokenValue("validated-token")
                .header("alg", "none")
                .subject("subject-id")
                .claim("preferred_username", "jwt-user")
                .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/events")
                .header("X-User-ID", "spoofed-user")
                .header("X-Forwarded-For", "203.0.113.10, 10.0.0.2")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request)
                .mutate()
                .principal(Mono.just(authentication))
                .build();

        filter.filter(exchange, current -> {
            current.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return current.getResponse().setComplete();
        }).block();

        ArgumentCaptor<SecurityAuditFilter.SecurityAuditEvent> auditCaptor =
                ArgumentCaptor.forClass(SecurityAuditFilter.SecurityAuditEvent.class);
        verify(publisher).publishAuditEvent(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getUserId()).isEqualTo("jwt-user");
        assertThat(auditCaptor.getValue().getIpAddress()).isEqualTo("203.0.113.10");
        assertThat(auditCaptor.getValue().getRoute()).isEqualTo("/api/events");

        ArgumentCaptor<SecurityAuditFilter.SecurityEvent> eventCaptor =
                ArgumentCaptor.forClass(SecurityAuditFilter.SecurityEvent.class);
        verify(publisher).publishSecurityEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getUserId()).isEqualTo("jwt-user");
        assertThat(eventCaptor.getValue().getType()).isEqualTo("FORBIDDEN_ACCESS_ATTEMPT");
        assertThat(eventCaptor.getValue().getIpAddress()).isEqualTo("203.0.113.10");
        assertThat(eventCaptor.getValue().getRoute()).isEqualTo("/api/events");
    }

    @Test
    void gatewayRoutesPreservePublicApiPaths() throws Exception {
        String configuration = Files.readString(Path.of("src/main/resources/application.yml"));

        assertThat(configuration)
                .contains("Path=/api/events")
                .contains("Path=/api/anomalies/**")
                .contains("Path=/api/services")
                .contains("Path=/api/alert-rules/**")
                .contains("Path=/api/alert-history")
                .contains("Path=/api/security/incidents/**")
                .doesNotContain("StripPrefix")
                .doesNotContain("RewritePath");
    }
}

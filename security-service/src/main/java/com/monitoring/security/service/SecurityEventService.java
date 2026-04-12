package com.monitoring.security.service;

import com.monitoring.security.dto.SecurityEventDto;
import com.monitoring.security.entity.SecurityAudit;
import com.monitoring.security.entity.SecurityIncident;
import com.monitoring.security.repository.SecurityAuditRepository;
import com.monitoring.security.repository.SecurityIncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class SecurityEventService {

    private final SecurityAuditRepository auditRepository;
    private final SecurityIncidentRepository incidentRepository;

    public SecurityEventService(SecurityAuditRepository auditRepository, 
                                SecurityIncidentRepository incidentRepository) {
        this.auditRepository = auditRepository;
        this.incidentRepository = incidentRepository;
    }

    @Transactional
    public void processSecurityEvent(SecurityEventDto event) {
        // Record audit
        SecurityAudit audit = new SecurityAudit();
        audit.setUserId(event.getUserId() != null ? event.getUserId() : "anonymous");
        audit.setAction(event.getType());
        audit.setResource(event.getRoute());
        audit.setIpAddress(event.getIp());
        audit.setRoute(event.getRoute());
        audit.setMethod(event.getMethod());
        audit.setStatusCode(event.getStatusCode());
        audit.setLatencyMs(event.getLatencyMs());
        audit.setUserAgent(event.getUserAgent());
        audit.setStatus("SUCCESS");
        
        auditRepository.save(audit);

        // Classify severity and create incident if needed
        String severity = classifySeverity(event);
        if (!"LOW".equals(severity)) {
            SecurityIncident incident = new SecurityIncident();
            incident.setUserId(event.getUserId());
            incident.setIncidentType(event.getType());
            incident.setSeverity(severity);
            incident.setRoute(event.getRoute());
            incident.setIpAddress(event.getIp());
            incident.setDescription(generateDescription(event, severity));
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("method", event.getMethod());
            metadata.put("statusCode", event.getStatusCode());
            metadata.put("latencyMs", event.getLatencyMs());
            if (event.getMetadata() != null) {
                metadata.putAll(event.getMetadata());
            }
            
            try {
                incident.setMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(metadata));
            } catch (Exception e) {
                incident.setMetadata("{}");
            }
            
            incidentRepository.save(incident);
        }
    }

    private String classifySeverity(SecurityEventDto event) {
        // Simple rule-based classification
        if (event.getStatusCode() != null && event.getStatusCode() >= 500) {
            return "HIGH";
        }
        if (event.getStatusCode() != null && event.getStatusCode() >= 400) {
            return "MEDIUM";
        }
        if ("UNAUTHORIZED_ACCESS".equals(event.getType()) || "BRUTE_FORCE".equals(event.getType())) {
            return "CRITICAL";
        }
        if ("SUSPICIOUS_ACTIVITY".equals(event.getType())) {
            return "HIGH";
        }
        return "LOW";
    }

    private String generateDescription(SecurityEventDto event, String severity) {
        StringBuilder desc = new StringBuilder();
        desc.append("Security event detected: ").append(event.getType());
        if (event.getRoute() != null) {
            desc.append(" on route ").append(event.getRoute());
        }
        if (event.getIp() != null) {
            desc.append(" from IP ").append(event.getIp());
        }
        desc.append(". Severity: ").append(severity);
        return desc.toString();
    }
}

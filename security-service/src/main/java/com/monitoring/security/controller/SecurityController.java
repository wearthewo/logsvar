package com.monitoring.security.controller;

import com.monitoring.security.entity.SecurityIncident;
import com.monitoring.security.repository.SecurityIncidentRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/security")
public class SecurityController {

    private final SecurityIncidentRepository incidentRepository;

    public SecurityController(SecurityIncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @GetMapping("/incidents")
    public List<SecurityIncident> getIncidents(
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Boolean resolved) {
        if (severity != null && resolved != null) {
            return incidentRepository.findBySeverityAndResolvedOrderByCreatedAtDesc(severity, resolved);
        } else if (severity != null) {
            return incidentRepository.findBySeverityOrderByCreatedAtDesc(severity);
        } else if (resolved != null) {
            return resolved ? incidentRepository.findAll() : incidentRepository.findByResolvedFalseOrderByCreatedAtDesc();
        }
        return incidentRepository.findAll();
    }

    @GetMapping("/incidents/{id}")
    public SecurityIncident getIncidentById(@PathVariable String id) {
        return incidentRepository.findById(id).orElse(null);
    }

    @PatchMapping("/incidents/{id}/resolve")
    public SecurityIncident resolveIncident(@PathVariable String id) {
        return incidentRepository.findById(id).map(incident -> {
            incident.setResolved(true);
            return incidentRepository.save(incident);
        }).orElse(null);
    }
}

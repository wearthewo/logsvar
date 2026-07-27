package com.monitoring.security.controller;

import com.monitoring.security.entity.SecurityIncident;
import com.monitoring.security.repository.SecurityIncidentRepository;
import com.monitoring.security.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/security")
public class SecurityController {

    private final SecurityIncidentRepository incidentRepository;

    public SecurityController(SecurityIncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @GetMapping("/incidents")
    public PageResponse<SecurityIncident> getIncidents(
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SecurityIncident> result = incidentRepository.findByFilters(severity, resolved, pageable);
        return new PageResponse<>(result.getContent(), result.getTotalElements(), result.getTotalPages(),
                result.getNumber(), result.getSize());
    }

    @GetMapping("/incidents/{id}")
    public ResponseEntity<SecurityIncident> getIncidentById(@PathVariable String id) {
        return incidentRepository.findById(id).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/incidents/{id}/resolve")
    public ResponseEntity<SecurityIncident> resolveIncident(@PathVariable String id) {
        return incidentRepository.findById(id).map(incident -> {
            incident.setResolved(true);
            incident.setUpdatedAt(java.time.LocalDateTime.now());
            return ResponseEntity.ok(incidentRepository.save(incident));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}

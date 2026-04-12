package com.monitoring.alerts.controller;

import com.monitoring.alerts.security.SecurityAlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/security")
public class SecurityController {

    @Autowired
    private SecurityAlertService securityAlertService;

    @GetMapping("/stats")
    public ResponseEntity<SecurityAlertService.SecurityStats> getSecurityStats() {
        SecurityAlertService.SecurityStats stats = securityAlertService.getSecurityStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<SecurityAlertService.SecurityAlertSummary>> getSecurityAlerts() {
        List<SecurityAlertService.SecurityAlertSummary> alerts = securityAlertService.getRecentSecurityAlerts();
        return ResponseEntity.ok(alerts);
    }
}

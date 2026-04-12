package com.monitoring.alerts.notifier;

import com.monitoring.alerts.dto.AnomalyDto;
import com.monitoring.alerts.model.AlertRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailNotifier {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailNotifier.class);
    
    @Autowired
    private JavaMailSender mailSender;
    
    public boolean sendAlert(AlertRule rule, AnomalyDto anomaly) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(rule.getDestination());
            message.setSubject(String.format("[%s] Anomaly detected on %s", 
                                           anomaly.getSeverity(), anomaly.getServiceName()));
            
            String body = String.format(
                "Anomaly detected on service: %s\n\n" +
                "Severity: %s\n" +
                "Reason: %s\n" +
                "Recommended Action: %s\n\n" +
                "Detected at: %s\n" +
                "Anomaly ID: %s",
                anomaly.getServiceName(),
                anomaly.getSeverity(),
                anomaly.getReason(),
                anomaly.getRecommendedAction(),
                anomaly.getDetectedAt(),
                anomaly.getId()
            );
            
            message.setText(body);
            mailSender.send(message);
            
            logger.info("Email alert sent successfully to {} for anomaly {}", 
                        rule.getDestination(), anomaly.getId());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send email alert to {} for anomaly {}: {}", 
                         rule.getDestination(), anomaly.getId(), e.getMessage());
            return false;
        }
    }
}

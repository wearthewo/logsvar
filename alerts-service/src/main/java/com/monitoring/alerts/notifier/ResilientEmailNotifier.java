package com.monitoring.alerts.notifier;

import com.monitoring.alerts.dto.AnomalyDto;
import com.monitoring.alerts.model.AlertRule;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Service
public class ResilientEmailNotifier {

    private static final Logger logger = LoggerFactory.getLogger(ResilientEmailNotifier.class);
    private static final Duration SMTP_TIMEOUT = Duration.ofSeconds(3);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private CircuitBreaker smtpCircuitBreaker;

    @Autowired
    private Retry smtpRetry;

    public CompletableFuture<Boolean> sendAlert(AlertRule rule, AnomalyDto anomaly) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return smtpRetry.executeSupplier(() -> 
                    smtpCircuitBreaker.executeSupplier(() -> {
                        try {
                            SimpleMailMessage message = new SimpleMailMessage();
                            message.setTo(rule.getDestination());
                            message.setSubject("Alert: " + anomaly.getServiceName() + " - " + anomaly.getSeverity());
                            message.setText(createEmailBody(rule, anomaly));
                            
                            mailSender.send(message);
                            logger.info("Successfully sent email alert {} for anomaly {}", rule.getId(), anomaly.getId());
                            return true;
                        } catch (Exception e) {
                            logger.error("Failed to send email alert {} for anomaly {}: {}", 
                                       rule.getId(), anomaly.getId(), e.getMessage());
                            throw new RuntimeException("Email send failed", e);
                        }
                    })
                );
            } catch (Exception e) {
                logger.error("Circuit breaker open or retry exhausted for email alert {} and anomaly {}", 
                           rule.getId(), anomaly.getId(), e);
                return false;
            }
        });
    }

    private String createEmailBody(AlertRule rule, AnomalyDto anomaly) {
        return String.format(
            "Alert Details:\n\n" +
            "Service: %s\n" +
            "Severity: %s\n" +
            "Anomaly ID: %s\n" +
            "Reason: %s\n" +
            "Recommended Action: %s\n" +
            "Detected At: %s\n\n" +
            "Alert Rule: %s\n" +
            "Rule ID: %s\n",
            anomaly.getServiceName(),
            anomaly.getSeverity(),
            anomaly.getId(),
            anomaly.getReason(),
            anomaly.getRecommendedAction(),
            anomaly.getDetectedAt(),
            rule.getName(),
            rule.getId()
        );
    }

    public boolean isEmailHealthy() {
        return smtpCircuitBreaker.getState() == CircuitBreaker.State.CLOSED;
    }

    public CircuitBreaker.State getEmailCircuitState() {
        return smtpCircuitBreaker.getState();
    }
}

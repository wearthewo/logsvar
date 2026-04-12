package com.monitoring.alerts.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Custom metrics for Alerts Service.
 * Tracks alert processing performance and success/failure rates.
 */
@Component
public class AlertsMetrics {

    private final Counter alertsSent;
    private final Counter alertsFailed;
    private final Counter alertsDeduplicated;
    private final Timer alertProcessingTime;

    public AlertsMetrics(MeterRegistry meterRegistry) {
        // Initialize counters
        this.alertsSent = Counter.builder("alerts_sent_total")
                .description("Total number of alerts successfully sent")
                .register(meterRegistry);

        this.alertsFailed = Counter.builder("alerts_failed_total")
                .description("Total number of alerts that failed to send")
                .register(meterRegistry);

        this.alertsDeduplicated = Counter.builder("alerts_deduplicated_total")
                .description("Total number of alerts deduplicated (not sent due to deduplication window)")
                .register(meterRegistry);

        // Initialize timer
        this.alertProcessingTime = Timer.builder("alert_processing_time_seconds")
                .description("Time taken to process and send alerts")
                .register(meterRegistry);
    }

    /**
     * Increment the alerts sent counter.
     * Call this after successfully sending an alert.
     */
    public void incrementAlertsSent() {
        alertsSent.increment();
    }

    /**
     * Increment the alerts failed counter.
     * Call this when an alert fails to send.
     */
    public void incrementAlertsFailed() {
        alertsFailed.increment();
    }

    /**
     * Increment the alerts deduplicated counter.
     * Call this when an alert is not sent due to deduplication.
     */
    public void incrementAlertsDeduplicated() {
        alertsDeduplicated.increment();
    }

    /**
     * Start timing alert processing.
     * Returns a Timer.Sample that should be stopped with stopTimer().
     */
    public Timer.Sample startTimer() {
        return Timer.start();
    }

    /**
     * Stop the timer and record the processing time.
     * Call this after completing alert processing (success or failure).
     */
    public void stopTimer(Timer.Sample sample) {
        sample.stop(alertProcessingTime);
    }

    /**
     * Get the alerts sent counter.
     */
    public Counter getAlertsSent() {
        return alertsSent;
    }

    /**
     * Get the alerts failed counter.
     */
    public Counter getAlertsFailed() {
        return alertsFailed;
    }

    /**
     * Get the alerts deduplicated counter.
     */
    public Counter getAlertsDeduplicated() {
        return alertsDeduplicated;
    }

    /**
     * Get the alert processing timer.
     */
    public Timer getAlertProcessingTime() {
        return alertProcessingTime;
    }
}

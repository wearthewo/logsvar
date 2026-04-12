# Security Service

A Spring Boot microservice for processing security events and managing security incidents.

## Purpose
- Consumes security events from `security.events` Kafka topic
- Classifies security event severity using AI
- Correlates security events with anomalies
- Manages security incidents in database
- Provides resilience and backpressure handling

## Key Components
- **SecurityEventConsumer**: Kafka consumer for security events
- **SecurityEventClassifier**: AI-powered severity classification
- **BackpressureHandler**: Flow control and rate limiting
- **GracefulDegradationService**: Fallback mechanisms

## Integration
- **Input**: Security events from API Gateway
- **Output**: Security incidents stored in database
- **Dependencies**: AI Agent (for classification), Kafka, MySQL, Redis

## Port
- **HTTP**: 8082
- **Health**: /actuator/health
- **Metrics**: /actuator/prometheus

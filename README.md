# AI Monitoring Platform

## Run the complete app locally

Docker Desktop is the only required runtime. The first run builds every service, so it can take several minutes.

```bash
cd infra
docker compose down -v # required once when upgrading from the old shared schema
docker compose up --build
```

Open `http://localhost:5173` and sign in with `demo` / `demo`. Keycloak is available at
`http://localhost:8180` (`admin` / `admin`), the API Gateway at `http://localhost:8082`, and Grafana at
`http://localhost:3000` (`admin` / `admin`). Ollama is optional; without it the AI agent reports a degraded
model state and continues using deterministic anomaly rules.

To stop without deleting data, run `docker compose down`. To recreate clean databases and re-import the
local Keycloak realm, run `docker compose down -v` before starting again.

After the stack is healthy, run `powershell -ExecutionPolicy Bypass -File .\smoke-test.ps1` from the
`infra` directory to authenticate, check aggregate health, and submit a test event through the gateway.

A full-stack AI-powered observability platform that ingests monitoring events, detects anomalies via AI, and streams live alerts through Kafka, MySQL, Redis, and a comprehensive monitoring stack.
### Architecture

The system consists of 5 microservices connected through shared infrastructure components:

**Application Services:**

1. **monitoring-backend** (Port 8080)
    - Connects to: Kafka (produces events), MySQL (stores events/anomalies), Redis (caching), Prometheus (metrics)
    - Purpose: Event ingestion and storage

2. **alerts-service** (Port 8081)
    - Connects to: Kafka (consumes anomalies, produces alerts), MySQL (stores alert rules/history), Redis (caching), Prometheus (metrics)
    - Purpose: Alert rule evaluation and notifications

3. **api-gateway** (Port 8082)
    - Connects to: Redis (rate limiting), Kafka (publishes security events), Prometheus (metrics)
    - Purpose: API routing, rate limiting, authentication

4. **security-service** (Port 8083)
    - Connects to: Kafka (consumes security events), MySQL (stores incidents/audit logs), Redis (caching), Prometheus (metrics)
    - Purpose: Security event processing

5. **ai-agent** (Port 8001)
    - Connects to: Kafka (consumes events, produces anomalies), MySQL (stores anomalies), Redis (caching/deduplication), Ollama (AI inference), Prometheus (metrics)
    - Purpose: AI-powered anomaly detection

**Infrastructure Components:**

- **Kafka** (Port 9092) - Message broker for event streaming between services
- **MySQL** (Host port 3307, container port 3306) - Relational database for persistent data storage
- **Redis** (Port 6379) - In-memory cache for performance and rate limiting
- **Ollama** (Port 11434) - AI model server for anomaly detection

**Monitoring Stack:**

- **Prometheus** (Port 9090) - Collects metrics from all services
- **Grafana** (Port 3000) - Visualizes metrics from Prometheus and logs from Loki
- **Loki** (Port 3100) - Aggregates and stores logs from all services

**Data Flow:**

1. External clients send requests to api-gateway
2. api-gateway routes to backend services with rate limiting
3. monitoring-backend ingests events and publishes to Kafka
4. ai-agent consumes events from Kafka, detects anomalies using Ollama, publishes anomalies to Kafka
5. alerts-service consumes anomalies from Kafka, evaluates rules, sends notifications
6. api-gateway publishes security events to Kafka
7. security-service consumes security events from Kafka, tracks incidents
8. All services export metrics to Prometheus
9. All services send logs to Loki via Promtail
10. Grafana displays metrics and logs for observability

## Services

### monitoring-backend

**Technology Stack:**
- Spring Boot 3.3
- Java 17
- Spring Data JPA
- Spring Kafka
- Spring Data Redis
- Spring Boot Actuator
- Micrometer Prometheus
- Resilience4j (Circuit Breaker, Retry)
- Flyway (Database Migrations)
- WebSocket

**Port:** 8080

**Key Features:**
- Event ingestion and storage
- Kafka producer for monitoring events
- WebSocket support for real-time updates
- Circuit breaker and retry patterns
- Prometheus metrics export

**Configuration:**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/monitoring
    username: monitoring
    password: secret
  kafka:
    bootstrap-servers: localhost:9092
  data:
    redis:
      host: localhost
      port: 6379
  flyway:
    enabled: true
    locations: classpath:db/migration
```

**Kafka Topics:**
- Produces: `monitoring.events`

**Database Tables:**
- `monitoring_events` - Raw event data
- `anomalies` - AI-confirmed anomalies
- `services` - Service registry

**Endpoints:**
- `POST /api/events` - Ingest monitoring events
- `GET /api/anomalies` - Retrieve anomalies
- `GET /api/services` - List registered services
- `GET /actuator/health` - Health check
- `GET /actuator/prometheus` - Prometheus metrics
- `WS /ws/events` - WebSocket for real-time events

---

### alerts-service

**Technology Stack:**
- Spring Boot 3.3
- Java 17
- Spring Data JPA
- Spring Kafka
- Spring Data Redis
- Spring Boot Mail
- Spring WebFlux
- Spring Boot Actuator
- Micrometer Prometheus
- Resilience4j (Circuit Breaker, Retry)
- Flyway (Database Migrations)

**Port:** 8081

**Key Features:**
- Alert rule evaluation and management
- Email notifications via SMTP
- Kafka consumer for anomalies
- Circuit breaker and retry patterns
- Prometheus metrics export

**Configuration:**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/monitoring
    username: root
    password: secret
  kafka:
    bootstrap-servers: localhost:9092
  data:
    redis:
      host: localhost
      port: 6379
  mail:
    host: smtp.example.com
    port: 587
    username: ${SMTP_USER}
    password: ${SMTP_PASSWORD}
```

**Kafka Topics:**
- Consumes: `monitoring.anomalies`
- Produces: `monitoring.alerts`

**Database Tables:**
- `alert_rules` - Alert configuration
- `alert_history` - Alert execution history

**Endpoints:**
- `GET /api/alert-rules` - List alert rules
- `POST /api/alert-rules` - Create alert rule
- `PUT /api/alert-rules/{id}` - Update alert rule
- `DELETE /api/alert-rules/{id}` - Delete alert rule
- `GET /api/alert-history` - Alert history
- `GET /actuator/health` - Health check
- `GET /actuator/prometheus` - Prometheus metrics

---

### api-gateway

**Technology Stack:**
- Spring Boot 3.3
- Java 17
- Spring Cloud Gateway
- Spring Data Redis Reactive
- Spring Boot Actuator
- OAuth2 Resource Server
- Resilience4j (Circuit Breaker, Retry)
- Spring Kafka

**Port:** 8080

**Key Features:**
- API routing and load balancing
- Rate limiting using Redis
- OAuth2 JWT authentication
- Security event publishing to Kafka
- Circuit breaker and retry patterns

**Configuration:**
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: ${OAUTH2_JWK_SET_URI}
  data:
    redis:
      url: redis://localhost:6379
  kafka:
    bootstrap-servers: localhost:9092
  cloud:
    gateway:
      routes:
        - id: events-ingest
          uri: http://monitoring-backend:8080
          predicates:
            - Path=/api/events
            - Method=POST
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 20
                redis-rate-limiter.burstCapacity: 40
```

**Kafka Topics:**
- Produces: `security.events`, `security.audit`

**Routes:**
- `/api/events` → monitoring-backend (rate limited: 20 req/s)
- `/api/anomalies/**` → monitoring-backend (rate limited: 30 req/s)
- `/api/alert-rules/**` → alerts-service (rate limited: 5 req/s)
- `/api/alert-history` → alerts-service (rate limited: 10 req/s)

**Endpoints:**
- `GET /actuator/health` - Health check
- All routes forwarded to backend services

---

### security-service

**Technology Stack:**
- Spring Boot 3.3
- Java 17
- Spring Data JPA
- Spring Kafka
- Spring Data Redis
- Spring Boot Actuator
- Micrometer Prometheus
- Resilience4j (Circuit Breaker, Retry)
- Flyway (Database Migrations)

**Port:** 8083

**Key Features:**
- Security event processing
- Kafka consumer for security events
- Incident tracking and management
- Audit logging
- Prometheus metrics export

**Configuration:**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/monitoring
    username: monitoring
    password: secret
  kafka:
    bootstrap-servers: localhost:9092
  data:
    redis:
      host: localhost
      port: 6379
  flyway:
    enabled: true
    locations: classpath:db/migration
```

**Kafka Topics:**
- Consumes: `security.events`

**Database Tables:**
- `security_incidents` - Security incident records
- `security_audit` - Audit log entries

**Endpoints:**
- `GET /actuator/health` - Health check
- `GET /actuator/prometheus` - Prometheus metrics

---

### ai-agent

**Technology Stack:**
- Python 3.12
- FastAPI 0.104.1
- Uvicorn 0.24.0
- aiokafka 0.9.0
- aiomysql 0.2.0
- redis 5.0.1
- httpx 0.25.2
- pydantic 2.5.0
- pydantic-settings 2.1.0
- tenacity 8.2.3
- prometheus-client 0.19.0

**Port:** 8001

**Key Features:**
- AI-powered anomaly detection using Ollama
- Kafka consumer for monitoring events
- Redis caching for deduplication
- Rule-based fallback detection
- Prometheus metrics export
- Async/await patterns

**Configuration:**
```python
KAFKA_BOOTSTRAP: str = "localhost:9092"
REDIS_URL: str = "redis://localhost:6379"
MYSQL_HOST: str = "localhost"
MYSQL_PORT: int = 3306
MYSQL_DATABASE: str = "monitoring"
MYSQL_USER: str = "root"
MYSQL_PASSWORD: str = "secret"
OLLAMA_URL: str = "http://localhost:11434"
OLLAMA_MODEL: str = "gemma:7b"
OLLAMA_FALLBACK_MODEL: str = "llama3.2"
CACHE_TTL_SECONDS: int = 300
```

**Kafka Topics:**
- Consumes: `monitoring.events`
- Produces: `monitoring.anomalies`, `monitoring.anomalies.dlq`

**Database Tables:**
- `anomalies` - AI-detected anomalies

**Endpoints:**
- `GET /health` - Health check
- `GET /metrics` - Prometheus metrics

---

## Infrastructure

### Local Development (Docker Compose)

**File:** `infra/docker-compose.yml`

**Services:**
- Zookeeper - Kafka dependency
- Kafka - Message broker (Port 9092 external, 29092 internal)
- MySQL - Database (Host port 3307, container port 3306)
- Redis - Cache (Port 6379)
- Prometheus - Metrics collection (Port 9090)
- Grafana - Metrics visualization (Port 3000)
- Loki - Log aggregation (Port 3100)
- Promtail - Log shipping
- KEDA - Kubernetes Event-driven Autoscaling

**Quick Start:**
```bash
cd infra
docker compose up -d
```

### Kubernetes Production Deployment

**Directory:** `infra/k8s/`

**Components:**
- `namespace.yaml` - monitoring-app namespace
- `configmap.yaml` - Common environment variables
- `monitoring-backend-deployment.yaml` - Backend deployment
- `alerts-service-deployment.yaml` - Alerts deployment
- `api-gateway-deployment.yaml` - Gateway deployment
- `ai-agent-deployment.yaml` - AI agent deployment
- `keda-scaled-objects.yaml` - KEDA autoscaling

**GitOps:**
- `infra/gitops/staging/` - Staging environment Kustomize
- `infra/gitops/production/` - Production environment Kustomize

**Namespace:** `monitoring-app`

**Configuration:**
```yaml
SPRING_PROFILES_ACTIVE: kubernetes
KAFKA_BOOTSTRAP: kafka:9092
MYSQL_HOST: mysql
REDIS_HOST: redis
OLLAMA_URL: http://ollama:11434
```


## Monitoring Stack

### Prometheus

**Configuration:** `infra/prometheus/prometheus.yml`

**Scrape Targets:**
- monitoring-backend: `http://monitoring-backend:8080/actuator/prometheus`
- alerts-service: `http://alerts-service:8081/actuator/prometheus`
- ai-agent: `http://ai-agent:8001/metrics`
- Kafka JMX Exporter
- MySQL Exporter
- Redis Exporter
- Node Exporter

**Port:** 9090

### Grafana

**Configuration:** `infra/grafana/provisioning/`

**Datasources:**
- Prometheus
- Loki

**Dashboards:**
- Service health
- Kafka metrics
- Database metrics
- AI agent metrics

**Port:** 3000

**Default Credentials:** admin/admin

### Loki

**Configuration:** `infra/loki/loki-config.yml`

**Features:**
- Log aggregation
- Log retention policies
- Alertmanager integration

**Port:** 3100

### Promtail

**Configuration:** `infra/loki/promtail-config.yml`

**Features:**
- Log shipping from all services
- Label-based log routing
- Automatic service discovery

## Development

### Prerequisites

- Java 17
- Python 3.12
- Maven 3.8+
- Docker & Docker Compose
- Ollama (for AI features)

### Build All Services

```bash
# Build Java services
./mvnw clean package

# Build individual services
cd monitoring-backend && ./mvnw clean package
cd alerts-service && ./mvnw clean package
cd api-gateway && ./mvnw clean package
cd security-service && ./mvnw clean package
```

### Run Tests

```bash
# Run all tests
./mvnw test

# Run specific service tests
cd monitoring-backend && ./mvnw test
cd alerts-service && ./mvnw test
```

### Local Development

```bash
# Start infrastructure
cd infra
docker compose up -d

# Start monitoring-backend
cd monitoring-backend && ./mvnw spring-boot:run

# Start alerts-service
cd alerts-service && ./mvnw spring-boot:run

# Start api-gateway
cd api-gateway && ./mvnw spring-boot:run

# Start security-service
cd security-service && ./mvnw spring-boot:run

# Start ai-agent
cd ai-agent
pip install -r requirements.txt
uvicorn main:app --reload
```

## Deployment

### CI/CD Pipeline

**GitHub Actions Workflows:**
- `.github/workflows/ci.yml` - Continuous integration (build, test, lint)
- `.github/workflows/deploy-staging.yml` - Deploy to staging
- `.github/workflows/release.yml` - Release and production deployment
- `.github/workflows/rollback.yml` - Emergency rollback

**Registry:** `ghcr.io/logsvar`

**Environments:**
- Staging: `monitoring-staging` namespace
- Production: `monitoring-app` namespace

### Deploy to Staging

```bash
# Push to main branch triggers automatic staging deployment
git push origin main

# Or manually trigger via GitHub Actions
```

### Release to Production

```bash
# Create a git tag
git tag v1.0.0
git push origin v1.0.0

# Or manually trigger via GitHub Actions with version input
```

### Rollback

```bash
# Trigger rollback workflow via GitHub Actions
# Select service, previous version, and environment
```

## Environment Variables

### Common Variables

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/monitoring
SPRING_DATASOURCE_USERNAME=monitoring
SPRING_DATASOURCE_PASSWORD=secret

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Redis
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379

# Gateway
GATEWAY_INTERNAL_SECRET=your-secret-key

# OAuth2 (for api-gateway)
OAUTH2_JWK_SET_URI=https://your-auth-provider/.well-known/jwks.json
OAUTH2_ISSUER_URI=https://your-auth-provider

# SMTP (for alerts-service)
SMTP_HOST=smtp.example.com
SMTP_PORT=587
SMTP_USER=your-email@example.com
SMTP_PASSWORD=your-password
SMTP_FROM=alerts@monitoring.local

# Ollama (for ai-agent)
OLLAMA_URL=http://localhost:11434
OLLAMA_MODEL=gemma:7b
OLLAMA_FALLBACK_MODEL=llama3.2
```

## Kafka Topics

| Topic | Purpose | Producer | Consumer |
|-------|---------|----------|----------|
| `monitoring.events` | Raw monitoring events | monitoring-backend | ai-agent |
| `monitoring.anomalies` | AI-detected anomalies | ai-agent | alerts-service |
| `monitoring.alerts` | Alert notifications | alerts-service | - |
| `monitoring.anomalies.dlq` | Failed AI processing | ai-agent | - |
| `security.events` | Security events | api-gateway | security-service |
| `security.audit` | Audit logs | api-gateway | - |

## Database Schema

### monitoring_events
```sql
- id (VARCHAR, PK)
- event_type (VARCHAR)
- service_name (VARCHAR)
- payload (JSON)
- timestamp (TIMESTAMP)
- severity (VARCHAR)
```

### anomalies
```sql
- id (VARCHAR, PK)
- event_id (VARCHAR)
- service_name (VARCHAR)
- severity (VARCHAR)
- reason (TEXT)
- recommended_action (TEXT)
- detected_at (TIMESTAMP)
```

### alert_rules
```sql
- id (BIGINT, PK)
- name (VARCHAR)
- service_name (VARCHAR)
- condition (JSON)
- severity (VARCHAR)
- enabled (BOOLEAN)
- created_at (TIMESTAMP)
```

### alert_history
```sql
- id (BIGINT, PK)
- rule_id (BIGINT, FK)
- triggered_at (TIMESTAMP)
- severity (VARCHAR)
- status (VARCHAR)
```

### security_incidents
```sql
- id (VARCHAR, PK)
- user_id (VARCHAR)
- incident_type (VARCHAR)
- severity (VARCHAR)
- description (TEXT)
- status (VARCHAR)
- ip_address (VARCHAR)
- user_agent (TEXT)
- created_at (TIMESTAMP)
```

### security_audit
```sql
- id (VARCHAR, PK)
- user_id (VARCHAR)
- action (VARCHAR)
- resource (VARCHAR)
- status (VARCHAR)
- details (TEXT)
- ip_address (VARCHAR)
- created_at (TIMESTAMP)
```

## Troubleshooting

### Infrastructure Reset
```bash
cd infra
docker compose down -v
docker compose up -d
```

### Check Service Health
```bash
# monitoring-backend
curl http://localhost:8080/actuator/health

# alerts-service
curl http://localhost:8081/actuator/health

# api-gateway
curl http://localhost:8080/actuator/health

# ai-agent
curl http://localhost:8001/health
```

### View Logs
```bash
# Docker logs
docker compose logs -f [service-name]

# Kubernetes logs
kubectl logs -n monitoring-app [pod-name]
```

### Check Kafka Topics
```bash
# List topics
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092

# View messages
docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic monitoring.events --from-beginning
```

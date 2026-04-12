#!/usr/bin/env python3
"""
Synthetic Event Generator for Load Testing
Generates realistic monitoring events to stress test the observability pipeline.
"""

import asyncio
import json
import random
import time
import uuid
from datetime import datetime, timedelta
from typing import Dict, List, Any

import aiokafka
from aiokafka import AIOKafkaProducer
import prometheus_client as prom

# Prometheus metrics
EVENTS_GENERATED = prom.Counter('events_generated_total', 'Total events generated', ['service', 'event_type'])
GENERATION_RATE = prom.Gauge('generation_rate_events_per_sec', 'Current generation rate')
GENERATION_ERRORS = prom.Counter('generation_errors_total', 'Generation errors')


class SyntheticEventGenerator:
    """Generates synthetic monitoring events for load testing."""
    
    def __init__(self, bootstrap_servers: str, topic: str):
        self.bootstrap_servers = bootstrap_servers
        self.topic = topic
        self.producer = None
        self.services = [
            'payment-service',
            'auth-service', 
            'order-service',
            'inventory-service',
            'notification-service',
            'user-service',
            'catalog-service',
            'shipping-service'
        ]
        
        # Event type probabilities (weighted)
        self.event_types = [
            ('HTTP_REQUEST', 0.4),
            ('EXCEPTION', 0.2),
            ('SYSTEM_METRIC', 0.25),
            ('DB_QUERY', 0.15)
        ]
        
        # Service health states for realistic patterns
        self.service_health = {service: 'HEALTHY' for service in self.services}
        self.last_anomaly_time = {service: 0 for service in self.services}
        
    async def start(self):
        """Initialize Kafka producer."""
        try:
            self.producer = AIOKafkaProducer(
                bootstrap_servers=self.bootstrap_servers,
                value_serializer=lambda v: json.dumps(v, default=str).encode('utf-8'),
                key_serializer=lambda k: k.encode('utf-8') if k else None,
                compression_type='gzip',
                max_request_size=1048576,  # 1MB
                batch_size=16384,
                linger_ms=10,  # Small batching for better throughput
                acks='all'
            )
            await self.producer.start()
            print(f"Producer connected to {self.bootstrap_servers}")
        except Exception as e:
            GENERATION_ERRORS.inc()
            print(f"Failed to start producer: {e}")
            raise
    
    async def stop(self):
        """Stop Kafka producer."""
        if self.producer:
            await self.producer.stop()
    
    def generate_http_request_event(self, service: str) -> Dict[str, Any]:
        """Generate HTTP request event."""
        endpoints = {
            'payment-service': ['/api/payments', '/api/refunds', '/api/webhook'],
            'auth-service': ['/api/login', '/api/logout', '/api/refresh'],
            'order-service': ['/api/orders', '/api/orders/{id}', '/api/orders/cancel'],
            'inventory-service': ['/api/stock', '/api/products', '/api/reserve'],
            'notification-service': ['/api/send', '/api/queue', '/api/status'],
            'user-service': ['/api/users', '/api/profile', '/api/preferences'],
            'catalog-service': ['/api/products', '/api/search', '/api/categories'],
            'shipping-service': ['/api/ship', '/api/track', '/api/rates']
        }
        
        service_endpoints = endpoints.get(service, ['/api/default'])
        endpoint = random.choice(service_endpoints)
        
        # Simulate varying response times and status codes
        if self.service_health[service] == 'DEGRADED':
            response_time = random.uniform(800, 2000)
            status_code = random.choices([200, 500, 503], weights=[0.7, 0.2, 0.1])[0]
        elif self.service_health[service] == 'CRITICAL':
            response_time = random.uniform(2000, 5000)
            status_code = random.choices([500, 503, 504], weights=[0.3, 0.4, 0.3])[0]
        else:
            response_time = random.uniform(50, 300)
            status_code = random.choices([200, 201, 400, 404], weights=[0.8, 0.1, 0.08, 0.02])[0]
        
        return {
            'id': str(uuid.uuid4()),
            'eventType': 'HTTP_REQUEST',
            'serviceName': service,
            'timestamp': datetime.utcnow().isoformat() + 'Z',
            'payload': {
                'method': random.choice(['GET', 'POST', 'PUT', 'DELETE']),
                'url': f"http://{service}:8080{endpoint}",
                'responseTime': response_time,
                'statusCode': status_code,
                'userAgent': random.choice(['Mozilla/5.0', 'curl/7.68.0', 'Python-requests/2.28.1']),
                'ip': f"192.168.1.{random.randint(1, 254)}"
            }
        }
    
    def generate_exception_event(self, service: str) -> Dict[str, Any]:
        """Generate exception event."""
        exceptions = [
            'NullPointerException',
            'DatabaseConnectionTimeout',
            'ServiceUnavailableException',
            'RateLimitExceededException',
            'AuthenticationFailedException',
            'DataValidationException',
            'ExternalServiceTimeout',
            'ResourceExhaustedException'
        ]
        
        stack_traces = [
            f"at {service}.Controller.processRequest(Controller.java:{random.randint(50, 200)})",
            f"at {service}.Service.execute(Service.java:{random.randint(30, 150)})",
            f"at {service}.Repository.findById(Repository.java:{random.randint(20, 100)})"
        ]
        
        return {
            'id': str(uuid.uuid4()),
            'eventType': 'EXCEPTION',
            'serviceName': service,
            'timestamp': datetime.utcnow().isoformat() + 'Z',
            'payload': {
                'exceptionType': random.choice(exceptions),
                'message': f"Error processing request in {service}",
                'stackTrace': stack_traces,
                'thread': random.choice(['http-nio-8080-exec-1', 'scheduler-1', 'kafka-consumer-1']),
                'severity': random.choice(['ERROR', 'WARN', 'FATAL'])
            }
        }
    
    def generate_system_metric_event(self, service: str) -> Dict[str, Any]:
        """Generate system metric event."""
        # Simulate degraded/critical states
        if self.service_health[service] == 'DEGRADED':
            cpu_usage = random.uniform(70, 85)
            memory_usage = random.uniform(75, 90)
        elif self.service_health[service] == 'CRITICAL':
            cpu_usage = random.uniform(85, 95)
            memory_usage = random.uniform(90, 98)
        else:
            cpu_usage = random.uniform(10, 60)
            memory_usage = random.uniform(20, 70)
        
        return {
            'id': str(uuid.uuid4()),
            'eventType': 'SYSTEM_METRIC',
            'serviceName': service,
            'timestamp': datetime.utcnow().isoformat() + 'Z',
            'payload': {
                'cpuUsage': cpu_usage,
                'memoryUsage': memory_usage,
                'diskUsage': random.uniform(10, 80),
                'networkIO': random.uniform(1000, 10000),
                'activeConnections': random.randint(50, 500),
                'gcCount': random.randint(0, 100)
            }
        }
    
    def generate_db_query_event(self, service: str) -> Dict[str, Any]:
        """Generate database query event."""
        tables = ['users', 'orders', 'products', 'payments', 'inventory', 'notifications']
        operations = ['SELECT', 'INSERT', 'UPDATE', 'DELETE']
        
        # Simulate degraded/critical states
        if self.service_health[service] == 'DEGRADED':
            query_time = random.uniform(200, 800)
        elif self.service_health[service] == 'CRITICAL':
            query_time = random.uniform(800, 2000)
        else:
            query_time = random.uniform(1, 100)
        
        return {
            'id': str(uuid.uuid4()),
            'eventType': 'DB_QUERY',
            'serviceName': service,
            'timestamp': datetime.utcnow().isoformat() + 'Z',
            'payload': {
                'operation': random.choice(operations),
                'table': random.choice(tables),
                'queryTime': query_time,
                'rowsAffected': random.randint(0, 1000),
                'connectionPool': random.randint(1, 20),
                'lockTime': random.uniform(0, 50)
            }
        }
    
    def generate_event(self, service: str) -> Dict[str, Any]:
        """Generate a single event based on weighted probabilities."""
        event_type = random.choices(
            [et[0] for et in self.event_types],
            weights=[et[1] for et in self.event_types]
        )[0]
        
        if event_type == 'HTTP_REQUEST':
            return self.generate_http_request_event(service)
        elif event_type == 'EXCEPTION':
            return self.generate_exception_event(service)
        elif event_type == 'SYSTEM_METRIC':
            return self.generate_system_metric_event(service)
        elif event_type == 'DB_QUERY':
            return self.generate_db_query_event(service)
    
    def update_service_health(self):
        """Randomly update service health to create realistic patterns."""
        current_time = time.time()
        
        for service in self.services:
            # Randomly change health state (low probability)
            if random.random() < 0.05:  # 5% chance per update
                new_health = random.choice(['HEALTHY', 'HEALTHY', 'HEALTHY', 'DEGRADED', 'CRITICAL'])
                self.service_health[service] = new_health
                
                if new_health in ['DEGRADED', 'CRITICAL']:
                    self.last_anomaly_time[service] = current_time
            elif (self.service_health[service] in ['DEGRADED', 'CRITICAL'] and 
                  current_time - self.last_anomaly_time[service] > 300):  # 5 minutes
                # Auto-recover after 5 minutes
                self.service_health[service] = 'HEALTHY'
    
    async def send_event(self, event: Dict[str, Any]):
        """Send a single event to Kafka."""
        try:
            await self.producer.send_and_wait(
                topic=self.topic,
                key=event['serviceName'],
                value=event
            )
            EVENTS_GENERATED.labels(
                service=event['serviceName'], 
                event_type=event['eventType']
            ).inc()
        except Exception as e:
            GENERATION_ERRORS.inc()
            print(f"Failed to send event: {e}")
            raise
    
    async def generate_load(self, events_per_second: float, duration_minutes: int):
        """Generate load for specified duration."""
        duration_seconds = duration_minutes * 60
        interval = 1.0 / events_per_second
        
        print(f"Starting load generation: {events_per_second} events/sec for {duration_minutes} minutes")
        print(f"Total events to generate: {int(events_per_second * duration_seconds)}")
        
        start_time = time.time()
        event_count = 0
        last_health_update = start_time
        
        try:
            while time.time() - start_time < duration_seconds:
                loop_start = time.time()
                
                # Update service health periodically
                if loop_start - last_health_update > 30:  # Every 30 seconds
                    self.update_service_health()
                    last_health_update = loop_start
                
                # Generate events for multiple services
                num_events = max(1, int(events_per_second / len(self.services)))
                selected_services = random.sample(self.services, min(num_events, len(self.services)))
                
                tasks = []
                for service in selected_services:
                    event = self.generate_event(service)
                    tasks.append(self.send_event(event))
                    event_count += 1
                
                # Send events in parallel
                if tasks:
                    await asyncio.gather(*tasks, return_exceptions=True)
                
                # Update generation rate metric
                GENERATION_RATE.set(events_per_second)
                
                # Maintain precise timing
                elapsed = time.time() - loop_start
                if elapsed < interval:
                    await asyncio.sleep(interval - elapsed)
                
                # Progress reporting
                if event_count % 100 == 0:
                    elapsed_total = time.time() - start_time
                    rate = event_count / elapsed_total if elapsed_total > 0 else 0
                    print(f"Generated {event_count} events in {elapsed_total:.1f}s (rate: {rate:.1f}/sec)")
        
        except KeyboardInterrupt:
            print("\nLoad generation interrupted")
        except Exception as e:
            print(f"Error during load generation: {e}")
            raise
        finally:
            total_time = time.time() - start_time
            actual_rate = event_count / total_time if total_time > 0 else 0
            print(f"\nLoad generation completed:")
            print(f"  Total events: {event_count}")
            print(f"  Duration: {total_time:.1f}s")
            print(f"  Actual rate: {actual_rate:.1f} events/sec")


async def main():
    """Main entry point."""
    import argparse
    
    parser = argparse.ArgumentParser(description='Synthetic Event Generator')
    parser.add_argument('--bootstrap-servers', default='localhost:9092', 
                       help='Kafka bootstrap servers')
    parser.add_argument('--topic', default='monitoring.events', 
                       help='Kafka topic')
    parser.add_argument('--rate', type=float, default=5.0, 
                       help='Events per second')
    parser.add_argument('--duration', type=int, default=3, 
                       help='Duration in minutes')
    parser.add_argument('--metrics-port', type=int, default=8001, 
                       help='Prometheus metrics port')
    
    args = parser.parse_args()
    
    # Start metrics server
    prom.start_http_server(args.metrics_port)
    print(f"Metrics server started on port {args.metrics_port}")
    
    generator = SyntheticEventGenerator(args.bootstrap_servers, args.topic)
    
    try:
        await generator.start()
        await generator.generate_load(args.rate, args.duration)
    finally:
        await generator.stop()


if __name__ == '__main__':
    asyncio.run(main())

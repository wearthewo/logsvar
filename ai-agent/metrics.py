from prometheus_client import Counter, Histogram, Gauge
import asyncio
import time
from typing import Optional

class MetricsCollector:
    def __init__(self):
        # Counters (exactly as specified in the spec)
        self.events_processed = Counter(
            'ai_events_processed_total',
            'Events pulled from Kafka',
            ['service_name', 'event_type']
        )
        
        self.anomalies_detected = Counter(
            'ai_anomalies_detected_total',
            'Confirmed anomalies written to MySQL',
            ['service_name', 'severity']
        )
        
        self.cache_hits = Counter(
            'ai_cache_hits_total',
            'Events skipped due to Redis cache hit'
        )
        
        self.dlq_publishes = Counter(
            'ai_dlq_publishes_total',
            'Events sent to DLQ after retry exhaustion'
        )
        
        # Histograms (exactly as specified in the spec)
        self.ai_call_duration = Histogram(
            'ai_call_duration_seconds',
            'Time waiting for Gemini/Ollama response',
            buckets=[.1, .25, .5, 1, 2, 3, 5, 10]
        )
    
    # Async-safe increment methods
    async def increment_events_processed(self, service_name: str, event_type: str):
        """Async-safe increment events processed counter"""
        loop = asyncio.get_event_loop()
        await loop.run_in_executor(None, 
            self.events_processed.labels(
                service_name=service_name,
                event_type=event_type
            ).inc
        )
    
    async def increment_anomalies_detected(self, service_name: str, severity: str):
        """Async-safe increment anomalies detected counter"""
        loop = asyncio.get_event_loop()
        await loop.run_in_executor(None,
            self.anomalies_detected.labels(
                service_name=service_name,
                severity=severity
            ).inc
        )
    
    async def increment_cache_hits(self):
        """Async-safe increment cache hits counter"""
        loop = asyncio.get_event_loop()
        await loop.run_in_executor(None, self.cache_hits.inc)
    
    async def increment_dlq_publishes(self):
        """Async-safe increment DLQ publishes counter"""
        loop = asyncio.get_event_loop()
        await loop.run_in_executor(None, self.dlq_publishes.inc)
    
    # Context manager for AI call timing
    class AICallTimer:
        def __init__(self, metrics_collector):
            self.metrics_collector = metrics_collector
            self.start_time = None
        
        async def __aenter__(self):
            self.start_time = time.time()
            return self
        
        async def __aexit__(self, exc_type, exc_val, exc_tb):
            if self.start_time is not None:
                duration = time.time() - self.start_time
                loop = asyncio.get_event_loop()
                await loop.run_in_executor(
                    None, 
                    self.metrics_collector.ai_call_duration.observe,
                    duration
                )
    
    def ai_call_timer(self):
        """Return a context manager for timing AI calls"""
        return self.AICallTimer(self)
    
    # Synchronous methods for backward compatibility
    def increment_events_processed_sync(self, service_name: str, event_type: str):
        """Synchronous increment events processed counter"""
        self.events_processed.labels(
            service_name=service_name,
            event_type=event_type
        ).inc()
    
    def increment_anomalies_detected_sync(self, service_name: str, severity: str):
        """Synchronous increment anomalies detected counter"""
        self.anomalies_detected.labels(
            service_name=service_name,
            severity=severity
        ).inc()
    
    def increment_cache_hits_sync(self):
        """Synchronous increment cache hits counter"""
        self.cache_hits.inc()
    
    def increment_dlq_publishes_sync(self):
        """Synchronous increment DLQ publishes counter"""
        self.dlq_publishes.inc()

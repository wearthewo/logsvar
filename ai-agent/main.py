import os
import asyncio
import json
import uuid
from datetime import datetime, timezone
from typing import Dict, Any, Optional
from contextlib import asynccontextmanager

import aiokafka
import aiomysql
import redis.asyncio as redis
from fastapi import FastAPI, HTTPException
from prometheus_client import Counter, Histogram, generate_latest, CONTENT_TYPE_LATEST
from fastapi.responses import Response

from config import Settings
from cache import CacheManager
from db import DatabaseManager
from kafka_client import KafkaClient
from ai_service import AIService
from metrics import MetricsCollector

settings = Settings()
metrics = MetricsCollector()
cache_manager = CacheManager(settings.REDIS_URL)
db_manager = DatabaseManager(settings)
kafka_client = KafkaClient(settings)
ai_service = AIService(settings, metrics)

consumer_task = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Lifespan context manager for startup and shutdown"""
    global consumer_task
    
    # Startup
    await cache_manager.connect()
    await db_manager.connect()
    await kafka_client.connect()
    await ai_service.initialize()
    
    # Start consumer in background
    consumer_task = asyncio.create_task(consume_events())
    
    print("AI Agent started successfully")
    
    yield
    
    # Shutdown
    if consumer_task:
        consumer_task.cancel()
        try:
            await consumer_task
        except asyncio.CancelledError:
            pass
    
    await cache_manager.disconnect()
    await db_manager.disconnect()
    await kafka_client.disconnect()
    print("AI Agent stopped")

app = FastAPI(title="AI Anomaly Detection Agent", version="1.0.0", lifespan=lifespan)

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    try:
        # Check all connections
        kafka_status = "connected" if kafka_client.is_connected() else "disconnected"
        redis_status = "connected" if cache_manager.is_connected() else "disconnected"
        mysql_status = "connected" if db_manager.is_connected() else "disconnected"
        model_status = "available" if ai_service.is_model_available() else "unavailable"
        
        overall_status = "ok" if all(
            status in ["connected", "available"]
            for status in [kafka_status, redis_status, mysql_status, model_status]
        ) else "error"
        
        return {
            "status": overall_status,
            "kafka": kafka_status,
            "redis": redis_status,
            "mysql": mysql_status,
            "model": model_status,
            "timestamp": datetime.now(timezone.utc).isoformat()
        }
    except Exception as e:
        raise HTTPException(status_code=503, detail=f"Health check failed: {str(e)}")

@app.get("/metrics")
async def metrics_endpoint():
    """Prometheus metrics endpoint"""
    return Response(generate_latest(), media_type=CONTENT_TYPE_LATEST)

async def process_event(event_data: Dict[str, Any]) -> bool:
    """Process a single monitoring event. Returns True if successful, False if failed."""
    event_id = event_data.get("id")
    service_name = event_data.get("serviceName", "unknown")
    event_type = event_data.get("eventType", "unknown")
    
    if not event_id:
        print("Event missing ID, skipping")
        return True  # Skip invalid events
    
    print(f"Processing event {event_id} from {service_name} ({event_type})")
    
    # Step 2: Check Redis cache for deduplication
    cached_result = await cache_manager.get_anomaly_result(event_id)
    if cached_result:
        await metrics.increment_cache_hits()
        print(f"Cache hit for event {event_id}, skipping processing")
        return True  # Successfully skipped due to cache
    
    # Step 3-4: Build prompt and call AI
    try:
        async with metrics.ai_call_timer():
            ai_result = await ai_service.detect_anomaly(event_data)
        
        await metrics.increment_events_processed(service_name, event_type)
        
        if not ai_result.get("is_anomaly", False):
            # Step 5: Cache non-anomaly result and skip
            await cache_manager.cache_anomaly_result(event_id, ai_result)
            print(f"Event {event_id} not anomalous, cached result")
            return True
        
        # Step 6: Write anomaly to MySQL
        anomaly_record = {
            "id": str(uuid.uuid4()),
            "event_id": event_id,
            "service_name": service_name,
            "severity": ai_result.get("severity"),
            "reason": ai_result.get("reason"),
            "recommended_action": ai_result.get("recommended_action"),
            "detected_at": datetime.utcnow()
        }
        
        await db_manager.save_anomaly(anomaly_record)
        print(f"Saved anomaly {anomaly_record['id']} to database")
        
        # Step 7: Cache the result
        await cache_manager.cache_anomaly_result(event_id, ai_result)
        
        # Step 8: Publish to monitoring.anomalies topic
        await kafka_client.publish_anomaly(anomaly_record)
        
        # Step 9: Publish to Redis live channel
        await cache_manager.publish_live_anomaly(anomaly_record)
        
        await metrics.increment_anomalies_detected(
            anomaly_record["service_name"],
            anomaly_record["severity"]
        )
        
        print(f"Successfully processed anomaly {anomaly_record['id']} for event {event_id}")
        return True
        
    except Exception as e:
        print(f"Error processing event {event_id}: {e}")
        # Step 10: On retry exhaustion, publish to DLQ
        try:
            await kafka_client.publish_dlq(event_data)
            await metrics.increment_dlq_publishes()
            print(f"Published event {event_id} to DLQ")
        except Exception as dlq_error:
            print(f"Failed to publish event {event_id} to DLQ: {dlq_error}")
        
        return False  # Processing failed

async def consume_events():
    """Main consumer loop for monitoring.events"""
    consumer = await kafka_client.get_consumer()
    
    try:
        print("Starting Kafka consumer loop...")
        async for msg in consumer:
            try:
                event_data = json.loads(msg.value.decode('utf-8'))
                event_id = event_data.get('id', 'unknown')
                
                print(f"Received event {event_id} from partition {msg.partition}, offset {msg.offset}")
                
                # Process the event - this includes all steps 2-9
                success = await process_event(event_data)
                
                if success:
                    # Step 10: Commit offset ONLY after successful processing
                    await consumer.commit()
                    print(f"Committed offset for event {event_id}")
                else:
                    print(f"Processing failed for event {event_id}, NOT committing offset")
                    # Don't commit offset, let Kafka retry
                
            except json.JSONDecodeError as e:
                print(f"JSON decode error: {e}")
                # Don't commit offset, let Kafka retry
            except Exception as e:
                print(f"Error processing message: {e}")
                # Don't commit offset, let Kafka retry
                
    except asyncio.CancelledError:
        print("Consumer task cancelled, shutting down...")
    except Exception as e:
        print(f"Consumer error: {e}")
    finally:
        await consumer.stop()
        print("Kafka consumer stopped")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)

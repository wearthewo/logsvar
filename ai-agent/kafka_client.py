import json
import asyncio
from typing import Dict, Any, Optional
import aiokafka
from aiokafka import AIOKafkaConsumer, AIOKafkaProducer

class KafkaClient:
    def __init__(self, settings):
        self.settings = settings
        self.consumer: Optional[AIOKafkaConsumer] = None
        self.producer: Optional[AIOKafkaProducer] = None
        
        # Topic names
        self.events_topic = "monitoring.events"
        self.anomalies_topic = "monitoring.anomalies"
        self.dlq_topic = "monitoring.anomalies.dlq"
    
    async def connect(self):
        """Initialize Kafka producer"""
        try:
            self.producer = AIOKafkaProducer(
                bootstrap_servers=self.settings.KAFKA_BOOTSTRAP,
                value_serializer=lambda v: json.dumps(v).encode('utf-8'),
                key_serializer=lambda k: k.encode('utf-8') if k else None
            )
            await self.producer.start()
            print("Connected to Kafka producer")
        except Exception as e:
            print(f"Failed to connect Kafka producer: {e}")
            raise
    
    async def disconnect(self):
        """Disconnect Kafka producer"""
        if self.producer:
            await self.producer.stop()
        if self.consumer:
            await self.consumer.stop()
    
    def is_connected(self) -> bool:
        """Check if Kafka is connected"""
        return self.producer is not None
    
    async def get_consumer(self) -> AIOKafkaConsumer:
        """Get or create Kafka consumer"""
        if not self.consumer:
            self.consumer = AIOKafkaConsumer(
                self.events_topic,
                bootstrap_servers=self.settings.KAFKA_BOOTSTRAP,
                group_id="ai-agent",
                enable_auto_commit=False,
                auto_offset_reset="earliest",
                value_deserializer=lambda m: json.loads(m.decode('utf-8')) if m else None,
                key_deserializer=lambda k: k.decode('utf-8') if k else None
            )
            await self.consumer.start()
            print("Connected to Kafka consumer")
        
        return self.consumer
    
    async def publish_anomaly(self, anomaly: Dict[str, Any]):
        """Publish anomaly to monitoring.anomalies topic"""
        if not self.producer:
            raise RuntimeError("Kafka producer not connected")
        
        try:
            await self.producer.send_and_wait(
                topic=self.anomalies_topic,
                key=anomaly["id"],
                value=anomaly
            )
            print(f"Published anomaly {anomaly['id']} to {self.anomalies_topic}")
        except Exception as e:
            print(f"Failed to publish anomaly: {e}")
            raise
    
    async def publish_dlq(self, event_data: Dict[str, Any]):
        """Publish failed event to DLQ"""
        if not self.producer:
            raise RuntimeError("Kafka producer not connected")
        
        try:
            await self.producer.send_and_wait(
                topic=self.dlq_topic,
                key=event_data.get("id"),
                value=event_data
            )
            print(f"Published event {event_data.get('id')} to {self.dlq_topic}")
        except Exception as e:
            print(f"Failed to publish to DLQ: {e}")
            raise

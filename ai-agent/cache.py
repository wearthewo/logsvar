import json
import asyncio
from typing import Dict, Any, Optional
import redis.asyncio as redis

class CacheManager:
    def __init__(self, redis_url: str):
        self.redis_url = redis_url
        self.redis_client: Optional[redis.Redis] = None
    
    async def connect(self):
        """Connect to Redis"""
        try:
            self.redis_client = redis.from_url(self.redis_url)
            await self.redis_client.ping()
            print("Connected to Redis")
        except Exception as e:
            print(f"Failed to connect to Redis: {e}")
            raise
    
    async def disconnect(self):
        """Disconnect from Redis"""
        if self.redis_client:
            await self.redis_client.close()
    
    def is_connected(self) -> bool:
        """Check if Redis is connected"""
        return self.redis_client is not None
    
    async def get_anomaly_result(self, event_id: str) -> Optional[Dict[str, Any]]:
        """Get cached anomaly result for deduplication"""
        try:
            if not self.redis_client:
                return None
            
            key = f"anomaly:{event_id}"
            cached = await self.redis_client.get(key)
            
            if cached:
                return json.loads(cached)
            
            return None
        except Exception as e:
            print(f"Error getting cached anomaly result: {e}")
            return None
    
    async def cache_anomaly_result(self, event_id: str, result: Dict[str, Any], ttl: int = 300):
        """Cache anomaly result with TTL"""
        try:
            if not self.redis_client:
                return
            
            key = f"anomaly:{event_id}"
            value = json.dumps(result)
            await self.redis_client.setex(key, ttl, value)
        except Exception as e:
            print(f"Error caching anomaly result: {e}")
    
    async def publish_live_anomaly(self, anomaly: Dict[str, Any]):
        """Publish anomaly to live channel"""
        try:
            if not self.redis_client:
                return
            
            channel = "anomalies:live"
            message = json.dumps(anomaly)
            await self.redis_client.publish(channel, message)
            print(f"Published to Redis channel {channel}")
        except Exception as e:
            print(f"Error publishing to Redis channel: {e}")
    
    async def get_alert_dedup_key(self, service_name: str, severity: str) -> bool:
        """Check if alert deduplication key exists"""
        try:
            if not self.redis_client:
                return False
            
            key = f"alert:dedup:{service_name}:{severity}"
            exists = await self.redis_client.exists(key)
            return bool(exists)
        except Exception as e:
            print(f"Error checking alert dedup key: {e}")
            return False
    
    async def set_alert_dedup_key(self, service_name: str, severity: str, ttl: int = 300):
        """Set alert deduplication key"""
        try:
            if not self.redis_client:
                return
            
            key = f"alert:dedup:{service_name}:{severity}"
            await self.redis_client.setex(key, ttl, "1")
        except Exception as e:
            print(f"Error setting alert dedup key: {e}")

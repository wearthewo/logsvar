import aiomysql
from typing import Dict, Any, Optional
from datetime import datetime

class DatabaseManager:
    def __init__(self, settings):
        self.settings = settings
        self.pool: Optional[aiomysql.Pool] = None
    
    async def connect(self):
        """Create MySQL connection pool"""
        try:
            self.pool = await aiomysql.create_pool(
                host=self.settings.MYSQL_HOST,
                port=self.settings.MYSQL_PORT,
                user=self.settings.MYSQL_USER,
                password=self.settings.MYSQL_PASSWORD,
                db=self.settings.MYSQL_DATABASE,
                minsize=1,
                maxsize=10
            )
            print("Connected to MySQL")
        except Exception as e:
            print(f"Failed to connect to MySQL: {e}")
            raise
    
    async def disconnect(self):
        """Close MySQL connection pool"""
        if self.pool:
            self.pool.close()
            await self.pool.wait_closed()
    
    def is_connected(self) -> bool:
        """Check if MySQL is connected"""
        return self.pool is not None
    
    async def save_anomaly(self, anomaly: Dict[str, Any]):
        """Save anomaly to database"""
        if not self.pool:
            raise RuntimeError("Database not connected")
        
        INSERT_ANOMALY = """
            INSERT INTO anomalies
              (id, event_id, service_name, severity,
               reason, recommended_action, detected_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s)
            ON DUPLICATE KEY UPDATE
              severity = VALUES(severity),
              reason = VALUES(reason),
              recommended_action = VALUES(recommended_action),
              detected_at = VALUES(detected_at)
        """
        
        async with self.pool.acquire() as conn:
            async with conn.cursor() as cur:
                await cur.execute(INSERT_ANOMALY, (
                    anomaly["id"],
                    anomaly["event_id"],
                    anomaly["service_name"],
                    anomaly["severity"],
                    anomaly["reason"],
                    anomaly.get("recommended_action"),
                    anomaly["detected_at"]
                ))
            await conn.commit()
        
        print(f"Saved anomaly {anomaly['id']} to database")
    
    async def test_connection(self):
        """Test database connection"""
        if not self.pool:
            return False
        
        try:
            async with self.pool.acquire() as conn:
                async with conn.cursor() as cur:
                    await cur.execute("SELECT 1")
                    return True
        except Exception as e:
            print(f"Database connection test failed: {e}")
            return False

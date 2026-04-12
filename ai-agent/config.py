from pydantic_settings import BaseSettings
from typing import Optional

class Settings(BaseSettings):
    # Kafka
    KAFKA_BOOTSTRAP: str = "kafka:29092"
    
    # Redis
    REDIS_URL: str = "redis://redis:6379"
    
    # MySQL
    MYSQL_HOST: str = "mysql"
    MYSQL_PORT: int = 3306
    MYSQL_DATABASE: str = "monitoring"
    MYSQL_USER: str = "root"
    MYSQL_PASSWORD: str = "secret"
    
    # Ollama
    OLLAMA_URL: str = "http://ollama:11434"
    OLLAMA_MODEL: str = "gemma:7b"
    OLLAMA_FALLBACK_MODEL: str = "llama3.2"
    
    # Cache TTL
    CACHE_TTL_SECONDS: int = 300
    
    model_config = {"env_file": ".env"}
    
    @property
    def mysql_dsn(self) -> str:
        return f"mysql://{self.MYSQL_USER}:{self.MYSQL_PASSWORD}@{self.MYSQL_HOST}:{self.MYSQL_PORT}/{self.MYSQL_DATABASE}"

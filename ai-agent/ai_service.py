import json
import httpx
from typing import Dict, Any, Optional
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type

class AIService:
    def __init__(self, settings, metrics_collector=None):
        self.settings = settings
        self.system_prompt = self._get_system_prompt()
        self.model_available = False
        self.fallback_model_available = False
        self.metrics = metrics_collector
    
    async def initialize(self):
        """Initialize AI service and check model availability"""
        # Test connection to Ollama and check available models
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                response = await client.get(f"{self.settings.OLLAMA_URL}/api/tags")
                if response.status_code == 200:
                    models = response.json().get("models", [])
                    model_names = [model.get("name", "") for model in models]
                    
                    self.model_available = any(
                        self.settings.OLLAMA_MODEL in name for name in model_names
                    )
                    self.fallback_model_available = any(
                        self.settings.OLLAMA_FALLBACK_MODEL in name for name in model_names
                    )
                    
                    print(f"Connected to Ollama at {self.settings.OLLAMA_URL}")
                    print(f"Primary model {self.settings.OLLAMA_MODEL} available: {self.model_available}")
                    print(f"Fallback model {self.settings.OLLAMA_FALLBACK_MODEL} available: {self.fallback_model_available}")
                else:
                    print(f"Ollama not available at {self.settings.OLLAMA_URL}")
        except Exception as e:
            print(f"Failed to connect to Ollama: {e}")
    
    def is_model_available(self):
        """Check if any AI model is available"""
        return self.model_available or self.fallback_model_available
    
    def _get_system_prompt(self) -> str:
        """Get the cached system prompt"""
        return """You are a monitoring anomaly detector.
Analyze the event and respond ONLY with valid JSON.
No explanation. No markdown. Raw JSON only.

Return exactly:
{
  "is_anomaly": bool,
  "severity": "LOW" | "MEDIUM" | "HIGH" | "CRITICAL",
  "reason": "plain English explanation under 100 words",
  "recommended_action": "what the engineer should do, under 50 words"
}"""
    
    def _build_user_prompt(self, event_data: Dict[str, Any]) -> str:
        """Build user prompt with event data and baselines"""
        event_json = json.dumps(event_data, indent=2)
        
        return f"""Analyze this monitoring event for anomalies:
{event_json}

Baselines:
- HTTP latency normal: < 500ms. Anomalous: > 2000ms
- Error rate normal: < 1%. Anomalous: > 5%
- CPU normal: < 70%. Anomalous: > 85%
- DB query normal: < 200ms. Anomalous: > 1000ms
- Connection pool anomalous: > 90% used
- Exception occurrences anomalous: > 10 in same thread"""
    
    async def _call_ollama_model(self, model_name: str, user_prompt: str) -> Dict[str, Any]:
        """Call specific Ollama model"""
        payload = {
            "model": model_name,
            "system": self.system_prompt,
            "prompt": user_prompt,
            "stream": False
        }
        
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(
                f"{self.settings.OLLAMA_URL}/api/generate",
                json=payload
            )
            
            if response.status_code != 200:
                raise Exception(f"Ollama API error: {response.status_code} - {response.text}")
            
            result = response.json()
            ai_response = result.get("response", "").strip()
            
            # Parse JSON response
            try:
                # Remove any markdown formatting
                if ai_response.startswith("```json"):
                    ai_response = ai_response[7:]
                if ai_response.endswith("```"):
                    ai_response = ai_response[:-3]
                
                parsed = json.loads(ai_response)
                
                # Validate required fields
                required_fields = ["is_anomaly", "severity", "reason", "recommended_action"]
                for field in required_fields:
                    if field not in parsed:
                        parsed[field] = "" if field in ["reason", "recommended_action"] else False
                
                # Validate severity
                valid_severities = ["LOW", "MEDIUM", "HIGH", "CRITICAL"]
                if parsed.get("severity") not in valid_severities:
                    parsed["severity"] = "MEDIUM"
                
                return parsed
                
            except json.JSONDecodeError as e:
                print(f"Failed to parse AI response: {ai_response}")
                raise Exception(f"JSON parsing error: {e}")
    
    def _rule_based_detection(self, event_data: Dict[str, Any]) -> Dict[str, Any]:
        """Fallback rule-based anomaly detection"""
        event_type = event_data.get("eventType", "")
        payload = event_data.get("payload", {})
        
        # Default non-anomaly
        result = {
            "is_anomaly": False,
            "severity": "LOW",
            "reason": "No anomaly detected by rules",
            "recommended_action": "No action required"
        }
        
        try:
            if event_type == "HTTP_REQUEST":
                latency = payload.get("latencyMs", 0)
                status_code = payload.get("statusCode", 200)
                
                if latency > 2000 or status_code >= 500:
                    result["is_anomaly"] = True
                    result["severity"] = "HIGH" if latency > 5000 else "MEDIUM"
                    result["reason"] = f"HTTP {status_code} with {latency}ms latency exceeds thresholds"
                    result["recommended_action"] = "Check service health and downstream dependencies"
                    
            elif event_type == "EXCEPTION":
                occurrences = payload.get("occurrences", 1)
                error_type = payload.get("errorType", "")
                
                if occurrences > 10 or "OutOfMemoryError" in error_type or "StackOverflowError" in error_type:
                    result["is_anomaly"] = True
                    result["severity"] = "HIGH" if "OutOfMemoryError" in error_type else "MEDIUM"
                    result["reason"] = f"Exception {error_type} occurred {occurrences} times"
                    result["recommended_action"] = "Review error logs and fix root cause"
                    
            elif event_type == "SYSTEM_METRIC":
                cpu_percent = payload.get("cpuPercent", 0)
                memory_used = payload.get("memoryUsedMb", 0)
                memory_total = payload.get("memoryTotalMb", 1)
                
                memory_percent = (memory_used / memory_total) * 100 if memory_total > 0 else 0
                
                if cpu_percent > 85 or memory_percent > 90:
                    result["is_anomaly"] = True
                    result["severity"] = "CRITICAL" if cpu_percent > 95 or memory_percent > 95 else "HIGH"
                    result["reason"] = f"High resource usage: CPU {cpu_percent}%, Memory {memory_percent:.1f}%"
                    result["recommended_action"] = "Scale up resources or optimize application"
                    
            elif event_type == "DB_QUERY":
                latency = payload.get("latencyMs", 0)
                connection_pool_used = payload.get("connectionPoolUsed", 0)
                connection_pool_max = payload.get("connectionPoolMax", 1)
                
                pool_percent = (connection_pool_used / connection_pool_max) * 100 if connection_pool_max > 0 else 0
                
                if latency > 1000 or pool_percent > 90:
                    result["is_anomaly"] = True
                    result["severity"] = "HIGH" if pool_percent > 95 else "MEDIUM"
                    result["reason"] = f"DB query {latency}ms with {pool_percent:.1f}% pool usage"
                    result["recommended_action"] = "Optimize query and check connection pool settings"
        
        except Exception as e:
            print(f"Error in rule-based detection: {e}")
            result["reason"] = f"Rule detection error: {str(e)}"
        
        return result
    
    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=4, max=10),
        retry=retry_if_exception_type((httpx.ConnectError, httpx.TimeoutException))
    )
    async def detect_anomaly(self, event_data: Dict[str, Any]) -> Dict[str, Any]:
        """Detect anomaly using AI with fallback to rules"""
        user_prompt = self._build_user_prompt(event_data)
        
        # Try primary AI model
        if self.model_available:
            try:
                if self.metrics:
                    async with self.metrics.ai_call_timer():
                        return await self._call_ollama_model(self.settings.OLLAMA_MODEL, user_prompt)
                else:
                    return await self._call_ollama_model(self.settings.OLLAMA_MODEL, user_prompt)
            except Exception as e:
                print(f"Primary model failed: {e}")
        
        # Try fallback AI model
        if self.fallback_model_available:
            try:
                if self.metrics:
                    async with self.metrics.ai_call_timer():
                        return await self._call_ollama_model(self.settings.OLLAMA_FALLBACK_MODEL, user_prompt)
                else:
                    return await self._call_ollama_model(self.settings.OLLAMA_FALLBACK_MODEL, user_prompt)
            except Exception as e:
                print(f"Fallback model failed: {e}")
        
        # Fallback to rule-based detection
        print("Using rule-based anomaly detection")
        return self._rule_based_detection(event_data)

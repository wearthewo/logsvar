import pytest
import asyncio
from unittest.mock import AsyncMock


def test_config_import():
    """Test that config can be imported."""
    try:
        from config import Settings
        settings = Settings()
        assert settings is not None
    except ImportError:
        pytest.fail("Could not import config.Settings")


def test_ai_service_import():
    """Test that AI service can be imported."""
    try:
        from ai_service import AIService
        assert AIService is not None
    except ImportError:
        pytest.fail("Could not import AIService")


def test_main_import():
    """Test that main module can be imported."""
    try:
        import main
        assert main is not None
    except ImportError:
        pytest.fail("Could not import main module")


@pytest.mark.parametrize("event,expected_severity", [
    ({"eventType": "HTTP_REQUEST", "payload": {"latencyMs": 3000, "statusCode": 503}}, "MEDIUM"),
    ({"eventType": "EXCEPTION", "payload": {"errorType": "OutOfMemoryError", "occurrences": 2}}, "HIGH"),
    ({"eventType": "SYSTEM_METRIC", "payload": {"cpuPercent": 97, "memoryUsedMb": 500, "memoryTotalMb": 1000}}, "CRITICAL"),
    ({"eventType": "DB_QUERY", "payload": {"latencyMs": 1500, "connectionPoolUsed": 8, "connectionPoolMax": 10}}, "MEDIUM"),
])
def test_canonical_payloads_trigger_rule_fallback(event, expected_severity):
    from ai_service import AIService
    from config import Settings
    result = AIService(Settings())._rule_based_detection(event)
    assert result["is_anomaly"] is True
    assert result["severity"] == expected_severity


@pytest.mark.parametrize("response", ["not-json", "[]", '{"is_anomaly": true}'])
def test_malformed_ai_responses_are_rejected(response):
    from ai_service import AIService
    with pytest.raises(ValueError, match="Invalid AI response"):
        AIService._parse_ai_response(response)


def test_model_failure_falls_back_to_rules():
    from ai_service import AIService
    from config import Settings
    service = AIService(Settings())
    service.model_available = True
    service._call_ollama_model = AsyncMock(side_effect=ValueError("malformed"))
    result = asyncio.run(service.detect_anomaly({
        "eventType": "HTTP_REQUEST",
        "payload": {"latencyMs": 3001, "statusCode": 503}
    }))
    assert result["is_anomaly"] is True
    assert result["severity"] == "MEDIUM"

import pytest


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

FROM python:3.12.7-slim AS runtime

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .

EXPOSE 8001
HEALTHCHECK --interval=10s --timeout=3s --retries=5 CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8001/health')"
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]

FROM runtime AS test
RUN pip install --no-cache-dir -r requirements-dev.txt
CMD ["python", "-m", "pytest", "tests", "-v"]

FROM runtime AS production

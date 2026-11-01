import os

from dotenv import load_dotenv

load_dotenv()

RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "localhost")
RABBITMQ_PORT = int(os.getenv("RABBITMQ_PORT", "5672"))
RABBITMQ_USER = os.getenv("RABBITMQ_USER", "videointel")
RABBITMQ_PASSWORD = os.getenv("RABBITMQ_PASSWORD", "videointel123")

MINIO_ENDPOINT = os.getenv("MINIO_ENDPOINT", "localhost:9000")
MINIO_ACCESS_KEY = os.getenv("MINIO_ACCESS_KEY", "videointel")
MINIO_SECRET_KEY = os.getenv("MINIO_SECRET_KEY", "videointel123")
MINIO_BUCKET = os.getenv("MINIO_BUCKET", "videos")
MINIO_SECURE = os.getenv("MINIO_SECURE", "false").lower() == "true"

# Transcription provider strategy: "local" (faster-whisper, $0) or "openai" (OpenAI Whisper API)
TRANSCRIPTION_PROVIDER = os.getenv("TRANSCRIPTION_PROVIDER", "local").lower()
WHISPER_MODEL_SIZE = os.getenv("WHISPER_MODEL_SIZE", "base")
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")

# Same queue names as RabbitMqConfig.java in video-intel-service - this worker and the
# backend must always agree on these literally, so if you ever rename one, rename both.
ANALYSIS_REQUESTED_QUEUE = "analysis.requested"
ANALYSIS_COMPLETED_QUEUE = "analysis.completed"

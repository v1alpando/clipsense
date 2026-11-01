# AGENT 03: AI Pipeline & Video Processing Architecture
**Роль:** Senior Python Engineer & AI Pipeline Architect  
**Рабочий файл синхронизации:** `context/AGENT_03_AI_PIPELINE.md`  
**Смежные компоненты:** `video-intel-worker`, `video-ai-worker`, RabbitMQ Contracts, MinIO / S3 Storage, AI Providers (faster-whisper local, OpenAI Whisper API, HuggingFace/BLIP).

---

## 1. Область ответственности (Scope)

- **Worker Architecture & Runtime:**
  - `video-intel-worker`: Worker транскрипции (RabbitMQ Consumer, Pika, MinIO SDK, Provider Strategy: `local` faster-whisper / `openai` Whisper API).
  - `video-ai-worker`: Local on-premise AI воркер мультимодального анализа (PyTorch, faster-whisper, BLIP/Transformers, OpenCV, Pillow, Pydantic).
- **Video & Audio Processing:**
  - Проверка потоков, извлечение аудиодорожки (16kHz mono MP3/WAV) через `ffmpeg`/`ffprobe` с таймаутами.
  - Умный сэмплинг кадров (fixed interval, scene detection) и ограничение нагрузки.
  - Извлечение метаданных контейнеров и видеопотоков.
- **AI/ML Engine Integration:**
  - Гибкая стратегия провайдеров (`TRANSCRIPTION_PROVIDER=local|openai`).
  - Локальный инференс через `faster-whisper` (`base`, CPU `int8`, $0 затрат, без лимита 25MB) с Singleton-кэшированием модели в памяти воркера.
  - Облачный инференс через OpenAI Whisper API (`verbose_json`) с автоматическим graceful fallback на локальный `faster-whisper` при ошибках квоты/429 (`credit_balance_exhausted`) или превышении 25MB.
  - Интеграция Vision/VLM (BLIP, локальные модели описания сцен и таймлайнов).
- **Message Processing & Reliability:**
  - RabbitMQ Consumer lifecycle, manual ACKs/NACKs, dead-letter exchanges (DLX).
  - Защита от потери heartbeat при долгом синхронном инференсе AI-моделей.
  - Идемпотентность обработки на уровне воркеров и гарантированная доставка результатов.
- **Resource & Performance Management:**
  - Управление памятью (Singleton-модели для исключения перезагрузки весов на каждую задачу).
  - Жизненный цикл временных файлов и директорий (`tempfile`, гарантированная очистка в `finally`).

---

## 2. Текущее состояние кодовой базы (Baseline Audit)

### Компонент `video-intel-worker` (Hybrid Local / Cloud Worker)
- **Стек:** Python 3.12 (`.venv` синхронизирован с `faster-whisper 1.0.3`), `pika 1.3.2`, `minio 7.2.9`, `openai 1.109.1`, `python-dotenv 1.0.1`.
- **Реализовано:**
  1. **Provider Strategy (`app/config.py`, `app/transcription.py`):**
     - Параметр `TRANSCRIPTION_PROVIDER` (`local` по умолчанию, либо `openai`) и `WHISPER_MODEL_SIZE` (`base`).
     - Локальный провайдер `transcribe_local` на базе `faster-whisper` (`device="cpu"`, `compute_type="int8"`). Модель инициализируется лениво один раз (Singleton) и переиспользуется между задачами.
     - Облачный провайдер `transcribe_openai` с автоматическим перехватом `429 / Quota / credit_balance_exhausted / AudioPayloadTooLargeError` и прозрачным fallback на локальный `faster-whisper`.
  2. **Аудио-препроцессинг (`app/audio.py`):**
     - Проверка `has_audio_stream` (`ffprobe`) и сжатие в 16kHz mono MP3 (`ffmpeg`, таймаут 180с).
  3. **Контракт и очистка (`app/models.py`, `app/main.py`):**
     - Возврат полного текста и массива сегментов `[{"start": float, "end": float, "text": str}]`.
     - Полная очистка временных видео- и аудиофайлов в блоке `finally`.

### Компонент `video-ai-worker` (Local Multimodal Worker)
- **Стек:** Python 3.12, `torch 2.4.1`, `transformers 4.44.2`, `faster-whisper 1.0.3`, `pillow 10.4.0`, `pydantic 2.9.2`.
- **Статус готовности:** Автономный CLI prototype (метаданные + faster-whisper + BLIP image captioning).

---

## 3. Ключевые проблемы и технический долг (Critical Technical Debt)

### Категория A: Архитектурные риски и надежность

1. **Poison Pill Crash Loop в Pika Consumer (`video-intel-worker`):**
   - В `queue_client.py` десериализация JSON и маппинг в DTO выполняются *до* блока `try/except`.
2. **RabbitMQ Heartbeat Dropout (`video-intel-worker`):**
   - Pika `BlockingConnection` блокируется во время инференса. На длинных видео локальный CPU-инференс может занять >60 секунд, что приведет к разрыву соединения по heartbeat.
3. **Согласование контракта `AnalysisResultMessage` с Агентом 02 (Backend):**
   - Поддержка поля `segments` на стороне Java consumer (`AnalysisResultConsumer` / `AnalysisResultMessage`).

---

## 4. Ожидаемые входящие задачи и синхронизация

1. **End-to-End проверка вертикального среза:**
   - Тестирование загрузки реального видео через UI/Backend -> MinIO -> RabbitMQ -> `video-intel-worker` (local faster-whisper) -> `analysis.completed`.
2. **Харденинг очереди (Задача для Агента 03):**
   - Вынос тяжелого инференса в фоновый поток / `add_callback_threadsafe` для сохранения RabbitMQ heartbeats.
   - Защита от Poison Pill в `queue_client.py`.

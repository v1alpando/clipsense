# AGENT 03: AI Pipeline & Video Processing Architecture
**Роль:** Senior Python Engineer & AI Pipeline Architect  
**Рабочий файл синхронизации:** `context/AGENT_03_AI_PIPELINE.md`  
**Смежные компоненты:** `video-intel-worker`, `video-ai-worker`, RabbitMQ Contracts, MinIO / S3 Storage, AI Providers (faster-whisper local, Google Gemini `gemini-3.8-flash` via `google-genai`, OpenAI Whisper/GPT-4o-mini, HuggingFace/BLIP).

---

## 1. Область ответственности (Scope)

- **Worker Architecture & Runtime:**
  - `video-intel-worker`: Worker транскрипции и семантического AI-анализа (RabbitMQ Consumer, Pika, MinIO SDK, Provider Strategy: `local` faster-whisper / `openai` Whisper API + Deep Analysis Generator).
  - `video-ai-worker`: Local on-premise AI воркер мультимодального анализа (PyTorch, faster-whisper, BLIP/Transformers, OpenCV, Pillow, Pydantic).
- **Video & Audio Processing:**
  - Проверка потоков, извлечение аудиодорожки (16kHz mono MP3/WAV) через `ffmpeg`/`ffprobe` с таймаутами.
  - Умный сэмплинг кадров (fixed interval, scene detection) и ограничение нагрузки.
  - Извлечение метаданных контейнеров и видеопотоков.
- **AI/ML Engine & Intelligence Layer Integration:**
  - Гибкая стратегия провайдеров транскрипции (`TRANSCRIPTION_PROVIDER=local|openai`).
  - Локальный инференс через `faster-whisper` (`base`, CPU `int8`, $0 затрат, без лимита 25MB) с Singleton-кэшированием модели.
  - Облачный инференс через OpenAI Whisper API (`verbose_json`) с автоматическим graceful fallback на локальный `faster-whisper` при ошибках квоты/429.
  - **Deep AI Analysis (`app/analysis_generator.py`):** Генерация структурированного семантического разбора (`summary`, `hook`, `key_moments`, `takeaways`) через официальный SDK `google-genai` (`gemini-3.8-flash` с JSON Schema валидацией Pydantic), OpenAI `gpt-4o-mini` или детерминированный локальный анализатор таймкодов (100% надежный fallback без API-ключей).
  - Интеграция Vision/VLM (BLIP, локальные модели описания сцен и таймлайнов).
- **Message Processing & Reliability:**
  - RabbitMQ Consumer lifecycle, manual ACKs/NACKs, dead-letter exchanges (DLX).
  - Защита от потери heartbeat при долгом синхронном инференсе AI-моделей.
  - Идемпотентность обработки на уровне воркеров и гарантированная доставка результатов.

---

## 2. Текущее состояние кодовой базы (Baseline Audit)

### Компонент `video-intel-worker` (Hybrid Local / Cloud Worker + Intelligence Layer)
- **Стек:** Python 3.12 (`.venv`), `faster-whisper 1.0.3`, `google-genai 2.34.0`, `pydantic 2.14.2`, `pika 1.3.2`, `minio 7.2.9`, `openai 1.109.1`, `python-dotenv 1.0.1`.
- **Реализовано:**
  1. **Deep AI Analysis Generator (`app/analysis_generator.py`):**
     - Строгая Pydantic-схема `DeepAnalysisResult` (`summary`, `hook: {score, type, critique, suggestion}`, `key_moments: [{timestamp, title, description}]`, `takeaways: [str]`).
     - Вызов Google Gemini (`gemini-3.8-flash`, актуальная замена устаревшему `gemini-1.5-flash`) через `client.interactions.create` с `response_format={"type": "text", "mime_type": "application/json", "schema": DeepAnalysisResult.model_json_schema()}`.
     - Поддержка резервного вызова OpenAI `gpt-4o-mini` (`chat.completions.parse`).
     - **Детерминированный локальный анализатор (`generate_deterministic_fallback`):** если ключи `GEMINI_API_KEY` / `OPENAI_API_KEY` не заданы или квота исчерпана, воркер строит валидный семантический разбор на основе первых 3–5 секунд (оценка хука), группирует сегменты в главы с таймкодами (`key_moments`) и выделяет ключевые тезисы (`takeaways`). Система никогда не падает.
  2. **Provider Strategy транскрипции (`app/config.py`, `app/transcription.py`):**
     - `TRANSCRIPTION_PROVIDER=local` по умолчанию (`faster-whisper` base на CPU `int8`) + fallback с OpenAI при 429/Quota.
  3. **Аудио-препроцессинг (`app/audio.py`):**
     - Проверка `has_audio_stream` (`ffprobe`) и сжатие в 16kHz mono MP3 (`ffmpeg`, таймаут 180с).
  4. **Контракт и связка (`app/models.py`, `app/main.py`):**
     - В `AnalysisResultMessage` добавлено поле `analysis: Optional[dict[str, Any]] = None` (наряду с `transcript_text` и `segments`).
     - В `main.py` после транскрипции (включая случай видео без звука) вызывается `analysis_generator.generate_analysis(transcript_text, segments)` и результат передается в очередь `analysis.completed`.

### Компонент `video-ai-worker` (Local Multimodal Worker)
- **Стек:** Python 3.12, `torch 2.4.1`, `transformers 4.44.2`, `faster-whisper 1.0.3`, `pillow 10.4.0`, `pydantic 2.9.2`.
- **Статус готовности:** Автономный CLI prototype (метаданные + faster-whisper + BLIP image captioning).

---

## 3. Ключевые проблемы и технический долг (Critical Technical Debt)

1. **Согласование контракта `AnalysisResultMessage` с Агентом 02 (Backend):**
   - Теперь сообщение в `analysis.completed` содержит поля `segments` и `analysis` (`summary`, `hook`, `key_moments`, `takeaways`).
   - Агенту 02 необходимо принимать и сохранять эти поля в `video-intel-service`.
2. **Poison Pill Crash Loop & RabbitMQ Heartbeat (`video-intel-worker`):**
   - Защита десериализации входящего сообщения в `queue_client.py` и вынос длительной обработки в фоновый поток для сохранения AMQP heartbeat.

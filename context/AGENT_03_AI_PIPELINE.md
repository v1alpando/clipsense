# AGENT 03: AI Pipeline & Video Processing Architecture
**Роль:** Senior Python Engineer & AI Pipeline Architect  
**Рабочий файл синхронизации:** `context/AGENT_03_AI_PIPELINE.md`  
**Смежные компоненты:** `video-intel-worker`, `video-ai-worker`, RabbitMQ Contracts, MinIO / S3 Storage, AI Providers (OpenAI Whisper, faster-whisper, HuggingFace/BLIP).

---

## 1. Область ответственности (Scope)

- **Worker Architecture & Runtime:**
  - `video-intel-worker` (Python 3.14): Cloud-based воркер транскрипции (RabbitMQ Consumer, Pika, MinIO SDK, OpenAI Whisper API).
  - `video-ai-worker` (Python 3.12): Local on-premise AI воркер мультимодального анализа (PyTorch, faster-whisper, BLIP/Transformers, OpenCV, Pillow, Pydantic).
- **Video & Audio Processing:**
  - Проверка потоков, извлечение аудиодорожки (16kHz mono MP3/WAV) через `ffmpeg`/`ffprobe` с таймаутами.
  - Умный сэмплинг кадров (fixed interval, scene detection) и ограничение нагрузки.
  - Извлечение метаданных контейнеров и видеопотоков.
- **AI/ML Engine Integration:**
  - Интеграция ASR (OpenAI Whisper API `verbose_json` с сегментами vs `faster-whisper` on CPU/GPU int8/float16).
  - Интеграция Vision/VLM (BLIP, локальные модели описания сцен и таймлайнов).
  - Контроль контекста, VAD (Voice Activity Detection), борьба с галлюцинациями на тишине.
- **Message Processing & Reliability:**
  - RabbitMQ Consumer lifecycle, manual ACKs/NACKs, dead-letter exchanges (DLX).
  - Защита от потери heartbeat при долгом синхронном инференсе AI-моделей.
  - Идемпотентность обработки на уровне воркеров и гарантированная доставка результатов.
- **Resource & Performance Management:**
  - Управление памятью (PyTorch CPU/CUDA caching, singleton-модели vs повторная загрузка).
  - Жизненный цикл временных файлов и директорий (`tempfile`, гарантированная очистка в `finally`).
  - Бюджетирование и стоимость (Pre-flight checks, 25MB limit guard, сжатие аудио в MP3 64 kbps).

---

## 2. Текущее состояние кодовой базы (Baseline Audit)

### Компонент `video-intel-worker` (Cloud/API Worker)
- **Стек:** Python 3.14, `pika 1.3.2`, `minio 7.2.9`, `openai 1.109.1`, `python-dotenv 1.0.1`.
- **Реализовано (Вертикальный срез транскрибации):**
  1. `app/audio.py`: Интегрирован модуль работы с `ffmpeg`/`ffprobe` (поиск бинарников в PATH и conda-окружении, таймаут подпроцессов 180с, проверка `has_audio_stream`, извлечение в 16kHz mono MP3 @ 64 kbps).
  2. `app/transcription.py`: Переведен на `response_format="verbose_json"` и `timestamp_granularities=["segment"]`. Добавлен пре-флайт лимит `AudioPayloadTooLargeError` на 25 MB. Извлечение структурированных сегментов `[{"start": float, "end": float, "text": str}]`.
  3. `app/models.py`: Контракт `AnalysisResultMessage` расширен опциональным полем `segments: Optional[list[dict]]`.
  4. `app/main.py`: Гарантированная очистка временных артефактов (видео и аудио) в блоке `finally`. Обработка беззвучных видео без падения (возврат пустого транскрипта).
- **Статус готовности:** Production-ready препроцессинг и вызов API с таймкодами.

### Компонент `video-ai-worker` (Local AI Worker)
- **Стек:** Python 3.12, `torch 2.4.1`, `transformers 4.44.2`, `faster-whisper 1.0.3`, `pillow 10.4.0`, `pydantic 2.9.2`.
- **Архитектура:** Консольный CLI пайплайн (`python -m app.main <video_path>`), выполняющий:
  1. `extract_metadata` (ffprobe)
  2. `extract_audio` (ffmpeg -> 16kHz mono WAV)
  3. `transcribe` (faster-whisper `base`, CPU `int8`)
  4. `extract_frames` (ffmpeg fps=1/N)
  5. `FrameCaptioner` (BLIP `blip-image-captioning-base`)
  6. `combine` (Pydantic сборка `AnalysisResult`)
- **Статус готовности:** Автономный CLI prototype. Ожидает демонизации и подключения к шине сообщений.

---

## 3. Ключевые проблемы и технический долг (Critical Technical Debt)

### Категория A: Архитектурные риски и надежность

1. **Poison Pill Crash Loop в Pika Consumer (`video-intel-worker`):**
   - В `queue_client.py` десериализация JSON и маппинг в DTO выполняются *до* блока `try/except`.
   - Невалидное сообщение роняет воркер с необработанным исключением, сообщение не подтверждается (`no ack`), при перезапуске забирается снова.
2. **RabbitMQ Heartbeat Dropout (`video-intel-worker`):**
   - Pika `BlockingConnection` работает в одном потоке. Скачивание видео и вызов OpenAI Whisper блокируют event loop на десятки секунд.
   - Риск разрыва TCP-соединения брокером по таймауту heartbeat.
3. **Согласование контракта `AnalysisResultMessage` с Агентом 02 (Backend):**
   - В Java backend `AnalysisResultMessage` является `record(UUID jobId, boolean success, String transcriptText, String failureReason)`.
   - Если Jackson на бэкенде сконфигурирован с `FAIL_ON_UNKNOWN_PROPERTIES=true` (дефолт Jackson 2 без аннотаций), десериализация нового поля `segments` вызовет `UnrecognizedPropertyException`.
   - *Задача для Агента 02:* Добавить поддержку `segments` в Java DTO или аннотировать класс `@JsonIgnoreProperties(ignoreUnknown = true)`.
4. **Аудиофайлы > 25 MB (Видео длительностью > 3–4 часов):**
   - Сейчас перехватывается с `AudioPayloadTooLargeError`. Требуется архитектурное решение: VAD-based чанкинг либо маршрутизация в `video-ai-worker`.

---

## 4. Ожидаемые входящие задачи и синхронизация

1. **Синхронизация с Агентом 02 (Backend):**
   - Подтвердить прием `segments` в `AnalysisResultMessage` на стороне Spring Boot сервиса.
   - Согласовать сохранение сегментов с таймкодами в БД (таблица `transcripts` / JSONB колонка).
2. **Харденинг очереди (Задача для Агента 03):**
   - Защита от Poison Pill в `queue_client.py`.
   - Фоновый поток/Keep-alive для RabbitMQ heartbeats при долгих транскрибациях.

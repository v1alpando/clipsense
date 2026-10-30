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
  - Проверка потоков, извлечение аудиодорожки (16kHz mono PCM) через `ffmpeg`/`ffprobe`.
  - Умный сэмплинг кадров (fixed interval, scene detection) и ограничение нагрузки.
  - Извлечение метаданных контейнеров и видеопотоков.
- **AI/ML Engine Integration:**
  - Интеграция ASR (OpenAI Whisper API vs faster-whisper on CPU/GPU int8/float16).
  - Интеграция Vision/VLM (BLIP, локальные модели описания сцен и таймлайнов).
  - Контроль контекста, VAD (Voice Activity Detection), борьба с галлюцинациями на тишине.
- **Message Processing & Reliability:**
  - RabbitMQ Consumer lifecycle, manual ACKs/NACKs, dead-letter exchanges (DLX).
  - Защита от потери heartbeat при долгом синхронном инференсе AI-моделей.
  - Идемпотентность обработки на уровне воркеров и гарантированная доставка результатов.
- **Resource & Performance Management:**
  - Управление памятью (PyTorch CPU/CUDA caching, singleton-модели vs повторная загрузка).
  - Жизненный цикл временных файлов и директорий (`tempfile`, чистка артефактов).
  - Бюджетирование и стоимость (API limits 25MB, тарификация за минуту, оптимизация размера аудио).

---

## 2. Текущее состояние кодовой базы (Baseline Audit)

### Компонент `video-intel-worker` (Cloud/API Worker)
- **Стек:** Python 3.14, `pika 1.3.2`, `minio 7.2.9`, `openai 1.109.1`, `python-dotenv 1.0.1`.
- **Архитектура:** Простой синхронный Pika consumer (`run_consumer`), читающий `analysis.requested`, скачивающий объект из MinIO во временную папку и отправляющий файл в OpenAI Whisper API (`whisper-1`). Результат отправляется в `analysis.completed`.
- **Статус готовности:** Proof of Concept (рабочий happy path для небольших файлов).

### Компонент `video-ai-worker` (Local AI Worker)
- **Стек:** Python 3.12, `torch 2.4.1`, `transformers 4.44.2`, `faster-whisper 1.0.3`, `pillow 10.4.0`, `pydantic 2.9.2`.
- **Архитектура:** Консольный CLI пайплайн (`python -m app.main <video_path>`), выполняющий:
  1. `extract_metadata` (ffprobe)
  2. `extract_audio` (ffmpeg -> 16kHz mono WAV) при наличии аудиопотока
  3. `transcribe` (faster-whisper `base`, CPU `int8`)
  4. `extract_frames` (ffmpeg fps=1/N)
  5. `FrameCaptioner` (BLIP `blip-image-captioning-base`)
  6. `combine` (Pydantic сборка `AnalysisResult`)
- **Статус готовности:** Автономный CLI prototype. Отсутствует интеграция с очередями (RabbitMQ) и хранилищем (MinIO).

---

## 3. Ключевые проблемы и технический долг (Critical Technical Debt)

### Категория A: Архитектурные риски и надежность (Очереди, Concurrency, Ресурсы)

1. **Poison Pill Crash Loop в Pika Consumer (`video-intel-worker`):**
   - В `queue_client.py` десериализация JSON и маппинг в DTO выполняются *до* блока `try/except`.
   - Невалидное сообщение роняет воркер с необработанным исключением, сообщение не подтверждается (`no ack`), при перезапуске забирается снова. Воркер уходит в crash-loop.
   - *Решение:* Защитная десериализация, явный NACK без requeue (или отправка в Dead Letter Queue / DLQ).

2. **RabbitMQ Heartbeat Dropout & Double Billing (`video-intel-worker`):**
   - Pika `BlockingConnection` работает в одном потоке. Скачивание видео из MinIO и обращение к Whisper API блокируют event loop на десятки секунд / минуты.
   - RabbitMQ разрывает соединение по таймауту отсутствия heartbeat (обычно 60 сек). При завершении инференса вызов `basic_ack` падает, соединение закрыто, сообщение возвращается в очередь другому консьюмеру.
   - *Последствия:* Дублирование расходов на OpenAI API, двойная нагрузка.
   - *Решение:* Разнесение I/O очередей и долгого пайплайна (thread pool / background execution с `connection.add_callback_threadsafe`), либо адаптивная настройка heartbeat/prefetch.

3. **Отсутствие контракта и интеграции с очередями у `video-ai-worker`:**
   - Локальный AI воркер сейчас не подключен к RabbitMQ и MinIO.
   - Требуется согласовать с backend архитектурное разделение: единый воркер со стратегиями (cloud vs local), разные очереди (`analysis.cloud.requested` vs `analysis.local.requested`), либо двухфазный пайплайн.

4. **Инициализация AI-моделей на каждый запрос (`video-ai-worker`):**
   - В `transcription/whisper.py` объект `WhisperModel` пересоздается внутри функции при каждом вызове.
   - В `main.py` экземпляр `FrameCaptioner` создается заново на каждый запуск.
   - *Решение:* Singleton / Service lifecycle management с прогревом (warm-up) при старте процесса.

5. **Resource Exhaustion & Subprocess Hangs (`video-ai-worker`):**
   - Извлечение кадров `fps=1/interval` на длинном видео (например, 1 час при интервале 2 сек) генерирует 1800+ файлов на диск и 1800 sequential инференсов BLIP без батчинга. Это O(N) по диску и десятки минут CPU.
   - Вызовы `subprocess.run` для `ffmpeg`/`ffprobe` не имеют аргумента `timeout`. Поврежденный видеофайл может навечно заблокировать процесс.
   - Временные директории в `WORK_DIR / video_path.stem` не очищаются автоматически и вызывают коллизии имен.

6. **Missing Whisper API 25MB Guard (`video-intel-worker`):**
   - Видео передается целиком в OpenAI. Лимит API составляет 25 MB. Любое видео более 3–5 минут падает с ошибкой 413/400.
   - *Решение:* Локальное предварительное сжатие/извлечение аудиодорожки (MP3/AAC с низким битрейтом до отправки в OpenAI) прямо в worker'е.

---

## 4. Ожидаемые входящие задачи от Агента 00 (Roadmap Sync)

1. **RFC: Архитектурное позиционирование двух воркеров:**
   - Формализация взаимодействия: кто обрабатывает видео по умолчанию?
   - Выбор сценария:
     - *Сценарий 1:* `video-intel-worker` остается быстрым SaaS/Cloud воркером (OpenAI API), а `video-ai-worker` подключается как Heavy/Local/Self-hosted альтернатива.
     - *Сценарий 2:* Слияние в модульный воркер с плагинами транскрипции/визуального анализа.
2. **RabbitMQ Contracts Alignment:**
   - Фиксация DTO `analysis.requested` и `analysis.completed`.
   - Включение информации о таймлайне сцен (`scenes: list[Scene]`), метаданных видео и сегментах транскрипции в контракт с backend (`video-intel-service`).
3. **Hardening Worker Runtime (Milestone 1):**
   - Починка Heartbeat & Connection recovery в `video-intel-worker`.
   - Внедрение надежной обработки ошибок (Dead Lettering, Sanitized Failure Reasons).
   - Подключение `ffmpeg` для стриппинга аудио перед отправкой в OpenAI Whisper API (обход лимита 25MB).
4. **Daemonization `video-ai-worker` (Milestone 2):**
   - Интеграция `video-ai-worker` с RabbitMQ и MinIO.
   - Lifecycle моделей (warm-up, singleton).
   - Scene change detection вместо жесткого 2-секундного слайсинга.

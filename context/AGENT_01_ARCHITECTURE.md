# AGENT_01_ARCHITECTURE.md — System Architecture & Distributed Systems

**Роль:** 01. System Architecture & Distributed Systems (Principal Software Architect & Senior Distributed Systems Engineer)  
**Проект:** Video Intel (Production-grade AI Video Intelligence SaaS)  
**Статус:** Active & Ready  
**Последнее обновление:** 2026-10-30  

---

## 1. Область ответственности (Scope)

Агент 01 отвечает за стратегический и тактический архитектурный надзор системы Video Intel:
1. **Общая архитектура системы и границы контекстов:**
   - Декомпозиция системы на Bounded Contexts в рамках Modular Monolith (`video-intel-service`).
   - Соблюдение принципов DDD и Hexagonal Architecture (Ports and Adapters).
   - Разделение ответственности компонентов (`video-intel-service`, `video-intel-worker`, `video-ai-worker`, `video-intel-frontend`).
2. **Межсервисное взаимодействие и Event-Driven Architecture:**
   - Топология RabbitMQ (Exchanges, Queues, Bindings, Routing Keys, DLX/DLQ).
   - Спецификация и версионирование контрактов сообщений (Message Schemas & Contracts).
   - Реализация Transactional Outbox Pattern и надежного релея сообщений.
3. **Data Ownership, транзакционные границы и консистентность:**
   - Изоляция данных по схемам PostgreSQL (`users`, `videos`, `analysis`, `chat`).
   - Межконтекстное взаимодействие исключительно через интерфейсы Application Ports (`VideoLookupPort`, `TranscriptLookupPort`).
   - Объектное хранение (MinIO/S3): разграничение ответственности метаданных и бинарных артефактов.
4. **Надежность, отказоустойчивость и параллелизм (Distributed Systems Hardening):**
   - Гарантии доставки (At-least-once, Publisher Confirms, Idempotency / Deduplication).
   - Управление транзакциями, пограничные состояния стейт-машин, rollback-изоляция.
   - Предотвращение зависания long-running consumers в RabbitMQ (heartbeats, prefetch, async task isolation).
5. **Architecture Decision Records (ADR):**
   - Фиксация ключевых компромиссов (trade-offs), контекста и последствий каждого архитектурного решения.

---

## 2. Текущее состояние кодовой базы (Baseline Audit)

Система Video Intel на данный момент состоит из 4 компонентов:

### 2.1. `video-intel-service` (Core Backend)
* **Стек:** Java 21, Spring Boot 3.x, Spring Data JPA, Liquibase, PostgreSQL, Spring AMQP, MinIO SDK.
* **Архитектура:** Модульный монолит с Hexagonal Architecture (пакеты разделены на `api`, `application`, `domain`, `infrastructure`).
* **Модули (Bounded Contexts):**
  1. `user`: Регистрация, аутентификация (JWT), анонимные гостевые сессии (`users`, `anonymous_sessions`).
  2. `video`: Управление жизненным циклом видео (`AWAITING_UPLOAD` -> `UPLOADING` -> `UPLOADED` / `FAILED`), генерация presigned S3 URLs, периодический реконсилер подвисших загрузок (`StaleUploadReconciler`).
  3. `analysis`: Управление джобами анализа (`QUEUED` -> `RUNNING` -> `COMPLETED` / `FAILED`), Transactional Outbox (`outbox_messages`), консьюминг результатов из RabbitMQ, сохранение транскриптов.
  4. `chat`: Сессии чата по видео (`chat_sessions`, `chat_messages`), заглушка LLM (`FakeChatCompletionAdapter`).
* **Инфраструктура очередей:**
  - Создаются очереди `analysis.requested` и `analysis.completed`.
  - Используется default exchange с прямым роутингом по имени очереди.
  - Сообщения сериализуются в JSON через Jackson 2.

### 2.2. `video-intel-worker` (Simple Transcription Worker)
* **Стек:** Python 3.11+, Pika (BlockingConnection), OpenAI API (`whisper-1`), MinIO SDK.
* **Поведение:**
  - Слушает `analysis.requested` (prefetch=1).
  - Скачивает видео из MinIO во временную папку.
  - Отправляет файл в OpenAI Audio API, получает текст.
  - Публикует `analysis.completed` с `{jobId, success, transcriptText, failureReason}` и делает `basic_ack`.
* **Ограничение:** Не выполняет глубокий видеоанализ, зависит от внешнего платного API, жесткий лимит файла 25MB в OpenAI Whisper.

### 2.3. `video-ai-worker` (Deep Multimodal Intelligence Pipeline)
* **Стек:** Python, FFmpeg, faster-whisper (CTranslate2), BLIP Vision Model (HuggingFace Transformers), PyAV, Torch.
* **Поведение:**
  - На данный момент **автономный CLI-скрипт** (`python -m app.main <path-to-video>`), не интегрирован с RabbitMQ и MinIO.
  - Извлекает технические метаданные (разрешение, FPS, кодек, длительность).
  - Извлекает аудио и формирует транскрипт с посегментными таймкодами (`TranscriptSegment: start, end, text`).
  - Нарезает ключевые кадры с интервалом (по умолчанию 2 сек) и прогоняет через BLIP Image Captioning для генерации сцен (`Scene: timestamp, description`).
  - Комбинирует всё в единый `AnalysisResult` со сводным summary.
* **Статус:** Высокая ценность для продукта, но полностью изолирован от бэкенда и брокера сообщений.

### 2.4. `video-intel-frontend`
* **Стек:** React 19, TypeScript, Vite, Tailwind CSS, Radix UI.
* **Поведение:**
  - Direct upload в MinIO через presigned PUT URL с последующим подтверждением `completeUpload`.
  - Запрос анализа видео и Polling статуса `AnalysisJob` (каждые N секунд).
  - Чат с AI по содержанию видео.

---

## 3. Ключевые проблемы и технический долг (Architectural Hotspots)

### Hotspot 1 (Critical): Бесконечный цикл redelivery при дедупликации в `AnalysisResultConsumer`
- **Файл:** `video-intel-service/.../analysis/infrastructure/AnalysisResultConsumer.java`
- **Проблема:** Метод `@RabbitListener` помечен `@Transactional`. Внутри вызывается `analysisService.markRunning()`. Если джоба уже не в `QUEUED` (дубликат сообщения), выбрасывается `IllegalAnalysisStateTransitionException`. Хотя в коде есть блок `catch`, Spring `@Transactional` перехватывает RuntimeException через `TransactionInterceptor` и помечает текущую транзакцию как **rollback-only**. При выходе из `handle()` коммит падает с `UnexpectedRollbackException`. Spring AMQP расценивает это как сбой слушателя и делает `requeue=true`, порождая вечный шторм сообщений.

### Hotspot 2 (Reliability): Отсутствие Publisher Confirms в Outbox и Worker
- **Проблема:** В `OutboxRelay` отправка через `rabbitTemplate.convertAndSend()` не дожидается ACK от брокера перед вызовом `outboxRepository.markProcessed()`. При падении брокера или сети в момент отправки сообщение считается обработанным, но в RabbitMQ не попадает (Silent Loss).
- Аналогично в Python `queue_client.py`: публикация в `analysis.completed` не использует confirms перед подтверждением входного сообщения.

### Hotspot 3 (Distributed Concurrency): Блокировка I/O в Pika BlockingConnection
- **Проблема:** Обработка видео (особенно в `video-ai-worker` с CPU/GPU инференсом) длится от десятков секунд до десятков минут. `pika.BlockingConnection` не прокачивает сетевой сокет во время синхронного вызова пайплайна. RabbitMQ разорвет соединение по таймауту heartbeat (обычно 60 сек), после чего сообщение вернется в очередь, а воркер уйдет в бесконечный цикл повторной обработки одного и того же тяжелого файла.

### Hotspot 4 (Domain & Contracts Mismatch): Разрыв модели данных анализа
- **Проблема:** Контракт `AnalysisResultMessage` и таблица `analysis.transcripts` рассчитаны только на плоский текст:
  `Transcript(video_id, content: text)`.
- `video-ai-worker` генерирует богатый структурированный результат:
  - Метаданные потока.
  - Сегменты речи с точными временными метками `[start, end]`.
  - Описания сцен по таймкодам `[timestamp, description]`.
  - Итоговое резюме (summary).
- Требуется проектирование схемы персистентности (JSONB vs реляционная модель) и claim-check паттерна при передаче больших payloads через RabbitMQ.

### Hotspot 5 (RabbitMQ Topology & Fault Tolerance):
- Отсутствие именованных Exchanges (Topic/Direct). Отправка через Default Exchange (`""`) создает жесткую связь producer -> queue.
- Отсутствие Dead Letter Queue (DLQ) для некорректных или перманентно падающих сообщений (Poison Pills).
- Отсутствие механизма распределения задач между воркерами (CPU-bound vs I/O-bound).

---

## 4. Ожидаемые входящие задачи от Агента 00 (Roadmap & Backlog)

| ID Задачи | Название | Приоритет | Категория | Роли исполнителей |
|---|---|---|---|---|
| **ARCH-01** | Устранение бага транзакций в `AnalysisResultConsumer` (Rollback-only bug) | P0 (Blocker) | Категория A | Агент 01 (Архитектура/Объяснение), User (Код) |
| **ADR-001** | Стратегия интеграции AI Worker (Конвейер vs Единый воркер, Payload vs Claim-Check) | P1 | Категория A | Агент 01 + Агент 03 |
| **ARCH-02** | Проектирование топологии RabbitMQ (Exchanges, DLX, DLQ, Routing Keys, Retry Backoff) | P1 | Категория A | Агент 01 + Агент 02 |
| **ARCH-03** | Эволюция схемы данных Analysis Context (Хранение сегментов, сцен, таймкодов) | P1 | Категория A | Агент 01 + Агент 02 |
| **ARCH-04** | Outbox Pattern Hardening (Publisher Confirms, Batching, `FOR UPDATE SKIP LOCKED`) | P2 | Категория A | Агент 01 + Агент 02 |
| **ARCH-05** | Асинхронный воркер и Heartbeat-safe архитектура (Pika / Celery / Threaded consumer) | P2 | Категория A | Агент 01 + Агент 03 |

---

## 5. Режим взаимодействия с пользователем

В соответствии с правилами разработки:
* **Категория A (Сложные задачи):** 
  1. Объясняем внутренние механизмы технологий (Spring TX proxy, Postgres MVCC/locks, AMQP framing, TCP socket buffers).
  2. Фиксируем constraints и trade-offs.
  3. Формулируем задачу пользователю с четкими требованиями.
  4. Проводим детальное Code Review написанной пользователем реализации.
* **Категория B (Бойлерплейт):** 
  - Реализуем автономно с лаконичным объяснением сути внесенных изменений.

# AGENT_02_BACKEND: Backend Engineering & Code Review

## 1. Область ответственности (Scope)
- **Компонент:** `video-intel-service` (Modular Monolith / Hexagonal Architecture / DDD).
- **Стек:** Java 25, Spring Boot 4.x, Spring Data JPA / Hibernate, PostgreSQL, Liquibase, MinIO, RabbitMQ (AMQP/Streams), Redis, Spring Security + JJWT.
- **Модули сервиса:**
  - `video`: жизненный цикл видео (регистрация, пресайнед ссылки MinIO, стейт-машина загрузки, reconciler зависших аплоадов).
  - `analysis`: запуск анализа, стейт-машина (`QUEUED` -> `RUNNING` -> `COMPLETED` / `FAILED`), Transactional Outbox, интеграция с воркерами через RabbitMQ, хранение транскриптов и сегментов таймкодов.
  - `chat`: сессии чата, история сообщений, контекст транскрипта, порты для LLM/AI.
  - `user`: аутентификация/регистрация, анонимные сессии, выпуск и верификация JWT, резолвинг владельцев (Owner Lookup).
  - `shared`: общая безопасность (SecurityFilter, JWT filter, CORS, OwnerExtractor), базовые доменные и HTTP исключения, модель владения (`Owner`, `OwnerType`).

---

## 2. Текущее состояние кодовой базы (Baseline Audit & Changelog)

### Реализовано в рамках синхронизации контракта транскрипции (2026-10-30):
1. **Liquibase миграция:** Добавлен changeset `005-add-segments-to-transcripts.yaml`, расширяющий `analysis.transcripts` полем `segments jsonb DEFAULT '[]' NOT NULL`.
2. **Контракт сообщений и доменная модель:**
   - `AnalysisResultMessage`: поддержан список `List<TranscriptSegmentDto> segments` (`start`, `end`, `text`).
   - Domain: `TranscriptSegment` и обновлённый `Transcript` с поддержкой сегментов.
   - JPA Persistence: `TranscriptEntity` с нативной сериализацией `@JdbcTypeCode(SqlTypes.JSON)`.
   - `AnalysisResultConsumer`: сохранение сегментов при получении результата из RabbitMQ.
3. **REST API & Авторизация владельца:**
   - Реализован эндпоинт `GET /api/v1/videos/{videoId}/transcript` (и алиас `GET /api/videos/{videoId}/transcript`).
   - DTO: `TranscriptResponse` (`videoId`, `fullText`, `segments: [{start, end, text}]`).
   - Безопасность: `OwnerExtractor` извлекает `Owner` из JWT (`SecurityContextHolder`) или заголовков/параметров `X-Owner-Id` + `X-Owner-Type`.
   - Проверка прав: `AnalysisService.getTranscript` сверяет владельца видео через `VideoLookupPort` и бросает `VideoAccessDeniedException` (HTTP 403 Forbidden) при несовпадении.

---

## 3. Выявленный технический долг и критические риски (Tech Debt & Risks)

### Категория A: Сложная инженерия (Архитектура, транзакции, конкурентность)

1. **Критическая ошибка транзакционности в `AnalysisResultConsumer` (Poison Pill / Infinite Requeue):**
   - `@RabbitListener` метод `handle(...)` помечен `@Transactional`.
   - Вызов `markRunning` кидает `IllegalAnalysisStateTransitionException` при повторной доставке сообщения. Spring помечает транзакцию как `rollback-only`. В блоке `catch` исключение подавляется, но при коммите летит `UnexpectedRollbackException`.
   - RabbitMQ не получает ack и зацикливает сообщение навсегда.

2. **Race Condition в `AnalysisService.requestAnalysis` (Категория A — В ПРОРАБОТКЕ):**
   - В `analysis.analysis_jobs` отсутствует `UNIQUE` constraint на `video_id`.
   - Паттерн `check-then-act` допускает параллельный запуск двух воркеров на одно видео.
   - Подготовлены варианты решения и trade-offs для реализации инженером.

3. **Масштабируемость и надёжность Transactional Outbox (`OutboxRelay`):**
   - Метод `relay()` запускается по таймеру `@Scheduled(fixedDelay = 5s)`.
   - Запрос `findByProcessedAtIsNull()` не содержит `LIMIT` и `FOR UPDATE SKIP LOCKED`. При горизонтальном масштабировании (2+ реплики сервиса) один и тот же outbox-батч дублируется в RabbitMQ.
   - Отсутствуют **Publisher Confirms** на `RabbitTemplate`. Сообщение помечается как `processedAt = now()`, даже если брокер сбросил соединение или не сохранил сообщение на диск.

4. **Отсутствие Optimistic Locking (`@Version`):**
   - Ни в одной сущности (`VideoEntity`, `AnalysisJobEntity`, `ChatSessionEntity`) нет версионирования. Возможны lost updates при параллельных вызовах `getUploadUrl` / `completeUpload` / фонового `StaleUploadReconciler`.

5. **Внешние сетевые вызовы внутри БД-транзакций:**
   - В `VideoService.completeUpload` и `getUploadUrl` производятся вызовы MinIO (`videoStoragePort.exists`, presigned url generation) внутри транзакции `@Transactional`.

### Категория B: Инфраструктурные оптимизации и улучшения
1. Оптимизация индекса Outbox: замена полного индекса на partial index PostgreSQL `CREATE INDEX ... WHERE processed_at IS NULL`.
2. Каскадная консистентность: при удалении видео не очищаются связанные артефакты в MinIO, история чатов и транскрипты.
3. RabbitMQ Configuration: настройка Dead Letter Exchange (DLX), Dead Letter Queue (DLQ) и `RabbitListenerErrorHandler` для consumer.

---

## 4. Ожидаемые задачи от Агента 00 (Roadmap & Backlog)
1. **TASK-BE-01: Fix Consumer Transaction & Idempotency** (Категория A — совместная реализация с разбором транзакций и state machine).
2. **TASK-BE-02: Fix Race Conditions & Unique Constraints in Analysis** (Категория A — в процессе совместной реализации).
3. **TASK-BE-03: Production-Grade Outbox Relay** (`SKIP LOCKED`, пагинация, Publisher Confirms, handling poison pills).
4. **TASK-BE-04: Auth Retrofit & Principal Ownership Enforcement** (Защита API, проверка прав владельца через JWT SecurityContext для всех эндпоинтов).
5. **TASK-BE-05: Connection Pool & Storage Decoupling** (Вынос MinIO I/O из `@Transactional`).
6. **TASK-BE-06: Optimistic Locking & State Audit** (`@Version` на доменных агрегатах).

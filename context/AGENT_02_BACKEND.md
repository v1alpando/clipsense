# AGENT_02_BACKEND: Backend Engineering & Code Review

## 1. Область ответственности (Scope)
- **Компонент:** `video-intel-service` (Modular Monolith / Hexagonal Architecture / DDD).
- **Стек:** Java 25, Spring Boot 4.x, Spring Data JPA / Hibernate, PostgreSQL, Liquibase, MinIO, RabbitMQ (AMQP/Streams), Redis, Spring Security + JJWT.
- **Модули сервиса:**
  - `video`: жизненный цикл видео (регистрация, пресайнед ссылки MinIO, стейт-машина загрузки, reconciler зависших аплоадов).
  - `analysis`: запуск анализа, стейт-машина (`QUEUED` -> `RUNNING` -> `COMPLETED` / `FAILED`), Transactional Outbox, интеграция с воркерами через RabbitMQ, хранение транскриптов.
  - `chat`: сессии чата, история сообщений, контекст транскрипта, порты для LLM/AI.
  - `user`: аутентификация/регистрация, анонимные сессии, выпуск и верификация JWT, резолвинг владельцев (Owner Lookup).
  - `shared`: общая безопасность (SecurityFilter, JWT filter, CORS), базовые доменные и HTTP исключения, модель владения (`Owner`, `OwnerType`).

---

## 2. Текущее состояние кодовой базы (Baseline Audit)

### Архитектура и слои
- Архитектурный стиль: **Hexagonal / Clean Architecture (Ports & Adapters)** внутри модульного монолита.
- Модули разделены на слои `domain`, `application`, `infrastructure`, `api`.
- Доменные модели изолированы: фабричные методы (`request()`, `register()`, `create()`), закрытые конструкторы, метод `reconstruct()`, бизнес-валидация инвариантов.
- Взаимодействие между модулями реализовано через порты приложения (`VideoLookupPort`, `OwnerLookupPort`, `TranscriptLookupPort`), что предотвращает прямое сцепление JPA-репозиториев между модулями.

### Схема БД и миграции (Liquibase)
- Модульные чейнджлоги разделены по схемам: `user`, `video`, `analysis`, `chat`.
- Схемы изолированы в PostgreSQL.
- Созданы базовые таблицы и индексы, однако отсутствуют критические ограничения уникальности для предотвращения race condition (в частности, на `analysis.analysis_jobs.video_id`).

---

## 3. Выявленный технический долг и критические риски (Tech Debt & Risks)

### Категория A: Сложная инженерия (Архитектура, транзакции, конкурентность)

1. **Критическая ошибка транзакционности в `AnalysisResultConsumer` (Poison Pill / Infinite Requeue):**
   - `@RabbitListener` метод `handle(...)` помечен `@Transactional`.
   - Внутри вызывается `analysisService.markRunning(jobId)`, который кидает `IllegalAnalysisStateTransitionException` (наследник `RuntimeException`).
   - Хотя исключение перехватывается блоком `catch`, Spring Transaction Manager уже пометил внешнюю транзакцию как `rollback-only`. При выходе из `handle` выбрасывается `UnexpectedRollbackException`, транзакция откатывается, ack не отправляется, и сообщение бесконечно ретраится RabbitMQ.
   - Метод `markRunning` выполняется в той же транзакции, поэтому предположение о раздельном коммите неверно.

2. **Race Condition в `AnalysisService.requestAnalysis`:**
   - Таблица `analysis.analysis_jobs` имеет обычный индекс `idx_analysis_jobs_video_id`, но **не имеет UNIQUE constraint** на `video_id`.
   - Паттерн `check-then-act` (`findByVideoId` -> `save`) при двух параллельных HTTP-запросах создаст 2 записи в `analysis_jobs` и 2 записи в `outbox_messages`.

3. **Масштабируемость и надёжность Transactional Outbox (`OutboxRelay`):**
   - Метод `relay()` запускается по таймеру `@Scheduled(fixedDelay = 5s)`.
   - Запрос `findByProcessedAtIsNull()` не содержит `LIMIT` и `FOR UPDATE SKIP LOCKED`. При горизонтальном масштабировании (2+ реплики сервиса) один и тот же outbox-батч будет дублироваться в RabbitMQ.
   - Отсутствуют **Publisher Confirms** на `RabbitTemplate`. Сообщение помечается как `processedAt = now()`, даже если брокер сбросил соединение или не сохранил сообщение на диск.
   - При неизвестном `messageType` выбрасывается исключение, вызывающее бесконечный повтор каждые 5 секунд без DLQ / Dead Letter механизма для outbox.

4. **Отсутствие Optimistic Locking (`@Version`):**
   - Ни в одной сущности (`VideoEntity`, `AnalysisJobEntity`, `ChatSessionEntity`) нет версионирования. Возможны lost updates при параллельных вызовах `getUploadUrl` / `completeUpload` / фонового `StaleUploadReconciler`.

5. **Внешние сетевые вызовы внутри БД-транзакций:**
   - В `VideoService.completeUpload` и `getUploadUrl` производятся вызовы MinIO (`videoStoragePort.exists`, presigned url generation) внутри транзакции `@Transactional`. Это неоправданно удерживает соединения из пула HikariCP.

6. **Безопасность и авторизация (IDOR):**
   - `SecurityConfig` содержит `permitAll()` для всех эндпоинтов видео, анализа и чата (отмечено TODO auth retrofit). Клиент может передать чужой `ownerId` или оперировать чужими UUID без проверки контекста аутентификации.

### Категория B: Инфраструктурные оптимизации и улучшения
1. Оптимизация индекса Outbox: замена полного индекса на partial index PostgreSQL `CREATE INDEX ... WHERE processed_at IS NULL`.
2. Каскадная консистентность: при удалении видео не очищаются связанные артефакты в MinIO, история чатов и транскрипты.
3. RabbitMQ Configuration: настройка Dead Letter Exchange (DLX), Dead Letter Queue (DLQ) и `RabbitListenerErrorHandler` для consumer.

---

## 4. Ожидаемые задачи от Агента 00 (Roadmap & Backlog)
1. **TASK-BE-01: Fix Consumer Transaction & Idempotency** (Категория A — совместная реализация с разбором транзакций и state machine).
2. **TASK-BE-02: Fix Race Conditions & Unique Constraints** (Liquibase changeset для `analysis_jobs.video_id` + обработка `DataIntegrityViolationException` / locks).
3. **TASK-BE-03: Production-Grade Outbox Relay** (`SKIP LOCKED`, пагинация, Publisher Confirms, handling poison pills).
4. **TASK-BE-04: Auth Retrofit & Principal Ownership Enforcement** (Защита API, проверка прав владельца через JWT SecurityContext, отказ от доверия клиентскому `ownerId`).
5. **TASK-BE-05: Connection Pool & Storage Decoupling** (Вынос MinIO I/O из `@Transactional`).
6. **TASK-BE-06: Optimistic Locking & State Audit** (`@Version` на доменных агрегатах).

---

## 5. Формат взаимодействия с инженером (Human-in-the-Loop)
- Для задач **Категории A**:
  1. Формулирую проблему на уровне протоколов, потоков данных, транзакций БД и блокировок.
  2. Предлагаю архитектурные варианты (trade-offs).
  3. Передаю реализацию инженеру для прокачки навыков.
  4. Провожу детальный Code Review с указанием edge-cases и production implications.
- Для задач **Категории B**:
  1. Генерирую проверенный boilerplate, конфигурации, DTO, миграции Liquibase с кратким объяснением.

# AGENT_02_BACKEND: Backend Engineering & Code Review

## 1. Область ответственности (Scope)
- **Компонент:** `video-intel-service` (Modular Monolith / Hexagonal Architecture / DDD).
- **Стек:** Java 25, Spring Boot 4.x, Spring Data JPA / Hibernate, PostgreSQL, Liquibase, MinIO, RabbitMQ (AMQP/Streams), Redis, Spring Security + JJWT.
- **Модули сервиса:**
  - `video`: жизненный цикл видео (регистрация, пресайнед ссылки MinIO, стейт-машина загрузки, reconciler зависших аплоадов).
  - `analysis`: запуск анализа, стейт-машина (`QUEUED` -> `RUNNING` -> `COMPLETED` / `FAILED`), Transactional Outbox, интеграция с воркерами через RabbitMQ, хранение транскриптов, сегментов таймкодов и глубокого AI-анализа (`summary`, `hook`, `key_moments`, `takeaways`).
  - `chat`: сессии чата, история сообщений, контекст транскрипта, порты для LLM/AI.
  - `user`: аутентификация/регистрация, анонимные сессии, выпуск и верификация JWT, резолвинг владельцев (Owner Lookup).
  - `shared`: общая безопасность (SecurityFilter, JWT filter, CORS, OwnerExtractor), базовые доменные и HTTP исключения, модель владения (`Owner`, `OwnerType`).

---

## 2. Текущее состояние кодовой базы (Baseline Audit & Changelog)

### Реализовано в рамках сохранения и отдачи глубокого AI-анализа видео (2026-11-01):
1. **Liquibase миграция:**
   - Добавлен changeset `006-add-analysis-data-to-transcripts.yaml`, расширяющий `analysis.transcripts` полем `analysis_data jsonb`.
2. **Контракт сообщений и доменная модель:**
   - `AnalysisResultMessage`: поддержан типизированный `AnalysisPayloadDto` с вложенными `HookDto`, `KeyMomentDto` (поддержка `@JsonProperty("key_moments")` и `@JsonAlias("keyMoments")`) и `takeaways`.
   - Domain Value Objects: `Hook` (`score`, `type`, `critique`, `suggestion`), `KeyMoment` (`timestamp`, `title`, `description`), `VideoAnalysisData` (`summary`, `hook`, `keyMoments`, `takeaways`).
   - `Transcript`: расширен полем `VideoAnalysisData analysisData` (с сохранением 100% обратной совместимости через конструкторы и перегрузки `create`/`reconstruct`).
   - JPA Persistence: `TranscriptEntity` с нативной сериализацией `@JdbcTypeCode(SqlTypes.JSON)`.
   - `AnalysisResultConsumer`: маппинг и атомарное сохранение `VideoAnalysisData` в транскрипт при успехе джобы.
3. **REST API & Авторизация владельца:**
   - `TranscriptResponse`: расширен объектом `analysis` (`summary`, `hook`, `keyMoments`, `takeaways`) и геттером `text()` для зеркалирования `fullText` (совместимость с фронтендом).
   - Эндпоинты:
     * `GET /api/v1/videos/{videoId}/transcript` (и алиас `/api/videos/{videoId}/transcript`) возвращает полный транскрипт вместе со структурированным анализом.
     * `GET /api/v1/videos/{videoId}/analysis-data` (и алиас `/api/videos/{videoId}/analysis-data`) возвращает только объект анализа `AnalysisResponse`.
   - Безопасность: оба эндпоинта защищены проверкой `Owner check` через `OwnerExtractor` (JWT Bearer или `X-Owner-Id` + `X-Owner-Type`). При несовпадении владельца возвращается HTTP 403 Forbidden.
4. **Тестирование:**
   - Модульные тесты `TranscriptTest` и `AnalysisServiceTest` расширены проверками валидации, маппинга и авторизации данных анализа (29 тестов зелёные).

### Ранее реализовано (2026-10-30):
1. **Liquibase миграция:** Changeset `005-add-segments-to-transcripts.yaml` (`segments jsonb DEFAULT '[]' NOT NULL`).
2. **Стейт сегментов:** `TranscriptSegment`, маппинг в `TranscriptEntity` и `AnalysisResultConsumer`.

---

## 3. Выявленный технический долг и критические риски (Tech Debt & Risks)

### Категория A: Сложная инженерия (Архитектура, транзакции, конкурентность)

1. **Критическая ошибка транзакционности в `AnalysisResultConsumer` (Poison Pill / Infinite Requeue):**
   - `@RabbitListener` метод `handle(...)` помечен `@Transactional`.
   - Вызов `markRunning` кидает `IllegalAnalysisStateTransitionException` при повторной доставке сообщения. Spring помечает транзакцию как `rollback-only`. В блоке `catch` исключение подавляется, но при коммите летит `UnexpectedRollbackException`.
   - RabbitMQ не получает ack и зацикливает сообщение навсегда.

2. **Race Condition в `AnalysisService.requestAnalysis`:**
   - В `analysis.analysis_jobs` отсутствует `UNIQUE` constraint на `video_id`.
   - Паттерн `check-then-act` допускает параллельный запуск двух воркеров на одно видео.
   - Рекомендуемое решение: Partial Unique Index в PostgreSQL (`WHERE status IN ('QUEUED', 'RUNNING')`).

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
1. **TASK-BE-01: Fix Consumer Transaction & Idempotency** (Категория A).
2. **TASK-BE-02: Fix Race Conditions & Unique Constraints in Analysis** (Категория A).
3. **TASK-BE-03: Production-Grade Outbox Relay** (`SKIP LOCKED`, пагинация, Publisher Confirms, handling poison pills).
4. **TASK-BE-04: Auth Retrofit & Principal Ownership Enforcement** (Защита API, проверка прав владельца через JWT SecurityContext для всех эндпоинтов).
5. **TASK-BE-05: Connection Pool & Storage Decoupling** (Вынос MinIO I/O из `@Transactional`).
6. **TASK-BE-06: Optimistic Locking & State Audit** (`@Version` на доменных агрегатах).

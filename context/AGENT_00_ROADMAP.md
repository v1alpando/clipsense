# AGENT_00_ROADMAP: Master Roadmap & Orchestration

**Роль:** Project Manager & Lead Software Architect  
**Рабочий файл:** `context/AGENT_00_ROADMAP.md`  
**Статус:** SPRINT 0 PLANNING ACTIVE  

---

## 1. Стратегический план развития (Engineering & Product Roadmap)

```
[ Sprint 0: Foundation & Base Parity ]  <--- МЫ ЗДЕСЬ
   - Git init & .gitignore
   - Полноценный dev docker-compose (Postgres + RabbitMQ + MinIO)
   - ADR-001: Выбор воркера (Cloud API vs Local models) & Контракт транскрипта с таймкодами
   - Добавление Testcontainers в Java и Python

[ Sprint 1: Resilient Media Pipeline (Vertical Slice) ]
   - Извлечение аудио через ffmpeg (16kHz mono) в воркере
   - Whisper API с разбивкой на сегменты (start, end, text)
   - Контракт сообщений RabbitMQ с DLQ и idempotency
   - Сохранение структурированного транскрипта в PostgreSQL
   - Frontend: интерактивный плеер с привязкой к таймкодам

[ Sprint 2: AI Intelligence & Real Chat ]
   - Замена FakeChatCompletionAdapter на реальный LLM Provider
   - Контекстное окно: промпт-инженерия с инъекцией сегментов видео и таймкодов
   - SSE / Streaming ответов на фронтенд
   - Защита от злоупотреблений / квоты

[ Sprint 3: Production Readiness & Observability ]
   - Multi-stage Dockerfiles для всех компонентов
   - CI/CD пайплайн (GitHub Actions)
   - Настройка HTTPS, Ingress (Nginx/Caddy)
   - Structured logging & Health checks

[ Sprint 4: Productization, Billing & Launch ]
   - Модуль Billing (Stripe / подписки / квоты)
   - Подключение аналитики (PostHog/Plausible)
   - Публичный релиз (Product Hunt / SEO)
```

---

## 2. Текущий статус задач по агентам (Sprint 0: Foundation & Media Pipeline Slice)

| Агент | Назначенная задача | Категория | Статус |
|---|---|---|---|
| **03. AI Pipeline** | Аудио-экстрактор ffmpeg (16kHz mono MP3 64k), Whisper `verbose_json` с сегментами | A / Code | ✅ ВЫПОЛНЕНО |
| **02. Backend** | Liquibase миграция `segments jsonb`, `TranscriptSegment`, эндпоинт `GET /api/v1/videos/{id}/transcript` | A / Code | ✅ ВЫПОЛНЕНО |
| **04. Frontend** | Интерактивный `TranscriptViewer` с таймкодами, поиском, автоскроллом и перемоткой плеера | B / Code | ✅ ВЫПОЛНЕНО |
| **00. Roadmap & Arch** | Синхронизация контрактов Бэк/Фронт (поля `text`/`fullText`, выравнивание доступа) | Arch Review | ✅ ВЫПОЛНЕНО |
| **05. DevOps** | Подготовить полноценный dev docker-compose (Postgres + RabbitMQ + MinIO) и Dockerfiles | B | ⏳ В ПЛАНЕ |
| **06. Testing** | Подключить Testcontainers для интеграционных тестов бэкенда | B | ⏳ В ПЛАНЕ |
| **07. Product** | Заполнить `AGENT_07_PRODUCT.md` (ICP, Unit-экономика) | Product | ⏳ ОЖИДАЕТСЯ |

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

## 2. Текущий статус задач по агентам (Sprint 0: Foundation)

| Агент | Назначенная задача | Категория | Статус |
|---|---|---|---|
| **05. DevOps** | Инициализировать Git, составить `.gitignore`, доработать `docker-compose.yml` (добавить Postgres 16+, убрать `external: true` для volumes) | B | ⏳ НАЗНАЧЕНО |
| **01. Architecture** | Сформировать ADR-001: "Worker Topology & Structured Transcript Schema (JSONB vs Table)" | A / Arch | ⏳ НАЗНАЧЕНО |
| **03. AI Pipeline** | Спроектировать модуль аудио-препроцессинга (ffmpeg extraction + audio chunking) | A / Code | ⏳ В ОЖИДАНИИ ADR-001 |
| **02. Backend** | Добавить констрейнты уникальности на повторные джобы анализа и подготовить модель для сегментов транскрипта | A / Code | ⏳ В ОЖИДАНИИ ADR-001 |
| **06. Testing** | Подключить Testcontainers (Postgres, RabbitMQ) в `video-intel-service/pom.xml` | B | ⏳ НАЗНАЧЕНО |
| **04. Frontend** | Аудит состояния `features/analysis` и подготовка типов под timestamps | B | ⏳ В ОЖИДАНИИ ADR-001 |
| **07. Product** | Заполнить `AGENT_07_PRODUCT.md` (ICP, Unit-экономика обработки минуты видео) | Product | ⏳ ОЖИДАЕТСЯ ВВОД |

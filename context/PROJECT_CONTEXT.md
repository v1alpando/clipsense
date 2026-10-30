# PROJECT_CONTEXT: Video Intel (AI Video Intelligence SaaS)

Единый источник правды и контекста для всех специализированных агентов команды Video Intel.

---

## 1. Миссия и Видение Продукта
**Video Intel** — это коммерческий B2B/Prosumer SaaS в области AI Video Intelligence.  
Платформа не является тривиальным "AI wrapper" (простой обёрткой над LLM с отправкой сырого промпта). Мы строим глубокую инженерную систему анализа видеоконтента:
* Мультимодальный анализ: транскрипция с точными таймкодами, сегментация речи, извлечение ключевых тем, анализ визуальных сцен.
* Интерактивная работа: умный плеер с привязкой транскрипта, поиск по моментам, диалоговый AI-чат с цитированием таймкодов видео.
* Долгосрочно: выявление контентных хуков, сравнение с конкурентами, анализ структуры рилсов/шортсов и ютуб-видео.

---

## 2. Ключевые Инженерные Принципы
1. **Баланс Бизнеса и Инженерии:** Не строим избыточную сложность ради сложности. Каждое решение должно быть оправдано нагрузкой и unit-экономикой.
2. **Modular Monolith First:** Сервис `video-intel-service` строится как модульный монолит с четкими границами Bounded Contexts (DDD / Hexagonal Architecture). Никакого преждевременного распила на микросервисы.
3. **AI через надежные внешние API:** На этапе запуска не обучаем и не хостим тяжелые локальные ML-модели на GPU (минимизируем CAPEX/OPEX). Воркеры фокусируются на препроцессинге медиа (ffmpeg) и надежном взаимодействии с API провайдеров.
4. **Обучение и Категоризация Задач:**
   - **Категория A (Сложная логика):** Транзакции, стейт-машины, конкурентность, идемпотентность, очереди, retries, distributed coordination. Пользователь реализует сам после разбора проблемы, вариантов и trade-offs. Агенты проводят строгое Code Review.
   - **Категория B (Рутина):** DTO, мапперы, конфиги, типовой CRUD, тестовые фикстуры, документация. Агенты автоматизируют с кратким объяснением.
5. **Zero Data Loss & Consistency:** 
   - PostgreSQL — единственный источник правды для транзакционных данных.
   - Transactional Outbox Pattern для публикации сообщений в RabbitMQ.
   - S3/MinIO — хранение больших бинарных файлов (видео, извлеченное аудио).

---

## 3. Топология Системы

```
                                  [ Frontend (React 19 / Vite) ]
                                                │
                                    REST API / JWT / Presigned URLs
                                                ▼
┌───────────────────────────────── video-intel-service (Java 25 / Spring Boot) ──────────────────────────────────┐
│                                                                                                                │
│   [ user (identity) ]      [ video ]              [ analysis ]                         [ chat ]                │
│   - Users & Auth           - Lifecycle            - Analysis Jobs                      - Sessions & Messages   │
│   - Anonymous Sessions     - S3 Presigned URLs    - Transactional Outbox               - LLM Context Port      │
│   - Owner Lookup Port      - Stale Reconciler     - Transcript Storage & Timestamps    - Video Reference       │
│                                                          │                                                     │
└──────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────┘
                             ▲                             │
             Events / Status │                             │ Events / Job dispatch
                             │                             ▼
                 [ RabbitMQ: analysis.results ]   [ RabbitMQ: analysis.requested ]
                             ▲                             │
                             │                             ▼
               ┌──────────────────────────────────────────────────────────────┐
               │              video-intel-worker (Python 3.14)                │
               │   - ffmpeg audio extraction (16kHz mono)                     │
               │   - Chunking for >25MB files                                 │
               │   - Whisper API (structured segments + timestamps)           │
               │   - S3 integration (MinIO)                                   │
               └──────────────────────────────────────────────────────────────┘
```

---

## 4. Карта Команды Агентов

| Агент | Специализация | Рабочий файл синхронизации |
|---|---|---|
| **00** | Project Management & Engineering Roadmap | `context/AGENT_00_ROADMAP.md` |
| **01** | Architecture & System Design | `context/AGENT_01_ARCHITECTURE.md` |
| **02** | Backend Engineering (Java/Spring) | `context/AGENT_02_BACKEND.md` |
| **03** | Python Workers & AI Pipeline | `context/AGENT_03_AI_PIPELINE.md` |
| **04** | Frontend Engineering (React/Vite) | `context/AGENT_04_FRONTEND.md` |
| **05** | DevOps & Infrastructure | `context/AGENT_05_DEVOPS.md` |
| **06** | Testing & Quality Engineering | `context/AGENT_06_TESTING.md` |
| **07** | Product, Business & Growth | `context/AGENT_07_PRODUCT.md` |

---

## 5. Текущий статус Baseline Audit (на 2026-10-30)
- **Архитектура:** Хорошая основа DDD/Hexagonal в Java. Проблема: воркер возвращает плоский текст без таймкодов, нет DLQ в RabbitMQ.
- **Backend:** Liquibase изолирован по схемам. Отсутствуют констрейнты уникальности на запуск повторного анализа, фейковый адаптер чата.
- **AI Pipeline:** Лимит 25MB у Whisper API не обойдён (отправляется сырое видео), риск потери heartbeat в RabbitMQ.
- **Frontend:** Отличный feature-based каркас, но нет поддержки сегментов транскрипта и стриминга чата.
- **DevOps:** Критический дефицит: нет Dockerfiles, нет Postgres в docker-compose, нет Git.
- **Testing:** Уровень зрелости 0/1. Только 3 простых теста домена. Нет Testcontainers, нет тестов очередей и стейт-машины.

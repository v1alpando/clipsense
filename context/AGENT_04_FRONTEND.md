# AGENT_04_FRONTEND: Frontend Architecture & Engineering

**Роль:** Senior Frontend Engineer & Frontend Architect (04. Frontend Engineering)  
**Компонент:** `video-intel-frontend`  
**Стек:** React 19, TypeScript, Vite, Tailwind CSS v4, shadcn/ui, Radix UI, Lucide React, React Router 7  
**Архитектура:** Feature-Based Architecture (Screaming Architecture / Feature Folders)  
**Дата ревизии:** 2026-10-30  
**Статус:** READY FOR SPRINT PLANNING

---

## 1. Scope (Зона ответственности)

- **Frontend Architecture & Modular Structure:** Проектирование границ модулей (`features/*`), изоляция предметных областей, построение масштабируемой композиции компонентов, обеспечение чистоты зависимостей.
- **Server State & Client State Management:** Стратегия кэширования, дедупликация запросов, синхронизация с бэкендом, оптимистичные обновления, управление жизненным циклом поллинга/стриминга.
- **API Contracts & Type Safety:** Синхронизация TypeScript-контрактов с DTO Spring Boot сервиса (`video-intel-service`), строгая валидация схем ответов/запросов, обработка доменных ошибок бэкенда.
- **Interactive Multimedia & AI UX:** 
  - Видеоплеер с двунаправленной синхронизацией по таймкодам.
  - Transcript Viewer (интерактивный просмотр транскрипции, поиск, переход к таймкоду, автоскролл за таймлайном воспроизведения).
  - Streaming AI Chat Interface (поддержка SSE/Streamed responses, цитирование фрагментов видео с кликабельными таймкодами).
  - Resilient Video Upload (chunked/direct-to-storage upload, отслеживание прогресса, пауза/возобновление).
- **Authentication & Anonymous Session Handover:** Механика гостевого режима (Anonymous Session) с бесшовной миграцией видео и сессий чата в аккаунт пользователя при регистрации/логине (JWT, refresh-flow).
- **Quality & Non-Functional Requirements:** Responsive SaaS UI/UX (desktop-first workspace, адаптивность под планшеты/мобильные), Accessibility (WAI-ARIA, клавиатурная навигация), Performance (Core Web Vitals, tree-shaking, lazy loading).

---

## 2. Baseline Audit (Текущее состояние кодовой базы)

### 2.1. Структура проекта
Репозиторий `video-intel-frontend` организован по фиче-ориентированной структуре:
```
src/
├── app/                  # Инициализация приложения, роутер, глобальный Layout
├── components/ui/        # Базовые UI-примитивы (shadcn/ui на базе Radix UI)
├── features/
│   ├── user/             # Управление сессиями и идентификацией владельца (Owner)
│   ├── video/            # Загрузка, получение метаданных, воспроизведение видео
│   ├── analysis/         # Запуск задач анализа и опрос их статусов
│   └── chat/             # Сессии диалога с AI по содержанию видео
├── lib/                  # Утилиты (apiClient, env, форматирование)
└── pages/
    ├── HomePage/         # Стартовая точка с интерфейсом загрузки видео
    └── VideoPage/        # Страница отдельного видео (плеер + статус + анализ + чат)
```

### 2.2. Реализованный функционал
1. **Anonymous Session:** Хранение ID анонимной сессии в `localStorage` с проверкой времени истечения (`features/user/anonymousOwner.ts`).
2. **Direct Upload Flow:** Создание сущности видео в сервисе -> получение presigned URL (MinIO/S3) -> загрузка бинарных данных через XHR с прогресс-баром -> нотификация завершения (`POST /complete`).
3. **Polling & Video Playback:** Опрос статуса обработки видео через `useVideo`, получение временного presigned playback URL через `usePlaybackUrl`, базовый HTML5 плеер.
4. **Analysis Triggering:** Запуск задачи анализа (FULL, TRANSCRIPTION, VISUAL) и поллинг состояния задачи (`PENDING` -> `PROCESSING` -> `COMPLETED`/`FAILED`).
5. **Chat Interface:** Создание сессии чата, загрузка истории сообщений, отправка сообщения с оптимистичным отображением в UI.

---

## 3. Ключевые проблемы и технический долг

### 🔴 Категория A (Сложная архитектура, стейт и контракты)

1. **Primitive Server State Management (Ручной поллинг и отсутствие кэширования):**
   - В `useVideo`, `useAnalysis` и `useChat` используется связка `useState + useEffect + setInterval/setTimeout`.
   - Проблемы: Race conditions при смене параметров, утечки памяти при быстром размонтировании, дублирование сетевых запросов при открытии нескольких компонентов, отсутствие глобального кэша и дедупликации (необходим переход на TanStack Query v5).
2. **Отсутствие интерактивного Transcript Viewer и связи «Плеер ↔ Таймкоды»:**
   - Данные транскрипта не отображаются как таймлайн. Нет контракта передачи сегментов речи (start, end, text).
   - Нет единого состояния текущего времени воспроизведения (`currentTime`), из-за чего нельзя подсвечивать текущую реплику спикера или перематывать плеер по клику на сообщение чата или сегмент транскрипта.
3. **Отсутствие авторизации и миграции сессий (Session Handover):**
   - Бэкенд уже содержит эндпоинты `/api/v1/auth/*` (`register`, `login`, `me`), однако фронтенд работает исключительно в режиме анонимной сессии.
   - Отсутствует интерцептор токенов (Bearer JWT), механизм хранения refresh-токенов/cookies и связывание анонимного `ownerId` с создаваемым пользователем.
4. **Отсутствие Streaming UI для AI Chat:**
   - Текущий чат ожидает полного завершения генерации ответа от бэкенда (`POST /messages` возвращает финальный `ChatMessageResponse`). Для production-grade SaaS необходим streaming (SSE / ReadableStream), иначе TTFT (Time To First Token) создает плохой UX на длинных видео.
5. **Монолитный вертикальный Layout на `VideoPage`:**
   - Компоненты сложены в одну вертикальную колонку. Для рабочего инструмента анализа видео требуется современный SaaS-лейаут (Split Workspace): слева — плеер и ключевые таймкоды, справа — вкладки (Transcript, AI Insights, Chat, Summaries).

### 🟡 Категория B (Инженерная гигиена и UI-рутина)

1. **`apiClient.ts`:**
   - Отсутствует поддержка `AbortSignal` для отмены устаревших запросов при переключении страниц.
   - Нет строгой типизации ошибок валидации (RFC 7807 Problem Details).
2. **Отсутствие страницы каталога / истории видео (Dashboard):**
   - После ухода со страницы `VideoPage` пользователь теряет ссылку на видео (если не сохранил URL), так как нет интерфейса списка видео пользователя/сессии.
3. **Обработка сетевых сбоев и Offline State:**
   - При кратковременном обрыве связи поллеры падают в терминальное состояние ошибки без экспоненциального retry (Backoff).

---

## 4. Ожидаемые входящие задачи от Агента 00 (Project Management & Roadmap)

1. **TASK-FE-001 (Server State Infrastructure):** Внедрение и стандартизация TanStack Query v5: настройка QueryClient, фабрика query-ключей, типизированные мутации и оптимистичные апдейты.
2. **TASK-FE-002 (Auth & Session Flow):** Интеграция аутентификации (Sign In / Sign Up Dialogs), внедрение Auth Context, интеграция Bearer-токена в `apiClient`, сценарий миграции анонимной сессии.
3. **TASK-FE-003 (SaaS Workspace Redesign):** Редизайн `VideoPage` в полноценную студию анализа (Split Panel Layout, Resizable Panels на базе `react-resizable-panels`).
4. **TASK-FE-004 (Video Player Synchronization Engine):** Реализация единой шины событий / контекста видеоплеера (`VideoPlayerContext`: `currentTime`, `duration`, `seekTo`, `playbackRate`).
5. **TASK-FE-005 (Interactive Transcript Viewer):** Реализация компонента просмотра расшифровки с виртуализацией списка сегментов, поиском и синхронным скроллом.
6. **TASK-FE-006 (Streaming Chat & Citations):** Обновление интерфейса чата для поддержки потоковых ответов AI с кликабельными таймкодами вида `[02:15]`.
7. **TASK-FE-007 (Video Library / Dashboard):** Разработка списка загруженных видео с карточками статусов, превью и возможностью фильтрации.

---

## 5. Формат взаимодействия с пользователем

- **Задачи Категории A (Архитектурные):**
  - Предоставление вариантов решения с анализом trade-offs (память, сложность, UX).
  - Формулирование технического ТЗ для самостоятельной реализации пользователем.
  - Проведение строгого Code Review (типобезопасность, граничные случаи, утечки рендеров).
- **Задачи Категории B (Шаблонные/Утилитарные):**
  - Автоматическая кодогенерация с детальным пояснением архитектурного соответствия.

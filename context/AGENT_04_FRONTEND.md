# AGENT_04_FRONTEND: Frontend Architecture & Engineering

**Роль:** Senior Frontend Engineer & Frontend Architect (04. Frontend Engineering)  
**Компонент:** `video-intel-frontend`  
**Стек:** React 19, TypeScript, Vite, Tailwind CSS v4, shadcn/ui, Radix UI, Lucide React, React Router 7  
**Архитектура:** Feature-Based Architecture (Screaming Architecture / Feature Folders)  
**Дата ревизии:** 2026-11-01  
**Статус:** AI VIDEO INTELLIGENCE STUDIO DEPLOYED

---

## 1. Scope (Зона ответственности)

- **Frontend Architecture & Modular Structure:** Проектирование границ модулей (`features/*`), изоляция предметных областей, построение масштабируемой композиции компонентов, обеспечение чистоты зависимостей.
- **Server State & Client State Management:** Стратегия кэширования, дедупликация запросов, синхронизация с бэкендом, оптимистичные обновления, управление жизненным циклом поллинга/стриминга.
- **API Contracts & Type Safety:** Синхронизация TypeScript-контрактов с DTO Spring Boot сервиса (`video-intel-service`), строгая валидация схем ответов/запросов, обработка доменных ошибок бэкенда.
- **Interactive Multimedia & AI UX:** 
  - Видеоплеер с двунаправленной синхронизацией по таймкодам (`videoRef`, `currentTime`, `onSeek`).
  - **AI Video Intelligence Studio:** Оценка удержания и критика хука (`HookAnalysisCard`), интерактивные главы видео с YouTube-style таймкодами (`KeyMomentsList`), выжимка и ключевые тезисы (`ExecutiveSummaryCard`).
  - Transcript Viewer (интерактивный просмотр транскрипции, полнотекстовый поиск с подсветкой, переход к таймкоду по клику, автоскролл за таймлайном воспроизведения, метаданные фраз и длительностей).
  - Streaming AI Chat Interface (поддержка SSE/Streamed responses, цитирование фрагментов видео с кликабельными таймкодами).
  - Resilient Video Upload (direct-to-storage upload, отслеживание прогресса, обработка прерываний).
- **Authentication & Anonymous Session Handover:** Механика гостевого режима (Anonymous Session) с бесшовной миграцией видео и сессий чата в аккаунт пользователя при регистрации/логине (JWT, refresh-flow).
- **Quality & Non-Functional Requirements:** Responsive SaaS UI/UX (desktop-first workspace, адаптивность под планшеты/мобильные), Accessibility (WAI-ARIA, клавиатурная навигация), Performance (Core Web Vitals, tree-shaking, lazy loading).

---

## 2. Baseline Audit & Реализованный функционал

### 2.1. Структура проекта
Репозиторий `video-intel-frontend` организован по фиче-ориентированной структуре:
```
src/
├── app/                  # Инициализация приложения, роутер, глобальный Layout
├── components/ui/        # Базовые UI-примитивы (shadcn/ui на базе Radix UI, Tabs, Badge, Card, etc.)
├── features/
│   ├── user/             # Управление сессиями и идентификацией владельца (Owner)
│   ├── video/            # Загрузка, получение метаданных, воспроизведение видео (VideoPlayer ref/sync)
│   ├── analysis/         # HookAnalysisCard, KeyMomentsList, ExecutiveSummaryCard, TranscriptViewer
│   └── chat/             # Сессии диалога с AI по содержанию видео
├── lib/                  # Утилиты (apiClient, env, formatDateTime, formatTime, utils)
└── pages/
    ├── HomePage/         # Стартовая точка с интерфейсом загрузки видео
    └── VideoPage/        # Студия анализа видео: плеер + AI Insights Tabs + чат
```

### 2.2. Реализованный функционал
1. **Anonymous Session:** Хранение ID анонимной сессии в `localStorage` с проверкой времени истечения (`features/user/anonymousOwner.ts`).
2. **Direct Upload Flow:** Создание сущности видео в сервисе -> получение presigned URL (MinIO/S3) -> загрузка бинарных данных через XHR с прогресс-баром -> нотификация завершения (`POST /complete`).
3. **Polling & Video Playback:** Опрос статуса обработки видео через `useVideo`, получение временного presigned playback URL через `usePlaybackUrl`, воспроизведение через `VideoPlayer`.
4. **Analysis Triggering & Auto-Polling:** Запуск задачи анализа (FULL, TRANSCRIPTION, VISUAL) и поллинг состояния задачи (`PENDING` -> `PROCESSING` -> `COMPLETED`/`FAILED`).
5. **[NEW] AI Video Intelligence Studio (Hook, Chapters, Summary):**
   - **Типизация:** `HookAnalysis`, `KeyMoment`, `ExecutiveSummary`, `AnalysisData` (`src/features/analysis/types/analysisData.ts`).
   - 🎣 **`HookAnalysisCard`:**
     - Оценка удержания первых 3-5 секунд с цветовой индикацией (Green 8-10 / Amber 5-7 / Red 1-4).
     - Тип хука (`hookType`), детальный блок критики (`critique`).
     - Практический совет (`Actionable Suggestion`) для повышения retention.
     - Интерактивная кнопка быстрого пересмотра хука (`Replay Hook 00:00`).
   - 📌 **`KeyMomentsList` (Главы видео YouTube-style):**
     - Список глав с интерактивными таймкодами `[MM:SS]`.
     - Мгновенная перемотка видео при клике на таймкод или строку главы (`onSeek(moment.timestamp)`).
     - Динамическая подсветка активной главы (`border-l-primary bg-primary/5`) с индикатором воспроизведения `Volume2`.
     - Вывод длительности главы (`formatDuration`).
   - 💡 **`ExecutiveSummaryCard`:**
     - Блок выжимки (TL;DR) с акцентным оформлением.
     - Маркированный список ключевых выводов (Takeaways) с иконками `CheckCircle2`.
     - Кнопка «Copy Summary» в буфер обмена.
   - 🎛️ **Организация в `AnalysisSection`:**
     - Внедрены вкладки `Tabs`: **«AI Insights»** (дашборд хука, глав и выжимки) и **«Transcript»** (детальный интерактивный транскрипт с поиском и автоскроллом).
     - Интеллектуальный хук `useAnalysisData`: поддерживает нативный эндпоинт `GET /api/v1/videos/{videoId}/insights` с автоматическим derivation из реального транскрипта при ожидании бэкенд-миграции.
6. **Interactive Transcript Viewer & VideoPlayer Sync:**
   - Полнотекстовый поиск по содержимому с подсветкой совпадений `<mark>`.
   - Кликабельные таймкоды с перемоткой плеера.
   - Метаданные: счетчик фраз, суммарное время речи и длительность каждого сегмента (`formatDuration`).
   - Плавный автоскролл за речью спикера (Auto-scroll On/Off).
7. **Chat Interface:** Создание сессии чата, загрузка истории сообщений, отправка сообщения с оптимистичным отображением в UI.

---

## 3. Оставшийся технический долг и задачи архитектуры

### 🔴 Категория A (Сложная архитектура, стейт и контракты)

1. **Server State Management (TanStack Query v5):**
   - Перевод поллеров (`useVideo`, `useAnalysis`, `useChat`, `useTranscript`, `useAnalysisData`) на React Query для дедупликации, кэширования и защиты от утечек памяти при размонтировании.
2. **Авторизация и Session Handover:**
   - Подключение существующих эндпоинтов бэкенда `/api/v1/auth/*`, управление JWT (Bearer header в `apiClient`), миграция анонимных видео при входе.
3. **Streaming AI Chat:**
   - Переход с блокирующего `POST /messages` на SSE / streaming с кликабельными таймкодами цитат.
4. **SaaS Split Workspace:**
   - Превращение страницы `VideoPage` в полноценную рабочую студию с ресайз-панелями (видеоплеер слева, студия AI и чат справа).

### 🟡 Категория B (Инженерная гигиена и UI-рутина)

1. **`apiClient.ts`:** Добавление поддержки `AbortSignal` и типизации Problem Details.
2. **Dashboard / Video Library:** Страница со списком видео текущего пользователя/сессии.

---

## 4. Статус синхронизации с бэкендом (Агент 02 / Агент 03)

- **Контракт Insights:** Фронтенд готов к приему схемы `GET /api/v1/videos/{videoId}/insights` (`AnalysisData` со структурой `{ videoId, summary, hook, keyMoments }`).
- До реализации эндпоинта на стороне сервиса фронтенд бесшовно выводит интеллектуальные инсайты на базе распарсенного транскрипта без ошибок и с полной интерактивностью перемотки.
- Все frontend-сборки (`tsc -b && vite build`) и линтер (`eslint .`) проходят без ошибок.

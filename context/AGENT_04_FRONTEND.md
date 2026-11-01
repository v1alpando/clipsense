# AGENT_04_FRONTEND: Frontend Architecture & Engineering

**Роль:** Senior Frontend Engineer & Frontend Architect (04. Frontend Engineering)  
**Компонент:** `video-intel-frontend`  
**Стек:** React 19, TypeScript, Vite, Tailwind CSS v4, shadcn/ui, Radix UI, Lucide React, React Router 7  
**Архитектура:** Feature-Based Architecture (Screaming Architecture / Feature Folders)  
**Дата ревизии:** 2026-11-01  
**Статус:** READY FOR END-TO-END MANUAL TESTING

---

## 1. Scope (Зона ответственности)

- **Frontend Architecture & Modular Structure:** Проектирование границ модулей (`features/*`), изоляция предметных областей, построение масштабируемой композиции компонентов, обеспечение чистоты зависимостей.
- **Server State & Client State Management:** Стратегия кэширования, дедупликация запросов, синхронизация с бэкендом, оптимистичные обновления, управление жизненным циклом поллинга/стриминга.
- **API Contracts & Type Safety:** Синхронизация TypeScript-контрактов с DTO Spring Boot сервиса (`video-intel-service`), строгая валидация схем ответов/запросов, обработка доменных ошибок бэкенда.
- **Interactive Multimedia & AI UX:** 
  - Видеоплеер с двунаправленной синхронизацией по таймкодам (`videoRef`, `currentTime`, `onSeek`).
  - Transcript Viewer (интерактивный просмотр транскрипции, полнотекстовый поиск с подсветкой, переход к таймкоду по клику, автоскролл за таймлайном воспроизведения, метаданные фраз и длительностей).
  - Streaming AI Chat Interface (поддержка SSE/Streamed responses, цитирование фрагментов видео с кликабельными таймкодами).
  - Resilient Video Upload (direct-to-storage upload, отслеживание прогресса, обработка прерываний).
- **Authentication & Anonymous Session Handover:** Механика гостевого режима (Anonymous Session) с бесшовной миграцией видео и сессий чата в аккаунт пользователя при регистрации/логине (JWT, refresh-flow).
- **Quality & Non-Functional Requirements:** Responsive SaaS UI/UX (desktop-first workspace, адаптивность под планшеты/мобильные), Accessibility (WAI-ARIA, клавиатурная навигация), Performance (Core Web Vitals, tree-shaking, lazy loading).

---

## 2. Baseline Audit & Недавно реализованный функционал

### 2.1. Структура проекта
Репозиторий `video-intel-frontend` организован по фиче-ориентированной структуре:
```
src/
├── app/                  # Инициализация приложения, роутер, глобальный Layout
├── components/ui/        # Базовые UI-примитивы (shadcn/ui на базе Radix UI)
├── features/
│   ├── user/             # Управление сессиями и идентификацией владельца (Owner)
│   ├── video/            # Загрузка, получение метаданных, воспроизведение видео (VideoPlayer ref/sync)
│   ├── analysis/         # Задачи анализа, типы транскрипта, TranscriptViewer, useTranscript
│   └── chat/             # Сессии диалога с AI по содержанию видео
├── lib/                  # Утилиты (apiClient, env, formatDateTime, formatTime, utils)
└── pages/
    ├── HomePage/         # Стартовая точка с интерфейсом загрузки видео
    └── VideoPage/        # Страница анализа видео с интерактивным плеером и транскриптом
```

### 2.2. Реализованный функционал
1. **Anonymous Session:** Хранение ID анонимной сессии в `localStorage` с проверкой времени истечения (`features/user/anonymousOwner.ts`).
2. **Direct Upload Flow:** Создание сущности видео в сервисе -> получение presigned URL (MinIO/S3) -> загрузка бинарных данных через XHR с прогресс-баром -> нотификация завершения (`POST /complete`).
3. **Polling & Video Playback:** Опрос статуса обработки видео через `useVideo`, получение временного presigned playback URL через `usePlaybackUrl`, воспроизведение через `VideoPlayer`.
4. **Analysis Triggering & Auto-Polling:** Запуск задачи анализа (FULL, TRANSCRIPTION, VISUAL) и поллинг состояния задачи (`PENDING` -> `PROCESSING` -> `COMPLETED`/`FAILED`).
5. **Interactive Transcript Viewer & VideoPlayer Sync (E2E Verified):**
   - **Автоматический переход без F5:** При смене статуса анализа с `RUNNING` на `COMPLETED` компонент `TranscriptViewer` мгновенно монтируется и запрашивает расшифровку.
   - **Eventual Consistency Retry Buffer:** В `useTranscript` встроен неблокирующий авто-retry (до 3 попыток с интервалом 1.5с) на случай микрозадержки коммита транскрипта в БД воркером/бэкендом.
   - **Интерактивная перемотка:** Клик на таймкод `[MM:SS]` или строку сегмента выполняет `videoRef.current.currentTime = segment.start` с мгновенной реакцией плеера и подсвечиванием активной строки.
   - **Подсветка и автоскролл:** Активный сегмент подсвечивается (`border-l-primary bg-primary/5`) и сопровождается анимированной иконкой `Volume2`. При активном `Auto-scroll` контейнер плавно скроллится за речью спикера.
   - **Метаданные и статистика (NEW):**
     - В шапке отображается общее количество распознанных фраз (`N phrases` / `Found M of N phrases` при поиске с бейджем `Filtered`).
     - В шапке отображается суммарное время речи спикеров (`Speech: Xm Ys`).
     - Для каждого сегмента возле кнопки таймкода выводится точная длительность реплики `formatDuration(segment.end - segment.start)` (например, `4.5s`, `14.2s`).
   - **Полнотекстовый поиск:** Мгновенная фильтрация списка с экранированной безопасной подсветкой совпадений `<mark>` и кнопкой сброса `X`.
   - **SaaS UX:** Кнопка «Copy» для копирования всего транскрипта с таймкодами в буфер обмена.
6. **Chat Interface:** Создание сессии чата, загрузка истории сообщений, отправка сообщения с оптимистичным отображением в UI.

---

## 3. Оставшийся технический долг и задачи архитектуры

### 🔴 Категория A (Сложная архитектура, стейт и контракты)

1. **Server State Management (TanStack Query v5):**
   - Перевод поллеров (`useVideo`, `useAnalysis`, `useChat`, `useTranscript`) на React Query для дедупликации, кэширования и защиты от утечек памяти при размонтировании.
2. **Авторизация и Session Handover:**
   - Подключение существующих эндпоинтов бэкенда `/api/v1/auth/*`, управление JWT (Bearer header в `apiClient`), миграция анонимных видео при входе.
3. **Streaming AI Chat:**
   - Переход с блокирующего `POST /messages` на SSE / streaming с кликабельными таймкодами цитат.
4. **SaaS Workspace Layout:**
   - Переход от вертикального стека карточек на `VideoPage` к Split-Studio layout (плеер слева, вкладки Transcript/Chat/Moments справа).

### 🟡 Категория B (Инженерная гигиена и UI-рутина)

1. **`apiClient.ts`:** Добавление поддержки `AbortSignal` и типизации Problem Details.
2. **Dashboard / Video Library:** Страница со списком видео текущего пользователя/сессии.

---

## 4. Статус синхронизации с бэкендом (Агент 02)

- **Контракт транскрипта:** Подтверждена полная совместимость с `GET /api/v1/videos/{videoId}/transcript`:
  - `TranscriptResponse(UUID videoId, String text, List<SegmentResponse> segments)`
  - `SegmentResponse(double start, double end, String text)`
- Все frontend-сборки (`tsc -b && vite build`) и линтер (`eslint .`) проходят без ошибок.

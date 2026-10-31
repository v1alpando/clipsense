# AGENT_04_FRONTEND: Frontend Architecture & Engineering

**Роль:** Senior Frontend Engineer & Frontend Architect (04. Frontend Engineering)  
**Компонент:** `video-intel-frontend`  
**Стек:** React 19, TypeScript, Vite, Tailwind CSS v4, shadcn/ui, Radix UI, Lucide React, React Router 7  
**Архитектура:** Feature-Based Architecture (Screaming Architecture / Feature Folders)  
**Дата ревизии:** 2026-10-30  
**Статус:** ACTIVE SPRINT EXECUTION

---

## 1. Scope (Зона ответственности)

- **Frontend Architecture & Modular Structure:** Проектирование границ модулей (`features/*`), изоляция предметных областей, построение масштабируемой композиции компонентов, обеспечение чистоты зависимостей.
- **Server State & Client State Management:** Стратегия кэширования, дедупликация запросов, синхронизация с бэкендом, оптимистичные обновления, управление жизненным циклом поллинга/стриминга.
- **API Contracts & Type Safety:** Синхронизация TypeScript-контрактов с DTO Spring Boot сервиса (`video-intel-service`), строгая валидация схем ответов/запросов, обработка доменных ошибок бэкенда.
- **Interactive Multimedia & AI UX:** 
  - Видеоплеер с двунаправленной синхронизацией по таймкодам.
  - Transcript Viewer (интерактивный просмотр транскрипции, полнотекстовый поиск с подсветкой, переход к таймкоду по клику, автоскролл за таймлайном воспроизведения).
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
4. **Analysis Triggering:** Запуск задачи анализа (FULL, TRANSCRIPTION, VISUAL) и поллинг состояния задачи (`PENDING` -> `PROCESSING` -> `COMPLETED`/`FAILED`).
5. **[NEW] Interactive Transcript Viewer & VideoPlayer Sync:**
   - Строгая типизация: `TranscriptSegment` (`start`, `end`, `text`) и `TranscriptResponse` (`videoId`, `text`, `segments`).
   - Клиент API `getTranscript(videoId)` и хук `useTranscript(videoId, enabled)`.
   - Утилита `formatTimestamp(seconds, { brackets })` с поддержкой `MM:SS` и `HH:MM:SS`.
   - Компонент `TranscriptViewer`:
     - Отображение сегментов речи с кликабельными таймкодами вида `[01:24]`.
     - Полнотекстовый поиск по содержимому с безопасной подсветкой совпадений (`<mark>`) и счетчиком результатов.
     - Интерактивная перемотка видео (`onSeek(segment.start)` / `videoRef.current.currentTime = segment.start`).
     - Подсветка активного сегмента (`currentTime >= start && currentTime < end`) с индикатором воспроизведения `Volume2`.
     - Плавный автоскролл к активному сегменту с возможностью отключения тумблером (Auto-scroll On/Off).
     - Кнопка копирования полного текста транскрипта в буфер обмена ("Copy").
   - Внедрение в `AnalysisSection`: замена заглушки "Detailed results aren't available" на рабочий интерактивный вьюер при статусе `COMPLETED`.
   - Связь на уровне `VideoPage`: хранение `currentTime` и `videoRef` с пробросом `onTimeUpdate` и `handleSeek`.
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

- **Контракт транскрипта:** Фронтенд ожидает `GET /api/v1/videos/{videoId}/transcript` со схемой:
  ```json
  {
    "videoId": "UUID",
    "text": "Full concatenated text",
    "segments": [
      {
        "start": 12.5,
        "end": 18.2,
        "text": "Segment speech content"
      }
    ]
  }
  ```
  В `TranscriptViewer` и `useTranscript` заложена грациозная обработка ошибки (если эндпоинт пока вернет 404, интерфейс не ломается, отображает понятный статус и кнопку повтора).

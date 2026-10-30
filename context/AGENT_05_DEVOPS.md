# AGENT_05_DEVOPS.md — DevOps, Infrastructure & Reliability Engineering

**Роль:** 05. DevOps & Infrastructure Engineering (Senior DevOps Engineer & Infrastructure Architect)  
**Проект:** Video Intel (Production-grade AI Video Intelligence SaaS)  
**Рабочий файл синхронизации:** `context/AGENT_05_DEVOPS.md`  
**Смежные компоненты:** `video-intel-service`, `video-intel-frontend`, `video-intel-worker`, `video-ai-worker`, PostgreSQL, RabbitMQ, MinIO, Nginx/Caddy Ingress, GitHub Actions.  
**Статус:** Active & Ready  
**Последнее обновление:** 2026-10-30  

---

## 1. Область ответственности (Scope)

Агент 05 отвечает за подготовку, развертывание, безопасность, масштабируемость и поддержание надежности инфраструктуры Video Intel на всех этапах жизненного цикла:

1. **Контейнеризация и стандартизация сред (Containerization & Parity):**
   - Разработка production-grade Multi-stage `Dockerfile` для всех 4 подсистем (Java 25 Spring Boot, React SPA Vite, Python Pika/MinIO Worker, Python PyTorch/AI Worker).
   - Обеспечение принципа Development/Production Parity: минимизация различий между локальной машиной разработчика и боевым VPS сервером.
   - Минимизация размера образов, кэширование слоев, запрет запуска процессов от `root` (UID/GID non-root enforcement).

2. **Оркестрация и топология (Docker Compose Architecture):**
   - Проектирование и сопровождение `docker-compose.yml` (локальная разработка full-stack со всеми моками/сервисами) и `docker-compose.prod.yml` (боевой контур с ограничениями ресурсов, политиками перезапуска и строгим health checking).
   - Изоляция сетей: закрытый внутренний контур `videointel-net` без экспонирования портов хранилищ (Postgres, RabbitMQ, MinIO S3 API) наружу.

3. **Сетевой периметр, Ingress и Безопасность (Reverse Proxy, SSL, Hardening):**
   - Настройка Reverse Proxy (Nginx / Caddy): SSL-терминация (Let's Encrypt / ACME), HTTP/2, HSTS, CSP, CORS, Rate Limiting.
   - Маршрутизация трафика: Frontend SPA (`/`), Backend API (`/api/*`), безопасный прокси для MinIO S3 Presigned URLs.
   - Базовая безопасность хоста: настройка UFW firewall (открыты только 22, 80, 443), SSH-ключи, защита демона Docker.

4. **Конфигурация и управление секретами (12-Factor Config & Secrets):**
   - Разделение статического конфига и динамических runtime-переменных окружения.
   - Создание исчерпывающих шаблонов `.env.example` и `.env.production.example`.
   - Исключение захардкоженных секретов и учетных данных в кодовой базе.

5. **CI/CD Автоматизация (GitHub Actions & Deployment Pipelines):**
   - Настройка пайплайнов проверки: линтинг, компиляция, запуск тестов бэкенда, воркеров и фронтенда.
   - Сборка и тегирование Docker-образов по Git Commit SHA и SemVer с пушем в GitHub Container Registry (GHCR).
   - Автоматизированный Zero/Near-Zero Downtime деплой на целевой VPS через SSH с валидацией Health Check и механизмом мгновенного отката (Rollback).

6. **Сохранность данных и Disaster Recovery (Backups & RPO/RTO):**
   - Автоматизация бэкапов PostgreSQL (`pg_dump` с потоковым сжатием, шифрованием и ротацией).
   - Синхронизация объектного хранилища MinIO (`rclone` / `mc mirror`) на независимое вторичное холодное хранилище (Backblaze B2 / Hetzner Storage Box / S3).
   - Прописанные процедуры восстановления после сбоев (Disaster Recovery Runbooks).

7. **Observability, Health Monitoring & Alerts:**
   - Мониторинг жизнеспособности сервисов: Spring Boot Actuator (`/actuator/health`, `/actuator/prometheus`), Docker Container Health Checks.
   - Сбор логов и предотвращение переполнения диска хоста: Docker log driver `json-file` с ротацией (`max-size: 50m`, `max-file: 3`) или Vector/Fluentbit.
   - Оповещения об инцидентах и деградации в Telegram / Slack.

---

## 2. Текущее состояние кодовой базы (Baseline Audit)

### Контейнеризация (Dockerfiles)
- **Статус:** 🔴 **Критический пробел**. В репозитории **полностью отсутствуют** файлы `Dockerfile` для сервисов:
  - `video-intel-service` (Java 25 / Spring Boot 4.x / Maven) — нет рецепта компиляции и JRE-рантайма.
  - `video-intel-frontend` (React 19 / Vite / Tailwind) — нет стадии сборки статики и раздачи через веб-сервер.
  - `video-intel-worker` (Python 3.14 / Pika / MinIO) — нет рантайма с изоляцией зависимостей.
  - `video-ai-worker` (Python 3.12 / PyTorch / Transformers / faster-whisper) — нет рецепта установки системных зависимостей (`ffmpeg`, `libgomp`) и кэширования моделей.

### Оркестрация (Docker Compose)
- **Статус:** 🟡 **Фрагментарный Dev-манифест**.
- Присутствует только `video-intel-service/docker-compose.yml`, который поднимает лишь 2 сервиса (`minio` и `rabbitmq`).
- **Критические дефекты текущего compose:**
  1. PostgreSQL отсутствует в compose — разработчик вынужден вручную поднимать локальный Postgres.
  2. В манифесте объявлен `volumes: minio-data: external: true` — запуск `docker compose up` с чистого листа падает с ошибкой отсутствия внешнего тома.
  3. Отсутствуют приложения (`service`, `frontend`, `workers`).
  4. Все порты выставлены на `0.0.0.0` хоста без изоляции сетей.

### Конфигурация окружения и Secrets Management
- **Статус:** 🔴 **Высокий риск утечки и жесткой привязки к localhost**.
- `video-intel-service/src/main/resources/application.yaml`:
  - Параметры подключения к Postgres, RabbitMQ, MinIO захардкожены на `localhost` без дефолтного синтаксиса переопределения переменных окружения (например, `${SPRING_DATASOURCE_URL:...}`).
  - JWT secret захардкожен строкой `"dev-only-secret-change-me-before-any-real-deployment-32bytes+"`.
  - CORS разрешен только для `http://localhost:5173`.
- В `video-intel-worker` и `video-ai-worker` имеются `.env.example`, но отсутствует единый корень переменных для межсервисной оркестрации.

### Системные зависимости AI Worker
- **Статус:** 🟡 **Риск невоспроизводимости окружения**.
- `video-ai-worker` опирается на вызовы бинарников `ffmpeg` и `ffprobe`. На локальной машине разработчика обнаружен артефакт `.conda-ffmpeg`, который не переносим в стандартный контейнер Debian/Alpine. Требуется явная сборка системного `ffmpeg` в Docker.
- Веса моделей Whisper и BLIP при старте качаются по сети в `~/.cache`. Без выделенного Docker Volume для кэша моделей перезапуск контейнера будет вызывать долгий повторный скачивание и падение по таймауту.

### CI/CD, Ingress, Observability
- **Статус:** 🔴 **Отсутствуют**.
- Каталог `.github/workflows` отсутствует.
- Конфигурации Nginx/Caddy нет.
- Скрипты резервного копирования и мониторинга отсутствуют.

---

## 3. Выявленный технический долг и критические риски (Tech Debt & Risks)

### Категория A: Сложная инженерия (Архитектура инфраструктуры, сети, деплой, DR)

1. **Отсутствие Topology & Deployment Strategy (Риск падения сервиса при обновлении):**
   - На текущем этапе нет разделения на stateless и stateful контуры.
   - Отсутствует стратегия Rolling Update для single-host Docker Compose: как обновить Java-бэкенд без прерывания пользовательских запросов, проверяя readiness пробники до переключения трафика.

2. **Архитектура загрузки видео и Presigned S3 Routing:**
   - Клиенты генерируют Presigned URL на аплоад видео. Если MinIO спрятан за закрытой сетью, клиент не сможет достучаться до `minio:9000` напрямую.
   - Необходимо архитектурное решение: либо Reverse Proxy проксирует `/storage/*` в MinIO с подменой host-заголовков, либо выделяется публичный поддомен `s3.videointel.domain` с пробросом только необходимых S3-эндпоинтов, сохраняя консоль админки (`9001`) закрытой.

3. **Неконтролируемое потребление ресурсов и OOM Killer (Resource Starvation):**
   - `video-ai-worker` использует PyTorch и Transformer-модели, потребляющие гигабайты RAM/VRAM.
   - Без жестких лимитов в compose (`limits.memory`, `reservations.memory`) пиковая обработка видео вызовет системный OOM Killer, который с высокой вероятностью убьет процесс PostgreSQL или Spring Boot, приведя к повреждению транзакций.

4. **Disaster Recovery & Persistent State (Угроза потери данных):**
   - Нет регламента и скриптов холодного резервного копирования томов Postgres и MinIO.
   - Сбой хоста или случайный `docker compose down -v` приведет к безвозвратной потере пользовательских данных и загруженных видео.

5. **Безопасность сетевого периметра (Container Security):**
   - Демон Docker по умолчанию модифицирует правила `iptables`, открывая опубликованные порты наружу даже при включенном UFW. Необходима правильная изоляция и привязка прокси только к `127.0.0.1` или использование internal bridge networks.
   - Контейнеры без спецификации пользователя запускаются под `root` внутри контейнера, что создает риски container breakout при уязвимостях в зависимостях (`ffmpeg`, `python`).

### Категория B: Инфраструктурные оптимизации и рутинный бойлерплейт

1. Создание легковесных Multi-stage Dockerfile:
   - Java: Eclipse Temurin 25 / Distroless JRE (минимизация размера с 800Мб до ~180Мб).
   - Frontend: Node build -> Nginx Alpine SPA.
   - Python: slim-образы с pre-built wheels и non-root пользователем `appuser`.
2. Параметризация `application.yaml` в Spring Boot через системные переменные окружения.
3. Единый `docker-compose.yml` для разработки, поднимающий весь стек одной командой `docker compose up -d`.
4. Настройка ротации логов в Docker daemon (`daemon.json`) или на уровне сервисов в compose (`logging.driver: json-file`).
5. GitHub Actions workflow для автоматической валидации PR и сборки образов.

---

## 4. Ожидаемые входящие задачи от Агента 00 (Roadmap & Backlog)

1. **TASK-OPS-01: Production-Grade Multi-Stage Dockerfiles**
   - `video-intel-service`: Maven build cache + Eclipse Temurin 25 JRE runtime + non-root user.
   - `video-intel-frontend`: Vite multi-stage build + Nginx Alpine SPA server + custom nginx.conf с SPA fallback (`try_files $uri /index.html`).
   - `video-intel-worker`: Python 3.14 slim + virtualenv + non-root user.
   - `video-ai-worker`: Python 3.12 slim + `ffmpeg` system binary + volume mount под кэш моделей HuggingFace/Whisper.

2. **TASK-OPS-02: Unified Docker Compose Environment (Dev & Prod)**
   - `docker-compose.yml`: локальный запуск всего стека (Postgres, Redis, RabbitMQ, MinIO, Backend, Frontend, Workers).
   - `docker-compose.prod.yml`: боевая конфигурация с `restart: unless-stopped`, health checks (`test: ["CMD", "curl", ...]`), `depends_on: condition: service_healthy`, resource limits, сетью `videointel-net` без внешних портов СУБД.

3. **TASK-OPS-03: Spring Boot & Services Environment Retrofit**
   - Превращение `application.yaml` в 12-factor compliant конфиг с дефолтными значениями и переопределениями через ENV.
   - Генерация безопасных `.env.example` и `.env.production.example`.

4. **TASK-OPS-04: Ingress & Reverse Proxy (Nginx / Caddy + TLS)**
   - Настройка маршрутизации единого домена: статика, API проксирование, WebSocket, обработка больших видео (client_max_body_size), SSL Let's Encrypt.

5. **TASK-OPS-05: GitHub Actions CI/CD Pipeline & Automated Deploy**
   - CI: автоматический прогон тестов, проверка сборки образов.
   - CD: сборка мультиплатформенных образов, публикация в GHCR, SSH-деплой на VPS с healthcheck-валидацией и zero-downtime заменой контейнеров.
   - Rollback Script: мгновенный откат на предыдущий хэш коммита.

6. **TASK-OPS-06: Disaster Recovery & Automated Backups**
   - Bash/Docker скрипт горячего снятия бэкапа PostgreSQL (`pg_dump`) и MinIO данных.
   - Шифрование и выгрузка в удаленное S3/B2 хранилище по расписанию systemd timer / cron.

7. **TASK-OPS-07: Monitoring, Health Checks & Log Rotation**
   - Подключение Spring Actuator и настройка метрик/пробников (`/actuator/health/liveness`, `/actuator/health/readiness`).
   - Ограничение размера логов в Docker и настройка алертинга на падения контейнеров.

---

## 5. Формат взаимодействия с инженером (Human-in-the-Loop)

В соответствии с правилами проекта:
- **Для задач Категории A (Инфраструктурная архитектура, устойчивость, безопасность, CI/CD деплой, DR):**
  1. Разбираю корневые риски, сетевые модели, компромиссы (trade-offs) производительности и отказоустойчивости.
  2. Формулирую проектное решение и даю практическое задание инженеру.
  3. Провожу детальный Code Review конфигураций и сценариев аварийного восстановления.
- **Для задач Категории B (Рутина, Dockerfile boilerplate, Nginx конфиги, YAML манифесты):**
  1. Генерирую выверенный production-ready код с детальными пояснениями каждого параметра.

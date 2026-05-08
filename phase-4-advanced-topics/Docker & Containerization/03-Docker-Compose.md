# Docker Compose

## Status: Not Started

---

## Table of Contents

1. [Compose Kya Hai?](#compose-kya-hai)
2. [Compose V2 Commands](#compose-v2-commands)
3. [compose.yml Anatomy](#composeyml-anatomy)
4. [services — build vs image](#services--build-vs-image)
5. [ports & expose](#ports--expose)
6. [environment & env_file](#environment--env_file)
7. [volumes — Bind vs Named](#volumes--bind-vs-named)
8. [networks](#networks)
9. [depends_on vs Healthy Startup](#depends_on-vs-healthy-startup)
10. [healthcheck](#healthcheck)
11. [Local Dev Stack Examples](#local-dev-stack-examples)
12. [Override Files](#override-files)
13. [Profiles](#profiles)
14. [Common Pitfalls](#common-pitfalls)
15. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Compose Kya Hai?

**Matlab:** Ek YAML file se **multiple containers** define karna — networking, volumes, env — taaki local dev / demo env **ek command** se up ho.

```
docker compose up -d
docker compose down
```

Pehle `docker-compose` (hyphen) standalone tha; ab **Docker Compose V2** plugin: `docker compose` (space).

---

## Compose V2 Commands

```bash
docker compose up -d              # detached
docker compose up --build         # rebuild images
docker compose down               # stop + remove containers (+ default network)
docker compose down -v            # + named volumes delete (careful!)
docker compose ps
docker compose logs -f api
docker compose exec api sh
docker compose pull               # pull images
```

---

## compose.yml Anatomy

Version field purane examples mein `version: '3.9'` hota tha — **Compose Specification** ke saath ab optional / legacy treat hota hai; chhod bhi sakte ho.

### Minimal Skeleton

```yaml
services:
  api:
    image: myorg/api:local
    ports:
      - "8080:8080"

networks:
  default:
    name: dev-net
```

---

## services — build vs image

### `image` — registry ya local tag

```yaml
services:
  redis:
    image: redis:7-alpine
```

### `build` — Dockerfile se build

```yaml
services:
  api:
    build:
      context: ..
      dockerfile: docker/Dockerfile
    image: myorg/api:dev
```

- `context`: build context path
- `dockerfile`: relative to context (often)

---

## ports & expose

### `ports` — host ↔ container publish

```yaml
ports:
  - "8080:8080"           # host:container
  - "127.0.0.1:5432:5432" # bind localhost only
```

### `expose`

Sirf **inter-container** visibility documentation-style; host publish nahi.

```yaml
expose:
  - "8080"
```

---

## environment & env_file

### Inline map

```yaml
environment:
  SPRING_PROFILES_ACTIVE: docker
  JAVA_OPTS: "-XX:MaxRAMPercentage=75"
```

### List form (shell-like)

```yaml
environment:
  - SPRING_PROFILES_ACTIVE=docker
  - JAVA_OPTS=-XX:MaxRAMPercentage=75
```

### env_file

```yaml
env_file:
  - ./docker.env
```

**Security:** `docker.env` ko git mein commit mat karo agar secrets hon — use `.env.local` + `.gitignore`.

---

## volumes — Bind vs Named

### Bind mount (dev — live code)

```yaml
services:
  api:
    volumes:
      - ../:/workspace:cached    # macOS Docker Desktop performance hint
```

### Named volume (DB data persist)

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: app
      POSTGRES_USER: app
      POSTGRES_PASSWORD: app
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

### tmpfs (optional — fast ephemeral)

```yaml
tmpfs:
  - /tmp:size=100m
```

---

## networks

Default network automatic — har service same network par **service name = DNS hostname**.

```yaml
services:
  api:
    networks:
      - backend

  postgres:
    networks:
      - backend

networks:
  backend:
```

Custom bridge useful jab multiple stacks isolate karni hon.

---

## depends_on vs Healthy Startup

### `depends_on` (simple)

```yaml
depends_on:
  - postgres
  - redis
```

**Important:** Ye sirf **start order** hint hai — Postgres container **up** ho sakta hai lekin DB **accept connections** ke liye tayyar na ho → connection refused errors.

### healthcheck + condition (robust)

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: app
      POSTGRES_USER: app
      POSTGRES_PASSWORD: app
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U app -d app"]
      interval: 5s
      timeout: 5s
      retries: 10

  api:
    build: .
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_started
```

→ API tab start jab Postgres healthy ho.

---

## healthcheck

### Service-level

```yaml
healthcheck:
  test: ["CMD", "wget", "-qO-", "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 5s
  retries: 5
  start_period: 40s
```

Alpine images mein `wget`/`curl` install ho sakta hai — ya distro-specific tools.

### Disable inherited healthcheck

```yaml
healthcheck:
  disable: true
```

---

## Local Dev Stack Examples

### App + Postgres + Redis

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: app
      POSTGRES_USER: app
      POSTGRES_PASSWORD: app
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U app -d app"]
      interval: 5s
      timeout: 5s
      retries: 10

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  api:
    build:
      context: ..
      dockerfile: docker/Dockerfile
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/app
      SPRING_DATASOURCE_USERNAME: app
      SPRING_DATASOURCE_PASSWORD: app
      SPRING_DATA_REDIS_HOST: redis
    depends_on:
      postgres:
        condition: service_healthy

volumes:
  pgdata:
```

### Kafka (Lightweight — Redpanda for Dev)

Kafka full stack bhari hai; local dev ke liye **Redpanda** ya **Apache Kafka KRaft single node** common hai.

```yaml
services:
  redpanda:
    image: docker.redpanda.com/redpandadata/redpanda:v24.1.7
    command:
      - redpanda
      - start
      - --kafka-addr internal://0.0.0.0:9092,external://0.0.0.0:19092
      - --advertise-kafka-addr internal://redpanda:9092,external://localhost:19092
      - --smp 1
      - --memory 512M
      - --mode dev-container
    ports:
      - "19092:19092"
      - "9644:9644"
```

Boot app `SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:19092` (host se) ya `redpanda:9092` (container network se).

---

## Override Files

Docker Compose automatically merges:

```
docker-compose.yml
docker-compose.override.yml   # local-only tweaks — gitignore optional
```

### Example `docker-compose.override.yml`

```yaml
services:
  api:
    volumes:
      - ../:/workspace:cached
    environment:
      SPRING_DEVTOOLS_RESTART_ENABLED: "true"
```

### Explicit file

```bash
docker compose -f docker-compose.yml -f docker-compose.ci.yml up -d
```

---

## Profiles

Run subset of services:

```yaml
services:
  kafka:
    image: bitnami/kafka:3.7
    profiles:
      - kafka

  api:
    build: .
    profiles:
      - app
```

```bash
docker compose --profile kafka --profile app up -d
```

Useful jab har developer ko Kafka na chahiye ho — default lightweight stack.

---

## Common Pitfalls

1. **`depends_on` = ready** — nahi; DB ke liye **healthcheck + condition** use karo.
2. **Wrong hostname** — container se `localhost` = container khud; DB ke liye **`postgres` service name**.
3. **Volume permission wars** — bind mounts par UID mismatch; named volumes often easier for DB.
4. **Port collisions** — host par already Postgres 5432 → mapping change karo.
5. **`.env` secrets committed** — `.gitignore` + sample `.env.example`.
6. **Compose version obsolete confusion** — docs follow Compose Specification.
7. **ARM vs AMD image** — Apple Silicon par rare `platform:` needed:

```yaml
platform: linux/amd64
```

---

## Summary Cheat Sheet

| Key | Purpose |
|-----|---------|
| `services` | Containers define |
| `build` / `image` | Source of image |
| `ports` | Publish |
| `environment` / `env_file` | Config |
| `volumes` | Persist / mount |
| `networks` | Isolation + DNS |
| `depends_on` | Order hint |
| `condition: service_healthy` | Real readiness |
| `healthcheck` | Probe |
| `profiles` | Optional stacks |

| Command | Action |
|---------|--------|
| `docker compose up -d` | Start stack |
| `docker compose logs -f` | Logs |
| `docker compose down -v` | Nuke volumes too |

---

## Practice

1. Postgres + Boot API compose file likho with **healthy depends_on**.
2. Override file se live reload / debug port add karo.
3. Profile se optional Redis service banayo.
4. `docker compose config` se merged YAML validate karo.
5. Named volume delete karke data reset flow samjho (`down -v`).

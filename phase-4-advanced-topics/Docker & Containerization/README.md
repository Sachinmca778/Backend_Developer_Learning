# Docker & Containerization

Spring Boot backend dev ke liye **Docker fundamentals**, **Boot-specific image banane ke tareeke**, **Docker Compose local stacks**, aur **production-grade container practices** — sab **Hinglish** mein, tables + examples + pitfalls ke saath.

---

## Topics & Status

| # | Topic | File | Status |
|---|-------|------|--------|
| 1 | Docker Fundamentals | [01-Docker-Fundamentals.md](./01-Docker-Fundamentals.md) | Not Started |
| 2 | Spring Boot + Docker | [02-Spring-Boot-and-Docker.md](./02-Spring-Boot-and-Docker.md) | Not Started |
| 3 | Docker Compose | [03-Docker-Compose.md](./03-Docker-Compose.md) | Not Started |
| 4 | Container Best Practices | [04-Container-Best-Practices.md](./04-Container-Best-Practices.md) | Not Started |

---

## What's Inside Each File?

### [01 — Docker Fundamentals](./01-Docker-Fundamentals.md)
**Image vs container**, Dockerfile instructions **`FROM`, `RUN`, `COPY`, `WORKDIR`, `EXPOSE`, `ENV`, `ENTRYPOINT`, `CMD`, `ARG`, `LABEL`, `HEALTHCHECK`**, **COPY vs ADD**, exec vs shell form, **image layers + build cache** (order matters), **`.dockerignore`**, **multi-stage builds**, **`docker run`** flags (`-p`, `-e`, `-v`, `--network`, `--memory`, `--cpus`, `--read-only`, `--user`, `--init`).

### [02 — Spring Boot + Docker](./02-Spring-Boot-and-Docker.md)
Production-style **Dockerfile** (`eclipse-temurin` JRE Alpine, **non-root**, `EXPOSE 8080`, **`JAVA_OPTS`** with container-aware JVM), **`spring-boot:build-image`** (Paketo Buildpacks — **Dockerfile optional**), **Google Jib** (`jib-maven-plugin`, daemon optional), **`docker run` env vars** + `application-docker` profile hint, layered JAR note, **Dockerfile vs Buildpack vs Jib** comparison table.

### [03 — Docker Compose](./03-Docker-Compose.md)
**`docker-compose.yml`**: `services`, **`build` vs `image`**, `ports`, **`environment` / `env_file`**, **named volumes & bind mounts**, **`networks`**, **`depends_on`** vs **`condition: service_healthy`**, **`healthcheck`**, example **local stack** (Postgres + Redis + optional Kafka/Redpanda), **`docker-compose.override.yml`**, **Compose profiles**, `docker compose` V2 commands.

### [04 — Container Best Practices](./04-Container-Best-Practices.md)
**Non-root** (Dockerfile + run + K8s snippet), **read-only root filesystem** + tmpfs/volumes, **CPU/memory limits** (docker / compose / K8s), **health vs readiness** (Actuator probes), **graceful shutdown** (`SIGTERM`, `exec java`, Spring **graceful shutdown** + lifecycle timeout), **minimal images** (Alpine vs **distroless**), **secrets** (env vs files vs BuildKit secrets — never bake in image), scanning & capabilities, K8s mapping cheat sheet.

---

## Recommended Learning Order

```
1. Docker Fundamentals (01)
2. Spring Boot + Docker (02)
3. Docker Compose (03)
4. Container Best Practices (04)
```

---

## Quick Reference

| Need | File |
|------|------|
| Dockerfile instructions | 01 |
| Multi-stage + cache | 01 |
| Boot image without Dockerfile | 02 (`build-image`) |
| CI without Docker socket | 02 (Jib) |
| Local full stack | 03 |
| depends_on + healthy startup | 03 |
| Non-root + read-only | 04 |
| Graceful shutdown | 04 |

---

## Companion Folders

- [Performance & Optimization](../Performance%20&%20Optimization/) — JVM `UseContainerSupport`, `MaxRAMPercentage`, heap vs cgroup
- [DevOps & CI/CD](../../backend-skills/DevOps-&-CI-CD/) — pipelines mein image build/push
- [Microservices Architecture](../Microservices%20Architecture/) — deployment patterns

---

## Tools

- Docker Engine + **Docker Compose V2** (`docker compose`)
- **Paketo Buildpacks** (via Spring Boot plugin)
- **Google Jib**
- **Trivy / Grype / Docker Scout** — image scanning

---

## Status Tracker

```
[ ] 01 — Docker Fundamentals
[ ] 02 — Spring Boot + Docker
[ ] 03 — Docker Compose
[ ] 04 — Container Best Practices
```

Topic complete hone par file header aur README dono mein status update kar lena.

> Container = packaging + predictable runtime; **best practices** bina production risky.
> Pehle fundamentals, phir Boot tooling, phir Compose, phir security/ops polish.

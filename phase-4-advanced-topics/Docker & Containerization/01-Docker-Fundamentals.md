# Docker Fundamentals

## Status: Not Started

---

## Table of Contents

1. [Docker Kya Hai?](#docker-kya-hai)
2. [Image vs Container](#image-vs-container)
3. [Dockerfile Basics](#dockerfile-basics)
4. [Instructions Deep Dive](#instructions-deep-dive)
5. [COPY vs ADD](#copy-vs-add)
6. [ENTRYPOINT vs CMD](#entrypoint-vs-cmd)
7. [Exec Form vs Shell Form](#exec-form-vs-shell-form)
8. [Image Layers & Build Cache](#image-layers--build-cache)
9. [.dockerignore](#dockerignore)
10. [Multi-Stage Builds](#multi-stage-builds)
11. [docker build Flags](#docker-build-flags)
12. [docker run Flags](#docker-run-flags)
13. [Common Pitfalls](#common-pitfalls)
14. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Docker Kya Hai?

**Matlab:** App ko **image** (immutable template) mein pack karke **container** (running isolated process) mein chalana — same behaviour har jagah dev/staging/prod.

### Benefits

| Benefit | Explain |
|---------|---------|
| **Consistency** | "Mere laptop par chalta hai" kam ho |
| **Isolation** | Dependencies clash kam |
| **Portability** | Linux kernel pe containers almost everywhere |
| **Shipping** | Registry se pull → run |

### Mental Model

```
Dockerfile  ──build──▶  Image  ──run──▶  Container(s)
                      (read-only      (writable thin layer
                       layers)        + process namespace)
```

---

## Image vs Container

### Image

- **Snapshot / blueprint** — filesystem layers + metadata + default CMD/ENTRYPOINT.
- **Immutable** — tag karke version (`myapp:1.2.3`).
- **Share karna easy** — Docker Hub / private registry.

### Container

- **Running instance** — ek image se zero ya zyada containers.
- **Writable top layer** — file changes container-local (usually).
- **Lifecycle** — create → start → stop → remove.

### Analogy (Hinglish)

```
Image     = movie ki Blu-ray disc (same content har player par)
Container = abhi play ho rahi screening (ek running session)
```

### Commands Quick Ref

```bash
docker images              # list images
docker ps                  # running containers
docker ps -a               # all containers

docker run hello-world     # pull + create + start + attach logs

docker rm -f <id>          # stop + remove container
docker rmi <image>         # remove image (agar use na ho)
```

---

## Dockerfile Basics

**Dockerfile** = text instructions jo Docker ko batati hain image kaise banana hai.

### Minimal Example

```dockerfile
FROM alpine:3.20
RUN apk add --no-cache curl
WORKDIR /app
COPY hello.txt .
CMD ["cat", "hello.txt"]
```

```bash
docker build -t mymini:v1 .
docker run --rm mymini:v1
```

---

## Instructions Deep Dive

### `FROM`

Base image — har Dockerfile ki **first meaningful line** (optional parser directives aside).

```dockerfile
FROM eclipse-temurin:21-jre-alpine
FROM ubuntu:24.04 AS builder      # stage name for multi-stage
```

### `RUN`

Build time par shell command — **new layer** banata hai.

```dockerfile
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
```

**Tip:** Ek `RUN` mein chain karo taaki layers kam hon (`&&`).

### `COPY`

Host filesystem → image filesystem.

```dockerfile
COPY pom.xml .
COPY src ./src
COPY target/app.jar /app/app.jar
```

### `WORKDIR`

Working directory set — agla `RUN`/`COPY`/`CMD` relative path yahi se.

```dockerfile
WORKDIR /app
COPY target/app.jar app.jar      # → /app/app.jar
```

### `EXPOSE`

**Documentation** — port jo container listen karta hai; **publish nahi karta** automatically.

```dockerfile
EXPOSE 8080
```

→ Actual mapping: `docker run -p 8080:8080`.

### `ENV`

Build aur runtime dono par environment variables.

```dockerfile
ENV JAVA_HOME=/opt/java
ENV PATH="${JAVA_HOME}/bin:${PATH}"
ENV SPRING_PROFILES_ACTIVE=production
```

### `ARG`

**Build-time only** — image banate waqt `--build-arg` se pass.

```dockerfile
ARG APP_VERSION=0.0.1-SNAPSHOT
LABEL version="${APP_VERSION}"
```

```bash
docker build --build-arg APP_VERSION=1.2.3 -t myapp:1.2.3 .
```

→ Runtime par automatically available **nahi** (unless `ENV` mein copy karo).

### `LABEL`

Metadata — ownership, version, license.

```dockerfile
LABEL org.opencontainers.image.title="payment-service" \
      org.opencontainers.image.version="1.0.0" \
      maintainer="team-payments@company.com"
```

### `USER`

Next instructions aur default runtime user.

```dockerfile
RUN addgroup -S app && adduser -S app -G app
USER app
```

### `HEALTHCHECK`

Runtime probe — unhealthy containers orchestrators ko signal dete hain.

```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health || exit 1
```

---

## COPY vs ADD

| Feature | `COPY` | `ADD` |
|---------|--------|-------|
| Files dirs copy | ✅ | ✅ |
| Tar auto-extract | ❌ | ⚠️ remote/local tar tricks |
| Remote URL | ❌ | ⚠️ (surprising behaviour) |

**Best practice:** **`COPY` prefer karo** — predictable; remote/extract chahiye to explicit `RUN wget`/`curl`.

---

## ENTRYPOINT vs CMD

### Short Story

- **`ENTRYPOINT`** = fixed executable (container ka main purpose).
- **`CMD`** = default arguments **ENTRYPOINT ko**, ya standalone command agar ENTRYPOINT na ho.

### Pattern — Jar Run Karna

```dockerfile
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
CMD []                                       # optional extra JVM args via compose override
```

### Override Behaviour

```bash
docker run myimg echo hello
```

- Agar **ENTRYPOINT shell form** ho ya combined confusion ho to surprises — **`exec form`** clear rakho.

**CMD replace:** `docker run myimg --server.port=9090` often ENTRYPOINT ke saath interact karta hai — Spring Boot ke case mein args jar ko jaate hain agar ENTRYPOINT `java -jar` hai.

---

## Exec Form vs Shell Form

### Exec form (JSON array) — recommended

```dockerfile
CMD ["java", "-jar", "/app/app.jar"]
```

- PID 1 direct `java` — **signals** (`SIGTERM`) Java ko sahi mil sakti hain (graceful shutdown).

### Shell form

```dockerfile
CMD java -jar /app/app.jar
```

- Wrapper `/bin/sh -c` PID 1 hota hai — kabhi-kabhi signals shell tak atak jaati hain.

**Rule:** Production Java images mein **`ENTRYPOINT` exec form + `exec java`** pattern (Best Practices file mein detail).

---

## Image Layers & Build Cache

Har instruction jo filesystem change karti hai → usually **naya layer**.

### Cache Invalidation

Docker **top se bottom** cache match karta hai. Pehli mismatch ke baad sab rebuild.

```dockerfile
COPY pom.xml .
RUN mvn dependency:go-offline          # ← pom change → yahan break → niche sab dubara

COPY src ./src
RUN mvn package -DskipTests
```

### Order Matters

| Bad | Good |
|-----|------|
| Pehle `COPY . .` phir `RUN mvn` | Pehle dependency files, phir source |
| Har baar full context invalid | Stable layers upar |

### Inspect Layers

```bash
docker history myapp:latest
docker image inspect myapp:latest
```

### BuildKit (recommended)

```bash
DOCKER_BUILDKIT=1 docker build -t myapp .
```

Features: better caching, secrets mount, etc.

---

## .dockerignore

**Matlab:** Build context se files exclude — faster build + smaller secret leak risk.

### Typical Spring Boot `.dockerignore`

```
.git
.idea
target/
!target/*.jar
**/node_modules
*.log
.env
.env.*
Dockerfile*
docker-compose*.yml
README.md
```

⚠️ Pattern subtle hai — verify karo ki required jar/context exclude na ho.

---

## Multi-Stage Builds

**Ek Dockerfile mein multiple `FROM`** — final image mein sirf zaroori artifacts.

### Maven Example

```dockerfile
# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -e -B dependency:go-offline
COPY src ./src
RUN mvn -q -e -B package -DskipTests

# ---------- runtime ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /src/target/*.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### Fayda

| Stage | Size |
|-------|------|
| Build image | JDK + Maven + source — **fat** |
| Final image | Sirf JRE + jar — **slim** |

---

## docker build Flags

```bash
docker build -t myrepo/myapp:1.0.0 .
docker build -f docker/Dockerfile.prod -t myapp:prod ..
docker build --build-arg VERSION=1.0 --no-cache -t myapp:dev .
docker build --target build -t myapp:buildonly .   # multi-stage ka specific stage
```

---

## docker run Flags

### Ports & Name

```bash
docker run -d --name api -p 8080:8080 myapp:1.0
#            ↑ detach    ↑ host:container
```

### Environment

```bash
docker run -e SPRING_PROFILES_ACTIVE=docker \
           -e JAVA_OPTS="-XX:MaxRAMPercentage=75" \
           myapp:1.0
```

### Volumes

```bash
docker run -v $(pwd)/config:/app/config:ro myapp:1.0
```

### Resource Limits

```bash
docker run --memory=512m --cpus=1.5 myapp:1.0
```

### Network

```bash
docker run --network mynet myapp:1.0
```

### User & Read-Only

```bash
docker run --user 1000:1000 --read-only \
           --tmpfs /tmp:rw,noexec,nosuid,size=100m \
           myapp:1.0
```

### Init (zombie reaping — optional)

```bash
docker run --init myapp:1.0
```

### Remove After Exit

```bash
docker run --rm myapp:1.0 --version
```

---

## Common Pitfalls

1. **`EXPOSE` = port open on host** — nahi; `-p` chahiye.
2. **Huge build context** — `.dockerignore` na hone se slow build + secrets leak risk.
3. **COPY . . pehle** — cache barbaad.
4. **Shell form CMD** — SIGTERM handling messy for Java.
5. **`latest` tag production** — reproducibility zero; semver digest use karo.
6. **Secrets in Dockerfile ARG/ENV** — layers mein leak ho sakta hai; BuildKit secrets ya runtime injection.
7. **Alpine + glibc assumptions** — kuch native libs break; test karo.
8. **Multi-stage stage name typo** — `--from=build` mismatch → build fail.

---

## Summary Cheat Sheet

| Instruction | Build vs Run | Notes |
|-------------|--------------|-------|
| `FROM` | Build | Base / stage |
| `RUN` | Build | New layer |
| `COPY` | Build | Prefer over ADD |
| `WORKDIR` | Build | cd equivalent |
| `ENV` | Both | Runtime env |
| `ARG` | Build | `--build-arg` |
| `EXPOSE` | Docs | Map with `-p` |
| `USER` | Both | Security |
| `ENTRYPOINT` | Run default | Exec form |
| `CMD` | Run default | Args / command |
| `HEALTHCHECK` | Run | Orchestrator friendly |

| Command | Use |
|---------|-----|
| `docker build -t tag .` | Build image |
| `docker run -dp 8080:8080 name` | Run detached + publish |
| `docker logs -f name` | Logs |
| `docker exec -it name sh` | Shell inside |

---

## Practice

1. `hello-world` image run karke layers samjho.
2. Ek Dockerfile likho jo intentionally cache break karti hai — order swap karke rebuild time compare karo.
3. Multi-stage build: Maven stage + slim JRE final.
4. `.dockerignore` add karke build context size dekho (`docker build --progress=plain`).
5. `docker run` with `--read-only` + `--tmpfs /tmp` try karo.
6. `docker history` se layer list dekho — kaunsi instruction kitni fat hai.

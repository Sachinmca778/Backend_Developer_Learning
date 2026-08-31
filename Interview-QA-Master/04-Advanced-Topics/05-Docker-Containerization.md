# Docker & Containerization - Top 10 Interview Questions & Answers

> Covering: Docker fundamentals, Image vs Container, Dockerfile, Multi-Stage Builds, Spring Boot + Docker, Docker Compose, and Container Best Practices

---

## Q1: Docker kya hai aur Image vs Container mein kya difference hai?

**Answer:**

Docker app ko **image** (immutable template) mein pack karke **container** (running isolated process) mein chalata hai — same behaviour har jagah dev/staging/prod.

### Mental Model:

```
Dockerfile ──build──▶ Image ──run──▶ Container(s)
                    (read-only       (writable thin layer
                     layers)          + process namespace)
```

### Image vs Container:

| | Image | Container |
|--|-------|-----------|
| **Kya hai** | Snapshot / blueprint | Running instance |
| **Mutability** | Immutable (versioned tag) | Writable top layer |
| **Count** | Ek image | Zero ya zyada containers |
| **Lifecycle** | Build once | create → start → stop → remove |

### Analogy:

```
Image     = movie ki Blu-ray disc (same content har player par)
Container = abhi play ho rahi screening (ek running session)
```

### Benefits:

| Benefit | Explain |
|---------|---------|
| **Consistency** | "Mere laptop par chalta hai" problem khatam |
| **Isolation** | Dependencies clash nahi |
| **Portability** | Almost everywhere chalta hai |

**Short Interview Answer:**

Docker apps ko image mein pack karta hai jo container ke roop mein run hote hain. Image = immutable blueprint (Blu-ray disc), Container = uska running instance (screening). Ek image se multiple containers ban sakte hain.

---

## Q2: Dockerfile ki main instructions kaunsi hain?

**Answer:**

| Instruction | Build vs Run | Purpose |
|-------------|--------------|---------|
| `FROM` | Build | Base image |
| `RUN` | Build | Command execute + new layer |
| `COPY` | Build | Host → image files |
| `WORKDIR` | Build | `cd` equivalent |
| `ENV` | Both | Runtime env variables |
| `ARG` | Build | Build-time only (`--build-arg`) |
| `EXPOSE` | Docs | Port document (publish nahi) |
| `USER` | Both | Security — non-root |
| `ENTRYPOINT` | Run | Fixed executable |
| `CMD` | Run | Default arguments |
| `HEALTHCHECK` | Run | Health probe |

### Example:

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Important:**

`EXPOSE` sirf documentation hai — port host par open nahi karta. Actual mapping `docker run -p 8080:8080` se hoti hai. `ARG` build-time only hai, runtime par available nahi (unless `ENV` mein copy karo).

---

## Q3: COPY vs ADD, aur ENTRYPOINT vs CMD mein kya difference hai?

**Answer:**

### COPY vs ADD:

| Feature | COPY | ADD |
|---------|------|-----|
| Files/dirs copy | ✅ | ✅ |
| Tar auto-extract | ❌ | ⚠️ |
| Remote URL | ❌ | ⚠️ (surprising) |

→ **COPY prefer karo** — predictable. Remote/extract chahiye to explicit `RUN wget`/`curl`.

### ENTRYPOINT vs CMD:

- **ENTRYPOINT** = fixed executable (container ka main purpose)
- **CMD** = default arguments ENTRYPOINT ko, ya standalone command agar ENTRYPOINT na ho

```dockerfile
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
CMD []   # optional extra args (compose override se)
```

`docker run myimg --server.port=9090` → args jar ko jaate hain (kyunki ENTRYPOINT `java -jar` hai).

### Exec Form vs Shell Form:

```dockerfile
CMD ["java", "-jar", "app.jar"]   # ✅ exec form — PID 1 = java, SIGTERM sahi milta hai
CMD java -jar app.jar             # ❌ shell form — /bin/sh -c wrapper PID 1
```

**Short Interview Answer:**

COPY predictable hai, ADD ke pas magic (tar extract, URL) hai jo surprises deta hai — COPY prefer karo. ENTRYPOINT fixed executable, CMD default args. Exec form (JSON array) use karo taaki SIGTERM Java tak sahi pahunche graceful shutdown ke liye.

---

## Q4: Docker image layers aur build cache kaise kaam karta hai?

**Answer:**

Har instruction jo filesystem change karti hai → usually **naya layer** banta hai. Docker layers ko cache karta hai.

### Cache Invalidation:

Docker **top se bottom** cache match karta hai. Pehli mismatch ke baad sab neeche rebuild.

```dockerfile
COPY pom.xml .
RUN mvn dependency:go-offline   # pom change → yahan break → neeche sab dubara

COPY src ./src                  # sirf code change → dependencies cache hit
RUN mvn package -DskipTests
```

### Order Matters:

| ❌ Bad | ✅ Good |
|--------|---------|
| Pehle `COPY . .` phir `RUN mvn` | Pehle dependency files, phir source |
| Har baar full context invalid | Stable layers upar |

### Inspect:

```bash
docker history myapp:latest      # layer sizes dekho
```

**Important:**

Cache optimization ke liye: pehle dependency files (`pom.xml`) copy karo aur dependencies download karo, phir source code copy karo. Isse code change par sirf app layer rebuild hoti hai, dependencies cache se aati hain. `.dockerignore` se build context chhota rakho.

---

## Q5: Multi-stage build kya hai aur kyun use karte hain?

**Answer:**

Multi-stage build mein **ek Dockerfile mein multiple `FROM`** hote hain — final image mein sirf zaroori artifacts jaate hain (build tools nahi).

### Example:

```dockerfile
# ---------- build stage ----------
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /src/target/*.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### Fayda:

| Stage | Contains | Size |
|-------|----------|------|
| Build image | JDK + Maven + source | **Fat** (~500MB+) |
| Final image | Sirf JRE + jar | **Slim** (~200MB) |

**Short Interview Answer:**

Multi-stage build mein ek stage code compile/build karti hai (JDK + Maven), aur final stage sirf JRE + jar leti hai `COPY --from=build` se. Result: chhoti, secure image bina build tools ke. Size aur attack surface dono kam.

---

## Q6: Spring Boot app ko Docker image kaise banate hain? Kaunse tareeke hain?

**Answer:**

Teen common raaste:

| Approach | Command | Control |
|----------|---------|---------|
| **Dockerfile** | `docker build -t app .` | Full |
| **Buildpacks** | `./mvnw spring-boot:build-image` | Medium (no Dockerfile) |
| **Jib** | `./mvnw compile jib:build` | Medium-high (no daemon) |

### 1. Dockerfile (Multi-stage) — full control.

### 2. Spring Boot Buildpacks:

```bash
./mvnw spring-boot:build-image \
  -Dspring-boot.build-image.imageName=myorg/myapp:1.0.0
```

→ Dockerfile likhne ki zaroorat nahi (Cloud Native Buildpacks / Paketo).

### 3. Google Jib:

```bash
./mvnw compile jib:build          # push to registry (no Docker daemon)
./mvnw compile jib:dockerBuild    # local Docker daemon
```

### Layered JAR (speed-up):

```xml
<configuration>
  <layers><enabled>true</enabled></layers>
</configuration>
```

→ Dependencies alag layer (stable), app code alag layer — code change par sirf app layer rebuild.

**Important:**

Choti teams ke liye Buildpack ya Jib easy hai (Dockerfile maintain nahi karna). Complex OS needs ke liye Dockerfile. Production mein hamesha non-root user, layered jars, aur JVM container flags use karo.

---

## Q7: Container mein JVM memory kaise configure karte hain?

**Answer:**

Purane JVMs container memory limits ko ignore karte the (host ki poori RAM dekhte the) → OOMKilled. Modern JVMs container-aware hain.

### Essential Flags:

```bash
JAVA_OPTS="-XX:+UseContainerSupport \
           -XX:MaxRAMPercentage=75.0 \
           -XX:InitialRAMPercentage=50.0 \
           -XX:+ExitOnOutOfMemoryError \
           -XX:+HeapDumpOnOutOfMemoryError"
```

| Flag | Purpose |
|------|---------|
| `-XX:+UseContainerSupport` | cgroup limits respect (default modern JDK) |
| `-XX:MaxRAMPercentage=75` | Heap = 75% of container memory |
| `-XX:+ExitOnOutOfMemoryError` | OOM par fail fast (orchestrator restart) |

### Docker Run Limits:

```bash
docker run --memory=512m --cpus=1.5 myapp:1.0
```

**Short Interview Answer:**

`-XX:+UseContainerSupport` (modern JDK default) JVM ko cgroup memory limits dikhata hai. `-XX:MaxRAMPercentage=75` heap ko container memory ka 75% deta hai — 100% mat do kyunki metaspace, threads, direct buffers bhi RAM khate hain. `docker run --memory` se hard limit set karo.

---

## Q8: Docker Compose kya hai aur kab use karte hain?

**Answer:**

Docker Compose ek **YAML file** se **multiple containers** define karta hai (networking, volumes, env) — local dev/demo env ek command se up.

### Commands (V2):

```bash
docker compose up -d          # detached start
docker compose up --build     # rebuild images
docker compose down           # stop + remove
docker compose down -v        # + volumes delete (careful!)
docker compose logs -f api
```

### Example:

```yaml
services:
  api:
    build:
      context: .
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/app
    depends_on:
      db:
        condition: service_healthy

  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: app
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      retries: 5

volumes:
  pgdata:
```

**Important:**

`depends_on` sirf container **start order** control karta hai, readiness nahi. Actual "wait until healthy" ke liye `condition: service_healthy` + `healthcheck` use karo. Compose local dev ke liye best hai, production orchestration ke liye Kubernetes.

---

## Q9: Container security best practices kya hain?

**Answer:**

Teen pillars: **Least privilege + Immutable runtime + Bounded resources**.

### 1. Non-Root User:

```dockerfile
RUN addgroup -S app && adduser -S app -G app
USER app
```

→ Root container breakout ka impact zyada dangerous hota hai.

### 2. Read-Only Root Filesystem:

```bash
docker run --read-only \
  --tmpfs /tmp:rw,noexec,nosuid,size=100m \
  myapp:1.0
```

### 3. Resource Limits:

```bash
docker run --memory=512m --cpus=1.5 myapp:1.0
```

### 4. Minimal Base Images:

- `eclipse-temurin:21-jre-alpine` (chhoti, kam CVEs)
- Distroless images (no shell)

### 5. Secrets Management:

```bash
# ❌ Plain env in prod
docker run -e DB_PASSWORD=secret ...

# ✅ File-based secrets
docker run -v /run/secrets/db_pw:/run/secrets/db_pw myapp
```

→ Dockerfile ARG/ENV mein secrets mat daalo — layers mein leak ho sakte hain.

**Short Interview Answer:**

Non-root user chalao, read-only root FS + tmpfs, resource limits (`--memory`, `--cpus`), minimal base images (alpine/distroless), aur secrets ko file/vault se inject karo (Dockerfile mein hardcode nahi). Image scanning bhi CI mein karo.

---

## Q10: Container mein graceful shutdown kaise handle karte hain?

**Answer:**

Jab orchestrator container stop karta hai, wo **SIGTERM** bhejta hai, phir grace period ke baad **SIGKILL**. App ko SIGTERM par in-flight requests complete karke clean shutdown karna chahiye.

### PID 1 Problem:

```dockerfile
# ❌ Shell form — /bin/sh PID 1 hai, SIGTERM java tak nahi pahunchta
ENTRYPOINT sh -c "java -jar app.jar"

# ✅ exec — java PID 1 ban jaata hai, SIGTERM sahi milta hai
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

# ✅ Ya direct exec form
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### Spring Boot Graceful Shutdown:

```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

### Zombie Reaping:

```bash
docker run --init myapp:1.0   # tini as PID 1
```

**Important:**

Sabse common mistake: shell form ENTRYPOINT jisse `/bin/sh` PID 1 ban jaata hai aur SIGTERM Java tak nahi pahunchta → forced SIGKILL, requests drop. `exec java` ya exec form use karo. Spring Boot mein `server.shutdown: graceful` set karo taaki in-flight requests complete ho.

**Short Interview Answer:**

Orchestrator SIGTERM bhejta hai, phir SIGKILL. Java ko PID 1 banao (`exec java` ya exec form ENTRYPOINT) taaki SIGTERM mile. Spring Boot mein `server.shutdown: graceful` + timeout set karo. `--init` se zombie processes reap hote hain.

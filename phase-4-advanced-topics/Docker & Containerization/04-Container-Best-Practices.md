# Container Best Practices

## Status: Not Started

---

## Table of Contents

1. [Mindset](#mindset)
2. [Non-Root User](#non-root-user)
3. [Read-Only Root Filesystem](#read-only-root-filesystem)
4. [Resource Limits — Memory & CPU](#resource-limits--memory--cpu)
5. [Health vs Readiness](#health-vs-readiness)
6. [Graceful Shutdown — SIGTERM](#graceful-shutdown--sigterm)
7. [Minimal Base Images](#minimal-base-images)
8. [Secrets Management](#secrets-management)
9. [Capabilities & Privileges](#capabilities--privileges)
10. [Image Scanning & Supply Chain](#image-scanning--supply-chain)
11. [Kubernetes Mapping](#kubernetes-mapping)
12. [Common Pitfalls](#common-pitfalls)
13. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Mindset

Production containers = **small attack surface + predictable failures + observable health**.

Teen pillars:

1. **Least privilege** — root mat chalao
2. **Immutable-ish runtime** — read-only FS jahan ho sake
3. **Bounded resources** — OOMKilled surprises kam

---

## Non-Root User

### Kyon?

Root container breakout impact **zyada dangerous** hota hai — CVE scenarios mein attacker ko host-like powers mil sakti hain.

### Dockerfile Pattern

```dockerfile
RUN addgroup -S app && adduser -S app -G app
USER app
```

Numeric UID/GID bhi common (enterprise policy):

```dockerfile
RUN addgroup -g 10001 app && adduser -u 10001 -G app -s /bin/false -D app
USER 10001:10001
```

### docker run

```bash
docker run --user 10001:10001 myapp:1.0
```

### Kubernetes Snippet

```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 10001
  fsGroup: 10001
```

### Filesystem Permissions

`/app` writable chahiye ho to:

```dockerfile
RUN mkdir -p /app/tmp && chown -R app:app /app
```

---

## Read-Only Root Filesystem

### Kyon?

Malware / compromised process ko **write** karna mushkil — drift kam.

### docker run

```bash
docker run --read-only \
  --tmpfs /tmp:rw,noexec,nosuid,size=100m \
  -v app-logs:/var/log/app \
  myapp:1.0
```

### Writable Paths Strategy

| Path | Why writable |
|------|----------------|
| `/tmp` | temp files |
| `/run` | pid files sometimes |
| Volume mounts | logs, heap dumps, uploads |

Spring Boot agar `/tmp` par Tomcat extract karta ho — ensure tmpfs ya writable dir.

### Compose Example

```yaml
services:
  api:
    image: myapp:1.0
    read_only: true
    tmpfs:
      - /tmp:rw,noexec,nosuid,size=100m
    volumes:
      - logs:/var/log/app
```

---

## Resource Limits — Memory & CPU

### Kyon?

Noisy neighbour + node stability — **limits** se cgroup enforce hota hai.

### docker run

```bash
docker run --memory=512m --memory-swap=512m \
           --cpus=1.5 \
           myapp:1.0
```

### Compose

```yaml
services:
  api:
    image: myapp:1.0
    deploy:
      resources:
        limits:
          cpus: "1.5"
          memory: 512M
        reservations:
          cpus: "0.25"
          memory: 256M
```

**Note:** `deploy.resources` historically Swarm-oriented; newer Compose implementations partially support for local — verify your Docker Desktop version. Kubernetes mein `resources` standard.

### JVM + cgroup

`-XX:MaxRAMPercentage=75` ke saath bhi **heap ≠ container memory** — non-heap bhi hai → cross-ref Performance folder.

---

## Health vs Readiness

### Liveness / Health

"Process **zinda** hai?" — hung/deadlock detect.

### Readiness

"Traffic **accept** kar sakta hai?" — DB down ho to ready=false, traffic band.

### Spring Boot Actuator

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true
```

Endpoints:

- `/actuator/health/liveness`
- `/actuator/health/readiness`

### Dockerfile HEALTHCHECK (container-level)

Coarse signal — orchestrator probes preferred.

### Kubernetes

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 5
```

---

## Graceful Shutdown — SIGTERM

### Flow

1. Orchestrator **SIGTERM** bhejta hai.
2. App **connections drain** + **in-flight requests** complete (within timeout).
3. Phir **SIGKILL** — hard kill.

### Spring Boot

```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

### Kubernetes `terminationGracePeriodSeconds`

```yaml
spec:
  terminationGracePeriodSeconds: 60
```

### PID 1 Issue — Fix

Dockerfile:

```dockerfile
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
```

`exec` → Java PID 1 → signals seedha JVM.

### Reactive / Tomcat

Ensure connector shutdown timeouts aligned — load balancers stop sending new traffic when readiness fails **before** SIGTERM ideally (preStop hook).

### Optional — `docker run --init`

Tiny init reaps zombies — mostly non-Java concern but useful for multi-process images.

---

## Minimal Base Images

| Image | Trade-off |
|-------|-----------|
| `eclipse-temurin:*-jre-alpine` | Small; musl vs glibc issues rare but possible |
| `eclipse-temurin:*-jre` (Debian slim) | Medium; fewer musl surprises |
| **`gcr.io/distroless/java21-debian12`** | Ultra minimal; **no shell** — debugging harder |
| Chainguard / Wolfi images | Security-focused maintained bases |

**Distroless example note:** Often combined with Jib/Buildpacks — awesome for prod, painful for `docker exec bash` debugging.

---

## Secrets Management

### Maturity Levels

| Level | Approach | Risk |
|-------|----------|------|
| ❌ Bad | `ENV DB_PASSWORD=secret` in Dockerfile | Permanent in layers |
| ⚠️ OK dev | `docker run -e` / compose env_file gitignored | Local leaks |
| ✅ Better | **Docker secrets** (Swarm) / **K8s Secret** + volume mount | Central rotation |
| ✅ Better | **External vault** (Vault, AWS SM) + IAM | Best |

### Build-Time Secrets

Docker BuildKit:

```dockerfile
# syntax=docker/dockerfile:1
RUN --mount=type=secret,id=mvn_token mvn -s /run/secrets/mvn_token package
```

```bash
DOCKER_BUILDKIT=1 docker build --secret id=mvn_token,src=.m2/settings.xml .
```

### Runtime

- Mount file at `/run/secrets/db_password`
- Spring read via config (`spring.datasource.password=${file:...}` patterns — verify Boot version docs)

**Rule:** **Never** bake prod secrets into image.

---

## Capabilities & Privileges

### Drop Linux capabilities

Kubernetes:

```yaml
securityContext:
  capabilities:
    drop:
      - ALL
```

### No privileged

```yaml
securityContext:
  privileged: false
  allowPrivilegeEscalation: false
```

---

## Image Scanning & Supply Chain

```bash
docker scout cves myapp:1.0
# or trivy image myapp:1.0
```

- Regular base image updates
- Pin tags + digest for reproducibility:

```yaml
image: myapp@sha256:abcdef...
```

---

## Kubernetes Mapping

| Docker practice | Kubernetes field |
|-----------------|------------------|
| `--user` | `securityContext.runAsUser` |
| `--read-only` | `securityContext.readOnlyRootFilesystem` |
| `--memory` / `--cpus` | `resources.limits` |
| `HEALTHCHECK` | `livenessProbe` / `readinessProbe` |
| `docker compose service DNS` | `Service` + cluster DNS |

---

## Common Pitfalls

1. **Root + latest tag** — compliance fail.
2. **Read-only without tmpfs** — Boot crash writing temp files.
3. **Graceful shutdown 0s** — connections cut mid-request.
4. **Only liveness, no readiness** — traffic DB-down pod par jaati rahe.
5. **Heap = container memory** — OOMKilled despite "75% heap".
6. **Secrets in env in screenshots/logs** — structured logging careful.
7. **Distroless debugging** — ensure observability outside shell (logs, traces).

---

## Summary Cheat Sheet

| Practice | Tooling |
|----------|---------|
| Non-root | `USER`, K8s `runAsNonRoot` |
| Read-only FS | `--read-only`, tmpfs, volumes |
| Limits | `--memory`, `--cpus`, K8s resources |
| Health | Actuator probes + K8s probes |
| Graceful stop | `server.shutdown=graceful`, `exec java`, termination grace |
| Small image | Alpine / distroless / Jib layers |
| Secrets | mounts / vault — not Dockerfile |

| Signal | Meaning |
|--------|---------|
| SIGTERM | Please shutdown gracefully |
| SIGKILL | Force stop |

---

## Practice

1. Container ko non-root run karke file permission issue reproduce + fix karo.
2. `--read-only` + tmpfs ke saath Boot app chalao.
3. `docker stats` se limits observe karo under load.
4. Actuator readiness ko DB down karke test karo (should go unhealthy).
5. `kubectl describe pod` style thinking — map Docker flags to K8s manifest mentally.

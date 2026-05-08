# Spring Boot + Docker

## Status: Not Started

---

## Table of Contents

1. [Goal Kya Hai?](#goal-kya-hai)
2. [Classic Dockerfile (JRE + Fat Jar)](#classic-dockerfile-jre--fat-jar)
3. [Layered JAR (Optional Speed-Up)](#layered-jar-optional-speed-up)
4. [JVM Flags in Containers](#jvm-flags-in-containers)
5. [Spring Boot Buildpacks — build-image](#spring-boot-buildpacks--build-image)
6. [Google Jib](#google-jib)
7. [Dockerfile vs Buildpack vs Jib](#dockerfile-vs-buildpack-vs-jib)
8. [docker run + Environment](#docker-run--environment)
9. [Profiles & Config](#profiles--config)
10. [Common Pitfalls](#common-pitfalls)
11. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Goal Kya Hai?

Spring Boot app ko **OCI/Docker image** mein pack karna taaki:

- Same artefact dev/stage/prod par chale
- JVM containers ke memory limits ko respect kare
- ideally **non-root**, minimal surface

Teen common raaste:

1. **Dockerfile** (full control)
2. **Spring Boot Buildpacks** (`spring-boot:build-image`)
3. **Jib** (Maven/Gradle plugin — daemon optional)

---

## Classic Dockerfile (JRE + Fat Jar)

### Approach

Pehle `mvn package` (CI ya multi-stage build ke andar), phir sirf **JRE + jar** final image mein.

### Example — Multi-stage + Temurin Alpine + Non-root

```dockerfile
# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -e -B dependency:go-offline
COPY src ./src
RUN mvn -q -e -B package -DskipTests

# ---------- runtime ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /workspace/target/*.jar /app/app.jar
RUN chown spring:spring /app/app.jar

USER spring

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 \
    -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/tmp/heapdump.hprof"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
```

### Kyon `exec java`?

`exec` shell ko replace karke **PID 1 = java** banata hai — **SIGTERM** graceful shutdown ke liye better (detail: `04-Container-Best-Practices.md`).

### Build & Run

```bash
docker build -t order-service:1.0.0 .
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  order-service:1.0.0
```

---

## Layered JAR (Optional Speed-Up)

Spring Boot **layers** extract karke Docker layers optimize kar sakte ho — frequently changing code alag layer, dependencies stable layer.

### `pom.xml`

```xml
<plugin>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-maven-plugin</artifactId>
  <configuration>
    <layers>
      <enabled>true</enabled>
    </layers>
  </configuration>
</plugin>
```

### Dockerfile Pattern (extract layers)

```dockerfile
FROM eclipse-temurin:21-jre-alpine AS layers
WORKDIR /extracted
COPY target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=layers /extracted/extracted/dependencies/ ./
COPY --from=layers /extracted/extracted/spring-boot-loader/ ./
COPY --from=layers /extracted/extracted/snapshot-dependencies/ ./
COPY --from=layers /extracted/extracted/application/ ./

RUN addgroup -S spring && adduser -S spring -G spring && chown -R spring:spring /app
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

**Fayda:** Sirf app code change par rebuild → dependency layers cache hit.

---

## JVM Flags in Containers

Boot apps ko container limits ke hisaab se:

```bash
JAVA_OPTS="-XX:+UseContainerSupport \
           -XX:MaxRAMPercentage=75.0 \
           -XX:InitialRAMPercentage=50.0"
```

→ Cross-ref: `Performance & Optimization/01-JVM-Performance-Tuning.md`.

**Tip:** heap = sab kuch nahi — metaspace, threads, direct buffers bhi RAM khate hain; isliye **100% mat do**.

---

## Spring Boot Buildpacks — build-image

**Matlab:** Cloud Native Buildpacks (often **Paketo**) Docker image banate hain — **Dockerfile likhne ki zaroorat nahi** (unless customize karna ho).

### Maven

```bash
./mvnw spring-boot:build-image \
  -Dspring-boot.build-image.imageName=docker.io/myorg/myapp:1.0.0
```

### Gradle

```bash
./gradlew bootBuildImage --imageName=docker.io/myorg/myapp:1.0.0
```

### `pom.xml` — Image Name + JVM Options

```xml
<plugin>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-maven-plugin</artifactId>
  <configuration>
    <image>
      <name>docker.io/myorg/${project.artifactId}:${project.version}</name>
      <env>
        <BPE_DELIM_JAVA_TOOL_OPTIONS xml:space="preserve"> </BPE_DELIM_JAVA_TOOL_OPTIONS>
        <BPE_APPEND_JAVA_TOOL_OPTIONS>-XX:MaxRAMPercentage=75</BPE_APPEND_JAVA_TOOL_OPTIONS>
      </env>
    </image>
  </configuration>
</plugin>
```

### Requirements

- Docker daemon **ya** compatible builder (CI agents pe Docker socket).

### Pros / Cons

| ✅ | ❌ |
|----|-----|
| Maintain Dockerfile kam | Fine-grained OS tweaks limited |
| Security patches buildpack side | Build slower first time |
| SBOM / reproducibility trend | Corporate mirror config kabhi tricky |

---

## Google Jib

**Matlab:** Maven/Gradle plugin jo **Docker daemon ke bina** (optional) **OCI image** push kar sakta hai — layers optimized, reproducible.

### Maven `pom.xml`

```xml
<plugin>
  <groupId>com.google.cloud.tools</groupId>
  <artifactId>jib-maven-plugin</artifactId>
  <version>3.4.5</version>
  <configuration>
    <from>
      <image>eclipse-temurin:21-jre-alpine</image>
    </from>
    <to>
      <image>registry.example.com/myorg/myapp:${project.version}</image>
    </to>
    <container>
      <mainClass>com.example.Application</mainClass>
      <ports>
        <port>8080</port>
      </ports>
      <environment>
        <JAVA_TOOL_OPTIONS>-XX:MaxRAMPercentage=75</JAVA_TOOL_OPTIONS>
      </environment>
      <user>1000:1000</user>
    </container>
  </configuration>
</plugin>
```

### Commands

```bash
./mvnw compile jib:build          # push to registry (needs auth)
./mvnw compile jib:dockerBuild    # load into local Docker daemon
```

### Pros / Cons

| ✅ | ❌ |
|----|-----|
| CI friendly (no Docker socket sometimes) | Debugging base image quirks |
| Layer caching smart | Advanced runtime customization DSL |

---

## Dockerfile vs Buildpack vs Jib

| Aspect | Dockerfile | `build-image` | Jib |
|--------|------------|---------------|-----|
| Control | Full | Medium | Medium-high |
| Dockerfile maintain | Yes | No | No |
| Docker daemon | Build par | Usually yes | Optional (`build`) |
| Learning curve | Docker | Boot plugin | Jib config |
| Corporate policy fit | Easy to audit | Good | Good |

**Practical:** Choti teams → **Buildpack** ya **Jib**. Complex OS/agent needs → **Dockerfile**.

---

## docker run + Environment

### Basic

```bash
docker run -d --name api -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SERVER_PORT=8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/app \
  -e SPRING_DATASOURCE_USERNAME=app \
  -e SPRING_DATASOURCE_PASSWORD_FILE=/run/secrets/db_password \
  myapp:1.0.0
```

### Env File

```bash
docker run --env-file ./docker.env myapp:1.0.0
```

`docker.env`:

```
SPRING_PROFILES_ACTIVE=docker
JAVA_OPTS=-XX:MaxRAMPercentage=75
```

### Secrets — Files (better than plain `-e` in prod)

Docker compose / Swarm / K8s secrets mount karke:

```properties
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD_FILE:file:/run/secrets/db_password}
```

Boot 2.4+ mein `spring.config.import` patterns bhi use ho sakte hain — team convention follow karo.

---

## Profiles & Config

### Typical Setup

```
application.yml              # defaults
application-docker.yml       # docker profile overrides
```

Activate:

```bash
-e SPRING_PROFILES_ACTIVE=docker
```

### Health Endpoint

Actuator enable karo taaki **HEALTHCHECK** / K8s probes kaam karen:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      probes:
        enabled: true   # K8s-style groups (Boot 2.3+)
```

---

## Common Pitfalls

1. **Fat jar + wrong working dir** — relative paths break; `WORKDIR` clear rakho.
2. **PID 1 issue** — `java` ko SIGTERM na mile; `exec java` use karo.
3. **Debug port expose** — prod image mein accidentally `--debug`; mat rakho.
4. **Build context mein secrets** — `.env` copy na karo; BuildKit secret ya runtime mount.
5. **Old JRE** — LTS track follow karo (17/21).
6. **Buildpack behind corporate proxy** — pull failures; mirror configure karo.
7. **Jib user mapping** — filesystem permission issues; numeric user verify karo.

---

## Summary Cheat Sheet

| Approach | Command |
|----------|---------|
| Dockerfile | `docker build -t app .` |
| Buildpack | `./mvnw spring-boot:build-image` |
| Jib push | `./mvnw compile jib:build` |
| Jib local | `./mvnw compile jib:dockerBuild` |

| Env Var | Use |
|---------|-----|
| `SPRING_PROFILES_ACTIVE` | Profile |
| `JAVA_OPTS` / `JAVA_TOOL_OPTIONS` | JVM flags |
| `SERVER_PORT` | Port |

| Must-have JVM (containers) | Flag |
|---------------------------|------|
| cgroup aware | `-XX:+UseContainerSupport` (default modern JDK) |
| Heap cap | `-XX:MaxRAMPercentage=75` |

---

## Practice

1. Multi-stage Dockerfile se Boot app build + run karo.
2. Same app `spring-boot:build-image` se banao — size compare karo.
3. Jib se registry push (ya local `dockerBuild`).
4. `JAVA_OPTS` change karke container memory behaviour dekho (`docker stats`).
5. Actuator health par `HEALTHCHECK` lagao Dockerfile mein.

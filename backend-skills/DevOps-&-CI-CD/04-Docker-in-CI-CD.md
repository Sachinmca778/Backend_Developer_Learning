# Docker in CI/CD

## Status: Not Started

---

## Table of Contents

1. [Why Docker in CI/CD?](#why-docker-in-cicd)
2. [Building Images in CI](#building-images-in-ci)
3. [Image Tagging Strategy](#image-tagging-strategy)
4. [Pushing to Registry](#pushing-to-registry)
5. [Multi-Platform Builds (ARM + AMD64)](#multi-platform-builds-arm--amd64)
6. [Image Scanning](#image-scanning)
7. [Layer Caching in CI](#layer-caching-in-ci)
8. [Best Practices](#best-practices)
9. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Why Docker in CI/CD?

**Matlab:** Reproducible **artifact** that runs identically dev → CI → staging → prod. "Build once, run anywhere".

### Benefits

- **Same artifact** across environments (no "works on my machine")
- **Immutable** — image tagged with SHA can't change
- **Fast deploys** — pull image vs build per env
- **Rollback** — point at older tag

---

## Building Images in CI

### Basic Dockerfile (Spring Boot)

```dockerfile
# ---- builder stage ----
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw -B dependency:go-offline
COPY src ./src
RUN ./mvnw -B package -DskipTests

# ---- runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

**Multi-stage build** — final image only has runtime + JAR (no build tools, smaller).

### Build in CI

```yaml
- uses: docker/setup-buildx-action@v3

- uses: docker/build-push-action@v6
  with:
    context: .
    push: true
    tags: ghcr.io/myorg/myapp:${{ github.sha }}
```

---

## Image Tagging Strategy

**Matlab:** Right tags = right immutability + traceability.

### Common Tag Patterns

| Tag | Use |
|-----|-----|
| `commit-sha` (e.g., `abc1234`) | **Immutable** — best for prod |
| `1.2.3` (SemVer) | Releases |
| `1.2` / `1` | Major/minor track (auto-update on patch) |
| `latest` | "Most recent" (avoid for prod!) |
| `main` / `develop` | Branch tip |
| `pr-123` | PR preview env |

### Recommended for Production

```
ghcr.io/myorg/myapp:abc1234567890       # always immutable
ghcr.io/myorg/myapp:1.2.3                # human-friendly
ghcr.io/myorg/myapp:1.2                  # tracking
```

**Avoid using `latest` in production manifests** — what `latest` points to changes silently → non-reproducible deploys.

### Tagging in GitHub Actions

```yaml
- uses: docker/metadata-action@v5
  id: meta
  with:
    images: ghcr.io/${{ github.repository }}
    tags: |
      type=sha
      type=ref,event=branch
      type=semver,pattern={{version}}
      type=semver,pattern={{major}}.{{minor}}

- uses: docker/build-push-action@v6
  with:
    push: true
    tags: ${{ steps.meta.outputs.tags }}
    labels: ${{ steps.meta.outputs.labels }}
```

---

## Pushing to Registry

### Common Registries

| Registry | Notes |
|----------|-------|
| **Docker Hub** | Public, free public repos, rate limits |
| **GitHub Container Registry (ghcr.io)** | GitHub-integrated, free for public |
| **AWS ECR** | AWS-native, OIDC-friendly |
| **Google Artifact Registry (GAR)** | GCP-native |
| **Azure Container Registry (ACR)** | Azure-native |
| **Quay (Red Hat)** | Vulnerability scanning built-in |
| **Self-hosted** | Harbor, Nexus |

### Login Examples

#### GHCR

```yaml
- uses: docker/login-action@v3
  with:
    registry: ghcr.io
    username: ${{ github.actor }}
    password: ${{ secrets.GITHUB_TOKEN }}
```

#### AWS ECR (OIDC)

```yaml
- uses: aws-actions/configure-aws-credentials@v4
  with:
    role-to-assume: arn:aws:iam::123:role/ci
    aws-region: ap-south-1

- uses: aws-actions/amazon-ecr-login@v2
```

#### Docker Hub

```yaml
- uses: docker/login-action@v3
  with:
    username: ${{ secrets.DOCKERHUB_USERNAME }}
    password: ${{ secrets.DOCKERHUB_TOKEN }}
```

### Manual Push

```bash
docker tag myapp:local myregistry.com/myapp:1.2.3
docker push myregistry.com/myapp:1.2.3
```

---

## Multi-Platform Builds (ARM + AMD64)

**Matlab:** Ek image jo ARM (Apple Silicon, AWS Graviton) + AMD64 (Intel) dono par chale.

### Why?

- Apple M-series Mac developers (ARM)
- AWS Graviton instances (ARM, cheaper)
- Mixed prod fleets

### `docker buildx` for Multi-Arch

```bash
docker buildx create --use
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t myorg/myapp:1.2.3 \
  --push .
```

### In GitHub Actions

```yaml
- uses: docker/setup-qemu-action@v3        # emulation for cross-platform
- uses: docker/setup-buildx-action@v3

- uses: docker/build-push-action@v6
  with:
    context: .
    platforms: linux/amd64,linux/arm64
    push: true
    tags: ghcr.io/myorg/myapp:${{ github.sha }}
```

⚠️ **Slow** — emulated builds 2-5x slower. Solutions:
- Native ARM runners (GitHub now offers `ubuntu-24.04-arm`)
- Build per platform on respective runners + manifest

---

## Image Scanning

**Matlab:** Image mein **vulnerabilities** detect karo before deploy.

### Tools

| Tool | Type |
|------|------|
| **Trivy** (Aqua) | OSS, fast, comprehensive |
| **Snyk** | SaaS, deep DB |
| **Grype** (Anchore) | OSS |
| **Docker Scout** | Built into Docker Desktop |
| **Clair** (Red Hat) | OSS, in Quay |
| **AWS ECR scanning** | Built into ECR |

### Trivy in GitHub Actions

```yaml
- name: Run Trivy
  uses: aquasecurity/trivy-action@master
  with:
    image-ref: ghcr.io/myorg/myapp:${{ github.sha }}
    format: 'sarif'
    output: 'trivy.sarif'
    severity: 'CRITICAL,HIGH'
    exit-code: '1'   # fail build on findings

- uses: github/codeql-action/upload-sarif@v3
  if: always()
  with:
    sarif_file: trivy.sarif
```

### CLI

```bash
trivy image myorg/myapp:1.2.3
trivy image --severity HIGH,CRITICAL --exit-code 1 myorg/myapp:1.2.3
```

### What Gets Scanned?

- OS packages (apt, apk, yum)
- Language deps (Maven, npm, pip, Go modules)
- Misconfigurations (Dockerfile bad practices)
- Secrets (leaked keys in layers)

### Reduce Vulnerabilities

- **Minimal base images** — `alpine`, `distroless`, `chainguard`
- **Multi-stage builds** — drop build tools from final image
- **Update base images** regularly
- **Pin dependency versions** + automated PR for updates (Dependabot)

---

## Layer Caching in CI

**Matlab:** Docker layers ko reuse karo across CI runs — build time drastically reduce.

### Why CI Cache Tricky

- Each CI run on fresh runner → no local Docker cache
- Need **external cache backend**

### GitHub Actions Cache (gha)

```yaml
- uses: docker/build-push-action@v6
  with:
    push: true
    cache-from: type=gha
    cache-to: type=gha,mode=max
```

`mode=max` → caches all intermediate layers (slower upload but faster next build).

### Registry-Backed Cache

```yaml
cache-from: type=registry,ref=ghcr.io/myorg/myapp:cache
cache-to: type=registry,ref=ghcr.io/myorg/myapp:cache,mode=max
```

### Inline Cache (Older)

```yaml
- run: |
    docker buildx build \
      --cache-from=ghcr.io/myorg/myapp:latest \
      --cache-to=type=inline \
      --push -t ghcr.io/myorg/myapp:${{ github.sha }} .
```

### Dockerfile Caching Tricks

```dockerfile
# ❌ Bad — every code change invalidates deps
COPY . /app
RUN ./mvnw package

# ✅ Good — deps cached separately
COPY pom.xml .
RUN ./mvnw dependency:go-offline    # cached if pom unchanged
COPY src ./src
RUN ./mvnw package
```

**Order:** stable layers first → frequently changing last.

---

## Best Practices

### 1. Multi-Stage Builds

Keep final image small. Build deps NOT in runtime image.

```dockerfile
FROM maven:3-eclipse-temurin-21 AS builder
# build...

FROM eclipse-temurin:21-jre-alpine
COPY --from=builder /target/app.jar /app.jar
```

### 2. Distroless / Alpine Base

```
eclipse-temurin:21-jre-alpine     ~150 MB
gcr.io/distroless/java21          ~80 MB (no shell — secure)
```

### 3. Don't Run as Root

```dockerfile
RUN useradd -r -u 1001 appuser
USER appuser
```

### 4. `.dockerignore`

```
.git
target/
node_modules/
.env
*.md
```

→ Smaller build context, faster builds, no leaked secrets.

### 5. One Process Per Container

Don't pack DB + app + cache in one container. Use Docker Compose or Kubernetes.

### 6. Health Check

```dockerfile
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -fs http://localhost:8080/actuator/health || exit 1
```

### 7. Labels (Metadata)

```dockerfile
LABEL org.opencontainers.image.source="https://github.com/myorg/myapp"
LABEL org.opencontainers.image.version="1.2.3"
```

`docker/metadata-action` does this automatically.

### 8. Sign Images (Sigstore / cosign)

```bash
cosign sign ghcr.io/myorg/myapp@sha256:abc...
```

→ Verify provenance in deploy.

### 9. SBOM (Software Bill of Materials)

```yaml
- uses: docker/build-push-action@v6
  with:
    sbom: true
    provenance: true
```

→ Inventory of what's in image (compliance).

### 10. Pin Base Image Digest

```dockerfile
# ❌ Tag floats
FROM eclipse-temurin:21-jre

# ✅ Pinned to immutable digest
FROM eclipse-temurin:21-jre@sha256:abc1234...
```

### 11. Don't Bake Secrets

```dockerfile
# ❌ NEVER
ENV DB_PASSWORD=supersecret

# ✅ At runtime
docker run -e DB_PASSWORD=$SECRET myapp
```

Secrets via Kubernetes Secrets, AWS Secrets Manager, Vault.

### 12. Use BuildKit (default in modern Docker)

- Cache mounts (`RUN --mount=type=cache,target=/root/.m2`)
- Secret mounts (`--mount=type=secret`)
- Parallel stages

---

## Summary Cheat Sheet

| Concept | Quick Note |
|---------|-----------|
| Multi-stage | Keep final image small |
| Tag with SHA | Immutable production reference |
| Avoid `latest` in prod | Non-reproducible |
| `buildx` + multi-arch | ARM + AMD64 |
| Trivy | Vuln scanning |
| `cache-from`/`cache-to` (gha) | Speed up CI |
| Dockerfile order | Stable first, dynamic last |
| `.dockerignore` | Smaller, secure context |
| Distroless | Tiny + secure base |
| Non-root user | Security hardening |
| OIDC | Auth registries without keys |
| SBOM + signing | Supply chain |

---

## Practice

1. Spring Boot multi-stage Dockerfile likho — final image size compare karo (full vs alpine vs distroless).
2. GitHub Actions: build + push to GHCR with SHA + SemVer tags via metadata-action.
3. Trivy scan add karo, fail on CRITICAL.
4. Multi-arch image (amd64 + arm64) build karke push karo.
5. Layer cache (`type=gha,mode=max`) ke pehle/baad build time measure.
6. Dockerfile order optimize: deps layer cache hit ratio improve.

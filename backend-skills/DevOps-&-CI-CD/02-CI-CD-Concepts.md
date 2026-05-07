# CI/CD Concepts

## Status: Not Started

---

## Table of Contents

1. [CI/CD Kya Hai?](#cicd-kya-hai)
2. [Continuous Integration (CI)](#continuous-integration-ci)
3. [Continuous Delivery (CD)](#continuous-delivery-cd)
4. [Continuous Deployment](#continuous-deployment)
5. [Pipeline Stages](#pipeline-stages)
6. [Pipeline Best Practices](#pipeline-best-practices)
7. [Common Tools](#common-tools)
8. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## CI/CD Kya Hai?

**Matlab:** Code commit hone se production tak **automated**, **reliable**, **fast** path. Manual deploy → reproducible pipeline.

```
Commit  →  CI (build, test)  →  CD (package, deploy)  →  Production
   ↑                                                          ↓
   └────────── Feedback loops (logs, monitoring) ────────────┘
```

**3 distinct concepts:**
1. **Continuous Integration** — merge + automated build/test
2. **Continuous Delivery** — always **deployable**, manual gate to deploy
3. **Continuous Deployment** — auto-deploy on merge

---

## Continuous Integration (CI)

**Matlab:** Developers **frequently merge** to shared branch (daily+), each merge → automated build + tests.

### Goals

- Detect integration bugs **early** (not at release time)
- Maintain "always green" main branch
- Fast feedback (minutes)

### Practices

- **Merge frequently** (small PRs, often)
- **Automated tests** on every push
- **Fix broken main** immediately (red build = team stops)
- **Same build everywhere** (Docker, scripts checked in)

### Without CI

```
Developer A: 3 weeks of work in branch
Developer B: 3 weeks of work in branch
Merge day → 5 days of conflict resolution + bugs!
```

### With CI

```
Daily merges, small conflicts resolved immediately.
Tests catch regressions same day.
```

---

## Continuous Delivery (CD)

**Matlab:** Code **always production-ready** — deploy ek button press away (or schedule).

### Practices

- Automated **deployment pipeline** to staging/prod
- **Zero manual steps** other than approval
- **Production parity** in lower environments
- **Database migrations** automated

### Difference from Deployment

```
Continuous Delivery: 
  main merged → auto-deploy to staging → manual approve → prod
  
Continuous Deployment:
  main merged → auto-deploy through all the way to prod (no manual step)
```

---

## Continuous Deployment

**Matlab:** Merge to main = **automatic prod deploy** (no human gate).

### Requirements

- Strong test coverage (unit, integration, e2e)
- Feature flags for risky changes
- Quick rollback mechanism
- Real-time monitoring + alerts
- Mature culture (trust automation)

### Examples

- Netflix, Etsy, Amazon — multiple deploys per minute
- Trunk-based dev + feature flags makes this safe

---

## Pipeline Stages

Typical sequence — fail fast, cheapest checks first.

```
1. Lint           (seconds)
2. Build          (1-3 min)
3. Unit Test      (1-3 min)
4. Integration Test (5-10 min)
5. Security Scan  (1-5 min)
6. Package        (1-2 min)
7. Deploy to staging
8. Smoke Test
9. (manual approve)
10. Deploy to prod
11. Smoke Test prod
```

### 1. Lint / Format

```bash
# Java
./mvnw spotless:check
./mvnw checkstyle:check

# JavaScript
npm run lint

# Generic
git diff --check     # whitespace
```

✅ Catches style issues without running tests.

### 2. Build / Compile

```bash
./mvnw compile
npm run build
go build ./...
```

✅ Catches compilation errors fast.

### 3. Unit Tests

```bash
./mvnw test
npm test
go test ./...
```

✅ **Fast** (< 5 min). Mock external deps. **Fail fast.**

### 4. Integration Tests

```bash
./mvnw verify -P integration-tests
```

✅ Test against real DB (Testcontainers), real Redis, real Kafka
❌ Slower — push later in pipeline

### 5. Security Scans

| Tool | Purpose |
|------|---------|
| **Snyk / Dependabot** | Dependency vulnerabilities (CVE) |
| **Trivy** | Container image scanning |
| **OWASP Dependency-Check** | Java/Maven scanning |
| **SAST** (SonarQube, CodeQL) | Static code analysis |
| **Secrets scanning** (GitGuardian, gitleaks) | Detect leaked keys |

### 6. Package

- Build Docker image
- Tag with commit SHA (immutable)
- Push to registry

```bash
docker build -t myapp:${GITHUB_SHA} .
docker push registry.example.com/myapp:${GITHUB_SHA}
```

### 7. Deploy to Staging

- `kubectl apply` / `helm upgrade` / Terraform / serverless
- Run **DB migrations** (Flyway / Liquibase)

### 8. Smoke Tests

Lightweight post-deploy checks:
- Health endpoint returns 200
- Critical user flow (login, place order) works
- Key API responds with expected schema

### 9. Manual Approval (Continuous Delivery)

UI gate — release manager / on-call approves.

### 10. Deploy to Production

Same artifact as staging — never re-build.

### 11. Post-Deploy Monitoring

- Error rate spike?
- Latency increase?
- Auto-rollback if threshold breached.

---

## Pipeline Best Practices

### 1. Fail Fast

Cheap stages first → expensive later. Lint failure should NOT wait for full test suite.

### 2. Same Artifact Everywhere

Build **once**, deploy same artifact to staging + prod.

```
❌ Build for staging, build separately for prod
✅ Build once → tag → promote to envs
```

### 3. Pipeline as Code

Pipeline definition **in repo** (`.github/workflows/`, `Jenkinsfile`). Version-controlled, reviewable.

### 4. Idempotent Steps

Re-running a stage shouldn't break things — DB migrations, infra apply.

### 5. Reproducible Builds

Same commit + same tools = same artifact bytes (where possible).

- Pin tool versions
- Pin dependency versions (lockfiles)

### 6. Caching

Speed up:
- Maven `~/.m2/repository`
- Node `node_modules`
- Docker layers
- Test results (skip unchanged tests if smart)

### 7. Parallelize

Tests, lint, security scans → run concurrently.

### 8. Secrets Management

- **Never** in repo
- Use secrets manager (GitHub Secrets, AWS Secrets Manager, Vault)
- Rotate periodically

### 9. Branch-Based Pipelines

```yaml
# Different rules per branch
main         → full pipeline + deploy prod
release/*    → full pipeline + deploy staging
feature/*    → lint + tests only
```

### 10. Notification

- Slack/Teams: failures only (avoid alert fatigue)
- Per-PR status checks (mandatory before merge)

---

## Common Tools

### CI/CD Platforms

| Tool | Notes |
|------|-------|
| **GitHub Actions** | Native to GitHub, YAML, free tier |
| **GitLab CI** | Native to GitLab, YAML |
| **Jenkins** | Self-hosted classic, plugin-rich |
| **CircleCI** | Cloud, fast |
| **Azure DevOps** | Microsoft stack |
| **AWS CodePipeline** | AWS-native |
| **Travis CI** | OSS-friendly |
| **Bitbucket Pipelines** | Atlassian |
| **TeamCity** | JetBrains, on-prem |

### Build Tools

- Java: Maven, Gradle
- JS: npm, yarn, pnpm
- Go: `go build`
- Docker: `docker buildx`

### Deployment Tools

- **Kubernetes**: `kubectl`, Helm, Argo CD
- **AWS**: CloudFormation, CDK, Terraform
- **Serverless**: Serverless Framework, SAM
- **Spinnaker** — multi-cloud CD

---

## Pipeline Examples

### Simple Java + Spring Boot

```
on: PR or push to main
  jobs:
    build:
      - checkout
      - setup-java 21
      - cache m2
      - mvn verify (compile + test)
      - upload test reports
    
    docker-build:
      needs: build
      if: branch == main
      - docker build
      - docker push (tag = commit SHA)
    
    deploy-staging:
      needs: docker-build
      - helm upgrade myapp --set image.tag=${SHA}
      - smoke test
```

(See `03-GitHub-Actions.md` for actual YAML.)

---

## Summary Cheat Sheet

| Concept | One-liner |
|---------|-----------|
| **CI** | Frequent merge + auto build/test |
| **CD (Delivery)** | Always deployable, manual approve |
| **CD (Deployment)** | Auto-deploy on merge |
| **Pipeline** | Lint → build → test → scan → deploy |
| **Fail fast** | Cheap checks first |
| **Same artifact** | Build once, promote |
| **Pipeline as code** | YAML in repo |
| **Cache** | Speed up builds |
| **Smoke test** | Post-deploy sanity |
| **Rollback** | Automatic on alert breach |

---

## Practice

1. Apne project ki current pipeline ka stages map karo.
2. List all manual steps in your deploy → automate karne ka plan banao.
3. Identify slowest stage → caching/parallelization se kitna kaata?
4. Implement smoke test endpoint check after deploy.
5. Fail-fast: lint stage add karo before tests; observe time saved on bad PRs.

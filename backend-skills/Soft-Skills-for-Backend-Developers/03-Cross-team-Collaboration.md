# Cross-team Collaboration

## Status: Not Started

---

## Table of Contents

1. [Why Collaboration Skill?](#why-collaboration-skill)
2. [Working with Frontend Teams](#working-with-frontend-teams)
3. [API Contracts & Mock Servers](#api-contracts--mock-servers)
4. [Working with DevOps / Platform](#working-with-devops--platform)
5. [Working with QA](#working-with-qa)
6. [Working with Product Managers](#working-with-product-managers)
7. [Working with Security & Compliance](#working-with-security--compliance)
8. [Working with Other Backend Teams](#working-with-other-backend-teams)
9. [Communication Tactics](#communication-tactics)
10. [Common Conflicts & Resolutions](#common-conflicts--resolutions)
11. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Why Collaboration Skill?

**Matlab:** Backend dev kabhi alone work nahi karta. Real value comes from **cross-functional execution**.

> "Code likhna 30%, baaki 70% — coordinate, communicate, unblock."

### Senior vs Junior Difference

| Junior | Senior |
|--------|--------|
| "Mera kaam ho gaya" | "Pura feature shipped end-to-end" |
| Frontend says broken → blame them | Pair to debug, learn their constraints |
| QA finds bug → annoyed | QA finds bug → thank them, fix root cause |
| PM unclear → silent confusion | PM unclear → clarifying questions early |

---

## Working with Frontend Teams

### What They Need from You

- **Stable, well-documented APIs**
- **Predictable response shapes**
- **Clear error contracts**
- **Mock data / staging access**
- **Early heads-up on changes**

### API Contracts

#### Define Shape Before Implementation

```yaml
# OpenAPI / Swagger
GET /api/v1/orders:
  parameters:
    - name: region
      in: query
      schema: { type: string }
  responses:
    200:
      content:
        application/json:
          schema:
            type: object
            properties:
              data: { type: array, items: { $ref: '#/components/Order' } }
              total: { type: integer }
              page: { type: integer }
```

→ Frontend can build against this **before** backend done.

#### Contract-First Workflow

```
1. PM/Designer/BE/FE meet → agree on data + interactions
2. Backend writes OpenAPI spec
3. Frontend mocks against spec
4. Backend implements, ships
5. Frontend swaps mock → real endpoint
```

### Shared Types / DTOs

For monorepos / TypeScript clients:

```
backend (Java) → generates OpenAPI → openapi-generator → TS client
                                                          ↓
                                                       frontend imports
```

→ Frontend gets typed client; breaking changes caught at build.

### CORS

Frontend on `app.example.com`, API on `api.example.com` → CORS issues common.

(Cross-ref: `Security-Best-Practices/07-API-Security.md`.)

```java
@Bean
public CorsConfigurationSource corsConfig() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(List.of("https://app.example.com"));
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    cfg.setAllowCredentials(true);
    cfg.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", cfg);
    return src;
}
```

### Pagination, Errors, Date Formats

Agree on conventions:

```json
// Response envelope
{
  "data": [...],
  "meta": { "total": 1234, "page": 1, "pageSize": 50 }
}

// Error format (RFC 7807-ish)
{
  "type": "validation_error",
  "title": "Invalid input",
  "status": 400,
  "errors": [
    { "field": "email", "message": "Must be valid email" }
  ]
}

// Dates always ISO-8601 UTC
"created_at": "2024-05-08T14:23:11Z"
```

### Common Pain Points (and Fixes)

| Frontend complains | Backend can do |
|--------------------|----------------|
| "Inconsistent error format" | RFC 7807 / shared error envelope |
| "Different field names per endpoint" | Style guide (camelCase, naming patterns) |
| "Why does this return null vs missing?" | Document. Pick one. |
| "Pagination differs across endpoints" | Standardize (cursor or offset, pick one) |
| "Breaking change without warning" | Version + deprecation period |

---

## API Contracts & Mock Servers

### Tools

- **OpenAPI / Swagger** — spec format
- **Swagger UI** — interactive docs
- **Springdoc OpenAPI** — auto-generates from Spring annotations
- **Postman / Insomnia** — testing + sharing collections
- **Mockoon / Prism / Wiremock** — mock servers from spec

### Springdoc Setup

```xml
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.5.0</version>
</dependency>
```

→ Auto-exposed at `/swagger-ui.html` and `/v3/api-docs`.

### Mock Server (for Frontend Parallel Dev)

```bash
# Prism — mock from spec
prism mock openapi.yaml
# → returns example responses immediately
```

→ Frontend codes against mock until backend ready.

### Stoplight / Postman Workspaces

Shared workspaces for spec collaboration. PRs against API spec.

### Versioning

```
/api/v1/orders         (current)
/api/v2/orders         (new shape, breaking change)
```

Run both for **deprecation period** (3-6 months) → frontends migrate.

→ More: `API-Design-&-Architecture/08-API-Versioning.md` (if exists).

---

## Working with DevOps / Platform

### Help Them Help You

#### Provide

- **Health check** endpoints
- **Metrics** endpoints (Prometheus)
- **Structured logs** (JSON)
- **Graceful shutdown** (handle SIGTERM)
- **Configuration via env vars** (12-factor)
- **Dockerfile** (build optimized + small image)
- **Resource hints** (CPU / memory needed)

#### Spring Boot Actuator

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      probes:
        enabled: true   # adds /readyz, /livez
```

```http
GET /actuator/health/readiness  → 200 if can serve
GET /actuator/health/liveness   → 200 if process alive
```

K8s uses these for `readinessProbe` / `livenessProbe`.

### 12-Factor Config

```yaml
# application.yml — pull from env
database:
  url: ${DATABASE_URL}
  username: ${DB_USERNAME}
  password: ${DB_PASSWORD}
```

→ Same artifact promoted dev → staging → prod, only env differs.

### Deployment Requirements Doc

Give DevOps a **README** with:

```markdown
## My Service Deployment Notes

- Docker image: `ghcr.io/myorg/orders:1.2.3`
- Port: 8080 (HTTP), 8081 (management)
- Memory: 512Mi requested, 1Gi limit
- CPU: 200m requested, 500m limit
- Env vars required:
  - DATABASE_URL
  - REDIS_URL
  - JWT_PUBLIC_KEY
- Health endpoints:
  - Liveness: GET /actuator/health/liveness
  - Readiness: GET /actuator/health/readiness
- Graceful shutdown: SIGTERM, 30s grace
- Dependencies: Postgres, Redis
- Migrations: Flyway on startup
```

### Don't Be Surprised By

- Read-only filesystem in containers
- Multiple replicas (cron tasks need leader election!)
- Autoscaling kills/spawns pods
- DNS for internal service discovery
- Rolling deploys (zero-downtime requirement)

### Be DevOps-Friendly

- Don't write to `/tmp` permanently
- Use `stdout`/`stderr` for logs (not files)
- Don't depend on local state across requests
- Make startup fast (slow startup = bad rolling deploy)

---

## Working with QA

### Mindset

> QA finding bug = **gift**, not insult.

If they didn't find it, your customers would.

### Help Them Test

#### 1. Test Environments

- Stable staging environment
- Separate from dev (don't break their tests with your in-flight changes)
- Realistic data volume

#### 2. Test Data

```sql
-- seed scripts
INSERT INTO users (email, role) VALUES
  ('test-user@example.com', 'USER'),
  ('test-admin@example.com', 'ADMIN');
```

→ Predictable scenarios.

#### 3. Test Accounts / API Keys

Pre-provisioned credentials they can use without bothering you.

#### 4. Feature Flags / Toggle

```
ENABLE_NEW_CHECKOUT=false  (default)
ENABLE_NEW_CHECKOUT=true   (QA can toggle)
```

→ QA tests new feature in shared env without affecting others.

#### 5. Reset / Cleanup APIs (Non-Prod Only)

```http
POST /test/reset-user/test-123    (only enabled with TEST_MODE=true)
```

### Bug Report Quality

#### Help QA Help You

Document expected report format:

```
Title: Brief description
Steps to reproduce:
  1. ...
  2. ...
  3. ...
Expected:
  ...
Actual:
  ...
Environment: staging / build SHA
Logs / screenshots: ...
Severity: Critical / High / Medium / Low
```

→ Vague reports waste time. Template helps.

### When QA Finds Bug

✅ "Thanks, let me reproduce."
✅ Reproduce locally → fix → write **regression test** → ship
✅ Comment on ticket: root cause, fix, regression test added

❌ "Works on my machine."
❌ "User error."
❌ Defensive — even if it's a misuse, **maybe error message is unclear**.

### Test Pyramid Awareness

```
       /\
      /  \   E2E (few, slow, owned by QA + dev)
     /----\
    / Integ \   Integration (more, medium speed)
   /--------\
  /   Unit   \   Unit tests (lots, fast, dev-owned)
 /------------\
```

Most tests should be unit (fast, in CI). E2E for critical flows.

---

## Working with Product Managers

### What PMs Need

- **Feasibility** estimates
- **Trade-off** clarity ("if you want X, we lose Y or take longer")
- **Risk** flags (compliance, perf)
- **Status** updates (without micro-management)

### Translate Tech ↔ Product

```
PM: "Make search faster"
You: "Two options:
      a) Add Elasticsearch — 1-2 weeks, big win on relevance + speed, ops cost
      b) Add Postgres GIN index — 2 days, decent improvement, no new infra
     Which matters more — relevance or quick win?"
```

→ Concrete options + trade-offs > "depends".

### Push Back (Constructively)

```
PM: "Can we ship Friday?"
You: "Yes if we cut scope of X. Otherwise next Wednesday. Recommend cutting X — it's lower priority."
```

→ Don't say yes to impossible just to please.

### Document Decisions

ADRs (Architecture Decision Records) — see `Code-Quality-&-Best-Practices/05-Documentation.md`.

```
ADR-007: Use cursor-based pagination for orders feed
Status: Accepted
Context: Offset-based was slow at high pages (40s on 100M rows)
Decision: Cursor-based, opaque token
Consequences: BC break for `?page=` callers — versioning to /v2
```

→ Future you / new joiners thank you.

### Weekly Update Format

```
This week:
  ✅ Shipped CSV export feature
  ✅ Migrated auth to OAuth2 PKCE
  
Next week:
  - Implement notification preferences UI
  - QA round for export

Risks / blockers:
  - Email vendor rate limit being hit — investigating throttling
```

---

## Working with Security & Compliance

### When They Find Issues

- ✅ Treat as urgent (security debt compounds)
- ✅ Fix and add **regression test**
- ✅ Don't merely silence the scanner

### Proactive

- Subscribe to dep alerts (Dependabot)
- Run SAST/SCA in CI (`Security-Best-Practices/08-Dependency-Security.md`)
- Threat model new features
- Engage security **early** for sensitive features (payments, auth, admin)

### Compliance Touchpoints

- **SOC 2** — access control, change management, audit logs
- **PCI** — payment data handling
- **GDPR / DPDP** — data subject rights, retention, deletion
- **HIPAA** — PHI handling

→ Document data flows, retention, encryption.

---

## Working with Other Backend Teams

### Service-to-Service

#### API Contracts

OpenAPI / gRPC proto / async event schemas (Avro, Protobuf).

#### Versioning + Deprecation

Coordinate breaking changes — give other team **time** + **warning**.

#### Shared Libraries

Internal SDK / common library — but be careful (versioning hell).

### Async via Events

Decouple where possible:

```
Order Service publishes "OrderPlaced"
  → Notification Service consumes (sends email)
  → Inventory Service consumes (decrements stock)
```

→ No tight HTTP dependency, fewer cascading failures.

### Cross-Team Initiatives

- **Tech docs** for shared concerns
- **RFC process** — propose, comment, decide
- **Office hours** for inter-team Q&A

---

## Communication Tactics

### Async > Sync (Most of the Time)

```
✅ Slack message with full context
✅ Detailed PR description
✅ Issue ticket with repro steps

❌ "Quick call?" (interrupts)
❌ "Hi" (no context, wait for reply)
```

→ Async respects others' focus + creates artifacts.

### Be Specific

```
❌ "Build is broken"
✅ "CI build #1234 failing on `mvn test`, error: NullPointerException at OrderService:42 — could you check?"
```

### Provide Context

```
❌ "Why does this fail?"
✅ "I'm trying to do X. I expected Y. Saw Z. I tried A and B. Stuck."
```

→ "X-Y-A-B" approach gets faster help than "why fail?".

### Disagree Respectfully

```
❌ "Your design is wrong."
✅ "I see the appeal, but I'm worried about scenario X. Have you considered approach Y?"
```

### Strong Opinions, Loosely Held

Argue your view; accept better idea when presented.

### Acknowledge Receipt

```
"Got it, looking" → 5 min
"Will look this afternoon" → calibrates expectation
```

> Silence breeds anxiety + duplicated nudges.

### Documentation as Communication

For repeated questions → write README / wiki → link instead of re-explaining.

---

## Common Conflicts & Resolutions

### "Frontend says API is slow, backend says network/DOM"

→ Together check:
- Browser network tab (TTFB)
- Server logs (response time)
- APM trace (full waterfall)
- Don't blame — debug together.

### "Backend pushed change, frontend broke"

→ Postmortem:
- Was breaking change communicated?
- Was version bumped?
- Process: contract testing in CI, deprecation period.

### "QA reports bug; dev can't reproduce"

→ Pair-debug session. Share env, payload, build SHA.

### "PM wants Friday; engineering says 2 weeks"

→ Trade-off conversation:
- What's must-have vs nice-to-have?
- Can we phase: ship MVP Friday, iterate next sprint?

### "Other team's API is unreliable"

→ Defensive coding:
- Timeouts
- Retries with backoff
- Circuit breaker
- Fallback / cached response
- Then talk to them about reliability commitments (SLA).

---

## Summary Cheat Sheet

| Stakeholder | Key Practice |
|-------------|-------------|
| **Frontend** | OpenAPI contract, mock server, consistent error shape |
| **DevOps** | Health endpoints, env-var config, deployment README |
| **QA** | Test data, stable env, bug report template, regression tests |
| **PM** | Trade-off options, weekly updates, ADRs |
| **Security** | Treat findings urgently, regression tests, threat model early |
| **Other backends** | Contracts, versioning, async events |

| Communication | Quick Note |
|--------------|-----------|
| Async > sync | Default for non-urgent |
| Specific | Logs, env, repro steps |
| Provide context | X-Y-A-B pattern |
| Acknowledge | "Got it, looking" |
| Disagree respectfully | Address idea not person |
| Document | One source > 10 retells |

| Anti-pattern | Better |
|--------------|--------|
| Silo / "my code's fine" | Pair to debug |
| Surprise breaking change | Versioning + deprecation |
| Vague bug report | Repro template |
| "Quick call?" with no agenda | Async with context |
| Saying yes to impossible | Trade-offs / phased plan |

---

## Practice

1. Write an OpenAPI spec for one of your endpoints; share with frontend.
2. Add a deployment README for one service.
3. Build / improve seed scripts so QA can self-serve test data.
4. Write a 1-page weekly update for your PM in the format above.
5. Send next "blocked" message using X-Y-A-B pattern.
6. Author one ADR for a recent design decision.
7. Pair-debug a "frontend says X / backend says Y" with that engineer.

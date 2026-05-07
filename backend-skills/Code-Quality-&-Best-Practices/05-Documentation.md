# Documentation

## Status: Not Started

---

## Table of Contents

1. [Documentation Kyun?](#documentation-kyun)
2. [Code Comments — Why, Not What](#code-comments--why-not-what)
3. [JavaDoc for Public APIs](#javadoc-for-public-apis)
4. [README.md](#readmemd)
5. [ADR — Architecture Decision Records](#adr--architecture-decision-records)
6. [Team Knowledge (Confluence / Notion)](#team-knowledge-confluence--notion)
7. [API Documentation](#api-documentation)
8. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Documentation Kyun?

**Matlab:** Future-you, teammates, ops — sab ko **context** chahiye bina har baar oral transfer ke.

**Principle:** Document **decisions & constraints** — code already "what" dikhata hai jab clean ho.

---

## Code Comments — Why, Not What

### Bad (states what code obviously does)

```java
// Increment i by 1
i++;
```

### Good (explains non-obvious why)

```java
// Stripe amounts are in cents — convert to Money for domain layer
Money total = Money.ofCents(stripeCharge.getAmount());

// Retry only on 429 — 4xx others are permanent client errors per Stripe docs
```

### When to comment

- Business rule nuance ("tax exempt for B2B in EU — legal ref XYZ")
- Workaround for library bug + link to issue
- Performance trick — **measurement** referenced
- Security-sensitive flow

### When not

- Comment duplicates method name clearly

---

## JavaDoc for Public APIs

**Matlab:** Libraries, shared modules, **public** classes/methods — contract document karo.

```java
/**
 * Places an order and returns a confirmed snapshot.
 *
 * <p>Idempotent when {@code clientRequestId} is reused — same response returned.</p>
 *
 * @param request must contain non-null lines and shipping address
 * @return confirmed order with assigned id
 * @throws InsufficientStockException if any line exceeds available inventory
 * @throws IllegalArgumentException if request fails bean validation
 */
public OrderConfirmation placeOrder(PlaceOrderRequest request) { ... }
```

**Include:**
- Preconditions / postconditions
- Thread-safety if non-obvious
- `@deprecated` + migration path
- `@since` version for libraries

**Don't:** JavaDoc jo sirf repeats method name in sentence form.

---

## README.md

Typical sections for a service/repo:

1. **What** — one paragraph purpose
2. **Requirements** — JDK version, Docker, etc.
3. **Quick start** — clone, env vars, `docker-compose up`, `./mvnw spring-boot:run`
4. **Configuration** — important env vars table
5. **Architecture** — diagram link or short layers description
6. **Testing** — how to run unit/integration tests
7. **Deployment** — links to CI/CD, helm chart
8. **Troubleshooting** — common errors

```markdown
## Local setup

1. Copy `.env.example` → `.env`
2. `docker compose up -d postgres`
3. `./mvnw spring-boot:run`

## Configuration

| Env | Required | Description |
|-----|----------|-------------|
| `DATABASE_URL` | yes | JDBC URL |
```

Keep **accurate** — outdated README worse than none.

---

## ADR — Architecture Decision Records

**Matlab:** Short doc per **significant** decision — **context, decision, consequences**.

### Format (Michael Nygard style)

```markdown
# ADR 012: Use PostgreSQL for primary OLTP store

## Status
Accepted

## Context
We need transactional consistency for orders; team knows SQL; expected load < 10k TPS.

## Decision
Use PostgreSQL 16 as primary database; Redis only for cache/session.

## Consequences
**Positive:** ACID, mature tooling, hiring pool.
**Negative:** Horizontal write scaling harder than some NoSQL — revisit if TPS grows 10x.

## Links
- RFC discussion: ...
```

Store in repo: `docs/adr/001-title.md` — version controlled with code.

**When:** New DB, messaging tech, auth approach, breaking API versioning strategy.

---

## Team Knowledge (Confluence / Notion)

**Repo README** = **how to run this service**.

**Wiki** = **how we work as a team:**
- On-call runbooks
- Incident response
- Coding conventions beyond linter
- Domain glossary ("what is a Settlement?")
- Links to dashboards, Slack channels

**Avoid duplication:** Link README → wiki for long prose; single source of truth.

---

## API Documentation

- **OpenAPI** (`springdoc-openapi`) — machine + human readable
- Keep examples **working** — CI can validate contract tests

---

## Summary Cheat Sheet

| Artifact | Purpose |
|----------|---------|
| **Comments** | Why, workarounds, non-obvious rules |
| **JavaDoc** | Public API contract |
| **README** | Setup, run, configure |
| **ADR** | Why big tech choices |
| **Wiki** | Runbooks, process, glossary |
| **OpenAPI** | HTTP contract |

---

## Practice

1. Ek useless comment dhundo aur delete ya replace karo "why" se.
2. Ek `README.md` skeleton apni dummy service ke liye likho.
3. Ek ADR draft likho: "Why REST vs gRPC for internal API" — 1 page.

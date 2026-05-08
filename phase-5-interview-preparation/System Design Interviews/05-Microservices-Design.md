# Microservices Design

## Status: Not Started

---

## Table of Contents

1. [Monolith vs Microservices](#monolith-vs-microservices)
2. [Service Decomposition](#service-decomposition)
3. [Sync vs Async Communication](#sync-vs-async-communication)
4. [REST vs gRPC vs GraphQL](#rest-vs-grpc-vs-graphql)
5. [Event-Driven Architecture](#event-driven-architecture)
6. [Saga Pattern](#saga-pattern)
7. [CQRS](#cqrs)
8. [API Gateway](#api-gateway)
9. [Service Mesh](#service-mesh)
10. [Observability — 3 Pillars](#observability--3-pillars)
11. [Common Output Traps](#common-output-traps)
12. [Pitfalls](#pitfalls)
13. [Cheat Sheet](#cheat-sheet)

---

## Monolith vs Microservices

### Monolith

```
Single deployable
├── User module
├── Order module
├── Payment module
└── Notification module
```

| Pros | Cons |
|------|------|
| Simple deploy | Scaling = entire app |
| One DB, easy TX | Tech lock-in |
| Easy debugging | Slow startup at size |
| Good for small teams | Big PRs / merge conflicts |

### Microservices

```
[User Service] [Order Service] [Payment Service] [Notification Service]
      │              │                │                  │
      └──────── Network calls / Events / API gateway ──────────┘
```

| Pros | Cons |
|------|------|
| Independent deploys | Distributed systems complexity |
| Per-service scaling | Network latency + failures |
| Tech polyglot | Distributed TX hard (saga) |
| Fault isolation | Operational overhead |

### When microservices?

- Team size > 50 engineers (Conway's law)
- Different scaling needs per component
- Different tech stacks justified
- Mature DevOps (CI/CD, observability)

→ **Don't start with microservices** unless you must. Many regret too-early decomposition.

---

## Service Decomposition

### 1. By business capability

```
Payment, Inventory, Shipping, Customer, Catalog
```

→ Aligns with org structure (Conway).

### 2. By DDD bounded context

> "**Bounded context** = boundary within which a model is consistent."

Different domains, different language → different services.

```
Sales Context: Order = customer-facing transaction
Fulfillment Context: Order = warehouse work item
```

→ Same name, different model — separate services.

### 3. By volatility / scale

If part of system has 10x traffic of rest → extract for independent scaling.

### 4. By single team ownership

> "Two-pizza team" rule (Bezos).

### Anti-patterns

- **Distributed monolith** — services that must deploy together
- **Nano services** — too granular, network overhead > business value
- **Shared database** across services — coupling reintroduced

→ Cross-ref: `phase-4 / Microservices Architecture/`.

---

## Sync vs Async Communication

### Synchronous (request-response)

```
A → B (wait for response)
```

**Pros:** Simple flow, immediate result.
**Cons:** Coupled availability — A fails if B down. Cascading failures.

### Asynchronous (event/message)

```
A → Queue → B (B processes later, no wait)
```

**Pros:** Decoupled, resilient to downtime, smooths load.
**Cons:** Eventual consistency, harder debug.

### Patterns

| Pattern | Sync/Async | Use |
|---------|-----------|-----|
| REST API | Sync | Direct read/write |
| gRPC | Sync | Service-to-service, low latency |
| GraphQL | Sync | Aggregation for clients |
| Message queue (RabbitMQ) | Async | Task queues, RPC |
| Event stream (Kafka) | Async | Domain events, fan-out |

→ Hybrid: most systems use both.

---

## REST vs gRPC vs GraphQL

| | REST | gRPC | GraphQL |
|--|------|------|---------|
| Protocol | HTTP/1.1 | HTTP/2 | HTTP (POST query) |
| Encoding | JSON (text) | Protobuf (binary) | JSON (with schema) |
| Schema | OpenAPI (optional) | .proto (mandatory) | GraphQL SDL |
| Streaming | Limited (SSE/WS) | Native bidirectional | Subscriptions |
| Browser native | ✅ | ❌ (gRPC-Web bridge) | ✅ |
| Latency | Higher (JSON parse) | Lower (binary) | Higher (query parse) |
| Discoverability | Endpoints | RPC methods | Single endpoint |
| Use | Public API | Internal service-to-service | Aggregation for client (mobile/web) |
| Tooling | Mature | Strong code-gen | Strong client tooling (Apollo) |

### Quick rules

- **Public API** → REST (universal)
- **Internal service-to-service** → gRPC (perf + contract)
- **Mobile / SPA aggregation** → GraphQL (avoid over-fetching)

---

## Event-Driven Architecture

### Events vs Commands

| | Event | Command |
|--|-------|---------|
| Tense | Past ("OrderPlaced") | Imperative ("PlaceOrder") |
| Direction | Broadcast (1 to N) | Targeted (1 to 1) |
| Coupling | Loose | Tighter |
| Ownership | Producer | Consumer |

### Choreography (event-driven, decentralized)

```
Order Service → "OrderPlaced" event
   ├─→ Inventory consumes → reserves stock
   ├─→ Payment consumes → charges
   └─→ Notification consumes → emails customer
```

**Pros:** Decoupled, scalable.
**Cons:** Hard to trace overall flow.

### Orchestration (centralized coordinator)

```
Order Service → tells Inventory → tells Payment → tells Shipping
```

**Pros:** Visible flow, easier debugging.
**Cons:** Single point of failure (orchestrator).

→ Real systems: **mix** — choreography for loose flows, orchestration for complex sagas.

### Cross-ref: `phase-4 / Messaging — Kafka, RabbitMQ`.

---

## Saga Pattern

> "Long-running distributed transaction split into local transactions; compensating actions on failure."

### Why?

ACID 2PC across microservices:
- Slow (locks for distributed coordination)
- Tightly couples services
- Doesn't scale

→ Use saga instead.

### Example: e-commerce checkout

```
1. Create order (Order service)
2. Reserve inventory (Inventory service)
3. Charge payment (Payment service)
4. Ship (Shipping service)

If step 3 fails:
- Compensate: release inventory + cancel order
```

### Choreography saga

```
Order created → InventoryReserveRequested
Inventory reserved → PaymentChargeRequested
Payment failed → InventoryReleaseRequested + OrderCancelled
```

→ Each service knows next event to publish + compensating event on failure.

### Orchestration saga

```
Saga orchestrator drives:
1. Call Order
2. Call Inventory
3. Call Payment
   if fail:
     - Call Inventory.release
     - Call Order.cancel
```

→ Implementations: **Camunda, Temporal, Axon Framework, Cadence**.

### Trade-offs

| | Choreography | Orchestration |
|--|-------------|---------------|
| Coupling | Loose | Tighter (orchestrator knows all) |
| Visibility | Spread across services | Centralized log |
| Complex flows | Hard | Easier |
| Failure handling | Each service handles | Orchestrator decides |

### Outbox pattern (reliable event publishing)

> "Update DB + publish event atomically."

```sql
BEGIN
  UPDATE orders SET status = 'placed' WHERE id = ...;
  INSERT INTO outbox (event_type, payload, created_at) VALUES ('OrderPlaced', ...);
COMMIT;
```

Background job polls outbox → publishes to Kafka. Ensures **no event lost** even if Kafka temporarily down.

→ Cross-ref: `phase-4 / Messaging — Kafka / 06-Kafka-Patterns.md`.

---

## CQRS

> **Command Query Responsibility Segregation** — separate read model and write model.

### Architecture

```
Commands → Write Model (normalized OLTP) → DB
                ↓
              Events
                ↓
        Read Model (denormalized) → Read DB / Cache
                ↑
              Queries
```

### Why?

- Read pattern often vastly different from write
- Write needs consistency; read needs scale + speed
- Separate scaling per side

### Example

```
Order Service writes to PostgreSQL (canonical orders)
   ↓ events to Kafka
   ↓
Read projector → builds:
  - Elasticsearch (for search)
  - Redis (for hot read)
  - Materialized views in Postgres (for reports)
```

### Trade-offs

| Pros | Cons |
|------|------|
| Independent scale read / write | Eventual consistency |
| Different storage per query type | Complexity |
| Optimal queries | Stale reads |

### When?

- Asymmetric load (50:1 read:write) ✅
- Complex query patterns ✅
- Simple CRUD app ❌ (overkill)

### Event Sourcing (often paired with CQRS)

> "Store events, not state. Replay events to derive current state."

```
events: [OrderPlaced, ItemAdded, ItemRemoved, PaymentApplied]
state: derive by applying events
```

**Pros:** Full audit, time-travel, multiple projections.
**Cons:** Complex, large storage.

---

## API Gateway

> "Single entry point for clients; cross-cutting concerns centralized."

### Responsibilities

- **Routing** to backend services
- **Auth + rate limiting**
- **TLS termination**
- **Request/response transformation**
- **Caching**
- **Logging / metrics**
- **API composition** (combine multiple service responses)

### Anti-pattern

- "Smart gateway" with business logic — keep gateway thin
- Bottleneck — must be HA (active-active)

### Implementations

- **Spring Cloud Gateway** (Java)
- **Kong, Tyk** (open source)
- **AWS API Gateway** (managed)
- **Envoy** (proxy as gateway)

→ Cross-ref: `phase-4 / Spring Cloud / 02-Spring-Cloud-Gateway.md`.

### BFF (Backend for Frontend)

Different gateway/aggregator per client type:

```
Mobile BFF  → tailored small payload
Web BFF     → richer payload
Public API  → standard payload
```

---

## Service Mesh

> "Sidecar proxy pattern — networking concerns moved out of app code."

```
Pod
├── App container
└── Sidecar proxy (Envoy)

All traffic → sidecar → other service's sidecar → other app
```

### What sidecar handles

- mTLS between services
- Load balancing
- Retries / circuit breaker
- Observability (metrics, traces)
- Traffic shifting (canary)

### Implementations

- **Istio** (most feature-rich, complex)
- **Linkerd** (lighter, Rust-based)
- **Consul Connect**
- **AWS App Mesh**

### Pros

- App code stays simple (no Resilience4j / mTLS lib)
- Polyglot (works across languages)
- Centralized policy

### Cons

- Operational complexity
- Latency overhead (extra hop)
- Resource cost (sidecar per pod)

→ Use when: 50+ services + polyglot + need uniform policy.

---

## Observability — 3 Pillars

### 1. Logs

- Structured (JSON), correlation ID
- Aggregated (ELK / Loki / Splunk / DataDog)
- Searchable across services

```json
{"ts":"...","service":"order","level":"INFO","trace_id":"abc","msg":"Order placed","order_id":123}
```

### 2. Metrics

- Counters, gauges, histograms
- Time-series DB (Prometheus, Datadog)
- Aggregated dashboards (Grafana)

```
http_requests_total{service="order",status="200"} 12345
order_processing_duration_seconds_bucket{le="0.5"} ...
```

### 3. Traces

- Distributed tracing (one user request → all services it touched)
- Span hierarchy with timing
- Tools: Jaeger, Zipkin, OpenTelemetry, Tempo

```
Trace: GET /checkout
├── Span: API Gateway (10ms)
│   └── Span: OrderService (8ms)
│       ├── Span: InventoryService (3ms)
│       └── Span: PaymentService (2ms)
```

### Combine all 3

- High latency alert (metric)
- Open trace → see slow service
- Open service logs at trace_id → see error detail

→ Cross-ref: `phase-4 / Performance & Optimization / 05-Profiling-and-APM.md`.

### SLI / SLO / SLA

| | Meaning |
|--|---------|
| SLI | Service Level Indicator (measured value, e.g., latency P99) |
| SLO | Service Level Objective (internal target, e.g., 99.9% < 200ms) |
| SLA | Service Level Agreement (external promise + penalty) |

---

## Common Output Traps

### Q1. "Microservices for any project"

→ Many small projects regret. Start monolith → extract when justified.

### Q2. Distributed TX with 2PC

→ Slow + coupled; use saga.

### Q3. Shared DB across services

→ Recreates monolith coupling.

### Q4. CQRS for CRUD

→ Overkill; introduce when query patterns diverge.

### Q5. No observability ("we'll add later")

→ Production debugging impossible. Build in from start.

---

## Pitfalls

1. **Premature decomposition** → distributed monolith.
2. **Shared DB** → coupling.
3. **Sync chains** without circuit breaker → cascading failures.
4. **No idempotency** in event consumers → double processing.
5. **No outbox** → event/state inconsistency.
6. **Over-orchestration** for simple flows.
7. **Smart gateway** with business logic.
8. **No tracing** → can't debug latency.
9. **Polyglot for fun** → operational nightmare.
10. **Choreography for complex saga** → debugging hell.
11. **API gateway SPOF** → active-active with floating IP.
12. **Service mesh too early** — overkill for < 20 services.

---

## Cheat Sheet

| Concept | Quick |
|---------|-------|
| Decomposition | Business capability / DDD bounded context |
| Comm | Sync (REST/gRPC) + Async (Kafka/RabbitMQ) |
| Distributed TX | Saga (NOT 2PC) |
| Read scale | CQRS + projection |
| Cross-cut | API Gateway / Service Mesh |
| Debug | Logs + metrics + traces (correlation ID) |

| Comm | Use |
|------|-----|
| REST | Public API |
| gRPC | Internal, low latency |
| GraphQL | Client aggregation |
| Kafka | Event stream, replay |
| RabbitMQ | Task queue, RPC |

| Saga | Pick |
|------|------|
| Choreography | Loose, simple flows |
| Orchestration | Complex, visible |

---

## Practice

1. Design e-commerce backend as 6 microservices — list bounded contexts.
2. Saga for order checkout (4 steps) — choreography vs orchestration trade-off.
3. CQRS read model design for product search (Elasticsearch projection).
4. API Gateway responsibilities (5+) + reasoning to NOT put business logic.
5. Distributed trace example — request → 3 services → identify slowest.

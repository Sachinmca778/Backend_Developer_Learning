# Message Queues & Async

## Status: Complete

---

## Table of Contents

1. [Why Async Messaging](#why-async-messaging)
2. [Sync vs Async](#sync-vs-async)
3. [Point-to-Point (Queue)](#point-to-point-queue)
4. [Publish/Subscribe (Topic)](#publishsubscribe-topic)
5. [Common Use Cases](#common-use-cases)
6. [Delivery Guarantees](#delivery-guarantees)
7. [Idempotency & Dedup](#idempotency--dedup)
8. [Ordering Guarantees](#ordering-guarantees)
9. [Retry & DLQ](#retry--dlq)
10. [RabbitMQ vs Kafka vs SQS](#rabbitmq-vs-kafka-vs-sqs)
11. [Pitfalls](#pitfalls)
12. [Cheat Sheet](#cheat-sheet)

---

## Why Async Messaging

> "**Decouple producer aur consumer** — producer message bhej ke chala jaata hai, consumer apni speed pe process karta hai. Failures isolated, spikes absorbed, retries possible."

Wins:

- **Decoupling** — producer doesn't know/care about consumers
- **Spike absorption** — queue acts as buffer; consumers drain at sustainable rate
- **Resilience** — consumer down? messages wait safely
- **Retry** — failed processing → re-queued / DLQ
- **Fan-out** — one event triggers many independent workflows
- **Async UX** — user sees instant ack, work happens in background

---

## Sync vs Async

```
SYNC:
  Client → Service A → Service B → Service C → DB
              (waits for entire chain — fragile, slow)

ASYNC:
  Client → Service A (returns 202 ack) → Queue → Worker → ...
              (decoupled, resilient, scales independently)
```

| Aspect | Sync | Async |
|--------|------|-------|
| Latency to ack | High (full chain) | Low (publish + ack) |
| Coupling | Tight | Loose |
| Failure cascade | Easy | Bounded |
| Complexity | Lower | Higher (eventual consistency, dedup) |
| Best for | User-facing reads, transactions | Background jobs, fan-out, integrations |

> "Hot path → sync. Side effects → async."

---

## Point-to-Point (Queue)

> "**Queue** = ek message **exactly one consumer** ko milta hai. Multiple consumers competing → load distributed."

```
Producer ──► Queue ──► Consumer 1
                  ──► Consumer 2  (load shared)
                  ──► Consumer 3
```

- One message → one worker
- Workers compete for messages → **work distribution**
- Add more workers = process faster
- Examples: **SQS Standard / FIFO**, **RabbitMQ queues**

### Use cases

- **Background job processing** (image resize, PDF generation, email send)
- **Order processing pipeline**
- **Web scraping workers**
- **Video transcoding**

---

## Publish/Subscribe (Topic)

> "**Topic** mein publish karo, **sab subscribers ko copy** milti hai. Event broadcast pattern."

```
Publisher ──► Topic ──► Subscriber A (copy)
                   ──► Subscriber B (copy)
                   ──► Subscriber C (copy)
```

- One message → many independent consumers
- Each consumer has own subscription / consumer group
- New consumer added = no producer change
- Examples: **SNS**, **Kafka topics with consumer groups**, **Redis Pub/Sub**, **RabbitMQ fanout exchange**

### Use cases

- **Event broadcasting** ("OrderPlaced" → email, inventory, analytics, search index)
- **Cache invalidation** broadcast
- **Real-time notifications**
- **Microservice integration** (event-driven architecture)

### Hybrid: Topic → Queue fan-out

```
Publisher → SNS Topic
                ├── SQS Queue 1 → Worker pool A
                ├── SQS Queue 2 → Worker pool B
                └── SQS Queue 3 → Worker pool C
```

> Each consumer gets own queue → isolated retries, DLQ, scaling. Industry standard.

---

## Common Use Cases

### 1. Async background processing

```
User uploads photo → API returns 202 immediately
               → Queue → Worker → resize, OCR, store, notify
```

User feels fast; heavy work happens off path.

### 2. Traffic spike absorption

```
Flash sale: 100,000 orders/sec hit API
        → Queue absorbs spike
        → Workers drain at 10,000/sec sustainable rate
        → DB / payment provider not overwhelmed
```

### 3. Retry with backoff

```
Worker fails (transient error) → message goes back to queue with delay
                              → exponential backoff retry
                              → after N attempts → DLQ for inspection
```

### 4. Event sourcing

```
Every state change → published as event to log (Kafka)
                  → multiple consumers project state into different stores
                  → replay events to rebuild state
```

### 5. Saga / workflow orchestration

```
Order saga: place → reserve inventory → charge payment → ship
        each step = message; failures trigger compensating messages
```

### 6. Real-time notifications

```
WebSocket server subscribes to "user:42:notifications" topic
        → other services publish notifications
        → server pushes to user
```

---

## Delivery Guarantees

> "**Three guarantees**, each with trade-offs."

| Guarantee | Behavior | Cost |
|-----------|----------|------|
| **At-most-once** | Message delivered 0 or 1 times — may lose | Cheapest, fastest |
| **At-least-once** | Message delivered ≥1 time — may duplicate | **Default for most systems** (SQS, Kafka, RabbitMQ default) |
| **Exactly-once** | Each message processed once — no loss, no dup | **Hardest** — needs transactional consumer + idempotency |

### Reality check

- Kafka claims **exactly-once** with transactions + idempotent producer + Kafka-only sink
- SQS FIFO = exactly-once **delivery** (5-min dedup window) but consumer must still be idempotent
- Most production systems = **at-least-once + idempotent consumer** (good enough)

---

## Idempotency & Dedup

> "**Same message twice → same final state.** Idempotency tames at-least-once delivery."

### Patterns

| Pattern | How |
|---------|-----|
| **Idempotency key** | Producer attaches unique ID; consumer dedups (Redis/DB seen-set) |
| **Natural idempotency** | Operation is inherently safe to repeat (e.g., `SET state=PAID`) |
| **Conditional update** | `UPDATE ... WHERE version = X` — only succeeds once |
| **Outbox pattern** | Write to DB + outbox table in same TX; relay to queue separately |
| **Inbox pattern** | Consumer dedups on inbox table |

### Example: payment processing

```python
def handle_payment(msg):
    idempotency_key = msg["payment_id"]
    if db.exists("processed:" + idempotency_key):
        ack(msg)  # already done, just ack
        return
    process_payment(msg)
    db.set("processed:" + idempotency_key, ttl=7days)
    ack(msg)
```

> Without idempotency, retries = double-charging, double-emails, duplicated state. **Always** design consumers idempotent.

---

## Ordering Guarantees

| System | Default ordering |
|--------|------------------|
| **SQS Standard** | Best-effort (no guarantee) |
| **SQS FIFO** | Strict, per `MessageGroupId` |
| **Kafka** | Strict, per partition (use key for ordering within partition) |
| **RabbitMQ** | Per queue (with single consumer) |
| **Redis Streams** | Per stream |

### Trick

- **Strict global order** = single partition / single consumer = no parallelism
- **Per-key order** = partition by key (e.g., user_id) = parallel across keys, ordered within
- Most systems use per-key ordering (Kafka pattern)

---

## Retry & DLQ

### Retry strategies

| Strategy | Description |
|----------|-------------|
| **Immediate retry** | Re-process right away (transient failure) |
| **Backoff retry** | Delay 1s, 2s, 4s, 8s, ... (avoid hammering broken downstream) |
| **Exponential + jitter** | Add randomness to avoid thundering herd |
| **Visibility timeout** (SQS) | Message invisible while processing; reappears if not deleted |

### Dead Letter Queue (DLQ)

> "**After N failed attempts, move message to DLQ**" — for inspection, alerting, manual replay.

```
Source Queue → consumer fails → retry → fails 5× → DLQ
                                              ↓
                                       Alarm: DLQ > 0
                                              ↓
                                   Engineer inspects, fixes, replays
```

### DLQ patterns

- **Per-source** DLQ (one DLQ per main queue)
- **Alarm on `ApproximateNumberOfMessages > 0`**
- **Retention 14 days max** for forensics
- **Redrive** mechanism to replay after fix

---

## RabbitMQ vs Kafka vs SQS

| Feature | **RabbitMQ** | **Kafka** | **SQS** |
|---------|--------------|------------|----------|
| Model | Smart broker, dumb consumer | Dumb broker, smart consumer | Managed queue |
| Protocol | AMQP, MQTT, STOMP | Custom binary | HTTPS API |
| Throughput | ~50K msg/s per node | **Millions/s per cluster** | Nearly unlimited (Standard) |
| Latency | Low (1–5 ms) | Low (5–10 ms) | ~10–50 ms (HTTP overhead) |
| Routing | **Rich** (direct, topic, fanout, headers exchange) | Topic + partition only | Standard / FIFO |
| Replay | No (msg consumed = gone) | **Yes** (offset-based, retention 7 days+) | No |
| Ordering | Per queue | Per partition | FIFO mode only |
| Durability | Persistent queues | Replication factor | Durable by default |
| Operations | Self-managed (Helm/operator) | Self-managed (heavy) or Confluent / MSK | **Managed** (no ops) |
| Use cases | Complex routing, RPC patterns | Event streaming, log, replay, stream processing | Async jobs, AWS-native fan-out |

### Kafka unique strengths

- **Durable log** — retain messages 7 days, replay any time
- **High throughput** at scale (Netflix, LinkedIn run trillions of msgs/day)
- **Stream processing** (Kafka Streams, Flink, Spark)
- **Event sourcing** backbone

### RabbitMQ strengths

- **Flexible routing** (exchange types: direct, topic, fanout, headers)
- **RPC-style** request/reply patterns
- **Dead-letter exchanges**, **delayed messages**
- Good for **microservice integration** with complex routing

### SQS strengths

- **Zero ops** — fully managed
- **Cheap** at small scale
- Pairs with **SNS** for fan-out
- **DLQ + retry** built-in

### Decision guide

| Need | Pick |
|------|------|
| AWS-native, simple async | **SQS** |
| Event log, replay, high throughput, streams | **Kafka** |
| Complex routing, RPC patterns | **RabbitMQ** |
| Pub/sub fan-out | **SNS → SQS** or **Kafka topic + groups** |

---

## Pitfalls

1. **No idempotency** in consumer → at-least-once delivery → duplicate charges / emails.
2. **Visibility timeout < processing time** → duplicate processing.
3. **No DLQ** → poison messages loop forever.
4. **Single MessageGroupId** in FIFO → throughput stuck at 3000/s.
5. **Treating queue as DB** → store state in app/DB, queue is for messages only.
6. **Synchronous wrapper** around async (caller waits for response) → defeats the purpose.
7. **Unbounded retries** → flood downstream during outage.
8. **Kafka without partitioning thought** → single partition = serial processing.
9. **Large payloads** in SQS (>256 KB) — store in S3, reference in message.
10. **No monitoring** of queue depth, consumer lag, DLQ count.
11. **Exactly-once myth** — design as at-least-once + idempotent consumer.
12. **Coupling DB transaction to queue publish** → either both succeed (use **outbox pattern**) or risk inconsistency.

---

## Cheat Sheet

| Topic | Quick |
|-------|-------|
| Queue | One consumer per message — work distribution |
| Topic | All subscribers get message — fan-out |
| At-least-once | Default; design idempotent |
| Exactly-once | Hard; usually at-least-once + idempotency |
| FIFO ordering | Strict — costs throughput |
| Per-key ordering | Partition / group ID |
| Retry | Backoff + jitter |
| DLQ | After N failures |
| Outbox pattern | DB write + queue publish atomic via outbox table |

| Tool | Best for |
|------|----------|
| **SQS** | Managed AWS jobs |
| **SNS** | Fan-out push (often → SQS) |
| **EventBridge** | Routing, schedules, SaaS events |
| **RabbitMQ** | Complex routing, RPC |
| **Kafka** | Event log, replay, streams |
| **Redis Streams** | Lightweight Kafka-lite |

---

## Practice

1. Design async pipeline for image upload: API → S3 → Queue → Workers → resize variants → DB update → notify.
2. Implement **idempotency key** pattern in Python consumer.
3. Set SQS visibility timeout = 6× function timeout for Lambda; explain why.
4. Outbox pattern: write DB row + outbox row in same TX; separate relay publishes to Kafka.
5. Choose: **Kafka vs SQS** for: clickstream / order events / OTP emails. Justify each.
6. Diagnose: DLQ has 1000 messages, queue depth growing — what to check first?

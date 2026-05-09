# SQS, SNS & EventBridge

## Status: Complete

---

## Table of Contents

1. [Why Async Messaging](#why-async-messaging)
2. [SQS — Simple Queue Service](#sqs--simple-queue-service)
3. [SQS Standard vs FIFO](#sqs-standard-vs-fifo)
4. [Visibility Timeout](#visibility-timeout)
5. [Long Polling](#long-polling)
6. [Dead Letter Queues](#dead-letter-queues)
7. [Message Retention](#message-retention)
8. [SNS — Simple Notification Service](#sns--simple-notification-service)
9. [Fan-Out Pattern (SNS → SQS)](#fan-out-pattern-sns--sqs)
10. [EventBridge](#eventbridge)
11. [Comparison Table](#comparison-table)
12. [Pitfalls](#pitfalls)
13. [Cheat Sheet](#cheat-sheet)

---

## Why Async Messaging

> "**Decouple producers from consumers** — producers don't wait, consumers process at own pace, failures isolated, traffic spikes absorbed."

| Pattern | Service |
|---------|---------|
| **Queue** (1 consumer per msg) | **SQS** |
| **Pub/Sub** (fan-out to many) | **SNS** |
| **Event router** (rich routing rules, schemas, scheduled) | **EventBridge** |

---

## SQS — Simple Queue Service

> "**Fully managed message queue.** Producers send messages, consumers poll and delete after processing."

### Key facts

- Pay per request (1M req free tier)
- Practically **unlimited messages** (no infra to manage)
- Max message size **256 KB** (use S3 + reference for bigger payloads — extended client lib)
- **No push to consumers** — they poll (long polling efficient)

### Lifecycle

```
Producer → Send → Queue
                    │
              Consumer Receives
                    ↓
              Visibility timeout starts (msg invisible to others)
                    ↓
              Consumer processes
                    ↓
       ┌────── Success → DeleteMessage
       └────── Failure / timeout → msg visible again → retry
                    ↓ (after maxReceiveCount)
              → DLQ
```

---

## SQS Standard vs FIFO

| | **Standard** | **FIFO** |
|--|--------------|----------|
| Throughput | Nearly unlimited | **3,000 msg/s** with batching (per group), 300/s without |
| Ordering | Best-effort | **Strict per MessageGroupId** |
| Delivery | At-least-once (dupes possible) | **Exactly-once** (5-min dedup window) |
| Naming | any | must end with `.fifo` |
| Use | Most workloads | Where order/dedup matters (financial txns, state machines) |

### FIFO essentials

- **MessageGroupId** — strict ordering within group, parallel across groups
- **MessageDeduplicationId** — dedup within 5-min window (or content-based)

```
ORDER:abc → group="user:42"  → strict order for user 42
PAYMENT:xyz → group="user:99" → strict order for user 99 (parallel to 42)
```

---

## Visibility Timeout

> "**How long a message is invisible** after a consumer receives it. If consumer doesn't delete in time → message reappears for another consumer."

```
Default: 30 seconds
Max:     12 hours
```

### Rule of thumb

- **Visibility timeout >= max processing time × 2**
- For Lambda trigger: AWS recommends **6× function timeout**
- Too low → duplicate processing (long-running task gets re-sent)
- Too high → slow recovery from crashed consumer

### Extending mid-processing

```python
sqs.change_message_visibility(
  QueueUrl=q, ReceiptHandle=rh, VisibilityTimeout=300
)
```

→ For genuinely long jobs, extend periodically (heartbeat).

---

## Long Polling

> "**Wait up to 20 s** for a message to arrive instead of returning empty immediately."

| | Short polling | Long polling |
|--|---------------|--------------|
| Default | 0 s wait | 1–20 s wait |
| Empty receives | Many (cost!) | Few |
| Latency to first msg | ~immediate | ~immediate too (wakes when msg arrives) |

### Configure

- Per-queue: `ReceiveMessageWaitTimeSeconds = 20`
- Per-call override

→ **Always use long polling** in production. Cuts costs significantly.

---

## Dead Letter Queues

> "**After N failed receives, move message to DLQ** for inspection / manual retry / alerting."

```
maxReceiveCount = 5  → after 5 failed receives, message → DLQ
```

### Use DLQ for

- Poison pill messages (bad payload that always crashes consumer)
- Detect bugs (alarm on DLQ depth > 0)
- Manual replay after fix

### Best practices

- DLQ is **same type** as source (Standard DLQ for Standard queue, FIFO DLQ for FIFO queue)
- **Alarm on `ApproximateNumberOfMessagesVisible`** of DLQ
- Set DLQ retention to **14 days** (max) for forensics
- Periodic **redrive** after fix: SQS now has built-in redrive API

---

## Message Retention

```
Default: 4 days
Min:     60 seconds
Max:     14 days
```

After retention, message **silently deleted**. Configure based on consumer SLA — usually 14 days for safety.

---

## SNS — Simple Notification Service

> "**Pub/Sub topic** — publishers post to topic, AWS fans out to all subscribers (push, not pull)."

### Subscribers

- **SQS** queues (fan-out pattern)
- **Lambda**
- **HTTP/HTTPS** webhooks
- **Email** / **SMS**
- **Mobile push** (APNS, FCM)
- **Kinesis Data Firehose**

### FIFO SNS

- Mirrors FIFO SQS (ordering, dedup)
- Subscribers must be FIFO SQS

### Filter policies

> "**Subscribers receive only matching messages** — based on attributes."

```json
{ "eventType": ["order.created", "order.cancelled"] }
```

→ Don't have one Lambda inspect every message and ignore most.

---

## Fan-Out Pattern (SNS → SQS)

```
                    ┌→ SQS-orders   → Lambda-fulfillment
Publisher → SNS ───├→ SQS-emails   → Lambda-notifier
                    └→ SQS-search   → Lambda-indexer
```

Why:

- **Decouple** producer from N consumers
- Each consumer has **own queue** = isolated retries / DLQ / scaling
- New consumer = subscribe new queue, no producer change

### vs direct SNS → Lambda

- Direct: faster, simpler, but no per-consumer DLQ control / batching
- Via SQS: durable buffer, batching, replay capability

---

## EventBridge

> "**Serverless event bus** with rich routing, schedules, partner integrations, schema registry. Replacement for CloudWatch Events."

### Components

- **Event bus** — `default`, **custom** buses, **partner** buses (SaaS like Stripe, Datadog)
- **Rule** — pattern match against event JSON, sends to **target(s)**
- **Schedule** — cron / rate expressions, replaces CW Events scheduler (now also "EventBridge Scheduler" service for advanced scheduling)
- **Schema Registry** — discovers and stores event schemas, generates code bindings

### Event shape

```json
{
  "version": "0",
  "id": "...",
  "detail-type": "Order Created",
  "source": "myapp.orders",
  "account": "123456789012",
  "time": "2024-05-08T...",
  "region": "ap-south-1",
  "resources": [],
  "detail": { "orderId": "abc", "amount": 99.5 }
}
```

### Rule example

```json
{
  "source": ["myapp.orders"],
  "detail-type": ["Order Created"],
  "detail": { "amount": [{ "numeric": [">=", 1000] }] }
}
```

→ Targets: Lambda, SQS, SNS, Step Functions, ECS task, API destination (HTTP), another bus (cross-account/region).

### Scheduled rules

```
rate(5 minutes)
cron(0 9 * * ? *)         # 9 AM UTC daily
```

### EventBridge Scheduler (newer service)

- One-time / recurring schedules at scale (1M+ schedules)
- Time zones, flexible time windows
- Per-schedule target — better than rule-per-schedule pattern

---

## Comparison Table

| | **SQS** | **SNS** | **EventBridge** |
|--|---------|---------|-----------------|
| Pattern | Queue (1 consumer) | Pub/Sub | Event router with rules |
| Push or Pull | **Pull** | **Push** | **Push** |
| Filtering | n/a (single queue) | Subscription filters | **Rich pattern matching** |
| Ordering | Best (Std) / Strict (FIFO) | Std/FIFO | Best-effort |
| Throughput | Very high | Very high | High |
| Targets | Consumers | SQS/Lambda/HTTP/Email/SMS | 30+ AWS targets, API destinations |
| Schedule | n/a | n/a | **Yes** |
| Schema | n/a | n/a | **Schema registry** |
| Replay | DLQ | n/a | **Archive + replay events** |
| Cost | Cheapest per msg | Cheap | Slightly higher per event |

---

## Pitfalls

1. **Visibility timeout < processing time** → duplicate work.
2. **Short polling** wasting money on empty receives.
3. **No DLQ** → poison pills loop forever, no alarm.
4. **FIFO with single MessageGroupId** for everything → throughput stuck at 3000/s aggregate.
5. **Large payloads** in SQS → use S3 + reference (extended client lib).
6. **SNS without filter policy** → every Lambda invoked for every event → bill explosion.
7. **EventBridge for high-volume per-message processing** → SQS cheaper.
8. **EventBridge schedule for 1M schedules** → use **EventBridge Scheduler** instead.
9. **No idempotency** in consumer (esp. SQS Standard) → duplicate side effects.
10. **DLQ left unmonitored** → silent failures pile up.

---

## Cheat Sheet

| Need | Pick |
|------|------|
| Decoupled queue | **SQS Standard** |
| Strict order | **SQS FIFO** |
| Fan-out to N consumers | **SNS** |
| SaaS event ingestion / rich routing | **EventBridge** |
| Cron jobs | **EventBridge Scheduler** |
| Push to SQS + Lambda + email | **SNS** |
| Cross-account events | **EventBridge** |
| Big payloads | SQS + S3 reference |
| Reliable consumer behavior | DLQ + alarm |

---

## Practice

1. Build **SNS → 3 SQS** fan-out: orders, emails, audit logs.
2. Convert short polling → long polling on a queue, observe cost drop.
3. Add **DLQ** with `maxReceiveCount=3`, simulate poison pill.
4. **EventBridge rule** matching `detail.amount >= 1000` → Lambda for VIP handling.
5. **EventBridge Scheduler** to invoke Lambda hourly, with retry policy.
6. Compare **SQS FIFO** vs **Standard** on duplicate behavior.

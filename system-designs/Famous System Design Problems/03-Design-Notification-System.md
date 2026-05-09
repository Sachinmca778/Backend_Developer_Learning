# Design: Notification System

## Status: Complete

---

## Table of Contents

1. [Requirements](#requirements)
2. [Channels](#channels)
3. [High-Level Architecture](#high-level-architecture)
4. [Queue / Topic Design](#queue--topic-design)
5. [Workers & Providers](#workers--providers)
6. [Retries & DLQ](#retries--dlq)
7. [User Preferences & Quiet Hours](#user-preferences--quiet-hours)
8. [Templates](#templates)
9. [Dedup & Idempotency](#dedup--idempotency)
10. [Notification Log](#notification-log)
11. [Scale & Ordering](#scale--ordering)
12. [Pitfalls](#pitfalls)
13. [Cheat Sheet](#cheat-sheet)

---

## Requirements

### Functional

- Send notifications via **Push (FCM/APNS)**, **SMS**, **Email**, **In-app**
- Respect **user preferences** (channel on/off, categories)
- **Quiet hours** — delay until window opens
- **Templates** with variables (`{{order_id}}`)
- **History** + status (queued / sent / failed)

### Non-functional

- High throughput (marketing bursts vs transactional priority)
- At-least-once delivery → **idempotent** consumers
- Provider rate limits (Twilio, SendGrid caps)

---

## Channels

| Channel | Provider examples | Notes |
|---------|-------------------|--------|
| Push Android | **Firebase Cloud Messaging (FCM)** | Device token registration |
| Push iOS | **Apple Push Notification service (APNS)** | Certificates / JWT auth |
| SMS | **Twilio**, Vonage | Cost per segment; compliance |
| Email | **SendGrid**, SES, Mailgun | Bounce webhooks |
| In-app | WebSocket / SSE + DB inbox | Persistent feed in app |

---

## High-Level Architecture

```
API / Event sources (order placed, mention, marketing campaign)
        │
        ▼
Notification Orchestrator Service
   ├── Validates prefs / quiet hours
   ├── Renders template
   ├── Writes notification_log (pending)
   └── Publishes events ──► Kafka (topics per channel OR single topic with routing key)
                │
    ┌───────────┼───────────┬──────────────┐
    ▼           ▼           ▼              ▼
Push Worker  SMS Worker  Email Worker   In-app Worker
    │           │           │              │
    ▼           ▼           ▼              ▼
 FCM/APNS    Twilio     SendGrid      User inbox DB + realtime fan-out
```

**Fan-out pattern**: orchestrator publishes **one logical notification**; workers handle channel-specific delivery.

---

## Queue / Topic Design

### Option A — Topic per channel

```
notifications.push
notifications.sms
notifications.email
notifications.inapp
```

✅ Isolated scaling / retry policies per channel  
❌ More topics to manage

### Option B — Single topic + type field

```
notifications.events   value: { user_id, channels[], payload, template_id }
```

Consumer groups filter — simpler piping, less isolation.

### Hybrid (common)

- **Transactional**: priority queue / separate topic (low latency)
- **Marketing**: bulk topic with rate limiting consumer

---

## Workers & Providers

- **Pull from Kafka / SQS** with batching (where provider supports)
- **Concurrency per provider** — respect Twilio QPS
- **Circuit breaker** when provider errors spike
- **Metrics**: lag, send latency, error rate by provider

---

## Retries & DLQ

```
Attempt 1 → fail → backoff 1s
Attempt 2 → fail → backoff 4s
Attempt 3 → fail → backoff 16s
Attempt 4 → DLQ + alert + mark log FAILED
```

- **Exponential backoff + jitter**
- **Poison message** — invalid token → don't infinite retry; update user device registry

---

## User Preferences & Quiet Hours

**prefs service** (could be DynamoDB / Postgres):

```
user_id → { email: true, push: true, sms: false, categories: { marketing: false } }
timezone, quiet_start, quiet_end
```

Orchestrator:

- If now in quiet hours → schedule delayed job (**Kafka delayed topic / SQS delay / Quartz**)
- Skip disabled channels entirely

---

## Templates

- Store in DB or CMS: `template_id`, channel, body, subject
- Render server-side with sandbox (avoid injection)
- **Localization** keys per locale

---

## Dedup & Idempotency

> "**Same order_shipped event twice** → user ko do SMS na jaye."

- **Idempotency key**: `(event_type, entity_id, channel)` unique in log table
- **Dedup window**: Redis `SETNX dedup:{key} 1 EX 86400`
- Workers check **processed** flag before calling provider

---

## Notification Log

Table / collection:

```
notification_id, user_id, channel, template_id, status, provider_msg_id,
timestamps, payload_snapshot, error_reason
```

Uses: support tickets, resend, compliance, analytics.

---

## Scale & Ordering

- **Ordering**: usually **per-user** ordering nice-to-have; cross-user irrelevant
- **Partition Kafka by user_id** for inbox ordering
- **Hot users**: separate rate bucket so one influencer doesn't starve others

---

## Pitfalls

1. **No idempotency** → duplicate pushes on retry
2. **Ignoring provider webhooks** (bounces, unsub) → reputation damage
3. **Marketing + transactional same queue** → OTP delayed
4. **PII in Kafka payloads** — minimize / encrypt
5. **FCM token stale** — remove on permanent failure
6. **Quiet hours bug** — wrong timezone math

---

## Cheat Sheet

| Piece | Choice |
|-------|--------|
| Bus | **Kafka** or **SQS+SNS** |
| Dedup | DB unique + Redis SETNX |
| Retry | Exponential backoff + **DLQ** |
| Prefs | Dedicated service + cache |

---

## Practice

1. Separate architecture for **OTP SMS** vs **weekly newsletter**.
2. Design **dead letter replay** tool safely (with dedup).
3. How would you implement **A/B test** on push copy?

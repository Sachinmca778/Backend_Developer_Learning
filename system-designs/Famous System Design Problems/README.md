# Famous System Design Problems

Interview-ready **classic system design problems** — requirements, capacity, APIs, high-level design, deep dives, trade-offs. Hinglish tone + diagrams + pitfalls.

---

## Topics & Status

| # | Problem | File | Status |
|---|---------|------|--------|
| 1 | URL Shortener | [01-Design-URL-Shortener.md](./01-Design-URL-Shortener.md) | Complete |
| 2 | Rate Limiter | [02-Design-Rate-Limiter.md](./02-Design-Rate-Limiter.md) | Complete |
| 3 | Notification System | [03-Design-Notification-System.md](./03-Design-Notification-System.md) | Complete |
| 4 | Twitter / X Feed | [04-Design-Twitter-Feed.md](./04-Design-Twitter-Feed.md) | Complete |
| 5 | WhatsApp / Chat | [05-Design-WhatsApp-Chat.md](./05-Design-WhatsApp-Chat.md) | Complete |
| 6 | YouTube / Netflix | [06-Design-YouTube-Netflix.md](./06-Design-YouTube-Netflix.md) | Complete |

---

## What's Inside Each File?

### [01 — URL Shortener](./01-Design-URL-Shortener.md)
Shorten + redirect + analytics. ID generation (**base62**, counter vs hash), collisions, SQL vs Cassandra, **Redis hot URLs**, custom aliases, TTL/expiry, **rate limiting per user**.

### [02 — Rate Limiter](./02-Design-Rate-Limiter.md)
Placement: client vs **gateway** vs middleware. **Redis INCR + EXPIRE** vs **Lua** sliding window. Headers (`X-RateLimit-*`, `Retry-After`). Soft vs hard limits. Scope: IP / user / API key / endpoint. Atomic race-handling.

### [03 — Notification System](./03-Design-Notification-System.md)
Channels: **FCM/APNS**, SMS, email, in-app. **Kafka topics per channel**, workers, providers. Retry + backoff. **Preferences + quiet hours**, templates, **dedup log**.

### [04 — Twitter Feed](./04-Design-Twitter-Feed.md)
**Fan-out on write vs read**, **hybrid for celebrities**. Timeline store (**Redis ZSET**), hot tweets cache, ranking hooks.

### [05 — WhatsApp / Chat](./05-Design-WhatsApp-Chat.md)
**WebSocket vs long polling**. **Cassandra** by `chat_id`. Delivery/read receipts. Groups + fan-out. **Presence** (Redis + TTL). **E2EE** note (Signal). Media → **S3 + URL**.

### [06 — YouTube / Netflix](./06-Design-YouTube-Netflix.md)
Chunk upload → **S3**, transcoding pipeline, **HLS/DASH + CDN**. Metadata DB + **Elasticsearch**. Recommendations sketch. **Approximate view counts** (Redis + batch).

---

## Recommended Order

```
1. URL Shortener      ← smallest scope, warm-up problem
2. Rate Limiter       ← standalone component (often asked solo)
3. Notification       ← queues + fan-out + reliability
4. Twitter Feed       ← fan-out hybrid + caching
5. WhatsApp           ← real-time + storage + presence
6. YouTube/Netflix    ← upload pipeline + CDN + streaming
```

---

## Companion Folders

- [System Design Fundamentals](../System%20Design%20Fundamentals/) — RESHADED, estimation, CAP/PACELC
- [Data Storage & Database Design](../Data%20Storage%20%26%20Database%20Design/) — caching, sharding, replication
- [Scalability & Infrastructure](../Scalability%20%26%20Infrastructure/) — LB, CDN, queues, gateway
- [Reliability & Fault Tolerance](../Reliability%20%26%20Fault%20Tolerance/) — HA, circuit breaker, rate limiting theory

---

## Quick Reference

| Problem | Core tension |
|---------|----------------|
| URL Shortener | Collision-free IDs vs scale |
| Rate Limiter | Accuracy vs memory vs latency |
| Notifications | Throughput vs provider SLA vs dedup |
| Twitter | Push vs pull vs hybrid |
| WhatsApp | Ordering, receipts, presence at scale |
| Video | Upload → transcode → CDN → ABR |

---

## Status Tracker

```
[x] 01 — URL Shortener
[x] 02 — Rate Limiter
[x] 03 — Notification System
[x] 04 — Twitter Feed
[x] 05 — WhatsApp / Chat
[x] 06 — YouTube / Netflix
```

> "**Interview mein answer perfect nahi hota — trade-offs clear bolna hi winning move.**"

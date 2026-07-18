# Famous System Design Problems - Top 10 Interview Questions & Answers

> Covering: URL Shortener, Rate Limiter, Notification System, Twitter Feed, WhatsApp Chat, and YouTube/Netflix style design trade-offs

---

## Q1: URL Shortener design mein core components kya hote hain?

**Answer:**

### Core flow:
1. Long URL input
2. Unique short code generate
3. Mapping store karo
4. Redirect endpoint short code resolve kare

```text
Client → API → ID Generator → DB
                 ↓
              Redis Cache
```

### Must-have concerns:
- Collision avoidance
- Fast redirect (<100ms)
- Custom alias support
- Analytics pipeline

**Short Interview Answer:**

I design URL shortener with a write path (generate + store mapping) and a read path (cache-first redirect), usually using base62 IDs, Redis for hot keys, and persistent DB for durability.

---

## Q2: URL short code generation ka best approach kya hai?

**Answer:**

Common options:

| Approach | Pros | Cons |
|----------|------|------|
| Global counter + Base62 | Simple, short codes | Predictable sequence |
| Hash(longURL) | Deterministic | Collision handling needed |
| Random code | Hard to predict | Collision checks required |

### Practical pattern:
- Counter service / Snowflake-style ID
- Base62 encode
- Collision check fallback

**Important:**

Security-sensitive systems mein sequential predictability avoid karne ke liye randomization/salting consider karo.

---

## Q3: Rate limiter system design question ka ideal answer structure kya hai?

**Answer:**

1. Clarify scope (user/IP/API-key/per-endpoint)
2. Choose algorithm (Token Bucket / Sliding Window)
3. Storage choice (Redis for distributed counters)
4. Atomicity (Lua script)
5. Response headers and 429 behavior

### Typical architecture:

```text
Client → API Gateway → Rate Limiter Service → Redis
                       allow / reject
```

**Short Interview Answer:**

I usually place distributed rate limiting at gateway, backed by Redis + Lua for atomic checks, and return 429 with Retry-After plus X-RateLimit headers.

---

## Q4: Notification system design mein toughest challenges kya hote hain?

**Answer:**

### Multi-channel pipeline:
- Email
- SMS
- Push (FCM/APNS)
- In-app

### Hard problems:
1. Fan-out at scale
2. Deduplication
3. User preferences + quiet hours
4. Provider failures and retries
5. Delivery tracking and observability

### Recommended pattern:
- Kafka topics per channel
- Worker pools
- Retry + DLQ
- Idempotency key per notification

---

## Q5: Twitter/X feed design mein fan-out on write vs fan-out on read kab use karte ho?

**Answer:**

| Strategy | Pros | Cons |
|----------|------|------|
| Fan-out on write | Fast read | Heavy write amplification |
| Fan-out on read | Lower write cost | Expensive read aggregation |

### Practical industry pattern:
**Hybrid**
- Normal users: fan-out on write
- Celebrity users: fan-out on read

### Data structures:
- Timeline cache: Redis ZSET
- Persistent store for durability

**Short Interview Answer:**

For feed systems, pure strategies rarely work at scale. Hybrid model handles celebrity skew while keeping average user reads fast.

---

## Q6: WhatsApp/chat design mein ordering aur delivery receipts kaise handle karte ho?

**Answer:**

### Real-time transport:
- WebSocket (preferred)
- Long polling fallback

### Message states:
1. Sent
2. Delivered
3. Read

### Ordering strategy:
- Per chat/conversation ordering guarantee
- Message IDs + server timestamps
- Sequence handling for retries/reconnect

### Storage:
- Cassandra-like write-optimized store by `chat_id`
- Media in object store (S3) + URL pointer

**Important:**

Global ordering unnecessary and expensive hota hai; per-conversation ordering enough for chat UX.

---

## Q7: Presence system (online/offline) scalable kaise banate ho?

**Answer:**

Presence highly volatile hota hai; DB-friendly workload nahi.

### Typical design:
- Redis in-memory store + TTL heartbeat
- Clients periodic heartbeat bhejte hain
- TTL expire = offline
- Pub/sub for presence updates

```text
last_seen:user123 = timestamp
online:user123 = true (TTL 60s)
```

**Short Interview Answer:**

I keep presence in Redis with heartbeat TTL, not in primary SQL DB, because presence is high-churn ephemeral state.

---

## Q8: YouTube/Netflix style video platform mein core pipeline kya hoti hai?

**Answer:**

### Pipeline:
1. Upload service
2. Object storage (S3/GCS)
3. Transcoding workers
4. Segmenting (HLS/DASH)
5. CDN distribution
6. Playback service + recommendations

```text
Upload → Object Store → Transcode Queue → Encoded variants → CDN → Player
```

### Key design concerns:
- Huge storage + bandwidth cost
- Adaptive bitrate streaming
- Global CDN strategy
- Async processing observability

---

## Q9: Famous SD problems mein consistency vs latency trade-offs kaise explain karte ho?

**Answer:**

| Problem | Strong consistency required? | Notes |
|---------|------------------------------|-------|
| URL redirect | Not always | Cache-first okay |
| Rate limiter counters | Mostly yes (per key window) | Atomic ops important |
| Notification delivery | Eventual okay | Dedup + retry needed |
| Feed ranking | Eventual okay | Latency critical |
| Chat message send | Medium-high | Per chat ordering critical |
| View count | Eventual okay | Batch aggregation fine |

**Short Interview Answer:**

I map consistency level to user impact: payments and quotas need stricter correctness, while feeds and analytics can be eventually consistent for better latency and scale.

---

## Q10: Interview mein famous problem solve karte waqt winning communication style kya hota hai?

**Answer:**

Use this sequence:
1. Clarify requirements + success metrics
2. Estimate scale
3. Draw high-level design
4. Explain data model
5. Deep dive on one bottleneck
6. Discuss failure handling
7. End with trade-offs + evolution plan

### Example ending line:

```text
This design handles current 50K RPS with cache-first read path and async processing. At 10x scale, we would introduce shard-by-tenant, multi-region replicas, and stronger observability around queue lag and tail latency.
```

**Important:**

Interviewer perfect architecture nahi dekh raha, clear thinking + trade-off clarity dekh raha hai.


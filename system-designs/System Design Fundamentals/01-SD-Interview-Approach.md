# SD Interview Approach (RESHADED)

## Status: Complete

---

## Table of Contents

1. [Why a Framework](#why-a-framework)
2. [The RESHADED Framework](#the-reshaded-framework)
3. [R — Requirements](#r--requirements)
4. [E — Estimation](#e--estimation)
5. [S — Storage / Data Model](#s--storage--data-model)
6. [H — High-Level Design](#h--high-level-design)
7. [A — API Design](#a--api-design)
8. [D — Detailed Design](#d--detailed-design)
9. [E — Evaluation / Trade-offs](#e--evaluation--trade-offs)
10. [Time Budget (45-min interview)](#time-budget-45-min-interview)
11. [Communication Tips](#communication-tips)
12. [Pitfalls](#pitfalls)
13. [Cheat Sheet](#cheat-sheet)

---

## Why a Framework

> "**SD interviews open-ended hote hain.** Without a framework you ramble or panic. RESHADED = consistent way to **drive the conversation** + show structured thinking."

Interviewers signal-track:

- Can you **clarify** ambiguous problems?
- Can you **estimate** scale realistically?
- Can you **make trade-offs** between options?
- Do you **communicate** before drawing?
- Do you handle **failure modes** + scale (10×, 100×)?

Framework gives a checklist so you don't forget any of these.

---

## The RESHADED Framework

```
R  → Requirements    (functional + non-functional)
E  → Estimation      (QPS, storage, bandwidth, memory)
S  → Storage         (data model, choose DB)
H  → High-Level      (boxes + arrows architecture)
A  → API Design      (endpoints / RPC / events)
D  → Detailed Design (deep dive into 1-2 components)
E  → Evaluation      (trade-offs, bottlenecks, scale, failures)
```

> Mnemonic: **R**ahul **E**veryday **S**imple **H**indi **A**ur **D**ekho **E**xplain karta hai.

---

## R — Requirements

> "**5 minutes most important.** Don't start designing until you know the problem precisely."

### Functional requirements (what the system does)

> "**User-facing features.** Verbs: post, like, search, message, upload."

Ask:

- Who are the users?
- Top **3–5 features** in scope (interviewer prioritizes)
- What is **out of scope** (auth? payments? recommendations?)

Example (Twitter-lite):

- User can post tweet (text only, ≤280 chars)
- User can follow other users
- Home timeline = tweets from followed users
- **Out of scope**: DM, video, ads, search

### Non-functional requirements (how well)

> "**Quality attributes.** Numbers + adjectives."

Always ask:

| Attribute | Question |
|-----------|----------|
| **Scale** | DAU? Peak QPS? Read/write ratio? |
| **Latency** | p50 / p99 SLA (e.g., timeline loads in <200 ms p99) |
| **Availability** | 99.9% (8.77 h/yr down) vs 99.99% (52 min/yr) |
| **Consistency** | Strong? Eventual? OK if user sees own write 2 s late? |
| **Durability** | Can we lose tweets? (No — write must persist) |
| **Geo** | Single region or global? |
| **Cost / budget** | (rare in interviews, but good to ask) |

### Pin the assumptions

Write on board. Interviewer agrees → you build to those numbers.

---

## E — Estimation

> "**5 minutes of math.** Don't go too deep — establish order-of-magnitude."

(Full details: see [02 — Capacity Estimation](./02-Capacity-Estimation.md).)

Standard checklist:

- **QPS** (read + write separately)
- **Storage** per year / per 5 years
- **Bandwidth** in/out
- **Memory** for hot working set / cache

### Example estimation (Twitter-lite)

```
DAU            = 200M
Tweets/user/day= 0.5         → write QPS  = 100M / 86400 ≈ 1160/s peak ≈ 5×= 6K/s
Reads/user/day = 50          → read QPS   = 10B / 86400  ≈ 116K/s peak ≈ 600K/s
Tweet size     = 300 B (text + meta)
Storage/year   = 100M × 365 × 300 B ≈ 11 TB/year
Bandwidth-out  = 600K × 300 B ≈ 180 MB/s
```

→ Numbers tell you: SQL primary won't handle 600K QPS reads → need cache + read replicas / fan-out.

---

## S — Storage / Data Model

> "**What entities? Which database?**"

Steps:

1. **List entities** (User, Tweet, Follow, Like)
2. **Sketch schema** (id, fk, indexes)
3. **Choose DB type per entity**:
   - Relational (Postgres/MySQL/Aurora) — strong consistency, joins
   - Key-value (DynamoDB, Redis) — single-digit ms lookups
   - Document (MongoDB) — flexible schema
   - Wide-column (Cassandra) — write-heavy time-series
   - Graph (Neptune) — relationships
   - Search (Elasticsearch / OpenSearch) — full-text

### Example mapping (Twitter-lite)

| Data | Store | Why |
|------|-------|-----|
| User profile | Postgres | Strong consistency, relational |
| Tweet | **Cassandra** or sharded MySQL | Write-heavy, time-ordered |
| Follow graph | **Redis Sets / Graph DB** | Fast neighbor lookup |
| Home timeline | **Redis ZSET** (precomputed) | Sub-ms reads |
| Search index | Elasticsearch | Full-text queries |

> Justify each choice with **CAP/PACELC** + access pattern.

---

## H — High-Level Design

> "**Boxes and arrows** — show data flow at architecture level."

Components to consider:

- **Client** (web, mobile)
- **CDN / Edge** (CloudFront, Cloudflare)
- **Load balancer** (ALB)
- **API Gateway**
- **App tier** (stateless services)
- **Cache** (Redis)
- **Database(s)**
- **Message queue** (SQS / Kafka)
- **Async workers**
- **Object storage** (S3 for media)
- **Search index**
- **Monitoring** (CloudWatch / Datadog)

### Example (Twitter-lite read path)

```
Client → CDN → ALB → API Gateway → Timeline Service
                                       ↓
                                   Redis (ZSET timeline) — hit
                                       ↓ (miss)
                                   Cassandra (tweets) + DB join
```

### Example (Twitter-lite write path)

```
Client → ALB → API Gateway → Tweet Service
                                ↓
                            Cassandra (write tweet)
                                ↓
                            Kafka topic "new_tweet"
                                ↓
                  Fan-out worker → Redis ZSET (followers' timelines)
```

> Talk while drawing. Explain **why** each box exists.

---

## A — API Design

> "**Define the public contract.** REST / gRPC / WebSocket / GraphQL — pick + justify."

For each main feature, write 1-line API:

```
POST   /v1/tweets                  body: { text }            → 201 { tweetId }
GET    /v1/users/{id}/timeline     ?cursor=xxx&limit=20      → { tweets[], next }
POST   /v1/users/{id}/follow       (auth: target user)        → 204
GET    /v1/tweets/{id}                                        → { tweet }
```

### What to call out

- **Auth**: JWT? session? API key?
- **Pagination**: cursor (recommended) vs offset
- **Rate limits**: 1000 req/min per user
- **Idempotency**: idempotency-key header on `POST` to avoid duplicate tweets on retry
- **Versioning**: `/v1/`

### When to use what

| Style | Use |
|-------|-----|
| REST / HTTP | Most public APIs |
| gRPC | Internal microservices, low latency |
| WebSocket | Real-time push (chat, notifications) |
| GraphQL | Multiple front-end clients with varied needs |
| Server-Sent Events | One-way live updates (lighter than WS) |

---

## D — Detailed Design

> "**Pick 1–2 components, deep-dive.** Interviewer steers — follow signals."

Common deep-dive areas:

1. **Database sharding strategy**
2. **Cache eviction & consistency**
3. **Notification fan-out** (push vs pull, hybrid)
4. **Search indexing pipeline**
5. **Idempotency / dedup**
6. **Rate limiting algorithm**
7. **Geo-distribution / failover**

### Example: Timeline fan-out (push vs pull)

| Approach | Pros | Cons |
|----------|------|------|
| **Pull** (compute on read) | Simple writes, low storage | Read path slow, expensive at high QPS |
| **Push (fan-out on write)** | Fast reads (precomputed) | Write amplification (1 tweet → 1M writes for 1M followers) |
| **Hybrid** (push for normal, pull for celebrities) | Best of both | More complex |

→ Pick **hybrid**, justify with celebrity-effect numbers.

---

## E — Evaluation / Trade-offs

> "**Last 5 min** — show senior thinking."

Discuss:

- **Bottlenecks** at current scale + 10×
- **Single points of failure** + mitigation
- **Failure modes** (DB primary down, cache cold, region outage)
- **Consistency trade-offs** (eventual vs strong where + why)
- **Cost considerations** (Redis cluster vs DB read replicas)
- **What you'd do differently with more time / data**

### Common closing prompts

- "If this had to scale 10×, what breaks?"
- "If 1 region goes down, what's the user impact?"
- "How would you A/B test new ranking logic?"

---

## Time Budget (45-min interview)

| Phase | Minutes |
|-------|---------|
| Intro / problem statement | 2 |
| **R — Requirements** | **5** |
| **E — Estimation** | **5** |
| **S — Storage / data model** | **5** |
| **H — High-level design** | **8** |
| **A — API design** | **3–5** |
| **D — Deep dive (1–2 components)** | **10–12** |
| **E — Trade-offs / scale / failure** | **5** |

Adjust per interviewer cues — if they want **D** more, give them more.

---

## Communication Tips

1. **Speak before drawing** — narrate intent, then sketch.
2. **Ask before assuming** — "Can I assume reads >> writes?" → check.
3. **Trade-offs explicit** — "Option A is X, Option B is Y, I'll go with A because…"
4. **Numbers, not adjectives** — "2 KB tweets" not "small tweets".
5. **Self-correct calmly** — "Wait, that won't scale, let me reconsider…"
6. **Drive the conversation** — don't wait for prompts; say what you'll cover next.
7. **Diagram cleanliness** — boxes, arrows with labels (sync vs async, REST vs Kafka).
8. **Confirm understanding** — "Does this match what you had in mind?" before deep dive.

---

## Pitfalls

1. **Jumping to design** without requirements clarification.
2. **Ignoring numbers** — solution doesn't match scale.
3. **Picking buzzword DB** (Cassandra for 100 users) — unjustified.
4. **No cache** in a read-heavy design.
5. **No failure mode** discussion.
6. **Too deep too early** in one component — runs out of time elsewhere.
7. **Single AZ / region** with no DR mention.
8. **Boxes without arrows / arrows without labels** — interviewer can't follow.
9. **Defending bad choice** stubbornly — okay to update mid-way.
10. **No estimation** — interviewer assumes you can't do math.

---

## Cheat Sheet

| Phase | Output |
|-------|--------|
| **R** | Functional list + NFR table (scale, latency, availability) |
| **E** | QPS, storage, bandwidth, memory |
| **S** | Entities + DB choice with 1-line justification |
| **H** | Boxes-arrows architecture |
| **A** | 3–5 endpoint signatures |
| **D** | 1–2 deep dives with trade-off table |
| **E** | Bottlenecks, SPOFs, scale 10×, failure scenarios |

| Interview signal | Show by |
|------------------|---------|
| Clarity | Asking before assuming |
| Math | Numbers, not adjectives |
| Trade-offs | Comparison tables |
| Senior thinking | Failure modes, future scale |
| Communication | Narrating + clean diagrams |

---

## Practice

1. Run RESHADED on a paper for **URL shortener** in 30 min — out loud.
2. Same for **rate limiter** — compare token bucket vs sliding window in detailed design.
3. Same for **WhatsApp** — focus on push vs pull for messages.
4. Time-box yourself; force the 5-5-5-8-5-12-5 split.
5. Record yourself; rewatch — am I narrating or just drawing?

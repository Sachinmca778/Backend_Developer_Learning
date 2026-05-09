# Design: Twitter / X Home Timeline

## Status: Complete

---

## Table of Contents

1. [Requirements](#requirements)
2. [Core Tension: Fan-out](#core-tension-fan-out)
3. [Fan-out on Write (Push)](#fan-out-on-write-push)
4. [Fan-out on Read (Pull)](#fan-out-on-read-pull)
5. [Hybrid Model](#hybrid-model)
6. [Timeline Storage](#timeline-storage)
7. [Ranking & Mixed Feed](#ranking--mixed-feed)
8. [Tweet Creation Flow](#tweet-creation-flow)
9. [Read Path](#read-path)
10. [Scale Numbers](#scale-numbers)
11. [Pitfalls](#pitfalls)
12. [Cheat Sheet](#cheat-sheet)

---

## Requirements

- Post tweet (text / media metadata)
- Follow / unfollow users
- **Home timeline**: recent tweets from people you follow (chronological or ranked)
- Like / retweet / reply (extensions)
- **Celebrity problem**: some users have **50M+ followers**

---

## Core Tension: Fan-out

| Approach | Read | Write |
|----------|------|-------|
| **Push** (fan-out on write) | ⚡ Fast — precomputed feeds | 🐢 Slow for mega-followers |
| **Pull** (fan-out on read) | 🐢 Slow — merge many sources | ⚡ Cheap writes |

Interview golden answer: **hybrid**.

---

## Fan-out on Write (Push)

```
New tweet from user U
   │
   ▼
Fan-out service: for each follower F of U
      └──► INSERT into F's home timeline (Redis ZSET / shard)
```

- Home timeline = **materialized view** per user
- Read = **single key fetch** — merged sorted list

**Problem**: User with **10M followers** → 10M writes per tweet → seconds + DB storm.

Mitigations:

- **Async fan-out** via queue (Kafka), shard workers by follower ranges
- Still expensive for celebs

---

## Fan-out on Read (Pull)

```
GET timeline(user=F)
   ├── fetch list of followees (cache)
   ├── for each followee (or top-K active), fetch recent tweets (merge)
   └── sort + truncate top N
```

- Writes cheap (only author's tweet store)
- Reads expensive — **avoid** for home timeline at Twitter scale for average user

Useful for:

- **Low follower count** lists
- **Second-pass** merging when combining push + celeb pull

---

## Hybrid Model

> "**Push for normal users** (e.g., followers < X threshold). **Pull / selective push for celebrities** (only fan-out to **cache layer** or store tweet in celeb's outbox; followers merge celeb tweets at read time)."

Typical constants (illustrative):

```
THRESHOLD = 10_000 followers

if follower_count < THRESHOLD:
    fan-out on write (async)
else:
    don't fan-out per follower; store in celebrity_tweets cache / separate structure

Read path:
    merged = push_timeline(user) UNION pull_recent(celebrity_followees)
    sort by score (time or ranking model)
```

Also: **fan-out to Redis only** for hot paths; colder storage tier for inactive users.

---

## Timeline Storage

### Redis `ZSET`

```
Key: timeline:{user_id}
Score: tweet_id (Snowflake — time-sortable) OR epoch_ms inverted
Member: tweet_id
```

- `ZREVRANGE timeline:user123 0 50` → latest 50  
- Trim with `ZREMRANGEBYRANK` to cap memory per user

### Durability

- Redis is **cache + fast serving**; source of truth tweets in **Cassandra / sharded SQL**

### Fan-out write

```
ZADD timeline:{follower_id} {tweet_ts_score} {tweet_id}
```

Batch pipeline for throughput.

---

## Ranking & Mixed Feed

Chronological first; production adds:

- **ML ranker** scores candidates → re-order top pool
- **Ads injection** slots
- **Mixed media** cards

Architecture: **candidate generation** (push + pull + reco) → **light ranker** → **heavy ranker** (async precompute scores).

---

## Tweet Creation Flow

```
Client → Tweet Service
           ├── Persist tweet (tweet_id, user_id, text, ts) → Cassandra/SQL shard by user_id or tweet_id
           ├── Publish event tweet.created → Kafka
           └── Return 201

Fan-out consumers:
           └── For each follower batch → update Redis timelines (push mode)
Search indexer / ML features → separate consumers
```

---

## Read Path

```
GET /timeline
   │
   ├─► Redis ZSET merge (fast path hit)
   │
   └─► miss / cold user → partial reconstruct + warm cache

Hydrate tweet bodies (batch get by tweet_id) from tweet store
```

Use **pipeline / multiget** to avoid N+1.

---

## Scale Numbers (Interview style)

Assume **500M DAU**, **50 timeline reads/user/day**:

```
Reads ≈ 25B/day → ~290K QPS average → peak ~1–2M QPS
Writes lower but fan-out multiplier dangerous
```

→ **Sharding timeline Redis** by user_id ranges; **many clusters**.

---

## Pitfalls

1. **Pure push for celebs** — write amplification kills
2. **Pure pull for everyone** — read latency unacceptable
3. **No timeline cap** — Redis memory infinite per inactive user
4. **Ordering across shards** — need global tweet ID ordering (Snowflake)
5. **Stale follow graph** — unfollow must remove future injection / purge entries
6. **Eventually consistent fan-out** — user sees own tweet late — special-case **merge own tweets immediately**

---

## Cheat Sheet

| Topic | Answer |
|-------|--------|
| Celebrity issue | **Hybrid push + pull** |
| Timeline store | **Redis ZSET** + durable tweet DB |
| Ordering | Time-sortable **tweet ID** |
| Fan-out | Async **Kafka** workers |

---

## Practice

1. Pick threshold X — justify fan-out vs pull trade-off.
2. Design unfollow: how to remove influencer tweets from materialized timeline?
3. How does **mutual follow** affect messaging vs timeline?

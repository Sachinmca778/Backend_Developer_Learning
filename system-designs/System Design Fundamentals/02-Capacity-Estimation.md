# Capacity Estimation

## Status: Complete

---

## Table of Contents

1. [Why Estimate](#why-estimate)
2. [Units & Conversions](#units--conversions)
3. [QPS Estimation](#qps-estimation)
4. [Read vs Write QPS](#read-vs-write-qps)
5. [Peak Factor](#peak-factor)
6. [Storage Estimation](#storage-estimation)
7. [Bandwidth Estimation](#bandwidth-estimation)
8. [Memory / Cache Sizing](#memory--cache-sizing)
9. [Latency Numbers Every Programmer Should Know](#latency-numbers-every-programmer-should-know)
10. [Worked Examples](#worked-examples)
11. [Pitfalls](#pitfalls)
12. [Cheat Sheet](#cheat-sheet)

---

## Why Estimate

> "**Numbers drive design choices.** 100 QPS → single Postgres. 100K QPS → cache + sharding + queue. Without numbers, you're guessing buzzwords."

Interviewer is checking:

- **Order-of-magnitude intuition** (10K vs 1M is huge)
- **Right unit** (MB vs GB vs TB)
- **Justification chain** — DAU → QPS → infra

You don't need exact numbers. You need **defensible** ones.

---

## Units & Conversions

### Bytes

```
1 byte   = 8 bits
1 KB     = 1024 B            (~10³)
1 MB     = 1024 KB           (~10⁶)
1 GB     = 1024 MB           (~10⁹)
1 TB     = 1024 GB           (~10¹²)
1 PB     = 1024 TB           (~10¹⁵)
```

> Marketing & disks often use **1 KB = 1000 B** (decimal). For SD interviews use **base 1024** unless asked. The order of magnitude is what matters.

### Time

```
1 day    = 86,400 seconds   ≈ 100,000 (handy approx)
1 month  = 2.5M seconds     (~30 days)
1 year   = 31.5M seconds    (~3 × 10⁷)
```

### Power-of-10 mental table

```
10³  = 1K  = thousand
10⁶  = 1M  = million
10⁹  = 1B  = billion
10¹² = 1T  = trillion
```

### Useful equivalences

```
1M req/day        ≈ 12 req/sec
86M req/day       ≈ 1000 req/sec  (1K QPS)
1B req/day        ≈ 11.6K req/sec (~10K QPS)
100B req/day      ≈ 1.16M req/sec (~1M QPS)
```

---

## QPS Estimation

### Formula

```
QPS = (DAU × actions_per_user_per_day) / 86,400
```

### Examples

```
10M DAU × 5 reads/day  = 50M reads/day  / 86400  ≈ 580 QPS
100M DAU × 50 reads/day = 5B reads/day  / 86400  ≈ 58K QPS
1B  DAU × 100 reads/day = 100B reads/day/ 86400  ≈ 1.16M QPS
```

> Tip: keep `1 day ≈ 100K seconds` in head — divide by 100K, then adjust by 15% (since 100,000 / 86,400 ≈ 1.16).

### From MAU to DAU

Common rule of thumb: **DAU ≈ 20–30% of MAU** (depends on app stickiness).

```
1B MAU → 200–300M DAU (Facebook-like)
```

---

## Read vs Write QPS

> "Most user-facing systems have **read-heavy** workloads. Always ask the ratio."

| App | Read : Write |
|-----|--------------|
| Twitter / news feed | 1000 : 1 |
| Instagram | 100 : 1 |
| Banking transactions | 5 : 1 |
| WhatsApp messages | 1 : 1 (each message is read once usually) |
| Analytics ingestion | 1 : 100 (reads are dashboards, far fewer than events) |

→ Drives DB choice (read replicas, cache, write-optimized DB).

---

## Peak Factor

> "**Average QPS** ≠ **Peak QPS.** Design for peak. Most apps see **3–5× peak** during business hours / events."

```
Average QPS = 10K
Peak QPS    ≈ 30–50K   (3–5× factor)
Burst QPS   ≈ 100K     (sale / launch / viral event — 10×)
```

> Always state: "Average X, planning capacity for **3× peak** = Y."

---

## Storage Estimation

### Formula

```
Storage = records × bytes_per_record × retention_factor
```

### Steps

1. Estimate **records added per day** (writes/day or events/day)
2. Estimate **average size** per record (in bytes)
3. Multiply by **retention period** (1 year, 5 years, forever)
4. Add **replication factor** (e.g., RF=3) and **indexes** (~1.5–2× of data)

### Example: tweet storage

```
Tweets/day        = 100M
Avg size          = 300 B (text + metadata)
Storage/day       = 100M × 300 B = 30 GB/day
Storage/year      = 30 GB × 365  ≈ 11 TB/year
With RF=3         = 33 TB/year
With indexes ~1.5× = ~50 TB/year
```

→ Now you can say "5-year storage ≈ 250 TB → need partitioning + multi-shard cluster".

### Media estimation

```
Images:   ~200 KB avg (after compression)
Videos:   ~5 MB / 30 sec clip
Audio:    ~1 MB / minute (compressed)
```

### Logs / events

```
Log line:  ~500 B
Event JSON: ~1 KB

10K QPS × 1 KB × 86400 s = 864 GB/day raw
With compression (~10×) = ~85 GB/day stored
```

---

## Bandwidth Estimation

### Formula

```
Bandwidth = QPS × payload_size
```

### Example

```
600K read QPS × 300 B/tweet = 180 MB/s outbound
With 5K writes × 300 B      = 1.5 MB/s inbound
```

→ At 180 MB/s = ~1.4 Gbps sustained → influences **CDN choice** + **edge caching strategy**.

### Network math shortcuts

```
1 Gbps = 125 MB/s
10 Gbps = 1.25 GB/s
100 Gbps = 12.5 GB/s
```

---

## Memory / Cache Sizing

> "**Working set** = hot data accessed frequently. Rule of thumb: **20% of data drives 80% of traffic** (Pareto)."

### Formula

```
Cache size ≈ 0.2 × total_dataset_size
```

### Example: tweet timeline cache

```
Total active users     = 50M (out of 200M DAU, last 30 days hot)
Avg cached items/user  = 200 (last 200 timeline tweets)
Bytes per cached item  = 100 B (id + minimal meta)

Memory = 50M × 200 × 100 B = 1 TB
```

→ With Redis cluster of `r6g.4xlarge` (130 GB each) → ~8 nodes × 2 (replication) = **16 nodes**.

### TTL planning

- Cache **doesn't grow forever** — TTL evicts cold keys
- Eviction policy: `allkeys-lru` for pure cache; `volatile-lru` if mixing TTL + persistent

---

## Latency Numbers Every Programmer Should Know

> "**Jeff Dean's classic** — burned into every backend engineer's head."

| Operation | Latency |
|-----------|---------|
| L1 cache reference | 0.5 ns |
| Branch mispredict | 5 ns |
| L2 cache reference | 7 ns |
| Mutex lock/unlock | 25 ns |
| Main memory reference | 100 ns |
| Compress 1 KB w/ Snappy | 3 µs (3,000 ns) |
| Send 1 KB over 1 Gbps network | 10 µs |
| Read 4 KB from SSD | 150 µs |
| Read 1 MB sequentially from memory | 250 µs |
| Round-trip within same DC | 500 µs |
| Read 1 MB sequentially from SSD | 1 ms |
| Disk seek (HDD) | 10 ms |
| Read 1 MB sequentially from HDD | 20 ms |
| Round-trip CA → Netherlands → CA | 150 ms |

### Implications for design

- **Network call > 1000× memory** — keep things local when possible
- **Cross-region call ~150 ms** — bad for sync user-facing flows
- **Disk seek 10 ms** — hot data must be in memory or SSD
- **CDN edge ~10 ms** vs origin ~100 ms — geography matters

---

## Worked Examples

### Example 1 — URL Shortener

**Requirements**

- 100M URLs created/month
- Avg 100 clicks per URL
- Read-heavy

**Estimation**

```
Writes:  100M / month / 30 days / 86400 ≈ 38 QPS
Reads:   100M × 100 / month / 30 / 86400 ≈ 3,800 QPS

Storage:
  Per record: 500 B (long URL + short + meta + analytics)
  Per year: 100M × 12 × 500 B = 600 GB/year
  5-year:   ~3 TB total

Cache (top 20% of URLs):
  20M × 500 B = 10 GB → easily fits Redis single node

Bandwidth (read): 3800 × 500 B = ~1.9 MB/s
```

→ Easy: single Postgres + Redis cache. No sharding needed at this scale.

### Example 2 — Chat App (WhatsApp-lite)

**Requirements**

- 500M DAU
- 50 messages/user/day average
- Avg msg size: 200 B
- Retention: 1 year on server, after that S3 archive

**Estimation**

```
Messages/day = 500M × 50 = 25B/day
Write QPS    = 25B / 86400 ≈ 290K QPS
Peak (3×)    ≈ 870K QPS

Storage:
  25B × 200 B = 5 TB/day
  1 year      = ~1.8 PB
  With RF=3   = 5.5 PB
```

→ Need wide-column DB (Cassandra/Scylla) sharded by `chat_id`, retention tier (hot 90d in DB, archive in S3).

### Example 3 — Video Streaming (YouTube-lite)

**Requirements**

- 1B DAU
- 30 min watch time/user/day
- HD = ~5 Mbps stream

**Bandwidth**

```
Concurrent watchers (peak)  ≈ 1B × (30 min / 24h) × 3 (peak factor) ≈ 60M concurrent
Bandwidth per viewer        = 5 Mbps
Total egress                = 60M × 5 Mbps = 300 Tbps
```

→ Impossible from single origin → **CDN mandatory** (CloudFront / global edge), HLS chunks served from edge.

---

## Pitfalls

1. **No peak factor** — designing for average misses launch day.
2. **Confusing bits and bytes** (Mbps vs MB/s — 8× difference).
3. **Forgetting replication** in storage estimates (RF=3 means 3× space).
4. **Forgetting indexes** (often add 50–100% to data size).
5. **Cache size = total data** — wasteful; 20% hot is enough.
6. **No retention thinking** — "infinite retention" → infinite cost.
7. **Per-user math wrong** — DAU vs MAU mix-up.
8. **Bandwidth from single server** vs distributed — design depends on it.
9. **Latency budgets ignored** — sync 5 cross-region calls = 750 ms baseline.
10. **Estimation theatre** — pulling random numbers without justification.

---

## Cheat Sheet

| Quick math |  |
|------------|--|
| 1M req/day | ≈ **12 QPS** |
| 1B req/day | ≈ **12K QPS** |
| 1 day | ≈ **100K seconds** (~1.16 actual factor) |
| Peak factor | **3–5×** average |
| MAU → DAU | **20–30%** |
| Reads/Writes | **100:1** typical social, **1000:1** read-heavy |
| Cache size | **~20%** of dataset (hot working set) |
| Index overhead | **+50% to +100%** of data |
| Replication factor | **3** typical (loss tolerance) |

| Speed of |  |
|----------|--|
| Same DC RTT | **0.5 ms** |
| Cross-region RTT | **50–150 ms** |
| Memory read | **100 ns** |
| SSD read 4 KB | **150 µs** |
| Disk seek | **10 ms** |

| Bandwidth |  |
|-----------|--|
| 1 Gbps | 125 MB/s |
| 10 Gbps | 1.25 GB/s |

---

## Practice

1. Estimate Twitter timeline read QPS at 200M DAU, 50 reads/day each.
2. Estimate 5-year storage for log events at 50K QPS, 500 B per event, 10× compression.
3. Bandwidth needed for 100M concurrent video viewers @ 4 Mbps.
4. Cache size for top 20% of 1B users (200 B per profile cached).
5. Convert: 5 Tbps = how many MB/s? How many concurrent 5 Mbps streams?

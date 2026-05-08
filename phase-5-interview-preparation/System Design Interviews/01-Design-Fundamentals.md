# Design Fundamentals

## Status: Not Started

---

## Table of Contents

1. [Interview Framework](#interview-framework)
2. [Requirements Gathering](#requirements-gathering)
3. [Functional vs Non-Functional](#functional-vs-non-functional)
4. [Back-of-Envelope Estimation](#back-of-envelope-estimation)
5. [CAP Theorem](#cap-theorem)
6. [PACELC (CAP Extension)](#pacelc-cap-extension)
7. [ACID](#acid)
8. [BASE](#base)
9. [Eventual Consistency](#eventual-consistency)
10. [Common Output Traps](#common-output-traps)
11. [Pitfalls](#pitfalls)
12. [Cheat Sheet](#cheat-sheet)

---

## Interview Framework

```
[Clarify] → [Estimate] → [High-Level Design] → [Deep Dive] → [Trade-offs]
   5 min       5 min         10 min               15 min        5-10 min
```

### Why structure matters

Without it: rambling, missing key components, no trade-offs articulated. Interviewer maps your structure to "senior engineer thinking".

---

## Requirements Gathering

### Always ask first — never assume

**Bad:** "I'll design Twitter."
**Good:** "Twitter has many features — should I focus on **timeline + tweet posting**, or include **search / DMs / trends**?"

### Standard clarifying questions

```
1. Use case — main features?
2. Users — DAU? read/write ratio?
3. Scale — global / regional?
4. Data volume — how much per record? retention?
5. Consistency — strong / eventual?
6. Latency target — P50 / P99 budgets?
7. Availability — 99.9% / 99.99%?
8. Out-of-scope — explicitly state
```

→ Lock down scope; otherwise infinite design.

---

## Functional vs Non-Functional

| | Functional | Non-Functional |
|--|-----------|----------------|
| Question | What does the system do? | How well does it perform? |
| Examples | Post tweet, follow user | 99.99% uptime, < 200ms latency |
| Output | Features list | SLOs / SLAs |

### Standard non-functional list

| NFR | Typical target |
|-----|----------------|
| **Scalability** | Handle X DAU, Y QPS |
| **Availability** | 99.9% / 99.99% / 99.999% |
| **Latency** | P95 < 200ms, P99 < 500ms |
| **Consistency** | Strong / eventual |
| **Durability** | 99.999999999% (11 9s S3) |
| **Throughput** | X requests/sec |
| **Security** | Auth, encryption, audit |

→ Pick 3-4 critical NFRs based on use case.

### Examples by use case

| System | Top NFRs |
|--------|----------|
| Banking | Strong consistency, durability, audit |
| Twitter feed | Availability, low read latency, eventual consistency |
| Real-time chat | Low latency (< 50ms), availability |
| Analytics dashboard | Throughput, eventual consistency |

---

## Back-of-Envelope Estimation

### Standard derivation

```
DAU                = 100M
Posts per user/day = 2
Total posts/day    = 200M

Sec/day            = 86,400 ≈ 10⁵

Write QPS          = 200M / 10⁵ = 2,000
Read QPS (100x)    = 200,000     // social = read-heavy
```

### Storage (1 year)

```
Avg post size      = 1 KB (text + metadata)
Posts/year         = 200M × 365 = 73B
Storage            = 73B × 1KB = 73 TB
+ 50% indexes/replicas → ~110 TB
```

### Bandwidth

```
Read QPS           = 200,000
Avg payload        = 5 KB (post + image thumbnail)
Read bandwidth     = 200,000 × 5 KB = 1 GB/sec
```

### Memory for cache

```
Hot data 80/20     = 20% of DAU active per moment = 20M users
Cache per user     = 1 MB
Cache size         = 20 TB → distributed Redis cluster
```

### Memorize powers

| | |
|--|--|
| 10³ | thousand |
| 10⁶ | million |
| 10⁹ | billion |
| 10¹² | trillion |
| 2¹⁰ | 1024 ≈ 10³ |
| 2²⁰ | ≈ 10⁶ |
| 2³² | ≈ 4 × 10⁹ |
| 2⁶⁴ | ≈ 1.8 × 10¹⁹ |

### Useful conversions

```
1 day  = 86,400 sec  ≈ 10⁵ sec
1 month = 2.6 × 10⁶ sec
1 year = 3.15 × 10⁷ sec
```

→ "QPS = events/day / 10⁵" — round-up shortcut.

---

## CAP Theorem

> "In a distributed system, **during a network partition** you must choose between **Consistency** and **Availability**."

```
            C
           / \
          /   \
         /     \
        P-------A
```

### Definitions

| Letter | Meaning |
|--------|---------|
| **C** Consistency | Every read sees most recent write (or error) |
| **A** Availability | Every request gets a non-error response |
| **P** Partition Tolerance | System keeps operating despite network split |

### Common confusion

> "Pick 2 of 3" — popular shorthand but **misleading**.

→ **P** is **not optional** in distributed systems (network failures real). Real choice: when partition happens, **CP or AP**.

### CP example

- Banking systems
- During partition, refuse writes (return error) → preserve consistency

### AP example

- Twitter feed
- During partition, serve stale data → preserve availability

### Examples by system

| System | CP / AP |
|--------|---------|
| HBase, MongoDB (default) | CP |
| Cassandra, DynamoDB | AP (tunable) |
| ZooKeeper, etcd | CP |
| Redis (with replication) | AP |
| PostgreSQL primary-only | CP (single node — partition rare) |

---

## PACELC (CAP Extension)

> "If **P**artition: choose **A**vailability or **C**onsistency. **E**lse: choose **L**atency or **C**onsistency."

```
           PA / PC
              |
              |
           EL / EC
```

→ Even without partition, distributed systems trade **latency for consistency**.

### Examples

| System | Partition behavior | No-partition behavior |
|--------|-------------------|----------------------|
| Cassandra | PA (available) | EL (low latency, eventual) |
| MongoDB | PC (refuse writes if leader unreachable) | EC (synchronous to leader) |
| DynamoDB | PA | EL or EC (configurable) |

→ Realistic system design discussion uses PACELC to capture **everyday trade-offs**, not just rare partitions.

---

## ACID

**For traditional RDBMS transactions.**

| Letter | Meaning |
|--------|---------|
| **A** Atomicity | All or nothing |
| **C** Consistency | Valid state → valid state (constraints hold) |
| **I** Isolation | Concurrent TX behave as if serial |
| **D** Durability | Once committed, persists despite crash |

### Real-world example

Bank transfer:

```
BEGIN
  UPDATE accounts SET balance = balance - 100 WHERE id = 1;
  UPDATE accounts SET balance = balance + 100 WHERE id = 2;
COMMIT
```

→ Either both updates apply (A), no negative balance violated (C), no partial dirty read (I), persists post-crash (D).

### Use ACID when

- Money / inventory / legal records
- Strong consistency required
- Schema fixed

→ PostgreSQL, MySQL, Oracle, SQL Server.

---

## BASE

**Common in NoSQL / distributed systems.**

| Letter | Meaning |
|--------|---------|
| **B** Basically Available | Always responds, possibly stale |
| **S** Soft state | State may change without input (replication) |
| **E** Eventual consistency | Eventually all replicas converge |

### Use BASE when

- Massive scale
- Read-heavy
- Stale reads acceptable (Twitter feed, product catalog, analytics)

→ Cassandra, DynamoDB, Couchbase.

### ACID vs BASE Trade-off

| | ACID | BASE |
|--|------|------|
| Consistency | Strong | Eventual |
| Scale | Vertical mostly | Horizontal |
| Availability | Lower (CP) | High (AP) |
| Use | Money, inventory | Social, analytics |

---

## Eventual Consistency

> "All replicas will **eventually** converge to the same value, given **no new updates**."

### Example timeline

```
Time 0: Write "X = 5" at replica A
Time 1: Read at replica A → 5
Time 2: Read at replica B → null (not yet replicated)
Time 3: Read at replica B → 5 (replication caught up)
```

### Strengthening guarantees

| Model | Guarantee |
|-------|-----------|
| **Read-your-writes** | User sees their own updates |
| **Monotonic reads** | Successive reads don't go backward |
| **Monotonic writes** | Writes from same client preserved order |
| **Causal consistency** | "Cause" before "effect" preserved |

### Implementation tricks

- Sticky session to one replica until write propagates
- Version vectors / vector clocks
- Last-write-wins with timestamps (Cassandra)
- Quorum reads/writes (R + W > N for strong consistency on Dynamo-style)

---

## Common Output Traps

### Q1. "Pick 2 of 3 in CAP"

→ Wrong. P is mandatory; choice is C or A during partition.

### Q2. "ACID is impossible at scale"

→ Wrong. Modern distributed SQL (CockroachDB, Spanner) provide ACID at scale via Paxos/Raft + 2PC + clock sync.

### Q3. "Eventual consistency means broken"

→ Wrong. For 90% web use cases, eventual consistency is acceptable + much faster.

### Q4. Rough QPS without specifying read vs write

→ Always split. Read-heavy ratio (10:1 or 100:1) common for social/content.

### Q5. Memory math without compression

→ Real systems compress + dedupe; mention as optimization.

---

## Pitfalls

1. **No clarifying questions** → designing wrong system.
2. **Skipping estimation** → can't justify scale choices.
3. **CAP misuse** as "pick 2" — explain partition context.
4. **Conflating consistency** levels — strong vs eventual vs sequential.
5. **No NFR explicit** — latency / availability / throughput targets unstated.
6. **Forgetting durability** — recovery + backups missing.
7. **Single point of failure** in design — interviewer probes.
8. **Capacity in steady state only** — no peak / Black Friday consideration.
9. **No compression / dedup** — overestimate storage.
10. **Ignoring cost** — over-engineering for 100x scale unnecessary.

---

## Cheat Sheet

| Step | Time | Output |
|------|------|--------|
| Clarify | 5 min | Functional + NFR + scope |
| Estimate | 5 min | QPS, storage, BW |
| HLD | 10 min | Boxes + arrows + DB |
| Deep dive | 15 min | 1-2 components in depth |
| Trade-offs | 5-10 min | Failures, scale-up |

| | ACID | BASE |
|--|------|------|
| | Strong | Eventual |
| | Vertical | Horizontal |
| | RDBMS | NoSQL |

| Estimation | Memorize |
|------------|----------|
| 1 day | 10⁵ sec |
| 2¹⁰ ≈ 10³ | KB ≈ 1024 |
| Read:Write | 10:1 (social), 100:1 (content) |

| CAP | When P |
|-----|--------|
| CP | Banking |
| AP | Feeds, search |

---

## Practice

1. Pick a use case (Twitter / WhatsApp / Uber); list 5 functional + 5 non-functional reqs in 3 min.
2. Estimate QPS / storage / BW for given DAU.
3. Pick CP or AP for: payment processor, news feed, search index.
4. Explain ACID vs BASE in 30 sec each (interview pace).
5. Describe 3 levels of eventual consistency (read-your-writes, monotonic, causal) with examples.

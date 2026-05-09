# PACELC Theorem

## Status: Complete

---

## Quick Walkthrough (Hinglish)

> "**PACELC = CAP ka realistic upgrade.** CAP sirf partition ke time ki baat karta hai. Lekin partitions toh rare hain — **99.9% time network theek** hota hai. Tab bhi ek trade-off hota hai: **Latency vs Consistency**. Wahi PACELC capture karta hai."

**Decode the name**:

```
P A C  E L C
│ │ │  │ │ └─ else Consistency
│ │ │  │ └─── (if no partition) Latency
│ │ │  └───── ELSE
│ │ └──────── (if partition) Consistency
│ └────────── (if partition) Availability
└──────────── PARTITION
```

Translation:

- **If P**artition → choose **A**vailability or **C**onsistency
- **E**lse (no partition) → choose **L**atency or **C**onsistency

**Real-world bucket**:

| System | PACELC | Meaning |
|--------|--------|---------|
| **DynamoDB**, Cassandra, Riak | **PA / EL** | Partition mein A chunte, normal mein latency chunte (eventual reads) |
| **HBase**, Zookeeper, etcd, Spanner | **PC / EC** | Partition mein C, normal mein bhi C (latency higher) |
| **MongoDB** (default) | **PA / EC** | Partition mein A, normal mein C (majority reads se) |
| **PNUTS** (Yahoo!) | **PC / EL** | Partition mein C, normal mein latency |

**Why PACELC matters more**:

- Apke users ko **latency dikhti hai every day**. Partition shayad **kabhi kabhi**.
- Toh "EC ya EL" decision **product behaviour** decide karta hai — feed lazy load karega ya har refresh par DB hit karega?

> "**Soundbite**: 'CAP textbook tha, PACELC production-grade hai. Real architects EL ya EC pe pehle baat karte hain — partition handling baad mein.'"

---

## Table of Contents

1. [Why PACELC](#why-pacelc)
2. [The Statement](#the-statement)
3. [Decomposing the Letters](#decomposing-the-letters)
4. [PA/EL Systems](#pael-systems)
5. [PC/EC Systems](#pcec-systems)
6. [PA/EC and PC/EL (Rare)](#paec-and-pcel-rare)
7. [PACELC vs CAP](#pacelc-vs-cap)
8. [Examples Mapped](#examples-mapped)
9. [Choosing for Your System](#choosing-for-your-system)
10. [Pitfalls](#pitfalls)
11. [Cheat Sheet](#cheat-sheet)

---

## Why PACELC

> "**CAP only describes behavior during a partition** — but partitions are rare. **Most of the time** the trade-off is **latency vs consistency**, not availability vs consistency."

Daniel Abadi (2010) proposed PACELC to capture this:

> "If there's a **P**artition (P), how does the system trade off **A**vailability vs **C**onsistency?  
> **E**lse (E, normal operation), how does it trade off **L**atency vs **C**onsistency?"

So real systems advertise **two** trade-offs, not one.

---

## The Statement

```
If Partition (P)  → choose Availability (A) or Consistency (C)
Else (E)          → choose Latency (L) or Consistency (C)
```

Combinations:

| Code | Meaning |
|------|---------|
| **PA/EL** | On partition prefer Availability; normally prefer Latency |
| **PA/EC** | On partition prefer Availability; normally prefer Consistency |
| **PC/EL** | On partition prefer Consistency; normally prefer Latency |
| **PC/EC** | On partition prefer Consistency; normally prefer Consistency |

The **second letter** (after the slash) is the new insight CAP missed.

---

## Decomposing the Letters

### Why latency matters in normal operation

> "**Strong consistency requires coordination** — quorum reads, synchronous replication, distributed transactions. All cost **latency**."

Concrete example:

- Strongly consistent read in a 3-node quorum cluster = wait for ≥2 nodes to respond
- Eventually consistent read = wait for **closest** replica only

| Operation | Strong consistency | Eventual consistency |
|-----------|---------------------|----------------------|
| Read | Quorum (slow) | Single node (fast) |
| Write | Majority replication (slow) | Single node + async (fast) |
| Cross-region | Cross-region RTT each op | Local RTT |

**You're paying latency 100% of the time** for a guarantee you only need during writes / sensitive reads.

### Why consistency matters in normal operation

> "**Even without partition**, async replication means a read on a stale replica returns old data. PACELC asks whether the system **prefers strong consistency at the cost of latency**, or prefers fast responses at the cost of staleness."

---

## PA/EL Systems

> "**Available + low latency.** Tolerates stale data both during partition and normal operation."

Characteristics:

- **Async replication** by default
- Local-region reads/writes
- **Eventually consistent** queries
- Conflict resolution after the fact (LWW, CRDTs)

Examples:

| System | Notes |
|--------|-------|
| **DynamoDB** (default eventual reads) | PA/EL — fast, geographically distributed |
| **Cassandra** (default `ONE`) | PA/EL — tunable to PC/EC if you set CONSISTENCY ALL |
| **Riak** | Vector clocks, async replication |
| **CouchDB** | Multi-master |
| **Voldemort** | LinkedIn's old KV store |

### Use cases

- Shopping carts, social timelines, IoT telemetry
- High write throughput across regions
- "User experience > absolute correctness"

---

## PC/EC Systems

> "**Consistent always — pays latency cost.**"

Characteristics:

- **Synchronous replication** / quorum
- Strong consistency on reads
- Higher latency for writes (cross-AZ / cross-region sync)

Examples:

| System | Notes |
|--------|-------|
| **HBase** | Single region server per region — strict consistency |
| **MongoDB** with `w: majority` and `readConcern: linearizable` | PC/EC mode |
| **BigTable** | Like HBase model |
| **Zookeeper** / **etcd** | Coordination services — must be consistent |
| **VoltDB** | In-memory consistent OLTP |

### Use cases

- Coordination, locks, leader election
- Financial ledgers (with read-your-own-writes guarantee)
- Inventory management (last-unit scenarios)

---

## PA/EC and PC/EL (Rare)

### PA/EC

> "**Available during partition, but consistent in normal operation.**"

- During partition: serve stale, prefer A
- Normal: synchronous replication, strong reads

Rare combo — implies during partition you intentionally relax. Some configurations of **MongoDB** (e.g., `readPreference: primary` for reads but `secondary` allowed during failure) approximate this.

### PC/EL

> "**Consistent during partition, but latency-optimized in normal operation.**"

- During partition: refuse rather than be stale
- Normal: read from local replica (fast, possibly slightly stale within its commit guarantees)

Some systems with **causal consistency** + tolerable bounded staleness fit here. **Spanner** is sometimes argued PC/EC because it pays sync cost always; others argue it's PC/EL based on read latency optimizations via TrueTime.

These two are less common — most systems pick PA/EL or PC/EC.

---

## PACELC vs CAP

| | **CAP** | **PACELC** |
|--|---------|------------|
| Scope | Only partition behavior | Partition + normal behavior |
| Trade-off | C vs A | C vs A (partition) + C vs L (normal) |
| Real-world relevance | Partial | Better — covers 99% normal time |
| Number of categories | 2 (CP, AP) | 4 (PA/EL, PA/EC, PC/EL, PC/EC) |

> "**Use CAP for the headline**, **use PACELC to describe steady-state behavior**. Senior engineers reach for PACELC."

---

## Examples Mapped

| System | CAP | PACELC | Notes |
|--------|-----|--------|-------|
| **DynamoDB** (default) | AP | **PA/EL** | Eventual reads default; can opt-in strong reads |
| **Cassandra** (default ONE) | AP | **PA/EL** | Tunable per query |
| **Cassandra** (CONSISTENCY ALL) | CP-ish | PC/EC | High latency price |
| **MongoDB** (default `w: 1`) | AP-ish | PA/EL | Older defaults |
| **MongoDB** (`w: majority`, `readConcern: majority`) | CP | **PC/EC** | Modern best practice |
| **HBase** | CP | **PC/EC** | Region server is single point of consistency |
| **Zookeeper / etcd** | CP | **PC/EC** | Coordination needs both partition and normal consistency |
| **Spanner** | CP | **PC/EC** (debated) | Sync replication via TrueTime |
| **Aurora MySQL** primary + reader | CP | PC/EC writes; reader is **EL** for reads (replication lag) | Mixed depending on read endpoint |
| **Redis** (single primary + async replicas) | AP-ish | **PA/EL** for replica reads; primary itself = strong | Tunable via WAIT |
| **DNS** | AP | **PA/EL** | Cached responses fast and may be stale |

---

## Choosing for Your System

### Pick PA/EL when

- Read-heavy, latency-sensitive, geo-distributed
- Stale-by-seconds is OK
- Examples: timelines, cache layers, recommendations, IoT telemetry

### Pick PC/EC when

- Money / locks / leader election / inventory
- "Better to fail fast than to be wrong"
- Examples: payments, tickets, distributed locks, schema metadata

### Mix per workload

> "Real systems = PA/EL for cart, PC/EC for checkout, PA/EL for recommendations, PC/EC for inventory at scale-down."

Talk about **per-feature consistency** in interviews.

---

## Pitfalls

1. **Treating PACELC as a label** rather than a per-operation choice — most systems are tunable.
2. **Quoting PACELC** without explaining why it matters (latency cost of consistency).
3. **Optimizing for consistency everywhere** — paying latency tax for no reason.
4. **Optimizing for latency everywhere** — losing money / overselling inventory.
5. **Forgetting partition is rare** — too much focus on CAP, too little on EL trade-off.
6. **Confusing read latency with write latency** — they have different consistency stories in many DBs.
7. **Assuming Spanner / Aurora "have no trade-off"** — they pay latency / cost.
8. **Picking PC/EC for analytics** — wasteful.

---

## Cheat Sheet

| Letter | Meaning |
|--------|---------|
| **P** | Partition (network split happens) |
| **A** | Availability (serve requests) |
| **C** | Consistency (linearizable / strong) |
| **E** | Else (no partition; normal operation) |
| **L** | Latency (low response time) |

| Combo | One-liner |
|-------|-----------|
| **PA/EL** | Always fast, accept staleness — DynamoDB, Cassandra, Redis replicas, DNS |
| **PC/EC** | Always consistent, accept latency — HBase, Zookeeper, etcd, MongoDB with majority, Spanner |
| **PA/EC** | Available on partition, consistent normally (rare) |
| **PC/EL** | Consistent on partition, fast normally (rare) |

| When | Pick |
|------|------|
| Read-heavy, geo, OK with stale | **PA/EL** |
| Money / locks / inventory | **PC/EC** |
| Mixed app (most) | **per-operation** choice — both |

---

## Practice

1. Classify Aurora MySQL writer vs reader endpoints in PACELC terms.
2. For an Uber-like ride matching system, which PACELC class for: driver location updates? ride state machine? earnings ledger?
3. Explain why DynamoDB's **strongly consistent read** option moves it toward PC for that single operation.
4. Design a chat app and pick PACELC per: message send, presence indicator, message read receipts.
5. Why is Spanner sometimes labeled PC/EC even though it is "always available"? (Hint: TrueTime + sync replication latency.)

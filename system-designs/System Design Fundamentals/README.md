# System Design Fundamentals

System design ki **foundational mastery** — interview approach, capacity estimation, CAP & PACELC theorems. Hinglish mein step-by-step explanations + interview-ready frameworks.

---

## Topics & Status

| # | Topic | File | Status |
|---|-------|------|--------|
| 1 | SD Interview Approach (RESHADED) | [01-SD-Interview-Approach.md](./01-SD-Interview-Approach.md) | Complete |
| 2 | Capacity Estimation | [02-Capacity-Estimation.md](./02-Capacity-Estimation.md) | Complete |
| 3 | CAP Theorem | [03-CAP-Theorem.md](./03-CAP-Theorem.md) | Complete |
| 4 | PACELC Theorem | [04-PACELC-Theorem.md](./04-PACELC-Theorem.md) | Complete |

---

## What's Inside Each File?

### [01 — SD Interview Approach](./01-SD-Interview-Approach.md)
**RESHADED framework**: **R**equirements → **E**stimation → **S**torage → **H**igh-level → **A**PI design → **D**etailed design → **E**valuation.  
Functional vs non-functional clarification, asking scale (QPS, storage, latency SLA, availability 99.9% vs 99.99%), time management (45-min interview), drawing diagrams, communicating tradeoffs.

### [02 — Capacity Estimation](./02-Capacity-Estimation.md)
Back-of-envelope math: **QPS = DAU × req/user / 86400**, storage (`bytes × records`), bandwidth (`QPS × payload`), memory working set (hot 20%). Reference numbers: **1M req/day ≈ 12 QPS**, byte/KB/MB/GB conversions, "**Latency Numbers Every Programmer Should Know**".

### [03 — CAP Theorem](./03-CAP-Theorem.md)
**C**onsistency, **A**vailability, **P**artition tolerance — pick any 2. Why **P is mandatory** in distributed systems. **CP** systems (MongoDB strict, HBase, Zookeeper) vs **AP** systems (Cassandra, DynamoDB default, CouchDB). Interview wording — "CAP doesn't mean you lose 1 forever, only during a partition."

### [04 — PACELC Theorem](./04-PACELC-Theorem.md)
**Real-world refinement of CAP**: if **P**artition → choose **A** vs **C**; **E**lse (no partition) → choose **L**atency vs **C**onsistency. Examples: DynamoDB **PA/EL**, HBase **PC/EC**, MongoDB **PC/EC** (default), Cassandra **PA/EL**.

---

## Recommended Order

```
1. Interview Approach   ← framework to handle any SD question
2. Capacity Estimation  ← numbers + intuition
3. CAP Theorem          ← classic distributed systems trade-off
4. PACELC Theorem       ← realistic extension of CAP
```

---

## Companion Folders

- [System Design Interviews](../../phase-5-interview-preparation/System%20Design%20Interviews/) — applied designs (URL shortener, etc.)
- [MySQL — Relational Database Mastery](../../mysql_aws_mongodb/MySQL%20%E2%80%94%20Relational%20Database%20Mastery/) — strong consistency layer
- [MongoDB — Document Database Mastery](../../mysql_aws_mongodb/MongoDB%20%E2%80%94%20Document%20Database%20Mastery/) — tunable consistency layer
- [AWS — Cloud Platform for Backend Engineers](../../mysql_aws_mongodb/AWS%20%E2%80%94%20Cloud%20Platform%20for%20Backend%20Engineers/) — building blocks for designs

---

## Quick Reference

| Need | Read |
|------|------|
| Walk into SD interview | 01 — RESHADED |
| Numbers under 30 seconds | 02 — Estimation |
| Pick consistency vs availability | 03 — CAP |
| Real-world latency vs consistency | 04 — PACELC |

---

## Status Tracker

```
[x] 01 — SD Interview Approach
[x] 02 — Capacity Estimation
[x] 03 — CAP Theorem
[x] 04 — PACELC Theorem
```

> "**System design = trade-offs**. Right answer almost never exists — communicating trade-offs **clearly** does."

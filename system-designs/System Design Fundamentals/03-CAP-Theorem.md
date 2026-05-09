# CAP Theorem

## Status: Complete

---

## Table of Contents

1. [What is CAP](#what-is-cap)
2. [The Three Properties](#the-three-properties)
3. [Why P is Mandatory](#why-p-is-mandatory)
4. [CP vs AP — The Real Choice](#cp-vs-ap--the-real-choice)
5. [CP Systems](#cp-systems)
6. [AP Systems](#ap-systems)
7. [Common Misconceptions](#common-misconceptions)
8. [Choosing for Your System](#choosing-for-your-system)
9. [Limitations of CAP](#limitations-of-cap)
10. [Pitfalls](#pitfalls)
11. [Cheat Sheet](#cheat-sheet)

---

## What is CAP

> "**CAP theorem** (Eric Brewer, 2000; Gilbert & Lynch proof, 2002): a **distributed system** can guarantee **at most 2 out of 3** of: **Consistency**, **Availability**, **Partition tolerance**."

Critical phrasing:

- It's about **distributed** systems (multiple nodes over network)
- The choice is **forced** only **during a network partition**
- During normal operation you can have all three; partition forces a pick

---

## The Three Properties

### C — Consistency (Linearizability)

> "**Every read returns the most recent write or an error.** All nodes see the same data at the same time."

This is **strong consistency** (linearizability), not the "C" of ACID (which is integrity constraints).

Example:

```
T1: write(x = 5)   → ack from coordinator
T2: read(x)        → MUST return 5 (or error), never older value
```

### A — Availability

> "**Every request gets a (non-error) response, even if not the latest data.**"

No request hangs forever, no `503 Service Unavailable`.

Example:

```
Network split → minority partition still answers requests (with possibly stale data)
```

### P — Partition Tolerance

> "**System keeps working despite network failures** between nodes — messages dropped, delayed, reordered."

Partition = nodes can't communicate temporarily. Real networks fail constantly: cable cuts, switch failures, AZ outages.

---

## Why P is Mandatory

> "**You don't choose P** — networks fail. So in distributed systems the real choice is **C vs A** during a partition."

If you "choose CA" (no P), you're saying:

- All nodes must communicate, always
- Single-node only (no real distribution)
- Or you accept downtime on any partition (which means giving up A)

→ **All real distributed systems are P**. The argument is **CP vs AP**.

```
                    Distributed System
                          │
                Network Partition occurs
                          │
              ┌───────────┴───────────┐
            Pick C                  Pick A
       Reject requests           Serve requests
       on minority side          even if data may be stale
       (CP)                      (AP)
```

---

## CP vs AP — The Real Choice

| | **CP (Consistency)** | **AP (Availability)** |
|--|----------------------|------------------------|
| During partition | Refuse some requests to keep data consistent | Accept all requests, may serve stale data |
| Behavior | Some users get errors / timeouts | All users get responses; some may be wrong/stale |
| Use case | Banking, inventory, payments, locks | Shopping carts, social feeds, DNS, analytics |
| Recovery | Strict reconciliation (no conflicts to resolve) | Conflict resolution at merge (LWW, vector clocks, CRDTs) |

### Mental model

> "**CP**: '**Sorry, try again** — we won't lie to you about your account balance.'  
> **AP**: '**Here's your feed**, may be 30 seconds old, we'll catch up later.'"

---

## CP Systems

> "**Sacrifice availability** to keep all nodes in sync."

Examples:

| System | Why CP |
|--------|--------|
| **MongoDB** (default majority writes) | Writes need majority quorum; if minority side, writes fail |
| **HBase** | Strong consistency via single region server per region |
| **Zookeeper** | ZAB protocol — minority partition refuses writes |
| **etcd / Consul** | Raft consensus — needs quorum |
| **Google Spanner** | Synchronously replicated via Paxos + TrueTime |
| **Traditional RDBMS in distributed setup** (Aurora primary, single-leader) | Single writer, others readonly or failover |

### Behavior on partition

```
Cluster of 5 nodes, partition splits into 3 (majority) + 2 (minority)
→ 3-node side keeps accepting reads/writes (has quorum)
→ 2-node side rejects writes (no quorum)
→ Some user requests fail
```

---

## AP Systems

> "**Sacrifice strong consistency** to keep responding."

Examples:

| System | Why AP |
|--------|--------|
| **Cassandra** | Tunable but typically AP — accepts writes anywhere, gossip resolves |
| **DynamoDB** (default eventual reads) | Returns whatever node has, eventually consistent |
| **CouchDB** | Multi-master, replicates async, conflict resolution |
| **Riak** | Vector clocks, accepts writes, resolves later |
| **DNS** | Cached records served even if authoritative unreachable |

### Behavior on partition

```
Both sides keep accepting reads + writes
→ Conflicts may emerge (same key written on both sides)
→ Resolved via:
   - Last-Write-Wins (LWW)
   - Vector clocks
   - CRDTs (mathematically merge)
   - Application-level merge logic
```

---

## Common Misconceptions

### 1. "CAP means you permanently lose 1 of 3"

❌ Wrong. **Only during a partition**. In healthy state you can have C + A + P.

### 2. "CA systems exist"

❌ Wrong in distributed context. Single-node DB (MySQL on one host) is "CA" because no P, but that's not a distributed system.

### 3. "MongoDB is AP"

❌ Old version (pre-3.x) MongoDB was sometimes labeled AP. Modern MongoDB with majority write concern is **CP**. Always read current docs.

### 4. "Cassandra is AP — no consistency"

❌ Wrong. Cassandra is **tunable** — you choose per query (CONSISTENCY ALL = strong; ONE = eventual). Default labels can mislead.

### 5. "Strong consistency = ACID"

❌ Different. ACID's "C" = integrity constraints (e.g., FK valid). CAP's "C" = linearizability across replicas.

---

## Choosing for Your System

### Pick CP when

- **Money / inventory / regulatory** — wrong data > no data
- **Coordinator / lock service** — Zookeeper, etcd
- **Strict ordering** required (event log, causal stream)
- Examples: banking, ticket sales, leader election

### Pick AP when

- **User-perceived availability matters more** than perfect freshness
- **Stale data is acceptable** for short windows
- **High write throughput** distributed across regions
- Examples: shopping cart (merge later), social feed, DNS, analytics, caches

### Hybrid in real systems

> "**Real apps use both.** Banking core = CP. Recommendations = AP. Cart = AP with TTL conflict resolution. Order placement = CP."

---

## Limitations of CAP

1. **Binary classification** — real DBs are tunable per query (Cassandra, Mongo)
2. **Doesn't address latency** — system might be available but slow → user calls it "down". Addressed by **PACELC**.
3. **Partition is temporary** — most of the time there's no partition; CAP doesn't describe normal-mode trade-offs.
4. **Doesn't model degree** — "eventually consistent" can mean 100 ms or 1 hour.
5. **Quorum-based systems** can be both CP-ish and AP-ish depending on quorum config.

→ This is why **PACELC** ([next file](./04-PACELC-Theorem.md)) is more useful in practice.

---

## Pitfalls

1. **Quoting CAP without context** — "X is CP" is incomplete; specify default config + quorum settings.
2. **Confusing ACID-C with CAP-C** in interviews.
3. **Designing strong consistency everywhere** — kills latency and throughput.
4. **Designing eventual consistency everywhere** — breaks money / inventory invariants.
5. **Ignoring partitions in design** — happy-path only thinking.
6. **Single primary "CA" claim** — that's just lack of distribution.
7. **Conflict resolution afterthought** in AP systems — LWW data loss.
8. **No timeout strategy** — hung requests on CP systems = effective unavailability.

---

## Cheat Sheet

| Letter | Meaning |
|--------|---------|
| **C** | Linearizability — read returns latest write |
| **A** | Every request gets non-error response |
| **P** | System works during network partitions |

| Choice | Behavior on partition |
|--------|----------------------|
| **CP** | Reject some requests, keep data consistent |
| **AP** | Serve all, may be stale, resolve later |

| Examples |  |
|----------|--|
| **CP** | MongoDB (majority), HBase, Zookeeper, etcd, Spanner |
| **AP** | Cassandra (default), DynamoDB (default), CouchDB, Riak, DNS |

| Use case → choice |  |
|---|--|
| Money / ledger | **CP** |
| Lock / leader election | **CP** |
| Shopping cart | **AP** |
| Social feed | **AP** |
| DNS | **AP** |
| Inventory at sale | **CP** for last unit, **AP** otherwise |

---

## Practice

1. For an e-commerce checkout, classify each step (cart, inventory check, payment, order placement) as needing CP or AP.
2. Explain to interviewer: why is "CA" not really an option in a distributed system?
3. Cassandra with `CONSISTENCY ALL` — is it CP or AP? Justify.
4. Design: chat message delivery — which side of CAP do you pick? Why?
5. Compare MongoDB default write concern vs `w: majority` — which is CP-ish?

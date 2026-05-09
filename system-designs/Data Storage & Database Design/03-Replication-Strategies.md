# Replication Strategies

## Status: Complete

---

## Table of Contents

1. [Why Replicate](#why-replicate)
2. [Single-Leader (Master-Slave)](#single-leader-master-slave)
3. [Multi-Leader (Master-Master)](#multi-leader-master-master)
4. [Leaderless Replication](#leaderless-replication)
5. [Synchronous vs Asynchronous](#synchronous-vs-asynchronous)
6. [Semi-Synchronous](#semi-synchronous)
7. [Replication Lag](#replication-lag)
8. [Read-Your-Writes Consistency](#read-your-writes-consistency)
9. [Quorum (W + R > N)](#quorum-w--r--n)
10. [Conflict Resolution](#conflict-resolution)
11. [Replication vs Sharding](#replication-vs-sharding)
12. [Pitfalls](#pitfalls)
13. [Cheat Sheet](#cheat-sheet)

---

## Why Replicate

> "**Same data on multiple nodes** for: **HA** (failover on node death), **read scale** (multiple readable copies), **DR** (cross-region copy), **geo-locality** (read from nearest replica)."

Three families of replication:

```
1. Single-Leader   (Master-Slave)
2. Multi-Leader    (Master-Master)
3. Leaderless      (Dynamo-style)
```

Each makes different trade-offs on **consistency**, **conflict handling**, and **operational complexity**.

---

## Single-Leader (Master-Slave)

> "**One leader (master) accepts all writes**; **followers (slaves / replicas) replicate from leader** asynchronously or synchronously. Most common model."

```
Client writes → Leader → Follower 1 (replication)
                       → Follower 2 (replication)

Client reads → Leader (strong) or any Follower (potentially stale)
```

### Pros

- **Simple consistency model** — one source of truth
- **No write conflicts** — only one writer
- **Easy reasoning** about ordering
- **Read scaling** via followers

### Cons

- **Leader = SPOF** — failover must be automated
- **Failover lag** — election + DNS / proxy reroute (~10s–60s)
- **Replica lag** for follower reads
- **Write throughput capped** by leader's capacity

### Use when

- Most relational DBs (MySQL, Postgres) by default
- MongoDB replica sets
- Aurora reader/writer model

### Failover

```
Leader dies → election → promote a follower to new leader →
update routing (DNS / proxy / app discovery) → followers re-attach
```

> Tools: **Patroni** (Postgres), **Orchestrator** (MySQL), **MongoDB built-in**, **Aurora managed**, **Sentinel** (Redis).

---

## Multi-Leader (Master-Master)

> "**Multiple leaders accept writes**; each replicates to others. Used for **multi-region active-active** or specific HA needs."

```
Region A Leader  ←→  Region B Leader  ←→  Region C Leader
   │                    │                    │
 followers            followers            followers
```

### Pros

- **Writes near user** (low latency, geo-distributed)
- **No single failover bottleneck**
- **Tolerate region outages** (other leaders keep going)

### Cons

- **Conflict resolution** required — same row updated in two regions simultaneously
- **Operational complexity** much higher
- **Eventually consistent** by nature

### Conflict resolution strategies

(See [Conflict Resolution](#conflict-resolution) below.)

### Use when

- **Multi-region active-active** writes (rare — most do active-passive instead)
- **Edge / offline-first apps** that sync later (CouchDB, Realm, Firebase)
- Examples: CouchDB, BDR for Postgres, MySQL Group Replication (special config), Cassandra (sort of)

> Most teams choose **single-leader per region + cross-region async replica** instead, to avoid conflict pain.

---

## Leaderless Replication

> "**No leader at all.** Client writes to **multiple replicas in parallel** (W of N), reads from multiple (R of N). Quorum guarantees consistency."

### Examples

- **Cassandra**, **DynamoDB internals**, **Riak**, **Voldemort** — all Dynamo-style

### How it works

```
Client write → coordinator → W replicas ack
Client read  → coordinator → R replicas → return latest by timestamp
```

- **W + R > N** → guaranteed read sees latest write
- **Read repair** — on read, if replicas disagree, oldest is updated
- **Hinted handoff** — if a replica is down, coordinator stores write hint and replays later
- **Anti-entropy / Merkle trees** — background sync to repair drift

### Pros

- **No leader election** — no failover lag
- **Highly available** — accepts writes even with some replicas down
- **Linear horizontal scale**

### Cons

- **Conflicts must be resolved** (LWW / vector clocks / CRDTs)
- **Operational subtlety** (tombstones, repairs)
- **Consistency tunable per query** (mental load)

---

## Synchronous vs Asynchronous

| | **Synchronous** | **Asynchronous** |
|--|------------------|-------------------|
| Acks | Wait for follower(s) to confirm | Return after leader writes locally |
| Latency | Higher (network RTT) | Low |
| Data loss on leader crash | None (if quorum was sync) | Up to last unreplicated write |
| Throughput | Lower | Higher |
| Default in | Aurora (sync to majority of storage), Postgres `synchronous_commit` (configurable), MongoDB `w: majority` | Most MySQL/Postgres setups, Redis primary→replica |

### Sync example (financial systems)

```
write → leader → wait for replica ack → ack to client
                  └─ no follower ack? → fail or block
```

### Async example (social feeds)

```
write → leader → ack to client immediately
              ↘  background → eventually replicate to followers
```

---

## Semi-Synchronous

> "**Compromise**: leader waits for **at least one** follower ack, not all. Bounds data loss without paying full sync cost."

- MySQL: `rpl_semi_sync_master`
- Often used in **Multi-AZ RDS Multi-AZ** setups

Behavior:

- Normal: feels async-fast
- Failure: at most **one node failure** = no data loss (because the write was on at least 2 nodes)

---

## Replication Lag

> "**Async replication** means followers are **behind leader** by some amount of time. Lag may be ms, sec, or min."

### Causes of lag spikes

- Network slow / saturated
- Follower CPU / disk slow
- Long-running write transactions on leader
- Schema changes (large `ALTER TABLE` blocks replication)
- Replica running heavy queries (saturating itself)

### Monitor

- MySQL: `SHOW SLAVE STATUS\G` → `Seconds_Behind_Master`
- Postgres: `pg_stat_replication.replay_lag`
- MongoDB: `rs.printReplicationInfo()` + `rs.printSlaveReplicationInfo()`

### What to do under lag

- Route critical reads to **leader**
- Pause read traffic to laggy follower (auto via load balancer health checks)
- Add capacity (faster disk, more CPU, smaller writes)

---

## Read-Your-Writes Consistency

> "**A user must see their own write immediately**, even if other replicas are slower."

### Why important

User updates profile → page reload → if read from stale follower → user sees old data → "bug!" report.

### Solutions

| Strategy | How |
|----------|-----|
| **Read from leader for own writes** | Track recent writes (in-memory or session); for those records, route to leader |
| **Sticky session to leader** for N seconds after write | Simple |
| **Track write timestamp** (LSN / log position) → wait until follower catches up | Strong; complex |
| **Causal consistency session** | DB feature (MongoDB) — driver tracks ordering |

### Example: MongoDB causal consistency

```javascript
const session = client.startSession({ causalConsistency: true });
// writes + reads in this session preserve causal order across replicas
```

---

## Quorum (W + R > N)

> "**Read latest write guaranteed** when number of nodes you write to (W) plus number you read from (R) exceeds total replicas (N)."

```
N = 3
W = 2  (write to 2 of 3)
R = 2  (read from 2 of 3)
W + R = 4 > N = 3   → at least one common node has latest write
```

### Tunable per query (Cassandra / DynamoDB)

| Setting | Latency | Consistency |
|---------|---------|-------------|
| W=1, R=1 | Lowest | Eventual |
| W=N, R=1 | Slow writes | Strong reads (if writes succeed) |
| W=1, R=N | Slow reads | Strong reads |
| W=quorum, R=quorum | Balanced | Strong |
| W=ALL, R=ALL | Slowest | Linearizable, but no failures tolerated |

### Common defaults

- **Quorum = ⌊N/2⌋ + 1** → tolerates ⌊N/2⌋ node failures
- N=3 → quorum=2, tolerates 1 failure
- N=5 → quorum=3, tolerates 2 failures

---

## Conflict Resolution

> "**When two nodes accept writes for same key**, you must resolve."

### Strategies

| Strategy | Description | Trade-off |
|----------|-------------|-----------|
| **Last-Write-Wins (LWW)** | Compare timestamps; latest wins | Simple; **silent data loss** if clocks skew |
| **Application-defined merge** | App provided merge function (e.g., `set union`, max) | Flexible, app complexity |
| **Vector clocks** | Track causality; coexist concurrent versions; surface conflicts to app | Correct, complex |
| **CRDTs** (Conflict-free Replicated Data Types) | Math-guaranteed mergeable types: counters, sets, maps | Powerful for collaborative apps (Figma, Yjs, Riak) |
| **Multi-value / siblings** | Store all conflicting versions; client picks (Riak default) | Pushes problem to app |

### When LWW is fine

- Last-update truly is the right answer (e.g., user profile name)
- Clock skew tightly bounded (NTP / Spanner TrueTime)

### When LWW is dangerous

- Counters / monotonic data — incrementing twice = lose one increment
- Money — silent data loss = audit nightmare

> Use **CRDTs / app-merge** for these.

---

## Replication vs Sharding

> "**Different problems, same diagram.** Replication = same data many copies (HA / read). Sharding = different data per node (write / storage scale)."

| Use both at scale | Cluster of N shards × M replicas each |
|--|--|
| Example | 8 shards × 3 replicas = 24 nodes |

| Need | Use |
|------|-----|
| HA + read scale | **Replication** |
| Write scale + storage scale | **Sharding** |
| Both | **Sharded cluster of replica sets** (MongoDB / Cassandra / Aurora Global) |

---

## Pitfalls

1. **Reading from replica + expecting fresh data** → ghost bugs.
2. **No automated failover** → manual DBA at 3 AM.
3. **Multi-master without conflict plan** → data corruption.
4. **Sync replication across regions** → latency tax (50–150 ms RTT per write).
5. **All writes to one leader** at scale → primary CPU pegged.
6. **No `read_replica` health check** in load balancer → routing to laggy node.
7. **Cross-AZ async replication** without monitoring → silent lag.
8. **Backups only on leader** → leader fails + recent writes lost.
9. **Quorum misconfigured** (W+R ≤ N) → silently inconsistent reads.
10. **LWW for counters** → silent decrement.

---

## Cheat Sheet

| Topology | Use |
|----------|-----|
| **Single-leader** | Default for SQL DBs |
| **Multi-leader** | Multi-region active-active, offline-first |
| **Leaderless** | Cassandra / DynamoDB style |

| Sync | When |
|------|------|
| **Sync** | Money, no-loss requirement |
| **Async** | Most apps, perf > tiny loss window |
| **Semi-sync** | Bounded loss (1 follower ack) |

| Quorum | Effect |
|--------|--------|
| W+R > N | Strong reads guaranteed |
| W=1, R=1 | Fast, eventual |

| Conflict | Tool |
|----------|------|
| Pick latest | LWW |
| Sets / counters | CRDT |
| Custom merge | App callback |
| Track causality | Vector clocks / siblings |

---

## Practice

1. Aurora has 6 storage replicas; classify writes — sync to majority or async?
2. Write reads to leader for last 5 sec after a user writes; pseudocode.
3. Configure **W=3, R=3, N=5** Cassandra cluster — what failures tolerated, what consistency guaranteed?
4. Multi-region notes app — pick replication strategy + conflict resolution.
5. Detect + alert on Postgres `replay_lag > 1 second` for 1 minute.
6. Why does LWW corrupt a "page view counter"? Propose fix.

# Data Storage & Database Design

Data storage layer ki **deep mastery** for system design — SQL vs NoSQL choice, sharding, replication, caching strategies. Hinglish + diagrams + interview-grade trade-off tables.

---

## Topics & Status

| # | Topic | File | Status |
|---|-------|------|--------|
| 1 | SQL vs NoSQL Choice | [01-SQL-vs-NoSQL-Choice.md](./01-SQL-vs-NoSQL-Choice.md) | Complete |
| 2 | Database Sharding | [02-Database-Sharding.md](./02-Database-Sharding.md) | Complete |
| 3 | Replication Strategies | [03-Replication-Strategies.md](./03-Replication-Strategies.md) | Complete |
| 4 | Caching Strategies | [04-Caching-Strategies.md](./04-Caching-Strategies.md) | Complete |

---

## What's Inside Each File?

### [01 — SQL vs NoSQL Choice](./01-SQL-vs-NoSQL-Choice.md)
**SQL** (PostgreSQL, MySQL): structured, ACID, joins, strong consistency.  
**NoSQL** flavors:
- **Document** (MongoDB) — flexible schema
- **Key-Value** (Redis, DynamoDB) — sub-ms lookups
- **Wide-column** (Cassandra, ScyllaDB) — write-heavy time-series
- **Graph** (Neo4j, Neptune) — relationships, traversals
Decision matrix + interview answers.

### [02 — Database Sharding](./02-Database-Sharding.md)
Horizontal partitioning. Strategies: **Range**, **Hash**, **Consistent Hash**, **Directory/Lookup**, **Geographic**. Shard key selection (cardinality, frequency, monotonicity), challenges (cross-shard joins, distributed TX → **saga**), rebalancing pain, hotspot avoidance.

### [03 — Replication Strategies](./03-Replication-Strategies.md)
**Single-leader** (master-slave) vs **Multi-leader** (master-master) vs **Leaderless** (Dynamo). Sync vs async replication, replication lag, **read-your-writes consistency**, semi-sync, quorum (W+R>N), conflict resolution (LWW, vector clocks, CRDTs).

### [04 — Caching Strategies](./04-Caching-Strategies.md)
**Cache-aside** (most common), **Write-through**, **Write-behind**, **Read-through**, **Refresh-ahead**. Eviction policies (LRU, LFU, FIFO, TTL). Invalidation patterns, cache stampede mitigation, multi-tier cache (L1 process, L2 Redis, CDN), consistency pitfalls.

---

## Recommended Order

```
1. SQL vs NoSQL       ← pick the right store first
2. Sharding           ← scale writes / storage horizontally
3. Replication        ← scale reads + HA
4. Caching            ← scale latency + reduce DB load
```

> Together these 4 form the **storage tier vocabulary** every senior engineer uses in design discussions.

---

## Companion Folders

- [System Design Fundamentals](../System%20Design%20Fundamentals/) — RESHADED, CAP, PACELC
- [System Design Interviews](../../phase-5-interview-preparation/System%20Design%20Interviews/) — applied designs
- [MySQL — Relational Database Mastery](../../mysql_aws_mongodb/MySQL%20%E2%80%94%20Relational%20Database%20Mastery/) — relational deep dive
- [MongoDB — Document Database Mastery](../../mysql_aws_mongodb/MongoDB%20%E2%80%94%20Document%20Database%20Mastery/) — document deep dive
- [AWS — Cloud Platform for Backend Engineers](../../mysql_aws_mongodb/AWS%20%E2%80%94%20Cloud%20Platform%20for%20Backend%20Engineers/) — RDS/Aurora/DynamoDB/ElastiCache

---

## Quick Reference

| Need | Read |
|------|------|
| Pick the right DB | 01 — SQL vs NoSQL |
| Scale writes / storage | 02 — Sharding |
| Scale reads / HA | 03 — Replication |
| Reduce latency / DB load | 04 — Caching |

---

## Status Tracker

```
[x] 01 — SQL vs NoSQL Choice
[x] 02 — Database Sharding
[x] 03 — Replication Strategies
[x] 04 — Caching Strategies
```

> "**Storage tier = bottleneck of most systems.** Right DB choice + smart sharding + healthy replication + disciplined cache = 90% of perf wins."

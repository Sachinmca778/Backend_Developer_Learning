# Database Mastery

Backend developer ke liye **database deep-dive** — SQL advanced, PostgreSQL internals, indexing, query tuning, transactions, NoSQL, connection pooling, aur data modeling. Sab kuch Hinglish mein, code examples ke saath.

---

## Topics & Status

| # | Topic | File | Status |
|---|-------|------|--------|
| 1 | SQL Advanced | [01-SQL-Advanced.md](./01-SQL-Advanced.md) | Not Started |
| 2 | PostgreSQL Specific | [02-PostgreSQL-Specific.md](./02-PostgreSQL-Specific.md) | Not Started |
| 3 | Indexing Strategy | [03-Indexing-Strategy.md](./03-Indexing-Strategy.md) | Not Started |
| 4 | Query Optimization | [04-Query-Optimization.md](./04-Query-Optimization.md) | Not Started |
| 5 | Database Transactions | [05-Database-Transactions.md](./05-Database-Transactions.md) | Not Started |
| 6 | NoSQL Databases | [06-NoSQL-Databases.md](./06-NoSQL-Databases.md) | Not Started |
| 7 | Database Connection Pooling | [07-Database-Connection-Pooling.md](./07-Database-Connection-Pooling.md) | Not Started |
| 8 | Data Modeling | [08-Data-Modeling.md](./08-Data-Modeling.md) | Not Started |

---

## What's Inside Each File?

### [01 — SQL Advanced](./01-SQL-Advanced.md)
- Window functions (`ROW_NUMBER`, `RANK`, `DENSE_RANK`, `LAG`, `LEAD`, `FIRST_VALUE`, `SUM OVER`, `PARTITION BY`)
- Common Table Expressions (CTEs) — including recursive CTEs for tree/hierarchy queries
- Subquery vs JOIN — performance considerations
- `EXISTS` vs `IN` — and the dangerous `NOT IN` with NULLs
- `HAVING` vs `WHERE` — execution order matters
- UPSERT pattern (`INSERT ON CONFLICT DO UPDATE`)

### [02 — PostgreSQL Specific](./02-PostgreSQL-Specific.md)
- `JSONB` — indexable JSON, operators, GIN indexes
- Full-text search (`tsvector`, `tsquery`) + trigram (`pg_trgm`)
- Advisory locks (app-level mutex, cron dedup)
- `LISTEN` / `NOTIFY` — built-in pub-sub
- Materialized views — cached aggregations
- Table partitioning (range, list, hash)
- `pg_stat_statements` — query performance tracking
- `VACUUM` and `ANALYZE` — bloat management

### [03 — Indexing Strategy](./03-Indexing-Strategy.md)
- B-tree (default — equality + range + sort)
- Hash (rare — equality only)
- GIN (JSONB, arrays, full-text)
- GiST (geometric, ranges, KNN)
- Partial indexes (`WHERE` clause)
- Composite indexes — leftmost prefix rule
- Covering indexes (`INCLUDE` columns)
- Index bloat and how to detect/fix

### [04 — Query Optimization](./04-Query-Optimization.md)
- `EXPLAIN` vs `EXPLAIN ANALYZE`
- Reading plan output (cost vs actual, loops)
- Scan types — Seq, Index, Index Only, Bitmap Heap
- Join types — Nested Loop, Hash Join, Merge Join
- Statistics (`pg_statistic`) and stale stats
- Common performance problems (N+1, missing index, type casts)
- Optimization workflow

### [05 — Database Transactions](./05-Database-Transactions.md)
- ACID properties deep dive
- Isolation levels (Read Uncommitted → Serializable)
- Concurrency phenomena (dirty read, non-repeatable read, phantom read, serialization anomaly, write skew)
- Explicit locking (`SELECT FOR UPDATE`, `FOR SHARE`, `NOWAIT`, `SKIP LOCKED`)
- Deadlocks — prevention and recovery
- Best practices (short txns, retry logic, outbox pattern)

### [06 — NoSQL Databases](./06-NoSQL-Databases.md)
- **MongoDB** — document model, aggregation pipeline, indexes, sharding, replica sets, transactions (4.0+)
- **Redis** — String / Hash / List / Set / Sorted Set / Stream, persistence (RDB / AOF), pub-sub, Lua scripting, Redis Cluster, Redis Sentinel
- **Elasticsearch** — inverted index, mappings, analyzers, query DSL (bool/match/term), aggregations
- When to use what + CAP theorem trade-offs

### [07 — Database Connection Pooling](./07-Database-Connection-Pooling.md)
- Why pooling? Connection creation cost
- HikariCP configuration — `maximumPoolSize`, `minimumIdle`, `connectionTimeout`, `idleTimeout`, `maxLifetime`, `leakDetectionThreshold`
- Pool sizing (counterintuitive — more isn't better)
- Monitoring metrics
- Connection leak detection
- PgBouncer — external pooler for high-concurrency apps (session vs transaction pool modes)

### [08 — Data Modeling](./08-Data-Modeling.md)
- Entity-Relationship modeling (1:1, 1:N, M:N)
- Normalization (1NF → 2NF → 3NF → BCNF)
- Denormalization trade-offs (counter caches, materialized views, embedded snapshots)
- Polymorphic associations (STI vs CTI vs polymorphic FK vs multiple nullable FKs)
- Time-series data modeling (partitioning + rollups)
- Audit tables (`created_at`, `updated_at`, `deleted_at`, audit log via triggers)
- Common patterns and anti-patterns

---

## Recommended Learning Order

```
1. Data Modeling (08)        ← Start here — foundation
2. SQL Advanced (01)         ← Master query language
3. Indexing Strategy (03)    ← Make queries fast
4. Query Optimization (04)   ← Diagnose & tune
5. Transactions (05)         ← Concurrency safety
6. PostgreSQL Specific (02)  ← Postgres power features
7. Connection Pooling (07)   ← Production scaling
8. NoSQL (06)                ← Beyond relational
```

---

## Quick Reference Index

### "Mujhe X karna hai" → kahan dekhun?

| Task | File | Section |
|------|------|---------|
| Top N per group query | 01 | Window Functions |
| Hierarchy / tree query | 01 | Recursive CTEs |
| Atomic upsert | 01 | UPSERT |
| Search text in DB | 02 | Full-Text Search |
| Store flexible JSON | 02 | JSONB |
| Cron dedup | 02 | Advisory Locks |
| Cache invalidation | 02 | LISTEN/NOTIFY |
| Speed up `LIKE '%x%'` | 03 | GIN + pg_trgm |
| Choose right index | 03 | Decision Tree |
| Find slow queries | 04 | pg_stat_statements |
| Read EXPLAIN output | 04 | Reading EXPLAIN Output |
| Money transfer safely | 05 | SELECT FOR UPDATE |
| Job queue | 05 | SKIP LOCKED |
| In-memory cache | 06 | Redis |
| Full-text search engine | 06 | Elasticsearch |
| Document store | 06 | MongoDB |
| Tune connection pool | 07 | HikariCP |
| Many app servers + DB conn limit | 07 | PgBouncer |
| Design schema | 08 | ER + Normalization |
| Audit history | 08 | Audit Tables |
| Time-series at scale | 08 | Time-Series Modeling |

---

## Companion Folder

- [API Design & Architecture](../API-Design-&-Architecture/) — REST, GraphQL, pagination, rate limiting, idempotency

---

## Status Tracker

Jab ek file complete kar lo, uski status `Not Started` se `Completed` change kar do (top of file aur is README mein dono jagah).

```
[ ] 01 — SQL Advanced
[ ] 02 — PostgreSQL Specific
[ ] 03 — Indexing Strategy
[ ] 04 — Query Optimization
[ ] 05 — Database Transactions
[ ] 06 — NoSQL Databases
[ ] 07 — Database Connection Pooling
[ ] 08 — Data Modeling
```

Happy learning! 🚀

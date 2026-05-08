# MySQL — Relational Database Mastery

MySQL ki **deep mastery** — architecture, indexing, query optimization, transactions, replication, partitioning, performance tuning, backup, security. Hinglish, with `EXPLAIN` walkthroughs, config cheat sheets, and production traps.

---

## Topics & Status

| # | Topic | File | Status |
|---|-------|------|--------|
| 1 | MySQL Architecture & Internals | [01-MySQL-Architecture-and-Internals.md](./01-MySQL-Architecture-and-Internals.md) | Not Started |
| 2 | Data Types & Schema Design | [02-Data-Types-and-Schema-Design.md](./02-Data-Types-and-Schema-Design.md) | Not Started |
| 3 | Indexing Deep Dive | [03-Indexing-Deep-Dive.md](./03-Indexing-Deep-Dive.md) | Not Started |
| 4 | Query Optimization & EXPLAIN | [04-Query-Optimization-and-EXPLAIN.md](./04-Query-Optimization-and-EXPLAIN.md) | Not Started |
| 5 | Transactions & ACID | [05-Transactions-and-ACID.md](./05-Transactions-and-ACID.md) | Not Started |
| 6 | Locking Mechanisms | [06-Locking-Mechanisms.md](./06-Locking-Mechanisms.md) | Not Started |
| 7 | Replication | [07-Replication.md](./07-Replication.md) | Not Started |
| 8 | Partitioning | [08-Partitioning.md](./08-Partitioning.md) | Not Started |
| 9 | MySQL 8 New Features | [09-MySQL-8-New-Features.md](./09-MySQL-8-New-Features.md) | Not Started |
| 10 | Performance Tuning & Configuration | [10-Performance-Tuning-and-Configuration.md](./10-Performance-Tuning-and-Configuration.md) | Not Started |
| 11 | Backup & Recovery | [11-Backup-and-Recovery.md](./11-Backup-and-Recovery.md) | Not Started |
| 12 | MySQL Security | [12-MySQL-Security.md](./12-MySQL-Security.md) | Not Started |

---

## What's Inside Each File?

### [01 — MySQL Architecture & Internals](./01-MySQL-Architecture-and-Internals.md)
**Client-server model**, connection pool, **query execution pipeline** (parser → optimizer → executor), **storage engine layer** (InnoDB vs MyISAM), **InnoDB buffer pool** (data + index cache), **redo log** (WAL for crash recovery), **undo log** (MVCC + rollback), **doublewrite buffer** (torn page protection), **change buffer**, **adaptive hash index**.

### [02 — Data Types & Schema Design](./02-Data-Types-and-Schema-Design.md)
**Integer types** (TINYINT/SMALLINT/INT/BIGINT — choose smallest fit), **DECIMAL vs FLOAT/DOUBLE** (DECIMAL for money), **VARCHAR vs CHAR** (variable vs fixed), **TEXT/BLOB** caveats with indexing, **DATE / DATETIME / TIMESTAMP** (TIMESTAMP UTC + auto timezone, DATETIME plain), **ENUM/SET** trade-offs, **JSON** type usage, **schema design rules** (1NF/2NF/3NF, when to denormalize).

### [03 — Indexing Deep Dive](./03-Indexing-Deep-Dive.md)
**B-Tree index** (default, equality + range + ORDER BY + GROUP BY), **Hash index** (Memory engine only), **Full-text index** (`MATCH AGAINST`, `BOOLEAN MODE`), **Composite indexes** + **leftmost prefix rule**, **covering index** (no table lookup), **functional indexes** (MySQL 8+ on expressions), **invisible indexes**, index cardinality + selectivity, when NOT to index.

### [04 — Query Optimization & EXPLAIN](./04-Query-Optimization-and-EXPLAIN.md)
**`EXPLAIN` / `EXPLAIN ANALYZE`** column-by-column walkthrough (`type` column ranking — `ALL` worst, `const` best), `key` / `key_len` / `ref` / `rows` × `filtered` = actual examined, `Extra` flags (**`Using filesort`**, **`Using temporary`**, **`Using index condition`**, **`Using index`**), **slow query log** + `long_query_time`, **`pt-query-digest`**, **query hints** (`FORCE INDEX`, `IGNORE INDEX`, `STRAIGHT_JOIN`), N+1 problem from app perspective.

### [05 — Transactions & ACID](./05-Transactions-and-ACID.md)
`BEGIN` / `COMMIT` / `ROLLBACK` / `SAVEPOINT`, `autocommit=1` default, **isolation levels** with examples for each (READ UNCOMMITTED dirty read, READ COMMITTED non-repeatable, **REPEATABLE READ** MySQL default + phantoms, SERIALIZABLE), **MVCC** internals (read view + version chains via undo log), **deadlock auto-detection**, `SHOW ENGINE INNODB STATUS`.

### [06 — Locking Mechanisms](./06-Locking-Mechanisms.md)
**Row-level locks** (InnoDB), **table locks** (MyISAM legacy), **intent locks (IS/IX)** for table-level markers, **shared lock (`SELECT ... FOR SHARE`)**, **exclusive lock (`SELECT ... FOR UPDATE`)**, **gap locks** (range between rows = phantom prevention), **next-key locks** (row + gap), **insert intention locks**, deadlock diagnosis via `SHOW ENGINE INNODB STATUS`, `innodb_lock_wait_timeout`, `innodb_deadlock_detect`.

### [07 — Replication](./07-Replication.md)
**Source-replica** (master-slave) topology, **binary log (binlog)** + 3 formats — STATEMENT (non-deterministic risk), ROW (safe + larger), **MIXED**, **GTID-based replication** (Global Transaction ID — easier failover, no manual binlog position), **semi-synchronous replication** (one replica acks before commit), **read replicas** for read scaling, replication lag detection (`Seconds_Behind_Source`), **failover** with ProxySQL/Orchestrator/MySQL Group Replication.

### [08 — Partitioning](./08-Partitioning.md)
**Partitioning vs Sharding** — partitioning is single-server horizontal, **types** — RANGE (e.g., year), LIST (enum values), HASH, KEY (MySQL hash), **partition pruning** by optimizer, **subpartitioning**, **limitations** (no FK with partitioning, all unique indexes must include partition key, max 8192 partitions), real use case patterns (time-series partitioned by month, archived data dropped via `DROP PARTITION`).

### [09 — MySQL 8 New Features](./09-MySQL-8-New-Features.md)
**Window functions** (`ROW_NUMBER`, `RANK`, `DENSE_RANK`, `LAG`, `LEAD`, `NTILE`, `FIRST_VALUE`, `LAST_VALUE`, `OVER (PARTITION BY ...)`), **CTEs** (`WITH` clause + **recursive CTEs** for hierarchical), **JSON data type** + functions (`JSON_EXTRACT`, `->`, `->>`, `JSON_TABLE`, `JSON_ARRAYAGG`), **invisible indexes**, **`SKIP LOCKED` + `NOWAIT`** for queue patterns, **descending indexes**, **roles** (`CREATE ROLE`), **histograms**, **persisted system variables**.

### [10 — Performance Tuning & Configuration](./10-Performance-Tuning-and-Configuration.md)
**`innodb_buffer_pool_size`** (70-80% RAM dedicated server — most impactful), **`innodb_log_file_size`** (larger = fewer checkpoints = better write throughput), **`max_connections`** (high → memory exhaustion → use **ProxySQL** / connection pooler), **query_cache** removed in MySQL 8 (was unreliable), **Performance Schema** + **sys schema** for diagnostics, **`pt-online-schema-change`** for zero-downtime ALTER, **server tuning checklist** (10 most important variables).

### [11 — Backup & Recovery](./11-Backup-and-Recovery.md)
**Logical backup** — `mysqldump` (portable, slow), `mysqlpump` (parallel), **`mydumper`** (fastest logical), **physical backup** — **Percona XtraBackup** (hot, no downtime, incremental support), **point-in-time recovery (PITR)** = full backup + replay binlogs up to target time, **binlog backup** strategy, **test restores regularly** (untested backup = no backup), backup retention strategy (3-2-1 rule), encryption at rest.

### [12 — MySQL Security](./12-MySQL-Security.md)
**Authentication** — `mysql_native_password` (legacy), **`caching_sha2_password`** (MySQL 8 default), LDAP plugin, **privilege system** — `GRANT` / `REVOKE` at global/db/table/column level, **principle of least privilege** (app user no `SUPER`/`FILE`), **SSL/TLS** for client connections, **audit plugins** (Enterprise + MariaDB Audit), `sql_mode` (`STRICT_TRANS_TABLES` catches truncation), preventing **SQL injection** at framework level (parameterized queries — cross-ref Security folder).

---

## Recommended Order

```
1. Architecture           ← mental model first
2. Data Types & Schema    ← foundation for design
3. Indexing               ← #1 perf lever
4. Query Optimization     ← read EXPLAIN fluently
5. Transactions & ACID    ← correctness
6. Locking                ← concurrency depth
7. Replication            ← HA + read scale
8. Partitioning           ← single-server scale
9. MySQL 8 Features       ← modern SQL
10. Performance Tuning    ← server-side knobs
11. Backup & Recovery     ← reliability
12. Security              ← lock down prod
```

---

## Why MySQL Mastery Matters

| Role | Why MySQL? |
|------|-----------|
| Backend Engineer | Default DB for most app stacks (still 60%+ market) |
| Senior Engineer | Tuning + EXPLAIN literacy = lifesaver in production |
| Architect | Choosing partitioning / replication / sharding strategies |
| DBA | Ops, backup, security — full ownership |

> "**Reading EXPLAIN fluently + understanding InnoDB locks = single biggest productivity unlock for DB-heavy backend roles.**"

---

## Architecture At-a-Glance

```
┌──────────────────────────────────────────────────┐
│              Application                         │
└─────────────┬────────────────────────────────────┘
              │ JDBC / Connector
┌─────────────▼────────────────────────────────────┐
│              MySQL Server                        │
│  ┌─────────────────────────────────────┐         │
│  │  Connection Pool (per-thread)       │         │
│  └─────────────────────────────────────┘         │
│  ┌─────────────────────────────────────┐         │
│  │  Parser → Optimizer → Executor      │         │
│  └─────────────────────────────────────┘         │
│  ┌─────────────────────────────────────┐         │
│  │  Storage Engine API                 │         │
│  │  ┌─────────┐  ┌─────────┐           │         │
│  │  │ InnoDB  │  │ MyISAM  │  ...      │         │
│  │  └─────────┘  └─────────┘           │         │
│  └─────────────────────────────────────┘         │
└──────────────────────────────────────────────────┘
              │
       ┌──────┴──────┐
   Data files     Logs (binlog, redo, undo)
```

---

## Companion Folders

- [Database Mastery](../../backend-skills/Database-Mastery/) — SQL fundamentals + advanced (cross-ref)
- [Performance & Optimization](../../phase-4-advanced-topics/Performance%20%26%20Optimization/) — query optimization in depth
- [Security Best Practices](../../backend-skills/Security-Best-Practices/) — SQL injection prevention
- [Spring Boot Interview Topics](../../phase-5-interview-preparation/Spring%20Boot%20Interview%20Topics/) — JPA + connection pool

---

## Quick Reference

| Need | Where |
|------|-------|
| Slow query | `EXPLAIN ANALYZE` (file 04) |
| Locking issue | `SHOW ENGINE INNODB STATUS` (file 06) |
| Replication lag | `Seconds_Behind_Source` (file 07) |
| Memory tuning | `innodb_buffer_pool_size` (file 10) |
| Schema change | `pt-online-schema-change` (file 10) |
| Backup | `mysqldump` / `xtrabackup` (file 11) |
| User mgmt | `GRANT` / `caching_sha2_password` (file 12) |

---

## Status Tracker

```
[ ] 01 — MySQL Architecture & Internals
[ ] 02 — Data Types & Schema Design
[ ] 03 — Indexing Deep Dive
[ ] 04 — Query Optimization & EXPLAIN
[ ] 05 — Transactions & ACID
[ ] 06 — Locking Mechanisms
[ ] 07 — Replication
[ ] 08 — Partitioning
[ ] 09 — MySQL 8 New Features
[ ] 10 — Performance Tuning & Configuration
[ ] 11 — Backup & Recovery
[ ] 12 — MySQL Security
```

> "MySQL ko **black box** mat samjho. Storage engine + buffer pool + locking samajh aaya, **80% prod issues debug ho jate**."

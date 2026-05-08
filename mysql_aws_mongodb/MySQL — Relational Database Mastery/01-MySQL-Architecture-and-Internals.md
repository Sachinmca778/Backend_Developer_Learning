# MySQL Architecture & Internals

## Status: Not Started

---

## Table of Contents

1. [Big Picture](#big-picture)
2. [Client-Server Model](#client-server-model)
3. [Connection Layer](#connection-layer)
4. [Query Execution Pipeline](#query-execution-pipeline)
5. [Storage Engine Layer](#storage-engine-layer)
6. [InnoDB vs MyISAM](#innodb-vs-myisam)
7. [InnoDB Buffer Pool](#innodb-buffer-pool)
8. [Redo Log (Crash Recovery)](#redo-log-crash-recovery)
9. [Undo Log (MVCC + Rollback)](#undo-log-mvcc--rollback)
10. [Doublewrite Buffer](#doublewrite-buffer)
11. [Change Buffer & Adaptive Hash Index](#change-buffer--adaptive-hash-index)
12. [On-Disk File Layout](#on-disk-file-layout)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## Big Picture

```
                  ┌──────────────────────────────────────────┐
   Client (JDBC) ─┤     Connection Pool / Thread per Conn   │
                  └─────────┬────────────────────────────────┘
                            │
                  ┌─────────▼──────────┐
                  │      Parser        │  → SQL → AST
                  ├────────────────────┤
                  │     Optimizer      │  → Best execution plan
                  ├────────────────────┤
                  │      Executor      │  → Calls storage engine API
                  └─────────┬──────────┘
                            │
                  ┌─────────▼──────────────────────────────┐
                  │   Storage Engine API (pluggable)       │
                  │  ┌──────────┐ ┌──────────┐ ┌────────┐  │
                  │  │  InnoDB  │ │  MyISAM  │ │ Memory │  │
                  │  └──────────┘ └──────────┘ └────────┘  │
                  └────────────────────────────────────────┘
                            │
                  ┌─────────▼──────────────────────────────┐
                  │   Data files + Logs (binlog/redo/undo) │
                  └────────────────────────────────────────┘
```

---

## Client-Server Model

> "MySQL **server process** persistently chalta hai (port 3306 default). Clients TCP/Unix socket pe connect karte."

```
mysqld (server) ────── 3306 ────── mysql client / app / Workbench
```

### Default ports

| Service | Port |
|---------|------|
| MySQL primary | 3306 |
| MySQL X Protocol | 33060 |
| MySQL admin (optional) | 33062 |

### Connection types

| | TCP | Unix socket |
|--|-----|-------------|
| Cross-machine | ✅ | ❌ |
| Localhost only | ✅ | ✅ (faster) |
| Path | `host:3306` | `/var/run/mysqld/mysqld.sock` |

---

## Connection Layer

### Per-connection thread (default model)

Har connection ke liye separate **thread** spawn hoti hai:

```
Connection 1 → Thread 1
Connection 2 → Thread 2
...
Connection N → Thread N
```

### Default `max_connections = 151`

Production: 200-500 typical. Beyond → resource pressure.

### Why connection pool zaroori?

- Each new connection: TCP handshake + auth + thread spawn (~1-5 ms overhead)
- Java apps: **HikariCP** pools connections
- Database side: Use **ProxySQL** / MySQL Router for further pooling

→ Cross-ref: `phase-4 / Performance & Optimization / 02-Connection-Pool-Tuning.md`.

### Show current connections

```sql
SHOW PROCESSLIST;
SHOW STATUS LIKE 'Threads_connected';
SHOW STATUS LIKE 'Max_used_connections';
```

### Thread cache

`thread_cache_size` reuses threads after disconnect (avoids re-spawn):

```sql
SHOW VARIABLES LIKE 'thread_cache_size';
```

---

## Query Execution Pipeline

```
SQL Query
  ↓
1. Parser           — Tokenize + check syntax → AST (parse tree)
  ↓
2. Optimizer        — Cost-based plan selection
                      • Index choice
                      • JOIN order
                      • Whether to use temp table / filesort
  ↓
3. Executor         — Calls storage engine API
                      • Fetches rows
                      • Applies filters / GROUP BY / ORDER BY
                      • Returns result set
  ↓
4. Result returned
```

### Example trace

```sql
SELECT name FROM users WHERE age > 30 ORDER BY created_at LIMIT 10;
```

| Phase | Action |
|-------|--------|
| Parser | Validate SQL — column names, table exists |
| Optimizer | Pick: index on `age` ya index on `created_at`? |
| Executor | Iterate via chosen index, apply LIMIT |

→ See `EXPLAIN` output in file `04`.

### Optimizer notes

- Cost-based (uses statistics)
- Histograms (MySQL 8+) for skewed data
- May choose **wrong** plan when stats stale → run `ANALYZE TABLE`

---

## Storage Engine Layer

> "MySQL ka **unique design**: storage engine pluggable hai. Same SQL → different engine = different on-disk layout, locking, transactions."

### Engines (modern)

| Engine | Use |
|--------|-----|
| **InnoDB** | Default since MySQL 5.5; ACID, MVCC, foreign keys |
| MyISAM | Read-only / table-level lock; legacy |
| **Memory** | RAM only; fast lookups; lost on restart |
| **Archive** | Append-only, compressed; old logs |
| **NDB** (Cluster) | In-memory clustered |
| **Federated** | Remote table access |
| **CSV** | Plain text — for export |

### Set per-table

```sql
CREATE TABLE users (...) ENGINE=InnoDB;
```

### Default engine

```sql
SHOW VARIABLES LIKE 'default_storage_engine';   -- InnoDB
```

### Why InnoDB?

- ACID + transactions
- Row-level locking (concurrency)
- MVCC (non-blocking reads)
- Crash recovery
- Foreign keys

> "**Use InnoDB always**. MyISAM only if you have a reason — and you usually don't."

---

## InnoDB vs MyISAM

| Feature | InnoDB | MyISAM |
|---------|--------|--------|
| Transactions | ✅ ACID | ❌ |
| Row-level locking | ✅ | ❌ (table-level only) |
| MVCC | ✅ | ❌ |
| Foreign keys | ✅ | ❌ |
| Crash recovery | ✅ (redo log) | ❌ (table corruption likely) |
| Full-text index | ✅ (5.6+) | ✅ |
| Spatial index | ✅ (5.7+) | ✅ |
| Auto-increment count | Slower (table scan on startup pre-8) | Stored separately |
| `COUNT(*)` no WHERE | Slow (must scan) | Fast (stored count) |
| Compressed read | ✅ | ✅ |
| Storage | tablespaces (`.ibd`) | `.MYD` + `.MYI` |
| Default since | 5.5 | Pre-5.5 |

### When MyISAM ever?

- **Almost never** in modern apps
- Read-only archive tables
- Legacy systems

→ Migration: `ALTER TABLE foo ENGINE=InnoDB;` (test on copy first).

---

## InnoDB Buffer Pool

> "InnoDB ka **brain** — RAM mein data + index pages cache karta hai."

### Why?

- Disk seek = ~10ms (HDD), 0.1ms (SSD)
- RAM access = ~100ns
- → 100,000x slower disk

→ Buffer pool memory mein hot pages rakhta — **most reads + writes never touch disk directly**.

### Structure

```
Buffer pool = Multiple instances (innodb_buffer_pool_instances)
              Each = LRU list of 16KB pages
              ┌─────────────────────────┐
              │ Young (5/8) — hot pages │
              ├─────────────────────────┤
              │ Old (3/8) — recent IO   │
              └─────────────────────────┘
```

### Sizing

```sql
-- Dedicated MySQL server: 70-80% of RAM
innodb_buffer_pool_size = 64G   -- on 96G server
innodb_buffer_pool_instances = 8
```

→ Most impactful tuning knob (file 10).

### Monitoring hit rate

```sql
SHOW GLOBAL STATUS LIKE 'Innodb_buffer_pool_reads';        -- disk reads
SHOW GLOBAL STATUS LIKE 'Innodb_buffer_pool_read_requests'; -- total

Hit rate = 1 - (reads / read_requests)
```

→ Goal: **99%+** for OLTP. < 95% → buffer pool too small / hot working set too big.

### Page types in buffer pool

- Data pages
- Index pages
- Undo pages
- Lock info pages
- Insert buffer pages
- Adaptive hash index pages

---

## Redo Log (Crash Recovery)

> "**Write-Ahead Log (WAL)** — change first redo log mein, fir buffer pool, fir later disk."

### Why?

If server crashes mid-write:
- Buffer pool lost (RAM)
- But redo log durable
- On restart → replay redo log → DB consistent

### Structure

- Circular file pair (`ib_logfile0`, `ib_logfile1`) — pre-8
- MySQL 8.0.30+: `#innodb_redo` directory with multiple files
- Sequential write (fast)

### Sizing — `innodb_log_file_size`

| Value | Effect |
|-------|--------|
| Small (256MB) | Frequent checkpoints, more disk IO |
| **Large (1-4GB)** | Fewer checkpoints, smoother writes |

```sql
-- Production typical
innodb_log_file_size = 2G
innodb_log_files_in_group = 2     -- Total = 4G redo
```

→ Larger = better write throughput, but **longer crash recovery time**.

### Force flush behaviour — `innodb_flush_log_at_trx_commit`

| Value | Behaviour | Durability |
|-------|-----------|------------|
| **1** (default) | Flush + fsync on every commit | Strong (no data loss) |
| 2 | Flush to OS, fsync once/sec | Modest (OS crash → 1s loss) |
| 0 | Flush + fsync once/sec | Weak (MySQL crash → 1s loss) |

→ **Stay at 1** for ACID. 2 for performance trade-off.

---

## Undo Log (MVCC + Rollback)

> "Old row versions store karta hai — rollback ke liye + MVCC ke liye."

### Two purposes

1. **Rollback** — `ROLLBACK` issued → reverse changes using undo
2. **MVCC** — Concurrent reads see consistent snapshot via undo chain

### MVCC example

```
TX1: BEGIN; UPDATE users SET name='X' WHERE id=1; (uncommitted)
TX2: BEGIN; SELECT name FROM users WHERE id=1;
        ↑
        Reads OLD value via undo chain (consistent snapshot)
```

→ TX2 not blocked by TX1 — **non-blocking reads**.

### Undo location

- MySQL 5.6+: separate **undo tablespaces** (`undo_001`, `undo_002`)
- Configurable via `innodb_undo_tablespaces`

### Undo log purge

Old undo entries cleaned by **purge thread** when no transactions need them.

```sql
SHOW GLOBAL STATUS LIKE 'Innodb_purge_undo_log_pages';
```

### "Long transactions hold undo"

Long-running TX blocks purge → undo grows → disk pressure + slowdown.

→ **Avoid 30+ min transactions** (cross-ref `05-Transactions-and-ACID.md`).

---

## Doublewrite Buffer

> "**Torn page protection** — partial page writes safe rakhta hai."

### Problem

Disk write a 16KB page mid-power-failure:
- Page partially on disk → corrupt
- Redo log helps, but redo log assumes intact pages

### Solution

```
1. InnoDB writes 16KB page to doublewrite buffer (sequential, fast)
2. fsync doublewrite buffer
3. Then write page to actual location
4. fsync actual location
```

If crash between step 3-4:
- On recovery, MySQL detects corrupt page
- Reads from doublewrite buffer → restores

### Cost

- 2x writes (but sequential write to doublewrite is cheap)
- ~5-10% overhead

→ **Don't disable** unless using a filesystem with built-in atomic writes (e.g., ZFS, btrfs configured properly).

```sql
SHOW VARIABLES LIKE 'innodb_doublewrite';   -- ON default
```

---

## Change Buffer & Adaptive Hash Index

### Change buffer

> "Secondary index changes deferred — buffer karke later merge."

For non-unique secondary indexes, when DML happens:

- Index page **not in buffer pool** → write to change buffer (RAM)
- Later, when page loaded → merge changes
- Saves random IO

→ Helps **insert-heavy** workloads.

```sql
SHOW VARIABLES LIKE 'innodb_change_buffering';
-- Values: all, none, inserts, deletes, changes, purges
```

### Adaptive hash index

> "Frequently accessed B-tree pages → builds in-memory hash → O(1) lookup."

InnoDB monitors index access patterns; auto-creates hash for hot prefixes.

```sql
SHOW VARIABLES LIKE 'innodb_adaptive_hash_index';   -- ON default
```

→ Helps **point lookups**. Can be disabled if buffer pool latch contention.

---

## On-Disk File Layout

```
/var/lib/mysql/
├── auto.cnf                    server UUID
├── ib_logfile0, ib_logfile1    redo log (pre 8.0.30)
├── #innodb_redo/               redo log (8.0.30+)
├── #innodb_temp/               session temp
├── ibdata1                     system tablespace (small)
├── ibtmp1                      global temp tablespace
├── undo_001, undo_002          undo tablespaces
├── mysql.ibd                   metadata DB
├── sys/                        sys schema
├── performance_schema/
│
├── <database_name>/
│   ├── <table>.ibd             InnoDB table data + indexes (per-table since 5.6+)
│
└── binlog.000001, ...          binary log (replication)
```

### `innodb_file_per_table` (default ON since 5.6)

Each table → own `.ibd` file. Easier to manage per-table.

### Show data dir

```sql
SHOW VARIABLES LIKE 'datadir';
```

---

## Pitfalls

1. **`innodb_buffer_pool_size` too small** — disk IO bottleneck.
2. **`innodb_log_file_size` too small** — frequent checkpoints; write stalls.
3. **MyISAM in transactional code** — silent corruption, no rollback.
4. **`max_connections` too high** — memory exhaustion (each connection ~1-2 MB stack).
5. **Connection without pool** — TCP handshake overhead per query.
6. **Long-running transaction** — undo bloat, replication lag, locking issues.
7. **Disable doublewrite** without atomic-write FS — corruption risk.
8. **Stale stats** — optimizer picks bad plan; `ANALYZE TABLE` periodically.
9. **No monitoring** of buffer pool hit rate.
10. **`innodb_flush_log_at_trx_commit = 0/2`** in production payment system — data loss on crash.

---

## Cheat Sheet

| Component | Purpose |
|-----------|---------|
| Buffer pool | RAM cache of data + index pages |
| Redo log | WAL — crash recovery |
| Undo log | MVCC + rollback |
| Doublewrite | Torn-page protection |
| Change buffer | Defer non-unique index updates |
| Adaptive hash index | O(1) hot-page lookup |
| binlog | Replication / PITR |

| Knob | Default | Production |
|------|---------|-----------|
| `innodb_buffer_pool_size` | 128M | 70-80% of RAM |
| `innodb_log_file_size` | 48M | 1-4G |
| `innodb_flush_log_at_trx_commit` | 1 | 1 (don't change for ACID) |
| `max_connections` | 151 | 200-500 + ProxySQL |
| `innodb_file_per_table` | ON | ON |

| Tool | Use |
|------|-----|
| `SHOW PROCESSLIST` | Active connections |
| `SHOW ENGINE INNODB STATUS` | Locks, transactions, deadlocks |
| `SHOW GLOBAL STATUS` | Counters (Innodb_*, Threads_*) |
| Performance Schema | Detailed instrumentation |
| sys schema | Pre-built diagnostic queries |

---

## Practice

1. Calculate buffer pool hit rate from `SHOW GLOBAL STATUS`. Goal 99%+.
2. Trace a SELECT through pipeline (parser → optimizer → executor → InnoDB).
3. Show your `SHOW ENGINE INNODB STATUS` and identify the buffer pool section.
4. Check `innodb_buffer_pool_size`, decide ideal value for your hardware.
5. Identify which engine your tables use — `SHOW TABLE STATUS`.

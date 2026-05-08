# Performance Tuning & Configuration

## Status: Not Started

---

## Table of Contents

1. [Tuning Methodology](#tuning-methodology)
2. [Top 10 Server Variables](#top-10-server-variables)
3. [innodb_buffer_pool_size](#innodb_buffer_pool_size)
4. [innodb_log_file_size](#innodb_log_file_size)
5. [innodb_flush_log_at_trx_commit](#innodb_flush_log_at_trx_commit)
6. [max_connections + Connection Pool](#max_connections--connection-pool)
7. [tmp_table_size & max_heap_table_size](#tmp_table_size--max_heap_table_size)
8. [Performance Schema & sys schema](#performance-schema--sys-schema)
9. [pt-online-schema-change](#pt-online-schema-change)
10. [Hardware Considerations](#hardware-considerations)
11. [Monitoring Checklist](#monitoring-checklist)
12. [Common Tuning Wins](#common-tuning-wins)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## Tuning Methodology

> "**Measure first.** Tune top bottlenecks. Don't tune randomly."

### Sequence

```
1. Define SLO (latency, QPS targets)
2. Profile: slow query log + Performance Schema
3. Top 5 slow queries → fix at app/index/schema level FIRST
4. Server-side tuning AFTER application optimization
5. Hardware upgrade LAST resort
```

### Common mistake

> "Buffer pool 8x karke speed improve karenge."

→ Most speed wins come from **better queries + indexes**, not server tuning.

---

## Top 10 Server Variables

| Variable | Impact |
|----------|--------|
| **`innodb_buffer_pool_size`** | #1 impact (memory cache) |
| `innodb_log_file_size` | Write throughput |
| `innodb_flush_log_at_trx_commit` | Durability vs speed |
| `innodb_flush_method` | OS file IO method |
| `innodb_io_capacity` | Background IO budget |
| `max_connections` | Connection limit |
| `tmp_table_size` | In-memory temp tables |
| `sort_buffer_size` | Per-connection sort memory |
| `join_buffer_size` | Per-connection join buffer |
| `binlog_format` | Replication safety |

→ Big impact knobs (top 5) tune carefully; small ones rarely matter.

---

## innodb_buffer_pool_size

> "**The single most impactful MySQL variable.**"

### Recommendation

| Server type | Setting |
|-------------|---------|
| Dedicated MySQL | **70-80% of RAM** |
| Shared host | 50% of RAM |
| Container | Lower of (75% allocated mem) or actual mem limit |

```ini
# 96GB RAM server
innodb_buffer_pool_size = 64G
innodb_buffer_pool_instances = 8
```

### Sizing rule

If **buffer pool > working set size** → 99%+ hit rate → fast.

### Check hit rate

```sql
SHOW GLOBAL STATUS LIKE 'Innodb_buffer_pool_reads';        -- disk reads
SHOW GLOBAL STATUS LIKE 'Innodb_buffer_pool_read_requests'; -- total

-- Hit rate = 1 - (reads / read_requests)
```

→ Goal: **>99%** for OLTP.

### Multiple instances

```ini
innodb_buffer_pool_instances = 8     # ~8GB per instance ideal
```

→ Reduces internal latch contention. Default 1 (legacy); MySQL auto-tunes if pool > 1GB.

### Online resize (8.0+)

```sql
SET GLOBAL innodb_buffer_pool_size = 96 * 1024 * 1024 * 1024;
```

→ Live resize without restart. Pre-8 required restart.

---

## innodb_log_file_size

> "**Redo log size**. Larger = fewer checkpoints = better write throughput."

### Pre-MySQL 8.0.30

```ini
innodb_log_file_size = 2G
innodb_log_files_in_group = 2     # total redo = 4GB
```

### MySQL 8.0.30+

```ini
innodb_redo_log_capacity = 4G     # new variable; auto-managed
```

### Sizing

| Workload | Recommendation |
|----------|----------------|
| OLTP (lots of small writes) | 1-4 GB |
| Bulk loading | 4-8 GB |
| Mostly read | 512 MB - 1 GB |

### Trade-off

| Larger | Smaller |
|--------|---------|
| Better write throughput | Faster crash recovery |
| Higher disk usage | More frequent checkpoints |
| Longer crash recovery | Less disk usage |

→ Balance: 1-4 GB for production OLTP.

### Set

Pre-8.0.30:

```sql
-- Stop server, edit my.cnf, start (required restart pre-8)
```

8.0.30+:

```sql
SET GLOBAL innodb_redo_log_capacity = 4 * 1024 * 1024 * 1024;
```

---

## innodb_flush_log_at_trx_commit

> "**Durability tuning**. Critical for ACID."

| Value | Behaviour | Durability |
|-------|-----------|------------|
| **1** (default) | Flush + fsync per commit | Strong (ACID) |
| 2 | Flush to OS page cache, fsync once/sec | OS crash → 1s loss |
| 0 | Flush + fsync once/sec | MySQL crash → 1s loss |

### Real-world

| Use case | Value |
|----------|-------|
| Banking / payment | **1** (no compromise) |
| General OLTP | **1** |
| High-write logging (loss OK) | 2 |
| Replicas (replicated from primary) | 2 |

### `sync_binlog` parallel

| Value | What |
|-------|------|
| 1 | fsync binlog per commit (default) |
| N | fsync every N commits |
| 0 | OS-controlled |

→ Both `innodb_flush_log_at_trx_commit = 1` + `sync_binlog = 1` for **full durability**.

→ Performance impact: ~10-30%. Acceptable.

---

## max_connections + Connection Pool

### Default

```sql
max_connections = 151
```

### Production

```ini
max_connections = 500    # typical mid-traffic
max_connections = 2000   # high-traffic with pooler
```

### Memory cost

Each connection ≈ 1-2 MB (thread stack + buffers).

→ 5000 connections = 5-10 GB just for connection state. **Use pooler instead**.

### Connection pooler — ProxySQL / MySQL Router

```
App pools (1000 connections)
   ↓
ProxySQL (multiplex)
   ↓
MySQL (50 connections actually open)
```

→ App side scales to 1000s; DB sees fewer real connections.

### Connection pool sizing

Common formula (cross-ref `phase-4 / Performance & Optimization / 02-Connection-Pool-Tuning.md`):

```
pool_size = (cores × 2) + spindles
```

For SSD: `cores × 2`.

### Show

```sql
SHOW STATUS LIKE 'Threads_connected';
SHOW STATUS LIKE 'Max_used_connections';
```

If `Max_used_connections / max_connections > 80%` → raise OR add pooler.

---

## tmp_table_size & max_heap_table_size

> "Internal **in-memory temp tables** for GROUP BY / DISTINCT / UNION."

```sql
tmp_table_size = 64M
max_heap_table_size = 64M     # set BOTH equal
```

→ When temp table exceeds → spills to disk (slow).

### Diagnose

```sql
SHOW GLOBAL STATUS LIKE 'Created_tmp_disk_tables';
SHOW GLOBAL STATUS LIKE 'Created_tmp_tables';

-- ratio = disk / total
```

→ Disk ratio > 25% → temp tables too small or query needs rewriting.

### Caveats

- Per-connection allocation possible
- Don't set huge (e.g., 4 GB) — could exhaust RAM with many connections

→ 64-256 MB typical.

---

## Performance Schema & sys schema

### Performance Schema

> "Extensive instrumentation framework — every event tracked."

```sql
SHOW VARIABLES LIKE 'performance_schema%';
```

→ Default ON in MySQL 5.6+.

#### Common queries

```sql
-- Slowest statements (current session)
SELECT * FROM performance_schema.events_statements_summary_by_digest
ORDER BY SUM_TIMER_WAIT DESC LIMIT 10;

-- Top tables by IO
SELECT * FROM performance_schema.table_io_waits_summary_by_table
ORDER BY SUM_TIMER_WAIT DESC LIMIT 10;

-- Lock waits
SELECT * FROM performance_schema.metadata_locks WHERE LOCK_STATUS='PENDING';
```

### sys schema (built-in views)

> "**User-friendly wrappers** over Performance Schema."

```sql
-- Top 10 slow statements
SELECT * FROM sys.statements_with_runtimes_in_95th_percentile;

-- Unused indexes
SELECT * FROM sys.schema_unused_indexes;

-- Tables with full scans
SELECT * FROM sys.statements_with_full_table_scans;

-- Lock waits
SELECT * FROM sys.innodb_lock_waits;

-- Connections summary
SELECT * FROM sys.processlist;
```

→ Production diagnostic gold mine. Use these views.

---

## pt-online-schema-change

> "**Zero-downtime ALTER TABLE** for huge tables. Percona Toolkit."

### Problem

Native `ALTER TABLE big_table ADD INDEX ...` may:
- Lock for hours
- Block writes
- Replicate badly

### How `pt-online-schema-change` works

```
1. Create empty new table with desired schema
2. Add triggers on original (capture INSERT/UPDATE/DELETE)
3. Copy rows from original → new table in chunks
4. Atomic rename (original_old + new = original)
5. Drop original_old
```

### Run

```bash
pt-online-schema-change \
    --alter "ADD INDEX idx_email (email)" \
    --execute \
    h=localhost,D=appdb,t=users,u=root,p=pwd
```

### Built-in alternative — MySQL 8 instant DDL

Some ALTERs are now instant (no copy):

```sql
-- Instant operations (no rebuild)
ALTER TABLE t ADD COLUMN c VARCHAR(50), ALGORITHM=INSTANT;
ALTER TABLE t DROP COLUMN c, ALGORITHM=INSTANT;
ALTER TABLE t RENAME COLUMN a TO b, ALGORITHM=INSTANT;
```

→ But many ALTERs still rebuild. Use `pt-osc` for those.

### gh-ost (alternative)

GitHub's tool — **no triggers** (uses binlog instead). Cleaner for replication.

```bash
gh-ost --alter="ADD INDEX idx_email (email)" --table=users --execute
```

---

## Hardware Considerations

### Disk

| Type | IOPS | Use |
|------|------|-----|
| HDD | 100-200 | Don't (only archive) |
| SATA SSD | 5K-50K | OK |
| NVMe SSD | 100K-1M | **Recommended** |

→ NVMe SSDs = night-and-day vs HDD for OLTP.

### RAM

- Buffer pool wants 70% of RAM
- Plus OS + other processes
- **More RAM = bigger working set in cache**

### CPU

- High-frequency cores helpful
- Many cores helpful for parallel queries (8+)
- 4-16 cores typical mid-tier

### Network

- 1 GbE OK for moderate
- 10 GbE for replication-heavy / large datasets

### Filesystem

- ext4 / XFS for Linux
- O_DIRECT recommended:

```ini
innodb_flush_method = O_DIRECT
```

→ Avoids double-caching (OS cache + buffer pool).

---

## Monitoring Checklist

### Metrics to track

| Metric | Goal |
|--------|------|
| Buffer pool hit rate | > 99% |
| Connections used / max | < 80% |
| Slow queries / min | Track trend |
| Queries / sec | Track baseline |
| CPU usage | < 70% normal |
| Disk IO util | < 80% |
| Replication lag | < 5s |
| Deadlocks / hour | < few |
| Aborted connections | Investigate spikes |

### Tools

- **Percona Monitoring and Management (PMM)** — free, comprehensive
- **MySQL Enterprise Monitor** — paid
- **Datadog / New Relic / Dynatrace** — APM with MySQL integration
- **Prometheus + mysqld_exporter + Grafana** — DIY
- **CloudWatch** (AWS RDS)
- **VictoriaMetrics**

### Alert on

- Replication broken
- Disk space < 20%
- Connection saturation
- Long queries > X seconds
- Deadlock spike

---

## Common Tuning Wins

### 1. Identify slow queries → add indexes

Slow query log → `pt-query-digest` → top 5 → indexes.

### 2. Convert OFFSET pagination → keyset

Massive win on pages > 1000.

### 3. `SELECT *` → specific columns

Enable covering indexes.

### 4. Long transactions → break up

Reduce undo bloat + lock contention.

### 5. Replica reads

Offload read load.

### 6. Schema cleanup

- Drop unused indexes (`sys.schema_unused_indexes`)
- Right-size data types
- Archive cold data to separate table

### 7. Configuration

Buffer pool 70-80% RAM. Check hit rate.

### 8. Hardware

NVMe SSD if HDD/SATA. RAM upgrade if buffer pool starved.

---

## Pitfalls

1. **`innodb_buffer_pool_size` too small** — bottleneck.
2. **Tuning without baseline metrics** — flying blind.
3. **`max_connections` too high** without pooler — memory exhaustion.
4. **`innodb_flush_log_at_trx_commit = 0/2`** in financial app — data loss.
5. **`tmp_table_size` huge** + 1000 connections → OOM.
6. **Native `ALTER TABLE` on 100M-row table** in business hours.
7. **Unused indexes never dropped** — write amplification.
8. **No monitoring** of replication lag.
9. **Hardware tuning skipped** — slow disk = perf cap.
10. **Tuning every variable** — most defaults fine; focus on top 5.
11. **No `O_DIRECT`** — double-caching wastes RAM.
12. **Stale stats** — `ANALYZE TABLE` periodically.

---

## Cheat Sheet

| Variable | Default | Production |
|----------|---------|-----------|
| `innodb_buffer_pool_size` | 128 MB | 70-80% RAM |
| `innodb_log_file_size` | 48 MB | 1-4 GB |
| `innodb_flush_log_at_trx_commit` | 1 | 1 (don't change) |
| `innodb_flush_method` | fsync | O_DIRECT |
| `innodb_io_capacity` | 200 | 1000-10000 (SSD) |
| `max_connections` | 151 | 200-500 + pooler |
| `tmp_table_size` | 16 MB | 64-256 MB |
| `binlog_format` | ROW | ROW |
| `sync_binlog` | 1 | 1 |

| Tool | Use |
|------|-----|
| Slow query log | Catch slow |
| pt-query-digest | Aggregate |
| pt-online-schema-change | Online ALTER |
| gh-ost | Online ALTER (no triggers) |
| sys.* views | Diagnostic |
| PMM / Datadog | Monitoring |
| EXPLAIN ANALYZE | Plan + actual timing |

| Diagnostic Q | View |
|-------------|------|
| Top slow queries | `sys.statements_with_runtimes_in_95th_percentile` |
| Unused indexes | `sys.schema_unused_indexes` |
| Full table scans | `sys.statements_with_full_table_scans` |
| Lock waits | `sys.innodb_lock_waits` |
| Connection summary | `sys.processlist` |

---

## Practice

1. Calculate ideal `innodb_buffer_pool_size` for your server. Set it.
2. Enable slow query log; identify top 3 slow queries; fix.
3. Use `sys.schema_unused_indexes` to find drop candidates.
4. Run `pt-online-schema-change` for ALTER on staging table.
5. Set up Prometheus + mysqld_exporter + Grafana dashboard.
6. Calculate your buffer pool hit rate from `SHOW GLOBAL STATUS`.

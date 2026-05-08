# Replication

## Status: Not Started

---

## Table of Contents

1. [Why Replication?](#why-replication)
2. [Replication Architecture](#replication-architecture)
3. [Binary Log (binlog)](#binary-log-binlog)
4. [Binlog Formats](#binlog-formats)
5. [Setup — Source-Replica](#setup--source-replica)
6. [GTID-Based Replication](#gtid-based-replication)
7. [Async vs Semi-Sync vs Sync](#async-vs-semi-sync-vs-sync)
8. [Replication Lag](#replication-lag)
9. [Read Scaling with Replicas](#read-scaling-with-replicas)
10. [Failover & Topology Tools](#failover--topology-tools)
11. [Group Replication & InnoDB Cluster](#group-replication--innodb-cluster)
12. [Common Output Traps](#common-output-traps)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## Why Replication?

| Use case | Benefit |
|----------|---------|
| **Read scaling** | Reads to replicas; primary handles writes |
| **HA / Failover** | Replica becomes new primary if primary dies |
| **Backups without locking** | Take backup from replica, not prod primary |
| **Analytics / reporting** | Heavy queries on replica don't impact prod |
| **Geo-distribution** | Local read replica per region |
| **Zero-downtime upgrades** | Replica upgraded → swap primary |

---

## Replication Architecture

### Primary-Replica (renamed from Master-Slave in 8.0.22+)

```
        Writes
           ↓
       ┌─────────┐
       │ Primary │ ──────binlog─────► Replica 1 (read-only)
       └─────────┘                ──► Replica 2
                                  ──► Replica 3
                                       ↑
                                  Reads load-balanced
```

### How it works

1. Primary writes → updates binlog
2. Replica's **IO thread** connects, fetches binlog events → writes to **relay log**
3. Replica's **SQL thread** reads relay log → applies to data files

```
Primary                       Replica
─────                         ───────
binlog → IO thread receives → relay log → SQL thread applies → data
```

---

## Binary Log (binlog)

> "**Append-only log of all data changes** on primary."

### Enable

```ini
[mysqld]
server_id = 1
log_bin = /var/lib/mysql/binlog
binlog_format = ROW
expire_logs_days = 7
max_binlog_size = 100M
sync_binlog = 1                    # fsync per commit (durability)
```

### View binlogs

```sql
SHOW BINARY LOGS;
SHOW MASTER STATUS;          -- current binlog file + position
SHOW BINLOG EVENTS IN 'binlog.000001' LIMIT 10;
```

### Binlog purpose

1. **Replication** — replicas read this
2. **Point-in-time recovery (PITR)** — replay binlog from backup point
3. **Audit / change tracking**

### `mysqlbinlog` tool

```bash
mysqlbinlog binlog.000001 > queries.sql
mysqlbinlog --start-datetime='2024-05-08 14:00:00' \
            --stop-datetime='2024-05-08 15:00:00' \
            binlog.* > recovery.sql
```

→ Cross-ref: `11-Backup-and-Recovery.md`.

---

## Binlog Formats

### STATEMENT

Logs **SQL statements** as-is.

```sql
-- binlog event
INSERT INTO orders (user_id, total) VALUES (100, 500);
```

| Pros | Cons |
|------|------|
| Compact | Non-deterministic functions (`NOW()`, `UUID()`, `RAND()`) → different result on replica |
| Easy to read | Triggers may differ |

→ **Risky for non-deterministic ops.**

### ROW (recommended)

Logs **actual row changes** before/after images.

```
-- binlog event (binary)
Row update: orders, before={user_id:100, total:500}, after={user_id:100, total:600}
```

| Pros | Cons |
|------|------|
| **Safe for any operation** | Larger binlog (especially for bulk DML) |
| Deterministic | Less human-readable |

→ **Default in MySQL 8+**.

### MIXED

- Default STATEMENT
- Auto-switches to ROW for non-deterministic statements

→ Hybrid; reasonable middle ground.

### Set format

```ini
binlog_format = ROW       # recommended
```

```sql
SET GLOBAL binlog_format = 'ROW';
```

→ ROW is the modern default; **prefer it**.

### Binlog row image

`binlog_row_image`:

| Value | What logged |
|-------|-------------|
| `FULL` | All columns before + after |
| `MINIMAL` | PK + changed columns |
| `NOBLOB` | All except BLOB / TEXT unless changed |

→ `MINIMAL` saves space.

---

## Setup — Source-Replica

### Step 1 — Configure primary

`my.cnf`:

```ini
[mysqld]
server_id = 1
log_bin = /var/lib/mysql/binlog
binlog_format = ROW
gtid_mode = ON
enforce_gtid_consistency = ON
```

### Step 2 — Create replication user

```sql
CREATE USER 'repl'@'%' IDENTIFIED BY 'strong_password';
GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';
FLUSH PRIVILEGES;
```

### Step 3 — Take initial snapshot

#### With XtraBackup (preferred — no lock)

```bash
xtrabackup --backup --target-dir=/backup/snapshot
xtrabackup --prepare --target-dir=/backup/snapshot
# transfer to replica
```

#### With mysqldump (small DBs)

```bash
mysqldump --all-databases --master-data=2 --single-transaction > snapshot.sql
```

→ `--master-data=2` records binlog position as comment.

### Step 4 — Restore on replica + start replication

```sql
-- on replica
CHANGE REPLICATION SOURCE TO
    SOURCE_HOST='primary_host',
    SOURCE_USER='repl',
    SOURCE_PASSWORD='strong_password',
    SOURCE_AUTO_POSITION=1;       -- GTID-based; no manual position

START REPLICA;
SHOW REPLICA STATUS\G
```

→ Pre-8.0.22 syntax: `CHANGE MASTER TO`, `START SLAVE`, `SHOW SLAVE STATUS`.

### Step 5 — Verify

```sql
SHOW REPLICA STATUS\G
```

Key fields:

```
Replica_IO_Running: Yes        # IO thread fetching binlog
Replica_SQL_Running: Yes       # SQL thread applying
Seconds_Behind_Source: 0       # lag
Last_Errno: 0                  # no errors
```

---

## GTID-Based Replication

> "**Global Transaction Identifier** — each TX gets unique ID `<server_uuid>:<sequence>` across cluster."

### Why?

- **No manual binlog position** tracking on failover
- **Easier to switch replicas** between primaries
- **Crash safety** — replica knows exactly which TXs applied

### Enable

```ini
gtid_mode = ON
enforce_gtid_consistency = ON
```

### Set source on replica

```sql
CHANGE REPLICATION SOURCE TO
    SOURCE_AUTO_POSITION = 1;   -- GTID auto
```

→ vs old: `MASTER_LOG_FILE = 'binlog.000123', MASTER_LOG_POS = 4567` (manual).

### View

```sql
SHOW VARIABLES LIKE 'gtid_executed';
-- 3E11FA47-71CA-11E1-9E33-C80AA9429562:1-100
```

→ Shows TX 1-100 from server with given UUID applied.

### Failover scenario

```
Primary fails. Promote Replica2.
Old replicas: SELECT highest GTID committed.
New primary: confirm GTID set.
Re-point other replicas to new primary using GTID auto-position.
```

→ Without GTID: manual binlog position calculation = error-prone.

---

## Async vs Semi-Sync vs Sync

### Async (default)

```
Primary commits → returns to client → binlog event sent to replica later
```

| Pros | Cons |
|------|------|
| Fastest writes | Data loss if primary dies before replica receives event |
| Lowest latency | Replica may lag |

→ Most common; suitable for most apps.

### Semi-Synchronous

```
Primary commits → waits for at least 1 replica to acknowledge → returns to client
```

```ini
plugin_load = "rpl_semi_sync_master=semisync_master.so;rpl_semi_sync_slave=semisync_slave.so"
rpl_semi_sync_master_enabled = 1
rpl_semi_sync_slave_enabled = 1
rpl_semi_sync_master_timeout = 1000   # 1 sec; falls back to async if no ack
```

| Pros | Cons |
|------|------|
| Low data loss risk | Slight latency increase |
| Falls back to async if replica down | Replica lag still possible (just IO ack, not SQL apply) |

→ **Common in production** — strong middle ground.

### Synchronous

True sync = primary waits for replica **to apply** event. **Not native in MySQL** — use Galera Cluster, MySQL Group Replication for synchronous.

---

## Replication Lag

> "Time between commit on primary and apply on replica."

### Cause

- Slow SQL thread (single-threaded by default)
- Disk IO on replica
- Network bandwidth
- Long-running TX on primary
- Lock waits on replica

### Detect

```sql
SHOW REPLICA STATUS\G

Seconds_Behind_Source: 120     -- 2 minutes lag
```

### Better metric — heartbeat

```sql
-- Use pt-heartbeat or custom heartbeat table
SELECT TIMESTAMPDIFF(SECOND, ts, NOW()) AS lag FROM heartbeat;
```

→ `Seconds_Behind_Source` based on event timestamp; **misleading** if SQL thread idle.

### Reduce lag

#### 1. Multi-threaded apply

```ini
replica_parallel_workers = 16
replica_parallel_type = LOGICAL_CLOCK     -- 8+; allows parallel apply within TX boundaries
```

→ Significant speedup on replica side.

#### 2. Faster disks on replica

SSD vs HDD makes huge difference for SQL apply.

#### 3. Avoid massive TXs on primary

Split bulk DML into batches.

#### 4. Reduce binlog row image to MINIMAL

Less data to ship + apply.

---

## Read Scaling with Replicas

### Pattern

```
Application
   ├── Writes → Primary
   └── Reads  → Replica1 / Replica2 / Replica3 (LB)
```

### Implementation

#### App-level routing

```java
@Transactional(readOnly = true)
public List<Order> findOrders(...) { ... }   // → replica
```

→ Spring Boot `AbstractRoutingDataSource` (cross-ref `phase-5 / Spring Boot Interview Topics / 08-Common-Spring-Boot-Qs.md`).

#### Proxy-based — ProxySQL

```
App → ProxySQL → Routing rules:
   SELECT → replica
   UPDATE/INSERT/DELETE → primary
```

→ Transparent to app. Production-grade.

### Read-your-writes consistency

After write, immediate read may go to replica → stale data!

**Fixes:**
- Read after write goes to **primary** (sticky for short window)
- Wait for replica catch-up using GTID:

```sql
SELECT WAIT_FOR_EXECUTED_GTID_SET('<gtid>', 5);
```

---

## Failover & Topology Tools

### Manual failover

1. Stop writes on primary (read-only)
2. Wait replicas to catch up (`Seconds_Behind_Source = 0`)
3. Promote replica → `STOP REPLICA; RESET REPLICA ALL; SET GLOBAL read_only = 0;`
4. Re-point other replicas to new primary
5. Update DNS / app config

→ **Many edge cases**. Don't do this manually for prod.

### Automated tools

| Tool | What |
|------|------|
| **Orchestrator** | GitHub-developed; topology graph, auto-failover |
| **MHA** | Master High Availability (older, popular) |
| **ProxySQL** + custom | Routing + auto-failover via scripts |
| **MySQL Router** | Native (works with InnoDB Cluster) |
| **MySQL Shell AdminAPI** | Manage InnoDB Cluster |

### MySQL InnoDB Cluster (managed)

- Group Replication + MySQL Router + MySQL Shell
- Native auto-failover
- Multi-primary or single-primary
- Easier setup for HA

---

## Group Replication & InnoDB Cluster

> "MySQL's **native multi-primary or single-primary HA** with synchronous-like guarantees."

### Architecture

```
   3 / 5 / 7 nodes (odd for quorum)
      ┌──────┐
      │ N1   │ ←── communication ──→ N2
      └──────┘                        │
         ↑                            │
         └─────────── Group ──────────┘
                       │
                       ↓
                   Consensus (Paxos-like)
                   for commits
```

### Modes

| | Single-Primary | Multi-Primary |
|--|----------------|----------------|
| Writes | Only primary | Any node |
| Conflict | None (single writer) | Detected + abort |
| Common use | HA with failover | Active-active scenarios |

### Pros

- Native HA, no external orchestrator needed
- Synchronous-like commit (majority ack required)
- Auto-failover

### Cons

- More operational complexity
- Network sensitive
- Performance overhead for consensus

→ Choose based on requirements: simple replicas → primary-replica; need HA → InnoDB Cluster.

---

## Common Output Traps

### Q1. `Seconds_Behind_Source = NULL`

→ IO or SQL thread stopped. Check `SHOW REPLICA STATUS` for errors.

### Q2. STATEMENT-format breaks on `NOW()` / `UUID()`

```sql
INSERT INTO logs (id, ts) VALUES (UUID(), NOW());
-- Replica applies different UUID + ts → diverges
```

→ Use ROW or MIXED format.

### Q3. Read-your-writes stale on replica

→ Use sticky session / GTID wait.

### Q4. Replica diverges from primary

```sql
Last_SQL_Error: ... duplicate key ...
```

→ Replica state corrupted. Re-clone from primary.

### Q5. `expire_logs_days` too low

→ Replica can't find binlog → broken replication. Set 7+ days.

---

## Pitfalls

1. **Async replication + assume consistency** — read-your-writes broken.
2. **STATEMENT format with non-deterministic functions** — silent divergence.
3. **No GTID** in modern setup — failover painful.
4. **Single SQL thread** in heavy workload — lag piles up. Enable parallel apply.
5. **Replica writes** allowed (`read_only = 0`) — diverges from primary.
6. **No monitoring** of replication lag.
7. **`expire_logs_days = 1`** + replica down for 2 days → binlog purged → re-clone.
8. **Inconsistent server_id** — replication fails.
9. **`sync_binlog = 0`** — primary crash loses binlog events not yet flushed.
10. **Schema changes during replication** — check binlog format compatibility.
11. **Long-running TX on primary** — replica catches up slowly.
12. **Replica lag dashboards** missing — invisible bottleneck.

---

## Cheat Sheet

| Component | What |
|-----------|------|
| binlog | Append log on primary |
| Relay log | Binlog copy on replica |
| IO thread | Pulls from primary |
| SQL thread | Applies relay log |

| Format | When |
|--------|------|
| STATEMENT | Compact, risky |
| ROW | Safe (default 8+) |
| MIXED | Hybrid |

| Setup checklist | |
|----------------|--|
| `server_id` unique | ✅ |
| `log_bin` enabled | ✅ |
| `binlog_format = ROW` | ✅ |
| `gtid_mode = ON` | ✅ |
| Replication user with `REPLICATION SLAVE` | ✅ |
| Initial snapshot via XtraBackup | ✅ |
| `SOURCE_AUTO_POSITION = 1` | ✅ |

| Sync mode | Use |
|-----------|-----|
| Async | Default; perf focus |
| Semi-sync | Most prod (1 ack) |
| Group Replication | HA |

| Diagnostic | Cmd |
|-----------|-----|
| Replica state | `SHOW REPLICA STATUS\G` |
| Primary state | `SHOW MASTER STATUS;` |
| Binlogs | `SHOW BINARY LOGS;` |
| GTID set | `SELECT @@GLOBAL.gtid_executed;` |
| Lag | `Seconds_Behind_Source` / pt-heartbeat |

---

## Practice

1. Set up source + 1 replica locally with Docker; verify replication.
2. Enable GTID; failover replica → primary; verify clean.
3. Test STATEMENT vs ROW format with non-deterministic INSERT.
4. Measure replication lag with `pt-heartbeat`; tune parallel apply.
5. Read-your-writes test: write on primary, immediately read replica → observe stale.
6. Implement Spring Boot read/write split with AbstractRoutingDataSource.

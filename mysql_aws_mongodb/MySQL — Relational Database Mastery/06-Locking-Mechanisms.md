# Locking Mechanisms

## Status: Not Started

---

## Table of Contents

1. [Why Locks?](#why-locks)
2. [Lock Granularity](#lock-granularity)
3. [Shared vs Exclusive Locks](#shared-vs-exclusive-locks)
4. [Intent Locks (IS / IX)](#intent-locks-is--ix)
5. [Row Locks](#row-locks)
6. [Gap Locks](#gap-locks)
7. [Next-Key Locks](#next-key-locks)
8. [Insert Intention Locks](#insert-intention-locks)
9. [Auto-Increment Locks](#auto-increment-locks)
10. [Reading Lock Info](#reading-lock-info)
11. [Lock Wait Timeout & Deadlock Settings](#lock-wait-timeout--deadlock-settings)
12. [Common Output Traps](#common-output-traps)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## Why Locks?

> "Concurrency control — multiple TXs can't corrupt each other's writes."

InnoDB provides:
- **Multi-granularity** locks (row, gap, table, intent)
- **Pessimistic** by default (locks acquired before access)
- **Lock-free reads** via MVCC (cross-ref `05-Transactions-and-ACID.md`)

→ Reads usually don't block. Writes do.

---

## Lock Granularity

```
Server-level         Server lock (rare — global mutex)
   ↓
Table-level          MyISAM uses; InnoDB also (intent locks)
   ↓
Row-level            InnoDB primary
   ↓
Gap-level            InnoDB unique — locks gaps between rows
```

→ **Finer granularity = more concurrency** (more parallel access) but more lock structures.

---

## Shared vs Exclusive Locks

| Lock | Symbol | Allows |
|------|--------|--------|
| **Shared (S)** | S | Other Ss; blocks Xs |
| **Exclusive (X)** | X | Blocks Ss + Xs |

### Compatibility matrix

| Existing → / New ↓ | S | X |
|------|---|---|
| **S** | ✅ | ❌ |
| **X** | ❌ | ❌ |

### Acquire shared lock

```sql
SELECT * FROM accounts WHERE id = 1 FOR SHARE;     -- 8.0+
-- or legacy syntax
SELECT * FROM accounts WHERE id = 1 LOCK IN SHARE MODE;
```

→ Other readers OK; writers blocked.

### Acquire exclusive lock

```sql
SELECT * FROM accounts WHERE id = 1 FOR UPDATE;
```

→ Others blocked from reading and writing this row.

### When to use

- **`FOR UPDATE`** — about to update row + want to prevent concurrent reads
- **`FOR SHARE`** — read with consistency guarantee, don't want row updated until you commit

---

## Intent Locks (IS / IX)

> "**Table-level markers** indicating intention to acquire row locks."

| Type | Meaning |
|------|---------|
| **IS** Intent Shared | Plan to acquire S row locks |
| **IX** Intent Exclusive | Plan to acquire X row locks |

### Why?

When acquiring `LOCK TABLE foo WRITE` (table-level X), need to know if any rows in `foo` already locked.

→ Without intent locks, MySQL would scan all rows. **Intent lock = O(1) check**.

### Acquired automatically

```sql
SELECT * FROM accounts WHERE id = 1 FOR UPDATE;
```

→ Internally: `IX` on table `accounts` + `X` on row id=1.

### Compatibility

```
        | IS | IX | S  | X
--------|----|----|-------|---
   IS   | ✅ | ✅ | ✅ | ❌
   IX   | ✅ | ✅ | ❌ | ❌
   S    | ✅ | ❌ | ✅ | ❌
   X    | ❌ | ❌ | ❌ | ❌
```

→ IS/IX compatible with each other. They block table-level S/X.

---

## Row Locks

> "**InnoDB default** — locks individual rows in PK / secondary index."

### Locked via index

```sql
SELECT * FROM users WHERE id = 100 FOR UPDATE;   -- locks PK index entry for id=100
```

### Without index = lock all rows scanned

```sql
SELECT * FROM users WHERE name = 'Sachin' FOR UPDATE;
-- If name not indexed → full table scan → locks ALL rows seen!
```

→ **Critical**: Always index `WHERE` column in `FOR UPDATE`.

### Lock mode by query

| Query | Lock |
|-------|------|
| `SELECT` (autocommit) | None (MVCC snapshot) |
| `SELECT FOR SHARE` | S row |
| `SELECT FOR UPDATE` | X row |
| `UPDATE` | X row |
| `DELETE` | X row |
| `INSERT` | X row (new) + insert intention gap lock |

---

## Gap Locks

> "Locks **range between rows** — prevents phantom inserts."

### Example

Index entries: 10, 20, 30 (gaps: -∞..10, 10..20, 20..30, 30..+∞)

```sql
SELECT * FROM t WHERE id > 15 AND id < 25 FOR UPDATE;
-- Locks gap 10..20 and 20..30
-- Concurrent INSERT id=18 would block!
```

### Why?

In **REPEATABLE READ**, prevents new rows appearing between subsequent reads in same TX.

→ This is how InnoDB prevents phantom reads (vs SQL standard RR).

### Gap-only lock

```sql
-- Range covers gap but not row 20:
SELECT * FROM t WHERE id > 10 AND id < 20 FOR UPDATE;
-- Locks gap 10..20
```

### Disabling gap locks

#### Use READ COMMITTED

In RC, gap locks **disabled** (mostly). Less locking, more phantom potential.

#### `innodb_locks_unsafe_for_binlog` (legacy)

Deprecated; don't use.

---

## Next-Key Locks

> "**Row lock + gap lock combined** = next-key lock."

### Default in REPEATABLE READ

```sql
-- Index has 10, 20, 30
SELECT * FROM t WHERE id BETWEEN 15 AND 25 FOR UPDATE;
-- Next-key lock on:
--   row 20 + gap 10..20 + gap 20..30
-- Concurrent INSERT id=18 blocked
-- Concurrent INSERT id=22 blocked
```

### Visualization

```
Range [15, 25]
            ↓
Index:    10 - - 20 - - 30
              [██████]              ← gap + row + gap locked
```

### Diagnostic

```sql
SELECT * FROM performance_schema.data_locks WHERE OBJECT_NAME = 't';
-- LOCK_TYPE: RECORD
-- LOCK_MODE: X,GAP / X / X,REC_NOT_GAP
```

| Lock mode | What |
|-----------|------|
| `X,GAP` | Gap lock only |
| `X` | Next-key lock (row + gap) |
| `X,REC_NOT_GAP` | Row lock only (no gap) |

---

## Insert Intention Locks

> "Special gap lock — multiple INSERTs can intend to insert in same gap **without blocking each other**."

### Compatibility

```
Existing gap lock (X,GAP) + new INSERT_INTENTION → blocks
Existing INSERT_INTENTION + new INSERT_INTENTION → compatible
```

→ Allows high INSERT concurrency in same gap region.

### Effect

```
Without insert intention: INSERTs serialize on gap → 1 INSERT/sec
With insert intention: many parallel INSERTs in same gap → high throughput
```

→ Internal optimization; mostly transparent.

---

## Auto-Increment Locks

> "InnoDB controls AUTO_INCREMENT generation; can lock to ensure no gaps."

### `innodb_autoinc_lock_mode`

| Mode | Behaviour |
|------|-----------|
| 0 (legacy) | Table-level lock for entire INSERT |
| 1 (consecutive) | Lock for INSERT, releases at end. **No gaps within statement**. Default pre-8 |
| **2 (interleaved)** | No lock; multiple INSERTs assign IDs concurrently. **May have gaps**. Default 8.0+ |

### Implication

In MySQL 8.0+ default (mode 2), AUTO_INCREMENT IDs may have gaps:

```
INSERT INTO orders ...;     -- id = 100
INSERT INTO orders ...;     -- id = 102 (101 used by another concurrent INSERT)
```

→ **Don't rely on consecutive IDs.** Use `created_at` for ordering if needed.

### Bulk inserts

Mode 2 = best for bulk; mode 1 = predictable but slower.

---

## Reading Lock Info

### Performance Schema (8+)

```sql
-- All current locks
SELECT * FROM performance_schema.data_locks;

-- Lock waits
SELECT * FROM performance_schema.data_lock_waits;

-- Joined view: who waits for whom
SELECT
    waiting_trx_id,
    waiting_thread,
    waiting_query,
    blocking_trx_id,
    blocking_thread,
    blocking_query
FROM sys.innodb_lock_waits;
```

### Pre-8 view

```sql
SELECT * FROM information_schema.INNODB_LOCKS;       -- removed in 8.0
SELECT * FROM information_schema.INNODB_LOCK_WAITS;
```

→ Use `performance_schema.data_locks` in MySQL 8+.

### Find blocking transactions

```sql
-- sys schema (built-in views)
SELECT * FROM sys.innodb_lock_waits;
```

### Show transactions

```sql
SELECT * FROM information_schema.INNODB_TRX;
```

→ Active transactions, what locks they hold, when started.

---

## Lock Wait Timeout & Deadlock Settings

### `innodb_lock_wait_timeout`

> "How long to wait for a lock before erroring."

```sql
SHOW VARIABLES LIKE 'innodb_lock_wait_timeout';   -- default 50 sec
```

```
ERROR 1205 (HY000): Lock wait timeout exceeded; try restarting transaction
```

### Set per session

```sql
SET innodb_lock_wait_timeout = 5;     -- 5 sec for shorter timeouts in app
```

→ For web app, **5-10 sec recommended** — fast user feedback vs default 50 sec.

### `innodb_deadlock_detect`

```sql
SHOW VARIABLES LIKE 'innodb_deadlock_detect';   -- default ON
```

When ON: InnoDB cycles through lock graph; finds cycles + picks victim.

When OFF: relies on `innodb_lock_wait_timeout`. Useful for very high concurrency where detection itself is bottleneck.

→ Default ON for most apps.

### `innodb_rollback_on_timeout`

```sql
SHOW VARIABLES LIKE 'innodb_rollback_on_timeout';   -- default OFF
```

When OFF: timeout error rolls back **only the failing statement** (TX continues).
When ON: rolls back entire transaction.

→ App should `ROLLBACK` after any lock error to be safe.

---

## Common Output Traps

### Q1. `FOR UPDATE` without index — locks all rows

```sql
UPDATE users SET name = 'X' WHERE country = 'IN';   -- no index on country
-- Locks ALL scanned rows, possibly all of `users` table
```

→ Add index on `country`.

### Q2. RR + gap lock surprise

```sql
-- Session A
BEGIN; SELECT * FROM accounts WHERE id BETWEEN 100 AND 200 FOR UPDATE;

-- Session B
INSERT INTO accounts (id, ...) VALUES (150, ...);
-- BLOCKED until A commits — even though row 150 doesn't exist
```

→ Use RC if gap locks too restrictive.

### Q3. Long transactions block others

Open TX in psql / DataGrip + step away → table effectively locked. Close ungranted sessions.

### Q4. Deadlock invisible to app

```java
try {
    statement.execute();   // deadlock here
} catch (SQLException e) {
    // forgot to retry
}
```

→ Always handle / retry deadlock errors.

### Q5. Mixed isolation levels in app

```java
connection1.setTransactionIsolation(REPEATABLE_READ);
connection2.setTransactionIsolation(READ_COMMITTED);
```

→ Inconsistent locking behaviour; debug nightmare.

---

## Pitfalls

1. **`FOR UPDATE` without index** → table-wide locks.
2. **Long-running transactions** → undo bloat + lock pile-up.
3. **No deadlock retry** in app.
4. **Default 50s lock timeout** in web app — user UI hangs.
5. **`COUNT(*)` in `FOR UPDATE`** — locks every row.
6. **Mixing RR + bulk INSERT** — gap locks slow batch.
7. **AUTO_INCREMENT gap dependency** — IDs not consecutive in mode 2.
8. **Implicit commits by DDL** — silent transaction termination.
9. **Holding locks during external API call** — pool exhaustion.
10. **Catching `Lock wait timeout` and continuing** without rollback.

---

## Cheat Sheet

| Lock Type | Granularity | Use |
|-----------|-------------|-----|
| Shared (S) | Row | Concurrent reads with guarantee |
| Exclusive (X) | Row | Update / delete |
| Intent S (IS) | Table | Marker for row S locks |
| Intent X (IX) | Table | Marker for row X locks |
| Gap | Index range | Phantom prevention (RR) |
| Next-Key | Index range + record | Default in RR |
| Insert Intention | Gap | High-concurrency INSERTs |

| Setting | Default | Recommend |
|---------|---------|-----------|
| `innodb_lock_wait_timeout` | 50s | 5-10s for web |
| `innodb_deadlock_detect` | ON | ON |
| `innodb_rollback_on_timeout` | OFF | Set + handle in app |
| `innodb_autoinc_lock_mode` | 2 | 2 (8+ default) |

| Diagnostic | Cmd |
|-----------|-----|
| Active TXs | `INFORMATION_SCHEMA.INNODB_TRX` |
| Lock waits | `sys.innodb_lock_waits` |
| Current locks | `performance_schema.data_locks` |
| Engine status | `SHOW ENGINE INNODB STATUS` |

| Best Practice | |
|---------------|--|
| Index `WHERE` in `FOR UPDATE` | Always |
| Short transactions | Always |
| Consistent lock order | Always |
| Retry on deadlock | Always |
| Avoid HTTP/external in TX | Always |

---

## Practice

1. Reproduce gap lock blocking INSERT in RR; same query in RC — observe difference.
2. `SELECT FOR UPDATE` without index → check locked rows count.
3. Force deadlock between 2 sessions; observe `SHOW ENGINE INNODB STATUS`.
4. Use `sys.innodb_lock_waits` to diagnose blocked queries.
5. Tune `innodb_lock_wait_timeout` to 5s; observe app behaviour change.
6. Java retry-on-deadlock helper — implement with exponential backoff + max attempts.

# Transactions & ACID

## Status: Not Started

---

## Table of Contents

1. [ACID Refresher](#acid-refresher)
2. [Transaction Basics](#transaction-basics)
3. [autocommit](#autocommit)
4. [SAVEPOINT](#savepoint)
5. [Isolation Levels Overview](#isolation-levels-overview)
6. [READ UNCOMMITTED](#read-uncommitted)
7. [READ COMMITTED](#read-committed)
8. [REPEATABLE READ (Default)](#repeatable-read-default)
9. [SERIALIZABLE](#serializable)
10. [MVCC Internals](#mvcc-internals)
11. [Deadlock Auto-Detection](#deadlock-auto-detection)
12. [`SHOW ENGINE INNODB STATUS`](#show-engine-innodb-status)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## ACID Refresher

| Letter | Meaning |
|--------|---------|
| **A** Atomicity | All-or-nothing |
| **C** Consistency | Constraints maintained |
| **I** Isolation | Concurrent TX as if serial |
| **D** Durability | Committed data persists despite crash |

### How InnoDB delivers each

| Property | Mechanism |
|----------|-----------|
| Atomicity | Undo log (rollback on fail) |
| Consistency | Constraint enforcement + transactions |
| Isolation | MVCC + locking |
| Durability | Redo log + fsync (`innodb_flush_log_at_trx_commit=1`) |

→ Cross-ref: `01-MySQL-Architecture-and-Internals.md`.

---

## Transaction Basics

### Start, commit, rollback

```sql
START TRANSACTION;       -- or BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 1;
UPDATE accounts SET balance = balance + 100 WHERE id = 2;
COMMIT;
-- or ROLLBACK;
```

### What happens

```
START TRANSACTION
  ↓
[changes accumulate in undo log]
[locks acquired]
  ↓
COMMIT → flush redo log → release locks → done
```

### Transaction = unit of work

If any statement fails (constraint, deadlock, etc.) → roll back entire transaction.

```sql
BEGIN;
INSERT INTO orders (user_id, total) VALUES (100, 500);
INSERT INTO order_items (order_id, product_id, qty) VALUES (LAST_INSERT_ID(), 1, 2);
-- If 2nd fails, can ROLLBACK to undo 1st
COMMIT;
```

---

## autocommit

> "By default, **every statement is its own transaction**."

```sql
SHOW VARIABLES LIKE 'autocommit';   -- ON (1)
```

### Implications

```sql
UPDATE users SET name = 'X' WHERE id = 1;
-- Already committed; no rollback possible
```

### Disable for batch work

```sql
SET autocommit = 0;
UPDATE ...;
UPDATE ...;
COMMIT;        -- ya ROLLBACK explicitly
```

### Or wrap in BEGIN

```sql
BEGIN;
... statements ...
COMMIT;
```

→ `BEGIN` overrides autocommit for the duration.

### Java / JDBC

```java
connection.setAutoCommit(false);
try {
    statement1.execute();
    statement2.execute();
    connection.commit();
} catch (SQLException e) {
    connection.rollback();
}
```

→ Spring `@Transactional` handles automatically.

---

## SAVEPOINT

> "Mid-transaction checkpoint — partial rollback."

```sql
BEGIN;

INSERT INTO orders (user_id, total) VALUES (100, 500);
SAVEPOINT after_order;

INSERT INTO order_items (order_id, product_id, qty) VALUES (LAST_INSERT_ID(), 999, 1);
-- Whoops, product 999 doesn't exist
ROLLBACK TO SAVEPOINT after_order;

INSERT INTO order_items (order_id, product_id, qty) VALUES (LAST_INSERT_ID(), 1, 2);
-- This works
COMMIT;
```

### Use cases

- Complex multi-step business flow
- Try-catch in stored procedures
- Long-running ETL with checkpoint recovery

### Release

```sql
RELEASE SAVEPOINT after_order;   -- explicit cleanup
```

---

## Isolation Levels Overview

> "**Trade-off: stronger isolation = lower concurrency.**"

### 4 levels (SQL standard)

| Level | Dirty Read | Non-Repeatable Read | Phantom Read |
|-------|-----------|--------------------|----|
| READ UNCOMMITTED | ✅ Possible | ✅ | ✅ |
| READ COMMITTED | ❌ | ✅ | ✅ |
| **REPEATABLE READ** (MySQL default) | ❌ | ❌ | ⚠️ (InnoDB prevents via gap locks) |
| SERIALIZABLE | ❌ | ❌ | ❌ |

### Set per session

```sql
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
```

### Set per transaction

```sql
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
BEGIN;
... ;
COMMIT;
```

### Set globally

```sql
SET GLOBAL TRANSACTION ISOLATION LEVEL READ COMMITTED;
-- in my.cnf:
transaction_isolation = READ-COMMITTED
```

---

## READ UNCOMMITTED

> "Reads see **uncommitted changes** of other transactions."

### Anomaly: Dirty Read

```
TX1: BEGIN; UPDATE accounts SET balance = 0 WHERE id = 1;  (uncommitted)
TX2: SELECT balance FROM accounts WHERE id = 1;  →  0    (DIRTY!)
TX1: ROLLBACK;
TX2 saw 0 that never officially existed.
```

### When?

- **Almost never.**
- Maybe analytics / reporting where stale OK.

→ Don't use unless explicit reason.

---

## READ COMMITTED

> "Each statement sees latest committed data — but **same row may differ between two reads in same TX**."

### Anomaly: Non-Repeatable Read

```
TX1: BEGIN; SELECT balance FROM accounts WHERE id = 1; → 1000
TX2: BEGIN; UPDATE accounts SET balance = 1500 WHERE id = 1; COMMIT;
TX1: SELECT balance FROM accounts WHERE id = 1; → 1500 (CHANGED!)
```

### Why use?

- **PostgreSQL default**
- Higher concurrency (less locking)
- Better for OLTP at scale (Oracle, SQL Server)

→ Many production MySQL systems run RC because of less lock contention.

```sql
SET GLOBAL transaction_isolation = 'READ-COMMITTED';
```

### Trade-off

| Pro | Con |
|-----|-----|
| Less locking → more concurrency | Same TX may see changing row data |

---

## REPEATABLE READ (Default)

> "Within a transaction, **same query returns same rows always**. MySQL default."

### Snapshot

First read in TX → fix snapshot view (via MVCC). Subsequent reads use that snapshot.

### Example

```
TX1: BEGIN; SELECT balance FROM accounts WHERE id = 1; → 1000
TX2: BEGIN; UPDATE accounts SET balance = 1500 WHERE id = 1; COMMIT;
TX1: SELECT balance FROM accounts WHERE id = 1; → 1000 (snapshot, ignores TX2's commit)
TX1: COMMIT;
```

### Phantom reads — InnoDB-specific behaviour

Standard SQL: REPEATABLE READ allows **phantom reads** (new rows appearing in range query).

InnoDB: **gap locks + next-key locks prevent phantoms** in most cases.

```
TX1: BEGIN; SELECT * FROM users WHERE age > 30 FOR UPDATE; → 5 rows
TX2: INSERT INTO users (age) VALUES (35);   -- BLOCKED by gap lock
TX1: SELECT * FROM users WHERE age > 30 FOR UPDATE; → still 5 rows
```

### When?

- **Default — most applications**
- Strong isolation, manageable contention
- Beware: long-running TX in RR holds snapshot → undo bloat

---

## SERIALIZABLE

> "Highest isolation. Reads behave as if `SELECT ... FOR SHARE`."

### Behavior

- All reads acquire shared locks
- Writes acquire exclusive
- Effectively: transactions execute as if **one at a time**

### Performance

- Lots of lock contention
- Deadlocks more frequent
- **Slowest level**

### When?

- Banking / accounting where money loss is unacceptable
- Single critical transaction wrapped (not all)

```sql
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
BEGIN;
SELECT balance FROM accounts WHERE id = 1;
... critical logic ...
UPDATE accounts SET balance = ? WHERE id = 1;
COMMIT;
```

---

## MVCC Internals

> "**Multi-Version Concurrency Control** — readers don't block writers, writers don't block readers."

### Mechanics

Each row has hidden columns:

```
DB_TRX_ID         transaction ID that last modified the row
DB_ROLL_PTR       pointer to undo log (previous version)
```

### Read view

When a TX starts (RR) or each query (RC):

```
read view = {
    creator_trx_id: 100,
    active_trx_ids: [101, 105, 110],
    min_trx_id: 101,
    max_trx_id: 111
}
```

### Visibility rule

For each row:
- If `DB_TRX_ID < min_trx_id` → version was committed before TX started → **visible**
- If `DB_TRX_ID > max_trx_id` → after TX → **not visible** (use undo to find earlier version)
- If `DB_TRX_ID == creator_trx_id` → own write → visible
- If `DB_TRX_ID in active_trx_ids` → uncommitted → **not visible**

### Result

- Reads use undo log to reconstruct past versions → **non-blocking**
- Writers continue updating current version
- Scales beautifully

### Cost

- Undo log space (for active TXs)
- Long-running TX = old undo retained = bloat

---

## Deadlock Auto-Detection

> "InnoDB has built-in deadlock detector. **Picks victim, rolls it back**."

### Deadlock example

```
TX1: BEGIN; UPDATE accounts SET balance = balance - 100 WHERE id = 1;
TX2: BEGIN; UPDATE accounts SET balance = balance - 50 WHERE id = 2;
TX1: UPDATE accounts SET balance = balance + 100 WHERE id = 2;   -- waits for TX2
TX2: UPDATE accounts SET balance = balance + 50 WHERE id = 1;    -- waits for TX1 → DEADLOCK
```

→ InnoDB detects cycle in lock graph; **rolls back smaller TX** (less work to undo).

### Victim experience

```
ERROR 1213 (40001): Deadlock found when trying to get lock; try restarting transaction
```

### App responsibility

**Catch + retry**:

```java
@Retryable(retryFor = SQLException.class, maxAttempts = 3, backoff = @Backoff(delay = 50))
@Transactional
public void transfer(...) { ... }
```

### Prevention

1. **Consistent lock ordering** — always lock account 1 before account 2 (sort IDs)
2. **Short transactions** — hold locks briefly
3. **Lower isolation** if possible
4. **Indexes on lookup columns** — locks specific rows, not gaps
5. **`SELECT ... FOR UPDATE`** explicitly when about to update

### Disable detection (advanced)

```ini
innodb_deadlock_detect = OFF
```

→ Falls back to `innodb_lock_wait_timeout` (default 50s). Useful for high-concurrency where detection itself is bottleneck. Rare.

---

## `SHOW ENGINE INNODB STATUS`

> "Comprehensive InnoDB diagnostic dump."

```sql
SHOW ENGINE INNODB STATUS\G
```

### Sections

```
TRANSACTIONS
============
... active TXs, lock waits, deadlock history (most recent only)

LATEST DETECTED DEADLOCK
========================
... full deadlock info

BUFFER POOL AND MEMORY
======================
... pool hit rate, dirty pages

ROW OPERATIONS
==============
... insert/update/delete counters

LOG
===
... redo log status
```

### Recent deadlock

```
LATEST DETECTED DEADLOCK
------------------------
2024-05-08 14:30:22 0x7f8a...
*** (1) TRANSACTION:
TRANSACTION 12345, ACTIVE 5 sec starting index read
mysql tables in use 1, locked 1
LOCK WAIT 4 lock struct(s), heap size 1136, 3 row lock(s)
MySQL thread id 100, query id 5000 ...
UPDATE accounts SET balance = balance + 100 WHERE id = 2

*** (2) TRANSACTION:
... similar ...
UPDATE accounts SET balance = balance + 50 WHERE id = 1
```

→ Shows **both TXs**, what they were doing, what they waited on. Critical for diagnosis.

### Lock waits

If section says "LOCK WAIT" repeatedly → contention.

→ Cross-ref: `06-Locking-Mechanisms.md`.

---

## Pitfalls

1. **Forgetting `BEGIN`** — autocommit makes each statement standalone.
2. **Long-running TX** in RR — undo bloat, replication lag, others wait.
3. **Mixing isolation levels** between sessions — race conditions.
4. **`SELECT FOR UPDATE` without index** — locks all rows scanned (often whole table).
5. **No deadlock retry** in app — random failures user-visible.
6. **Catching deadlock + retrying with same logic** — infinite loop.
7. **`autocommit = 0`** + forgetting COMMIT — data lost.
8. **Try-catch swallowing rollback** — partial commits.
9. **Holding locks across user input** — UI freeze hangs DB.
10. **Implicit commits** by DDL (`CREATE`, `ALTER`, `DROP`) inside TX — abort transaction.

---

## Cheat Sheet

| Isolation | Dirty | Non-Repeat | Phantom | Use |
|-----------|-------|-----------|---------|-----|
| READ UNCOMMITTED | ✅ | ✅ | ✅ | Almost never |
| READ COMMITTED | ❌ | ✅ | ✅ | High concurrency OLTP |
| REPEATABLE READ | ❌ | ❌ | InnoDB ❌ | MySQL default |
| SERIALIZABLE | ❌ | ❌ | ❌ | Critical sections only |

| Action | Effect |
|--------|--------|
| `BEGIN` | Start TX (overrides autocommit) |
| `COMMIT` | Persist changes |
| `ROLLBACK` | Discard changes |
| `SAVEPOINT name` | Mid-TX checkpoint |
| `ROLLBACK TO SAVEPOINT name` | Partial rollback |

| Detection | |
|-----------|--|
| Deadlock | InnoDB auto-detects + rolls back victim |
| Lock wait | `innodb_lock_wait_timeout` (default 50s) |
| Diagnostic | `SHOW ENGINE INNODB STATUS` |

| Best practices | |
|---------------|--|
| Short TX | Always |
| Consistent lock order | Yes |
| Indexes for `WHERE` in TX | Yes |
| Retry on deadlock | Yes |
| Don't include external HTTP in TX | Avoid |

---

## Practice

1. Demonstrate dirty read in READ UNCOMMITTED with two sessions.
2. Show non-repeatable read in READ COMMITTED.
3. Verify InnoDB prevents phantoms in RR via gap locks.
4. Force a deadlock; capture `SHOW ENGINE INNODB STATUS` output.
5. Implement Java retry-on-deadlock pattern.
6. Use SAVEPOINT in a multi-step procedure.

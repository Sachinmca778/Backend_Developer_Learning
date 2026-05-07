# Database Transactions

## Status: Not Started

---

## Table of Contents

1. [Transaction Kya Hai?](#transaction-kya-hai)
2. [ACID Properties](#acid-properties)
3. [Isolation Levels](#isolation-levels)
4. [Concurrency Phenomena](#concurrency-phenomena)
5. [Explicit Locking](#explicit-locking)
6. [Deadlocks](#deadlocks)
7. [Best Practices](#best-practices)

---

## Transaction Kya Hai?

**Matlab:** Ek logical unit of work — multiple operations jo "all or nothing" basis par execute hote hain.

### Classic Example: Bank Transfer

```sql
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 'A';
UPDATE accounts SET balance = balance + 100 WHERE id = 'B';
COMMIT;
```

Agar second UPDATE fail ho gaya → first UPDATE bhi rollback. **Money kabhi vanish nahi hota.**

### Commands

```sql
BEGIN;          -- ya START TRANSACTION;
-- queries
COMMIT;         -- changes save (durable)
-- OR
ROLLBACK;       -- changes discard
```

### Savepoints (Nested-like)

```sql
BEGIN;
INSERT INTO orders ...;
SAVEPOINT sp1;
INSERT INTO order_items ...;  -- agar fail, sirf yahan se rollback
ROLLBACK TO sp1;              -- order intact, items roll back
COMMIT;
```

---

## ACID Properties

### A — Atomicity

**Matlab:** Transaction "atomic" hai — pura ya kuch nahi. Beech mein crash → automatic rollback.

```sql
BEGIN;
DEBIT account A;     -- ✅ done
CREDIT account B;    -- ❌ crash here!
-- Restart: DEBIT bhi rollback ho jaata hai
```

**How achieved?** Write-Ahead Log (WAL).

### C — Consistency

**Matlab:** Transaction ke baad DB **valid state** mein rehni chahiye — constraints, FKs, triggers sab honor.

```sql
-- Constraint: balance >= 0
BEGIN;
UPDATE accounts SET balance = balance - 1000 WHERE id = 'A';
-- balance = -500 → constraint violation → ROLLBACK
```

⚠️ Application-level invariants application ki responsibility hai (DB constraints define karte ho jab possible ho).

### I — Isolation

**Matlab:** Concurrent transactions ek-doosre ko **isolated** lagne chahiye — jaise akele chal rahi ho.

→ Detail mein next section: **Isolation Levels**.

### D — Durability

**Matlab:** Once committed, data **permanent** — power failure / crash ke baad bhi survive.

**How?** WAL — commit return karne se pehle log disk pe `fsync`.

```
1. Transaction writes go to WAL (sequential log)
2. WAL fsync to disk
3. COMMIT returns to client
4. Later: changes applied to data files (checkpoint)
```

---

## Isolation Levels

SQL Standard defines 4 levels — har level kuch concurrency anomalies prevent karta hai (cost of performance).

| Level | Dirty Read | Non-Repeatable Read | Phantom Read | Serialization Anomaly |
|-------|------------|---------------------|--------------|----------------------|
| Read Uncommitted | ✅ Possible | ✅ | ✅ | ✅ |
| Read Committed | ❌ | ✅ | ✅ | ✅ |
| Repeatable Read | ❌ | ❌ | ⚠️* | ✅ |
| Serializable | ❌ | ❌ | ❌ | ❌ |

*In Postgres, Repeatable Read also prevents phantoms (snapshot-based)*

### Setting Isolation Level

```sql
-- Per transaction
BEGIN ISOLATION LEVEL REPEATABLE READ;
-- ...
COMMIT;

-- Per session
SET SESSION CHARACTERISTICS AS TRANSACTION ISOLATION LEVEL SERIALIZABLE;

-- Postgres default: READ COMMITTED
```

---

### 1. Read Uncommitted

**Matlab:** Other transactions ke uncommitted changes bhi visible. **Dirty reads** allowed.

🚨 Postgres mein actually behaves like `Read Committed` (no real Read Uncommitted). MySQL mein support hai.

### 2. Read Committed (Postgres Default)

**Matlab:** Sirf committed data dikhta hai. Lekin **same query do baar chalao** → different result possible.

```sql
-- Transaction T1
BEGIN;
SELECT balance FROM accounts WHERE id = 'A';   -- 1000

-- Meanwhile T2 commits: UPDATE balance = 500

SELECT balance FROM accounts WHERE id = 'A';   -- 500 (changed!)
COMMIT;
```

→ "Non-repeatable read"

### 3. Repeatable Read

**Matlab:** Transaction ke shuru mein ek **snapshot** lete hain — pure transaction wahi snapshot dekhta hai.

```sql
BEGIN ISOLATION LEVEL REPEATABLE READ;
SELECT balance FROM accounts WHERE id = 'A';   -- 1000

-- T2 commits: UPDATE balance = 500

SELECT balance FROM accounts WHERE id = 'A';   -- 1000 (still!)
COMMIT;
```

✅ Same value across transaction (snapshot isolation).

⚠️ **Serialization failure** ho sakti hai — concurrent updates par error:
```
ERROR: could not serialize access due to concurrent update
```
→ Application ko retry karna hota hai.

### 4. Serializable

**Matlab:** Transactions ke result aisa lagna chahiye jaise ek-ek karke (serial) chali ho. Strongest isolation.

Postgres mein **SSI (Serializable Snapshot Isolation)** use hota hai — snapshot + dependency tracking.

```sql
BEGIN ISOLATION LEVEL SERIALIZABLE;
-- Heavy work
COMMIT;
-- May fail with serialization error → retry
```

✅ Safest, but most expensive (more conflict failures).

---

## Concurrency Phenomena

### 1. Dirty Read

**Definition:** Uncommitted changes read karna.

```
T1: BEGIN; UPDATE balance = 500;  (not committed yet)
T2: SELECT balance;                → reads 500 (DIRTY!)
T1: ROLLBACK;
                                   T2 saw value that never existed!
```

Postgres mein nahi hota (Read Committed minimum).

### 2. Non-Repeatable Read

**Definition:** Ek hi transaction mein same row do baar read kiya, different value mili.

```
T1: BEGIN; SELECT balance FROM accounts WHERE id = 1;   -- 1000
T2: BEGIN; UPDATE balance = 500 WHERE id = 1; COMMIT;
T1: SELECT balance FROM accounts WHERE id = 1;          -- 500
T1: COMMIT;
```

Read Committed mein possible, Repeatable Read mein nahi.

### 3. Phantom Read

**Definition:** Ek hi range query do baar chalayi, naye rows mil gaye (ya ud gaye).

```
T1: BEGIN; SELECT * FROM orders WHERE amount > 1000;    -- 5 rows
T2: BEGIN; INSERT INTO orders (amount) VALUES (2000); COMMIT;
T1: SELECT * FROM orders WHERE amount > 1000;           -- 6 rows!
T1: COMMIT;
```

Standard mein Repeatable Read mein possible. Postgres mein snapshot ki wajah se nahi.

### 4. Serialization Anomaly

**Definition:** Concurrent transactions ka result kisi serial order se match nahi karta.

**Classic example: Write Skew**

```
Constraint: at least one doctor on call

Initial: Alice (on_call=true), Bob (on_call=true)

T1 (Alice goes off): 
  COUNT on_call = 2 → can go off
  UPDATE Alice on_call = false

T2 (Bob goes off, parallel):
  COUNT on_call = 2 → can go off
  UPDATE Bob on_call = false

Both COMMIT → Now NO doctor on call! 🚨
```

Sirf **Serializable** isolation se prevent.

---

## Explicit Locking

Default isolation enough nahi → manually lock karo.

### `SELECT FOR UPDATE`

**Matlab:** Read karte time row ko **exclusive lock** lagao. Aur transactions tab tak wait karenge jab tak hum commit/rollback nahi karte.

```sql
BEGIN;
SELECT balance FROM accounts WHERE id = 'A' FOR UPDATE;
-- Lock acquired

UPDATE accounts SET balance = balance - 100 WHERE id = 'A';
COMMIT;
-- Lock released
```

**Use case:** Read-then-write pattern (compute new value based on current).

### `SELECT FOR SHARE`

**Matlab:** **Shared lock** — other transactions read kar sakte hain (FOR SHARE bhi le sakte hain), but UPDATE/DELETE block.

```sql
SELECT * FROM orders WHERE id = 1 FOR SHARE;
-- Others can read, but can't modify
```

### `NOWAIT`

**Matlab:** Lock available na ho toh wait mat karo, error throw karo.

```sql
SELECT * FROM accounts WHERE id = 'A' FOR UPDATE NOWAIT;
-- ERROR: could not obtain lock on row
```

✅ User-facing operations (no hanging UI).

### `SKIP LOCKED`

**Matlab:** Locked rows skip karo — non-blocked rows return karo.

```sql
SELECT * FROM jobs 
WHERE status = 'pending' 
ORDER BY created_at 
LIMIT 1 
FOR UPDATE SKIP LOCKED;
```

✅ **Job queue pattern!** Multiple workers parallel pull karte hain bina conflict ke.

### Locking Modes (Cheat Sheet)

| Lock | Blocks | Allows |
|------|--------|--------|
| `FOR UPDATE` | UPDATE, DELETE, FOR UPDATE, FOR SHARE | Plain SELECT |
| `FOR NO KEY UPDATE` | Like FOR UPDATE but weaker (allows FK insert) | |
| `FOR SHARE` | UPDATE, DELETE | SELECT, FOR SHARE |
| `FOR KEY SHARE` | Weakest — only blocks key updates | |

### Lock Granularity

```sql
-- Row-level (ideal)
SELECT * FROM accounts WHERE id = 1 FOR UPDATE;

-- Table-level (heavy)
LOCK TABLE accounts IN EXCLUSIVE MODE;

-- Advisory (covered in PostgreSQL Specific)
SELECT pg_advisory_xact_lock(123);
```

### Real-World Pattern: Atomic Counter

```sql
-- ❌ Race condition
SELECT count FROM stats WHERE id = 1;       -- 100
-- 100 + 1 = 101 in app
UPDATE stats SET count = 101 WHERE id = 1;
-- Concurrent transaction also did this → 101 (lost +1!)

-- ✅ Option 1: SELECT FOR UPDATE
BEGIN;
SELECT count FROM stats WHERE id = 1 FOR UPDATE;
-- compute
UPDATE stats SET count = count + 1 WHERE id = 1;
COMMIT;

-- ✅ Option 2: Atomic UPDATE (no SELECT needed!)
UPDATE stats SET count = count + 1 WHERE id = 1;
```

---

## Deadlocks

**Matlab:** Do transactions ek doosre ka lock wait kar rahi hain → infinite wait → DB ek ko kill karta hai.

### Classic Deadlock

```
T1: LOCK row A
T2: LOCK row B
T1: try LOCK row B  → wait
T2: try LOCK row A  → wait

Postgres detects → kills one with:
ERROR: deadlock detected
```

### Prevention

**Rule 1:** Hamesha same order mein locks acquire karo.

```sql
-- ❌ Inconsistent order
T1: LOCK A, then B
T2: LOCK B, then A

-- ✅ Consistent order (e.g., by id ASC)
T1: LOCK A, then B
T2: LOCK A, then B
```

**Rule 2:** Transactions short rakho.

**Rule 3:** Application-level retry on deadlock error.

```javascript
async function withRetry(fn, maxAttempts = 3) {
    for (let i = 0; i < maxAttempts; i++) {
        try {
            return await fn();
        } catch (err) {
            if (err.code === '40P01') continue; // deadlock_detected
            if (err.code === '40001') continue; // serialization_failure
            throw err;
        }
    }
    throw new Error('Max retries exceeded');
}
```

### Detect Deadlocks

```sql
-- Currently waiting locks
SELECT pid, locktype, mode, granted, query
FROM pg_locks l
JOIN pg_stat_activity a ON l.pid = a.pid
WHERE NOT granted;

-- Deadlock log
-- postgresql.conf: log_lock_waits = on
```

---

## Best Practices

### 1. Keep Transactions Short

Long transactions → long locks → poor concurrency + bloat.

```sql
-- ❌ BAD
BEGIN;
SELECT * FROM users WHERE id = 1 FOR UPDATE;
-- Wait for external API response (5 seconds!)
UPDATE ...;
COMMIT;

-- ✅ GOOD
-- Do API call OUTSIDE transaction
const result = await externalAPI();
BEGIN;
UPDATE users SET data = $result WHERE id = 1;
COMMIT;
```

### 2. Use Right Isolation Level

| Workload | Recommended |
|----------|-------------|
| Most CRUD | Read Committed (default) |
| Reports / consistency-critical | Repeatable Read |
| Multi-step business logic with invariants | Serializable |

### 3. Handle Retry-able Errors

```
40001 → serialization_failure
40P01 → deadlock_detected
```

Both → safe to retry from app layer.

### 4. Avoid Long-Running Transactions

- Vacuum can't clean tuples newer than oldest open transaction
- Even 5-minute transaction → table bloat

### 5. Don't Mix Transactional with Non-Transactional Side Effects

```sql
-- ❌ DANGEROUS
BEGIN;
INSERT INTO orders ...;
-- Send email via app
-- ROLLBACK happens
-- Email already sent! 🚨

-- ✅ GOOD: Outbox pattern
BEGIN;
INSERT INTO orders ...;
INSERT INTO outbox (event) VALUES ('order_created');
COMMIT;
-- Background worker reads outbox, sends email
```

### 6. Use `SKIP LOCKED` for Queues

```sql
-- Worker
BEGIN;
SELECT * FROM jobs WHERE status = 'pending' 
ORDER BY priority DESC, created_at 
LIMIT 1 FOR UPDATE SKIP LOCKED;

-- Process
UPDATE jobs SET status = 'done' WHERE id = ?;
COMMIT;
```

### 7. Atomic UPDATEs Over Read-Modify-Write

```sql
-- ❌ Two-step
SELECT counter FROM x WHERE id = 1;
UPDATE x SET counter = ? WHERE id = 1;

-- ✅ One step (no race possible)
UPDATE x SET counter = counter + 1 WHERE id = 1;
```

---

## Summary Cheat Sheet

| Concept | Quick Note |
|---------|-----------|
| **Atomicity** | All or nothing |
| **Consistency** | Constraints honored |
| **Isolation** | Concurrent → as if serial |
| **Durability** | Committed = permanent |
| **Read Committed** | Postgres default; sees committed data |
| **Repeatable Read** | Snapshot isolation; no NRR |
| **Serializable** | Strongest; conflict failures possible |
| **Dirty Read** | See uncommitted (Postgres prevents) |
| **Non-Repeatable Read** | Same row → different value |
| **Phantom Read** | Same range → different rows |
| **Write Skew** | Constraint violated by parallel txns |
| **FOR UPDATE** | Exclusive row lock |
| **FOR SHARE** | Shared row lock |
| **NOWAIT** | Fail fast if locked |
| **SKIP LOCKED** | Skip locked rows (queues!) |
| **Deadlock** | Always lock in same order |

---

## Practice

1. Two psql sessions: simulate a non-repeatable read in Read Committed; fix with Repeatable Read.
2. Implement a money transfer with `SELECT FOR UPDATE` — test concurrent transfers.
3. Build a job queue using `FOR UPDATE SKIP LOCKED`.
4. Trigger a deadlock between two sessions; observe Postgres killing one.
5. Implement write-skew scenario, fix with Serializable isolation.
6. Add app-level retry for `40001` and `40P01` errors.

# MySQL — Relational Database Mastery - Top 10 Interview Questions & Answers

> Covering: MySQL Architecture, InnoDB Internals, Data Types, Indexing, EXPLAIN, Transactions, Locks, Replication, Partitioning, Performance Tuning, Backup, and Security

---

## Q1: MySQL architecture ka high-level flow kya hota hai?

**Answer:**

MySQL ek client-server database hai. Application JDBC/driver ke through MySQL server se connect karti hai, query SQL layer se pass hoti hai, aur actual data storage engine handle karta hai.

### Request Flow:

```text
Application
   ↓ JDBC / MySQL Connector
MySQL Server
   ↓ Connection handling
Parser → Optimizer → Executor
   ↓
Storage Engine API
   ↓
InnoDB / MyISAM / Memory
   ↓
Data files + Logs
```

### Important Layers:

| Layer | Responsibility |
|-------|----------------|
| Connection layer | Client sessions, auth, thread handling |
| SQL layer | Parsing, optimization, execution plan |
| Storage engine | Actual read/write, indexes, transactions |
| Logs | Binlog, redo log, undo log |

### InnoDB vs MyISAM:

| Feature | InnoDB | MyISAM |
|---------|--------|--------|
| Transactions | Yes | No |
| Row-level locks | Yes | No, table locks |
| Foreign keys | Yes | No |
| Crash recovery | Strong | Weak |
| Production default | Yes | Legacy |

**Important:**

Modern MySQL production systems almost always use **InnoDB** because it supports ACID transactions, MVCC, row-level locking, crash recovery, and foreign keys.

**Short Interview Answer:**

MySQL has a SQL layer (connection, parser, optimizer, executor) and a storage engine layer. InnoDB is the default production engine because it provides transactions, row-level locking, MVCC, redo/undo logs, and crash recovery.

---

## Q2: InnoDB internals mein buffer pool, redo log, undo log kya hote hain?

**Answer:**

InnoDB performance aur reliability ke core components:

| Component | Purpose |
|-----------|---------|
| **Buffer Pool** | Data + index pages ka memory cache |
| **Redo Log** | Crash recovery ke liye WAL (Write-Ahead Log) |
| **Undo Log** | Rollback + MVCC old versions |
| **Doublewrite Buffer** | Torn page protection |
| **Change Buffer** | Secondary index writes ko defer/cache |

### Buffer Pool:

```text
Query reads page
   ↓
If page in buffer pool → memory read (fast)
Else disk se load → buffer pool mein cache
```

`innodb_buffer_pool_size` MySQL tuning ka sabse impactful variable hota hai.

### Redo Log:

```text
Transaction changes data page
   ↓
Redo log mein durable record
   ↓
Later dirty page disk par flush
```

Crash ke baad redo log replay karke committed changes recover hote hain.

### Undo Log:

- Transaction rollback ke liye old values
- MVCC consistent reads ke liye previous row versions

**Important:**

Redo log durability deta hai; undo log rollback + MVCC deta hai. Buffer pool performance deta hai. InnoDB ko samajhna production MySQL debugging ka base hai.

---

## Q3: MySQL data types choose karte waqt kya rules follow karte ho?

**Answer:**

Data type choice performance, storage, indexing, and correctness affect karti hai.

### Integer Types:

| Type | Size | Use |
|------|------|-----|
| TINYINT | 1 byte | flags/status |
| INT | 4 bytes | common IDs/counts |
| BIGINT | 8 bytes | very large IDs |

Rule: **smallest type choose karo jo future growth handle kare**.

### DECIMAL vs FLOAT/DOUBLE:

| Type | Use |
|------|-----|
| DECIMAL | Money, exact values |
| FLOAT/DOUBLE | Approx scientific measurements |

```sql
price DECIMAL(10,2)  -- money ke liye correct
```

### VARCHAR vs CHAR:

| Type | Use |
|------|-----|
| CHAR | Fixed length values (country code, hash) |
| VARCHAR | Variable text (name, email) |

### DATETIME vs TIMESTAMP:

| Type | Behavior |
|------|----------|
| DATETIME | Plain date-time, timezone conversion nahi |
| TIMESTAMP | UTC store + session timezone conversion |

**Important:**

Money ke liye FLOAT/DOUBLE mat use karo. `DECIMAL` exact hota hai. Index columns ko unnecessarily large mat banao, warna index size + memory usage badh jaata hai.

**Short Interview Answer:**

Choose smallest correct type: `DECIMAL` for money, `VARCHAR` for variable text, `CHAR` for fixed values, `TIMESTAMP` when timezone conversion matters, `DATETIME` for plain timestamps. Data type directly affects index size and query performance.

---

## Q4: MySQL indexing kaise kaam karta hai? Composite index aur leftmost prefix rule kya hai?

**Answer:**

MySQL InnoDB default B+Tree indexes use karta hai. Index lookup table scan avoid karta hai.

### B+Tree Index:

```text
Root → Internal nodes → Leaf pages
Leaf pages sorted order mein keys store karte hain
```

Best for:
- Equality (`=`)
- Range (`>`, `<`, `BETWEEN`)
- `ORDER BY`
- `GROUP BY`

### Composite Index:

```sql
CREATE INDEX idx_user_status_created
ON orders(user_id, status, created_at);
```

### Leftmost Prefix Rule:

Ye index useful hoga:

```sql
WHERE user_id = ?
WHERE user_id = ? AND status = ?
WHERE user_id = ? AND status = ? AND created_at > ?
```

Ye index fully useful nahi hoga:

```sql
WHERE status = ?        -- user_id missing
WHERE created_at > ?    -- leftmost columns missing
```

### Covering Index:

```sql
CREATE INDEX idx_cover ON orders(user_id, status, total);

SELECT status, total FROM orders WHERE user_id = 10;
```

Agar query ke required columns index mein hi mil gaye, table lookup avoid hota hai. `EXPLAIN` mein `Using index` dikhta hai.

**Important:**

Index read fast karta hai but write slow karta hai because every insert/update/delete index update karta hai. Random indexes mat banao; slow queries + access pattern ke basis par banao.

---

## Q5: EXPLAIN kaise read karte ho? Slow query optimize kaise karte ho?

**Answer:**

`EXPLAIN` query execution plan dikhata hai. MySQL optimizer kaise query run karega, ye samajhne ke liye use hota hai.

```sql
EXPLAIN ANALYZE
SELECT * FROM orders
WHERE user_id = 10
ORDER BY created_at DESC
LIMIT 20;
```

### Important EXPLAIN Columns:

| Column | Meaning |
|--------|---------|
| `type` | Access method |
| `key` | Chosen index |
| `rows` | Estimated rows scanned |
| `filtered` | Percentage rows after filter |
| `Extra` | Extra operations |

### `type` ranking:

| Type | Quality |
|------|---------|
| `const` / `system` | Best |
| `eq_ref` | Excellent |
| `ref` | Good |
| `range` | Good for ranges |
| `index` | Full index scan |
| `ALL` | Worst, full table scan |

### Bad flags:

| Extra | Meaning |
|-------|---------|
| `Using filesort` | Sort index se nahi ho raha |
| `Using temporary` | Temp table ban rahi |
| `Using where` | Server-side filter |

### Optimization Steps:

1. Slow query log enable karo
2. `EXPLAIN ANALYZE` run karo
3. `type=ALL`, high `rows`, `Using filesort` identify karo
4. Correct composite/covering index add karo
5. Query rewrite karo if needed

**Short Interview Answer:**

I read `EXPLAIN` by checking access type, chosen key, rows scanned, filtered percentage, and Extra flags. `ALL`, high rows, `Using filesort`, and `Using temporary` are red flags. Then I add/reorder indexes based on WHERE, JOIN, ORDER BY patterns.

---

## Q6: MySQL transactions, ACID, isolation levels aur MVCC kaise kaam karte hain?

**Answer:**

Transaction multiple SQL statements ko one logical unit banata hai.

```sql
START TRANSACTION;
UPDATE accounts SET balance = balance - 100 WHERE id = 1;
UPDATE accounts SET balance = balance + 100 WHERE id = 2;
COMMIT;
```

### ACID:

| Property | Meaning |
|----------|---------|
| Atomicity | All or nothing |
| Consistency | Valid state maintain |
| Isolation | Concurrent transactions interfere na kare |
| Durability | Commit ke baad data safe |

### Isolation Levels:

| Level | Allows |
|-------|--------|
| READ UNCOMMITTED | Dirty reads |
| READ COMMITTED | Non-repeatable reads |
| REPEATABLE READ | MySQL default, stable snapshot |
| SERIALIZABLE | Strictest, lowest concurrency |

### MVCC:

InnoDB old row versions undo log mein rakhta hai. Read query ek **read view** ke through consistent snapshot dekhti hai.

```text
Transaction A updates row
Transaction B old snapshot read kar sakta hai
Undo log old version provide karta hai
```

**Important:**

MySQL default isolation level **REPEATABLE READ** hai. InnoDB MVCC consistent reads deta hai without blocking writers in many cases.

---

## Q7: MySQL locks: row locks, gap locks, next-key locks kya hote hain?

**Answer:**

InnoDB row-level locking support karta hai, but isolation level aur query pattern ke hisaab se different locks lag sakte hain.

### Lock Types:

| Lock | Meaning |
|------|---------|
| Shared lock | Read lock, multiple readers allowed |
| Exclusive lock | Write lock, others block |
| Intent lock | Table-level marker for row locks |
| Gap lock | Index records ke beech gap lock |
| Next-key lock | Row lock + gap lock |

### Explicit Locking:

```sql
SELECT * FROM orders WHERE id = 10 FOR UPDATE; -- exclusive

SELECT * FROM orders WHERE id = 10 FOR SHARE;  -- shared
```

### Gap/Next-key Lock:

Range queries phantom prevent karne ke liye gaps lock ho sakte hain:

```sql
SELECT * FROM orders
WHERE amount BETWEEN 100 AND 200
FOR UPDATE;
```

Ye matching rows ke saath surrounding index gaps bhi lock kar sakta hai.

### Deadlock Debug:

```sql
SHOW ENGINE INNODB STATUS;
```

**Important:**

Indexes locking ko affect karte hain. Agar proper index nahi hai, MySQL zyada rows scan/lock kar sakta hai, जिससे lock contention aur deadlocks badh sakte hain.

---

## Q8: MySQL replication kaise kaam karti hai? Binlog, GTID aur lag kya hai?

**Answer:**

Replication source DB ke changes replicas tak copy karta hai.

```text
Source DB
  ↓ writes binlog
Replica IO thread reads binlog
  ↓ relay log
Replica SQL thread applies changes
```

### Binlog Formats:

| Format | Pros | Cons |
|--------|------|------|
| STATEMENT | Compact | Non-deterministic risk |
| ROW | Safe, exact row changes | Larger |
| MIXED | Hybrid | Complexity |

### GTID:

GTID = Global Transaction ID. Failover easy karta hai because replica position manually binlog file/offset se track nahi karni padti.

### Read Replicas:

- Read scaling
- Analytics/reporting
- Backup offload

### Replication Lag:

```sql
SHOW REPLICA STATUS\G
-- Seconds_Behind_Source
```

Lag ke causes:
- Replica slow hardware
- Long transactions
- Heavy write burst
- Single-thread apply bottleneck

**Short Interview Answer:**

MySQL replication source binlog se replicas par changes apply karti hai. ROW format safer hota hai. GTID failover simplify karta hai. Replication lag stale reads cause kar sakta hai, so critical read-after-write flows leader par route karne chahiye.

---

## Q9: Partitioning vs sharding kya difference hai? MySQL partitioning kab useful hai?

**Answer:**

| Concept | Meaning |
|---------|---------|
| Partitioning | Same MySQL server ke andar table split |
| Sharding | Multiple DB servers mein data split |

### MySQL Partition Types:

| Type | Example use |
|------|-------------|
| RANGE | Date/month/year based logs |
| LIST | Region/status categories |
| HASH | Even distribution |
| KEY | MySQL internal hash |

### Example:

```sql
CREATE TABLE orders (
    id BIGINT,
    created_at DATE,
    amount DECIMAL(10,2)
)
PARTITION BY RANGE (YEAR(created_at)) (
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION pmax VALUES LESS THAN MAXVALUE
);
```

### Benefits:

- Partition pruning
- Faster archival (`DROP PARTITION`)
- Manage large time-series tables

### Limitations:

- All unique indexes must include partition key
- Foreign key limitations
- Too many partitions hurt performance

**Important:**

Partitioning horizontal scale across machines nahi deta. Agar single server CPU/IO/storage limit hit kar raha hai, sharding/replication needed ho sakta hai.

---

## Q10: MySQL performance tuning, backup aur security ke production best practices kya hain?

**Answer:**

### Performance Tuning:

| Setting | Rule |
|---------|------|
| `innodb_buffer_pool_size` | Dedicated DB server par 70-80% RAM |
| `innodb_log_file_size` | Larger for write-heavy workloads |
| `max_connections` | Blindly high mat karo; pool use karo |
| Slow query log | Enable and review |

### Backup:

| Type | Tool | Use |
|------|------|-----|
| Logical | `mysqldump`, `mydumper` | Portable, slower |
| Physical | Percona XtraBackup | Hot backup, faster |
| PITR | Full backup + binlogs | Point-in-time recovery |

**Rule:** Untested backup = no backup. Restore drills mandatory.

### Security:

- Least privilege users
- `caching_sha2_password` (MySQL 8 default)
- TLS for client connections
- Parameterized queries to prevent SQL injection
- Strict `sql_mode`
- Audit logs for sensitive systems

```sql
GRANT SELECT, INSERT, UPDATE ON appdb.* TO 'app_user'@'%';
-- Avoid SUPER, FILE, ALL PRIVILEGES for app users
```

**Short Interview Answer:**

For production MySQL, tune buffer pool first, monitor slow queries, use connection pooling, take regular physical/logical backups with restore testing, enable PITR via binlogs, enforce least privilege, TLS, strict SQL mode, and parameterized queries.


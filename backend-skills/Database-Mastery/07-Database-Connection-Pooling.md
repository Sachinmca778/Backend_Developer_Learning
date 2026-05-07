# Database Connection Pooling

## Status: Not Started

---

## Table of Contents

1. [Connection Pooling Kya Hai?](#connection-pooling-kya-hai)
2. [Connection Creation Expensive Kyun Hai?](#connection-creation-expensive-kyun-hai)
3. [How a Pool Works](#how-a-pool-works)
4. [HikariCP (Java)](#hikaricp-java)
5. [Pool Sizing](#pool-sizing)
6. [Monitoring Pool Metrics](#monitoring-pool-metrics)
7. [Connection Leak Detection](#connection-leak-detection)
8. [PgBouncer (External Pooler)](#pgbouncer-external-pooler)
9. [Best Practices](#best-practices)

---

## Connection Pooling Kya Hai?

**Matlab:** Application aur database ke beech mein ek "pool" of pre-made connections rakhna. Har request ke liye new connection create karne ke bajaye, pool se ek borrow karo, use karo, wapas pool mein chod do.

### Without Pool (Bad)

```
Request 1: Create connection → query → close (cost: ~50ms each!)
Request 2: Create connection → query → close
Request 3: Create connection → query → close
...
```

🚨 1000 req/s × 50ms overhead = bottleneck.

### With Pool (Good)

```
App start: Pre-create 10 connections
Request 1: Borrow → query → return
Request 2: Borrow → query → return
Request 3: Borrow → query → return
```

✅ Connection creation cost saved per request.

---

## Connection Creation Expensive Kyun Hai?

Har naya database connection involve karta hai:

1. **TCP handshake** (3-way) — ~1 RTT
2. **TLS handshake** (if SSL) — 2 more RTTs
3. **Authentication** — username/password verify
4. **Backend process spawn** — Postgres mein har connection ek process hai!
5. **Session setup** — config, search_path, etc.

```
Total: 5-50ms per connection (depending on network + DB)
```

Plus **resource cost:**
- Postgres mein: ~10MB RAM per connection
- 10000 connections = 100GB RAM (just for connections!)

---

## How a Pool Works

```
┌─────────────┐
│ Application │
└──────┬──────┘
       │ borrow()
       ↓
┌─────────────────────┐
│  Connection Pool    │
│  ┌──┐ ┌──┐ ┌──┐    │  ← available connections
│  │C1│ │C2│ │C3│    │
│  └──┘ └──┘ └──┘    │
│  ┌──┐ ┌──┐         │  ← in-use connections
│  │C4│ │C5│         │
│  └──┘ └──┘         │
└──────────┬──────────┘
           ↓
┌─────────────┐
│  Database   │
└─────────────┘
```

**Lifecycle:**
1. App requests connection
2. Pool checks: idle connection available?
   - Yes → return it
   - No → create new (until max), or wait (until timeout)
3. App uses connection
4. App returns connection to pool
5. Pool keeps it idle (or closes if past `idleTimeout`)

---

## HikariCP (Java)

**Matlab:** Java ki sabse fast connection pool library. "Hikari" = light in Japanese.

### Maven Dependency

```xml
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.1.0</version>
</dependency>
```

### Basic Setup

```java
HikariConfig config = new HikariConfig();
config.setJdbcUrl("jdbc:postgresql://localhost:5432/mydb");
config.setUsername("myuser");
config.setPassword("mypass");
config.setMaximumPoolSize(20);

HikariDataSource ds = new HikariDataSource(config);

try (Connection conn = ds.getConnection()) {
    // use conn
} // auto-returned to pool
```

### Spring Boot (`application.yml`)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: myuser
    password: mypass
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

### Key Configuration Parameters

#### `maximumPoolSize`

**Matlab:** Pool mein maximum kitne connections rakh sakte ho.

```yaml
maximum-pool-size: 20
```

⚠️ Bahut bada → DB choke. Bahut chhota → app waits.

**Sweet spot formula** (HikariCP wiki):
```
connections = ((cores × 2) + effective_spindle_count)
```

For typical web apps: **10-30** is usually right.

#### `minimumIdle`

**Matlab:** Idle connections ka minimum count maintain — bursts handle karne ke liye.

```yaml
minimum-idle: 5
```

**Recommendation:** Match with `maximumPoolSize` for steady workloads (avoids ramp-up).

```yaml
maximum-pool-size: 20
minimum-idle: 20  # always 20 ready
```

#### `connectionTimeout`

**Matlab:** Pool se connection lene ke liye max wait time. Exceeded → exception thrown.

```yaml
connection-timeout: 30000   # 30 sec (default)
```

If hitting this regularly → pool too small or queries too slow.

#### `idleTimeout`

**Matlab:** Idle connection ko kab close karna — agar `minimumIdle` se zyada idle hain.

```yaml
idle-timeout: 600000   # 10 min (default)
```

Set 0 to keep connections forever (NOT recommended).

#### `maxLifetime`

**Matlab:** Connection ki max age — kitne time tak alive rahe.

```yaml
max-lifetime: 1800000   # 30 min (default)
```

**Why?** DB-side or network-side might silently close stale connections. Recycling them periodically prevents stale-connection errors.

⚠️ Set **less than DB's `idle_in_transaction_session_timeout`** and load balancer's connection limits.

#### `leakDetectionThreshold`

**Matlab:** Agar koi connection itne time se borrowed hai aur return nahi ki → leak warning log.

```yaml
leak-detection-threshold: 60000   # 60 sec
```

✅ Always enable in production. Helps catch missing `close()` / `try-with-resources`.

### Full Recommended Config

```yaml
hikari:
  pool-name: MainPool
  maximum-pool-size: 20
  minimum-idle: 20         # match max for steady traffic
  connection-timeout: 30000
  idle-timeout: 0          # disabled (always keep min)
  max-lifetime: 1800000
  leak-detection-threshold: 60000
  
  # Connection test
  connection-test-query: SELECT 1
  validation-timeout: 5000
  
  # Postgres specific
  data-source-properties:
    cachePrepStmts: true
    prepStmtCacheSize: 250
    prepStmtCacheSqlLimit: 2048
    useServerPrepStmts: true
    socketTimeout: 30
    tcpKeepAlive: true
```

---

## Pool Sizing

### The Counterintuitive Truth

**More connections ≠ better performance.**

### Why?

DB has limited resources (CPU cores, disk I/O). Beyond a point, more concurrent connections cause:
- Context switching overhead
- Lock contention
- Memory pressure
- Worse latency for everyone

### Real Story

> A study by HikariCP author: With 96 cores, optimal connection count was **~96-100** for max throughput. Going to 1000 connections **dropped throughput by 50%**.

### Rule of Thumb

```
Pool size = ((CPU cores × 2) + effective spindles)

For SSD: spindles = 1 (no spinning disks)
8 cores + SSD → 8*2 + 1 = ~17

For most apps: 10-30 connections is enough
```

### App-Side vs DB-Side

```
App Server 1: pool size 20
App Server 2: pool size 20
App Server 3: pool size 20
                    ↓
DB max connections: 100 (Postgres default)

Total app demand: 60 (fits!)
```

⚠️ Calculate total: `app_servers × pool_size ≤ db_max_connections`.

### Postgres `max_connections`

```sql
SHOW max_connections;        -- default 100
```

For >300 connections, **always** use PgBouncer.

---

## Monitoring Pool Metrics

### Key Metrics to Track

| Metric | What it Tells |
|--------|--------------|
| **Active connections** | Currently in use |
| **Idle connections** | Available |
| **Pending threads** | Threads waiting for a connection |
| **Connection acquisition time** | Pool latency |
| **Total connections** | Active + idle |

### HikariCP via Micrometer / JMX

```java
HikariPoolMXBean poolMXBean = ds.getHikariPoolMXBean();
poolMXBean.getActiveConnections();
poolMXBean.getIdleConnections();
poolMXBean.getThreadsAwaitingConnection();
poolMXBean.getTotalConnections();
```

### Spring Boot Actuator + Prometheus

```yaml
management:
  metrics:
    enable:
      hikaricp: true
```

Exposed metrics:
- `hikaricp_connections_active`
- `hikaricp_connections_idle`
- `hikaricp_connections_pending`
- `hikaricp_connections_acquire_seconds`

### Red Flags

🚨 **`pending > 0` consistently** → pool too small.
🚨 **`active` always = `max`** → pool saturated.
🚨 **Acquisition time > 100ms** → contention.
🚨 **Frequent connection resets** → check `maxLifetime`.

---

## Connection Leak Detection

### What is a Leak?

Connection borrowed lekin return nahi ki → pool exhaust ho jaata hai.

### Common Causes

```java
// ❌ LEAK: forgot to close
Connection conn = ds.getConnection();
PreparedStatement ps = conn.prepareStatement(...);
ps.execute();
// conn never closed!
```

```java
// ❌ LEAK: exception path doesn't close
Connection conn = ds.getConnection();
try {
    // ... if exception thrown here
    conn.close();
} catch (Exception e) {
    // conn never closed!
}
```

### Fix: try-with-resources

```java
// ✅ Always closed (even on exception)
try (Connection conn = ds.getConnection();
     PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.execute();
}
```

### Enable Leak Detection

```yaml
leak-detection-threshold: 60000   # warn after 60s
```

Logs:
```
HikariPool-1 - Apparent connection leak detected
java.lang.Exception
    at MyService.doSomething(MyService.java:45)
```

→ Stack trace points to where leaked connection was acquired!

---

## PgBouncer (External Pooler)

**Matlab:** Postgres ka external connection pooler — sits between app and DB. **Critical** for high-concurrency apps.

### Why PgBouncer?

- Postgres = process-per-connection → 10000 connections = disaster
- App-side pool = 20-30 per app server. Many app servers = many connections to DB
- PgBouncer multiplexes: 10000 client conns → 100 actual DB conns

### Architecture

```
┌────┐ ┌────┐ ┌────┐ ┌────┐
│App │ │App │ │App │ │App │      → 10000 client connections
└─┬──┘ └─┬──┘ └─┬──┘ └─┬──┘
  └──────┴──────┴──────┘
         ↓
   ┌─────────────┐
   │  PgBouncer  │              → multiplexes
   └──────┬──────┘
          ↓
   ┌─────────────┐
   │  Postgres   │              → 100 server connections
   └─────────────┘
```

### Pool Modes

#### 1. Session Pooling

**Matlab:** Connection client ki entire session ke liye allocated.

- ✅ Same as direct connection (any feature works)
- ❌ Less multiplexing benefit
- Use for compatibility, prepared statements

#### 2. Transaction Pooling (most common)

**Matlab:** Connection sirf ek transaction ke liye, transaction end → release.

- ✅ Massive multiplexing
- ❌ Some session-level features broken (prepared statements, SET)

#### 3. Statement Pooling

**Matlab:** Connection sirf ek statement ke liye.

- ✅ Maximum multiplexing
- ❌ No transactions, very limited

### Configuration (`pgbouncer.ini`)

```ini
[databases]
mydb = host=127.0.0.1 port=5432 dbname=mydb

[pgbouncer]
listen_port = 6432
listen_addr = 0.0.0.0
auth_type = md5
auth_file = /etc/pgbouncer/userlist.txt

pool_mode = transaction
max_client_conn = 10000
default_pool_size = 25
reserve_pool_size = 5
reserve_pool_timeout = 3
server_idle_timeout = 600
```

| Param | Meaning |
|-------|---------|
| `pool_mode` | session / transaction / statement |
| `max_client_conn` | Max client connections to PgBouncer |
| `default_pool_size` | DB connections per (db, user) pair |
| `reserve_pool_size` | Extra emergency connections |
| `server_idle_timeout` | Close idle DB connection after N sec |

### App-Side Config with PgBouncer

```yaml
spring.datasource.url: jdbc:postgresql://pgbouncer-host:6432/mydb
hikari:
  maximum-pool-size: 20      # app-side still useful
  # IMPORTANT in transaction pooling mode:
  data-source-properties:
    prepareThreshold: 0       # disable server-side prepared statements
```

### Transaction Pooling Caveats

⚠️ Things that break in transaction pool mode:
- Server-side prepared statements (use `prepareThreshold=0`)
- `SET` (session-wide config)
- `LISTEN/NOTIFY`
- Temporary tables outside transactions
- Cursors with hold

### When to Use PgBouncer?

✅ Web apps with > 200 connections needed
✅ Serverless / Lambda (each invocation = new connection)
✅ Microservices with many small services
✅ Connection storms (sudden traffic spikes)

❌ Small monolith (10-30 conns) — app pool enough

---

## Best Practices

### 1. Always Use a Pool

Even in scripts. Direct connections waste cycles.

### 2. Tune Carefully

Default pool sizes (10) often wrong. Measure your workload.

### 3. Monitor

Track active/idle/pending. Alert on saturation.

### 4. Two-Tier Pooling for Scale

```
App pool (HikariCP)  →  PgBouncer  →  Postgres
       ↓
   30 conns per app   →  multiplex  →  100 actual
```

### 5. Use try-with-resources / equivalent

Java: `try-with-resources`
Python: `with` block
Node.js: framework-managed (e.g., `pg.pool` callback)

### 6. Set Reasonable Timeouts

Avoid hanging forever:
- `connectionTimeout` (acquire from pool)
- `socketTimeout` (network read)
- `statement_timeout` (Postgres-level)

### 7. Validate Connections

```yaml
connection-test-query: SELECT 1
```

Or in JDBC 4+, `Connection.isValid()` is used automatically.

### 8. Pool per Database

Don't share pools across different DBs. Different `DataSource` per DB.

### 9. Reload Pool on Config Changes Carefully

Some changes require recreating the pool. Avoid frequent reconfig.

### 10. Don't Hold Connections During External Calls

```java
// ❌ BAD
try (Connection conn = ds.getConnection()) {
    String data = httpClient.get("https://...");  // 5 sec wait!
    // pool starved during this time
}

// ✅ GOOD
String data = httpClient.get("https://...");
try (Connection conn = ds.getConnection()) {
    // quick DB operation
}
```

---

## Pool Sizing Decision Flowchart

```
How many app instances?
├── 1-3 small         → HikariCP only, pool 10-20
├── 5-10 medium       → HikariCP per app, pool 15-25
├── 10+ large         → HikariCP per app + PgBouncer
└── Serverless        → PgBouncer essential
```

---

## Summary Cheat Sheet

| Problem | Solution |
|---------|----------|
| Connection creation slow | Use a pool |
| Pool too small (pending threads) | Increase `maximumPoolSize` |
| Stale connections error | Lower `maxLifetime` |
| Connection leak | Enable `leakDetectionThreshold` |
| Many app servers, DB conn limit hit | Add PgBouncer |
| Long external call | Don't hold pool conn |
| Prepared statement errors with PgBouncer | Use transaction pool + `prepareThreshold=0` |

| HikariCP Param | Default | Recommended |
|---------------|---------|-------------|
| `maximumPoolSize` | 10 | 10-30 |
| `minimumIdle` | same as max | match max for steady traffic |
| `connectionTimeout` | 30s | 30s |
| `idleTimeout` | 10min | 10min |
| `maxLifetime` | 30min | 30min |
| `leakDetectionThreshold` | 0 (off) | 60s |

---

## Practice

1. Set up HikariCP in a Spring Boot app, expose metrics via Actuator.
2. Run a load test, observe `hikaricp_connections_pending` rising — increase pool, observe.
3. Intentionally leak a connection (skip close), watch leak detection log.
4. Install PgBouncer, configure transaction pooling, route app through it.
5. Compare query latency: direct Postgres vs PgBouncer-fronted.
6. Calculate optimal pool size for a 4-core DB host with SSD.

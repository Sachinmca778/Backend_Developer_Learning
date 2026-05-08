# Connection Pool Tuning

## Status: Not Started

---

## Table of Contents

1. [Connection Pool Kya Hai?](#connection-pool-kya-hai)
2. [Why Pool? (Cost of Connections)](#why-pool-cost-of-connections)
3. [HikariCP — Spring Boot's Default](#hikaricp--spring-boots-default)
4. [Pool Sizing Formula](#pool-sizing-formula)
5. [Key HikariCP Settings](#key-hikaricp-settings)
6. [Spring Boot Configuration](#spring-boot-configuration)
7. [Multiple DataSources](#multiple-datasources)
8. [Other Pool Implementations](#other-pool-implementations)
9. [HTTP Connection Pools](#http-connection-pools)
10. [Monitoring Pool Health](#monitoring-pool-health)
11. [Troubleshooting](#troubleshooting)
12. [Common Pitfalls](#common-pitfalls)
13. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Connection Pool Kya Hai?

**Matlab:** Pre-created **reusable connections** to a resource (DB, HTTP, Redis) so app doesn't pay opening cost on every operation.

```
Without pool:
  Every request → open DB connection (50-100ms) → query → close
  
With pool:
  Pool keeps 10 connections open
  Request → borrow → query → return to pool (microseconds)
```

### Real Numbers

| Operation | Without Pool | With Pool |
|-----------|-------------|-----------|
| Get connection | 50-100ms | < 1ms |
| Connection cost (CPU + memory) | High | Amortized |
| Throughput | Low (limited by handshake) | High |

---

## Why Pool? (Cost of Connections)

### Per-Connection Cost

| Resource | Cost |
|----------|------|
| TCP handshake | 1 RTT |
| TLS handshake | 1-2 RTTs |
| DB authentication | Variable |
| Server-side memory | 1-10 MB per connection |
| Server-side process/thread | 1 per connection (PostgreSQL) |

### Database Limits

```
PostgreSQL: max_connections = 100 (default)
MySQL: max_connections = 151 (default)
SQL Server: ~32K (high)
```

→ **Each app instance × pool size** = total connections to DB.

```
10 app instances × 50-pool-size = 500 connections required!
But DB max = 100.
Result: app fails to get connections.
```

→ Pool sizing must account for total DB capacity.

---

## HikariCP — Spring Boot's Default

**Why HikariCP?**
- Fastest (proven in benchmarks)
- Smallest (200 KB jar)
- Reliable, well-maintained
- Default in Spring Boot 2+

### Auto-Configured

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    # HikariCP used by default — no extra config needed
```

→ Spring Boot auto-detects HikariCP on classpath; uses it.

---

## Pool Sizing Formula

### The Famous Formula (PostgreSQL Wiki)

```
pool_size = (core_count × 2) + effective_spindle_count
```

| Variable | Meaning |
|----------|---------|
| `core_count` | DB server CPU cores |
| `effective_spindle_count` | Independent disk spindles (1 for SSD-backed) |

### Example

```
DB server: 8 cores, SSD storage
pool_size = (8 × 2) + 1 = 17 connections per app instance
```

### Why So Small?

> "Smaller pools = more throughput. Counterintuitive but true."

**Why?**
- Each connection = a session on DB
- Too many concurrent queries → CPU + I/O contention
- DB serializes locking → larger pool ≠ more parallelism
- Context switching overhead

### Workload-Specific Adjustments

```
Read-heavy + good caching: 5-10 per app
OLTP (mixed): 10-30 per app
Heavy reporting / batch: 50-100 (with care)
```

### Multiple App Instances

```
Total DB connections = pool_size × app_instances + buffer

8 instances × 20 pool = 160 connections
DB max should be ~200 (with safety margin)
```

→ Increase DB `max_connections` if needed, OR use **PgBouncer** as connection broker.

### PgBouncer Pattern

```
[App1 pool=20] ──┐
[App2 pool=20] ──┼──▶ [PgBouncer 1000 connections] ──▶ PostgreSQL (50 actual)
[App3 pool=20] ──┘
```

→ PgBouncer multiplexes many app connections onto fewer DB connections.

---

## Key HikariCP Settings

### Essential Settings

| Property | Default | Recommended |
|----------|---------|-------------|
| `maximum-pool-size` | 10 | 10-30 (per formula) |
| `minimum-idle` | same as max | Equal to max for hot path |
| `connection-timeout` | 30000ms | 5000-30000ms |
| `idle-timeout` | 600000ms | 600000ms (10 min) |
| `max-lifetime` | 1800000ms | 1800000ms (30 min) |
| `leak-detection-threshold` | 0 (off) | 60000ms in production |
| `validation-timeout` | 5000ms | 5000ms |
| `keepalive-time` | 0 (off) | 300000ms (5 min) |

### `maximum-pool-size`

Max active connections at any time.

```yaml
spring.datasource.hikari.maximum-pool-size: 20
```

### `minimum-idle`

```yaml
spring.datasource.hikari.minimum-idle: 20
```

→ Keep `minimum-idle` = `maximum-pool-size` for stable performance (avoid creation cost on burst).

### `connection-timeout`

How long to wait when borrowing.

```yaml
spring.datasource.hikari.connection-timeout: 5000   # 5s
```

→ Throws `SQLException` if no connection in 5s. Tune to fit SLA.

### `idle-timeout`

How long an idle connection lives before being closed (above `minimum-idle`).

```yaml
spring.datasource.hikari.idle-timeout: 600000   # 10 min
```

### `max-lifetime`

Max time any connection lives, even if active.

```yaml
spring.datasource.hikari.max-lifetime: 1800000   # 30 min
```

⚠️ Set **less than** any DB-side timeout (e.g., PostgreSQL `idle_in_transaction_session_timeout`, AWS RDS Proxy timeout).

```
DB max idle = 1 hour
HikariCP max-lifetime = 30 min   ✅ rotates before DB closes
```

### `leak-detection-threshold`

Warns if connection held longer than threshold.

```yaml
spring.datasource.hikari.leak-detection-threshold: 60000   # 60s
```

→ Set this in production. Logs warning + stack trace if connection unreturned.

### `validation-timeout` & `connection-test-query`

```yaml
spring.datasource.hikari.validation-timeout: 5000
spring.datasource.hikari.connection-test-query: SELECT 1   # rarely needed; JDBC4 isValid() preferred
```

### `keepalive-time` (HikariCP 4+)

Keep connections fresh by pinging idle ones.

```yaml
spring.datasource.hikari.keepalive-time: 300000   # 5 min — useful for cloud LBs that close idle TCP
```

---

## Spring Boot Configuration

### Production Defaults

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}?useSSL=true
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    
    hikari:
      pool-name: AppPrimaryPool
      maximum-pool-size: 20
      minimum-idle: 20
      connection-timeout: 5000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
      keepalive-time: 300000
      auto-commit: false                # for JPA, often false
      
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        useServerPrepStmts: true        # MySQL
        rewriteBatchedStatements: true  # MySQL — much faster batch
```

### Java Bean (Programmatic)

```java
@Configuration
public class DataSourceConfig {
    
    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource dataSource(@Value("${spring.datasource.url}") String url,
                                  @Value("${spring.datasource.username}") String user,
                                  @Value("${spring.datasource.password}") String pass) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(pass);
        config.setMaximumPoolSize(20);
        config.setLeakDetectionThreshold(60000);
        return new HikariDataSource(config);
    }
}
```

---

## Multiple DataSources

### Read/Write Split

```yaml
spring:
  datasource:
    primary:
      url: jdbc:postgresql://master.db:5432/app
      hikari.maximum-pool-size: 20
    
    replica:
      url: jdbc:postgresql://replica.db:5432/app
      hikari.maximum-pool-size: 30   # often more for reads
```

```java
@Configuration
public class DataSourceConfig {
    
    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    @ConfigurationProperties("spring.datasource.replica")
    public DataSource replicaDataSource() {
        return DataSourceBuilder.create().build();
    }
}
```

### Routing DataSource

```java
public class RoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly()
            ? "replica" : "primary";
    }
}
```

→ `@Transactional(readOnly = true)` → routes to replica.

---

## Other Pool Implementations

### Apache DBCP2

```xml
<dependency>
  <groupId>org.apache.commons</groupId>
  <artifactId>commons-dbcp2</artifactId>
</dependency>
```

```yaml
spring.datasource.type: org.apache.commons.dbcp2.BasicDataSource
```

→ Older; HikariCP wins in benchmarks.

### Tomcat JDBC

```yaml
spring.datasource.type: org.apache.tomcat.jdbc.pool.DataSource
```

→ Decent; previous Spring Boot default before 2.0.

### C3P0

Legacy. Don't choose for new projects.

### Vibur

Niche; HikariCP-comparable performance but smaller community.

→ **HikariCP wins** for almost all use cases.

---

## HTTP Connection Pools

DB pools = main focus, but HTTP pools matter too!

### Apache HttpClient (Old `RestTemplate`)

```java
PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
cm.setMaxTotal(100);
cm.setDefaultMaxPerRoute(20);

CloseableHttpClient httpClient = HttpClients.custom()
    .setConnectionManager(cm)
    .build();

ClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
RestTemplate restTemplate = new RestTemplate(factory);
```

### OkHttp

```java
ConnectionPool pool = new ConnectionPool(20, 5, TimeUnit.MINUTES);
OkHttpClient client = new OkHttpClient.Builder()
    .connectionPool(pool)
    .build();
```

### Spring WebClient (Reactor Netty)

```java
ConnectionProvider provider = ConnectionProvider.builder("custom")
    .maxConnections(50)
    .pendingAcquireTimeout(Duration.ofSeconds(5))
    .pendingAcquireMaxCount(1000)
    .maxIdleTime(Duration.ofMinutes(5))
    .maxLifeTime(Duration.ofMinutes(30))
    .evictInBackground(Duration.ofSeconds(30))
    .build();

HttpClient httpClient = HttpClient.create(provider);
WebClient webClient = WebClient.builder()
    .clientConnector(new ReactorClientHttpConnector(httpClient))
    .build();
```

### Why HTTP Pool Tuning Matters

```
Calling 5 microservices per request, no pool:
  5 × TCP handshake + TLS = 100-300ms wasted per request
  
With pool: ~0ms reuse cost
```

→ **Always pool external HTTP calls in production.**

---

## Monitoring Pool Health

### HikariCP Metrics (Micrometer)

Spring Boot exposes automatically:

```
hikaricp.connections                    ← total
hikaricp.connections.active             ← in use
hikaricp.connections.idle               ← available
hikaricp.connections.pending            ← waiting (RED FLAG if > 0)
hikaricp.connections.usage              ← timing
hikaricp.connections.acquire            ← acquire latency
hikaricp.connections.creation           ← creation count
hikaricp.connections.timeout            ← timeouts (RED FLAG)
```

### Prometheus + Grafana

```yaml
management:
  endpoints:
    web.exposure.include: prometheus, health, metrics
  metrics:
    export:
      prometheus.enabled: true
```

### What to Alert On

| Metric | Alert |
|--------|-------|
| `hikaricp.connections.pending > 0` for 1 min | Pool exhausted |
| `hikaricp.connections.active == max` for 5 min | Pool too small or leak |
| `hikaricp.connections.timeout > 0` | Critical |
| `hikaricp.connections.creation` spike | Pool churn — check max-lifetime |

### Logging Settings

```yaml
logging:
  level:
    com.zaxxer.hikari: INFO          # connection lifecycle
    com.zaxxer.hikari.HikariConfig: DEBUG   # startup config dump
```

---

## Troubleshooting

### "Connection is not available, request timed out after Xms"

**Cause:** Pool exhausted.

**Diagnose:**
- Check `hikaricp.connections.active` = max?
- Check leaked connections (`leak-detection-threshold` warnings)
- Long-running queries holding connections?
- Threads blocked on something else (downstream API)?

**Fix:**
- Increase `maximum-pool-size` (with DB capacity check)
- Find + fix slow queries
- Cancel hung connections
- Add timeouts on downstream calls

### "Connection refused" / "Too many clients"

**Cause:** DB hit `max_connections`.

**Fix:**
- Reduce `maximum-pool-size` per app
- Reduce app instances
- Add PgBouncer / connection broker
- Increase DB `max_connections` (with care — RAM cost)

### Connections Closed by DB Mid-Use

**Cause:** DB / load balancer closes idle connections.

**Symptom:**
```
Communications link failure / Connection reset by peer
```

**Fix:**
- Reduce `max-lifetime` below DB timeout
- Enable `keepalive-time`
- Check NAT / load balancer idle timeouts

### Pool Stuck After Database Restart

**Cause:** All connections invalidated; pool slow to recover.

**Fix:**
- Recent HikariCP handles this gracefully
- Use `validation-timeout` + `connection-test-query` (rarely needed in modern setups)

### Slow Startup (Pool Initialization)

**Cause:** `minimum-idle` opening many connections at boot.

**Fix:**
- Reduce `minimum-idle` (let pool grow on demand)
- Or accept slower startup for stable steady-state

---

## Common Pitfalls

### 1. Default Pool Size (10)

Too small for 100 RPS apps; too big for 5 RPS apps. Always tune.

### 2. Pool Larger Than DB Capacity

```
DB max=100, 20 instances × 50 pool = 1000 connections needed → fails
```

→ Use formula + check total.

### 3. No `leak-detection-threshold` in Production

Connection leaks silently → eventual exhaustion. Always enable in production.

### 4. Forgetting `max-lifetime` < DB Timeout

```
DB closes idle at 5 min, HikariCP max-lifetime = 30 min
Result: stale connections in pool → errors mid-query
```

### 5. Holding Connections Across External Calls

```java
@Transactional
public void process() {
    Order o = orderRepo.findById(id);   // connection borrowed
    String s = restTemplate.getForObject(externalApi);  // SLOW — connection held
    orderRepo.save(o);                  // connection still held
}
```

→ **Don't make external calls inside `@Transactional`.** Refactor.

### 6. Pool Per Operation

```java
new HikariDataSource(config);   // ❌ creates pool per call
```

→ Pool should be **singleton** (Spring bean).

### 7. Ignoring `prepStmtCache` for MySQL

```yaml
data-source-properties:
  cachePrepStmts: true
  prepStmtCacheSize: 250
  useServerPrepStmts: true
```

→ 5-10× faster for prepared statements.

### 8. No HTTP Pooling

Default `RestTemplate` (no factory) opens new TCP per call. Always configure pooled `RequestFactory`.

### 9. Wrong DataSource Type

`spring.datasource.type` set to non-Hikari without good reason.

### 10. Using Pools for Long Async Tasks

```java
@Async
public void slowJob() {
    // Holds DB connection for 30 minutes
}
```

→ Don't borrow connection until you need it; release ASAP.

### 11. Same Pool for OLTP + Reports

Reports run for minutes → block OLTP requests. Use **separate pools**.

### 12. Pool Sized for Peak

If peak traffic = 10× average, sizing for peak wastes resources. Use HPA + smaller per-instance pool.

### 13. Forgetting Failover

When DB failover happens, HikariCP recovers; but driver / URL must be cluster-aware.

```
jdbc:postgresql://primary,replica:5432/db?targetServerType=primary
```

---

## Summary Cheat Sheet

| Setting | Recommended | Why |
|---------|-------------|-----|
| `maximum-pool-size` | 10-30 | Formula + DB capacity |
| `minimum-idle` | = max | Avoid creation overhead |
| `connection-timeout` | 5000ms | Fast fail per SLA |
| `idle-timeout` | 600000ms | 10 min |
| `max-lifetime` | 1800000ms | 30 min, < DB timeout |
| `leak-detection-threshold` | 60000ms | Catch leaks |
| `keepalive-time` | 300000ms | Beat NAT timeouts |

| Pool Type | Implementation |
|-----------|----------------|
| **Database** | HikariCP (Spring Boot default) |
| **HTTP** (RestTemplate) | Apache HttpClient + PoolingHttpClientConnectionManager |
| **HTTP** (WebClient) | Reactor Netty ConnectionProvider |
| **HTTP** (OkHttp) | OkHttp ConnectionPool |

| Formula | Use |
|---------|-----|
| `(cores × 2) + spindles` | Initial size |
| `Total = pool × instances + buffer` | DB capacity check |
| `max-lifetime < DB timeout` | Avoid stale |

| ✅ Do | ❌ Don't |
|-------|---------|
| Use HikariCP | Pick C3P0 / DBCP for new |
| Tune for actual workload | Default = 10 always |
| `leak-detection-threshold` ON | Find leaks too late |
| Pool external HTTP too | Per-call connection |
| Monitor `pending` connections | Fly blind |
| `max-lifetime` < DB timeout | Stale errors |
| Separate pools for OLTP / reports | Single pool blocks |
| PgBouncer for many instances | Naive scale |
| External calls outside `@Transactional` | Hold connections during HTTP |

---

## Practice

1. Set up Spring Boot app with PostgreSQL; observe default HikariCP settings.
2. Run JMeter / k6 load test; observe `hikaricp.connections.active` rise.
3. Trigger pool exhaustion (low pool size + long queries); verify `connection-timeout` exception.
4. Implement connection leak (don't close); enable leak detection; verify warning.
5. Apply formula to your DB; size pool; measure throughput change.
6. Set `max-lifetime` > DB idle timeout; observe stale connection errors.
7. Add `keepalive-time`; test through NAT / load balancer.
8. Configure read/write split with two DataSources + routing.
9. Enable Micrometer + Prometheus; create Grafana dashboard for pool metrics.
10. Set up PgBouncer locally; switch app to use it; observe connection multiplexing.
11. Configure pooled `RestTemplate` with Apache HttpClient; benchmark vs default.
12. Configure `WebClient` with Reactor Netty pool; tune `maxConnections`.

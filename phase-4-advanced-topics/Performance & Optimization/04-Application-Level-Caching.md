# Application-Level Caching

## Status: Not Started

---

## Table of Contents

1. [Caching Kya Hai?](#caching-kya-hai)
2. [Cache Hierarchy (L1 / L2 / CDN)](#cache-hierarchy-l1--l2--cdn)
3. [Spring Cache Abstraction](#spring-cache-abstraction)
4. [Cache Key Design](#cache-key-design)
5. [TTL vs TTI (Time-To-Live vs Time-To-Idle)](#ttl-vs-tti-time-to-live-vs-time-to-idle)
6. [Cache Population Strategies](#cache-population-strategies)
7. [Cache Invalidation](#cache-invalidation)
8. [Cache Warming](#cache-warming)
9. [Thundering Herd Problem](#thundering-herd-problem)
10. [Probabilistic Early Expiration](#probabilistic-early-expiration)
11. [Distributed Cache Locking](#distributed-cache-locking)
12. [Cache Stampede vs Hot Key Problem](#cache-stampede-vs-hot-key-problem)
13. [Negative Caching](#negative-caching)
14. [Common Pitfalls](#common-pitfalls)
15. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Caching Kya Hai?

**Matlab:** **Recently / frequently** accessed data ko fast storage mein rakhna so subsequent reads avoid expensive computation / I/O.

> "There are only two hard things in Computer Science: cache invalidation and naming things."  
> — Phil Karlton

### Wins from Caching

| Without Cache | With Cache |
|--------------|-----------|
| DB query: 50ms | Memory hit: 0.01ms (5000×) |
| External API: 200ms | Local Redis hit: 1ms (200×) |
| Computation: 500ms | Pre-computed: 1ms (500×) |

→ Properly cached app = order of magnitude faster.

### When to Cache?

✅ Read-heavy data (read:write > 10:1)
✅ Expensive computation
✅ External API calls
✅ Slow DB queries (after optimization)
✅ Static reference data

❌ Write-heavy data (cache constantly invalidated)
❌ User-specific data (low hit rate)
❌ Strongly consistent requirements
❌ Tiny lookup costs already (premature)

---

## Cache Hierarchy (L1 / L2 / CDN)

```
┌────────────────────────────────────────────────┐
│  Browser cache (HTTP cache headers)             │  ← fastest
└────────────────────────┬───────────────────────┘
                         │
┌────────────────────────▼───────────────────────┐
│  CDN (CloudFront, Cloudflare)                   │
│  - Static assets, API responses                 │
└────────────────────────┬───────────────────────┘
                         │
┌────────────────────────▼───────────────────────┐
│  Reverse proxy / Edge cache (Nginx, Varnish)    │
└────────────────────────┬───────────────────────┘
                         │
┌────────────────────────▼───────────────────────┐
│  L2 — Distributed Cache (Redis, Memcached)      │
│  - Shared across app instances                  │
│  - Survives app restart                          │
│  - Network roundtrip cost                        │
└────────────────────────┬───────────────────────┘
                         │
┌────────────────────────▼───────────────────────┐
│  L1 — Local in-app Cache (Caffeine, ConcurrentMap)│
│  - Per-app-instance                             │
│  - Microseconds access                           │
│  - Lost on restart                               │
│  - Bounded size                                  │
└────────────────────────┬───────────────────────┘
                         │
┌────────────────────────▼───────────────────────┐
│  Database (last resort)                          │
└────────────────────────────────────────────────┘
```

### Multi-Level Strategy (Common)

```
Read flow:
  L1 → L2 → DB
  (write each layer back as we go)

Write flow:
  DB → invalidate L1 + L2
```

### Caffeine (L1) + Redis (L2)

```java
@Configuration
public class CacheConfig {
    
    @Bean
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cm = new CaffeineCacheManager("users", "products");
        cm.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .recordStats());
        return cm;
    }
    
    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory cf) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        return RedisCacheManager.builder(cf).cacheDefaults(config).build();
    }
}
```

### When Each Layer Wins

| Layer | Pros | Cons |
|-------|------|------|
| **L1 (in-app)** | Fastest (μs) | Per-instance; small |
| **L2 (Redis)** | Shared; bigger | Network (1ms+) |
| **CDN** | Edge close to user | Stale; harder to invalidate |
| **DB** | Source of truth | Slowest |

---

## Spring Cache Abstraction

### Setup

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

<!-- Choose backend -->
<dependency>
  <groupId>com.github.ben-manes.caffeine</groupId>
  <artifactId>caffeine</artifactId>
</dependency>
<!-- OR -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### Enable

```java
@SpringBootApplication
@EnableCaching
public class Application { ... }
```

### Annotations

```java
@Service
public class UserService {
    
    @Cacheable(value = "users", key = "#id")
    public User getById(Long id) {
        log.info("Fetching from DB for id={}", id);
        return userRepo.findById(id).orElseThrow();
    }
    
    @CachePut(value = "users", key = "#user.id")
    public User update(User user) {
        return userRepo.save(user);
    }
    
    @CacheEvict(value = "users", key = "#id")
    public void delete(Long id) {
        userRepo.deleteById(id);
    }
    
    @CacheEvict(value = "users", allEntries = true)
    public void deleteAll() {
        userRepo.deleteAll();
    }
    
    @Caching(
        evict = {
            @CacheEvict(value = "users", key = "#user.id"),
            @CacheEvict(value = "userByEmail", key = "#user.email")
        }
    )
    public void updateEmail(User user) { ... }
}
```

### `@Cacheable` Attributes

| Attribute | Purpose |
|-----------|---------|
| `value` | Cache name(s) |
| `key` | SpEL for key |
| `condition` | SpEL — cache only if true |
| `unless` | SpEL — don't cache if true |
| `sync` | Single thread loads (true = no thundering herd) |

```java
@Cacheable(value = "users", key = "#id",
           condition = "#id > 0",
           unless = "#result == null",
           sync = true)
public User getById(Long id) { ... }
```

### Custom Key Generator

```java
@Bean
public KeyGenerator customKeyGenerator() {
    return (target, method, params) -> {
        return method.getName() + ":" + Arrays.deepToString(params);
    };
}
```

```java
@Cacheable(value = "users", keyGenerator = "customKeyGenerator")
public User getById(Long id) { ... }
```

---

## Cache Key Design

### Bad Keys

```
"user"                  ← collisions; everything in one slot
toString() of object    ← brittle; changes if any field changes
```

### Good Keys

```
"user:123"
"user:profile:123:v2"
"product:catalog:electronics:page=2:size=20"
"search:query=spring+boot:lang=en:user=456"
```

### Key Design Principles

1. **Namespace by entity** (`user:`, `product:`)
2. **Include version** if schema can change (`v2`)
3. **Include relevant params** (filters, locale)
4. **Avoid PII** in keys (logs may expose)
5. **Limit length** (some caches have key size limits)
6. **Hash if too long** (SHA-256 → fixed length)

### Versioning Pattern

```java
private static final String CACHE_VERSION = "v3";

@Cacheable(value = "users", key = "'" + CACHE_VERSION + ":' + #id")
public User getById(Long id) { ... }
```

→ Bump version → effectively invalidates all entries (old keys age out via TTL).

---

## TTL vs TTI (Time-To-Live vs Time-To-Idle)

### TTL (Time-To-Live)

**Absolute** — entry expires X seconds after **write**, regardless of usage.

```java
Caffeine.newBuilder()
    .expireAfterWrite(5, TimeUnit.MINUTES)   // TTL
    .build();
```

```yaml
spring:
  cache:
    redis:
      time-to-live: 600000   # 10 min
```

### TTI (Time-To-Idle)

**Relative** — entry expires if **not accessed** for X seconds.

```java
Caffeine.newBuilder()
    .expireAfterAccess(10, TimeUnit.MINUTES)   // TTI
    .build();
```

### Combined

```java
Caffeine.newBuilder()
    .expireAfterWrite(30, TimeUnit.MINUTES)    // hard TTL
    .expireAfterAccess(10, TimeUnit.MINUTES)   // also TTI
    .build();
```

### When to Use Each

| Pattern | Use |
|---------|-----|
| **TTL only** | Time-bound freshness (price, feed) |
| **TTI only** | Hot keys stay forever (sessions) |
| **TTL + TTI** | Belt-and-suspenders |

### Per-Entry TTL (Redis)

```java
@Bean
public RedisCacheManager cacheManager(RedisConnectionFactory cf) {
    Map<String, RedisCacheConfiguration> configs = Map.of(
        "users", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(10)),
        "products", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(1)),
        "static", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(1))
    );
    return RedisCacheManager.builder(cf).withInitialCacheConfigurations(configs).build();
}
```

---

## Cache Population Strategies

### 1. Cache-Aside (Lazy Loading) — Most Common

```
Read:
  if (in cache) return cache;
  else { result = DB; cache.put(); return result; }

Write:
  DB.update();
  cache.invalidate();   // OR cache.put(new value)
```

→ Simple. App controls cache. Stale data possible briefly.

### 2. Read-Through

```
Read:
  cache.get(key)   ← cache itself loads from DB if miss
```

→ Cache layer integrated with DB. Less app code.

### 3. Write-Through

```
Write:
  app → cache → DB (synchronous through cache)
```

→ Strong consistency; slower writes.

### 4. Write-Behind / Write-Back

```
Write:
  app → cache (immediately)
  cache → DB (async batch later)
```

→ Fast writes; risk of data loss if cache dies before DB write.

### 5. Refresh-Ahead

```
Cache pre-fetches before TTL expires (in background).
Users always hit fresh cache.
```

→ Caffeine supports via `refreshAfterWrite`:

```java
Caffeine.newBuilder()
    .refreshAfterWrite(5, TimeUnit.MINUTES)
    .expireAfterWrite(30, TimeUnit.MINUTES)
    .build(key -> loadFromDb(key));
```

### Choosing

| Need | Strategy |
|------|----------|
| Simple, common case | Cache-aside |
| Strong consistency | Write-through |
| High write throughput | Write-behind (with care) |
| Hot keys, no stalls | Refresh-ahead |

---

## Cache Invalidation

### Strategies

#### 1. TTL-Based (Simplest)

Just expire after N minutes. Acceptable staleness.

#### 2. Event-Based

```java
@Service
public class UserService {
    
    @CacheEvict(value = "users", key = "#user.id")
    public User update(User user) { ... }
}
```

Or via events:

```java
@EventListener
public void onUserUpdated(UserUpdatedEvent event) {
    cacheManager.getCache("users").evict(event.getUserId());
}
```

#### 3. Pattern-Based (Redis)

```java
redisTemplate.delete(redisTemplate.keys("user:*"));
```

⚠️ `KEYS` is O(N) blocking — bad for production. Use `SCAN`.

```java
ScanOptions options = ScanOptions.scanOptions().match("user:*").count(100).build();
Cursor<byte[]> cursor = redisTemplate.executeWithStickyConnection(
    conn -> conn.scan(options));
```

#### 4. Versioning (Soft Invalidation)

```
cache key: "user:123:v2"
Bump version on schema change → old keys expire naturally
```

#### 5. Tag-Based

Group keys by tags; invalidate by tag.

```java
// Hibernate 2nd-level cache uses regions / tags
```

### Multi-Layer Invalidation

```java
public void update(User user) {
    userRepo.save(user);
    
    // Invalidate L1 + L2
    caffeineCache.invalidate(user.getId());
    redisCache.evict(user.getId());
}
```

### Pub-Sub for Distributed Invalidation

```
Instance A updates user 123:
  - Updates DB
  - Updates own L1 cache
  - PUBLISHES to "cache.invalidate" channel: "user:123"

Instance B subscribes:
  - Receives "user:123"
  - Invalidates own L1 cache
```

→ Spring Data Redis or custom pub-sub.

---

## Cache Warming

**Matlab:** Pre-populate cache **before** users hit it — avoid cold-start misses.

### When?

- After deploy / restart (cache empty)
- Before known traffic spike (Black Friday)
- For critical low-latency endpoints

### Strategies

#### 1. Eager Load on Startup

```java
@Component
public class CacheWarmer {
    
    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        log.info("Warming cache...");
        productRepo.findTop100ByOrderByPopularityDesc()
            .forEach(p -> cacheManager.getCache("products").put(p.getId(), p));
    }
}
```

#### 2. Scheduled Refresh

```java
@Scheduled(fixedRate = 300000)   // every 5 min
public void refreshHotKeys() {
    productRepo.findHotItems().forEach(p -> 
        cacheManager.getCache("products").put(p.getId(), p));
}
```

#### 3. Replay Logs / Real Traffic

Send synthetic queries based on prior traffic patterns.

#### 4. Warm via Read-Through Pre-fetch

Use `refreshAfterWrite` (Caffeine) — auto-refreshes background.

---

## Thundering Herd Problem

**Matlab:** Cache entry expires; **many simultaneous requests** all miss + try to fetch from DB at once → DB overload.

```
Time 0:  cache valid, 1000 RPS, 100% hit, 0 DB queries
Time 5m: cache expires
Time 5m: 1000 requests miss → all hit DB simultaneously → DB melts
```

### Solutions

#### 1. Single Loader (`sync = true`)

```java
@Cacheable(value = "users", key = "#id", sync = true)
public User getById(Long id) { ... }
```

→ Spring serializes loaders for same key. Other threads wait.

```java
// Caffeine native
Caffeine.newBuilder()
    .build(key -> loadFromDb(key));   // built-in single-loader semantics
```

#### 2. Probabilistic Early Expiration (XFetch)

→ Next section.

#### 3. Distributed Lock

→ Below.

#### 4. Stale-While-Revalidate

Return slightly stale value while refreshing in background.

```java
Caffeine.newBuilder()
    .refreshAfterWrite(5, TimeUnit.MINUTES)
    .build(key -> loadFromDb(key));
```

→ At 5 min, first read returns stale + triggers async refresh.

#### 5. Queue-Based Loading

```java
public User getCached(Long id) {
    return cache.computeIfAbsent(id, k -> loadFromDb(k));
}
```

→ ConcurrentMap.computeIfAbsent is single-loader.

---

## Probabilistic Early Expiration

**Matlab:** Each read has small chance to **trigger refresh before TTL expires**, distributing reload load over time.

### Algorithm (XFetch)

```
on read:
  if expires_at - now < random(0, beta * delta):
    refresh_async()
  return cached_value
```

```java
public User get(Long id) {
    CacheEntry entry = cache.get(id);
    
    long ttlRemaining = entry.expiresAt - now();
    long delta = entry.computationTime;   // how long DB query takes
    
    if (ttlRemaining < delta * Math.log(random()) * BETA) {
        // Probabilistically expired early
        scheduleAsyncRefresh(id);
    }
    
    return entry.value;
}
```

→ Probability of refresh **increases** as TTL approaches.

### Effect

```
Without XFetch: spike at TTL expiry
With XFetch:    smooth refresh distribution
```

### Implementations

- **Caffeine** doesn't natively, but `refreshAfterWrite` + jitter approximates
- Custom logic in your loader

---

## Distributed Cache Locking

For preventing concurrent fetches across **multiple app instances**:

### Redis Lock (SETNX + Expiry)

```java
public User get(Long id) {
    User cached = cache.get(id);
    if (cached != null) return cached;
    
    String lockKey = "lock:user:" + id;
    if (redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS)) {
        try {
            User user = userRepo.findById(id).orElseThrow();
            cache.put(id, user, 5, TimeUnit.MINUTES);
            return user;
        } finally {
            redisTemplate.delete(lockKey);
        }
    } else {
        // Another instance loading; wait briefly + retry from cache
        Thread.sleep(50);
        return cache.get(id);   // hopefully populated by now
    }
}
```

### Redisson DistributedLock

```java
RLock lock = redisson.getLock("lock:user:" + id);
lock.lock(10, TimeUnit.SECONDS);
try {
    // load + cache
} finally {
    lock.unlock();
}
```

### When to Use?

✅ Expensive computation (DB query, ML inference)
✅ Many app instances + thundering herd
✅ Hot keys

❌ Tiny computations (lock cost > load cost)

---

## Cache Stampede vs Hot Key Problem

### Cache Stampede

Many keys expire near simultaneously → wave of DB queries.

**Fix:** Add jitter to TTLs.

```java
long ttl = baseTtl + ThreadLocalRandom.current().nextLong(jitter);
```

### Hot Key Problem

One key gets disproportionate traffic (90% of requests for one product).

**Fix:**
- Replicate key across multiple cache nodes (key sharding)
- L1 cache (handle locally)
- Pre-compute expensive parts

---

## Negative Caching

**Matlab:** Cache **"not found"** results too — avoid hammering DB for non-existent items.

```java
@Cacheable(value = "users", key = "#id")   // null result is cached!
public Optional<User> findById(Long id) {
    return userRepo.findById(id);
}
```

⚠️ Cache poisoning risk: if user later created, stale "not found" returned.

### Better — Short TTL for Negatives

```java
public User get(Long id) {
    User cached = cache.get(id);
    if (cached == null) {
        if (cache.containsKey(id)) {
            // negative cache hit
            return null;
        }
        User user = userRepo.findById(id).orElse(null);
        if (user != null) {
            cache.put(id, user, 30, TimeUnit.MINUTES);
        } else {
            cache.put(id, NULL_MARKER, 1, TimeUnit.MINUTES);   // short TTL for negative
        }
        return user;
    }
    return cached == NULL_MARKER ? null : cached;
}
```

### Bloom Filter for "Definitely Doesn't Exist"

```java
BloomFilter<Long> bloom = BloomFilter.create(...);
// On insert: bloom.put(id);

if (!bloom.mightContain(id)) {
    return null;   // 100% guaranteed not in DB
}
// else: check cache + DB
```

→ False positives possible; false negatives never.

---

## Common Pitfalls

### 1. Caching Without Measuring

"Cache helps" — sometimes hit rate too low to matter. Measure hit rate first.

### 2. Cache as Source of Truth

```java
public User get(Long id) {
    return cache.get(id);   // ❌ if cache empty, returns null even though DB has it
}
```

→ Always fall back to DB.

### 3. Forgetting Invalidation

DB updated, cache not → users see stale data forever (until TTL).

### 4. Over-Long TTL

7-day TTL → 7-day staleness. Match TTL to acceptable freshness.

### 5. Caching User-Specific Data Globally

```java
@Cacheable("dashboard")
public Dashboard get() { return userContextService.dashboard(); }
// ❌ caches first user's dashboard for everyone!
```

→ Include user ID in key.

### 6. Caching Mutable Returned Objects

```java
@Cacheable("config")
public Config get() { return cfg; }

// Caller mutates: cfg.add("x");
// Next call returns mutated cache!
```

→ Return immutable / defensive copy.

### 7. Serialization Issues (Redis)

Default JDK serialization = brittle, slow. Use JSON / Kryo / Protobuf.

```java
RedisCacheConfiguration.defaultCacheConfig()
    .serializeValuesWith(RedisSerializationContext.SerializationPair
        .fromSerializer(new GenericJackson2JsonRedisSerializer()));
```

### 8. Caching Too Much

Cache everything → eviction storms, memory pressure. Cache hot data.

### 9. No Eviction Policy

`maximumSize` not set → unbounded growth → OOM.

```java
Caffeine.newBuilder().maximumSize(10_000).build();
```

### 10. Self-Invocation Misses Spring Cache

```java
public class MyService {
    public User caller() { return this.cached(id); }   // bypasses proxy!
    
    @Cacheable("users")
    public User cached(Long id) { ... }
}
```

→ Same proxy issue as `@Transactional`.

### 11. No Monitoring

```
Hit rate? Eviction rate? Memory? — unknown
```

→ Enable Caffeine stats + Redis monitoring.

### 12. Inconsistent Cache + DB

```java
@Transactional
public void update(User u) {
    userRepo.save(u);
    cache.put(u.getId(), u);   // what if TX rolls back?
}
```

→ Use `@TransactionalEventListener(AFTER_COMMIT)` for cache updates.

### 13. Caching Big Objects

```
Cache value: 10 MB JSON
1000 entries = 10 GB
```

→ Cache references / IDs; fetch full object only when needed.

### 14. Negative Cache Forever

`null` cached without short TTL = "not exists" forever even if created later.

---

## Summary Cheat Sheet

| Concept | Quick Note |
|---------|-----------|
| L1 (Caffeine) | Fastest, per-instance, small |
| L2 (Redis) | Shared, network |
| `@Cacheable` | Read-through populate |
| `@CacheEvict` | Remove |
| `@CachePut` | Update without skipping |
| TTL | Absolute expiry |
| TTI | Idle-based expiry |
| Cache-aside | Most common pattern |
| Refresh-ahead | Background refresh before expiry |
| Thundering herd | Many requests on miss |
| `sync=true` | Single loader |
| Probabilistic refresh | Spread load over time |
| Distributed lock | Cross-instance single loader |
| Negative caching | Cache "not found" briefly |
| Bloom filter | "Definitely not in DB" |

| Strategy | Use For |
|----------|---------|
| Cache-aside | General default |
| Read-through | Unified cache + DB layer |
| Write-through | Strong consistency |
| Write-behind | High write throughput |
| Refresh-ahead | No stall on hot keys |

| ✅ Do | ❌ Don't |
|-------|---------|
| Measure hit rate | Cache blindly |
| Match TTL to freshness need | 1-week TTL "just in case" |
| Bound cache size | Unbounded → OOM |
| Use jitter | Stampede on simultaneous expiry |
| `sync=true` for hot keys | Thundering herd |
| Distributed lock for shared expensive load | Per-instance retry storm |
| `@TransactionalEventListener(AFTER_COMMIT)` | Cache update in TX (rollback issue) |
| Immutable cache values | Mutable shared objects |
| Short TTL for negatives | "Not found" cached forever |

---

## Practice

1. Add `@Cacheable` to a slow service method; measure hit rate via Caffeine stats.
2. Compare Caffeine (L1) vs Redis (L2) latency.
3. Implement multi-level cache (L1 + L2 + DB) for hot endpoint.
4. Trigger thundering herd by clearing cache + bursting requests; observe DB load spike.
5. Add `sync = true`; verify only one DB query.
6. Implement probabilistic early expiration; observe smooth refresh load.
7. Implement Redis distributed lock for expensive load; test with multiple JVMs.
8. Add cache warming on `ApplicationReadyEvent`; measure cold start vs warm start.
9. Implement negative caching with short TTL.
10. Add Bloom filter to skip DB for definitely-missing items.
11. Test self-invocation pitfall — verify `@Cacheable` bypassed.
12. Set up Grafana dashboard for cache hit rate, eviction rate, memory usage.

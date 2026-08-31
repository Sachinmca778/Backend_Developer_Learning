# Caching - Top 10 Interview Questions & Answers

> Covering: Spring Cache Abstraction, @Cacheable/@CachePut/@CacheEvict, Redis, Cache Strategies, Caffeine, TTL, and Cache Stampede

---

## Q1: Spring Cache Abstraction kya hai aur kaise enable karte hain?

**Answer:**

Spring Cache Abstraction caching ko declarative banata hai — annotations se cache operations, bina manual cache code likhe.

### Setup:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("users", "products");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000));
        return cacheManager;
    }
}
```

### How It Works:

```
@EnableCaching → Spring AOP proxy create karta hai
→ Cached methods intercept hote hain
→ Cache check → Hit → Return from cache (method skip)
→ Cache check → Miss → Method execute → Store in cache → Return
```

**Important:**

Caching AOP-based hai — sirf external bean calls pe kaam karta hai. Same class ke andar cached method call pe cache bypass ho sakta hai (self-invocation).

---

## Q2: @Cacheable, @CachePut aur @CacheEvict mein kya difference hai?

**Answer:**

| Annotation | Method Execution | Cache Update | Use Case |
|------------|-----------------|--------------|----------|
| **@Cacheable** | Skip if cache hit | On cache miss | Read operations |
| **@CachePut** | Always execute | Always | Update operations |
| **@CacheEvict** | Normal execute | Remove entries | Delete/invalidate |

### @Cacheable — Read from Cache:

```java
@Cacheable("users")
public User getUserById(Long id) {
    System.out.println("Fetching from DB...");
    return userRepository.findById(id).orElse(null);
}

User u1 = userService.getUserById(1L);  // DB hit → cache store
User u2 = userService.getUserById(1L);  // Cache hit — DB call nahi!
```

### @CachePut — Always Execute + Update Cache:

```java
@CachePut(value = "users", key = "#user.id")
public User updateUser(User user) {
    return userRepository.save(user);  // Hamesha DB update + cache update
}
```

### @CacheEvict — Remove from Cache:

```java
@CacheEvict(value = "users", key = "#id")
public void deleteUser(Long id) {
    userRepository.deleteById(id);
}

@CacheEvict(value = "users", allEntries = true)  // Puraa cache clear
public void clearAllUsersCache() { }
```

### @Caching — Multiple Operations:

```java
@Caching(
    put = @CachePut(value = "users", key = "#user.id"),
    evict = @CacheEvict(value = "userList", allEntries = true)
)
public User saveUser(User user) {
    return userRepository.save(user);
}
```

**Short Interview Answer:**

`@Cacheable` = read-through (skip on hit), `@CachePut` = always update cache, `@CacheEvict` = invalidate cache entries.

---

## Q3: Cache key generation aur SpEL expressions kaise use karte hain?

**Answer:**

### Default Key:

Method parameters automatically key ban jaate hain.

```java
@Cacheable("users")  // Key = id
public User getUserById(Long id) { }
```

### Custom Key with SpEL:

```java
@Cacheable(value = "users", key = "#id")
public User getUserById(Long id) { }

@Cacheable(value = "users", key = "#username + '_' + #includeDetails")
public User getUserByUsername(String username, boolean includeDetails) { }

@Cacheable(value = "users", key = "#user.id")
public User saveAndCache(User user) { }
```

### Custom KeyGenerator:

```java
@Bean("customKeyGenerator")
public KeyGenerator customKeyGenerator() {
    return (target, method, params) ->
        method.getName() + "_" + Arrays.deepToString(params);
}

@Cacheable(value = "users", keyGenerator = "customKeyGenerator")
public User getUser(String username) { }
```

### condition & unless:

```java
// Sirf active users cache karo
@Cacheable(value = "users", key = "#id", condition = "#id > 0")
public User getUserById(Long id) { }

// Null results cache mat karo
@Cacheable(value = "users", key = "#id", unless = "#result == null")
public User getUserById(Long id) { }

// SpEL in unless
@Cacheable(value = "users", unless = "#result.status == 'INACTIVE'")
public User getUser(Long id) { }
```

| Attribute | Purpose |
|-----------|---------|
| **condition** | Method execute hone se pehle check — false pe cache skip |
| **unless** | Result aane ke baad check — true pe cache mat karo |

---

## Q4: Redis ko Spring Boot caching ke saath kaise integrate karte hain?

**Answer:**

Redis distributed caching provide karta hai — multiple app instances share kar sakte hain.

### Dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### Configuration:

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.timeout=5000ms
```

### Redis CacheManager:

```java
@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();

        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .withInitialCacheConfigurations(Map.of(
                "users", config.entryTtl(Duration.ofMinutes(30)),
                "products", config.entryTtl(Duration.ofHours(1))
            ))
            .build();
    }
}
```

### RedisTemplate Direct Usage:

```java
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }
}
```

### Lettuce vs Jedis:

| Feature | Lettuce | Jedis |
|---------|---------|-------|
| Default in Spring Boot | ✅ Yes | No |
| Thread-safe | ✅ Yes | Connection pool needed |
| Async support | ✅ Yes | Limited |

---

## Q5: Cache strategies — Cache-Aside, Write-Through, Write-Behind explain karo.

**Answer:**

### Cache-Aside (Lazy Loading) — Most Common:

```
Read:  Check Cache → Hit? Return : Fetch DB → Store Cache → Return
Write: Update DB → Invalidate Cache
```

```java
@Cacheable("users")
public User getUserById(Long id) { return userRepository.findById(id).orElse(null); }

@CacheEvict(value = "users", key = "#user.id")
public User updateUser(User user) { return userRepository.save(user); }
```

| Pros | Cons |
|------|------|
| Simple, memory efficient | First request slow (cache miss) |
| Only popular data cached | Cache stampede possible |

### Write-Through:

```
Write: Update Cache + Update DB (both sync)
Read:  Always from Cache
```

Cache aur DB dono simultaneously update hote hain — cache hamesha fresh.

| Pros | Cons |
|------|------|
| Cache always up-to-date | Write slow (DB + cache) |
| Consistent reads | Rarely accessed data bhi cached |

### Write-Behind (Write-Back):

```
Write: Update Cache → Queue for DB sync → Return immediately
Read:  From Cache
```

Pehle cache update, baad mein async DB sync — high write throughput.

| Pros | Cons |
|------|------|
| Very fast writes | Data loss risk if cache crashes |
| High throughput | Eventual consistency |

### Read-Through:

Cache itself DB se fetch karta hai on miss — application code simpler.

**Interview Point:** Spring `@Cacheable` = Cache-Aside pattern implement karta hai.

---

## Q6: Cache Stampede problem kya hai aur kaise solve karte hain?

**Answer:**

**Cache Stampede** tab hota hai jab popular cache key expire hone pe bahut saare concurrent requests ek saath DB hit karte hain.

### Problem:

```
Cache key "user:1" expires
    ↓
100 concurrent requests → all cache miss
    ↓
100 DB queries for same user! 💥
```

### Solutions:

**1. Lock-based (Synchronized Loading):**

```java
public User getUserById(Long id) {
    String key = "user:" + id;
    User cached = cache.get(key);
    if (cached != null) return cached;

    synchronized (this) {  // Only one thread fetches
        cached = cache.get(key);  // Double-check
        if (cached != null) return cached;
        User user = userRepository.findById(id).orElse(null);
        cache.put(key, user);
        return user;
    }
}
```

**2. Random TTL (Jitter):**

```java
// Sab keys same time expire na hon
int ttl = baseTtl + random.nextInt(jitterRange);
redisTemplate.expire(key, ttl, TimeUnit.SECONDS);
```

**3. Caffeine refreshAfterWrite:**

```java
Caffeine.newBuilder()
    .refreshAfterWrite(5, TimeUnit.MINUTES)  // Background refresh before expiry
    .build();
```

**4. @Cacheable with sync=true:**

```java
@Cacheable(value = "users", key = "#id", sync = true)
public User getUserById(Long id) {
    return userRepository.findById(id).orElse(null);
}
// sync=true → only one thread loads, others wait
```

---

## Q7: Caffeine aur EhCache — local cache kaise configure karte hain?

**Answer:**

### Caffeine (Spring Boot Default Local Cache):

```java
@Configuration
@EnableCaching
public class CaffeineCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("users", "products");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .recordStats());
        return cacheManager;
    }
}
```

**Why Caffeine?**
- W-TinyLFU eviction — near-optimal hit rate
- Spring Boot 2.x+ default
- Guava Cache ka successor — better performance

### Key Settings:

| Setting | Purpose |
|---------|---------|
| **maximumSize** | Max entries before eviction |
| **expireAfterWrite** | TTL from write time |
| **expireAfterAccess** | TTL from last access |
| **recordStats** | Hit/miss statistics |

---

## Q8: Local Cache vs Distributed Cache (Redis) — kab kya use karein?

**Answer:**

| Feature | Local (Caffeine) | Distributed (Redis) |
|---------|------------------|---------------------|
| **Speed** | Fastest (in-memory, no network) | Fast (network hop) |
| **Scope** | Single JVM instance | Multiple instances share |
| **Persistence** | Lost on restart | Configurable persistence |
| **Memory** | JVM heap limited | Separate server |
| **Consistency** | Per-instance | Shared across cluster |
| **Setup** | Zero config | Redis server needed |

### When to Use What:

| Scenario | Use |
|----------|-----|
| Single instance app | Caffeine |
| Multi-instance / microservices | Redis |
| Session storage | Redis |
| Frequently accessed, small data | Caffeine |
| Shared cache across services | Redis |
| High availability needed | Redis (with replication) |

### Hybrid Approach:

```
L1 Cache: Caffeine (local, ultra-fast)
L2 Cache: Redis (distributed, shared)
```

**Short Interview Answer:**

Caffeine = single JVM, fastest, no setup. Redis = multi-instance, shared, persistent. Production microservices mein usually Redis.

---

## Q9: TTL (Time To Live) configuration caching mein kaise karte hain?

**Answer:**

TTL define karta hai ki cache entry kitni der tak valid rahegi.

### Caffeine TTL:

```java
Caffeine.newBuilder()
    .expireAfterWrite(10, TimeUnit.MINUTES)   // Write se 10 min
    .expireAfterAccess(5, TimeUnit.MINUTES)   // Last access se 5 min
```

### Redis TTL per Cache:

```java
RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
    .entryTtl(Duration.ofMinutes(10));

// Per-cache TTL
.withInitialCacheConfigurations(Map.of(
    "users", config.entryTtl(Duration.ofMinutes(30)),
    "products", config.entryTtl(Duration.ofHours(1)),
    "sessions", config.entryTtl(Duration.ofMinutes(5))
))
```

### @Cacheable with TTL (via CacheManager config):

```java
@Cacheable("users")      // 30 min TTL (CacheManager mein defined)
@Cacheable("sessions")   // 5 min TTL
@Cacheable("products")   // 1 hour TTL
```

### TTL Best Practices:

| Data Type | Suggested TTL |
|-----------|---------------|
| User profiles | 15-30 minutes |
| Product catalog | 1-6 hours |
| Sessions | 5-30 minutes |
| Config/settings | 24 hours |
| Static content | Long TTL + manual evict on update |

**Important:**

TTL ke saath `@CacheEvict` on update/delete use karo — warna stale data serve ho sakta hai TTL expire hone tak.

---

## Q10: Caching ke best practices kya hain?

**Answer:**

### Design:

| Practice | Description |
|----------|-------------|
| **Cache what matters** | Frequently read, rarely changed data |
| **Don't cache everything** | Write-heavy, real-time data avoid karo |
| **TTL always set** | Stale data limit karo |
| **Evict on write** | Update/delete pe cache invalidate karo |
| **Null values** | `unless = "#result == null"` — null cache mat karo |

### Implementation:

```java
// ✅ Good — read with cache, write with evict
@Cacheable(value = "users", key = "#id", unless = "#result == null")
public User getUserById(Long id) { }

@CacheEvict(value = "users", key = "#user.id")
public User updateUser(User user) { }

@CacheEvict(value = "users", key = "#id")
public void deleteUser(Long id) { }
```

### Pitfalls:

| Pitfall | Solution |
|---------|----------|
| Self-invocation | External bean call karo |
| Cache stampede | `sync=true` or lock |
| Stale data | TTL + @CacheEvict on writes |
| Memory overflow | `maximumSize` set karo |
| Serialization issues | JSON serializer use karo (Redis) |

### Monitoring:

```
Actuator metrics: cache.gets, cache.puts, cache.evictions
Caffeine: recordStats() → hit rate monitor karo
Redis: INFO stats → memory usage, hit rate
```

**Short Interview Answer:**

Cache frequently read data, set TTL, evict on writes, avoid caching nulls, use sync for stampede prevention, and monitor hit rates.

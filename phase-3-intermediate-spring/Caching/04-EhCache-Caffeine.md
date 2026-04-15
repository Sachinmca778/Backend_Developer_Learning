# EhCache / Caffeine

## Status: Not Started

---

## Table of Contents

1. [Caffeine Overview](#caffeine-overview)
2. [Caffeine Configuration](#caffeine-configuration)
3. [EhCache Overview](#ehcache-overview)
4. [EhCache Configuration](#ehcache-configuration)
5. [Local vs Distributed Cache](#local-vs-distributed-cache)
6. [Cache Selection Guide](#cache-selection-guide)

---

## Caffeine Overview

**Matlab:** High-performance in-process Java cache — W-TinyLFU algorithm use karta hai.

### Features

```
✅ W-TinyLFU eviction policy — near-optimal hit rate
✅ Asynchronous loading
✅ Size-based eviction
✅ Time-based expiration (TTL)
✅ Reference-based garbage collection integration
✅ Statistics tracking
✅ Java 8+ compatible
```

### Why Caffeine?

```
Guava Cache ka successor — better performance, better eviction
Spring Boot 2.x+ mein default local cache
Near-optimal cache hit rates — ML-inspired algorithm
```

---

## Caffeine Configuration

### Dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

### Basic Configuration

```java
@Configuration
@EnableCaching
public class CaffeineCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000));
        return cacheManager;
    }
}
```

### Advanced Configuration

```java
@Configuration
@EnableCaching
public class AdvancedCaffeineConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "users", "products", "orders", "sessions"
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
            // Size-based eviction
            .maximumSize(10_000)

            // Time-based expiration
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .expireAfterAccess(5, TimeUnit.MINUTES)

            // Weak references (optional)
            // .weakKeys()
            // .weakValues()

            // Soft values — GC pe collect ho sakte hain
            // .softValues()

            // Statistics tracking
            .recordStats()

            // Removal listener
            .removalListener((String key, Object value, RemovalCause cause) ->
                System.out.println("Removed: " + key + " because of " + cause))

            // Refresh after write (async)
            // .refreshAfterWrite(5, TimeUnit.MINUTES)
        );

        // Per-cache configuration
        cacheManager.registerCustomCache("sessions", Caffeine.newBuilder()
            .maximumSize(50_000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build());

        cacheManager.registerCustomCache("config", Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(24, TimeUnit.HOURS)
            .build());

        return cacheManager;
    }

    // Statistics endpoint expose karo
    @Bean
    public CaffeineStatsCacheProcessor statsProcessor() {
        return new CaffeineStatsCacheProcessor();
    }
}
```

### Usage with @Cacheable

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Cacheable(value = "users", key = "#id")
    public User getUserById(Long id) {
        System.out.println("Fetching from DB...");
        return userRepository.findById(id).orElse(null);
    }

    @CachePut(value = "users", key = "#user.id")
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @CacheEvict(value = "users", key = "#id")
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
```

### Programmatic Usage

```java
@Service
public class CaffeineService {

    private final Cache<String, User> userCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .recordStats()
        .build();

    public User getUser(Long id, UserRepository userRepository) {
        String key = "user:" + id;

        return userCache.get(key, k -> {
            // Cache miss — loader function execute hoga
            System.out.println("Loading from DB...");
            return userRepository.findById(id).orElse(null);
        });
    }

    public void invalidate(Long id) {
        userCache.invalidate("user:" + id);
    }

    public void invalidateAll() {
        userCache.invalidateAll();
    }

    // Statistics
    public CacheStats getStats() {
        return userCache.stats();
        // hitRate(), missRate(), hitCount(), missCount(), 
        // evictionCount(), loadSuccessCount(), etc.
    }
}
```

### Async Loading

```java
// AsyncLoadingCache — background loading
AsyncLoadingCache<String, User> asyncCache = Caffeine.newBuilder()
    .maximumSize(1000)
    .expireAfterWrite(10, TimeUnit.MINUTES)
    .buildAsync(key -> {
        // Async DB call
        return userRepository.findById(Long.parseLong(key))
            .orElse(null);
    });

// Usage
CompletableFuture<User> future = asyncCache.get("1");
User user = future.get();  // Wait for result
```

### Refresh After Write

```java
LoadingCache<String, User> refreshCache = Caffeine.newBuilder()
    .maximumSize(1000)
    .refreshAfterWrite(5, TimeUnit.MINUTES)  // 5 min baad async refresh
    .build(key -> userRepository.findById(Long.parseLong(key)).orElse(null));

// Pehla call — DB se load
User user1 = refreshCache.get("1");

// 5 min baad — next get() pe stale value return hoga, background refresh trigger hoga
// Next call pe refreshed value milega
```

---

## EhCache Overview

**Matlab:** Enterprise-grade cache — disk overflow, tiered storage, clustering support.

### Features

```
✅ Multi-tier storage (Heap, Off-heap, Disk)
✅ Persistence — restart pe cache survive kar sakta hai
✅ Clustering — distributed caching
✅ JSR-107 (JCache) compliant
✅ Spring Boot integration
✅ Statistics & monitoring
```

### When to Use EhCache

```
Use EhCache when:
- Large data sets cache karne hain (heap se zyada)
- Disk overflow chahiye (memory full hone pe disk pe spill)
- Cache persistence chahiye (app restart pe data survive)
- JSR-107 compliance chahiye
- Enterprise features chahiye

Don't use EhCache when:
- Simple in-memory cache kaafi hai (use Caffeine)
- Distributed cache chahiye (use Redis/Hazelcast)
- Performance critical hai (Caffeine faster hai)
```

---

## EhCache Configuration

### Dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>org.ehcache</groupId>
    <artifactId>ehcache</artifactId>
</dependency>
<dependency>
    <groupId>javax.cache</groupId>
    <artifactId>cache-api</artifactId>
</dependency>
```

### XML Configuration

```xml
<!-- src/main/resources/ehcache.xml -->
<config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="http://www.ehcache.org/v3"
        xsi:schemaLocation="http://www.ehcache.org/v3 http://www.ehcache.org/schema/ehcache-core-3.0.xsd">

    <!-- Default cache template -->
    <cache-template name="default">
        <expiry>
            <ttl unit="minutes">30</ttl>
        </expiry>
        <resources>
            <heap unit="entries">1000</heap>
            <offheap unit="MB">100</offheap>
        </resources>
    </cache-template>

    <!-- Custom caches -->
    <cache alias="users" uses-template="default">
        <expiry>
            <ttl unit="minutes">60</ttl>
        </expiry>
        <resources>
            <heap unit="entries">5000</heap>
            <offheap unit="MB">200</offheap>
            <disk unit="MB" persistent="true">500</disk>
        </resources>
    </cache>

    <cache alias="products" uses-template="default">
        <expiry>
            <ttl unit="hours">24</ttl>
        </expiry>
        <resources>
            <heap unit="entries">10000</heap>
            <offheap unit="MB">500</offheap>
        </resources>
    </cache>

    <cache alias="sessions">
        <expiry>
            <ttl unit="minutes">30</ttl>
        </expiry>
        <resources>
            <heap unit="entries">50000</heap>
        </resources>
    </cache>
</config>
```

### Java Configuration

```java
@Configuration
@EnableCaching
public class EhCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        JCacheCacheManager cacheManager = new JCacheCacheManager();

        // Ehcache configuration
        org.ehcache.config.CacheConfiguration<String, Object> cacheConfig = 
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                String.class, Object.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .heap(1000, EntryUnit.ENTRIES)
                    .offheap(100, MemoryUnit.MB)
                    .disk(500, MemoryUnit.MB, true)  // Persistent
            )
            .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(30)))
            .build();

        CacheManager ehcacheManager = Caching.getCachingProvider()
            .getCacheManager()
            .createCache("users", Eh107Configuration.fromCacheConfiguration(cacheConfig));

        return cacheManager;
    }
}
```

### Spring Boot Properties

```properties
# application.properties

# Ehcache config file location
spring.cache.jcache.config=classpath:ehcache.xml

# Cache type explicitly
spring.cache.type=jcache
```

### Tiered Storage Explained

```
Tier 1: Heap (Fastest)
- JVM heap memory
- GC affected
- Small size (MBs)

Tier 2: Off-heap (Fast)
- Outside JVM heap
- Not GC affected
- Medium size (100s of MBs)
- Serialization overhead

Tier 3: Disk (Slow)
- Persistent storage
- Survives restarts
- Large size (GBs)
- Highest serialization overhead

Flow: Heap → Off-heap → Disk (eviction chain)
Access: Disk → Off-heap → Heap (promotion chain)
```

---

## Local vs Distributed Cache

### Comparison

| Feature | Local Cache (Caffeine/EhCache) | Distributed Cache (Redis/Hazelcast) |
|---------|-------------------------------|-----------------------------------|
| **Location** | Same JVM mein | Separate server/cluster |
| **Latency** | Very low (nanoseconds) | Higher (network round-trip) |
| **Consistency** | Per-instance — inconsistent across nodes | Consistent across all nodes |
| **Memory** | Limited by JVM heap | Scalable across cluster |
| **Persistence** | EhCache disk support | Redis RDB/AOF |
| **Setup** | Zero config — embedded | Separate infrastructure needed |
| **Use Case** | Single instance, low consistency needs | Multi-instance, strong consistency |

### When to Use Local Cache

```java
// ✅ Use local cache when:
// - Single instance application
// - Data consistency across nodes critical nahi hai
// - Ultra-low latency chahiye
// - Infrastructure complexity avoid karni hai

// Examples:
// - Configuration data
// - Reference/master data
// - Session data (sticky sessions)
// - Computed results (expensive calculations)
```

### When to Use Distributed Cache

```java
// ✅ Use distributed cache when:
// - Multiple application instances hain
// - Data consistency across nodes chahiye
// - Cache size JVM heap se zyada hai
// - Cache sharing between services chahiye

// Examples:
// - User sessions (stateless apps)
// - Shared lookup data
// - Rate limiting counters
// - Cross-service data sharing
```

### Hybrid Approach

```java
// L1 (Local) + L2 (Distributed) caching

// L1: Caffeine (ultra-fast, per-instance)
// L2: Redis (shared, consistent)

@Service
public class HybridCacheService {

    private final Cache<String, User> localCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .build();

    private final RedisTemplate<String, Object> redisTemplate;

    public User getUser(Long id) {
        String key = "user:" + id;

        // L1: Local cache check
        User local = localCache.getIfPresent(key);
        if (local != null) {
            return local;  // Fastest
        }

        // L2: Distributed cache check
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            User user = (User) cached;
            localCache.put(key, user);  // Promote to L1
            return user;
        }

        // L3: Database
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            redisTemplate.opsForValue().set(key, user, 30, TimeUnit.MINUTES);  // L2
            localCache.put(key, user);  // L1
        }

        return user;
    }

    // Update — invalidate both layers
    public User updateUser(User user) {
        User updated = userRepository.save(user);

        String key = "user:" + user.getId();
        localCache.invalidate(key);           // L1 invalidate
        redisTemplate.delete(key);            // L2 invalidate

        return updated;
    }
}
```

---

## Cache Selection Guide

```
Cache chahiye?
├── Single instance application?
│   ├── Simple in-memory cache? → Caffeine
│   ├── Disk overflow chahiye? → EhCache
│   └── Persistence chahiye? → EhCache (disk tier)
├── Multi-instance application?
│   ├── Strong consistency chahiye? → Redis
│   ├── Eventual consistency acceptable? → Redis with TTL
│   └── Ultra-low latency + consistency? → L1 (Caffeine) + L2 (Redis)
└── Specific requirements?
    ├── JSR-107 compliance? → EhCache
    ├── Reactive support? → Lettuce/Redis
    └── Enterprise features (clustering, persistence)? → EhCache / Redis
```

---

## Summary

| Feature | Caffeine | EhCache | Redis |
|---------|----------|---------|-------|
| **Type** | In-memory | In-memory + Disk | Distributed |
| **Eviction** | W-TinyLFU | LRU/LFU/FIFO | LRU/TTL |
| **Performance** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| **Persistence** | ❌ No | ✅ Disk | ✅ RDB/AOF |
| **Distributed** | ❌ No | ✅ Clustering | ✅ Built-in |
| **Best For** | Single-instance caching | Enterprise caching | Multi-instance caching |
| **Complexity** | Low | Medium | High |
| **Spring Default** | ✅ Yes | ❌ No | ❌ No |

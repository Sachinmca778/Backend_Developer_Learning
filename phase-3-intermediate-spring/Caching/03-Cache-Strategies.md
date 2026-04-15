# Cache Strategies

## Status: Not Started

---

## Table of Contents

1. [Cache-Aside (Lazy Loading)](#cache-aside-lazy-loading)
2. [Write-Through](#write-through)
3. [Write-Behind (Write-Back)](#write-behind-write-back)
4. [Read-Through](#read-through)
5. [Cache-First](#cache-first)
6. [TTL Configuration](#ttl-configuration)
7. [Cache Stampede Problem](#cache-stampede-problem)

---

## Cache-Aside (Lazy Loading)

**Matlab:** Application pehle cache check karti hai — miss pe DB se fetch karke cache mein daalti hai. Most common strategy.

### Flow

```
Request → Check Cache
    ↓ Hit
    Return from Cache ✅

    ↓ Miss
    Fetch from DB
    Store in Cache
    Return from DB ✅
```

### Implementation

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // Cache-Aside pattern
    public User getUserById(Long id) {
        String key = "user:" + id;

        // Step 1: Cache check
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return (User) cached;  // Cache hit
        }

        // Step 2: Cache miss — DB se fetch
        User user = userRepository.findById(id).orElse(null);

        // Step 3: Cache mein store
        if (user != null) {
            redisTemplate.opsForValue().set(key, user, 30, TimeUnit.MINUTES);
        }

        return user;
    }

    // Update — cache invalidate
    public User updateUser(User user) {
        User updated = userRepository.save(user);

        // Cache invalidate (next read pe cache refresh hoga)
        redisTemplate.delete("user:" + user.getId());

        return updated;
    }

    // Delete — cache + DB
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
        redisTemplate.delete("user:" + id);  // Cache bhi clear
    }
}
```

### Pros & Cons

```
✅ Pros:
- Simple to implement
- Only caches requested data
- Memory efficient — only popular data cached
- Cache miss pe stale data nahi milega

❌ Cons:
- First request always slow (cache miss)
- Cache stampede possible (multiple requests for same key)
- Stale data possible (TTL expire hone tak)
```

---

## Write-Through

**Matlab:** Write hote hi cache aur DB dono update hote hain — read hamesha cache se.

### Flow

```
Write Request → Update Cache
                ↓
                Update DB
                ↓
                Return Success ✅

Read Request → Return from Cache ✅
```

### Implementation

```java
@Service
@RequiredArgsConstructor
public class WriteThroughService {

    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // Write-through — cache aur DB dono update
    public User createUser(User user) {
        // Step 1: DB mein save
        User saved = userRepository.save(user);

        // Step 2: Cache mein bhi save
        String key = "user:" + saved.getId();
        redisTemplate.opsForValue().set(key, saved, 30, TimeUnit.MINUTES);

        return saved;
    }

    public User updateUser(User user) {
        // Step 1: DB update
        User updated = userRepository.save(user);

        // Step 2: Cache update
        String key = "user:" + updated.getId();
        redisTemplate.opsForValue().set(key, updated, 30, TimeUnit.MINUTES);

        return updated;
    }

    // Read hamesha cache se
    public User getUserById(Long id) {
        String key = "user:" + id;
        Object cached = redisTemplate.opsForValue().get(key);

        if (cached != null) {
            return (User) cached;
        }

        // Fallback — cache miss pe DB se
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            redisTemplate.opsForValue().set(key, user, 30, TimeUnit.MINUTES);
        }

        return user;
    }
}
```

### Pros & Cons

```
✅ Pros:
- Cache hamesha up-to-date
- Read hamesha fast (cache hit)
- No stale data problem
- Consistent reads

❌ Cons:
- Write operation slow (DB + cache)
- Write failure pe cache inconsistent ho sakta hai
- Unnecessary caching (rarely accessed data bhi cache hota hai)
```

---

## Write-Behind (Write-Back)

**Matlab:** Pehle cache mein write karo — baad mein asynchronously DB mein sync karo.

### Flow

```
Write Request → Update Cache
                ↓
                Queue for DB sync
                ↓
                Return Success immediately ✅

Background Process → Batch write to DB
```

### Implementation

```java
@Service
@RequiredArgsConstructor
public class WriteBehindService {

    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // Write-behind — pehle cache, phir async DB
    public User createUser(User user) {
        // Step 1: Cache mein save (immediate)
        String key = "user:" + user.getId();
        redisTemplate.opsForValue().set(key, user, 30, TimeUnit.MINUTES);

        // Step 2: DB sync queue (async)
        syncQueue.add(user);

        return user;
    }

    // Batch sync — periodically run karo
    @Scheduled(fixedDelay = 5000)  // Every 5 seconds
    public void syncToDatabase() {
        List<User> pending = new ArrayList<>(syncQueue);
        syncQueue.clear();

        if (!pending.isEmpty()) {
            userRepository.saveAll(pending);  // Batch insert
        }
    }
}
```

### Pros & Cons

```
✅ Pros:
- Extremely fast writes (cache only)
- DB load kam hota hai (batch writes)
- High throughput scenarios ke liye best

❌ Cons:
- Data loss risk (cache crash pe DB sync nahi hoga)
- Eventually consistent — stale data possible
- Complex error handling (DB sync fail hone pe)
- Recovery mechanism chahiye
```

---

## Read-Through

**Matlab:** Cache khud responsible hai data load karne ke liye — application ko sirf cache se read karna hai.

### Flow

```
Request → Cache check
    ↓ Hit
    Return from Cache ✅

    ↓ Miss
    Cache loads from DB (internally)
    Cache stores data
    Return to application ✅
```

### Implementation

```java
// Spring Cache abstraction read-through provide karti hai
@Service
@RequiredArgsConstructor
public class ReadThroughService {

    private final UserRepository userRepository;

    // @Cacheable — Spring automatically handles cache miss
    @Cacheable(value = "users", key = "#id")
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}

// Internally:
// 1. Cache pehle check hota hai
// 2. Miss hua toh method execute hota hai (DB call)
// 3. Result cache mein store hota hai
// 4. Return hota hai
```

### Custom Read-Through Cache

```java
@Component
public class ReadThroughCache<K, V> {

    private final Map<K, V> cache = new ConcurrentHashMap<>();
    private final Function<K, V> loader;
    private final Duration ttl;

    public ReadThroughCache(Function<K, V> loader, Duration ttl) {
        this.loader = loader;
        this.ttl = ttl;
    }

    public V get(K key) {
        V value = cache.get(key);
        if (value == null) {
            // Cache miss — loader se load karo
            value = loader.apply(key);
            if (value != null) {
                cache.put(key, value);
                // TTL schedule karo (production mein proper cache use karo)
            }
        }
        return value;
    }
}

// Usage
ReadThroughCache<Long, User> userCache = new ReadThroughCache<>(
    id -> userRepository.findById(id).orElse(null),
    Duration.ofMinutes(30)
);

User user = userCache.get(1L);  // Cache miss → DB → cache → return
User user2 = userCache.get(1L); // Cache hit → return
```

### Pros & Cons

```
✅ Pros:
- Clean separation — caching logic separate
- Application code simple
- Cache miss automatically handled

❌ Cons:
- Cache loader failure handle karna complex
- Tight coupling between cache and data source
- First request still slow
```

---

## Cache-First

**Matlab:** Hamesha cache se pehle read karo — fallback sirf jab cache miss ho.

### Flow

```
Request → Cache check
    ↓ Hit
    Return from Cache ✅

    ↓ Miss
    Fallback to DB
    (Optional: cache update)
    Return from DB ✅
```

### Implementation

```java
@Service
@RequiredArgsConstructor
public class CacheFirstService {

    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public User getUserById(Long id) {
        String key = "user:" + id;

        // Priority: Cache first
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return (User) cached;  // Fast path
        }

        // Fallback: DB
        return userRepository.findById(id).orElse(null);
        // Optional: redisTemplate.opsForValue().set(key, user, 30, MINUTES);
    }
}
```

### Use Cases

```
Use cache-first when:
- Data rarely changes (static/reference data)
- Stale data acceptable hai
- DB load minimize karna hai
- Performance is critical

Don't use when:
- Real-time accuracy chahiye
- Data frequently changes
- Consistency critical hai
```

---

## TTL Configuration

**Matlab:** Time-To-Live — cache entries ka expiry time set karna.

### Global TTL

```java
@Bean
public CacheManager cacheManager(RedisConnectionFactory factory) {
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(30))  // Global TTL — 30 minutes
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

    return RedisCacheManager.builder(factory)
        .cacheDefaults(config)
        .build();
}
```

### Per-Cache TTL

```java
@Bean
public CacheManager cacheManager(RedisConnectionFactory factory) {
    return RedisCacheManager.builder(factory)
        .cacheDefaults(
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))  // Default: 30 min
        )
        .withInitialCacheConfigurations(Map.of(
            "users", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(60)),      // Users: 1 hour
            "products", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(24)),        // Products: 24 hours
            "sessions", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5)),       // Sessions: 5 min
            "config", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofDays(1))           // Config: 1 day
        ))
        .build();
}
```

### Dynamic TTL

```java
@Service
@RequiredArgsConstructor
public class DynamicTTLService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void saveWithDynamicTTL(String key, Object value, int priority) {
        Duration ttl = switch (priority) {
            case 1 -> Duration.ofMinutes(5);     // High priority — short TTL
            case 2 -> Duration.ofMinutes(30);    // Medium priority
            default -> Duration.ofHours(24);      // Low priority — long TTL
        };

        redisTemplate.opsForValue().set(key, value, ttl);
    }
}
```

### TTL Best Practices

```
Static data (config, reference data): Long TTL (hours/days)
User data: Medium TTL (minutes/hours)
Session data: Short TTL (minutes)
Real-time data: Very short TTL (seconds) or no cache

Rule of thumb:
- Data change frequency dekho
- Stale data tolerance dekho
- Cache size constraints dekho
```

---

## Cache Stampede Problem

**Matlab:** Jab cache entry expire hoti hai aur multiple requests ek saath DB hit karte hain — DB overload!

### The Problem

```
Time T: Cache entry expires

T+1ms: Request 1 → Cache miss → DB query
T+1ms: Request 2 → Cache miss → DB query
T+1ms: Request 3 → Cache miss → DB query
T+1ms: Request 100 → Cache miss → DB query

DB pe 100 simultaneous queries → Slowdown/Crash!
```

### Solution 1: Probabilistic Early Expiry

```java
@Service
@RequiredArgsConstructor
public class CacheStampedeService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final Random random = new Random();

    public void saveWithJitter(String key, Object value, Duration baseTtl) {
        // TTL mein random jitter add karo
        long jitterMs = (long) (baseTtl.toMillis() * 0.2 * random.nextDouble());
        Duration actualTtl = baseTtl.plusMillis(jitterMs);

        redisTemplate.opsForValue().set(key, value, actualTtl);
    }

    public User getUser(Long id) {
        String key = "user:" + id;
        Object cached = redisTemplate.opsForValue().get(key);

        if (cached != null) {
            return (User) cached;
        }

        // Cache miss — lock leke DB call karo
        return loadWithLock(key, id);
    }
}
```

### Solution 2: Distributed Lock

```java
@Service
@RequiredArgsConstructor
public class LockedCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public User getUser(Long id) {
        String key = "user:" + id;
        String lockKey = "lock:user:" + id;

        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return (User) cached;
        }

        // Distributed lock try karo
        Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, "locked", 10, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(acquired)) {
            try {
                // Double-check — kisi aur ne toh cache nahi kar diya?
                cached = redisTemplate.opsForValue().get(key);
                if (cached != null) {
                    return (User) cached;
                }

                // DB se fetch
                User user = userRepository.findById(id).orElse(null);
                if (user != null) {
                    redisTemplate.opsForValue().set(key, user, 30, TimeUnit.MINUTES);
                }
                return user;
            } finally {
                redisTemplate.delete(lockKey);  // Lock release
            }
        } else {
            // Lock nahi mila — wait karo aur retry karo
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // Retry from cache
            cached = redisTemplate.opsForValue().get(key);
            return cached != null ? (User) cached : userRepository.findById(id).orElse(null);
        }
    }
}
```

### Solution 3: Background Refresh

```java
@Service
@RequiredArgsConstructor
public class BackgroundRefreshService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;

    // Cache hit pe check karo — TTL expire hone se pehle refresh
    public User getUser(Long id) {
        String key = "user:" + id;
        Object cached = redisTemplate.opsForValue().get(key);

        if (cached != null) {
            // TTL remaining check karo
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

            if (ttl != null && ttl < 60) {  // 1 minute se kam bacha hai
                // Background refresh trigger karo
                refreshCacheAsync(id);
            }

            return (User) cached;
        }

        return loadAndCache(id);
    }

    @Async
    public void refreshCacheAsync(Long id) {
        String key = "user:" + id;
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            redisTemplate.opsForValue().set(key, user, 30, TimeUnit.MINUTES);
        }
    }
}
```

---

## Summary

| Strategy | Write Speed | Read Speed | Consistency | Use Case |
|----------|-------------|------------|-------------|----------|
| **Cache-Aside** | Fast (DB only) | First slow, then fast | Eventually consistent | Most common |
| **Write-Through** | Slow (DB + cache) | Fast (cache) | Strong consistency | Read-heavy, consistent data |
| **Write-Behind** | Fastest (cache only) | Fast (cache) | Eventually consistent | High-throughput writes |
| **Read-Through** | N/A | First slow, then fast | Eventually consistent | Clean separation |
| **Cache-First** | N/A | Fast | May be stale | Static data |
| **Stampede Prevention** | Lock overhead | Slight overhead | Consistent | High-concurrency scenarios |

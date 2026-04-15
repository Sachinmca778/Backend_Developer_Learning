# Spring Cache Abstraction

## Status: Not Started

---

## Table of Contents

1. [@EnableCaching](#enablecaching)
2. [@Cacheable](#cacheable)
3. [@CachePut](#cacheput)
4. [@CacheEvict](#cacheevict)
5. [@Caching](#caching)
6. [Key Generation (SpEL)](#key-generation-spel)
7. [Condition & Unless Attributes](#condition--unless-attributes)

---

## @EnableCaching

**Matlab:** Spring caching enable karna — caching infrastructure setup hoti hai.

### Setup

```java
@Configuration
@EnableCaching  // Caching enable karo
public class CacheConfig {

    // Cache Manager define karo
    @Bean
    public CacheManager cacheManager() {
        // In-memory Caffeine cache
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("users", "products", "orders");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000));
        return cacheManager;
    }
}
```

### How It Works

```
@EnableCaching → Spring AOP proxy create karta hai
→ Cached methods intercept hote hain
→ Cache check → Hit → Return from cache
→ Cache check → Miss → Method execute → Cache → Return
```

---

## @Cacheable

**Matlab:** Method ka result cache karo — next calls pe cache se return hoga, method execute nahi hoga.

### Basic Usage

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Cacheable("users")  // "users" cache mein store hoga
    public User getUserById(Long id) {
        System.out.println("Fetching from DB...");
        return userRepository.findById(id).orElse(null);
    }
}

// Usage
User user1 = userService.getUserById(1L);  // DB se fetch → cache
User user2 = userService.getUserById(1L);  // Cache se return — DB call nahi!
```

### Custom Key

```java
// Default key: Method parameters
@Cacheable("users")  // Key = id
public User getUserById(Long id) { }

// Custom key — SpEL expression
@Cacheable(value = "users", key = "#id")
public User getUserById(Long id) { }

// Multiple parameters
@Cacheable(value = "users", key = "#username")
public User getUserByUsername(String username, boolean includeDetails) { }

// Composite key
@Cacheable(value = "users", key = "#username + '_' + #includeDetails")
public User getUserByUsername(String username, boolean includeDetails) { }

// Object property as key
@Cacheable(value = "users", key = "#user.id")
public User saveAndCache(User user) { }
```

### KeyGenerator

```java
// Custom KeyGenerator bean
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean("customKeyGenerator")
    public KeyGenerator customKeyGenerator() {
        return (target, method, params) -> {
            return method.getName() + "_" + Arrays.deepToString(params);
        };
    }
}

// Usage
@Cacheable(value = "users", keyGenerator = "customKeyGenerator")
public User getUser(String username) { }
```

---

## @CachePut

**Matlab:** Method hamesha execute hoga — result cache mein update hoga. Read-through nahi hai.

### Usage

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // @CachePut — method execute hoga, cache update hoga
    @CachePut(value = "users", key = "#user.id")
    public User updateUser(User user) {
        System.out.println("Updating user in DB...");
        return userRepository.save(user);
    }

    // @Cacheable — cache hit pe method skip hoga
    @Cacheable(value = "users", key = "#id")
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}

// Usage
userService.updateUser(new User(1L, "Sachin Updated"));  // DB update + cache update
User user = userService.getUserById(1L);                 // Cache se return (updated value)
```

### @Cacheable vs @CachePut

| Feature | @Cacheable | @CachePut |
|---------|-----------|-----------|
| **Method Execution** | Skip if cache hit | Always execute |
| **Cache Update** | On cache miss | Always |
| **Use Case** | Read operations | Update operations |
| **Performance** | Better (skip possible) | Slower (always execute) |

---

## @CacheEvict

**Matlab:** Cache se entries remove karna — stale data avoid karne ke liye.

### Basic Usage

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // Single entry evict karo
    @CacheEvict(value = "users", key = "#id")
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    // Puraa cache clear karo
    @CacheEvict(value = "users", allEntries = true)
    public void clearAllUsersCache() {
        // Sab user entries cache se remove hongi
    }

    // Method se pehle evict karo
    @CacheEvict(value = "users", allEntries = true, beforeInvocation = true)
    public void refreshAllUsers() {
        // Pehle cache clear, phir method execute
    }
}
```

### beforeInvocation

```java
// Default: false — method execute hone ke baad evict hoga
@CacheEvict(value = "users", key = "#id")
public void deleteUser(Long id) {
    // Method execute → phir cache evict
}

// beforeInvocation = true — method se pehle evict hoga
@CacheEvict(value = "users", key = "#id", beforeInvocation = true)
public void deleteUser(Long id) {
    // Pehle cache evict → phir method execute
    // Useful: Method fail bhi ho toh cache clear ho jayega
}
```

---

## @Caching

**Matlab:** Multiple cache operations ek saath apply karna.

### Usage

```java
@Service
@RequiredArgsConstructor
public class UserService {

    // Multiple operations ek saath
    @Caching(
        evict = {
            @CacheEvict(value = "users", key = "#id"),
            @CacheEvict(value = "userDetails", key = "#id"),
            @CacheEvict(value = "userCache", allEntries = true)
        }
    )
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    // Put + Evict combination
    @Caching(
        put = {
            @CachePut(value = "users", key = "#user.id"),
            @CachePut(value = "userEmails", key = "#user.email")
        },
        evict = {
            @CacheEvict(value = "userSearch", allEntries = true)
        }
    )
    public User updateUser(User user) {
        return userRepository.save(user);
    }
}
```

### @CacheConfig

```java
// Class level cache config — sab methods pe apply hoga
@Service
@CacheConfig(cacheNames = "users")
@RequiredArgsConstructor
public class UserService {

    @Cacheable(key = "#id")  // "users" cache use hoga
    public User getUserById(Long id) { }

    @CachePut(key = "#user.id")  // "users" cache use hoga
    public User updateUser(User user) { }

    @CacheEvict(key = "#id")  // "users" cache use hoga
    public void deleteUser(Long id) { }
}
```

---

## Key Generation (SpEL)

**Matlab:** Spring Expression Language se dynamic keys banana.

### SpEL Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `#root.method` | Method object | `#root.method.name` |
| `#root.target` | Target object | `#root.target.class` |
| `#root.args` | Method arguments | `#root.args[0]` |
| `#root.caches` | Cache names | `#root.caches[0].name` |
| `#argumentName` | Any argument | `#id`, `#username` |
| `#result` | Method return (put/evict) | `#result.id` |
| `#p0`, `#a0` | Argument by index | `#p0`, `#a0` |

### SpEL Examples

```java
// Method name as key
@Cacheable(value = "data", key = "#root.method.name")
public Data getData() { }

// Argument property
@Cacheable(value = "users", key = "#user.email")
public User getUserByEmail(User user) { }

// Composite key
@Cacheable(value = "orders", key = "#customerId + '_' + #orderId")
public Order getOrder(Long customerId, Long orderId) { }

// Conditional key
@Cacheable(value = "products", key = "#category != null ? #category : 'default'")
public List<Product> getProducts(String category) { }

// Using #result (only @CachePut, @CacheEvict)
@CachePut(value = "users", key = "#result.id")
public User createUser(User user) {
    return userRepository.save(user);
}

// Using T() for static methods
@Cacheable(value = "config", key = "T(java.time.LocalDate).now().toString()")
public Config getDailyConfig() { }
```

---

## Condition & Unless Attributes

### condition

**Matlab:** Cache tabhi use karo jab condition true ho.

```java
// Sirf positive IDs ko cache karo
@Cacheable(value = "users", key = "#id", condition = "#id > 0")
public User getUserById(Long id) {
    return userRepository.findById(id).orElse(null);
}

// Sirf long usernames ko cache karo
@Cacheable(value = "users", key = "#username", condition = "#username.length() > 3")
public User getUserByUsername(String username) { }

// Method parameter pe condition
@Cacheable(value = "products", key = "#id", condition = "#includeDetails == false")
public Product getProduct(Long id, boolean includeDetails) { }
```

### unless

**Matlab:** Cache tabhi mat use karo jab condition true ho — method execute hone ke baad check hota hai.

```java
// Null results ko cache mat karo
@Cacheable(value = "users", key = "#id", unless = "#result == null")
public User getUserById(Long id) {
    return userRepository.findById(id).orElse(null);
}

// Empty lists ko cache mat karo
@Cacheable(value = "products", key = "#category", unless = "#result.isEmpty()")
public List<Product> getProductsByCategory(String category) {
    return productRepository.findByCategory(category);
}

// Large results ko cache mat karo
@Cacheable(value = "reports", key = "#reportId", unless = "#result.size() > 10000")
public List<ReportData> getReport(Long reportId) { }
```

### condition vs unless

| Feature | condition | unless |
|---------|-----------|--------|
| **When checked** | Method call se pehle | Method execution ke baad |
| `#result` access | ❌ No | ✅ Yes |
| **Use Case** | Input-based filtering | Output-based filtering |
| **Example** | `condition = "#id > 0"` | `unless = "#result == null"` |

### Combined Usage

```java
@Cacheable(
    value = "users",
    key = "#id",
    condition = "#id > 0",           // Input check — method call se pehle
    unless = "#result.status == 'INACTIVE'"  // Output check — method ke baad
)
public User getUserById(Long id) {
    return userRepository.findById(id).orElse(null);
}
```

---

## Summary

| Annotation | Purpose | When to Use |
|------------|---------|-------------|
| **@Cacheable** | Cache se read, miss pe execute + cache | Read operations |
| **@CachePut** | Always execute + cache update | Update operations |
| **@CacheEvict** | Cache se entry remove karo | Delete/invalidate operations |
| **@Caching** | Multiple cache ops ek saath | Complex scenarios |
| **@CacheConfig** | Class-level cache defaults | Shared config |
| **condition** | Cache use karne se pehle check | Input-based filtering |
| **unless** | Cache mein store karne se pehle check | Output-based filtering |

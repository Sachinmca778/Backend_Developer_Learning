# Rate Limiting

## Status: Not Started

---

## Table of Contents

1. [What is Rate Limiting?](#what-is-rate-limiting)
2. [Why Rate Limiting?](#why-rate-limiting)
3. [Rate Limiting Algorithms](#rate-limiting-algorithms)
   - [Token Bucket](#token-bucket)
   - [Fixed Window Counter](#fixed-window-counter)
   - [Sliding Window Log](#sliding-window-log)
   - [Sliding Window Counter](#sliding-window-counter)
4. [Implementation](#implementation)
   - [Redis + Lua Script](#redis--lua-script)
   - [Bucket4j](#bucket4j)
   - [Resilience4j RateLimiter](#resilience4j-ratelimiter)
5. [Response Headers](#response-headers)

---

## What is Rate Limiting?

**Matlab:** Ek time period mein kitne requests allow hongi, uski limit set karna.

**Example:** 
- 100 requests per minute per user
- 1000 requests per hour per IP
- 5 login attempts per minute

**Jab limit cross jaati hai:** Server extra requests ko reject kar deta hai.

---

## Why Rate Limiting?

| Reason | Explanation |
|--------|-------------|
| **DDoS Protection** | Ek user itna traffic na bhej paaye ki server down ho jaaye |
| **Fair Usage** | Sab users ko equal resources milein - ek user sab bandwidth na le |
| **Cost Control** | API calls ka cost control karna (especially third-party APIs) |
| **Abuse Prevention** | Brute force attacks, scraping, spamming se bachna |
| **SLA Management** | Different tiers (free: 100/min, paid: 1000/min) |

### Real-World Examples

| Service | Rate Limit |
|---------|-----------|
| **Twitter API** | 900 requests / 15 min |
| **GitHub API** | 5000 requests / hour |
| **Google Maps API** | 100,000 requests / day (free tier) |
| **Stripe API** | 100 requests / second |

---

## Rate Limiting Algorithms

### Token Bucket

**Matlab:** Ek bucket mein fixed tokens hote hain. Har request ek token consume karti hai. Tokens ek fixed rate se refill hote hain.

```
Bucket Capacity: 10 tokens
Refill Rate: 1 token / second

Request 1: Token available (10 → 9) ✅
Request 2: Token available (9 → 8) ✅
...
Request 10: Token available (1 → 0) ✅
Request 11: No token! ❌ REJECTED
...
(After 1 second: 1 token refilled)
Request 12: Token available (1 → 0) ✅
```

#### Key Properties

| Property | Behavior |
|----------|----------|
| **Bursty Traffic** | ✅ Allow karta hai (bucket full hai toh burst possible) |
| **Smooth Traffic** | ✅ Steady rate pe bhi kaam karta hai |
| **Tokens Expire?** | Nahi (bucket capacity se zyada accumulate nahi hote) |

#### Example

```
Capacity: 10 tokens
Refill: 2 tokens/second
Initial: 10 tokens (full)

t=0s:  5 requests arrive → 5 tokens used → 5 remaining
t=1s:  2 tokens refilled → 7 tokens
t=1s:  10 requests arrive → 7 allowed, 3 rejected
t=2s:  2 tokens refilled → 2 tokens
t=3s:  2 tokens refilled → 4 tokens (accumulate ho raha hai)
```

#### When to Use
- **API gateways** - bursty traffic handle karna ho
- **User-facing APIs** - sudden spikes allow karni ho
- **Network bandwidth shaping**

---

### Fixed Window Counter

**Matlab:** Time ko fixed windows mein divide karo. Har window mein counter track karo. Window khatam → counter reset.

```
Window: 1 minute
Limit: 100 requests/minute

10:00:00 - 10:00:59 → Window 1 (Counter: 0 → 100)
10:01:00 - 10:01:59 → Window 2 (Counter: 0 → 100)
10:02:00 - 10:02:59 → Window 3 (Counter: 0 → 100)
```

#### Implementation

```java
@RestController
@RequestMapping("/api/data")
public class DataController {
    
    // Map<WindowKey, Counter>
    private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    
    private static final int LIMIT = 100;
    private static final long WINDOW_MS = 60_000; // 1 minute
    
    public ResponseEntity<String> getData(HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        String windowKey = clientIp + ":" + (System.currentTimeMillis() / WINDOW_MS);
        
        int count = counters.computeIfAbsent(windowKey, k -> new AtomicInteger(0))
                            .incrementAndGet();
        
        if (count > LIMIT) {
            return ResponseEntity.status(429).body("Rate limit exceeded");
        }
        
        return ResponseEntity.ok("Data: " + count);
    }
}
```

#### Problem: Boundary Burst

**Scenario:** Limit = 100/min. User last 10 seconds mein 100 requests bhejta hai, aur next window ke first 10 seconds mein bhi 100 requests.

```
Window 1: |          100 requests |
Window 2:                        | 100 requests          |

Actual 20-second window: 200 requests! (Limit should be ~33)
```

**2x burst possible** - boundary pe limit double ho sakta hai.

#### When to Use
- Simple use cases
- Jahan thodi inaccuracy acceptable hai
- Low-traffic APIs

---

### Sliding Window Log

**Matlab:** Har request ka timestamp log karo. Jab bhi request aaye, last N seconds ke requests count karo.

```
Limit: 100 requests / 60 seconds

Request Log:
10:00:05 → Request 1
10:00:10 → Request 2
10:00:15 → Request 3
...
10:00:55 → Request 10

10:01:00 → New request aaya
Check: How many requests in [10:00:00 - 10:01:00]?
Count: 10 requests → ALLOWED (10 < 100)

10:01:10 → New request
Check: How many requests in [10:00:10 - 10:01:10]?
Count: 9 requests (Request 1 expired) → ALLOWED
```

#### Implementation

```java
public class SlidingWindowLog {
    
    private final Queue<Long> requestLog = new ConcurrentLinkedQueue<>();
    private final int limit;
    private final long windowMs;
    
    public SlidingWindowLog(int limit, long windowMs) {
        this.limit = limit;
        this.windowMs = windowMs;
    }
    
    public synchronized boolean allowRequest() {
        long now = System.currentTimeMillis();
        long windowStart = now - windowMs;
        
        // Expired timestamps hata do
        while (!requestLog.isEmpty() && requestLog.peek() < windowStart) {
            requestLog.poll();
        }
        
        // Check limit
        if (requestLog.size() >= limit) {
            return false;  // REJECTED
        }
        
        // Log this request
        requestLog.offer(now);
        return true;  // ALLOWED
    }
}
```

#### Pros & Cons

| Pros | Cons |
|------|------|
| ✅ Accurate (no boundary burst) | ❌ Memory intensive (har request log karni hai) |
| ✅ Smooth behavior | ❌ Queue maintain karni padti hai |
| | ❌ Large scale pe expensive |

#### When to Use
- Jahan accuracy critical hai
- Moderate traffic APIs
- Payment APIs, critical operations

---

### Sliding Window Counter

**Matlab:** Fixed window + sliding window ka hybrid. Current window aur previous window ka weighted average use karta hai.

```
Window: 1 minute
Limit: 100 requests/minute

Current time: 10:01:30 (30 seconds into current window)
Current window count (10:01:00-10:01:59): 50 requests
Previous window count (10:00:00-10:00:59): 80 requests

Estimated count = current + (previous × overlap%)
Overlap% = (60 - 30) / 60 = 50%

Estimated = 50 + (80 × 0.5) = 50 + 40 = 90

90 < 100 → ALLOWED
```

#### Formula

```
overlap_ratio = (window_size - elapsed) / window_size
estimated_count = current_window_count + (previous_window_count × overlap_ratio)

if estimated_count < limit → ALLOW
else → REJECT
```

#### Implementation

```java
public class SlidingWindowCounter {
    
    private final AtomicLong currentCount = new AtomicLong(0);
    private final AtomicLong previousCount = new AtomicLong(0);
    private volatile long windowStart;
    
    private final int limit;
    private final long windowMs;
    
    public SlidingWindowCounter(int limit, long windowMs) {
        this.limit = limit;
        this.windowMs = windowMs;
        this.windowStart = System.currentTimeMillis();
    }
    
    public synchronized boolean allowRequest() {
        long now = System.currentTimeMillis();
        
        // Window rotate hui?
        if (now - windowStart >= windowMs) {
            long elapsedWindows = (now - windowStart) / windowMs;
            
            if (elapsedWindows == 1) {
                previousCount.set(currentCount.get());
            } else {
                previousCount.set(0);  // Multiple windows gap - no overlap
            }
            
            currentCount.set(0);
            windowStart += elapsedWindows * windowMs;
        }
        
        // Sliding window estimate
        double overlapRatio = (double) (now - windowStart) / windowMs;
        double estimatedCount = currentCount.get() + 
                                (previousCount.get() * (1 - overlapRatio));
        
        if (estimatedCount >= limit) {
            return false;  // REJECTED
        }
        
        currentCount.incrementAndGet();
        return true;  // ALLOWED
    }
}
```

#### Pros & Cons

| Pros | Cons |
|------|------|
| ✅ Approximate sliding window accuracy | ❌ Thodi inaccuracy (approximation) |
| ✅ Low memory (sirf 2 counters) | |
| ✅ Fast (no queue maintain) | |

#### When to Use
- **Most production APIs** - best trade-off
- High-traffic APIs (low memory, good accuracy)
- General-purpose rate limiting

---

## Algorithm Comparison

| Algorithm | Accuracy | Memory | Burst Support | Complexity |
|-----------|----------|--------|---------------|------------|
| **Token Bucket** | Medium | Low | ✅ Best | Low |
| **Fixed Window** | Low (boundary burst) | Lowest | ❌ | Lowest |
| **Sliding Window Log** | ✅ Highest | High | ✅ | Medium |
| **Sliding Window Counter** | High (approximate) | Low | ✅ | Medium |

### Which to Choose?

| Scenario | Recommended |
|----------|-------------|
| **Simple, low traffic** | Fixed Window Counter |
| **Bursty traffic** | Token Bucket |
| **High accuracy needed** | Sliding Window Log |
| **Production API (balanced)** | Sliding Window Counter |
| **Distributed system** | Token Bucket (Redis) |

---

## Implementation

### Redis + Lua Script

**Why Redis?** Distributed systems mein - multiple server instances ek shared counter use karein.

**Why Lua?** Redis mein Lua scripts **atomic** hoti hain - race conditions nahi hote.

#### Lua Script (Sliding Window)
```lua
-- KEYS[1] = rate limit key (e.g., "rl:user:123")
-- ARGV[1] = current timestamp (microseconds)
-- ARGV[2] = window size (microseconds)
-- ARGV[3] = max requests

local key = KEYS[1]
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])

-- Window start calculate karo
local windowStart = now - window

-- Purane entries hata do
redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)

-- Current count
local count = redis.call('ZCARD', key)

if count >= limit then
    -- Rate limit exceeded
    return 0
end

-- New entry add karo
redis.call('ZADD', key, now, now .. '-' .. math.random())

-- TTL set karo (auto cleanup)
redis.call('PEXPIRE', key, window / 1000)

return 1  -- Allowed
```

#### Java Implementation
```java
@RestController
@RequestMapping("/api/data")
public class RateLimitedController {
    
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript;
    
    private static final int LIMIT = 100;
    private static final long WINDOW_MS = 60_000;
    
    public RateLimitedController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        
        rateLimitScript = new DefaultRedisScript<>();
        rateLimitScript.setScriptSource(
            new ResourceScriptSource(new ClassPathResource("rate-limit.lua"))
        );
        rateLimitScript.setResultType(Long.class);
    }
    
    @GetMapping
    public ResponseEntity<String> getData(HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        String key = "rl:ip:" + clientIp;
        
        long now = System.currentTimeMillis() * 1000; // microseconds
        
        Long result = redisTemplate.execute(
            rateLimitScript,
            Collections.singletonList(key),
            String.valueOf(now),
            String.valueOf(WINDOW_MS * 1000),
            String.valueOf(LIMIT)
        );
        
        if (result == 0) {
            return ResponseEntity.status(429).body("Rate limit exceeded");
        }
        
        return ResponseEntity.ok("Data fetched");
    }
}
```

#### Redis Dependency
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

---

### Bucket4j

**Bucket4j** = Java rate limiting library based on Token Bucket algorithm.

#### Dependency
```xml
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.7.0</version>
</dependency>
```

#### Basic Usage
```java
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;

@RestController
@RequestMapping("/api/data")
public class Bucket4jController {
    
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    private Bucket createNewBucket() {
        // 100 tokens, 10 tokens per minute refill
        Refill refill = Refill.greedy(10, Duration.ofMinutes(1));
        Bandwidth limit = Bandwidth.classic(100, refill);
        return Bucket.builder().addLimit(limit).build();
    }
    
    @GetMapping
    public ResponseEntity<String> getData(HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(clientIp, k -> createNewBucket());
        
        if (bucket.tryConsume(1)) {
            return ResponseEntity.ok("Data fetched");
        }
        
        return ResponseEntity.status(429).body("Rate limit exceeded");
    }
}
```

#### Bucket4j with Spring Boot Starter
```xml
<dependency>
    <groupId>com.giffing.bucket4j.spring.boot.starter</groupId>
    <artifactId>bucket4j-spring-boot-starter</artifactId>
    <version>0.10.0</version>
</dependency>
```

```yaml
# application.yml
bucket4j:
  enabled: true
  filters:
    - cache-name: buckets
      url: /api/.*
      rate-limits:
        - cache-key: getHeader('X-API-Key')  # Per API key
          bandwidths:
            - capacity: 100
              time: 1
              unit: minutes
```

#### Bucket4j with Redis (Distributed)
```java
@Configuration
public class Bucket4jConfig {
    
    @Bean
    public ProxyManager<String> proxyManager(RedissonClient redissonClient) {
        return RedissonProxyManager.Builder.build(redissonClient);
    }
    
    @Bean
    public Bucket bucket(ProxyManager<String> proxyManager) {
        Refill refill = Refill.greedy(10, Duration.ofMinutes(1));
        Bandwidth limit = Bandwidth.classic(100, refill);
        
        Configuration configuration = Configuration.builder()
            .addLimit(limit)
            .build();
        
        return proxyManager.builder().build("rate-limit-key", configuration);
    }
}
```

---

### Resilience4j RateLimiter

**Resilience4j** = Resilience library (circuit breaker, rate limiter, retry, etc.)

#### Dependency
```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-ratelimiter</artifactId>
    <version>2.1.0</version>
</dependency>
```

#### Basic Usage
```java
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

@RestController
@RequestMapping("/api/data")
public class Resilience4jController {
    
    private final RateLimiter rateLimiter;
    
    public Resilience4jController() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .limitForPeriod(100)
            .timeoutDuration(Duration.ofMillis(0))  // No wait, immediate reject
            .build();
        
        RateLimiterRegistry registry = RateLimiterRegistry.of(config);
        rateLimiter = registry.rateLimiter("api-limiter");
    }
    
    @GetMapping
    public ResponseEntity<String> getData() {
        Supplier<ResponseEntity<String>> supplier = RateLimiter.decorateCheckedSupplier(
            rateLimiter,
            () -> ResponseEntity.ok("Data fetched")
        );
        
        return Try.ofSupplier(supplier)
            .recover(throwable -> ResponseEntity.status(429).body("Rate limit exceeded"))
            .get();
    }
}
```

#### Resilience4j with Spring Boot Starter
```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>
```

```yaml
# application.yml
resilience4j:
  ratelimiter:
    instances:
      apiLimiter:
        limit-for-period: 100
        limit-refresh-period: 60s
        timeout-duration: 0s
        subscribe-for-events: true
```

```java
@RestController
@RequestMapping("/api/data")
public class Resilience4jAnnotatedController {
    
    @GetMapping
    @RateLimiter(name = "apiLimiter", fallbackMethod = "rateLimitFallback")
    public ResponseEntity<String> getData() {
        return ResponseEntity.ok("Data fetched");
    }
    
    public ResponseEntity<String> rateLimitFallback(RequestNotPermitted ex) {
        return ResponseEntity.status(429).body("Rate limit exceeded. Try again later.");
    }
}
```

---

## Implementation Comparison

| Library | Algorithm | Distributed | Spring Integration | Complexity |
|---------|-----------|-------------|-------------------|------------|
| **Redis + Lua** | Custom | ✅ Yes | Manual | Medium |
| **Bucket4j** | Token Bucket | ✅ (with Redis) | ✅ (Starter available) | Low |
| **Resilience4j** | Fixed/Sliding | ❌ (in-memory) | ✅ (Best integration) | Low |

### When to Use Which?

| Scenario | Recommended |
|----------|-------------|
| **Single instance app** | Resilience4j (simplest) |
| **Multi-instance app** | Bucket4j + Redis |
| **Custom logic needed** | Redis + Lua Script |
| **Spring Boot native** | Resilience4j Spring Boot Starter |

---

## Response Headers

**Rate limit headers client ko batate hain:**
- Kitni limit hai
- Kitni remaining hai
- Kab retry karna hai

### Standard Headers

| Header | Description | Example |
|--------|-------------|---------|
| `X-RateLimit-Limit` | Total requests allowed | `100` |
| `X-RateLimit-Remaining` | Remaining requests | `42` |
| `X-RateLimit-Reset` | Unix timestamp jab limit reset hogi | `1704067200` |
| `Retry-After` | Seconds jab tak wait karna hai (429 response pe) | `30` |

### Implementation

```java
@RestController
@RequestMapping("/api/data")
public class RateLimitedController {
    
    private final RateLimiterService rateLimiterService;
    
    @GetMapping
    public ResponseEntity<String> getData(HttpServletRequest request, 
                                          HttpServletResponse response) {
        String clientIp = request.getRemoteAddr();
        RateLimitResult result = rateLimiterService.checkLimit(clientIp);
        
        // Headers set karo
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.getResetTime()));
        
        if (result.isAllowed()) {
            return ResponseEntity.ok("Data fetched");
        }
        
        // Retry-After header (429 response)
        long retryAfter = (result.getResetTime() - System.currentTimeMillis()) / 1000;
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        
        return ResponseEntity.status(429).body("Rate limit exceeded");
    }
}
```

### Example Response (429)

```http
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1704067200
Retry-After: 45
Content-Type: application/json

{
  "error": "Rate limit exceeded",
  "message": "You have exceeded the rate limit. Please retry after 45 seconds."
}
```

### Example Response (200)

```http
HTTP/1.1 200 OK
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 42
X-RateLimit-Reset: 1704067200
Content-Type: application/json

{
  "data": "..."
}
```

---

## Complete Example: Rate Limiting with Spring Boot

### 1. Rate Limiter Service
```java
@Service
public class RateLimiterService {
    
    private final Map<String, SlidingWindowCounter> limiters = new ConcurrentHashMap<>();
    
    private static final int LIMIT = 100;
    private static final long WINDOW_MS = 60_000;
    
    public RateLimitResult checkLimit(String key) {
        SlidingWindowCounter limiter = limiters.computeIfAbsent(
            key, 
            k -> new SlidingWindowCounter(LIMIT, WINDOW_MS)
        );
        
        boolean allowed = limiter.allowRequest();
        long remaining = Math.max(0, limiter.getRemaining());
        long resetTime = limiter.getWindowResetTime();
        
        return new RateLimitResult(allowed, LIMIT, remaining, resetTime);
    }
}
```

### 2. Global Exception Handler
```java
@RestControllerAdvice
public class RateLimitExceptionHandler {
    
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(
            RateLimitExceededException ex, HttpServletResponse response) {
        
        response.setHeader("X-RateLimit-Limit", String.valueOf(ex.getLimit()));
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setHeader("X-RateLimit-Reset", String.valueOf(ex.getResetTime()));
        response.setHeader("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
        
        ErrorResponse error = new ErrorResponse(
            429,
            "Rate limit exceeded",
            "Please retry after " + ex.getRetryAfterSeconds() + " seconds"
        );
        
        return ResponseEntity.status(429).body(error);
    }
}
```

### 3. Custom Filter
```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    
    private final RateLimiterService rateLimiterService;
    
    public RateLimitFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String clientIp = request.getRemoteAddr();
        RateLimitResult result = rateLimiterService.checkLimit(clientIp);
        
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.getResetTime()));
        
        if (!result.isAllowed()) {
            long retryAfter = (result.getResetTime() - System.currentTimeMillis()) / 1000;
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.sendError(429, "Rate limit exceeded");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
}
```

---

## Quick Reference

```java
// Algorithms
Token Bucket:         Bursty traffic, token refill
Fixed Window:         Simple counter, reset on window boundary
Sliding Window Log:   Accurate, timestamp log
Sliding Window Counter: Hybrid, weighted average

// Libraries
Bucket4j:        Bucket.builder().addLimit(limit).build()
Resilience4j:    @RateLimiter(name = "apiLimiter")
Redis + Lua:     redis.call('ZADD', key, now, requestId)

// Response Headers
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 42
X-RateLimit-Reset: 1704067200
Retry-After: 30
```

---

## Summary

| Algorithm | Best For | Trade-off |
|-----------|----------|-----------|
| **Token Bucket** | Bursty traffic | Allows bursts, smooth refill |
| **Fixed Window** | Simple, low traffic | Boundary burst issue |
| **Sliding Window Log** | High accuracy | High memory usage |
| **Sliding Window Counter** | Production APIs | Best accuracy/memory trade-off |

| Implementation | When to Use |
|----------------|-------------|
| **Redis + Lua** | Distributed systems, custom logic |
| **Bucket4j** | Token Bucket, Spring Boot integration |
| **Resilience4j** | Single instance, easiest setup |

| Header | Purpose |
|--------|---------|
| `X-RateLimit-Limit` | Total allowed requests |
| `X-RateLimit-Remaining` | Remaining requests |
| `X-RateLimit-Reset` | Reset timestamp |
| `Retry-After` | Wait time (429 response) |

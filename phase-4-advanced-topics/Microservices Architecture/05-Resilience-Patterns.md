# 05 — Resilience Patterns in Microservices

> Microservices mein failures **inevitable** hain. Network timeout hoga, database slow hoga, koi service crash karegi. Resilience patterns ye ensure karte hain ki ek service ka failure dusre services ko na affect kare. Isko **Fault Tolerance** bhi bolte hain.

---

## Table of Contents

1. [Why Resilience Matters?](#why-resilience-matters)
2. [Circuit Breaker Pattern (Resilience4j)](#circuit-breaker-pattern-resilience4j)
3. [Retry Pattern](#retry-pattern)
4. [Bulkhead Pattern](#bulkhead-pattern)
5. [Rate Limiter](#rate-limiter)
6. [Timeout Pattern](#timeout-pattern)
7. [Fallback Mechanism](#fallback-mechanism)
8. [Annotations Reference](#annotations-reference)
9. [Combining Multiple Patterns](#combining-multiple-patterns)
10. [Comparison Table](#comparison-table)
11. [Best Practices](#best-practices)

---

## Why Resilience Matters?

Ek simple scenario samjho:

```
Without Resilience — Cascading Failure:

  User Request
       |
       v
+------+------+
|  API GW     |
+------+------+
       |
       v
+------+------+     calls      +-------------+     calls     +-----------+
| Order       | -------------> | Payment     | -----------> | Bank      |
| Service     |    (slow)      | Service     |  (timeout)    | Gateway   |
+-------------+                +-------------+               +-----------+
                                      |
                                      | 100 threads waiting...
                                      | All threads BLOCKED
                                      v
                           +-------------------+
                           | Thread Pool FULL  |
                           | Payment Service   |
                           | CRASHED!          |
                           +-------------------+
                                      |
                            Cascading failure!
                                      |
                           +----------v----------+
                           | Order Service bhi   |
                           | CRASH! (no threads) |
                           +---------------------+

Result: Ek service ke slow hone ne POORA system down kar diya.
```

```
With Resilience — Isolated Failure:

  User Request
       |
       v
+------+------+
|  API GW     |
+------+------+
       |
       v
+-------------+              +-------------+
| Order Svc   |---CIRCUIT--->| Payment Svc |
| Service     |   OPEN!      | (slow/down) |
+------+------+              +-------------+
       |                             |
       | Circuit Breaker returns     | Threads NOT blocked
       | FALLBACK response           | because of Bulkhead
       v                             v
  User gets                        Payment Svc
  "Payment is                      recovers
   temporarily                     independently
   unavailable"
```

---

## Circuit Breaker Pattern (Resilience4j)

Circuit Breaker electrical circuit breaker jaisa hai. Jab current zyada ho jaye, circuit breaker trip ho jata hai aur aage current jaana band ho jata hai. Same concept microservices mein.

### Circuit Breaker States (FINITE STATE MACHINE)

```
                    failure rate > threshold
                         OR
                  slow call rate > threshold
    +--------+    +-------------------+    +--------+
    |        |    |                   |    |        |
    | CLOSED |--- |                   |--->|  OPEN  |
    |        |    |                   |    |        |
    +--------+    +-------------------+    +--------+
         ^                   |                   |
         |                   |                   |
         |    success        |                   | waitDuration expires
         |    rate OK        |                   |
         |                   |                   v
         |             +-----------+      +-----------+
         |             |           |      |           |
         +-------------|  HALF_    |<-----|  HALF_    |
                       |  OPEN     |      |  OPEN     |
                       |  (test)   |      |  (forced) |
                       |           |      |           |
                       +-----------+      +-----------+

State Details:

CLOSED (Normal Operation):
  - Normal traffic flow
  - Failures count ho rahi hain
  - Jab failure rate threshold cross kare -> OPEN
  - "Everything is working fine" state

OPEN (Failing — Stop Requests):
  - NO requests go to the failing service
  - Immediate fallback response
  - waitDuration ke baad -> HALF_OPEN
  - "Service is down, don't bother calling" state

HALF_OPEN (Testing — Limited Requests):
  - Few test requests bhejte hain
  - Agar success -> CLOSED (service recovered!)
  - Agar failure -> OPEN (still broken, wait again)
  - "Let me check if service is back" state
```

### Real-World Analogy

```
Restaurant Example:

CLOSED State:
  - Tum regular customer ho
  - Restaurant mein jaake order dete ho
  - Food milta hai -> sab normal
  - Kabhi-kabhi food late aata hai (some failures)
  - But mostly OK

OPEN State:
  - Restaurant ne 5 baar late food diya (threshold cross)
  - Tum decide karte ho: "Ab is restaurant se order nahi karunga"
  - Next time directly bolte ho: "Restaurant band hai, backup plan use karo"
  - 30 minutes wait karte ho (waitDuration)

HALF_OPEN State:
  - 30 minutes baad tum ek test order dete ho
  - Agar food time pe aaya -> CLOSED (back to normal!)
  - Agar food phir se late aaya -> OPEN (still bad, wait another 30 min)
```

### Setup — Resilience4j with Spring Boot

**pom.xml:**

```xml
<!-- Circuit Breaker -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>

<!-- For annotation-based approach (@CircuitBreaker) -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>

<!-- Actuator for monitoring -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Circuit Breaker — Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        # Failure threshold calculation
        failure-rate-threshold: 50        # 50% failures pe OPEN hoga
        slow-call-rate-threshold: 80      # 80% slow calls pe OPEN
        slow-call-duration-threshold: 2s  # 2 seconds se zyada = slow call

        # State transitions
        wait-duration-in-open-state: 10s  # OPEN se HALF_OPEN jaane mein 10s wait
        permitted-number-of-calls-in-half-open-state: 3  # HALF_OPEN mein 3 test requests
        sliding-window-type: COUNT_BASED  # COUNT_BASED or TIME_BASED
        sliding-window-size: 10           # Last 10 calls ka data dekho

        # Events
        record-exceptions: java.io.IOException,java.util.concurrent.TimeoutException
        ignore-exceptions: java.lang.NullPointerException  # NPE ko count mat karo

      inventoryService:
        failure-rate-threshold: 40
        wait-duration-in-open-state: 30s
        sliding-window-size: 20
        sliding-window-type: TIME_BASED
        minimum-number-of-calls: 10      # Kam se kam 10 calls hone chahiye calculation ke liye
```

### Circuit Breaker — Code Example

**Annotation-Based Approach:**

```java
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    // @CircuitBreaker annotation
    // name = circuit breaker instance ka naam (config mein define kiya hai)
    // fallbackMethod = jab circuit OPEN ho, ye method call hoga

    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    public String processPayment(Long orderId, double amount) {
        // Ye method call hoga normally
        // Jab circuit CLOSED hai
        // Agar circuit OPEN hai -> fallbackMethod call hoga
        // Agar HALF_OPEN hai -> ye method test request ke taur pe call hoga

        String url = "http://payment-service/api/payments";
        return restTemplate.postForObject(url, request, String.class);
    }

    // Fallback method — Circuit OPEN hone pe ye execute hoga
    // IMPORTANT: Fallback method ka signature same hona chahiye + 1 extra Parameter (Throwable)
    public String paymentFallback(Long orderId, double amount, Throwable t) {
        // Log the error
        System.err.println("Circuit is OPEN for paymentService. Error: " + t.getMessage());

        // Return graceful fallback response
        return "Payment service is temporarily unavailable. Order ID: " + orderId
               + ". Please try after some time.";
    }
}
```

### Programmatic CircuitBreaker Usage

```java
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.function.Supplier;

@Service
public class OrderService {

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    public String processOrder(Long orderId) {
        // Registry se circuit breaker instance lo
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("paymentService");

        // Execute with circuit breaker protection
        Supplier<String> paymentCall = () -> {
            return restTemplate.postForObject(
                "http://payment-service/api/payments",
                request, String.class
            );
        };

        // decorateSupplier circuit breaker wrap karta hai
        Supplier<String> decorated = CircuitBreaker
            .decorateSupplier(circuitBreaker, paymentCall);

        // Recovery — fallback jab circuit OPEN ho
        Supplier<String> recovery = throwable -> "Fallback: Payment unavailable";

        return decorated.orElse(recovery).get();
    }
}
```

### Monitoring Circuit Breaker State

```java
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CircuitBreakerHealthController {

    @Autowired
    private CircuitBreakerRegistry registry;

    @GetMapping("/cb/status")
    public String getCircuitBreakerStatus() {
        CircuitBreaker cb = registry.circuitBreaker("paymentService");

        CircuitBreaker.State state = cb.getState();
        // state will be one of: CLOSED, OPEN, HALF_OPEN

        return "Circuit Breaker State: " + state
               + "\nMetrics: " + cb.getMetrics();
    }
}
```

**Actuator Endpoint** (`/actuator/circuitbreakers`):

```json
{
  "circuitBreakers": {
    "paymentService": {
      "state": "CLOSED",
      "failureRate": 15.0,
      "slowCallRate": 5.0,
      "bufferedCalls": 10,
      "failedCalls": 1,
      "slowCalls": 0
    }
  }
}
```

### Circuit Breaker Events

```
Circuit Breaker yeh events emit karta hai (for logging/monitoring):

Event                    | When
-------------------------|------------------------------------------
onSuccess                | Call successful (CLOSED ya HALF_OPEN state)
onError                  | Call failed (CLOSED state, failure count)
onCallNotPermitted       | Call blocked because circuit is OPEN
onStateTransition        | State change: CLOSED->OPEN, OPEN->HALF_OPEN, etc.
onSlowCall               | Call took longer than slow-call-duration-threshold
```

```java
// Event listener example
@EventListener
public void onStateTransition(CircuitBreakerOnStateTransitionEvent event) {
    log.warn("Circuit Breaker [{}] transitioned from {} to {}",
        event.getCircuitBreakerName(),
        event.getTransition().getFromState(),
        event.getTransition().getToState()
    );
}
```

### Circuit Breaker — Complete State Transition Example

```
Timeline with failure-rate-threshold = 50%, wait-duration = 10s:

Time     Request   Result    State       Action
---------------------------------------------------------------
00:00    Req 1     SUCCESS   CLOSED      Normal, success count++
00:01    Req 2     SUCCESS   CLOSED      Normal, success count++
00:02    Req 3     FAILURE   CLOSED      Failure count++
00:03    Req 4     FAILURE   CLOSED      Failure rate = 50% (threshold!)
00:04    Req 5     ---       OPEN!       Circuit tripped! Fallback returned
00:05    Req 6     ---       OPEN        Fallback returned
00:06    Req 7     ---       OPEN        Fallback returned
00:07    Req 8     ---       OPEN        Fallback returned
00:08    Req 9     ---       OPEN        Fallback returned
00:09    Req 10    ---       OPEN        Fallback returned
00:10    ---       ---       HALF_OPEN   waitDuration expired, testing mode!
00:11    Req 11    SUCCESS   HALF_OPEN   Test request passed!
00:12    Req 12    SUCCESS   HALF_OPEN   Test request passed!
00:13    Req 13    SUCCESS   HALF_OPEN   Test request passed!
00:14    ---       ---       CLOSED!     All tests passed, back to normal!
```

---

## Retry Pattern

Sometimes failures temporary hote hain — network glitch, timeout, etc. Retry pattern automatically retry karta hai failed calls.

### Retry Configuration

```yaml
resilience4j:
  retry:
    instances:
      paymentService:
        max-attempts: 3              # Total 3 baar try karega (1 original + 2 retries)
        wait-duration: 1s            # Har retry ke beech 1 second wait
        wait-duration-open-timeout: 5s # OPEN state ke baad wait
        retry-exceptions:            # In exceptions pe retry karo
          - java.io.IOException
          - java.util.concurrent.TimeoutException
        ignore-exceptions:           # In exceptions pe retry mat karo
          - java.lang.NullPointerException
          - java.lang.IllegalArgumentException
```

### Exponential Backoff

```
Linear Retry (fixed wait):
  Attempt 1 -> fail -> wait 1s
  Attempt 2 -> fail -> wait 1s
  Attempt 3 -> fail -> DONE

Exponential Backoff (smart wait — increasing):
  Attempt 1 -> fail -> wait 1s
  Attempt 2 -> fail -> wait 2s  (1s * 2)
  Attempt 3 -> fail -> wait 4s  (2s * 2)
  Attempt 4 -> fail -> wait 8s  (4s * 2)
  ...

Why? Agar service down hai, immediately retry karne se koi fayda nahi.
     Dheere-dheere wait badhao taaki service recover ho sake.
```

```yaml
resilience4j:
  retry:
    instances:
      paymentService:
        max-attempts: 4
        wait-duration: 1s
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2    # Har baar wait double hoga
        max-wait-duration: 30s               # Maximum wait 30s se zyada nahi
```

### Retry — Code Example

```java
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    @Retry(name = "paymentService", fallbackMethod = "retryFallback")
    public String processPayment(Long orderId, double amount) {
        // Agar ye call fail ho gayi
        // Retry automatically retry karega (max-attempts times)
        // Exponential backoff ke saath
        return restTemplate.postForObject(
            "http://payment-service/api/payments",
            request, String.class
        );
    }

    public String retryFallback(Long orderId, double amount, Throwable t) {
        return "Payment failed after all retries. Order: " + orderId
               + ". Error: " + t.getMessage();
    }
}
```

### Retry — Programmatic Approach

```java
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.function.Supplier;

@Service
public class OrderService {

    @Autowired
    private RetryRegistry retryRegistry;

    public String callPayment(Long orderId) {
        Retry retry = retryRegistry.retry("paymentService");

        Supplier<String> paymentCall = () -> {
            return restTemplate.postForObject(
                "http://payment-service/api/payments", request, String.class
            );
        };

        return retry.executeSupplier(paymentCall);
    }

    // Custom Retry configuration programmatically
    public void createCustomRetry() {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(5)
            .waitDuration(Duration.ofSeconds(2))
            .retryExceptions(IOException.class, TimeoutException.class)
            .ignoreExceptions(IllegalArgumentException.class)
            .enableExponentialBackoff()
            .exponentialBackoffMultiplier(2)
            .build();

        Retry retry = retryRegistry.retry("customPayment", config);
    }
}
```

### Retry — Common Pitfalls

```
WARNING: Blindly retry mat karo sab kuch!

DON'T Retry:
  - POST/PUT/DELETE calls without idempotency check
    (Ek payment do baar process ho sakta hai!)
  - Validation errors (IllegalArgumentException)
  - Business logic errors (insufficient balance)

DO Retry:
  - Network timeouts
  - Connection refused
  - HTTP 503 (Service Unavailable)
  - Transient failures
```

```java
// Idempotent key pattern for safe retries
public String processPayment(PaymentRequest request) {
    // Har request ka unique idempotency key
    String idempotencyKey = request.getIdempotencyKey();

    // Check karo kya ye request already processed hai?
    if (paymentRepo.existsByTransactionId(idempotencyKey)) {
        return "Already processed: " + idempotencyKey;
    }

    // Safe to process
    return processPaymentInternal(request);
}
```

---

## Bulkhead Pattern

Bulkhead ship ke compartments jaisa hai. Agar ek compartment mein paani aa jaye, toh baaki compartments safe rehte hain. Ship doobta nahi.

### Without Bulkhead — Shared Thread Pool Problem

```
+------------------------------------------------+
|           Order Service Thread Pool            |
|           (Total: 100 threads)                 |
|                                                |
|  [=== Payment calls ===]    70 threads         |
|  [=== Payment calls ===]                       |
|  [=== Payment calls ===]                       |
|  [=== Payment calls ===]                       |
|  [=== Payment calls ===]                       |
|                                                |
|  [User Profile calls]         10 threads       |
|  [Notification calls]          5 threads       |
|  [Inventory calls]             5 threads       |
|  [Free]                       10 threads       |
+------------------------------------------------+

Problem: Payment service slow ho gaya
-> 70 threads stuck waiting on payment-service
-> Sirf 20 threads bache for ALL other calls
-> User Profile, Notification, Inventory — sab slow!
-> Ek service ne POORA service affect kar diya
```

### With Bulkhead — Isolated Thread Pools

```
+------------------------------------------------+
|           Order Service Thread Pool            |
|                                                |
|  +------------------------+  30 threads        |
|  | BULKHEAD: Payment      |                    |
|  | [waiting...]           |                    |
|  | [waiting...]           |                    |
|  | (max 30 threads)       |                    |
|  +------------------------+                    |
|                                                |
|  +------------------------+  20 threads        |
|  | BULKHEAD: User Profile |                    |
|  | [active] [active]      |                    |
|  | (max 20 threads)       |                    |
|  +------------------------+                    |
|                                                |
|  +------------------------+  20 threads        |
|  | BULKHEAD: Notification |                    |
|  | [active]               |                    |
|  | (max 20 threads)       |                    |
|  +------------------------+                    |
|                                                |
|  +------------------------+  30 threads        |
|  | BULKHEAD: Inventory    |                    |
|  | [active] [active]      |                    |
|  | [active]               |                    |
|  | (max 30 threads)       |                    |
|  +------------------------+                    |
+------------------------------------------------+

Ab Payment service slow ho -> sirf 30 threads affected
User Profile, Notification, Inventory — unaffected!
```

### Bulkhead Types

```
1. Semaphore Bulkhead:
   - Lightweight (no extra threads)
   - Max concurrent calls limit karta hai
   - Current thread pool se kaam karta hai
   - Fast, low overhead

2. ThreadPool Bulkhead:
   - Dedicated thread pool per service
   - Complete isolation
   - Higher overhead but better isolation
```

### Bulkhead — Configuration

```yaml
resilience4j:
  bulkhead:
    instances:
      paymentService:
        max-concurrent-calls: 10       # Max 10 concurrent calls to payment
        max-wait-duration: 500ms       # Agar pool full hai, 500ms wait karo

      inventoryService:
        max-concurrent-calls: 20
        max-wait-duration: 200ms

  thread-pool-bulkhead:
    instances:
      paymentService:
        max-thread-pool-size: 20       # Max 20 threads
        core-thread-pool-size: 5       # Min 5 threads always alive
        queue-capacity: 10             # Max 10 calls queue mein wait kar sakte hain
```

### Bulkhead — Code Example

```java
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    // @Bulkhead annotation
    // name = bulkhead instance ka naam
    // fallbackMethod = jab bulkhead full ho (limit exceeded)
    // type = SEMAPHORE (default) ya THREADPOOL

    @Bulkhead(name = "paymentService", fallbackMethod = "bulkheadFallback")
    public String processPayment(Long orderId, double amount) {
        // Max 10 concurrent calls allowed (from config)
        // Agar 11th call aayi -> immediately fallback
        return restTemplate.postForObject(
            "http://payment-service/api/payments",
            request, String.class
        );
    }

    public String bulkheadFallback(Long orderId, double amount, Throwable t) {
        return "Payment service is busy. Too many concurrent requests. "
               + "Order: " + orderId + ". Try again later.";
    }

    // ThreadPool Bulkhead example
    @Bulkhead(name = "paymentService", type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<String> processPaymentAsync(Long orderId, double amount) {
        // Ye dedicated thread pool mein execute hoga
        // Complete isolation from other calls
        return CompletableFuture.completedFuture(
            restTemplate.postForObject(
                "http://payment-service/api/payments",
                request, String.class
            )
        );
    }
}
```

---

## Rate Limiter

Rate Limiter requests per second/minute limit karta hai. Overwhelming traffic ko control karta hai.

### Rate Limiter — ASCII Diagram

```
Without Rate Limiter:

  User ----->+
  User ----->+
  User ----->+   1000 req/s ----->  [Payment Service]  ----> CRASH!
  User ----->+
  User ----->+

With Rate Limiter:

  User ----->+
  User ----->+
  User ----->+                    [Rate Limiter]     [Payment Service]
  User ----->+   1000 req/s  ->   (allow 100/s)  ->  (handles 100/s)
  User ----->+                    [Reject 900]       (happy & healthy)
  User ----->+
```

### Rate Limiter Configuration

```yaml
resilience4j:
  ratelimiter:
    instances:
      paymentService:
        limit-for-period: 100          # 100 requests per period
        limit-refresh-period: 1s       # Har 1 second mein limit refresh hogi
        timeout-duration: 500ms        # Agar limit exceeded, 500ms wait karo
```

### Rate Limiter — Token Bucket Algorithm

```
Token Bucket Algorithm (most common):

Bucket capacity: 100 tokens
Refill rate: 100 tokens/second

  +------------------+
  |  [token]         |
  |  [token]         |  <-- Tokens refill at fixed rate
  |  [token]         |
  |  ...             |
  |  [token]         |
  +------------------+
         |
         | Each request takes 1 token
         v
  +------------------+
  | Request arrives  |
  | Check tokens > 0 |
  | If yes -> allow & -1 token
  | If no -> reject/wait
  +------------------+

Example:
  t=0s: 100 tokens (full)
  50 requests aaye -> 50 tokens consumed -> 50 remaining
  t=0.5s: 50 + 50 = 100 tokens (refilled)
  150 requests aaye -> 100 tokens consumed -> 0 remaining
  50 requests -> REJECTED! No tokens left.
```

### Rate Limiter — Code Example

```java
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    @RateLimiter(name = "paymentService", fallbackMethod = "rateLimitFallback")
    public String processPayment(Long orderId, double amount) {
        return restTemplate.postForObject(
            "http://payment-service/api/payments",
            request, String.class
        );
    }

    public String rateLimitFallback(Long orderId, double amount, Throwable t) {
        return "Too many requests! Payment service rate limited. "
               + "Order: " + orderId + ". Please slow down.";
    }
}
```

---

## Timeout Pattern

Timeout sabse basic resilience pattern hai — har call ka maximum wait time define karo.

### Timeout — Why It Matters

```
Without Timeout:

  Service A ----call----> Service B (stuck)
              |
              | Thread BLOCKED forever
              | (waiting for response that never comes)
              |
              v
         Thread Pool EXHAUSTED
         All threads waiting
         Service A CRASHED

With Timeout:

  Service A ----call(2s timeout)---> Service B (stuck)
              |
              | After 2 seconds...
              | TimeoutException!
              | Thread freed
              v
         Fallback / Retry / Error
         Other threads still working
```

### Timeout Configuration

```yaml
# Resilience4j TimeLimiter
resilience4j:
  timelimiter:
    instances:
      paymentService:
        timeout-duration: 2s           # 2 seconds max wait
        cancel-running-future: true    # Timeout pe future cancel karo

# RestTemplate timeout (lower level)
# WebClient timeout
```

### Timeout — Code Example

```java
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;

@Service
public class PaymentService {

    // TimeLimiter always async calls ke saath kaam karta hai
    @TimeLimiter(name = "paymentService")
    public CompletableFuture<String> processPaymentAsync(Long orderId, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            return restTemplate.postForObject(
                "http://payment-service/api/payments",
                request, String.class
            );
        });
    }
}
```

### RestTemplate Timeout (Direct)

```java
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@Bean
public RestTemplate restTemplate() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(3000);   // Connection establish: max 3s
    factory.setReadTimeout(5000);      // Response wait: max 5s
    return new RestTemplate(factory);
}
```

---

## Fallback Mechanism

Fallback har resilience pattern ka last line of defense hai. Jab kuch kaam na kare, fallback response do.

### Fallback Strategies

```
Fallback Pattern:

  Primary Call:  payment-service ---> FAILS (circuit open / timeout / error)
                                          |
                                          v
  Fallback Chain:
    1. Try cached response               ---> Cache hit? Return cached data
    2. Try secondary service             ---> Backup service available? Use it
    3. Return default/static response    ---> "Service unavailable"
    4. Return empty/null                 ---> Last resort
```

### Fallback — Code Examples

**Strategy 1: Static Default Response**

```java
@CircuitBreaker(name = "paymentService", fallbackMethod = "defaultFallback")
public PaymentResponse getPaymentStatus(Long orderId) {
    return paymentClient.getPaymentStatus(orderId);
}

public PaymentResponse defaultFallback(Long orderId, Throwable t) {
    // Static response
    return new PaymentResponse(
        orderId,
        "PENDING",  // Default status
        "Payment status temporarily unavailable"
    );
}
```

**Strategy 2: Cached Response**

```java
@Autowired
private CacheManager cacheManager;

@CircuitBreaker(name = "paymentService", fallbackMethod = "cachedFallback")
public PaymentResponse getPaymentStatus(Long orderId) {
    PaymentResponse response = paymentClient.getPaymentStatus(orderId);
    // Cache successful response
    cacheManager.getCache("payments").put(orderId, response);
    return response;
}

public PaymentResponse cachedFallback(Long orderId, Throwable t) {
    // Cache se purana data return karo
    Cache.ValueWrapper cached = cacheManager.getCache("payments").get(orderId);
    if (cached != null) {
        return (PaymentResponse) cached.get();
    }
    // Cache bhi nahi mila
    return new PaymentResponse(orderId, "UNKNOWN", "No data available");
}
```

**Strategy 3: Secondary Service**

```java
@CircuitBreaker(name = "primaryPaymentService", fallbackMethod = "useBackupPayment")
public String processPayment(Long orderId, double amount) {
    return primaryPaymentClient.pay(orderId, amount);
}

@CircuitBreaker(name = "backupPaymentService", fallbackMethod = "backupFallback")
public String useBackupPayment(Long orderId, double amount, Throwable t) {
    // Primary failed, try backup service
    return backupPaymentClient.pay(orderId, amount);
}

public String backupFallback(Long orderId, double amount, Throwable t) {
    return "Both primary and backup payment services are down. "
           + "Order: " + orderId + ". Please retry later.";
}
```

---

## Annotations Reference

Quick reference for all Resilience4j annotations:

| Annotation | Purpose | Key Config Params |
|-----------|---------|-------------------|
| `@CircuitBreaker` | Failure protection | `failure-rate-threshold`, `wait-duration`, `sliding-window-size` |
| `@Retry` | Auto-retry on failure | `max-attempts`, `wait-duration`, `exponential-backoff` |
| `@Bulkhead` | Thread/concurrent call isolation | `max-concurrent-calls`, `max-wait-duration` |
| `@RateLimiter` | Request rate control | `limit-for-period`, `limit-refresh-period` |
| `@TimeLimiter` | Async call timeout | `timeout-duration` |

### Annotation Stacking — Multiple Annotations on One Method

```java
@Service
public class PaymentService {

    // Multiple resilience patterns ek saath!
    // Order matters — top to bottom execute hoga

    @CircuitBreaker(name = "paymentService", fallbackMethod = "circuitFallback")
    @Retry(name = "paymentService", fallbackMethod = "retryFallback")
    @Bulkhead(name = "paymentService", fallbackMethod = "bulkheadFallback")
    @TimeLimiter(name = "paymentService")
    public CompletableFuture<String> processPayment(Long orderId, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            return restTemplate.postForObject(
                "http://payment-service/api/payments",
                request, String.class
            );
        });
    }

    // Har annotation ka apna fallback method
    public CompletableFuture<String> circuitFallback(Long orderId, double amount, Throwable t) {
        return CompletableFuture.completedFuture("Circuit open: " + t.getMessage());
    }

    public CompletableFuture<String> retryFallback(Long orderId, double amount, Throwable t) {
        return CompletableFuture.completedFuture("All retries exhausted: " + t.getMessage());
    }

    public CompletableFuture<String> bulkheadFallback(Long orderId, double amount, Throwable t) {
        return CompletableFuture.completedFuture("Bulkhead full: " + t.getMessage());
    }
}
```

### Fallback Method Signature Rules

```java
// ORIGINAL method:
public ReturnType methodName(Param1 p1, Param2 p2, ...) { ... }

// FALLBACK method MUST be:
public ReturnType methodName(Param1 p1, Param2 p2, ..., Throwable t) { ... }

RULES:
1. Same method name
2. Same parameters (exact same types, same order)
3. SAME return type
4. ONE extra parameter at the end: Throwable
5. Can be in same class OR parent class
```

---

## Combining Multiple Patterns

Production mein tumhe multiple patterns combine karne padenge:

### Recommended Stack

```
+-----------------------------------------------------------+
|                    Resilience Stack                       |
|                                                           |
|  @RateLimiter     -> Rate control (entry gate)            |
|  @Bulkhead        -> Thread isolation (compartment)       |
|  @CircuitBreaker  -> Failure protection (safety switch)   |
|  @Retry           -> Transient error recovery (auto-fix)  |
|  @TimeLimiter     -> Timeout guard (max wait)             |
|  Fallback         -> Graceful degradation (backup plan)   |
+-----------------------------------------------------------+
```

### Complete Example — Production-Grade Service

```java
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RobustPaymentService {

    private static final Logger log = LoggerFactory.getLogger(RobustPaymentService.class);

    @RateLimiter(name = "paymentRateLimit", fallbackMethod = "rateLimitFallback")
    @Bulkhead(name = "paymentBulkhead", fallbackMethod = "bulkheadFallback")
    @CircuitBreaker(name = "paymentService", fallbackMethod = "circuitBreakerFallback")
    @Retry(name = "paymentRetry", fallbackMethod = "retryFallback")
    public PaymentResult processPayment(PaymentRequest request) {
        log.info("Processing payment for order: {}", request.getOrderId());

        PaymentResult result = restTemplate.postForObject(
            "http://payment-service/api/payments",
            request, PaymentResult.class
        );

        log.info("Payment successful for order: {}", request.getOrderId());
        return result;
    }

    // Rate limiter fallback
    public PaymentResult rateLimitFallback(PaymentRequest request, Throwable t) {
        log.warn("Rate limited for order: {}", request.getOrderId());
        return PaymentResult.rateLimited("Too many requests, try later");
    }

    // Bulkhead fallback
    public PaymentResult bulkheadFallback(PaymentRequest request, Throwable t) {
        log.warn("Bulkhead full for order: {}", request.getOrderId());
        return PaymentResult.busy("Payment service is busy");
    }

    // Circuit breaker fallback
    public PaymentResult circuitBreakerFallback(PaymentRequest request, Throwable t) {
        log.error("Circuit open for order: {}. Error: {}", request.getOrderId(), t.getMessage());
        return PaymentResult.unavailable("Payment service temporarily unavailable");
    }

    // Retry fallback (last resort before returning)
    public PaymentResult retryFallback(PaymentRequest request, Throwable t) {
        log.error("All retries exhausted for order: {}. Error: {}",
            request.getOrderId(), t.getMessage());
        return PaymentResult.failed("Payment processing failed after retries");
    }
}
```

---

## Comparison Table

| Pattern | Problem It Solves | When to Use | Key Config |
|---------|------------------|-------------|------------|
| **Circuit Breaker** | Repeated failures | Service consistently failing | failure-rate-threshold, wait-duration |
| **Retry** | Transient failures | Network glitches, timeouts | max-attempts, wait-duration, backoff |
| **Bulkhead** | Resource exhaustion | Shared thread pool overload | max-concurrent-calls |
| **Rate Limiter** | Overwhelming traffic | High traffic, DoS protection | limit-for-period, refresh-period |
| **TimeLimiter** | Blocking forever | Slow/unresponsive services | timeout-duration |
| **Fallback** | No response at all | Any failure scenario | Graceful degradation strategy |

---

## Best Practices

### 1. Circuit Breaker Thresholds — Start Conservative

```yaml
# BAD — Too aggressive, circuit jaldi open hoga
failure-rate-threshold: 10     # 10% failures pe trip? Too sensitive!

# GOOD — Balanced
failure-rate-threshold: 50     # 50% failures pe trip

# GOOD — Conservative for critical services
failure-rate-threshold: 30     # Critical service pe early warning
```

### 2. Retry — Only for Safe Operations

```java
// SAFE to retry:
- GET requests (idempotent)
- Network timeouts
- HTTP 503 errors

// DANGEROUS to retry:
- POST without idempotency key (duplicate charges!)
- Validation errors
- Business logic errors
- HTTP 400 (client error — retry se fix nahi hoga)
```

### 3. Bulkhead — Size According to Capacity

```yaml
# Payment service ke liye bulkhead sizing:
# Agar payment-service max 50 concurrent requests handle kar sakta hai:

paymentBulkhead:
  max-concurrent-calls: 50       # Service capacity ke barabar
  max-wait-duration: 1000ms      # Caller thoda wait kar sakta hai
```

### 4. Timeout — Network Timeout + Buffer

```yaml
# Agar average response time 500ms hai:

timelimiter:
  timeout-duration: 2s           # 500ms * 4 = safe buffer

# RestTemplate (lower level):
factory.setConnectTimeout(3000)  # 3s connection timeout
factory.setReadTimeout(5000)     # 5s read timeout
```

### 5. Fallback — Meaningful Responses Do

```java
// BAD — Empty fallback
public String fallback(Throwable t) {
    return null;  // Caller ko NullPointerException milega
}

// GOOD — Informative fallback
public String fallback(Long orderId, Throwable t) {
    return new PaymentResponse(
        orderId,
        "TEMPORARILY_UNAVAILABLE",
        "Our payment service is experiencing high load. "
        + "Your order is saved. Please retry in a few minutes."
    );
}
```

### 6. Monitoring is Mandatory

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,circuitbreakers
  endpoint:
    circuitbreakers:
      enabled: true

# Grafana/Prometheus dashboards pe dekho:
# - Circuit breaker state transitions
# - Retry attempt counts
# - Bulkhead utilization
# - Rate limiter rejection rates
```

### 7. Test Your Resilience

```java
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ResilienceTest {

    @Autowired
    private CircuitBreakerRegistry registry;

    @Test
    void circuitBreakerShouldOpenAfterFailures() {
        CircuitBreaker cb = registry.circuitBreaker("paymentService");

        // Manually record failures
        for (int i = 0; i < 10; i++) {
            cb.onError(0, new RuntimeException("Connection refused"));
        }

        // Verify circuit is OPEN
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
    }
}
```

### 8. Use Chaos Engineering

```
Production mein intentionally failures inject karo
taaki resilience patterns ka test ho sake:

Tools:
- Chaos Monkey (Netflix)
- Chaos Mesh
- Gremlin

What to test:
- Kill random service instances
- Add network latency
- Simulate circuit breaker trips
- Verify fallback responses
```

---

## Common Interview Questions

**Q1: Circuit Breaker ke states explain karo?**
A: CLOSED (normal), OPEN (failing, no requests), HALF_OPEN (testing, few requests). Failure threshold cross hone pe CLOSED -> OPEN. waitDuration baad OPEN -> HALF_OPEN. Success pe HALF_OPEN -> CLOSED, failure pe HALF_OPEN -> OPEN.

**Q2: Retry aur Circuit Breaker mein kya difference hai?**
A: Retry transient (temporary) failures ke liye hai — ek call fail hui toh dobara try karo. Circuit Breaker persistent failures ke liye hai — service baar-baar fail ho rahi hai toh usko call karna band karo. Retry = optimistic (try again). Circuit Breaker = pessimistic (stop calling).

**Q3: Semaphore vs ThreadPool Bulkhead?**
A: Semaphore same thread pool mein concurrent calls limit karta hai (lightweight). ThreadPool alag thread pool deta hai (better isolation but more overhead).

**Q4: Rate Limiter vs Bulkhead?**
A: Rate Limiter = time-based (requests per second). Bulkhead = concurrency-based (max parallel calls). Rate Limiter overall traffic control karta hai, Bulkhead resource isolation karta hai.

**Q5: Fallback method ka signature kaisa hona chahiye?**
A: Same return type, same parameters (original method ke), plus ek extra `Throwable` parameter at the end.

**Q6: Resilience4j vs Hystrix?**
A: Netflix Hystrix deprecated hai (2018 se). Resilience4j modern replacement hai — functional, lightweight, Spring Boot 3 compatible.

---

> **Summary:** Microservices mein failures guaranteed hain. Inhe handle karne ke liye:
> - **Circuit Breaker** — Repeated failures pe stop calling
> - **Retry** — Temporary failures pe try again
> - **Bulkhead** — Thread pool isolate karo
> - **Rate Limiter** — Traffic control karo
> - **Timeout** — Max wait time define karo
> - **Fallback** — Graceful degradation do
>
> **Previous:** [`04-Service-Discovery.md`](./04-Service-Discovery.md)

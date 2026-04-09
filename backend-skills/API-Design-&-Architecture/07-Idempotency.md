# Idempotency

## Status: Not Started

---

## Table of Contents

1. [What is Idempotency?](#what-is-idempotency)
2. [Idempotent Operations](#idempotent-operations)
3. [Idempotency-Key Header Pattern](#idempotency-key-header-pattern)
4. [Idempotency in Payment APIs](#idempotency-in-payment-apis)
5. [Database-Level Idempotency](#database-level-idempotency)
6. [Duplicate Request Detection](#duplicate-request-detection)

---

## What is Idempotency?

**Matlab:** Ek operation ko kitni bhi baar repeat karo, result wahi aana chahiye jo pehli baar aaya tha.

**Mathematical Example:**
```
abs(abs(abs(-5))) = 5    ✅ Idempotent (result same rehta hai)
x + 1 + 1 + 1 = x + 3    ❌ NOT idempotent (har baar result badalta hai)
x × 1 × 1 × 1 = x        ✅ Idempotent
```

**API Example:**
```
GET /api/users/1        ✅ Idempotent (kitni baar bhi call karo, same data aayega)
PUT /api/users/1        ✅ Idempotent (same body bhejo, same result)
DELETE /api/users/1     ✅ Idempotent (pehli baar delete, baaki baar 404 - but state same)
POST /api/users         ❌ NOT idempotent (har baar naya user create hoga)
```

### Why Idempotency Matters?

**Network unreliable hai:**
- Request timeout ho sakta hai
- Client retry kar sakta hai
- Network duplicate request bhej sakta hai

**Without Idempotency:**
```
Client: POST /api/payments { amount: 100 }
Server: Processing... (200 OK sent, but network timeout)
Client: (Timeout!) Retry karta hai
Server: POST /api/payments { amount: 100 }
Server: Ek aur payment process kar diya!

Result: User se 200 deduct ho gaya instead of 100! 💸
```

**With Idempotency:**
```
Client: POST /api/payments { amount: 100, idempotency_key: "abc123" }
Server: Processing... (200 OK sent, but network timeout)
Client: (Timeout!) Retry karta hai with same idempotency_key
Server: "abc123" already processed! Returning cached response.

Result: Sirf 100 deduct hua ✅
```

---

## Idempotent Operations

### HTTP Methods aur Idempotency

| Method | Idempotent? | Explanation |
|--------|-------------|-------------|
| **GET** | ✅ Yes | Data fetch - koi change nahi |
| **HEAD** | ✅ Yes | GET jaisa, sirf headers |
| **PUT** | ✅ Yes | Same resource ko same data se update |
| **DELETE** | ✅ Yes | Pehli baar delete, baaki baar already deleted |
| **PATCH** | ⚠️ Depends | Agar operation idempotent hai toh yes |
| **POST** | ❌ No | Har baar naya resource create |

### PUT Idempotent Kyun Hai?
```
PUT /api/users/1 { name: "John" }
PUT /api/users/1 { name: "John" }  ← Same request
PUT /api/users/1 { name: "John" }  ← Same request

Result: User ka naam "John" hi rahega (no matter how many times)
```

### DELETE Idempotent Kyun Hai?
```
DELETE /api/users/1  → 200 OK (User deleted)
DELETE /api/users/1  → 404 (Already deleted - but state same hai)
DELETE /api/users/1  → 404 (Already deleted)

Result: User deleted hi hai (consistent state)
```

### POST Non-Idempotent Kyun Hai?
```
POST /api/payments { amount: 100 }  → Payment 1 created
POST /api/payments { amount: 100 }  → Payment 2 created (duplicate!)
POST /api/payments { amount: 100 }  → Payment 3 created (duplicate!)

Result: 3 payments instead of 1! ❌
```

### Idempotent POST Banana

**Solution:** `Idempotency-Key` header use karo.

```
POST /api/payments
Idempotency-Key: "unique-key-123"
{ amount: 100, currency: "USD" }
```

Server logic:
```
1. Check if "unique-key-123" already exists
2. If YES → Return cached response (no reprocessing)
3. If NO → Process payment, store result with key
```

---

## Idempotency-Key Header Pattern

### How It Works

```
Client generates unique key
       ↓
Request + Idempotency-Key → Server
       ↓
Server checks if key exists
       ↓
   YES → Return cached response
   NO  → Process, store result with key
       ↓
Response (with cached result for retries)
```

### Client-Side Implementation

```java
@RestController
public class PaymentController {
    
    private final IdempotencyService idempotencyService;
    private final PaymentService paymentService;
    
    @PostMapping("/api/payments")
    public ResponseEntity<PaymentResponse> createPayment(
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @RequestBody PaymentRequest request
    ) {
        // Idempotency key nahi hai toh generate karo
        if (idempotencyKey == null) {
            idempotencyKey = UUID.randomUUID().toString();
        }
        
        // Check if already processed
        PaymentResponse cached = idempotencyService.getCachedResponse(idempotencyKey);
        if (cached != null) {
            return ResponseEntity.ok(cached);  // Cached response return karo
        }
        
        // Process payment
        PaymentResponse response = paymentService.processPayment(request);
        
        // Cache the response
        idempotencyService.cacheResponse(idempotencyKey, response);
        
        return ResponseEntity.status(201).body(response);
    }
}
```

### Idempotency Service

```java
@Service
public class IdempotencyService {
    
    private final IdempotencyRepository idempotencyRepository;
    
    // TTL: 24 hours (keys auto-expire)
    private static final Duration TTL = Duration.ofHours(24);
    
    public PaymentResponse getCachedResponse(String key) {
        return idempotencyRepository.findById(key)
            .map(IdempotencyRecord::getResponse)
            .orElse(null);
    }
    
    public void cacheResponse(String key, PaymentResponse response) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(key);
        record.setResponse(response);
        record.setCreatedAt(Instant.now());
        record.setExpiresAt(Instant.now().plus(TTL));
        
        idempotencyRepository.save(record);
    }
}
```

### Idempotency Record (Entity)

```java
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyRecord {
    
    @Id
    private String idempotencyKey;
    
    @Column(columnDefinition = "json")
    private String response;  // JSON serialized response
    
    private Instant createdAt;
    private Instant expiresAt;
    
    // Getters and setters
}
```

### Idempotency Repository

```java
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, String> {
    
    // Auto-expire old records (run via @Scheduled)
    @Modifying
    @Query("DELETE FROM IdempotencyRecord r WHERE r.expiresAt < :now")
    void deleteExpired(@Param("now") Instant now);
}
```

### Generating Idempotency Key (Client-Side)

```java
public class PaymentClient {
    
    public PaymentResponse createPayment(PaymentRequest request) {
        // Unique key generate karo
        String idempotencyKey = UUID.randomUUID().toString();
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", idempotencyKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<PaymentRequest> entity = new HttpEntity<>(request, headers);
        
        try {
            ResponseEntity<PaymentResponse> response = restTemplate.exchange(
                "/api/payments",
                HttpMethod.POST,
                entity,
                PaymentResponse.class
            );
            return response.getBody();
        } catch (ResourceAccessException e) {
            // Timeout! Retry with same key
            return retryWithSameKey(entity);
        }
    }
    
    private PaymentResponse retryWithSameKey(HttpEntity<PaymentRequest> entity) {
        // Same Idempotency-Key ke saath retry
        return restTemplate.exchange(
            "/api/payments",
            HttpMethod.POST,
            entity,
            PaymentResponse.class
        ).getBody();
    }
}
```

### Idempotency Key Best Practices

| Rule | Detail |
|------|--------|
| **Key Format** | UUID v4 recommended (`550e8400-e29b-41d4-a716-446655440000`) |
| **Key Scope** | Per-user ya per-account (ek user ka key dusre user se conflict nahi karega) |
| **TTL** | 24 hours to 7 days (business logic pe depend) |
| **Storage** | Database ya Redis (distributed systems mein) |
| **Response Cache** | Sirf success responses cache karo, errors ko nahi |

---

## Idempotency in Payment APIs

Payment APIs mein idempotency sabse zyada critical hai - double charge = unhappy customer.

### Payment Flow Without Idempotency (Problem)

```
User: ₹1000 pay karna chahta hai
Client: POST /api/pay { amount: 1000 }
Server: Processing... (bank se baat kar raha hai)
Bank: ₹1000 debited ✅
Server: Response send kiya... (NETWORK TIMEOUT!)
Client: Timeout! Retry karta hai
Server: POST /api/pay { amount: 1000 } (duplicate request)
Bank: ₹1000 aur debited! ❌

Result: User se ₹2000 deduct ho gaya!
```

### Payment Flow With Idempotency (Solution)

```
User: ₹1000 pay karna chahta hai
Client: POST /api/pay { amount: 1000 }
        Idempotency-Key: "pay_abc123"
Server: "pay_abc123" not found → Process karo
Bank: ₹1000 debited ✅
Server: Response cache kiya with key "pay_abc123"
        Response send kiya... (NETWORK TIMEOUT!)
Client: Timeout! Retry with same key "pay_abc123"
Server: "pay_abc123" already processed! Returning cached response.

Result: Sirf ₹1000 deduct hua ✅
```

### Stripe-Style Idempotency

Stripe ka pattern industry standard hai:

```http
POST /v1/payments
Idempotency-Key: pay_abc123
Authorization: Bearer sk_test_xxx

{
  "amount": 1000,
  "currency": "INR",
  "source": "tok_visa"
}
```

**Response:**
```http
HTTP/1.1 200 OK
Idempotency-Key: pay_abc123
Stripe-Should-Retry: false

{
  "id": "pay_xyz789",
  "amount": 1000,
  "status": "succeeded",
  "created": 1704067200
}
```

### Payment States aur Idempotency

```java
public enum PaymentStatus {
    PENDING,    // Processing started
    SUCCEEDED,  // Payment complete
    FAILED,     // Payment failed
    REFUNDED    // Refunded
}

@Entity
@Table(name = "payments")
public class Payment {
    
    @Id
    @GeneratedValue
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String idempotencyKey;  // Unique constraint
    
    private BigDecimal amount;
    private String currency;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    
    private String transactionId;  // Bank ka reference
    
    private Instant createdAt;
    private Instant updatedAt;
}
```

### Payment Service with Idempotency

```java
@Service
@Transactional
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final BankGateway bankGateway;
    
    public PaymentResponse processPayment(PaymentRequest request, String idempotencyKey) {
        // Check if already exists
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        
        if (existing.isPresent()) {
            Payment payment = existing.get();
            
            // Agar payment already succeeded hai toh wahi response return karo
            if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
                return toResponse(payment);
            }
            
            // Agar payment pending hai toh status check karo
            if (payment.getStatus() == PaymentStatus.PENDING) {
                // Bank se status poll karo
                return pollPaymentStatus(payment);
            }
        }
        
        // Naya payment create karo
        Payment payment = new Payment();
        payment.setIdempotencyKey(idempotencyKey);
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setStatus(PaymentStatus.PENDING);
        
        payment = paymentRepository.save(payment);
        
        // Bank se process karo
        try {
            String transactionId = bankGateway.charge(
                request.getAmount(),
                request.getSource()
            );
            
            payment.setTransactionId(transactionId);
            payment.setStatus(PaymentStatus.SUCCEEDED);
            
        } catch (BankException e) {
            payment.setStatus(PaymentStatus.FAILED);
            throw new PaymentFailedException(e.getMessage());
        }
        
        paymentRepository.save(payment);
        return toResponse(payment);
    }
    
    private PaymentResponse pollPaymentStatus(Payment payment) {
        // Bank se current status check karo
        String status = bankGateway.getTransactionStatus(payment.getTransactionId());
        
        if ("SUCCESS".equals(status)) {
            payment.setStatus(PaymentStatus.SUCCEEDED);
        } else if ("FAILED".equals(status)) {
            payment.setStatus(PaymentStatus.FAILED);
        }
        
        paymentRepository.save(payment);
        return toResponse(payment);
    }
}
```

---

## Database-Level Idempotency

Application-level idempotency ke alawa, database level pe bhi protection chahiye.

### Unique Constraint

```sql
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,  -- Unique constraint
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);
```

**Benefit:** Agar application logic fail ho gaya aur duplicate request aaya, database reject kar dega.

```sql
-- First request
INSERT INTO payments (idempotency_key, amount, currency, status)
VALUES ('pay_abc123', 1000, 'INR', 'PENDING');
-- ✅ Success

-- Duplicate request (same idempotency_key)
INSERT INTO payments (idempotency_key, amount, currency, status)
VALUES ('pay_abc123', 1000, 'INR', 'PENDING');
-- ❌ ERROR: duplicate key value violates unique constraint
```

### INSERT ON CONFLICT (Upsert)

**PostgreSQL:**
```sql
INSERT INTO payments (idempotency_key, amount, currency, status)
VALUES ('pay_abc123', 1000, 'INR', 'PENDING')
ON CONFLICT (idempotency_key)
DO NOTHING;  -- Ya DO UPDATE SET status = EXCLUDED.status
```

**MySQL:**
```sql
INSERT INTO payments (idempotency_key, amount, currency, status)
VALUES ('pay_abc123', 1000, 'INR', 'PENDING')
ON DUPLICATE KEY UPDATE status = VALUES(status);
```

### JPA Implementation

```java
@Service
public class IdempotentPaymentService {
    
    private final PaymentRepository paymentRepository;
    
    @Transactional
    public PaymentResponse createPayment(PaymentRequest request, String idempotencyKey) {
        try {
            Payment payment = new Payment();
            payment.setIdempotencyKey(idempotencyKey);
            payment.setAmount(request.getAmount());
            payment.setCurrency(request.getCurrency());
            payment.setStatus(PaymentStatus.PENDING);
            
            payment = paymentRepository.save(payment);
            
            // Process payment
            processBankTransaction(payment);
            
            payment.setStatus(PaymentStatus.SUCCEEDED);
            paymentRepository.save(payment);
            
            return toResponse(payment);
            
        } catch (DataIntegrityViolationException e) {
            // Unique constraint violated - duplicate request
            Payment existing = paymentRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow();
            return toResponse(existing);
        }
    }
}
```

### Optimistic Locking (Version Column)

```java
@Entity
public class Payment {
    
    @Id
    private Long id;
    
    @Version
    private Long version;  // Optimistic locking
    
    private String status;
}
```

**Scenario:** Two requests same time pe process ho rahe hain.

```
Request 1: Read payment (version = 1)
Request 2: Read payment (version = 1)
Request 1: Update payment SET status = 'SUCCEEDED', version = 2 WHERE version = 1
           ✅ Success
Request 2: Update payment SET status = 'SUCCEEDED', version = 2 WHERE version = 1
           ❌ OptimisticLockException (version mismatch)
```

---

## Duplicate Request Detection

### Detection Strategies

| Strategy | How It Works | Best For |
|----------|-------------|----------|
| **Idempotency Key** | Client unique key bhejta hai | Payment APIs, critical operations |
| **Request Fingerprint** | Request body hash (MD5/SHA256) calculate karo | Jab client key nahi bhej sakta |
| **Unique Business Key** | Order ID, Transaction ID | Domain-specific |
| **Time Window** | Same request within X seconds = duplicate | Simple deduplication |

### Request Fingerprint

```java
@Component
public class RequestFingerprinter {
    
    public String fingerprint(HttpServletRequest request, String body) {
        String content = request.getMethod() + 
                        request.getRequestURI() + 
                        body;
        
        return DigestUtils.sha256Hex(content);
    }
}
```

**Usage:**
```java
String fingerprint = fingerprinter.fingerprint(request, requestBody);
// Same request = same fingerprint

if (recentRequests.contains(fingerprint)) {
    return cachedResponse;  // Duplicate detected
}

// Process and store fingerprint
recentRequests.add(fingerprint, TTL);
```

### Time-Window Based Detection

```java
@Component
public class DuplicateDetector {
    
    // Map<Fingerprint, Timestamp>
    private final ConcurrentHashMap<String, Instant> recentRequests = new ConcurrentHashMap<>();
    
    private static final Duration WINDOW = Duration.ofSeconds(5);
    
    public boolean isDuplicate(String fingerprint) {
        Instant now = Instant.now();
        
        // Clean expired entries
        recentRequests.entrySet().removeIf(
            entry -> Duration.between(entry.getValue(), now).compareTo(WINDOW) > 0
        );
        
        // Check if fingerprint exists in window
        if (recentRequests.containsKey(fingerprint)) {
            return true;  // Duplicate
        }
        
        recentRequests.put(fingerprint, now);
        return false;
    }
}
```

### Redis-Based Duplicate Detection (Distributed)

```java
@Service
public class RedisDuplicateDetector {
    
    private final StringRedisTemplate redisTemplate;
    private static final Duration WINDOW = Duration.ofSeconds(5);
    
    public boolean isDuplicate(String idempotencyKey) {
        // SET NX (set if not exists) - atomic operation
        Boolean wasNew = redisTemplate.opsForValue()
            .setIfAbsent(idempotencyKey, "1", WINDOW);
        
        return Boolean.FALSE.equals(wasNew);  // false = duplicate
    }
}
```

### Duplicate Request Flow

```
Request arrives
       ↓
Generate fingerprint / extract Idempotency-Key
       ↓
Check in cache/DB (within time window)
       ↓
   FOUND → Duplicate detected!
       ↓
   Return cached response
   OR
   Return 409 Conflict
       ↓
   NOT FOUND → New request
       ↓
   Store fingerprint/key
       ↓
   Process request
       ↓
   Cache response
       ↓
   Return response
```

---

## Idempotency Implementation Checklist

### Application Level
- [ ] Idempotency-Key header support
- [ ] Response caching with TTL
- [ ] Retry logic with same key
- [ ] Error handling for duplicate keys

### Database Level
- [ ] Unique constraint on idempotency_key
- [ ] INSERT ON CONFLICT handling
- [ ] Transaction isolation (avoid race conditions)
- [ ] Cleanup expired keys

### Client Level
- [ ] Generate unique key per request
- [ ] Retry with same key on timeout
- [ ] Handle 409 Conflict response
- [ ] Log idempotency key for debugging

---

## Quick Reference

```java
// Idempotent HTTP Methods
GET, HEAD, PUT, DELETE  ✅
POST                    ❌ (needs Idempotency-Key)

// Idempotency-Key Pattern
@IdempotencyKey String key
→ Check if exists
→ YES: Return cached response
→ NO: Process, cache, return

// Database
UNIQUE KEY (idempotency_key)
INSERT ... ON CONFLICT DO NOTHING

// Redis Duplicate Detection
Boolean wasNew = redis.setIfAbsent(key, "1", Duration.ofSeconds(5));
return Boolean.FALSE.equals(wasNew);
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Idempotent Operations** | GET, PUT, DELETE idempotent hain; POST nahi |
| **Idempotency-Key** | Client unique key bhejta hai - server cache karta hai |
| **Payment APIs** | Double charge se bachne ke liye mandatory |
| **Unique Constraint** | Database-level protection (safety net) |
| **INSERT ON CONFLICT** | Upsert - duplicate insert handle karta hai |
| **Duplicate Detection** | Request fingerprint, time window, Redis-based |

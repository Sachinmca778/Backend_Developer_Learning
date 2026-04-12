# Transactions

## Status: Not Started

---

## Table of Contents

1. [@Transactional Overview](#transactional-overview)
2. [Class vs Method Level](#class-vs-method-level)
3. [Propagation](#propagation)
4. [Isolation Levels](#isolation-levels)
5. [rollbackFor](#rollbackfor)
6. [readOnly=true](#readonlytrue)
7. [Transaction Best Practices](#transaction-best-practices)

---

## @Transactional Overview

**Matlab:** Ek ya multiple database operations ko ek **atomic unit** mein wrap karta hai — ya toh sab operations succeed honge ya sab fail (rollback) honge.

### The Problem Without Transaction

```java
// ❌ Without @Transactional
public void transferMoney(Long fromId, Long toId, BigDecimal amount) {
    Account from = accountRepository.findById(fromId).orElseThrow();
    Account to = accountRepository.findById(toId).orElseThrow();

    from.setBalance(from.getBalance().subtract(amount));
    accountRepository.save(from);  // ✅ Success

    // ❌ Error yahan aaya!
    throw new RuntimeException("Something went wrong");

    // to account update nahi hoga — data inconsistent ho jayega!
    to.setBalance(to.getBalance().add(amount));
    accountRepository.save(to);  // ❌ Never reached
}
```

**Result:** `from` account se paisa kat gaya, lekin `to` account mein add nahi hua. 💸 Gone!

### The Solution With Transaction

```java
// ✅ With @Transactional
@Transactional
public void transferMoney(Long fromId, Long toId, BigDecimal amount) {
    Account from = accountRepository.findById(fromId).orElseThrow();
    Account to = accountRepository.findById(toId).orElseThrow();

    from.setBalance(from.getBalance().subtract(amount));
    accountRepository.save(from);

    // Error aaya toh DONO operations rollback honge
    throw new RuntimeException("Something went wrong");

    to.setBalance(to.getBalance().add(amount));
    accountRepository.save(to);
}
```

**Result:** Exception aaya → Transaction rollback → `from` account mein paisa wapas aa gaya. 💸 Safe!

### How @Transactional Works

```
@Transactional method call hota hai
    ↓
Spring proxy transaction start karta hai
    ↓
Database operations execute
    ↓
┌─────────────────────────────────────┐
│ Success → Transaction commit        │
│ Exception → Transaction rollback    │
└─────────────────────────────────────┘
    ↓
Transaction close
```

---

## Class vs Method Level

### Method Level

```java
@Service
public class UserService {

    // Sirf is method pe transaction
    @Transactional
    public void createUser(User user) {
        userRepository.save(user);
        // Additional operations...
    }

    // Is method pe transaction nahi
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}
```

### Class Level

```java
// Sab methods pe transaction apply hoga
@Service
@Transactional
public class UserService {

    // Yeh method bhi transactional hai
    public void createUser(User user) {
        userRepository.save(user);
    }

    // Yeh bhi transactional hai
    public void updateUser(User user) {
        userRepository.save(user);
    }

    // Override — is method pe alag config
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}
```

### Method vs Class — Which Wins?

```java
@Service
@Transactional  // Class level
public class UserService {

    @Transactional(readOnly = true, timeout = 5)  // Method level — this wins!
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}
```

**Rule:** Method level annotation class level ko override karta hai.

---

## Propagation

**Matlab:** Jab ek transactional method dusre transactional method ko call karta hai — toh transaction kaise behave karega.

### Propagation Types

| Propagation | Behavior | Use Case |
|-------------|----------|----------|
| **REQUIRED** (default) | Existing transaction use karo, nahi to naya banao | Most common |
| **REQUIRES_NEW** | Hamesha naya transaction banao (existing suspend karo) | Audit logging, notifications |
| **NESTED** | Existing transaction ke andar nested transaction | Partial rollback |
| **SUPPORTS** | Transaction hai to use karo, nahi to bina transaction ke | Read-only operations |
| **NOT_SUPPORTED** | Transaction mein mat chalao | Batch operations |
| **NEVER** | Transaction mein chalao toh exception throw karo | Debugging |
| **MANDATORY** | Transaction zaruri hai — nahi to exception | Internal services |

### REQUIRED (Default)

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final PaymentService paymentService;
    private final InventoryService inventoryService;

    @Transactional  // REQUIRED (default)
    public Order placeOrder(Order order) {
        // 1. Inventory check (same transaction)
        inventoryService.checkStock(order.getItems());

        // 2. Payment process (same transaction)
        paymentService.processPayment(order);

        // 3. Save order
        return orderRepository.save(order);

        // Kisi mein bhi error aaya → SAB rollback honge
    }
}

@Service
public class PaymentService {

    @Transactional  // REQUIRED → existing transaction use karega
    public void processPayment(Order order) {
        // Payment logic
    }
}
```

### REQUIRES_NEW

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final AuditService auditService;

    @Transactional  // REQUIRED
    public Order placeOrder(Order order) {
        // Order save
        Order savedOrder = orderRepository.save(order);

        // Audit log — ALAG transaction mein (rollback se bachane ke liye)
        auditService.logOrderPlacement(savedOrder);

        // Order rollback hua toh bhi audit log commit rahega
        return savedOrder;
    }
}

@Service
public class AuditService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)  // Naya transaction
    public void logOrderPlacement(Order order) {
        auditRepository.save(new AuditLog("ORDER_PLACED", order.getId()));
        // Yeh commit hoga chahe outer transaction rollback ho
    }
}
```

### Visual: REQUIRED vs REQUIRES_NEW

```
REQUIRED:
┌────────────────────────────────────────────┐
│ Transaction 1 (OrderService.placeOrder)    │
│   ↓                                        │
│   inventoryService.checkStock()            │  ← Same transaction
│   ↓                                        │
│   paymentService.processPayment()          │  ← Same transaction
│   ↓                                        │
│   auditService.log()                       │  ← Same transaction
│   ↓                                        │
│   All commit ya all rollback               │
└────────────────────────────────────────────┘

REQUIRES_NEW:
┌────────────────────────────────────────────┐
│ Transaction 1 (OrderService.placeOrder)    │
│   ↓                                        │
│   inventoryService.checkStock()            │  ← Same transaction
│   ↓                                        │
│   ┌────────────────────────────────────┐  │
│   │ Transaction 2 (AuditService.log)   │  │  ← NEW transaction
│   │   → Commit independently           │  │
│   └────────────────────────────────────┘  │
│   ↓                                        │
│   paymentService.processPayment()          │  ← Same as Transaction 1
│   ↓                                        │
│   Error → Transaction 1 rollback           │
│   But Transaction 2 (audit) still committed│
└────────────────────────────────────────────┘
```

### NESTED

```java
@Transactional
public void processBatch(List<Order> orders) {
    for (Order order : orders) {
        try {
            // Nested transaction — fail hua toh sirf yeh rollback
            processSingleOrder(order);
        } catch (Exception e) {
            // Ek order fail hua toh bhi baaki process honge
            log.error("Failed to process order: " + order.getId());
        }
    }
}

@Transactional(propagation = Propagation.NESTED)
public void processSingleOrder(Order order) {
    // Individual order logic
    // Fail hua toh sirf yeh nested transaction rollback hoga
    orderRepository.save(order);
}
```

**Note:** NESTED sirf JDBC savepoints pe kaam karta hai — JPA providers mein limited support.

### Complete Example

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final EmailService emailService;
    private final AuditService auditService;

    @Transactional  // REQUIRED (default)
    public void registerUser(User user) {
        // 1. Save user (same transaction)
        userRepository.save(user);

        // 2. Send welcome email (ALAG transaction — user save fail hua toh bhi log rahega)
        emailService.sendWelcomeEmail(user);

        // 3. Audit log (ALAG transaction — independent commit)
        auditService.logRegistration(user);

        // 4. Setup user profile (same transaction — fail hua toh user save bhi rollback)
        setupUserProfile(user);
    }
}

@Service
public class EmailService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendWelcomeEmail(User user) { }
}

@Service
public class AuditService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logRegistration(User user) { }
}
```

---

## Isolation Levels

**Matlab:** Multiple transactions ek saath chal rahi hon toh ek dusre ko kaise affect karengi.

### Isolation Levels

| Level | Dirty Read | Non-Repeatable Read | Phantom Read | Performance |
|-------|-----------|---------------------|--------------|-------------|
| **READ_UNCOMMITTED** | ✅ Possible | ✅ Possible | ✅ Possible | Fastest |
| **READ_COMMITTED** (default) | ❌ No | ✅ Possible | ✅ Possible | Fast |
| **REPEATABLE_READ** | ❌ No | ❌ No | ✅ Possible | Medium |
| **SERIALIZABLE** | ❌ No | ❌ No | ❌ No | Slowest |

### Dirty Read Example

```
Transaction 1: UPDATE users SET balance = 500 WHERE id = 1;  -- Not committed yet
Transaction 2: SELECT balance FROM users WHERE id = 1;        -- Reads 500 (dirty!)
Transaction 1: ROLLBACK;                                      -- Balance wapas 1000
Transaction 2: Ab galat value read kar liya!
```

### Non-Repeatable Read Example

```
Transaction 1: SELECT balance FROM users WHERE id = 1;  -- Reads 1000
Transaction 2: UPDATE users SET balance = 500 WHERE id = 1; COMMIT;
Transaction 1: SELECT balance FROM users WHERE id = 1;  -- Reads 500 (different!)
```

### Phantom Read Example

```
Transaction 1: SELECT COUNT(*) FROM users WHERE city = 'Delhi';  -- Returns 10
Transaction 2: INSERT INTO users (name, city) VALUES ('New', 'Delhi'); COMMIT;
Transaction 1: SELECT COUNT(*) FROM users WHERE city = 'Delhi';  -- Returns 11 (phantom!)
```

### Usage

```java
@Service
public class UserService {

    // Default isolation: READ_COMMITTED (most databases)
    @Transactional
    public User createUser(User user) {
        return userRepository.save(user);
    }

    // SERIALIZABLE — strongest isolation (but slowest)
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public User createUserStrict(User user) {
        return userRepository.save(user);
    }

    // READ_UNCOMMITTED — fastest (but dirty reads possible)
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public List<User> getAllUsersForReport() {
        return userRepository.findAll();
    }
}
```

### Recommended Isolation

| Use Case | Isolation Level |
|----------|----------------|
| **Most apps** | READ_COMMITTED (default) |
| **Financial transactions** | REPEATABLE_READ |
| **Report queries (approximate OK)** | READ_UNCOMMITTED |
| **Critical data integrity** | SERIALIZABLE |

---

## rollbackFor

**Matlab:** Default mein sirf `RuntimeException` aur `Error` pe rollback hota hai. Checked exceptions pe rollback nahi hota. `rollbackFor` se customize kar sakte ho.

### Default Behavior

```java
@Transactional
public void processOrder(Order order) {
    // ✅ RuntimeException → ROLLBACK
    throw new RuntimeException("Something went wrong");

    // ✅ Error → ROLLBACK
    throw new OutOfMemoryError();

    // ❌ Checked Exception → NO ROLLBACK (commit ho jayega!)
    throw new IOException("Database error");
}
```

### Custom rollbackFor

```java
// Checked exception pe bhi rollback karo
@Transactional(rollbackFor = IOException.class)
public void processOrder(Order order) throws IOException {
    throw new IOException("Database error");  // Ab ROLLBACK hoga
}

// Multiple exceptions
@Transactional(rollbackFor = {IOException.class, SQLException.class})
public void processOrder(Order order) throws IOException, SQLException {
    // Dono pe rollback hoga
}

// Exception + subclasses
@Transactional(rollbackFor = Exception.class)
public void processOrder(Order order) throws Exception {
    // Kisi bhi Exception pe rollback
}
```

### noRollbackFor

```java
// Specific exception pe rollback nahi karna
@Transactional(noRollbackFor = ValidationException.class)
public void processOrder(Order order) {
    throw new ValidationException("Invalid data");  // Commit hoga, rollback nahi
}
```

### Complete Example

```java
@Transactional(
    rollbackFor = {OrderProcessingException.class, PaymentException.class},
    noRollbackFor = ValidationException.class
)
public void processOrder(Order order) throws OrderProcessingException {
    // ValidationException → NO rollback
    // OrderProcessingException → ROLLBACK
    // PaymentException → ROLLBACK
}
```

---

## readOnly=true

**Matlab:** Database ko batata hai ki yeh transaction sirf read karega — koi changes nahi karega. Performance optimization.

### Usage

```java
@Service
public class UserService {

    // Read-only transaction
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    // Read-only list
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    // Write transaction (default readOnly = false)
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }
}
```

### Benefits

| Benefit | Description |
|---------|-------------|
| **Database optimization** | DB read-only lock lagata hai — writes prevent |
| **No flush** | Hibernate dirty checking skip karta hai |
| **Replica routing** | Read replicas pe route kar sakta hai |
| **Performance** | Slight performance improvement |

### ⚠️ readOnly mein Write Attempt

```java
@Transactional(readOnly = true)
public User updateUser(Long id, String name) {
    User user = userRepository.findById(id).orElseThrow();
    user.setName(name);
    return userRepository.save(user);  // ❌ Exception: could not execute statement
}
```

### Class Level readOnly

```java
@Service
@Transactional(readOnly = true)  // Sab methods read-only by default
public class UserService {

    // Yeh method read-only rahega
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    // Override — write method
    @Transactional(readOnly = false)
    public User save(User user) {
        return userRepository.save(user);
    }
}
```

---

## Transaction Best Practices

### 1. Service Layer pe Lagao

```java
// ✅ Recommended: Service layer
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public User createUser(User user) {
        return userRepository.save(user);
    }
}

// ❌ Avoid: Controller layer
@RestController
public class UserController {

    @Transactional  // Yeh nahi
    @PostMapping("/users")
    public User createUser(@RequestBody User user) { }
}
```

### 2. Self-Invocation Problem

```java
@Service
@Transactional
public class UserService {

    public void methodA() {
        // ❌ Yeh kaam nahi karega — same class ka method call
        // @Transactional annotation ignore ho jayega
        methodB();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void methodB() {
        // Naya transaction nahi banega!
    }
}
```

**Fix:**

```java
// Fix 1: Self-inject
@Service
@Transactional
public class UserService {

    @Autowired
    private UserService self;  // Self-inject

    public void methodA() {
        self.methodB();  // ✅ Proxy ke through call hoga
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void methodB() {
        // Ab naya transaction banega
    }
}
```

### 3. Transaction Timeout

```java
// 30 seconds timeout
@Transactional(timeout = 30)
public void processLargeBatch(List<Order> orders) {
    // 30 seconds mein complete nahi hua → rollback
}
```

### 4. Transaction Naming (Debugging)

```java
@Transactional("placeOrder")
public Order placeOrder(Order order) {
    // Transaction name logs mein dikhega
}
```

### 5. Exception Handling in Transactions

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final PaymentService paymentService;

    @Transactional
    public Order placeOrder(Order order) {
        try {
            paymentService.processPayment(order);
            return orderRepository.save(order);
        } catch (PaymentException e) {
            // Exception ko wrap karo RuntimeException mein — taaki rollback ho
            throw new RuntimeException("Payment failed", e);
        }
    }
}
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **@Transactional** | Multiple operations ko atomic unit mein wrap karta hai |
| **Class Level** | Sab methods pe apply hota hai |
| **Method Level** | Sirf us method pe — class level ko override karta hai |
| **REQUIRED** | Default — existing transaction use karo |
| **REQUIRES_NEW** | Hamesha naya transaction banao |
| **NESTED** | Existing ke andar nested — partial rollback possible |
| **READ_COMMITTED** | Default isolation — most apps ke liye kaafi |
| **SERIALIZABLE** | Strongest isolation — sabse slow |
| **rollbackFor** | Checked exceptions pe bhi rollback karo |
| **readOnly=true** | Performance optimization — writes nahi honge |
| **Self-Invocation** | Same class method call → @Transactional ignore hota hai |
| **Service Layer** | @Transactional hamesha service layer pe lagao |

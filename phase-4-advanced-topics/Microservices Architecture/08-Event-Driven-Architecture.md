# 08 - Event-Driven Architecture: Event Sourcing, CQRS, Outbox Pattern

> "State mat store karo, events store karo. Read-Write ko separate karo. Reliability ke liye outbox use karo."

---

## Table of Contents

1. [Why Event-Driven Architecture?](#1-why-event-driven-architecture)
2. [Event Sourcing](#2-event-sourcing)
3. [CQRS (Command Query Responsibility Segregation)](#3-cqrs-command-query-responsibility-segregation)
4. [Event Sourcing + CQRS Together](#4-event-sourcing--cqrs-together)
5. [Outbox Pattern](#5-outbox-pattern)
6. [Idempotency](#6-idempotency)
7. [At-Least-Once vs Exactly-Once Delivery](#7-at-least-once-vs-exactly-once-delivery)
8. [Complete Implementation Example](#8-complete-implementation-example)
9. [Best Practices](#9-best-practices)
10. [When to Use What](#10-when-to-use-what)

---

## 1. Why Event-Driven Architecture?

Traditional architecture mein services **synchronously communicate** karti hain. Event-driven architecture mein services **events ke through** communicate karti hain.

```
Traditional (Synchronous):
Client ──► Service A ──► Service B ──► Service C
              │              │             │
              └── WAIT ──────┴── WAIT ─────┘
              
  Problem: Sab service available honi chahiye
           Ek slow = sab slow
           Tight coupling

Event-Driven (Asynchronous):
Client ──► Service A ──► [Event Bus]
                            │
                     ┌──────┼──────┐
                     ▼      ▼      ▼
                  Service B  C    D
                  (whenever) (if) (when)
                  
  Benefit: Loose coupling
           Better scalability
           Natural audit trail
           Easy to add new consumers
```

---

## 2. Event Sourcing

### Current State vs Event Sourcing

```
Traditional (Current State):
┌────────────────────────────┐
│  Account Table              │
├────────┬───────────────────┤
│ id     │ 1                 │
│ balance│ 750               │  ← Sirf current value pata hai
│        │                   │     History pata nahi!
└────────┴───────────────────┘

Event Sourcing:
┌────────────────────────────────────────────┐
│  Events Table                               │
├────┬────────────────┬───────┬──────────────┤
│ seq│ event_type     │amount │  timestamp   │
├────┼────────────────┼───────┼──────────────┤
│ 1  │ AccountCreated │       │ 2024-01-01   │
│ 2  │ MoneyDeposited │ +1000 │ 2024-01-05   │
│ 3  │ MoneyWithdrawn │ -200  │ 2024-01-10   │
│ 4  │ MoneyWithdrawn │ -50   │ 2024-01-15   │
└────┴────────────────┴───────┴──────────────┘

Current State = Replay all events
Balance = 0 + 1000 - 200 - 50 = 750

History bhi preserved hai! Time-travel possible hai!
```

### Event Sourcing kya hai?

> **Event Sourcing** mein hum **current state store nahi karte**. Sirf **events store karte hain**. Current state ko events se **reconstruct** karte hain.

```
CRUD Approach:
┌───────────────────────────────────┐
│  INSERT INTO accounts ...         │  ← Data overwrite hota hai
│  UPDATE accounts SET balance=...  │  ← Purana data gayab
│  DELETE FROM accounts ...         │  ← History lost
└───────────────────────────────────┘

Event Sourcing Approach:
┌───────────────────────────────────┐
│  APPEND AccountCreatedEvent       │  ← Sirf append, kabhi delete nahi
│  APPEND MoneyDepositedEvent       │  ← History preserved
│  APPEND MoneyWithdrawnEvent       │  ← Full audit trail built-in
└───────────────────────────────────┘
```

### Implementation:

#### Step 1: Event Definitions

```java
// Base Event interface
public interface DomainEvent {
    String getAggregateId();
    Instant getTimestamp();
    int getVersion();
}

// Concrete Events
@Data
@AllArgsConstructor
public class AccountCreatedEvent implements DomainEvent {
    private String accountId;
    private String ownerName;
    private Instant timestamp;
    private final int version = 1;
}

@Data
@AllArgsConstructor
public class MoneyDepositedEvent implements DomainEvent {
    private String accountId;
    private BigDecimal amount;
    private String transactionRef;
    private Instant timestamp;
    private final int version = 1;
}

@Data
@AllArgsConstructor
public class MoneyWithdrawnEvent implements DomainEvent {
    private String accountId;
    private BigDecimal amount;
    String transactionRef;
    private Instant timestamp;
    private final int version = 1;
}
```

#### Step 2: Event Store

```java
@Entity
@Table(name = "domain_events")
@Data
public class StoredEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String aggregateType;  // "Account", "Order", etc.

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String eventType;      // Fully qualified class name

    @Column(nullable = false, columnDefinition = "JSON")
    private String eventData;      // JSON serialized event

    @Column(nullable = false)
    private Instant timestamp;

    // Custom constructor
    public static StoredEvent from(DomainEvent event, String aggregateType) {
        StoredEvent stored = new StoredEvent();
        stored.aggregateType = aggregateType;
        stored.aggregateId = event.getAggregateId();
        stored.version = event.getVersion();
        stored.eventType = event.getClass().getName();
        stored.eventData = serializeToJson(event);
        stored.timestamp = event.getTimestamp();
        return stored;
    }
}

public interface EventStoreRepository extends JpaRepository<StoredEvent, Long> {
    List<StoredEvent> findByAggregateIdOrderByVersionAsc(String aggregateId);
    int countByAggregateId(String aggregateId);
}
```

#### Step 3: Aggregate Root (Account)

```java
@Getter
public class Account {

    private String id;
    private String ownerName;
    private BigDecimal balance;
    private int version;
    private List<DomainEvent> uncommittedEvents = new ArrayList<>();

    // Factory method
    public static Account create(String id, String ownerName) {
        Account account = new Account();
        account.id = id;
        account.version = 0;
        account.balance = BigDecimal.ZERO;

        // Event apply karo
        AccountCreatedEvent event = new AccountCreatedEvent(
            id, ownerName, Instant.now()
        );
        account.applyEvent(event);
        account.uncommittedEvents.add(event);

        return account;
    }

    // Commands
    public void deposit(BigDecimal amount, String transactionRef) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        MoneyDepositedEvent event = new MoneyDepositedEvent(
            id, amount, transactionRef, Instant.now()
        );
        applyEvent(event);
        uncommittedEvents.add(event);
    }

    public void withdraw(BigDecimal amount, String transactionRef) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                "Balance: " + balance + ", Requested: " + amount);
        }

        MoneyWithdrawnEvent event = new MoneyWithdrawnEvent(
            id, amount, transactionRef, Instant.now()
        );
        applyEvent(event);
        uncommittedEvents.add(event);
    }

    // Event handler - state update hota hai
    private void applyEvent(DomainEvent event) {
        switch (event) {
            case AccountCreatedEvent e -> {
                this.ownerName = e.getOwnerName();
                this.balance = BigDecimal.ZERO;
            }
            case MoneyDepositedEvent e ->
                this.balance = this.balance.add(e.getAmount());

            case MoneyWithdrawnEvent e ->
                this.balance = this.balance.subtract(e.getAmount());
        }
        this.version++;
    }

    // Reconstruction from events
    public static Account reconstruct(String id, List<StoredEvent> events) {
        Account account = new Account();
        account.id = id;
        account.version = 0;
        account.balance = BigDecimal.ZERO;

        for (StoredEvent stored : events) {
            DomainEvent event = deserialize(stored);
            account.applyEvent(event);
        }

        return account;
    }
}
```

#### Step 4: Event Store Service

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class EventStore {

    private final EventStoreRepository eventRepo;
    private final ApplicationEventPublisher publisher;

    // Events save karo
    @Transactional
    public void saveEvents(String aggregateType, 
                           List<DomainEvent> events) {
        for (DomainEvent event : events) {
            StoredEvent stored = StoredEvent.from(event, aggregateType);
            eventRepo.save(stored);
        }
    }

    // Events publish karo (for other services)
    public void publishEvents(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            publisher.publishEvent(event);
        }
    }

    // Events load karo for reconstruction
    public List<StoredEvent> getEvents(String aggregateId) {
        return eventRepo.findByAggregateIdOrderByVersionAsc(aggregateId);
    }
}
```

#### Step 5: Account Service (Using Event Sourcing)

```java
@Service
@RequiredArgsConstructor
public class AccountService {

    private final EventStore eventStore;
    private final EventStoreRepository eventRepo;

    // Account create karo
    public Account createAccount(String id, String ownerName) {
        Account account = Account.create(id, ownerName);
        eventStore.saveEvents("Account", account.getUncommittedEvents());
        eventStore.publishEvents(account.getUncommittedEvents());
        return account;
    }

    // Account load karo (reconstruct from events)
    public Account loadAccount(String id) {
        List<StoredEvent> events = eventStore.getEvents(id);
        if (events.isEmpty()) {
            throw new AccountNotFoundException("Account not found: " + id);
        }
        return Account.reconstruct(id, events);
    }

    // Deposit
    public Account deposit(String accountId, 
                          BigDecimal amount, 
                          String transactionRef) {
        Account account = loadAccount(accountId);
        account.deposit(amount, transactionRef);

        eventStore.saveEvents("Account", account.getUncommittedEvents());
        eventStore.publishEvents(account.getUncommittedEvents());

        return account;
    }

    // Withdraw
    public Account withdraw(String accountId,
                           BigDecimal amount,
                           String transactionRef) {
        Account account = loadAccount(accountId);
        account.withdraw(amount, transactionRef);

        eventStore.saveEvents("Account", account.getUncommittedEvents());
        eventStore.publishEvents(account.getUncommittedEvents());

        return account;
    }
}
```

### Event Sourcing Benefits:

| Benefit | Explanation |
|---------|-------------|
| **Full Audit Trail** | Har change recorded hai, koi data delete nahi hota |
| **Time Travel** | Kisi bhi point pe state reconstruct kar sakte ho |
| **Event Replay** | Events se naya projection bana sakte ho |
| **Temporal Query** | "What was balance on Jan 15?" answer kar sakte ho |
| **Debugging** | Kya hua, kab hua - sab trace ho sakta hai |
| **CQRS Compatible** | Events se multiple read models bana sakte ho |

### Event Sourcing Challenges:

| Challenge | Solution |
|-----------|----------|
| **Event Versioning** | Schema versioning + upcasters |
| **Large Event Streams** | Snapshots (periodic state save) |
| **Complex Queries** | CQRS with read projections |
| **Learning Curve** | Team training + gradual adoption |
| **Storage Size** | Event compression + archiving |

### Snapshotting (Optimization):

```
Problem: Agar 1 million events hain, toh reconstruction slow hoga

Solution: Periodic snapshots store karo

┌─────────────────────────────────────────┐
│  Snapshot Table                          │
├────┬──────────┬─────────┬────┬──────────┤
│ id │agg_type  │agg_id   │ver │ snapshot │
├────┼──────────┼─────────┼────┼──────────┤
│ 1  │ Account  │ ACC-001│1000│ {balance:5000, ...} │
└────┴──────────┴─────────┴────┴──────────┘

Reconstruction:
1. Load latest snapshot (version 1000)
2. Apply events after version 1000 (1001-1050)
3. Only 50 events replay karni hain instead of 1050!
```

```java
@Entity
@Data
public class Snapshot {
    @Id
    private String aggregateId;
    private String aggregateType;
    private int version;
    @Lob
    private byte[] snapshotData;  // Serialized state
    private Instant createdAt;
}

public Account reconstructWithSnapshot(String id) {
    // 1. Latest snapshot load karo
    Snapshot snapshot = snapshotRepo.findByAggregateId(id);

    Account account;
    int fromVersion = 0;

    if (snapshot != null) {
        account = deserializeSnapshot(snapshot);
        fromVersion = snapshot.getVersion();
    } else {
        account = new Account();
        account.id = id;
    }

    // 2. Sirf events after snapshot version apply karo
    List<StoredEvent> events = eventRepo
        .findByAggregateIdAndVersionAfterOrderByVersionAsc(id, fromVersion);

    for (StoredEvent stored : events) {
        account.applyEvent(deserialize(stored));
    }

    return account;
}
```

---

## 3. CQRS (Command Query Responsibility Segregation)

### Problem: Single Model for Everything

```
Traditional (CRUD):
┌─────────────────────────────────────┐
│           Account Service           │
│                                     │
│  createAccount()  ← Write (Command) │
│  updateAccount()  ← Write (Command) │
│  getAccount()     ← Read  (Query)   │
│  searchAccounts() ← Read  (Query)   │
│                                     │
│  Same DB table for ALL operations   │
│  Complex queries slow down writes   │
└─────────────────────────────────────┘

Problem:
- Read patterns alag hain (search, filter, join)
- Write patterns alag hain (validate, business logic)
- Ek model dono ke liye optimize nahi ho sakta
```

### CQRS Solution:

```
CQRS Architecture:
┌──────────────────────────────────────────────────────┐
│                      Commands                         │
│  ┌──────────────────────────────────────────────┐    │
│  │  Command Model (Write Side)                  │    │
│  │  - CreateOrderCommand                        │    │
│  │  - UpdateOrderCommand                        │    │
│  │  - CancelOrderCommand                        │    │
│  │                                              │    │
│  │  Business Logic, Validation                  │    │
│  │  ┌─────────────────────┐                    │    │
│  │  │   Write Database    │                    │    │
│  │  │  (Normalized, ACID) │                    │    │
│  │  └─────────┬───────────┘                    │    │
│  └────────────┼────────────────────────────────┘    │
│               │                                      │
│               │ Events / Messages                    │
│               ▼                                      │
│  ┌──────────────────────────────────────────────┐    │
│  │  Query Model (Read Side)                     │    │
│  │                                              │    │
│  │  ┌─────────────────────┐                    │    │
│  │  │   Read Database     │                    │    │
│  │  │  (Denormalized,     │                    │    │
│  │  │   optimized for     │                    │    │
│  │  │   queries)          │                    │    │
│  │  └─────────────────────┘                    │    │
│  └──────────────────────────────────────────────┘    │
│                                                       │
│                      Queries                          │
└──────────────────────────────────────────────────────┘
```

### Implementation:

#### Step 1: Command Side

```java
// Command
@Data
@Builder
public class CreateOrderCommand {
    private String orderId;
    private String customerId;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
}

// Command Handler
@Service
@RequiredArgsConstructor
public class OrderCommandHandler {

    private final OrderWriteRepository writeRepo;
    private final EventPublisher eventPublisher;

    @Transactional
    public OrderId handle(CreateOrderCommand command) {
        // Validation
        if (command.getItems().isEmpty()) {
            throw new InvalidOrderException("Order cannot be empty");
        }

        // Entity create
        Order order = new Order(
            command.getOrderId(),
            command.getCustomerId(),
            command.getItems(),
            command.getTotalAmount()
        );
        order.setStatus(OrderStatus.CREATED);

        writeRepo.save(order);

        // Event publish karo (read side update ke liye)
        eventPublisher.publish(new OrderCreatedEvent(
            order.getId(),
            order.getCustomerId(),
            order.getTotalAmount(),
            Instant.now()
        ));

        return new OrderId(order.getId());
    }
}

// Write Repository - simple, normalized
public interface OrderWriteRepository extends JpaRepository<Order, String> {
    // Minimal queries - sirf write ke liye
}
```

#### Step 2: Query Side

```java
// Projection (Read Model)
@Entity
@Table(name = "order_read_model")
@Data
public class OrderReadModel {
    @Id
    private String orderId;
    private String customerId;
    private String customerName;      // Join se populate
    private BigDecimal totalAmount;
    private String status;
    private int itemCount;
    private Instant createdAt;
    private Instant updatedAt;

    // Denormalized fields for fast queries
    private String searchKeywords;    // Full-text search ke liye
    private String category;          // Easy filtering
}

// Event Handler - Read Model update karta hai
@Component
@RequiredArgsConstructor
public class OrderProjectionHandler {

    private final OrderReadModelRepository readRepo;

    @EventListener
    @Transactional
    public void handle(OrderCreatedEvent event) {
        OrderReadModel model = new OrderReadModel();
        model.setOrderId(event.getOrderId());
        model.setCustomerId(event.getCustomerId());
        model.setTotalAmount(event.getTotalAmount());
        model.setStatus("CREATED");
        model.setCreatedAt(Instant.now());
        readRepo.save(model);
    }

    @EventListener
    @Transactional
    public void handle(OrderStatusChangedEvent event) {
        OrderReadModel model = readRepo.findById(event.getOrderId())
            .orElseThrow(() -> new OrderNotFoundException("Not found"));
        model.setStatus(event.getNewStatus());
        model.setUpdatedAt(Instant.now());
        readRepo.save(model);
    }
}

// Read Repository - complex queries ke liye optimized
public interface OrderReadModelRepository 
        extends JpaRepository<OrderReadModel, String> {

    // Complex queries bina performance issue ke
    Page<OrderReadModel> findByCustomerIdAndStatus(
        String customerId, String status, Pageable pageable);

    @Query("SELECT o FROM OrderReadModel o WHERE " +
           "o.totalAmount BETWEEN :min AND :max AND " +
           "o.status = :status")
    List<OrderReadModel> findByAmountRangeAndStatus(
        BigDecimal min, BigDecimal max, String status);

    // Full-text search
    @Query("SELECT o FROM OrderReadModel o WHERE " +
           "o.searchKeywords LIKE %:keyword%")
    Page<OrderReadModel> searchOrders(
        String keyword, Pageable pageable);
}
```

#### Step 3: Query Service

```java
@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderReadModelRepository readRepo;

    // Fast queries - read model se
    public OrderDto getOrder(String orderId) {
        OrderReadModel model = readRepo.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException("Not found"));
        return toDto(model);
    }

    public Page<OrderDto> getOrdersByCustomer(String customerId,
                                               Pageable pageable) {
        return readRepo.findByCustomerIdAndStatus(
                customerId, "ACTIVE", pageable)
            .map(this::toDto);
    }

    public OrderSummaryDto getCustomerSummary(String customerId) {
        // Complex aggregation - read model mein easy hai
        return readRepo.findCustomerSummary(customerId);
    }
}
```

### CQRS vs CRUD Comparison:

| Aspect | CRUD (Traditional) | CQRS |
|--------|-------------------|------|
| **Model** | Single model for read+write | Separate models |
| **Database** | Same DB for everything | Can use different DBs |
| **Optimization** | Compromise between read & write | Each optimized independently |
| **Complexity** | Simple | More complex |
| **Scalability** | Limited | Read & write scale independently |
| **Best For** | Simple apps | Complex domains, high read/write ratio |

### CQRS Kab Use Karein?

```
CQRS USE karo jab:
├── Read/Write ratio bahut alag hai (e.g., 100:1)
├── Complex read queries (search, aggregation, reporting)
├── Different teams for read/write
├── Event Sourcing ke saath use karna hai
└── Multiple read models chahiye (UI, API, Analytics)

CQRS MAT karo jab:
├── Simple CRUD application
├── Small team, tight deadline
├── Consistency requirements bahut strict hain
└── Team ko CQRS/Event-driven experience nahi hai
```

---

## 4. Event Sourcing + CQRS Together

Ye dono patterns ek saath bahut powerful hain:

```
┌────────────────────────────────────────────────────────────┐
│                    Commands                                  │
│                      │                                       │
│                      ▼                                       │
│  ┌──────────────────────────────────────────────┐           │
│  │          Command Side                         │           │
│  │                                               │           │
│  │  ┌──────────────┐    ┌──────────────┐        │           │
│  │  │ Command      │───►│  Aggregate   │        │           │
│  │  │ Handler      │    │  (Business   │        │           │
│  │  │              │    │   Logic)     │        │           │
│  │  └──────────────┘    └──────┬───────┘        │           │
│  │                              │                 │           │
│  │                              ▼                 │           │
│  │                    ┌──────────────┐            │           │
│  │                    │ Event Store  │            │           │
│  │                    │ (Append Only)│            │           │
│  │                    └──────┬───────┘            │           │
│  └───────────────────────────┼────────────────────┘           │
│                              │ Events                          │
│                              ▼                                 │
│  ┌──────────────────────────────────────────────┐           │
│  │          Query Side                           │           │
│  │                                               │           │
│  │  ┌──────────────┐    ┌──────────────┐        │           │
│  │  │ Event        │───►│ Read Model   │        │           │
│  │  │ Handlers     │    │ Projections  │        │           │
│  │  │              │    │ (Multiple)   │        │           │
│  │  └──────────────┘    └──────┬───────┘        │           │
│  └─────────────────────────────┼────────────────┘           │
│                                │                             │
│                                ▼                             │
│                         Queries                              │
└────────────────────────────────────────────────────────────┘
```

### Flow:

```
1. Command aaya (CreateOrder)
2. Command Handler business logic validate karta hai
3. Aggregate state load karta hai (Event Store se)
4. Business logic execute hota hai
5. New events generate hote hain
6. Events Event Store mein append hote hain
7. Events publish hote hain Message Broker pe
8. Event Handlers events consume karte hain
9. Read Model Projections update hote hain
10. Queries read model se serve hote hain
```

---

## 5. Outbox Pattern

### The Problem: Dual Write

```
Service A mein:
┌─────────────────────────────────────┐
│                                     │
│  1. Database UPDATE karo             │
│     UPDATE orders SET status='PAID' │
│     ✅ Committed!                    │
│                                     │
│  2. Event publish karo (Kafka)      │
│     kafka.send("order-paid", event) │
│     ❌ FAIL! Network issue!          │
│                                     │
│  Result: Database updated, but      │
│          event publish nahi hua     │
│          → Data inconsistency!      │
└─────────────────────────────────────┘

Ya fir:
┌─────────────────────────────────────┐
│                                     │
│  1. Event publish karo               │
│     ✅ Published!                    │
│                                     │
│  2. Database UPDATE karo             │
│     ❌ FAIL! DB connection lost!     │
│                                     │
│  Result: Event published, but       │
│          database update nahi hua   │
│          → Ghost event!             │
└─────────────────────────────────────┘
```

### Outbox Pattern Solution:

```
Outbox Pattern:
┌─────────────────────────────────────────────────┐
│                                                 │
│  Transaction START                              │
│    │                                            │
│    ├──► 1. Business data update karo            │
│    │      UPDATE orders SET status='PAID'       │
│    │                                            │
│    └──► 2. Event OUTBOX table mein insert karo  │
│           INSERT INTO outbox                    │
│             (event_type, payload, status)       │
│           VALUES ('OrderPaid', {...}, 'NEW')    │
│    │                                            │
│  Transaction COMMIT                             │
│    │                                            │
│    └──► DONO ya toh commit honge ya rollback    │
│         ACID guarantee! ✅                       │
│                                                 │
│  ┌───────────────────────────┐                  │
│  │  Outbox Table             │                  │
│  │  ┌───┬─────────┬────┐     │                  │
│  │  │id │payload  │stat│     │                  │
│  │  ├───┼─────────┼────┤     │                  │
│  │  │1  │{...}    │NEW │     │                  │
│  │  │2  │{...}    │NEW │     │                  │
│  │  └───┴─────────┴────┘     │                  │
│  └───────────┬───────────────┘                  │
│              │                                   │
│              ▼                                   │
│  ┌──────────────────────┐                       │
│  │  Message Relayer     │                       │
│  │  (Background Thread) │                       │
│  │                      │                       │
│  │  1. NEW events pick  │                       │
│  │  2. Kafka pe publish │                       │
│  │  3. Status = SENT    │                       │
│  └──────────────────────┘                       │
│              │                                   │
│              ▼                                   │
│         Kafka / RabbitMQ                        │
│              │                                   │
│              ▼                                   │
│         Other Services                          │
│                                                 │
└─────────────────────────────────────────────────┘
```

### Implementation:

#### Step 1: Outbox Entity

```java
@Entity
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_outbox_status", columnList = "status"),
    @Index(name = "idx_outbox_created", columnList = "createdAt")
})
@Data
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;  // JSON serialized

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant sentAt;

    public enum OutboxStatus {
        NEW,      // Abhi publish nahi hua
        SENT,     // Successfully published
        FAILED    // Publish failed after retries
    }

    // Helper method - event create karna
    public static OutboxEvent from(DomainEvent event, String eventType) {
        return OutboxEvent.builder()
            .eventType(eventType)
            .aggregateId(event.getAggregateId())
            .aggregateType(event.getClass().getSimpleName())
            .payload(serializeToJson(event))
            .status(OutboxStatus.NEW)
            .retryCount(0)
            .createdAt(Instant.now())
            .build();
    }
}
```

#### Step 2: Service with Outbox

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepo;
    private final OutboxEventRepository outboxRepo;

    @Transactional  // Dono ek transaction mein!
    public Order completeOrder(String orderId, PaymentDetails payment) {
        // 1. Business update
        Order order = orderRepo.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException("Not found"));

        order.completeOrder(payment);
        orderRepo.save(order);

        // 2. Outbox mein event save karo
        OrderCompletedEvent event = new OrderCompletedEvent(
            order.getId(),
            order.getCustomerId(),
            order.getTotalAmount(),
            Instant.now()
        );

        OutboxEvent outboxEvent = OutboxEvent.from(
            event, "OrderCompleted");
        outboxRepo.save(outboxEvent);

        // Dono commit honge ya dono rollback!
        log.info("Order completed, outbox event saved");
        return order;
    }
}
```

#### Step 3: Message Relayer (Background Publisher)

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayer {

    private final OutboxEventRepository outboxRepo;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // Har 5 seconds mein check karo
    @Scheduled(fixedDelay = 5000)
    public void relayEvents() {
        // NEW status ke events pick karo
        List<OutboxEvent> pendingEvents = outboxRepo
            .findByStatusOrderByCreatedAtAsc(
                OutboxEvent.OutboxStatus.NEW);

        for (OutboxEvent event : pendingEvents) {
            publishEvent(event);
        }
    }

    private void publishEvent(OutboxEvent event) {
        try {
            // Kafka pe publish karo
            kafkaTemplate.send(
                "outbox." + event.getEventType(),
                event.getAggregateId(),
                event.getPayload()
            ).get(5, TimeUnit.SECONDS);

            // Success - status update
            event.setStatus(OutboxEvent.OutboxStatus.SENT);
            event.setSentAt(Instant.now());
            outboxRepo.save(event);

            log.debug("Outbox event sent: {}", event.getId());

        } catch (Exception e) {
            // Failed - retry count increment
            event.setRetryCount(event.getRetryCount() + 1);

            if (event.getRetryCount() >= 5) {
                event.setStatus(OutboxEvent.OutboxStatus.FAILED);
                log.error("Outbox event FAILED after 5 retries: {}", 
                    event.getId(), e);
                // Alert bhejo!
                alertService.sendAlert(
                    "Outbox relay failed: " + event.getId());
            }

            outboxRepo.save(event);
        }
    }
}
```

### Transactional Outbox with Debezium (CDC Approach):

```
Debezium CDC Approach:
┌─────────────────────────────────────────────┐
│                                             │
│  Database:                                  │
│  ┌──────────────┐  ┌─────────────────┐     │
│  │ orders table │  │ outbox_events   │     │
│  └──────┬───────┘  └────────┬────────┘     │
│         │                   │               │
│         └────────┬──────────┘               │
│                  │                           │
│                  ▼                           │
│  ┌───────────────────────┐                  │
│  │  Debezium Connector   │                  │
│  │  (CDC - Change Data   │                  │
│  │   Capture)            │                  │
│  │                       │                  │
│  │  Reads DB transaction │                  │
│  │  log (WAL/binlog)     │                  │
│  │  Detects outbox       │                  │
│  │  INSERT automatically │                  │
│  └───────────┬───────────┘                  │
│              │                               │
│              ▼                               │
│  ┌───────────────────────┐                  │
│  │  Kafka                │                  │
│  │  (Debezium pushes     │                  │
│  │   events directly)    │                  │
│  └───────────────────────┘                  │
│                                             │
│  Benefit: Application ko extra publish      │
│           logic nahi likhna!                │
│           Debezium automatically capture     │
│           kar deta hai                      │
└─────────────────────────────────────────────┘
```

---

## 6. Idempotency

### Idempotency kya hai?

> **Idempotency** ka matlab: Ek operation ko ek baar karo ya multiple baar - result SAME hona chahiye.

```
Idempotent Operations:
┌─────────────────────────────────────────────┐
│                                             │
│  SET balance = 100                          │
│  → 1 baar run karo: balance = 100           │
│  → 10 baar run karo: balance = 100          │
│  ✅ IDEMPOTENT                               │
│                                             │
│  balance = balance + 100                    │
│  → 1 baar run karo: balance = +100          │
│  → 10 baar run karo: balance = +1000 ❌     │
│  ❌ NOT IDEMPOTENT                           │
│                                             │
└─────────────────────────────────────────────┘
```

### Why Idempotency Matters:

```
Problem: Network mein duplicate messages

Service A ──► Service B
  │              │
  │── Message ──►│  ✅ Processed
  │              │
  │── Message ──►│  ⚠️ DUPLICATE! (network retry)
  │   (same)     │     Agar idempotent nahi: Double charge!
  │              │
  │── Message ──►│  ⚠️ DUPLICATE! (network retry)
  │   (same)     │     Aur ek baar double charge!
```

### Implementing Idempotency:

#### Method 1: Idempotency Key

```java
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final IdempotencyRepository idempotencyRepo;

    public PaymentResult processPayment(
            PaymentRequest request,
            String idempotencyKey) {  // Client se unique key

        // 1. Check if already processed
        Optional<IdempotencyRecord> existing = 
            idempotencyRepo.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            log.info("Duplicate request, returning cached result");
            return existing.get().getResult();  // Same result return
        }

        // 2. Process payment
        PaymentResult result = doProcessPayment(request);

        // 3. Store result for future duplicate detection
        idempotencyRepo.save(IdempotencyRecord.builder()
            .idempotencyKey(idempotencyKey)
            .result(result)
            .createdAt(Instant.now())
            .build());

        return result;
    }
}

@Entity
@Data
public class IdempotencyRecord {
    @Id
    private String idempotencyKey;

    @Column(columnDefinition = "TEXT")
    private String result;  // Serialized result

    private Instant createdAt;
    private Instant expiresAt;
}
```

#### Method 2: Unique Constraint

```java
// Database level idempotency
@Entity
@Table(name = "payments",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"transactionRef"}))
public class Payment {
    @Id
    private String id;

    @Column(unique = true, nullable = false)
    private String transactionRef;  // Unique reference

    private BigDecimal amount;
    private PaymentStatus status;
}

@Service
public class PaymentService {

    @Transactional
    public Payment createPayment(PaymentRequest request) {
        try {
            Payment payment = Payment.builder()
                .id(UUID.randomUUID().toString())
                .transactionRef(request.getTransactionRef())
                .amount(request.getAmount())
                .status(PaymentStatus.PENDING)
                .build();

            return paymentRepo.save(payment);

        } catch (DataIntegrityViolationException e) {
            // Duplicate transactionRef!
            // Already processed hai, existing return karo
            return paymentRepo
                .findByTransactionRef(request.getTransactionRef())
                .orElseThrow();
        }
    }
}
```

#### Method 3: Event Deduplication

```java
@Component
public class EventDeduplicationHandler {

    private final DeduplicationCache cache;

    @KafkaListener(topics = "payment-events")
    public void handlePaymentEvent(PaymentEvent event) {
        String eventId = event.getEventId();

        // Check if already processed
        if (cache.isProcessed(eventId)) {
            log.debug("Duplicate event, skipping: {}", eventId);
            return;
        }

        // Process
        processEvent(event);

        // Mark as processed
        cache.markProcessed(eventId);
    }
}

@Component
public class DeduplicationCache {

    // Redis / Caffeine cache with TTL
    private final Cache<String, Boolean> processedEvents;

    public DeduplicationCache() {
        this.processedEvents = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterWrite(Duration.ofHours(24))
            .build();
    }

    public boolean isProcessed(String eventId) {
        return processedEvents.getIfPresent(eventId) != null;
    }

    public void markProcessed(String eventId) {
        processedEvents.put(eventId, true);
    }
}
```

---

## 7. At-Least-Once vs Exactly-Once Delivery

### Delivery Guarantees:

```
┌─────────────────────────────────────────────────────────────┐
│                  DELIVERY GUARANTEES                         │
├──────────────────┬──────────────────┬───────────────────────┤
│  At-Most-Once    │  At-Least-Once   │  Exactly-Once         │
│  (Fire & Forget) │  (Retry on Fail) │  (Idempotent + Dedup) │
├──────────────────┼──────────────────┼───────────────────────┤
│  Message         │  Message         │  Message              │
│  may be lost     │  delivered 1+    │  delivered EXACTLY    │
│                  │  times           │  1 time               │
│                  │                  │                       │
│  ❌ Possible: 0  │  ❌ Possible: 2+ │  ✅ Guaranteed: 1     │
│     deliveries   │     deliveries   │     delivery          │
│                  │                  │                       │
│  Fast but        │  Most common     │  Hardest to           │
│  unreliable      │  in practice     │  implement            │
└──────────────────┴──────────────────┴───────────────────────┘
```

### At-Least-Once Delivery (Most Common):

```
Flow:
Producer ──► Kafka ──► Consumer
                │          │
                │          │── Process message
                │          │── Ack if success
                │          │
                │          │── NAK/Timeout if fail
                │          │
                └──────────┘── Kafka redelivers!
                              Consumer processes AGAIN
                              → Duplicate processing!
```

```java
// At-Least-Once consumer with idempotency
@KafkaListener(topics = "orders")
public void consumeOrderEvent(ConsumerRecord<String, String> record) {
    String eventId = record.headers().lastHeader("event-id").toString();

    // Idempotency check
    if (deduplicationCache.isProcessed(eventId)) {
        log.info("Already processed, skipping: {}", eventId);
        return;
    }

    // Process
    processEvent(record.value());

    // Mark as processed
    deduplicationCache.markProcessed(eventId);

    // Commit offset (manual commit)
    ack.acknowledge();
}
```

### Exactly-Once Semantics (EOS):

```
Exactly-Once = At-Least-Once + Idempotency + Deduplication

Implementation approaches:

Approach 1: Kafka Transactions (Producer Side)
┌─────────────────────────────────────────┐
│                                         │
│  producer.beginTransaction()            │
│    │                                    │
│    ├──► Send message 1 to Topic A      │
│    ├──► Send message 2 to Topic B      │
│    │                                    │
│  producer.commitTransaction()           │
│    │                                    │
│    └──► Ya toh sab commit honge        │
│         ya sab rollback                 │
│                                         │
└─────────────────────────────────────────┘

Approach 2: Idempotent Producer (Kafka Built-in)
┌─────────────────────────────────────────┐
│                                         │
│  properties.put(                        │
│    "enable.idempotence", "true"         │
│  );                                     │
│  properties.put(                        │
│    "transactional.id", "my-producer-1"  │
│  );                                     │
│                                         │
│  Kafka automatically:                  │
│  - Deduplicates on producer side        │
│  - Maintains ordering                   │
│  - Handles retries safely               │
│                                         │
└─────────────────────────────────────────┘
```

### Comparison Table:

| Aspect | At-Most-Once | At-Least-Once | Exactly-Once |
|--------|-------------|---------------|--------------|
| **Message Loss** | Possible | Not possible | Not possible |
| **Duplicates** | Not possible | Possible | Not possible |
| **Performance** | Fastest | Medium | Slowest |
| **Complexity** | Lowest | Medium | Highest |
| **Use Case** | Metrics, logs | Orders, payments | Financial transactions |
| **Implementation** | Fire & forget | Retry + Ack | Idempotency + Dedup + TX |

### Kafka Configuration:

```java
// At-Least-Once Consumer Config
@Bean
public ConsumerFactory<String, String> atLeastOnceConsumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "my-group");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);  // Manual commit
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    return new DefaultKafkaConsumerFactory<>(props);
}

// Exactly-Once Producer Config
@Bean
public ProducerFactory<String, String> exactlyOnceProducerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);     // Idempotent
    props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "txn-prod-1");
    props.put(ProducerConfig.ACKS_CONFIG, "all");                  // All replicas
    props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

    return new DefaultKafkaProducerFactory<>(props);
}
```

---

## 8. Complete Implementation Example

### Full Event-Driven Order Processing System:

```
┌─────────────────────────────────────────────────────────────────┐
│                    SYSTEM ARCHITECTURE                           │
│                                                                  │
│  ┌─────────────┐                                                │
│  │    API       │── CreateOrder Command                         │
│  │   Gateway    │                                                │
│  └─────┬───────┘                                                │
│        │                                                         │
│        ▼                                                         │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                   Order Service                          │    │
│  │                                                          │    │
│  │  ┌──────────────┐    ┌──────────────┐                   │    │
│  │  │ Command      │───►│  Aggregate   │                   │    │
│  │  │ Handler      │    │  (ES)        │                   │    │
│  │  └──────┬───────┘    └──────┬───────┘                   │    │
│  │         │                   │                            │    │
│  │         │            ┌──────▼───────┐                   │    │
│  │         │            │  Event Store │                   │    │
│  │         │            └──────┬───────┘                   │    │
│  │         │                   │                            │    │
│  │    ┌────▼───────────────────▼────────────────────┐       │    │
│  │    │              Outbox Table                   │       │    │
│  │    │  (Atomic: DB + Event in same transaction)   │       │    │
│  │    └────────────────┬────────────────────────────┘       │    │
│  └─────────────────────┼────────────────────────────────────┘    │
│                        │ Events                                   │
│                        ▼                                           │
│              ┌─────────────────────┐                              │
│              │    Kafka Broker     │                              │
│              └──────────┬──────────┘                              │
│                         │                                          │
│              ┌──────────┼──────────┐                              │
│              ▼          ▼          ▼                              │
│  ┌──────────────┐ ┌──────────┐ ┌──────────────┐                 │
│  │  Payment     │ │Inventory │ │ Notification │                 │
│  │  Service     │ │ Service  │ │ Service      │                 │
│  │              │ │          │ │              │                 │
│  │ Read Model   │ │Read Model│ │ Read Model   │                 │
│  │ Projection   │ │Projection│ │ Projection   │                 │
│  └──────────────┘ └──────────┘ └──────────────┘                 │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Full Code Example:

```java
// ==========================================
// 1. EVENT DEFINITIONS
// ==========================================

public interface DomainEvent {
    String getEventId();
    String getAggregateId();
    Instant getTimestamp();
    int getVersion();
}

@Data
@Builder
public class OrderCreatedEvent implements DomainEvent {
    private String eventId;
    private String orderId;
    private String customerId;
    private BigDecimal totalAmount;
    private Instant timestamp;
    private int version;
}

@Data
@Builder
public class OrderPaidEvent implements DomainEvent {
    private String eventId;
    private String orderId;
    private String paymentId;
    private Instant timestamp;
    private int version;
}

@Data
@Builder
public class OrderShippedEvent implements DomainEvent {
    private String eventId;
    private String orderId;
    private String trackingNumber;
    private Instant timestamp;
    private int version;
}


// ==========================================
// 2. OUTBOX ENTITY
// ==========================================

@Entity
@Table(name = "outbox_events",
    indexes = {
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_type", columnList = "eventType")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false, columnDefinition = "JSON")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant processedAt;

    public enum Status { PENDING, PROCESSED, FAILED }


// ==========================================
// 3. ORDER SERVICE (Command Side)
// ==========================================

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCommandService {

    private final EventStore eventStore;
    private final OutboxRepository outboxRepo;

    @Transactional
    public String createOrder(CreateOrderCommand command) {
        String orderId = UUID.randomUUID().toString();

        // Event create
        OrderCreatedEvent event = OrderCreatedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .orderId(orderId)
            .customerId(command.getCustomerId())
            .totalAmount(command.getTotalAmount())
            .timestamp(Instant.now())
            .version(1)
            .build();

        // Event store mein save
        eventStore.save(orderId, event);

        // Outbox mein bhi save (same transaction!)
        OutboxEventEntity outboxEvent = OutboxEventEntity.builder()
            .eventId(event.getEventId())
            .eventType("OrderCreated")
            .aggregateId(orderId)
            .payload(JsonUtil.toJson(event))
            .status(OutboxEventEntity.Status.PENDING)
            .retryCount(0)
            .createdAt(Instant.now())
            .build();
        outboxRepo.save(outboxEvent);

        log.info("Order created: {}, outbox event saved", orderId);
        return orderId;
    }
}


// ==========================================
// 4. OUTBOX RELAYER
// ==========================================

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayer {

    private final OutboxRepository outboxRepo;
    private final KafkaTemplate<String, String> kafka;

    @Scheduled(fixedDelay = 2000, initialDelay = 5000)
    @Transactional
    public void relay() {
        List<OutboxEventEntity> pending = outboxRepo
            .findByStatusOrderByCreatedAtAsc(
                OutboxEventEntity.Status.PENDING);

        for (OutboxEventEntity event : pending) {
            try {
                kafka.send(
                    "order-events." + event.getEventType(),
                    event.getAggregateId(),
                    event.getPayload()
                ).get(5, TimeUnit.SECONDS);

                event.setStatus(OutboxEventEntity.Status.PROCESSED);
                event.setProcessedAt(Instant.now());
                outboxRepo.save(event);

                log.debug("Event relayed: {}", event.getEventId());

            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= 10) {
                    event.setStatus(OutboxEventEntity.Status.FAILED);
                    log.error("Event relay FAILED: {}", event.getId(), e);
                }
                outboxRepo.save(event);
            }
        }
    }
}


// ==========================================
// 5. PAYMENT SERVICE PROJECTION
// ==========================================

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentProjection {

    private final PaymentReadModelRepository repo;

    @KafkaListener(
        topics = "order-events.OrderCreated",
        groupId = "payment-projection"
    )
    @Transactional
    public void handleOrderCreated(String payload) {
        OrderCreatedEvent event = JsonUtil.fromJson(
            payload, OrderCreatedEvent.class);

        // Idempotency check
        if (repo.existsByEventId(event.getEventId())) {
            log.debug("Duplicate event, skipping: {}", event.getEventId());
            return;
        }

        // Read model update
        PaymentOrderView view = new PaymentOrderView();
        view.setOrderId(event.getOrderId());
        view.setCustomerId(event.getCustomerId());
        view.setAmount(event.getTotalAmount());
        view.setStatus("PENDING_PAYMENT");
        view.setLastProcessedEventId(event.getEventId());
        repo.save(view);

        log.info("Payment projection updated for order: {}", event.getOrderId());
    }
}


// ==========================================
// 6. QUERY SERVICE
// ==========================================

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderReadModelRepository readRepo;

    // Fast queries from read model
    public OrderDto getOrder(String orderId) {
        return readRepo.findById(orderId)
            .map(this::toDto)
            .orElseThrow(() -> new OrderNotFoundException("Not found"));
    }

    public Page<OrderDto> getOrdersByCustomer(
            String customerId, Pageable pageable) {
        return readRepo.findByCustomerId(customerId, pageable)
            .map(this::toDto);
    }

    public List<OrderDto> searchOrders(String keyword) {
        return readRepo.searchByKeyword(keyword)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
}
```

---

## 9. Best Practices

### Event Sourcing:

```
DO:
├── Events ko immutable banao (once written, never changed)
├── Events mein past tense naming use karo (OrderCreated, not CreateOrder)
├── Event versioning implement karo (schema evolution ke liye)
├── Snapshots use karo (long event streams ke liye)
├── Events mein sirf necessary data rakho
└── Idempotent event handlers banao

DON'T:
├── Events ko update/delete mat karo (append only!)
├── Events mein sensitive data mat rakho (GDPR issues)
├── Events ko bahut granular mat banao
├── External system ke events pe blindly trust mat karo
└── Bina idempotency ke event process mat karo
```

### CQRS:

```
DO:
├── Read aur write models alag rakho
├── Read models denormalized banao (query performance ke liye)
├── Eventual consistency accept karo
├── Multiple read models banao (different use cases ke liye)
└── Consistency window define karo (how stale is acceptable?)

DON'T:
├── Simple CRUD mein CQRS mat use karo (over-engineering)
├── Read model mein business logic mat rakho
├── Strong consistency expect mat karo
├── Ek read model se sab kaam karne ki koshish mat karo
└── CQRS ko CQS (without segregation) mat banao
```

### Outbox Pattern:

```
DO:
├── Outbox insert ko business transaction ke saath rakho (same DB transaction)
├── Background relayer with retry logic banao
├── Duplicate event detection implement karo
├── Dead letter queue for failed events rakho
└── Monitoring: pending count, retry count, failure rate

DON'T:
├── Outbox ko skip mat karo (dual write problem real hai)
├── Relayer ko too aggressive mat banao (DB load)
├── Failed events ko silently ignore mat karo
├── Outbox table ko clean mat karo without archiving
└── CDC tools (Debezium) ko ignore mat karo (easier alternative)
```

### Idempotency:

```
DO:
├── Har consumer/processor ko idempotent banao
├── Idempotency keys use karo (client-provided unique IDs)
├── Database unique constraints use karo
├── Deduplication cache rakho (Redis/Caffeine)
└── Idempotency key TTL set karo (storage optimization)

DON'T:
├── Assume network kabhi duplicate nahi bhejega
├── Idempotency check ko skip mat karo
├── Non-idempotent operations ko event-driven mat banao bina handling
└── Deduplication window too short mat rakho
```

---

## 10. When to Use What

```
Decision Matrix:

Event Sourcing use karo jab:
├── Full audit trail chahiye (finance, healthcare)
├── Time-travel queries chahiye
├── Event replay se naya insight mil sakta hai
├── Compliance requirements hain
└── Team experienced hai event-driven architecture mein

CQRS use karo jab:
├── Read/Write ratio bahut different hai (>10:1)
├── Complex read queries hain (reporting, analytics)
├── Different scaling requirements for read/write
└── Multiple UI/API views chahiye same data ke liye

Outbox Pattern use karo jab:
├── Events publish karne hain with DB updates
├── Data consistency critical hai
├── Microservices communicate karte hain via events
└── Dual write problem avoid karna hai

Idempotency implement karo jab:
├── Network retries possible hain (always!)
├── Message brokers use kar rahe ho
├── Payment/financial operations hain
└── Duplicate operations costly hain

At-Least-Once use karo jab:
├── Message loss acceptable nahi hai
├── Idempotency implement ki hai consumer pe
└── Most common case - yehi default rakho

Exactly-Once use karo jab:
├── Financial transactions hain
├── Duplicate processing bahut costly hai
├── Kafka transactions support karta hai
└── Performance compromise acceptable hai
```

### Summary / Cheat Sheet:

```
┌──────────────────────────────────────────────────────────────┐
│                  EVENT-DRIVEN ARCHITECTURE CHEAT SHEET        │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  EVENT SOURCING:                                              │
│  ├─ State nahi, events store karo                             │
│  ├─ Append-only event log                                     │
│  ├─ State = replay all events                                 │
│  ├─ Built-in audit trail                                      │
│  └─ Snapshots for performance                                 │
│                                                               │
│  CQRS:                                                        │
│  ├─ Read aur Write models separate                            │
│  ├─ Write side: business logic, validation                    │
│  ├─ Read side: denormalized, query-optimized                  │
│  ├─ Eventual consistency accept karo                          │
│  └─ Multiple read models possible                             │
│                                                               │
│  OUTBOX PATTERN:                                              │
│  ├─ DB update + event in SAME transaction                     │
│  ├─ Background relayer publishes to broker                    │
│  ├─ Or use Debezium CDC (easier)                              │
│  └─ Solves dual write problem                                 │
│                                                               │
│  IDEMPOTENCY:                                                 │
│  ├─ Same operation, multiple times = same result              │
│  ├─ Idempotency keys (client-provided unique IDs)             │
│  ├─ Unique constraints in DB                                  │
│  └─ Deduplication cache (Redis/Caffeine)                      │
│                                                               │
│  DELIVERY GUARANTEES:                                         │
│  ├─ At-Most-Once: Fast, may lose messages                     │
│  ├─ At-Least-Once: Most common, may duplicate (handle it!)    │
│  └─ Exactly-Once: Hardest, no loss, no dup (Kafka TX)         │
│                                                               │
│  GOLDEN RULE:                                                 │
│  └─ Assume duplicates WILL happen. Always be idempotent.      │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

---

> **Previous:** [07 - Saga Pattern](./07-Saga-Pattern.md) ← Distributed Transactions
> **Next:** Explore implementation projects in the `phase-4-advanced-topics/projects/` directory

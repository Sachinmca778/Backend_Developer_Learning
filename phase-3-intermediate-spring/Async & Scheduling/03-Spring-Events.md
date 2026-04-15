# Spring Events

## Status: Not Started

---

## Table of Contents

1. [ApplicationEvent](#applicationevent)
2. [ApplicationEventPublisher](#applicationeventpublisher)
3. [@EventListener](#eventlistener)
4. [@TransactionalEventListener](#transactionaleventlistener)
5. [Async Event Processing](#async-event-processing)
6. [Custom Events](#custom-events)
7. [Event Ordering with @Order](#event-ordering-with-order)

---

## ApplicationEvent

**Matlab:** Spring ka event mechanism — loosely coupled components ke beech communication.

### Built-in Events

```java
// Spring framework events
ContextRefreshedEvent      // ApplicationContext refresh complete
ContextStartedEvent        // ApplicationContext start
ContextStoppedEvent        // ApplicationContext stop
ContextClosedEvent         // ApplicationContext close
RequestHandledEvent        // HTTP request handled
PayloadApplicationEvent    // Generic payload wrapper
```

### Custom Event

```java
// Event class — ApplicationEvent extend karo
public class UserRegisteredEvent extends ApplicationEvent {

    private final User user;
    private final Instant registeredAt;

    public UserRegisteredEvent(Object source, User user) {
        super(source);  // source = event publisher
        this.user = user;
        this.registeredAt = Instant.now();
    }

    // Getters
    public User getUser() { return user; }
    public Instant getRegisteredAt() { return registeredAt; }
}
```

### Simple Event (Java 8+)

```java
// ApplicationEvent extend karna zaruri nahi — koi bhi object event ho sakta hai
public class OrderCreatedEvent {
    private final Order order;
    private final BigDecimal totalAmount;

    public OrderCreatedEvent(Order order, BigDecimal totalAmount) {
        this.order = order;
        this.totalAmount = totalAmount;
    }

    public Order getOrder() { return order; }
    public BigDecimal getTotalAmount() { return totalAmount; }
}
```

---

## ApplicationEventPublisher

**Matlab:** Events publish karne ke liye — ApplicationContext mein inject hota hai.

### Basic Usage

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;  // Inject publisher

    public User registerUser(UserRegistrationRequest request) {
        // User save karo
        User user = new User(request.getName(), request.getEmail());
        user = userRepository.save(user);

        // Event publish karo
        eventPublisher.publishEvent(new UserRegisteredEvent(this, user));

        // Method returns immediately — event handlers asynchronously process karenge
        return user;
    }
}
```

### Event Publishing Flow

```
UserService.registerUser()
    ↓
userRepository.save(user)
    ↓
eventPublisher.publishEvent(new UserRegisteredEvent(this, user))
    ↓
ApplicationEventMulticaster
    ↓
All registered listeners notified (sequentially by default)
    ↓
EmailService.onUserRegistered() → Send welcome email
AuditService.onUserRegistered() → Log audit
AnalyticsService.onUserRegistered() → Track analytics
```

---

## @EventListener

**Matlab:** Events ko listen karna — method pe annotation lagao.

### Basic Usage

```java
@Component
public class EmailService {

    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        User user = event.getUser();
        System.out.println("Sending welcome email to: " + user.getEmail());
        // Email sending logic
    }
}

@Component
public class AuditService {

    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        User user = event.getUser();
        System.out.println("Auditing user registration: " + user.getName());
        // Audit logging
    }
}
```

### Multiple Event Types

```java
@Component
public class NotificationService {

    // Ek method — multiple events handle karo
    @EventListener({UserRegisteredEvent.class, OrderCreatedEvent.class})
    public void handleMultipleEvents(Object event) {
        if (event instanceof UserRegisteredEvent userEvent) {
            System.out.println("User registered: " + userEvent.getUser().getName());
        } else if (event instanceof OrderCreatedEvent orderEvent) {
            System.out.println("Order created: " + orderEvent.getOrder().getId());
        }
    }
}
```

### Conditional Event Listening

```java
@Component
public class ConditionalEventListener {

    // Sirf high-value orders pe notify karo
    @EventListener(condition = "#orderEvent.totalAmount > 1000")
    public void onHighValueOrder(OrderCreatedEvent orderEvent) {
        System.out.println("High value order: " + orderEvent.getTotalAmount());
        // Send VIP notification
    }

    // SpEL expressions use kar sakte ho
    @EventListener(condition = "#event.user.role.name == 'ADMIN'")
    public void onAdminRegistration(UserRegisteredEvent event) {
        System.out.println("Admin registered: " + event.getUser().getName());
    }
}
```

### Returning Events from Listeners

```java
@Component
public class ChainedEventListener {

    // Return type Event → automatically published
    @EventListener
    public OrderProcessedEvent onOrderCreated(OrderCreatedEvent event) {
        System.out.println("Processing order: " + event.getOrder().getId());

        // Processing logic
        Order order = event.getOrder();
        order.setStatus("PROCESSED");

        // Naya event return karo — automatically publish hoga
        return new OrderProcessedEvent(order);
    }

    // OrderProcessedEvent automatically listen hoga
    @EventListener
    public void onOrderProcessed(OrderProcessedEvent event) {
        System.out.println("Order processed, sending notification");
    }
}
```

---

## @TransactionalEventListener

**Matlab:** Transaction ke specific phase mein event handle karna — data consistency ke liye.

### The Problem

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public User registerUser(UserRegistrationRequest request) {
        User user = userRepository.save(user);

        // ❌ Problem: Event immediately handle hota hai
        // EmailService user ko query karti hai — DB mein commit nahi hua!
        // UserNotFoundException aa sakta hai
        eventPublisher.publishEvent(new UserRegisteredEvent(this, user));

        return user;
    }
    // Transaction yahan commit hota hai
}
```

### Solution: @TransactionalEventListener

```java
@Component
public class EmailService {

    // Sirf AFTER_COMMIT pe handle karo — transaction complete hone ke baad
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        // Ab user DB mein commit ho chuka hai
        User user = event.getUser();
        System.out.println("Sending welcome email to: " + user.getEmail());
        // Safe to query user
    }
}
```

### Transaction Phases

```java
@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
public void beforeCommit(MyEvent event) {
    // Transaction commit hone se PEhle
    // Use: Final validation before commit
}

@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void afterCommit(MyEvent event) {
    // Transaction commit hone ke BAAD
    // Use: Email, notifications, audit logs
}

@TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
public void afterRollback(MyEvent event) {
    // Transaction rollback hone ke BAAD
    // Use: Cleanup, compensating actions
}

@TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION)
public void afterCompletion(MyEvent event) {
    // Transaction complete hone ke BAAD (commit ya rollback dono pe)
    // Use: Resource cleanup
}
```

### Default Phase

```java
// Default phase = AFTER_COMMIT
@TransactionalEventListener  // Same as phase = AFTER_COMMIT
public void handleEvent(MyEvent event) { }

// Agar current transaction nahi hai toh event immediately handle hoga
```

### Fallback Execution

```java
// No transaction scenario
@TransactionalEventListener(
    phase = TransactionPhase.AFTER_COMMIT,
    fallbackExecution = true  // No transaction pe bhi execute karo
)
public void handleEvent(MyEvent event) { }

// Without fallbackExecution — no transaction pe event IGNORE ho jayega
```

---

## Async Event Processing

**Matlab:** Events ko asynchronously handle karna — publisher block nahi hoga.

### Setup

```java
@Configuration
@EnableAsync
public class AsyncEventConfig {

    @Bean(name = "eventExecutor")
    public ThreadPoolTaskExecutor eventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("EventExecutor-");
        executor.initialize();
        return executor;
    }
}
```

### Async Event Listener

```java
@Component
public class AsyncEventListener {

    // @Async + @EventListener = async event handling
    @Async("eventExecutor")
    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        // Separate thread pe execute hoga
        System.out.println("Thread: " + Thread.currentThread().getName());
        System.out.println("Sending email async to: " + event.getUser().getEmail());

        // Slow operation — email, SMS, etc.
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        System.out.println("Email sent successfully");
    }

    // Transactional + Async — careful!
    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegisteredAfterCommit(UserRegisteredEvent event) {
        // Transaction commit hone ke baad async execute hoga
        System.out.println("Processing after commit");
    }
}
```

### SimpleAsyncTaskExecutor (Default Async)

```java
// @EnableAsync ke bina @Async — SimpleAsyncTaskExecutor use hota hai
// Har call pe naya thread — production mein avoid karo

// ✅ Always configure proper ThreadPoolTaskExecutor
```

---

## Custom Events

**Matlab:** Application-specific events define karna.

### Event Class

```java
public class PaymentCompletedEvent {
    private final String paymentId;
    private final BigDecimal amount;
    private final String currency;
    private final Instant completedAt;

    public PaymentCompletedEvent(String paymentId, BigDecimal amount, String currency) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.currency = currency;
        this.completedAt = Instant.now();
    }

    public String getPaymentId() { return paymentId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public Instant getCompletedAt() { return completedAt; }
}
```

### Generic Event Class

```java
public class EntityEvent<T> {
    private final String eventType;
    private final String entityId;
    private final T entity;
    private final Instant occurredAt;

    public EntityEvent(String eventType, String entityId, T entity) {
        this.eventType = eventType;
        this.entityId = entityId;
        this.entity = entity;
        this.occurredAt = Instant.now();
    }

    public String getEventType() { return eventType; }
    public String getEntityId() { return entityId; }
    public T getEntity() { return entity; }
    public Instant getOccurredAt() { return occurredAt; }
}

// Usage
EntityEvent<User> userCreated = new EntityEvent<>("USER_CREATED", "123", user);
EntityEvent<Order> orderShipped = new EntityEvent<>("ORDER_SHIPPED", "456", order);
```

### Event Publisher Service

```java
@Service
public class EventPublisherService {

    private final ApplicationEventPublisher eventPublisher;

    public EventPublisherService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishUserCreated(User user) {
        eventPublisher.publishEvent(new UserRegisteredEvent(this, user));
    }

    public void publishOrderCreated(Order order) {
        eventPublisher.publishEvent(new OrderCreatedEvent(order));
    }

    public void publishGenericEvent(String type, String entityId, Object data) {
        eventPublisher.publishEvent(new EntityEvent<>(type, entityId, data));
    }
}
```

---

## Event Ordering with @Order

**Matlab:** Multiple listeners hain toh kis order mein execute honge — `@Order` se control karo.

### Basic Ordering

```java
@Component
@Order(1)  // Pehle execute hoga
public class FirstListener {
    @EventListener
    public void onEvent(MyEvent event) {
        System.out.println("First listener executed");
    }
}

@Component
@Order(2)  // Doosra execute hoga
public class SecondListener {
    @EventListener
    public void onEvent(MyEvent event) {
        System.out.println("Second listener executed");
    }
}

@Component
@Order(3)  // Teesra execute hoga
public class ThirdListener {
    @EventListener
    public void onEvent(MyEvent event) {
        System.out.println("Third listener executed");
    }
}
```

### Order with TransactionalEventListener

```java
@Component
@Order(1)
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public class AuditListener {
    public void onEvent(MyEvent event) {
        // Pehle audit log
        System.out.println("Audit logged");
    }
}

@Component
@Order(2)
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public class EmailListener {
    public void onEvent(MyEvent event) {
        // Phir email bhejo
        System.out.println("Email sent");
    }
}
```

### Ordering Without @Order

```java
// Agar @Order nahi hai toh order undefined hai
// Spring internally order determine karta hai (usually registration order)

// ✅ Best practice: Hamesha @Order use karo jab order important ho
```

### Execution Flow

```
Event Published
    ↓
@Order(1) FirstListener → Execute
    ↓
@Order(2) SecondListener → Execute
    ↓
@Order(3) ThirdListener → Execute
    ↓
Event processing complete
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **ApplicationEvent** | Event object — data carry karta hai |
| **ApplicationEventPublisher** | Events publish karne ke liye |
| **@EventListener** | Events listen karne ke liye |
| **@TransactionalEventListener** | Transaction phase pe handle karo (BEFORE_COMMIT, AFTER_COMMIT, etc.) |
| **Async Events** | `@Async` + `@EventListener` — separate thread pe handle |
| **Custom Events** | Koi bhi object event ho sakta hai |
| **@Order** | Listener execution order control karo |
| **Conditional Events** | `condition = "#event.amount > 1000"` — SpEL expressions |
| **Chained Events** | Listener se Event return karo → automatically publish |

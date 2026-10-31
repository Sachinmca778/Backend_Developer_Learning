# Behavioral Pattern: Observer Pattern (Spring Events) 📢

---

### 1. Core Concept (Observer Pattern Kya Hai?)
**Answer (Hinglish):**

> **When one object changes its state, all of its registered subscribers (observers) are notified automatically.**  
> *(Analogy: YouTube Channel Subscription: Jab creator naya video upload karta hai [Event Publish], to notification ghanti sabhi subscribers [Listeners] tak automatically baj jaati hai).*

Enterprise microservices me decoupling maintain karne ke liye Observer pattern sabse clean approach hai.

---

### 2. ❌ The Anti-Pattern: Tightly Coupled Monolith Method

Dekhiye jab koi naya developer order placement code likhta hai:

```java
@Service
public class OrderService {

    public void placeOrder(Order order) {
        orderRepository.save(order);

        // ❌ Tightly coupled dependencies!
        emailService.sendConfirmationEmail(order);
        smsService.sendOtp(order);
        loyaltyProgramService.addRewardPoints(order);
        inventoryService.reserveStock(order);
        analyticsService.trackPurchase(order);
        // ❌ Har nayi requirement aane par OrderService class modify karni padegi!
    }
}
```

#### Why is this problematic?
1. **Slow API Response:** Email aur SMS bhejne me 2-3 seconds lagte hain. User screen par wait karta rahega!
2. **Blast Radius (Failure Propagation):** Agar Loyalty Point API fail ho gayi ya exception throw kar diya, to poora valid order rollback ho jayega!
3. **High Coupling:** `OrderService` ko duniya ke 6 alag services ke baare me pata hona pad raha hai.

---

### 3. ✅ The Spring Solution: Spring Application Events

Spring Boot me Observer Pattern **`ApplicationEventPublisher`** aur **`@EventListener`** se achieve hota hai.

```
                  ┌───────────────────────────────┐
                  │       OrderPlacedEvent        │
                  │   (Immutable event record)    │
                  └───────────────┬───────────────┘
                                  │
    ┌─────────────────────────────┼─────────────────────────────┐
    ▼                             ▼                             ▼
[ EmailListener ]          [ SmsListener ]              [ LoyaltyListener ]
(Sends invoice)            (Sends SMS)                  (Adds reward points)
```

#### Step 1: Define Event Object (Java 16+ Record)
```java
public record OrderPlacedEvent(Long orderId, String customerEmail, double amount) {}
```

#### Step 2: Publisher (`OrderService` Publishes Event)
```java
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void placeOrder(Order order) {
        Order savedOrder = orderRepository.save(order);

        // Sirf event fire karo aur apna kaam khatam samjho!
        eventPublisher.publishEvent(new OrderPlacedEvent(
            savedOrder.getId(), savedOrder.getCustomerEmail(), savedOrder.getAmount()
        ));
    }
}
```

#### Step 3: Listeners (Independent Subscribers)
```java
@Component
public class EmailNotificationListener {

    @EventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        System.out.println("Email sent to " + event.customerEmail() + " for order #" + event.orderId());
    }
}

@Component
public class LoyaltyRewardListener {

    @EventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        System.out.println("Added " + (event.amount() * 0.1) + " points for order #" + event.orderId());
    }
}
```

---

### 4. Advanced Listener Flavors (Production Ready)

Spring Boot 3 tarike ke listeners provide karta hai:

| Annotation Combination | Thread Execution | Best Use Case |
| :--- | :--- | :--- |
| **`@EventListener`** | **Same Thread** (Synchronous) | Jab listener ko transaction ke sath fail hona zaroori ho. |
| **`@Async + @EventListener`** | **Background Thread** (Asynchronous) | Heavy I/O operations (PDF generation, Email/SMS) taaki HTTP API instantly return ho sake. |
| **`@TransactionalEventListener`** | **Runs only AFTER DB Commit** (`phase = TransactionPhase.AFTER_COMMIT`) | **Crucial for emails/notifications!** Taaki user ko tabhi email jaye jab database me order successfully commit ho chuka ho (No fake emails on rollback). |

```java
@Component
public class AsyncNotificationListener {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCommitted(OrderPlacedEvent event) {
        // Safe, non-blocking execution after successful DB commit
        emailService.sendInvoice(event.customerEmail(), event.orderId());
    }
}
```

---

### 💡 Pro Interview Tip:
> **Question:** *"What is the difference between Spring Application Events and Apache Kafka?"*  
> **Your Answer:**  
> - **Spring Events:** Observer pattern implementation **inside a SINGLE JVM instance (In-process)**.  
> - **Apache Kafka:** Observer pattern implementation **across MANY distributed microservices (Out-of-process)** over network brokers.  
> *Both follow the exact same Publisher-Subscriber / Observer design pattern, just at different architectural scales!*

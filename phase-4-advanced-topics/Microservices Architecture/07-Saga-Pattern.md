# 07 - Saga Pattern: Distributed Transactions in Microservices

> "Microservices mein 2PC (Two-Phase Commit) use nahi karte - Saga pattern use karo!"

---

## Table of Contents

1. [The Problem: Distributed Transactions](#1-the-problem-distributed-transactions)
2. [Why NOT 2PC in Microservices?](#2-why-not-2pc-in-microservices)
3. [What is the Saga Pattern?](#3-what-is-the-saga-pattern)
4. [Choreography-Based Saga (Event-Driven)](#4-choreography-based-saga-event-driven)
5. [Orchestration-Based Saga (Central Coordinator)](#5-orchestration-based-saga-central-coordinator)
6. [Choreography vs Orchestration - Comparison](#6-choreography-vs-orchestration---comparison)
7. [Compensating Transactions](#7-compensating-transactions)
8. [Implementing with Axon Framework](#8-implementing-with-axon-framework)
9. [Best Practices](#9-best-practices)
10. [Real-World Example: E-Commerce Order Flow](#10-real-world-example-e-commerce-order-flow)

---

## 1. The Problem: Distributed Transactions

Jab tumhare paas multiple microservices hote hain, toh ek business transaction mein **kai services involve** hoti hain. Har service ki apni database hoti hai. Problem ye hai:

**Sabko ya toh sab commit karna hai, ya sabko rollback karna hai.**

```
Traditional Monolith (Single DB):
┌─────────────────────────────────┐
│         Application             │
│  ┌────────┬────────┬────────┐   │
│  │ Order  │Payment │  Stock │   │
│  │ Service│Service │ Service│   │
│  └────┬───┴────┬───┴────┬───┘   │
│       │        │        │        │
│  ┌────▼────────▼────────▼───┐   │
│  │     SINGLE DATABASE       │   │
│  │   ACID Transactions ✅    │   │
│  └───────────────────────────┘   │
└─────────────────────────────────┘
  → @Transactional se kaam ho jata hai
```

```
Microservices (Multiple DBs):
┌──────────┐   ┌──────────┐   ┌──────────┐
│  Order   │   │ Payment  │   │  Stock   │
│ Service  │   │ Service  │   │ Service  │
└────┬─────┘   └────┬─────┘   └────┬─────┘
     │              │              │
┌────▼─────┐  ┌─────▼────┐  ┌─────▼────┐
│ Order DB │  │Payment DB│  │ Stock DB │
└──────────┘  └──────────┘  └──────────┘
  ❌ ACID transactions possible nahi hain across services!
  ❌ Ek service commit ho, dusra fail - kya hoga?
```

**Example Scenario: E-Commerce Order**

```
Step 1: Order Service -> Order create kare     ✅
Step 2: Payment Service -> Charge kare         ✅
Step 3: Stock Service -> Inventory update kare ❌ FAIL!
Step 4: Shipping Service -> Ship kare          ⏭️ Skip

Ab Order aur Payment committed hai, par Stock fail ho gaya!
Data inconsistent ho gaya. Kya karein?
```

---

## 2. Why NOT 2PC in Microservices?

### 2PC (Two-Phase Commit) kya hai?

```
Transaction Coordinator
        │
   ┌────┴────┐
   ▼         ▼
Service A  Service B

Phase 1 (Prepare):
  → Coordinator: "Kya tum commit kar sakte ho?"
  → Service A: "Yes, ready hoon"
  → Service B: "Yes, ready hoon"

Phase 2 (Commit):
  → Coordinator: "Sabne Yes bola, COMMIT!"
  → Sab commit kar dete hain

Agar kisi ne No bola:
  → Coordinator: "ROLLBACK sab!"
```

### 2PC Kyun Nahi Use Karna Chahiye?

| Problem | Explanation |
|---------|-------------|
| **Blocking Protocol** | Ek service slow hai toh sab block ho jayenge |
| **Single Point of Failure** | Coordinator down = sab stuck |
| **Network Partitions** | Network issue mein sab hang ho jata hai |
| **Performance** | Multiple round trips = bahut slow |
| **Tight Coupling** | Services ko ek dusre ke saath coordinate karna padta hai |
| **No Cloud-Native** | Kubernetes, scaling ke saath compatible nahi |
| **Database Lock** | Long-running locks = poor concurrency |

```
CAP Theorem ke context mein:
2PC = Consistency + Partition Tolerance
Par Availability compromise hoti hai!

Microservices mein hum chahte hain:
Availability + Partition Tolerance
Isliye Eventual Consistency accept karte hain
```

---

## 3. What is the Saga Pattern?

Saga pattern ek **design pattern** hai jo distributed transactions ko handle karta hai bina 2PC ke.

### Core Concept:

```
Saga = Sequence of Local Transactions

Har local transaction:
1. Apne service ki database mein update karta hai
2. Event/message publish karta hai next step trigger karne ke liye

Agar koi step fail ho:
→ Compensating transactions chalao (undo karo)
→ Pichle steps ko reverse order mein rollback karo
```

```
Saga Structure:

T1 → T2 → T3 → ... → Tn  (Success path)

Agar T3 fail:
C1 ← C2  (Compensation: T2 undo, T1 undo)

Har Ti ka corresponding Ci hota hai
```

### Key Properties:

```
1. ATOMICITY (Eventual):
   → Ya toh sab complete hoga, ya sab compensated hoga

2. CONSISTENCY (Eventual):
   → Transaction ke baad system consistent state mein aayega
   → Temporary inconsistency allowed (short time ke liye)

3. ISOLATION (Different):
   → Concurrent sagas interfere kar sakte hain
   → Application-level handling needed

4. DURABILITY:
   → Ek baar commit, toh committed (compensation ke alawa)
```

---

## 4. Choreography-Based Saga (Event-Driven)

Isme **koi central coordinator nahi hota**. Har service apne events pe react karti hai.

### Architecture:

```
┌──────────────┐
│ Order Service│
│   (Step 1)   │
└──────┬───────┘
       │ publishes "OrderCreated" event
       ▼
┌──────────────────────────────────────┐
│          Message Broker               │
│      (Kafka / RabbitMQ)               │
└──┬──────────┬───────────┬────────────┘
   │          │           │
   ▼          ▼           ▼
┌──────┐  ┌───────┐  ┌────────┐
│Paymt │  │ Stock │  │ Other  │
│Svc   │  │ Svc   │  │ Svc    │
│(Step2)│  │(Step3)│  │        │
└──────┘  └───────┘  └────────┘
   │           │
   │ publishes │ publishes
   │ "Payment  │ "Stock
   │ Done"     │Reserved"
   ▼           ▼
```

### Implementation Example (Spring Boot):

#### Step 1: Order Service

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        // 1. Order save karo
        Order order = Order.builder()
            .userId(request.getUserId())
            .items(request.getItems())
            .totalAmount(request.getTotalAmount())
            .status(OrderStatus.PENDING)
            .build();
        order = orderRepo.save(order);

        // 2. Event publish karo
        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getId(),
            order.getUserId(),
            order.getItems(),
            order.getTotalAmount()
        );
        kafkaTemplate.send("order-created-topic", 
            order.getId().toString(), event);

        log.info("Order created, event published: {}", order.getId());
        return order;
    }
}
```

#### Step 2: Payment Service (Listens to OrderCreated)

```java
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @KafkaListener(
        topics = "order-created-topic",
        groupId = "payment-service-group"
    )
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        try {
            // 1. Payment process karo
            Payment payment = Payment.builder()
                .orderId(event.getOrderId())
                .amount(event.getTotalAmount())
                .status(PaymentStatus.SUCCESS)
                .build();
            paymentRepo.save(payment);

            // 2. Success event publish karo
            PaymentCompletedEvent successEvent = 
                new PaymentCompletedEvent(event.getOrderId());
            kafkaTemplate.send("payment-completed-topic",
                event.getOrderId().toString(), successEvent);

            log.info("Payment done for order: {}", event.getOrderId());

        } catch (Exception e) {
            // 3. Payment fail - compensation event publish karo
            PaymentFailedEvent failEvent = new PaymentFailedEvent(
                event.getOrderId(),
                e.getMessage()
            );
            kafkaTemplate.send("payment-failed-topic",
                event.getOrderId().toString(), failEvent);

            log.error("Payment failed for order: {}", event.getOrderId());
        }
    }
}
```

#### Step 3: Inventory Service (Listens to PaymentCompleted)

```java
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepo;
    private final KafkaTemplate<String, InventoryEvent> kafkaTemplate;

    @KafkaListener(
        topics = "payment-completed-topic",
        groupId = "inventory-service-group"
    )
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        try {
            // 1. Stock check karo
            Inventory inventory = inventoryRepo
                .findByOrderId(event.getOrderId())
                .orElseThrow(() -> new InventoryException("Not found"));

            if (inventory.getQuantity() < event.getRequiredQuantity()) {
                throw new InsufficientStockException("Stock kam hai!");
            }

            // 2. Stock update karo
            inventory.setQuantity(
                inventory.getQuantity() - event.getRequiredQuantity()
            );
            inventoryRepo.save(inventory);

            // 3. Success event
            StockReservedEvent successEvent = 
                new StockReservedEvent(event.getOrderId());
            kafkaTemplate.send("stock-reserved-topic",
                event.getOrderId().toString(), successEvent);

        } catch (Exception e) {
            // 4. Fail event
            StockReservationFailedEvent failEvent = 
                new StockReservationFailedEvent(
                    event.getOrderId(), e.getMessage()
                );
            kafkaTemplate.send("stock-failed-topic",
                event.getOrderId().toString(), failEvent);
        }
    }
}
```

#### Step 4: Compensation - Order Service (Listens to PaymentFailed / StockFailed)

```java
@Service
@RequiredArgsConstructor
public class OrderCompensationHandler {

    private final OrderRepository orderRepo;
    private final KafkaTemplate<String, CompensatingEvent> kafkaTemplate;

    @KafkaListener(
        topics = {"payment-failed-topic", "stock-failed-topic"},
        groupId = "order-compensation-group"
    )
    @Transactional
    public void handleFailure(FailureEvent event) {
        // 1. Order status update karo
        Order order = orderRepo.findById(event.getOrderId())
            .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason("Saga failed: " + event.getReason());
        orderRepo.save(order);

        log.warn("Order cancelled due to saga failure: {}", event.getOrderId());
    }
}
```

#### Step 5: Compensation - Payment Service (Listens to StockFailed)

```java
@Service
@RequiredArgsConstructor
public class PaymentCompensationHandler {

    private final PaymentRepository paymentRepo;

    @KafkaListener(
        topics = "stock-failed-topic",
        groupId = "payment-compensation-group"
    )
    @Transactional
    public void handleStockFailure(StockFailedEvent event) {
        // Payment reverse karo (refund)
        Payment payment = paymentRepo.findByOrderId(event.getOrderId())
            .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundReason("Stock unavailable, refunding payment");
        paymentRepo.save(payment);

        log.warn("Payment refunded for order: {} due to stock failure", 
            event.getOrderId());
    }
}
```

### Choreography Flow Diagram:

```
Order Service          Payment Service          Inventory Service
     │                        │                        │
     │── Create Order ────────│                        │
     │                        │                        │
     │── Publish ──────────────────────────────────────│
     │   "OrderCreated" ───────────────────────────────│
     │                        │                        │
     │          ◄── Listen ───│                        │
     │          Process Payment                        │
     │                        │                        │
     │          Publish ───────────────────────────────│
     │          "PaymentCompleted" ────────────────────│
     │                        │                        │
     │                        │          ◄── Listen ───│
     │                        │          Reserve Stock  │
     │                        │                        │
     │                        │          Publish ──────│
     │                        │          "StockReserved"
     │                        │                        │
     │◄───────────────────────│◄───────────────────────│
     │  (All steps complete)  │                        │
```

### Choreography Pros and Cons:

| Pros | Cons |
|------|------|
| Simple to implement for small sagas | Cyclomatic complexity badha jayega |
| No single point of failure | Debug karna mushkil (kaun kya trigger kar raha hai?) |
| Decoupled services | Circular dependency risk |
| Easy to add new services | Saga flow samajhna mushkil (koi diagram nahi) |
| No extra infrastructure | Testing complex |

---

## 5. Orchestration-Based Saga (Central Coordinator)

Isme ek **Saga Orchestrator** hota hai jo sabko control karta hai. Wo decide karta hai kis step ko chalana hai.

### Architecture:

```
┌───────────────────────────────┐
│      Saga Orchestrator        │
│   (Central Coordinator)       │
│                               │
│  Logic:                       │
│  1. Call Order Service        │
│  2. If success → Call Payment │
│  3. If success → Call Stock   │
│  4. If fail → Compensate      │
└───────┬───────┬───────┬───────┘
        │       │       │
        ▼       ▼       ▼
   ┌────┐  ┌────┐  ┌────┐
   │Order│  │Pay │  │Stk │
   │Svc  │  │Svc │  │Svc │
   └─────┘  └────┘  └────┘
```

### Implementation:

#### Step 1: Saga State Machine Definition

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderSagaState {
    private String sagaId;
    private String orderId;
    private Long userId;
    private BigDecimal totalAmount;
    private SagaState currentState;
    private String lastFailedStep;
    private String failureReason;

    public enum SagaState {
        ORDER_CREATED,
        PAYMENT_PROCESSING,
        PAYMENT_COMPLETED,
        INVENTORY_RESERVING,
        INVENTORY_RESERVED,
        COMPLETED,
        COMPENSATING,
        FAILED
    }
}
```

#### Step 2: Saga Orchestrator

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderSagaOrchestrator {

    private final OrderClient orderClient;
    private final PaymentClient paymentClient;
    private final InventoryClient inventoryClient;
    private final SagaStateRepository sagaRepo;

    // Yeh method saga start karta hai
    public SagaExecutionResult startSaga(CreateOrderRequest request) {
        SagaExecutionResult result = new SagaExecutionResult();
        OrderSagaState state = new OrderSagaState();
        state.setSagaId(UUID.randomUUID().toString());
        state.setOrderId(request.getOrderId());
        state.setUserId(request.getUserId());
        state.setTotalAmount(request.getTotalAmount());

        try {
            // STEP 1: Create Order
            state.setCurrentSagaState(
                OrderSagaState.SagaState.ORDER_CREATED);
            saveState(state);

            OrderResponse order = orderClient.createOrder(request);
            state.setOrderId(order.getOrderId());
            log.info("Step 1 done: Order created {}", order.getOrderId());

            // STEP 2: Process Payment
            state.setCurrentState(
                OrderSagaState.SagaState.PAYMENT_PROCESSING);
            saveState(state);

            PaymentResponse payment = paymentClient.processPayment(
                new PaymentRequest(order.getOrderId(), 
                    request.getTotalAmount())
            );
            state.setCurrentState(
                OrderSagaState.SagaState.PAYMENT_COMPLETED);
            saveState(state);
            log.info("Step 2 done: Payment completed");

            // STEP 3: Reserve Inventory
            state.setCurrentState(
                OrderSagaState.SagaState.INVENTORY_RESERVING);
            saveState(state);

            InventoryResponse inventory = 
                inventoryClient.reserveStock(
                    new StockRequest(order.getOrderId(), 
                        request.getItems())
                );
            state.setCurrentState(
                OrderSagaState.SagaState.INVENTORY_RESERVED);
            saveState(state);
            log.info("Step 3 done: Inventory reserved");

            // SUCCESS
            state.setCurrentState(
                OrderSagaState.SagaState.COMPLETED);
            saveState(state);
            result.setSuccess(true);
            result.setMessage("Order saga completed successfully");

        } catch (PaymentException e) {
            log.error("Payment failed, compensating...", e);
            compensate(state, CompensationStep.FROM_PAYMENT);
            result.setSuccess(false);
            result.setFailureStep("PAYMENT");
            result.setReason(e.getMessage());

        } catch (InventoryException e) {
            log.error("Inventory reservation failed, compensating...", e);
            compensate(state, CompensationStep.FROM_INVENTORY);
            result.setSuccess(false);
            result.setFailureStep("INVENTORY");
            result.setReason(e.getMessage());
        }

        return result;
    }

    // Compensation logic - reverse order mein undo karna
    private void compensate(OrderSagaState state, 
                            CompensationStep fromStep) {
        state.setCurrentState(
            OrderSagaState.SagaState.COMPENSATING);
        saveState(state);

        switch (fromStep) {
            case FROM_INVENTORY:
                // Inventory compensation nahi chahiye 
                // (reservation tha, actual deduction nahi)
                log.info("Inventory reservation auto-expired");

            case FROM_PAYMENT:
                // Payment refund karo
                try {
                    paymentClient.refundPayment(
                        new RefundRequest(state.getOrderId())
                    );
                    log.info("Payment refunded for order: {}", 
                        state.getOrderId());
                } catch (Exception e) {
                    log.error("Refund failed! Manual intervention needed", e);
                    // Yeh critical hai - alert bhejo
                    alertService.sendCriticalAlert(
                        "Payment refund failed for order: " 
                        + state.getOrderId());
                }

                // Order cancel karo
                orderClient.cancelOrder(state.getOrderId());
                log.info("Order cancelled");
                break;

            default:
                log.warn("No compensation needed");
        }

        state.setCurrentState(
            OrderSagaState.SagaState.FAILED);
        saveState(state);
    }
}
```

#### Step 3: REST Controller to Trigger Saga

```java
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderSagaOrchestrator orchestrator;

    @PostMapping
    public ResponseEntity<ApiResponse> placeOrder(
            @RequestBody CreateOrderRequest request) {

        SagaExecutionResult result = 
            orchestrator.startSaga(request);

        if (result.isSuccess()) {
            return ResponseEntity.ok(
                ApiResponse.success(result.getMessage()));
        } else {
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .body(ApiResponse.error(
                    "Order failed at step: " + result.getFailureStep() 
                    + " - Reason: " + result.getReason()));
        }
    }
}
```

### Orchestration Flow Diagram:

```
Client              Orchestrator              Order Svc    Payment Svc   Inventory Svc
  │                      │                        │            │              │
  │── Place Order ──────►│                        │            │              │
  │                      │── Create Order ───────►│            │              │
  │                      │◄── Order Created ──────│            │              │
  │                      │                        │            │              │
  │                      │── Process Payment ─────────────────►│              │
  │                      │◄── Payment Done ────────────────────│              │
  │                      │                        │            │              │
  │                      │── Reserve Stock ──────────────────────────────────►│
  │                      │◄── Stock Reserved ────────────────────────────────│
  │                      │                        │            │              │
  │◄── Order Confirmed ──│                        │            │              │
  │                      │                        │            │              │


Agar Payment FAIL:
  │                      │                        │            │              │
  │                      │── Create Order ───────►│            │              │
  │                      │◄── Order Created ──────│            │              │
  │                      │                        │            │              │
  │                      │── Process Payment ─────────────────►│              │
  │                      │◄── Payment FAILED ──────────────────│              │
  │                      │                        │            │              │
  │                      │── Cancel Order ───────►│            │              │
  │                      │  (Compensation)        │            │              │
  │                      │                        │            │              │
  │◄── Order Failed ─────│                        │            │              │
```

### Orchestration Pros and Cons:

| Pros | Cons |
|------|------|
| Centralized control = easy to debug | Orchestrator single point of failure |
| Clear saga flow (easy to understand) | Orchestrator services se coupled ho jata hai |
| Easy to add retry/recovery logic | Extra service deploy karna padta hai |
| Better monitoring & tracing | Orchestrator bottleneck ban sakta hai |
| No circular dependencies | |

---

## 6. Choreography vs Orchestration - Comparison

| Aspect | Choreography | Orchestration |
|--------|-------------|---------------|
| **Control** | Decentralized (har service khud decide karti hai) | Centralized (orchestrator decide karta hai) |
| **Coupling** | Services events ke through loosely coupled | Orchestrator services ko jaanta hai |
| **Complexity** | Chote sagas mein simple, bade mein complex | Hamesha clear structure |
| **Debugging** | Difficult (events trace karna padta hai) | Easy (orchestrator logs dekho) |
| **Adding Steps** | Easy (naya listener add karo) | Orchestrator modify karna padta hai |
| **Single Point of Failure** | Nahi | Haan (orchestrator) |
| **Best For** | 2-4 steps, simple flows | 5+ steps, complex business logic |
| **Testing** | Integration testing complex | Unit testing orchestrator easy |
| **Scalability** | Highly scalable | Orchestrator pe load aata hai |
| **Visibility** | Kam (distributed tracing needed) | Zyada (central logs) |

### Kab Kya Use Karein?

```
Choreography choose karo jab:
├── Saga steps kam hain (2-4)
├── Services naturally event-driven hain
├── Tumhe maximum decoupling chahiye
└── Team comfortable with event-driven architecture

Orchestration choose karo jab:
├── Saga steps zyada hain (5+)
├── Complex compensation logic hai
├── Centralized monitoring chahiye
├── Business flow clear hona chahiye
└── Debugging easy honi chahiye
```

---

## 7. Compensating Transactions

Compensating transaction ka matlab hai: **jo kaam kiya hai use undo karna**.

### Key Rules:

```
1. Har step ka compensating action define karo
2. Compensation REVERSE ORDER mein chalana hai
3. Compensations idempotent hone chahiye
4. Compensation kabhi fail nahi hona chahiye
   (agar fail ho toh manual intervention / retry)
```

### Compensation Table for E-Commerce:

| Step | Action | Compensation | Notes |
|------|--------|-------------|-------|
| 1. Create Order | Order PENDING | Order CANCEL | Simple status update |
| 2. Process Payment | Charge customer | Refund payment | Refund mein time lag sakta hai |
| 3. Reserve Stock | Decrement inventory | Increment inventory | Reservation auto-expire bhi ho sakta hai |
| 4. Create Shipment | Generate shipping label | Void shipping label | Carrier API call needed |
| 5. Send Email | Welcome email | (N/A - can't undo) | Compensate nahi hota |

### Compensation Implementation:

```java
@Service
@Slf4j
public class CompensationManager {

    // Compensation actions ka registry
    private final Map<String, CompensatingAction> actions = new LinkedHashMap<>();

    // Register karna
    public void registerAction(String stepName, CompensatingAction action) {
        actions.put(stepName, action);
    }

    // Execute karna - reverse order mein!
    public void compensate(List<String> completedSteps, SagaContext context) {
        // Reverse order mein iterate karo
        List<String> reversedSteps = new ArrayList<>(completedSteps);
        Collections.reverse(reversedSteps);

        for (String step : reversedSteps) {
            CompensatingAction action = actions.get(step);
            if (action == null) {
                log.warn("No compensation action for step: {}", step);
                continue;
            }

            try {
                log.info("Compensating step: {}", step);
                action.compensate(context);
                log.info("Compensation successful for: {}", step);
            } catch (Exception e) {
                log.error("Compensation FAILED for step: {}", step, e);
                // Critical: Compensation fail hone pe alert
                alertService.sendCriticalAlert(
                    "Compensation failed for: " + step + 
                    " - Manual intervention required!"
                );
                // Retry logic yahan add kar sakte ho
                throw new CompensationException(
                    "Compensation failed for step: " + step, e);
            }
        }
    }
}

// Interface
public interface CompensatingAction {
    void compensate(SagaContext context) throws Exception;
}

// Implementations
@Component
public class CancelOrderCompensation implements CompensatingAction {
    private final OrderRepository orderRepo;

    @Override
    public void compensate(SagaContext context) {
        Order order = orderRepo.findById(context.getOrderId())
            .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason("Saga compensation");
        orderRepo.save(order);
    }
}

@Component
public class RefundPaymentCompensation implements CompensatingAction {
    private final PaymentService paymentService;

    @Override
    public void compensate(SagaContext context) {
        paymentService.refund(context.getOrderId(), context.getAmount());
    }
}

@Component
public class ReleaseInventoryCompensation implements CompensatingAction {
    private final InventoryService inventoryService;

    @Override
    public void compensate(SagaContext context) {
        inventoryService.releaseReservation(
            context.getOrderId(), context.getItems());
    }
}
```

### Compensation Flow:

```
Saga Execution:
T1 ────► T2 ────► T3 ────► T4 (FAIL!)
                │
                │ Compensation triggers
                ▼
            C2 ◄──── C1
            (reverse order)

Important:
- T3 ka compensation nahi chahiye (T3 run hi nahi hua)
- T4 fail hua toh uska bhi compensation nahi (wo toh failed hai)
- Sirf successfully completed steps ka compensation hota hai
```

---

## 8. Implementing with Axon Framework

[Axon Framework](https://axoniq.io/) ek powerful Java framework hai jo **CQRS + Event Sourcing + Saga** ko support karta hai.

### Setup:

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.axonframework</groupId>
    <artifactId>axon-spring-boot-starter</artifactId>
    <version>4.9.0</version>
</dependency>
```

### Step 1: Commands Define Karo

```java
// Command classes
@Data
public class CreateOrderCommand {
    @TargetAggregateIdentifier
    private String orderId;
    private String userId;
    private BigDecimal totalAmount;
}

@Data
public class ValidatePaymentCommand {
    @TargetAggregateIdentifier
    private String paymentId;
    private String orderId;
    private BigDecimal amount;
}

@Data
public class ReserveStockCommand {
    @TargetAggregateIdentifier
    private String stockId;
    private String orderId;
    private List<OrderItem> items;
}
```

### Step 2: Events Define Karo

```java
// Event classes
@Data
public class OrderCreatedEvent {
    private String orderId;
    private String userId;
    private BigDecimal totalAmount;
}

@Data
public class PaymentValidatedEvent {
    private String paymentId;
    private String orderId;
    private boolean validated;
}

@Data
public class StockReservedEvent {
    private String stockReservationId;
    private String orderId;
}

@Data
public class OrderCancelledEvent {
    private String orderId;
    private String reason;
}
```

### Step 3: Aggregate (Order)

```java
@Aggregate
public class OrderAggregate {

    @AggregateIdentifier
    private String orderId;
    private String userId;
    private BigDecimal totalAmount;
    private OrderStatus status;

    public OrderAggregate() {}

    // Command Handler
    @CommandHandler
    public OrderAggregate(CreateOrderCommand command) {
        // Event apply karo
        apply(new OrderCreatedEvent(
            command.getOrderId(),
            command.getUserId(),
            command.getTotalAmount()
        ));
    }

    // Event Handler - state update hota hai
    @EventSourcingHandler
    public void on(OrderCreatedEvent event) {
        this.orderId = event.getOrderId();
        this.userId = event.getUserId();
        this.totalAmount = event.getTotalAmount();
        this.status = OrderStatus.CREATED;
    }
}
```

### Step 4: Saga Definition (Axon Style)

```java
@Saga
@Slf4j
public class OrderProcessingSaga {

    @SagaEventHandler(associationProperty = "orderId")
    public void handle(OrderCreatedEvent event) {
        log.info("Saga started for order: {}", event.getOrderId());

        // Payment validate command dispatch karo
        ValidatePaymentCommand cmd = new ValidatePaymentCommand(
            UUID.randomUUID().toString(),
            event.getOrderId(),
            event.getTotalAmount()
        );
        commandGateway.send(cmd);
    }

    @SagaEventHandler(associationProperty = "orderId")
    public void handle(PaymentValidatedEvent event) {
        if (!event.isValidated()) {
            log.error("Payment validation failed, compensating...");
            // Compensate: Order cancel karo
            commandGateway.send(new CancelOrderCommand(
                event.getOrderId(), "Payment validation failed"
            ));
            return;
        }

        log.info("Payment validated, reserving stock...");
        // Next step: Stock reserve
        ReserveStockCommand cmd = new ReserveStockCommand(
            UUID.randomUUID().toString(),
            event.getOrderId(),
            event.getItems()
        );
        commandGateway.send(cmd);
    }

    @SagaEventHandler(associationProperty = "orderId")
    public void handle(StockReservedEvent event) {
        log.info("Stock reserved, saga completed for order: {}", 
            event.getOrderId());
        // Saga complete
        SagaLifecycle.end();
    }

    @SagaEventHandler(associationProperty = "orderId")
    public void handle(StockReservationFailedEvent event) {
        log.error("Stock reservation failed, compensating...");

        // Compensate: Payment reverse karo
        commandGateway.send(new RefundPaymentCommand(
            event.getOrderId()
        ));

        // Compensate: Order cancel karo
        commandGateway.send(new CancelOrderCommand(
            event.getOrderId(), "Stock unavailable"
        ));
    }

    // End-to-end lifecycle
    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void handle(OrderCompletedEvent event) {
        log.info("Saga ended successfully for order: {}", event.getOrderId());
    }
}
```

### Step 5: Configuration

```java
@Configuration
public class AxonConfig {

    @Bean
    public TokenStore tokenStore(EntityManagerProvider emProvider) {
        return JpaTokenStore.builder()
            .entityManagerProvider(emProvider)
            .serializer(JacksonSerializer.defaultSerializer())
            .build();
    }

    // Saga event store config
    @Bean
    public SagaStore<?> sagaStore(DataSource dataSource) {
        return JpaSagaStore.builder()
            .dataSource(dataSource)
            .serializer(JacksonSerializer.defaultSerializer())
            .build();
    }
}
```

### Axon Framework Architecture:

```
┌─────────────────────────────────────────────┐
│              Axon Server                     │
│  (Event Store + Message Router)              │
├─────────────────────────────────────────────┤
│                                              │
│  Command Model (Write Side)                 │
│  ┌──────────────┐  ┌──────────────┐        │
│  │ Command GW   │  │  Aggregates  │        │
│  │              │  │  (Event Src) │        │
│  └──────┬───────┘  └──────┬───────┘        │
│         │                 │                 │
│  Query Model (Read Side)  │                 │
│  ┌──────────────┐  ┌──────▼───────┐        │
│  │  Projections │  │  Event Store │        │
│  │  (View DBs)  │  │              │        │
│  └──────────────┘  └──────────────┘        │
│                                              │
│  ┌──────────────────────────────┐           │
│  │       Saga Orchestrator      │           │
│  │  (State machine tracking)    │           │
│  └──────────────────────────────┘           │
└─────────────────────────────────────────────┘
```

---

## 9. Best Practices

### 1. Saga Design

```markdown
DO:
├── Har saga ka ek clear trigger event hona chahiye
├── Har step idempotent hona chahiye
├── Compensation logic robust hona chahiye
├── Saga state persist karo (recovery ke liye)
├── Timeout mechanism add karo
└── Monitoring & alerting setup karo

DON'T:
├── Saga mein user interaction mat rakho
├── Compensation ko skip mat karo
├── Infinite retry loops mat banao
├── External APIs ko directly saga mein mat rakho
│   (adapter/pattern use karo)
└── Long-running transactions mat banao
```

### 2. Event Design

```java
// GOOD - Immutable, explicit events
@Data
@AllArgsConstructor
public class OrderCreatedEvent {
    private final String orderId;          // Business key
    private final String userId;           // Correlation ID
    private final BigDecimal amount;       // Only needed data
    private final Instant timestamp;       // When it happened
    private final int schemaVersion = 1;   // Schema versioning
}

// BAD - Mutable, vague events
@Data
public class OrderEvent {
    public String orderId;       // Mutable - bad!
    public Map<String, Object> data;  // Type unsafe!
}
```

### 3. Error Handling

```java
@Service
public class ResilientSagaHandler {

    @Retryable(
        value = {TimeoutException.class, 
                 TransientDataAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void handleWithRetry(FailureEvent event) {
        // Yeh method automatic retry karega
        processSagaStep(event);
    }

    @Recover
    public void recoverFromFailure(FailureEvent event, 
                                    Exception exception) {
        // Retry exhaust hone ke baad yeh chalega
        log.error("All retries failed, triggering compensation", 
            exception);
        compensateSaga(event);
    }
}
```

### 4. Monitoring

```java
@Component
public class SagaMetrics {

    private final MeterRegistry registry;

    public void recordSagaStart(String sagaType) {
        registry.counter("saga.started", 
            "type", sagaType).increment();
    }

    public void recordSagaSuccess(String sagaType) {
        registry.counter("saga.completed", 
            "type", sagaType).increment();
    }

    public void recordSagaCompensation(String sagaType, 
                                        String failedStep) {
        registry.counter("saga.compensated",
            "type", sagaType,
            "failedStep", failedStep).increment();
    }

    public void recordSagaDuration(String sagaType, 
                                    long durationMs) {
        registry.timer("saga.duration",
            "type", sagaType).record(
                Duration.ofMillis(durationMs));
    }
}
```

---

## 10. Real-World Example: E-Commerce Order Flow

### Complete Saga Definition:

```
E-Commerce Order Saga:

┌─────────────────────────────────────────────────────────────┐
│                                                              │
│  Start: User places order                                   │
│    │                                                         │
│    ▼                                                         │
│  Step 1: Create Order (PENDING)                             │
│    │                                                         │
│    ▼                                                         │
│  Step 2: Validate Payment                                   │
│    ├─ Success ──► Step 3: Reserve Inventory                  │
│    │                   ├─ Success ──► Step 4: Create Shipment│
│    │                   │                ├─ Success ► Step 5: │
│    │                   │                │          Send Email│
│    │                   │                │          COMPLETE ✓│
│    │                   │                │                    │
│    │                   │                └─ Fail ──► Comp:    │
│    │                   │                           Refund    │
│    │                   │                           Cancel    │
│    │                   │                                    │
│    │                   └─ Fail ──► Comp: Cancel Order        │
│    │                                                         │
│    └─ Fail ──► Comp: Cancel Order                           │
│                                                              │
└─────────────────────────────────────────────────────────────┘

Compensation Actions:
├── Cancel Order → Set status = CANCELLED
├── Refund Payment → Initiate refund via payment gateway
├── Release Inventory → Increment available stock
├── Void Shipment → Cancel shipping label with carrier
└── Send Email → (N/A - no compensation needed)
```

### Complete Implementation:

```java
@Data
@Builder
public class OrderSagaContext {
    private String sagaId;
    private String orderId;
    private String userId;
    private BigDecimal totalAmount;
    private List<OrderItem> items;

    // Track completed steps for compensation
    private List<String> completedSteps = new ArrayList<>();

    // Current state
    private SagaStatus status;
    private String failureReason;

    public void markStepCompleted(String step) {
        completedSteps.add(step);
    }
}

@Service
@Slf4j
public class ECommerceOrderSaga {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final ShippingService shippingService;
    private final NotificationService notificationService;
    private final SagaRepository sagaRepo;
    private final CompensationManager compensationManager;

    @Transactional
    public SagaResult execute(OrderSagaContext context) {
        context.setSagaId(UUID.randomUUID().toString());
        context.setStatus(SagaStatus.STARTED);
        sagaRepo.save(context);

        try {
            // Step 1: Create Order
            executeStep(context, "CREATE_ORDER", () -> {
                orderService.createOrder(context);
            });

            // Step 2: Process Payment
            executeStep(context, "PROCESS_PAYMENT", () -> {
                paymentService.charge(context);
            });

            // Step 3: Reserve Inventory
            executeStep(context, "RESERVE_INVENTORY", () -> {
                inventoryService.reserve(context);
            });

            // Step 4: Create Shipment
            executeStep(context, "CREATE_SHIPMENT", () -> {
                shippingService.createShipment(context);
            });

            // Step 5: Send Confirmation
            executeStep(context, "SEND_CONFIRMATION", () -> {
                notificationService.sendOrderConfirmation(context);
            });

            // All steps completed
            context.setStatus(SagaStatus.COMPLETED);
            sagaRepo.save(context);

            return SagaResult.success(context.getSagaId());

        } catch (Exception e) {
            log.error("Saga failed at step: {}", 
                context.getCompletedSteps().isEmpty() 
                    ? "CREATE_ORDER" 
                    : context.getCompletedSteps()
                        .get(context.getCompletedSteps().size() - 1), 
                e);

            // Compensate in reverse order
            compensationManager.compensate(
                context.getCompletedSteps(), context);

            context.setStatus(SagaStatus.COMPENSATED);
            context.setFailureReason(e.getMessage());
            sagaRepo.save(context);

            return SagaResult.failure(context.getSagaId(), e.getMessage());
        }
    }

    private void executeStep(OrderSagaContext context,
                             String stepName,
                             Runnable action) {
        log.info("Executing step: {}", stepName);
        action.run();
        context.markStepCompleted(stepName);
        context.setStatus(SagaStatus.valueOf(stepName));
        sagaRepo.save(context);
        log.info("Step {} completed", stepName);
    }
}
```

---

## Summary / Cheat Sheet

```
┌─────────────────────────────────────────────────────────────┐
│                    SAGA PATTERN CHEAT SHEET                  │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  WHEN: Multiple services, single business transaction        │
│                                                              │
│  WHY NOT 2PC: Blocking, slow, single point of failure        │
│                                                              │
│  APPROACH:                                                   │
│  ├─ Choreography: Event-driven, no coordinator               │
│  │   → Best for 2-4 simple steps                             │
│  └─ Orchestration: Central coordinator                       │
│      → Best for 5+ complex steps                             │
│                                                              │
│  COMPENSATION:                                               │
│  ├─ Reverse order mein undo karo                             │
│  ├─ Har step ka compensating action define karo              │
│  └─ Compensation kabhi fail nahi honi chahiye                │
│                                                              │
│  KEY RULES:                                                  │
│  ├─ Steps idempotent hone chahiye                            │
│  ├─ State persist karo (recovery ke liye)                    │
│  ├─ Timeout mechanism rakho                                  │
│  └─ Monitoring & alerting setup karo                         │
│                                                              │
│  TOOLS:                                                      │
│  ├─ Axon Framework (Java, CQRS + Event Sourcing + Saga)     │
│  ├─ Temporal / Cadence (Workflow engine)                     │
│  ├─ Apache Camel (Integration)                               │
│  └─ Custom implementation with Kafka                         │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

> **Next:** [08 - Event-Driven Architecture](./08-Event-Driven-Architecture.md) → Event Sourcing, CQRS, Outbox Pattern

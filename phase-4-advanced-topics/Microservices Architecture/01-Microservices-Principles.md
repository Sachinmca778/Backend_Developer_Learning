# Microservices Principles

## Status: Not Started

---

## Table of Contents

1. [What are Microservices?](#what-are-microservices)
2. [Single Responsibility Principle](#single-responsibility-principle)
3. [Loose Coupling, High Cohesion](#loose-coupling-high-cohesion)
4. [Independent Deployability](#independent-deployability)
5. [Decentralized Data Management](#decentralized-data-management)
6. [Failure Isolation](#failure-isolation)
7. [Polyglot Persistence](#polyglot-persistence)
8. [Bounded Context (DDD)](#bounded-context-ddd)
9. [Conway's Law](#conways-law)
10. [Summary & Best Practices](#summary--best-practices)

---

## What are Microservices?

**Matlab:** Ek large application ko chhote-chhote independent services mein tod dena, jahan har service ek specific business capability ko handle karta hai.

### Monolith vs Microservices

```
MONOLITHIC ARCHITECTURE
┌─────────────────────────────────────────────┐
│              Monolith Application             │
│  ┌──────────┬──────────┬──────────────────┐  │
│  │  User    │  Order   │   Payment        │  │
│  │  Module  │  Module  │   Module         │  │
│  ├──────────┴──────────┴──────────────────┤  │
│  │         Single Database                 │  │
│  └─────────────────────────────────────────┘  │
│                                                 │
│  ❌ Ek module mein bug = pura app down         │
│  ❌ Ek team doosri team ka code block karti hai │
│  ❌ Scaling = pura app scale karo               │
└─────────────────────────────────────────────────┘


MICROSERVICES ARCHITECTURE
┌─────────────────────────────────────────────────────────────┐
│                      API Gateway                             │
└─────────────┬──────────────┬──────────────┬─────────────────┘
              │              │              │
     ┌────────▼────┐ ┌──────▼──────┐ ┌─────▼──────┐
     │   User      │ │   Order     │ │  Payment   │
     │   Service   │ │   Service   │ │  Service   │
     │             │ │             │ │            │
     │ ┌─────────┐ │ │ ┌─────────┐ │ │ ┌────────┐ │
     │ │User DB  │ │ │ │Order DB │ │ │ │Pay DB  │ │
     │ └─────────┘ │ │ └─────────┘ │ │ └────────┘ │
     └─────────────┘ └─────────────┘ └────────────┘

  ✅ Har service independent deploy hoti hai
  ✅ Ek service fail ho baaki kaam karti hain
  ✅ Har service alag tech stack use kar sakti hai
  ✅ Teams independently kaam kar sakti hain
```

### Microservices Key Characteristics

| Characteristic | Description |
|----------------|-------------|
| **Single Responsibility** | Har service ek hi kaam karti hai — aur woh acche se |
| **Loose Coupling** | Services ek doosre se loosely coupled hoti hain |
| **Independent Deploy** | Ek service ko bina baaki services ko touch kiye deploy kar sakte ho |
| **Decentralized Data** | Har service apna data manage karti hai — shared database nahi |
| **Failure Isolation** | Ek service ka failure doosri services ko affect nahi karta |
| **Polyglot** | Har service apni language/database choose kar sakti hai |

---

## Single Responsibility Principle

**Matlab:** Ek service ka ek hi reason to change hona chahiye. Ek service = ek business capability.

### SRP in Monolith (BAD)

```java
// ❌ GOD CLASS — sab kuch ek hi class mein
@Service
public class GodService {

    public void registerUser(User user) { /* user logic */ }
    public void sendWelcomeEmail(User user) { /* email logic */ }
    public void createOrder(Order order) { /* order logic */ }
    public void processPayment(Order order) { /* payment logic */ }
    public void generateInvoice(Order order) { /* invoice logic */ }
    public void updateInventory(Order order) { /* inventory logic */ }
    public void sendShippingNotification(Order order) { /* notification logic */ }
    public void generateSalesReport() { /* reporting logic */ }
    public void calculateTax(Order order) { /* tax logic */ }
    public void applyDiscount(Order order) { /* discount logic */ }
}

// Problem:
// - User feature change karo toh Order module ka test bhi chahiye
// - Ek bug fix = pura app redeploy
// - Multiple teams ek hi file mein conflict
```

### SRP in Microservices (GOOD)

```
┌──────────────────────────────────────────────────────┐
│              Single Responsibility Model               │
├──────────────┬──────────────┬─────────────────────────┤
│ User Service │Order Service │   Notification Service   │
├──────────────┼──────────────┼─────────────────────────┤
│ • Register   │ • Create     │ • Send Email             │
│ • Login      │   Order      │ • Send SMS               │
│ • Profile    │ • Cancel     │ • Push Notification      │
│ • Auth       │ • Track      │ • Webhook                │
│ • Password   │ • History    │ • Template Management    │
└──────────────┴──────────────┴─────────────────────────┘

Har service ka EK hi reason to change:
- User Service → User management change hone pe
- Order Service → Order logic change hone pe
- Notification Service → Notification channel change hone pe
```

### Java Example — Proper SRP

```java
// ✅ User Service — sirf user-related kaam
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerUser(RegisterRequest request) {
        User user = User.builder()
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .role("USER")
            .build();
        return userRepository.save(user);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
    }

    public User updateUser(Long id, UpdateRequest request) {
        User user = getUserById(id);
        user.setEmail(request.email());
        return userRepository.save(user);
    }
}

// ✅ Order Service — sirf order-related kaam
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate; // UserService call karne ke liye

    public Order createOrder(OrderRequest request) {
        // User verify karo
        User user = restTemplate.getForObject(
            "http://user-service/users/" + request.userId(), User.class);

        if (user == null) {
            throw new UserNotFoundException("User not found");
        }

        Order order = Order.builder()
            .userId(user.getId())
            .items(request.items())
            .status("CREATED")
            .totalAmount(calculateTotal(request.items()))
            .build();

        return orderRepository.save(order);
    }

    private BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream()
            .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

### SRP Best Practices

| Rule | Description |
|------|-------------|
| **One business capability** | Ek service = ek domain (User, Order, Payment, etc.) |
| **One reason to change** | Agar do alag reasons se change ho rahi hai, toh todo |
| **Small team ownership** | Ek service = 1-2 developers (pizza rule: 2 pizza se zyada log = service badi hai) |
| **Cohesive functionality** | Service ke andar ke methods related hone chahiye |
| **API boundaries clear** | Har service ka API clearly define karo — kya expose karna hai, kya nahi |

---

## Loose Coupling, High Cohesion

### Loose Coupling

**Matlab:** Services ek doosre se minimally depend karein. Ek service change karo toh doosri services ko change na karna pade.

### Tight Coupling (BAD)

```java
// ❌ Tight Coupling — Service A ko Service B ki internal details pata hain
@RestController
@RequiredArgsConstructor
public class OrderController {

    private final UserRepository userRepository; // ❌ Direct dusre service ka DB access
    private final EmailSender emailSender;       // ❌ Direct implementation dependency

    public Order createOrder(@RequestBody OrderRequest request) {
        // User ko directly DB se fetch — tightly coupled
        User user = userRepository.findById(request.userId()).orElseThrow();

        // Email sender ki specific implementation pe depend
        emailSender.sendEmail(user.getEmail(), "Order Created");

        return orderRepository.save(order);
    }
}

// Problem:
// - UserRepository change hua → OrderService break hoga
// - EmailSender ka implementation change hua → OrderService update karna padega
// - Test karna muskil — mock bahut saare dependencies
```

### Loose Coupling (GOOD)

```java
// ✅ Loose Coupling — interfaces aur events se communication
@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    public Order createOrder(@RequestBody OrderRequest request) {
        // OrderService ke through — implementation details hidden
        return orderService.createOrder(request);
    }
}

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserServiceClient userServiceClient;  // Interface/Feign client
    private final ApplicationEventPublisher eventPublisher;

    public Order createOrder(OrderRequest request) {
        // UserService ko HTTP call se — loose coupling
        UserDTO user = userServiceClient.getUser(request.userId());

        Order order = Order.builder()
            .userId(user.id())
            .items(request.items())
            .status("CREATED")
            .build();

        order = orderRepository.save(order);

        // Event publish karo — Notification service khud listen karega
        eventPublisher.publishEvent(new OrderCreatedEvent(order.getId(), user.email()));

        return order;
    }
}

// ✅ Feign Client — interface-based, loosely coupled
@FeignClient(name = "user-service", url = "${user.service.url}")
public interface UserServiceClient {

    @GetMapping("/api/users/{id}")
    UserDTO getUser(@PathVariable Long id);
}

// ✅ Event Listener — async, loosely coupled
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final EmailService emailService;

    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        emailService.sendEmail(event.userEmail(), "Order Created: " + event.orderId());
    }
}
```

### High Cohesion

**Matlab:** Ek service ke andar ke elements (classes, methods) closely related hone chahiye.

```
HIGH COHESION (✅)                    LOW COHESION (❌)
┌─────────────────────┐              ┌─────────────────────────┐
│   Order Service     │              │   Utility Service       │
│                     │              │                         │
│  OrderController    │              │  sendEmail()            │
│  OrderService       │              │  calculateTax()         │
│  OrderRepository    │              │  generatePDF()          │
│  OrderValidator     │              │  resizeImage()          │
│  OrderMapper        │              │  sendSMS()              │
│                     │              │  parseCSV()             │
│  Sab related ✅     │              │  unrelated methods ❌   │
└─────────────────────┘              └─────────────────────────┘
```

### Coupling vs Cohesion Comparison

| Aspect | Tight Coupling | Loose Coupling |
|--------|---------------|----------------|
| **Dependency** | Direct class/database access | Interface/HTTP/Event-based |
| **Change Impact** | Ek change = multiple services update | Ek change = sirf wohi service |
| **Testing** | Muskil — bahut saare mocks | Aasaan — contract testing |
| **Deployment** | Saath mein deploy karna padega | Independent deploy |
| **Technology Lock** | Same tech stack forced | Different tech stack possible |

| Aspect | High Cohesion | Low Cohesion |
|--------|--------------|--------------|
| **Focus** | Ek domain pe focused | Multiple unrelated domains |
| **Understandability** | Code samajhna aasaan | Code samajhna mushkil |
| **Maintainability** | Easy to maintain | Hard to maintain |
| **Reusability** | Service reusable | Service specific, not reusable |

---

## Independent Deployability

**Matlab:** Har service ko independently build, test, aur deploy kar sakte ho bina baaki services ko affect kiye.

### Monolith Deployment (BAD)

```
Monolith Deployment Pipeline:
┌─────────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ Code Change │ -> │ Build    │ -> │ Test All │ -> │ Deploy   │ -> │ Downtime │
│ (User mod)  │    │ 15 min   │    │ 30 min   │    │ 5 min    │    │ 2 min    │
└─────────────┘    └──────────┘    └──────────┘    └──────────┘    └──────────┘

Total: 52 minutes — aur pura app downtime pe jaata hai!
Problem:
- User module mein chhota change → pura app build/test/deploy
- Payment team ka code ready nahi → Order team ka deploy block
- Rollback = pura app rollback
```

### Microservices Deployment (GOOD)

```
Microservices Deployment Pipeline:
User Service Change:
┌─────────────┐    ┌──────────┐    ┌─────────────┐    ┌──────────┐
│ Code Change │ -> │ Build    │ -> │ Test (US)   │ -> │ Deploy   │
│ (User only) │    │ 2 min    │    │ 5 min       │    │ 1 min    │
└─────────────┘    └──────────┘    └─────────────┘    └──────────┘

Total: 8 minutes — sirf User Service deploy, baaki services untouched!

Order Service independently deploy ho sakti hai:
┌─────────────┐    ┌──────────┐    ┌─────────────┐    ┌──────────┐
│ Code Change │ -> │ Build    │ -> │ Test (OS)   │ -> │ Deploy   │
│ (Order only)│    │ 2 min    │    │ 5 min       │    │ 1 min    │
└─────────────┘    └──────────┘    └─────────────┘    └──────────┘
```

### CI/CD Pipeline for Microservices

```yaml
# .github/workflows/user-service.yml
name: User Service CI/CD

on:
  push:
    paths:
      - 'services/user-service/**'  # Sirf user-service changes pe trigger

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Build User Service
        run: |
          cd services/user-service
          ./mvnw clean package -DskipTests

      - name: Run Unit Tests
        run: |
          cd services/user-service
          ./mvnw test

      - name: Run Integration Tests
        run: |
          cd services/user-service
          ./mvnw verify -Pintegration-test

      - name: Build Docker Image
        run: |
          docker build -t user-service:${{ github.sha }} \
            services/user-service

      - name: Push to Registry
        run: |
          docker push user-service:${{ github.sha }}

      - name: Deploy to Kubernetes
        run: |
          kubectl set image deployment/user-service \
            user-service=user-service:${{ github.sha }} \
            -n production
```

### Independent Deployability ke liye Best Practices

```java
// 1. API Versioning — backward compatibility maintain karo
@RestController
@RequestMapping("/api/v1/users")
public class UserControllerV1 {

    @GetMapping("/{id}")
    public UserDTO getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }
}

@RestController
@RequestMapping("/api/v2/users")
public class UserControllerV2 {

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        // Enhanced response — v1 clients unaffected
        return userService.getUserV2(id);
    }
}

// 2. Database Migration独立 — service deployment se alag
// Flyway migration — service start hone pe auto-run
// Lekin migration backward compatible honi chahiye
-- V1__add_user_email.sql
ALTER TABLE users ADD COLUMN email VARCHAR(255);
-- New column add karo, purana code still works

// V2__make_email_not_null.sql
-- Tab run karo jab sab services new column use karne lagein
ALTER TABLE users ALTER COLUMN email SET NOT NULL;

// 3. Feature Flags — bina deploy kiye feature on/off karo
@Service
public class UserService {

    @Value("${feature.new-profile.enabled:false}")
    private boolean newProfileEnabled;

    public User updateProfile(Long id, ProfileRequest request) {
        if (newProfileEnabled) {
            return updateProfileV2(id, request);
        }
        return updateProfileV1(id, request);
    }
}
```

### Deployment Strategies

| Strategy | Description | Downtime | Rollback |
|----------|-------------|----------|----------|
| **Rolling Update** | Ek-ek instance update hota hai | Zero | Automatic |
| **Blue-Green** | Do environments — traffic switch | Near-zero | Instant switch |
| **Canary** | Pehle 5% users ko new version | Zero | Gradual rollback |
| **Shadow** | Production traffic copy karo test pe | Zero | No impact |

```
Blue-Green Deployment:
                    ┌──────────────┐
                    │   Load        │
                    │   Balancer    │
                    └──────┬───────┘
                           │
              ┌────────────┴────────────┐
              │                         │
     ┌────────▼────────┐      ┌────────▼────────┐
     │  Blue (v1.0)    │      │  Green (v2.0)   │
     │  ● Production   │      │  ● Testing       │
     │  ● 100% traffic │      │  ● 0% traffic    │
     └─────────────────┘      └─────────────────┘

     Switch karo → Green ko 100% traffic do
     Problem aaya → Blue pe wapas switch
```

---

## Decentralized Data Management

**Matlab:** Har microservice apna database manage karta hai. Shared database anti-pattern hai microservices mein.

### Shared Database Anti-Pattern (BAD)

```
❌ SHARED DATABASE (ANTI-PATTERN)
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ User Service │  │Order Service │  │Payment Svc   │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                 │
       └─────────────────┼─────────────────┘
                         │
               ┌─────────▼─────────┐
               │   Shared Database  │
               │   ┌─────────────┐  │
               │   │ users       │  │
               │   │ orders      │  │
               │   │ payments    │  │
               │   │ inventory   │  │
               │   └─────────────┘  │
               └───────────────────┘

Problems:
1. Schema change = sab services coordinate karein
2. Database lock = sab services affected
3. Scaling bottleneck — ek hi database pe load
4. Team blocking — DBA team se permission leni padegi
5. Tight coupling — services ek doosre ke tables pe depend
```

### Database per Service (GOOD)

```
✅ DATABASE PER SERVICE
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ User Service │  │Order Service │  │Payment Svc   │
│              │  │              │  │              │
│ ┌──────────┐ │  │ ┌──────────┐ │  │ ┌──────────┐ │
│ │User DB   │ │  │ │Order DB  │ │  │ │Payment DB│ │
│ │(Postgres)│ │  │ │(MySQL)   │ │  │ │(MongoDB) │ │
│ └──────────┘ │  │ └──────────┘ │  │ └──────────┘ │
└──────────────┘  └──────────────┘  └──────────────┘

Benefits:
1. Har service apna schema independently evolve karti hai
2. Database failure isolated — User DB down ≠ Order DB down
3. Har service apna optimal DB choose kar sakti hai
4. Teams independent — kisi se permission nahi chahiye
5. Scaling per service basis pe
```

### Java Implementation — Database per Service

```java
// User Service — PostgreSQL with JPA
// application.yml
/*
spring:
  datasource:
    url: jdbc:postgresql://user-db:5432/userdb
    username: user_service
    password: ${USER_DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    database-platform: org.hibernate.dialect.PostgreSQLDialect
*/

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    private String password;
    private String name;
}

// Order Service — MySQL with JPA
// application.yml
/*
spring:
  datasource:
    url: jdbc:mysql://order-db:3306/orderdb
    username: order_service
    password: ${ORDER_DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    database-platform: org.hibernate.dialect.MySQLDialect
*/

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;  // Foreign key nahi — sirf reference
    private BigDecimal totalAmount;
    private String status;

    @OneToMany(mappedBy = "order")
    private List<OrderItem> items;
}

// Payment Service — MongoDB
// application.yml
/*
spring:
  data:
    mongodb:
      uri: mongodb://payment-db:27017/paymentdb
*/

@Document(collection = "payments")
public class Payment {
    @Id
    private String id;
    private String orderId;
    private String paymentMethod;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
}
```

### Cross-Service Data Access Patterns

Jab ek service ko doosri service ka data chahiye, toh direct DB access mat karo — API ya events use karo.

```java
// Pattern 1: API Call (Sync) — jab immediate data chahiye
@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserServiceClient userServiceClient;

    public Order createOrder(OrderRequest request) {
        // User data chahiye — API call se lo
        UserDTO user = userServiceClient.getUser(request.userId());

        if (!user.isActive()) {
            throw new BusinessException("User is not active");
        }

        return orderRepository.save(buildOrder(user, request));
    }
}

// Pattern 2: Event-Driven (Async) — jab eventual consistency kaafi hai
@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final OrderService orderService;

    // User deactivate hua → Orders bhi suspend karo
    @KafkaListener(topics = "user-events", groupId = "order-service")
    public void handleUserDeactivated(UserDeactivatedEvent event) {
        orderService.suspendUserOrders(event.userId());
    }
}

// Pattern 3: CQRS — Read/Write separation
@Service
@RequiredArgsConstructor
public class OrderQueryService {

    // Read model — denormalized, optimized for queries
    // Order DB mein user ka bhi data store (denormalized)
    private final OrderReadRepository orderReadRepository;

    public OrderDTO getOrderWithUserDetails(Long orderId) {
        // Single query mein sab data — read model se
        return orderReadRepository.findOrderWithDetails(orderId);
    }
}
```

### Distributed Transactions — Saga Pattern

Shared database nahi hai toh distributed transactions kaise handle karein? Saga pattern use karo.

```
SAGA PATTERN — Order + Payment + Inventory
┌─────────┐     ┌─────────┐     ┌──────────┐
│ Order   │     │ Payment │     │Inventory │
│ Service │     │ Service │     │ Service  │
└────┬────┘     └────┬────┘     └────┬─────┘
     │               │               │
     │ 1. Create     │               │
     │   Order       │               │
     │──────────────>│               │
     │               │ 2. Charge     │
     │               │   Payment     │
     │               │──────────────>│
     │               │               │ 3. Reserve
     │               │               │   Stock
     │               │               │
     │               │ 4. Payment    │
     │               │   Confirm     │
     │<──────────────│               │
     │ 5. Order      │               │
     │   Confirmed   │               │
     │               │               │

     FAILURE SCENARIO:
     │ 1. Create     │               │
     │   Order       │               │
     │──────────────>│               │
     │               │ 2. Charge     │
     │               │   Payment     │
     │               │──────────────>│
     │               │               │ 3. FAIL!
     │               │               │   Insufficient Stock
     │               │               │
     │               │ 4. Compensate │
     │               │   Refund      │
     │<──────────────│               │
     │ 5. Compensate │               │
     │   Cancel      │               │
     │   Order       │               │
```

```java
// Saga Implementation — Choreography (Event-based)
@Service
@RequiredArgsConstructor
public class OrderSagaService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void createOrder(Order order) {
        // Step 1: Order CREATED
        order.setStatus("PENDING_PAYMENT");
        orderRepository.save(order);

        // Event publish karo
        kafkaTemplate.send("order-events",
            new OrderCreatedEvent(order.getId(), order.getUserId(), order.getTotalAmount()));
    }

    @KafkaListener(topics = "payment-events", groupId = "order-service")
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        Order order = orderRepository.findById(event.orderId()).orElseThrow();
        order.setStatus("PAYMENT_DONE");
        orderRepository.save(order);

        // Next step trigger
        kafkaTemplate.send("order-events",
            new PaymentConfirmedEvent(event.orderId()));
    }

    @KafkaListener(topics = "payment-events", groupId = "order-service")
    public void handlePaymentFailure(PaymentFailedEvent event) {
        // Compensating transaction — Order cancel karo
        Order order = orderRepository.findById(event.orderId()).orElseThrow();
        order.setStatus("CANCELLED");
        orderRepository.save(order);
    }

    @KafkaListener(topics = "inventory-events", groupId = "order-service")
    public void handleInventoryFailure(InsufficientStockEvent event) {
        // Compensating transaction — Payment refund + Order cancel
        Order order = orderRepository.findById(event.orderId()).orElseThrow();
        order.setStatus("CANCELLED_NO_STOCK");
        orderRepository.save(order);

        // Payment ko refund command bhejo
        kafkaTemplate.send("payment-events",
            new RefundCommand(event.orderId()));
    }
}
```

### Decentralized Data Best Practices

| Practice | Description |
|----------|-------------|
| **Database per service** | Har service ka apna DB — koi sharing nahi |
| **No direct DB access** | Doosri service ke DB ko directly access mat karo |
| **API or Events** | Data chahiye toh API call ya event listen karo |
| **Eventual Consistency** | Strong consistency ki jagah eventual consistency accept karo |
| **Saga for transactions** | Distributed transactions ke liye Saga pattern |
| **CQRS for reads** | Read-side pe denormalized data store karo |
| **Outbox Pattern** | Events reliable banane ke liye outbox table use karo |

---

## Failure Isolation

**Matlab:** Ek service fail ho toh baaki services affected na hon. Failure isolate karo, cascade mat hone do.

### Cascading Failure (BAD)

```
❌ CASCADING FAILURE — Ek service down = pura system down

User → API Gateway → Order Service → Payment Service → User Service
                                                    (DOWN!)
                                                    │
                     Payment Service waits...      │
                     (timeout: 30s)                │
                     │                             │
                     Order Service waits...        │
                     (timeout: 30s + 30s = 60s)    │
                     │                             │
                     API Gateway waits...          │
                     (timeout: 60s + 30s = 90s)    │
                     │                             │
                     User gets 504 Gateway Timeout │
                     ALL threads blocked!          │

                     Result:
                     - Order Service threads exhausted
                     - Payment Service threads exhausted
                     - API Gateway threads exhausted
                     - Pura system DOWN — ek service ke kaaran!
```

### Failure Isolation Techniques

#### 1. Circuit Breaker Pattern

```java
// Resilience4j Circuit Breaker
@Service
@RequiredArgsConstructor
public class PaymentServiceClient {

    private final RestTemplate restTemplate;

    @CircuitBreaker(name = "payment-service", fallbackMethod = "fallbackGetPayment")
    @Retry(name = "payment-service")
    @TimeLimiter(name = "payment-service")
    public CompletableFuture<PaymentDTO> getPayment(Long orderId) {
        return CompletableFuture.supplyAsync(() -> {
            return restTemplate.getForObject(
                "http://payment-service/payments/order/" + orderId,
                PaymentDTO.class);
        });
    }

    // Fallback method — jab payment service down ho
    public CompletableFuture<PaymentDTO> fallbackGetPayment(Long orderId, Throwable t) {
        log.warn("Payment service is down, returning cached/default data: {}", t.getMessage());
        return CompletableFuture.supplyAsync(() ->
            PaymentDTO.builder()
                .orderId(orderId)
                .status("UNAVAILABLE")
                .message("Payment service temporarily unavailable")
                .build()
        );
    }
}

// Circuit Breaker States:
/*

CLOSED (Normal State)
┌─────────────┐
│  Requests   │ ────> Payment Service ────> Response
│  flowing    │
└─────────────┘
Failure count < threshold = sab theek


OPEN (Service Down)
┌─────────────┐
│  Requests   │ ────> BLOCKED ────> Fallback Response
│  blocked    │
└─────────────┘
Failure count >= threshold = circuit OPEN
Requests ko directly fail karo — wait mat karo


HALF-OPEN (Testing Recovery)
┌─────────────┐
│  Few test   │ ────> Payment Service ────> Check response
│  requests   │     ┌──────────────┐
│  allowed    │     │ Success?     │ ──> CLOSED (recover)
└─────────────┘     │ Failure?     │ ──> OPEN (back to blocking)
                    └──────────────┘
*/

// application.yml — Resilience4j config
/*
resilience4j:
  circuitbreaker:
    instances:
      payment-service:
        slidingWindowSize: 10          # Last 10 calls dekho
        failureRateThreshold: 50       # 50% fail ho toh OPEN
        waitDurationInOpenState: 30s   # 30s baad HALF-OPEN
        permittedNumberOfCallsInHalfOpenState: 3  # 3 test calls
        automaticTransitionFromOpenToHalfOpenEnabled: true

  retry:
    instances:
      payment-service:
        maxAttempts: 3
        waitDuration: 1s
        retryExceptions:
          - java.io.IOException

  timelimiter:
    instances:
      payment-service:
        timeoutDuration: 3s  # 3 seconds se zyada wait mat karo
*/
```

#### 2. Bulkhead Pattern

```java
// Bulkhead — resources isolate karo
// Ek service ke liye allocated resources doosri services ko affect nahi karte

@Service
@RequiredArgsConstructor
public class MultiServiceOrchestrator {

    private final RestTemplate restTemplate;

    // Payment Service ke liye alag thread pool
    @Bulkhead(name = "payment-bulkhead", type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<PaymentDTO> callPaymentService(Long orderId) {
        return CompletableFuture.supplyAsync(() ->
            restTemplate.getForObject(
                "http://payment-service/payments/" + orderId,
                PaymentDTO.class));
    }

    // User Service ke liye alag thread pool
    @Bulkhead(name = "user-bulkhead", type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<UserDTO> callUserService(Long userId) {
        return CompletableFuture.supplyAsync(() ->
            restTemplate.getForObject(
                "http://user-service/users/" + userId,
                UserDTO.class));
    }
}

// application.yml
/*
resilience4j:
  bulkhead:
    instances:
      payment-bulkhead:
        maxConcurrentCalls: 10    # Payment ke liye max 10 threads
        maxWaitDuration: 5s       # 5s wait karo agar full ho

      user-bulkhead:
        maxConcurrentCalls: 20    # User ke liye max 20 threads
        maxWaitDuration: 3s
*/

/*
BULKHEAD VISUAL:
┌──────────────────────────────────────────────────────┐
│                Application                            │
│                                                      │
│  ┌────────────────────┐  ┌─────────────────────────┐ │
│  │ Payment Bulkhead   │  │ User Bulkhead           │ │
│  │ Max: 10 threads    │  │ Max: 20 threads         │ │
│  │                    │  │                         │ │
│  │ ████████░░ 8/10    │  │ ████████████░░░░ 12/20  │ │
│  └────────────────────┘  └─────────────────────────┘ │
│                                                      │
│  Payment service slow? Uska bulkhead full hoga       │
│  User service unaffected — apna thread pool hai      │
└──────────────────────────────────────────────────────┘
*/
```

#### 3. Timeout & Deadline

```java
// Timeout — har external call pe timeout lagao
@Service
@RequiredArgsConstructor
public class OrderService {

    private final RestTemplate restTemplate;

    public Order createOrder(OrderRequest request) {
        // Timeout with RestTemplate
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(3000)     // Connection: 3s max
            .setSocketTimeout(5000)      // Response: 5s max
            .build();

        // Timeout with WebClient (Spring WebFlux)
        WebClient client = WebClient.builder()
            .baseUrl("http://payment-service")
            .build();

        PaymentDTO payment = client.post()
            .uri("/payments")
            .bodyValue(paymentRequest)
            .retrieve()
            .bodyToMono(PaymentDTO.class)
            .timeout(Duration.ofSeconds(5))  // 5 second timeout
            .onErrorResume(TimeoutException.class, e -> {
                log.warn("Payment service timeout");
                return Mono.just(PaymentDTO.timeout());
            })
            .block();

        return orderRepository.save(order);
    }
}
```

#### 4. Graceful Degradation

```java
// Graceful Degradation — full functionality na mile toh
// limited functionality mein kaam chalao
@Service
public class ProductRecommendationService {

    private final RecommendationEngineClient engineClient;
    private final CacheManager cacheManager;

    public List<Product> getRecommendations(Long userId) {
        try {
            // Try ML-based recommendations
            return engineClient.getRecommendations(userId)
                .timeout(Duration.ofSeconds(2))
                .block();
        } catch (Exception e) {
            log.warn("ML engine unavailable, falling back: {}", e.getMessage());

            // Fallback 1: Cache se purani recommendations
            Cache cache = cacheManager.getCache("recommendations");
            if (cache != null) {
                List<Product> cached = cache.get(userId, List.class);
                if (cached != null) {
                    log.info("Returning cached recommendations");
                    return cached;
                }
            }

            // Fallback 2: Popular products
            log.info("Returning popular products as fallback");
            return getPopularProducts();
        }
    }

    private List<Product> getPopularProducts() {
        // Static popular products — ML nahi chahiye
        return List.of(
            new Product(1L, "iPhone 15", 999.99),
            new Product(2L, "MacBook Air", 1199.99),
            new Product(3L, "AirPods Pro", 249.99)
        );
    }
}
```

### Failure Isolation Summary Table

| Technique | Purpose | Implementation |
|-----------|---------|----------------|
| **Circuit Breaker** | Failing service ko repeatedly call karne se rok | Resilience4j, Hystrix |
| **Bulkhead** | Resource isolation — ek failure doosre ko affect na kare | Thread pool separation |
| **Timeout** | Indefinite wait se bachao | WebClient timeout, RestTemplate config |
| **Retry** | Transient failures handle karo | Resilience4j Retry, exponential backoff |
| **Fallback** | Service down ho toh alternative response | Default values, cached data |
| **Rate Limiter** | Overload se bachao | Resilience4j RateLimiter, API Gateway |

---

## Polyglot Persistence

**Matlab:** Har microservice apne data ke liye best database choose kar sakti hai — sabko ek hi database use karne ki zaroorat nahi.

### Why Polyglot Persistence?

Different problems ke liye different databases better hote hain:

```
ONE SIZE DOES NOT FIT ALL

Relational Data (Orders, Users, Payments)
→ PostgreSQL / MySQL — ACID transactions, complex joins

Document Data (Product Catalog, Content)
→ MongoDB — Flexible schema, hierarchical data

Cache Data (Sessions, Frequently accessed data)
→ Redis — Sub-millisecond reads

Search Data (Full-text search, Analytics)
→ Elasticsearch — Lucene-based search

Time-Series Data (Metrics, IoT, Logs)
→ InfluxDB / TimescaleDB — Time-optimized writes

Graph Data (Social networks, Recommendations)
→ Neo4j — Relationship-heavy queries

Key-Value (Session store, Config)
→ Redis, DynamoDB — Simple lookups
```

### Polyglot Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                    Microservices Ecosystem                     │
│                                                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐   │
│  │ User Service│  │Order Service│  │  Product Service    │   │
│  │             │  │             │  │                     │   │
│  │ PostgreSQL  │  │   MySQL     │  │   MongoDB           │   │
│  │ • Strong    │  │ • ACID      │  │ • Flexible schema   │   │
│  │   ACID      │  │ • Joins     │  │ • Hierarchical      │   │
│  │ • Relational│  │ • Financial │  │ • Document-oriented │   │
│  └─────────────┘  └─────────────┘  └─────────────────────┘   │
│                                                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐   │
│  │ Search Svc  │  │ Session Svc │  │  Analytics Service  │   │
│  │             │  │             │  │                     │   │
│  │ Elastic     │  │   Redis     │  │   InfluxDB          │   │
│  │ • Full-text │  │ • Sub-ms    │  │ • Time-series       │   │
│  │ • Aggregates│  │ • Ephemeral │  │ • Metrics           │   │
│  └─────────────┘  └─────────────┘  └─────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
```

### Java Implementation — Multiple Databases

```java
// 1. Product Service — MongoDB (Document DB for flexible product schema)
@Document(collection = "products")
public class Product {
    @Id
    private String id;
    private String name;
    private BigDecimal price;
    private List<String> categories;
    private Map<String, Object> specifications;  // Flexible specs
    private List<ProductImage> images;

    @Data
    public static class ProductImage {
        private String url;
        private boolean isPrimary;
        private int order;
    }
}

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    List<Product> findByCategoriesContaining(String category);

    // Full-text style search
    @Query("{ '$text': { '$search': ?0 } }")
    List<Product> searchByText(String query);
}

// 2. Session Service — Redis (Fast key-value store)
@Service
@RequiredArgsConstructor
public class SessionService {

    private final StringRedisTemplate redisTemplate;

    // Session store karo — TTL ke saath
    public void saveSession(String sessionId, String userId, long ttlMinutes) {
        redisTemplate.opsForValue().set(
            "session:" + sessionId,
            userId,
            ttlMinutes,
            TimeUnit.MINUTES
        );
    }

    // Session verify karo — sub-millisecond
    public Optional<String> getUserId(String sessionId) {
        String userId = redisTemplate.opsForValue().get("session:" + sessionId);
        return Optional.ofNullable(userId);
    }

    // Rate limiting — sliding window
    public boolean isRateLimited(String userId, int maxRequests, int windowSeconds) {
        String key = "ratelimit:" + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        return count > maxRequests;
    }
}

// 3. Analytics Service — Time-Series data collection
@Service
public class AnalyticsService {

    private final InfluxDB influxDB;

    public void recordPageView(String userId, String page, long duration) {
        Point point = Point.measurement("page_views")
            .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
            .tag("userId", userId)
            .tag("page", page)
            .addField("duration", duration)
            .build();

        influxDB.write("analytics", "autogen", point);
    }

    public List<PageViewStats> getTopPages(int hours) {
        String query = String.format(
            "SELECT count(*) FROM page_views WHERE time > now() - %dh GROUP BY page",
            hours
        );
        // Execute query and return results
        return influxDB.query(new Query(query)).getResults().stream()
            .map(this::mapToStats)
            .collect(Collectors.toList());
    }
}

// 4. Search Service — Elasticsearch integration
@Service
@RequiredArgsConstructor
public class SearchService {

    private final ElasticsearchRestTemplate elasticsearchTemplate;

    // Product index karo
    public void indexProduct(Product product) {
        ProductDocument doc = ProductDocument.builder()
            .id(product.getId())
            .name(product.getName())
            .description(product.getDescription())
            .category(product.getCategories())
            .price(product.getPrice())
            .build();

        elasticsearchTemplate.save(doc);
    }

    // Full-text search with filters
    public List<ProductDocument> searchProducts(String query, String category,
                                                 BigDecimal minPrice, BigDecimal maxPrice) {
        Criteria criteria = Criteria.where("name").matches(query)
            .or(Criteria.where("description").matches(query));

        if (category != null) {
            criteria = criteria.and("category").is(category);
        }
        if (minPrice != null && maxPrice != null) {
            criteria = criteria.and("price").between(minPrice.doubleValue(), maxPrice.doubleValue());
        }

        Query searchQuery = new CriteriaQuery(criteria);
        return elasticsearchTemplate.search(searchQuery, ProductDocument.class)
            .stream()
            .map(SearchHit::getContent)
            .collect(Collectors.toList());
    }
}
```

### Database Selection Guide

| Use Case | Recommended DB | Why |
|----------|---------------|-----|
| User accounts, auth | PostgreSQL, MySQL | ACID, relational, joins |
| Orders, payments | PostgreSQL, MySQL | Transactions, consistency |
| Product catalog | MongoDB | Flexible schema, nested data |
| Session storage | Redis | Fast, TTL support |
| Cache | Redis, Memcached | Sub-millisecond reads |
| Full-text search | Elasticsearch | Lucene, relevance scoring |
| Recommendations | Neo4j | Graph relationships |
| Metrics, logs | InfluxDB, ELK | Time-series optimized |
| Messaging queue | RabbitMQ, Kafka | Async, event streaming |
| Config storage | etcd, Consul | Distributed, consistent |

### Polyglot Persistence Best Practices

| Practice | Description |
|----------|-------------|
| **Right tool for the job** | Data pattern ke hisaab se DB choose karo |
| **Team expertise** | Wo DB use karo jo team jaanti ho (ya seekh sakti ho) |
| **Operational cost** | Zyada databases = zyada operational overhead |
| **Data sync** | Multiple DBs mein data sync kaise hoga — events/CDC |
| **Don't over-engineer** | Shuru mein 1-2 DB se start karo, need pe badhao |

---

## Bounded Context (DDD)

**Matlab:** Domain-Driven Design ka core concept — har bounded context ka apna model, apna vocabulary, apna boundaries hote hain.

### What is a Bounded Context?

Bounded context ek logical boundary hai jahan ek specific domain model applies hota hai. Same entity ka different context mein different meaning ho sakta hai.

```
┌──────────────────────────────────────────────────────────────┐
│                    E-Commerce Domain                           │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  BOUNDED CONTEXT: Sales                                  │ │
│  │                                                          │ │
│  │  Product (Sales view):                                   │ │
│  │  - id, name, price, discount, stockQuantity             │ │
│  │  - "Product = bechne wali cheez"                        │ │
│  │  - Focus: pricing, availability, promotion               │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  BOUNDED CONTEXT: Shipping                               │ │
│  │                                                          │ │
│  │  Product (Shipping view):                                │ │
│  │  - id, name, weight, dimensions, shippingCategory       │ │
│  │  - "Product = bhejne wali cheez"                        │ │
│  │  - Focus: size, weight, handling, packaging              │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  BOUNDED CONTEXT: Billing                                │ │
│  │                                                          │ │
│  │  Product (Billing view):                                 │ │
│  │  - id, name, price, taxRate, invoiceDescription         │ │
│  │  - "Product = bill mein line item"                      │ │
│  │  - Focus: tax, invoice, accounting                       │ │
│  └─────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘

Same "Product" — teen alag contexts mein teen alag models!
```

### Bounded Context Implementation

```java
// SALES CONTEXT — Product ka Sales view
// com.ecommerce.sales.domain.Product
package com.ecommerce.sales.domain;

@Entity
@Table(name = "products")
public class Product {
    @Id
    private String id;
    private String name;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private Integer stockQuantity;
    private boolean isPromoted;

    // Sales-specific behavior
    public BigDecimal getEffectivePrice() {
        return discountPrice != null ? discountPrice : price;
    }

    public boolean isInStock() {
        return stockQuantity > 0;
    }

    public void applyDiscount(BigDecimal discount) {
        this.discountPrice = price.subtract(discount);
    }
}

// SHIPPING CONTEXT — Product ka Shipping view
// com.ecommerce.shipping.domain.Product
package com.ecommerce.shipping.domain;

@Entity
@Table(name = "shipping_products")
public class Product {
    @Id
    private String id;
    private String name;
    private BigDecimal weight;  // in kg
    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal height;
    private String shippingCategory;  // FRAGILE, LIQUID, STANDARD, OVERSIZED

    // Shipping-specific behavior
    public BigDecimal getVolumetricWeight() {
        return length.multiply(width).multiply(height).divide(new BigDecimal("5000"));
    }

    public BigDecimal getChargeableWeight() {
        return weight.max(getVolumetricWeight());
    }

    public boolean requiresSpecialHandling() {
        return "FRAGILE".equals(shippingCategory) || "LIQUID".equals(shippingCategory);
    }
}

// BILLING CONTEXT — Product ka Billing view
// com.ecommerce.billing.domain.Product
package com.ecommerce.billing.domain;

@Entity
@Table(name = "billing_products")
public class Product {
    @Id
    private String id;
    private String name;
    private BigDecimal price;
    private BigDecimal taxRate;  // GST rate
    private String invoiceDescription;
    private String accountingCode;

    // Billing-specific behavior
    public BigDecimal getPriceWithTax() {
        return price.add(price.multiply(taxRate));
    }

    public String getLineItemDescription() {
        return invoiceDescription != null ? invoiceDescription : name;
    }
}
```

### Context Mapping Patterns

Jab multiple bounded contexts aapas mein communicate karte hain, toh Context Mapping patterns apply hote hain:

```
CONTEXT MAPPING PATTERNS:

1. UPSTREAM-DOWNSTREAM (Most Common)
   ┌──────────────┐     ┌──────────────┐
   │   Upstream   │────>│  Downstream  │
   │  (Provider)  │     │  (Consumer)  │
   └──────────────┘     └──────────────┘

   Upstream team ka model Downstream team use karta hai
   Example: User Service (upstream) → Order Service (downstream)


2. CONFORMIST
   ┌──────────────┐     ┌──────────────┐
   │   Upstream   │────>│  Downstream  │
   │              │     │  (Conforms)  │
   └──────────────┘     └──────────────┘

   Downstream team Upstream ke model ko follow karta hai
   No translation layer — seedha use


3. ANTI-CORRUPTION LAYER (ACL)
   ┌──────────────┐     ┌────────┐     ┌──────────────┐
   │   External   │────>│   ACL  │────>│  Our Context  │
   │   System     │     │(Trans- │     │              │
   └──────────────┘     │ lator) │     └──────────────┘
                        └────────┘

   External system ke model ko apne model mein translate karo
   External changes se apna context protected rahta hai


4. OPEN HOST SERVICE
   ┌──────────────┐     ┌─────────────────────┐
   │   Our Svc    │────>│  Published Language │
   │  (Host)      │     │  (Standard API)     │
   └──────────────┘     └────────┬────────────┘
                                 │
                    ┌────────────┼────────────┐
                    │            │            │
              ┌─────▼──┐  ┌─────▼──┐  ┌─────▼──┐
              │Consumer│  │Consumer│  │Consumer│
              │   1    │  │   2    │  │   3    │
              └────────┘  └────────┘  └────────┘

   Ek standard protocol/API publish karo — sab consumers usi ko use karein


5. SHARED KERNEL
   ┌──────────────┐     ┌──────────────┐
   │  Context A   │     │  Context B   │
   │              │     │              │
   │  ┌────────┐  │     │  ┌────────┐  │
   │  │Shared  │◄─┼─────┼─>│Shared  │  │
   │  │Kernel  │  │     │  │Kernel  │  │
   │  └────────┘  │     │  └────────┘  │
   └──────────────┘     └──────────────┘

   Dono contexts ek shared library/model share karte hain
   Careful — yeh coupling badhata hai!
```

### Anti-Corruption Layer Implementation

```java
// External payment gateway ka model humare model se alag hai
// ACL use karke translate karo

// External system ka model (hum control nahi kar sakte)
public class ExternalPaymentRequest {
    private String merchantId;
    private String transactionRef;
    private Long amountInCents;
    private String currencyCode;
    private String customerEmail;
    private Map<String, String> metadata;
}

// Humare context ka model
public class PaymentCommand {
    private String orderId;
    private BigDecimal amount;
    private String userEmail;
    private String description;
}

// Anti-Corruption Layer — Translation responsibility
@Component
public class PaymentGatewayACL {

    private final ExternalPaymentGatewayClient gatewayClient;

    @Value("${payment.gateway.merchant-id}")
    private String merchantId;

    // External model ko humare model mein translate karo
    public PaymentResult processPayment(PaymentCommand command) {
        // Translate: Our model → External model
        ExternalPaymentRequest externalRequest = translateToExternal(command);

        // External system call
        ExternalPaymentResponse externalResponse = gatewayClient.charge(externalRequest);

        // Translate: External model → Our model
        return translateFromExternal(externalResponse);
    }

    private ExternalPaymentRequest translateToExternal(PaymentCommand cmd) {
        ExternalPaymentRequest req = new ExternalPaymentRequest();
        req.setMerchantId(merchantId);
        req.setTransactionRef("ORDER-" + cmd.getOrderId());
        req.setAmountInCents(cmd.getAmount().multiply(new BigDecimal("100")).longValue());
        req.setCurrencyCode("INR");
        req.setCustomerEmail(cmd.getUserEmail());
        req.setMetadata(Map.of(
            "orderId", cmd.getOrderId(),
            "description", cmd.getDescription()
        ));
        return req;
    }

    private PaymentResult translateFromExternal(ExternalPaymentResponse response) {
        return PaymentResult.builder()
            .success("SUCCESS".equals(response.getStatus()))
            .transactionId(response.getTransactionId())
            .message(response.getMessage())
            .build();
    }
}
```

### Bounded Context se Microservice Design

```
STEP 1: Identify Bounded Contexts
┌─────────────────────────────────────────────────────────────┐
│                    E-Commerce Domain                         │
│                                                              │
│  [User Mgmt]  [Catalog]  [Order Mgmt]  [Payment]  [Shipping]│
│                                                              │
│  Har bounded context = ek potential microservice             │
└─────────────────────────────────────────────────────────────┘

STEP 2: Define Context Maps
┌───────────┐     ┌───────────┐     ┌───────────┐
│  User     │────>│  Order    │────>│  Payment  │
│  Service  │     │  Service  │     │  Service  │
└───────────┘     └───────────┘     └───────────┘
                       │
                       │                ┌───────────┐
                       └───────────────>│ Shipping  │
                                        │  Service  │
                                        └───────────┘

STEP 3: Deploy as Independent Services
┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐
│  User     │  │  Order    │  │  Payment  │  │  Shipping │
│  Service  │  │  Service  │  │  Service  │  │  Service  │
│           │  │           │  │           │  │           │
│ User DB   │  │ Order DB  │  │ Payment DB│  │ Ship DB   │
└───────────┘  └───────────┘  └───────────┘  └───────────┘
```

### Bounded Context Best Practices

| Practice | Description |
|----------|-------------|
| **Ubiquitous Language** | Har context ki apni vocabulary — same word, different context mein different meaning ho sakta hai |
| **Explicit boundaries** | Context boundaries clearly define karo — kya andar hai, kya bahar |
| **No shared models** | Ek context ka model doosre context mein directly use mat karo |
| **Translation layer** | Contexts ke beech mapping/translation use karo (ACL) |
| **Context map maintain karo** | Document karo kaunsa context kiske saath kaise communicate karta hai |

---

## Conway's Law

**Matlab:** "Organizations which design systems are constrained to produce designs which are copies of the communication structures of these organizations." — Melvin Conway, 1967

**Simple Hindi mein:** Jaise team structure hota hai, waisa hi software design banta hai.

### Conway's Law Explained

```
TEAM STRUCTURE → SOFTWARE ARCHITECTURE

Scenario 1: Monolith Team
┌──────────────────────────────────────┐
│         Single Large Team             │
│  ┌────┬────┬────┬────┬────┬────┐     │
│  │ FE │ BE │ DB │ QA │ Dev│Ops │     │
│  └────┴────┴────┴────┴────┴────┘     │
│                                       │
│  Result: Monolithic Application        │
│  — Ek hi codebase, ek hi deploy        │
└──────────────────────────────────────┘

Scenario 2: Microservices Teams
┌────────────┐  ┌────────────┐  ┌────────────┐
│  Team A    │  │  Team B    │  │  Team C    │
│  (User)    │  │  (Order)   │  │  (Payment) │
│            │  │            │  │            │
│  2 devs    │  │  3 devs    │  │  2 devs    │
│  1 QA      │  │  1 QA      │  │  1 QA      │
└─────┬──────┘  └─────┬──────┘  └─────┬──────┘
      │               │               │
      ▼               ▼               ▼
┌────────────┐  ┌────────────┐  ┌────────────┐
│ User Svc   │  │ Order Svc  │  │ Payment Svc│
│ User DB    │  │ Order DB   │  │ Payment DB │
└────────────┘  └────────────┘  └────────────┘

Result: Microservices Architecture
— Independent services, independent teams
```

### Reverse Conway's Law

Agar microservices architecture chahiye, toh pehle teams ko microservices structure mein organize karo:

```
DESIRED ARCHITECTURE → DESIGN TEAM STRUCTURE

Humein yeh architecture chahiye:
┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐
│ User    │  │ Order   │  │ Payment │  │ Search  │
│ Service │  │ Service │  │ Service │  │ Service │
└─────────┘  └─────────┘  └─────────┘  └─────────┘

Toh pehle yeh teams banao:
┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
│Team User │  │Team Order│  │Team Pay  │  │Team Search│
│ 2 devs   │  │ 3 devs   │  │ 2 devs   │  │ 2 devs   │
│ 1 QA     │  │ 1 QA     │  │ 1 QA     │  │ 1 QA     │
└──────────┘  └──────────┘  └──────────┘  └──────────┘

Har team:
— Owns apna service end-to-end
— Independently deploy kar sakti hai
— Apna tech stack choose kar sakti hai
— Apna database manage karti hai
```

### Team Topologies for Microservices

```
TEAM TOPOLOGY TYPES:

1. STREAM-ALIGNED TEAM (Primary)
   ┌─────────────────────────────────────┐
   │  Team: Order Management               │
   │  Responsibility: Order lifecycle      │
   │  Services: Order Service, Order API   │
   │  Data: Order Database                 │
   │                                       │
   │  Yeh team seedha business value       │
   │  deliver karti hai — idea to prod     │
   └─────────────────────────────────────┘

2. PLATFORM TEAM (Enabling)
   ┌─────────────────────────────────────┐
   │  Team: Infrastructure Platform        │
   │  Responsibility: DevOps, infra        │
   │  Services: CI/CD, K8s, Monitoring    │
   │                                       │
   │  Yeh team stream-aligned teams ko     │
   │  enable karti hai — self-service      │
   └─────────────────────────────────────┘

3. ENABLING TEAM (Specialist)
   ┌─────────────────────────────────────┐
   │  Team: ML/AI Engineering              │
   │  Responsibility: ML infrastructure    │
   │  Services: Recommendation Engine      │
   │                                       │
   │  Yeh team specialised expertise       │
   │  provide karti hai — temporarily      │
   └─────────────────────────────────────┘

4. COMPLICATED-SUBSYSTEM TEAM
   ┌─────────────────────────────────────┐
   │  Team: Payment Gateway Integration    │
   │  Responsibility: Complex payment      │
   │  Services: Multi-gateway routing      │
   │                                       │
   │  Yeh team ek complicated subsystem    │
   │  manage karti hai jisme deep          │
   │  expertise chahiye                    │
   └─────────────────────────────────────┘
```

### Conway's Law in Practice

```java
// Organization structure should mirror service boundaries

// ❌ BAD — Team structure aur service structure mismatch
/*
Organization:
  Team A: User + Order features
  Team B: Order + Payment features

Services:
  User Service (owned by Team A)
  Order Service (owned by Team A AND Team B) ← CONFLICT!
  Payment Service (owned by Team B)

Result:
  - Order service pe dono team ka conflict
  - Deployment coordination overhead
  - Blame game when things break
*/

// ✅ GOOD — One team, one service (or set of related services)
/*
Organization:
  Team A (User Team): User Service
  Team B (Order Team): Order Service
  Team C (Payment Team): Payment Service

Services:
  User Service (owned by Team A only)
  Order Service (owned by Team B only)
  Payment Service (owned by Team C only)

Result:
  - Clear ownership
  - Independent deployment
  - No cross-team coordination for deploy
  - Fast iteration
*/
```

### Conway's Law Best Practices

| Practice | Description |
|----------|-------------|
| **Inverse Conway Maneuver** | Pehle teams restructure karo, phir architecture follow karega |
| **Two-Pizza Teams** | Ek team itni chhoti ki 2 pizza se feed ho sake (5-9 people) |
| **Clear ownership** | Har service ka ek clear owner team ho |
| **Minimize cross-team dependencies** | Services aur teams dono mein coupling minimize karo |
| **Communication paths** | Team boundaries = API boundaries |
| **Autonomous teams** | Teams ko technical decisions lene ki freedom do |

---

## Summary & Best Practices

### All Principles at a Glance

| Principle | One-Liner | Key Benefit |
|-----------|-----------|-------------|
| **Single Responsibility** | Ek service = ek business capability | Focus, maintainability |
| **Loose Coupling** | Services minimally depend on each other | Independent evolution |
| **High Cohesion** | Related code ek saath | Understandability |
| **Independent Deploy** | Deploy without coordinating | Fast releases |
| **Decentralized Data** | Database per service | Schema autonomy |
| **Failure Isolation** | One failure doesn't cascade | System resilience |
| **Polyglot Persistence** | Right DB for each service | Optimal performance |
| **Bounded Context** | Clear domain model boundaries | No model pollution |
| **Conway's Law** | Team structure = system structure | Organizational alignment |

### Microservices Design Checklist

```
┌──────────────────────────────────────────────────────────────┐
│              MICROSERVICES DESIGN CHECKLIST                   │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  □ Har service ka ek clear business capability hai?           │
│  □ Services loosely coupled hain (interface/HTTP/events)?    │
│  □ Har service independently deploy ho sakti hai?            │
│  □ Har service ka apna database hai?                         │
│  □ Circuit breaker / bulkhead implement kiya hai?            │
│  □ Timeouts aur retries properly configure hain?             │
│  □ Database choice data pattern ke hisaab se hai?            │
│  □ Bounded contexts clearly defined hain?                    │
│  □ Team structure service boundaries se match karti hai?     │
│  □ API versioning strategy hai?                              │
│  □ Distributed transactions (Saga) handle kiye hain?         │
│  □ Logging, monitoring, tracing setup hai?                   │
│  □ Health checks implement kiye hain?                        │
│  □ Security (auth, authz) properly handle kiya hai?          │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

### Common Pitfalls to Avoid

| Pitfall | Problem | Solution |
|---------|---------|----------|
| **Distributed Monolith** | Services alag par tightly coupled | Proper boundaries, async communication |
| **Shared Database** | Multiple services ek hi DB use karein | Database per service |
| **No failure handling** | Ek failure = cascading failure | Circuit breaker, bulkhead, fallback |
| **Too many services** | 50+ microservices se start | Monolith se start karo, need pe todo |
| **Ignoring team structure** | Same team, multiple services = overhead | Conway's Law follow karo |
| **No monitoring** | Pata nahi chalta kaunsi service slow hai | APM, distributed tracing |
| **Synchronous everything** | Sab sync calls = latency cascade | Async events wherever possible |
| **Premature microservices** | Bina valid kiye microservices banaye | Pehle monolith mein domain validate karo |

### When to Use Microservices (and When Not To)

```
USE MICROSERVICES WHEN:
✅ Team size 10+ developers (multiple teams)
✅ Different scaling needs for different parts
✅ Different tech requirements
✅ Need independent deployment
✅ Complex domain with clear boundaries
✅ Organization can support operational complexity

DON'T USE MICROSERVICES WHEN:
❌ Small team (< 5 developers)
❌ Simple application, simple domain
❌ No DevOps/infra expertise
❌ Tight deadline, need to validate idea fast
❌ No clear bounded contexts identified
❌ Just because it's "trendy"
```

---

## Interview Questions

### Single Responsibility Principle

**Q: Microservices mein SRP kya hai?**
A: Ek microservice ka sirf ek reason to change hona chahiye — ek business capability. Jaise User Service sirf user management handle karta hai, Order Service sirf orders. Agar ek service multiple kaam kar rahi hai toh use aur chhote services mein tod do.

**Q: SRP violate ho raha hai toh kaise pata chalega?**
A: Agar ek service ko different reasons se baar-baar change karna pad raha hai (jaise user feature aur order dono change ho rahe hain), ya team mein different log different parts of same service maintain kar rahe hain — toh SRP violate ho raha hai.

### Loose Coupling & High Cohesion

**Q: Loose coupling kaise achieve karte ho?**
A: Interface-based communication (Feign clients), event-driven architecture (Kafka), API contracts. Direct database access mat karo doosri service ka — hamesha API ya events use karo.

**Q: High cohesion ka kya matlab hai?**
A: Ek service ke andar ke classes, methods closely related hone chahiye. User Service mein sirf user-related code hona chahiye — order, payment, notification logic nahi.

### Independent Deployability

**Q: Independent deployability ka kya benefit hai?**
A: Ek team apni service ko bina baaki teams coordinate kiye deploy kar sakti hai. Fast releases, no deployment windows, no big-bang deployments.

**Q: API versioning kyun zaroori hai?**
A: Jab ek service update hoti hai, toh purane clients ko break nahi hona chahiye. API versioning se old clients v1 use karte hain, new clients v2 — both work simultaneously.

### Decentralized Data Management

**Q: Shared database anti-pattern kyun hai?**
A: Shared database se services tightly coupled ho jaati hain. Schema change karne ke liye sab services coordinate karni padti hain. Database lock ya performance issue sab services ko affect karta hai.

**Q: Distributed transactions kaise handle karte ho?**
A: Saga pattern use karte hain. Har step ka compensating action hota hai. Agar koi step fail ho toh compensating actions se system ko consistent state mein wapas laate hain.

### Failure Isolation

**Q: Circuit breaker kya hai?**
A: Circuit breaker ek pattern hai jo failing service ko baar-baar call karne se rokta hai. Jab failure threshold cross ho jaata hai, circuit OPEN ho jaata hai aur requests directly fail ho jaati hain (no wait). Kuch time baad HALF-OPEN mein jaake test karta hai ki service recover hui ya nahi.

**Q: Bulkhead pattern kya hai?**
A: Bulkhead resources ko isolate karta hai. Payment service ke liye alag thread pool, User service ke liye alag. Agar Payment service ka thread pool full ho, User service unaffected rehta hai.

### Bounded Context

**Q: Bounded context kya hai?**
A: Bounded context ek logical boundary hai jahan ek specific domain model apply hota hai. Same entity (jaise Product) different contexts mein different attributes aur behaviors rakhti hai.

**Q: Anti-Corruption Layer kya hai?**
A: ACL ek translation layer hai jo external system ke model ko apne context ke model mein convert karta hai. External changes se apna context protected rehta hai.

### Conway's Law

**Q: Conway's Law kya hai?**
A: Conway's Law kehta hai ki organizations jo systems design karte hain, unka design organization ke communication structure ki copy hota hai. Team structure = software architecture.

**Q: Reverse Conway Maneuver kya hai?**
A: Agar specific architecture chahiye (jaise microservices), toh pehle teams ko us architecture ke hisaab se organize karo. Architecture automatically follow karega.

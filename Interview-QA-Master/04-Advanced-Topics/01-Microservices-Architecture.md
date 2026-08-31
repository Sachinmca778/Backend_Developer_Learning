# Microservices Architecture - Top 10 Interview Questions & Answers

> Covering: Microservices principles, Service Communication, API Gateway, Service Discovery, Resilience Patterns, Saga Pattern, Event-Driven Architecture, and Distributed Tracing

---

## Q1: Microservices kya hain aur Monolith se kaise alag hain?

**Answer:**

Microservices architecture ek large application ko chhote **independent services** mein todta hai, jahan har service ek specific **business capability** handle karti hai.

### Monolith vs Microservices:

```
MONOLITH                          MICROSERVICES
┌────────────────────┐           ┌──────────┐ ┌──────────┐ ┌──────────┐
│ User │ Order │ Pay  │           │  User    │ │  Order   │ │ Payment  │
├──────┴───────┴──────┤           │ Service  │ │ Service  │ │ Service  │
│  Single Database    │           │ ┌──────┐ │ │ ┌──────┐ │ │ ┌──────┐ │
└─────────────────────┘           │ │ DB   │ │ │ │ DB   │ │ │ │ DB   │ │
                                   │ └──────┘ │ │ └──────┘ │ │ └──────┘ │
                                   └──────────┘ └──────────┘ └──────────┘
```

### Key Characteristics:

| Characteristic | Description |
|----------------|-------------|
| **Single Responsibility** | Har service ek hi kaam acche se |
| **Loose Coupling** | Services ek doosre se loosely coupled |
| **Independent Deploy** | Ek service ko alag deploy kar sakte |
| **Decentralized Data** | Har service ka apna database |
| **Failure Isolation** | Ek service fail ho, baaki chalti rahen |
| **Polyglot** | Har service apni tech/DB choose kare |

**Short Interview Answer:**

Microservices independent, small services hain jinme har ek business capability handle karti hai, apna database rakhti hai, aur independently deploy hoti hai. Monolith mein sab ek unit hota hai — ek bug pura app down kar sakta hai, scaling poore app ko scale karna padta hai. Microservices failure isolation aur independent scaling dete hain.

---

## Q2: Microservices ke main principles kaunse hain?

**Answer:**

### 1. Single Responsibility:

Ek service = ek business capability. God service (sab kuch ek jagah) anti-pattern hai.

### 2. Loose Coupling, High Cohesion:

Services APIs/events se communicate karti hain, ek doosre ke internal database ko directly access nahi karti.

### 3. Decentralized Data Management (Database per Service):

```
User Service → User DB
Order Service → Order DB
Payment Service → Payment DB
```

→ Koi shared database nahi. Ek service doosre ka data API se maangti hai.

### 4. Bounded Context (DDD):

Har service ek clear domain boundary rakhti hai. Same concept alag context mein alag ho sakta hai (Order mein "Customer" ≠ Support mein "Customer").

### 5. Failure Isolation:

Ek service fail ho to baaki kaam karti rahen (resilience patterns se).

### 6. Conway's Law:

System design organization ki communication structure ko mirror karta hai — isliye teams service boundaries ke around organize hone chahiye.

**Important:**

Sabse critical principle **Database per Service** hai — ye true independence deta hai. Shared database = distributed monolith (worst of both worlds). Bounded Context (DDD) se service boundaries decide karo, technical layers se nahi.

---

## Q3: Microservices mein services aapas mein kaise communicate karti hain?

**Answer:**

Do main patterns: **Synchronous** aur **Asynchronous**.

| Aspect | Synchronous | Asynchronous |
|--------|-------------|--------------|
| **Blocking** | Client wait karta hai | Fire-and-forget |
| **Coupling** | Tight (both up hone chahiye) | Loose (queue buffer) |
| **Use Case** | Query, real-time data | Events, notifications |
| **Example** | Order → Inventory check | Order placed → Email |

### Synchronous — REST / gRPC / OpenFeign:

```java
@FeignClient(name = "inventory-service")
public interface InventoryClient {
    @GetMapping("/inventory/{productId}")
    InventoryResponse check(@PathVariable String productId);
}
```

- **REST** — simple, human-readable, HTTP/JSON
- **gRPC** — fast, binary (Protobuf), strong contracts
- **OpenFeign** — declarative REST client
- **WebClient** — reactive, non-blocking (recommended over RestTemplate)

### Asynchronous — Message Queues:

```java
// Producer fire-and-forget
kafkaTemplate.send("order-events", orderEvent);
```

- Kafka, RabbitMQ, AWS SQS
- Loose coupling, buffering, resilience

**Short Interview Answer:**

Synchronous (REST, gRPC, OpenFeign, WebClient) jab immediate response chahiye — but tight coupling. Asynchronous (Kafka, RabbitMQ) events/notifications ke liye — loose coupling, buffering. Best practice: queries ke liye sync, events ke liye async. Sync chains avoid karo (cascading failures).

---

## Q4: API Gateway kya hai aur kyun use karte hain?

**Answer:**

API Gateway saare client requests ka **single entry point** hai jo backend microservices ke aage baithta hai.

```
Client → [API Gateway] → User Service
                       → Order Service
                       → Payment Service
```

### Responsibilities:

| Function | Purpose |
|----------|---------|
| **Routing** | Request ko sahi service par bhejna |
| **Authentication** | Central auth/token validation |
| **Rate Limiting** | Throttling, abuse prevention |
| **Load Balancing** | Requests distribute karna |
| **Aggregation** | Multiple services ke responses combine |
| **SSL Termination** | HTTPS handle karna |
| **Logging/Metrics** | Central observability |

### Spring Cloud Gateway Example:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
          filters:
            - name: CircuitBreaker
              args:
                name: orderCB
                fallbackUri: forward:/fallback/orders
```

**Important:**

API Gateway cross-cutting concerns (auth, rate limiting, logging) ko centralize karta hai taaki har service mein repeat na karna pade. Client ko internal service structure nahi pata chalta. But gateway single point of failure ban sakta hai — isliye HA (multiple instances) mein deploy karo. Gateway mein business logic mat daalo.

---

## Q5: Service Discovery kya hai aur kaise kaam karta hai?

**Answer:**

Microservices mein service instances dynamically aati-jaati hain (scaling, failures, deployments) aur unke IP/port change hote rehte hain. Service Discovery services ko ek doosre ko dhundhne deta hai bina hardcoded addresses ke.

### Components:

```
Service A → [Service Registry] ← Service B registers
              (Eureka/Consul)
Service A "order-service kahan hai?" → Registry returns instances
```

### Patterns:

| Pattern | Description |
|---------|-------------|
| **Client-side** | Client registry se poochta hai, khud load balance karta hai (Eureka) |
| **Server-side** | Load balancer registry query karta hai (K8s Service) |

### Spring Cloud + Eureka:

```java
@EnableEurekaServer   // registry
public class RegistryApplication { }

@EnableDiscoveryClient  // service registers itself
public class OrderServiceApplication { }
```

```java
// Discovery se load-balanced call
@FeignClient(name = "inventory-service")  // logical name, not IP
public interface InventoryClient { ... }
```

**Short Interview Answer:**

Service Discovery services ko dynamic IPs ke bawajood ek doosre ko dhundhne deta hai. Services registry (Eureka/Consul) par register hoti hain, aur consumers logical name se instances lookup karte hain. Client-side discovery (Eureka) mein client load balance karta hai; Kubernetes mein server-side discovery (DNS + Service) built-in hai.

---

## Q6: Resilience patterns kaunse hain (Circuit Breaker, Retry, Bulkhead)?

**Answer:**

Microservices mein failures inevitable hain. Resilience patterns ek service ke failure ko doosri services par cascade hone se rokte hain.

### Cascading Failure Problem:

```
Order → Payment (slow) → Bank (timeout)
         100 threads waiting → thread pool full → Payment crash → Order crash
```

### 1. Circuit Breaker (Resilience4j):

```java
@CircuitBreaker(name = "paymentService", fallbackMethod = "fallback")
public PaymentResponse charge(PaymentRequest req) {
    return paymentClient.charge(req);
}

public PaymentResponse fallback(PaymentRequest req, Throwable t) {
    return PaymentResponse.pending();
}
```

States: **CLOSED** (normal) → **OPEN** (fail fast) → **HALF_OPEN** (test recovery).

### 2. Retry:

```java
@Retry(name = "inventoryService", fallbackMethod = "fallback")
```

→ Transient failures ke liye. Exponential backoff use karo.

### 3. Bulkhead:

Resources (threads) ko isolate karta hai taaki ek service ka overload doosri ko na dubaye.

### 4. Rate Limiter:

Requests limit karta hai overload rokne ke liye.

### 5. Timeout:

Har call par timeout taaki thread hamesha ke liye block na ho.

**Important:**

Circuit Breaker sabse important hai — repeated failures detect karke "fail fast" karta hai (OPEN state), taaki failing service ko recover hone ka time mile aur threads waste na ho. Patterns combine hote hain: Timeout + Retry + CircuitBreaker + Fallback. Fallback hamesha graceful degradation de (cached data, default response).

---

## Q7: Distributed transactions kaise handle karte hain? Saga Pattern kya hai?

**Answer:**

Microservices mein har service ka apna database hai, isliye traditional ACID transactions (`@Transactional`) across services kaam nahi karti.

### Why NOT 2PC:

2-Phase Commit blocking hai, coordinator single point of failure hai, aur high latency deta hai — microservices ke liye suitable nahi.

### Saga Pattern:

Distributed transaction ko **local transactions ki sequence** mein todta hai, har step ke saath ek **compensating transaction** (rollback).

```
Order created → Payment charged → Stock reserved → Shipped
   If Stock fails → compensate: refund payment, cancel order
```

### Do Types:

| Type | Description |
|------|-------------|
| **Choreography** | Events se drive — har service event publish/listen karti hai (decentralized) |
| **Orchestration** | Central orchestrator steps coordinate karta hai |

### Choreography (Event-Driven):

```
Order Service → "OrderCreated" event
Payment Service listens → charges → "PaymentCompleted" event
Stock Service listens → reserves → "StockReserved" event
```

### Orchestration (Central Coordinator):

```java
// Orchestrator har step call karta hai, fail par compensate
orderSaga.start()
    → createOrder()
    → chargePayment()   // fail? → cancelOrder()
    → reserveStock()    // fail? → refundPayment() + cancelOrder()
```

**Short Interview Answer:**

Saga distributed transactions ko local transactions ki chain mein todta hai + har step ka compensating action (rollback). 2PC ki jagah use karte hain kyunki wo blocking aur slow hai. Choreography (event-driven, decentralized) chhote flows ke liye; Orchestration (central coordinator) complex flows ke liye jahan visibility chahiye.

---

## Q8: Event-Driven Architecture kya hai aur kab use karte hain?

**Answer:**

Event-Driven Architecture (EDA) mein services **events** produce aur consume karti hain, direct calls ki jagah. Ek service state change hone par event publish karti hai; interested services react karti hain.

```
Order Service → publishes "OrderPlaced" → Kafka
                                            ├→ Inventory Service (reserve stock)
                                            ├→ Notification Service (send email)
                                            └→ Analytics Service (track)
```

### Benefits:

| Benefit | Explain |
|---------|---------|
| **Loose Coupling** | Producer consumers ko nahi jaanta |
| **Scalability** | Async processing, buffering |
| **Extensibility** | Naya consumer add karna easy |
| **Resilience** | Consumer down ho to events queue mein wait |

### Patterns:

- **Event Notification** — "kuch hua" (thin event)
- **Event-Carried State Transfer** — event mein poora data
- **Event Sourcing** — state ko events ke sequence se derive karna
- **CQRS** — read/write models separate

### Transactional Outbox:

```
DB transaction: save entity + save event (same transaction)
→ Separate process outbox se event publish karta hai
```

→ Dual-write problem solve karta hai (DB commit + event publish atomically).

**Important:**

EDA loose coupling aur scalability deta hai, but eventual consistency introduce karta hai — data turant consistent nahi hota. Dual-write problem (DB save + event publish dono succeed hone chahiye) ke liye Transactional Outbox pattern use karo. EDA tab use karo jab async workflows, multiple consumers, ya high decoupling chahiye.

---

## Q9: Distributed Tracing kya hai aur kyun zaroori hai?

**Answer:**

Ek single user request kai microservices se guzarti hai. Distributed tracing us request ka **poora journey** track karta hai saari services ke across, taaki debugging aur performance analysis ho sake.

### Problem:

```
Request → Gateway → Order → Payment → Inventory → Notification
Agar slow/fail ho — kaunsi service culprit hai?
```

### Solution — Trace + Span:

```
Trace ID: abc123 (poori request ke liye same)
  ├─ Span: Gateway (5ms)
  ├─ Span: Order Service (20ms)
  │    ├─ Span: Payment Service (100ms) ← slow!
  │    └─ Span: Inventory Service (10ms)
```

- **Trace ID** — poore request journey ke liye unique
- **Span ID** — har service/operation ke liye
- Ye IDs headers ke through propagate hote hain

### Tools:

- **Micrometer Tracing** (Spring Boot 3+, Sleuth ka successor)
- **Zipkin / Jaeger** — trace visualization
- **OpenTelemetry** — vendor-neutral standard

### Spring Boot Setup:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

**Short Interview Answer:**

Distributed tracing ek request ka journey saari services ke across track karta hai using Trace ID (poore request ka) aur Span IDs (har operation ka), jo headers se propagate hote hain. Isse pata chalta hai kaunsi service slow/fail ho rahi hai. Spring Boot 3 mein Micrometer Tracing + Zipkin/Jaeger use karte hain.

---

## Q10: Microservices ke main challenges aur best practices kya hain?

**Answer:**

### Challenges:

| Challenge | Detail |
|-----------|--------|
| **Distributed complexity** | Network calls, partial failures |
| **Data consistency** | No ACID across services (eventual consistency) |
| **Testing** | Integration testing mushkil |
| **Observability** | Debugging across services |
| **Deployment** | Many services to manage |
| **Distributed transactions** | Saga pattern chahiye |

### Best Practices:

1. **Database per Service** — shared DB avoid karo
2. **Design for failure** — resilience patterns (Circuit Breaker, Retry)
3. **API Gateway** — single entry point
4. **Service Discovery** — dynamic addressing
5. **Centralized config** — Spring Cloud Config
6. **Distributed tracing** — observability
7. **Async where possible** — loose coupling
8. **Bounded Context** — DDD se boundaries decide karo
9. **CI/CD + containers** — independent deployment
10. **Idempotency** — retries safe banana

### Kab NOT to use Microservices:

- Small team / simple app
- Early-stage startup (monolith se start, baad mein split)
- Clear domain boundaries nahi hain

**Important:**

Microservices free lunch nahi hai — distributed systems complexity aati hai (network, consistency, observability). **Monolith-first** approach aksar sahi hai: simple monolith se start karo, jab scaling/team boundaries clear ho tab split karo. Har service independent honi chahiye (own DB, own deploy) warna "distributed monolith" ban jaata hai jo worst of both worlds hai.

**Short Interview Answer:**

Challenges: distributed complexity, eventual consistency (no ACID), observability, deployment overhead. Best practices: database per service, design for failure (resilience), API gateway, service discovery, distributed tracing, async communication, DDD bounded contexts. Small/simple apps ke liye monolith better hai — microservices scaling aur team independence ke liye.

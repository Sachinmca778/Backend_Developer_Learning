# Level 3: Microservices Architecture & Apache Kafka (Q214 - Q241)

---

### Q214 & Q229: Monolithic vs Microservices Architecture
**Answer (Hinglish):**

| Feature | Monolithic Architecture | Microservices Architecture |
| :--- | :--- | :--- |
| **Codebase** | Single unified codebase for all features. | Independent, domain-driven separate codebases. |
| **Deployment** | Poori application ek single unit me deploy hoti hai. | Har service independently deploy aur scale ho sakti hai. |
| **Technology** | Single tech stack (e.g. Pure Java). | Polyglot (Service A Java me, Service B Python/Go me). |
| **Failure Scope** | Ek memory leak pure system ko crash kar sakti hai. | Fault isolation (Ek service fail hone par baaki chalti rehti hain). |
| **Complexity** | Simple networking, simple testing. | Complex distributed networking, distributed tracing, network latency. |

---

### Q217 & Q218: API Gateway & Service Discovery
**Answer (Hinglish):**

- **API Gateway (Spring Cloud Gateway):**  
  Single Entry Point jo sabhi external client requests ko receive karta hai.  
  - *Responsibilities:* Reverse proxy routing, SSL termination, centralized authentication/JWT validation, rate limiting, request logging.
- **Service Discovery (Netflix Eureka / Consul):**  
  Cloud environment me containers dynamic IPs par spawn hote hain.  
  - Microservices startup par service discovery registry me apna service name aur dynamic IP register karti hain.
  - Calling service IP hardcode karne ki jagah registry se service name (e.g. `http://ORDER-SERVICE/orders`) se lookup karti hai.

---

### Q219 & Q220: Resilience Patterns & Resilience4j (Circuit Breaker)
**Answer (Hinglish):**

- **Circuit Breaker Pattern (Resilience4j):**  
  Agar downstream dependency (e.g. Payment Gateway) fail ya slow ho rahi hai, to bar-bar requests bhej kar apne threads exhaust karne ke bajaye circuit ko "OPEN" kar diya jata hai aur instant Fallback response return kiya jata hai.
- **3 States:**
  1. **CLOSED (Normal):** Saari requests downstream service tak jaati hain.
  2. **OPEN (Tripped):** Failure rate threshold cross hone par requests block ho jaati hain; direct fallback execute hota hai.
  3. **HALF-OPEN:** Specified wait time ke baad kuch trial requests pass karke check karta hai ki dependency recover hui ya nahi.

```java
@CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
public PaymentResponse makePayment(PaymentRequest request) {
    return restTemplate.postForObject("/pay", request, PaymentResponse.class);
}

// Fallback method
public PaymentResponse paymentFallback(PaymentRequest request, Throwable ex) {
    return new PaymentResponse("FAILED_QUEUED", "Payment gateway is currently down. Request queued.");
}
```

---

### Q226 & Q227: CAP Theorem & Saga Pattern for Distributed Transactions
**Answer (Hinglish):**

- **CAP Theorem:** Distributed system me aap simultaneously sirf do hi choose kar sakte hain:
  - **Consistency (C):** Har read latest write return karega.
  - **Availability (A):** Har request non-error response receive karegi.
  - **Partition Tolerance (P):** Network split hone par bhi system chalega.
  - *Real-world:* Network splits inevitable hain, isliye systems **CP** (e.g. HBase, MongoDB) ya **AP** (e.g. Cassandra, DynamoDB) hote hain.

- **Saga Pattern (Event-Driven Distributed Transactions):**
  - Microservices me 2-Phase Commit (2PC) slow hota hai aur scale nahi karta.
  - Saga ek series of local transactions hoti hai. Har service apni local DB update karti hai aur event publish karti hai.
  - **Compensating Transactions (Rollback):** Agar Step 3 (Payment) fail ho jaye, to Saga backward compensating events trigger karta hai jo Step 2 (Inventory) aur Step 1 (Order) ko cancel/reverse kar dete hain.
  - *Types:* **Choreography** (Event-driven without central coordinator) vs **Orchestration** (Central orchestrator service guides steps).

---

### Q224: Distributed Tracing (Micrometer, OpenTelemetry, Zipkin)
**Answer (Hinglish):**

Jab ek user request 6 microservices se pass hoti hai, to debugging ke liye Distributed Tracing use hoti hai:
- **Trace ID:** Poore end-to-end request lifecycle ke liye unique ID jo har service ke log me propagate hoti hai.
- **Span ID:** Har individual microservice ke andar ke execution segment ka ID.
- **Tools:** Spring Boot 3 Micrometer Tracing + Zipkin / Jaeger UI me visually dekh sakte hain ki 5 seconds ke total time me se kis microservice ne kitne milliseconds liye.

---

### Q231 - Q234: Apache Kafka Core Architecture
**Answer (Hinglish):**

Kafka ek distributed, horizontally scalable, fault-tolerant event streaming platform hai:

```
[ Producer ] ───writes───> [ Topic: "orders" ]
                            ├── Partition 0 ───> Consumer Group A (Consumer 1)
                            ├── Partition 1 ───> Consumer Group A (Consumer 2)
                            └── Partition 2 ───> Consumer Group A (Consumer 3)
```

- **Topic:** Categories jisme messages publish hote hain.
- **Partition:** Topic ko horizontally parallel split karne ki unit. Scalability aur parallelism partition level par achieve hoti hai.
- **Offset:** Har message ka partition ke andar sequential unique integer ID.
- **Consumer Group:** Multiple consumers jo milkar ek topic ko process karte hain. Har partition ek consumer group ke sirf ek hi consumer ko assign hota hai (Parallelism limit = Number of partitions).
- **Rebalancing:** Jab group me naya consumer add ya purana crash hota hai, to partitions re-assign hote hain.

---

### Q235: Endlessly Retrying Consumer & Dead Letter Queue (DLQ)
**Answer (Hinglish):**

- **Poison Pill Problem:** Ek corrupt message consumer ko bar-bar crash karta hai, jisse consumer offset commit nahi kar pata aur endless loop me retry karta rehta hai (lag sky-rockets).
- **Solution:**
  1. Configured retry count (e.g. 3 attempts with exponential backoff).
  2. Retry exhaust hone par message ko **Dead Letter Topic / Queue (DLQ)** me publish kar diya jata hai (`orders-dlt`).
  3. Consumer normal offset commit karke aage badh jata hai. Developers alerts dekh kar DLQ messages inspect karte hain.

---

### Q236: Message Ordering Guarantee in Kafka
**Answer (Hinglish):**

- **Crucial Rule:** Kafka **poore Topic level par total ordering guarantee NAHI karta**, sirf **single Partition level par strict order guarantee** karta hai!
- **How to achieve ordering:**
  - Messages produce karte waqt **Message Key** provide karein (e.g. `orderId` ya `userId`).
  - Kafka ka default partitioner key ka hash nikalta hai (`murmur2(key) % totalPartitions`). Same key ke saare messages guaranteed ek hi specific partition me jayenge aur usi sequence me consume honge!

---

### Q238: Exactly-Once Semantics (EOS) in Kafka
**Answer (Hinglish):**

1. **Idempotent Producer:** `enable.idempotence=true`. Producer har batch ke sath Producer ID (PID) aur Sequence Number bhejta hai. Broker duplicates ko silently discard kar deta hai.
2. **Transactional Messaging (`KafkaTemplate.executeInTransaction`):** Stream processing me read-process-write operations ko atomic transaction me wrap karta hai using Transaction Coordinator.

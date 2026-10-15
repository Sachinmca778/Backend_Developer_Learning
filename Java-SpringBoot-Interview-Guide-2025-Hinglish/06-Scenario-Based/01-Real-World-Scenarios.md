# Real-World Scenario-Based Interview Questions (Q307 - Q319)

---

### Q307: App takes 45s to start in Production vs 5s Locally. Why?
**Answer (Hinglish):**

Production aur Local me startup difference troubleshoot karne ke steps:
1. **Database Schema Validation (`ddl-auto`):** Production me agar database bahut bada hai aur `spring.jpa.hibernate.ddl-auto=validate` 500 tables ka foreign key metadata scan kar raha hai over remote AWS network, to heavy lag aata hai.
2. **Reverse DNS Lookup (`InetAddress.getLocalHost`):** JVM start hote waqt machine ka hostname resolve karta hai. Agar cloud container me DNS properly configured nahi hai, to 30-second ka timeout wait hota hai.
3. **Database Connection Pool Pre-warming:** Production pool size bada hota hai (e.g. 50 connections), local me 5 hota hai.
4. **Investigation Tool:** Spring Boot 2.4+ Startup Actuator enable karein:
   ```properties
   spring.application.startup.enabled=true
   ```
   `/actuator/startup` inspect karke exact bean execution time identify karein (e.g. kaun sa bean 35 seconds le raha hai).

---

### Q308: REST APIs responding in 5+ seconds in Production. Debugging Approach?
**Answer (Hinglish):**

Structured Top-to-Bottom Debugging:
1. **Network vs Server Isolation:** Browser Network Tab me **TTFB (Time To First Byte)** check karein. Agar TTFB high hai, matlab server side delay hai.
2. **Distributed Tracing (Zipkin/APM):** Request ka trace ID check karein. Timeline me split dekhein:
   - Controller Layer: 5ms
   - Service Logic: 10ms
   - **Database Query: 4.8 seconds (Bottleneck Found!)**
3. **Check DB Slow Query Log & Locks:**
   - Missing Index? (Table Scan on 50 lakh rows).
   - Database Connection Pool Starvation (HikariCP threads waiting for connections).
   - N+1 Query Problem in loop.
4. **Downstream 3rd Party API Calls:** Agar external API call synchronous chal rahi hai bina timeout ke, use `@Async` ya Circuit Breaker fallback se shield karein.

---

### Q309: App fine with 100 users, OOM with 1000 users. Fix?
**Answer (Hinglish):**

- **Root Cause Analysis:** 100 users me GC objects ko reclaim kar pa raha tha, 1000 users par object creation rate > GC reclaim rate ho gaya.
- **Top Suspects:**
  1. **Unbounded Queries:** Listing endpoints me bina pagination ke saara data memory me load karna (`userRepo.findAll()`). Fix: Mandatory Pageable (`size = 20`).
  2. **Thread Stack Memory Exhaustion:** Har HTTP thread 1MB leta hai. 1000 concurrent threads = 1GB RAM sirf stacks me! Fix: Tomcat max connections limit karein ya Java 21 Virtual Threads adopt karein.
  3. **Static Caches / Unclosed Streams:** Memory leaks. Heap dump analyze karein using Eclipse MAT.

---

### Q310: 503 Service Unavailable during Peak Traffic. Causes?
**Answer (Hinglish):**

1. **Tomcat Accept Count Queue Overflow:** Tomcat ka `server.tomcat.threads.max` (e.g. 200) aur `server.tomcat.accept-count` (e.g. 100) exhaust ho gaya.
2. **Kubernetes Autoscaling (HPA) Latency:** Traffic 5 seconds me 10x badha, jabki naye Pod ko scale aur warm-up hone me 45 seconds lagte hain. Solution: Pre-emptive scaling ya warm standby replicas.
3. **Downstream Gateway Circuit Breaker Tripped:** Circuit breaker ne downstream fail dekh kar fast 503 return kar diya.

---

### Q311: Users logged out every 30 mins despite 24h JWT. Why?
**Answer (Hinglish):**

Yeh classic production architectural mismatch hai:
- **Reason:** Client ne JWT token to 24 ghante ke liye banaya, lekin application code me **Spring Security default in-memory HTTP Session ya Redis session timeout** abhi bhi 30 minutes set tha!
- Spring Security filter request receive karne par JWT decode karne se pehle session validity check kar raha tha, aur 30 minutes inactive hone par session invalidate kar diya.
- **Solution:** Application ko 100% **Stateless** configure karein:
  ```java
  http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
  ```
  Iske baad Spring Security session par depend nahi karega, sirf direct JWT expiration claim verify karega.

---

### Q312: 10,000 req/sec from same IP (DDoS/Abuse). How to rate limit?
**Answer (Hinglish):**

Multi-layered protection design:
1. **Cloud Edge (Cloudflare / AWS WAF):** App server par aane se pehle edge layer par IP block ya CAPTCHA challenge trigger karein (Zero server load).
2. **API Gateway Level (Redis Token Bucket Algorithm):**
   - Spring Cloud Gateway + Redis Rate Limiter:
   ```yaml
   filters:
     - name: RequestRateLimiter
       args:
         redis-rate-limiter.replenishRate: 10
         redis-rate-limiter.burstCapacity: 20
         key-resolver: "#{@ipKeyResolver}"
   ```
   - Same IP ko HTTP `429 Too Many Requests` return kar dega.

---

### Q314: 30 seconds to load 10,000 products. How to optimize?
**Answer (Hinglish):**

1. **Mandatory Pagination:** 10,000 ek sath return karna galat design hai. Infinite scroll ya pagination (`page=0&size=20`) implement karein.
2. **DTO Projections:** Entity ke saare 40 columns load karne ki jagah sirf product card ke liye zaroori columns fetch karein (`id`, `name`, `thumbnail`, `price`).
3. **Redis Caching:** First 5 pages ka catalog data Redis me cache karein.
4. **Elasticsearch:** Complex faceted filter/search ke liye Elasticsearch use karein instead of SQL `LIKE '%query%'`.

---

### Q315: 'LazyInitializationException' in Prod but not in Tests. Why?
**Answer (Hinglish):**

- **Why it happened:**  
  Tests me `@Transactional` annotation test method par laga hota hai, isliye Hibernate Session test method end hone tak open rehta hai aur lazy collection successfully load ho jati hai.  
  Production me `@Transactional` Service layer par end ho jata hai. Jab Controller ya Jackson JSON serializer detached entity ke lazy field (`user.getOrders()`) ko read karta hai, to Session already closed hoti hai -> Crash!
- **Fix:**
  - Test method se `@Transactional` hatayein taaki test real prod behavior simulate kare.
  - Query me **`JOIN FETCH`** ya **`@EntityGraph`** use karein taaki data session ke andar hi fetch ho jaye.
  - Entities direct controller se return karne ke bajaye **DTOs** me map karein service layer ke andar.

---

### Q316: Two users updating same product simultaneously (Race Condition).
**Answer (Hinglish):**

- **Scenario:** Stock me sirf 1 item bacha hai. Dono users ne ek hi time par "Buy Now" dabaya.
- **Handling Strategies:**
  1. **Optimistic Locking (`@Version`):**
     - Entity me version column hoga. Jo user pehle commit karega uska version badh jayega. Doosre user ko `OptimisticLockException` aayegi: "Product was updated by another user, please retry."
  2. **Pessimistic Locking (`SELECT ... FOR UPDATE`):**
     - Pehla user row lock kar leta hai. Doosra user DB level par wait karta hai jab tak transaction complete na ho.
  3. **Atomic SQL Decrement:**
     ```sql
     UPDATE products SET stock = stock - 1 WHERE id = 101 AND stock > 0;
     ```
     Updated rows count check karein. Agar `0` rows affected hui, matlab stock khatam ho gaya!

---

### Q318 & Q319: Cascade Failures & Saga Pattern for Distributed Rollback
**Answer (Hinglish):**

- **Service A -> Service B -> Service C (Service C is down):**
  - **Cascade Failure:** Service B C ke timeout ka wait karte-karte apne saare threads exhaust kar leti hai, fir Service A bhi B ke liye exhaust ho jati hai -> Poora ecosystem freeze!
  - **Prevention:**
    1. Short timeouts configure karein (e.g. 1.5 seconds connect/read timeout).
    2. Resilience4j Circuit Breaker + Fallback cache response.
    3. Bulkhead isolation: Service C calls ke liye separate isolated thread pool reserve karein.

- **5 Microservices Order Transaction (One Fails):**
  - **Distributed Saga Pattern:**
  - Services: *Order (Created) -> Inventory (Reserved) -> Payment (Debited) -> Shipping (FAILED)*.
  - Shipping fail hote hi compensating events trigger honge:
    - *Refund Payment Event* (Payment Service money wapas karegi).
    - *Release Inventory Event* (Stock wapas increase hoga).
    - *Cancel Order Event* (Order status mark as FAILED).
  - Eventual consistency achieve hoti hai bina kisi distributed 2PC database lock ke!

# Level 4: System Design & Architecture (Q251 - Q261)

---

### Q251: How do you design a Scalable E-Commerce System?
**Answer (Hinglish):**

E-Commerce system design ke core architectural pillars:

```
[ DNS / Route 53 ]
       ↓
[ Cloudflare CDN (Static assets / Images) ]
       ↓
[ Load Balancer (ALB / NGINX) ]
       ↓
[ API Gateway (Auth, Rate Limiting, Routing) ]
       ↓
+---------------+---------------+---------------+
|               |               |               |
[Auth Service]  [Product Catalog] [Order Service] [Payment Service]
| (MySQL)       | (Redis+Mongo) | (PostgreSQL)  | (Vault+Stripe)
+---------------+---------------+---------------+
                        |               |
                        └───> [Kafka] <──┘ (Async Event Bus)
                                  ↓
                        [Inventory & Notification]
```

1. **High Concurrency Reads (Product Catalog):** Read-Heavy traffic (>95% reads). Redis distributed cache + CDN use karein.
2. **Flash Sale Inventory (Zero Over-Selling):** Redis distributed locks (`Redisson`) ya Lua scripts se atomic decrement (`DECRBY`) karein.
3. **Checkout & Payment:** Idempotency keys (`X-Idempotency-Key`) use karein taaki network retry me double charge na ho.
4. **Order Processing:** Kafka based asynchronous message queues use karein.

---

### Q252 & Q261: Distributed Caching with Redis
**Answer (Hinglish):**

- **In-Memory vs Distributed Cache:**
  - Local cache (`Caffeine`) microservice ke specific pod memory me rehta hai. Pod restart hone par ya multiple instances hone par cache inconsistent ho jata hai.
  - Redis ek centralized, in-memory, sub-millisecond latency wala distributed cache cluster hai.
- **Caching Patterns:**
  1. **Cache-Aside (Lazy Loading):** App pehle Redis me check karegi. Cache miss hone par DB se fetch karegi aur Redis me set karegi with TTL.
  2. **Write-Through:** Pehle cache me likhta hai, cache synchronous DB me likhta hai.
  3. **Write-Behind (Write-Back):** Cache me fast write karta hai, background thread async DB update karta hai (risk of data loss).
- **Cache Pitfalls:**
  - **Cache Stampede (Thundering Herd):** Hot key expire hone par 50,000 requests ek sath DB par gir jati hain. Solution: Mutex lock ya probabilistic early expiration.
  - **Cache Penetration:** Null / Non-existent keys query karna. Solution: Bloom Filters ya caching empty nulls with short TTL.

---

### Q253 & Q254: CQRS (Command Query Responsibility Segregation) & Event Sourcing
**Answer (Hinglish):**

- **CQRS Pattern:**
  - Traditional architecture me same DB model se read aur write dono hote hain.
  - **CQRS me Read aur Write models ko separate kar diya jata hai**:
    - **Command (Write):** Create, Update, Delete requests normalised relational DB (PostgreSQL) par execute hoti hain.
    - **Query (Read):** Read requests Elasticsearch ya MongoDB par execute hoti hain jo fast search aur denormalized view deti hain.
    - Sync Kafka events se asynchronous hoti hai.

- **Event Sourcing:**
  - Database me current state (e.g. `Balance = 500`) store karne ke bajaye, **har ek past state change ko ek immutable event** ke roop me append-only log me store kiya jata hai (`AccountCreated`, `MoneyDeposited +1000`, `MoneyWithdrawn -500`).
  - Current balance saare past events ko replay karke derive hota hai. Auditing aur financial applications ke liye industry standard hai.

---

### Q255: Database Sharding & Strategies
**Answer (Hinglish):**

Jab ek hi DB table itna bada ho jaye (e.g. 500 million rows) ki single server disk aur CPU handle na kar sake, to data ko multiple physical databases me horizontally split karte hain.
- **Sharding Strategies:**
  1. **Range-based Sharding:** E.g. User IDs 1-100000 Shard 1 me, 100001-200000 Shard 2 me (Hotspots ka risk rehta hai).
  2. **Hash-based Sharding:** `shardId = hash(userId) % totalShards` (Uniform distribution).
  3. **Geo-based Sharding:** Asia users Asia Shard me, US users US Shard me.

---

### Q259: The Strangler Fig Pattern (Legacy Migration)
**Answer (Hinglish):**

- **Big-Bang Rewrite Trap:** Pure monolithic application ko ek hi baar me scrap karke microservices me rewrite karna lagbhag hamesha fail hota hai.
- **Strangler Fig Pattern:**
  - Monolith ke aage ek API Gateway / Reverse Proxy baithate hain.
  - Ek chhota module (e.g. `User Notifications`) choose karke use nayi modern microservice me develop karte hain.
  - API Gateway par rule change karke notifications traffic naye service par route kar dete hain, baaki sab monolith par chalta rehta hai.
  - Dheere-dheere ek-ek karke saare features migrate karte hain jab tak purana monolith completely replace (strangle) na ho jaye!

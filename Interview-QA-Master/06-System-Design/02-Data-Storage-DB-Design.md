# Data Storage & Database Design - Top 10 Interview Questions & Answers

> Covering: SQL vs NoSQL, Sharding, Replication, Caching, and storage trade-offs for system design interviews

---

## Q1: SQL vs NoSQL ka decision kaise lete ho?

**Answer:**

Ek-line rule: **access pattern aur consistency needs** ke basis par choose karo.

| Factor | SQL | NoSQL |
|--------|-----|-------|
| Schema | Fixed/structured | Flexible |
| Transactions | Strong ACID | Often limited/tunable |
| Joins | Excellent | Usually denormalized model |
| Horizontal scaling | Harder | Easier by design |
| Best for | Financial/core transactional | Huge scale, flexible data |

### NoSQL families:
- Document (MongoDB)
- Key-Value (Redis, DynamoDB)
- Wide-column (Cassandra)
- Graph (Neo4j)

**Short Interview Answer:**

I choose SQL when strong consistency and relational queries are primary; NoSQL when scale, flexible schema, and high-throughput distributed access dominate.

---

## Q2: Database sharding kya hota hai? Partitioning se kya difference?

**Answer:**

Sharding = data ko multiple database nodes mein horizontally split karna.

```text
Users table:
Shard-1: user_id 1-10M
Shard-2: user_id 10M-20M
Shard-3: user_id 20M-30M
```

### Difference:
- Partitioning often same DB instance ke andar logical split
- Sharding multiple DB instances/servers across

### Why shard?
- Write throughput scale
- Storage scale
- Hotspot isolation

---

## Q3: Sharding strategies kaunsi hoti hain?

**Answer:**

| Strategy | Pros | Cons |
|----------|------|------|
| Range-based | Simple | Hotspot risk (monotonic keys) |
| Hash-based | Balanced distribution | Range query hard |
| Consistent hashing | Rebalancing cost kam | Operational complexity |
| Directory/lookup | Flexible control | Extra lookup dependency |
| Geo-sharding | Local latency improve | Cross-region complexity |

**Important:**

Shard key selection sabse critical decision hai:
- High cardinality
- Even distribution
- Query-friendly
- Non-monotonic preferred

---

## Q4: Sharding ke common challenges kya hain?

**Answer:**

1. Cross-shard joins expensive  
2. Cross-shard transactions complex  
3. Rebalancing painful  
4. Global secondary indexes difficult  
5. Hot key / hotspot partition risk  

### Mitigations:
- Denormalization
- Application-level join
- Saga pattern for distributed transactions
- Async data pipelines for analytics

**Short Interview Answer:**

Sharding solves scale but adds query and operational complexity. The biggest risk is poor shard-key choice leading to hotspots and difficult migrations.

---

## Q5: Replication strategies kaunsi hoti hain?

**Answer:**

| Strategy | Description | Use case |
|----------|-------------|----------|
| Single-leader | One write leader, many read replicas | Most relational systems |
| Multi-leader | Multiple writable nodes | Multi-region active-active |
| Leaderless | Quorum-based writes/reads | Dynamo-style systems |

### Sync vs Async replication:
- **Synchronous**: better consistency, higher latency
- **Asynchronous**: lower latency, lag risk

**Important:**

Replication != sharding.
- Replication = copies for HA/read scaling
- Sharding = split for write/storage scaling

---

## Q6: Replication lag ka impact kya hota hai? Kaise handle karte ho?

**Answer:**

Replication lag se stale reads aati hain.

### Common issue:

```text
User profile update write leader par hua.
Immediate read replica se aayi, old value mili.
```

### Solutions:
- Read-your-writes routing (user ke immediate reads leader par)
- Lag-aware replica selection
- Quorum reads
- Critical flows mein strong consistency mode

**Short Interview Answer:**

For user-facing critical flows, I enforce read-your-writes consistency by routing post-write reads to leader or a guaranteed-fresh replica.

---

## Q7: Caching strategies kaun si hain aur kab use karte hain?

**Answer:**

| Strategy | Flow | Best for |
|----------|------|----------|
| Cache-aside | App miss pe DB se laake cache set kare | Most common read-heavy |
| Write-through | Write cache + DB together | Read consistency simple |
| Write-behind | Cache first, DB async | High write throughput |
| Read-through | Cache system DB fetch handle kare | Managed cache layers |
| Refresh-ahead | Expiry se pehle refresh | Predictable hot keys |

### Most practical:
Cache-aside + TTL + explicit invalidation

---

## Q8: Cache invalidation aur cache stampede kaise handle karte ho?

**Answer:**

### Invalidation patterns:
- On write: key delete/update
- Versioned keys
- Event-driven invalidation

### Stampede problem:
Hot key expire hote hi thousands of misses DB ko hit karte hain.

### Mitigation:
1. Per-key lock
2. Jittered TTL (randomized expiry)
3. Soft TTL + background refresh
4. Multi-level cache (L1 + L2 Redis)

**Important:**

Caching ka hardest part invalidation hai, not putting values in cache.

---

## Q9: Eviction policies kaunsi choose karte ho?

**Answer:**

| Policy | Best for | Limitation |
|--------|----------|------------|
| LRU | Recency-biased workloads | Frequency ignore karta |
| LFU | Stable hot-key workloads | More bookkeeping |
| FIFO | Simple | Quality lower |
| TTL-only | Time-sensitive data | Access pattern ignore |

### Practical:
- Redis: often `allkeys-lru` or `allkeys-lfu`
- Per key TTL based on data freshness requirements

**Short Interview Answer:**

I choose eviction based on access pattern: LRU for recency-heavy traffic, LFU for long-term hot keys. TTL is mandatory for stale data control.

---

## Q10: Interview mein storage architecture ka final answer kaise frame karte ho?

**Answer:**

Answer format:
1. Data model + access pattern
2. Primary store choice (SQL/NoSQL) with reason
3. Read scaling (replicas + cache)
4. Write scaling (sharding if needed)
5. Consistency model
6. Failure/recovery approach

### Example closing:

```text
I’d use PostgreSQL for transactional integrity, Redis for hot reads, read replicas for read scale, and shard by tenant_id once write throughput crosses single-node limits. Critical user flows use strong consistency; analytics and counters use eventual consistency.
```

**Important:**

Storage decisions always trade-off karte hain: consistency, latency, cost, and operability. Yeh explicitly bolna interview mein high-signal answer hota hai.


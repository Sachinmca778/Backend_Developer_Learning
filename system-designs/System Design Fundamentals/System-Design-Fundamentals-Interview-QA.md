# System Design Fundamentals - Top 10 Interview Questions & Answers

> Covering: RESHADED Interview Framework, Capacity Estimation, CAP Theorem, PACELC Theorem, and interview trade-off communication

---

## Q1: System design interview start kaise karein? Best framework kaunsa hai?

**Answer:**

Best approach: random bolna start mat karo. Structured framework use karo:

## **RESHADED**

| Step | Meaning | Interview mein kya karna hai |
|------|---------|------------------------------|
| **R** | Requirements | Functional + non-functional clear karo |
| **E** | Estimation | QPS, storage, bandwidth, DAU assumptions |
| **S** | Storage | SQL/NoSQL + schema + access patterns |
| **H** | High-level | Components + request flow diagram |
| **A** | API Design | Core endpoints and contracts |
| **D** | Detailed Design | DB/cache/queue/index/consistency deep dive |
| **E** | Evaluation | Bottlenecks, trade-offs, failure modes |
| **D** | Decisions | Why this design vs alternatives |

**Important:**

Interviewer ko dikhna chahiye ki tum "systematically think" karte ho. Pehle 5-7 min requirement clarifications mein lagana best practice hai.

**Short Interview Answer:**

I use a structured framework like RESHADED: clarify requirements, estimate scale, choose storage, design high-level architecture, define APIs, deep dive into bottlenecks, then evaluate trade-offs. This keeps the interview organized and avoids random design decisions.

---

## Q2: Functional vs Non-Functional requirements kaise identify karein?

**Answer:**

### Functional Requirements (system kya karega):
- User kya operations karega? (create/read/update/delete)
- Core use cases kya hain?
- Must-have features vs out-of-scope kya hai?

### Non-Functional Requirements (system kaisa behave karega):
- Scale: DAU, QPS, data growth
- Latency SLA: P95 < ? ms
- Availability: 99.9% vs 99.99%
- Consistency needs: strong vs eventual
- Durability, security, compliance

### Example (URL Shortener):
| Type | Requirement |
|------|-------------|
| Functional | Long URL ko short URL mein convert |
| Functional | Short URL hit pe redirect |
| Non-functional | 100M URLs/day handle |
| Non-functional | Redirect latency < 100ms |
| Non-functional | 99.99% availability |

**Important:**

Agar requirement ambiguous ho, assume mat karo — explicitly ask karo. Interviewer clarity aur communication evaluate karta hai.

---

## Q3: Capacity estimation kaise karte hain? Quick formulas kya hain?

**Answer:**

System design mein rough math bahut important hota hai.

### Common Formulas:

```text
QPS = (Daily Requests) / 86400

Storage per day = records_per_day × bytes_per_record

Bandwidth = QPS × payload_size
```

### Example:

```text
10M requests/day
QPS = 10,000,000 / 86,400 ≈ 116

Peak QPS (x3) ≈ 350
```

### Storage Example:

```text
5M records/day
record size = 1 KB

daily = 5 GB
yearly ≈ 1.8 TB
```

**Short Interview Answer:**

I do back-of-envelope estimation first: average and peak QPS, storage growth per day/year, and bandwidth. These numbers drive database, cache, and partitioning decisions.

---

## Q4: CAP theorem kya hai? Real life mein iska use kaise hota hai?

**Answer:**

CAP theorem ke 3 properties:

| Property | Meaning |
|----------|---------|
| **C** | Consistency: every read latest write de |
| **A** | Availability: every request ko response mile |
| **P** | Partition tolerance: network partition ke baad bhi system run kare |

### Key Rule:
Distributed systems mein **P mandatory** hai. Partition ke time tumhe **C ya A** choose karna padta hai.

### CP vs AP:
| Type | During partition behavior | Example style |
|------|--------------------------|---------------|
| **CP** | Consistency preserve, kuch requests reject | Zookeeper/HBase style |
| **AP** | Availability preserve, stale data possible | Cassandra/Dynamo style |

**Important:**

CAP ka matlab "hamesha C ya A permanently lose" nahi — mostly partition duration ke liye trade-off hota hai.

---

## Q5: PACELC theorem CAP se better kyun hai?

**Answer:**

CAP sirf partition time trade-off batata hai. PACELC normal operation (no partition) ka trade-off bhi cover karta hai.

```text
If P (partition): choose A or C
Else (no partition): choose L (latency) or C (consistency)
```

### Examples:
| System type | PACELC style |
|------------|--------------|
| Dynamo-like | PA/EL |
| HBase-like | PC/EC |
| Cassandra-like | PA/EL |

**Short Interview Answer:**

PACELC is a more practical extension of CAP. It says: under partition you choose availability vs consistency, and otherwise you still choose latency vs consistency. So real-world design is always about trade-offs, not binary labels.

---

## Q6: 99.9% vs 99.99% availability ka real difference kya hai?

**Answer:**

Availability ko “nines” mein express karte hain.

| Availability | Yearly downtime approx |
|-------------|-------------------------|
| 99.9% | ~8.76 hours/year |
| 99.99% | ~52.6 minutes/year |
| 99.999% | ~5.26 minutes/year |

### Interview Insight:
- 99.99% bolne ka matlab architecture mein redundancy har layer par chahiye.
- Higher nines = exponentially higher engineering + infra cost.

**Important:**

“High availability” bolna enough nahi — number mention karo aur trade-off discuss karo.

---

## Q7: Latency optimize karein ya consistency? Kaise decide karte ho?

**Answer:**

Depends on use case:

| Use Case | Preference |
|----------|-----------|
| Payments, balances | Strong consistency |
| Social feed, likes count | Low latency + eventual consistency |
| Analytics dashboards | Eventual consistency acceptable |
| Inventory locking | Strong consistency preferred |

### Practical strategy:
- Core critical flows: CP-ish
- Non-critical read-heavy flows: AP-ish + cache
- Hybrid patterns: read replicas, async projections

**Short Interview Answer:**

I map consistency level to business risk. Money and inventory workflows need strong consistency; feeds and counters can tolerate eventual consistency for lower latency and better availability.

---

## Q8: Interview mein assumptions kaise handle karein?

**Answer:**

Assumptions mandatory hain, but explicit honi chahiye.

### Good assumption style:

```text
Assume DAU = 10M
Average requests/user/day = 20
Peak factor = 3x
Data retention = 1 year
```

Phir bolo:

```text
If DAU doubles, we will shard by user_id and add read replicas.
```

**Important:**

Hidden assumptions dangerous hain. Visible assumptions + sensitivity analysis (what if 10x growth?) interviewer ko confidence deta hai.

---

## Q9: System design interview mein common mistakes kaun si hoti hain?

**Answer:**

1. Requirement clarify kiye bina design start karna  
2. Scale estimate skip karna  
3. Har jagah over-engineering (Kafka, sharding, CQRS bina need)  
4. Trade-offs explain na karna  
5. Failure cases ignore karna  
6. Monitoring/alerting discuss na karna  
7. Time manage na karna (deep dive too early)

### Better approach:
- 5-7 min requirements
- 5 min estimation
- 10 min high-level architecture
- 10 min deep dive
- 5 min bottlenecks + alternatives

---

## Q10: Strong interview closing answer kaise dena chahiye?

**Answer:**

End mein 1-minute summary do:

1. Requirements recap
2. Chosen architecture
3. Biggest trade-offs
4. Future improvements

### Example Closing:

```text
We designed a scalable URL shortener using hash-based ID generation,
Redis cache for hot mappings, SQL storage for durability, and read replicas
for scale. We prioritize low-latency redirects with eventual consistency
for analytics. Next scale step would be sharding by short-code prefix.
```

**Short Interview Answer:**

I close by summarizing requirements, architecture, key trade-offs, and next scaling steps. This shows structured thinking and helps the interviewer evaluate decisions quickly.


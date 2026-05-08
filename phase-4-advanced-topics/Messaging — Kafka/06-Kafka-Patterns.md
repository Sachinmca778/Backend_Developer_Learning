# Kafka Patterns

## Status: Not Started

---

## Table of Contents

1. [Why Patterns Matter](#why-patterns-matter)
2. [Dead Letter Topic (DLT)](#dead-letter-topic-dlt)
3. [Retry Topics](#retry-topics)
4. [Exactly-Once Semantics (EOS)](#exactly-once-semantics-eos)
5. [Transactional Outbox](#transactional-outbox)
6. [Compacted Topics (Event Sourcing)](#compacted-topics-event-sourcing)
7. [Kafka Streams](#kafka-streams)
8. [Consumer Lag Monitoring](#consumer-lag-monitoring)
9. [Idempotent Consumers](#idempotent-consumers)
10. [Saga Pattern with Kafka](#saga-pattern-with-kafka)
11. [Change Data Capture (CDC)](#change-data-capture-cdc)
12. [Topic Naming Conventions](#topic-naming-conventions)
13. [Common Pitfalls](#common-pitfalls)
14. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Why Patterns Matter

Kafka basics (topics, producers, consumers) are easy. **Production-grade** systems need:

- Handling failed messages gracefully
- Avoiding duplicate processing
- Coordinating across multiple writes (DB + Kafka)
- Replay-ability + auditability
- Observability + lag tracking

These patterns are battle-tested solutions.

---

## Dead Letter Topic (DLT)

**Matlab:** When message processing **repeatedly fails**, send the message to a **separate topic** for manual inspection — don't block the partition.

### Without DLT

```
Bad message arrives
  → process fails
  → retry (auto-commit off → re-deliver)
  → retry forever
  → partition stuck, lag grows
```

### With DLT

```
Bad message arrives
  → process fails N times
  → publish to "<topic>.DLT"
  → ack (move on)
  → main pipeline healthy
```

### Spring Kafka — DefaultErrorHandler + DeadLetterPublishingRecoverer

```java
@Bean
public DefaultErrorHandler errorHandler(KafkaTemplate<String, String> template) {
    DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(template,
            (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
    
    DefaultErrorHandler handler = new DefaultErrorHandler(
        recoverer,
        new FixedBackOff(1000L, 3L));      // 3 retries with 1s backoff
    
    handler.addNotRetryableExceptions(
        IllegalArgumentException.class,
        JsonParseException.class);
    
    return handler;
}
```

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, String> factory(
        ConsumerFactory<String, String> cf, DefaultErrorHandler errorHandler) {
    ConcurrentKafkaListenerContainerFactory<String, String> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(cf);
    factory.setCommonErrorHandler(errorHandler);
    return factory;
}
```

### DLT Listener (Inspect / Re-Process)

```java
@KafkaListener(topics = "orders.DLT", groupId = "dlt-orders-processor")
public void handleDLT(ConsumerRecord<String, String> record) {
    log.error("DLT: topic={} key={} value={}", record.topic(), record.key(), record.value());
    
    // Optionally inspect headers — Spring adds:
    record.headers().forEach(h -> log.info("Header {}: {}", h.key(), new String(h.value())));
    // kafka_dlt-original-topic, kafka_dlt-exception-message, etc.
    
    // Persist to DB for manual review, alert ops, etc.
    dltRepo.save(new DltRecord(record.topic(), record.key(), record.value(), Instant.now()));
}
```

### DLT Headers Auto-Added

```
kafka_dlt-original-topic
kafka_dlt-original-partition
kafka_dlt-original-offset
kafka_dlt-original-timestamp
kafka_dlt-exception-class
kafka_dlt-exception-message
kafka_dlt-exception-stacktrace
```

→ Useful for diagnosing.

### DLT Cleanup

DLT topic itself needs retention. Don't let it grow forever:

```bash
kafka-configs.sh --alter --topic orders.DLT --add-config retention.ms=2592000000   # 30 days
```

---

## Retry Topics

**Matlab:** Instead of in-place retries (which delay all subsequent records), publish to **delayed retry topics**.

### Architecture

```
orders → consumer-A
            │ on failure
            ▼
       orders.retry-1m → consumer-B (waits 1 min, retries)
            │ on failure
            ▼
       orders.retry-5m → consumer-C (waits 5 min, retries)
            │ on failure
            ▼
       orders.retry-30m → consumer-D (waits 30 min, retries)
            │ on failure
            ▼
       orders.DLT
```

### Spring Kafka @RetryableTopic

```java
@RetryableTopic(
    attempts = "4",
    backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 30000),
    autoCreateTopics = "true",
    topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
    dltStrategy = DltStrategy.FAIL_ON_ERROR
)
@KafkaListener(topics = "orders", groupId = "order-processor")
public void handle(Order order) {
    process(order);
}

@DltHandler
public void handleDLT(Order order) {
    log.error("Final failure: {}", order);
    // persist for review
}
```

→ Auto-creates `orders-retry-0`, `orders-retry-1`, ... + `orders-dlt`.

### Why Separate Topics for Retries?

In-place retry blocks the partition. Separate topics let normal processing continue while bad ones cool off.

### Order vs Retries Trade-Off

⚠️ Strict ordering broken — if record M fails and goes to retry, M+1 processed first.

Acceptable for most async workflows; not for strictly-ordered pipelines.

---

## Exactly-Once Semantics (EOS)

**Matlab:** Each record processed **exactly once** — no duplicates, no losses.

### EOS Within Kafka

#### Producer Side

```yaml
enable.idempotence: true              # no duplicates from producer retries
transactional.id: tx-orders-${HOSTNAME}
```

#### Consumer Side

```yaml
isolation.level: read_committed       # ignore aborted transactions
```

### EOS for Read-Process-Write

Pattern: read from input topic → transform → write to output topic.

```java
producer.initTransactions();

while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
    
    producer.beginTransaction();
    
    for (ConsumerRecord<String, String> record : records) {
        ProducerRecord<String, String> output = transform(record);
        producer.send(output);
    }
    
    // Commit offsets WITHIN transaction — atomic with sends
    Map<TopicPartition, OffsetAndMetadata> offsets = currentOffsets(records);
    producer.sendOffsetsToTransaction(offsets, consumer.groupMetadata());
    
    producer.commitTransaction();
}
```

→ Either all sends + offset-commit happen, or none.

### EOS Limitations

✅ Within Kafka — producer + consumer + Kafka Streams = EOS
❌ External side effects (DB writes, HTTP calls, emails) — NOT covered

→ Use **transactional outbox** for crossing boundaries.

### Kafka Streams EOS

```java
Properties props = new Properties();
props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, "exactly_once_v2");
```

→ EOS native to Kafka Streams.

---

## Transactional Outbox

**Matlab:** Atomic "DB write + Kafka publish" — solves the **dual-write problem**.

### The Problem

```java
@Transactional
public void placeOrder(Order order) {
    orderRepo.save(order);                            // DB write
    kafkaTemplate.send("orders", toJson(order));      // Kafka publish
}
```

❌ DB succeeds → Kafka fails → inconsistent
❌ DB succeeds → app crashes → Kafka never receives
❌ DB fails → Kafka succeeds → ghost event

### Outbox Pattern

```
@Transactional:
  1. INSERT into orders (...)
  2. INSERT into outbox (event_type, payload, ...)
  ↓
  Both committed atomically (same DB transaction)
  ↓
Background process / CDC reads outbox → publishes to Kafka → marks sent
```

### Schema

```sql
CREATE TABLE outbox (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(50),
    aggregate_id VARCHAR(100),
    event_type VARCHAR(50),
    payload JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    sent_at TIMESTAMP NULL
);
```

### Service Code

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final OrderRepository orderRepo;
    private final OutboxRepository outboxRepo;
    
    @Transactional
    public Order placeOrder(CreateOrderRequest req) {
        Order order = orderRepo.save(new Order(req));
        
        OutboxEvent event = new OutboxEvent(
            "Order", order.getId().toString(),
            "OrderCreated", toJson(order));
        outboxRepo.save(event);
        
        return order;
    }
}
```

### Outbox Polling Worker

```java
@Component
@RequiredArgsConstructor
public class OutboxWorker {
    
    private final OutboxRepository outboxRepo;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPending() {
        List<OutboxEvent> events = outboxRepo.findUnsent(100);
        
        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(
                    event.getAggregateType().toLowerCase(),
                    event.getAggregateId(),
                    event.getPayload()).get(5, TimeUnit.SECONDS);
                
                event.setSentAt(Instant.now());
                outboxRepo.save(event);
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}", event.getId(), e);
                // Will retry next tick
            }
        }
    }
}
```

### Better — CDC (Debezium)

Instead of polling, use **Change Data Capture** to read outbox table from DB transaction log:

```
Postgres WAL → Debezium → Kafka topic
```

→ No polling overhead; near-realtime. (See CDC section below.)

### Why It Works

Both inserts in **same DB transaction** → atomic.
If outbox publishes to Kafka multiple times (retry) → consumer must be **idempotent** (use the outbox event ID for dedup).

---

## Compacted Topics (Event Sourcing)

**Matlab:** Kafka retains **only the latest value per key** — like a key-value store backed by an event log.

### Use Cases

- **Event sourcing** state snapshots
- **KTable** in Kafka Streams
- **Configuration topics** (latest config per key)
- **Materialized views**

### Configure

```bash
kafka-topics.sh --create --topic user-profiles \
  --partitions 6 \
  --replication-factor 3 \
  --config cleanup.policy=compact
```

### Behavior

```
Time T1: producer sends key=u1, value=A
Time T2: producer sends key=u2, value=B
Time T3: producer sends key=u1, value=C
Time T4: producer sends key=u3, value=D

After compaction (background process):
  key=u2, value=B
  key=u1, value=C    ← only latest
  key=u3, value=D
```

### Tombstones (Deletes)

To delete a key, send record with `value=null`:

```java
producer.send(new ProducerRecord<>("user-profiles", "u1", null));
```

→ After retention, that key is removed from compacted topic.

### Combine with Time Retention

```yaml
cleanup.policy: compact,delete
retention.ms: 604800000        # 7 days
```

→ Both apply: keep latest per key, but also age-out old.

### Reading from Beginning Reconstructs State

Consumer with `auto.offset.reset=earliest` on compacted topic gets latest snapshot of all keys → useful for rebuilding state.

---

## Kafka Streams

**Matlab:** Library for building **stream processing** apps on Kafka — read, transform, aggregate, join, write back.

### Setup

```xml
<dependency>
  <groupId>org.apache.kafka</groupId>
  <artifactId>kafka-streams</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.kafka</groupId>
  <artifactId>spring-kafka</artifactId>
</dependency>
```

### Enable Streams in Spring

```java
@SpringBootApplication
@EnableKafkaStreams
public class StreamApp { ... }
```

### Configuration

```yaml
spring:
  kafka:
    streams:
      application-id: order-stream-app
      bootstrap-servers: kafka:9092
      properties:
        processing.guarantee: exactly_once_v2
        commit.interval.ms: 100
        state.dir: /var/lib/kafka-streams
```

### Simple Topology — Filter + Transform

```java
@Component
public class OrderStream {
    
    @Autowired
    public void buildTopology(StreamsBuilder builder) {
        KStream<String, String> orders = builder.stream("orders");
        
        orders
            .filter((key, value) -> value.contains("PRIORITY"))
            .mapValues(value -> uppercase(value))
            .to("priority-orders");
    }
}
```

### Aggregation Example — Count Per User

```java
KStream<String, Order> orders = builder.stream("orders");

KTable<String, Long> ordersPerUser = orders
    .groupByKey()
    .count(Materialized.as("orders-by-user"));

ordersPerUser.toStream().to("user-order-counts");
```

### Stateful Operations

Kafka Streams keeps state in **RocksDB** (local) backed by **changelog topic** (Kafka) → fault tolerant.

### Joins

```java
KStream<String, Order> orders = builder.stream("orders");
KTable<String, User> users = builder.table("users");

KStream<String, EnrichedOrder> enriched = orders.join(users,
    (order, user) -> new EnrichedOrder(order, user));

enriched.to("enriched-orders");
```

### Time Windows

```java
KTable<Windowed<String>, Long> count = orders
    .groupByKey()
    .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
    .count();
```

→ Counts per 5-min window.

### When to Use Kafka Streams vs Plain Consumer

| Need | Use |
|------|-----|
| Simple consume + business logic | KafkaConsumer / @KafkaListener |
| Filter, map, branch | Kafka Streams |
| Stateful aggregations | Kafka Streams |
| Joins (stream-stream, stream-table) | Kafka Streams |
| Windowed analytics | Kafka Streams |
| Need to call DBs/APIs | Plain consumer (Streams not designed for it) |

→ Or use **ksqlDB** for SQL-like stream processing.

---

## Consumer Lag Monitoring

**Matlab:** **Lag** = how many records behind the consumer is.

```
Topic latest offset:    1000
Consumer committed:      950
Lag:                      50 records
```

### Why Monitor?

- Lag growing → consumer can't keep up → users see stale data
- Lag oscillating → bursts overwhelming system
- Lag = 0 always → potentially over-provisioned

### Tools

#### `kafka-consumer-groups.sh`

```bash
kafka-consumer-groups.sh --bootstrap-server kafka:9092 \
  --describe --group order-processor

GROUP            TOPIC      PARTITION CURRENT-OFFSET  LOG-END-OFFSET  LAG
order-processor  orders     0         950             1000            50
order-processor  orders     1         800             900             100
```

#### Burrow (LinkedIn)

External lag monitoring tool — alerts on lag thresholds.

#### Kafka Lag Exporter

Prometheus metrics for consumer lag.

#### Confluent Control Center

UI for clusters + lag monitoring.

#### CloudWatch / Datadog Kafka integrations

Auto-collect lag metrics.

### Alerting

```
Alert if lag > 1000 records for > 5 min
Alert if lag growing for 10 min consecutive
```

### Application Metrics

```java
@Bean
public MeterBinder kafkaMetrics(KafkaListenerEndpointRegistry registry) {
    return registry::getListenerContainerIds;
}
```

→ Spring Kafka exposes via Micrometer:

```
spring.kafka.listener.records.lag
```

### What to Do When Lag Grows

1. **Scale consumers** (up to partition count)
2. **Increase partitions** (one-time, can only grow)
3. **Optimize processing** (faster code, async I/O)
4. **Drop slow records to DLT**
5. **Scale downstream dependencies** (DB, APIs)

---

## Idempotent Consumers

**Matlab:** Consumer can process **same record multiple times** without side effects.

### Why?

At-least-once delivery → duplicates possible (re-delivery on consumer crash, retries).

### Approaches

#### 1. Dedup Table

```sql
CREATE TABLE processed_events (
    event_id VARCHAR PRIMARY KEY,
    processed_at TIMESTAMP
);
```

```java
@Transactional
public void process(OrderEvent event) {
    if (processedRepo.existsById(event.getId())) {
        log.info("Skipping duplicate event {}", event.getId());
        return;
    }
    
    // do business logic
    orderRepo.save(...);
    
    processedRepo.save(new ProcessedEvent(event.getId(), Instant.now()));
}
```

→ Same DB transaction = atomic dedup + business work.

#### 2. Upsert Pattern

```sql
INSERT INTO orders (id, ...) VALUES (?, ...)
ON CONFLICT (id) DO NOTHING;
```

→ Database does the dedup.

#### 3. Conditional Update with Version

```sql
UPDATE inventory SET stock = stock - 1
WHERE product_id = ? AND last_event_id < ?
```

→ Only processes if event hasn't been applied yet.

#### 4. Check Idempotency Key

If event has ID (set by producer / outbox), use it:

```java
String idempotencyKey = record.headers().lastHeader("event-id").value().toString();
```

### Trade-Offs

| Approach | Pros | Cons |
|----------|------|------|
| Dedup table | Universal | Extra writes, table grows |
| Upsert | Simple | Specific to DB |
| Versioned update | Strong correctness | Requires version column |

→ Combine with **outbox** + **EOS** producer for full pipeline correctness.

---

## Saga Pattern with Kafka

For distributed transactions across multiple services — see `Microservices Architecture/07-Saga-Pattern.md`.

### Brief

```
Order Service: place order → emit OrderPlaced
   ↓
Inventory Service: reserve stock → emit StockReserved (or StockReservationFailed)
   ↓
Payment Service: charge → emit PaymentSuccess (or PaymentFailed)
   ↓
On failure → emit compensation events → other services roll back
```

→ Kafka as event backbone for choreography-based sagas.

---

## Change Data Capture (CDC)

**Matlab:** Capture every DB change (INSERT/UPDATE/DELETE) and publish to Kafka.

### Why?

- Stream DB changes to other systems (analytics, search index, cache invalidation)
- Implement outbox pattern without polling
- Replicate data across environments
- Build event-sourced views from existing DBs

### Tool — Debezium

```
PostgreSQL WAL → Debezium connector → Kafka topic per table
```

### Example Topology

```
DB table:  public.orders
Topic:     dbserver1.public.orders

Each row insert/update/delete = Kafka record:

Insert:
  {
    "before": null,
    "after": {"id": 1, "user_id": 100, "amount": 500},
    "op": "c"
  }

Update:
  {
    "before": {"id": 1, "user_id": 100, "amount": 500},
    "after": {"id": 1, "user_id": 100, "amount": 600},
    "op": "u"
  }
```

### Connectors

- Postgres, MySQL, SQL Server, Oracle, MongoDB, etc.
- Run on **Kafka Connect** infrastructure
- Distributed, scalable, fault-tolerant

### Outbox Event Router (Debezium)

Routes outbox table records to per-aggregate topics:

```
outbox table → Debezium → orders, payments, notifications topics
```

→ Combined with outbox pattern = best-of-both-worlds: atomic DB+publish, no polling.

---

## Topic Naming Conventions

Consistency = sanity. Common conventions:

```
<domain>.<entity>.<event>           # orders.order.created
<domain>.<entity>                    # orders.order
<env>.<domain>.<entity>.<event>     # prod.orders.order.created
```

### Examples

```
orders.events                            # all events for orders domain
orders.commands                          # commands targeted at orders
inventory.snapshot                       # compacted topic of latest state
analytics.clickstream                    # raw events
internal.outbox                          # outbox publication topic
```

### Suffixes for Special Topics

```
.DLT       (dead letter topic)
.retry-1m  (retry topics)
.changelog (Kafka Streams internal)
.state     (state stores)
```

### Versioning

```
orders.v1.events       # schema v1
orders.v2.events       # schema v2 (breaking change)
```

→ Run both during migration.

---

## Common Pitfalls

### 1. No DLT Strategy

Bad messages → infinite retry → lag → outage.

### 2. Outbox Without Idempotency on Consumer

Outbox guarantees at-least-once → consumer must dedup.

### 3. Forgetting Tombstones in Compacted Topics

To "delete" key, must send `value=null`. Otherwise key persists forever.

### 4. Compaction + Critical Audit

Compaction removes intermediate values. If audit needs full history, **don't compact**.

### 5. Kafka Streams State Loss

If state directory deleted or instance migrates → re-builds from changelog topic. Slow on large state.

### 6. Long Transactions Blocking Consumers

Transactional producer with long-lived txn → `read_committed` consumers block until commit.

### 7. CDC Without Schema Registry

Schema changes break downstream. Use Avro + Schema Registry for CDC streams.

### 8. Retry Topic Storm

If retry topics fill up faster than they drain → cascading lag everywhere.

→ Limit retries + always have DLT.

### 9. Lag Monitoring Skipped

"Things look fine" = "we have no observability." Set up lag dashboards day 1.

### 10. Saga Without Compensation

Saga without rollback events = inconsistent state. Plan compensation for each step.

### 11. Topic Sprawl

50 services → 500 topics — confusion. Document, naming convention, governance.

### 12. Misusing Kafka as DB

Kafka isn't a primary store for KV lookups. Use compacted topics + KTable for stream-based access; for direct point queries, materialize to a DB.

---

## Summary Cheat Sheet

| Pattern | Use Case |
|---------|----------|
| **DLT** | Quarantine bad messages |
| **Retry topics** | Delayed retries without blocking partition |
| **EOS** | Exactly-once within Kafka boundary |
| **Transactional outbox** | Atomic DB + Kafka publish |
| **Compacted topics** | KV state, event sourcing snapshots |
| **Kafka Streams** | Stateful stream processing |
| **Idempotent consumers** | Handle duplicates safely |
| **CDC (Debezium)** | DB changes → Kafka |
| **Saga** | Distributed transactions |

| Setting / Tool | Purpose |
|----------------|---------|
| `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` | DLT |
| `@RetryableTopic` | Retry topics |
| `enable.idempotence` + `transactional.id` | Idempotent + transactional producer |
| `isolation.level=read_committed` | Consume only committed |
| `cleanup.policy=compact` | Log compaction |
| `kafka-streams` | Stream processing |
| `kafka-consumer-groups.sh` | Lag inspection |
| Burrow / Kafka Lag Exporter | Lag monitoring |
| Debezium | CDC |

| ✅ Do | ❌ Don't |
|-------|---------|
| Set up DLT for every consumer | Hope no bad messages arrive |
| Idempotent consumer logic | Trust at-least-once + don't dedup |
| Outbox for atomic DB+publish | Direct dual-write |
| Monitor lag | Notice outage from customers |
| Schema Registry + Avro | Plain JSON for cross-team |
| Compensation events for sagas | Hope happy path always works |
| Document topic naming | Free-for-all topic creation |
| Test rebalances + failures | Only test happy path |

---

## Practice

1. Set up DLT for an existing `@KafkaListener`; send a poison message; verify routing.
2. Use `@RetryableTopic` for exponential retry topics with backoff.
3. Implement transactional outbox: `orders` table + `outbox` table in single transaction.
4. Build worker that polls outbox + publishes; verify retry on Kafka failure.
5. Switch outbox publication from polling to Debezium CDC.
6. Create a compacted topic; verify only latest value per key persists.
7. Build a Kafka Streams app that aggregates orders per user per hour.
8. Implement idempotent consumer using dedup table.
9. Set up lag monitoring dashboard (Prometheus + Grafana or Burrow).
10. Build a 3-step saga (order → inventory → payment) with compensation events.
11. Apply EOS: consumer group reading from input, producing to output, atomically.
12. Document your team's topic naming convention; audit existing topics.

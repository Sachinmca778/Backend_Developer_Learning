# Messaging — Kafka - Top 10 Interview Questions & Answers

> Covering: Kafka fundamentals, Topics & Partitions, Consumer Groups, Replication & ISR, Delivery Semantics, Spring Kafka (KafkaTemplate, @KafkaListener), and Error Handling

---

## Q1: Kafka kya hai aur traditional message queues se kaise alag hai?

**Answer:**

Kafka ek **distributed event streaming platform** hai — high-throughput, fault-tolerant, durable log of records (events), jo many producers aur consumers ke beech share hota hai.

### Mental Model — Append-Only Log:

```
Producer 1 ──┐
Producer 2 ──┼─▶ [m1][m2][m3][m4][m5][m6] ─▶ Consumer A
Producer 3 ──┘                            └──▶ Consumer B
                                            ↑
                            Old records readable until retention expires
```

→ Git log jaisa — append-only, replayable, ordered.

### Kafka vs Traditional Queues (RabbitMQ):

| Feature | RabbitMQ | Kafka |
|---------|----------|-------|
| **Model** | Push (broker → consumer) | Pull (consumer polls) |
| **Storage** | Ack ke baad delete | Durable, retention-based |
| **Replay** | Hard — message gone | Native — any offset se read |
| **Throughput** | 10K-100K msg/sec | 1M+ msg/sec per broker |
| **Ordering** | Per queue | Per partition |
| **Multiple consumers** | Fanout exchange | Native (consumer groups) |
| **Best for** | Task queue, RPC | Event streaming, log |

**Short Interview Answer:**

Kafka distributed event streaming platform hai — durable append-only log jisme records retention tak store rehte hain aur replay ho sakte hain. RabbitMQ traditional queue hai (ack ke baad delete). Kafka high-throughput event streaming ke liye, RabbitMQ task queues ke liye best.

---

## Q2: Topic, Partition aur Offset kya hote hain?

**Answer:**

| Term | Meaning |
|------|---------|
| **Topic** | Logical category of messages (e.g., `orders`) |
| **Partition** | Topic ka ordered, immutable sub-log |
| **Offset** | Partition ke andar record ka sequential ID |

### Partitions — Parallelism:

```
Topic "orders" — 3 partitions:
Partition 0:  [m0][m1][m2][m3]   ← order matters WITHIN partition
Partition 1:  [m0][m1][m2][m3]   ← parallel
Partition 2:  [m0][m1][m2][m3]   ← parallel
Offsets:        0   1   2   3
```

### Record Distribution:

```
key == null  → round-robin across partitions (no cross-partition ordering)
key != null  → partition = hash(key) % numPartitions
             → same key → same partition → ordering guaranteed for that key
```

```java
// user-123 ke saare orders → same partition → ordered
producer.send(new ProducerRecord<>("orders", "user-123", order1));
producer.send(new ProducerRecord<>("orders", "user-123", order2));
```

**Important:**

Kafka ordering **sirf per partition** guarantee karta hai, cross-partition nahi. Ordering chahiye to same key use karo (jaise `userId`). Partitions sirf **increase** kar sakte ho, decrease nahi — isliye soch samajh kar choose karo (start 6-12).

---

## Q3: Consumer Group kya hai aur rebalancing kaise hoti hai?

**Answer:**

Consumer Group ek set of consumers hai jo mil kar ek topic consume karte hain. **Har partition group ke exactly ek consumer** ko assign hoti hai.

### Consumer Group Mechanics:

```
Topic "orders" — 6 partitions [P0..P5]

Group "order-processor":
  Consumer A → P0, P1
  Consumer B → P2, P3
  Consumer C → P4, P5
```

### Multiple Groups — Independent Consumption:

```
Topic "orders" → Group "order-processor" → Analytics
              → Group "order-notifier"    → Email
              → Group "order-fulfillment"  → Warehouse
```

→ Har group independently read karta hai. Same record har group ko milta hai.

### Rebalancing:

Jab consumers join/leave karte hain, Kafka partitions reassign karta hai.

```
Triggers: new consumer joins / consumer leaves / partition added / subscription change
```

⚠️ **Rebalance ke dauran consumption pause** hoti hai. Long rebalances = lag.

**Short Interview Answer:**

Consumer group cooperating consumers ka set hai — har partition group ke ek hi consumer ko jaati hai (parallelism). Alag-alag groups same topic independently read karte hain. Jab consumer join/leave karta hai to rebalancing hoti hai jisme partitions reassign hote hain (temporarily pause).

---

## Q4: Replication aur ISR (In-Sync Replicas) kya hai?

**Answer:**

Har partition ke multiple **replicas** (copies) alag brokers par hote hain — broker failure se protection.

### How It Works:

```
Partition 0 (replication-factor=3):
  Broker 1 — LEADER    (reads + writes handle karta hai)
  Broker 2 — FOLLOWER  (leader se replicate)
  Broker 3 — FOLLOWER  (leader se replicate)
```

### ISR — In-Sync Replicas:

ISR = wo followers jo leader ke saath **caught up** hain (within `replica.lag.time.max.ms`, default 30s).

```
Leader: Broker 1
ISR: Broker 2 (in-sync), Broker 3 (lagging > 30s → ISR se nikaal diya)
```

### min.insync.replicas:

```
acks=all + min.insync.replicas=2 + replication-factor=3:
  → 3 mein se 2 ISR chahiye write success ke liye
  → 1 broker failure tolerate
  → strong durability
```

### Trade-Offs:

| Config | Behaviour |
|--------|-----------|
| RF=1, min.insync=1 | Fast, no durability |
| RF=3, min.insync=2 | Balanced (production default) |
| RF=3, min.insync=3 | Strongest, but koi loss tolerate nahi |

**Important:**

Production mein replication-factor=3, min.insync.replicas=2, acks=all use karo — ye 1 broker failure tolerate karta hai bina data loss ke. `unclean.leader.election.enable=false` (default) rakho taaki out-of-sync replica leader na bane (data loss avoid).

---

## Q5: Kafka mein message delivery semantics kaunse hote hain?

**Answer:**

| Semantic | Kaise | Result |
|----------|-------|--------|
| **At Most Once** | Fire and forget, no retry | Records lost ho sakte, duplicate nahi |
| **At Least Once** (default) | Retry + commit after process | Duplicate ho sakte, lost nahi |
| **Exactly Once** | Idempotent producer + transactions | No loss, no duplicate |

### At Least Once (Default):

```
Producer: retry on failure
Consumer: process → phir commit offset
```

→ Agar commit se pehle crash → dubara process (duplicate possible).

### Exactly Once (EOS):

```java
// Idempotent producer — retries se duplicate nahi
props.put("enable.idempotence", true);

// Transactions — all-or-nothing
producer.initTransactions();
producer.beginTransaction();
producer.send(record1);
producer.send(record2);
producer.commitTransaction();   // ya abortTransaction()
```

+ Consumer par `isolation.level=read_committed`.

**Short Interview Answer:**

Teen semantics: At-most-once (loss possible), At-least-once (duplicate possible, default), Exactly-once (idempotent producer + transactions). Zyadatar systems at-least-once use karte hain + consumer ko idempotent banate hain. EOS ka scope Kafka ke andar hai — external DB writes ke liye transactional outbox pattern chahiye.

---

## Q6: Spring Kafka mein message kaise produce aur consume karte hain?

**Answer:**

Spring Kafka Apache Kafka client ka idiomatic abstraction deta hai.

### Setup:

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

```yaml
spring:
  kafka:
    bootstrap-servers: kafka:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      properties:
        enable.idempotence: true
    consumer:
      group-id: order-processor
      auto-offset-reset: earliest
      enable-auto-commit: false
```

### Produce — KafkaTemplate:

```java
@Service
public class OrderProducer {
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public void send(OrderEvent event) {
        kafkaTemplate.send("orders", event.getUserId(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) log.error("Send failed", ex);
                else log.info("Sent to offset {}", result.getRecordMetadata().offset());
            });
    }
}
```

### Consume — @KafkaListener:

```java
@Component
public class OrderConsumer {
    @KafkaListener(topics = "orders", groupId = "order-processor")
    public void consume(OrderEvent event) {
        log.info("Received: {}", event);
        process(event);
    }
}
```

**Important:**

`KafkaTemplate` producer wrapper hai, `@KafkaListener` declarative consumer. Spring Boot auto-configuration se producer/consumer factories ban jaate hain. Production mein `enable-auto-commit: false` + manual acknowledgment use karo taaki processing ke baad hi offset commit ho.

---

## Q7: Spring Kafka mein manual acknowledgment aur batch listeners kaise kaam karte hain?

**Answer:**

### Manual Acknowledgment:

```yaml
spring:
  kafka:
    consumer:
      enable-auto-commit: false
    listener:
      ack-mode: manual
```

```java
@KafkaListener(topics = "orders", groupId = "order-processor")
public void consume(OrderEvent event, Acknowledgment ack) {
    try {
        process(event);
        ack.acknowledge();   // process ke baad hi commit
    } catch (Exception e) {
        log.error("Processing failed, offset not committed", e);
    }
}
```

### Batch Listener:

```java
@KafkaListener(topics = "orders", batch = "true")
public void consumeBatch(List<OrderEvent> events, Acknowledgment ack) {
    events.forEach(this::process);
    ack.acknowledge();
}
```

### Ack Modes:

| Mode | Behaviour |
|------|-----------|
| `RECORD` | Har record ke baad commit |
| `BATCH` | Poll batch ke baad commit (default) |
| `MANUAL` | App `ack.acknowledge()` call kare |
| `MANUAL_IMMEDIATE` | Immediately commit |

**Short Interview Answer:**

Manual acknowledgment (`ack-mode: manual` + `Acknowledgment` param) se offset tabhi commit hota hai jab processing successful ho — at-least-once ke liye zaroori. Batch listener ek poll mein multiple records ek saath process karta hai (higher throughput).

---

## Q8: Spring Kafka mein error handling aur Dead Letter Topic (DLT) kaise implement karte hain?

**Answer:**

Default mein failed record retry hota hai. Configured retries ke baad DLT bhej sakte hain.

### DefaultErrorHandler + Retry:

```java
@Bean
public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> template) {
    // 3 retries, 1 second interval
    var recoverer = new DeadLetterPublishingRecoverer(template);
    return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
}
```

→ 3 retry fail hone ke baad record `orders.DLT` topic par chala jaata hai.

### @RetryableTopic (Non-Blocking Retry):

```java
@RetryableTopic(
    attempts = "4",
    backoff = @Backoff(delay = 1000, multiplier = 2.0),
    dltStrategy = DltStrategy.FAIL_ON_ERROR
)
@KafkaListener(topics = "orders")
public void consume(OrderEvent event) {
    process(event);
}

@DltHandler
public void handleDlt(OrderEvent event) {
    log.error("Sent to DLT: {}", event);
}
```

### Non-Retryable Exceptions:

```java
errorHandler.addNotRetryableExceptions(ValidationException.class);
```

→ Validation errors retry nahi hone chahiye (kabhi succeed nahi karenge) — seedha DLT.

**Important:**

Poison messages (jo hamesha fail karenge) infinite retry loop bana sakte hain aur consumer block kar dete hain. Retry limit + DLT use karo. Non-retryable exceptions (validation) ko immediately DLT bhejo. `@RetryableTopic` non-blocking retry deta hai (alag retry topics use karke).

---

## Q9: Kafka mein retention aur log compaction mein kya difference hai?

**Answer:**

### Retention (Delete Policy — Default):

Kafka records ko time ya size ke basis par delete karta hai.

```yaml
retention.ms=604800000      # 7 days (default)
retention.bytes=10737418240 # 10GB per partition
```

→ Jo pehle hit ho → us segment ke records delete.

### Log Compaction:

Delete karne ki jagah, **har key ka latest value** rakhta hai.

```yaml
cleanup.policy=compact
```

```
Before compaction:
  key=u1, value=v1@offset0
  key=u2, value=v2@offset1
  key=u1, value=v3@offset2      ← u1 ka naya value

After compaction:
  key=u2, value=v2@offset1
  key=u1, value=v3@offset2      ← sirf latest u1 raha
```

### Use Cases:

| Policy | Use Case |
|--------|----------|
| `delete` (retention) | Event streams, logs, metrics |
| `compact` | Event sourcing, KTables, config topics, latest-state |
| `compact,delete` | Both — latest per key + time bound |

**Short Interview Answer:**

Retention time/size ke basis par purane records delete karta hai (event streams ke liye). Log compaction har key ka sirf latest value rakhta hai (config topics, event sourcing, latest-state ke liye). Dono combine bhi ho sakte hain.

---

## Q10: KRaft mode kya hai? ZooKeeper se kaise better hai?

**Answer:**

### Old Way — ZooKeeper:

Kafka ko metadata, controller election, ACLs ke liye ZooKeeper (alag cluster) chahiye tha.

```
❌ Do clusters manage karo (Kafka + ZooKeeper)
❌ ZK metadata operations ka bottleneck
❌ Scalability limit (~200K partitions)
```

### New Way — KRaft (Kafka Raft):

Kafka 2.8+ ne KRaft introduce kiya — Kafka apna metadata Raft consensus se manage karta hai. ZooKeeper-free. Kafka 4.0 mein ZK fully removed.

```
┌─────────────────────────────────────┐
│       Kafka Cluster (KRaft)          │
│  ┌────────┐ ┌────────┐ ┌────────┐    │
│  │Broker 1│ │Broker 2│ │Broker 3│    │
│  │+Ctrl   │ │+Ctrl   │ │+Ctrl   │    │
│  └────────┘ └────────┘ └────────┘    │
└─────────────────────────────────────┘
```

### Benefits:

| ZooKeeper | KRaft |
|-----------|-------|
| Do systems | Single system |
| Slow metadata ops | Fast metadata ops |
| ~200K partitions | Millions of partitions |
| Slow leader election (~10-30s) | Fast (~ms-seconds) |

**Important:**

KRaft Kafka ka naya architecture hai jisme metadata management Kafka ke andar hi Raft consensus se hota hai — ZooKeeper ki dependency khatam. Naye clusters KRaft se start karo. Yeh operations simplify karta hai aur recovery/leader-election dramatically fast karta hai.

**Short Interview Answer:**

KRaft (Kafka Raft) ZooKeeper ko replace karta hai — Kafka apna metadata khud Raft consensus se manage karta hai. Ek hi system manage karna padta hai, metadata ops fast hain, millions of partitions scale hote hain, aur leader election bahut fast hai. Kafka 4.0 se ZK completely gone.

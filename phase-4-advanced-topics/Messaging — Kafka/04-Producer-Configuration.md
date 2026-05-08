# Kafka Producer Configuration

## Status: Not Started

---

## Table of Contents

1. [Producer Architecture](#producer-architecture)
2. [acks (Acknowledgments)](#acks-acknowledgments)
3. [retries & retry.backoff.ms](#retries--retrybackoffms)
4. [max.block.ms](#maxblockms)
5. [delivery.timeout.ms](#deliverytimeoutms)
6. [linger.ms & batch.size](#lingerms--batchsize)
7. [compression.type](#compressiontype)
8. [enable.idempotence (Idempotent Producer)](#enableidempotence-idempotent-producer)
9. [Transactional Producer](#transactional-producer)
10. [max.in.flight.requests.per.connection](#maxinflightrequestsperconnection)
11. [Buffer Memory](#buffer-memory)
12. [Partitioner](#partitioner)
13. [Producer Tuning Profiles](#producer-tuning-profiles)
14. [Common Pitfalls](#common-pitfalls)
15. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Producer Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                      KafkaProducer (Java)                         │
│                                                                   │
│  send(record) ──▶ Serializer ──▶ Partitioner ──▶ Record Accumulator
│                                                          │         │
│                                                          ▼         │
│                                                  RecordBatch       │
│                                                  per (topic,part)  │
│                                                          │         │
└──────────────────────────────────────────────────────────┼─────────┘
                                                           │
                                            ┌──────────────▼──────────────┐
                                            │      Sender Thread           │
                                            │  (background I/O thread)     │
                                            │  Pulls batches → sends       │
                                            └──────────────┬──────────────┘
                                                           │
                                                           ▼
                                                   Kafka Broker (Leader)
```

### Send Flow

```
1. Producer.send(record) — non-blocking, returns Future
2. Serializer converts key + value to bytes
3. Partitioner picks partition (based on key or sticky/round-robin)
4. Record added to in-memory RecordAccumulator (buffer)
5. Sender thread pulls full batches (or after linger.ms)
6. Sends to broker; gets ack (per acks setting)
7. Future completes (or callback fires)
```

### Why Async?

Synchronous = block per record = terrible throughput.
Async = batch + parallel = millions of records/sec.

---

## acks (Acknowledgments)

**Matlab:** Producer ko **kab confirm milega** ki record successfully written.

### Options

| Setting | Behavior | Durability | Throughput |
|---------|----------|------------|-----------|
| `0` | Fire and forget | ❌ Records lost on broker failure | Highest |
| `1` | Wait for leader ack | ⚠️ Lost if leader fails before replication | Medium |
| `all` (or `-1`) | Wait for all in-sync replicas | ✅ Strongest (with `min.insync.replicas`) | Lower |

### `acks=0`

```yaml
acks: 0
```

```
Producer ──▶ Broker
            (no ack expected)
Producer continues immediately
```

→ Use **only** for log/metric ingestion where loss acceptable.

### `acks=1`

```yaml
acks: 1
```

```
Producer ──▶ Leader Broker
              writes to log
              acks
Producer continues
```

→ If leader crashes before replication → record lost.

### `acks=all`

```yaml
acks: all
```

```
Producer ──▶ Leader Broker
              ├─▶ Follower 1 (replicates)
              └─▶ Follower 2 (replicates)
              waits for ISR replicas to ack
              acks producer
Producer continues
```

→ Combined with `min.insync.replicas=2`, strongest durability.

### Recommendation

**Production: `acks=all`** unless throughput-critical streams (logs/metrics) explicitly accept loss.

---

## retries & retry.backoff.ms

### Retries

```yaml
retries: 2147483647            # MAX (Integer.MAX_VALUE)
retry.backoff.ms: 100          # 100ms between retries
```

→ Producer auto-retries on **retriable errors** (network blips, leader election).

### Why Max Retries?

Combined with `delivery.timeout.ms`, producer keeps retrying until total time elapses, not retry count. Max retries is "essentially infinite within delivery.timeout".

### Modern Default (Spring Boot 3+)

```yaml
spring:
  kafka:
    producer:
      retries: 2147483647        # auto-set
      properties:
        delivery.timeout.ms: 120000   # 2 min total
```

### What's "Retriable"?

| Error | Retried? |
|-------|---------|
| `NotLeaderForPartitionException` | ✅ |
| `NetworkException` | ✅ |
| `LeaderNotAvailableException` | ✅ |
| `RetriableException` (general) | ✅ |
| `InvalidTopicException` | ❌ (config issue) |
| `TopicAuthorizationException` | ❌ (auth issue) |
| `RecordTooLargeException` | ❌ (data issue) |

---

## max.block.ms

```yaml
max.block.ms: 60000          # 60 seconds (default)
```

How long `send()` and `partitionsFor()` block when:
- Buffer is full
- Metadata not available

### Why It Matters

```java
producer.send(record);
// → blocks if buffer full → waits up to max.block.ms
// → throws TimeoutException if exceeded
```

### Tune For

- **Low** (1-5s) — fail fast, propagate backpressure to caller
- **High** (60s default) — tolerate transient buffer pressure

---

## delivery.timeout.ms

```yaml
delivery.timeout.ms: 120000      # 2 minutes (default)
```

**Total time** producer is willing to spend retrying a single record.

```
delivery.timeout.ms ≥ linger.ms + request.timeout.ms
```

### Behavior

```
record sent → retry → retry → retry → ...
              if total time > delivery.timeout.ms: fail with TimeoutException
```

→ Replaces older `request.timeout.ms` semantics for end-to-end deadline.

### Tuning

```yaml
delivery.timeout.ms: 30000   # 30s — fail faster, retry sooner at app level
delivery.timeout.ms: 600000  # 10 min — tolerate broker outages
```

---

## linger.ms & batch.size

**Matlab:** Producer **batches** records before sending (efficiency). These two settings control batching.

### batch.size

```yaml
batch.size: 16384            # 16 KB (default) per partition batch
```

→ Once batch fills up to `batch.size`, sender ships immediately.

### linger.ms

```yaml
linger.ms: 0                 # default — send as fast as possible
```

→ Wait at most `linger.ms` for more records to accumulate, then send.

### Trade-Off

| linger.ms | batch.size | Result |
|-----------|-----------|--------|
| `0` | small | Lowest latency, low throughput |
| `5-50` | 32-128 KB | Balanced |
| `100+` | 256+ KB | Highest throughput, higher latency |

### Tuning Examples

#### Latency-Sensitive (User-Facing)

```yaml
linger.ms: 0
batch.size: 16384
```

#### Throughput-Heavy (Backend Pipeline)

```yaml
linger.ms: 50
batch.size: 65536            # 64 KB
```

### How They Interact

```
linger.ms: 10
batch.size: 16384

Records arrive:
  T=0    record1   batch=200B
  T=2    record2   batch=400B
  T=10   linger triggered → send (batch may be partial)

OR

  T=0    record1
  T=2    batch reaches 16384B → send immediately (don't wait linger)
```

---

## compression.type

```yaml
compression.type: none       # default
```

| Option | Speed | Compression Ratio | Notes |
|--------|-------|--------------------|-------|
| `none` | Fastest | 1:1 | Default |
| `gzip` | Slow | Best | Older / interop |
| `snappy` | Fast | Good | Most popular |
| `lz4` | Faster | Good | Often best balance |
| `zstd` | Medium | Best (better than gzip) | Newer (0.10+ broker) |

### Pros / Cons

✅ Less network bandwidth
✅ Less disk usage
✅ Faster broker-to-consumer transfer
❌ CPU overhead at producer + consumer
❌ Slight latency

### Recommendations

```yaml
compression.type: lz4      # great balance
# or
compression.type: snappy   # very common
# or
compression.type: zstd     # best ratio, slight CPU cost
```

→ Compression happens at **batch level**, so larger batches → better compression.

---

## enable.idempotence (Idempotent Producer)

**Problem:** Without idempotence, retries can cause duplicates.

```
Producer sends record → broker writes → broker ack lost (network)
Producer retries → broker writes AGAIN → 2 copies in topic!
```

### Solution

```yaml
enable.idempotence: true
```

→ Producer attaches **producer ID + sequence number** per record. Broker dedupes.

### Required Configs (Auto-Set When Enabled)

```yaml
acks: all
max.in.flight.requests.per.connection: ≤ 5
retries: > 0
```

Spring Boot 3+ enables idempotence **by default**.

### Guarantees

✅ **Exactly-once write** to a single partition within a single producer session
❌ NOT cross-partition exactly-once
❌ NOT across producer restarts (use transactions for that)

### Cost

Negligible. Recommended for all production producers.

---

## Transactional Producer

**Matlab:** Multiple writes (across topics/partitions) committed **atomically** — all or nothing.

### Use Cases

- "Read from topic A → write to topic B" patterns
- Multi-topic event publication
- Exactly-once semantics across producer restarts

### Setup

```yaml
spring:
  kafka:
    producer:
      transaction-id-prefix: tx-orders-
      properties:
        enable.idempotence: true
```

### Code

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    @Transactional("kafkaTransactionManager")
    public void publish(Order order) {
        kafkaTemplate.send("orders", order.getId().toString(), toJson(order));
        kafkaTemplate.send("audit", order.getId().toString(), "ORDER_CREATED");
        // Both committed atomically; or both aborted on exception
    }
}
```

### Manual API

```java
producer.initTransactions();        // once at startup

producer.beginTransaction();
try {
    producer.send(record1);
    producer.send(record2);
    producer.sendOffsetsToTransaction(offsetsMap, "consumerGroupId");  // for read-process-write
    producer.commitTransaction();
} catch (Exception e) {
    producer.abortTransaction();
}
```

### Consumer Side — `read_committed`

```yaml
spring:
  kafka:
    consumer:
      isolation-level: read_committed
```

→ Only delivered records from committed transactions; aborted records skipped.

### transaction.id Importance

Each producer instance must have **unique** `transactional.id` — survives restarts:
- Broker uses it to recover transaction state
- Two producers with same ID → fence (older one fenced out)

For Spring Boot multi-instance:
```yaml
transaction-id-prefix: tx-orders-${HOSTNAME}-
```

→ Each replica gets unique prefix.

---

## max.in.flight.requests.per.connection

```yaml
max.in.flight.requests.per.connection: 5    # default
```

How many **unacknowledged** requests producer sends per broker connection at once.

### Impact

#### Higher Value (5+)

✅ Better throughput
❌ Risk of out-of-order on retry (without idempotence)

#### Lower Value (1)

✅ Strict order preserved on retries
❌ Lower throughput

### With Idempotence

```yaml
enable.idempotence: true
max.in.flight.requests.per.connection: 5      # safe — broker dedupes + reorders
```

→ Idempotence works up to in-flight=5; if you set higher, idempotence breaks.

### Without Idempotence + Need Order

```yaml
max.in.flight.requests.per.connection: 1
```

→ Strict per-partition order, even on retry. Throughput cost.

---

## Buffer Memory

```yaml
buffer.memory: 33554432       # 32 MB (default)
```

Total memory the producer uses for buffering records waiting to send.

### When Buffer Fills

```
Producer.send(record)
  → buffer full
  → blocks up to max.block.ms
  → throws TimeoutException
```

### Tuning

```yaml
buffer.memory: 67108864      # 64 MB for high throughput
```

→ Combine with appropriate `max.block.ms` for backpressure handling.

---

## Partitioner

### Default — `DefaultPartitioner`

```
key=null:    Sticky partitioner (groups records to same partition for batching, rotates periodically)
key!=null:   hash(key) % numPartitions
```

### Sticky Partitioner (Modern Default)

Old default was round-robin per record (poor batching).
**Sticky** groups all keyless records to same partition for a batch → better compression + throughput.

### Custom Partitioner

```java
public class TenantPartitioner implements Partitioner {
    
    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                         Object value, byte[] valueBytes, Cluster cluster) {
        if (key == null) return 0;     // null → partition 0
        
        String tenantId = ((String) key).split(":")[0];
        int numPartitions = cluster.partitionCountForTopic(topic);
        return Math.abs(tenantId.hashCode()) % numPartitions;
    }
    
    @Override public void close() {}
    @Override public void configure(Map<String, ?> configs) {}
}
```

```yaml
partitioner.class: com.example.TenantPartitioner
```

### Override Per-Send

```java
ProducerRecord<String, String> record = new ProducerRecord<>(
    "orders", 5, key, value);     // explicitly partition 5
```

---

## Producer Tuning Profiles

### Profile 1 — Low Latency / User-Facing

```yaml
producer:
  acks: all
  enable.idempotence: true
  linger.ms: 0
  batch.size: 16384
  compression.type: lz4
  max.in.flight.requests.per.connection: 5
  buffer.memory: 33554432
  delivery.timeout.ms: 30000
  max.block.ms: 5000
```

### Profile 2 — High Throughput / Pipeline

```yaml
producer:
  acks: all
  enable.idempotence: true
  linger.ms: 50
  batch.size: 131072
  compression.type: lz4
  max.in.flight.requests.per.connection: 5
  buffer.memory: 134217728            # 128 MB
  delivery.timeout.ms: 120000
```

### Profile 3 — Best-Effort (Logs/Metrics)

```yaml
producer:
  acks: 1                              # speed > durability
  retries: 0
  linger.ms: 100
  batch.size: 65536
  compression.type: snappy
```

### Profile 4 — Transactional / Exactly-Once

```yaml
producer:
  acks: all
  enable.idempotence: true
  transactional.id: "${HOSTNAME}-tx-orders"
  linger.ms: 5
  batch.size: 32768
  compression.type: lz4
```

---

## Common Pitfalls

### 1. Forgetting `enable.idempotence`

Without it, retries → duplicates. Always enable in production (default in Spring Boot 3+).

### 2. acks=0 in Production

Records lost silently. Almost never appropriate for business data.

### 3. Throwing Away Send Future

```java
producer.send(record);   // ignored future → silent failures
```

→ Always handle with callback / `whenComplete`.

### 4. Blocking the Send Thread

```java
producer.send(record).get();   // sync call in async loop → kills throughput
```

→ Use callbacks. Sync only when truly needed.

### 5. linger.ms=0 + Wondering Why Slow

Each record sent immediately = no batching = max ~1000 records/sec per producer.

→ Set `linger.ms=5-50` for 10-100x throughput.

### 6. compression.type=none

Throughput halved unnecessarily. Snappy/LZ4 free win.

### 7. Buffer Pressure → max.block Timeouts

Producer slow / broker slow → buffer fills → `send()` blocks → 60s timeout.

→ Monitor buffer-utilization, increase `buffer.memory`, or fail fast (lower `max.block.ms`).

### 8. Same transactional.id Across Replicas

Two replicas with same `transactional.id` → one fenced out → confusing failures.

→ Use `${HOSTNAME}` or unique-per-instance suffix.

### 9. Transactional Producer + Long-Running Transaction

Block consumers reading `read_committed` for transaction duration. Keep transactions **short**.

### 10. RecordTooLargeException

```yaml
max.request.size: 1048576    # 1 MB default — Producer side
```

```bash
kafka-configs.sh --alter --topic ... --add-config max.message.bytes=...   # Broker side
```

→ Both must agree. Don't push huge records.

### 11. Custom Partitioner Without Cluster Awareness

Hardcoding partition counts breaks when topics resized.

```java
int numPartitions = cluster.partitionCountForTopic(topic);   // ✅
```

### 12. Ignoring Producer Metrics

Metrics like `record-error-rate`, `request-latency-avg`, `buffer-available-bytes` are gold for tuning.

---

## Summary Cheat Sheet

| Setting | Default | Production Recommendation |
|---------|---------|---------------------------|
| `acks` | `all` (since 3.0) | `all` |
| `enable.idempotence` | `true` (since 3.0) | `true` |
| `retries` | MAX | MAX (with `delivery.timeout.ms`) |
| `delivery.timeout.ms` | 120000 | 30000-120000 |
| `max.block.ms` | 60000 | 5000-60000 |
| `linger.ms` | 0 | 5-50 (throughput) |
| `batch.size` | 16384 | 32768-131072 |
| `compression.type` | `none` | `lz4` or `snappy` |
| `max.in.flight.requests.per.connection` | 5 | 5 |
| `buffer.memory` | 33554432 | 64-128 MB |
| `transactional.id` | none | unique per instance |

| Goal | Settings |
|------|---------|
| **Lowest latency** | linger=0, batch small, lz4, no transactions |
| **Highest throughput** | linger=50, batch large, lz4, big buffer |
| **Strongest durability** | acks=all, idempotence=true, min.insync=2 |
| **Exactly-once** | + transactional.id, isolation_level=read_committed |
| **Best-effort logs** | acks=1, no retries, light compression |

| ✅ Do | ❌ Don't |
|-------|---------|
| `acks=all` for business data | `acks=0` in production |
| Enable idempotence | Disable for "speed" |
| Handle send() future | Fire-and-forget |
| Compress (lz4/snappy) | Leave as none |
| Set unique transactional.id | Same ID across replicas |
| Tune linger/batch for use case | Default settings forever |
| Monitor producer metrics | Hope it's fine |

---

## Practice

1. Compare throughput: `linger.ms=0` vs `linger.ms=50` (run benchmarks).
2. Compare wire size: `compression.type=none` vs `lz4`.
3. Enable idempotence; test retry scenarios for duplicates.
4. Set `acks=0` and observe records lost on broker restart.
5. Set `acks=all` + `min.insync.replicas=2`; kill 1 broker (of 3); verify producer continues.
6. Implement transactional producer; verify atomic multi-topic write.
7. Test `max.block.ms` by setting buffer small and producing fast.
8. Build a custom Partitioner for tenant-based routing.
9. Monitor producer JMX metrics; identify bottleneck.
10. Configure each profile (low latency, high throughput, transactional) and benchmark.

# Kafka Fundamentals

## Status: Not Started

---

## Table of Contents

1. [Kafka Kya Hai?](#kafka-kya-hai)
2. [Why Kafka?](#why-kafka)
3. [Core Concepts](#core-concepts)
4. [Topics & Partitions](#topics--partitions)
5. [Offsets](#offsets)
6. [Producers](#producers)
7. [Consumers & Consumer Groups](#consumers--consumer-groups)
8. [Brokers & Cluster](#brokers--cluster)
9. [ZooKeeper vs KRaft](#zookeeper-vs-kraft)
10. [Replication & ISR](#replication--isr)
11. [Leader Election](#leader-election)
12. [Retention Policy](#retention-policy)
13. [Message Delivery Semantics](#message-delivery-semantics)
14. [Common Pitfalls](#common-pitfalls)
15. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Kafka Kya Hai?

**Matlab:** **Distributed event streaming platform** — high-throughput, fault-tolerant, durable log of records (events), shared between many producers + consumers.

> Originally LinkedIn (2011), open-sourced via Apache Foundation. Now powers most modern event-driven systems.

### Three Main Capabilities

```
1. Publish + Subscribe (like a message queue)
2. Store streams of records durably (like a database)
3. Process streams of records in real-time (Kafka Streams)
```

### Mental Model

```
Append-only commit log:

Producer 1 ──┐
Producer 2 ──┼─▶ [m1][m2][m3][m4][m5][m6][m7][m8] ─▶ Consumer A
Producer 3 ──┘                                  └──▶ Consumer B
                  ↑
            New records appended at end
            Old records readable until retention expires
```

→ Like Git log — append-only, replayable, ordered.

---

## Why Kafka?

### Pre-Kafka Pain

```
Service A ──HTTP──▶ Service B
        ──HTTP──▶ Service C
        ──HTTP──▶ Service D
        ──HTTP──▶ Service E
```

❌ Tight coupling
❌ Synchronous = cascading failures
❌ Hard to add new consumers
❌ No replay if downstream was down
❌ Each consumer needs to be reachable when event happens

### With Kafka

```
Service A ──▶ Kafka topic "orders" ─┬─▶ Service B
                                     ├─▶ Service C
                                     ├─▶ Service D
                                     └─▶ Service E (added later — replays history)
```

✅ Loose coupling
✅ Async — A doesn't wait
✅ New consumers can replay
✅ Buffer during downstream outage
✅ One event, many consumers

### Use Cases

- **Event sourcing** — system of record as event log
- **Real-time analytics** — clickstream, IoT
- **Log aggregation** — collect all app logs
- **Stream processing** — join, aggregate streams
- **Data pipelines** — Kafka Connect → DB / S3
- **CDC** (Change Data Capture) — Debezium
- **Inter-service async** in microservices

### Kafka vs Traditional Message Queues

| Feature | RabbitMQ / ActiveMQ | Kafka |
|---------|---------------------|-------|
| **Model** | Push (broker pushes to consumer) | Pull (consumer polls) |
| **Storage** | Until acknowledged (then deleted) | Durable, retention-based |
| **Replay** | Hard — message gone | Native — read from any offset |
| **Throughput** | 10K-100K msg/sec | 1M+ msg/sec per broker |
| **Ordering** | Per queue | Per partition |
| **Multiple consumers** | Need fanout exchange | Native (consumer groups) |
| **Best for** | Task queue, RPC | Event streaming, log |

→ Cross-ref: `Microservices Architecture/08-Event-Driven-Architecture.md`.

---

## Core Concepts

### The Vocabulary

```
Cluster
├── Broker (Kafka server)
├── Broker
└── Broker
        │
        ├── Topic "orders"
        │     ├── Partition 0  →  [m1][m2][m3]...
        │     ├── Partition 1  →  [m1][m2][m3]...
        │     └── Partition 2  →  [m1][m2][m3]...
        │
        └── Topic "payments"
              ├── Partition 0
              └── Partition 1
```

| Term | Meaning |
|------|---------|
| **Cluster** | Group of brokers |
| **Broker** | Single Kafka server (process + storage) |
| **Topic** | Logical category of messages (e.g., `orders`) |
| **Partition** | Ordered, immutable sequence of records within a topic |
| **Offset** | Sequential ID of a record within a partition |
| **Producer** | Writes records to topics |
| **Consumer** | Reads records from topics |
| **Consumer Group** | Set of consumers cooperating to consume a topic |
| **Replication** | Each partition has copies on multiple brokers |
| **Leader** | The broker handling reads/writes for a partition |
| **Follower** | Replicas keeping in-sync with leader |
| **ISR** | In-Sync Replicas — followers caught up with leader |

---

## Topics & Partitions

### Topic

**Matlab:** A category / feed name. E.g., `orders`, `user-signups`, `payment-events`.

```bash
# Create topic
kafka-topics.sh --bootstrap-server kafka:9092 --create \
  --topic orders \
  --partitions 6 \
  --replication-factor 3
```

### Partition

A topic is split into **N partitions**. Each partition is an **append-only log** stored on disk.

```
Topic "orders" — 3 partitions

Partition 0:  [m0][m1][m2][m3][m4]...  ← order matters within partition
Partition 1:  [m0][m1][m2][m3][m4]...  ← parallel
Partition 2:  [m0][m1][m2][m3][m4]...  ← parallel
```

### Why Partitions?

1. **Parallelism** — multiple consumers in a group read different partitions
2. **Throughput** — distributes load across brokers
3. **Scalability** — add more brokers, redistribute partitions

### How Are Records Distributed?

```
Producer sends record (key=K, value=V)

If key == null:
  → Round-robin across partitions (default StickyPartitioner)
  → No ordering across partitions

If key != null:
  → partition = hash(key) % numPartitions
  → Same key → same partition → ordering guaranteed for that key
```

#### Example

```java
// All orders for user-123 → same partition → ordered
producer.send(new ProducerRecord<>("orders", "user-123", orderJson1));
producer.send(new ProducerRecord<>("orders", "user-123", orderJson2));
// → both end up in same partition, in order

producer.send(new ProducerRecord<>("orders", "user-456", orderJson));
// → may go to different partition
```

### Choosing Number of Partitions

```
Throughput target / single-partition-throughput
≈ how many partitions
```

Trade-offs:
- **More partitions** → better parallelism, higher overhead
- **Less partitions** → less overhead, lower max throughput
- **Hard to decrease** later (only increase)

Common: **start with 6-12** partitions for new topics; tune later.

### Partition Storage on Disk

```
/kafka-logs/orders-0/
├── 00000000000000000000.log        ← segment file (1GB default)
├── 00000000000000000000.index      ← offset index
├── 00000000000000123456.log        ← next segment
├── 00000000000000123456.index
└── leader-epoch-checkpoint
```

→ Old segments deleted on retention or compaction.

---

## Offsets

**Matlab:** Sequential ID of each record in a partition. Monotonically increasing.

```
Partition 0:  [m0][m1][m2][m3][m4][m5]
Offsets:        0   1   2   3   4   5
```

### Producer Offset

When producer sends record, broker assigns next offset:

```
producer.send(record).get();    // returns RecordMetadata with assigned offset
```

### Consumer Offset

Consumer **tracks** its position per partition:

```
Consumer X has read up to offset 3 in Partition 0.
Next poll → starts from offset 4.
```

→ Stored in `__consumer_offsets` (internal Kafka topic) by default.

### Reset Strategies

```
auto.offset.reset:
  earliest    → start from offset 0 (read all history)
  latest      → start from end (only new messages)
  none        → fail if no committed offset
```

(Deep dive: `05-Consumer-Configuration.md`.)

### Replay Capability

Consumer can **seek** to any offset:

```java
consumer.seek(new TopicPartition("orders", 0), 100);
consumer.seekToBeginning(...);
consumer.seekToEnd(...);
```

→ Powerful for reprocessing, debugging, audits.

---

## Producers

**Matlab:** Application that writes records to Kafka.

### Basic Producer Flow

```
1. Producer creates ProducerRecord(topic, key, value)
2. Serializer converts key + value to bytes
3. Partitioner assigns to partition (based on key or round-robin)
4. Record buffered in memory (batched)
5. Sender thread sends batches to broker (leader of that partition)
6. Broker writes to log + replicates to followers
7. ack returned to producer
```

### Java Producer Example

```java
Properties props = new Properties();
props.put("bootstrap.servers", "kafka:9092");
props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
props.put("acks", "all");

try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
    
    ProducerRecord<String, String> record = new ProducerRecord<>(
        "orders", "user-123", "{\"orderId\":1,\"total\":500}");
    
    // Async with callback
    producer.send(record, (metadata, exception) -> {
        if (exception != null) {
            log.error("Send failed", exception);
        } else {
            log.info("Sent to {}-{} @ offset {}",
                metadata.topic(), metadata.partition(), metadata.offset());
        }
    });
    
    producer.flush();    // ensure pending sends complete
}
```

### Async vs Sync

```java
// Async (recommended for throughput)
producer.send(record, callback);

// Sync (slower; useful for ordered/critical writes)
RecordMetadata md = producer.send(record).get();
```

(Deep dive: `04-Producer-Configuration.md`.)

---

## Consumers & Consumer Groups

**Matlab:** Application that reads records from topics. Multiple consumers cooperate via **consumer groups**.

### Consumer Group Mechanics

```
Topic "orders" — 6 partitions [P0..P5]

Consumer Group "order-processor":
  Consumer A → P0, P1
  Consumer B → P2, P3
  Consumer C → P4, P5

Each partition consumed by EXACTLY ONE consumer in the group.
```

### Add a Consumer to the Group

```
Consumer D joins → rebalance
  Consumer A → P0
  Consumer B → P1, P2
  Consumer C → P3, P4
  Consumer D → P5
```

### Multiple Consumer Groups

```
Topic "orders" → Group "order-processor" → Service A (analytics)
              → Group "order-notifier"    → Service B (email)
              → Group "order-fulfillment"  → Service C (warehouse)
```

→ Each group reads independently. Same record delivered to **each group**.

### Java Consumer Example

```java
Properties props = new Properties();
props.put("bootstrap.servers", "kafka:9092");
props.put("group.id", "order-processor");
props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
props.put("auto.offset.reset", "earliest");

try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
    consumer.subscribe(List.of("orders"));
    
    while (true) {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
        
        for (ConsumerRecord<String, String> record : records) {
            log.info("Got: {}-{} @ {} → key={}, value={}",
                record.topic(), record.partition(), record.offset(),
                record.key(), record.value());
            
            process(record);
        }
        
        consumer.commitSync();
    }
}
```

### Rebalancing

When consumers join/leave the group, Kafka **rebalances** partition assignments:

```
Triggers:
- New consumer joins
- Consumer leaves (graceful or crash)
- Partition added to topic
- Subscription change
```

⚠️ **During rebalance**, no consumption — pause. Long rebalances = lag.

→ See **cooperative rebalancing** (`incremental.cooperative.rebalance`) in newer Kafka versions for less disruptive rebalances.

---

## Brokers & Cluster

### Broker

A single Kafka server. Each broker has:
- **Unique ID** (`broker.id`)
- **Storage** (where partition logs live)
- **Network** (port for clients + inter-broker)

```yaml
# server.properties
broker.id=1
listeners=PLAINTEXT://broker1:9092
log.dirs=/var/lib/kafka/data
```

### Cluster

Multiple brokers form a cluster. Topics' partitions are distributed across brokers.

```
Cluster:
├── Broker 1 (leader of P0, P3; follower of P1, P2)
├── Broker 2 (leader of P1, P4; follower of P0, P3)
├── Broker 3 (leader of P2, P5; follower of P4, P5)
```

### Bootstrap Servers

Producers/consumers connect to **any** broker (bootstrap):

```
bootstrap.servers=broker1:9092,broker2:9092,broker3:9092
```

→ Client gets full cluster metadata, then talks directly to relevant brokers.

### Controller

One broker acts as **controller** — coordinates cluster:
- Tracks broker liveness
- Handles partition reassignments
- Triggers leader elections

In **KRaft**: controller is a quorum (multiple controllers).

### Listeners

Internal vs external:

```yaml
listeners=INTERNAL://0.0.0.0:9092,EXTERNAL://0.0.0.0:9094
advertised.listeners=INTERNAL://kafka:9092,EXTERNAL://public.example.com:9094
listener.security.protocol.map=INTERNAL:PLAINTEXT,EXTERNAL:SSL
inter.broker.listener.name=INTERNAL
```

→ Important: clients must reach `advertised.listeners` URLs.

---

## ZooKeeper vs KRaft

### Old Way — ZooKeeper

Until ~2022, Kafka **required ZooKeeper** for:
- Cluster metadata (topics, partitions, configs)
- Controller election
- ACL storage

```
                ┌──────────────────┐
                │  ZooKeeper       │
                │  (3-5 nodes)     │
                └────────┬─────────┘
                         │
    ┌────────────────────┼────────────────────┐
    │                    │                    │
┌───▼────┐         ┌─────▼─────┐        ┌────▼─────┐
│Broker 1│         │ Broker 2  │        │ Broker 3 │
└────────┘         └───────────┘        └──────────┘
```

❌ Operational burden — manage two clusters
❌ ZK is bottleneck for metadata operations
❌ Limits Kafka scalability (~200K partitions max)

### New Way — KRaft (Kafka Raft)

Kafka 2.8+ introduced **KRaft mode** — Kafka manages own metadata via Raft consensus. ZooKeeper-free.

Default in Kafka 3.5+; **fully removed** ZK in Kafka 4.0.

```
┌─────────────────────────────────────────┐
│           Kafka Cluster (KRaft)          │
│                                          │
│  ┌────────┐  ┌────────┐  ┌────────┐     │
│  │Broker 1│  │Broker 2│  │Broker 3│     │
│  │+Ctrl   │  │+Ctrl   │  │+Ctrl   │     │
│  └────────┘  └────────┘  └────────┘     │
└─────────────────────────────────────────┘
```

✅ Single system to operate
✅ Faster metadata ops
✅ Scales to millions of partitions
✅ Faster recovery / leader election

### Migration

For existing ZK-based clusters → migration tools available; new clusters should start KRaft.

```bash
# KRaft setup — generate cluster ID
KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/kraft/server.properties
bin/kafka-server-start.sh config/kraft/server.properties
```

```yaml
# server.properties (KRaft mode)
process.roles=broker,controller
node.id=1
controller.quorum.voters=1@broker1:9093,2@broker2:9093,3@broker3:9093
listeners=PLAINTEXT://:9092,CONTROLLER://:9093
controller.listener.names=CONTROLLER
```

---

## Replication & ISR

**Matlab:** Each partition has multiple copies (replicas) on different brokers — protect against broker failure.

### Replication Factor

```bash
kafka-topics.sh --create --topic orders \
  --partitions 6 \
  --replication-factor 3   # 3 copies of each partition
```

→ Each partition has 1 leader + 2 followers across different brokers.

### How It Works

```
Partition 0:
  Broker 1 — LEADER (handles reads + writes)
  Broker 2 — FOLLOWER (replicates from leader)
  Broker 3 — FOLLOWER (replicates from leader)
```

```
Producer write:
  → Leader (Broker 1) writes to log
  → Followers fetch from leader, write to their logs
  → Once enough replicas ACK → producer notified (depends on `acks`)
```

### ISR — In-Sync Replicas

ISR = followers **caught up** with leader (within `replica.lag.time.max.ms`, default 30s).

```
Partition 0 leader: Broker 1
Followers: Broker 2 (in-sync), Broker 3 (in-sync, but lagging)

If lag > 30s → Broker 3 kicked out of ISR
```

### `min.insync.replicas`

Minimum replicas that must acknowledge for a successful write:

```bash
kafka-topics.sh --alter --topic orders \
  --config min.insync.replicas=2
```

→ With `acks=all` + `min.insync.replicas=2` + replication factor 3:
- Need 2 of 3 in ISR for write to succeed
- Tolerates 1 broker failure
- Stronger durability guarantees

### Trade-Offs

```
replication-factor=1, min.insync=1:    fast, no durability
replication-factor=3, min.insync=2:    balanced (production default)
replication-factor=3, min.insync=3:    strongest, but can't tolerate any loss
```

### Unclean Leader Election

```
unclean.leader.election.enable=false   (default since 0.11)
```

If true: out-of-sync follower can become leader → **data loss possible**, but availability better.

→ Default `false` is safer; only enable if availability > durability.

---

## Leader Election

When leader broker fails, a follower (in ISR) becomes new leader.

### Process

```
1. Controller detects broker is down (timeout)
2. For each partition where down broker was leader:
   a. Pick first replica in ISR (excluding failed)
   b. Promote it to leader
   c. Update metadata
   d. Notify all clients (refresh metadata)
3. Producers / consumers re-route to new leader
```

### Recovery Time

- Old ZK-based: ~10-30s for many partitions
- KRaft: dramatically faster (~ms-seconds)

### Preferred Replica

Each partition has **preferred replica** (first in replica list) — original leader.

```
auto.leader.rebalance.enable=true   (default)
leader.imbalance.check.interval.seconds=300
```

→ Periodically rebalance to preferred leaders to spread load evenly.

---

## Retention Policy

How long Kafka keeps records.

### Time-Based

```yaml
retention.ms=604800000         # 7 days (default)
retention.bytes=-1              # no size limit (or specific value)
```

### Size-Based

```yaml
retention.bytes=10737418240     # 10 GB per partition
retention.ms=-1                  # no time limit
```

→ Hits whichever first → segments deleted.

### Per-Topic Override

```bash
kafka-configs.sh --alter --topic orders --add-config retention.ms=86400000
# orders kept only 1 day
```

### Log Compaction (Different Mode)

Instead of deleting old, compaction keeps **latest value per key**:

```yaml
cleanup.policy=compact
```

```
Before:
  key=u1, value=v1@offset0
  key=u2, value=v2@offset1
  key=u1, value=v3@offset2
  key=u3, value=v4@offset3

After compaction:
  key=u2, value=v2@offset1
  key=u1, value=v3@offset2
  key=u3, value=v4@offset3
```

→ Used for event sourcing, KTables, configuration topics.

```yaml
cleanup.policy=compact,delete   # both: compact + time-bound
```

(Deep dive: `06-Kafka-Patterns.md`.)

### Segment Files

Logs split into **segments** (files, ~1GB default). Retention deletes whole segments.

```yaml
segment.bytes=1073741824        # 1GB
segment.ms=604800000             # rotate every 7 days
```

---

## Message Delivery Semantics

### At Most Once

```
Producer: fire and forget (no retry)
Consumer: commit before processing
```

→ Records may be **lost**. Never duplicated.

### At Least Once (Default)

```
Producer: retry on failure
Consumer: commit after processing
```

→ Records may be **duplicated** (retry after partial success). Never lost.

### Exactly Once

Kafka 0.11+ supports exactly-once via:
- **Idempotent producer** (`enable.idempotence=true`) — no duplicates from retries
- **Transactions** — all-or-nothing across multiple writes
- **`isolation.level=read_committed`** consumer config

```java
producer.initTransactions();
producer.beginTransaction();
producer.send(record1);
producer.send(record2);
producer.commitTransaction();    // or abortTransaction()
```

→ Producer + consumer must cooperate. (Deep dive: `04-Producer-Configuration.md`.)

⚠️ EOS scope: within Kafka. External side effects (DB writes, HTTP) need additional patterns (transactional outbox).

---

## Common Pitfalls

### 1. Wrong Number of Partitions

Too few: throughput bottleneck.
Too many: more overhead, higher recovery times.

→ Start with thoughtful number; can **only increase** later (rebalancing pain).

### 2. Key Skew

If 90% of records have same key → 90% to one partition → that consumer overloaded.

→ Choose keys with good distribution. Or use `null` key for even round-robin.

### 3. Ordering Across Partitions

```
"Kafka guarantees order!"
→ Per partition, yes. Across partitions, NO.
```

For ordering, use partitioning key (e.g., `userId`).

### 4. Forgetting Replication

`replication-factor=1` → broker failure = data loss + topic unavailable.

→ Always 3 in production.

### 5. Auto-Commit Surprises

Default consumer auto-commits every 5s. If processing fails after commit → records skipped.

→ Manual commit after processing for at-least-once.

(Deep dive: `05-Consumer-Configuration.md`.)

### 6. Big Messages

Kafka not designed for huge payloads (>1MB). Increases broker memory, GC pressure.

→ Default `message.max.bytes=1MB`. For large blobs, store in S3 + send pointer.

### 7. Long Processing → Rebalance Storm

If consumer takes > `max.poll.interval.ms` (default 5min) to process a batch → kicked out → rebalance → next consumer might also fail → cycle.

→ Tune `max.poll.records` low, or process async.

### 8. Topic Deletion Disabled

```
delete.topic.enable=true   (default since 1.0+)
```

Older versions: topic deletion was disabled by default. Verify config.

### 9. Insufficient Disk

Retention defaults: 7 days. If volume = 500GB/day, retention=7d → ~3.5TB needed per replica.

→ Capacity planning critical.

### 10. Different Kafka Versions

Producer/consumer/broker version compatibility matrix matters. Generally clients newer than broker is okay (within reason).

---

## Summary Cheat Sheet

| Concept | Quick Note |
|---------|-----------|
| **Topic** | Named stream of records |
| **Partition** | Ordered, parallel sub-stream within topic |
| **Offset** | Position within a partition |
| **Producer** | Writes records |
| **Consumer** | Reads records |
| **Consumer Group** | Cooperating consumers, each partition → one consumer |
| **Broker** | Single Kafka server |
| **Cluster** | Group of brokers |
| **Replication factor** | # copies per partition |
| **ISR** | Followers caught up with leader |
| **Leader** | Handles reads/writes for partition |
| **`acks`** | 0 (no), 1 (leader), all (all ISR) |
| **`min.insync.replicas`** | Minimum replicas for write success |
| **Retention** | Time + size based |
| **Compaction** | Keep latest per key |
| **KRaft** | New metadata mode (no ZooKeeper) |

| Setup | Production Default |
|-------|--------------------|
| Brokers | 3+ (HA) |
| Replication | 3 |
| min.insync.replicas | 2 |
| acks | all |
| Partitions | Start 6-12, tune |
| Retention | 7 days |
| Disk | SSD recommended |

| Delivery | Achieved by |
|----------|------------|
| At most once | No retry |
| At least once | Retry + commit after process |
| Exactly once | Idempotent producer + transactions |

---

## Practice

1. Set up Kafka in KRaft mode locally (Docker Compose).
2. Create topic `orders` with 3 partitions, replication 1.
3. Use console producer/consumer to produce + consume.
4. Send records with same key — verify all in same partition.
5. Add a second consumer in same group — observe partition reassignment.
6. Add another consumer group — see same records consumed independently.
7. Set replication=3 (3-broker cluster) — kill one broker, verify availability.
8. Configure log compaction on a topic — verify only latest value per key persists.
9. Use `kafka-consumer-groups.sh` to inspect consumer lag.
10. Practice `seek()` — replay from offset 0 to debug.

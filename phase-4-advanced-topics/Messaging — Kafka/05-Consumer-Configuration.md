# Kafka Consumer Configuration

## Status: Not Started

---

## Table of Contents

1. [Consumer Lifecycle](#consumer-lifecycle)
2. [auto.offset.reset](#autoofsetreset)
3. [enable.auto.commit](#enableautocommit)
4. [Manual Commit (SYNC vs ASYNC)](#manual-commit-sync-vs-async)
5. [max.poll.records](#maxpollrecords)
6. [max.poll.interval.ms](#maxpollintervalms)
7. [session.timeout.ms & heartbeat.interval.ms](#sessiontimeoutms--heartbeatintervalms)
8. [fetch.min.bytes & fetch.max.wait.ms](#fetchminbytes--fetchmaxwaitms)
9. [isolation.level (read_committed)](#isolationlevel-readcommitted)
10. [Consumer Group & Rebalance](#consumer-group--rebalance)
11. [Cooperative Rebalancing](#cooperative-rebalancing)
12. [Static Membership](#static-membership)
13. [Partition Assignment Strategy](#partition-assignment-strategy)
14. [Consumer Tuning Profiles](#consumer-tuning-profiles)
15. [Common Pitfalls](#common-pitfalls)
16. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Consumer Lifecycle

```
Consumer.subscribe(topics)
  ↓
poll(timeout) returns ConsumerRecords
  ↓
process records
  ↓
commit offsets
  ↓
poll() again
  ↓
... loop ...
  ↓
close() (graceful)
```

### Group Coordinator

Each consumer group has a **coordinator broker** that:
- Tracks membership
- Manages rebalances
- Stores committed offsets (in `__consumer_offsets`)

### Heartbeats

Consumer sends heartbeats to coordinator every `heartbeat.interval.ms` to indicate it's alive.

### What Triggers Rebalance?

- Consumer joins/leaves group
- New partition added
- Subscription changes
- Consumer didn't poll within `max.poll.interval.ms` → kicked out

---

## auto.offset.reset

**Matlab:** What to do if **no committed offset** exists for the consumer group on a partition.

```yaml
spring:
  kafka:
    consumer:
      auto-offset-reset: earliest      # or latest, none
```

| Value | Behavior |
|-------|----------|
| `earliest` | Start from beginning of partition (replay history) |
| `latest` | Start from end (only new messages) — **default** |
| `none` | Throw exception if no committed offset |

### Use Cases

#### `earliest`

- New consumer group joining production data → wants full history
- Audit / analytics jobs
- Disaster recovery (re-process all)

#### `latest`

- Real-time alerts — don't care about old data
- Production new consumer group → only new events

#### `none`

- Strict — explicit control. Want failure if state lost.

### Effective Only First Time

```
Day 1: New group, no committed offsets → applies auto.offset.reset
Day 2: Group has committed offsets → reads from there (auto.offset.reset ignored)
```

Want to **re-read from beginning**? Reset offsets explicitly:

```bash
kafka-consumer-groups.sh --bootstrap-server kafka:9092 \
  --group order-processor \
  --topic orders \
  --reset-offsets --to-earliest \
  --execute
```

---

## enable.auto.commit

**Matlab:** Should consumer auto-commit offsets in background?

```yaml
spring:
  kafka:
    consumer:
      enable-auto-commit: false        # Spring default
      auto-commit-interval: 5000        # ms
```

### `enable.auto.commit=true`

Background thread commits every `auto.commit.interval.ms` (default 5s).

```
poll() → records → process → background commits offsets
```

❌ **Risk:** if processing fails after commit → records skipped (lost).
❌ Hard to reason about "did this record actually process?"

### `enable.auto.commit=false` (Recommended)

App **manually** commits after processing succeeds.

→ Spring Kafka sets this to `false` automatically when using `@KafkaListener` (uses container `AckMode`).

### When `true` Acceptable

- Append-only metric/log forwarding
- Where occasional loss is acceptable
- Never for business-critical events

---

## Manual Commit (SYNC vs ASYNC)

### Native Client

```java
KafkaConsumer<String, String> consumer = ...;
consumer.subscribe(List.of("orders"));

while (running) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
    
    for (ConsumerRecord<String, String> record : records) {
        process(record);
    }
    
    consumer.commitSync();      // OR
    consumer.commitAsync();
}
```

### `commitSync()`

```java
consumer.commitSync();
```

✅ Blocks until committed
✅ Auto-retries on retriable errors
❌ Slower (each commit = round-trip)

### `commitAsync()`

```java
consumer.commitAsync((offsets, ex) -> {
    if (ex != null) log.error("Commit failed", ex);
});
```

✅ Faster (non-blocking)
❌ Doesn't auto-retry
❌ Out-of-order commits possible (but generally fine)

### Common Pattern — Mix

```java
try {
    while (true) {
        records = consumer.poll(...);
        process(records);
        consumer.commitAsync();          // fast in normal flow
    }
} catch (Exception e) {
    log.error("Consumer error", e);
} finally {
    try {
        consumer.commitSync();           // sync on shutdown
    } finally {
        consumer.close();
    }
}
```

### Per-Record / Per-Partition Commit

```java
Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
for (ConsumerRecord<String, String> record : records) {
    process(record);
    offsets.put(new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1));
}
consumer.commitSync(offsets);
```

→ Commit **only** offsets for partitions/records that succeeded.

### In Spring Kafka

Manual ack is via `Acknowledgment.acknowledge()`:

```java
@KafkaListener(topics = "orders")
public void handle(ConsumerRecord<String, String> record, Acknowledgment ack) {
    process(record);
    ack.acknowledge();      // Spring handles sync/async based on AckMode
}
```

→ See `02-Spring-Kafka.md` for ack modes.

---

## max.poll.records

```yaml
max.poll.records: 500            # default
```

Max records returned in single `poll()` call.

### Trade-Offs

| Higher | Lower |
|--------|-------|
| Fewer round-trips | More frequent polls |
| Bigger batches | Smaller commit units |
| Risk of long processing → rebalance | Less risk of timeout |

### Common Settings

```yaml
# Lightweight processing
max.poll.records: 500

# Heavy processing per record (DB writes, ML inference)
max.poll.records: 10

# Bulk pipelines (Kafka Streams, batch ETL)
max.poll.records: 5000
```

### Relationship with max.poll.interval.ms

```
Need to process all returned records within max.poll.interval.ms.
Otherwise: kicked out of group → rebalance.

Processing time per record = X ms
max.poll.records × X ≤ max.poll.interval.ms

Adjust max.poll.records OR max.poll.interval.ms accordingly.
```

---

## max.poll.interval.ms

```yaml
max.poll.interval.ms: 300000     # 5 minutes (default)
```

Max time between successive `poll()` calls before consumer considered dead.

### Failure Mode

```
poll() → records → very slow processing (e.g., 6 min)
     ↓
Coordinator: "Consumer hasn't polled in 5 min → kick out"
     ↓
Rebalance triggers
     ↓
Records re-delivered to other consumer
```

→ Slow consumer → infinite rebalance loop.

### Solutions

#### A. Reduce Batch Size

```yaml
max.poll.records: 50           # less work per poll
```

#### B. Increase Interval

```yaml
max.poll.interval.ms: 600000   # 10 min
```

⚠️ Higher interval = slower failure detection.

#### C. Async Processing

```java
@KafkaListener(topics = "orders")
public void handle(ConsumerRecord<String, String> record, Acknowledgment ack) {
    executor.submit(() -> {
        process(record);
        ack.acknowledge();
    });
    // Returns quickly → poll() called soon
}
```

⚠️ Now ordering / commits get tricky. Be careful.

---

## session.timeout.ms & heartbeat.interval.ms

### session.timeout.ms

```yaml
session.timeout.ms: 45000        # 45 seconds (default)
```

Max time consumer can be **silent** (no heartbeat) before considered dead.

### heartbeat.interval.ms

```yaml
heartbeat.interval.ms: 3000      # 3 seconds (default)
```

How often consumer sends heartbeat to coordinator.

### Relationship

```
heartbeat.interval.ms ≤ session.timeout.ms / 3
```

Common: heartbeat = 3s, session.timeout = 10-45s.

### Quick Failure Detection

```yaml
session.timeout.ms: 10000        # 10s
heartbeat.interval.ms: 3000      # 3s
```

→ Failed consumer detected within ~10s.

### Tolerant of Network Glitches

```yaml
session.timeout.ms: 60000        # 1 min
heartbeat.interval.ms: 10000     # 10s
```

→ Slower failover but more resilient.

### Heartbeat ≠ poll()

Heartbeat thread is **separate** — sends regularly even if app is processing.
But `max.poll.interval.ms` separately checks if app is calling `poll()`.

→ Heartbeats keep "session alive"; polls keep "processing alive".

---

## fetch.min.bytes & fetch.max.wait.ms

### fetch.min.bytes

```yaml
fetch.min.bytes: 1               # default
```

Broker waits to accumulate at least this many bytes before responding to consumer's fetch.

### fetch.max.wait.ms

```yaml
fetch.max.wait.ms: 500           # default
```

Max time broker waits for `fetch.min.bytes` before responding (even if less data).

### Trade-Offs

#### Default — Latency First

```yaml
fetch.min.bytes: 1
fetch.max.wait.ms: 500
```

→ Broker responds immediately when any data available.

#### Throughput First

```yaml
fetch.min.bytes: 65536           # 64 KB
fetch.max.wait.ms: 5000           # 5s
```

→ Broker waits for batch → fewer fetch round-trips → higher throughput.

### When to Tune

- High-volume topics: increase `fetch.min.bytes` to reduce broker load
- Real-time needs: keep defaults

---

## isolation.level (read_committed)

```yaml
isolation.level: read_uncommitted    # default
```

| Value | Behavior |
|-------|----------|
| `read_uncommitted` | See ALL records (incl. aborted transactions) |
| `read_committed` | See only **committed** transactional records |

### When `read_committed` Matters

If producers use **transactions**:

```
Producer transaction:
  send(record A)
  send(record B)
  abortTransaction()      ← A and B should be invisible
```

- `read_uncommitted`: consumer sees A and B (rollback ignored)
- `read_committed`: consumer skips A and B

### Cost

Slightly higher latency — broker buffers records until transaction commits/aborts.

### Use With

```yaml
# Producer
transactional.id: "tx-..."

# Consumer
isolation.level: read_committed
```

→ Together = exactly-once semantics across read-process-write.

---

## Consumer Group & Rebalance

### Joining the Group

```
Consumer1 starts → joins group "order-processor"
Coordinator: "Welcome. You get partitions [0, 1, 2]."
```

### Adding More Consumers

```
Consumer2 joins → coordinator triggers rebalance
After: Consumer1 = [0, 1], Consumer2 = [2]
```

### Consumer Leaves

```
Consumer1 closes gracefully → coordinator rebalances
After: Consumer2 = [0, 1, 2]
```

### Crash (No Graceful Leave)

```
Consumer1 stops sending heartbeats → after session.timeout.ms → coordinator rebalances
```

→ Slower than graceful leave.

### Stop-the-World Rebalance (Old Default)

```
Rebalance starts:
  All consumers stop processing
  All partitions revoked
  New assignment computed
  Partitions reassigned
  Processing resumes
```

⚠️ Disruption proportional to group size + partition count.

---

## Cooperative Rebalancing

**Matlab:** Newer rebalance protocol (Kafka 2.4+) that **doesn't stop everyone** — only affected partitions move.

### Configure

```yaml
partition.assignment.strategy:
  - org.apache.kafka.clients.consumer.CooperativeStickyAssignor
```

### How

```
Old: revoke ALL → reassign ALL
New: revoke only what changes → others keep processing
```

→ Smaller "stop-the-world" window. Highly recommended for large groups.

### Combine with `StickyAssignor` Strategy

`CooperativeStickyAssignor` keeps prior assignments **stable** + cooperative.

---

## Static Membership

**Matlab:** Consumer instance has stable identity across restarts → no rebalance on restart.

```yaml
group.instance.id: order-consumer-1   # unique per instance
session.timeout.ms: 60000              # higher to tolerate restart window
```

### Benefit

```
Without static: restart → rebalance → lag spike
With static: restart within session.timeout → resume same partitions, no rebalance
```

### Use Case

- StatefulSet pods (K8s) where each replica has stable hostname
- Reduces rebalance during deploys

### Caution

If a static member crashes for real → coordinator waits `session.timeout.ms` → no failover during that window. Tune accordingly.

---

## Partition Assignment Strategy

### RangeAssignor (Default for Older Versions)

Distributes contiguous ranges per topic.

```
Topic A: 6 partitions, 2 consumers
  C1 = [0, 1, 2]
  C2 = [3, 4, 5]
```

❌ Imbalanced if multiple topics with different sizes.

### RoundRobinAssignor

Distributes partitions round-robin across all subscribed topics.

```
Topics A (3 parts) + B (3 parts), 2 consumers
  C1 = A0, A2, B1
  C2 = A1, B0, B2
```

✅ Better balance.

### StickyAssignor

Tries to keep prior assignments + minimize movement on rebalance.

### CooperativeStickyAssignor (Recommended Modern)

Sticky + cooperative rebalancing.

```yaml
partition.assignment.strategy: org.apache.kafka.clients.consumer.CooperativeStickyAssignor
```

### Custom

Implement `PartitionAssignor` for special needs.

---

## Consumer Tuning Profiles

### Profile 1 — Real-Time User-Facing

```yaml
consumer:
  group-id: live-orders
  auto-offset-reset: latest
  enable-auto-commit: false
  
  properties:
    max.poll.records: 50
    max.poll.interval.ms: 60000
    session.timeout.ms: 15000
    heartbeat.interval.ms: 3000
    fetch.min.bytes: 1
    fetch.max.wait.ms: 100
    partition.assignment.strategy: org.apache.kafka.clients.consumer.CooperativeStickyAssignor
```

### Profile 2 — Throughput Pipeline

```yaml
consumer:
  group-id: bulk-processor
  auto-offset-reset: earliest
  enable-auto-commit: false
  
  properties:
    max.poll.records: 1000
    max.poll.interval.ms: 600000
    session.timeout.ms: 60000
    fetch.min.bytes: 65536           # 64 KB
    fetch.max.wait.ms: 5000
    partition.assignment.strategy: org.apache.kafka.clients.consumer.CooperativeStickyAssignor
```

### Profile 3 — Strict Order, Slow Processing

```yaml
consumer:
  group-id: order-fulfillment
  auto-offset-reset: earliest
  enable-auto-commit: false
  isolation-level: read_committed
  
  properties:
    max.poll.records: 10
    max.poll.interval.ms: 600000     # tolerate slow processing
    session.timeout.ms: 30000
    partition.assignment.strategy: org.apache.kafka.clients.consumer.CooperativeStickyAssignor
```

### Profile 4 — Static Membership (K8s StatefulSet)

```yaml
consumer:
  group-id: payment-processor
  group.instance.id: ${HOSTNAME}     # e.g., payment-processor-0
  
  properties:
    max.poll.records: 100
    session.timeout.ms: 60000        # tolerate brief restart
    partition.assignment.strategy: org.apache.kafka.clients.consumer.CooperativeStickyAssignor
```

---

## Common Pitfalls

### 1. Auto-Commit + Slow Processing

Records committed before processing complete → if processor crashes → records lost.

→ Disable auto-commit; ack after processing.

### 2. Long Processing Triggers Rebalance

`max.poll.interval.ms` exceeded → kicked out → rebalance → next consumer also slow → loop.

→ Lower `max.poll.records` or process async.

### 3. session.timeout > rebalance.timeout (Broker Side)

Broker config `group.max.session.timeout.ms` (default 30 min) limits client. Make sure consumer ≤ broker.

### 4. Forgetting `consumer.close()`

Without graceful close → coordinator waits `session.timeout.ms` → unnecessary rebalance.

→ Use try-with-resources or shutdown hook:

```java
Runtime.getRuntime().addShutdownHook(new Thread(consumer::close));
```

### 5. heartbeat.interval > session.timeout

Bad math:

```yaml
heartbeat.interval.ms: 60000     # 60s
session.timeout.ms: 30000        # 30s
```

→ Heartbeat misses → kicked out before sending.

→ Always `heartbeat ≤ session.timeout / 3`.

### 6. Using Same group.id for Different Apps

```
App A: group-id=processor
App B: group-id=processor (different code, different topic-list)
```

→ Conflict — they steal partitions from each other.

→ Unique `group-id` per logical consumer role.

### 7. Concurrency in Consumer Application

Native consumer is **NOT thread-safe**. One thread per consumer instance.

(Spring Kafka handles this for you when you use `concurrency` parameter — creates multiple consumer instances.)

### 8. Skipping Poison Messages Wrong

```java
catch (Exception e) {
    log.error("Failed", e);
    // ❌ no commit → infinite retry
}
```

→ Set up DLT and ack after sending to DLT.

### 9. `auto.offset.reset=latest` for New Group → "Where's my data?"

Topic has 1M records, you start a fresh consumer → only sees new ones.

→ Use `earliest` if you want history.

### 10. Rebalance Storms

If consumers come and go frequently (e.g., autoscaling) → rebalance constantly.

→ Use cooperative rebalancing + static membership where possible.

### 11. Partition Skew

Heavy keys → 80% records to one partition → that consumer overloaded, others idle.

→ Choose distribution-friendly keys.

### 12. Forgetting Headers

`__TypeId__`, trace IDs, correlation IDs — easy to drop. Read explicitly.

```java
String traceId = new String(record.headers().lastHeader("trace-id").value());
```

---

## Summary Cheat Sheet

| Setting | Default | Recommendation |
|---------|---------|----------------|
| `auto.offset.reset` | latest | earliest (new group needs history) or latest |
| `enable.auto.commit` | true (client) / false (Spring) | **false** |
| `max.poll.records` | 500 | 50-1000 (tune to processing time) |
| `max.poll.interval.ms` | 300000 | 60-600s (cover processing time) |
| `session.timeout.ms` | 45000 | 10-60s |
| `heartbeat.interval.ms` | 3000 | session/3 |
| `fetch.min.bytes` | 1 | 1 (latency) or 64K (throughput) |
| `fetch.max.wait.ms` | 500 | 100-5000 |
| `isolation.level` | read_uncommitted | read_committed (with txn producers) |
| `partition.assignment.strategy` | Range | CooperativeStickyAssignor |

| Goal | Settings |
|------|---------|
| **Lowest latency** | Small max.poll.records, fetch.min.bytes=1 |
| **Highest throughput** | Large max.poll.records, fetch.min.bytes=64K |
| **Strict at-least-once** | Manual commit after process |
| **Exactly-once** | + isolation_level=read_committed + transactional producer |
| **Stable across restarts** | Static membership + cooperative |
| **Fast failover** | Low session.timeout |

| ✅ Do | ❌ Don't |
|-------|---------|
| Manual commit (after processing) | Trust auto-commit for business |
| Cooperative + sticky assignment | Default Range assignor |
| Static membership (K8s) | Long rebalances on every restart |
| Tune max.poll.records to processing | Default 500 always |
| Graceful close | Crash without close |
| DLT for poison messages | Infinite retry loop |
| Unique group.id per role | Reuse across apps |

---

## Practice

1. Start a consumer with `auto-offset-reset=earliest` on a topic with existing data; observe replay.
2. Disable auto-commit; manually commit only after processing succeeds.
3. Simulate slow processing (sleep 6 min) — observe rebalance kick.
4. Switch from RangeAssignor to CooperativeStickyAssignor; restart one consumer; observe minimal disruption.
5. Set `group.instance.id` for static membership; restart without rebalance.
6. Use `kafka-consumer-groups.sh` to inspect lag, offsets, members.
7. Reset offsets to a specific timestamp:
   ```bash
   --reset-offsets --to-datetime 2024-05-08T00:00:00.000
   ```
8. Test transactional producer + `isolation.level=read_committed`; abort a transaction; verify consumer skips.
9. Increase `max.poll.records=2000` for batch processing; bulk-insert to DB.
10. Build a healthy commit pattern: async during steady state, sync on shutdown.

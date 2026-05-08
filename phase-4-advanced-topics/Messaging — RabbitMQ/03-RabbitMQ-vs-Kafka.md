# RabbitMQ vs Kafka

## Status: Not Started

---

## Table of Contents

1. [Why This Comparison?](#why-this-comparison)
2. [Quick Verdict](#quick-verdict)
3. [Mental Model Difference](#mental-model-difference)
4. [Push vs Pull](#push-vs-pull)
5. [Message Lifecycle](#message-lifecycle)
6. [Routing Capabilities](#routing-capabilities)
7. [Throughput & Performance](#throughput--performance)
8. [Ordering & Partitioning](#ordering--partitioning)
9. [Scalability](#scalability)
10. [Reliability & Durability](#reliability--durability)
11. [Replay-ability](#replay-ability)
12. [Operational Complexity](#operational-complexity)
13. [Latency](#latency)
14. [Ecosystem & Tooling](#ecosystem--tooling)
15. [Decision Matrix — When to Use What](#decision-matrix--when-to-use-what)
16. [Hybrid Patterns](#hybrid-patterns)
17. [Migration Considerations](#migration-considerations)
18. [Common Pitfalls](#common-pitfalls)
19. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Why This Comparison?

Both are popular **messaging systems** but solve **different problems**. Choosing wrong = pain.

> "Use Kafka because it's hot" → maybe wrong tool for your task queue.
> "Stick with RabbitMQ" → maybe wrong tool for event streaming at scale.

This file: clear-eyed comparison with practical guidance.

---

## Quick Verdict

| Use Case | Pick |
|----------|------|
| **Task queue** (background jobs, work distribution) | **RabbitMQ** |
| **RPC** (request-reply) | **RabbitMQ** |
| **Complex routing** (topic patterns, headers) | **RabbitMQ** |
| **Pub/sub with limited consumers** | **RabbitMQ** |
| **Event streaming** (replay, history) | **Kafka** |
| **High-throughput logs / metrics** | **Kafka** |
| **Event sourcing / CDC** | **Kafka** |
| **Stream processing** | **Kafka** (Streams / KSQL / Flink) |
| **Simple "send a message"** | RabbitMQ (smaller learning curve) |
| **Many consumers reading same data** | **Kafka** (no duplication overhead) |

→ Often: **both in the same architecture**, each for what it's best at.

---

## Mental Model Difference

### RabbitMQ — "Smart Broker, Dumb Consumer"

```
Producer ──▶ Smart Exchange ──(routing logic)──▶ Queue ──▶ Consumer
                                                              ↑
                                                     Just receives
                                                     Doesn't track position
                                                     Message deleted after ack
```

→ Broker decides routing. Consumer just receives + acks.

### Kafka — "Dumb Broker, Smart Consumer"

```
Producer ──▶ Topic Partition ──▶ Consumer
                                    ↑
                            Tracks own position (offset)
                            Reads at own pace
                            Can rewind/replay
                            Message stays for retention period
```

→ Broker is essentially a log. Consumer manages position.

### Implications

| Aspect | RabbitMQ | Kafka |
|--------|----------|-------|
| Where's the routing logic? | Broker (exchanges) | Producer (chooses partition) |
| What about messages after consume? | Deleted | Persisted for retention |
| Multiple consumer groups? | Each needs separate queue (with bindings) | All read same topic, independent offsets |
| Replay? | Hard (re-publish) | Native (seek to offset) |

---

## Push vs Pull

### RabbitMQ — Push

```
Broker pushes messages to consumer when available.
Consumer prefetch limit controls flow.
```

✅ Lower latency (no polling)
✅ Simpler consumer model
❌ Broker tracks consumer state

### Kafka — Pull

```
Consumer polls broker for messages periodically.
Broker doesn't track much per consumer (just offsets in __consumer_offsets).
```

✅ Consumer controls pace (backpressure naturally)
✅ Easier to scale broker (less state per consumer)
❌ Slightly higher latency (poll interval)

---

## Message Lifecycle

### RabbitMQ

```
1. Producer publishes
2. Exchange routes to queue(s)
3. Broker stores message (memory or disk if persistent)
4. Consumer receives + processes
5. Consumer acks → broker DELETES message
   OR
   Consumer rejects → message requeued or sent to DLX
```

→ **Once consumed and acked, message is gone.** Different consumer groups can't see it later.

### Kafka

```
1. Producer publishes to partition
2. Broker appends to log (always to disk)
3. Replicated to ISR
4. Consumer fetches from offset N
5. Consumer commits offset (in __consumer_offsets)
6. Message stays for retention period (e.g., 7 days)
   Other consumer groups can read same message independently
```

→ **Messages persist; consumers track position.** Multiple uses of same data possible.

### Key Implications

```
RabbitMQ: "Process this work item" → done, gone
Kafka:    "Here's a stream of events" → many consumers, replayable
```

---

## Routing Capabilities

### RabbitMQ — Rich Routing

```
Producer ──▶ Topic Exchange "events"
              │
              ├─[order.placed.high]─▶ priority-queue
              ├─[order.placed.*]──▶ all-orders-queue
              ├─[*.placed.*]──▶ analytics-queue
              ├─[#]──▶ audit-queue
              └─[order.cancelled]──▶ refunds-queue
```

✅ Complex routing without producer changes
✅ Pattern matching (`*`, `#`)
✅ Header-based routing
✅ Multiple exchange types

### Kafka — Simple Routing

```
Producer chooses topic + partition (via key hash or explicit).
That's it.
```

→ Want filtering? **Each consumer filters in app code** (or use Kafka Streams).
→ Want fanout? **Each consumer group reads the topic.** No "exchange" concept.

```
Topic "orders" → Consumer Group A → filters → handles
                → Consumer Group B → filters → handles
                → Kafka Streams app → branches → topic-A, topic-B
```

### When Each Wins

- **Complex routing rules** that change frequently → RabbitMQ
- **High volume, simple "all consumers read all"** → Kafka
- **Routing by message content** → Kafka Streams (more powerful but heavier)

---

## Throughput & Performance

### Typical Numbers

| Metric | RabbitMQ | Kafka |
|--------|----------|-------|
| Per broker, single queue/topic | 10K-50K msg/sec | 100K-1M+ msg/sec |
| Multi-broker cluster | 100K-500K msg/sec | 1M-10M msg/sec |
| Latency (p99) | 1-10ms | 5-50ms |
| Storage | Memory + disk (persistence optional) | Always disk |

⚠️ Numbers vary wildly with config, hardware, message size.

### Why Kafka is Faster?

- **Sequential disk writes** (append-only log)
- **Zero-copy** with sendfile syscall
- **Batching** at producer + broker
- **Partitioning** for parallel I/O
- **No per-message routing** (already in partition)

### Why RabbitMQ is Slower?

- **Per-message routing** (exchange decides per send)
- **Per-message ack tracking**
- **In-memory queues** can hit memory limits
- **Erlang VM** overhead vs JVM

→ For most apps (10K-100K msg/sec), **RabbitMQ is plenty**.

---

## Ordering & Partitioning

### RabbitMQ Ordering

- **Per queue**: messages received in order (FIFO)
- **Multiple consumers on same queue**: order LOST (each consumer takes from queue)
- **Single consumer per queue**: order preserved

→ For ordered processing: 1 queue + 1 consumer (or sharded queues per key).

### Kafka Ordering

- **Per partition**: strictly ordered
- **Across partitions**: NOT ordered
- **Same key → same partition**: per-key ordering guaranteed

```java
// All "user-123" messages → same partition → ordered
producer.send(new ProducerRecord<>("orders", "user-123", order));
```

### Implications

| Need | RabbitMQ | Kafka |
|------|----------|-------|
| Strict global order | 1 queue, 1 consumer | 1 partition (no parallelism) |
| Per-entity order | Sharded queues | Use entity ID as key |
| Order doesn't matter | Multiple consumers per queue | Many partitions |

→ Kafka's per-key ordering scales better than RabbitMQ for ordered work.

---

## Scalability

### RabbitMQ Scaling

- **Vertical**: bigger broker (more RAM, faster disk)
- **Horizontal**: cluster nodes + quorum queues
- **Limits**:
  - Single queue → single master node (queue ≠ partitioned)
  - Beyond ~50K msg/sec/queue → scale by sharding queues

### Kafka Scaling

- **Vertical**: bigger broker
- **Horizontal**: add brokers + partitions
- **Limits**:
  - Practically unlimited at 100s of brokers
  - Partition count = scaling unit

### Multi-Tenant / Multi-Region

- **RabbitMQ**: Federation, Shovel plugins (cross-broker)
- **Kafka**: MirrorMaker 2, Confluent Replicator

---

## Reliability & Durability

### RabbitMQ

- **Persistent messages** + **durable queues** = survive broker restart
- **Quorum queues** = HA via Raft consensus
- **Publisher confirms** = at-least-once publish guarantee
- **DLX** = handle failures

### Kafka

- **Always disk-based** (no "memory only" mode)
- **Replication** via ISR — strong durability with `acks=all` + `min.insync.replicas`
- **Idempotent producer** + **transactions** = exactly-once within Kafka

→ Both reliable; Kafka has stronger out-of-box durability.

---

## Replay-ability

### RabbitMQ — No Native Replay

Once consumed + acked = gone. To "replay", you need to:
- Re-publish messages
- Or use **Streams** (RabbitMQ 3.9+) — Kafka-like log queues

### Kafka — Native Replay

```java
consumer.seek(new TopicPartition("orders", 0), 100);     // jump to offset 100
consumer.seekToBeginning(...);                            // jump to start
consumer.seekToEnd(...);                                  // skip to end
```

→ Reset consumer group offsets:

```bash
kafka-consumer-groups.sh --reset-offsets --to-earliest --topic orders --group my-group --execute
```

### Why Replay Matters

- **Bug fix**: re-process events after fixing logic
- **New consumer**: catches up on history
- **Audit / debugging**: see what happened
- **Materialized views**: rebuild state from event log

→ Kafka's killer feature for event-driven systems.

---

## Operational Complexity

### RabbitMQ

✅ Simpler to start (single binary, web UI)
✅ Less infra (no ZK / KRaft)
❌ Erlang ecosystem can be unfamiliar
❌ Cluster operations (mirrored vs quorum) confusing historically

### Kafka

❌ More moving parts (broker + ZK or KRaft + Schema Registry + Connect + Streams)
❌ JVM tuning required
❌ Topic / partition management overhead
✅ Modern KRaft simplifies setup
✅ Many managed offerings (MSK, Confluent Cloud, Aiven, Redpanda Cloud)

→ For small teams: RabbitMQ is **operationally lighter**.

---

## Latency

### RabbitMQ

- **Direct push** = low latency
- **In-memory routing** = fast
- **Typical**: 1-10 ms

### Kafka

- **Polling** model = some latency (poll interval)
- **Disk persistence** = small overhead
- **Typical**: 5-50 ms

→ For sub-10ms latency requirements (RPC, real-time UI), RabbitMQ edges out.
→ For 100ms+ tolerance, Kafka is fine.

---

## Ecosystem & Tooling

### RabbitMQ Ecosystem

- **Management UI** — built-in, excellent
- **Plugins** — Shovel, Federation, MQTT, STOMP, Stream
- **AMQP standard** — many clients
- **Spring AMQP** — first-class Spring integration

### Kafka Ecosystem

- **Schema Registry** (Confluent)
- **Kafka Connect** — connectors for DBs, S3, etc.
- **Kafka Streams** + **ksqlDB** — stream processing
- **MirrorMaker** — cross-cluster replication
- **Debezium** — CDC
- **kcat** (kafkacat) — CLI tool
- **Burrow** — lag monitoring
- **Confluent Control Center**, **Kowl**, **AKHQ** — UIs

→ Kafka has **bigger** ecosystem for data pipelines + streaming.

---

## Decision Matrix — When to Use What

### Use RabbitMQ When...

| Scenario | Why |
|----------|-----|
| **Task / job queue** (email, image processing, reports) | Simpler, deletes after consume |
| **RPC / request-reply** | Built-in `replyTo` correlation |
| **Complex routing** (topic patterns, headers) | Native exchange types |
| **Sub-10ms latency** required | Push model + in-memory |
| **Few consumers per message** | No history needed |
| **Small ops team** | Easier to run |
| **Need MQTT / STOMP** | Plugins available |
| **Per-message TTL** | Native support |
| **Priority queues** | Native (classic queues) |

### Use Kafka When...

| Scenario | Why |
|----------|-----|
| **Event streaming** (clickstream, IoT, telemetry) | High throughput, retention |
| **Event sourcing** | Append-only log model |
| **CDC** (DB changes) | Debezium + Kafka standard |
| **Many consumer groups** for same data | Pull-based, replayable |
| **Stream processing** (joins, aggregations) | Kafka Streams |
| **Replay needed** for new consumers / debugging | Native seek/replay |
| **Throughput >100K msg/sec** | Designed for this |
| **Strict per-key ordering at scale** | Partition by key |
| **Long-term storage of events** | Configurable retention |
| **Microservices "central nervous system"** | Decouples many services |

### Use Both When...

```
RabbitMQ for:    Internal task queues, RPC, complex routing
Kafka for:       Event log, CDC, analytics pipeline
```

→ Many large architectures use **both**. They complement each other.

---

## Hybrid Patterns

### Pattern 1 — Kafka for Events, RabbitMQ for Tasks

```
Event Source ──▶ Kafka (event log)
                      ↓
                Stream Processor
                      ↓
                Specific tasks ──▶ RabbitMQ (task queue) ──▶ Workers
```

- Kafka = source of truth
- RabbitMQ = work distribution

### Pattern 2 — RabbitMQ → Kafka Bridge

```
Legacy systems publish to RabbitMQ ──▶ Bridge service ──▶ Kafka (analytics)
```

- Don't disrupt existing apps
- Add Kafka for new use cases

### Pattern 3 — Kafka Connect Source/Sink

```
Kafka ──▶ Kafka Connect ──▶ RabbitMQ (or vice versa)
```

- Tools exist (community connectors)

### Pattern 4 — RabbitMQ for Sync, Kafka for Async

```
User request → RabbitMQ RPC → Service → reply
            └─▶ Kafka event ─▶ analytics (async)
```

---

## Migration Considerations

### RabbitMQ → Kafka

**Why?**
- Outgrew throughput
- Need replay
- Want stream processing

**Considerations**:
- Routing logic moves to consumer side
- "Delete after consume" → "configure retention"
- Single queue → topic with partitions
- Update client libraries
- Schema management (introduce Schema Registry)

### Kafka → RabbitMQ

**Why?** (less common)
- Overkill for use case
- Need RPC
- Want simpler ops

**Considerations**:
- Lose replay capability
- Need to design routing (exchanges)
- Re-build dashboards / monitoring

### Strangler Pattern

Migrate one consumer at a time:
1. Bridge service: copy from old to new
2. Move new consumers to new system
3. Eventually decommission old

---

## Common Pitfalls

### 1. "We Need Kafka" Without Real Need

Default-Kafka mindset for a 1000-msg/sec task queue → operational pain for no benefit.

### 2. "RabbitMQ Can't Scale"

It can, to most needs. ~100K msg/sec single broker, much more clustered.

### 3. Using RabbitMQ as Event Log

No native replay, messages deleted on ack. Use **streams** (3.9+) or pick Kafka.

### 4. Using Kafka for RPC

Possible but awkward. Latency higher, no native correlation. Use HTTP/gRPC or RabbitMQ.

### 5. Per-Message Routing in Kafka

Producer must decide partition. Want runtime routing? Use Kafka Streams or just consume-and-forward.

### 6. Comparing on Throughput Alone

Routing flexibility, ops simplicity, latency are also important factors.

### 7. "Same Tool for All Messaging"

Sometimes one tool fits 80% of needs and a second fits the other 20%.

### 8. Switching for Hype

Big migration cost. Solid reasons required.

### 9. Underestimating Kafka Ops

Schema Registry, rebalances, partition planning, retention sizing — non-trivial.

### 10. Underestimating RabbitMQ for Streaming

RabbitMQ Streams (3.9+) closes the gap somewhat. For mixed loads, single tool can work.

---

## Summary Cheat Sheet

| Feature | RabbitMQ | Kafka |
|---------|----------|-------|
| **Model** | Message broker | Distributed log |
| **Delivery** | Push | Pull |
| **Routing** | Smart (exchanges) | Dumb (partitions) |
| **Storage** | Until acked | For retention period |
| **Replay** | No (or Streams) | Yes (native) |
| **Order** | Per queue (1 consumer) | Per partition / per key |
| **Throughput (per broker)** | 10K-50K msg/sec | 100K-1M+ msg/sec |
| **Latency** | 1-10 ms | 5-50 ms |
| **Routing patterns** | Direct, Topic, Fanout, Headers | None (consumer filters) |
| **Multiple consumers same data** | Need separate queues | Native (consumer groups) |
| **HA** | Quorum queues | ISR replication |
| **Ecosystem** | AMQP, MQTT, STOMP, plugins | Streams, Connect, Schema Registry |
| **Ops complexity** | Lower | Higher |

| Use Case | Pick |
|----------|------|
| Background job queue | RabbitMQ |
| RPC | RabbitMQ |
| Complex routing | RabbitMQ |
| Sub-10ms latency | RabbitMQ |
| Event streaming | Kafka |
| Replay required | Kafka |
| 100K+ msg/sec | Kafka |
| Stream processing | Kafka |
| CDC | Kafka |
| Event sourcing | Kafka |
| Few consumers per message | RabbitMQ |
| Many consumer groups | Kafka |

| ✅ Right Reason | ❌ Wrong Reason |
|----------------|-----------------|
| Tool fits use case | "It's hot" / "Big tech uses it" |
| Throughput need clear | "Just in case" |
| Replay actually needed | "Nice to have" |
| Team has expertise | Adopt random library |
| Operational tolerance matched | Underestimate ops cost |

---

## Practice

1. Build same task queue in both — note differences in setup, code, ops.
2. Build same event broadcast (5 consumers reading same data) in both — compare resource usage.
3. Run benchmarks: 10K, 100K, 1M msg/sec — observe where each plateaus.
4. Implement RPC in RabbitMQ → try same in Kafka → note complexity difference.
5. Implement event sourcing (replay-able log) in Kafka → try same in RabbitMQ → discuss.
6. Set up DLX in RabbitMQ vs DLT in Kafka — compare patterns.
7. List your current async needs — categorize task-queue vs event-stream → recommend tool per category.
8. Try RabbitMQ Streams (3.9+) — does it close the gap for your needs?
9. Read post-mortems online about choosing wrong tool — what would you have decided?
10. Document your team's "when to use which" decision tree.

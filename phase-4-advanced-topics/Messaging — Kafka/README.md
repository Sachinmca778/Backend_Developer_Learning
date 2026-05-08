# Messaging — Kafka

Production-grade **Apache Kafka** — fundamentals, Spring Kafka integration, serialization (JSON / Avro / Protobuf), producer + consumer tuning, aur production patterns (DLT, retry topics, outbox, EOS, CDC, Kafka Streams). Sab Hinglish mein, deep code examples + tuning profiles ke saath. Companion to **Microservices Architecture/08-Event-Driven-Architecture**.

---

## Topics & Status

| # | Topic | File | Status |
|---|-------|------|--------|
| 1 | Kafka Fundamentals | [01-Kafka-Fundamentals.md](./01-Kafka-Fundamentals.md) | Not Started |
| 2 | Spring Kafka | [02-Spring-Kafka.md](./02-Spring-Kafka.md) | Not Started |
| 3 | Serialization | [03-Serialization.md](./03-Serialization.md) | Not Started |
| 4 | Producer Configuration | [04-Producer-Configuration.md](./04-Producer-Configuration.md) | Not Started |
| 5 | Consumer Configuration | [05-Consumer-Configuration.md](./05-Consumer-Configuration.md) | Not Started |
| 6 | Kafka Patterns | [06-Kafka-Patterns.md](./06-Kafka-Patterns.md) | Not Started |

---

## What's Inside Each File?

### [01 — Kafka Fundamentals](./01-Kafka-Fundamentals.md)
Topics, partitions, offsets, producers, consumers, **consumer groups** (parallelism + isolation), brokers + cluster, **ZooKeeper vs KRaft** (modern KRaft mode for ZK-free Kafka), replication factor, **ISR** (in-sync replicas) + `min.insync.replicas`, **leader election**, retention policy (time + size + log compaction), message delivery semantics (at-most-once / at-least-once / **exactly-once**), pitfalls (key skew, ordering across partitions, big messages, rebalance storms).

### [02 — Spring Kafka](./02-Spring-Kafka.md)
`KafkaTemplate` for producing (sync + async + ProducerRecord with headers), **`@KafkaListener`** with all signature variants (ConsumerRecord, headers, multiple topics, topic patterns, specific partitions/offsets, concurrency), `@EnableKafka` + ProducerFactory + ConsumerFactory + **`KafkaListenerContainerFactory`**, **Acknowledgment modes** (RECORD/BATCH/MANUAL/MANUAL_IMMEDIATE), manual ack patterns, **batch listeners** for bulk processing, error handling with `DefaultErrorHandler` + DLT, `ReplyingKafkaTemplate` for request-reply, transactional producer + consumer, testing with `@EmbeddedKafka` + Testcontainers, observability.

### [03 — Serialization](./03-Serialization.md)
String/JSON for prototyping, **`JsonSerializer`** with type headers + **trusted packages** security, type aliases (`spring.json.type.mapping`), ByteArray for custom formats, **Avro + Confluent Schema Registry** (schema definition, code generation, subject naming strategies, BACKWARD/FORWARD/FULL compatibility), **Protobuf** alternative, custom serializers, **`ErrorHandlingDeserializer`** wrapper for poison messages, schema evolution rules, full producer/consumer property reference.

### [04 — Producer Configuration](./04-Producer-Configuration.md)
Producer architecture (Sender thread + RecordAccumulator), **`acks=0/1/all`** durability trade-offs, `retries` + `retry.backoff.ms`, `max.block.ms`, **`delivery.timeout.ms`** (end-to-end deadline), **`linger.ms` + `batch.size`** for batching efficiency, **`compression.type`** (none/gzip/snappy/lz4/zstd), **`enable.idempotence`** (no duplicates from retries), **transactional producer** (multi-topic atomic writes), `max.in.flight.requests.per.connection`, buffer memory, custom partitioner, **4 tuning profiles** (low-latency / high-throughput / best-effort / transactional).

### [05 — Consumer Configuration](./05-Consumer-Configuration.md)
Consumer lifecycle + group coordinator, **`auto.offset.reset`** (earliest / latest / none), `enable.auto.commit` (and why to disable), **manual commit (SYNC vs ASYNC)** patterns including hybrid, **`max.poll.records`** + **`max.poll.interval.ms`** (slow-processing rebalance trap), `session.timeout.ms` + `heartbeat.interval.ms` relationship, `fetch.min.bytes` + `fetch.max.wait.ms`, **`isolation.level=read_committed`** for transactional reads, rebalance triggers, **cooperative rebalancing** (`CooperativeStickyAssignor`) + **static membership** (`group.instance.id`) for K8s, partition assignment strategies, **4 tuning profiles**.

### [06 — Kafka Patterns](./06-Kafka-Patterns.md)
**Dead Letter Topic (DLT)** with Spring's `DefaultErrorHandler` + `DeadLetterPublishingRecoverer`, **retry topics** with `@RetryableTopic` + exponential backoff, **Exactly-Once Semantics** (producer txn + consumer `read_committed` + `sendOffsetsToTransaction`), **transactional outbox** (atomic DB + Kafka publish, polling worker, **Debezium CDC alternative**), **compacted topics** for event sourcing + tombstones, **Kafka Streams** (filter/map/join/window/state stores), **consumer lag monitoring** (kafka-consumer-groups.sh, Burrow, Lag Exporter), **idempotent consumers** (dedup table, upsert, versioned update), saga pattern with Kafka, **CDC with Debezium**, topic naming conventions.

---

## Recommended Learning Order

```
1. Kafka Fundamentals (01)         ← core mental model
2. Spring Kafka (02)                ← Java/Spring integration
3. Serialization (03)                ← message formats
4. Producer Configuration (04)      ← write-side tuning
5. Consumer Configuration (05)      ← read-side tuning
6. Kafka Patterns (06)              ← production patterns
```

---

## Quick Reference

### "Mujhe X karna hai" → kahan dekhun?

| Task | File | Section |
|------|------|---------|
| Topics + partitions samajhna | 01 | Topics & Partitions |
| Replication setup karna | 01 | Replication & ISR |
| KRaft mode pe migrate | 01 | ZooKeeper vs KRaft |
| Spring Boot mein Kafka use | 02 | Setup |
| `KafkaTemplate` se publish | 02 | Producing Messages |
| `@KafkaListener` set up | 02 | Consuming Messages |
| Manual ack ka pattern | 02 | Manual Acknowledgment |
| Batch process karna | 02 | Batch Listeners |
| JSON ke jagah Avro use | 03 | Avro + Schema Registry |
| Trusted packages security | 03 | Trusted Packages |
| Schema evolution rules | 03 | Schema Evolution |
| `acks=all` ya `acks=1`? | 04 | acks (Acknowledgments) |
| Throughput improve karna | 04 | linger.ms & batch.size |
| Producer compression | 04 | compression.type |
| Idempotent producer | 04 | enable.idempotence |
| Transactional producer | 04 | Transactional Producer |
| Kahan se start karega consumer? | 05 | auto.offset.reset |
| Manual commit pattern | 05 | Manual Commit |
| Slow processing rebalance fix | 05 | max.poll.interval.ms |
| K8s mein static membership | 05 | Static Membership |
| Bad messages handle karna | 06 | Dead Letter Topic |
| Retries with backoff | 06 | Retry Topics |
| DB + Kafka atomically | 06 | Transactional Outbox |
| Event sourcing snapshots | 06 | Compacted Topics |
| Stream processing | 06 | Kafka Streams |
| Consumer lag dekhna | 06 | Consumer Lag Monitoring |
| DB changes Kafka mein | 06 | Change Data Capture |

---

## End-to-End Architecture Picture

```
┌──────────────────────────────────────────────────────────────────┐
│                          Producer App                              │
│                                                                    │
│  Service Logic                                                     │
│      │                                                             │
│      ▼                                                             │
│  ┌─────────────────────────┐                                       │
│  │ KafkaTemplate (file 02) │                                       │
│  │  + Serializer (file 03) │                                       │
│  │  + Producer Config      │                                       │
│  │    (file 04)            │                                       │
│  │  + Outbox/Txn (file 06) │                                       │
│  └─────────────────────────┘                                       │
└──────────────────────────────┬───────────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────────┐
│                       Kafka Cluster (file 01)                      │
│                                                                    │
│   Topic: orders                                                    │
│   ┌──────┐  ┌──────┐  ┌──────┐                                    │
│   │ P0   │  │ P1   │  │ P2   │   replicated × 3                   │
│   │ ISR  │  │ ISR  │  │ ISR  │   leader + followers               │
│   └──────┘  └──────┘  └──────┘                                    │
└──────────────────────────────┬───────────────────────────────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
        ▼                      ▼                      ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ Consumer Group A │  │ Consumer Group B │  │ Kafka Streams    │
│  (file 02 + 05)  │  │  (file 02 + 05)  │  │  (file 06)       │
│                  │  │                  │  │                  │
│ + DLT (file 06)  │  │ + Idempotent     │  │  KTable / Joins  │
│ + Manual Ack     │  │   (file 06)      │  │  Windows         │
└──────────────────┘  └──────────────────┘  └──────────────────┘
```

---

## Companion Folders

- [Microservices Architecture](../Microservices%20Architecture/) — `08-Event-Driven-Architecture.md` overlaps; `07-Saga-Pattern.md` complements
- [Spring Cloud](../Spring%20Cloud/) — Spring Cloud Bus uses Kafka as transport
- [API Design & Architecture](../../backend-skills/API-Design-&-Architecture/) — async vs sync API choices
- [Database Mastery](../../backend-skills/Database-Mastery/) — outbox table design + transactional patterns
- [Security Best Practices](../../backend-skills/Security-Best-Practices/) — secrets management for Kafka credentials
- [DevOps & CI/CD](../../backend-skills/DevOps-&-CI-CD/) — monitoring/alerting, Docker for local Kafka

---

## Tools & Libraries Reference

### Kafka Distributions
- **Apache Kafka** (open source)
- **Confluent Platform** (Kafka + Schema Registry + ksqlDB + Connect + Control Center)
- **Amazon MSK** (managed Kafka on AWS)
- **Aiven Kafka** (managed multi-cloud)
- **Redpanda** (Kafka-compatible, single binary)
- **WarpStream** (Kafka-compatible, S3-backed)

### Spring Kafka Stack
- **spring-kafka** — core integration
- **spring-kafka-test** — `@EmbeddedKafka` + Testcontainers
- **spring-cloud-stream-binder-kafka** — abstraction (alternative API)

### Serialization
- **Apache Avro** + **Confluent Schema Registry**
- **Protobuf** (Google) + Confluent Schema Registry
- **Spring Kafka JsonSerializer/JsonDeserializer**

### Operations
- **kafka-topics.sh, kafka-console-producer.sh, kafka-console-consumer.sh** — CLI
- **kcat (formerly kafkacat)** — fast CLI client
- **Conduktor / Offset Explorer / AKHQ** — UI tools
- **Burrow** — consumer lag monitoring (LinkedIn)
- **Kafka Lag Exporter** — Prometheus exporter
- **Cruise Control** — cluster balancing

### Streaming + Connect
- **Kafka Streams** — stream processing library
- **ksqlDB** — SQL-like stream processing
- **Kafka Connect** — connector framework
- **Debezium** — CDC connectors
- **MirrorMaker 2** — cross-cluster replication

---

## Status Tracker

```
[ ] 01 — Kafka Fundamentals
[ ] 02 — Spring Kafka
[ ] 03 — Serialization
[ ] 04 — Producer Configuration
[ ] 05 — Consumer Configuration
[ ] 06 — Kafka Patterns
```

Topic complete hone par file header aur is README dono mein status update kar lena.

> Kafka mastery = fundamentals + Spring integration + correct configs + production patterns. Iss folder mein sab cover ho gaya — combine with Microservices Architecture for full event-driven picture.

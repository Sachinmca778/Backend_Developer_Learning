# Messaging — RabbitMQ

Production-grade **RabbitMQ** — concepts (exchanges, queues, bindings, DLX), Spring AMQP integration, aur **comprehensive RabbitMQ vs Kafka** decision guide. Sab Hinglish mein, deep code examples + comparison tables ke saath. Companion to **Messaging — Kafka** folder.

---

## Topics & Status

| # | Topic | File | Status |
|---|-------|------|--------|
| 1 | RabbitMQ Concepts | [01-RabbitMQ-Concepts.md](./01-RabbitMQ-Concepts.md) | Not Started |
| 2 | Spring AMQP | [02-Spring-AMQP.md](./02-Spring-AMQP.md) | Not Started |
| 3 | RabbitMQ vs Kafka | [03-RabbitMQ-vs-Kafka.md](./03-RabbitMQ-vs-Kafka.md) | Not Started |

---

## What's Inside Each File?

### [01 — RabbitMQ Concepts](./01-RabbitMQ-Concepts.md)
Architecture (broker, vhosts, connections, channels), **AMQP protocol** basics, all 4 exchange types: **Direct** (exact key match), **Topic** (wildcard patterns `*` and `#`), **Fanout** (broadcast), **Headers** (`x-match: all/any`) plus default exchange, **queues** (durable, exclusive, auto-delete, server-named), **bindings + routing keys**, message properties (deliveryMode, persistence, TTL, priority, correlationId, replyTo), **acknowledgments** (basicAck/basicNack/basicReject), **prefetch count (QoS)** for fair distribution, **message TTL + queue TTL**, **Dead Letter Exchange (DLX)** with full headers, **lazy queues / quorum queues / streams** (modern HA), clustering basics.

### [02 — Spring AMQP](./02-Spring-AMQP.md)
Setup with `spring-boot-starter-amqp`, comprehensive YAML configuration, declaring **`Exchange` / `Queue` / `Binding` beans** with `RabbitAdmin` auto-declaration, **`RabbitTemplate.convertAndSend()`** for producing (with post-processors and properties), **`@RabbitListener`** consuming (POJO conversion, headers, multiple queues, `@RabbitHandler` for type-routing, inline `@QueueBinding`), `@EnableRabbit` (when needed), **`Jackson2JsonMessageConverter`** with **trusted packages security** + class mapping, **acknowledgment modes** (NONE/AUTO/MANUAL) + manual ack patterns, concurrency (3-10 auto-scale), **`SimpleRabbitListenerContainerFactory`** customization, **error handling + retry** with `RetryOperationsInterceptor` + `RepublishMessageRecoverer`, **publisher confirms** (correlated + returns callback), **RPC** with `convertSendAndReceive`, transactions warning, **Testcontainers** for testing, observability (Micrometer + Management UI + Prometheus plugin).

### [03 — RabbitMQ vs Kafka](./03-RabbitMQ-vs-Kafka.md)
Quick decision matrix at top, **mental model differences** (smart broker vs smart consumer), push vs pull, message lifecycle (deletion vs retention), routing capabilities (rich exchanges vs simple partitions), **throughput numbers** (10K-50K vs 100K-1M+ msg/sec), **ordering** (per queue vs per partition with key), scalability, reliability, **replay-ability** (Kafka's killer feature), operational complexity, latency comparison (1-10ms vs 5-50ms), ecosystem comparison, **decision matrix per use case** (task queue, RPC, event streaming, CDC, etc.), **hybrid patterns** (using both), migration considerations, common wrong-reason traps.

---

## Recommended Learning Order

```
1. RabbitMQ Concepts (01)         ← core mental model
2. Spring AMQP (02)                ← Spring Boot integration
3. RabbitMQ vs Kafka (03)          ← when to use which
```

---

## Quick Reference

### "Mujhe X karna hai" → kahan dekhun?

| Task | File | Section |
|------|------|---------|
| Exchange types samajhna | 01 | Exchanges (sections 7-11) |
| Topic exchange wildcards | 01 | Topic Exchange |
| Fanout broadcast | 01 | Fanout Exchange |
| Headers exchange | 01 | Headers Exchange |
| Queue declarations | 01 | Queues |
| DLX setup | 01 | Dead Letter Exchange |
| Quorum queues for HA | 01 | Lazy Queues, Quorum Queues, Streams |
| Spring Boot setup | 02 | Setup |
| Producer code | 02 | RabbitTemplate — Producing |
| Consumer code | 02 | @RabbitListener — Consuming |
| JSON conversion + security | 02 | MessageConverter |
| Manual ack pattern | 02 | Manual Acknowledgment |
| Retry with backoff | 02 | Error Handling & Retry |
| Publisher confirms | 02 | Publisher Confirms |
| RPC pattern | 02 | RPC (Request-Reply) |
| Testing | 02 | Testing (Testcontainers) |
| RabbitMQ vs Kafka decision | 03 | Decision Matrix |
| Use both? | 03 | Hybrid Patterns |
| Migrating between them | 03 | Migration Considerations |

---

## Architecture at a Glance

```
┌──────────────────────────────────────────────────────────────────┐
│                       Spring Boot App                              │
│                                                                    │
│  ┌──────────────────────┐         ┌──────────────────────────┐    │
│  │ RabbitTemplate       │         │ @RabbitListener          │    │
│  │ (file 02)            │         │ (file 02)                │    │
│  │ + MessageConverter   │         │ + ackMode + concurrency  │    │
│  │ + Publisher Confirms │         │ + Retry + ErrorHandler   │    │
│  └──────────┬───────────┘         └────────────▲──────────────┘    │
└─────────────┼──────────────────────────────────┼────────────────────┘
              │                                  │
              ▼                                  │
┌──────────────────────────────────────────────────────────────────┐
│                       RabbitMQ Broker (file 01)                    │
│                                                                    │
│  ┌─────────────┐  routing keys/patterns  ┌──────────────┐         │
│  │  Exchange   │ ──────────────────────▶ │  Queue(s)    │         │
│  │  - Direct   │   via bindings          │  - Durable   │         │
│  │  - Topic    │                         │  - DLX setup │         │
│  │  - Fanout   │                         │  - TTL       │         │
│  │  - Headers  │                         │  - Quorum    │         │
│  └─────────────┘                         └──────────────┘         │
│                                                  │                 │
│                                          on rejection              │
│                                                  ▼                 │
│                                          ┌──────────────┐         │
│                                          │  DLX         │         │
│                                          │  → DLQ       │         │
│                                          └──────────────┘         │
└────────────────────────────────────────────────────────────────────┘
```

---

## Companion Folders

- [Messaging — Kafka](../Messaging%20—%20Kafka/) — covered side-by-side, file 03 here is the comparison
- [Microservices Architecture](../Microservices%20Architecture/) — `08-Event-Driven-Architecture.md` covers async patterns
- [Spring Cloud](../Spring%20Cloud/) — Spring Cloud Bus can use RabbitMQ as transport
- [Database Mastery](../../backend-skills/Database-Mastery/) — outbox pattern for atomic DB+messaging
- [Security Best Practices](../../backend-skills/Security-Best-Practices/) — secrets for broker credentials, TLS

---

## Tools & Plugins Reference

### Distributions
- **RabbitMQ open-source** (Erlang)
- **CloudAMQP** (managed)
- **Amazon MQ for RabbitMQ** (AWS managed)

### Plugins
- **rabbitmq_management** — Web UI (port 15672)
- **rabbitmq_prometheus** — Prometheus metrics (port 15692)
- **rabbitmq_shovel** — Cross-broker message movement
- **rabbitmq_federation** — Transparent multi-broker
- **rabbitmq_mqtt** — MQTT protocol support
- **rabbitmq_stomp** — STOMP protocol support
- **rabbitmq_stream** — Streams (Kafka-like queues)

### Spring AMQP Stack
- **spring-boot-starter-amqp** — auto-config + RabbitTemplate + RabbitListener
- **spring-rabbit-test** — Testcontainers integration

### Operational Tools
- **rabbitmqctl** — CLI admin
- **rabbitmqadmin** — HTTP API CLI
- **Management UI** — built-in plugin
- **Prometheus + Grafana** — metrics dashboards
- **Datadog / New Relic** — APM integrations

### Clients (Polyglot)
- Java (Spring AMQP), Python (pika), Node.js (amqplib), Go (amqp091-go), .NET, Ruby, etc.

---

## Status Tracker

```
[ ] 01 — RabbitMQ Concepts
[ ] 02 — Spring AMQP
[ ] 03 — RabbitMQ vs Kafka
```

Topic complete hone par file header aur is README dono mein status update kar lena.

> RabbitMQ = **smart broker**, **deletes after consume**, **rich routing** — perfect for task queues, RPC, complex topology.
> Kafka = **distributed log**, **retain + replay**, **partitioned scale** — perfect for event streaming, CDC, high throughput.
> Pick **right tool** for the job (often **both**).

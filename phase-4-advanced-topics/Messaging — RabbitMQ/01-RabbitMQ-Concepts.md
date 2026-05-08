# RabbitMQ Concepts

## Status: Not Started

---

## Table of Contents

1. [RabbitMQ Kya Hai?](#rabbitmq-kya-hai)
2. [AMQP Protocol](#amqp-protocol)
3. [Architecture Overview](#architecture-overview)
4. [Connections & Channels](#connections--channels)
5. [Virtual Hosts (vhost)](#virtual-hosts-vhost)
6. [Exchanges](#exchanges)
7. [Direct Exchange](#direct-exchange)
8. [Topic Exchange](#topic-exchange)
9. [Fanout Exchange](#fanout-exchange)
10. [Headers Exchange](#headers-exchange)
11. [Default (Nameless) Exchange](#default-nameless-exchange)
12. [Queues](#queues)
13. [Bindings & Routing Keys](#bindings--routing-keys)
14. [Message Properties](#message-properties)
15. [Acknowledgments](#acknowledgments)
16. [Prefetch Count (QoS)](#prefetch-count-qos)
17. [Message TTL & Queue TTL](#message-ttl--queue-ttl)
18. [Dead Letter Exchange (DLX)](#dead-letter-exchange-dlx)
19. [Lazy Queues, Quorum Queues, Streams](#lazy-queues-quorum-queues-streams)
20. [Clustering & High Availability](#clustering--high-availability)
21. [Common Pitfalls](#common-pitfalls)
22. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## RabbitMQ Kya Hai?

**Matlab:** **Open-source message broker** implementing AMQP (Advanced Message Queuing Protocol). Routes messages from producers to consumers with rich routing capabilities.

> Originally Erlang-based (LShift, 2007), now part of VMware/Pivotal. Battle-tested in production at scale.

### What It Does

```
Producer ──▶ Exchange ──(routing key)──▶ Queue ──▶ Consumer
                  │
                  └─▶ Queue ──▶ Consumer
                  │
                  └─▶ Queue ──▶ Consumer
```

→ Producer doesn't know consumers; just publishes to exchange. Routing decides where messages go.

### Key Capabilities

- **Multiple exchange types** for flexible routing
- **Reliable delivery** (acks, persistence, publisher confirms)
- **Pub/Sub** (fanout)
- **Work queues** (load distribution)
- **RPC** (request-reply)
- **DLX** for failed messages
- **Plugins** (MQTT, STOMP, Federation, Shovel, Management UI)

### Common Use Cases

- **Task queues** (background jobs — email send, image processing, reports)
- **Microservice communication** (async commands, events)
- **RPC** (request-reply with reply-to queue)
- **Decouple producer + consumer**
- **Buffer bursts** during traffic spikes

---

## AMQP Protocol

**Matlab:** **Wire-level protocol** — defines how clients talk to broker. Standard, language-agnostic.

### AMQP 0-9-1 (RabbitMQ Default)

- Most widely used
- Strong routing model (exchanges, queues, bindings)
- Connection / Channel / Frame structure

### AMQP 1.0

- Different model — peer-to-peer messaging
- Used in some enterprise systems (ActiveMQ Artemis)
- RabbitMQ supports via plugin

### Other Protocols (via Plugins)

- **MQTT** (IoT)
- **STOMP** (text-based, simpler)
- **HTTP** (Management API + STOMP-over-WS)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        RabbitMQ Broker                            │
│                                                                   │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐         │
│  │ Connection 1 │   │ Connection 2 │   │ Connection 3 │         │
│  │ ┌──────────┐ │   │ ┌──────────┐ │   │ ┌──────────┐ │         │
│  │ │ Channel  │ │   │ │ Channel  │ │   │ │ Channel  │ │         │
│  │ │ Channel  │ │   │ │ Channel  │ │   │ │ Channel  │ │         │
│  │ └──────────┘ │   │ └──────────┘ │   │ └──────────┘ │         │
│  └──────────────┘   └──────────────┘   └──────────────┘         │
│                                                                   │
│  ┌─────────────────── vhost: /production ─────────────────┐      │
│  │                                                          │      │
│  │   Exchange A ──▶ Queue 1 ──▶ Consumer X                 │      │
│  │       │                                                  │      │
│  │       └──▶ Queue 2 ──▶ Consumer Y                       │      │
│  │                                                          │      │
│  │   Exchange B (DLX) ──▶ Queue 3 (dead letters)           │      │
│  └──────────────────────────────────────────────────────────┘      │
│                                                                    │
│  ┌─────────────────── vhost: /staging ──────────────────────┐     │
│  │   ... separate isolation ...                              │     │
│  └────────────────────────────────────────────────────────────┘     │
└────────────────────────────────────────────────────────────────────┘
```

### Producer Flow

```
1. Open Connection (TCP) to broker
2. Open Channel (lightweight session)
3. Declare Exchange (idempotent — creates if not exists)
4. Publish message to Exchange with routing key
5. Exchange routes to bound queues
```

### Consumer Flow

```
1. Open Connection + Channel
2. Declare Queue (or just use existing)
3. Subscribe (consume) to Queue
4. Receive messages (push from broker)
5. Process + acknowledge (or reject)
```

---

## Connections & Channels

### Connection

**TCP connection** to broker — expensive to open.

```
Default port: 5672 (AMQP), 5671 (AMQPS / TLS), 15672 (Management UI HTTP)
```

### Channel

**Logical session** within a connection — cheap, multiplexed.

```
Connection (1 TCP)
    ├── Channel 1
    ├── Channel 2
    ├── Channel 3
    └── ...
```

### Best Practices

- **One connection per app** (or small pool)
- **One channel per thread** (channels not thread-safe)
- **Reuse channels** for many publish/consume operations
- Open channels generously, but **don't leak** (close when done)

### Connection Limits

```
Broker per-vhost: thousands of connections OK
But each connection has overhead (memory, file descriptors)
```

→ Spring AMQP handles connection pooling for you.

---

## Virtual Hosts (vhost)

**Matlab:** Logical isolation within a single broker — like databases in PostgreSQL.

```
Broker:
├── vhost "/"  (default)
├── vhost "/production"
├── vhost "/staging"
└── vhost "/team-payments"
```

### Why?

- **Multi-tenancy** — separate teams / environments
- **Permissions** — users have permissions per vhost
- **Resource isolation** — exchanges/queues namespaced per vhost

### Permissions Per User

```bash
rabbitmqctl set_permissions -p /production app-user ".*" ".*" ".*"
#                          vhost     user        configure read write
```

→ Pattern matching on resource names.

### Connection String

```
amqp://user:pass@broker:5672/production
                              ↑ vhost (URL-encoded /)
```

---

## Exchanges

**Matlab:** Producer publishes to **exchange** (not directly to queue). Exchange decides routing using **bindings** + **routing keys**.

### Exchange Properties

| Property | Meaning |
|----------|---------|
| **Name** | Exchange identifier |
| **Type** | Direct / Topic / Fanout / Headers |
| **Durable** | Survives broker restart |
| **Auto-delete** | Removed when last queue unbound |
| **Internal** | Can only be reached from other exchanges |
| **Arguments** | Optional config (alternate-exchange, etc.) |

### Declaration

```bash
# CLI
rabbitmqadmin declare exchange name=orders.exchange type=topic durable=true
```

```java
// Spring AMQP
@Bean
public TopicExchange ordersExchange() {
    return new TopicExchange("orders.exchange", true, false);
    //                       name              durable autoDelete
}
```

### Exchange Types Comparison

| Type | Routing Logic | Use Case |
|------|--------------|----------|
| **Direct** | routing key == binding key (exact) | Selective routing by category |
| **Topic** | routing key matches pattern with wildcards | Pub/sub with topic filtering |
| **Fanout** | broadcast to all bound queues | Pub/sub broadcast |
| **Headers** | match on message headers | Complex/multi-criteria routing |

---

## Direct Exchange

**Matlab:** Routes by **exact match** on routing key.

```
Producer ──▶ Exchange "orders" (direct)
              │ routing key: "PLACED"
              │
              ├── binding "PLACED" ──▶ Queue A
              ├── binding "PLACED" ──▶ Queue B
              └── binding "CANCELLED" ──▶ Queue C
```

→ Message with routing key `PLACED` → goes to A and B (both bound with `PLACED`).
→ Routing key `CANCELLED` → goes to C only.

### Java Example

```java
@Bean DirectExchange orderExchange() {
    return new DirectExchange("orders.direct");
}

@Bean Queue placedQueue() {
    return new Queue("orders.placed");
}

@Bean Binding placedBinding(Queue placedQueue, DirectExchange orderExchange) {
    return BindingBuilder.bind(placedQueue).to(orderExchange).with("PLACED");
}
```

### Use Cases

- Simple "by category" routing
- Multiple queues for same routing key (pub/sub-like)
- Logger by severity (`error`, `warn`, `info`)

---

## Topic Exchange

**Matlab:** Routing key is a **dotted pattern**; binding key supports wildcards.

```
Wildcards:
  *  → exactly one word
  #  → zero or more words
```

### Example

```
Routing keys:
  order.placed
  order.cancelled
  payment.success
  payment.failed
  user.signup

Bindings:
  Queue A: order.*           → matches order.placed, order.cancelled
  Queue B: *.success         → matches payment.success, *.success of any domain
  Queue C: order.#           → matches order.placed, order.cancelled, order.foo.bar
  Queue D: #                  → matches everything
```

### Java Example

```java
@Bean TopicExchange eventsExchange() {
    return new TopicExchange("events");
}

@Bean Queue orderQueue() { return new Queue("orders.queue"); }
@Bean Queue paymentQueue() { return new Queue("payments.queue"); }
@Bean Queue auditQueue() { return new Queue("audit.queue"); }

@Bean Binding orderBinding() {
    return BindingBuilder.bind(orderQueue()).to(eventsExchange()).with("order.*");
}

@Bean Binding paymentBinding() {
    return BindingBuilder.bind(paymentQueue()).to(eventsExchange()).with("payment.*");
}

@Bean Binding auditBinding() {
    return BindingBuilder.bind(auditQueue()).to(eventsExchange()).with("#");
}
```

### Use Cases

- **Event-driven** systems with hierarchical topics
- Multi-tenant: `<tenant>.<domain>.<event>`
- Loose coupling — new consumers subscribe to patterns without producer changes

---

## Fanout Exchange

**Matlab:** **Broadcast** — every bound queue gets a copy. Routing key ignored.

```
Producer ──▶ Fanout Exchange
              │
              ├──▶ Queue A
              ├──▶ Queue B
              ├──▶ Queue C
              └──▶ Queue D
```

### Java Example

```java
@Bean FanoutExchange notificationsExchange() {
    return new FanoutExchange("notifications.fanout");
}

@Bean Queue emailQueue() { return new Queue("notifications.email"); }
@Bean Queue smsQueue() { return new Queue("notifications.sms"); }
@Bean Queue pushQueue() { return new Queue("notifications.push"); }

@Bean Binding emailBinding() {
    return BindingBuilder.bind(emailQueue()).to(notificationsExchange());
}
@Bean Binding smsBinding() {
    return BindingBuilder.bind(smsQueue()).to(notificationsExchange());
}
@Bean Binding pushBinding() {
    return BindingBuilder.bind(pushQueue()).to(notificationsExchange());
}
```

### Use Cases

- **Pub/sub broadcast** — notifications, cache invalidation
- **Event streaming** to multiple handlers
- **Logging** to multiple sinks

---

## Headers Exchange

**Matlab:** Route based on **message headers** (not routing key). Use `x-match: all|any`.

```
Bindings:
  Queue A: { "x-match": "all", "format": "pdf", "type": "report" }
  Queue B: { "x-match": "any", "format": "pdf", "format": "xml" }

Message with headers { "format": "pdf", "type": "report" }
  → matches A (all match) and B (any match)

Message with headers { "format": "xml" }
  → matches B (any) but not A (all)
```

### Java Example

```java
@Bean HeadersExchange reportsExchange() {
    return new HeadersExchange("reports.headers");
}

@Bean Queue pdfReportsQueue() { return new Queue("reports.pdf"); }

@Bean Binding pdfBinding() {
    return BindingBuilder.bind(pdfReportsQueue())
        .to(reportsExchange())
        .whereAll(Map.of("format", "pdf", "type", "report"))    // all match
        .match();
}
```

### Send Message with Headers

```java
MessageProperties props = new MessageProperties();
props.setHeader("format", "pdf");
props.setHeader("type", "report");
Message msg = new Message(payload.getBytes(), props);
rabbitTemplate.send("reports.headers", "", msg);
```

### Use Cases

- Complex multi-criteria routing
- Routing without polluting routing key naming

⚠️ Less performant than direct/topic; use when truly needed.

---

## Default (Nameless) Exchange

Every queue is **automatically bound** to the default exchange `""` with binding key = queue name.

```java
// Send to "my-queue" via default exchange
rabbitTemplate.convertAndSend("", "my-queue", payload);
//                            ↑   ↑              ↑
//                         exchange routing key  message
```

→ Useful for simple "send to specific queue" without declaring exchanges.

### Use Case

- Simple worker pattern
- Test setups
- Quick prototyping

---

## Queues

**Matlab:** Buffer where messages wait until consumed.

### Properties

| Property | Meaning |
|----------|---------|
| **Name** | Queue identifier |
| **Durable** | Survives broker restart (queue itself, not necessarily messages) |
| **Exclusive** | Used by only one connection; deleted when connection closes |
| **Auto-delete** | Deleted when last consumer disconnects |
| **Arguments** | Extra config (TTL, DLX, max-length, etc.) |

### Declare

```java
@Bean
public Queue ordersQueue() {
    return QueueBuilder.durable("orders.queue")
        .withArgument("x-message-ttl", 60000)
        .withArgument("x-max-length", 10000)
        .withArgument("x-dead-letter-exchange", "orders.dlx")
        .build();
}
```

### Server-Named Queue

For temporary / RPC reply queues:

```java
new Queue("", false, true, true);   // empty name → broker generates
//        durable exclusive autoDelete
```

→ Broker assigns name like `amq.gen-XXXXX`.

### Queue Length & Memory

```
Queue with 1M messages — memory pressure → broker degrades
```

→ Use `x-max-length` (drop / DLX overflow), `x-max-length-bytes`, or **lazy queues** (paged to disk).

### Inspecting

```bash
rabbitmqctl list_queues name messages consumers state
```

```
Name              Messages   Consumers   State
orders.queue      245        3           running
```

---

## Bindings & Routing Keys

### Binding

Connects an exchange to a queue (or another exchange) with optional routing key / arguments.

```
Exchange ────[binding key: "X"]────▶ Queue
```

### Java Bind

```java
BindingBuilder.bind(queue).to(exchange).with("routing.key.pattern");
```

### Inspect Bindings

```bash
rabbitmqadmin list bindings
rabbitmqctl list_bindings
```

### Routing Key

A **string label** producer attaches to message at publish time.

```java
rabbitTemplate.convertAndSend("orders.exchange", "order.placed.priority", message);
//                            exchange            routing key
```

### Routing Key Conventions

```
Domain-driven hierarchical:
  <domain>.<entity>.<event>      order.payment.success
  <region>.<env>.<event>          us.prod.user.signup
  <tenant>.<domain>.<event>       acme.orders.placed
```

→ Topic exchanges shine with these.

---

## Message Properties

Beyond payload, each message has **properties / headers**:

```java
MessageProperties props = new MessageProperties();
props.setContentType("application/json");
props.setContentEncoding("UTF-8");
props.setMessageId(UUID.randomUUID().toString());
props.setCorrelationId("corr-123");
props.setReplyTo("reply.queue");
props.setExpiration("60000");   // 60s TTL
props.setPriority(5);            // 0-255
props.setTimestamp(new Date());
props.setHeader("custom-header", "value");
props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);   // disk, not memory only
```

### Important Standard Properties

| Property | Use |
|----------|-----|
| `messageId` | Unique ID — for dedup |
| `correlationId` | Trace across services |
| `replyTo` | RPC pattern — where to send reply |
| `expiration` | Per-message TTL (in ms, as string!) |
| `priority` | Priority queues |
| `deliveryMode` | 1 = transient (memory), 2 = persistent (disk) |
| `contentType` | Hints to consumer how to deserialize |

### Persistent Messages

```java
props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
```

→ Stored on disk → survives broker restart.

⚠️ Persistence requires **durable queue** + **persistent message** — both.

---

## Acknowledgments

**Matlab:** Consumer **confirms** message processed; broker can then delete it.

### Modes

| Mode | When to Ack |
|------|-------------|
| `NONE` (auto-ack) | Broker assumes ack on delivery — fastest, riskiest |
| `AUTO` | Spring acks after listener returns successfully |
| `MANUAL` | Listener calls `channel.basicAck()` explicitly |

### Without Ack

Message **not deleted** on consumer crash → re-delivered to another consumer.

### With Auto-Ack (`NONE`)

```
Consumer receives → broker deletes
Consumer crashes during processing → MESSAGE LOST
```

⚠️ Only for non-critical (logs, metrics).

### With Manual Ack (Recommended)

```java
@RabbitListener(queues = "orders.queue", ackMode = "MANUAL")
public void handle(Message msg, Channel channel,
                   @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
    try {
        process(msg);
        channel.basicAck(tag, false);    // tag, multiple
    } catch (PoisonException e) {
        channel.basicReject(tag, false);  // false = don't requeue (DLX)
    } catch (TransientException e) {
        channel.basicNack(tag, false, true);  // requeue=true → retry
    }
}
```

### `basicAck` vs `basicNack` vs `basicReject`

| Method | Effect |
|--------|--------|
| `basicAck(tag, multiple)` | Confirm received; if `multiple=true`, all up to tag |
| `basicNack(tag, multiple, requeue)` | Negative — requeue or DLX |
| `basicReject(tag, requeue)` | Like nack but single message only |

### Multiple Ack

```java
channel.basicAck(tag, true);   // ack all messages with tag <= this one
```

→ More efficient for batch processing.

---

## Prefetch Count (QoS)

**Matlab:** How many **unacknowledged** messages broker pushes to a consumer at once.

```yaml
# Default Spring AMQP
prefetch: 250
```

### Why?

```
Default (no QoS):
  Broker pushes ALL messages to first consumer → it gets overwhelmed
  Other consumers idle

With prefetch=10:
  Broker pushes max 10 unacked at a time per consumer
  Backpressure → fair distribution
```

### Set in Spring

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        prefetch: 10        # tune based on processing speed
```

### Tuning

| Prefetch | When |
|----------|------|
| **1** | Slow processing — strictly fair distribution |
| **10-50** | Most cases — balance throughput + fairness |
| **100+** | Fast processing, high throughput |
| **0** (unlimited) | ❌ Avoid (defeats purpose) |

### Math

```
prefetch = (max desired messages in flight) / (number of consumers per channel)
```

For prefetch=10 with 1 consumer per channel:
- Up to 10 messages "checked out"
- Consumer processes → ack → broker sends another

### Per-Channel vs Per-Consumer

```
basic.qos(prefetchCount, global)
  global=false: per consumer (Spring default)
  global=true:  per channel (shared)
```

---

## Message TTL & Queue TTL

### Per-Message TTL

```java
MessageProperties props = new MessageProperties();
props.setExpiration("60000");   // 60s — ⚠️ as STRING
```

→ Message expires after TTL, removed (or sent to DLX).

### Per-Queue TTL (All Messages in Queue)

```java
QueueBuilder.durable("temp-queue")
    .withArgument("x-message-ttl", 60000)
    .build();
```

### Queue TTL (Queue Itself Auto-Delete)

```java
QueueBuilder.nonDurable("temp-queue")
    .withArgument("x-expires", 1800000)    // queue deleted if unused 30 min
    .build();
```

→ Useful for ephemeral queues (RPC replies, temporary subscriptions).

### Why TTL?

- **Stale data** — events past relevance
- **Backpressure** — drop old when overwhelmed
- **Cleanup** — temporary state expires

---

## Dead Letter Exchange (DLX)

**Matlab:** When a message can't be processed, route it to a **separate exchange** for inspection / retry.

### When Messages Become "Dead"

1. Rejected with `requeue=false` (`basicReject` / `basicNack`)
2. Expired (TTL hit)
3. Queue length limit exceeded (with `x-overflow=reject-publish-dlx`)

### Setup

```java
@Bean
public DirectExchange ordersDlx() {
    return new DirectExchange("orders.dlx");
}

@Bean
public Queue ordersDlq() {
    return QueueBuilder.durable("orders.dlq").build();
}

@Bean
public Binding dlqBinding() {
    return BindingBuilder.bind(ordersDlq()).to(ordersDlx()).with("orders.dead");
}

@Bean
public Queue ordersQueue() {
    return QueueBuilder.durable("orders.queue")
        .withArgument("x-dead-letter-exchange", "orders.dlx")
        .withArgument("x-dead-letter-routing-key", "orders.dead")
        .build();
}
```

### Flow

```
Message → orders.queue → fails N times → rejected (requeue=false)
                  ↓
        → routed to orders.dlx with key "orders.dead"
                  ↓
        → orders.dlq (for inspection)
```

### DLQ Headers

Dead letter messages have `x-death` headers with full history:

```json
[
  {
    "exchange": "orders.exchange",
    "queue": "orders.queue",
    "reason": "rejected",
    "time": "2024-05-08 10:30:00",
    "count": 3
  }
]
```

→ Helpful for diagnosis.

### DLQ Listener

```java
@RabbitListener(queues = "orders.dlq")
public void handleDlq(Message msg) {
    log.error("Dead letter: {}", new String(msg.getBody()));
    // Persist for review, alert ops, etc.
}
```

### Retry Pattern with DLX

```
Main Queue → fails → DLX → retry queue (x-message-ttl=60s, DLX back to main)
                                       ↓ TTL expires
                                       → DLX → main queue → retry
```

→ Spring AMQP `RetryInterceptor` does this elegantly (covered in `02-Spring-AMQP.md`).

---

## Lazy Queues, Quorum Queues, Streams

### Classic Mirrored Queues (Deprecated)

Old HA pattern — messages mirrored to multiple nodes. Deprecated in RabbitMQ 3.10+.

### Quorum Queues (Recommended for HA)

```java
QueueBuilder.durable("orders.queue")
    .quorum()
    .build();
```

- Built on **Raft consensus**
- Replicated across nodes (typically 3-5)
- Stronger guarantees, simpler to operate
- ❌ Slower than classic queues
- ❌ Doesn't support some features (priority, etc.)

### Lazy Queues

```java
QueueBuilder.durable("backlog.queue")
    .withArgument("x-queue-mode", "lazy")
    .build();
```

→ Messages **paged to disk immediately**, not kept in memory.
→ For large backlogs (millions of messages) without OOM risk.
→ Slower throughput.

### Streams (Kafka-Like in RabbitMQ 3.9+)

```java
new Queue("orders.stream", true, false, false,
    Map.of("x-queue-type", "stream"));
```

- **Append-only log** — like Kafka
- Replayable
- High throughput
- Multiple consumers can read same data

→ For event-streaming use cases without leaving RabbitMQ.

→ Cross-ref: `Messaging — Kafka/01-Kafka-Fundamentals.md` for comparison.

---

## Clustering & High Availability

### Cluster Setup

```
Node 1 (broker1) ←→ Node 2 (broker2) ←→ Node 3 (broker3)
              \         |          /
               └────[Erlang cluster]────┘
```

- Erlang clustering for control plane
- Quorum queues for data replication
- All nodes know about all exchanges, queues, bindings

### Node Types

- **Disc nodes** — persist metadata to disk
- **RAM nodes** — metadata in memory only (faster, less durable)

→ Production: all disc nodes.

### Client Connection

```yaml
spring:
  rabbitmq:
    addresses: broker1:5672,broker2:5672,broker3:5672
```

→ Spring picks one; reconnects on failure.

### Behind Load Balancer

Or put HAProxy/nginx in front, single endpoint:

```yaml
addresses: rabbitmq.internal:5672
```

### Federation & Shovel

For cross-cluster / cross-region replication:

- **Shovel** — pull/push messages between clusters
- **Federation** — broker-level transparent routing

---

## Common Pitfalls

### 1. Confusing Exchanges and Queues

Producer publishes to **exchange**, not queue. Default exchange = "", binding key = queue name.

### 2. Auto-Ack with Critical Data

Crash during processing = message lost. Use manual ack.

### 3. Forgetting Persistence

Durable queue + persistent message = both required for surviving broker restart.

### 4. Prefetch Too High

One slow consumer hogs all unacked messages → others idle.

### 5. Prefetch Too Low

Round-trips dominate → throughput killed.

### 6. Channel Per Message

Channels are cheap, but creating per message wastes resources. Reuse.

### 7. No DLX → Stuck Messages

Failures requeued forever, blocking consumers.

### 8. TTL as int (Should Be String!)

```java
props.setExpiration("60000");   // ✅ string
props.setExpiration(60000);     // ❌ method doesn't exist
```

### 9. Headers Exchange Performance

Slower than direct/topic. Don't use unless necessary.

### 10. Mirrored Queues in Modern RabbitMQ

Deprecated. Use Quorum queues for HA.

### 11. Connection per Operation

Connections expensive. Use connection pool.

### 12. Wildcards Mismatch

```
* → exactly one word
# → zero or more words
```

`order.*` matches `order.placed`, NOT `order.placed.priority`. Use `order.#` for nested.

### 13. Big Messages

RabbitMQ not optimized for huge messages. Default `frame_max` ~128KB. For large payloads, use S3 + send pointer.

### 14. Unmonitored Queue Length

Queue grows silently → memory pressure → broker degrades. Monitor + alert.

---

## Summary Cheat Sheet

| Concept | Quick Note |
|---------|-----------|
| **Connection** | TCP — expensive, share |
| **Channel** | Lightweight session — one per thread |
| **vhost** | Logical broker isolation |
| **Exchange** | Routes messages to queues |
| **Direct** | Exact key match |
| **Topic** | Pattern with `*` (1 word) and `#` (0+ words) |
| **Fanout** | Broadcast to all bound queues |
| **Headers** | Match on headers (`x-match: all/any`) |
| **Queue** | Buffer of messages |
| **Binding** | Exchange ↔ Queue with routing key |
| **Routing key** | String label on publish |
| **Manual Ack** | Explicit `basicAck()` |
| **Prefetch** | Max unacked per consumer |
| **TTL** | Message / queue lifetime |
| **DLX** | Failed messages → separate exchange |
| **Quorum Queue** | Modern HA queue (Raft) |
| **Lazy Queue** | Paged to disk for huge backlogs |
| **Stream** | Kafka-like append log |

| Property | Common Value |
|----------|-------------|
| `durable` | `true` (production) |
| `delivery-mode` | 2 (persistent) |
| `prefetch` | 10-50 |
| `ackMode` | MANUAL (Spring) |
| `x-dead-letter-exchange` | DLX name |
| `x-message-ttl` | as integer ms |
| Per-message expiration | as String ms |

| ✅ Do | ❌ Don't |
|-------|---------|
| Manual ack after process | Auto-ack for critical data |
| Durable + persistent for survival | One without other |
| Set DLX on every queue | Infinite requeue |
| Tune prefetch | Default 250 always |
| Quorum queues for HA | Mirrored (deprecated) |
| Lazy queues for backlogs | OOM on million-message queue |
| Reuse connection + channels | New connection per message |
| Topic exchange for events | Direct for everything |

---

## Practice

1. Run RabbitMQ locally (Docker) with management UI on http://localhost:15672.
2. Create a topic exchange + 3 queues with different binding patterns; publish messages with various routing keys.
3. Compare direct vs topic vs fanout for a notification system.
4. Configure DLX + DLQ; reject a message; verify it's routed to DLQ.
5. Set per-message TTL to 5s; verify message expires + goes to DLX.
6. Use prefetch=1 vs prefetch=100; compare throughput.
7. Convert classic queue to quorum queue; test with broker node failure.
8. Try lazy queue with 100K messages; observe memory usage.
9. Set up streams; consume same data from multiple groups.
10. Write a producer + consumer with manual ack + retry logic.

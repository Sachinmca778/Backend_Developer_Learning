# Messaging — RabbitMQ - Top 10 Interview Questions & Answers

> Covering: RabbitMQ concepts, AMQP, Exchanges (Direct/Topic/Fanout/Headers), Queues, Acknowledgments, Prefetch, DLX, Spring AMQP, and RabbitMQ vs Kafka

---

## Q1: RabbitMQ kya hai aur AMQP protocol kya hai?

**Answer:**

RabbitMQ ek **open-source message broker** hai jo AMQP (Advanced Message Queuing Protocol) implement karta hai. Producers se consumers tak messages route karta hai rich routing capabilities ke saath.

### Basic Flow:

```
Producer ──▶ Exchange ──(routing key)──▶ Queue ──▶ Consumer
                  │
                  └─▶ Queue ──▶ Consumer
```

→ Producer consumers ko nahi jaanta; sirf exchange par publish karta hai. Routing decide karti hai messages kahan jaayenge.

### AMQP:

Wire-level protocol jo define karta hai clients broker se kaise baat karte hain. Language-agnostic, standard.

- **AMQP 0-9-1** — RabbitMQ default, strong routing model
- Plugins se MQTT, STOMP, HTTP bhi support

### Key Capabilities:

- Multiple exchange types (flexible routing)
- Reliable delivery (acks, persistence, publisher confirms)
- Pub/Sub (fanout), Work queues, RPC (request-reply)
- DLX for failed messages

**Short Interview Answer:**

RabbitMQ AMQP protocol par based message broker hai. Producer exchange par message bhejta hai, exchange routing rules ke basis par messages ko queues mein route karti hai, consumers queue se read karte hain. Task queues, RPC, aur complex routing ke liye best.

---

## Q2: Connection aur Channel mein kya difference hai?

**Answer:**

| | Connection | Channel |
|--|-----------|---------|
| **Kya hai** | TCP connection to broker | Logical session within connection |
| **Cost** | Expensive to open | Cheap, multiplexed |
| **Thread safety** | — | Not thread-safe (one per thread) |

### Structure:

```
Connection (1 TCP)
    ├── Channel 1
    ├── Channel 2
    └── Channel 3
```

### Best Practices:

- **One connection per app** (ya small pool)
- **One channel per thread** (channels thread-safe nahi hain)
- Channels reuse karo many operations ke liye
- Leak mat karo (done hone par close)

**Important:**

Connection TCP-level hai aur expensive hai — isliye reuse karo. Channels lightweight sessions hain jo ek connection ke andar multiplex hote hain. Channels thread-safe nahi hote, isliye har thread ka apna channel hona chahiye. Spring AMQP connection pooling handle karta hai.

---

## Q3: RabbitMQ ke exchange types kaunse hain?

**Answer:**

| Type | Routing Logic | Use Case |
|------|--------------|----------|
| **Direct** | routing key == binding key (exact) | Category-based routing |
| **Topic** | pattern match with wildcards | Pub/sub with filtering |
| **Fanout** | broadcast to all queues | Pub/sub broadcast |
| **Headers** | match on headers | Multi-criteria routing |

### Direct — Exact Match:

```java
BindingBuilder.bind(placedQueue).to(orderExchange).with("PLACED");
```

→ routing key `PLACED` → sirf `PLACED` binding wali queues.

### Topic — Wildcards:

```
*  → exactly one word
#  → zero or more words

order.*   → order.placed, order.cancelled (NOT order.placed.priority)
order.#   → order.placed, order.placed.priority, order.foo.bar
#         → everything
```

### Fanout — Broadcast:

```java
// routing key ignored — har bound queue ko copy milti hai
BindingBuilder.bind(emailQueue()).to(notificationsExchange());
```

### Headers — Match on Headers:

```java
.whereAll(Map.of("format", "pdf", "type", "report")).match();
// x-match: all → sab headers match; any → koi ek match
```

**Short Interview Answer:**

4 types — Direct (exact routing key match), Topic (wildcard patterns `*` aur `#`), Fanout (broadcast to all, routing key ignore), Headers (message headers par match). Event-driven systems ke liye Topic exchange most flexible hai.

---

## Q4: Acknowledgments kaise kaam karte hain? Auto-ack vs Manual ack?

**Answer:**

Consumer confirm karta hai ki message process ho gaya; tabhi broker use delete karta hai.

### Modes:

| Mode | When to Ack |
|------|-------------|
| `NONE` (auto-ack) | Delivery par hi ack — fastest, riskiest |
| `AUTO` | Spring listener successfully return karne par ack |
| `MANUAL` | Listener explicitly `channel.basicAck()` call kare |

### Auto-Ack Risk:

```
Consumer receives → broker deletes → consumer crashes during processing → MESSAGE LOST
```

⚠️ Sirf non-critical data ke liye (logs, metrics).

### Manual Ack (Recommended):

```java
@RabbitListener(queues = "orders.queue", ackMode = "MANUAL")
public void handle(Message msg, Channel channel,
                   @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
    try {
        process(msg);
        channel.basicAck(tag, false);           // confirm
    } catch (PoisonException e) {
        channel.basicReject(tag, false);        // false = requeue nahi (DLX)
    } catch (TransientException e) {
        channel.basicNack(tag, false, true);    // requeue=true → retry
    }
}
```

### Methods:

| Method | Effect |
|--------|--------|
| `basicAck(tag, multiple)` | Confirm received |
| `basicNack(tag, multiple, requeue)` | Negative — requeue ya DLX |
| `basicReject(tag, requeue)` | Nack jaisa but single message |

**Important:**

Critical data ke liye hamesha manual ack use karo — processing successful hone ke baad hi ack karo. Auto-ack mein consumer crash hone par message lost ho jaata hai. Poison messages ko `requeue=false` ke saath reject karo taaki DLX par jaayen, infinite loop na bane.

---

## Q5: Prefetch count (QoS) kya hai aur kyun important hai?

**Answer:**

Prefetch count decide karta hai ki broker ek consumer ko ek baar mein kitne **unacknowledged** messages push karega.

### Problem Without QoS:

```
Default (no QoS):
  Broker saare messages first consumer ko push kar deta hai → overwhelm
  Baaki consumers idle
```

### With Prefetch:

```
prefetch=10:
  Broker max 10 unacked messages per consumer
  Backpressure → fair distribution
```

### Configure:

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        prefetch: 10
```

### Tuning:

| Prefetch | When |
|----------|------|
| **1** | Slow processing — strictly fair |
| **10-50** | Most cases — balance |
| **100+** | Fast processing, high throughput |
| **0** (unlimited) | ❌ Avoid |

**Short Interview Answer:**

Prefetch = broker ek consumer ko kitne unacked messages ek saath deta hai. Bina QoS ke ek consumer saare messages le leta hai aur baaki idle rehte hain. Prefetch se backpressure aur fair distribution milti hai. Slow processing → low prefetch (1), fast → high (100+).

---

## Q6: Dead Letter Exchange (DLX) kya hai aur kaise setup karte hain?

**Answer:**

Jab message process nahi ho pata, use ek **separate exchange** (DLX) par route kar dete hain inspection/retry ke liye.

### Message "Dead" Kab Hota Hai:

1. Rejected with `requeue=false`
2. TTL expire ho gaya
3. Queue length limit exceed

### Setup:

```java
@Bean
public Queue ordersQueue() {
    return QueueBuilder.durable("orders.queue")
        .withArgument("x-dead-letter-exchange", "orders.dlx")
        .withArgument("x-dead-letter-routing-key", "orders.dead")
        .build();
}

@Bean
public Queue ordersDlq() {
    return QueueBuilder.durable("orders.dlq").build();
}
```

### Flow:

```
Message → orders.queue → fails → rejected (requeue=false)
                  ↓
        → orders.dlx (with key "orders.dead")
                  ↓
        → orders.dlq (inspection ke liye)
```

### DLQ Listener:

```java
@RabbitListener(queues = "orders.dlq")
public void handleDlq(Message msg) {
    log.error("Dead letter: {}", new String(msg.getBody()));
    // review ke liye persist, ops alert, etc.
}
```

**Important:**

DLX poison messages ko handle karne ka standard tareeka hai — jo messages baar-baar fail hote hain wo main queue block na karen. Dead letter messages ke `x-death` headers mein full failure history hoti hai (diagnosis ke liye). Retry pattern ke liye TTL wali retry queue + DLX loop bana sakte ho.

---

## Q7: Spring AMQP mein message kaise produce aur consume karte hain?

**Answer:**

### Setup:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

### Configuration (Exchange, Queue, Binding):

```java
@Configuration
public class RabbitConfig {
    @Bean TopicExchange ordersExchange() { return new TopicExchange("orders.exchange"); }
    @Bean Queue ordersQueue() { return QueueBuilder.durable("orders.queue").build(); }
    @Bean Binding binding() {
        return BindingBuilder.bind(ordersQueue()).to(ordersExchange()).with("order.*");
    }
    // JSON converter
    @Bean MessageConverter converter() { return new Jackson2JsonMessageConverter(); }
}
```

### Produce — RabbitTemplate:

```java
@Service
public class OrderProducer {
    private final RabbitTemplate rabbitTemplate;

    public void send(OrderEvent event) {
        rabbitTemplate.convertAndSend("orders.exchange", "order.placed", event);
    }
}
```

### Consume — @RabbitListener:

```java
@Component
public class OrderConsumer {
    @RabbitListener(queues = "orders.queue")
    public void consume(OrderEvent event) {
        log.info("Received: {}", event);
        process(event);
    }
}
```

**Short Interview Answer:**

`RabbitTemplate` se `convertAndSend(exchange, routingKey, message)` karke produce karo, `@RabbitListener(queues=...)` se consume karo. Exchange, queue, binding ko `@Bean` methods se declare karo. JSON ke liye `Jackson2JsonMessageConverter` register karo.

---

## Q8: Message durability aur persistence kaise ensure karte hain?

**Answer:**

Broker restart ke baad messages survive karne ke liye **teen cheezein** chahiye:

### 1. Durable Exchange:

```java
new TopicExchange("orders.exchange", true, false);  // durable=true
```

### 2. Durable Queue:

```java
QueueBuilder.durable("orders.queue").build();
```

### 3. Persistent Message:

```java
props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);  // 2 = disk
```

→ Spring AMQP mein `RabbitTemplate` default persistent messages bhejta hai.

### Publisher Confirms (Extra Reliability):

```yaml
spring:
  rabbitmq:
    publisher-confirm-type: correlated
    publisher-returns: true
```

```java
rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
    if (!ack) log.error("Message not confirmed: {}", cause);
});
```

**Important:**

Durability ke liye durable queue + persistent message **dono** chahiye — sirf ek se kaam nahi chalega. Publisher confirms ensure karte hain ki broker ne message actually receive kiya. Persistence throughput thoda kam karti hai (disk writes) but data safety deti hai.

---

## Q9: RabbitMQ mein high availability kaise achieve karte hain (Quorum Queues, Lazy Queues)?

**Answer:**

### Quorum Queues (Recommended for HA):

```java
QueueBuilder.durable("orders.queue").quorum().build();
```

- **Raft consensus** par built
- Multiple nodes (3-5) par replicated
- Strong guarantees, simpler to operate
- ❌ Classic queues se slow, kuch features nahi (priority)

→ Purane **mirrored queues deprecated** hain (RabbitMQ 3.10+).

### Lazy Queues:

```java
QueueBuilder.durable("backlog.queue")
    .withArgument("x-queue-mode", "lazy")
    .build();
```

→ Messages seedhe **disk par page** hote hain, memory mein nahi. Large backlogs (millions) OOM ke bina. Slower throughput.

### Streams (Kafka-like):

```java
new Queue("orders.stream", true, false, false,
    Map.of("x-queue-type", "stream"));
```

→ Append-only log, replayable, high throughput, multiple consumers same data.

### Clustering:

```yaml
spring:
  rabbitmq:
    addresses: broker1:5672,broker2:5672,broker3:5672
```

**Short Interview Answer:**

Production HA ke liye Quorum queues use karo (Raft-based, replicated across nodes) — mirrored queues ab deprecated hain. Large backlogs ke liye lazy queues (disk paging, OOM avoid). Kafka-like event streaming ke liye RabbitMQ Streams. Cluster mein multiple broker addresses do failover ke liye.

---

## Q10: RabbitMQ vs Kafka — kab kya use karein?

**Answer:**

### Fundamental Difference:

| | RabbitMQ | Kafka |
|--|----------|-------|
| **Model** | Smart broker, dumb consumer | Dumb broker, smart consumer |
| **Routing** | Broker (exchanges) | Producer (partition choice) |
| **After consume** | Message deleted | Persisted (retention) |
| **Replay** | Hard (re-publish) | Native (seek offset) |
| **Delivery** | Push | Pull |
| **Throughput** | 10K-100K msg/sec | 1M+ msg/sec |
| **Ordering** | Per queue | Per partition |

### Kab RabbitMQ:

- **Task queues** (background jobs, work distribution)
- **RPC** (request-reply)
- **Complex routing** (topic patterns, headers)
- Pub/sub with limited consumers
- Simple "send a message"

### Kab Kafka:

- **Event streaming** (replay, history)
- **High-throughput logs/metrics**
- **Event sourcing / CDC**
- **Stream processing** (Streams, Flink)
- Many consumers same data read karte hain

**Important:**

RabbitMQ "smart broker" hai — routing logic broker mein, message ack ke baad delete. Kafka "smart consumer" hai — broker sirf log hai, consumer apna offset track karta hai, messages retention tak rehte hain aur replay ho sakte hain. Task queue/RPC → RabbitMQ, event streaming/high-throughput → Kafka. Aksar dono ek hi architecture mein use hote hain.

**Short Interview Answer:**

RabbitMQ task queues, RPC, aur complex routing ke liye best (smart broker, message deleted after ack). Kafka event streaming, high-throughput, aur replay ke liye best (durable log, consumer tracks offset). Choose based on use case — "hot hai" isliye Kafka mat choose karo.

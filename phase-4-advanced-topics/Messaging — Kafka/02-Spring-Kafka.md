# Spring Kafka

## Status: Not Started

---

## Table of Contents

1. [Spring Kafka Kya Hai?](#spring-kafka-kya-hai)
2. [Setup](#setup)
3. [Producing Messages — KafkaTemplate](#producing-messages--kafkatemplate)
4. [Consuming Messages — @KafkaListener](#consuming-messages--kafkalistener)
5. [@EnableKafka & Configuration](#enablekafka--configuration)
6. [ProducerFactory & ConsumerFactory](#producerfactory--consumerfactory)
7. [KafkaListenerContainerFactory](#kafkalistenercontainerfactory)
8. [Acknowledgment Modes](#acknowledgment-modes)
9. [Manual Acknowledgment](#manual-acknowledgment)
10. [Batch Listeners](#batch-listeners)
11. [Error Handling](#error-handling)
12. [Reply Templates (Request-Reply)](#reply-templates-request-reply)
13. [Transactional Producer + Consumer](#transactional-producer--consumer)
14. [Testing](#testing)
15. [Observability](#observability)
16. [Common Pitfalls](#common-pitfalls)
17. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Spring Kafka Kya Hai?

**Matlab:** Spring's idiomatic abstraction over the Apache Kafka client — declarative producers (`KafkaTemplate`), declarative consumers (`@KafkaListener`), error handling, transactions, testing utilities.

```
KafkaTemplate     → producer wrapper
@KafkaListener    → consumer annotation
KafkaAdmin        → topic management beans
```

### Why Spring Kafka?

| Without | With Spring Kafka |
|---------|-------------------|
| Manual Producer/Consumer instantiation | Auto-configured via Spring Boot |
| Manual polling loops | `@KafkaListener` annotation |
| Manual error handling | `DefaultErrorHandler`, DLT |
| Manual transactions | `@Transactional` |
| Boilerplate config | YAML-driven |

---

## Setup

### Maven Dependencies

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.2.5</version>
</parent>

<dependencies>
  <dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
  </dependency>
  
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
  </dependency>
  
  <!-- Testing -->
  <dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

### Basic `application.yml`

```yaml
spring:
  kafka:
    bootstrap-servers: kafka:9092
    
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
    
    consumer:
      group-id: order-processor
      auto-offset-reset: earliest
      enable-auto-commit: false
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      properties:
        max.poll.records: 100
        max.poll.interval.ms: 300000
    
    listener:
      ack-mode: manual_immediate
      concurrency: 3
```

### Main App Class

```java
@SpringBootApplication
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
```

→ Spring Boot auto-configures `KafkaTemplate`, `ProducerFactory`, `ConsumerFactory`.

---

## Producing Messages — KafkaTemplate

### Basic Send

```java
@Service
@RequiredArgsConstructor
public class OrderEventPublisher {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    public void publish(Order order) {
        String json = toJson(order);
        kafkaTemplate.send("orders", order.getUserId().toString(), json);
    }
}
```

### Async with Callback (Modern — CompletableFuture)

```java
public CompletableFuture<SendResult<String, String>> publish(Order order) {
    String json = toJson(order);
    
    CompletableFuture<SendResult<String, String>> future =
        kafkaTemplate.send("orders", order.getUserId().toString(), json);
    
    future.whenComplete((result, ex) -> {
        if (ex != null) {
            log.error("Send failed for order {}", order.getId(), ex);
        } else {
            log.info("Sent order {} to {}-{} @ {}",
                order.getId(),
                result.getRecordMetadata().topic(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
        }
    });
    
    return future;
}
```

> Note: In older Spring Kafka, `send()` returned `ListenableFuture` (deprecated in Spring Framework 6, replaced by `CompletableFuture`).

### Sync (Block until ack)

```java
SendResult<String, String> result = 
    kafkaTemplate.send("orders", key, value).get(5, TimeUnit.SECONDS);
```

### ProducerRecord (Full Control)

```java
ProducerRecord<String, String> record = new ProducerRecord<>(
    "orders",
    null,                              // partition (null = let producer pick)
    System.currentTimeMillis(),         // timestamp
    order.getUserId().toString(),       // key
    json,                               // value
    List.of(                            // headers
        new RecordHeader("trace-id", traceId.getBytes()),
        new RecordHeader("source", "order-service".getBytes())
    )
);

kafkaTemplate.send(record);
```

### Using Default Topic

```yaml
spring:
  kafka:
    template:
      default-topic: orders
```

```java
kafkaTemplate.sendDefault(key, value);   // sends to default topic
```

### Send to Multiple Topics — Multiple Templates

```java
@Configuration
public class KafkaProducerConfig {
    
    @Bean
    public KafkaTemplate<String, OrderEvent> orderEventTemplate(
            ProducerFactory<String, OrderEvent> factory) {
        return new KafkaTemplate<>(factory);
    }
    
    @Bean
    public KafkaTemplate<String, NotificationEvent> notificationTemplate(
            ProducerFactory<String, NotificationEvent> factory) {
        return new KafkaTemplate<>(factory);
    }
}
```

---

## Consuming Messages — @KafkaListener

### Simplest Listener

```java
@Component
@Slf4j
public class OrderEventListener {
    
    @KafkaListener(topics = "orders", groupId = "order-processor")
    public void handle(String message) {
        log.info("Received: {}", message);
        // Process
    }
}
```

→ Auto-deserializes; commits offset (depends on `ack-mode`).

### Access ConsumerRecord

```java
@KafkaListener(topics = "orders", groupId = "order-processor")
public void handle(ConsumerRecord<String, String> record) {
    log.info("Topic={}, Partition={}, Offset={}, Key={}, Value={}",
        record.topic(), record.partition(), record.offset(),
        record.key(), record.value());
}
```

### Access Headers

```java
@KafkaListener(topics = "orders", groupId = "order-processor")
public void handle(
        @Payload String value,
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        @Header(value = "trace-id", required = false) String traceId) {
    
    log.info("traceId={}, key={}, value={}", traceId, key, value);
}
```

### Multiple Topics

```java
@KafkaListener(topics = {"orders", "order-updates"}, groupId = "order-processor")
public void handle(String message) { ... }
```

### Topic Pattern (Wildcard)

```java
@KafkaListener(topicPattern = "orders\\..*", groupId = "order-processor")
public void handle(String message) { ... }
```

### Specific Partitions

```java
@KafkaListener(
    topicPartitions = {
        @TopicPartition(topic = "orders", partitions = {"0", "1"})
    },
    groupId = "order-processor")
public void handle(String message) { ... }
```

### From Specific Offset

```java
@KafkaListener(
    topicPartitions = {
        @TopicPartition(topic = "orders",
            partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "100"))
    },
    groupId = "order-processor")
public void handle(String message) { ... }
```

### Concurrency (Multiple Threads)

```java
@KafkaListener(topics = "orders", groupId = "order-processor", concurrency = "3")
public void handle(String message) { ... }
```

→ Creates 3 consumer threads (each handles subset of partitions).

```yaml
spring:
  kafka:
    listener:
      concurrency: 3       # global default
```

⚠️ Concurrency ≤ partition count. Otherwise extra threads idle.

### Container ID + Auto-Start Control

```java
@KafkaListener(
    id = "orderListener",
    topics = "orders",
    groupId = "order-processor",
    autoStartup = "false")
public void handle(String message) { ... }
```

```java
@Autowired KafkaListenerEndpointRegistry registry;

public void start() {
    registry.getListenerContainer("orderListener").start();
}

public void stop() {
    registry.getListenerContainer("orderListener").stop();
}
```

→ Useful for ad-hoc / cron-like consumption.

---

## @EnableKafka & Configuration

### When to Use `@EnableKafka`

Spring Boot auto-includes it. **Only need explicitly** if:
- Not using Spring Boot
- Customizing `KafkaListenerContainerFactory`

```java
@Configuration
@EnableKafka
public class KafkaConfig { ... }
```

### Custom Configuration

If YAML isn't enough, define beans:

```java
@Configuration
public class KafkaConfig {
    
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        
        return new DefaultKafkaProducerFactory<>(props);
    }
    
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(
            ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
    
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-processor");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        return new DefaultKafkaConsumerFactory<>(props);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
            kafkaListenerContainerFactory(ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }
    
    @Bean
    public DefaultErrorHandler errorHandler() {
        return new DefaultErrorHandler(
            new FixedBackOff(1000L, 2L));    // 2 retries with 1s delay
    }
}
```

---

## ProducerFactory & ConsumerFactory

### ProducerFactory

Creates `KafkaProducer` instances. Spring auto-creates from YAML.

### Per-Object-Type Factories

```java
@Bean
public ProducerFactory<String, OrderEvent> orderProducerFactory() {
    Map<String, Object> props = baseProducerConfig();
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    return new DefaultKafkaProducerFactory<>(props);
}

@Bean
public KafkaTemplate<String, OrderEvent> orderKafkaTemplate(
        ProducerFactory<String, OrderEvent> orderProducerFactory) {
    return new KafkaTemplate<>(orderProducerFactory);
}
```

### Different Consumer Configs per Listener

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, OrderEvent>
        orderListenerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, OrderEvent> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(orderConsumerFactory());
    return factory;
}
```

```java
@KafkaListener(topics = "orders", containerFactory = "orderListenerFactory")
public void handle(OrderEvent event) { ... }
```

---

## KafkaListenerContainerFactory

The factory that creates listener containers. Two types:

### 1. ConcurrentKafkaListenerContainerFactory (Most Common)

Multiple consumer threads:

```java
factory.setConcurrency(3);   // 3 KafkaConsumer instances
```

→ Each consumer gets a subset of partitions.

### 2. KafkaListenerContainerFactory (Single Threaded)

Less commonly used directly.

### Container Properties

```java
ContainerProperties props = factory.getContainerProperties();

props.setAckMode(AckMode.MANUAL);                    // ack mode
props.setPollTimeout(3000);                          // ms
props.setIdleEventInterval(60000L);                  // emit ListenerContainerIdleEvent
props.setIdleBetweenPolls(1000);                      // ms idle between polls
props.setMissingTopicsFatal(false);                  // don't crash if topic missing
```

---

## Acknowledgment Modes

**Matlab:** When does the consumer commit offsets?

### Modes

| Mode | When Committed |
|------|---------------|
| `RECORD` | After each individual record processed |
| `BATCH` | After all records in a poll() are processed (default) |
| `TIME` | Every `ackTime` milliseconds |
| `COUNT` | Every `ackCount` records |
| `COUNT_TIME` | Whichever (count or time) hits first |
| `MANUAL` | App calls `acknowledge()` — committed at next `poll()` |
| `MANUAL_IMMEDIATE` | App calls `acknowledge()` — committed **immediately** |

### Set in YAML

```yaml
spring:
  kafka:
    listener:
      ack-mode: manual_immediate
```

### Set in Java

```java
factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
```

### Default Behavior

Spring Kafka default is `BATCH` (commits after batch processing). With `enable.auto.commit=false` in client (recommended), Spring controls commits.

---

## Manual Acknowledgment

### Why Manual?

You want to commit **only after** successful processing → at-least-once semantics.

```java
@KafkaListener(topics = "orders", groupId = "order-processor")
public void handle(ConsumerRecord<String, String> record, Acknowledgment ack) {
    try {
        process(record.value());
        ack.acknowledge();        // commit offset
    } catch (Exception e) {
        log.error("Failed to process", e);
        // Don't ack → record will be re-delivered after poll
    }
}
```

### Synchronous Commit

```java
ack.acknowledge();    // returns once committed
```

### Sync vs Async (in Native Client)

When **not using Spring Kafka**:

```java
consumer.commitSync();    // blocks, retries
consumer.commitAsync();   // fire-and-forget, faster
consumer.commitAsync((offsets, ex) -> {
    if (ex != null) log.error("Commit failed", ex);
});
```

→ Spring Kafka abstracts this — `acknowledge()` does what's right based on `ackMode`.

### Important: Don't Skip Ack on Error

```java
@KafkaListener(topics = "orders")
public void handle(String message, Acknowledgment ack) {
    try {
        process(message);
        ack.acknowledge();
    } catch (PoisonMessageException e) {
        log.error("Poison message — sending to DLT", e);
        sendToDLT(message);
        ack.acknowledge();    // ack to skip past it!
    } catch (TransientException e) {
        log.warn("Transient error — will retry", e);
        // Don't ack → re-delivered
    }
}
```

→ See `06-Kafka-Patterns.md` for proper error handling patterns.

---

## Batch Listeners

Process records in **batches** instead of one-by-one.

### Setup

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, String> batchFactory() {
    ConcurrentKafkaListenerContainerFactory<String, String> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    factory.setBatchListener(true);
    return factory;
}
```

```yaml
spring:
  kafka:
    listener:
      type: batch
```

### Batch Listener Method

```java
@KafkaListener(topics = "orders", containerFactory = "batchFactory")
public void handleBatch(List<ConsumerRecord<String, String>> records,
                        Acknowledgment ack) {
    log.info("Got batch of {} records", records.size());
    
    for (ConsumerRecord<String, String> record : records) {
        process(record.value());
    }
    
    ack.acknowledge();   // commit all in batch
}
```

### Or Just Payloads

```java
@KafkaListener(topics = "orders", containerFactory = "batchFactory")
public void handleBatch(List<String> messages, Acknowledgment ack) {
    bulkInsert(messages);
    ack.acknowledge();
}
```

### Why Batch?

- **Throughput** — bulk DB inserts much faster
- **Efficiency** — fewer round-trips downstream
- **Aggregation** — combine related records

### Batch Size Control

```yaml
spring:
  kafka:
    consumer:
      max-poll-records: 500
      fetch-min-bytes: 1024
      fetch-max-wait: 500
```

→ `max.poll.records` = max batch size returned by poll().

---

## Error Handling

### DefaultErrorHandler (Spring Kafka 2.8+)

Handles exceptions thrown from listeners — retries + dead letter.

### Setup

```java
@Bean
public DefaultErrorHandler errorHandler(KafkaTemplate<String, String> template) {
    DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(template,
            (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
    
    DefaultErrorHandler handler = new DefaultErrorHandler(
        recoverer,
        new FixedBackOff(1000L, 3L));   // 3 retries with 1s delay
    
    // Don't retry on certain exceptions — go straight to DLT
    handler.addNotRetryableExceptions(IllegalArgumentException.class,
                                       JsonParseException.class);
    
    return handler;
}
```

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, String> factory(...) {
    factory.setCommonErrorHandler(errorHandler);
    return factory;
}
```

### Exponential Backoff

```java
ExponentialBackOff backOff = new ExponentialBackOff();
backOff.setInitialInterval(1000);
backOff.setMultiplier(2.0);
backOff.setMaxInterval(10000);
backOff.setMaxElapsedTime(60000);

new DefaultErrorHandler(recoverer, backOff);
```

### Per-Topic DLT

```
orders         → orders.DLT (failed records)
payments       → payments.DLT
```

### Process DLT Manually

```java
@KafkaListener(topics = "orders.DLT", groupId = "dlt-processor")
public void handleDlt(ConsumerRecord<String, String> record) {
    log.error("DLT message: key={}, value={}", record.key(), record.value());
    // Send to alerting / persist for manual review
}
```

→ Deeper patterns in `06-Kafka-Patterns.md`.

---

## Reply Templates (Request-Reply)

Kafka isn't strictly request-reply, but Spring Kafka supports it via `ReplyingKafkaTemplate`.

### Setup

```java
@Bean
public ReplyingKafkaTemplate<String, String, String> replyingTemplate(
        ProducerFactory<String, String> pf,
        ConcurrentMessageListenerContainer<String, String> container) {
    return new ReplyingKafkaTemplate<>(pf, container);
}
```

### Send + Wait for Reply

```java
ProducerRecord<String, String> request = new ProducerRecord<>("requests", "give me data");
RequestReplyFuture<String, String, String> future = 
    replyingTemplate.sendAndReceive(request);

ConsumerRecord<String, String> reply = future.get(5, TimeUnit.SECONDS);
log.info("Got reply: {}", reply.value());
```

### Reply Listener (Other Side)

```java
@KafkaListener(topics = "requests")
@SendTo
public String handle(String request) {
    return "response to: " + request;
}
```

→ Spring auto-includes correlation ID in headers.

⚠️ Use sparingly — Kafka isn't designed for low-latency request-reply (use HTTP/gRPC for that).

---

## Transactional Producer + Consumer

### Producer Side

```yaml
spring:
  kafka:
    producer:
      transaction-id-prefix: tx-
```

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    @Transactional("kafkaTransactionManager")
    public void process(Order order) {
        kafkaTemplate.send("orders", "key1", "data1");
        kafkaTemplate.send("audit", "key2", "data2");
        // Both sent atomically; either both committed or both aborted
    }
}
```

### Consumer Side — Read Committed

```yaml
spring:
  kafka:
    consumer:
      isolation-level: read_committed
```

→ Consumer only reads transactionally-committed records.

### Combine with DB Transaction

For "consume → DB write → produce" — see **transactional outbox** pattern in `06-Kafka-Patterns.md`.

---

## Testing

### EmbeddedKafka

```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"orders"})
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
class OrderEventTest {
    
    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;
    
    @Test
    void should_send_order() throws Exception {
        kafkaTemplate.send("orders", "key1", "value1").get(5, TimeUnit.SECONDS);
        // Assert via consumer
    }
}
```

### Consumer Test (Verify Listener)

```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"orders"})
class OrderListenerTest {
    
    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;
    
    @MockBean
    OrderProcessor processor;
    
    @Test
    void listener_processes_message() throws InterruptedException {
        kafkaTemplate.send("orders", "key1", "{\"id\":1}").get();
        
        // Wait for listener to consume + verify processor called
        Thread.sleep(2000);
        verify(processor).process(any());
    }
}
```

### Testcontainers Kafka (Closer to Production)

```xml
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>kafka</artifactId>
  <scope>test</scope>
</dependency>
```

```java
@SpringBootTest
@Testcontainers
class OrderTest {
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
    
    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
    
    @Test
    void test() { ... }
}
```

→ Real Kafka in Docker. More accurate, slower.

---

## Observability

### Spring Boot Actuator + Kafka Metrics

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  metrics:
    enable:
      kafka: true
```

### Useful Metrics

```
kafka.producer.record.send.total
kafka.producer.record.error.total
kafka.consumer.records.consumed.total
kafka.consumer.records.lag       ← consumer lag per partition
kafka.consumer.commit.latency.avg
```

→ Send to Prometheus / Datadog → alerts.

### Logging

```yaml
logging:
  level:
    org.apache.kafka: WARN
    org.springframework.kafka: INFO
```

⚠️ DEBUG on Kafka is very noisy. Use only when troubleshooting.

### Distributed Tracing

```xml
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
  <groupId>io.opentelemetry.instrumentation</groupId>
  <artifactId>opentelemetry-kafka-clients-2.6</artifactId>
</dependency>
```

→ Auto-injects trace headers; spans across producer → consumer.

---

## Common Pitfalls

### 1. `enable.auto.commit=true` + Manual Ack

Conflicts. Spring sets it to `false` automatically when you use `@KafkaListener`.

### 2. Forgetting to Ack with `MANUAL` Mode

Records re-delivered forever. Set `MANUAL_IMMEDIATE` and call `ack.acknowledge()` always.

### 3. Long Processing → Rebalance

```yaml
spring:
  kafka:
    consumer:
      max-poll-interval-ms: 300000   # 5 min default
      max-poll-records: 500
```

If processing 500 records > 5 min → kicked out → rebalance storm.

→ Lower `max.poll.records` or increase `max.poll.interval.ms` or process async.

### 4. Concurrency > Partitions

```java
@KafkaListener(topics = "orders", concurrency = "10")
// Topic has only 3 partitions → 7 threads idle
```

→ Set `concurrency` ≤ partition count.

### 5. ReplyingKafkaTemplate Misuse

Trying to do RPC over Kafka — it's slower than HTTP/gRPC. Use only for legitimate async req-reply patterns.

### 6. `JsonDeserializer` Trusted Packages

```yaml
spring.kafka.consumer.properties.spring.json.trusted.packages: '*'   # ❌ open
```

→ Restrict to your packages (covered in `03-Serialization.md`).

### 7. Send Method Returns Future — Not Awaited

```java
kafkaTemplate.send(...).get();   // ❌ blocks indefinitely if no timeout
kafkaTemplate.send(...);         // ❌ fire-and-forget — might fail silently
kafkaTemplate.send(...).whenComplete((r, ex) -> { ... });   // ✅ handle outcome
```

### 8. Schema Drift / Deserialization Errors

A poison pill message → listener throws → infinite retry → blocks partition.

→ Configure `DefaultErrorHandler` + DLT.

### 9. Not Using Idempotent Producer

Default in Spring Boot 3+. Verify: `enable.idempotence=true`. Without it, retries can produce duplicates.

### 10. Topic Auto-Creation in Production

```yaml
spring.kafka.admin.fail-fast: true
```

Or pre-create topics in deploy script. Auto-creation often hides config issues.

---

## Summary Cheat Sheet

| Concept | Quick Note |
|---------|-----------|
| **`KafkaTemplate`** | Send messages |
| **`@KafkaListener`** | Consume messages |
| **`@EnableKafka`** | Auto-included in Spring Boot |
| **`ProducerFactory`** | Creates Kafka producers |
| **`ConsumerFactory`** | Creates Kafka consumers |
| **`ConcurrentKafkaListenerContainerFactory`** | Default listener container factory |
| **`AckMode`** | RECORD / BATCH / MANUAL / MANUAL_IMMEDIATE |
| **Concurrency** | # parallel consumers (≤ partitions) |
| **`Acknowledgment.acknowledge()`** | Manual commit |
| **Batch listener** | `setBatchListener(true)`, List<...> param |
| **Error handler** | `DefaultErrorHandler` + DLT |
| **Transaction** | `@Transactional("kafkaTransactionManager")` |
| **Testing** | `@EmbeddedKafka` or Testcontainers |

| Property | Common Value |
|----------|-------------|
| `bootstrap-servers` | broker1:9092,broker2:9092 |
| `producer.acks` | all |
| `producer.retries` | 3 |
| `producer.idempotence` | true |
| `consumer.group-id` | unique per app role |
| `consumer.auto-offset-reset` | earliest / latest |
| `consumer.enable-auto-commit` | false (manual ack) |
| `listener.ack-mode` | manual_immediate |
| `listener.concurrency` | partitions count |

| ✅ Do | ❌ Don't |
|-------|---------|
| Manual ack with MANUAL_IMMEDIATE | Auto-commit + business logic |
| Concurrency ≤ partitions | More threads than partitions |
| DLT for poison messages | Infinite retry loop |
| Idempotent producer (default) | Disable idempotence |
| Pre-create topics | Auto-create in prod |
| Trust specific JSON packages | `spring.json.trusted.packages=*` |
| Handle send() future | Fire-and-forget without callback |

---

## Practice

1. Set up a Spring Boot app with `KafkaTemplate` sending to `orders`.
2. Add `@KafkaListener` consuming from `orders`.
3. Switch to manual acknowledgment (`MANUAL_IMMEDIATE`).
4. Implement batch listener for bulk DB insert.
5. Configure `DefaultErrorHandler` with retries + DLT.
6. Add a separate listener for the DLT to log failed messages.
7. Use `@EmbeddedKafka` for an integration test.
8. Switch to Testcontainers Kafka for closer-to-prod test.
9. Configure transactional producer; send to two topics atomically.
10. Add Micrometer metrics; visualize consumer lag.

# Spring AMQP (Spring Boot + RabbitMQ)

## Status: Not Started

---

## Table of Contents

1. [Spring AMQP Kya Hai?](#spring-amqp-kya-hai)
2. [Setup](#setup)
3. [Configuration via YAML](#configuration-via-yaml)
4. [Declaring Exchanges, Queues, Bindings](#declaring-exchanges-queues-bindings)
5. [RabbitTemplate — Producing](#rabbittemplate--producing)
6. [@RabbitListener — Consuming](#rabbitlistener--consuming)
7. [@EnableRabbit](#enablerabbit)
8. [MessageConverter (Jackson2JsonMessageConverter)](#messageconverter-jackson2jsonmessageconverter)
9. [Acknowledgment Modes](#acknowledgment-modes)
10. [Manual Acknowledgment](#manual-acknowledgment)
11. [Concurrency](#concurrency)
12. [Listener Container Factory](#listener-container-factory)
13. [Error Handling & Retry](#error-handling--retry)
14. [Dead Letter Pattern](#dead-letter-pattern)
15. [Publisher Confirms](#publisher-confirms)
16. [RPC (Request-Reply)](#rpc-request-reply)
17. [Transactions](#transactions)
18. [Testing](#testing)
19. [Observability](#observability)
20. [Common Pitfalls](#common-pitfalls)
21. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Spring AMQP Kya Hai?

**Matlab:** Spring's idiomatic abstraction over the RabbitMQ Java client — declarative producers, listener annotations, error handling, message conversion.

```
RabbitTemplate     → producer wrapper
@RabbitListener    → consumer annotation
RabbitAdmin        → declares exchanges/queues/bindings
MessageConverter   → POJO ↔ bytes
```

### Why?

| Without | With Spring AMQP |
|---------|------------------|
| Manual `Connection`/`Channel` lifecycle | Auto-managed pool |
| Manual `consumer.handleDelivery()` | `@RabbitListener` |
| Manual byte serialization | `MessageConverter` |
| Manual error handling | Retry + DLX integration |
| Boilerplate AMQP client code | YAML-driven config |

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
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
  </dependency>
  
  <!-- Testing -->
  <dependency>
    <groupId>org.springframework.amqp</groupId>
    <artifactId>spring-rabbit-test</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

### Local RabbitMQ (Docker)

```bash
docker run -d --name rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=guest \
  -e RABBITMQ_DEFAULT_PASS=guest \
  rabbitmq:3.13-management
```

Management UI: http://localhost:15672 (guest/guest)

---

## Configuration via YAML

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: ${RABBIT_PASSWORD:guest}
    virtual-host: /
    
    # Connection settings
    connection-timeout: 30000
    
    # SSL (production)
    ssl:
      enabled: false
      key-store: classpath:keystore.jks
      key-store-password: ${KEYSTORE_PASSWORD}
    
    # Publisher confirms (reliability)
    publisher-confirm-type: correlated
    publisher-returns: true
    
    # Producer template
    template:
      mandatory: true                  # return on unroutable
      retry:
        enabled: true
        initial-interval: 1000
        max-attempts: 3
        multiplier: 2.0
    
    # Consumer listener
    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 10
        concurrency: 3
        max-concurrency: 10
        retry:
          enabled: true
          initial-interval: 1000
          max-attempts: 5
          multiplier: 2.0
          max-interval: 10000
        default-requeue-rejected: false
```

### Cluster (Multiple Brokers)

```yaml
spring:
  rabbitmq:
    addresses: broker1:5672,broker2:5672,broker3:5672
    username: ...
    password: ...
```

→ Spring connects to first available; reconnects on failure.

---

## Declaring Exchanges, Queues, Bindings

### Java Beans

```java
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_ORDERS = "orders.exchange";
    public static final String QUEUE_ORDERS = "orders.queue";
    public static final String QUEUE_ORDERS_DLQ = "orders.dlq";
    public static final String EXCHANGE_DLX = "orders.dlx";
    public static final String ROUTING_KEY_ORDER_PLACED = "order.placed";
    
    @Bean
    public TopicExchange ordersExchange() {
        return new TopicExchange(EXCHANGE_ORDERS, true, false);
    }
    
    @Bean
    public DirectExchange ordersDlx() {
        return new DirectExchange(EXCHANGE_DLX, true, false);
    }
    
    @Bean
    public Queue ordersQueue() {
        return QueueBuilder.durable(QUEUE_ORDERS)
            .withArgument("x-dead-letter-exchange", EXCHANGE_DLX)
            .withArgument("x-dead-letter-routing-key", "dead")
            .build();
    }
    
    @Bean
    public Queue ordersDlq() {
        return QueueBuilder.durable(QUEUE_ORDERS_DLQ).build();
    }
    
    @Bean
    public Binding ordersBinding(Queue ordersQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(ordersQueue)
            .to(ordersExchange)
            .with("order.*");
    }
    
    @Bean
    public Binding dlqBinding(Queue ordersDlq, DirectExchange ordersDlx) {
        return BindingBuilder.bind(ordersDlq)
            .to(ordersDlx)
            .with("dead");
    }
}
```

### Auto-Declaration

`RabbitAdmin` (auto-configured) declares all `Exchange`, `Queue`, `Binding` beans on app startup. **Idempotent** — only creates if missing.

### Annotation Style (Inline)

```java
@RabbitListener(bindings = @QueueBinding(
    value = @Queue(name = "orders.queue", durable = "true"),
    exchange = @Exchange(name = "orders.exchange", type = "topic"),
    key = "order.*"
))
public void handle(Order order) { ... }
```

→ Declares exchange + queue + binding automatically when listener registers.

---

## RabbitTemplate — Producing

### Inject + Send

```java
@Service
@RequiredArgsConstructor
public class OrderEventPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    
    public void publishPlaced(Order order) {
        rabbitTemplate.convertAndSend(
            "orders.exchange",          // exchange
            "order.placed",             // routing key
            order);                      // body (auto-converted to JSON)
    }
}
```

→ `convertAndSend()` uses configured `MessageConverter` (default `SimpleMessageConverter` → bytes; with Jackson, JSON).

### `send()` (Raw Message)

```java
MessageProperties props = MessagePropertiesBuilder.newInstance()
    .setContentType(MediaType.APPLICATION_JSON_VALUE)
    .setMessageId(UUID.randomUUID().toString())
    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
    .setHeader("source", "order-service")
    .build();

Message msg = MessageBuilder.withBody(json.getBytes())
    .andProperties(props)
    .build();

rabbitTemplate.send("orders.exchange", "order.placed", msg);
```

### Send to Default Exchange (Direct to Queue)

```java
rabbitTemplate.convertAndSend("", "orders.queue", order);
//                            ↑    ↑
//                         exchange routing key = queue name
```

### Custom Properties via Post-Processor

```java
rabbitTemplate.convertAndSend("orders.exchange", "order.placed", order, msg -> {
    msg.getMessageProperties().setHeader("trace-id", traceId);
    msg.getMessageProperties().setExpiration("60000");
    return msg;
});
```

### Send + Receive (RPC)

```java
String reply = (String) rabbitTemplate.convertSendAndReceive(
    "orders.exchange", "order.placed", order);
```

→ Synchronous call with auto-correlation. (See RPC section below.)

---

## @RabbitListener — Consuming

### Basic Listener

```java
@Component
@Slf4j
public class OrderEventListener {
    
    @RabbitListener(queues = "orders.queue")
    public void handle(Order order) {
        log.info("Received order: {}", order);
        process(order);
    }
}
```

→ Auto-deserializes via `MessageConverter`; auto-ack on successful return.

### Access Message + Properties

```java
@RabbitListener(queues = "orders.queue")
public void handle(@Payload Order order,
                   @Header(AmqpHeaders.MESSAGE_ID) String messageId,
                   @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey,
                   @Header(value = "trace-id", required = false) String traceId,
                   Message rawMessage) {
    
    log.info("messageId={} routingKey={} traceId={}", messageId, routingKey, traceId);
    process(order);
}
```

### Multiple Queues

```java
@RabbitListener(queues = {"orders.queue", "priority-orders.queue"})
public void handle(Order order) { ... }
```

### Multiple Listeners on Class — Use `@RabbitHandler`

```java
@Component
@RabbitListener(queues = "events.queue")
public class EventListener {
    
    @RabbitHandler
    public void handleOrder(OrderEvent event) {
        log.info("Order event: {}", event);
    }
    
    @RabbitHandler
    public void handlePayment(PaymentEvent event) {
        log.info("Payment event: {}", event);
    }
    
    @RabbitHandler(isDefault = true)
    public void handleOther(Object event) {
        log.warn("Unknown event: {}", event);
    }
}
```

→ Routes by message type (uses `__TypeId__` header from JSON).

### Inline Bindings

```java
@RabbitListener(bindings = @QueueBinding(
    value = @Queue(value = "orders.dynamic.queue",
                    durable = "true",
                    arguments = {
                        @Argument(name = "x-dead-letter-exchange", value = "orders.dlx"),
                        @Argument(name = "x-dead-letter-routing-key", value = "dead")
                    }),
    exchange = @Exchange(value = "orders.exchange", type = "topic", durable = "true"),
    key = "order.placed.*"
))
public void handle(Order order) { ... }
```

### Container ID + Auto-Start

```java
@RabbitListener(id = "orderListener", queues = "orders.queue", autoStartup = "false")
public void handle(Order order) { ... }
```

```java
@Autowired RabbitListenerEndpointRegistry registry;

public void start() {
    registry.getListenerContainer("orderListener").start();
}
```

---

## @EnableRabbit

When?
- **Spring Boot:** auto-configured. **Don't** need to add explicitly (unless customizing).
- **Plain Spring:** add to `@Configuration` class.

```java
@Configuration
@EnableRabbit
public class RabbitConfig { ... }
```

→ Without it, `@RabbitListener` not picked up.

---

## MessageConverter (Jackson2JsonMessageConverter)

### Default — `SimpleMessageConverter`

Handles `byte[]`, `String`, `Serializable` (Java native serialization).

❌ Java serialization = security risk + couples consumers to producer's classes.

### Use Jackson JSON

```java
@Configuration
public class RabbitConfig {

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(converter);
        return template;
    }
    
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory cf, MessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(cf);
        factory.setMessageConverter(converter);
        return factory;
    }
}
```

### How It Works

Producer:
```
POJO → Jackson → JSON bytes → Message body
                + adds header __TypeId__ = com.example.Order
```

Consumer:
```
Message body (JSON bytes) → Jackson → POJO
  uses __TypeId__ header to determine target class
```

### Trusted Packages (Security)

```java
@Bean
public MessageConverter messageConverter() {
    Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
    converter.setClassMapper(typeMapper());
    return converter;
}

@Bean
public DefaultClassMapper typeMapper() {
    DefaultClassMapper mapper = new DefaultClassMapper();
    mapper.setTrustedPackages("com.example.events");
    
    // Or alias-based mapping (decouple from package names)
    Map<String, Class<?>> idClassMapping = new HashMap<>();
    idClassMapping.put("OrderEvent", OrderEvent.class);
    idClassMapping.put("PaymentEvent", PaymentEvent.class);
    mapper.setIdClassMapping(idClassMapping);
    
    return mapper;
}
```

⚠️ Without trusted packages, deserialization vulnerabilities possible.

### Custom MessageConverter

```java
public class ProtobufMessageConverter implements MessageConverter {
    @Override
    public Message toMessage(Object object, MessageProperties props) { ... }
    
    @Override
    public Object fromMessage(Message message) { ... }
}
```

---

## Acknowledgment Modes

### Modes

| Mode | Behavior |
|------|----------|
| `NONE` (auto-ack) | Broker assumes ack on delivery. Fastest, message-loss risk. |
| `AUTO` (default) | Spring acks if listener returns normally; nacks on exception. |
| `MANUAL` | Listener calls `channel.basicAck()` / `basicNack()` explicitly. |

### Set in YAML

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        acknowledge-mode: manual
```

### Set in Java

```java
factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
```

### `AUTO` Mode (Recommended Default)

```java
@RabbitListener(queues = "orders.queue")
public void handle(Order order) {
    process(order);
    // Listener returns → Spring acks
    // Listener throws → Spring nacks (with requeue depending on config)
}
```

### `default-requeue-rejected`

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        default-requeue-rejected: false   # don't requeue on exception → DLX
```

→ With DLX configured, failed messages → DLX (no infinite requeue loop).

---

## Manual Acknowledgment

For fine-grained control:

```yaml
spring.rabbitmq.listener.simple.acknowledge-mode: manual
```

```java
@RabbitListener(queues = "orders.queue")
public void handle(Order order, Channel channel,
                   @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
    try {
        process(order);
        channel.basicAck(tag, false);     // ack single
    } catch (PoisonException e) {
        log.error("Poison — DLQ", e);
        channel.basicReject(tag, false);  // requeue=false → DLX
    } catch (TransientException e) {
        log.warn("Transient — requeue", e);
        channel.basicNack(tag, false, true);  // requeue=true
    }
}
```

### Multiple Ack

```java
channel.basicAck(tag, true);   // ack all up to tag
```

### Don't Forget!

If you don't ack/nack, message stays "unacked" forever (until consumer disconnects → re-delivered). **Always ack/nack/reject**.

---

## Concurrency

### Multiple Consumer Threads

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        concurrency: 3                # initial consumer count
        max-concurrency: 10           # auto-scale up under load
```

```java
@RabbitListener(queues = "orders.queue", concurrency = "3-10")
public void handle(Order order) { ... }
```

→ Container creates 3 consumer threads at startup; up to 10 if backlog grows.

### Per-Listener vs Global

```java
@Bean
public SimpleRabbitListenerContainerFactory factory(...) {
    factory.setConcurrentConsumers(3);
    factory.setMaxConcurrentConsumers(10);
    return factory;
}
```

### Direct Container Factory (Newer)

```java
@Bean
public DirectRabbitListenerContainerFactory directFactory(ConnectionFactory cf) {
    DirectRabbitListenerContainerFactory factory = new DirectRabbitListenerContainerFactory();
    factory.setConnectionFactory(cf);
    factory.setConsumersPerQueue(5);
    return factory;
}
```

→ `Direct` uses single thread per consumer — different model. `Simple` is most common.

---

## Listener Container Factory

### SimpleRabbitListenerContainerFactory (Default)

```java
@Bean
public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
        ConnectionFactory cf, MessageConverter converter) {
    
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(cf);
    factory.setMessageConverter(converter);
    factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
    factory.setConcurrentConsumers(3);
    factory.setMaxConcurrentConsumers(10);
    factory.setPrefetchCount(20);
    factory.setDefaultRequeueRejected(false);
    
    factory.setAdviceChain(retryInterceptor());
    factory.setErrorHandler(errorHandler());
    
    return factory;
}
```

### Multiple Factories

```java
@Bean public SimpleRabbitListenerContainerFactory orderFactory(...) { ... }
@Bean public SimpleRabbitListenerContainerFactory paymentFactory(...) { ... }
```

```java
@RabbitListener(queues = "orders.queue", containerFactory = "orderFactory")
public void handleOrder(Order o) { ... }

@RabbitListener(queues = "payments.queue", containerFactory = "paymentFactory")
public void handlePayment(Payment p) { ... }
```

---

## Error Handling & Retry

### Retry Interceptor

```java
@Bean
public RetryOperationsInterceptor retryInterceptor() {
    return RetryInterceptorBuilder.stateless()
        .maxAttempts(5)
        .backOffOptions(1000, 2.0, 10000)   // initial, multiplier, max
        .recoverer(new RejectAndDontRequeueRecoverer())   // → DLX
        .build();
}
```

```java
@Bean
public SimpleRabbitListenerContainerFactory factory(...) {
    factory.setAdviceChain(retryInterceptor());
    return factory;
}
```

### Recoverer Strategies

| Recoverer | Behavior |
|-----------|----------|
| `RejectAndDontRequeueRecoverer` | Reject → DLX (default-requeue-rejected=false) |
| `ImmediateRequeueMessageRecoverer` | Requeue (will retry on same/other consumer) |
| `RepublishMessageRecoverer` | Publish to specific exchange/queue with diagnostic headers |

### RepublishMessageRecoverer (Recommended)

```java
@Bean
public MessageRecoverer messageRecoverer(RabbitTemplate template) {
    return new RepublishMessageRecoverer(
        template,
        "orders.dlx",          // exchange
        "dead.${routing.key}");  // routing key pattern
}
```

→ Adds headers like `x-exception-message`, `x-exception-stacktrace`, `x-original-exchange`, `x-original-routingKey`.

### Listener-Level Retry (Spring Retry)

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        retry:
          enabled: true
          initial-interval: 1000
          max-attempts: 5
          multiplier: 2.0
          max-interval: 10000
```

→ Same effect as advice chain but YAML-configured.

### Custom ErrorHandler

```java
public class MyErrorHandler implements RabbitListenerErrorHandler {
    @Override
    public Object handleError(Message amqpMessage,
                              org.springframework.messaging.Message<?> message,
                              ListenerExecutionFailedException exception) {
        log.error("Listener error", exception);
        return null;
    }
}
```

```java
@RabbitListener(queues = "orders.queue", errorHandler = "myErrorHandler")
```

---

## Dead Letter Pattern

Combine **DLX** (broker config) + **Recoverer** (Spring config) for clean failure handling.

### Setup Reminder

```java
@Bean
public Queue ordersQueue() {
    return QueueBuilder.durable("orders.queue")
        .withArgument("x-dead-letter-exchange", "orders.dlx")
        .withArgument("x-dead-letter-routing-key", "dead.orders")
        .build();
}
```

### DLQ Listener

```java
@RabbitListener(queues = "orders.dlq")
public void handleDLQ(Message msg) {
    log.error("Dead letter: body={}, headers={}",
        new String(msg.getBody()),
        msg.getMessageProperties().getHeaders());
    
    // Persist for manual inspection / alerting
    dltRepo.save(new DltRecord(msg.getBody(), msg.getMessageProperties().getHeaders()));
}
```

### Useful Headers in DLQ

```
x-death                    → array of failure history
x-original-exchange       → from RepublishMessageRecoverer
x-original-routingKey
x-exception-message
x-exception-stacktrace
```

### Retry Topic Pattern (Cross-Reference)

For exponential delay between retries, use DLX + TTL queues:

```
main.queue → fails → DLX → retry-1m queue (TTL=60s, DLX back to main)
                              ↓ TTL expires
                              → DLX → main.queue (retry)
```

→ Cross-ref: `Messaging — Kafka/06-Kafka-Patterns.md` for similar pattern.

---

## Publisher Confirms

**Matlab:** Producer **confirmed** by broker that message was received.

```yaml
spring:
  rabbitmq:
    publisher-confirm-type: correlated
    publisher-returns: true
    template:
      mandatory: true
```

### Setup

```java
@Configuration
public class RabbitProducerConfig {
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(converter);
        template.setMandatory(true);
        
        // Confirm callback
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("Message confirmed: {}", correlationData);
            } else {
                log.error("Message NACKED: {} — {}", correlationData, cause);
            }
        });
        
        // Return callback (unroutable)
        template.setReturnsCallback(returned -> {
            log.error("Message returned: {} — {}",
                returned.getMessage(), returned.getReplyText());
        });
        
        return template;
    }
}
```

### Send with Correlation Data

```java
CorrelationData correlation = new CorrelationData(UUID.randomUUID().toString());
rabbitTemplate.convertAndSend("orders.exchange", "order.placed", order, correlation);

correlation.getFuture().whenComplete((confirm, ex) -> {
    if (ex != null) {
        log.error("Send failed", ex);
    } else if (confirm.isAck()) {
        log.info("Confirmed");
    } else {
        log.warn("Nacked: {}", confirm.getReason());
    }
});
```

### Why?

- Detect publish failures (broker down, exchange missing, etc.)
- Implement **at-least-once publishing**
- Combine with idempotent consumers for exactly-once-ish

---

## RPC (Request-Reply)

```java
String reply = (String) rabbitTemplate.convertSendAndReceive(
    "rpc.exchange",
    "rpc.routing.key",
    request);
```

### How It Works

1. Spring creates **temporary reply queue** (auto-deleted)
2. Sets `replyTo` + `correlationId` on outgoing message
3. Sends + waits for reply
4. Server processes + sends to `replyTo` queue with same `correlationId`
5. Spring matches reply, returns

### Server Side

```java
@RabbitListener(queues = "rpc.requests")
public String handleRpc(String request) {
    return "response: " + request;     // Spring sends to replyTo queue automatically
}
```

### With Custom Reply Queue

```yaml
spring:
  rabbitmq:
    template:
      reply-timeout: 5000
```

```java
@Bean
public Queue replyQueue() {
    return new Queue("rpc.replies");
}

template.setReplyAddress("rpc.replies");
```

⚠️ RPC over AMQP works but adds complexity. Often HTTP/gRPC is simpler.

---

## Transactions

### Channel Transactions (Slow — Avoid)

```yaml
spring:
  rabbitmq:
    template:
      channel-transacted: true
```

→ Each send blocks until commit. ~100x slower. Use **publisher confirms** instead.

### Spring Transaction Synchronization

```java
@Transactional
public void placeOrder(Order order) {
    orderRepo.save(order);                              // DB
    rabbitTemplate.convertAndSend("orders.exchange",
        "order.placed", order);                          // RabbitMQ
}
```

⚠️ DB and RabbitMQ are **separate transactions** — no XA by default. If DB commits but Rabbit fails (or vice versa) → inconsistency.

### Solutions

- **Transactional outbox** (covered in Kafka file — same idea)
- **Best-effort with confirms + idempotent consumer**
- For true XA: `JtaTransactionManager` + JMS broker (RabbitMQ doesn't natively XA)

---

## Testing

### Embedded RabbitMQ

There's no official embedded RabbitMQ for Spring (it's Erlang-based). Options:

#### 1. Testcontainers RabbitMQ (Recommended)

```xml
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>rabbitmq</artifactId>
  <scope>test</scope>
</dependency>
```

```java
@SpringBootTest
@Testcontainers
class OrderEventTest {
    
    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer(
        DockerImageName.parse("rabbitmq:3.13-management"));
    
    @DynamicPropertySource
    static void rabbitProps(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
    }
    
    @Autowired RabbitTemplate rabbitTemplate;
    
    @Test
    void send_message() {
        rabbitTemplate.convertAndSend("orders.exchange", "order.placed", new Order(...));
    }
}
```

#### 2. Mock RabbitTemplate (Pure Unit)

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock RabbitTemplate rabbitTemplate;
    @InjectMocks OrderService service;
    
    @Test
    void publishes_event() {
        service.placeOrder(new Order(...));
        verify(rabbitTemplate).convertAndSend(eq("orders.exchange"), eq("order.placed"), any());
    }
}
```

### Verify Listener

```java
@MockBean OrderProcessor processor;

@Test
void listener_processes() throws Exception {
    rabbitTemplate.convertAndSend("orders.exchange", "order.placed", order);
    
    // Wait for async processing
    Awaitility.await().atMost(5, TimeUnit.SECONDS)
        .untilAsserted(() -> verify(processor).process(any()));
}
```

---

## Observability

### Spring Boot Actuator

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
```

### Useful Metrics (Micrometer)

```
rabbitmq.published          ← messages published
rabbitmq.consumed           ← messages consumed
rabbitmq.connections        ← open connections
rabbitmq.channels           ← open channels
rabbitmq.acknowledged
rabbitmq.rejected
spring.rabbit.template.executions
```

### RabbitMQ Management UI

http://localhost:15672 — exchange/queue stats, message rates, consumer details, dead letter inspection.

### Plugins

```bash
rabbitmq-plugins enable rabbitmq_prometheus
```

→ Exports metrics on `:15692/metrics` for Prometheus scraping.

### Distributed Tracing

```xml
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
```

→ Auto-injects trace IDs in AMQP headers.

---

## Common Pitfalls

### 1. Forgetting MessageConverter

Default = Java serialization (security risk + couples consumers). Always configure `Jackson2JsonMessageConverter`.

### 2. No Trusted Packages

`Jackson2JsonMessageConverter` deserializes arbitrary classes from `__TypeId__` header → security risk. Set trusted packages or use ID-class mapping.

### 3. Auto-Ack with Critical Data

Crash during processing = message lost. Use AUTO (Spring) with proper exception handling, or MANUAL.

### 4. `default-requeue-rejected: true` + No DLX

Rejected message → requeued forever → infinite retry → consumer choked.

→ Set to `false` + configure DLX.

### 5. Channel Transactions

```yaml
channel-transacted: true   # ❌ slow
```

→ Use publisher confirms instead.

### 6. Big Messages

RabbitMQ default `frame_max` ~128KB. Sending 5MB messages = poor performance + memory pressure. Use S3 + pointer.

### 7. No Publisher Confirms

Producer thinks send succeeded; broker actually rejected → silent loss.

→ Enable `publisher-confirm-type: correlated` for important data.

### 8. Wildcards in Topic Bindings

```
order.* → matches order.placed (1 word)
        → does NOT match order.placed.priority (2 words)
        
order.# → matches both
```

### 9. Connection Per Operation

```java
ConnectionFactory.createConnection()   // per send → terrible
```

→ Spring auto-pools — use injected `ConnectionFactory`.

### 10. Quorum Queues + Unsupported Features

Priority queues, lazy mode, etc. not supported on quorum queues. Check feature matrix.

### 11. Unmonitored Queue Length

Queue silently grows → OOM. Set up alerts on queue depth.

### 12. Mixing JSON Producer + Java Serialization Consumer

Type mismatches. Standardize on Jackson across all services.

---

## Summary Cheat Sheet

| Concept | Quick Note |
|---------|-----------|
| `RabbitTemplate.convertAndSend(exchange, key, body)` | Producer |
| `@RabbitListener(queues = ...)` | Consumer |
| `Queue`, `Exchange`, `Binding` beans | Auto-declared |
| `Jackson2JsonMessageConverter` | JSON conversion |
| Trusted packages / class mapping | Security |
| `acknowledge-mode: manual` | Explicit ack |
| `default-requeue-rejected: false` + DLX | Failure pattern |
| `concurrency: 3-10` | Auto-scale consumers |
| `prefetch: 10-50` | QoS |
| Publisher confirms | At-least-once publish |
| `convertSendAndReceive` | RPC |
| Testcontainers | Integration test |

| Property | Common Value |
|----------|-------------|
| `acknowledge-mode` | manual / auto |
| `prefetch` | 10-50 |
| `concurrency` | 3 |
| `max-concurrency` | 10 |
| `default-requeue-rejected` | false |
| `publisher-confirm-type` | correlated |
| `mandatory` | true (returns on unroutable) |
| `retry.max-attempts` | 3-5 |

| ✅ Do | ❌ Don't |
|-------|---------|
| Jackson MessageConverter | Default (Java serialization) |
| Set trusted packages | Trust all classes |
| `default-requeue-rejected: false` + DLX | Infinite requeue |
| Publisher confirms for important data | Trust send() success |
| Manual / Auto ack with proper handling | Auto-ack for business data |
| Concurrency tuned to processing | Single consumer always |
| Quorum queues for HA | Mirrored queues (deprecated) |
| Testcontainers for integration tests | Mock everything |

---

## Practice

1. Set up RabbitMQ in Docker; access management UI.
2. Declare `TopicExchange` + `Queue` + `Binding` beans; verify in UI.
3. Send a POJO with `RabbitTemplate.convertAndSend`; receive in `@RabbitListener`.
4. Configure `Jackson2JsonMessageConverter` + trusted packages.
5. Set `acknowledge-mode: manual` + handle ack/nack/reject.
6. Configure DLX + DLQ; reject a message; verify routing to DLQ.
7. Enable publisher confirms; log ack/nack.
8. Configure retry with exponential backoff via `RetryOperationsInterceptor`.
9. Build RPC: `convertSendAndReceive` + `@RabbitListener` returning value.
10. Use Testcontainers RabbitMQ for integration test.
11. Add Micrometer metrics; visualize message rates.
12. Implement transactional outbox pattern: DB write + outbox row in same TX, separate publisher.

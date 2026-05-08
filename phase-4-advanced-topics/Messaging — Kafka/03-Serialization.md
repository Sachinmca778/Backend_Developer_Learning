# Serialization in Kafka

## Status: Not Started

---

## Table of Contents

1. [Why Serialization Matters](#why-serialization-matters)
2. [Built-in Serializers](#built-in-serializers)
3. [String Serializer / Deserializer](#string-serializer--deserializer)
4. [JsonSerializer / JsonDeserializer](#jsonserializer--jsondeserializer)
5. [Trusted Packages](#trusted-packages)
6. [ByteArray Serializer](#bytearray-serializer)
7. [Avro + Schema Registry](#avro--schema-registry)
8. [Protobuf](#protobuf)
9. [Custom Serializers](#custom-serializers)
10. [Schema Evolution](#schema-evolution)
11. [Producer / Consumer Config Properties](#producer--consumer-config-properties)
12. [Comparison Table](#comparison-table)
13. [Common Pitfalls](#common-pitfalls)
14. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Why Serialization Matters

**Matlab:** Kafka stores **bytes**. Producer converts Java object → bytes (serialize), consumer converts bytes → Java object (deserialize). Both ends must agree on format.

```
Producer side:           Wire (bytes):                Consumer side:
Order { id=1, ...}  ──▶  [0x7B, 0x22, 0x69 ...]  ──▶  Order { id=1, ...}
                          (UTF-8 of JSON)
```

### Format Choice Affects

- **Size on disk + network** (smaller = better)
- **Schema evolution** (can old consumers read new producer's data?)
- **Cross-language support** (Java producer + Python consumer?)
- **Speed** (CPU cost of serialize/deserialize)
- **Tooling** (debug, schema registry, code gen)

### Common Choices

| Format | Use Case |
|--------|----------|
| **String (JSON, plain)** | Simple, debuggable, dev-friendly |
| **Avro + Schema Registry** | Production, strong contracts, evolution |
| **Protobuf** | High-performance, polyglot |
| **ByteArray** | Custom / pre-serialized payloads |
| **JSON Schema** | JSON with explicit schema (compromise) |

---

## Built-in Serializers

Apache Kafka client provides:

```java
org.apache.kafka.common.serialization.StringSerializer
org.apache.kafka.common.serialization.IntegerSerializer
org.apache.kafka.common.serialization.LongSerializer
org.apache.kafka.common.serialization.DoubleSerializer
org.apache.kafka.common.serialization.ByteArraySerializer
org.apache.kafka.common.serialization.UUIDSerializer
```

Spring Kafka provides:

```java
org.springframework.kafka.support.serializer.JsonSerializer
org.springframework.kafka.support.serializer.JsonDeserializer
org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
```

Confluent provides:

```java
io.confluent.kafka.serializers.KafkaAvroSerializer
io.confluent.kafka.serializers.KafkaAvroDeserializer
io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer
io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer
```

---

## String Serializer / Deserializer

### Use Case

- Simple strings (plain text or JSON-as-string)
- Quick prototyping
- Cross-language (any language can read UTF-8)

### Producer Config

```yaml
spring:
  kafka:
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
```

### Consumer Config

```yaml
spring:
  kafka:
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
```

### Sending JSON-as-String (Manual)

```java
ObjectMapper mapper = new ObjectMapper();

@Service
public class OrderPublisher {
    public void publish(Order order) throws Exception {
        String json = mapper.writeValueAsString(order);
        kafkaTemplate.send("orders", order.getId().toString(), json);
    }
}
```

### Receiving JSON-as-String

```java
@KafkaListener(topics = "orders", groupId = "order-processor")
public void handle(String json) throws Exception {
    Order order = mapper.readValue(json, Order.class);
    process(order);
}
```

### Pros / Cons

| Pros | Cons |
|------|------|
| Simple | Manual serialization in app |
| Debug-friendly (`kafkacat` / console-consumer) | No schema enforcement |
| Polyglot | No type safety on broker |
| No extra dependencies | Larger size than binary formats |

---

## JsonSerializer / JsonDeserializer

Spring Kafka's typed JSON serializers — auto-converts Java POJOs ↔ JSON.

### Producer Config

```yaml
spring:
  kafka:
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

```java
@Service
@RequiredArgsConstructor
public class OrderPublisher {
    
    private final KafkaTemplate<String, Order> kafkaTemplate;
    
    public void publish(Order order) {
        kafkaTemplate.send("orders", order.getId().toString(), order);
        // Order auto-serialized to JSON
    }
}
```

### Producer Config (Java)

```java
@Bean
public ProducerFactory<String, Order> orderProducerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
    props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    return new DefaultKafkaProducerFactory<>(props);
}
```

### Consumer Config

```yaml
spring:
  kafka:
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.example.events"
        spring.json.value.default.type: "com.example.events.Order"
```

```java
@KafkaListener(topics = "orders", groupId = "order-processor")
public void handle(Order order) {
    process(order);
}
```

### How JsonSerializer Works

1. Producer adds **type headers** to record:
   ```
   __TypeId__: com.example.events.Order
   ```
2. Consumer reads type header → deserializes to that class.

### Without Type Headers

Disable type headers:

```yaml
spring.kafka.producer.properties.spring.json.add.type.headers: false
```

→ Consumer must specify default type:

```yaml
spring.json.value.default.type: "com.example.Order"
```

### Send Different Types on Same Topic

By default, type headers tell consumer how to deserialize:

```java
kafkaTemplate.send("events", new OrderCreated(...));
kafkaTemplate.send("events", new OrderCancelled(...));
```

Consumer:
```java
@KafkaListener(topics = "events")
public void handle(Object event) {
    if (event instanceof OrderCreated o) { ... }
    else if (event instanceof OrderCancelled c) { ... }
}
```

### Type Mapping (Aliases)

Avoid coupling to package names:

```yaml
spring:
  kafka:
    producer:
      properties:
        spring.json.type.mapping: 'order.created:com.example.events.OrderCreated, order.cancelled:com.example.events.OrderCancelled'
    consumer:
      properties:
        spring.json.type.mapping: 'order.created:com.example.events.OrderCreated, order.cancelled:com.example.events.OrderCancelled'
```

→ Producer sends `__TypeId__: order.created`. Consumer deserializes to `OrderCreated`. Independent of package paths.

---

## Trusted Packages

**Matlab:** `JsonDeserializer` security feature — only deserialize objects from **whitelisted packages**.

### Why?

```java
// ❌ Without trust list — anyone can send a class name
{"__TypeId__": "java.util.HashMap", "data": {...}}
{"__TypeId__": "com.evil.MaliciousClass", ...}
```

→ Deserialization vulnerabilities → RCE possible.

### Configure Trusted Packages

```yaml
spring:
  kafka:
    consumer:
      properties:
        spring.json.trusted.packages: "com.example.events,com.shared.dto"
```

### Trust All (DANGEROUS)

```yaml
spring.json.trusted.packages: "*"
```

→ Only for **dev** or fully internal trusted Kafka clusters.

### Programmatic

```java
JsonDeserializer<Order> deserializer = new JsonDeserializer<>(Order.class);
deserializer.addTrustedPackages("com.example.events", "com.shared.dto");
```

### Best Practices

✅ Whitelist explicitly
✅ Use type aliases instead of package names (decouple)
✅ Use Avro/Protobuf for cross-team production scenarios

---

## ByteArray Serializer

For pre-serialized payloads or binary data.

### Use Case

- Already-encoded payload (Avro, Protobuf manually)
- Binary blobs (images, files)
- Custom format

### Config

```yaml
spring:
  kafka:
    producer:
      value-serializer: org.apache.kafka.common.serialization.ByteArraySerializer
    consumer:
      value-deserializer: org.apache.kafka.common.serialization.ByteArrayDeserializer
```

### Producer

```java
byte[] data = encodeAvroOrCustomFormat(order);
kafkaTemplate.send("orders", key, data);
```

### Consumer

```java
@KafkaListener(topics = "orders")
public void handle(byte[] data) {
    Order order = decodeAvroOrCustomFormat(data);
    process(order);
}
```

⚠️ Schema responsibility on you. Useful when you need full control or interop with non-JVM tools.

---

## Avro + Schema Registry

**Matlab:** **Apache Avro** = schema-based binary format. **Confluent Schema Registry** = central place for schemas, enforces compatibility.

### Why Avro?

✅ Compact binary (smaller than JSON)
✅ Strong schema (typed)
✅ Schema evolution (forward/backward compat)
✅ Cross-language (Java, Python, Go, etc.)
✅ Fast serialization
❌ Requires Schema Registry infrastructure
❌ Less human-readable

### Architecture

```
                ┌──────────────────────┐
                │ Schema Registry      │
                │ (HTTP API)           │
                │  store schemas by ID │
                └──────────┬───────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
   ┌────▼─────┐      ┌─────▼──────┐    ┌─────▼──────┐
   │ Producer │      │ Consumer   │    │ Consumer   │
   │ → registry│      │ ← registry │    │ ← registry │
   │  fetch ID │      │  by ID     │    │  by ID     │
   └─────┬────┘      └────────────┘    └────────────┘
         │ produce: [schema-id][payload-bytes]
         ▼
       Kafka
```

### Wire Format

```
Magic byte (1 byte: 0x00) + Schema ID (4 bytes int) + Avro payload (binary)
```

### Define Schema (Avro IDL or JSON)

`order.avsc`:

```json
{
  "type": "record",
  "name": "Order",
  "namespace": "com.example.events",
  "fields": [
    {"name": "id", "type": "long"},
    {"name": "userId", "type": "string"},
    {"name": "amount", "type": "double"},
    {"name": "currency", "type": "string", "default": "INR"}
  ]
}
```

### Generate Java Classes (Maven)

```xml
<plugin>
  <groupId>org.apache.avro</groupId>
  <artifactId>avro-maven-plugin</artifactId>
  <version>1.11.3</version>
  <executions>
    <execution>
      <phase>generate-sources</phase>
      <goals>
        <goal>schema</goal>
      </goals>
      <configuration>
        <sourceDirectory>${project.basedir}/src/main/avro</sourceDirectory>
        <outputDirectory>${project.basedir}/target/generated-sources/avro</outputDirectory>
      </configuration>
    </execution>
  </executions>
</plugin>
```

### Producer Config

```yaml
spring:
  kafka:
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: io.confluent.kafka.serializers.KafkaAvroSerializer
      properties:
        schema.registry.url: http://schema-registry:8081
```

### Consumer Config

```yaml
spring:
  kafka:
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: io.confluent.kafka.serializers.KafkaAvroDeserializer
      properties:
        schema.registry.url: http://schema-registry:8081
        specific.avro.reader: true
```

### Producer Code

```java
@Service
@RequiredArgsConstructor
public class OrderPublisher {
    
    private final KafkaTemplate<String, Order> kafkaTemplate;   // Order is Avro-generated
    
    public void publish(long id, String userId, double amount) {
        Order order = Order.newBuilder()
            .setId(id)
            .setUserId(userId)
            .setAmount(amount)
            .setCurrency("INR")
            .build();
        
        kafkaTemplate.send("orders-avro", userId, order);
    }
}
```

### Consumer Code

```java
@KafkaListener(topics = "orders-avro", groupId = "order-processor")
public void handle(Order order) {       // Avro-generated class
    log.info("Got order id={} amount={}", order.getId(), order.getAmount());
}
```

### Subject Naming Strategy

Schema Registry uses **subjects** to organize:

```
TopicNameStrategy (default):     subject = topic + "-value"
                                            topic + "-key"
RecordNameStrategy:              subject = record.full.name
TopicRecordNameStrategy:         subject = topic + "-" + record.full.name
```

```yaml
spring.kafka.producer.properties.value.subject.name.strategy: io.confluent.kafka.serializers.subject.RecordNameStrategy
```

→ `RecordNameStrategy` allows multiple schema versions on same topic.

### Compatibility Rules

```
BACKWARD (default):    new schema can read old data
FORWARD:               old schema can read new data
FULL:                  both directions
NONE:                  no compatibility check
```

→ Set per-subject:
```bash
curl -X PUT http://schema-registry:8081/config/orders-avro-value \
  -d '{"compatibility": "BACKWARD"}'
```

---

## Protobuf

**Google's Protocol Buffers** — alternative to Avro.

### vs Avro

| Aspect | Avro | Protobuf |
|--------|------|----------|
| Schema location | Schema Registry (binary) | `.proto` files compiled |
| Wire format | Schema ID + payload | Just payload (schema implicit) |
| Reflection | Limited | Strong |
| Tooling | Confluent ecosystem | Google ecosystem (gRPC) |
| Language support | Excellent | Excellent |

### `.proto` File

```protobuf
syntax = "proto3";

package com.example.events;

option java_package = "com.example.events";
option java_outer_classname = "OrderProto";

message Order {
    int64 id = 1;
    string user_id = 2;
    double amount = 3;
    string currency = 4;
}
```

### Maven Plugin

```xml
<plugin>
  <groupId>com.google.protobuf</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>0.6.1</version>
  <configuration>
    <protocArtifact>com.google.protobuf:protoc:3.25.1:exe:${os.detected.classifier}</protocArtifact>
  </configuration>
  <executions>
    <execution>
      <goals>
        <goal>compile</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

### With Schema Registry

```yaml
spring:
  kafka:
    producer:
      value-serializer: io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer
      properties:
        schema.registry.url: http://schema-registry:8081
    consumer:
      value-deserializer: io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer
      properties:
        schema.registry.url: http://schema-registry:8081
        specific.protobuf.value.type: com.example.events.OrderProto$Order
```

---

## Custom Serializers

### When?

- Existing custom binary format
- Compression / encryption pre-Kafka
- Non-standard schema system

### Implement Serializer Interface

```java
public class CustomOrderSerializer implements Serializer<Order> {
    
    @Override
    public byte[] serialize(String topic, Order order) {
        if (order == null) return null;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(out)) {
            dos.writeLong(order.getId());
            dos.writeUTF(order.getUserId());
            dos.writeDouble(order.getAmount());
            dos.writeUTF(order.getCurrency());
            return out.toByteArray();
        } catch (IOException e) {
            throw new SerializationException("Failed to serialize order", e);
        }
    }
}
```

### Implement Deserializer

```java
public class CustomOrderDeserializer implements Deserializer<Order> {
    
    @Override
    public Order deserialize(String topic, byte[] data) {
        if (data == null) return null;
        try (ByteArrayInputStream in = new ByteArrayInputStream(data);
             DataInputStream dis = new DataInputStream(in)) {
            return Order.builder()
                .id(dis.readLong())
                .userId(dis.readUTF())
                .amount(dis.readDouble())
                .currency(dis.readUTF())
                .build();
        } catch (IOException e) {
            throw new SerializationException("Failed to deserialize order", e);
        }
    }
}
```

### Register

```yaml
spring:
  kafka:
    producer:
      value-serializer: com.example.CustomOrderSerializer
    consumer:
      value-deserializer: com.example.CustomOrderDeserializer
```

### ErrorHandlingDeserializer (Wrap Custom)

Bad messages can poison consumers. Wrap with error handler:

```yaml
spring:
  kafka:
    consumer:
      value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
      properties:
        spring.deserializer.value.delegate.class: com.example.CustomOrderDeserializer
```

→ Bad messages → exception captured in record header; doesn't crash consumer.

---

## Schema Evolution

**Matlab:** Schema badalna without breaking existing producers/consumers.

### Common Evolutions

#### Adding a Field (with default) — Backward Compatible

```json
// v1
{"type":"record","fields":[{"name":"id","type":"long"}]}

// v2 — added "amount"
{"type":"record","fields":[
  {"name":"id","type":"long"},
  {"name":"amount","type":"double","default":0.0}
]}
```

→ Old consumer reading v2: ignores new field. New consumer reading v1: uses default.

#### Removing a Field (with default) — Forward Compatible

#### Renaming a Field — INCOMPATIBLE

→ Use **aliases** in Avro:

```json
{"name":"userId","type":"string","aliases":["user_id"]}
```

#### Changing a Field's Type — Mostly INCOMPATIBLE

→ Add new field with new type, deprecate old.

### Compatibility Modes Quick Ref

| Mode | New schema can read old data | Old schema can read new data |
|------|-------------------------------|-------------------------------|
| **BACKWARD** | ✅ | ❌ |
| **FORWARD** | ❌ | ✅ |
| **FULL** | ✅ | ✅ |
| **NONE** | — | — |

### Strategy

For event-streaming where consumers might lag versions:

```
Use FULL or BACKWARD (most common)
Add fields with defaults
Never rename without aliases
Never change types without deprecation flow
```

---

## Producer / Consumer Config Properties

### Producer Properties Reference

```yaml
spring:
  kafka:
    producer:
      bootstrap-servers: kafka:9092
      
      # Serialization
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      
      # Reliability
      acks: all
      retries: 2147483647         # MAX (combined with delivery.timeout.ms)
      properties:
        enable.idempotence: true
        max.in.flight.requests.per.connection: 5
        delivery.timeout.ms: 120000
        
        # Batching
        linger.ms: 10
        batch.size: 32768
        
        # Compression
        compression.type: snappy
        
        # JSON
        spring.json.add.type.headers: true
        spring.json.type.mapping: order.created:com.example.OrderCreated
        
        # Schema Registry (for Avro/Protobuf)
        schema.registry.url: http://schema-registry:8081
```

### Consumer Properties Reference

```yaml
spring:
  kafka:
    consumer:
      bootstrap-servers: kafka:9092
      group-id: order-processor
      
      # Deserialization
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      
      # Behavior
      auto-offset-reset: earliest
      enable-auto-commit: false
      isolation-level: read_committed
      
      properties:
        max.poll.records: 100
        max.poll.interval.ms: 300000
        session.timeout.ms: 45000
        heartbeat.interval.ms: 3000
        fetch.min.bytes: 1
        fetch.max.wait.ms: 500
        
        # JSON
        spring.json.trusted.packages: com.example.events
        spring.json.value.default.type: com.example.events.Order
        spring.json.use.type.headers: true
        
        # Avro
        schema.registry.url: http://schema-registry:8081
        specific.avro.reader: true
```

---

## Comparison Table

| Aspect | String/JSON | JsonSerializer (Spring) | Avro | Protobuf |
|--------|-------------|--------------------------|------|----------|
| **Size** | Large | Large | Small | Smallest |
| **Schema** | None | Type headers | Schema Registry | `.proto` files |
| **Evolution** | Manual | Manual | Strong (rules) | Strong (numbered fields) |
| **Cross-language** | ✅ | ⚠️ Spring-specific | ✅ | ✅ |
| **Debugging** | ✅ readable | ✅ | ❌ binary | ❌ binary |
| **Schema Registry** | optional | optional | required | required |
| **Speed** | Slow | Slow | Fast | Fastest |
| **Setup complexity** | Easy | Easy | Medium | Medium |
| **Best for** | Prototyping, internal | Spring shops | Production, polyglot | High-perf, gRPC ecosystem |

---

## Common Pitfalls

### 1. Trusted Packages = `*`

Security risk in JsonDeserializer. Use explicit packages.

### 2. Type Headers Mismatch

Producer uses `add.type.headers=true` but consumer doesn't trust packages → deserialization fails.

### 3. Schema Registry Down → Producer Stuck

If `schema.registry.url` unreachable, producer can't register/lookup → fails. Make registry HA.

### 4. Cross-Topic Schema Conflicts

Default subject naming strategy: `topic-value`. If you want same Avro schema across topics, use `RecordNameStrategy`.

### 5. Compatibility Mode Set Too Loose

`compatibility=NONE` → field renames slip through → broken consumers. Default to `BACKWARD` or `FULL`.

### 6. Forgetting `specific.avro.reader: true`

Without it, consumer gets `GenericRecord` instead of generated specific class.

### 7. Big Messages

JSON for 5MB payloads → Kafka not happy. Compress (gzip) at producer or store outside (S3 + pointer).

### 8. Mixed Serializers on Same Topic

Producer A sends JSON, Producer B sends Avro → consumers crash. Standardize per topic.

### 9. Lombok + Avro

Avro generates classes — don't try to add Lombok to them. Wrap if needed.

### 10. ErrorHandlingDeserializer Forgotten

One bad message → infinite retry → blocks partition. Always wrap with `ErrorHandlingDeserializer` + DLT.

### 11. Type Aliases Out of Sync

Producer uses alias `order.created`, consumer uses `OrderCreated`. Type mapping mismatch. Centralize the mapping.

### 12. JSON `null` vs Missing

```json
{"id": 1, "amount": null}     // present, null
{"id": 1}                       // missing
```

→ Different in Java (Optional, default values). Be explicit.

---

## Summary Cheat Sheet

| Task | Choice |
|------|--------|
| Quick prototype | StringSerializer + JSON-as-string |
| Spring app, internal | JsonSerializer + trusted packages |
| Polyglot, contracts | Avro + Schema Registry |
| Performance critical / gRPC | Protobuf |
| Custom binary | ByteArraySerializer + custom |
| Bad messages possible | Wrap with ErrorHandlingDeserializer |

| Property | Common Value |
|----------|-------------|
| `key-serializer` | `StringSerializer` |
| `value-serializer` | `JsonSerializer` / `KafkaAvroSerializer` |
| `spring.json.trusted.packages` | `com.example.events` |
| `spring.json.type.mapping` | `alias:fqcn,...` |
| `schema.registry.url` | `http://schema-registry:8081` |
| `specific.avro.reader` | `true` |

| ✅ Do | ❌ Don't |
|-------|---------|
| Whitelist trusted packages | `trusted.packages: "*"` in prod |
| Use type aliases (decouple from package) | Hardcode FQCN in headers |
| Use Avro/Protobuf for cross-team contracts | JsonSerializer for cross-language |
| Wrap with ErrorHandlingDeserializer | Crash on poison message |
| Set compatibility BACKWARD/FULL | NONE (no schema enforcement) |
| Store big payloads in S3 | Send 10MB JSON to Kafka |
| Compress (snappy/lz4) for big messages | Default no compression |

---

## Practice

1. Switch from `StringSerializer` (JSON-as-string) to `JsonSerializer`.
2. Configure `spring.json.trusted.packages` and `spring.json.type.mapping`.
3. Set up Schema Registry locally (Confluent Platform or Docker).
4. Create an Avro schema; generate Java class; produce + consume.
5. Evolve schema (add field with default); verify backward compatibility.
6. Configure subject naming strategy `RecordNameStrategy`.
7. Wrap consumer with `ErrorHandlingDeserializer`; send a poison message; verify graceful handling.
8. Compare wire size: JSON vs Avro for same payload.
9. Configure compression (`snappy`) at producer; observe size reduction.
10. Build a custom serializer for a legacy binary format.

# Redis Integration

## Status: Not Started

---

## Table of Contents

1. [Spring Boot Redis Setup](#spring-boot-redis-setup)
2. [RedisTemplate](#redistemplate)
3. [StringRedisTemplate](#stringredistemplate)
4. [Lettuce vs Jedis](#lettuce-vs-jedis)
5. [Redis Serialization (JSON)](#redis-serialization-json)
6. [Redis Pub/Sub](#redis-pubsub)
7. [Redis Streams](#redis-streams)

---

## Spring Boot Redis Setup

**Matlab:** Redis ko Spring Boot application mein integrate karna — distributed caching ke liye.

### Dependency

```xml
<!-- Spring Data Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Connection pool (optional, Jedis ke liye) -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

### Configuration

```properties
# application.properties

# Redis server
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=  # Agar password hai

# Connection pool (Lettuce default)
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=0
spring.data.redis.lettuce.pool.max-wait=-1ms

# Timeout
spring.data.redis.timeout=5000ms

# Database index (0-15)
spring.data.redis.database=0
```

### Cache Manager Configuration

```java
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class RedisCacheConfig {

    private final RedisConnectionFactory redisConnectionFactory;

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // Key serializer — String
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value serializer — JSON
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager() {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(config)
            .withInitialCacheConfigurations(Map.of(
                "users", RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(30)),
                "products", RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofHours(1)),
                "sessions", RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(5))
            ))
            .build();
    }
}
```

---

## RedisTemplate

**Matlab:** Redis operations ke liye high-level API — key-value store, lists, sets, hashes, etc.

### Basic Usage

```java
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // String operations
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // Delete
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    // Check existence
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    // Set expiry
    public void expire(String key, long timeout, TimeUnit unit) {
        redisTemplate.expire(key, timeout, unit);
    }
}
```

### Redis Data Structures

```java
@Service
@RequiredArgsConstructor
public class RedisOperationsService {

    private final RedisTemplate<String, Object> redisTemplate;

    // ===== VALUE (String) Operations =====
    public void stringValue() {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();

        ops.set("name", "Sachin");
        String name = (String) ops.get("name");

        ops.set("counter", 0);
        ops.increment("counter");  // Atomic increment

        ops.setIfAbsent("lock", "locked", 10, TimeUnit.SECONDS);  // Distributed lock
    }

    // ===== LIST Operations =====
    public void listOperations() {
        ListOperations<String, Object> ops = redisTemplate.opsForList();

        ops.leftPush("tasks", "Task1");  // LPUSH
        ops.leftPush("tasks", "Task2");
        ops.rightPush("tasks", "Task3"); // RPUSH

        Object first = ops.leftPop("tasks");   // LPOP
        Object last = ops.rightPop("tasks");   // RPOP

        Object item = ops.index("tasks", 0);   // LINDEX
        Long size = ops.size("tasks");         // LLEN

        List<Object> range = ops.range("tasks", 0, -1);  // LRANGE
    }

    // ===== SET Operations =====
    public void setOperations() {
        SetOperations<String, Object> ops = redisTemplate.opsForSet();

        ops.add("tags", "java", "spring", "redis");  // SADD
        Set<Object> members = ops.members("tags");   // SMEMBERS
        Boolean isMember = ops.isMember("tags", "java");  // SISMEMBER
        Long size = ops.size("tags");                // SCARD
        ops.remove("tags", "redis");                 // SREM

        // Set operations
        Set<Object> intersection = ops.intersect("set1", "set2");
        Set<Object> union = ops.union("set1", "set2");
        Set<Object> difference = ops.difference("set1", "set2");
    }

    // ===== ZSET (Sorted Set) Operations =====
    public void sortedSetOperations() {
        ZSetOperations<String, Object> ops = redisTemplate.opsForZSet();

        ops.add("leaderboard", "Sachin", 100);  // ZADD
        ops.add("leaderboard", "Rahul", 85);
        ops.add("leaderboard", "Priya", 95);

        Set<Object> range = ops.range("leaderboard", 0, -1);  // Lowest to highest
        Set<Object> revRange = ops.reverseRange("leaderboard", 0, 2);  // Top 3

        Double score = ops.score("leaderboard", "Sachin");  // ZSCORE
        Long rank = ops.rank("leaderboard", "Sachin");      // ZRANK
        Long size = ops.zCard("leaderboard");               // ZCARD

        ops.incrementScore("leaderboard", "Sachin", 10);  // ZINCRBY
    }

    // ===== HASH Operations =====
    public void hashOperations() {
        HashOperations<String, String, Object> ops = redisTemplate.opsForHash();

        ops.put("user:1", "name", "Sachin");     // HSET
        ops.put("user:1", "age", 25);
        ops.put("user:1", "email", "sachin@mail.com");

        Object name = ops.get("user:1", "name"); // HGET
        Map<String, Object> all = ops.entries("user:1");  // HGETALL
        Boolean hasField = ops.hasKey("user:1", "name");  // HEXISTS
        ops.delete("user:1", "email");                    // HDEL
    }
}
```

---

## StringRedisTemplate

**Matlab:** Specialized RedisTemplate — sirf String keys aur values ke liye.

### Usage

```java
@Service
@RequiredArgsConstructor
public class StringRedisService {

    private final StringRedisTemplate stringRedisTemplate;

    public void example() {
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();

        ops.set("name", "Sachin");
        String name = ops.get("name");  // Direct String — no casting!

        ops.set("counter", "0");
        Long counter = ops.increment("counter");  // 1

        // TTL
        ops.set("session:abc123", "user-data", 30, TimeUnit.MINUTES);
    }
}
```

### RedisTemplate vs StringRedisTemplate

| Feature | RedisTemplate<String, Object> | StringRedisTemplate |
|---------|------------------------------|-------------------|
| **Key Type** | String | String |
| **Value Type** | Object (needs serialization) | String |
| **Serialization** | Configurable | StringRedisSerializer |
| **Use Case** | Complex objects | Simple strings, counters |
| **Type Casting** | Required | Not required |

---

## Lettuce vs Jedis

### Comparison

| Feature | Lettuce | Jedis |
|---------|---------|-------|
| **Default** | ✅ Spring Boot 2+ default | ❌ Manual configure |
| **Connection** | Async, non-blocking | Blocking, synchronous |
| **Thread Safety** | ✅ Thread-safe connections | ❌ Not thread-safe |
| **Connection Pool** | Optional | Required for multi-threading |
| **Performance** | Better for high concurrency | Good for simple use cases |
| **Reactive Support** | ✅ Supports WebFlux | ❌ No reactive support |

### Lettuce (Default)

```xml
<!-- Default — alag se add karne ki zarurat nahi -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

```java
// Lettuce automatically creates shared connections
// No connection pool needed for most use cases
```

### Jedis

```xml
<!-- Lettuce exclude karo, Jedis include karo -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
    <exclusions>
        <exclusion>
            <groupId>io.lettuce</groupId>
            <artifactId>lettuce-core</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
</dependency>
<!-- Connection pool required for Jedis -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

```java
// Jedis ke liye connection pool configure karna padta hai
@Bean
public JedisConnectionFactory jedisConnectionFactory() {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
    config.setHostName("localhost");
    config.setPort(6379);

    JedisClientConfiguration clientConfig = JedisClientConfiguration.builder()
        .usePooling()
        .poolConfig(poolConfig())
        .build();

    return new JedisConnectionFactory(config, clientConfig);
}

@Bean
public JedisPoolConfig poolConfig() {
    JedisPoolConfig config = new JedisPoolConfig();
    config.setMaxTotal(10);
    config.setMaxIdle(5);
    config.setMinIdle(1);
    config.setMaxWaitMillis(2000);
    return config;
}
```

### When to Use What

```
Use Lettuce when:
- Spring Boot 2+ default use karna hai
- Reactive/WebFlux support chahiye
- High concurrency scenarios
- Simple setup chahiye

Use Jedis when:
- Legacy application migrate kar rahe ho
- Synchronous operations kaafi hain
- Connection pooling explicitly control karni hai
```

---

## Redis Serialization (JSON)

**Matlab:** Java objects ko JSON format mein Redis mein store karna.

### Configuration

```java
@Bean
public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);

    // Key serializer
    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());

    // Value serializer — JSON
    GenericJackson2JsonRedisSerializer jsonSerializer = 
        new GenericJackson2JsonRedisSerializer();

    template.setValueSerializer(jsonSerializer);
    template.setHashValueSerializer(jsonSerializer);

    template.afterPropertiesSet();
    return template;
}
```

### Storing Objects

```java
@Service
@RequiredArgsConstructor
public class RedisObjectService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void saveUser(User user) {
        String key = "user:" + user.getId();
        redisTemplate.opsForValue().set(key, user);
        redisTemplate.expire(key, 1, TimeUnit.HOURS);
    }

    public User getUser(Long id) {
        String key = "user:" + id;
        Object obj = redisTemplate.opsForValue().get(key);
        return obj != null ? (User) obj : null;
    }
}

// Redis mein aisa dikhega:
// SET user:1 "{\"@class\":\"com.example.User\",\"id\":1,\"name\":\"Sachin\",\"age\":25}"
```

### Custom ObjectMapper

```java
@Bean
public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);

    // Custom ObjectMapper
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());  // Java 8 dates
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    GenericJackson2JsonRedisSerializer serializer = 
        new GenericJackson2JsonRedisSerializer(objectMapper);

    template.setValueSerializer(serializer);
    template.setHashValueSerializer(serializer);
    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());

    template.afterPropertiesSet();
    return template;
}
```

---

## Redis Pub/Sub

**Matlab:** Publish/Subscribe messaging — real-time communication between services.

### Publisher

```java
@Service
@RequiredArgsConstructor
public class RedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(String channel, Object message) {
        redisTemplate.convertAndSend(channel, message);
    }
}
```

### Subscriber

```java
@Component
public class RedisSubscriber {

    public void handleMessage(Object message) {
        System.out.println("Received: " + message);
    }
}
```

### Configuration

```java
@Configuration
public class RedisPubSubConfig {

    @Bean
    public MessageListenerAdapter messageListenerAdapter(RedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "handleMessage");
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory factory,
            MessageListenerAdapter listenerAdapter) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(listenerAdapter, new PatternTopic("my-channel"));
        return container;
    }
}
```

### Usage

```java
// Publish
redisPublisher.publish("my-channel", "Hello from publisher!");

// Subscriber receives: "Received: Hello from publisher!"
```

---

## Redis Streams

**Matlab:** Persistent message queue — Kafka jaisa functionality Redis mein.

### Producer

```java
@Service
@RequiredArgsConstructor
public class RedisStreamProducer {

    private final RedisTemplate<String, Object> redisTemplate;

    public String publish(String stream, Map<String, Object> data) {
        RecordId recordId = redisTemplate.opsForStream()
            .add(StreamRecords.newRecord()
                .in(stream)
                .ofObjects(data));

        return recordId.getValue();
    }
}
```

### Consumer

```java
@Service
@RequiredArgsConstructor
public class RedisStreamConsumer {

    private final RedisTemplate<String, Object> redisTemplate;

    public void consume(String stream, String group, String consumer) {
        // Create consumer group
        try {
            redisTemplate.opsForStream().createGroup(stream, group);
        } catch (Exception e) {
            // Group already exists
        }

        // Read messages
        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
            .read(Consumer.from(group, consumer),
                StreamReadOptions.empty().count(10).block(Duration.ofSeconds(2)),
                StreamOffset.create(stream, ReadOffset.lastConsumed()));

        for (MapRecord<String, Object, Object> record : records) {
            System.out.println("Stream ID: " + record.getId());
            System.out.println("Data: " + record.getValue());

            // Acknowledge message
            redisTemplate.opsForStream().acknowledge(group, record);
        }
    }
}
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **RedisTemplate** | General Redis operations — configurable serializers |
| **StringRedisTemplate** | String-only operations — no casting needed |
| **Lettuce** | Default client — async, thread-safe, reactive support |
| **Jedis** | Legacy client — blocking, needs connection pool |
| **JSON Serialization** | GenericJackson2JsonRedisSerializer — human-readable |
| **Pub/Sub** | Real-time messaging — no persistence |
| **Streams** | Persistent message queue — consumer groups, acknowledgment |

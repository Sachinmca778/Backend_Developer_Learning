# Testcontainers

## Status: Not Started

---

## Table of Contents

1. [Testcontainers Overview](#testcontainers-overview)
2. [Library Setup](#library-setup)
3. [@Testcontainers and @Container](#testcontainers-and-container)
4. [PostgreSQLContainer](#postgresqlcontainer)
5. [MySQLContainer](#mysqlcontainer)
6. [GenericContainer](#genericcontainer)
7. [Lifecycle: Class vs Method Scope](#lifecycle-class-vs-method-scope)
8. [DynamicPropertySource for Config](#dynamicpropertysource-for-config)
9. [Complete Testcontainers Setup](#complete-testcontainers-setup)

---

## Testcontainers Overview

**Matlab:** Docker containers ko tests mein use karna. Real databases, message queues, caching systems — sab Docker containers ke through — tests ke liye.

### Why Testcontainers?

```
H2 In-Memory:
    → Fast, no Docker needed
    → But behavior may differ from production

Testcontainers:
    → Production-identical environment
    → Real PostgreSQL, MySQL, Redis, Kafka
    → Docker containers automatically start/stop
    → Tests ke baad automatic cleanup
```

### What Can You Test?

```
Databases:     PostgreSQL, MySQL, MongoDB, Oracle
Caching:       Redis, Memcached
Message Queues: Kafka, RabbitMQ, ActiveMQ
Search:        Elasticsearch, Solr
Others:        Nginx, LocalStack (AWS), Vault
```

### Prerequisites

```
Docker installed and running
Testcontainers library in test scope
```

---

## Library Setup

### Maven Dependencies

```xml
<!-- Core Testcontainers -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.7</version>
    <scope>test</scope>
</dependency>

<!-- Database modules -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.7</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <version>1.19.7</version>
    <scope>test</scope>
</dependency>

<!-- JUnit 5 extension (for @Testcontainers annotation) -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.7</version>
    <scope>test</scope>
</dependency>

<!-- JDBC Driver (for your database) -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

---

## @Testcontainers and @Container

**Matlab:** Annotations jo Docker containers manage karte hain — start before tests, stop after tests.

### @Testcontainers

```java
@Testcontainers  // Class-level annotation — containers ko manage karta hai
class MyTest {

    // Is class ke saare @Container fields automatically start/stop honge
}
```

### @Container

```java
@Container  // Field-level annotation — yeh ek container hai
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

// @Testcontainers automatically:
// → @BeforeAll pe container start karta hai
// → @AfterAll pe container stop karta hai
```

### Static vs Non-Static Containers

```java
@Testcontainers
class ContainerScopeTest {

    // STATIC @Container — Class-level lifecycle
    // Sirf ek baar start hoga (sab tests share karte hain)
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    // NON-STATIC @Container — Method-level lifecycle
    // Har test ke liye naya container start/stop hoga
    @Container
    RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @Test
    void test1() {
        // postgres → shared instance (fast)
        // redis → new instance for this test
    }

    @Test
    void test2() {
        // postgres → same instance as test1 (still running)
        // redis → brand new instance (previous one stopped)
    }
}
```

---

## PostgreSQLContainer

**Matlab:** PostgreSQL database Docker container tests ke liye.

### Basic Usage

```java
@SpringBootTest
@Testcontainers
class UserRepositoryPostgresTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUser() {
        User user = new User("sachin", "sachin@email.com");
        userRepository.save(user);

        User found = userRepository.findByUsername("sachin").orElseThrow();
        assertEquals("sachin@email.com", found.getEmail());
    }
}
```

### With Custom Database Name

```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
    .withDatabaseName("testdb")
    .withUsername("testuser")
    .withPassword("testpass");

@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
}
```

### With Init Script

```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
    .withInitScript("init-schema.sql");  // src/test/resources/init-schema.sql

// init-schema.sql
// CREATE TABLE custom_types (...);
// INSERT INTO custom_types VALUES (...);
```

### PostgreSQL-Specific Features

```java
@Test
void testPostgresSpecificFeatures() {
    // PostgreSQL specific data types: JSONB, ARRAY, UUID, etc.
    // Yeh H2 mein test nahi hote — real PostgreSQL chahiye

    // JSONB column test
    UserWithJsonb user = new UserWithJsonb("sachin");
    user.setMetadata(Map.of("role", "admin", "department", "engineering"));
    userRepository.save(user);

    UserWithJsonb found = userRepository.findByUsername("sachin").orElseThrow();
    assertEquals("admin", found.getMetadata().get("role"));
}
```

---

## MySQLContainer

**Matlab:** MySQL database Docker container tests ke liye.

### Basic Usage

```java
@SpringBootTest
@Testcontainers
class UserRepositoryMySQLTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldWorkWithMySQL() {
        userRepository.save(new User("sachin", "sachin@email.com"));
        assertTrue(userRepository.existsByEmail("sachin@email.com"));
    }
}
```

### MySQL Dependency

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <version>1.19.7</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>test</scope>
</dependency>
```

---

## GenericContainer

**Matlab:** Koi bhi Docker image run karna — Redis, Kafka, Elasticsearch, ya custom services.

### Redis Container

```java
@SpringBootTest
@Testcontainers
class RedisCacheTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
        DockerImageName.parse("redis:7-alpine")
    )
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void shouldStoreAndRetrieveFromRedis() {
        redisTemplate.opsForValue().set("user:1", "sachin");

        String value = redisTemplate.opsForValue().get("user:1");
        assertEquals("sachin", value);
    }

    @Test
    void shouldExpireKey() throws InterruptedException {
        redisTemplate.opsForValue().set("temp:key", "value", 1, TimeUnit.SECONDS);

        assertNotNull(redisTemplate.opsForValue().get("temp:key"));

        Thread.sleep(1500);  // Wait for expiry

        assertNull(redisTemplate.opsForValue().get("temp:key"));
    }
}
```

### Kafka Container

```java
@SpringBootTest
@Testcontainers
class KafkaProducerConsumerTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    @DynamicPropertySource
    static void configureKafka(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void shouldSendMessageAndReceive() {
        kafkaTemplate.send("test-topic", "key", "Hello from Kafka!");

        // Consumer se verify karo
        // (Kafka consumer setup aur receive logic)
    }
}
```

### Elasticsearch Container

```java
@SpringBootTest
@Testcontainers
class ElasticsearchSearchTest {

    @Container
    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
        DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.11.0")
    )
        .withEnv("discovery.type", "single-node")
        .withEnv("xpack.security.enabled", "false");

    @DynamicPropertySource
    static void configureElasticsearch(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris",
            () -> "http://" + elasticsearch.getHttpHostAddress());
    }

    @Autowired
    private ElasticsearchRestTemplate elasticsearchTemplate;

    @Test
    void shouldIndexAndSearchDocument() {
        // Index document
        Product product = new Product("Laptop", "Electronics", new BigDecimal("999.99"));
        elasticsearchTemplate.save(product);

        // Search
        NativeQuery query = NativeQuery.builder()
            .withQuery(q -> q.match(m -> m.field("name").query("Laptop")))
            .build();

        SearchHits<Product> hits = elasticsearchTemplate.search(query, Product.class);
        assertThat(hits.getTotalHits()).isGreaterThan(0);
    }
}
```

---

## Lifecycle: Class vs Method Scope

**Matlab:** Container kab start/stop hoga — class level (ek baar) ya method level (har test ke liye).

### Class Scope (Recommended for Most Cases)

```java
@Testcontainers
class ClassScopeTest {

    // static → Class-level lifecycle
    // BeforeAll: Start once
    // AfterAll: Stop once
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void test1() {
        // Container already running
        // Fast — reuse same container
    }

    @Test
    void test2() {
        // Same container instance
        // No restart overhead
    }

    @Test
    void test3() {
        // Still same container
    }
}
```

### Method Scope

```java
@Testcontainers
class MethodScopeTest {

    // Non-static → Method-level lifecycle
    // BeforeEach: Start new container
    // AfterEach: Stop container
    @Container
    PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void test1() {
        // Fresh container for this test
        // Isolated from other tests
    }
    // Container stops here

    @Test
    void test2() {
        // Brand new container
        // No state from test1
    }
    // Container stops here
}
```

### Comparison

| Aspect | Class Scope (static) | Method Scope (non-static) |
|--------|---------------------|--------------------------|
| **Speed** | ⚡ Fast (start once) | 🐢 Slow (start each test) |
| **Isolation** | ❌ Tests share state | ✅ Complete isolation |
| **Use when** | Most tests | Tests that modify DB schema |
| **Docker calls** | Minimal | Many (pull/start/stop) |

### Shared Container Across Classes

```java
// Singleton container pattern — multiple test classes share same container
public class PostgresContainer {

    private static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
        POSTGRES.start();
    }

    public static PostgreSQLContainer<?> getInstance() {
        return POSTGRES;
    }
}

// Use in multiple test classes
@SpringBootTest
class TestClass1 {

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        PostgreSQLContainer<?> pg = PostgresContainer.getInstance();
        registry.add("spring.datasource.url", pg::getJdbcUrl);
        registry.add("spring.datasource.username", pg::getUsername);
        registry.add("spring.datasource.password", pg::getPassword);
    }
}

@SpringBootTest
class TestClass2 {
    // Same container — no restart
}
```

---

## DynamicPropertySource for Config

**Matlab:** Spring properties ko dynamically set karna based on container runtime values (random ports, URLs, etc.).

### Why DynamicPropertySource?

```
Container random port pe run hota hai:
    → Port test run time pe decide hoti hai
    → application.properties mein hardcoded nahi ho sakti
    → @DynamicPropertySource se runtime pe set karo
```

### Basic Usage

```java
@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
    // Property name → Supplier (lambda)
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
}

// Equivalent to:
// spring.datasource.url=jdbc:postgresql://localhost:54321/test
// (Port is dynamically assigned by Docker)
```

### Multiple Containers

```java
@Testcontainers
class MultiContainerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
        DockerImageName.parse("redis:7-alpine")
    ).withExposedPorts(6379);

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    @DynamicPropertySource
    static void configureAllProperties(DynamicPropertyRegistry registry) {
        // Database
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Redis
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
}
```

### Custom Logic

```java
@DynamicPropertySource
static void configureWithCustomLogic(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);

    // Custom property
    registry.add("app.feature.new-database-enabled", () -> "true");

    // Conditional property
    registry.add("app.cache.enabled", () -> redis.isRunning());
}
```

---

## Complete Testcontainers Setup

### Project Structure

```
src/test/
├── java/com/example/app/
│   ├── container/
│   │   └── PostgresTestContainer.java    # Singleton container
│   ├── repository/
│   │   └── UserRepositoryTest.java       # Repository tests
│   └── integration/
│       └── UserApiIntegrationTest.java   # Full integration tests
└── resources/
    └── init-schema.sql                   # Database initialization
```

### Singleton Container

```java
public class PostgresTestContainer {

    private static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withInitScript("init-schema.sql");

        // Start container
        POSTGRES.start();

        // JVM shutdown pe stop karo
        Runtime.getRuntime().addShutdownHook(new Thread(POSTGRES::stop));
    }

    public static PostgreSQLContainer<?> getInstance() {
        return POSTGRES;
    }
}
```

### application-test.yml

```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    # These will be overridden by @DynamicPropertySource
    url: jdbc:postgresql://localhost:5432/testdb
    username: testuser
    password: testpass
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

### Test Class

```java
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
class UserApiWithRealDatabaseTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        PostgreSQLContainer<?> pg = PostgresTestContainer.getInstance();
        registry.add("spring.datasource.url", pg::getJdbcUrl);
        registry.add("spring.datasource.username", pg::getUsername);
        registry.add("spring.datasource.password", pg::getPassword);
    }

    @Test
    void shouldCreateUserInRealDatabase() {
        ResponseEntity<UserResponse> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/users",
            new CreateUserRequest("sachin", "sachin@email.com"),
            UserResponse.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
    }
}
```

### Docker Compose for Local Development

```yaml
# docker-compose.yml (local dev, not tests)
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: myapp
      POSTGRES_USER: myuser
      POSTGRES_PASSWORD: mypass
    ports:
      - "5432:5432"

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
```

---

## Quick Reference

```java
// PostgreSQL
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

// MySQL
@Container
static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

// Redis
@Container
static GenericContainer<?> redis = new GenericContainer<>(
    DockerImageName.parse("redis:7-alpine")
).withExposedPorts(6379);

// Dynamic config
@DynamicPropertySource
static void configure(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
}

// Class scope (static) → Start once, shared
// Method scope (non-static) → Each test gets fresh container
```

---

## Summary

| Feature | Use When |
|---------|----------|
| **@Testcontainers** | Enable container management in test class |
| **@Container (static)** | Class-level lifecycle — start once, share across tests |
| **@Container (non-static)** | Method-level lifecycle — fresh container each test |
| **PostgreSQLContainer** | Production PostgreSQL testing |
| **MySQLContainer** | Production MySQL testing |
| **GenericContainer** | Any Docker image (Redis, Kafka, Elasticsearch) |
| **DynamicPropertySource** | Runtime properties set from container values |
| **Singleton pattern** | Share container across multiple test classes |

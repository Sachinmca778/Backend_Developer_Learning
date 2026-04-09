# Integration Testing

## Status: Not Started

---

## Table of Contents

1. [Integration Testing Overview](#integration-testing-overview)
2. [@SpringBootTest with RANDOM_PORT](#springboottest-with-random_port)
3. [TestRestTemplate](#testresttemplate)
4. [WebTestClient (Reactive)](#webtestclient-reactive)
5. [Real DB vs @Transactional Rollback](#real-db-vs-transactional-rollback)
6. [@Sql (Run SQL Before/After Tests)](#sql-run-sql-beforeafter-tests)
7. [H2 In-Memory for Tests](#h2-in-memory-for-tests)
8. [Testcontainers Preview](#testcontainers-preview)

---

## Integration Testing Overview

**Matlab:** Multiple layers ko milake test karna — Controller → Service → Repository → Database. Unit tests sirf ek class test karte hain, integration tests pura flow test karte hain.

### Unit vs Integration

```
Unit Test (@WebMvcTest, @DataJpaTest):
    → Sirf ek layer test hoti hai
    → Baaki layers mocked
    → Fast
    → "Does this class work correctly?"

Integration Test (@SpringBootTest):
    → Multiple layers real hote hain
    → Pura flow test hota hai
    → Slower
    → "Do these layers work together correctly?"
```

### Testing Pyramid

```
        ┌─────────┐
        │  E2E    │  ← Few (slow, expensive)
       ┌┴─────────┴┐
      │Integration │  ← Some (medium speed)
     ┌┴───────────┴┐
    │    Unit      │  ← Many (fast, cheap)
    └──────────────┘
```

---

## @SpringBootTest with RANDOM_PORT

**Matlab:** Real embedded server start karna random port pe — actual HTTP calls test karne ke liye.

### WebEnvironment Options

```java
// MOCK (default) — No real server, MockMvc use hota hai
@SpringBootTest  // Same as @SpringBootTest(webEnvironment = MOCK)

// RANDOM_PORT — Real server on random available port
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

// DEFINED_PORT — Real server on configured port (application.properties)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)

// NONE — No server at all (sirf beans load hote hain)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
```

### RANDOM_PORT Example

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserApiIntegrationTest {

    @LocalServerPort  // Actual random port inject karo
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;  // Auto-configured with random port

    @Test
    @DisplayName("GET /api/users — Should return list of users")
    void shouldReturnUsers() {
        String url = "http://localhost:" + port + "/api/users";

        ResponseEntity<UserResponse[]> response = restTemplate.getForEntity(
            url,
            UserResponse[].class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
```

### Why RANDOM_PORT?

```
DEFINED_PORT:
    → Fixed port (e.g., 8080)
    → CI/CD mein port already occupied ho sakta hai
    → Parallel test execution mein conflict

RANDOM_PORT:
    → Available port automatically milta hai
    → CI/CD mein safe
    → Parallel test execution possible
    → Tests isolated rehte hain
```

---

## TestRestTemplate

**Matlab:** RestTemplate ka test-friendly version. @SpringBootTest(RANDOM_PORT) ke saath auto-configure hota hai.

### Basic Usage

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RestTemplateIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    // GET — Object return
    @Test
    void testGetForObject() {
        UserResponse user = restTemplate.getForObject(
            baseUrl() + "/api/users/1",
            UserResponse.class
        );

        assertNotNull(user);
        assertEquals("sachin", user.getUsername());
    }

    // GET — ResponseEntity return (with status, headers)
    @Test
    void testGetForEntity() {
        ResponseEntity<UserResponse> response = restTemplate.getForEntity(
            baseUrl() + "/api/users/1",
            UserResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("application/json",
            response.getHeaders().getContentType().toString());
    }

    // POST — Create resource
    @Test
    void testPostForObject() {
        CreateUserRequest request = new CreateUserRequest("newuser", "new@email.com");

        UserResponse created = restTemplate.postForObject(
            baseUrl() + "/api/users",
            request,
            UserResponse.class
        );

        assertNotNull(created.getId());
        assertEquals("newuser", created.getUsername());
    }

    // POST — ResponseEntity
    @Test
    void testPostForEntity() {
        CreateUserRequest request = new CreateUserRequest("newuser", "new@email.com");

        ResponseEntity<UserResponse> response = restTemplate.postForEntity(
            baseUrl() + "/api/users",
            request,
            UserResponse.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getHeaders().getLocation());  // Location header
    }

    // PUT — Update resource
    @Test
    void testPut() {
        UpdateUserRequest request = new UpdateUserRequest("updated", "updated@email.com");

        restTemplate.put(
            baseUrl() + "/api/users/1",
            request
        );

        // Verify
        UserResponse user = restTemplate.getForObject(
            baseUrl() + "/api/users/1",
            UserResponse.class
        );

        assertEquals("updated", user.getUsername());
    }

    // DELETE
    @Test
    void testDelete() {
        restTemplate.delete(baseUrl() + "/api/users/1");

        // Verify deleted
        ResponseEntity<UserResponse> response = restTemplate.getForEntity(
            baseUrl() + "/api/users/1",
            UserResponse.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
```

### Error Handling

```java
@Test
void testNotFound() {
    assertThrows(HttpClientErrorException.NotFound.class, () -> {
        restTemplate.getForObject(baseUrl() + "/api/users/999", UserResponse.class);
    });
}

@Test
void testBadRequest() {
    assertThrows(HttpClientErrorException.BadRequest.class, () -> {
        restTemplate.postForObject(baseUrl() + "/api/users",
            Map.of("username", ""),  // Invalid — empty username
            UserResponse.class);
    });
}

@Test
void testErrorResponseHandling() {
    try {
        restTemplate.getForObject(baseUrl() + "/api/users/999", UserResponse.class);
    } catch (HttpClientErrorException e) {
        assertEquals(404, e.getStatusCode().value());
        assertTrue(e.getResponseBodyAsString().contains("not found"));
    }
}
```

### With Authentication

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthenticatedIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    // Basic Auth
    @Test
    void testWithBasicAuth() {
        TestRestTemplate authRest = restTemplate.withBasicAuth("admin", "admin123");

        ResponseEntity<UserResponse[]> response = authRest.getForEntity(
            "http://localhost:" + port + "/api/admin/users",
            UserResponse[].class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // Bearer Token (JWT)
    @Test
    void testWithBearerToken() {
        String token = obtainJwtToken("sachin", "password123");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<UserResponse> response = restTemplate.exchange(
            "http://localhost:" + port + "/api/users/me",
            HttpMethod.GET,
            entity,
            UserResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private String obtainJwtToken(String username, String password) {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/auth/login",
            new AuthRequest(username, password),
            AuthResponse.class
        );

        return response.getBody().accessToken();
    }
}
```

---

## WebTestClient (Reactive)

**Matlab:** WebFlux (reactive Spring Boot) apps ko test karne ka fluent API. MockMvc ka reactive equivalent.

### Setup

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

### WebTestClient with Random Port

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReactiveApiIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;  // Auto-configured with random port

    @Test
    void testGetUser() {
        webTestClient.get()
            .uri("/api/users/1")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(UserResponse.class)
            .value(user -> {
                assertNotNull(user.getId());
                assertEquals("sachin", user.getUsername());
            });
    }

    @Test
    void testCreateUser() {
        CreateUserRequest request = new CreateUserRequest("sachin", "sachin@email.com");

        webTestClient.post()
            .uri("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.id").isNotEmpty()
            .jsonPath("$.username").isEqualTo("sachin")
            .jsonPath("$.email").isEqualTo("sachin@email.com");
    }

    @Test
    void testDeleteUser() {
        webTestClient.delete()
            .uri("/api/users/1")
            .exchange()
            .expectStatus().isOk();

        // Verify deletion
        webTestClient.get()
            .uri("/api/users/1")
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void testListUsers() {
        webTestClient.get()
            .uri("/api/users")
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(UserResponse.class)
            .hasSizeGreaterThan(0)
            .contains(new UserResponse(1L, "sachin", "sachin@email.com"));
    }
}
```

### Fluent API Chaining

```java
@Test
void testFluentAssertions() {
    webTestClient.get()
        .uri("/api/users")
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectHeader().exists("X-Total-Count")
        .expectBodyList(UserResponse.class)
        .value(users -> {
            assertThat(users).isNotEmpty();
            assertThat(users).allMatch(u -> u.getUsername() != null);
        });
}
```

---

## Real DB vs @Transactional Rollback

**Matlab:** Integration tests mein database behavior — real data persist hota hai ya rollback hota hai.

### Default: @Transactional Rollback

```java
@DataJpaTest  // Default: @Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void testWithRollback() {
        // Save user
        userRepository.save(new User("sachin", "sachin@email.com"));

        // Verify
        assertTrue(userRepository.existsByEmail("sachin@email.com"));
    }

    // Test ke baad automatically ROLLBACK hota hai
    // Next test mein "sachin" user nahi hoga
}
```

### Disable Rollback (Real Persistence)

```java
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)  // No transaction
class UserRepositoryNoRollbackTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void testWithRealPersistence() {
        userRepository.save(new User("sachin", "sachin@email.com"));
        // Data actually saved — next test bhi dekh sakta hai
    }
}
```

### @Commit — Changes Persist

```java
@DataJpaTest
class UserRepositoryCommitTest {

    @Test
    @Commit  // Rollback nahi — commit karo
    void testDataPersists() {
        userRepository.save(new User("sachin", "sachin@email.com"));
    }

    @Test
    @Order(2)
    @Commit
    void testCanSeePreviousData() {
        // Pehle test ka data yahan available hai
        assertTrue(userRepository.existsByEmail("sachin@email.com"));
    }
}
```

### @Rollback(false)

```java
@Test
@Rollback(false)  // Same as @Commit
void testWithoutRollback() {
    // Data persists after test
}
```

---

## @Sql (Run SQL Before/After Tests)

**Matlab:** Test se pehle ya baad mein SQL scripts run karna. Test data seed karne ka declarative way.

### Basic @Sql Usage

```java
@DataJpaTest
class UserRepositoryWithSqlTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @Sql("/test-data/users.sql")  // Test se pehle yeh SQL run hoga
    void shouldFindAllUsers() {
        List<User> users = userRepository.findAll();

        assertThat(users).hasSize(3);  // users.sql mein 3 users defined hain
    }
}
```

### SQL File (src/test/resources/test-data/users.sql)

```sql
-- Clean existing data
DELETE FROM posts;
DELETE FROM users;

-- Insert test data
INSERT INTO users (id, username, email, enabled) VALUES
    (1, 'sachin', 'sachin@email.com', true),
    (2, 'john', 'john@email.com', true),
    (3, 'jane', 'jane@email.com', false);
```

### Multiple @Sql Scripts

```java
@Test
@Sql({"/test-data/users.sql", "/test-data/posts.sql"})  // Multiple scripts in order
void shouldFindUsersWithPosts() {
    List<User> users = userRepository.findAll();
    assertThat(users).hasSize(3);
}
```

### Execution Phase

```java
// Default: BEFORE_TEST_METHOD
@Test
@Sql("/test-data/setup.sql")  // Test se pehle run
void testWithSetupData() { }

// AFTER_TEST_METHOD — Test ke baad cleanup
@Test
@Sql(statements = "DELETE FROM test_results", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
void testThatCreatesData() {
    // Test data create karo
    // Test ke baad automatically cleanup hoga
}
```

### Inline SQL Statements

```java
@Test
@Sql(statements = {
    "DELETE FROM users",
    "INSERT INTO users (id, username, email, enabled) VALUES (1, 'sachin', 'sachin@email.com', true)"
})
void shouldFindSpecificUser() {
    User user = userRepository.findById(1L).orElseThrow();
    assertEquals("sachin", user.getUsername());
}
```

### Class-Level @Sql

```java
@DataJpaTest
@Sql("/test-data/base-data.sql")  // Har test se pehle run hoga
class AllTestsNeedBaseDataTest {

    @Test
    void test1() {
        // base-data.sql already loaded
    }

    @Test
    void test2() {
        // base-data.sql already loaded
    }

    @Test
    @Sql("/test-data/extra-data.sql")  // Additional data for this test only
    void test3() {
        // base-data.sql + extra-data.sql both loaded
    }
}
```

---

## H2 In-Memory for Tests

**Matlab:** Tests ke liye lightweight in-memory database. Fast, isolated, no external setup needed.

### Maven Dependency

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>  // Sirf tests ke liye
</dependency>
```

### application.properties (Test)

```properties
# src/test/resources/application.properties

# H2 in-memory database
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=create-drop  # Tables create karo, end mein drop karo
spring.jpa.show-sql=true                    # SQL queries console pe dikhao
spring.jpa.properties.hibernate.format_sql=true  # Formatted SQL

# H2 Console (optional — debugging ke liye)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

### DDL-Auto Options

```properties
# create-drop — Start pe create, end pe drop (@DataJpaTest default)
spring.jpa.hibernate.ddl-auto=create-drop

# create — Har run pe fresh tables (data lost)
spring.jpa.hibernate.ddl-auto=create

# update — Existing tables ko update karo (data preserved)
spring.jpa.hibernate.ddl-auto=update

# validate — Sirf schema validate karo (no changes)
spring.jpa.hibernate.ddl-auto=validate

# none — Kuch mat karo
spring.jpa.hibernate.ddl-auto=none
```

### H2 Console for Debugging

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryDebugTest {

    // H2 console: http://localhost:8080/h2-console
    // JDBC URL: jdbc:h2:mem:testdb
    // Username: sa
    // Password: (blank)

    @Test
    void testWithDebug() {
        // H2 console mein live data dekh sakte ho
        userRepository.save(new User("sachin", "sachin@email.com"));
        // Go to h2-console → SELECT * FROM users → Data dikh raha hoga
    }
}
```

### H2 vs Production DB Differences

```
⚠️ Warning: H2 behavior may differ from production database

Common differences:
    → Data types (H2 more lenient)
    → SQL dialect variations
    → Index behavior
    → Transaction isolation levels
    → Case sensitivity

Solution: Testcontainers use karo for production-like testing
```

---

## Testcontainers Preview

**Matlab:** Real database (PostgreSQL, MySQL, etc.) Docker container mein run karna. Production jaisa environment.

### Why Testcontainers?

```
H2 In-Memory:
    ✅ Fast
    ✅ No Docker needed
    ❌ May differ from production DB
    ❌ PostgreSQL-specific features test nahi hote

Testcontainers:
    ✅ Production-identical database
    ✅ Real PostgreSQL/MySQL behavior
    ❌ Docker required
    ❌ Slower than H2
```

### Quick Example

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

```java
@SpringBootTest
@Testcontainers
class UserApiWithPostgresTest {

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
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Test
    void shouldWorkWithRealPostgres() {
        ResponseEntity<UserResponse> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/users/1",
            UserResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
```

---

## Quick Reference

```java
// Random port integration test
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTest {
    @LocalServerPort int port;
    @Autowired TestRestTemplate restTemplate;
}

// WebTestClient (reactive)
webTestClient.get().uri("/api/users/1")
    .exchange()
    .expectStatus().isOk()
    .expectBody().jsonPath("$.username").isEqualTo("sachin");

// Transactional rollback
@DataJpaTest  // Auto @Transactional — test data rollback hota hai

// @Sql — run SQL scripts
@Test
@Sql("/test-data/users.sql")  // Before test
@Sql(statements = "DELETE FROM temp", executionPhase = AFTER_TEST_METHOD)  // After test

// H2 config (src/test/resources/application.properties)
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
```

---

## Summary

| Feature | Use When |
|---------|----------|
| **RANDOM_PORT** | Real HTTP integration testing |
| **TestRestTemplate** | Calling actual REST endpoints |
| **WebTestClient** | Reactive (WebFlux) API testing |
| **@Transactional rollback** | Test data cleanup (default behavior) |
| **@Sql** | Test data seeding via SQL scripts |
| **H2 in-memory** | Fast, lightweight tests |
| **Testcontainers** | Production-like database testing |

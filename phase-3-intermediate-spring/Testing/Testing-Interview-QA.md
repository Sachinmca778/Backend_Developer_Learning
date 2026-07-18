# Testing - Top 10 Interview Questions & Answers

> Covering: JUnit 5, Mockito, Spring Boot Test Slices, MockMvc, Integration Testing, and Testcontainers

---

## Q1: JUnit 5 kya hai aur basic test kaise likhte hain?

**Answer:**

JUnit 5 (Jupiter) Java ka modern testing framework hai.

### Architecture:

```
JUnit 5 = JUnit Platform + JUnit Jupiter + JUnit Vintage
```

| Component | Purpose |
|-----------|---------|
| **JUnit Platform** | Test run karta hai (Maven, IDE) |
| **JUnit Jupiter** | Modern annotations (@Test, @BeforeEach) |
| **JUnit Vintage** | JUnit 3/4 backward compatibility |

### Dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

### Basic Test:

```java
class CalculatorTest {

    @Test
    void testAddition() {
        Calculator calc = new Calculator();
        assertEquals(5, calc.add(2, 3));
    }

    @Test
    void testSubtraction() {
        Calculator calc = new Calculator();
        assertEquals(6, calc.subtract(10, 4));
    }
}
```

### @Test Rules:

- Method `public` nahi hona chahiye (package-private OK)
- `static` nahi hona chahiye
- `void` return type
- Descriptive names: `shouldReturnUserWhenIdExists()`

### Project Structure:

```
src/test/java/
└── com/example/app/
    ├── service/UserServiceTest.java
    ├── controller/UserControllerTest.java
    └── repository/UserRepositoryTest.java
```

---

## Q2: JUnit 5 test lifecycle annotations kya hain?

**Answer:**

### Lifecycle Order:

```
@BeforeAll    → Class level, ek baar (static)
    ↓
@BeforeEach  → Har test se pehle
    ↓
@Test        → Test execute
    ↓
@AfterEach   → Har test ke baad
    ↓
(repeat for each test)
    ↓
@AfterAll    → Class level, sab tests ke baad (static)
```

### Example:

```java
class UserServiceTest {

    private UserService userService;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        userService = new UserService(userRepository);
        userRepository.save(new User("sachin", "sachin@email.com"));
    }

    @AfterEach
    void tearDown() {
        // Cleanup after each test
    }

    @Test
    void shouldFindAllUsers() {
        assertEquals(1, userService.getAllUsers().size());
    }
}
```

### Other Useful Annotations:

| Annotation | Purpose |
|------------|---------|
| **@Disabled** | Test skip karo |
| **@DisplayName** | Readable test name |
| **@Nested** | Group related tests |
| **@ParameterizedTest** | Same test, different inputs |
| **@RepeatedTest** | Test multiple times run karo |

### Parameterized Tests:

```java
@ParameterizedTest
@ValueSource(strings = {"sachin", "john", "admin"})
void shouldAcceptValidUsernames(String username) {
    assertTrue(userService.isValidUsername(username));
}

@ParameterizedTest
@CsvSource({"1, Sachin, sachin@email.com", "2, John, john@email.com"})
void shouldFindUserById(Long id, String name, String email) {
    User user = userService.findById(id);
    assertEquals(name, user.getName());
}
```

---

## Q3: Mockito kya hai aur @Mock, @InjectMocks kaise use karte hain?

**Answer:**

Mockito dependencies ko mock (fake) karke unit tests isolate karta hai — real database ya external services ki zarurat nahi.

### Why Mocking:

```
Without Mock: UserServiceTest → UserRepository → Database (slow, setup needed)
With Mock:    UserServiceTest → Mock UserRepository (fast, isolated)
```

### Setup:

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldFindUserByUsername() {
        User mockUser = new User("sachin", "sachin@email.com");
        when(userRepository.findByUsername("sachin")).thenReturn(Optional.of(mockUser));

        User user = userService.findByUsername("sachin");

        assertNotNull(user);
        assertEquals("sachin", user.getUsername());
    }
}
```

### @Mock vs @InjectMocks:

| Annotation | Purpose |
|------------|---------|
| **@Mock** | Fake dependency create karo |
| **@InjectMocks** | Mocks ko real class mein inject karo |

### Default Mock Returns:

| Type | Default Return |
|------|---------------|
| Objects | `null` |
| Primitives | `0`, `false` |
| Collections | Empty |
| Optional | `Optional.empty()` |

### Stubbing:

```java
when(userRepository.findById(1L)).thenReturn(Optional.of(user));
when(userRepository.findById(999L)).thenReturn(Optional.empty());
when(userRepository.save(any(User.class))).thenReturn(savedUser);
when(userRepository.deleteById(1L)).thenThrow(new DataAccessException("DB error") {});
doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());
```

---

## Q4: Mockito mein verify() aur ArgumentMatchers kaise use karte hain?

**Answer:**

### verify() — Method Call Check:

```java
@Test
void shouldSaveUserWhenCreating() {
    userService.createUser(new CreateUserRequest("sachin", "sachin@email.com"));

    verify(userRepository).save(any(User.class));
    verify(userRepository, times(1)).save(any(User.class));
    verify(emailService, never()).sendErrorEmail(anyString());
}
```

### Verification Options:

| Method | Meaning |
|--------|---------|
| `verify(mock).method()` | Called at least once |
| `verify(mock, times(1)).method()` | Called exactly once |
| `verify(mock, never()).method()` | Never called |
| `verify(mock, atLeast(2)).method()` | At least 2 times |
| `verify(mock, atMost(5)).method()` | At most 5 times |

### ArgumentMatchers:

```java
when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
when(userRepository.save(any(User.class))).thenReturn(savedUser);
when(userRepository.findByAge(greaterThan(18))).thenReturn(users);
when(userRepository.findByEmail(eq("sachin@email.com"))).thenReturn(Optional.of(user));
when(userRepository.findByName(contains("sach"))).thenReturn(users);
```

### ArgumentCaptor — Capture Arguments:

```java
@Captor
private ArgumentCaptor<User> userCaptor;

@Test
void shouldSaveUserWithCorrectData() {
    userService.createUser(new CreateUserRequest("sachin", "sachin@email.com"));

    verify(userRepository).save(userCaptor.capture());
    User captured = userCaptor.getValue();
    assertEquals("sachin", captured.getUsername());
    assertEquals("sachin@email.com", captured.getEmail());
}
```

### @Spy — Partial Mock:

```java
@Spy
private List<String> list = new ArrayList<>();

@Test
void testSpy() {
    list.add("real");           // Real method call
    when(list.size()).thenReturn(100);  // Override specific method
    assertEquals(100, list.size());
}
```

---

## Q5: Spring Boot Test Slices kya hain — @WebMvcTest aur @DataJpaTest?

**Answer:**

Test Slices sirf specific layer load karte hain — pura application context nahi. Tests fast hote hain.

### Available Slices:

| Annotation | Loads | Auto-Mocks | Use For |
|------------|-------|------------|---------|
| **@WebMvcTest** | Controllers, Filters | Services, Repos | REST API tests |
| **@DataJpaTest** | JPA, Hibernate, H2 | Everything else | Repository tests |
| **@JsonTest** | JSON serializers | Everything | JSON serialization |
| **@SpringBootTest** | Full context | Nothing (real beans) | Integration tests |

### @WebMvcTest:

```java
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void shouldReturnUser() throws Exception {
        when(userService.findById(1L)).thenReturn(new User(1L, "sachin", "sachin@email.com"));

        mockMvc.perform(get("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("sachin"));
    }
}
```

### @DataJpaTest:

```java
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindByUsername() {
        entityManager.persist(new User("sachin", "sachin@email.com"));

        Optional<User> found = userRepository.findByUsername("sachin");
        assertTrue(found.isPresent());
    }
}
```

**Note:** `@DataJpaTest` automatically H2 in-memory database use karta hai.

---

## Q6: @MockBean aur @SpyBean — @Mock se kya difference hai?

**Answer:**

| Feature | @Mock (Mockito) | @MockBean (Spring) |
|---------|-----------------|---------------------|
| **Scope** | Plain unit test | Spring context test |
| **Registration** | Test class only | Spring ApplicationContext |
| **Use With** | `@ExtendWith(MockitoExtension.class)` | `@WebMvcTest`, `@SpringBootTest` |
| **Replaces Bean** | No | Yes — real bean ko replace karta hai |

### @MockBean:

```java
@WebMvcTest(UserController.class)
class UserControllerTest {

    @MockBean  // Spring context mein UserService ko mock se replace
    private UserService userService;

    @Autowired
    private MockMvc mockMvc;
}
```

### @SpyBean:

```java
@SpringBootTest
class UserServiceIntegrationTest {

    @SpyBean  // Real bean, but specific methods override kar sakte ho
    private EmailService emailService;

    @Test
    void shouldNotSendRealEmail() {
        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());
        // Real EmailService bean, but sendEmail mocked
    }
}
```

**Rule:** Spring context tests mein `@MockBean`/`@SpyBean` use karo. Plain unit tests mein `@Mock`/`@Spy`.

---

## Q7: MockMvc se REST API kaise test karte hain?

**Answer:**

MockMvc HTTP requests simulate karta hai bina real server start kiye.

### Basic Test:

```java
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void shouldCreateUser() throws Exception {
        User created = new User(1L, "sachin", "sachin@email.com");
        when(userService.createUser(any())).thenReturn(created);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username": "sachin", "email": "sachin@email.com"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("sachin"))
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldReturn400ForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username": "", "email": "invalid"}
                    """))
            .andExpect(status().isBadRequest());
    }
}
```

### Common Assertions:

```java
.andExpect(status().isOk())                    // 200
.andExpect(status().isCreated())               // 201
.andExpect(status().isNotFound())              // 404
.andExpect(status().isBadRequest())            // 400
.andExpect(jsonPath("$.username").value("sachin"))
.andExpect(jsonPath("$.items").isArray())
.andExpect(jsonPath("$.items.length()").value(3))
.andExpect(header().string("Location", containsString("/api/users/1")))
```

### HTTP Methods:

```java
mockMvc.perform(get("/api/users/1"))       // GET
mockMvc.perform(post("/api/users"))        // POST
mockMvc.perform(put("/api/users/1"))       // PUT
mockMvc.perform(delete("/api/users/1"))    // DELETE
mockMvc.perform(patch("/api/users/1"))     // PATCH
```

---

## Q8: @SpringBootTest integration testing kaise karte hain?

**Answer:**

`@SpringBootTest` pura application context load karta hai — multiple layers real hote hain.

### WebEnvironment Options:

| Option | Description |
|--------|-------------|
| **MOCK** (default) | No real server, MockMvc |
| **RANDOM_PORT** | Real server on random port |
| **DEFINED_PORT** | Real server on configured port |
| **NONE** | No server, only beans |

### Integration Test with TestRestTemplate:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldCreateAndGetUser() {
        CreateUserRequest request = new CreateUserRequest("sachin", "sachin@email.com");

        ResponseEntity<UserResponse> createResponse = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/users",
            request,
            UserResponse.class
        );

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());

        UserResponse user = restTemplate.getForObject(
            "http://localhost:" + port + "/api/users/" + createResponse.getBody().getId(),
            UserResponse.class
        );

        assertEquals("sachin", user.getUsername());
    }
}
```

### @Transactional Rollback:

```java
@SpringBootTest
@Transactional  // Har test ke baad rollback — DB clean rehta hai
class UserServiceIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveUser() {
        userRepository.save(new User("sachin", "sachin@email.com"));
        assertEquals(1, userRepository.count());
    }
    // Test ke baad automatically rollback
}
```

### @Sql for Test Data:

```java
@Test
@Sql("/test-data/users.sql")
void shouldFindUsers() {
    assertEquals(3, userRepository.count());
}

@Test
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
void shouldCleanupAfterTest() { }
```

---

## Q9: Unit Test vs Integration Test — kab kya use karein?

**Answer:**

### Testing Pyramid:

```
        ┌─────────┐
        │  E2E    │  ← Few (slow)
       ┌┴─────────┴┐
      │Integration │  ← Some (medium)
     ┌┴───────────┴┐
    │    Unit      │  ← Many (fast)
    └──────────────┘
```

### Comparison:

| Feature | Unit Test | Integration Test |
|---------|-----------|-----------------|
| **Scope** | Single class/method | Multiple layers |
| **Dependencies** | Mocked | Real (or test DB) |
| **Speed** | Milliseconds | Seconds |
| **Annotation** | `@ExtendWith(MockitoExtension.class)` | `@SpringBootTest` |
| **Question** | "Does this class work?" | "Do layers work together?" |

### When to Use What:

| Scenario | Test Type | Annotation |
|----------|-----------|------------|
| Service business logic | Unit | `@Mock` + `@InjectMocks` |
| Controller HTTP mapping | Slice | `@WebMvcTest` + MockMvc |
| Repository queries | Slice | `@DataJpaTest` + H2 |
| Full API flow | Integration | `@SpringBootTest` + TestRestTemplate |
| JSON serialization | Slice | `@JsonTest` |

### Example Strategy:

```
UserService business logic     → Unit test (Mockito)
UserController REST endpoints  → @WebMvcTest (MockMvc)
UserRepository DB queries      → @DataJpaTest (H2)
Full user registration flow    → @SpringBootTest (Integration)
```

**Short Interview Answer:**

Unit tests fast aur isolated — mock dependencies. Integration tests slower but real flow verify karte hain. Pyramid: zyada unit, kam integration, bahut kam E2E.

---

## Q10: Testcontainers kya hai aur kaise use karte hain?

**Answer:**

Testcontainers Docker containers ko tests mein use karta hai — real PostgreSQL, MySQL, Redis jaise production-identical environment.

### Why Testcontainers over H2?

| Feature | H2 | Testcontainers |
|---------|-----|----------------|
| Speed | Fast | Slower (Docker startup) |
| Accuracy | May differ from prod DB | Production-identical |
| SQL dialect | H2-specific | Real PostgreSQL/MySQL |
| Docker needed | No | Yes |

### Setup:

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

### PostgreSQL Example:

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

### Container Lifecycle:

| Scope | Behavior |
|-------|----------|
| **static @Container** | Class-level — ek baar start, sab tests share |
| **non-static @Container** | Method-level — har test ke liye naya container |

### @DynamicPropertySource:

Spring properties runtime pe set karta hai — container ke dynamic port/username/password ke saath.

**Best Practice:** Static container use karo class-level tests ke liye — faster (container ek baar start).

**Short Interview Answer:**

Testcontainers = real Docker databases in tests. `@Container` + `@DynamicPropertySource` + `@SpringBootTest` = production-like integration tests without mocking the database.

# Spring Boot Test Slices

## Status: Not Started

---

## Table of Contents

1. [Test Slices Overview](#test-slices-overview)
2. [@WebMvcTest (Web Layer)](#webmvctest-web-layer)
3. [@DataJpaTest (JPA Layer)](#datajpatest-jpa-layer)
4. [@JsonTest](#jsontest)
5. [@RestClientTest](#restclienttest)
6. [@SpringBootTest (Full Context)](#springboottest-full-context)
7. [@MockBean](#mockbean)
8. [@SpyBean](#spybean)
9. [TestEntityManager](#testentitymanager)

---

## Test Slices Overview

**Matlab:** Spring Boot ka feature jo sirf specific layer ko load karta hai — pura application context nahi. Tests fast hote hain kyunki sirf needed beans load hote hain.

### Full Context vs Slice

```
@SpringBootTest (Full Context):
    → Pura Spring Boot app load hota hai
    → All controllers, services, repositories, databases
    → Slow (5-15 seconds startup)
    → Integration testing ke liye

@Test Slice (e.g., @WebMvcTest):
    → Sirf web layer load hoti hai
    → Controllers + validation + serialization
    → Services, repositories → MOCKED (automatically)
    → Fast (1-3 seconds startup)
    → Unit testing ke liye
```

### Available Test Slices

| Annotation | Loads | Auto-Mocks | Use For |
|------------|-------|------------|---------|
| **@WebMvcTest** | Controllers, Filters, Validators | Services, Repositories | REST API tests |
| **@DataJpaTest** | JPA, Hibernate, H2 | Everything else | Repository tests |
| **@JsonTest** | JSON serializers | Everything | JSON serialization |
| **@RestClientTest** | RestTemplate, WebClient | Everything | HTTP client tests |
| **@WebFluxTest** | WebFlux controllers | Everything | Reactive API tests |
| **@JdbcTest** | JdbcTemplate | Everything | JDBC repository tests |
| **@DataMongoTest** | MongoDB repositories | Everything | MongoDB tests |

---

## @WebMvcTest (Web Layer)

**Matlab:** Sirf Spring MVC layer test karna — Controllers, Validation, JSON serialization. Services aur Repositories automatically mock hote hain.

### Basic Usage

```java
// Real class
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(UserResponse.fromUser(user));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(UserResponse.fromUser(user));
    }
}
```

```java
@WebMvcTest(UserController.class)  // Sirf UserController load hoga
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;  // Auto-configured

    @MockBean  // UserService ko mock karo
    private UserService userService;

    @Test
    @DisplayName("GET /api/users/{id} — Should return user when found")
    void shouldReturnUser() throws Exception {
        // Given
        User user = new User(1L, "sachin", "sachin@email.com");
        when(userService.findById(1L)).thenReturn(user);

        // When & Then
        mockMvc.perform(get("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("sachin"))
            .andExpect(jsonPath("$.email").value("sachin@email.com"));
    }

    @Test
    @DisplayName("GET /api/users/{id} — Should return 404 when not found")
    void shouldReturn404WhenNotFound() throws Exception {
        when(userService.findById(999L)).thenThrow(new UserNotFoundException());

        mockMvc.perform(get("/api/users/999")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/users — Should create user")
    void shouldCreateUser() throws Exception {
        User created = new User(1L, "sachin", "sachin@email.com");
        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "username": "sachin",
                        "email": "sachin@email.com"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("sachin"))
            .andExpect(jsonPath("$.id").value(1));
    }
}
```

### Multiple Controllers

```java
// Multiple controllers test karni hain
@WebMvcTest({UserController.class, AuthController.class})
class ControllersTest {

    @MockBean
    private UserService userService;

    @MockBean
    private AuthService authService;

    @Autowired
    private MockMvc mockMvc;
}
```

### Validation Testing

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        // @Valid validation pehle run hogi
        User user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(UserResponse.fromUser(user));
    }
}

public record CreateUserRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be 3-20 characters")
    String username,

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    String email
) {}
```

```java
@WebMvcTest(UserController.class)
class UserControllerValidationTest {

    @MockBean
    private UserService userService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should return 400 for invalid request")
    void shouldReturn400ForInvalidRequest() throws Exception {
        // Empty username — validation fail hoga
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "username": "",
                        "email": "not-an-email"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.username").exists())  // Validation errors
            .andExpect(jsonPath("$.email").exists());
    }
}
```

---

## @DataJpaTest (JPA Layer)

**Matlab:** Sirf JPA layer test karna — Repositories, Entities, Relationships. In-memory H2 database automatically use hoti hai.

### Basic Usage

```java
// Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.enabled = true")
    List<User> findAllEnabledUsers();
}
```

```java
@DataJpaTest  // Sirf JPA layer — UserRepository auto-injected
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager testEntityManager;  // Test helper

    @Test
    @DisplayName("Should find user by username")
    void shouldFindByUsername() {
        // Given — directly save (H2 in-memory DB)
        User user = new User("sachin", "sachin@email.com");
        user.setEnabled(true);
        userRepository.save(user);

        // When
        Optional<User> found = userRepository.findByUsername("sachin");

        // Then
        assertTrue(found.isPresent());
        assertEquals("sachin@email.com", found.get().getEmail());
    }

    @Test
    @DisplayName("Should check email exists")
    void shouldCheckEmailExists() {
        userRepository.save(new User("sachin", "sachin@email.com"));

        assertTrue(userRepository.existsByEmail("sachin@email.com"));
        assertFalse(userRepository.existsByEmail("john@email.com"));
    }

    @Test
    @DisplayName("Should return only enabled users")
    void shouldReturnOnlyEnabledUsers() {
        User enabledUser = new User("sachin", "sachin@email.com");
        enabledUser.setEnabled(true);
        userRepository.save(enabledUser);

        User disabledUser = new User("john", "john@email.com");
        disabledUser.setEnabled(false);
        userRepository.save(disabledUser);

        List<User> enabledUsers = userRepository.findAllEnabledUsers();

        assertThat(enabledUsers).hasSize(1);
        assertThat(enabledUsers.get(0).getUsername()).isEqualTo("sachin");
    }
}
```

### @DataJpaTest Behavior

```
@DataJpaTest automatically:
    ✅ Configures H2 in-memory database
    ✅ Scans @Entity classes
    ✅ Configures Spring Data repositories
    ✅ Wraps each test in @Transactional (auto-rollback)
    ❌ Does NOT load @Service beans
    ❌ Does NOT load @Controller beans
    ❌ Does NOT load @Component beans
```

### Test with Relationships

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Post> posts = new HashSet<>();

    public void addPost(Post post) {
        posts.add(post);
        post.setUser(this);
    }
}

@Entity
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
```

```java
@DataJpaTest
class UserPostRelationshipTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveUserWithPosts() {
        User user = new User("sachin", "sachin@email.com");
        user.addPost(new Post("First Post", "Hello World"));
        user.addPost(new Post("Second Post", "Testing"));

        userRepository.save(user);

        // Fetch with relationship
        User saved = userRepository.findById(user.getId()).get();

        assertThat(saved.getPosts()).hasSize(2);
        assertThat(saved.getPosts())
            .extracting(Post::getTitle)
            .containsExactlyInAnyOrder("First Post", "Second Post");
    }
}
```

---

## @JsonTest

**Matlab:** JSON serialization/deserialization test karna. Jackson ObjectMapper configuration verify karna.

### Basic Usage

```java
@JsonTest  // Only JSON serialization beans loaded
class UserJsonTest {

    @Autowired
    private JacksonTester<UserResponse> json;

    @Test
    @DisplayName("Should serialize user to JSON")
    void shouldSerializeUser() throws Exception {
        UserResponse user = new UserResponse(1L, "sachin", "sachin@email.com");

        // Object → JSON
        assertThat(json.write(user))
            .extractingJsonPathStringValue("$.username")
            .isEqualTo("sachin");

        assertThat(json.write(user))
            .extractingJsonPathNumberValue("$.id")
            .isEqualTo(1);

        // Full JSON comparison
        assertThat(json.write(user))
            .isEqualToJson("""
                {
                    "id": 1,
                    "username": "sachin",
                    "email": "sachin@email.com"
                }
                """);
    }

    @Test
    @DisplayName("Should deserialize JSON to object")
    void shouldDeserializeJson() throws Exception {
        String jsonContent = """
            {
                "id": 1,
                "username": "sachin",
                "email": "sachin@email.com"
            }
            """;

        UserResponse user = json.parseObject(jsonContent);

        assertEquals(1L, user.getId());
        assertEquals("sachin", user.getUsername());
        assertEquals("sachin@email.com", user.getEmail());
    }
}
```

### Custom Serializer/Deserializer Test

```java
public class UserResponse {

    private Long id;
    private String username;
    private String email;

    @JsonSerialize(using = CustomDateSerializer.class)
    private LocalDateTime createdAt;
}

@JsonTest
class CustomSerializerTest {

    @Autowired
    private JacksonTester<UserResponse> json;

    @Test
    void shouldSerializeDateCorrectly() throws Exception {
        UserResponse user = new UserResponse(1L, "sachin", "sachin@email.com");
        user.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 30, 0));

        assertThat(json.write(user))
            .extractingJsonPathStringValue("$.createdAt")
            .isEqualTo("2024-01-15T10:30:00");
    }
}
```

---

## @RestClientTest

**Matlab:** REST client (RestTemplate, WebClient) test karna. External API calls mock karne ke liye.

### REST Client

```java
@Service
@RequiredArgsConstructor
public class ExternalApiClient {

    private final RestTemplate restTemplate;

    @Value("${external.api.url}")
    private String baseUrl;

    public ExternalUser getUser(Long id) {
        return restTemplate.getForObject(
            baseUrl + "/users/" + id,
            ExternalUser.class
        );
    }

    public ExternalUser createUser(ExternalUser user) {
        return restTemplate.postForObject(
            baseUrl + "/users",
            user,
            ExternalUser.class
        );
    }
}
```

### RestClient Test

```java
@RestClientTest(ExternalApiClient.class)
class ExternalApiClientTest {

    @Autowired
    private ExternalApiClient apiClient;

    @Autowired
    private MockRestServiceServer mockServer;  // Mock external API

    @Test
    @DisplayName("Should fetch user from external API")
    void shouldFetchUser() {
        // Mock external API response
        mockServer.expect(MockRestRequestMatchers.requestTo("/api/users/1"))
            .andRespond(MockRestResponseCreators.withSuccess("""
                {
                    "id": 1,
                    "name": "Sachin Kumar",
                    "email": "sachin@example.com"
                }
                """, MediaType.APPLICATION_JSON));

        // Test
        ExternalUser user = apiClient.getUser(1L);

        assertNotNull(user);
        assertEquals("Sachin Kumar", user.getName());
        assertEquals("sachin@example.com", user.getEmail());

        mockServer.verify();  // Verify all expectations were met
    }

    @Test
    @DisplayName("Should handle external API error")
    void shouldHandleExternalApiError() {
        mockServer.expect(requestTo("/api/users/999"))
            .andRespond(withServerError());

        assertThrows(RestClientException.class,
            () -> apiClient.getUser(999L));

        mockServer.verify();
    }
}
```

---

## @SpringBootTest (Full Context)

**Matlab:** Pura Spring Boot application context load karta hai — sab beans, sab layers. Integration testing ke liye.

### Basic Usage

```java
@SpringBootTest  // Pura application context
class FullIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should create user and save to database")
    void shouldCreateUserAndSave() {
        User user = userService.createUser("sachin", "sachin@email.com");

        assertNotNull(user.getId());
        assertTrue(userRepository.existsByEmail("sachin@email.com"));
    }
}
```

### With Random Port

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RestApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;  // Auto-configured for random port

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("Should call actual HTTP endpoint")
    void shouldCallActualEndpoint() {
        ResponseEntity<UserResponse> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/users/1",
            UserResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
```

### @SpringBootTest vs @WebMvcTest

```
@SpringBootTest:
    ✅ Full integration — controller → service → repository → database
    ✅ Real dependencies test hoti hain
    ❌ Slow (pura context load hota hai)
    ❌ External dependencies mock karni padti hain

@WebMvcTest:
    ✅ Fast (sirf web layer)
    ✅ Services automatically mocked
    ❌ Service/Repository integration test nahi hota
    ❌ Sirf controller logic test hota hai
```

---

## @MockBean

**Matlab:** Spring context mein mock bean add karna. Real bean ki jagah mock inject hota hai.

### @MockBean vs @Mock

```
@Mock (Mockito):
    → Sirf test class ke andar kaam karta hai
    → Spring context mein register nahi hota
    → @WebMvcTest, @DataJpaTest ke saath use nahi hota

@MockBean (Spring Boot):
    → Spring ApplicationContext mein mock bean add karta hai
    → Real bean replace ho jata hai mock se
    → @SpringBootTest, @WebMvcTest, @DataJpaTest ke saath use hota hai
```

### Usage in @SpringBootTest

```java
@SpringBootTest
class UserServiceWithExternalDependencyTest {

    @MockBean  // EmailService ka mock Spring context mein add karo
    private EmailService emailService;

    @Autowired
    private UserService userService;  // Isme mock EmailService inject hoga

    @Test
    void shouldCreateUserAndSendEmail() {
        doNothing().when(emailService).sendWelcomeEmail(any(User.class));

        User user = userService.createUser("sachin", "sachin@email.com");

        assertNotNull(user);
        verify(emailService).sendWelcomeEmail(any(User.class));
    }
}
```

### Usage in @WebMvcTest

```java
@WebMvcTest(UserController.class)
class UserControllerTest {

    @MockBean  // UserService mock — Spring context mein register
    private UserService userService;

    @Autowired
    private MockMvc mockMvc;  // MockMvc auto-configured

    @Test
    void shouldReturnUser() throws Exception {
        when(userService.findById(1L)).thenReturn(new User(1L, "sachin", "sachin@email.com"));

        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("sachin"));
    }
}
```

### Multiple MockBeans

```java
@SpringBootTest
class OrderProcessingTest {

    @MockBean
    private PaymentGateway paymentGateway;

    @MockBean
    private InventoryService inventoryService;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private OrderService orderService;

    @Test
    void shouldProcessOrder() {
        when(inventoryService.checkStock(1L)).thenReturn(true);
        when(paymentGateway.charge(any())).thenReturn(true);
        doNothing().when(notificationService).sendConfirmation(any());

        Order order = orderService.processOrder(new OrderRequest(1L, 100));

        assertNotNull(order);
        verify(paymentGateway).charge(any());
        verify(notificationService).sendConfirmation(any());
    }
}
```

---

## @SpyBean

**Matlab:** Real bean ko spy karna — kuch methods mock karo, baaki real behavior rakho.

```java
@SpringBootTest
class UserServiceSpyTest {

    @SpyBean  // Real UserService ko spy karo
    private UserService userService;

    @MockBean
    private EmailService emailService;

    @Test
    void shouldUseRealServiceWithPartialMock() {
        // Stub only specific method
        doReturn(false).when(userService).validateEmail("invalid-email");

        // Real method call for everything else
        User user = userService.createUser("sachin", "sachin@email.com");

        // Real logic ran (except validateEmail for "invalid-email")
        assertNotNull(user);
        verify(emailService).sendWelcomeEmail(any(User.class));
    }
}
```

### SpyBean vs MockBean

```
@MockBean → Fake bean — sab methods stubbed hain
@SpyBean  → Real bean — kuch methods stubbed, baaki real
```

---

## TestEntityManager

**Matlab:** @DataJpaTest ke saath aata hai — JPA operations test karne ka helper class.

```java
@DataJpaTest
class UserRepositoryWithEntityManagerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;  // JPA helper

    @Test
    void shouldPersistUserWithEntityManager() {
        // Save through TestEntityManager
        User user = new User("sachin", "sachin@email.com");
        user = entityManager.persistAndFlush(user);

        assertNotNull(user.getId());

        // Find through repository
        User found = userRepository.findByUsername("sachin").orElseThrow();
        assertEquals("sachin@email.com", found.getEmail());
    }

    @Test
    void shouldFindPersistedUser() {
        // Persist directly
        User user = entityManager.persist(new User("sachin", "sachin@email.com"));
        entityManager.flush();

        // Repository se find karo
        Optional<User> found = userRepository.findById(user.getId());
        assertTrue(found.isPresent());
    }

    @Test
    void shouldClearEntityManager() {
        entityManager.persist(new User("temp", "temp@email.com"));
        entityManager.flush();
        entityManager.clear();  // Clear persistence context

        // Entity ab detached hai
        // repository se fresh fetch karna padega
    }
}
```

### TestEntityManager vs Direct Repository

```java
// Direct Repository (recommended)
User user = new User("sachin", "sachin@email.com");
userRepository.save(user);

// TestEntityManager (jab EntityManager control chahiye)
User user = new User("sachin", "sachin@email.com");
user = entityManager.persistAndFlush(user);

// Dono same kaam karte hain — repository approach cleaner hai
```

---

## Quick Reference

```java
// Test Slice Annotations
@WebMvcTest(UserController.class)       // Web layer only
@DataJpaTest                             // JPA layer only
@JsonTest                                // JSON serialization only
@RestClientTest(ApiClient.class)         // REST client only
@SpringBootTest                          // Full context
@SpringBootTest(webEnvironment = RANDOM_PORT)  // Full app with real port

// Bean Replacement
@MockBean   → Replace real bean with mock in Spring context
@SpyBean    → Spy on real bean (partial mock)

// TestEntityManager (in @DataJpaTest)
entityManager.persistAndFlush(entity);
entityManager.persist(entity);
entityManager.flush();
entityManager.clear();
```

---

## Summary

| Annotation | Loads | Auto-Mocks | Speed | Best For |
|------------|-------|------------|-------|----------|
| **@WebMvcTest** | Controllers, Validation | Services, Repositories | ⚡ Fast | Controller logic |
| **@DataJpaTest** | JPA, H2 | Controllers, Services | ⚡ Fast | Repository queries |
| **@JsonTest** | Jackson ObjectMapper | Everything | ⚡ Fast | JSON serialization |
| **@RestClientTest** | RestTemplate/WebClient | Everything | ⚡ Fast | HTTP client calls |
| **@SpringBootTest** | Everything | Nothing | 🐢 Slow | Integration tests |
| **@MockBean** | N/A | Replaces real bean | - | Mocking in Spring context |
| **@SpyBean** | N/A | Spies on real bean | - | Partial mocking in context |

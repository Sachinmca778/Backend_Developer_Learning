# JUnit 5 (Jupiter)

## Status: Not Started

---

## Table of Contents

1. [JUnit 5 Overview](#junit-5-overview)
2. [@Test Annotation](#test-annotation)
3. [Test Lifecycle: @BeforeEach, @AfterEach, @BeforeAll, @AfterAll](#test-lifecycle)
4. [@Disabled](#disabled)
5. [@DisplayName](#displayname)
6. [@Nested (Nested Test Classes)](#nested-nested-test-classes)
7. [@ParameterizedTest](#parameterizedtest)
8. [@ValueSource](#valuesource)
9. [@CsvSource](#csvsource)
10. [@MethodSource](#methodsource)
11. [@EnumSource](#enumsource)
12. [@RepeatedTest](#repeatedtest)
13. [Assumptions](#assumptions)

---

## JUnit 5 Overview

**Matlab:** Java ka testing framework. JUnit 5 = JUnit Jupiter — modern, modular, aur powerful.

### JUnit 5 Architecture

```
JUnit 5 = JUnit Platform + JUnit Jupiter + JUnit Vintage

JUnit Platform:
    → Test run karta hai (console, IDE, Maven/Gradle)
    → Foundation layer

JUnit Jupiter:
    → Modern JUnit 5 annotations (@Test, @BeforeEach, etc.)
    → Yeh use karna hai

JUnit Vintage:
    → JUnit 3/4 backward compatibility
    → Legacy code ke liye
```

### Maven Dependency

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.2</version>
    <scope>test</scope>
</dependency>

<!-- Spring Boot mein already included hai -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

### Project Structure

```
src/test/java/
└── com/example/app/
    ├── service/
    │   └── UserServiceTest.java
    ├── controller/
    │   └── UserControllerTest.java
    └── repository/
        └── UserRepositoryTest.java
```

---

## @Test Annotation

**Matlab:** Ek method ko test method mark karta hai. Har @Test method ek individual test case hai.

### Basic Test

```java
import org.junit.jupiter.api.Test;

class CalculatorTest {

    @Test
    void testAddition() {
        Calculator calc = new Calculator();
        int result = calc.add(2, 3);

        org.junit.jupiter.api.Assertions.assertEquals(5, result);
    }

    @Test
    void testSubtraction() {
        Calculator calc = new Calculator();
        int result = calc.subtract(10, 4);

        org.junit.jupiter.api.Assertions.assertEquals(6, result);
    }
}
```

### @Test Rules

| Rule | Description |
|------|-------------|
| Method public hona nahi chahiye | `void` return type, `public` nahi chahiye (package-private kaafi hai) |
| Static nahi hona chahiye | Instance method hona chahiye |
| Ek class mein multiple @Test | Har method independently run hoga |
| Naming convention | Descriptive names: `testAddition()`, `addition_of_two_numbers()` |

---

## Test Lifecycle

**Matlab:** Test execution ke different phases — setup, cleanup, one-time setup.

### Lifecycle Order

```
@BeforeAll          → Class level, ek baar (static method)
    ↓
@BeforeEach       → Har test se pehle
    ↓
@Test             → Pehla test
    ↓
@AfterEach        → Har test ke baad
    ↓
@BeforeEach       → Har test se pehle
    ↓
@Test             → Doosra test
    ↓
@AfterEach        → Har test ke baad
    ↓
... (repeat for each test)
    ↓
@AfterAll         → Class level, sab tests ke baad (static method)
```

### @BeforeEach — Har Test Se Pehle

```java
class UserServiceTest {

    private UserService userService;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Har test se pehle run hoga
        userRepository = new InMemoryUserRepository();
        userService = new UserService(userRepository);

        // Test data setup
        userRepository.save(new User("sachin", "sachin@email.com"));
        userRepository.save(new User("john", "john@email.com"));
    }

    @Test
    void testFindAllUsers() {
        List<User> users = userService.getAllUsers();
        assertEquals(2, users.size());  // setUp() mein 2 users add kiye
    }

    @Test
    void testFindByUsername() {
        User user = userService.findByUsername("sachin");
        assertNotNull(user);
        assertEquals("sachin@email.com", user.getEmail());
    }

    // Note: Dono tests fresh setup pe run hote hain
    // setUp() har test se pehle call hota hai
}
```

### @AfterEach — Har Test Ke Baad

```java
class FileProcessorTest {

    private Path tempFile;

    @BeforeEach
    void setUp() throws IOException {
        tempFile = Files.createTempFile("test", ".txt");
    }

    @AfterEach
    void tearDown() throws IOException {
        // Har test ke baad cleanup
        if (tempFile != null && Files.exists(tempFile)) {
            Files.delete(tempFile);
            System.out.println("Cleaned up: " + tempFile);
        }
    }

    @Test
    void testWriteToFile() throws IOException {
        Files.writeString(tempFile, "Hello World");
        String content = Files.readString(tempFile);
        assertEquals("Hello World", content);
    }

    @Test
    void testReadEmptyFile() throws IOException {
        String content = Files.readString(tempFile);
        assertEquals("", content);
    }
}
```

### @BeforeAll — Class Level, Ek Baar

```java
class DatabaseConnectionTest {

    private static DatabaseConnection dbConnection;

    @BeforeAll
    static void setUpClass() {
        // Sirf ek baar run hoga — expensive operations ke liye
        System.out.println("Starting database tests...");
        dbConnection = new DatabaseConnection("jdbc:h2:mem:testdb");
        dbConnection.connect();
    }

    @AfterAll
    static void tearDownClass() {
        // Sirf ek baar run hoga — cleanup ke liye
        System.out.println("All database tests completed.");
        if (dbConnection != null) {
            dbConnection.disconnect();
        }
    }

    @Test
    void testConnection() {
        assertTrue(dbConnection.isConnected());
    }

    @Test
    void testQuery() {
        List<String> tables = dbConnection.getTableNames();
        assertNotNull(tables);
    }
}
```

### @BeforeAll Rules

| Rule | Description |
|------|-------------|
| Method static hona chahiye | Kyunki class instance abhi bana nahi |
| @BeforeAll pe @AfterAll hona chahiye | Cleanup ke liye (recommended) |
| Expensive setup ke liye | DB connection, server start, etc. |

### Complete Lifecycle Example

```java
class CompleteLifecycleTest {

    @BeforeAll
    static void beforeAll() {
        System.out.println("@BeforeAll — Runs ONCE before all tests");
    }

    @AfterAll
    static void afterAll() {
        System.out.println("@AfterAll — Runs ONCE after all tests");
    }

    @BeforeEach
    void beforeEach() {
        System.out.println("@BeforeEach — Runs before EACH test");
    }

    @AfterEach
    void afterEach() {
        System.out.println("@AfterEach — Runs after EACH test");
    }

    @Test
    void test1() {
        System.out.println("Test 1 running");
    }

    @Test
    void test2() {
        System.out.println("Test 2 running");
    }
}

/* Output:
@BeforeAll — Runs ONCE before all tests
@BeforeEach — Runs before EACH test
Test 1 running
@AfterEach — Runs after EACH test
@BeforeEach — Runs before EACH test
Test 2 running
@AfterEach — Runs after EACH test
@AfterAll — Runs ONCE after all tests
*/
```

---

## @Disabled

**Matlab:** Test ko temporarily skip karna bina delete kiye. CI/CD mein useful jab test fail ho raha ho par fix baad mein karna ho.

```java
class PaymentServiceTest {

    @Disabled("Payment gateway integration pending")
    @Test
    void testPaymentWithRealGateway() {
        // Yeh test temporarily skip karo
        fail("Not implemented yet");
    }

    @Disabled("Bug #1234 — flaky test, needs investigation")
    @Test
    void testConcurrentPayments() {
        // Flaky test — debug karne tak disable
    }

    // Baaki tests normal run honge
    @Test
    void testPaymentValidation() {
        // Normal test
    }
}
```

### Disable on Class Level

```java
@Disabled("Entire test class temporarily disabled")
class LegacyPaymentTest {
    // Yeh class ka koi test nahi run hoga

    @Test
    void testOldPayment() { /* skipped */ }

    @Test
    void testNewPayment() { /* skipped */ }
}
```

**Warning:** `@Disabled` sirf temporary ke liye hai. Jira/GitHub issue link karo comment mein taaki pata rahe ki kyun disabled hai.

---

## @DisplayName

**Matlab:** Test ko readable name dena. Reports aur IDE mein achha dikhta hai.

```java
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("User Service Tests")
class UserServiceTest {

    @Test
    @DisplayName("Should create user with valid data")
    void createUser_validData() {
        // ...
    }

    @Test
    @DisplayName("Should throw exception when email is duplicate")
    void createUser_duplicateEmail() {
        // ...
    }

    @Test
    @DisplayName("Should return empty when user not found")
    void findByUsername_notFound() {
        // ...
    }
}
```

### Emoji Support

```java
@DisplayName("🔐 Authentication Tests")
class AuthTest {

    @Test
    @DisplayName("✅ Login should succeed with valid credentials")
    void loginValid() { }

    @Test
    @DisplayName("❌ Login should fail with invalid credentials")
    void loginInvalid() { }

    @Test
    @DisplayName("🔑 Should generate JWT token on successful login")
    void jwtTokenGeneration() { }
}
```

---

## @Nested (Nested Test Classes)

**Matlab:** Test classes ko group karna within a single file. Related tests ko organize karne ka tarika.

### Why Nested?

```
Without @Nested:
    UserServiceTest
        ├── testCreateUser
        ├── testCreateUser_duplicateEmail
        ├── testFindUser
        ├── testFindUser_notFound
        ├── testDeleteUser
        └── testDeleteUser_notFound
    (Sab ek hi level — confusing jab bahut tests hon)

With @Nested:
    UserServiceTest
        ├── CreateUser
        │   ├── shouldSucceedWithValidData
        │   └── shouldFailWithDuplicateEmail
        ├── FindUser
        │   ├── shouldReturnUser
        │   └── shouldReturnEmptyWhenNotFound
        └── DeleteUser
            ├── shouldDeleteExistingUser
            └── shouldThrowWhenNotFound
    (Organized, readable)
```

### Nested Test Example

```java
@DisplayName("User Service Tests")
class UserServiceTest {

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(new InMemoryUserRepository());
    }

    @Nested
    @DisplayName("Create User")
    class CreateUserTests {

        @Test
        @DisplayName("Should succeed with valid data")
        void shouldSucceedWithValidData() {
            User user = userService.createUser("sachin", "sachin@email.com");
            assertNotNull(user.getId());
        }

        @Test
        @DisplayName("Should fail with duplicate email")
        void shouldFailWithDuplicateEmail() {
            userService.createUser("sachin", "sachin@email.com");
            assertThrows(DuplicateEmailException.class,
                () -> userService.createUser("john", "sachin@email.com"));
        }

        @Test
        @DisplayName("Should fail with empty username")
        void shouldFailWithEmptyUsername() {
            assertThrows(ValidationException.class,
                () -> userService.createUser("", "test@email.com"));
        }
    }

    @Nested
    @DisplayName("Find User")
    class FindUserTests {

        @Test
        @DisplayName("Should return existing user")
        void shouldReturnExistingUser() {
            userService.createUser("sachin", "sachin@email.com");
            User user = userService.findByUsername("sachin");
            assertEquals("sachin@email.com", user.getEmail());
        }

        @Test
        @DisplayName("Should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            Optional<User> result = userService.findByUsername("nonexistent");
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Delete User")
    class DeleteUserTests {

        @BeforeEach
        void setUpForDelete() {
            // @Nested class ka apna @BeforeEach
            userService.createUser("sachin", "sachin@email.com");
        }

        @Test
        @DisplayName("Should delete existing user")
        void shouldDeleteExistingUser() {
            userService.deleteUser("sachin");
            assertTrue(userService.findByUsername("sachin").isEmpty());
        }

        @Test
        @DisplayName("Should throw when user not found")
        void shouldThrowWhenNotFound() {
            assertThrows(UserNotFoundException.class,
                () -> userService.deleteUser("nonexistent"));
        }
    }
}
```

### Nested Classes with Shared State

```java
class ShoppingCartTest {

    private ShoppingCart cart;

    @BeforeEach
    void setUp() {
        cart = new ShoppingCart();
    }

    @Nested
    class WhenEmpty {

        @Test
        void shouldReturnZeroItems() {
            assertEquals(0, cart.getItemCount());
        }

        @Test
        void shouldReturnZeroTotal() {
            assertEquals(BigDecimal.ZERO, cart.getTotal());
        }
    }

    @Nested
    class WhenItemAdded {

        @BeforeEach
        void addItems() {
            cart.addItem(new Item("Book", new BigDecimal("19.99")));
            cart.addItem(new Item("Pen", new BigDecimal("2.99")));
        }

        @Test
        void shouldHaveTwoItems() {
            assertEquals(2, cart.getItemCount());
        }

        @Test
        void shouldCalculateCorrectTotal() {
            assertEquals(new BigDecimal("22.98"), cart.getTotal());
        }

        @Nested
        class WhenItemRemoved {

            @BeforeEach
            void removeItem() {
                cart.removeItem("Pen");
            }

            @Test
            void shouldHaveOneItem() {
                assertEquals(1, cart.getItemCount());
            }

            @Test
            void shouldUpdateTotal() {
                assertEquals(new BigDecimal("19.99"), cart.getTotal());
            }
        }
    }
}
```

---

## @ParameterizedTest

**Matlab:** Ek test method ko multiple inputs pe run karna. Same logic, different data — code duplication avoid karta hai.

### Basic Parameterized Test

```java
@ParameterizedTest
@ValueSource(strings = {"sachin", "john", "jane"})
void usernamesShouldNotBeNull(String username) {
    assertNotNull(username);
    assertTrue(username.length() > 0);
}
// Yeh test 3 baar run hoga — har value ke liye ek baar
```

### Without @ParameterizedTest (Old Way)

```java
// ❌ Repetitive code
@Test
void testUsernames() {
    testUsername("sachin");
    testUsername("john");
    testUsername("jane");
}

private void testUsername(String username) {
    assertNotNull(username);
}

// ✅ @ParameterizedTest — cleaner
@ParameterizedTest
@ValueSource(strings = {"sachin", "john", "jane"})
void testUsername(String username) {
    assertNotNull(username);
}
```

---

## @ValueSource

**Matlab:** Simple values provide karna — strings, ints, longs, doubles, etc.

```java
@ParameterizedTest
@ValueSource(strings = {"hello", "world", "test"})
void stringsShouldNotBeEmpty(String value) {
    assertFalse(value.isEmpty());
}

@ParameterizedTest
@ValueSource(ints = {1, 2, 3, 4, 5})
void numbersShouldBePositive(int value) {
    assertTrue(value > 0);
}

@ParameterizedTest
@ValueSource(doubles = {1.5, 2.5, 3.5})
void doublesShouldBeGreaterThanOne(double value) {
    assertTrue(value > 1.0);
}

@ParameterizedTest
@ValueSource(booleans = {true, true, true})
void allShouldBeTrue(boolean value) {
    assertTrue(value);
}

// Supported types: strings, ints, longs, doubles, floats, shorts, bytes, booleans, chars, classes
```

---

## @CsvSource

**Matlab:** Comma-separated values — multiple columns ka data ek saath provide karna.

```java
@ParameterizedTest
@CsvSource({
    "sachin, sachin@email.com, true",
    "john, john@email.com, true",
    "admin, admin@email.com, true",
    ", , false"  // Empty values
})
void shouldCreateUserWithValidData(String username, String email, boolean expectedValid) {
    if (expectedValid) {
        User user = userService.createUser(username, email);
        assertNotNull(user);
    } else {
        assertThrows(ValidationException.class,
            () -> userService.createUser(username, email));
    }
}
```

### Custom Delimiter

```java
@ParameterizedTest
@CsvSource(value = {
    "sachin | sachin@email.com | ADMIN",
    "john   | john@email.com   | USER"
}, delimiter = '|')
void shouldParseCsvWithCustomDelimiter(String username, String email, String role) {
    assertNotNull(username);
    assertNotNull(email);
    assertNotNull(role);
}
```

### Null and Empty Values

```java
@ParameterizedTest
@CsvSource({
    "sachin, sachin@email.com",
    "john, null",           // null as string, not actual null
    "'', jane@email.com"    // empty string
})
void shouldHandleNullAndEmpty(String username, String email) {
    System.out.println("username: " + username + ", email: " + email);
}
```

### Real CSV with Column Headers

```java
@ParameterizedTest
@CsvFileSource(resources = "/test-users.csv", numLinesToSkip = 1)
void shouldImportUsersFromCsv(String username, String email, String role) {
    User user = userService.createUser(username, email, role);
    assertNotNull(user.getId());
}

// test-users.csv (src/test/resources/)
// username,email,role
// sachin,sachin@email.com,ADMIN
// john,john@email.com,USER
// jane,jane@email.com,USER
```

---

## @MethodSource

**Matlab:** Method se complex data provide karna. Arguments, objects, collections — kuch bhi return kar sakte ho.

### Single Argument

```java
@ParameterizedTest
@MethodSource("provideUsernames")
void shouldValidateUsernames(String username) {
    assertTrue(username.length() >= 3);
    assertTrue(username.length() <= 20);
}

static Stream<String> provideUsernames() {
    return Stream.of("sachin", "john", "jane", "admin", "testuser");
}
```

### Multiple Arguments (Arguments Object)

```java
@ParameterizedTest
@MethodSource("provideUsers")
void shouldCreateUser(String username, String email, String expectedRole) {
    User user = userService.createUser(username, email);
    assertNotNull(user);
    assertEquals(expectedRole, user.getRole());
}

static Stream<Arguments> provideUsers() {
    return Stream.of(
        Arguments.of("sachin", "sachin@email.com", "ADMIN"),
        Arguments.of("john", "john@email.com", "USER"),
        Arguments.of("jane", "jane@email.com", "USER")
    );
}
```

### Complex Objects

```java
@ParameterizedTest
@MethodSource("provideTestCases")
void shouldCalculateDiscount(TestCase testCase) {
    BigDecimal discounted = priceService.applyDiscount(
        testCase.originalPrice, testCase.discountPercent
    );
    assertEquals(testCase.expectedPrice, discounted);
}

static Stream<Arguments> provideTestCases() {
    return Stream.of(
        Arguments.of(new TestCase(new BigDecimal("100"), 10, new BigDecimal("90"))),
        Arguments.of(new TestCase(new BigDecimal("200"), 25, new BigDecimal("150"))),
        Arguments.of(new TestCase(new BigDecimal("50"), 0, new BigDecimal("50"))),
        Arguments.of(new TestCase(new BigDecimal("1000"), 50, new BigDecimal("500")))
    );
}

record TestCase(BigDecimal originalPrice, int discountPercent, BigDecimal expectedPrice) {}
```

### External Method Reference

```java
@ParameterizedTest
@MethodSource("com.example.testdata.UserTestData#validUsers")
void shouldProcessValidUsers(User user) {
    assertNotNull(user);
    assertNotNull(user.getEmail());
}

// Separate test data provider class
class UserTestData {
    static Stream<User> validUsers() {
        return Stream.of(
            new User("sachin", "sachin@email.com"),
            new User("john", "john@email.com")
        );
    }
}
```

---

## @EnumSource

**Matlab:** Enum ke sab values ya selected values provide karna.

```java
public enum UserRole {
    ADMIN, MANAGER, USER, GUEST
}
```

### All Enum Values

```java
@ParameterizedTest
@EnumSource(UserRole.class)
void shouldHaveRoleForEveryUserType(UserRole role) {
    assertNotNull(role);
    assertTrue(Set.of(UserRole.values()).contains(role));
}
// Runs 4 times — ADMIN, MANAGER, USER, GUEST
```

### Include Specific Values

```java
@ParameterizedTest
@EnumSource(value = UserRole.class, names = {"ADMIN", "MANAGER"})
void shouldGrantAccessToPrivilegedRoles(UserRole role) {
    assertTrue(role == UserRole.ADMIN || role == UserRole.MANAGER);
}
```

### Exclude Specific Values

```java
@ParameterizedTest
@EnumSource(value = UserRole.class, names = {"GUEST"}, mode = Mode.EXCLUDE)
void shouldNotGrantAccessToRegularRoles(UserRole role) {
    assertTrue(role != UserRole.GUEST);
}
```

### Regex Matching

```java
@ParameterizedTest
@EnumSource(value = UserRole.class, names = ".*ER$", mode = Mode.MATCH_ALL)
void shouldMatchRolesEndingWithER(UserRole role) {
    assertTrue(role.name().endsWith("ER"));  // MANAGER, USER
}
```

---

## @RepeatedTest

**Matlab:** Ek test ko multiple baar run karna. Flaky tests debug karne ke liye ya stress testing ke liye.

```java
@RepeatedTest(5)
void testShouldPassFiveTimes() {
    // Yeh test 5 baar run hoga
    assertTrue(true);
}

// Custom display name
@RepeatedTest(value = 3, name = "{displayName} — run {currentRepetition}/{totalRepetitions}")
@DisplayName("Flaky test investigation")
void investigateFlakyTest() {
    // Custom message har run ke saath
}

// With RepetitionInfo
@RepeatedTest(5)
void testWithRepetitionInfo(RepetitionInfo info) {
    int currentRun = info.getCurrentRepetition();
    int totalRuns = info.getTotalRepetitions();

    System.out.println("Running test " + currentRun + " of " + totalRuns);

    // Different behavior based on repetition
    if (currentRun == 1) {
        // First run — setup
    } else {
        // Subsequent runs
    }
}
```

### Finding Flaky Tests

```java
// Jab test kabhi pass ho aur kabhi fail — 10 baar run karo
@RepeatedTest(10)
void testConcurrentUserAccess() {
    // Agar 10 mein se 1 baar bhi fail hua → flaky hai
    List<User> users = userService.getAllUsers();
    assertEquals(5, users.size());
}
```

---

## Assumptions

**Matlab:** Test tabhi run ho jab certain condition true ho. False assumption pe test **skip** hota hai (fail nahi).

### assumeTrue

```java
@Test
void testOnlyOnLinux() {
    assumeTrue(System.getProperty("os.name").contains("Linux"));
    // Yeh test sirf Linux pe run hoga
    // Windows pe: SKIPPED (not failed)
    assertEquals("/home", System.getProperty("user.home").substring(0, 5));
}

@Test
void testOnlyInCI() {
    assumeTrue("true".equals(System.getenv("CI")));
    // Sirf CI environment mein run hoga
    performIntegrationTest();
}

@Test
void testOnlyWhenDatabaseAvailable() {
    assumeTrue(isDatabaseAvailable());
    // DB available nahi hai toh skip
    testDatabaseOperations();
}
```

### assumingThat

```java
@Test
void testWithPartialAssumption() {
    assumingThat(
        "true".equals(System.getenv("RUN_EXPENSIVE_TESTS")),
        () -> {
            // Yeh block sirf tab run hoga jab assumption true ho
            // Assumption false hai toh block skip, rest of test runs
            performExpensiveTest();
        }
    );

    // Yeh hamesha run hoga (assumption ke baahar)
    assertTrue(true);
}
```

### Assumptions vs Assertions

```java
// Assertion fail hone pe → TEST FAILS
assertEquals(5, actual);  // 5 nahi hai → Test Failed ❌

// Assumption fail hone pe → TEST SKIPPED
assumeTrue(condition);     // Condition false hai → Test Skipped ⏭️

// Use assertion jab test MUST pass for this condition
// Use assumption jab test SHOULD ONLY run for this condition
```

---

## Quick Reference

```java
// Basic test
@Test
void testMethod() {
    assertEquals(expected, actual);
}

// Lifecycle
@BeforeAll  static void setUpClass() { }     // Once, static
@AfterAll   static void tearDownClass() { }  // Once, static
@BeforeEach void setUp() { }                 // Before each test
@AfterEach  void tearDown() { }              // After each test

// Disable/skip
@Disabled("reason")

// Display name
@DisplayName("Readable test name")

// Nested tests
@Nested
class SubTests { }

// Parameterized tests
@ParameterizedTest
@ValueSource(ints = {1, 2, 3})
@CsvSource({"a,1", "b,2"})
@MethodSource("provideData")
@EnumSource(MyEnum.class)

// Repeated test
@RepeatedTest(5)

// Assumptions
assumeTrue(condition);       // Skip test if false
assumingThat(cond, () -> {}); // Skip block if false
```

---

## Summary

| Annotation | Purpose |
|------------|---------|
| **@Test** | Mark method as test case |
| **@BeforeEach/@AfterEach** | Setup/cleanup before/after each test |
| **@BeforeAll/@AfterAll** | One-time setup/cleanup (static methods) |
| **@Disabled** | Temporarily skip a test |
| **@DisplayName** | Human-readable test name |
| **@Nested** | Group related tests in nested classes |
| **@ParameterizedTest** | Run test with multiple inputs |
| **@ValueSource** | Simple values (int, string, boolean) |
| **@CsvSource** | Comma-separated multi-column data |
| **@MethodSource** | Complex data from method (Streams, Arguments) |
| **@EnumSource** | Enum values (all, include, exclude, regex) |
| **@RepeatedTest** | Run test N times (flaky test debugging) |
| **assumeTrue** | Skip test if condition false (not fail) |

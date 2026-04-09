# Mockito

## Status: Not Started

---

## Table of Contents

1. [Mockito Overview](#mockito-overview)
2. [@ExtendWith(MockitoExtension.class)](#extendwithmockitoextensionclass)
3. [@Mock](#mock)
4. [@InjectMocks](#injectmocks)
5. [@Spy (Partial Mock)](#spy-partial-mock)
6. [@Captor](#captor)
7. [when().thenReturn()](#whenthenreturn)
8. [when().thenThrow()](#whenthenThrow)
9. [doNothing().when()](#donothingwhen)
10. [verify()](#verify)
11. [times(), never()](#times-never)
12. [ArgumentCaptor](#argumentcaptor)
13. [ArgumentMatchers](#argumentmatchers)

---

## Mockito Overview

**Matlab:** Mocking framework jo real objects ki jagah fake (mock) objects create karta hai. Dependencies ko isolate karke sirf unit test karna hai.

### Why Mocking?

```
Real Scenario:
    UserServiceTest → UserRepository → Database
    → Database setup karna padta hai
    → Test slow ho jata hai
    → External dependency pe depend karta hai

Mocked Scenario:
    UserServiceTest → Mock UserRepository
    → No database needed
    → Fast tests
    → Isolated unit testing
```

### Maven Dependency

```xml
<!-- Included in spring-boot-starter-test by default -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>

<!-- JUnit 5 extension -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

### Static Imports

```java
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
```

---

## @ExtendWith(MockitoExtension.class)

**Matlab:** JUnit 5 extension jo @Mock, @InjectMocks, @Spy annotations ko automatically initialize karta hai.

### Without Extension (Manual)

```java
class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        // Manual mock creation
        userRepository = Mockito.mock(UserRepository.class);
        userService = new UserService(userRepository);
    }
}
```

### With Extension (Recommended)

```java
@ExtendWith(MockitoExtension.class)  // Auto-initialize @Mock, @InjectMocks
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    // @BeforeEach mein manually mock create karne ki zarurat nahi
    // Extension automatically kar deta hai

    @Test
    void testFindUser() {
        User mockUser = new User("sachin", "sachin@email.com");
        when(userRepository.findByUsername("sachin")).thenReturn(Optional.of(mockUser));

        User user = userService.findByUsername("sachin");
        assertNotNull(user);
        assertEquals("sachin", user.getUsername());
    }
}
```

### Spring Boot Alternative

```java
// Spring Boot mein @MockBean use hota hai (not @Mock)
@WebMvcTest(UserController.class)
class UserControllerTest {

    @MockBean  // Spring context mein mock register hota hai
    private UserService userService;

    @Autowired
    private MockMvc mockMvc;
}
```

---

## @Mock

**Matlab:** Interface ya class ka mock (fake) object banana. Mock object ka koi real behavior nahi hota — aap define karte ho ki kya return karega.

### Basic Mock

```java
@Mock
private UserRepository userRepository;

@Test
void testMockBehavior() {
    // Mock methods default null return karte hain
    assertNull(userRepository.findByUsername("test"));

    // Stub karo — specific behavior define karo
    User mockUser = new User("sachin", "sachin@email.com");
    when(userRepository.findByUsername("sachin")).thenReturn(Optional.of(mockUser));

    // Ab stubbed value return hogi
    Optional<User> result = userRepository.findByUsername("sachin");
    assertTrue(result.isPresent());
    assertEquals("sachin", result.get().getUsername());

    // Non-stubbed call → null
    assertNull(userRepository.findByUsername("john"));
}
```

### Mock Return Values by Default

```java
@Mock
private SomeService someService;

@Test
void testDefaultMockReturns() {
    // Objects → null
    assertNull(someService.getUser());

    // Primitives → default values
    assertEquals(0, someService.getCount());          // int → 0
    assertEquals(false, someService.isActive());      // boolean → false

    // Collections → empty
    assertTrue(someService.getUsers().isEmpty());     // List → empty
    assertTrue(someService.getRoles().isEmpty());     // Set → empty

    // Strings → null
    assertNull(someService.getName());
}
```

---

## @InjectMocks

**Matlab:** Mock objects ko real object mein inject karna. Constructor, setter, ya field injection ke through.

### Constructor Injection (Recommended)

```java
// Real class
public class UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;

    // Constructor injection
    public UserService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public User createUser(String username, String email) {
        User user = new User(username, email);
        userRepository.save(user);
        emailService.sendWelcomeEmail(user);
        return user;
    }
}
```

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks  // Constructor se mocks inject honge
    private UserService userService;

    @Test
    void testCreateUser() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        doNothing().when(emailService).sendWelcomeEmail(any(User.class));

        User user = userService.createUser("sachin", "sachin@email.com");

        assertNotNull(user.getId());
        verify(userRepository).save(any(User.class));
        verify(emailService).sendWelcomeEmail(any(User.class));
    }
}
```

### Field Injection

```java
public class OrderService {

    @Autowired  // Field injection (not recommended in prod, but Mockito handles it)
    private OrderRepository orderRepository;
}

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks  // Field injection through reflection
    private OrderService orderService;
}
```

### Injection Priority

```
Mockito inject karta hai is order mein:
1. Constructor injection (sabse preferred)
2. Setter injection
3. Field injection (reflection se)

Agar multiple constructors hon toh @InjectMocks fail ho sakta hai
→ Sirf ek constructor hona chahiye (ya default + ek parameterized)
```

---

## @Spy (Partial Mock)

**Matlab:** Real object ko wrap karta hai — kuch methods real behavior rakhte hain, kuch methods stub kar sakte ho.

### Mock vs Spy

```
Mock:
    → Sab methods fake hain (default return values)
    → Aapko sab stub karna padta hai
    → Real object ka koi use nahi

Spy:
    → Real object use hota hai
    → Default behavior real hai
    → Specific methods ko stub kar sakte ho
    → Baaki methods real behavior follow karte hain
```

### Spy Example

```java
@Spy
private ArrayList<String> list = new ArrayList<>();

@Test
void testSpyBehavior() {
    // Real method call — real behavior
    list.add("sachin");
    list.add("john");
    assertEquals(2, list.size());  // Real method

    // Stub a specific method
    when(list.size()).thenReturn(100);
    assertEquals(100, list.size());  // Stubbed value

    // Unstubbed methods still real
    assertEquals("sachin", list.get(0));  // Real method
}
```

### Spy with Service Class

```java
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Spy
    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private PaymentGateway paymentGateway;

    @Test
    void testPartialMock() {
        // Stub external dependency
        when(paymentGateway.charge(any())).thenReturn(true);

        // Stub internal method
        doReturn(false).when(paymentService).validateAmount(new BigDecimal("1000000"));

        // Real method calls for everything else
        boolean result = paymentService.processPayment(new PaymentRequest());

        assertFalse(result);  // validateAmount returned false
        verify(paymentGateway, never()).charge(any());  // Charge was not called
    }
}
```

### Important: Spy Stubbing Syntax

```java
// ❌ WRONG — Real method gets called first
@Spy
private List<String> list = new ArrayList<>();

when(list.get(0)).thenReturn("mocked");  // list.get(0) called → IndexOutOfBoundsException!

// ✅ CORRECT — doReturn syntax
doReturn("mocked").when(list).get(0);  // Real method NOT called
```

---

## @Captor

**Matlab:** Method arguments capture karna. Verify karna ki mock ko kya arguments pass kiye gaye.

### Basic Argument Captor

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    void testCreateUser() {
        userService.createUser("sachin", "sachin@email.com");

        // Capture the User object passed to save()
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("sachin", savedUser.getUsername());
        assertEquals("sachin@email.com", savedUser.getEmail());
        assertNotNull(savedUser.getCreatedAt());
    }
}
```

### Multiple Captures

```java
@Captor
private ArgumentCaptor<String> emailCaptor;

@Captor
private ArgumentCaptor<String> subjectCaptor;

@Test
void testMultipleEmails() {
    emailService.sendEmail("user1@email.com", "Welcome!");
    emailService.sendEmail("user2@email.com", "Hello!");

    verify(emailService, times(2)).sendEmail(emailCaptor.capture(), subjectCaptor.capture());

    List<String> emails = emailCaptor.getAllValues();
    List<String> subjects = subjectCaptor.getAllValues();

    assertThat(emails).containsExactly("user1@email.com", "user2@email.com");
    assertThat(subjects).containsExactly("Welcome!", "Hello!");
}
```

### Manual ArgumentCaptor (No Annotation)

```java
@Test
void testManualCaptor() {
    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

    userService.createUser("sachin", "sachin@email.com");

    verify(userRepository).save(captor.capture());

    User captured = captor.getValue();
    assertEquals("sachin", captured.getUsername());
}
```

---

## when().thenReturn()

**Matlab:** Mock method ko specific value return karne ke liye configure karna.

### Basic Stubbing

```java
@Mock
private UserRepository userRepository;

@Test
void testBasicStubbing() {
    User mockUser = new User("sachin", "sachin@email.com");

    // Jab "sachin" username mile toh mockUser return karo
    when(userRepository.findByUsername("sachin")).thenReturn(Optional.of(mockUser));

    // Test
    Optional<User> result = userRepository.findByUsername("sachin");
    assertTrue(result.isPresent());
    assertEquals("sachin", result.get().getUsername());
}
```

### Multiple Return Values

```java
@Test
void testMultipleReturns() {
    when(userRepository.findByUsername("sachin"))
        .thenReturn(Optional.of(new User("sachin", "sachin@email.com")))
        .thenReturn(Optional.empty())  // Second call → empty
        .thenReturn(Optional.of(new User("sachin", "updated@email.com")));  // Third call

    // First call
    assertTrue(userRepository.findByUsername("sachin").isPresent());

    // Second call
    assertTrue(userRepository.findByUsername("sachin").isEmpty());

    // Third call
    assertTrue(userRepository.findByUsername("sachin").isPresent());
}
```

### Dynamic Return Value

```java
@Test
void testDynamicReturn() {
    when(userRepository.findByUsername(anyString())).thenAnswer(invocation -> {
        String username = invocation.getArgument(0);
        return Optional.of(new User(username, username + "@email.com"));
    });

    Optional<User> result1 = userRepository.findByUsername("sachin");
    assertEquals("sachin@email.com", result1.get().getEmail());

    Optional<User> result2 = userRepository.findByUsername("john");
    assertEquals("john@email.com", result2.get().getEmail());
}
```

### Throwing Exceptions

```java
@Test
void testThrowing() {
    when(userRepository.findByUsername("error"))
        .thenThrow(new RuntimeException("Database error"));

    assertThrows(RuntimeException.class,
        () -> userRepository.findByUsername("error"));
}
```

---

## when().thenThrow()

**Matlab:** Mock method se exception throw karwana — error handling test karne ke liye.

```java
@Mock
private UserRepository userRepository;

@Test
void testExceptionThrowing() {
    // Single exception
    when(userRepository.save(any(User.class)))
        .thenThrow(new DataAccessException("Database is down"));

    assertThrows(DataAccessException.class,
        () -> userRepository.save(new User()));

    // Multiple exceptions (sequence)
    when(userRepository.findByUsername("test"))
        .thenThrow(new RuntimeException("First call fails"))
        .thenThrow(new RuntimeException("Second call also fails"))
        .thenReturn(Optional.of(new User()));  // Third call succeeds

    assertThrows(RuntimeException.class,
        () -> userRepository.findByUsername("test"));  // First
    assertThrows(RuntimeException.class,
        () -> userRepository.findByUsername("test"));  // Second
    Optional<User> result = userRepository.findByUsername("test");  // Third → works
}
```

### DoThrow for Void Methods

```java
@Mock
private EmailService emailService;

@Test
void testVoidMethodException() {
    // ❌ Can't use when() for void methods
    // when(emailService.send(any())).thenThrow(...);  // Compile error

    // ✅ Use doThrow()
    doThrow(new EmailException("SMTP server down"))
        .when(emailService).sendEmail(anyString(), anyString());

    assertThrows(EmailException.class,
        () -> emailService.sendEmail("sachin@email.com", "Hello"));
}
```

---

## doNothing().when()

**Matlab:** Void methods ko explicitly "nothing do" bolna. By default void methods nothing return karti hain, par sometimes explicit declaration zaruri hai.

```java
@Mock
private EmailService emailService;
@Mock
private AuditService auditService;

@Test
void testDoNothing() {
    // EmailService.sendEmail() — void method
    // By default, void mocks do nothing already
    emailService.sendEmail("test@email.com", "Hello");  // No error

    // But explicit declaration better for readability
    doNothing().when(emailService).sendEmail(anyString(), anyString());

    // AuditService — do nothing AND verify it was called
    doNothing().when(auditService).logLogin(anyString());
}
```

### doAnswer for Void Methods

```java
@Mock
private UserRepository userRepository;

@Test
void testDoAnswer() {
    // Save method: user.setId() side effect test karna hai
    doAnswer(invocation -> {
        User user = invocation.getArgument(0);
        user.setId(42L);  // Side effect
        return null;  // void method returns null
    }).when(userRepository).save(any(User.class));

    User user = new User("sachin", "sachin@email.com");
    userRepository.save(user);

    assertEquals(42L, user.getId());  // Side effect worked!
}
```

---

## verify()

**Matlab:** Check karna ki mock method call hui ya nahi, aur kitni baar hui.

### Basic Verification

```java
@Mock
private UserRepository userRepository;

@InjectMocks
private UserService userService;

@Test
void testVerification() {
    userService.createUser("sachin", "sachin@email.com");

    // Verify save() was called
    verify(userRepository).save(any(User.class));

    // Verify findByUsername() was called
    verify(userRepository).findByUsername("sachin");

    // Verify delete() was NOT called
    verify(userRepository, never()).deleteById(anyLong());
}
```

### Verify with Times

```java
@Test
void testVerifyWithTimes() {
    userService.bulkCreateUsers(List.of("sachin", "john", "jane"));

    // Exactly 3 times
    verify(userRepository, times(3)).save(any(User.class));

    // At least 3 times
    verify(userRepository, atLeast(3)).save(any(User.class));

    // At most 5 times
    verify(userRepository, atMost(5)).save(any(User.class));
}
```

### Verify No Interactions

```java
@Test
void testVerifyNoInteractions() {
    // Verify specific method was NOT called
    verify(emailService, never()).sendEmail(anyString(), anyString());

    // Verify NO methods on mock were called
    verifyNoInteractions(emailService);

    // Verify NO MORE interactions after specific verifications
    verify(userRepository).save(any(User.class));
    verifyNoMoreInteractions(userRepository);
    // Agar koi aur method call hua toh test fail hoga
}
```

### Verify Order of Calls

```java
@Test
void testVerifyOrder() {
    InOrder inOrder = Mockito.inOrder(userRepository, emailService);

    userService.createUser("sachin", "sachin@email.com");

    inOrder.verify(userRepository).save(any(User.class));  // First
    inOrder.verify(emailService).sendWelcomeEmail(any());   // Second

    // Agar order galat hai toh test fail
}
```

---

## times(), never()

**Matlab:** Method call count verification.

```java
@Test
void testTimesAndNever() {
    userService.processBulkUsers(List.of("a", "b", "c"));

    // Exact count
    verify(userRepository, times(3)).save(any(User.class));

    // At least
    verify(userRepository, atLeast(2)).save(any(User.class));

    // At most
    verify(userRepository, atMost(5)).save(any(User.class));

    // Never called
    verify(emailService, never()).sendEmail(anyString(), anyString());

    // Only once (default)
    verify(userRepository).save(any(User.class));  // Same as times(1)

    // AtLeastOnce
    verify(userRepository, atLeastOnce()).save(any(User.class));
}
```

---

## ArgumentCaptor

**Matlab:** Already covered in @Captor section — here's a complete standalone example.

```java
@Test
void testArgumentCaptorStandalone() {
    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

    userService.createUser("sachin", "sachin@email.com");

    verify(userRepository).save(captor.capture());

    User captured = captor.getValue();
    assertEquals("sachin", captured.getUsername());
    assertEquals("sachin@email.com", captured.getEmail());
    assertNotNull(captured.getCreatedAt());
}
```

---

## ArgumentMatchers

**Matlab:** Method arguments ko match karne ke patterns. Exact value match ki jagah flexible matching.

### Basic Matchers

```java
@Mock
private UserRepository userRepository;

@Test
void testMatchers() {
    // any() — koi bhi value
    when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(mockUser));
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    // eq() — exact value match
    when(userRepository.findByUsername(eq("sachin"))).thenReturn(Optional.of(mockUser));

    // Combine eq and any
    when(userRepository.findByEmailAndStatus(
            anyString(),           // Any email
            eq(UserStatus.ACTIVE)  // Specific status
        )).thenReturn(Optional.of(mockUser));
}
```

### Available Matchers

```java
// Type matchers
any()               // Koi bhi object
any(Class.class)    // Koi bhi specific type
anyString()         // Koi bhi String
anyInt()            // Koi bhi int
anyLong()           // Koi bhi long
anyBoolean()        // Koi bhi boolean
anyDouble()         // Koi bhi double
anyList()           // Koi bhi List
anyMap()            // Koi bhi Map
anySet()            // Koi bhi Set

// String matchers
anyString()         // Koi bhi String (including null)
nullable(Class.class)  // Nullable value

// Exact match
eq(value)           // Exact value match

// Null/not null
isNull()            // Null value
isNotNull()         // Not null value
same(instance)      // Same instance (reference match)

// Collection matchers
anyList()
anyCollection()
anyCollectionOf(Class.class)

// Number matchers
anyInt()
anyLong()
anyDouble()
anyFloat()
anyShort()
anyByte()

// RefEq — object fields match
refEq(object)       // Reflection-based equality
```

### Custom Matchers

```java
// Custom argument matcher
@Test
void testCustomMatcher() {
    when(userRepository.save(argThat(user ->
        user.getUsername() != null && user.getUsername().length() >= 3
    ))).thenReturn(savedUser);

    // User with valid username gets saved
    User validUser = new User("sachin", "test@email.com");
    userRepository.save(validUser);  // Matched → returns savedUser

    // User with short username doesn't match
    User shortUser = new User("ab", "test@email.com");
    assertNull(userRepository.save(shortUser));  // No stub → null
}
```

### Important Rule: All or Nothing

```java
// ❌ WRONG — Mix of matchers and values
when(userRepository.findByEmailAndStatus(
    anyString(),     // Matcher
    UserStatus.ACTIVE  // Raw value ← ERROR!
)).thenReturn(Optional.of(mockUser));

// ✅ CORRECT — All matchers or all raw values
when(userRepository.findByEmailAndStatus(
    anyString(),
    eq(UserStatus.ACTIVE)  // Use eq() wrapper
)).thenReturn(Optional.of(mockUser));

// ✅ CORRECT — All raw values
when(userRepository.findByEmailAndStatus(
    "sachin@email.com",
    UserStatus.ACTIVE
)).thenReturn(Optional.of(mockUser));
```

---

## Quick Reference

```java
// Setup
@ExtendWith(MockitoExtension.class)
class MyTest {
    @Mock       → Fake object
    @InjectMocks → Real object with injected mocks
    @Spy        → Partial mock (real + stub)
    @Captor     → ArgumentCaptor
}

// Stubbing
when(mock.method(arg)).thenReturn(value);
when(mock.method(arg)).thenThrow(new Exception());
when(mock.method(arg)).thenAnswer(invocation -> { ... });

// Void methods
doNothing().when(mock).voidMethod(any());
doThrow(new Exception()).when(mock).voidMethod(any());
doAnswer(invocation -> { ... }).when(mock).voidMethod(any());

// Verification
verify(mock).method(any());          // Called once
verify(mock, times(3)).method(any()); // Called 3 times
verify(mock, never()).method(any());  // Never called
verify(mock, atLeast(2)).method(any());

// Argument capture
ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
verify(mock).save(captor.capture());
User captured = captor.getValue();

// Matchers
any(), anyString(), anyInt(), any(Class.class)
eq(value), isNull(), isNotNull()
argThat(predicate)
```

---

## Summary

| Annotation/Method | Purpose |
|-------------------|---------|
| **@ExtendWith(MockitoExtension.class)** | Auto-initialize @Mock, @InjectMocks, @Spy |
| **@Mock** | Create fake object (all methods are stubs) |
| **@InjectMocks** | Create real object with mocks injected |
| **@Spy** | Partial mock — real object with selective stubs |
| **@Captor** | Capture method arguments for verification |
| **when().thenReturn()** | Stub method to return value |
| **when().thenThrow()** | Stub method to throw exception |
| **doNothing().when()** | Void method ko explicitly no-op bolna |
| **verify()** | Check if method was called |
| **times(), never()** | Method call count verification |
| **ArgumentCaptor** | Capture arguments passed to mock |
| **ArgumentMatchers** | Flexible argument matching (any(), eq(), etc.) |

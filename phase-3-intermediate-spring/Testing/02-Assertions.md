# Assertions

## Status: Not Started

---

## Table of Contents

1. [Assertions Overview](#assertions-overview)
2. [JUnit Assertions](#junit-assertions)
3. [assertEquals](#assertequals)
4. [assertNotNull, assertNull](#assertnotnull-assertnull)
5. [assertTrue, assertFalse](#asserttrue-assertfalse)
6. [assertThrows](#assertthrows)
7. [assertAll](#assertall)
8. [assertTimeout, assertTimeoutPreemptively](#asserttimeout)
9. [assertDoesNotThrow](#assertdoesnotthrow)
10. [AssertJ Fluent Assertions](#assertj-fluent-assertions)
11. [Hamcrest Matchers](#hamcrest-matchers)

---

## Assertions Overview

**Matlab:** Test mein verify karna ki actual result expected se match karta hai ya nahi. Assertion fail hone pe test fail hota hai.

### JUnit vs AssertJ vs Hamcrest

```
JUnit Assertions (built-in):
    assertEquals(expected, actual)
    → Simple, no dependency needed
    → Chhoti apps ke liye kaafi

AssertJ (recommended):
    assertThat(actual).isEqualTo(expected)
    → Fluent, readable, IDE-friendly
    → Best for production code

Hamcrest:
    assertThat(actual, equalTo(expected))
    → Matcher-based, readable
    → Legacy projects mein common
```

### Maven Dependencies

```xml
<!-- JUnit 5 (already included in spring-boot-starter-test) -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<!-- AssertJ (recommended) -->
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>

<!-- Hamcrest (optional, included in spring-boot-starter-test) -->
<dependency>
    <groupId>org.hamcrest</groupId>
    <artifactId>hamcrest</artifactId>
    <scope>test</scope>
</dependency>
```

---

## JUnit Assertions

**Matlab:** `org.junit.jupiter.api.Assertions` class ke static methods.

### Basic Usage Pattern

```java
import static org.junit.jupiter.api.Assertions.*;

class MyTest {
    @Test
    void testMethod() {
        // assertion
        assertEquals(expected, actual);
    }
}
```

---

## assertEquals

**Matlab:** Expected aur actual values ko compare karna. Equal nahi hain toh test fail.

### Basic Comparison

```java
@Test
void testEquals() {
    assertEquals(5, 2 + 3);                    // int comparison
    assertEquals("hello", "hel" + "lo");        // String comparison
    assertEquals(3.14, Math.PI, 0.01);          // double with delta
    assertEquals(List.of(1, 2), Arrays.asList(1, 2));  // List comparison
}
```

### With Custom Message

```java
@Test
void testWithMessage() {
    int result = calculator.add(2, 3);
    assertEquals(5, result, "2 + 3 should equal 5");

    String name = user.getName();
    assertEquals("Sachin", name, () -> "Expected name to be Sachin but got: " + name);
}
```

### Lazy Message Evaluation

```java
// ✅ Better — message sirf tab evaluate hoga jab assertion fail ho
assertEquals(expected, actual, () -> "Expensive computation: " + buildErrorMessage(actual));

// ❌ Worse — message hamesha evaluate hota hai (even if test passes)
assertEquals(expected, actual, "Expensive computation: " + buildErrorMessage(actual));
```

### Double Comparison with Delta

```java
@Test
void testDoubleComparison() {
    // ❌ This may fail due to floating point precision
    assertEquals(0.3, 0.1 + 0.2);  // May fail!

    // ✅ Use delta (tolerance)
    assertEquals(0.3, 0.1 + 0.2, 0.001);  // Passes — within 0.001
}
```

---

## assertNotNull, assertNull

**Matlab:** Check karna ki value null hai ya nahi.

```java
@Test
void testNullChecks() {
    User user = userService.findByUsername("sachin");

    // Not null check
    assertNotNull(user);
    assertNotNull(user, "User should not be null");

    // Null check
    User missingUser = userService.findByUsername("nonexistent");
    assertNull(missingUser);
    assertNull(missingUser, "Non-existent user should be null");

    // AssertJ alternative
    assertThat(user).isNotNull();
    assertThat(missingUser).isNull();
}
```

### assertNotNull with Further Assertions

```java
@Test
void testFindUser() {
    User user = userService.findByUsername("sachin");

    // ✅ assertNotNull returns the value — chain kar sakte ho
    assertNotNull(user);
    assertEquals("sachin@email.com", user.getEmail());
    assertEquals("ADMIN", user.getRole());

    // AssertJ — better chaining
    assertThat(user)
        .isNotNull()
        .extracting(User::getEmail, User::getRole)
        .containsExactly("sachin@email.com", "ADMIN");
}
```

---

## assertTrue, assertFalse

**Matlab:** Boolean condition check karna.

```java
@Test
void testBooleanAssertions() {
    assertTrue(5 > 3);
    assertTrue(list.isEmpty(), "List should be empty");

    assertFalse(5 < 3);
    assertFalse(user.isDeleted(), "User should not be deleted");

    // With lambda message
    int size = list.size();
    assertTrue(size == 0, () -> "Expected empty list but had " + size + " elements");

    // AssertJ alternatives
    assertThat(list).isEmpty();
    assertThat(user.isDeleted()).isFalse();
    assertThat(condition).isTrue();
}
```

---

## assertThrows

**Matlab:** Verify karna ki code expected exception throw karta hai.

### Basic Usage

```java
@Test
void testException() {
    // Exception aana chahiye
    assertThrows(UserNotFoundException.class,
        () -> userService.findByUsername("nonexistent"));
}
```

### Capturing Exception

```java
@Test
void testExceptionDetails() {
    // Exception object capture karo
    UserNotFoundException exception = assertThrows(UserNotFoundException.class,
        () -> userService.findByUsername("nonexistent"));

    // Exception ki details verify karo
    assertEquals("User not found: nonexistent", exception.getMessage());
}
```

### Multiple Exception Scenarios

```java
@Test
void testValidationExceptions() {
    // Empty username
    assertThrows(ValidationException.class,
        () -> userService.createUser("", "test@email.com"));

    // Invalid email
    assertThrows(ValidationException.class,
        () -> userService.createUser("sachin", "invalid-email"));

    // Duplicate email
    userService.createUser("sachin", "sachin@email.com");
    assertThrows(DuplicateEmailException.class,
        () -> userService.createUser("john", "sachin@email.com"));
}
```

### AssertJ Alternative

```java
@Test
void testExceptionWithAssertJ() {
    assertThatThrownBy(() -> userService.findByUsername("nonexistent"))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining("nonexistent")
        .hasMessageStartingWith("User not found");

    // No exception expected
    assertThatNoException()
        .isThrownBy(() -> userService.findByUsername("sachin"));
}
```

---

## assertAll

**Matlab:** Multiple assertions ek saath run karna. Ek fail hone pe baaki bhi run honge — sab failures ek saath dikhenge.

### Without assertAll (Fail Fast)

```java
@Test
void testUserWithoutAssertAll() {
    User user = userService.findByUsername("sachin");

    assertEquals("sachin", user.getUsername());  // Pass
    assertEquals("sachin@email.com", user.getEmail());  // Pass
    assertEquals("ADMIN", user.getRole());  // ❌ FAIL — yahan ruk jayega
    assertTrue(user.isEnabled());  // Yeh run hi nahi hoga
}
```

### With assertAll (Grouped Assertions)

```java
@Test
void testUserWithAssertAll() {
    User user = userService.findByUsername("sachin");

    // Sab assertions run hongi — sab failures ek saath dikhengi
    assertAll("User properties",
        () -> assertEquals("sachin", user.getUsername()),
        () -> assertEquals("sachin@email.com", user.getEmail()),
        () -> assertEquals("ADMIN", user.getRole()),
        () -> assertTrue(user.isEnabled())
    );
}
```

### Nested assertAll

```java
@Test
void testComplexObject() {
    User user = userService.createUser("sachin", "sachin@email.com", "ADMIN");

    assertAll("User creation",
        () -> assertNotNull(user.getId()),
        () -> assertAll("Basic fields",
            () -> assertEquals("sachin", user.getUsername()),
            () -> assertEquals("sachin@email.com", user.getEmail())
        ),
        () -> assertAll("Permissions",
            () -> assertEquals("ADMIN", user.getRole()),
            () -> assertTrue(user.canAccessAdminPanel()),
            () -> assertTrue(user.canCreateUsers())
        )
    );
}
```

### AssertJ Alternative (Soft Assertions)

```java
@Test
void testWithAssertJSoftAssertions() {
    User user = userService.findByUsername("sachin");

    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(user.getUsername()).isEqualTo("sachin");
    softly.assertThat(user.getEmail()).isEqualTo("sachin@email.com");
    softly.assertThat(user.getRole()).isEqualTo("ADMIN");
    softly.assertThat(user.isEnabled()).isTrue();
    softly.assertAll();  // Sab failures ek saath
}
```

---

## assertTimeout

**Matlab:** Verify karna ki code within time limit complete hota hai.

### assertTimeout vs assertTimeoutPreemptively

```java
@Test
void testTimeout() {
    // assertTimeout — thread ko interrupt nahi karta, bas wait karta hai
    assertTimeout(Duration.ofSeconds(1), () -> {
        Thread.sleep(500);  // Pass — 500ms < 1s
    });

    // assertTimeoutPreemptively — thread ko abort kar deta hai
    assertTimeoutPreemptively(Duration.ofMillis(200), () -> {
        Thread.sleep(500);  // Fail — aborted after 200ms
    });
}
```

### Practical Usage

```java
@Test
void testApiPerformance() {
    // API should respond within 500ms
    assertTimeout(Duration.ofMillis(500), () -> {
        List<User> users = apiService.getAllUsers();
        assertNotNull(users);
    });
}

@Test
void testDatabaseQuery() {
    // Query should complete within 100ms
    assertTimeout(Duration.ofMillis(100), () -> {
        User user = userRepository.findByEmail("test@email.com");
        assertNotNull(user);
    });
}

// AssertJ alternative
@Test
void testTimeoutWithAssertJ() {
    assertThatCode(() -> {
        Thread.sleep(500);
    }).doesNotThrowAnyException();

    assertThat(Duration.ofMillis(500))
        .isLessThan(Duration.ofSeconds(1));
}
```

---

## assertDoesNotThrow

**Matlab:** Verify karna ki code koi exception nahi throw karta.

```java
@Test
void testNoException() {
    // Yeh code exception nahi phenkna chahiye
    assertDoesNotThrow(() -> {
        User user = userService.findByUsername("sachin");
        assertNotNull(user);
    });
}

@Test
void testValidParsing() {
    assertDoesNotThrow(() -> {
        JsonNode parsed = jsonParser.parse("{\"name\": \"sachin\"}");
        assertEquals("sachin", parsed.get("name").asText());
    });
}

// AssertJ alternative
@Test
void testNoExceptionWithAssertJ() {
    assertThatCode(() -> userService.findByUsername("sachin"))
        .doesNotThrowAnyException();
}
```

---

## AssertJ Fluent Assertions

**Matlab:** Readable, chainable assertions library. JUnit assertions se zyada expressive.

### Why AssertJ?

```java
// JUnit — verbose
assertNotNull(user);
assertEquals("sachin", user.getUsername());
assertEquals("sachin@email.com", user.getEmail());
assertTrue(user.getRoles().contains("ADMIN"));
assertTrue(user.getRoles().contains("USER"));
assertEquals(2, user.getRoles().size());

// AssertJ — fluent, readable
assertThat(user)
    .isNotNull()
    .extracting(User::getUsername, User::getEmail)
    .containsExactly("sachin", "sachin@email.com");

assertThat(user.getRoles())
    .contains("ADMIN", "USER")
    .hasSize(2);
```

### String Assertions

```java
@Test
void testStringAssertions() {
    String text = "Hello, World!";

    assertThat(text)
        .isNotNull()
        .isNotEmpty()
        .startsWith("Hello")
        .endsWith("!")
        .contains(",")
        .doesNotContain("foo")
        .hasSize(13)
        .isEqualToIgnoringCase("hello, world!")
        .matches("Hello, \\w+!");
}
```

### Number Assertions

```java
@Test
void testNumberAssertions() {
    int number = 42;

    assertThat(number)
        .isPositive()
        .isNotZero()
        .isGreaterThan(40)
        .isLessThan(50)
        .isEqualTo(42)
        .isBetween(30, 50);

    // BigDecimal
    BigDecimal price = new BigDecimal("19.99");
    assertThat(price)
        .isEqualByComparingTo("19.99")
        .isGreaterThan(new BigDecimal("10"))
        .isCloseTo(new BigDecimal("20"), Percentage.withPercentage(5));
}
```

### Collection Assertions

```java
@Test
void testCollectionAssertions() {
    List<String> names = List.of("sachin", "john", "jane");

    assertThat(names)
        .isNotNull()
        .isNotEmpty()
        .hasSize(3)
        .contains("sachin", "john")
        .doesNotContain("admin")
        .containsExactly("sachin", "john", "jane")  // Order matters
        .containsExactlyInAnyOrder("jane", "sachin", "john");  // Order doesn't matter

    // Empty/not empty
    List<String> empty = List.of();
    assertThat(empty).isEmpty();
    assertThat(empty).hasSize(0);
}
```

### Object Assertions

```java
@Test
void testObjectAssertions() {
    User user = new User("sachin", "sachin@email.com");
    user.setRole("ADMIN");
    user.setEnabled(true);

    // Extracting fields
    assertThat(user)
        .isNotNull()
        .extracting(User::getUsername)
        .isEqualTo("sachin");

    // Multiple fields
    assertThat(user)
        .extracting("username", "email", "role")
        .containsExactly("sachin", "sachin@email.com", "ADMIN");

    // Using method references
    assertThat(user)
        .extracting(User::getUsername, User::getEmail, User::getRole)
        .containsExactly("sachin", "sachin@email.com", "ADMIN");
}
```

### Exception Assertions with AssertJ

```java
@Test
void testExceptionAssertions() {
    assertThatThrownBy(() -> userService.findByUsername("nonexistent"))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessage("User not found: nonexistent")
        .hasMessageContaining("nonexistent")
        .hasMessageStartingWith("User not found")
        .hasMessageEndingWith("nonexistent")
        .hasNoCause();

    // Exception with cause
    assertThatThrownBy(() -> {
        throw new RuntimeException("Root cause",
            new IllegalArgumentException("Bad argument"));
    })
        .isInstanceOf(RuntimeException.class)
        .hasRootCauseInstanceOf(IllegalArgumentException.class)
        .hasRootCauseMessage("Bad argument");
}
```

### Map Assertions

```java
@Test
void testMapAssertions() {
    Map<String, Object> map = Map.of(
        "name", "sachin",
        "age", 25,
        "active", true
    );

    assertThat(map)
        .isNotEmpty()
        .hasSize(3)
        .containsEntry("name", "sachin")
        .containsKey("age")
        .doesNotContainKey("password")
        .containsKeys("name", "age")
        .containsValues("sachin", 25);
}
```

### Optional Assertions

```java
@Test
void testOptionalAssertions() {
    Optional<User> found = userService.findByUsername("sachin");

    assertThat(found)
        .isPresent()
        .get().extracting(User::getUsername).isEqualTo("sachin");

    Optional<User> notFound = userService.findByUsername("nonexistent");
    assertThat(notFound).isEmpty();
}
```

---

## Hamcrest Matchers

**Matlab:** Matcher-based assertion library. `assertThat(actual, matcher)` syntax use karta hai.

### Basic Usage

```java
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Test
void testHamcrest() {
    String name = "sachin";

    assertThat(name, is("sachin"));
    assertThat(name, is(not("john")));
    assertThat(name, equalTo("sachin"));
    assertThat(name, equalToIgnoringCase("SACHIN"));
}
```

### Collection Matchers

```java
@Test
void testCollectionMatchers() {
    List<String> names = List.of("sachin", "john", "jane");

    assertThat(names, hasSize(3));
    assertThat(names, hasItem("sachin"));
    assertThat(names, hasItems("sachin", "john"));
    assertThat(names, not(hasItem("admin")));
    assertThat(names, contains("sachin", "john", "jane"));  // Order matters
    assertThat(names, containsInAnyOrder("jane", "sachin", "john"));  // Order doesn't matter
    assertThat(names, not(empty()));
}
```

### String Matchers

```java
@Test
void testStringMatchers() {
    String text = "Hello, World!";

    assertThat(text, containsString("World"));
    assertThat(text, startsWith("Hello"));
    assertThat(text, endsWith("!"));
    assertThat(text, equalToIgnoringCase("hello, world!"));
    assertThat(text, equalToIgnoringWhiteSpace(" Hello, World! "));
}
```

### Object Matchers

```java
@Test
void testObjectMatchers() {
    User user = new User("sachin", "sachin@email.com");

    assertThat(user, notNullValue());
    assertThat(user, not(nullValue()));
    assertThat(user.getUsername(), is("sachin"));
    assertThat(user.getEmail(), containsString("@"));
}
```

### Combining Matchers

```java
@Test
void testCombinedMatchers() {
    int number = 42;

    assertThat(number, allOf(
        greaterThan(30),
        lessThan(50),
        not(equalTo(0))
    ));

    assertThat(number, anyOf(
        equalTo(42),
        equalTo(100)
    ));
}
```

---

## Quick Reference

```java
// JUnit Assertions
assertEquals(expected, actual);
assertNotNull(object);
assertNull(object);
assertTrue(condition);
assertFalse(condition);
assertThrows(Exception.class, () -> { ... });
assertAll(() -> assert1(), () -> assert2());
assertTimeout(Duration.ofSeconds(1), () -> { ... });
assertDoesNotThrow(() -> { ... });

// AssertJ (recommended)
assertThat(actual)
    .isEqualTo(expected)
    .isNotNull()
    .isNull()
    .isTrue()
    .isFalse()
    .contains("item")
    .hasSize(3)
    .startsWith("prefix")
    .extracting(User::getEmail).isEqualTo("test@email.com");

assertThatThrownBy(() -> { ... })
    .isInstanceOf(MyException.class)
    .hasMessageContaining("error");

// Hamcrest
assertThat(actual, is(equalTo(expected)));
assertThat(list, hasItem("item"));
assertThat(str, containsString("sub"));
assertThat(num, allOf(greaterThan(0), lessThan(100)));
```

---

## Summary

| Library | Best For | Style |
|---------|----------|-------|
| **JUnit Assertions** | Basic tests, no extra dependency | Static methods |
| **AssertJ** | Production code (recommended) | Fluent chaining |
| **Hamcrest** | Legacy projects, readable matchers | Matcher-based |

**Recommendation:** AssertJ use karo — readable, IDE-friendly, aur powerful. JUnit assertions simple checks ke liye theek hain.

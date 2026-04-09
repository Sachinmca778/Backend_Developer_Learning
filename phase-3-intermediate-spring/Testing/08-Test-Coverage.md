# Test Coverage

## Status: Not Started

---

## Table of Contents

1. [Test Coverage Overview](#test-coverage-overview)
2. [JaCoCo Plugin](#jacoco-plugin)
3. [Coverage Reports](#coverage-reports)
4. [Line Coverage vs Branch Coverage](#line-coverage-vs-branch-coverage)
5. [Mutation Testing (PIT)](#mutation-testing-pit)
6. [Coverage Thresholds in Build](#coverage-thresholds-in-build)
7. [What to Test](#what-to-test)

---

## Test Coverage Overview

**Matlab:** Kitna percentage code ka tests dwara cover kiya gaya hai. Coverage metric batata hai ki tests ne code ke kitne parts ko execute kiya.

### Why Coverage Matters

```
High Coverage (80%+):
    ✅ Most code paths tested
    ✅ Refactoring safe
    ✅ Bugs kam aayenge
    ⚠️ Coverage ≠ Quality (100% coverage bhi bugs allow kar sakta hai)

Low Coverage (<50%):
    ❌ Most code untested
    ❌ Refactoring dangerous
    ❌ Production bugs likely
    ❌ Technical debt indicator
```

### Coverage Types

```
Line Coverage:
    → Kitni lines execute hui
    → Most common metric

Branch Coverage:
    → Kitne if/else branches execute hue
    → More thorough than line coverage

Method Coverage:
    → Kitne methods call hue

Class Coverage:
    → Kitne classes load/call hue
```

---

## JaCoCo Plugin

**Matlab:** Java Code Coverage — Maven/Gradle plugin jo test coverage measure karta hai aur report generate karta hai.

### Maven Setup

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <!-- Test se pehle JaCoCo agent prepare karo -->
        <execution>
            <id>prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <!-- Test ke baad report generate karo -->
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Gradle Setup

```groovy
plugins {
    id 'jacoco'
}

jacoco {
    toolVersion = "0.8.12"
}

test {
    finalizedBy jacocoTestReport  // Test ke baad report generate karo
}

jacocoTestReport {
    dependsOn test
    reports {
        xml.required = true    // CI/CD ke liye
        html.required = true   // Human-readable report
        csv.required = false
    }
}
```

### Run JaCoCo

```bash
# Maven
mvn clean test           # Tests run + coverage report generate
mvn jacoco:report        # Sirf report generate karo (agar pehle se exec data hai)

# Gradle
./gradlew test           # Tests run + report (auto-configured)
./gradlew jacocoTestReport  # Sirf report generate karo
```

---

## Coverage Reports

**Matlab:** Coverage data ko readable format mein dekhna — HTML, XML, CSV.

### HTML Report

```bash
# Maven
mvn clean test
# Report: target/site/jacoco/index.html

# Gradle
./gradlew test
# Report: build/reports/jacoco/test/html/index.html
```

### HTML Report Structure

```
target/site/jacoco/index.html
├── Coverage Summary
│   ├── Classes: 85% (45/53 covered)
│   ├── Methods: 78% (234/300 covered)
│   ├── Lines: 75% (1,200/1,600 covered)
│   └── Branches: 65% (130/200 covered)
│
├── Package-wise Coverage
│   ├── com.example.service: 90%
│   ├── com.example.controller: 85%
│   ├── com.example.repository: 40%  (Low — need more tests)
│   └── com.example.config: 20%  (Low — mostly auto-config)
│
└── Class-wise Detail
    └── Click to see line-by-line coverage
        ├── Green line = Covered ✅
        ├── Red line = Not covered ❌
        └── Yellow line = Partially covered ⚠️ (branch coverage)
```

### XML Report (CI/CD)

```xml
<!-- target/site/jacoco/jacoco.xml -->
<!-- SonarQube, Codecov, Coveralls jaise tools ke liye -->
<report name="JaCoCo Coverage Report">
    <package name="com/example/service">
        <class name="UserService" sourcefilename="UserService.java">
            <method name="createUser" desc="...">
                <counter type="LINE" missed="0" covered="15"/>
                <counter type="BRANCH" missed="1" covered="3"/>
            </method>
        </class>
    </package>
</report>
```

### Exclude Packages from Report

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <configuration>
        <excludes>
            <!-- Configuration classes — test karna zaruri nahi -->
            <exclude>**/config/*</exclude>
            <exclude>**/dto/*</exclude>

            <!-- Auto-generated classes -->
            <exclude>**/model/*Entity_.class</exclude>

            <!-- Main application class -->
            <exclude>**/Application.class</exclude>
        </excludes>
    </configuration>
</plugin>
```

---

## Line Coverage vs Branch Coverage

**Matlab:** Do different coverage metrics — ek lines count karta hai, doosra branches (if/else paths).

### Line Coverage

```java
public User createUser(String username, String email) {
    User user = new User(username, email);       // Line 1 — covered ✅
    user.setCreatedAt(LocalDateTime.now());       // Line 2 — covered ✅
    user.setEnabled(true);                         // Line 3 — covered ✅
    userRepository.save(user);                     // Line 4 — covered ✅
    emailService.sendWelcomeEmail(user);           // Line 5 — covered ✅
    return user;                                   // Line 6 — covered ✅
}

Line Coverage: 6/6 = 100% ✅
```

### Branch Coverage

```java
public String getUserRole(User user) {
    if (user.isAdmin()) {          // Branch 1: true → covered ✅
        return "ADMIN";            // Covered ✅
    } else {                        // Branch 2: false → NOT covered ❌
        return "USER";             // NOT covered ❌
    }
}

Line Coverage: 2/4 = 50% ❌  (sirf if block execute hua)
Branch Coverage: 1/2 = 50% ❌  (sirf true path test hua)
```

### Complete Branch Coverage

```java
@Test
void testAdminUser() {
    User admin = new User("admin", "admin@email.com");
    admin.setAdmin(true);
    assertEquals("ADMIN", userService.getUserRole(admin));  // true path ✅
}

@Test
void testRegularUser() {
    User user = new User("sachin", "sachin@email.com");
    assertEquals("USER", userService.getUserRole(user));     // false path ✅
}

Branch Coverage: 2/2 = 100% ✅
```

### Complex Branch Coverage Example

```java
public String validatePassword(String password) {
    if (password == null) {                    // Branch 1
        return "Password cannot be null";
    }
    if (password.length() < 8) {               // Branch 2
        return "Password too short";
    }
    if (!password.matches(".*[A-Z].*")) {      // Branch 3
        return "Need uppercase letter";
    }
    if (!password.matches(".*[0-9].*")) {      // Branch 4
        return "Need a digit";
    }
    return "Valid";                             // Happy path
}

// Required test cases for 100% branch coverage:
// 1. null input
// 2. Short password ("abc")
// 3. No uppercase ("abcdefgh1")
// 4. No digit ("Abcdefgh")
// 5. Valid password ("Abcdefg1")
// Total: 5 test cases needed
```

### Coverage Quality Guidelines

| Coverage % | Status | Action |
|------------|--------|--------|
| **0-30%** | 🔴 Critical | Immediate attention needed |
| **30-50%** | 🟡 Poor | Add more tests urgently |
| **50-70%** | 🟡 Fair | Acceptable for legacy, improve for new code |
| **70-80%** | 🟢 Good | Standard target for most projects |
| **80-90%** | 🟢 Great | Well-tested codebase |
| **90%+** | 🔵 Excellent | Diminishing returns — focus on quality over % |

---

## Mutation Testing (PIT)

**Matlab:** Test quality check karna by intentionally introducing small bugs (mutations) aur dekhna ki tests detect karte hain ya nahi.

### Why Mutation Testing?

```
Code Coverage = Tests ne code execute kiya? (quantity)
Mutation Testing = Tests ne bugs detect kiye? (quality)

Example:
    Code: int result = a + b;
    Coverage: 100% — test ne yeh line execute ki ✅

    Mutation: int result = a - b;  // Bug introduced
    Test result: PASS (because test ne sirf execution check kiya, correctness nahi)

    Mutation score: 0% — Test ne bug nahi pakda ❌
```

### PIT Setup

```xml
<plugin>
    <groupId>org.pitest</groupId>
    <artifactId>pitest-maven</artifactId>
    <version>1.15.3</version>
    <dependencies>
        <dependency>
            <groupId>org.pitest</groupId>
            <artifactId>pitest-junit5-plugin</artifactId>
            <version>1.2.1</version>
        </dependency>
    </dependencies>
    <configuration>
        <targetClasses>
            <param>com.example.service.*</param>
        </targetClasses>
        <targetTests>
            <param>com.example.service.*</param>
        </targetTests>
    </configuration>
</plugin>
```

### Run PIT

```bash
mvn org.pitest:pitest-maven:mutationCoverage
# Report: target/pit-reports/YYYYMMDDHHMI/index.html
```

### Mutation Types

```java
// Original code
public int calculate(int a, int b) {
    return a + b;
}

// Mutations PIT introduces:
public int calculate(int a, int b) {
    return a - b;       // Arithmetic mutation (+ → -)
    return a * b;       // Arithmetic mutation (+ → *)
    return a / b;       // Arithmetic mutation (+ → /)
    return a % b;       // Arithmetic mutation (+ → %)
    return b;           // Return value mutation
    return 0;           // Return value mutation (to 0)
    return 1;           // Return value mutation (to 1)
    return -a;          // Negation mutation
}
```

### PIT Report

```
Mutation Coverage Report
├── Mutation Score: 85%  (17/20 mutations killed)
├── Line Coverage: 100%  (20/20 lines covered)
│
├── Killed Mutations (17) ✅
│   → Tests ne detect kiya
│
├── Survived Mutations (3) ❌
│   → Tests ne detect NAHI kiya — need better assertions
│
└── No Coverage (0) ⏭️
    → Tests ne execute hi nahi kiya
```

### Improving Mutation Score

```java
// Before — 100% coverage but 0% mutation score
@Test
void testCalculate() {
    int result = calculator.calculate(2, 3);
    assertNotNull(result);  // Sirf null check — mutation survive karegi!
}

// After — 100% coverage AND 100% mutation score
@Test
void testCalculate() {
    assertEquals(5, calculator.calculate(2, 3));   // Exact value check
    assertEquals(0, calculator.calculate(0, 0));   // Edge case
    assertEquals(-1, calculator.calculate(2, 3));  // Wait, wrong test!
    // PIT will detect if someone changes + to -
}
```

---

## Coverage Thresholds in Build

**Matlab:** Minimum coverage set karna — agar coverage threshold se neeche gaya toh build fail hoga. CI/CD mein quality gate.

### Maven Thresholds

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <id>check-coverage</id>
            <phase>verify</phase>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>  <!-- Pura project -->
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>  <!-- 80% line coverage required -->
                            </limit>
                            <limit>
                                <counter>BRANCH</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.60</minimum>  <!-- 60% branch coverage required -->
                            </limit>
                        </limits>
                    </rule>
                    <rule>
                        <element>CLASS</element>  <!-- Har individual class -->
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.50</minimum>  <!-- Har class min 50% covered -->
                            </limit>
                        </limits>
                        <excludes>
                            <exclude>com.example.config.*</exclude>  <!-- Config classes skip -->
                            <exclude>com.example.dto.*</exclude>     <!-- DTOs skip -->
                        </excludes>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Gradle Thresholds

```groovy
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = 'LINE'
                value = 'COVEREDRATIO'
                minimum = 0.80
            }
            limit {
                counter = 'BRANCH'
                value = 'COVEREDRATIO'
                minimum = 0.60
            }
        }

        rule {
            element = 'CLASS'
            includes = ['com.example.service.*']
            limit {
                counter = 'LINE'
                value = 'COVEREDRATIO'
                minimum = 0.90  // Services need 90% coverage
            }
        }
    }
}

// Build fail hoga agar coverage threshold se neeche hai
check.dependsOn jacocoTestCoverageVerification
```

### CI/CD Integration

```yaml
# GitHub Actions
name: Build & Test
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21

      - name: Run tests with coverage
        run: mvn clean verify  # Includes JaCoCo check

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v4
        with:
          file: target/site/jacoco/jacoco.xml
          fail_ci_if_error: true
```

### Build Failure Example

```
[ERROR] Rule violated for bundle my-project:
  lines covered ratio is 0.65, but expected minimum is 0.80
  branches covered ratio is 0.40, but expected minimum is 0.60

[ERROR] BUILD FAILURE
```

---

## What to Test

**Matlab:** Sab kuch test karna practical nahi hai. Priority set karna zaruri hai — kya test karna hai aur kya skip karna hai.

### Test Priority Matrix

```
Must Test (Priority 1):
    ✅ Business logic (Service layer)
    ✅ Edge cases (null, empty, boundary values)
    ✅ Error handling (exceptions, failures)
    ✅ Security rules (auth, authorization)
    ✅ Data validation (input validation)

Should Test (Priority 2):
    ✅ Repository queries (custom @Query methods)
    ✅ API endpoints (status codes, response format)
    ✅ External service integration
    ✅ Configuration loading

Can Skip (Priority 3):
    ⏭️ Getters/Setters (trivial)
    ⏭️ DTOs/Records (data holders)
    ⏭️ Configuration classes
    ⏭️ Lombok-generated code
    ⏭️ Auto-generated code
```

### Happy Path + Edge Cases + Error Cases

```java
class UserServiceTest {

    // === HAPPY PATH ===
    @Test
    void shouldCreateUserWithValidData() {
        User user = userService.createUser("sachin", "sachin@email.com");
        assertNotNull(user.getId());
        assertEquals("sachin", user.getUsername());
    }

    @Test
    void shouldFindExistingUser() {
        userService.createUser("sachin", "sachin@email.com");
        User found = userService.findByUsername("sachin");
        assertNotNull(found);
    }

    // === EDGE CASES ===
    @Test
    void shouldHandleMinimumUsername() {
        User user = userService.createUser("abc", "test@email.com");  // 3 chars — minimum
        assertNotNull(user);
    }

    @Test
    void shouldHandleSpecialCharactersInEmail() {
        User user = userService.createUser("sachin.kumar", "sachin.kumar+test@email.com");
        assertNotNull(user);
    }

    @Test
    void shouldHandleUnicodeCharacters() {
        User user = userService.createUser("sachin_हिंदी", "test@email.com");
        assertNotNull(user);
    }

    @Test
    void shouldHandleMaximumUsernameLength() {
        String longUsername = "a".repeat(255);
        assertThrows(ValidationException.class,
            () -> userService.createUser(longUsername, "test@email.com"));
    }

    // === ERROR CASES ===
    @Test
    void shouldThrowForNullUsername() {
        assertThrows(ValidationException.class,
            () -> userService.createUser(null, "test@email.com"));
    }

    @Test
    void shouldThrowForEmptyUsername() {
        assertThrows(ValidationException.class,
            () -> userService.createUser("", "test@email.com"));
    }

    @Test
    void shouldThrowForDuplicateEmail() {
        userService.createUser("sachin", "sachin@email.com");
        assertThrows(DuplicateEmailException.class,
            () -> userService.createUser("john", "sachin@email.com"));
    }

    @Test
    void shouldThrowForInvalidEmailFormat() {
        assertThrows(ValidationException.class,
            () -> userService.createUser("sachin", "not-an-email"));
    }
}
```

### Testing Anti-Patterns

```java
// ❌ Anti-pattern: Testing trivial getters/setters
@Test
void testUsernameGetter() {
    User user = new User();
    user.setUsername("sachin");
    assertEquals("sachin", user.getUsername());  // Lombok generate karta hai — test mat karo
}

// ❌ Anti-pattern: Testing framework behavior
@Test
void testSpringAutowiredWorks() {
    assertNotNull(userRepository);  // Spring already tests this
}

// ❌ Anti-pattern: Testing implementation details
@Test
void testPrivateMethodIndirectly() {
    // Private methods ka behavior public methods se test hota hai
    // Directly test mat karo
}

// ✅ Good pattern: Testing behavior
@Test
void shouldReturnFilteredUsers() {
    // Focus on WHAT, not HOW
    List<User> activeUsers = userService.getActiveUsers();
    assertThat(activeUsers).allMatch(User::isEnabled);
}
```

### Coverage vs Quality

```
100% Coverage ≠ Bug-Free Code

Example:
    Code: if (a > b) { return a; } else { return b; }
    Test: assertEquals(5, max(5, 3));  // Covers both branches

    Coverage: 100% ✅
    But test: max(3, 5) → 3 (bug!) → 5 expected

    Mutation testing would catch this
    → Change > to < → Test should fail
```

---

## Quick Reference

```bash
# JaCoCo — Run tests + coverage report
mvn clean test
mvn jacoco:report

# PIT — Mutation testing
mvn org.pitest:pitest-maven:mutationCoverage

# Coverage thresholds (Maven)
<minimum>0.80</minimum>  <!-- 80% line coverage -->
<minimum>0.60</minimum>  <!-- 60% branch coverage -->

# Report locations
JaCoCo HTML: target/site/jacoco/index.html
PIT HTML:    target/pit-reports/index.html
JaCoCo XML:  target/site/jacoco/jacoco.xml
```

---

## Summary

| Feature | Purpose |
|---------|---------|
| **JaCoCo** | Measure line and branch coverage |
| **Line Coverage** | Kitni lines execute hui |
| **Branch Coverage** | Kitne if/else paths execute hue |
| **Mutation Testing (PIT)** | Test quality check — tests bugs detect karte hain? |
| **Coverage Thresholds** | Build fail agar coverage minimum se neeche ho |
| **Happy Path** | Normal scenario test karo |
| **Edge Cases** | Boundary values, null, empty, unicode |
| **Error Cases** | Invalid input, exceptions, failures |

**Golden Rule:** Coverage tells you "how much" — mutation testing tells you "how well". Aim for 70-80% coverage with strong assertions, not just 100% with weak assertions.

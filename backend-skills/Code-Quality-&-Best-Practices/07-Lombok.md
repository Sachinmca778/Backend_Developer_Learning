# Lombok (Java)

## Status: Not Started

---

## Table of Contents

1. [Lombok Kya Hai?](#lombok-kya-hai)
2. [Setup](#setup)
3. [@Getter / @Setter](#getter--setter)
4. [@ToString](#tostring)
5. [@EqualsAndHashCode](#equalsandhashcode)
6. [@Data](#data)
7. [@Value — Immutable](#value--immutable)
8. [@Builder](#builder)
9. [Constructors](#constructors)
10. [@Slf4j](#slf4j)
11. [@NonNull](#nonnull)
12. [@SneakyThrows](#sneakythrows)
13. [Pitfalls — Especially JPA](#pitfalls--especially-jpa)
14. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Lombok Kya Hai?

**Matlab:** Annotation processor jo compile time par **boilerplate** generate karta hai — getters, constructors, builders, logging field.

```xml
<!-- Maven -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
</dependency>
```

IDE plugin install karo (IntelliJ: Lombok plugin bundled).

---

## Setup

- `annotationProcessorPaths` with Maven compiler plugin if needed
- Spring Boot parent usually manages version

---

## @Getter / @Setter

Class ya field level.

```java
@Getter
@Setter
public class User {
    private String name;
    private int age;
}
```

Generated: `getName()`, `setName()`, etc.

---

## @ToString

```java
@ToString(exclude = "passwordHash")
public class User {
    private String email;
    private String passwordHash;
}
```

**Care:** Large lazy collections / circular refs → stack overflow or huge logs.

---

## @EqualsAndHashCode

```java
@EqualsAndHashCode(of = "id")  // entity often identity only
public class Order {
    private UUID id;
    private BigDecimal total; // excluded from equals/hashCode
}
```

**Important:** Only mutable fields in equals → broken hash sets if object mutates after insert.

---

## @Data

**Shortcut =**

`@Getter` + `@Setter` + `@ToString` + `@EqualsAndHashCode` + `@RequiredArgsConstructor`

```java
@Data
public class OrderDto {
    private String orderId;
    private BigDecimal amount;
}
```

### Equivalent (mental model)

- All fields private with getters/setters
- `equals`/`hashCode` on **all** non-static fields (unless configured)
- Required-args constructor for `final` fields

---

## @Value — Immutable

**Immutable class** — all fields `final`, no setters, class `final`, `@AllArgsConstructor`, `@EqualsAndHashCode`, `@ToString`.

```java
@Value
public class Money {
    BigDecimal amount;
    Currency currency;
}
```

→ Getters only (`amount()`, `currency()` — **no `get` prefix** by default for `@Value`).

Use for **value objects**, DTOs that shouldn't mutate after creation.

---

## @Builder

Fluent object construction.

```java
@Builder
public class EmailMessage {
    String to;
    String subject;
    String body;
}

EmailMessage m = EmailMessage.builder()
    .to("a@b.com")
    .subject("Hi")
    .body("...")
    .build();
```

**With defaults:**

```java
@Builder
public class Config {
    @Builder.Default
    int timeoutMs = 5000;
}
```

**Superclass:** `@SuperBuilder` for inheritance hierarchies.

---

## Constructors

| Annotation | Generates |
|------------|-----------|
| `@NoArgsConstructor` | `public ClassName() {}` |
| `@AllArgsConstructor` | Constructor with every field |
| `@RequiredArgsConstructor` | `final` fields + `@NonNull` fields only |

Spring DI loves **`@RequiredArgsConstructor`** for constructor injection:

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orders;
}
```

---

## @Slf4j

Adds:

```java
private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderService.class);
```

```java
@Slf4j
@Service
public class OrderService {
    void place(Order o) {
        log.info("Placing order {}", o.getId());
    }
}
```

---

## @NonNull

Field / parameter — generated code throws `NullPointerException` if null passed (validation at runtime / constructor).

Often combined with `@RequiredArgsConstructor`.

---

## @SneakyThrows

Checked exception ko **declare kiye bina** throw — bytecode trick.

```java
@SneakyThrows(IOException.class)
void read() {
    Files.readAllLines(Path.of("x")); // IOException sneaky
}
```

**⚠️ Use sparingly:** Hides checked exceptions — reviewers often dislike except lambdas/interop cases.

Prefer: wrap in unchecked `UncheckedIOException` or proper throws clause.

---

## Pitfalls — Especially JPA

### @Data on JPA Entities — Dangerous Default

`@Data` includes **`equals`/`hashCode` on all fields** — relationships (`@OneToMany`) often cause:

- **Infinite recursion** (lazy collections in `toString`/`equals`)
- **Performance disaster** (touching whole graph)
- **Subtle bugs** in collections if entity mutates

**Better for entities:**

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Order {
    @EqualsAndHashCode.Include
    @Id
    private UUID id;

    @ToString.Exclude
    @OneToMany(mappedBy = "order")
    private List<OrderLine> lines;
}
```

Or: **`@EqualsAndHashCode(of = "id")`** + exclude lazy associations from `toString`.

### Immutability vs JPA

JPA entities typically **mutable** — `@Value` usually wrong for entities.

### @Builder on Entities

Default `@Builder` bypasses no-arg constructor — JPA needs **no-args ctor**. Use:

```java
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Order { ... }
```

Or `@SuperBuilder` carefully with inheritance.

### Lombok + MapStruct

Annotation processor **order** matters in `pom.xml` — Lombok before MapStruct.

---

## Summary Cheat Sheet

| Annotation | Purpose |
|------------|---------|
| `@Data` | Getter/Setter/ToString/EqualsHashCode/ReqArgsCtor |
| `@Value` | Immutable all-args value type |
| `@Builder` | Fluent builder |
| `@Slf4j` | Logger field |
| `@NoArgsConstructor` | Empty ctor |
| `@AllArgsConstructor` | Full ctor |
| `@RequiredArgsConstructor` | `final` + `@NonNull` fields ctor |
| `@NonNull` | Null check generate |
| `@SneakyThrows` | Hide checked (use rarely) |

| Pitfall | Fix |
|---------|-----|
| `@Data` entity | Narrow annotations; equals only `id`; exclude relations |
| Builder + JPA | Provide protected no-arg ctor |
| toString + lazy | Exclude associations |

---

## Practice

1. Ek DTO ko plain Java vs `@Data` compare karo — compile `target/generated-sources` dekho (delombok).
2. Ek JPA entity par `@Data` hata kar safe `@Getter` + `@EqualsAndHashCode(of="id")` likho.
3. `@Builder` + `@NoArgsConstructor(protected)` pattern implement karo.

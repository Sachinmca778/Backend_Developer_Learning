# Design Principles (Backend / Layered Apps)

## Status: Not Started

---

## Table of Contents

1. [Separation of Concerns](#separation-of-concerns)
2. [Fail Fast](#fail-fast)
3. [Defensive Programming](#defensive-programming)
4. [Immutability](#immutability)
5. [Composition Over Inheritance](#composition-over-inheritance)
6. [Program to Interfaces](#program-to-interfaces)
7. [Spring-Oriented Notes](#spring-oriented-notes)
8. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Separation of Concerns

**Matlab:** Har layer/module ka **apna kaam** — HTTP parsing, business rules, persistence alag.

### Typical layering (Spring)

```
Controller (Web)     → HTTP status, request/response mapping, validation trigger
Service (Domain)   → business rules, transactions, orchestration
Repository (Infra) → DB / external API access
```

```java
// ❌ Controller mein SQL / business rules
@RestController
class OrderController {
    @PostMapping("/orders")
    void create(@RequestBody OrderDto dto) {
        if (dto.getAmount() < 0) throw ...
        jdbcTemplate.update("INSERT ..."); // wrong layer
    }
}

// ✅ Thin controller
@RestController
class OrderController {
    private final OrderService orders;

    @PostMapping("/orders")
    ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest req) {
        return ResponseEntity.ok(orders.create(req));
    }
}
```

**Benefits:** Testing (service without MVC), reuse (CLI + REST same service), clear ownership.

**Watch:** **Anemic domain** — service sab kuch kare, entities getters/setters only — kabhi-kabhi rich domain models useful (DDD).

---

## Fail Fast

**Matlab:** Invalid state **jaldi** pakdo — deep call stack ke baad mat fail karo.

```java
// ✅ Entry par validate
public void transfer(Account from, Account to, Money amount) {
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
    if (amount.isNegativeOrZero()) {
        throw new IllegalArgumentException("amount must be positive");
    }
    // ...
}

// ❌ Deep inside DB transaction fail — harder to debug
```

**APIs:** `@Valid` on request DTOs, global exception handler for `MethodArgumentNotValidException`.

**Config:** Startup par critical config missing → fail (don't run half-broken).

---

## Defensive Programming

**Matlab:** Assume **external inputs unreliable** — validate boundaries; internal invariants **assert** / types.

Practices:
- Null-safe APIs (`Optional`, non-null annotations)
- Guard clauses at public methods
- Don't trust caller in library code

```java
public Money applyDiscount(Money price, int percentOff) {
    if (percentOff < 0 || percentOff > 100) {
        throw new IllegalArgumentException("percentOff out of range");
    }
    return price.multiply(BigDecimal.ONE.subtract(
        BigDecimal.valueOf(percentOff).movePointLeft(2)));
}
```

**Balance:** Har line par null check → noise. Use **Optional**, **constructor validation**, **bean validation** strategically.

---

## Immutability

**Matlab:** Jo change nahi hona chahiye use **immutable** rakho — bugs kam (especially concurrency).

### Java patterns

```java
// ✅ final fields set in constructor
public final class Money {
    private final BigDecimal amount;
    private final Currency currency;

    public Money(BigDecimal amount, Currency currency) {
        this.amount = amount;
        this.currency = Objects.requireNonNull(currency);
    }

    public Money add(Money other) {
        // new instance — mutating existing not
        return new Money(this.amount.add(other.amount), currency);
    }
}
```

- Prefer **`final`** class fields where possible
- **Immutable value objects** for money, date ranges, IDs with validation
- Collections: `List.copyOf`, `Collections.unmodifiableList`, or immutable libs (Guava)

```java
// ❌ Shared mutable state between threads without sync
```

---

## Composition Over Inheritance

**Matlab:** Behavior reuse ke liye **has-a** prefer karo **is-a** par — deep inheritance trees fragile.

```java
// ❌ Cart extends ArrayList<OrderLine> — wrong semantics, fragile

// ✅ Compose
class Cart {
    private final List<OrderLine> lines = new ArrayList<>();

    void add(OrderLine line) { ... }
}
```

**When inheritance OK:** True polymorphic substitution (LSP), framework hooks (`extends AbstractValidatingExecutorService` sparingly).

---

## Program to Interfaces

**Matlab:** Depend on **abstraction** — concrete class sirf wiring/DI par.

```java
interface PaymentGateway {
    PaymentResult charge(Money amount, Card card);
}

class StripePaymentGateway implements PaymentGateway { ... }

class CheckoutService {
    private final PaymentGateway payments; // interface

    CheckoutService(PaymentGateway payments) {
        this.payments = payments;
    }
}
```

Tests mein `FakePaymentGateway` inject — fast, deterministic.

---

## Spring-Oriented Notes

- **`@Service`** business orchestration; **`@Repository`** persistence; **`@RestController`** HTTP edge
- **Constructor injection** > field injection (testing, immutability)
- **`@Transactional`** service layer par usually — controller par thin
- Domain exceptions → map to HTTP in `@ControllerAdvice`

---

## Summary Cheat Sheet

| Principle | Meaning |
|-----------|---------|
| **SoC** | Controller / service / repo boundaries |
| **Fail fast** | Validate early; bad config at startup |
| **Defensive** | External input untrusted |
| **Immutability** | Value objects, final fields, fewer bugs |
| **Composition** | Prefer over deep inheritance |
| **Interfaces** | DIP + testability |

---

## Practice

1. Ek controller dhundo jisme business logic hai — service mein move ka plan banao.
2. Ek mutable DTO ko immutable response record/benefits se compare karo.
3. `extends` hierarchy ko composition se replace karne ka sketch banao.

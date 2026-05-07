# Refactoring Techniques

## Status: Not Started

---

## Table of Contents

1. [Refactoring Kya Hai?](#refactoring-kya-hai)
2. [Safety](#safety)
3. [Catalog of Techniques](#catalog-of-techniques)
4. [Smell → Technique Map](#smell--technique-map)
5. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Refactoring Kya Hai?

**Matlab:** External behavior **same** rakhte hue internal structure improve karna — readability, duplication kam, extension easy.

**Not:** Bug fix ya feature — ideally separate commit/PR for clarity.

---

## Safety

- **Tests green** before — characterization tests agar coverage kam
- **Small steps** — compile pass har step
- IDE automated refactorings use karo (rename, extract) — fewer mistakes
- Version control — frequent commits or one refactor branch with atomic commits

---

## Catalog of Techniques

### Extract Method

Long method ko meaningful chunks mein todna.

```java
// Before: one long method

// After
void process(Order o) {
    validate(o);
    applyPricing(o);
    persist(o);
}
```

### Extract Class

Ek class zyada kar rahi ho → fields + methods nayi class mein.

```java
// Order har cheez na kare — PricingCalculator, ShippingEstimator alag
```

### Rename

Poor name → clear intent. Cheapest high-impact refactor.

```java
void proc(Order o) { ... }
// → void submitOrderForFulfillment(Order o) { ... }
```

### Move Method

Method galat class mein hai → us class mein shift jahan data zyada use hoti hai (**feature envy** fix).

### Introduce Parameter Object

Bahut saare parameters → ek object.

```java
// Before
void book(String userId, LocalDate from, LocalDate to, int guests, String promo);

// After
void book(BookingRequest req);
```

### Replace Conditional with Polymorphism

Switch/type codes → interface implementations (**Strategy**).

```java
interface NotificationChannel {
    void send(User u, String msg);
}
```

### Replace Magic Numbers with Named Constants

```java
// Before
if (status == 3) { ... }

// After
private static final int STATUS_SHIPPED = 3;
// Better: enum OrderStatus { SHIPPED, ... }
```

### Decompose Conditional

Complex `if` → boolean methods with names.

```java
// Before
if (order.getLines().isEmpty() || order.getUser().isBlocked()) { ... }

// After
if (!order.hasShippableLines() || order.userBlocked()) { ... }
```

### Remove Duplication

Extract common logic — **after** third copy often clearer (DRY).

---

## Smell → Technique Map

| Smell | Technique |
|-------|-----------|
| Long method | Extract method |
| Large class | Extract class |
| Feature envy | Move method |
| Long parameter list | Introduce parameter object |
| Switch on type | Replace conditional with polymorphism |
| Magic number | Named constant / enum |
| Complex conditional | Decompose conditional |
| Duplicate code | Extract method/class |

---

## Summary Cheat Sheet

| Technique | Fixes |
|-----------|-------|
| Extract Method | Long methods |
| Extract Class | God class |
| Rename | Unclear intent |
| Move Method | Wrong ownership |
| Parameter Object | Many params |
| Polymorphism | Growing conditionals |
| Named constants | Magic values |
| Decompose conditional | Readable guards |

---

## Practice

1. 30+ line method dhundo — Extract Method se outline likho (names only).
2. 5-parameter method ko Parameter Object design karo.
3. Ek `switch (type)` ko polymorphism sketch se replace karo.

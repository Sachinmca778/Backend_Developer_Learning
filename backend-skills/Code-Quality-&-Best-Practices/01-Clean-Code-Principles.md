# Clean Code Principles

## Status: Not Started

---

## Table of Contents

1. [Clean Code Kya Hai?](#clean-code-kya-hai)
2. [SOLID Principles](#solid-principles)
3. [DRY — Don't Repeat Yourself](#dry--dont-repeat-yourself)
4. [KISS — Keep It Simple](#kiss--keep-it-simple)
5. [YAGNI — You Aren't Gonna Need It](#yagni--you-arent-gonna-need-it)
6. [Law of Demeter](#law-of-demeter)
7. [Meaningful Naming](#meaningful-naming)
8. [Small Functions & Files](#small-functions--files)
9. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Clean Code Kya Hai?

**Matlab:** Aisa code jo **padhna saaf**, **badlav safe**, aur **team ke saath maintain** karna easy ho. Sirf "chalta hai" nahi — quality matters.

**Goals:**
- Correctness + readability > clever tricks
- Future-you aur teammates ko thank-you milna chahiye, gaali nahi

---

## SOLID Principles

SOLID **object-oriented design** ke liye 5 principles hain (Robert C. Martin). Java/Spring backend mein roz ka kaam.

### S — Single Responsibility Principle (SRP)

**Matlab:** Ek class/module ka **ek hi reason** ho badalne ka — ek hi responsibility.

```java
// ❌ Payment + email + logging sab ek jagah
class OrderService {
    void placeOrder(Order o) {
        db.save(o);
        email.send(o.getUser(), "Order placed");
        logger.info("...");
    }
}

// ✅ Alag concerns alag classes (ya kam se kam methods ko thin wrappers)
class OrderService {
    private final OrderRepository orders;
    private final NotificationService notifications;

    void placeOrder(Order o) {
        orders.save(o);
        notifications.orderPlaced(o);
    }
}
```

**Signal:** Class naam ke peeche "aur" lagta hai mentally ("OrderAndEmailService") → split socho.

---

### O — Open/Closed Principle (OCP)

**Matlab:** **Extend** karne ke liye open, **modify** karne ke liye closed — naye behavior ke liye purani class ko baar-baar edit na karna.

```java
// ❌ Har naye discount type par switch badlega
double discount(Order o, String type) {
    return switch (type) {
        case "FLAT" -> 10;
        case "PERCENT" -> o.getTotal() * 0.1;
        default -> 0;
    };
}

// ✅ Strategy / polymorphism — nayi strategy = nayi class
interface DiscountPolicy {
    Money apply(Order o);
}

class PercentDiscount implements DiscountPolicy { ... }
class FlatDiscount implements DiscountPolicy { ... }
```

---

### L — Liskov Substitution Principle (LSP)

**Matlab:** Parent ki jagah child daalo — **program break na ho**. Subtypes parent ki contract **violate** na karein.

```java
// ❌ Square extends Rectangle — height set karne par width badal do → expects rectangle wala behavior toot jaata hai
class Rectangle {
    void setWidth(int w) { ... }
    void setHeight(int h) { ... }
}

// ✅ Alag shapes, common interface without wrong inheritance
interface Shape { int area(); }
```

**Common violations:** Throw `UnsupportedOperationException` on overridden methods, empty overrides, stricter preconditions on subclass.

---

### I — Interface Segregation Principle (ISP)

**Matlab:** Client ko **wo interfaces do jo use karta hai** — mota "God interface" mat do.

```java
// ❌ Sabko Printer + Scanner + Fax ek interface
interface MultiFunctionDevice {
    void print();
    void scan();
    void fax();
}

// ✅ Chhote interfaces
interface Printer { void print(); }
interface Scanner { void scan(); }
```

Spring mein: bahut bade service interfaces ko split karna readability aur testing ke liye helpful.

---

### D — Dependency Inversion Principle (DIP)

**Matlab:** High-level modules low-level details par depend na karein — **abstractions** par depend karo.

```java
// ❌ OrderService directly JDBC / concrete HttpClient
class OrderService {
    void save() {
        DriverManager.getConnection(...); // tight coupling
    }
}

// ✅ Interface inject karo (constructor injection — Spring default)
class OrderService {
    private final OrderRepository repo;

    OrderService(OrderRepository repo) {
        this.repo = repo;
    }
}
```

**Spring:** `@Bean` interfaces + implementations — testing mein mocks/fakes easy.

---

## DRY — Don't Repeat Yourself

**Matlab:** Same logic **do jagah mat likho** — ek jagah badlo, sab sync rahe.

**But:** Over-DRY bhi dangerous — unrelated cheezein force merge mat karo ("accidental duplication" vs "conceptual duplication").

```java
// ❌ Validation email ka copy-paste har endpoint par

// ✅ Shared validator / utility / domain method
boolean isValidEmail(String e) { ... }
```

**Counterbalance:** Sometimes **small duplication** is cheaper than wrong abstraction. Rule of three: third copy dikhe tab abstract karo.

---

## KISS — Keep It Simple

**Matlab:** Simple solution jo requirement poora kare — **complexity tabhi** jab zaroori ho.

- Nested callbacks / clever bit hacks → avoid unless perf-critical and measured
- Prefer boring readable code over "smart" one-liners

```java
// ❌ Clever but unreadable
return list.stream().collect(groupingBy(...)).entrySet().stream()...

// ✅ Clear steps (sometimes loop is OK!)
Map<String, List<Item>> byCategory = new HashMap<>();
for (Item item : items) {
    byCategory.computeIfAbsent(item.getCategory(), k -> new ArrayList<>()).add(item);
}
```

---

## YAGNI — You Aren't Gonna Need It

**Matlab:** Abhi **requirement nahi** hai toh feature / abstraction **mat banao** — future speculation expensive hoti hai.

- Generic plugin system jab sirf 2 cases hain → YAGNI violation
- `abstract FactoryFactory` — usually overkill for MVP

**Balance:** Architecture boundaries (interfaces for DB, messaging) often worth it early — "needless flexibility" vs "healthy seams" distinguish karna seekho.

---

## Law of Demeter

**Matlab:** Object ko **sirf apne direct friends** se baat karni chahiye — **train wreck** avoid (`a.getB().getC().doThing()`).

Also called **principle of least knowledge**.

```java
// ❌ Demeter violation — Order expose chain
order.getCustomer().getAddress().getCity();

// ✅ Tell, don't ask — behavior customer/address par
order.getShippingCity();
// ya
customer.getShippingCity();
```

**Benefits:** Coupling kam, refactoring easy (address structure badle toh ek jagah fix).

---

## Meaningful Naming

### Rules of Thumb

| Do | Avoid |
|----|-------|
| Intention reveal kare (`calculateDiscount`) | `process`, `handle`, `doIt` |
| Domain language use karo | Generic tech jargon everywhere |
| Consistent vocabulary (`fetch` vs `get` mix mat karo) | Same concept different names |
| Pronounceable names | `genymdhms` |
| Searchable names (`MAX_RETRIES` not magic `7`) | Single-letter except tiny scopes |

### Booleans

```java
boolean isActive;
boolean hasPermission;
boolean shouldRetry;
```

### Methods

```java
// Verb / verb phrase
void saveOrder(Order o);
Money calculateTax(Order o);
Optional<User> findUserById(UUID id);
```

### Collections

```java
List<Order> pendingOrders;   // plural
Map<UserId, Cart> cartsByUserId;
```

### Avoid Encodings

```java
// ❌ Hungarian notation, type prefix (mostly obsolete in Java)
String strName;

// ✅
String customerName;
```

---

## Small Functions & Files

- Function ek **clear level of abstraction** — mix mat karo (low-level parsing + high-level workflow ek method mein)
- **Early returns** se nesting kam
- Class/file **reasonably sized** — 300+ lines smell hai review karo (hard rule nahi, guideline)

---

## Summary Cheat Sheet

| Principle | One Line |
|-----------|----------|
| **S**RP | Ek class, ek reason to change |
| **O**CP | Extend karo, har feature par purani file mat todo |
| **L**SP | Subclass parent ki jagah safely substitute ho |
| **I**SP | Chhote interfaces — client ko extra methods mat thopo |
| **D**IP | Details par nahi, abstractions par depend karo |
| **DRY** | Duplication hatao — par galat abstraction mat |
| **KISS** | Simple > clever |
| **YAGNI** | Abhi zaroorat nahi toh mat banao |
| **Demeter** | Chain getters avoid — encapsulate behavior |
| **Naming** | Intent clear, domain words, consistent |

---

## Practice

1. Ek class identify karo jisme 2+ responsibilities hon — SRP se split plan likho.
2. Switch/if ladder jo naye types par badhti hai — OCP ke hisaab se polymorphism sketch karo.
3. Codebase mein `getX().getY().getZ()` chain dhundo — Demeter-friendly API suggest karo.
4. Ek YAGNI example (over-engineered code) aur simpler alternative likho.
5. Poorly named 5 variables/methods rename karo (same behavior).

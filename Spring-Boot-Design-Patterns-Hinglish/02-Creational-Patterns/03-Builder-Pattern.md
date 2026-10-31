# Creational Pattern: Builder Pattern in Spring Boot

---

### 1. Core Concept (Builder Pattern Kya Hai?)
**Answer (Hinglish):**

> **Build a complex object step by step, with highly readable and fluent code.**  
> *(Bilkul Subway burger ki tarah: Aap step-by-step bread, sauces, aur veggies choose karke custom burger build karte ho).*

Jab kisi class me bahut saari fields hoti hain (kuch mandatory, kuch optional), to unke objects banana aur unka code read karna difficult ho jata hai. Builder pattern is complexity ko solve karta hai.

---

### 2. The Problem: Telescoping Constructor Anti-Pattern

Maan lo aapke paas ek `Order` class hai jisme 8-10 fields hain:

#### ❌ The Ugly Constructor Call:
```java
Order order = new Order(101L, "Sachin", null, null, true, 0.0, "UPI", null, true);
```

**Isme 3 badi problems hain:**
1. **Unreadable Code:** `null, null, true` kya hai? Kaun sa boolean discount ka hai aur kaun sa express delivery ka? Samajh hi nahi aata!
2. **Order Dependency:** Agar constructor me 2 `String` parameters sath me hain (`email`, `address`), to swap hone par compiler koi error nahi dega, direct production bug aayega.
3. **Multiple Constructor Overloads:** Har optional combination ke liye alag constructor likhna padta hai (Telescoping problem).

---

### 3. The Modern Solution: Builder Pattern (with Lombok `@Builder`)

```java
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Order {
    private final Long id;
    private final String customer;
    private final String paymentMode;
    private final boolean express;
    private final Double discount;
    private final String couponCode;
}
```

#### ✅ Clean, Named & Readable Usage:
```java
Order order = Order.builder()
    .id(101L)
    .customer("Sachin")
    .paymentMode("UPI")
    .express(true)
    .build(); // Optional fields (discount, couponCode) naturally omitted!
```

**Kyu Best Hai:**
- **Named Arguments:** Har value ke pehle method name hota hai (`.customer("Sachin")`), jisse code self-explanatory ban jata hai.
- **Optional Fields Easy:** Jo fields nahi chahiye unhe pass hi mat karo (Default `null`/`false` rehti hain).
- **Immutability Guarantee:** Saare fields ko `private final` bana sakte hain, object create hone ke baad tamper-proof rehta hai.

---

### 4. Builders You Already Use Everyday in Spring Boot!

Spring Framework ke andar Builder pattern har jagah built-in hai:

#### A. `ResponseEntity` (HTTP Responses):
```java
@GetMapping("/{id}")
public ResponseEntity<OrderDTO> getOrder(@PathVariable Long id) {
    OrderDTO dto = orderService.findById(id);

    return ResponseEntity.ok() // Builder starts
        .header("X-App-Version", "v2.5")
        .header("Cache-Control", "no-cache")
        .body(dto); // .build() internally called
}
```

#### B. `WebClient` (Modern Reactive HTTP Client):
```java
WebClient webClient = WebClient.builder()
    .baseUrl("https://api.payments.com")
    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    .build();
```

#### C. `UriComponentsBuilder` (Dynamic URL Construction):
```java
URI uri = UriComponentsBuilder.fromHttpUrl("https://example.com/api")
    .path("/orders/{id}")
    .queryParam("status", "ACTIVE")
    .buildAndExpand(101)
    .toUri();
// Result: https://example.com/api/orders/101?status=ACTIVE
```

---

### 💡 Pro Interview Tip:
> **Question:** *"When should you choose Builder Pattern vs Java Records?"*  
> **Your Answer:**  
> - Agar class me **4 ya usse zyada fields** hain, jisme se kayi fields **optional** hain, to **`Builder Pattern`** best choice hai.  
> - Agar class ek simple, immutable data-carrier DTO hai jisme sirf 2-3 fields hain, to unnecessary Builder boilerplate lagane ke bajaye modern **Java `record`** (`public record UserDTO(Long id, String name) {}`) prefer karein!

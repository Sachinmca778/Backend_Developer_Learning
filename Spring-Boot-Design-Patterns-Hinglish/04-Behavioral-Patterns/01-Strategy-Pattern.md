# Behavioral Pattern: Strategy Pattern in Spring Boot 🎯

---

### 1. Core Concept (Strategy Pattern Kya Hai?)
**Answer (Hinglish):**

> **Define a family of interchangeable algorithms, put each algorithm in its own dedicated class, and choose one dynamically at runtime.**  
> *(Analogy: Destination tak pahunchne ke multiple raaste hain: Car, Bike ya Metro. Destination same hai, lekin context aur zaroorat ke hisaab se aap Strategy choose karte ho).*

---

### 2. ❌ The Nightmare: The "If-Else Monster"

Jab kisi project me business rules grow hote hain, to inexperienced developers ek hi method me 200 lines ka if-else ya switch-case likh dete hain:

```java
@Service
public class BadPaymentService {

    public void processPayment(String mode, double amount) {
        if ("UPI".equalsIgnoreCase(mode)) {
            // 40 lines of UPI logic
        } else if ("CARD".equalsIgnoreCase(mode)) {
            // 60 lines of Credit/Debit card logic
        } else if ("NETBANKING".equalsIgnoreCase(mode)) {
            // 50 lines of NetBanking logic
        } else if ("CRYPTO".equalsIgnoreCase(mode)) {
            // 50 lines of Crypto logic
        } // ❌ Horrible design!
    }
}
```

#### Why is this dangerous?
1. **Violates Open/Closed Principle (OCP):** Har bar naya payment option aane par aapko purani working tested class me ghus kar modify karna padta hai (Regression bug risk).
2. **Merge Conflicts:** Multiple developers ek hi class aur method par kaam karte hain.
3. **Hard to Unit Test:** Har test case me pure if-else block ko mock karna padta hai.

---

### 3. ✅ The Solution: Strategy Pattern + Spring Map/List Injection

#### Step 1: Define the Strategy Interface
```java
public interface PaymentStrategy {
    String getMode(); // Identifies strategy, e.g. "UPI"
    PaymentResult pay(Order order);
}
```

#### Step 2: Implement Isolated Strategy Classes
```java
@Component
public class UpiPaymentStrategy implements PaymentStrategy {
    @Override
    public String getMode() { return "UPI"; }

    @Override
    public PaymentResult pay(Order order) {
        System.out.println("Processing UPI Payment for order: " + order.getId());
        return new PaymentResult("SUCCESS");
    }
}

@Component
public class CardPaymentStrategy implements PaymentStrategy {
    @Override
    public String getMode() { return "CARD"; }

    @Override
    public PaymentResult pay(Order order) {
        System.out.println("Processing Credit/Debit Card Payment");
        return new PaymentResult("SUCCESS");
    }
}

@Component
public class NetBankingStrategy implements PaymentStrategy {
    @Override
    public String getMode() { return "NETBANKING"; }

    @Override
    public PaymentResult pay(Order order) {
        System.out.println("Processing NetBanking Payment");
        return new PaymentResult("SUCCESS");
    }
}
```

#### Step 3: Context Service (Dynamic Strategy Resolution)
Spring Boot automatic constructor injection se saari strategies ko ek `List<PaymentStrategy>` me inject kar deta hai:

```java
@Service
public class PaymentService {

    private final Map<String, PaymentStrategy> strategyMap;

    // Spring auto-discovers all PaymentStrategy beans and injects them as a List!
    public PaymentService(List<PaymentStrategy> strategies) {
        this.strategyMap = strategies.stream()
            .collect(Collectors.toMap(PaymentStrategy::getMode, Function.identity()));
    }

    public PaymentResult processPayment(String mode, Order order) {
        PaymentStrategy strategy = strategyMap.get(mode.toUpperCase());
        
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported payment mode: " + mode);
        }

        // Clean, polymorphic execution!
        return strategy.pay(order);
    }
}
```

---

### 4. Benefits in Real Projects:
- **New Payment Mode = Add Just ONE New Class:** Agar kal **"APPLE_PAY"** add karna ho, to sirf ek nayi class `ApplePayStrategy` banani hai. Existing code me ek character bhi change nahi hoga!
- **Pure Unit Testing:** `UpiPaymentStrategyTest` ko isolated test karo, kisi doosre mode se koi matlab nahi.
- **Java Standard Library Example:** Java ka `Comparator` jo hum `list.sort(comparator)` me pass karte hain, wo standard Strategy Pattern ka example hai!

---

### 💡 Pro Interview Tip:
> **Question:** *"How do you eliminate a large if-else ladder or switch block in production code?"*  
> **Your Answer:** *"By applying the **Strategy Pattern** combined with Spring's automatic `List<Interface>` or `Map<String, Interface>` dependency injection. Each branch becomes an independent Spring bean implementing a common interface, and the service resolves the right strategy at runtime dynamically!"*

# Design Patterns in Java - Top 10 Interview Questions & Answers

> Covering: Creational (Singleton, Builder, Factory), Structural (Adapter, Decorator, Proxy, Facade), Behavioral (Strategy, Observer, Template Method), and Spring-specific patterns

---

## Q1: Design Patterns kya hote hain aur inke kitne types hain?

**Answer:**

Design patterns reusable **solutions** hain common software design problems ke liye. Ye code nahi, balki proven **blueprints** hain jo maintainability aur flexibility badhate hain.

### Teen Main Categories (GoF):

| Category | Purpose | Examples |
|----------|---------|----------|
| **Creational** | Object creation ko abstract karna | Singleton, Builder, Factory, Prototype |
| **Structural** | Classes/objects ko compose karna | Adapter, Decorator, Proxy, Facade, Composite |
| **Behavioral** | Objects ke beech communication | Strategy, Observer, Template Method, Command |

### Spring already inko use karta hai:

- **Singleton** → default bean scope
- **Factory Method** → `@Bean` methods
- **Proxy** → AOP, `@Transactional`, `@Cacheable`
- **Template Method** → `JdbcTemplate`, `RestTemplate`
- **Prototype** → `@Scope("prototype")`

**Short Interview Answer:**

Design patterns proven solutions hain common problems ke. 3 types — Creational (object creation), Structural (composition), Behavioral (communication). Spring internally inhi patterns par bana hai.

---

## Q2: Singleton pattern thread-safe kaise banate hain? Best approach kaunsa hai?

**Answer:**

Singleton ensure karta hai ki class ka **sirf ek instance** ho aur global access mile.

### Naive (❌ Not Thread-Safe):

```java
public static Config getInstance() {
    if (instance == null) {
        instance = new Config();   // race condition!
    }
    return instance;
}
```

### Double-Checked Locking (DCL):

```java
public class Config {
    private static volatile Config instance;   // volatile MANDATORY
    private Config() {}

    public static Config getInstance() {
        if (instance == null) {
            synchronized (Config.class) {
                if (instance == null) {
                    instance = new Config();
                }
            }
        }
        return instance;
    }
}
```

### Bill Pugh (Holder) — Cleanest:

```java
public class Config {
    private Config() {}
    private static class Holder {
        private static final Config INSTANCE = new Config();
    }
    public static Config getInstance() { return Holder.INSTANCE; }
}
```

### Enum Singleton — Best (Joshua Bloch):

```java
public enum Config {
    INSTANCE;
    private final Map<String, String> settings = new HashMap<>();
}
```

### Comparison:

| Approach | Thread Safe | Lazy | Reflection Safe | Serialization Safe |
|----------|:-:|:-:|:-:|:-:|
| Naive | ❌ | ✅ | ❌ | ❌ |
| DCL | ✅ | ✅ | ❌ | ❌ |
| Bill Pugh | ✅ | ✅ | ❌ | ❌ |
| **Enum** | ✅ | ❌ | ✅ | ✅ |

**Important:**

`volatile` DCL mein mandatory hai — instruction reordering rokta hai, warna partially-constructed object dikh sakta hai. Real projects mein Spring ka singleton bean use karo, manual singleton avoid karo (testability kharab hoti hai).

---

## Q3: Builder pattern kab aur kyun use karte hain?

**Answer:**

Builder pattern **step-by-step object construction** deta hai, especially jab bahut saare optional parameters ho aur immutable object chahiye.

### Problem — Telescoping Constructor:

```java
new Pizza("Large", true, true, false, "Thin");  // booleans ka matlab kya?
```

### Builder Solution:

```java
Pizza pizza = Pizza.builder()
    .size("Large")
    .cheese(true)
    .crust("Thin")
    .topping("mushroom")
    .build();
```

### Lombok @Builder (Production):

```java
@Builder
@Getter
public class Pizza {
    private String size;
    @Builder.Default private boolean cheese = true;
    @Singular private List<String> toppings;
}
```

### Kab Use Kare?

| ✅ Use | ❌ Avoid |
|--------|---------|
| 4+ constructor params | Simple 1-2 fields |
| Many optional fields | Mutable state needed |
| Immutable objects chahiye | |
| Readable construction | |

**Short Interview Answer:**

Builder tab use karo jab object mein bahut optional params ho — telescoping constructor ki jagah readable, immutable construction milta hai. Production mein Lombok `@Builder` best hai. `build()` mein validation daalo.

---

## Q4: Factory Method aur Abstract Factory mein kya difference hai?

**Answer:**

| | Factory Method | Abstract Factory |
|--|---------------|------------------|
| **Creates** | Single product | Family of related products |
| **Mechanism** | Subclass/method override | Composition (factory object) |
| **Example** | `Calendar.getInstance()` | `Connection` → Statement, ResultSet |

### Factory Method:

```java
public class PaymentProcessorFactory {
    public static PaymentProcessor create(String type) {
        return switch (type) {
            case "CARD" -> new CardProcessor();
            case "UPI" -> new UpiProcessor();
            default -> throw new IllegalArgumentException(type);
        };
    }
}
```

### Better — Registry Pattern (Spring DI, no switch):

```java
@Component
public class PaymentRegistry {
    private final Map<String, PaymentProcessor> processors;

    public PaymentRegistry(List<PaymentProcessor> all) {
        this.processors = all.stream()
            .collect(Collectors.toMap(p -> p.type(), p -> p));
    }
    public PaymentProcessor get(String type) { return processors.get(type); }
}
```

→ Naya type add karna = sirf nayi class banao (Open-Closed Principle).

### Abstract Factory:

```java
public interface UIFactory {
    Button createButton();
    Window createWindow();
}
public class DarkThemeFactory implements UIFactory { ... }
```

→ Ek hi family ke saare objects milte hain (consistency guaranteed).

**Important:**

Spring mein `@Bean` methods = factory methods. `FactoryBean<T>` = abstract factory mechanism. Switch statement grow hone lage to Map-based registry + DI use karo.

---

## Q5: Proxy pattern kya hai? Spring AOP isko kaise use karta hai?

**Answer:**

Proxy ek **stand-in object** hai jo doosre object tak access control karta hai — same interface, but calls intercept karta hai.

### Types of Proxies:

| Type | Purpose |
|------|---------|
| Virtual Proxy | Lazy initialization (Hibernate) |
| Protection Proxy | Access control / auth |
| Remote Proxy | Network call hide (RPC) |
| Caching Proxy | Results cache karna |

### Spring AOP — Built-in Proxy:

```java
@Component
public class UserService {
    @Transactional
    @Cacheable("users")
    public User findById(Long id) { ... }
}
```

Spring `UserService` ko proxy se wrap karta hai:

```
Client → UserService Proxy → Real UserService
              ├─ start transaction
              ├─ check cache
              └─ real method
```

### JDK Dynamic Proxy vs CGLIB:

| | JDK Proxy | CGLIB |
|--|-----------|-------|
| Works for | Interfaces only | Classes (subclassing) |
| Mechanism | `Proxy.newProxyInstance` | Bytecode generation |

### ⚠️ Self-Invocation Pitfall:

```java
@Service
public class MyService {
    @Transactional
    public void outer() {
        inner();   // ❌ NOT transactional — proxy bypass!
    }
    @Transactional
    public void inner() { ... }
}
```

→ Spring proxy sirf **external** calls intercept karta hai. Internal `this.inner()` skip ho jaata hai.

**Fix:** Injected self-reference ya alag class mein refactor karo.

**Short Interview Answer:**

Proxy access control karta hai same interface ke saath. Spring AOP proxy se `@Transactional`, `@Cacheable` implement karta hai — JDK proxy (interfaces) ya CGLIB (classes). Biggest gotcha: self-invocation proxy ko bypass karta hai.

---

## Q6: Decorator pattern kya hai? Ye inheritance se better kaise hai?

**Answer:**

Decorator object mein **behavior dynamically add** karta hai, class modify kiye bina — wrap, wrap, wrap.

### Inheritance ki Problem:

```java
// ❌ Class explosion
class CompressedEncryptedBufferedFile extends File { ... }
class EncryptedBufferedFile extends File { ... }
// Combinatorial explosion!
```

### Decorator Solution:

```java
// ✅ Flexibly combine
File file = new CompressedFile(new EncryptedFile(new BufferedFile(rawFile)));
```

### Classic Java Example — java.io:

```java
InputStream raw = new FileInputStream("data.bin");
InputStream buffered = new BufferedInputStream(raw);
InputStream gzipped = new GZIPInputStream(buffered);
DataInputStream data = new DataInputStream(gzipped);
```

Har layer behavior add karti hai — buffering, decompression, primitive read.

### Custom Decorator:

```java
public abstract class NotifierDecorator implements Notifier {
    protected final Notifier wrapped;
    public NotifierDecorator(Notifier w) { this.wrapped = w; }
}

Notifier n = new SlackDecorator(new SmsDecorator(new EmailNotifier()));
n.send("Alert!");   // Email + SMS + Slack all triggered
```

**Important:**

Decorator vs Inheritance — inheritance compile-time, static hoti hai; decorator runtime pe flexible combinations deta hai bina subclass explosion ke. Downside: har layer = ek method call overhead.

---

## Q7: Strategy pattern kya hai aur real projects mein kaise use hota hai?

**Answer:**

Strategy pattern **algorithms ki family** define karta hai, har ek ko encapsulate karta hai, aur runtime pe swap karne deta hai.

### Use Cases:

- Payment methods (card, UPI, wallet)
- Compression (gzip, zip, brotli)
- Sorting (`Comparator`)
- Pricing (regular, premium, discount)

### Implementation:

```java
public interface PaymentStrategy {
    void pay(double amount);
}

public class CardPayment implements PaymentStrategy {
    public void pay(double amount) { System.out.println("Card: " + amount); }
}

public class UpiPayment implements PaymentStrategy {
    public void pay(double amount) { System.out.println("UPI: " + amount); }
}

// Context
public class PaymentContext {
    private PaymentStrategy strategy;
    public void setStrategy(PaymentStrategy s) { this.strategy = s; }
    public void checkout(double amount) { strategy.pay(amount); }
}
```

### Spring Way — DI + Map:

```java
@Service
public class PaymentService {
    private final Map<String, PaymentStrategy> strategies;

    public PaymentService(Map<String, PaymentStrategy> strategies) {
        this.strategies = strategies;   // bean name → strategy
    }
    public void pay(String type, double amount) {
        strategies.get(type).pay(amount);
    }
}
```

**Short Interview Answer:**

Strategy runtime pe algorithm swap karne deta hai — same interface, alag implementations. Java mein `Comparator` classic example hai. Spring mein `Map<String, Strategy>` inject karke if-else ladders eliminate karte hain.

---

## Q8: Observer pattern kya hai? Spring Events isse kaise relate karte hain?

**Answer:**

Observer pattern mein ek **subject** apni state change hone par multiple **observers** ko automatically notify karta hai (one-to-many dependency).

### Classic Implementation:

```java
public interface Observer {
    void update(String event);
}

public class Subject {
    private List<Observer> observers = new ArrayList<>();
    public void subscribe(Observer o) { observers.add(o); }
    public void notifyAll(String event) {
        observers.forEach(o -> o.update(event));
    }
}
```

### Spring Events — Built-in Observer:

```java
// 1. Event
public class OrderPlacedEvent {
    private final Long orderId;
    // constructor, getter
}

// 2. Publisher
@Service
public class OrderService {
    private final ApplicationEventPublisher publisher;

    public void placeOrder(Order order) {
        // ... save order
        publisher.publishEvent(new OrderPlacedEvent(order.getId()));
    }
}

// 3. Listener (Observer)
@Component
public class EmailListener {
    @EventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        // send confirmation email
    }
}
```

### Benefits:

- **Loose coupling** — publisher listeners ko nahi jaanta
- **Extensible** — naya listener add karna easy
- `@Async` se async processing possible

**Important:**

Spring's `ApplicationEventPublisher` = observer pattern. Multiple listeners ek event pe react kar sakte hain bina publisher change kiye. `@TransactionalEventListener` se transaction commit ke baad hi event fire karwa sakte ho.

---

## Q9: Template Method aur Facade pattern mein kya difference hai?

**Answer:**

### Template Method:

Ek algorithm ka **skeleton** define karta hai, kuch steps subclass ko override karne deta hai.

```java
public abstract class DataProcessor {
    // Template method — algorithm skeleton (final)
    public final void process() {
        readData();
        transform();   // subclass-specific
        saveData();
    }
    private void readData() { ... }    // fixed
    protected abstract void transform(); // hook
    private void saveData() { ... }    // fixed
}
```

→ Spring's `JdbcTemplate`, `RestTemplate` isi pattern par bane hain — boilerplate (connection, exception handling) fix, aap sirf logic dete ho.

### Facade:

Complex subsystem ke liye **ek unified simple interface**.

```java
@Service
public class OrderFacade {
    public OrderResult placeOrder(OrderRequest req) {
        inventory.reserve(req.getItems());
        payment.charge(req.getCustomer(), req.getTotal());
        shipping.create(req.getAddress(), req.getItems());
        notification.send(req.getCustomer());
        return new OrderResult(...);
    }
}
```

→ Caller ko inventory/payment/shipping internals ka pata nahi.

### Difference:

| | Template Method | Facade |
|--|----------------|--------|
| Goal | Algorithm skeleton + hooks | Subsystem simplify |
| Mechanism | Inheritance / abstract methods | Composition / delegation |
| Spring Example | `JdbcTemplate` | `JpaRepository`, `RestTemplate` |

**Short Interview Answer:**

Template Method algorithm ka fixed skeleton deta hai with overridable steps (JdbcTemplate). Facade complex subsystem ko ek simple interface ke peeche chupata hai (OrderFacade, JpaRepository).

---

## Q10: Spring mein kaunse important design patterns idiomatic hain?

**Answer:**

Spring apps mein ye patterns standard/idiomatic hain:

| Pattern | Spring Mein | Purpose |
|---------|-------------|---------|
| **Repository** | `JpaRepository` | Data access abstraction (DDD) |
| **Service Layer** | `@Service` | Business logic centralize |
| **DTO** | Request/Response DTOs | Entity aur API decouple |
| **Front Controller** | `DispatcherServlet` | Single entry point for requests |
| **Dependency Injection** | `@Autowired`, constructor | Loose coupling, testability |
| **Proxy** | AOP, `@Transactional` | Cross-cutting concerns |
| **Factory** | `@Bean`, `BeanFactory` | Object creation |
| **Singleton** | Default bean scope | Shared instances |
| **Template Method** | `JdbcTemplate` | Boilerplate removal |
| **Specification** | `JpaSpecificationExecutor` | Dynamic queries |

### Layered Architecture (Sab Milaakar):

```
Client → [Front Controller: DispatcherServlet]
              → [Controller: @RestController]  (DTOs)
              → [Service: @Service]
              → [Repository: JpaRepository]
              → [Database]
```

### DTO Pattern Example:

```java
// Entity DB ke liye, DTO API ke liye
public record UserResponse(Long id, String name, String email) {}

@GetMapping("/{id}")
public UserResponse getUser(@PathVariable Long id) {
    User user = userService.findById(id);
    return new UserResponse(user.getId(), user.getName(), user.getEmail());
}
```

**Important:**

Spring apps ka backbone hi patterns hai — DI (loose coupling), Repository (data access), Service Layer (business logic), DTO (API/entity separation), aur Proxy (AOP). Inhe samajhna Spring internals samajhne mein madad karta hai.

**Short Interview Answer:**

Spring idiomatic patterns: DI, Repository, Service Layer, DTO, Front Controller (DispatcherServlet), Proxy (AOP/`@Transactional`), Factory (`@Bean`), Singleton (default scope), Template Method (`JdbcTemplate`). Ye layered architecture banate hain with loose coupling.

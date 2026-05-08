# Structural Design Patterns

## Status: Not Started

---

## Table of Contents

1. [Structural Patterns Kya Hai?](#structural-patterns-kya-hai)
2. [Adapter Pattern](#adapter-pattern)
3. [Decorator Pattern](#decorator-pattern)
4. [Facade Pattern](#facade-pattern)
5. [Proxy Pattern](#proxy-pattern)
6. [Composite Pattern](#composite-pattern)
7. [Bridge Pattern](#bridge-pattern)
8. [Flyweight (Bonus)](#flyweight-bonus)
9. [Comparison Matrix](#comparison-matrix)
10. [Common Pitfalls](#common-pitfalls)
11. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Structural Patterns Kya Hai?

**Matlab:** Patterns jo classes/objects ko **compose** karke larger structures banaate hain — flexibility + efficient relationships.

### Quick Mapping

| Pattern | Composition Goal |
|---------|------------------|
| **Adapter** | Make incompatible interface compatible |
| **Decorator** | Add behavior without subclassing |
| **Facade** | Simplify a complex subsystem |
| **Proxy** | Control / intercept access |
| **Composite** | Treat single + group uniformly |
| **Bridge** | Decouple abstraction from implementation |
| **Flyweight** | Share fine-grained objects |

---

## Adapter Pattern

**Matlab:** **Wrap** an existing class so it matches the interface your client expects.

### Real-World Analogy

```
US plug ⟶ [Adapter] ⟶ EU socket
```

→ Same device, different interface — adapter bridges.

### Use Cases

- **Legacy API integration** (old library, new interface)
- **Third-party API → your domain interface**
- **Standardize multiple SDKs** behind one interface

### Example — Legacy Logger

```java
// Old library
public class LegacyLogger {
    public void writeLog(String tag, String message, int severity) { ... }
}

// New interface our app uses
public interface AppLogger {
    void info(String msg);
    void error(String msg, Throwable t);
}

// Adapter
public class LegacyLoggerAdapter implements AppLogger {
    private final LegacyLogger legacy;
    
    public LegacyLoggerAdapter(LegacyLogger legacy) {
        this.legacy = legacy;
    }
    
    @Override
    public void info(String msg) {
        legacy.writeLog("APP", msg, 1);
    }
    
    @Override
    public void error(String msg, Throwable t) {
        legacy.writeLog("APP", msg + " — " + t.getMessage(), 5);
    }
}

// Client uses clean interface
AppLogger logger = new LegacyLoggerAdapter(new LegacyLogger());
logger.info("Started");
```

### Two Forms

#### Object Adapter (Composition — Preferred)

```java
class Adapter implements Target {
    private Adaptee adaptee;
    public void method() { adaptee.differentMethod(); }
}
```

#### Class Adapter (Inheritance — Less Flexible)

```java
class Adapter extends Adaptee implements Target {
    public void method() { differentMethod(); }
}
```

→ Composition wins (Java doesn't allow multiple inheritance anyway).

### Real-World Examples

```java
// Arrays ↔ List
List<Integer> list = Arrays.asList(1, 2, 3);    // Arrays.asList = adapter

// InputStream → Reader
InputStreamReader reader = new InputStreamReader(inputStream);

// Spring's HandlerAdapter
// Adapts different controller types (annotated, functional, etc.) to a common DispatcherServlet interface
```

### Multi-Provider Adapter Pattern

```java
public interface PaymentGateway {
    PaymentResult charge(double amount, String token);
}

public class StripeAdapter implements PaymentGateway {
    private final StripeClient stripe;
    public PaymentResult charge(double amount, String token) {
        Charge c = stripe.charges().create(...);
        return new PaymentResult(c.getId(), c.getStatus());
    }
}

public class RazorpayAdapter implements PaymentGateway {
    private final RazorpayClient razorpay;
    public PaymentResult charge(double amount, String token) {
        Payment p = razorpay.payments().create(...);
        return new PaymentResult(p.getId(), p.getStatus());
    }
}

// App always uses PaymentGateway, not vendor SDK
```

→ Cross-ref: Strategy pattern (next file) for runtime selection.

---

## Decorator Pattern

**Matlab:** **Add behavior** to an object **dynamically** without modifying its class. Wrap, wrap, wrap.

### vs Inheritance

```java
// ❌ Inheritance explosion
class CompressedEncryptedBufferedFile extends File { ... }
class EncryptedBufferedFile extends File { ... }
class CompressedFile extends File { ... }
class BufferedFile extends File { ... }
// Combinatorial explosion!

// ✅ Decorators — combine flexibly
File file = new CompressedFile(new EncryptedFile(new BufferedFile(rawFile)));
```

### Classic Java Example — InputStream Wrappers

```java
InputStream raw = new FileInputStream("data.bin");
InputStream buffered = new BufferedInputStream(raw);
InputStream gzipped = new GZIPInputStream(buffered);
DataInputStream data = new DataInputStream(gzipped);

int value = data.readInt();
// Each layer adds behavior — buffering, decompression, primitive read
```

### Implementation

```java
public interface Notifier {
    void send(String message);
}

public class EmailNotifier implements Notifier {
    public void send(String message) { System.out.println("Email: " + message); }
}

// Decorator base
public abstract class NotifierDecorator implements Notifier {
    protected final Notifier wrapped;
    public NotifierDecorator(Notifier wrapped) { this.wrapped = wrapped; }
}

// Concrete decorators
public class SmsDecorator extends NotifierDecorator {
    public SmsDecorator(Notifier n) { super(n); }
    public void send(String message) {
        wrapped.send(message);
        System.out.println("SMS: " + message);
    }
}

public class SlackDecorator extends NotifierDecorator {
    public SlackDecorator(Notifier n) { super(n); }
    public void send(String message) {
        wrapped.send(message);
        System.out.println("Slack: " + message);
    }
}

// Usage — choose channels at runtime
Notifier n = new SlackDecorator(new SmsDecorator(new EmailNotifier()));
n.send("System alert!");
// Email, SMS, Slack all triggered
```

### Spring Examples

#### `BufferedHttpServletRequestWrapper`

Wraps `HttpServletRequest` to allow re-reading body multiple times.

#### Spring AOP

```java
@Component
@Cacheable("users")
public class UserService { ... }
```

→ Spring wraps `UserService` with caching decorator at runtime.

### When to Use?

✅ Add features to objects dynamically
✅ Want combinations without subclass explosion
✅ Add cross-cutting concerns (logging, caching, security)

❌ Need to know wrapped object's exact type
❌ Performance-critical hot path (each layer = method call overhead)

---

## Facade Pattern

**Matlab:** **One unified interface** for a complex subsystem — hide complexity, simplify usage.

### Use Case

```
Without facade:
  Client → DB Connection
       → Connection Pool config
       → Transaction Manager
       → Query Builder
       → Result Mapper
       → Cache layer
       → Audit logger

With facade:
  Client → UserService.findById(id)
              ↓ (handles all the above internally)
```

### Example — Order Processing Facade

```java
// Subsystems
public class InventoryService { ... }
public class PaymentService { ... }
public class ShippingService { ... }
public class NotificationService { ... }

// Facade
@Service
public class OrderFacade {
    
    private final InventoryService inventory;
    private final PaymentService payment;
    private final ShippingService shipping;
    private final NotificationService notification;
    
    public OrderResult placeOrder(OrderRequest req) {
        if (!inventory.reserve(req.getItems())) {
            throw new OutOfStockException();
        }
        
        PaymentResult p = payment.charge(req.getCustomer(), req.getTotal());
        if (!p.isSuccess()) {
            inventory.release(req.getItems());
            throw new PaymentFailedException(p.getReason());
        }
        
        Shipment s = shipping.create(req.getAddress(), req.getItems());
        notification.send(req.getCustomer(), s);
        
        return new OrderResult(s.getTrackingId());
    }
}

// Client
OrderResult result = orderFacade.placeOrder(request);
```

→ Caller doesn't know about inventory/payment/shipping internals.

### Spring Data Repositories — Facade Over JPA

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}

// Hides EntityManager, criteria queries, transactions, caching
userRepo.save(user);
userRepo.findByEmail("a@b.com");
```

→ `JpaRepository` is a facade over JPA's complex API.

### `RestTemplate` / `WebClient` — Facade Over HTTP

```java
String response = restTemplate.getForObject("https://api.example.com", String.class);
// Hides connection mgmt, headers, parsing, error handling
```

### Difference vs Adapter

| | Facade | Adapter |
|--|--------|---------|
| Goal | Simplify | Make compatible |
| Wraps | Multiple subsystems | One incompatible interface |
| Adds | Higher-level interface | Specific interface translation |

---

## Proxy Pattern

**Matlab:** A **stand-in object** that controls access to another. Same interface; intercepts calls.

### Types of Proxies

| Type | Purpose |
|------|---------|
| **Virtual Proxy** | Lazy initialization |
| **Protection Proxy** | Access control (auth) |
| **Remote Proxy** | Hide network call (RPC) |
| **Logging Proxy** | Audit calls |
| **Caching Proxy** | Cache results |
| **Smart Reference** | Reference counting, locking |

### Static Proxy

```java
public interface UserService {
    User findById(Long id);
}

public class UserServiceImpl implements UserService { ... }

public class LoggingUserServiceProxy implements UserService {
    private final UserService delegate;
    
    public LoggingUserServiceProxy(UserService d) { this.delegate = d; }
    
    @Override
    public User findById(Long id) {
        log.info("findById({}) called", id);
        long start = System.nanoTime();
        try {
            return delegate.findById(id);
        } finally {
            log.info("findById took {}ns", System.nanoTime() - start);
        }
    }
}
```

### JDK Dynamic Proxy

```java
import java.lang.reflect.*;

UserService proxy = (UserService) Proxy.newProxyInstance(
    UserService.class.getClassLoader(),
    new Class<?>[]{UserService.class},
    (proxy_, method, args) -> {
        log.info("Calling {}", method.getName());
        long start = System.nanoTime();
        Object result = method.invoke(realService, args);
        log.info("{} took {}ns", method.getName(), System.nanoTime() - start);
        return result;
    });
```

→ Works for **interfaces only**.

### CGLIB Proxy

```java
Enhancer enhancer = new Enhancer();
enhancer.setSuperclass(UserServiceImpl.class);
enhancer.setCallback((MethodInterceptor) (obj, method, args, methodProxy) -> {
    log.info("Calling {}", method.getName());
    return methodProxy.invokeSuper(obj, args);
});
UserServiceImpl proxy = (UserServiceImpl) enhancer.create();
```

→ Works for **classes** (subclasses them).

### Spring AOP — Built-In Proxy

```java
@Component
public class UserService {
    @Transactional         // Spring wraps with transactional proxy
    @Cacheable("users")    // and caching proxy
    public User findById(Long id) { ... }
}
```

→ `@EnableTransactionManagement`, `@EnableCaching` enable proxy creation.

### How Spring AOP Works

```
Client ──▶ UserService Proxy ──▶ Real UserService
              │
              ├─ @Around (check tx, start tx)
              ├─ @Around (check cache, return cached)
              └─ Real method
              ├─ @Around (commit tx)
              └─ @Around (cache result)
```

### Common Pitfall — Self-Invocation

```java
@Service
public class MyService {
    @Transactional
    public void outer() {
        inner();   // ❌ NOT transactional — bypasses proxy
    }
    
    @Transactional
    public void inner() { ... }
}
```

→ Spring proxy intercepts **external** calls. Internal `this.inner()` skips it.

**Fix:** Use injected self-reference, or refactor (separate class).

### When to Use?

✅ Lazy loading (Hibernate proxies)
✅ Caching, logging, transactions, security (cross-cutting)
✅ Remote service calls (RMI, gRPC stubs)
✅ Smart references (reference counting)

---

## Composite Pattern

**Matlab:** Treat **single objects + groups** uniformly via common interface. Tree structures.

### Use Cases

- File system (file vs folder)
- Menu system (menu item vs sub-menu)
- HTML / XML / DOM
- Org chart (employee vs manager-with-team)
- Math expressions (number vs operation)

### Implementation

```java
public interface FileSystemNode {
    String getName();
    long getSize();
    void print(String indent);
}

public class File implements FileSystemNode {
    private final String name;
    private final long size;
    
    public File(String name, long size) {
        this.name = name;
        this.size = size;
    }
    
    public String getName() { return name; }
    public long getSize() { return size; }
    public void print(String indent) {
        System.out.println(indent + name + " (" + size + ")");
    }
}

public class Folder implements FileSystemNode {
    private final String name;
    private final List<FileSystemNode> children = new ArrayList<>();
    
    public Folder(String name) { this.name = name; }
    
    public void add(FileSystemNode child) { children.add(child); }
    
    public String getName() { return name; }
    
    public long getSize() {
        return children.stream().mapToLong(FileSystemNode::getSize).sum();
    }
    
    public void print(String indent) {
        System.out.println(indent + name + "/");
        children.forEach(c -> c.print(indent + "  "));
    }
}

// Usage
Folder root = new Folder("root");
Folder docs = new Folder("docs");
docs.add(new File("readme.md", 1024));
docs.add(new File("notes.txt", 512));
root.add(docs);
root.add(new File("config.yml", 256));

root.print("");
System.out.println("Total: " + root.getSize());
```

### Output

```
root/
  docs/
    readme.md (1024)
    notes.txt (512)
  config.yml (256)
Total: 1792
```

### Real-World

- **Spring Security**: filter chain (composite of filters)
- **Swing/AWT**: containers + components
- **JSF / React VDOM**: tree of components

### When to Use?

✅ Hierarchical structures
✅ Want to treat individual + composite uniformly
✅ Recursive operations on trees

---

## Bridge Pattern

**Matlab:** **Decouple** abstraction from implementation so both can vary independently.

### Problem

Without Bridge: cartesian explosion.

```
Shape × Color = Class explosion
  RedCircle, BlueCircle, GreenCircle
  RedSquare, BlueSquare, GreenSquare
  RedTriangle, ...
```

### ✅ Bridge Solution

```java
// Implementation hierarchy
public interface Color {
    void apply();
}

public class Red implements Color {
    public void apply() { System.out.println("Red"); }
}

public class Blue implements Color {
    public void apply() { System.out.println("Blue"); }
}

// Abstraction hierarchy
public abstract class Shape {
    protected final Color color;          // bridge
    protected Shape(Color color) { this.color = color; }
    public abstract void draw();
}

public class Circle extends Shape {
    public Circle(Color c) { super(c); }
    public void draw() {
        System.out.print("Circle: ");
        color.apply();
    }
}

public class Square extends Shape {
    public Square(Color c) { super(c); }
    public void draw() {
        System.out.print("Square: ");
        color.apply();
    }
}

// Usage — combine independently
new Circle(new Red()).draw();      // Circle: Red
new Square(new Blue()).draw();     // Square: Blue
new Circle(new Blue()).draw();     // Circle: Blue
```

### vs Adapter

| | Bridge | Adapter |
|--|--------|---------|
| Designed | Up-front for variation | After-the-fact (legacy) |
| Goal | Decouple two hierarchies | Make incompatible compatible |

### Real-World Examples

#### JDBC

```
Abstraction: Connection, Statement (java.sql)
Implementation: Driver (mysql, postgres, oracle JDBC drivers)

Same Connection API works with any driver — bridge.
```

#### Logger Facades

```
Abstraction: SLF4J Logger
Implementation: Logback, Log4j2, java.util.logging
```

→ Code uses SLF4J; runtime swaps implementation.

#### Spring Data

```
Abstraction: JpaRepository, MongoRepository
Implementation: Hibernate, MongoDB driver
```

---

## Flyweight (Bonus)

**Matlab:** Share fine-grained objects to save memory. Separate **intrinsic** (shared) state from **extrinsic** (passed at use time).

### Use Cases

- Game characters (10K bullets sharing same texture)
- Document editors (each character object)
- String pool (`String.intern()`)
- `Integer` cache (-128 to 127)

### Example — String Pool

```java
String s1 = "hello";
String s2 = "hello";
System.out.println(s1 == s2);   // true — same intern pool object
```

### Spring Pattern

Spring beans (singleton scope) are flyweights — one instance shared.

---

## Comparison Matrix

| Pattern | Use Case | Java Example | Spring Example |
|---------|----------|--------------|----------------|
| **Adapter** | Incompatible interface | `Arrays.asList`, `InputStreamReader` | `HandlerAdapter` |
| **Decorator** | Add behavior | `BufferedInputStream`, `GZIPInputStream` | AOP wrappers |
| **Facade** | Simplify subsystem | JDBC `Connection` | `JpaRepository`, `RestTemplate` |
| **Proxy** | Control access | `Proxy.newProxyInstance` | AOP, `@Transactional` |
| **Composite** | Tree structures | DOM, Swing components | Filter chain |
| **Bridge** | Decouple two hierarchies | JDBC API + drivers | Spring Data |
| **Flyweight** | Share fine-grained | String pool, Integer cache | Singleton beans |

---

## Common Pitfalls

### 1. Adapter Doing Too Much

If adapter has business logic + many fields → it's not an adapter, it's a translator service.

### 2. Decorator + Concrete Type Check

```java
if (notifier instanceof EmailNotifier) ...   // breaks decorator chain!
```

→ Decorators hide concrete type intentionally.

### 3. Decorator Performance

Each layer = method call. Hot loops with 5 decorators slow.

### 4. Facade as God Object

If facade grows to 50+ methods → it's not simplifying anymore. Split.

### 5. Proxy Self-Invocation

`this.method()` skips Spring proxy → `@Transactional`, `@Cacheable` ignored.

### 6. JDK Proxy Only Works for Interfaces

Use CGLIB (`proxyTargetClass=true`) for class proxies.

### 7. Composite Without Common Interface

If only `Folder.print()` exists, not on `File` → can't treat uniformly.

### 8. Bridge When Adapter Suffices

Bridge is up-front design choice; adapter retrofits. Don't over-engineer.

### 9. Flyweight + Mutable Shared State

```java
flyweight.setX(5);   // affects ALL users of this flyweight!
```

→ Flyweights must be immutable (intrinsic state) or pass extrinsic externally.

### 10. Confusing Proxy with Decorator

```
Proxy: control access (lifecycle, security, location)
Decorator: add behavior (always works, just wraps)
```

→ Proxy can refuse / replace; decorator augments.

### 11. Adapter Hiding Performance Issues

Wrapping every method = overhead. For hot paths, prefer direct API.

### 12. Spring AOP Misuse

Adding `@Aspect` for trivial logging when SLF4J + line of code suffices.

---

## Summary Cheat Sheet

| Pattern | One-Liner | When |
|---------|-----------|------|
| **Adapter** | Wrap to fit | Legacy / 3rd party API |
| **Decorator** | Wrap to add | Optional behaviors |
| **Facade** | Wrap to simplify | Complex subsystem |
| **Proxy** | Wrap to control | Cross-cutting concerns |
| **Composite** | Tree of same type | Hierarchical data |
| **Bridge** | Two-axis variation | Independent hierarchies |
| **Flyweight** | Share to save memory | Many similar objects |

| ✅ Do | ❌ Don't |
|-------|---------|
| Adapter for legacy / vendor | Adapter for everything |
| Decorator for combos | Subclass explosion |
| Facade for libraries | Facade for 2-method API |
| Spring proxy via annotations | Hand-rolled proxy code |
| Composite for trees | Composite for flat lists |
| Bridge for true variation | Bridge for premature flexibility |
| Flyweight for memory issues | Premature optimization |

---

## Practice

1. Write Adapter for two payment SDKs (Stripe, Razorpay) behind common `PaymentGateway`.
2. Implement Decorator for `Notifier` (Email + SMS + Slack); combine flexibly.
3. Build Facade `OrderService` that orchestrates inventory + payment + shipping.
4. Implement JDK Dynamic Proxy with logging interceptor.
5. Implement CGLIB Proxy for a class without interface.
6. Test Spring AOP self-invocation pitfall — verify `@Transactional` skipped.
7. Build Composite for file system; calculate total size recursively.
8. Implement Bridge for `Shape` × `Color` (Circle/Square × Red/Blue/Green).
9. Identify decorators in `java.io.*` (count layers in real apps).
10. Identify proxies in Spring source code (`AopProxy`, `JdkDynamicAopProxy`).

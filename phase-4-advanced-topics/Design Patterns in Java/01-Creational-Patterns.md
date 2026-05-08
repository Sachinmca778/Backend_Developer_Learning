# Creational Design Patterns

## Status: Not Started

---

## Table of Contents

1. [Creational Patterns Kya Hai?](#creational-patterns-kya-hai)
2. [Singleton Pattern](#singleton-pattern)
3. [Builder Pattern](#builder-pattern)
4. [Factory Method Pattern](#factory-method-pattern)
5. [Abstract Factory Pattern](#abstract-factory-pattern)
6. [Prototype Pattern](#prototype-pattern)
7. [Object Pool (Bonus)](#object-pool-bonus)
8. [Comparison Matrix](#comparison-matrix)
9. [Common Pitfalls](#common-pitfalls)
10. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Creational Patterns Kya Hai?

**Matlab:** Patterns jo **object creation** ko abstract karte hain — caller ko `new` keyword ka pata nahi hota, kab/kaise/kahan object banta hai.

### Why Use?

| Without | With Pattern |
|---------|--------------|
| `new BigComplexThing(a, b, c, d, e, f)` | `Builder.with(a).b(b).build()` |
| `new ServiceImpl()` everywhere | `Factory.create("type")` |
| Multiple instances of "global" thing | Singleton |
| Slow, expensive object creation | Prototype (clone) |
| Family of related objects | Abstract Factory |

### Spring's Take

Spring **already implements many** of these for you:
- **Singleton**: default bean scope
- **Prototype**: `@Scope("prototype")`
- **Factory Method**: `@Bean` methods
- **Abstract Factory**: `FactoryBean<T>`
- **Builder**: Lombok `@Builder` + Spring Data fluent APIs

→ Knowing patterns helps you understand Spring's internals.

---

## Singleton Pattern

**Matlab:** Class ka **only one instance** ho aur global access mile.

### Use Cases

- **Configuration manager** (one app-wide config)
- **Cache** (shared across requests)
- **Connection pool** (single pool instance)
- **Logger** (one logger instance)
- **Spring beans** (default scope)

### ❌ Naive (Not Thread-Safe)

```java
public class Config {
    private static Config instance;
    
    private Config() {}
    
    public static Config getInstance() {
        if (instance == null) {
            instance = new Config();    // race condition!
        }
        return instance;
    }
}
```

### ❌ Synchronized Method (Slow)

```java
public static synchronized Config getInstance() {
    if (instance == null) {
        instance = new Config();
    }
    return instance;
}
```

→ Thread-safe but locks every call (even after init).

### ✅ Double-Checked Locking (DCL)

```java
public class Config {
    private static volatile Config instance;
    
    private Config() {}
    
    public static Config getInstance() {
        if (instance == null) {                       // 1st check (no lock)
            synchronized (Config.class) {
                if (instance == null) {                // 2nd check (with lock)
                    instance = new Config();
                }
            }
        }
        return instance;
    }
}
```

⚠️ `volatile` is **mandatory** — prevents instruction reordering. Without it, another thread might see partially-constructed object.

### ✅ Bill Pugh / Initialization-on-Demand Holder

```java
public class Config {
    private Config() {}
    
    private static class Holder {
        private static final Config INSTANCE = new Config();
    }
    
    public static Config getInstance() {
        return Holder.INSTANCE;
    }
}
```

→ JVM guarantees class initialization is thread-safe + lazy. **Cleanest solution.**

### ✅✅ Enum Singleton (Joshua Bloch's Recommendation)

```java
public enum Config {
    INSTANCE;
    
    private final Map<String, String> settings = new HashMap<>();
    
    public String get(String key) { return settings.get(key); }
    public void set(String key, String value) { settings.put(key, value); }
}

// Usage
Config.INSTANCE.set("theme", "dark");
String theme = Config.INSTANCE.get("theme");
```

✅ Thread-safe by JVM
✅ Serialization-safe (no duplicate instances on deserialization)
✅ Reflection-safe (can't construct enum via reflection)
✅ Concise

❌ Can't extend (enums are final)
❌ Eager init (loaded with class)

### Spring Singleton (Default)

```java
@Component   // singleton scope by default
public class UserService { ... }
```

→ Spring guarantees one instance **per ApplicationContext**.

```java
@Configuration
public class AppConfig {
    @Bean
    public DataSource dataSource() {
        return new HikariDataSource(...);
    }
    // single instance returned every time @Autowired
}
```

### Comparison Table

| Approach | Thread Safe | Lazy | Reflection Safe | Serialization Safe |
|----------|------------|------|-----------------|-------------------|
| Naive | ❌ | ✅ | ❌ | ❌ |
| Synchronized | ✅ | ✅ | ❌ | ❌ |
| Double-Checked Locking | ✅ | ✅ | ❌ | ❌ |
| Bill Pugh Holder | ✅ | ✅ | ❌ | ❌ |
| **Enum** | ✅ | ❌ (eager) | ✅ | ✅ |

### When to Use Singleton?

✅ Truly global state (config, logger)
✅ Resource pools (DB, threads)
❌ Just to avoid passing object (= global variable)
❌ For testability (singletons hard to mock)

→ **Prefer dependency injection** over manual singletons. Spring does the singleton management for you.

---

## Builder Pattern

**Matlab:** **Step-by-step object construction** with optional parameters; final object is **immutable**.

### Problem It Solves — Telescoping Constructor

```java
// ❌ Constructor explosion
new Pizza("Large");
new Pizza("Large", true);
new Pizza("Large", true, true);
new Pizza("Large", true, true, false);
new Pizza("Large", true, true, false, "Thin");
new Pizza("Large", true, true, false, "Thin", List.of("mushroom", "olives"));
// Caller can't tell what booleans mean!
```

### ✅ Builder Solution

```java
public class Pizza {
    private final String size;
    private final boolean cheese;
    private final boolean pepperoni;
    private final boolean mushroom;
    private final String crust;
    private final List<String> toppings;
    
    private Pizza(Builder b) {
        this.size = b.size;
        this.cheese = b.cheese;
        this.pepperoni = b.pepperoni;
        this.mushroom = b.mushroom;
        this.crust = b.crust;
        this.toppings = b.toppings;
    }
    
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private String size;
        private boolean cheese;
        private boolean pepperoni;
        private boolean mushroom;
        private String crust = "Regular";   // default
        private List<String> toppings = new ArrayList<>();
        
        public Builder size(String s) { this.size = s; return this; }
        public Builder cheese(boolean c) { this.cheese = c; return this; }
        public Builder pepperoni(boolean p) { this.pepperoni = p; return this; }
        public Builder mushroom(boolean m) { this.mushroom = m; return this; }
        public Builder crust(String c) { this.crust = c; return this; }
        public Builder topping(String t) { this.toppings.add(t); return this; }
        
        public Pizza build() {
            if (size == null) throw new IllegalStateException("size required");
            return new Pizza(this);
        }
    }
}

// Usage — readable!
Pizza pizza = Pizza.builder()
    .size("Large")
    .cheese(true)
    .crust("Thin")
    .topping("mushroom")
    .topping("olives")
    .build();
```

### ✅ Lombok @Builder (Production)

```java
@Builder
@Getter
@ToString
public class Pizza {
    private String size;
    @Builder.Default private boolean cheese = true;
    private boolean pepperoni;
    @Builder.Default private String crust = "Regular";
    @Singular private List<String> toppings;   // adds .topping() singular method
}

// Generated automatically
Pizza pizza = Pizza.builder()
    .size("Large")
    .pepperoni(true)
    .topping("mushroom")
    .topping("olives")
    .build();
```

### Variants

#### Director (GoF Original)

```java
public class PizzaDirector {
    public static Pizza margherita(Pizza.Builder b) {
        return b.size("Medium").cheese(true).topping("basil").build();
    }
}

Pizza p = PizzaDirector.margherita(Pizza.builder());
```

#### Step Builder (Force Order)

```java
Pizza.SizeStep().size("Large")        // forced first
    .cheese(true).build();
```

→ Compile-time enforcement of mandatory params.

### Real-World Examples

```java
// Spring REST clients
RestTemplate restTemplate = new RestTemplateBuilder()
    .rootUri("https://api.example.com")
    .basicAuthentication("user", "pass")
    .build();

// HTTP Client (JDK 11+)
HttpRequest req = HttpRequest.newBuilder()
    .uri(URI.create("https://example.com"))
    .header("Accept", "application/json")
    .GET()
    .build();

// StringBuilder (kind of a builder)
String s = new StringBuilder().append("Hello, ").append("World!").toString();
```

### When to Use?

✅ 4+ constructor parameters
✅ Many optional fields
✅ Want immutable objects
✅ Need readable construction

❌ Simple objects with 1-2 fields
❌ Mutable state needed

---

## Factory Method Pattern

**Matlab:** Define a method that **decides which concrete class to instantiate** — defer instantiation to subclasses or strategy.

### Problem

```java
// ❌ Caller couples to concrete class
PaymentProcessor p;
if (type.equals("CARD")) p = new CardProcessor();
else if (type.equals("UPI")) p = new UpiProcessor();
else if (type.equals("WALLET")) p = new WalletProcessor();
```

### ✅ Factory Method

```java
public interface PaymentProcessor {
    void process(double amount);
}

public class CardProcessor implements PaymentProcessor { ... }
public class UpiProcessor implements PaymentProcessor { ... }
public class WalletProcessor implements PaymentProcessor { ... }

public class PaymentProcessorFactory {
    public static PaymentProcessor create(String type) {
        return switch (type) {
            case "CARD" -> new CardProcessor();
            case "UPI" -> new UpiProcessor();
            case "WALLET" -> new WalletProcessor();
            default -> throw new IllegalArgumentException("Unknown: " + type);
        };
    }
}

// Caller decoupled
PaymentProcessor p = PaymentProcessorFactory.create("CARD");
p.process(100);
```

### Spring's Take — `@Bean` Factory Method

```java
@Configuration
public class PaymentConfig {
    
    @Bean
    public PaymentProcessor paymentProcessor(@Value("${payment.type}") String type) {
        return switch (type) {
            case "CARD" -> new CardProcessor();
            case "UPI" -> new UpiProcessor();
            default -> throw new IllegalArgumentException(type);
        };
    }
}
```

→ `@Bean` methods are factory methods! Spring calls them once + caches result.

### Polymorphic Factory (No Switch — Map Lookup)

```java
@Component
public class PaymentProcessorRegistry {
    
    private final Map<String, PaymentProcessor> processors;
    
    public PaymentProcessorRegistry(List<PaymentProcessor> all) {
        this.processors = all.stream()
            .collect(Collectors.toMap(
                p -> p.getClass().getSimpleName().replace("Processor", "").toUpperCase(),
                p -> p));
    }
    
    public PaymentProcessor get(String type) {
        return Optional.ofNullable(processors.get(type))
            .orElseThrow(() -> new IllegalArgumentException(type));
    }
}
```

→ Spring auto-injects all `PaymentProcessor` implementations. Adding new type = just create class.

### `FactoryBean<T>` (Spring's Mechanism)

For complex bean creation:

```java
public class JpaSessionFactoryBean implements FactoryBean<SessionFactory> {
    @Override
    public SessionFactory getObject() throws Exception {
        return new Configuration()
            .configure()
            .buildSessionFactory();
    }
    
    @Override
    public Class<?> getObjectType() { return SessionFactory.class; }
}

// Registered as bean — Spring calls getObject() to produce SessionFactory
@Bean public JpaSessionFactoryBean sessionFactoryBean() { return new JpaSessionFactoryBean(); }
```

→ Used internally by Spring (e.g., `LocalContainerEntityManagerFactoryBean`).

### When to Use?

✅ Object creation logic complex (config, env-specific)
✅ Want to hide concrete classes
✅ Want central place for instantiation

---

## Abstract Factory Pattern

**Matlab:** Factory **of factories** — create families of related objects.

### Use Case

```
Theme: Light vs Dark
  - Button (light vs dark style)
  - Window (light vs dark style)
  - Panel (light vs dark style)

You need ALL "light" or ALL "dark" — never mixed.
```

### Implementation

```java
public interface Button { void render(); }
public interface Window { void render(); }

public class LightButton implements Button {
    public void render() { System.out.println("Light button"); }
}
public class DarkButton implements Button {
    public void render() { System.out.println("Dark button"); }
}
public class LightWindow implements Window {
    public void render() { System.out.println("Light window"); }
}
public class DarkWindow implements Window {
    public void render() { System.out.println("Dark window"); }
}

public interface UIFactory {
    Button createButton();
    Window createWindow();
}

public class LightThemeFactory implements UIFactory {
    public Button createButton() { return new LightButton(); }
    public Window createWindow() { return new LightWindow(); }
}

public class DarkThemeFactory implements UIFactory {
    public Button createButton() { return new DarkButton(); }
    public Window createWindow() { return new DarkWindow(); }
}

// Client uses ONE factory — guarantees consistency
public class App {
    private final UIFactory factory;
    
    public App(UIFactory factory) { this.factory = factory; }
    
    public void render() {
        factory.createButton().render();
        factory.createWindow().render();
        // All from same family
    }
}

App app = new App(new DarkThemeFactory());
app.render();
```

### Real-World Examples

#### JDBC

```java
DataSource ds = ...;
Connection con = ds.getConnection();           // factory method
Statement stmt = con.createStatement();         // factory method
ResultSet rs = stmt.executeQuery("SELECT 1");   // factory method
```

→ `Connection` is **abstract factory** — produces family of related JDBC objects (Statement, PreparedStatement, etc.).

#### JAXB

```java
JAXBContext ctx = JAXBContext.newInstance(MyClass.class);
Marshaller m = ctx.createMarshaller();
Unmarshaller um = ctx.createUnmarshaller();
```

#### Spring's `BeanFactory`

```java
ApplicationContext ctx = new ClassPathXmlApplicationContext(...);
BeanA a = ctx.getBean(BeanA.class);   // factory method
BeanB b = ctx.getBean(BeanB.class);   // factory method
```

### Factory Method vs Abstract Factory

| | Factory Method | Abstract Factory |
|--|---------------|------------------|
| Creates | Single product | Family of products |
| Mechanism | Subclass overrides method | Composition (factory object) |
| Example | `getInstance(type)` | `Connection` → Statement, ResultSet |

---

## Prototype Pattern

**Matlab:** Create new object by **cloning an existing one**, instead of building from scratch.

### When?

- Object creation is **expensive** (heavy I/O, complex calc)
- Need many **similar** objects with small variations

### Implementation

```java
public class Document implements Cloneable {
    private String title;
    private String content;
    private List<String> tags;
    
    @Override
    public Document clone() {
        try {
            Document copy = (Document) super.clone();
            copy.tags = new ArrayList<>(this.tags);   // deep copy mutable fields
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}

Document template = new Document("Template", "...", List.of("legal"));
Document copy1 = template.clone();
copy1.setTitle("Copy 1");
```

### ❌ Java Cloneable Pitfalls

- Shallow copy by default (mutable fields shared!)
- `Cloneable` is a marker interface (no `clone()` method to override properly)
- Can't clone via interface
- Joshua Bloch advises: avoid `Cloneable`; use copy constructor / static factory

### ✅ Copy Constructor (Modern Approach)

```java
public class Document {
    private String title;
    private String content;
    private List<String> tags;
    
    public Document(Document other) {
        this.title = other.title;
        this.content = other.content;
        this.tags = new ArrayList<>(other.tags);
    }
}

Document copy = new Document(original);
```

### ✅ Static Factory Method

```java
public class Document {
    public static Document copyOf(Document other) {
        return new Document(other);
    }
}
```

### Spring Prototype Scope

```java
@Component
@Scope("prototype")    // new instance every getBean / @Autowired
public class TaskRunner { ... }
```

```java
@Autowired
private ObjectFactory<TaskRunner> taskRunnerFactory;

TaskRunner r = taskRunnerFactory.getObject();   // new instance
```

→ Spring creates fresh instance instead of reusing singleton.

#### When to Use Prototype Scope

✅ Stateful beans (per-request data)
✅ Test scenarios needing fresh instances
✅ Workers / runners with mutable state

❌ Stateless services (singleton fine)

### Deep Copy Strategies

For complex object graphs:
- **Manual** copy constructors
- **Apache Commons** `SerializationUtils.clone()` (slow, requires Serializable)
- **Jackson** `mapper.readValue(mapper.writeValueAsString(obj), Class)` (JSON round-trip)
- **MapStruct / ModelMapper** (codegen)

---

## Object Pool (Bonus)

**Matlab:** Reuse expensive objects instead of creating new each time.

### Use Cases

- **Database connection pools** (HikariCP, DBCP)
- **Thread pools** (`ExecutorService`)
- **HTTP connection pools** (Apache HttpClient, OkHttp)

### Example

```java
ExecutorService pool = Executors.newFixedThreadPool(10);

for (int i = 0; i < 100; i++) {
    pool.submit(() -> processTask());    // reuses 10 threads, not 100
}

pool.shutdown();
```

### Spring Doesn't Have @Pool

But you configure pool beans:

```java
@Bean
public DataSource dataSource() {
    HikariConfig cfg = new HikariConfig();
    cfg.setJdbcUrl("jdbc:postgresql://...");
    cfg.setMaximumPoolSize(20);
    return new HikariDataSource(cfg);
}
```

---

## Comparison Matrix

| Pattern | Intent | Java Example | Spring Use |
|---------|--------|--------------|------------|
| **Singleton** | One instance | `Runtime.getRuntime()` | Default bean scope |
| **Builder** | Step-by-step, immutable | `StringBuilder`, `HttpRequest.newBuilder()` | Lombok `@Builder` |
| **Factory Method** | Create polymorphic objects | `Calendar.getInstance()` | `@Bean` methods |
| **Abstract Factory** | Family of related objects | `Connection` → Statement | `BeanFactory` |
| **Prototype** | Clone existing | `Object.clone()` | `@Scope("prototype")` |
| **Object Pool** | Reuse expensive | `ExecutorService` | HikariCP DataSource |

---

## Common Pitfalls

### 1. Singleton Hides Dependencies

```java
class Service {
    void doStuff() { Config.INSTANCE.get("key"); }  // hidden global dep
}
```

→ Hard to test (can't mock Config). Prefer DI:

```java
class Service {
    private final Config config;   // explicit
    Service(Config config) { this.config = config; }
}
```

### 2. Singleton + Mutable State + Threads

Race conditions if singleton has mutable state without synchronization.

### 3. DCL Without Volatile

```java
private static Config instance;   // ❌ should be volatile
```

→ JIT can reorder instructions; another thread may see partially constructed object.

### 4. Builder Without Validation

```java
return new Pizza(this);   // no checks → invalid Pizza possible
```

→ Validate required fields in `build()`.

### 5. Lombok @Builder + Inheritance

`@Builder` on child class doesn't include parent fields. Use `@SuperBuilder` for that.

### 6. Factory Switch Statement Bloat

Every new type → modify factory. Use registry pattern (Map lookup) + Spring DI for open-closed.

### 7. Prototype + Mutable Fields = Shallow Copy Bug

```java
Document copy = original.clone();
copy.tags.add("X");   // also mutates original.tags!
```

→ Deep-copy mutable collections.

### 8. Cloneable Antipattern

`Cloneable` is broken. Use copy constructor / static factory instead.

### 9. Misusing Prototype Bean

```java
@Component
@Scope("prototype")
class StatelessHelper { ... }   // why prototype? singleton fine
```

→ Prototype only for genuinely stateful beans.

### 10. Singleton Used as God Object

```java
GlobalManager.INSTANCE.doEverything();
```

→ Anti-pattern. Singleton ≠ centralized everything.

### 11. Serialization Breaks Singleton

```java
class Config implements Serializable {
    private static Config instance = new Config();
    // serialization creates NEW instance — singleton broken!
}
```

→ Override `readResolve()`:

```java
private Object readResolve() { return instance; }
```

→ Or use enum (immune to this).

---

## Summary Cheat Sheet

| Pattern | One-Liner | When |
|---------|-----------|------|
| **Singleton** | One instance, global access | Truly global state |
| **Builder** | Step-by-step construction | Many params, immutable |
| **Factory Method** | Polymorphic creation | Hide concrete class |
| **Abstract Factory** | Factory of factories | Family of related objects |
| **Prototype** | Clone existing | Expensive creation |
| **Object Pool** | Reuse expensive | DB / Thread / HTTP |

| Spring Equivalents |
|-------------------|
| `@Component` / `@Bean` (default) → Singleton |
| Lombok `@Builder` → Builder |
| `@Bean` method → Factory Method |
| `FactoryBean<T>` → Abstract Factory mechanism |
| `@Scope("prototype")` → Prototype |
| HikariDataSource → Object Pool |

| ✅ Do | ❌ Don't |
|-------|---------|
| Use enum singleton | Naive lazy singleton |
| Validate in `build()` | Allow incomplete object |
| Map-based factory + DI | Switch statement that grows |
| Copy constructor | `Cloneable` |
| Singleton sparingly | Singleton everywhere = global state |
| Volatile in DCL | DCL without volatile |
| Spring's defaults | Reinvent the wheel |

---

## Practice

1. Implement Singleton — try all 4 thread-safe approaches; benchmark.
2. Build a `Pizza` class with manual Builder; then refactor to Lombok `@Builder`.
3. Build `PaymentProcessorFactory` with switch; refactor to Map-based registry with Spring DI.
4. Implement Abstract Factory for theme system (Light + Dark); switch at runtime.
5. Implement Prototype for `Document` with deep + shallow versions; observe bugs.
6. Use Spring `@Scope("prototype")` for a `TaskRunner`; verify each request gets new instance.
7. Try `FactoryBean<MyService>` to create a complex bean.
8. Write enum singleton for `AppConfig`; compare to DCL version.
9. Simulate object pool with `ExecutorService`; observe thread reuse via thread name.
10. Identify creational patterns in JDK / Spring source (e.g., `Calendar.getInstance`, `BeanFactory.getBean`).

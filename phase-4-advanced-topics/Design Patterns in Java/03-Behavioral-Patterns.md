# Behavioral Design Patterns

## Status: Not Started

---

## Table of Contents

1. [Behavioral Patterns Kya Hai?](#behavioral-patterns-kya-hai)
2. [Strategy Pattern](#strategy-pattern)
3. [Observer Pattern](#observer-pattern)
4. [Template Method Pattern](#template-method-pattern)
5. [Command Pattern](#command-pattern)
6. [Chain of Responsibility](#chain-of-responsibility)
7. [State Pattern](#state-pattern)
8. [Visitor Pattern](#visitor-pattern)
9. [Iterator (Bonus)](#iterator-bonus)
10. [Mediator (Bonus)](#mediator-bonus)
11. [Comparison Matrix](#comparison-matrix)
12. [Common Pitfalls](#common-pitfalls)
13. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Behavioral Patterns Kya Hai?

**Matlab:** Patterns jo objects ke **behavior** + **communication** ko handle karte hain — kaun kis ko message bhejta hai, kaise responsibility share hoti hai.

### Quick Mapping

| Pattern | Behavior Goal |
|---------|---------------|
| **Strategy** | Swap algorithm at runtime |
| **Observer** | Notify many listeners |
| **Template Method** | Skeleton with hooks |
| **Command** | Encapsulate request as object |
| **Chain of Responsibility** | Pass request along chain |
| **State** | Behavior changes by state |
| **Visitor** | Operation across object structure |
| **Iterator** | Traverse without exposing internals |
| **Mediator** | Centralize complex communication |

---

## Strategy Pattern

**Matlab:** Define a **family of algorithms**, encapsulate each, swap at runtime.

### Use Cases

- **Payment methods** (card, UPI, wallet)
- **Compression algorithms** (gzip, zip, brotli)
- **Sorting** (`Comparator`)
- **Pricing** (regular, premium, discount)
- **Authentication** (LDAP, OAuth, JWT)

### Implementation

```java
public interface PaymentStrategy {
    PaymentResult pay(double amount);
}

public class CardPayment implements PaymentStrategy {
    public PaymentResult pay(double amount) { /* card logic */ }
}

public class UpiPayment implements PaymentStrategy {
    public PaymentResult pay(double amount) { /* UPI logic */ }
}

public class WalletPayment implements PaymentStrategy {
    public PaymentResult pay(double amount) { /* wallet logic */ }
}

// Context
public class PaymentService {
    private PaymentStrategy strategy;
    
    public void setStrategy(PaymentStrategy s) { this.strategy = s; }
    
    public PaymentResult checkout(double amount) {
        return strategy.pay(amount);
    }
}

// Usage
PaymentService svc = new PaymentService();
svc.setStrategy(new UpiPayment());
svc.checkout(500);
```

### Spring Idiom — Map of Strategies

```java
public interface PaymentStrategy {
    String getType();
    PaymentResult pay(double amount);
}

@Component
public class CardPayment implements PaymentStrategy {
    public String getType() { return "CARD"; }
    public PaymentResult pay(double amount) { ... }
}

@Component
public class UpiPayment implements PaymentStrategy {
    public String getType() { return "UPI"; }
    public PaymentResult pay(double amount) { ... }
}

@Service
public class PaymentService {
    private final Map<String, PaymentStrategy> strategies;
    
    public PaymentService(List<PaymentStrategy> list) {
        this.strategies = list.stream()
            .collect(Collectors.toMap(PaymentStrategy::getType, s -> s));
    }
    
    public PaymentResult pay(String type, double amount) {
        PaymentStrategy s = strategies.get(type);
        if (s == null) throw new IllegalArgumentException("Unknown: " + type);
        return s.pay(amount);
    }
}
```

→ Adding new strategy = just create a `@Component`. Open-closed principle.

### Functional Strategy (Java 8+)

```java
Map<String, Function<Double, PaymentResult>> strategies = Map.of(
    "CARD", amount -> new PaymentResult(...),
    "UPI", amount -> new PaymentResult(...)
);

PaymentResult r = strategies.get("CARD").apply(500.0);
```

### Real-World

```java
// Comparator IS Strategy
list.sort(Comparator.comparing(User::getName));
list.sort(Comparator.comparing(User::getAge).reversed());

// Spring Security AuthenticationProvider
// LDAP, JWT, OAuth strategies
```

### When?

✅ Multiple ways to do same thing
✅ Want to add new ways without modifying existing code
✅ Algorithm choice depends on context

---

## Observer Pattern

**Matlab:** **One-to-many** relationship — when subject changes, all observers notified automatically.

### Manual Implementation

```java
public interface Observer<T> {
    void onUpdate(T event);
}

public class Subject<T> {
    private final List<Observer<T>> observers = new CopyOnWriteArrayList<>();
    
    public void subscribe(Observer<T> o) { observers.add(o); }
    public void unsubscribe(Observer<T> o) { observers.remove(o); }
    
    public void notify(T event) {
        observers.forEach(o -> o.onUpdate(event));
    }
}

// Usage
Subject<String> subject = new Subject<>();
subject.subscribe(event -> System.out.println("Logger: " + event));
subject.subscribe(event -> System.out.println("Email: " + event));

subject.notify("User registered");
// Logger: User registered
// Email: User registered
```

### ❌ Java's Built-In `Observable` (Deprecated)

`java.util.Observable` deprecated since Java 9. Don't use.

### ✅ Spring Application Events

#### Define Event

```java
public class UserRegisteredEvent {
    private final User user;
    public UserRegisteredEvent(User u) { this.user = u; }
    public User getUser() { return user; }
}
```

#### Publish

```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final ApplicationEventPublisher publisher;
    
    public User register(String email) {
        User u = userRepo.save(new User(email));
        publisher.publishEvent(new UserRegisteredEvent(u));
        return u;
    }
}
```

#### Listen (Multiple Listeners)

```java
@Component
@Slf4j
public class WelcomeEmailListener {
    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Sending welcome email to {}", event.getUser().getEmail());
    }
}

@Component
public class AnalyticsListener {
    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        analyticsService.track("USER_REGISTERED", event.getUser().getId());
    }
}
```

→ Add more listeners without touching `UserService`. Loose coupling.

### Async Event Listeners

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean
    public Executor taskExecutor() {
        return Executors.newFixedThreadPool(10);
    }
}

@Component
public class WelcomeEmailListener {
    @Async
    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) { ... }
}
```

### Transaction-Bound Listeners

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onUserRegistered(UserRegisteredEvent event) {
    // fires only after DB commit succeeds
}
```

### Reactive — Project Reactor

```java
Flux<String> events = Flux.create(sink -> {
    subject.subscribe(sink::next);
});

events.subscribe(System.out::println);
```

### When?

✅ Event-driven systems
✅ Decouple producer from many consumers
✅ Publish-subscribe within app

---

## Template Method Pattern

**Matlab:** Define **skeleton** in abstract class; subclasses fill in steps.

### Use Cases

- **Algorithm with fixed structure, varying steps** (parsing, ETL, network calls)
- **Spring's `JdbcTemplate`, `RestTemplate`, `RedisTemplate`**
- **Test fixtures** (`@BeforeEach`, `@Test`, `@AfterEach`)

### Manual Implementation

```java
public abstract class DataExporter {
    
    // Template method — final so can't override
    public final void export(String filename) {
        Data data = readData();
        Data transformed = transform(data);
        write(filename, transformed);
        notifyDone();
    }
    
    protected abstract Data readData();
    protected abstract Data transform(Data data);
    protected abstract void write(String filename, Data data);
    
    // Hook with default — subclass can override
    protected void notifyDone() {
        System.out.println("Export complete");
    }
}

public class CsvExporter extends DataExporter {
    protected Data readData() { /* read from DB */ }
    protected Data transform(Data d) { /* CSV format */ }
    protected void write(String f, Data d) { /* write CSV */ }
}

public class ExcelExporter extends DataExporter {
    protected Data readData() { /* read from API */ }
    protected Data transform(Data d) { /* Excel format */ }
    protected void write(String f, Data d) { /* write XLSX */ }
}

// Usage
new CsvExporter().export("users.csv");
new ExcelExporter().export("users.xlsx");
```

### Spring's JdbcTemplate — Template Method

```java
public <T> List<T> query(String sql, RowMapper<T> mapper) {
    Connection con = null;
    try {
        con = dataSource.getConnection();           // step 1
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();           // step 2
        List<T> results = new ArrayList<>();
        while (rs.next()) {
            results.add(mapper.mapRow(rs, rs.getRow()));   // ← varying step (callback)
        }
        return results;
    } catch (SQLException e) {
        throw new DataAccessException(e);           // step 3
    } finally {
        if (con != null) con.close();               // step 4
    }
}
```

→ Connection mgmt + error handling = fixed; row mapping = varying.

### Spring's RestTemplate

Same pattern: connection mgmt + serialization fixed; HTTP method + body varies via params.

### Hook Methods (Optional Steps)

```java
protected void preExport() { /* default: nothing */ }
protected void postExport() { /* default: nothing */ }

// Subclass overrides only what it needs
```

### When?

✅ Algorithm structure consistent, steps differ
✅ Avoid duplicating skeleton across classes
✅ Frameworks giving you "extend + fill in" model

---

## Command Pattern

**Matlab:** Encapsulate a **request as an object** — letting you parameterize, queue, log, undo.

### Use Cases

- **Undo / Redo** (text editors)
- **Job queues** (Runnable in `ExecutorService`)
- **Macro recording**
- **Transaction commit/rollback**
- **GUI buttons** (each button → command)

### Implementation

```java
public interface Command {
    void execute();
    void undo();
}

public class CreateOrderCommand implements Command {
    private final OrderService service;
    private final OrderData data;
    private Long createdId;
    
    public CreateOrderCommand(OrderService s, OrderData d) {
        this.service = s;
        this.data = d;
    }
    
    public void execute() {
        createdId = service.create(data);
    }
    
    public void undo() {
        if (createdId != null) service.delete(createdId);
    }
}

// Invoker
public class CommandInvoker {
    private final Deque<Command> history = new ArrayDeque<>();
    
    public void run(Command cmd) {
        cmd.execute();
        history.push(cmd);
    }
    
    public void undoLast() {
        if (!history.isEmpty()) {
            history.pop().undo();
        }
    }
}
```

### Java Examples

#### Runnable / Callable

```java
ExecutorService pool = Executors.newFixedThreadPool(5);
pool.submit(() -> processOrder(order));   // command queued
```

→ `Runnable` is essentially a Command.

#### Functional Style (Java 8+)

```java
List<Runnable> commands = new ArrayList<>();
commands.add(() -> service.create(data));
commands.add(() -> service.notify(user));
commands.add(() -> service.audit("ACTION"));

commands.forEach(Runnable::run);
```

### Spring Application Events as Commands

Each event = command-like message dispatched.

### When?

✅ Need undo/redo
✅ Want to queue / schedule operations
✅ Want to log / audit operations
✅ Decouple invoker from receiver

---

## Chain of Responsibility

**Matlab:** Pass request along a **chain of handlers** — each decides to handle or forward.

### Use Cases

- **Middleware / filter chains** (HTTP, logging)
- **Spring Security filter chain**
- **Servlet filters**
- **Validation chains**
- **Approval workflows** (manager → director → CEO)

### Implementation

```java
public abstract class Handler {
    protected Handler next;
    
    public Handler setNext(Handler n) { this.next = n; return n; }
    
    public abstract void handle(Request req);
}

public class AuthHandler extends Handler {
    public void handle(Request req) {
        if (!req.isAuthenticated()) {
            throw new UnauthorizedException();
        }
        if (next != null) next.handle(req);
    }
}

public class RateLimitHandler extends Handler {
    public void handle(Request req) {
        if (rateLimitExceeded(req)) {
            throw new TooManyRequestsException();
        }
        if (next != null) next.handle(req);
    }
}

public class LoggingHandler extends Handler {
    public void handle(Request req) {
        log.info("Request: {}", req);
        if (next != null) next.handle(req);
    }
}

// Build chain
Handler chain = new AuthHandler();
chain.setNext(new RateLimitHandler())
     .setNext(new LoggingHandler());

chain.handle(request);
```

### Spring Security Filter Chain

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .addFilter(new CorsFilter())
            .addFilter(new RateLimitFilter())
            .addFilterBefore(new JwtAuthFilter(), UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .build();
    }
}
```

→ Each request flows through filter chain. Each filter can short-circuit or forward.

### Servlet Filters

```java
public class LoggingFilter implements Filter {
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        log.info("Before");
        chain.doFilter(req, res);   // forward
        log.info("After");
    }
}
```

### Functional Chain

```java
Predicate<Request> chain = isAuthenticated()
    .and(notRateLimited())
    .and(hasPermission());

if (chain.test(req)) process(req);
```

### When?

✅ Want to compose request processing pipeline
✅ Each step independent + reusable
✅ Order matters; each can short-circuit

---

## State Pattern

**Matlab:** Object's **behavior changes** based on internal state — implement state-specific behavior in separate classes.

### Problem — Endless `if/switch`

```java
public void process(Order order) {
    if (order.getStatus() == "DRAFT") { ... }
    else if (order.getStatus() == "PLACED") { ... }
    else if (order.getStatus() == "PAID") { ... }
    else if (order.getStatus() == "SHIPPED") { ... }
    // ... 20 more
}
// Repeated everywhere → maintenance nightmare
```

### ✅ State Pattern

```java
public interface OrderState {
    void pay(OrderContext ctx);
    void ship(OrderContext ctx);
    void cancel(OrderContext ctx);
}

public class DraftState implements OrderState {
    public void pay(OrderContext ctx) {
        throw new IllegalStateException("Place order first");
    }
    public void ship(OrderContext ctx) { throw new IllegalStateException(); }
    public void cancel(OrderContext ctx) { ctx.setState(new CancelledState()); }
}

public class PlacedState implements OrderState {
    public void pay(OrderContext ctx) { ctx.setState(new PaidState()); }
    public void ship(OrderContext ctx) { throw new IllegalStateException("Pay first"); }
    public void cancel(OrderContext ctx) { ctx.setState(new CancelledState()); }
}

public class PaidState implements OrderState {
    public void pay(OrderContext ctx) { throw new IllegalStateException("Already paid"); }
    public void ship(OrderContext ctx) { ctx.setState(new ShippedState()); }
    public void cancel(OrderContext ctx) {
        // refund + cancel
        ctx.setState(new CancelledState());
    }
}

public class ShippedState implements OrderState {
    public void pay(OrderContext ctx) { throw new IllegalStateException(); }
    public void ship(OrderContext ctx) { throw new IllegalStateException(); }
    public void cancel(OrderContext ctx) { throw new IllegalStateException(); }
}

public class CancelledState implements OrderState {
    public void pay(OrderContext ctx) { throw new IllegalStateException(); }
    public void ship(OrderContext ctx) { throw new IllegalStateException(); }
    public void cancel(OrderContext ctx) { /* already cancelled */ }
}

// Context
public class OrderContext {
    private OrderState state = new DraftState();
    
    public void setState(OrderState s) { this.state = s; }
    
    public void pay() { state.pay(this); }
    public void ship() { state.ship(this); }
    public void cancel() { state.cancel(this); }
}
```

### Usage

```java
OrderContext order = new OrderContext();
order.cancel();   // DraftState → CancelledState ✅

OrderContext order2 = new OrderContext();
order2.pay();     // DraftState.pay() → throws ✅
```

### Spring State Machine

For complex state machines, use Spring State Machine library:

```java
@Configuration
@EnableStateMachine
public class OrderStateMachineConfig
        extends StateMachineConfigurerAdapter<OrderState, OrderEvent> {
    
    @Override
    public void configure(StateMachineTransitionConfigurer<OrderState, OrderEvent> tx) throws Exception {
        tx.withExternal().source(DRAFT).target(PLACED).event(PLACE)
          .and().withExternal().source(PLACED).target(PAID).event(PAY)
          .and().withExternal().source(PAID).target(SHIPPED).event(SHIP);
    }
}
```

### vs Strategy

| | State | Strategy |
|--|-------|----------|
| Trigger | State changes by transitions | Caller picks |
| Awareness | States know each other | Strategies don't know each other |
| Goal | Manage lifecycle | Choose algorithm |

### When?

✅ Object lifecycle with valid/invalid transitions
✅ Behavior radically differs per state
✅ Want to enforce state transition rules

---

## Visitor Pattern

**Matlab:** Separate **operation** from object structure — define new operations without modifying classes.

### Use Case

```
Object hierarchy: Circle, Square, Triangle (shapes)
Operations needed: area(), perimeter(), draw(), serialize(), validate()...

Without Visitor: every new operation = modify all shape classes
With Visitor: new operation = new visitor class; shapes unchanged
```

### Implementation

```java
public interface ShapeVisitor<T> {
    T visit(Circle c);
    T visit(Square s);
    T visit(Triangle t);
}

public interface Shape {
    <T> T accept(ShapeVisitor<T> visitor);
}

public class Circle implements Shape {
    public final double radius;
    public Circle(double r) { this.radius = r; }
    public <T> T accept(ShapeVisitor<T> v) { return v.visit(this); }
}

public class Square implements Shape {
    public final double side;
    public Square(double s) { this.side = s; }
    public <T> T accept(ShapeVisitor<T> v) { return v.visit(this); }
}

public class Triangle implements Shape {
    public final double base, height;
    public Triangle(double b, double h) { this.base = b; this.height = h; }
    public <T> T accept(ShapeVisitor<T> v) { return v.visit(this); }
}

// Operation 1: Area
public class AreaVisitor implements ShapeVisitor<Double> {
    public Double visit(Circle c) { return Math.PI * c.radius * c.radius; }
    public Double visit(Square s) { return s.side * s.side; }
    public Double visit(Triangle t) { return 0.5 * t.base * t.height; }
}

// Operation 2: Serialize
public class JsonVisitor implements ShapeVisitor<String> {
    public String visit(Circle c) { return "{\"type\":\"circle\",\"radius\":" + c.radius + "}"; }
    public String visit(Square s) { return "{\"type\":\"square\",\"side\":" + s.side + "}"; }
    public String visit(Triangle t) { return "{\"type\":\"triangle\",\"base\":" + t.base + "}"; }
}

// Usage
List<Shape> shapes = List.of(new Circle(5), new Square(3), new Triangle(4, 6));

AreaVisitor area = new AreaVisitor();
double total = shapes.stream().mapToDouble(s -> s.accept(area)).sum();
```

### Modern Alternative — `instanceof` Pattern Matching (Java 17+)

```java
public double area(Shape s) {
    return switch (s) {
        case Circle c -> Math.PI * c.radius * c.radius;
        case Square sq -> sq.side * sq.side;
        case Triangle t -> 0.5 * t.base * t.height;
    };
}
```

→ Sealed classes + pattern matching = ergonomic alternative to visitor.

### When?

✅ Stable object hierarchy, frequently changing operations
✅ Don't want to pollute classes with unrelated methods
✅ Operations need full type info (heterogeneous structure)

❌ Object hierarchy changes often (each change = update all visitors)

---

## Iterator (Bonus)

**Matlab:** Traverse a collection without exposing internals.

### Java Built-In

```java
List<String> list = List.of("a", "b", "c");

// External iterator
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    System.out.println(it.next());
}

// Enhanced for-loop (uses iterator)
for (String s : list) System.out.println(s);

// Stream (internal iterator)
list.stream().forEach(System.out::println);
```

→ `Iterable` + `Iterator` = pattern in JDK.

---

## Mediator (Bonus)

**Matlab:** Centralize complex communication between objects to one mediator.

### Use Cases

- Chat rooms (clients ↔ mediator ↔ clients, not direct)
- Air traffic control
- UI components communicating via central dispatcher

### Spring's `ApplicationEventPublisher` is Mediator

Components publish to it; it dispatches to listeners. Components don't know about each other.

---

## Comparison Matrix

| Pattern | Use Case | Java Example | Spring Example |
|---------|----------|--------------|----------------|
| **Strategy** | Swap algorithm | `Comparator` | `AuthenticationProvider` |
| **Observer** | Notify listeners | `PropertyChangeListener` | `@EventListener` |
| **Template Method** | Skeleton + steps | Servlet `doGet()` | `JdbcTemplate`, `RestTemplate` |
| **Command** | Encapsulate request | `Runnable` | `@Async` methods |
| **Chain of Responsibility** | Pipeline | Servlet filters | Spring Security filter chain |
| **State** | Behavior per state | `Thread.State` | Spring State Machine |
| **Visitor** | Operation across structure | Java Compiler API | Annotation processors |
| **Iterator** | Traverse | `Iterable` | `Stream`, `JpaRepository.findAll()` |
| **Mediator** | Centralize comm | `EventBus` | `ApplicationEventPublisher` |

---

## Common Pitfalls

### 1. Strategy Without Common Interface

```java
if (type.equals("A")) ...  // strategy idea but no interface = still coupled
```

→ Define interface; let strategies implement.

### 2. Observer Memory Leak

Long-lived subject + short-lived observers = leak. Use weak references or unsubscribe.

### 3. Synchronous Observer Blocking

Listener throws → publisher fails. Use `@Async` for fire-and-forget.

### 4. Template Method Tight Coupling

Forced inheritance = brittle. Sometimes prefer composition (Strategy).

### 5. Command Without Undo Capability

If you don't need undo, command pattern overkill. Just call method.

### 6. Chain Order Sensitivity

```
auth → ratelimit → log
vs
log → auth → ratelimit
```

→ Different behaviors. Document order requirements.

### 7. State Pattern Explosion

20 states × 10 methods = 200 method implementations. Use state machine library.

### 8. Visitor Adds Pain When Hierarchy Changes

New shape = update all visitors. Bad if hierarchy unstable.

### 9. Confusing State with Strategy

State = lifecycle-driven; Strategy = caller-driven.

### 10. Spring Events Without TX Awareness

```java
@EventListener
public void send(...) { ... }   // fires before TX commits → race condition
```

→ Use `@TransactionalEventListener(phase = AFTER_COMMIT)` for "after success" actions.

### 11. Self-Invocation Misses Async

```java
@Service
public class S {
    @Async
    public void async() { ... }
    
    public void caller() { this.async(); }   // ❌ runs synchronously (Spring proxy bypassed)
}
```

### 12. Heavy `@EventListener`

Events are sync by default → blocks publisher. Use `@Async` or queue.

---

## Summary Cheat Sheet

| Pattern | One-Liner | When |
|---------|-----------|------|
| **Strategy** | Swap algorithm | Multiple ways to do same thing |
| **Observer** | Notify many | Pub-sub within app |
| **Template Method** | Skeleton + hooks | Algorithm with varying steps |
| **Command** | Request as object | Queue / undo / log |
| **Chain of Responsibility** | Pipeline of handlers | Middleware / filters |
| **State** | Behavior per state | Lifecycle / state machine |
| **Visitor** | Operation across hierarchy | Stable hierarchy, changing ops |
| **Iterator** | Traverse | Hide collection internals |
| **Mediator** | Central dispatcher | Many ↔ many → many ↔ one ↔ many |

| ✅ Do | ❌ Don't |
|-------|---------|
| Strategy via Spring Map | Strategy with switch |
| `@TransactionalEventListener` for after-commit | Event before TX commits |
| `@Async` for non-blocking listeners | Block publisher |
| Template Method via composition (sometimes) | Inheritance everywhere |
| State pattern for complex lifecycle | If/switch in many places |
| Visitor for stable hierarchy | Visitor for fluid hierarchy |
| Spring AOP for cross-cutting | Hand-roll proxies |

---

## Practice

1. Implement Strategy for `PaymentMethod`; use Spring Map injection.
2. Build event system with `@EventListener` (sync + async + transactional).
3. Recreate `JdbcTemplate.query()` using Template Method.
4. Write Command pattern with undo (text editor for set/delete operations).
5. Build Chain of Responsibility for HTTP request validation.
6. Implement State Pattern for `Order` lifecycle (Draft → Placed → Paid → Shipped → Cancelled).
7. Try Spring State Machine for same.
8. Implement Visitor for AST (number, add, multiply); add new operation (eval, print, simplify).
9. Use sealed classes + pattern matching as alternative to Visitor.
10. Identify behavioral patterns in Spring Security filter chain source.
11. Build a fluent `Comparator` chain; observe Strategy at work.
12. Write your own `Iterator` for a tree structure.

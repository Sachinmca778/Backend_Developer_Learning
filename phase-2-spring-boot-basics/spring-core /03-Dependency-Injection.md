# Dependency Injection

## Status: Not Started

---

## Table of Contents

1. [What is Dependency Injection?](#what-is-dependency-injection)
2. [Constructor Injection (Recommended)](#constructor-injection-recommended)
3. [Setter Injection](#setter-injection)
4. [Field Injection (@Autowired - Avoid)](#field-injection-autowired---avoid)
5. [@Inject (JSR-330)](#inject-jsr-330)
6. [Injection of Collections/Maps](#injection-of-collectionsmaps)
7. [Optional Injection](#optional-injection)
8. [@Lazy Injection](#lazy-injection)

---

## What is Dependency Injection?

**Dependency Injection (DI)** IoC (Inversion of Control) ka implementation hai.

**Simple matlab:** Ek class ko uske dependencies (dusre objects) manually create nahi karne padte - Spring unhe automatically inject (provide) kar deta hai.

### Without DI (Tight Coupling)
```java
public class OrderService {
    private PaymentProcessor processor = new CreditCardProcessor();  // Khud create kiya
    private EmailService emailService = new EmailService();          // Khud create kiya
}
```
**Problem:** 
- `PaymentProcessor` change karna ho to `OrderService` modify karna padega
- Testing nahi kar sakte (mock object nahi daal sakte)

### With DI (Loose Coupling)
```java
public class OrderService {
    private PaymentProcessor processor;  // Spring inject karega
    private EmailService emailService;   // Spring inject karega
    
    public OrderService(PaymentProcessor processor, EmailService emailService) {
        this.processor = processor;
        this.emailService = emailService;
    }
}
```
**Fayda:** 
- Easy to test (mock inject kar sakte ho)
- Implementation change kar sakte ho bina `OrderService` modify kiye
- Code clean aur maintainable

---

## Constructor Injection (Recommended)

**Yeh best practice hai.** Spring 4.3+ mein agar sirf ek constructor hai to `@Autowired` likhna bhi zarurat nahi.

### Basic Example
```java
@Service
public class OrderService {
    
    private final PaymentProcessor paymentProcessor;
    private final InventoryManager inventoryManager;
    
    // Constructor Injection
    public OrderService(PaymentProcessor paymentProcessor, 
                        InventoryManager inventoryManager) {
        this.paymentProcessor = paymentProcessor;
        this.inventoryManager = inventoryManager;
    }
    
    public void placeOrder(Order order) {
        inventoryManager.checkStock(order.getItems());
        paymentProcessor.process(order.getTotal());
    }
}
```

### Spring Boot 4.3+ (No @Autowired needed for single constructor)
```java
@Service
public class OrderService {
    
    private final PaymentProcessor paymentProcessor;
    
    // @Autowired likhna optional hai jab sirf ek constructor ho
    public OrderService(PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }
}
```

### Multiple Constructors? `@Autowired` zaruri hai
```java
@Service
public class OrderService {
    
    private final PaymentProcessor paymentProcessor;
    private final EmailService emailService;
    
    // Default constructor
    public OrderService(PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
        this.emailService = null;
    }
    
    // Primary constructor (Spring yeh use karega)
    @Autowired
    public OrderService(PaymentProcessor paymentProcessor, EmailService emailService) {
        this.paymentProcessor = paymentProcessor;
        this.emailService = emailService;
    }
}
```

### Kyun Constructor Injection Best Hai?

| Reason | Explanation |
|--------|-------------|
| **Immutability** | Dependencies `final` ho sakte hain - koi baad mein change nahi kar sakta |
| **Required dependencies clear** | Constructor dekh ke pata chal jaata hai kaunse dependencies zaruri hain |
| **Testing easy** | Test mein directly constructor se mock objects pass kar do |
| **Circular dependency detect** | Startup pe hi error aa jaata hai agar circular dependency ho |
| **Null safety** | Bean kabhi partially initialized nahi ho sakta |

### Testing Example
```java
class OrderServiceTest {
    
    @Test
    void testPlaceOrder() {
        // Mock objects create karo
        PaymentProcessor mockProcessor = mock(PaymentProcessor.class);
        InventoryManager mockInventory = mock(InventoryManager.class);
        
        // Constructor se inject karo
        OrderService service = new OrderService(mockProcessor, mockInventory);
        
        // Test karo
        service.placeOrder(new Order());
        verify(mockProcessor).process(any());
    }
}
```

---

## Setter Injection

Setter methods ke through dependencies inject hote hain. **Optional dependencies ke liye use karo.**

### Example
```java
@Service
public class OrderService {
    
    private PaymentProcessor paymentProcessor;
    private NotificationService notificationService;
    
    // Required dependency (constructor se)
    public OrderService(PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }
    
    // Optional dependency (setter se)
    @Autowired(required = false)
    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    public void placeOrder(Order order) {
        paymentProcessor.process(order.getTotal());
        
        // Null check zaruri hai (optional hai)
        if (notificationService != null) {
            notificationService.sendConfirmation(order.getEmail());
        }
    }
}
```

### Kyun Constructor Injection Better Hai?
```java
// ❌ Setter Injection - Problems
@Service
public class BadExample {
    private DataSource dataSource;
    
    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    public void query() {
        // dataSource null ho sakta hai agar injection fail ho gaya
        // Class partially initialized ho sakta hai
    }
}

// ✅ Constructor Injection - Safe
@Service
public class GoodExample {
    private final DataSource dataSource;
    
    public GoodExample(DataSource dataSource) {
        this.dataSource = dataSource;  // Guaranteed non-null
    }
}
```

### Setter Injection Kab Use Karein?
- Jab dependency truly optional ho
- Jab dependency runtime pe change ho sakti ho
- Jab circular dependency ho (last resort)

---

## Field Injection (@Autowired - Avoid)

**Yeh sabse common hai but sabse bad practice bhi.**

### Example (Avoid This)
```java
@Service
public class OrderService {
    
    @Autowired
    private PaymentProcessor paymentProcessor;
    
    @Autowired
    private InventoryManager inventoryManager;
}
```

### Kyun Avoid Karein?

| Problem | Detail |
|---------|--------|
| **Testing mushkil** | `@Autowired` field pe reflection use karna padta hai testing mein |
| **Dependencies chhup jaati hain** | Class dekh ke pata nahi chalta kaunse dependencies chahiye |
| **Immutability nahi** | Fields `final` nahi ho sakte |
| **Spring-specific** | `@Autowired` Spring ka annotation hai - framework se tightly coupled |
| **Circular dependency hide** | Runtime pe fail ho sakta hai (`NullPointerException`) |

### Testing Problem
```java
// ❌ Field injection - Testing mushkil
class OrderServiceTest {
    @Test
    void testPlaceOrder() {
        OrderService service = new OrderService();
        
        // Ab kaise mock inject karein? 
        // @Autowired private field pe directly set nahi kar sakte
        // ReflectionTestUtils use karna padega (ugly!)
        ReflectionTestUtils.setField(service, "paymentProcessor", mockProcessor);
    }
}
```

### Better Alternative
```java
// ✅ Constructor injection - Testing easy
@Service
public class OrderService {
    private final PaymentProcessor paymentProcessor;
    
    public OrderService(PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }
}

class OrderServiceTest {
    @Test
    void testPlaceOrder() {
        // Direct constructor call - clean!
        OrderService service = new OrderService(mockProcessor);
    }
}
```

---

## @Inject (JSR-330)

`@Inject` Java ka standard annotation hai (JSR-330). `@Autowired` ka drop-in replacement.

### Setup
```xml
<dependency>
    <groupId>jakarta.inject</groupId>
    <artifactId>jakarta.inject-api</artifactId>
    <version>2.0.1</version>
</dependency>
```

### Usage
```java
import jakarta.inject.Inject;

@Service
public class OrderService {
    
    private final PaymentProcessor paymentProcessor;
    
    @Inject  // @Autowired ki jagah @Inject
    public OrderService(PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }
}
```

### @Autowired vs @Inject

| Feature | @Autowired (Spring) | @Inject (JSR-330) |
|---------|---------------------|-------------------|
| **Standard** | Spring-specific | Java standard (JSR-330) |
| **required attribute** | `@Autowired(required = false)` | `@Inject` + `@Nullable` |
| **Portability** | Sirf Spring ke saath | Kisi bhi DI framework ke saath |
| **Spring Boot mein** | Works | Works (same behavior) |

### Optional with @Inject
```java
@Inject
private @Nullable PaymentProcessor processor;  // Optional
```

**Tip:** Agar Spring Boot use kar rahe ho to `@Autowired` hi use karo (zyada convenient hai). `@Inject` tab use karo jab code ko framework-independent rakhna ho.

---

## Injection of Collections/Maps

Spring automatically multiple beans ko ek collection ya map mein inject kar sakta hai.

### List Injection
```java
@Component
public class PaymentProcessor {
    private final List<PaymentMethod> paymentMethods;
    
    // Spring automatically sab PaymentMethod beans ko List mein daal dega
    public PaymentProcessor(List<PaymentMethod> paymentMethods) {
        this.paymentMethods = paymentMethods;
    }
    
    public void pay(String type, double amount) {
        for (PaymentMethod method : paymentMethods) {
            if (method.supports(type)) {
                method.process(amount);
                break;
            }
        }
    }
}

// Multiple beans defined
@Component
class CreditCardPayment implements PaymentMethod {
    public boolean supports(String type) { return "credit".equals(type); }
    public void process(double amount) { System.out.println("Credit: " + amount); }
}

@Component
class DebitCardPayment implements PaymentMethod {
    public boolean supports(String type) { return "debit".equals(type); }
    public void process(double amount) { System.out.println("Debit: " + amount); }
}

// Usage
paymentProcessor.pay("credit", 100.0);  // CreditCardPayment use hoga
```

### Map Injection (Bean Name as Key)
```java
@Component
public class PaymentProcessor {
    private final Map<String, PaymentMethod> paymentMethodMap;
    
    // Key = bean name, Value = bean instance
    public PaymentProcessor(Map<String, PaymentMethod> paymentMethodMap) {
        this.paymentMethodMap = paymentMethodMap;
    }
    
    public void pay(String method, double amount) {
        PaymentMethod pm = paymentMethodMap.get(method);
        if (pm != null) {
            pm.process(amount);
        }
    }
}

// Bean names define karte ho
@Component("creditCard")
class CreditCardPayment implements PaymentMethod { }

@Component("debitCard")
class DebitCardPayment implements PaymentMethod { }

// Usage
Map<String, PaymentMethod> methods = paymentMethodMap;
// methods = {"creditCard": CreditCardPayment@..., "debitCard": DebitCardPayment@...}
```

### Set Injection
```java
@Component
public class Validator {
    private final Set<ValidationRule> rules;
    
    public Validator(Set<ValidationRule> rules) {
        this.rules = rules;  // Duplicates automatically removed
    }
}
```

### Array Injection
```java
@Component
public class EventDispatcher {
    private final EventListener[] listeners;
    
    public EventDispatcher(EventListener[] listeners) {
        this.listeners = listeners;
    }
}
```

---

## Optional Injection

Jab dependency zaruri nahi hai - bean exist karta hai to inject karo, nahi to null/empty chhod do.

### Using @Autowired(required = false)
```java
@Service
public class OrderService {
    
    private NotificationService notificationService;
    
    @Autowired(required = false)  // Optional - null ho sakta hai
    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    public void placeOrder(Order order) {
        // Null check zaruri
        if (notificationService != null) {
            notificationService.send(order.getEmail());
        }
    }
}
```

### Using @Nullable
```java
@Service
public class OrderService {
    
    private final @Nullable NotificationService notificationService;
    
    public OrderService(@Nullable NotificationService notificationService) {
        this.notificationService = notificationService;
    }
}
```

### Using Optional<>
```java
@Service
public class OrderService {
    
    private final Optional<NotificationService> notificationService;
    
    public OrderService(Optional<NotificationService> notificationService) {
        this.notificationService = notificationService;
    }
    
    public void placeOrder(Order order) {
        notificationService.ifPresent(ns -> ns.send(order.getEmail()));
    }
}
```

### Collections - Always Injected (Empty if No Beans)
```java
@Service
public class OrderService {
    
    private final List<Interceptor> interceptors;
    
    // Agar koi Interceptor bean nahi hai to empty List milega (null nahi)
    public OrderService(List<Interceptor> interceptors) {
        this.interceptors = interceptors;
    }
}
```

---

## @Lazy Injection

**Normal behavior:** Spring startup pe sab dependencies resolve karta hai.  
**@Lazy behavior:** Dependency tab inject hogi jab actually use hogi.

### Use Cases
1. **Circular dependency solve karna**
2. **Heavy beans ko delay load karna**
3. **Startup time improve karna**

### Example
```java
@Service
public class OrderService {
    
    private final ReportService reportService;
    
    // @Lazy - ReportService tab initialize hoga jab actually call hoga
    public OrderService(@Lazy ReportService reportService) {
        this.reportService = reportService;
    }
    
    public void placeOrder(Order order) {
        // Order place hoga
        // ReportService abhi initialize nahi hua
    }
    
    public void generateReport() {
        // Yeh pehli baar call hua - toh ReportService ab initialize hoga
        reportService.generate();
    }
}
```

### How @Lazy Works Internally
```java
// Normal injection
OrderService service = new OrderService(new ReportService());  // Startup pe create

// Lazy injection
OrderService service = new OrderService(ReportServiceProxy());  // Proxy create hoga
// ReportService tab create hoga jab service.generateReport() call hoga
```

### @Lazy on @Component
```java
@Lazy
@Component
public class HeavyService {
    public HeavyService() {
        System.out.println("HeavyService created");  // Startup pe nahi, jab chahiye tab
    }
}
```

### Circular Dependency Fix with @Lazy
```java
@Service
public class ServiceA {
    private final ServiceB serviceB;
    
    public ServiceA(@Lazy ServiceB serviceB) {
        this.serviceB = serviceB;
    }
}

@Service
public class ServiceB {
    private final ServiceA serviceA;
    
    public ServiceB(ServiceA serviceA) {
        this.serviceA = serviceA;
    }
}
```

---

## Comparison Table

| Type | Syntax | Recommended? | Best For |
|------|--------|--------------|----------|
| **Constructor** | `public Service(Dependency d)` | ✅ Yes | Required dependencies |
| **Setter** | `@Autowired setDependency(Dependency d)` | ⚠️ Sometimes | Optional dependencies |
| **Field** | `@Autowired private Dependency d` | ❌ No | Avoid completely |
| **@Inject** | `@Inject public Service(Dependency d)` | ✅ Yes | Framework-independent code |
| **Collection** | `List<Dependency>` | ✅ Yes | Multiple beans of same type |
| **Optional** | `@Nullable` or `Optional<T>` | ✅ Yes | Truly optional dependencies |
| **@Lazy** | `@Lazy Dependency d` | ⚠️ Sometimes | Circular dependency, heavy beans |

---

## Quick Reference

```java
// ✅ Best Practice - Constructor Injection
@Service
public class OrderService {
    private final PaymentProcessor processor;
    
    public OrderService(PaymentProcessor processor) {
        this.processor = processor;
    }
}

// Inject all beans of a type
public OrderService(List<PaymentProcessor> processors) { }
public OrderService(Map<String, PaymentProcessor> processors) { }

// Optional injection
public OrderService(@Nullable NotificationService notifier) { }
public OrderService(Optional<NotificationService> notifier) { }

// Lazy injection
public OrderService(@Lazy ReportService reportService) { }
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Constructor Injection** | Hamesha yehi use karo - required dependencies ke liye |
| **Setter Injection** | Sirf optional dependencies ke liye |
| **Field Injection (@Autowired)** | Avoid karo - testing mushkil hoti hai |
| **@Inject (JSR-330)** | Standard Java annotation - `@Autowired` ka alternative |
| **Collection Injection** | `List<T>`, `Map<String, T>`, `Set<T>`, `T[]` - multiple beans inject karo |
| **Optional Injection** | `@Nullable`, `Optional<T>`, `@Autowired(required=false)` |
| **@Lazy** | Bean tab create hoga jab actually use hoga - circular dependency ke liye useful |

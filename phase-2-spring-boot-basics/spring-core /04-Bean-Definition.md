# Bean Definition

## Status: Not Started

---

## Table of Contents

1. [What is a Bean?](#what-is-a-bean)
2. [Stereotype Annotations](#stereotype-annotations)
3. [@Component, @Service, @Repository, @Controller](#component-service-repository-controller)
4. [@Configuration and @Bean](#configuration-and-bean)
5. [Component Scanning (@ComponentScan)](#component-scanning-componentscan)
6. [Bean Naming Conventions](#bean-naming-conventions)
7. [@Primary](#primary)
8. [@Qualifier](#qualifier)

---

## What is a Bean?

**Bean kya hai?**
Spring container jo bhi object manage karta hai, use **Bean** kehte hain. Normal Java object se farq yeh hai ki bean ka lifecycle Spring control karta hai - create, configure, aur destroy sab Spring karta hai.

```java
// Normal Java object
UserService service = new UserService();  // Aapne khud banaya

// Spring Bean
@Component
public class UserService { }  // Spring automatically manage karega
```

### Bean Banane ke 2 Tarike

| Method | Annotation | Use Case |
|--------|-----------|----------|
| **Annotation-based** | `@Component`, `@Service`, etc. | Apni classes ko bean banana ho |
| **Java Config** | `@Configuration` + `@Bean` | Third-party classes ko bean banana ho |

---

## Stereotype Annotations

**Stereoype annotations** woh annotations hain jo Spring ko batate hain: "Yeh class ek bean hai aur isko container mein register karo."

```
@Component (Generic - koi bhi bean)
    ├── @Service       (Business logic layer)
    ├── @Repository    (Data access layer)
    └── @Controller    (Presentation layer - returns views)
```

**Yeh sab internally `@Component` hi hain** - bas readability aur extra features ke liye alag annotations hain.

---

## @Component, @Service, @Repository, @Controller

### @Component

**Generic annotation** - kisi bhi Spring-managed class ke liye use hota hai.

```java
@Component
public class EmailValidator {
    public boolean isValid(String email) {
        return email.contains("@");
    }
}
```

**Kab use karein:** Jab class kisi specific layer (Service, Repository, Controller) mein fit nahi hoti. Utility/helper classes ke liye.

---

### @Service

**Business logic layer** ke liye use hota hai. Service classes yahan aati hain.

```java
@Service
public class OrderService {
    
    private final OrderRepository orderRepository;
    
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
    
    public Order placeOrder(Order order) {
        // Business logic yahan
        return orderRepository.save(order);
    }
}
```

**Kab use karein:** Jab class business logic handle kar rahi ho - calculations, validations, workflow management.

**@Component vs @Service:** Technically same hai, but `@Service` use karne se code readable hota hai - dekh ke pata chal jaata hai yeh service layer ki class hai.

---

### @Repository

**Data access layer** ke liye use hota hai. Database operations yahan hote hain.

```java
@Repository
public class UserRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public User findById(Long id) {
        return jdbcTemplate.queryForObject(
            "SELECT * FROM users WHERE id = ?", 
            new Object[]{id}, 
            (rs, row) -> new User(rs.getLong("id"), rs.getString("name"))
        );
    }
}
```

**Extra Feature:** `@Repository` automatically **SQLException ko Spring ke DataAccessException mein convert** kar deta hai.

```java
// Without @Repository - checked SQLException handle karna padta
public User find() throws SQLException { }

// With @Repository - Spring unchecked exception mein convert kar deta hai
// Try-catch ki zarurat nahi, consistent exception hierarchy
```

**Kab use karein:** Jab class database ya external data source se interact kar rahi ho.

---

### @Controller

**Presentation layer** (web layer) ke liye use hota hai. HTTP requests handle karta hai.

```java
@Controller
public class UserController {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping("/users/{id}")
    public String getUser(@PathVariable Long id, Model model) {
        User user = userService.findById(id);
        model.addAttribute("user", user);
        return "user-details";  // View name return karega
    }
}
```

**@Controller vs @RestController:**

| Feature | @Controller | @RestController |
|---------|-------------|-----------------|
| **Returns** | View name (JSP, Thymeleaf) | JSON/XML response body |
| **Use Case** | Server-side rendering | REST APIs |
| **@ResponseBody** | Manually add karna padta hai | Automatically included |

```java
// @Controller - View return karega
@Controller
public class PageController {
    @GetMapping("/home")
    public String home() {
        return "home-page";  // home-page.html render hoga
    }
}

// @RestController - JSON return karega
@RestController
public class ApiController {
    @GetMapping("/api/user")
    public User getUser() {
        return new User("John");  // {"name": "John"} JSON milega
    }
}
```

---

## @Configuration and @Bean

### @Configuration

**Java-based configuration** define karta hai. XML configuration ka replacement.

```java
@Configuration
public class AppConfig {
    
    @Bean
    public UserService userService() {
        return new UserService();
    }
    
    @Bean
    public EmailService emailService() {
        return new EmailService();
    }
}
```

**Matlab:** Yeh class bean definitions contain karti hai - Spring isko process karega aur beans create karega.

---

### @Bean

**Method pe lagta hai** aur batata hai: "Is method ka return value ek bean hai."

```java
@Configuration
public class AppConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate template = new RestTemplate();
        template.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        return template;  // Yeh object Spring container mein register ho jayega
    }
}
```

### @Component vs @Bean

| Feature | @Component | @Bean |
|---------|-----------|-------|
| **Kahan lagta hai** | Class pe | Method pe |
| **Control** | Spring khud object banata hai | Aap manually object return karte ho |
| **Use Case** | Apni classes ke liye | Third-party classes ke liye |
| **Customization** | Limited | Full control over bean creation |

### Example: Third-party Library Bean
```java
// Third-party class - aap @Component nahi laga sakte
// (Code aapka nahi hai, annotation add nahi kar sakte)
public class ExternalCache {
    private int maxSize;
    
    public ExternalCache(int maxSize) {
        this.maxSize = maxSize;
    }
}

// Solution: @Bean use karo
@Configuration
public class CacheConfig {
    
    @Bean
    public ExternalCache externalCache() {
        return new ExternalCache(1000);  // Manual configuration possible
    }
}
```

### @Bean with Parameters
```java
@Configuration
public class AppConfig {
    
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl("jdbc:mysql://localhost:3306/mydb");
        ds.setUsername("root");
        ds.setPassword("pass");
        return ds;
    }
    
    // @Bean method ke parameters bhi automatically inject hote hain
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        // Spring automatically dataSource bean inject kar dega
        return new JdbcTemplate(dataSource);
    }
}
```

---

## Component Scanning (@ComponentScan)

**Component scanning** ka matlab: Spring automatically `@Component`, `@Service`, `@Repository`, `@Controller` annotated classes ko dhundh ke beans register karta hai.

### Default Behavior

`@SpringBootApplication` already includes `@ComponentScan`. Yeh **us package aur uske sub-packages** ko scan karta hai jahan main application class hai.

```
com.example.myapp/
├── Application.java          ← @SpringBootApplication yahan
├── controller/
│   └── UserController.java   ← @Controller (auto-detected)
├── service/
│   └── UserService.java      ← @Service (auto-detected)
└── repository/
    └── UserRepository.java   ← @Repository (auto-detected)
```

```java
@SpringBootApplication  // @ComponentScan included automatically
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

**Yeh kaam karega** kyunki sab classes `com.example.myapp` package ke andar ya uske sub-packages mein hain.

### Different Package? Specify Karna Padega

```
com.example.myapp/
├── Application.java
└── controller/
    └── UserController.java

com.example.common/
└── SharedService.java      ← Yeh SCAN NAHI HOGA (different package)
```

```java
// Fix: Explicitly scan other packages
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.example.myapp",
    "com.example.common"    // Ab yeh bhi scan hoga
})
public class Application { }
```

### Custom ComponentScan

```java
@Configuration
@ComponentScan("com.example")  // Specific package scan
public class AppConfig { }

@ComponentScan(basePackages = {"com.example.service", "com.example.repo"})
public class AppConfig { }

// Exclude specific classes
@ComponentScan(
    basePackages = "com.example",
    excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, 
                                           classes = Controller.class)
)
public class AppConfig { }
```

---

## Bean Naming Conventions

### Default Naming

Spring beans ko **camelCase** mein naam deta hai (class name ke first letter ko lowercase kar ke).

```java
@Component
public class UserService { }        // Bean name: "userService"

@Component
public class XMLParser { }          // Bean name: "XMLParser" (special case)

@Service
public class OrderService { }       // Bean name: "orderService"
```

### Custom Bean Name

```java
@Component("customUserService")
public class UserService { }

@Service("myOrderService")
public class OrderService { }
```

### @Bean Name

```java
@Configuration
public class AppConfig {
    
    @Bean("myRestTemplate")  // Custom name
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    @Bean  // Default name: "dataSource"
    public DataSource dataSource() {
        return new DriverManagerDataSource();
    }
}
```

### Bean Access by Name

```java
ApplicationContext context = // ...

// By type
UserService service = context.getBean(UserService.class);

// By name
UserService service = (UserService) context.getBean("userService");
UserService service = (UserService) context.getBean("customUserService");
```

---

## @Primary

**Jab ek type ke multiple beans hon**, Spring confuse ho jaata hai - kaunsa inject karein?

**Solution:** `@Primary` lagao - Spring ko batao: "Default yeh wala bean use karo."

### Example
```java
// Interface
public interface PaymentProcessor {
    void process(double amount);
}

// Multiple implementations
@Component("creditCard")
public class CreditCardProcessor implements PaymentProcessor {
    public void process(double amount) {
        System.out.println("Credit Card: " + amount);
    }
}

@Component("debitCard")
public class DebitCardProcessor implements PaymentProcessor {
    public void process(double amount) {
        System.out.println("Debit Card: " + amount);
    }
}

// Problem: Spring confuse hoga - kaunsa bean inject karein?
@Service
public class OrderService {
    private final PaymentProcessor processor;
    
    // ERROR: NoUniqueBeanDefinitionException
    // 2 beans found: creditCardProcessor, debitCardProcessor
    public OrderService(PaymentProcessor processor) {
        this.processor = processor;
    }
}

// Solution 1: @Primary use karo
@Component
@Primary  // Default yehi bean inject hoga
public class CreditCardProcessor implements PaymentProcessor { }

// Ab kaam karega - CreditCardProcessor inject hoga
```

### @Bean with @Primary
```java
@Configuration
public class AppConfig {
    
    @Bean
    @Primary  // Yeh default hoga
    public PaymentProcessor creditCardProcessor() {
        return new CreditCardProcessor();
    }
    
    @Bean
    public PaymentProcessor debitCardProcessor() {
        return new DebitCardProcessor();
    }
}
```

---

## @Qualifier

`@Primary` se ek default bean set ho jaata hai. **Lekin agar aapko specifically koi aur bean chahiye?**

**Solution:** `@Qualifier("beanName")` use karo - exact bean specify karo.

### Example
```java
// Multiple beans
@Component("creditCard")
public class CreditCardProcessor implements PaymentProcessor { }

@Component("debitCard")
public class DebitCardProcessor implements PaymentProcessor { }

@Service
public class OrderService {
    private final PaymentProcessor creditProcessor;
    private final PaymentProcessor debitProcessor;
    
    // @Qualifier se specify karo kaunsa bean chahiye
    public OrderService(
        @Qualifier("creditCard") PaymentProcessor creditProcessor,
        @Qualifier("debitCard") PaymentProcessor debitProcessor
    ) {
        this.creditProcessor = creditProcessor;
        this.debitProcessor = debitProcessor;
    }
}
```

### @Qualifier with @Bean
```java
@Configuration
public class AppConfig {
    
    @Bean("creditCard")
    public PaymentProcessor creditCardProcessor() {
        return new CreditCardProcessor();
    }
    
    @Bean("debitCard")
    public PaymentProcessor debitCardProcessor() {
        return new DebitCardProcessor();
    }
}

@Service
public class OrderService {
    public OrderService(@Qualifier("creditCard") PaymentProcessor processor) {
        // CreditCardProcessor inject hoga
    }
}
```

### Custom Qualifier Annotation
```java
// Apna custom qualifier bana sakte ho
@Qualifier
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CreditCard { }

@Qualifier
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface DebitCard { }

// Use
@CreditCard
@Component
public class CreditCardProcessor implements PaymentProcessor { }

@Service
public class OrderService {
    public OrderService(@CreditCard PaymentProcessor processor) {
        // Type-safe qualifier - string typos nahi honge
    }
}
```

---

## @Primary vs @Qualifier

| Feature | @Primary | @Qualifier |
|---------|----------|------------|
| **Purpose** | Default bean set karta hai | Specific bean select karta hai |
| **Kahan lagta hai** | Bean definition pe | Injection point pe |
| **Count** | Ek hi @Primary ho sakta hai per type | Multiple qualifiers ho sakte hain |
| **Use Case** | Jab ek default chahiye | Jab different scenarios mein different beans chahiye |

### Together Use Karna
```java
@Component
@Primary  // Default yehi hai
public class CreditCardProcessor implements PaymentProcessor { }

@Component
public class DebitCardProcessor implements PaymentProcessor { }

@Service
public class OrderService {
    private final PaymentProcessor defaultProcessor;
    private final PaymentProcessor debitProcessor;
    
    public OrderService(
        PaymentProcessor defaultProcessor,  // CreditCard (Primary) inject hoga
        @Qualifier("debitCard") PaymentProcessor debitProcessor  // Specific bean
    ) {
        this.defaultProcessor = defaultProcessor;
        this.debitProcessor = debitProcessor;
    }
}
```

---

## Quick Reference

```java
// Bean define karne ke tarike
@Component
@Service
@Repository
@Controller
@RestController
@Configuration + @Bean

// Component scanning
@SpringBootApplication  // Default scan
@ComponentScan("com.example")  // Custom package scan

// Bean naming
@Component("customName")
@Bean("customName")

// Multiple beans resolution
@Primary          // Default bean
@Qualifier("name")  // Specific bean
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **@Component** | Generic bean annotation |
| **@Service** | Business logic layer ke liye |
| **@Repository** | Data access layer + exception translation |
| **@Controller** | Web layer - views return karta hai |
| **@RestController** | Web layer - JSON/XML return karta hai |
| **@Configuration + @Bean** | Third-party classes ko bean banana |
| **@ComponentScan** | Automatically beans dhundhta hai |
| **Bean Naming** | Default: camelCase, Custom: `@Component("name")` |
| **@Primary** | Multiple beans mein default select karta hai |
| **@Qualifier** | Specific bean explicitly select karta hai |

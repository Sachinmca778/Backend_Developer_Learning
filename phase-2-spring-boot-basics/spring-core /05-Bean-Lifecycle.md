# Bean Lifecycle

## Status: Not Started

---

## Table of Contents

1. [Bean Lifecycle Overview](#bean-lifecycle-overview)
2. [Instantiation](#instantiation)
3. [Dependency Injection](#dependency-injection)
4. [@PostConstruct](#postconstruct)
5. [InitializingBean Interface](#initializingbean-interface)
6. [@PreDestroy](#predestroy)
7. [DisposableBean Interface](#disposablebean-interface)
8. [BeanPostProcessor](#beanpostprocessor)
9. [BeanFactoryPostProcessor](#beanfactorypostprocessor)

---

## Bean Lifecycle Overview

Spring container beans ko ek well-defined lifecycle mein manage karta hai. **Jab application start hoti hai toh Spring beans create karta hai, configure karta hai, use karta hai, aur finally destroy karta hai.**

### Complete Lifecycle Flow

```
1. Bean Instantiation (Object create hota hai)
       ↓
2. Dependency Injection (@Autowired properties set hote hain)
       ↓
3. BeanFactoryPostProcessor callbacks
       ↓
4. BeanPostProcessor - postProcessBeforeInitialization
       ↓
5. @PostConstruct / InitializingBean / init-method
       ↓
6. BeanPostProcessor - postProcessAfterInitialization
       ↓
7. BEAN READY (Application use kar sakti hai)
       ↓
8. @PreDestroy / DisposableBean / destroy-method
       ↓
9. Bean Destroyed
```

**Note:** Steps 3-6 initialization phase hain, step 7 runtime phase hai, steps 8-9 destruction phase hain.

---

## Instantiation

**Pehla step:** Spring bean ka object create karta hai (constructor call hota hai).

```java
@Component
public class UserService {
    
    public UserService() {
        System.out.println("1. UserService constructor called");
        // Bean abhi just create hua hai
        // Dependencies abhi inject nahi hui hain
    }
}
```

**Important:** Is stage pe:
- Object create ho gaya hai
- **Lekin dependencies abhi inject nahi hui** - `@Autowired` fields `null` honge
- Bean container mein register ho gaya hai

---

## Dependency Injection

**Second step:** Spring `@Autowired` fields, constructor parameters, aur setter methods ke through dependencies inject karta hai.

```java
@Component
public class UserService {
    
    private final UserRepository userRepository;
    private EmailService emailService;
    
    // Constructor injection
    public UserService(UserRepository userRepository) {
        System.out.println("1. Constructor called");
        this.userRepository = userRepository;  // Manual assignment
    }
    
    // Setter injection
    @Autowired
    public void setEmailService(EmailService emailService) {
        System.out.println("2. Setter injection: EmailService set");
        this.emailService = emailService;
    }
    
    // Field injection
    @Autowired
    private NotificationService notificationService;  // Spring automatically set karega
}
```

**Is stage pe:**
- Sab dependencies available ho jaati hain
- Bean abhi fully initialized nahi hai (callbacks abhi baaki hain)

---

## @PostConstruct

**Initialization callback** - dependency injection ke baad run hota hai.

```java
@Component
public class DatabaseService {
    
    private ConnectionPool connectionPool;
    
    public DatabaseService() {
        System.out.println("1. Constructor");
    }
    
    @Autowired
    public void setConnectionPool(ConnectionPool pool) {
        this.connectionPool = pool;
        System.out.println("2. Dependency injected");
    }
    
    @PostConstruct
    public void init() {
        System.out.println("3. @PostConstruct - Setup complete");
        // Yahan setup karo - connections open karo, cache load karo
        // Dependencies already available hain
        connectionPool.initialize();
    }
}
```

**Output:**
```
1. Constructor
2. Dependency injected
3. @PostConstruct - Setup complete
```

**Kab use karein:**
- Database connections open karna
- Cache pre-load karna
- Configuration validation
- Resource initialization

### Multiple @PostConstruct
```java
@PostConstruct
public void method1() {
    System.out.println("Method 1");
}

@PostConstruct
public void method2() {
    System.out.println("Method 2");
}
```

**Execution order guaranteed nahi hai.** Multiple `@PostConstruct` avoid karo - ek hi `@PostConstruct` method rakho.

---

## InitializingBean Interface

`@PostConstruct` ka **Spring-specific alternative**. Interface implement karna padta hai.

```java
@Component
public class LegacyService implements InitializingBean {
    
    public LegacyService() {
        System.out.println("1. Constructor");
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("2. After properties set - initialization");
        // Dependencies abhi inject ho chuki hain
    }
}
```

### @PostConstruct vs InitializingBean

| Feature | @PostConstruct | InitializingBean |
|---------|---------------|------------------|
| **Type** | JSR-250 (Java standard) | Spring-specific interface |
| **Method name** | Kuch bhi ho sakta hai | Fixed: `afterPropertiesSet()` |
| **Invasiveness** | Low (sirf annotation) | High (interface implement karna padta hai) |
| **Recommended?** | ✅ Yes | ❌ No |

**Recommendation:** Hamesha `@PostConstruct` use karo. `InitializingBean` sirf legacy code mein milta hai.

### init-method (XML-style in Java Config)
```java
@Configuration
public class AppConfig {
    
    @Bean(initMethod = "init")
    public MyService myService() {
        return new MyService();
    }
}

public class MyService {
    public void init() {
        System.out.println("Init method called");
    }
}
```

---

## @PreDestroy

**Destruction callback** - bean destroy hone se pehle run hota hai.

```java
@Component
public class DatabaseService {
    
    private Connection connection;
    
    @PostConstruct
    public void init() {
        this.connection = DriverManager.getConnection("jdbc:mysql://...");
        System.out.println("1. Connection opened");
    }
    
    @PreDestroy
    public void cleanup() {
        System.out.println("2. @PreDestroy - Cleanup");
        // Yahan cleanup karo - connections close karo, files close karo
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
```

**Output (app shutdown pe):**
```
1. Connection opened
... (application runs) ...
2. @PreDestroy - Cleanup
```

**Kab use karein:**
- Database connections close karna
- File handles release karna
- Scheduled tasks cancel karna
- Cache flush karna

### @PreDestroy with Scope
```java
@Component
@Scope("prototype")  // @PreDestroy sirf tab call hoga jab container explicitly destroy karega
public class PrototypeBean {
    
    @PreDestroy
    public void cleanup() {
        System.out.println("Cleanup");
    }
}
```

**Important:** Singleton beans ke liye `@PreDestroy` automatically call hota hai. Prototype beans ke liye manually manage karna padta hai.

---

## DisposableBean Interface

`@PreDestroy` ka **Spring-specific alternative**.

```java
@Component
public class LegacyResource implements DisposableBean {
    
    @Override
    public void destroy() throws Exception {
        System.out.println("DisposableBean.destroy() - Cleanup");
        // Resource cleanup
    }
}
```

### @PreDestroy vs DisposableBean

| Feature | @PreDestroy | DisposableBean |
|---------|-------------|----------------|
| **Type** | JSR-250 (Java standard) | Spring-specific interface |
| **Method name** | Kuch bhi ho sakta hai | Fixed: `destroy()` |
| **Invasiveness** | Low | High |
| **Recommended?** | ✅ Yes | ❌ No |

### destroy-method (Java Config)
```java
@Configuration
public class AppConfig {
    
    @Bean(destroyMethod = "cleanup")
    public DataSource dataSource() {
        return new DataSource();
    }
}

public class DataSource {
    public void cleanup() {
        System.out.println("Custom destroy method");
    }
}
```

**Note:** Spring Boot automatically `destroyMethod` detect kar leta hai (public `close()` ya `shutdown()` methods).

---

## BeanPostProcessor

**BeanPostProcessor** ek special interface hai jo **har bean ke initialization phase ke beech mein intercept** karta hai.

### Interface Methods

```java
public interface BeanPostProcessor {
    
    // @PostConstruct se PEHLE call hota hai
    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;  // Bean return karo (modified ya original)
    }
    
    // @PostConstruct ke BAAD call hota hai
    default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;  // Bean return karo (modified ya original)
    }
}
```

### Execution Order
```
1. Bean Constructor
2. Dependency Injection
3. postProcessBeforeInitialization
4. @PostConstruct
5. postProcessAfterInitialization  ← Yahan bean ready hai
```

### Example: Logging All Beans
```java
@Component
public class LoggingBeanPostProcessor implements BeanPostProcessor {
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        System.out.println("Before init: " + beanName);
        return bean;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        System.out.println("After init: " + beanName);
        return bean;
    }
}
```

**Output:**
```
Before init: userService
1. UserService constructor
2. Dependencies injected
After init: userService
Before init: orderController
...
```

### Example: Bean Modification
```java
@Component
public class ValidationPostProcessor implements BeanPostProcessor {
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // Bean ko modify kar sakte ho
        if (bean instanceof Validator) {
            Validator validator = (Validator) bean;
            validator.validate();  // Validation run karo
        }
        return bean;
    }
}
```

### Proxy Creation (AOP ke liye useful)
```java
@Component
public class ProxyPostProcessor implements BeanPostProcessor {
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (shouldProxy(bean)) {
            return Proxy.newProxyInstance(
                bean.getClass().getClassLoader(),
                bean.getClass().getInterfaces(),
                new LoggingInvocationHandler(bean)
            );
        }
        return bean;
    }
    
    private boolean shouldProxy(Object bean) {
        return bean.getClass().isAnnotationPresent(Transactional.class);
    }
}
```

**Real-world use:** Spring internally `@Transactional`, `@Async` jaise annotations ke liye BeanPostProcessor use karta hai proxy banane ke liye.

---

## BeanFactoryPostProcessor

**BeanFactoryPostProcessor** bean create hone se **pehle** bean definitions ko modify karne deta hai.

### BeanPostProcessor vs BeanFactoryPostProcessor

| Feature | BeanPostProcessor | BeanFactoryPostProcessor |
|---------|-------------------|--------------------------|
| **Kab call hota hai** | Bean create hone ke **baad** | Bean definition register hone ke **baad**, bean create hone se **pehle** |
| **Kya modify kar sakta hai** | Bean instance | Bean definition (properties, class, scope) |
| **Scope** | Per bean | Factory level (sab beans pe effect) |

### Example: Property Value Change
```java
@Component
public class CustomBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
    
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        System.out.println("BeanFactoryPostProcessor called");
        
        // Bean definition modify karo
        BeanDefinition bd = beanFactory.getBeanDefinition("dataSource");
        
        // Properties change kar sakte ho
        MutablePropertyValues properties = bd.getPropertyValues();
        properties.addPropertyValue("url", "jdbc:mysql://production:3306/mydb");
    }
}
```

### Example: PropertySourcesPlaceholderConfigurer
Spring Boot ka `@Value` annotation internally BeanFactoryPostProcessor use karta hai:

```java
@Configuration
public class AppConfig {
    
    // ${app.name} values .properties file se aati hain
    // Yeh BeanFactoryPostProcessor ka kaam hai
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
```

### Real-world Use Case
```java
@Component
public class ConditionalBeanPostProcessor implements BeanFactoryPostProcessor {
    
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory factory) {
        // Environment check karo
        String profile = System.getProperty("spring.profiles.active");
        
        if ("test".equals(profile)) {
            // Test profile hai - test beans register karo
            BeanDefinition testDbBean = BeanDefinitionBuilder
                .genericBeanDefinition(TestDataSource.class)
                .getBeanDefinition();
            factory.registerBeanDefinition("dataSource", testDbBean);
        }
    }
}
```

---

## Complete Example - Full Lifecycle

```java
@Component
public class FullLifecycleDemo implements InitializingBean, DisposableBean {
    
    public FullLifecycleDemo() {
        System.out.println("1. Constructor called");
    }
    
    @Autowired
    public void setDependency(Object dep) {
        System.out.println("2. Dependency injected");
    }
    
    @PostConstruct
    public void postConstruct() {
        System.out.println("3. @PostConstruct");
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("4. InitializingBean.afterPropertiesSet()");
    }
    
    @PreDestroy
    public void preDestroy() {
        System.out.println("5. @PreDestroy");
    }
    
    @Override
    public void destroy() throws Exception {
        System.out.println("6. DisposableBean.destroy()");
    }
}

// BeanPostProcessor
@Component
class DemoPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String name) {
        if (bean instanceof FullLifecycleDemo) {
            System.out.println("BeanPostProcessor - Before");
        }
        return bean;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String name) {
        if (bean instanceof FullLifecycleDemo) {
            System.out.println("BeanPostProcessor - After");
        }
        return bean;
    }
}
```

**Startup Output:**
```
1. Constructor called
2. Dependency injected
BeanPostProcessor - Before
3. @PostConstruct
4. InitializingBean.afterPropertiesSet()
BeanPostProcessor - After
// Bean ready - application running
```

**Shutdown Output:**
```
5. @PreDestroy
6. DisposableBean.destroy()
```

**Note:** `@PostConstruct` pehle call hota hai, fir `InitializingBean`. Same for destruction - `@PreDestroy` pehle, fir `DisposableBean`.

---

## Order of Execution (Multiple Methods)

### Initialization Order
```
1. @PostConstruct
2. InitializingBean.afterPropertiesSet()
3. init-method (from @Bean(initMethod="init"))
```

### Destruction Order
```
1. @PreDestroy
2. DisposableBean.destroy()
3. destroy-method (from @Bean(destroyMethod="cleanup"))
```

---

## Quick Reference

```java
// Bean Lifecycle Annotations
@PostConstruct   // Setup after DI
@PreDestroy      // Cleanup before destruction

// Interfaces (Avoid unless necessary)
InitializingBean  → afterPropertiesSet()
DisposableBean    → destroy()

// BeanPostProcessor (Modify beans after creation)
postProcessBeforeInitialization()  // Before @PostConstruct
postProcessAfterInitialization()   // After @PostConstruct

// BeanFactoryPostProcessor (Modify bean definitions before creation)
postProcessBeanFactory(ConfigurableListableBeanFactory factory)
```

---

## Summary

| Phase | Method/Interface | Recommended? |
|-------|-----------------|--------------|
| **Setup** | `@PostConstruct` | ✅ Yes |
| **Setup** | `InitializingBean` | ❌ No |
| **Setup** | `@Bean(initMethod)` | ⚠️ Sometimes |
| **Intercept** | `BeanPostProcessor` | ✅ For AOP/Proxies |
| **Modify Definitions** | `BeanFactoryPostProcessor` | ✅ For dynamic config |
| **Cleanup** | `@PreDestroy` | ✅ Yes |
| **Cleanup** | `DisposableBean` | ❌ No |
| **Cleanup** | `@Bean(destroyMethod)` | ⚠️ Sometimes |

### Key Points
- **Constructor** → **DI** → **@PostConstruct** → **Ready** → **@PreDestroy** → **Destroyed**
- Hamesha `@PostConstruct` aur `@PreDestroy` prefer karo (Java standard hain)
- `BeanPostProcessor` har bean ko modify/intercept kar sakta hai
- `BeanFactoryPostProcessor` bean definitions ko modify karta hai (before instantiation)

# IoC Container

## Status: Not Started

---

## Table of Contents

1. [Inversion of Control (IoC)](#inversion-of-control-ioc)
2. [BeanFactory vs ApplicationContext](#beanfactory-vs-applicationcontext)
3. [ApplicationContext Implementations](#applicationcontext-implementations)
4. [Container Lifecycle](#container-lifecycle)
5. [Bean Lifecycle Callbacks](#bean-lifecycle-callbacks)
6. [ApplicationContextAware](#applicationcontextaware)
7. [Bean Scopes](#bean-scopes)

---

## Inversion of Control (IoC)

**IoC kya hai?**
Normal programming mein aap khud objects create karte ho (`new Service()`). IoC mein yeh responsibility Spring ko de di jaati hai - **"Don't call us, we'll call you."**

### Without IoC (Tight Coupling)
```java
public class UserController {
    private UserService userService = new UserService();  // Khud create kiya
}
```
**Problem:** 
- `UserService` change karna ho to `UserController` bhi change karna padega
- Testing mushkil (mock object nahi daal sakte)

### With IoC (Loose Coupling)
```java
public class UserController {
    private UserService userService;  // Spring inject karega
    
    // Constructor Injection (Recommended)
    public UserController(UserService userService) {
        this.userService = userService;
    }
}
```

### Fayda
- Loose coupling - implementation change kar sakte ho bina class change kiye
- Easy testing - mock objects inject kar sakte ho
- Configuration centralize ho jaati hai

---

## BeanFactory vs ApplicationContext

Spring mein 2 types ke IoC containers hote hain:

| Feature | BeanFactory | ApplicationContext |
|---------|-------------|-------------------|
| **Type** | Basic container | Advanced container |
| **Loading** | Lazy (jab bean chahiye tab load) | Eager (startup pe sab beans load) |
| **Features** | Sirf DI | DI + AOP + i18n + Event Handling |
| **Performance** | Slow (lazy loading) | Fast (eager loading) |
| **Use Case** | Rarely used directly | **Production mein yahi use hota hai** |

```
ApplicationContext = BeanFactory + Extra Features
```

**Rule:** Hamesha `ApplicationContext` use karo. `BeanFactory` sirf tab jab memory bahut limited ho (mobile/legacy systems).

### BeanFactory Example
```java
Resource resource = new ClassPathResource("beans.xml");
BeanFactory factory = new XmlBeanFactory(resource);

// Lazy loading - bean tab create hoga jab getBean call hoga
UserService service = (UserService) factory.getBean("userService");
```

### ApplicationContext Example
```java
ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

// Eager loading - startup pe sab beans ready
UserService service = context.getBean(UserService.class);
```

---

## ApplicationContext Implementations

### 1. AnnotationConfigApplicationContext

Java annotations se configuration read karta hai. **Spring Boot internally yahi use karta hai.**

```java
@Configuration
@ComponentScan("com.example")
public class AppConfig { }

// Usage
ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
UserService service = context.getBean(UserService.class);
service.doWork();
context.close();
```

**Kab use karein:** Jab XML nahi, sirf annotations (`@Component`, `@Bean`, `@Configuration`) use kar rahe ho.

---

### 2. ClassPathXmlApplicationContext

XML files se bean definitions read karta hai.

```xml
<!-- beans.xml (classpath pe hona chahiye) -->
<beans>
    <bean id="userService" class="com.example.service.UserService"/>
    <bean id="userController" class="com.example.controller.UserController">
        <property name="userService" ref="userService"/>
    </bean>
</beans>
```

```java
// Usage
ApplicationContext context = new ClassPathXmlApplicationContext("beans.xml");
UserService service = context.getBean(UserService.class);
context.close();
```

**Kab use karein:** Legacy projects mein jab XML-based configuration ho. New projects mein annotations prefer karo.

---

### 3. Other Implementations (Reference)

| Implementation | Use Case |
|---------------|----------|
| `FileSystemXmlApplicationContext` | File system se XML read karta hai (classpath se nahi) |
| `WebApplicationContext` | Web apps ke liye (Spring MVC) |
| `AnnotationConfigWebApplicationContext` | Web apps + annotation-based config |

---

## Container Lifecycle

**Spring container kaise start hota hai:**

```
1. ApplicationContext create hota hai
       ↓
2. Bean definitions read hote hain (@Component, @Bean, XML)
       ↓
3. Bean instances create hote hain
       ↓
4. Dependency Injection hota hai (@Autowired)
       ↓
5. BeanPostProcessor run hote hain
       ↓
6. @PostConstruct methods run hote hain
       ↓
7. Application ready hai (beans use kar sakte ho)
       ↓
8. Application band hoti hai
       ↓
9. @PreDestroy methods run hote hain
       ↓
10. Beans destroy hote hain
```

---

## Bean Lifecycle Callbacks

```java
@Component
public class MyService {
    
    public MyService() {
        System.out.println("1. Constructor called");
    }
    
    @PostConstruct
    public void init() {
        System.out.println("2. Bean initialized - setup yahan karo");
        // Database connection, cache load, etc.
    }
    
    public void doWork() {
        System.out.println("3. Business logic");
    }
    
    @PreDestroy
    public void cleanup() {
        System.out.println("4. Bean destroyed - cleanup yahan karo");
        // Connection close, file close, etc.
    }
}
```

**Output:**
```
1. Constructor called
2. Bean initialized - setup yahan karo
3. Business logic
4. Bean destroyed - cleanup yahan karo
```

### Lifecycle Order
1. **Constructor** - Object create hota hai
2. **@PostConstruct** - Initialization ke baad run (setup ke liye)
3. **Business methods** - Actual kaam
4. **@PreDestroy** - Destruction se pehle run (cleanup ke liye)

---

## ApplicationContextAware

**Problem:** Kabhi-kabhi aapko manually beans chahiye hote hain container se, lekin aapke paas `ApplicationContext` reference nahi hai.

**Solution:** `ApplicationContextAware` implement karo - Spring automatically `ApplicationContext` inject kar dega.

```java
@Component
public class BeanHelper implements ApplicationContextAware {
    
    private static ApplicationContext context;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        context = applicationContext;
    }
    
    // Ab kahin se bhi bean get kar sakte ho
    public static <T> T getBean(Class<T> clazz) {
        return context.getBean(clazz);
    }
    
    public static String getProperty(String key) {
        return context.getEnvironment().getProperty(key);
    }
}
```

**Usage:**
```java
// Kahin bhi use kar sakte ho, even without @Autowired
UserService service = BeanHelper.getBean(UserService.class);
String dbUrl = BeanHelper.getProperty("spring.datasource.url");
```

### ⚠️ Warning

Yeh approach **avoid karo** jab tak zarurat na ho. Prefer karo `@Autowired` (Constructor Injection).

```java
// ✅ Recommended - Constructor Injection
@Component
public class MyComponent {
    private final UserService userService;
    
    public MyComponent(UserService userService) {
        this.userService = userService;
    }
}

// ❌ Avoid - ApplicationContextAware (unless absolutely necessary)
```

**Kab use karein:**
- Static utility classes jahan `@Autowired` kaam nahi karta
- Legacy code migration
- Dynamic bean access jahan bean type runtime pe decide hota hai

---

## Bean Scopes

| Scope | Matlab | Use Case |
|-------|--------|----------|
| **singleton** (default) | Ek hi instance pura container mein | Stateless services (DB calls, utils) |
| **prototype** | Har baar naya instance | Stateful beans (user session data) |
| **request** | Ek instance per HTTP request | Web apps - request-specific data |
| **session** | Ek instance per HTTP session | User session data |
| **application** | Ek instance per ServletContext | Global web app data |

### Singleton (Default)
```java
@Component
public class UserService { }  // Default: singleton

// Usage
UserService u1 = context.getBean(UserService.class);
UserService u2 = context.getBean(UserService.class);
System.out.println(u1 == u2);  // true (same instance)
```

### Prototype
```java
@Component
@Scope("prototype")  // Har baar naya object
public class ReportGenerator { }

// Usage
ReportGenerator r1 = context.getBean(ReportGenerator.class);
ReportGenerator r2 = context.getBean(ReportGenerator.class);
System.out.println(r1 == r2);  // false (different instances)
```

### Request/Session (Web Apps)
```java
@Component
@Scope("request")  // Har HTTP request ke liye naya instance
public class RequestData { }

@Component
@Scope("session")  // Har user session ke liye naya instance
public class UserPreferences { }
```

---

## Quick Reference

```java
// 1. Container create karna
ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

// 2. Bean get karna
MyService service = context.getBean(MyService.class);

// 3. Bean exist karta hai ya nahi check karna
boolean exists = context.containsBean("myService");

// 4. Bean definition names dekhna
String[] beans = context.getBeanDefinitionNames();

// 5. Property read karna
String value = context.getEnvironment().getProperty("app.name");

// 6. Container close karna
ConfigurableApplicationContext configurable = (ConfigurableApplicationContext) context;
configurable.close();
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **IoC** | Object creation Spring ko delegate karo |
| **BeanFactory** | Basic container (rarely used) |
| **ApplicationContext** | Advanced container (always use this) |
| **AnnotationConfigApplicationContext** | Annotations se config read karta hai |
| **ClassPathXmlApplicationContext** | XML se config read karta hai |
| **Lifecycle** | Constructor → @PostConstruct → Work → @PreDestroy |
| **ApplicationContextAware** | Manual bean access (avoid unless needed) |
| **Scopes** | singleton (default), prototype, request, session |

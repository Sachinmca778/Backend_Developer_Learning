# Spring Core - Top 10 Interview Questions & Answers

> Covering: Spring Overview, IoC Container, Dependency Injection, Bean Definition, Bean Lifecycle, Bean Scopes, Configuration, and Profiles

---

## Q1: Spring Framework kya hai aur Spring Boot se kaise alag hai?

**Answer:**

Spring Framework ek modular Java framework hai jo enterprise applications banane ke liye infrastructure support deta hai. Iske kaafi modules hain:

- **Core Container** - DI/IoC, Bean management
- **AOP** - Aspect-Oriented Programming (logging, security, transactions)
- **Data Access** - JDBC, ORM, Transactions
- **Web Layer** - MVC, WebFlux
- **Security** - Authentication & Authorization
- **Test** - Testing support

**Spring Framework (Traditional):**
- Manual configuration chahiye hota tha (XML ya Java-based)
- External server (Tomcat) chahiye deployment ke liye
- WAR file deploy karni padti thi
- Setup mein kaafi time lagta tha

**Spring Boot:**
```
Spring Boot = Spring Framework
            + Auto-configuration
            + Embedded Server (Tomcat/Jetty/Undertow)
            + Starter Dependencies
            + Production-ready Features (Actuator)
```

| Feature | Spring Framework | Spring Boot |
|---------|------------------|-------------|
| Configuration | Manual | Auto-configuration |
| Deployment | WAR → External Tomcat | JAR (embedded server) |
| Dependencies | Manual version manage | Starter POMs |

**Simple bhasha mein:** Spring Boot = Spring Framework ki easy version - sab kuch automatically configure ho jata hai.

---

## Q2: Inversion of Control (IoC) kya hai? BeanFactory vs ApplicationContext explain karo.

**Answer:**

**IoC (Inversion of Control)** ek principle hai jisme object create karne ki responsibility container ko de di jaati hai instead of manually banane ke.

**Without IoC (Tight Coupling):**
```java
public class UserController {
    private UserService userService = new UserService();  // Khud create kiya
}
```

**With IoC (Loose Coupling):**
```java
public class UserController {
    private UserService userService;  // Spring inject karega

    public UserController(UserService userService) {
        this.userService = userService;
    }
}
```

**BeanFactory vs ApplicationContext:**

| Feature | BeanFactory | ApplicationContext |
|---------|-------------|-------------------|
| Type | Basic container | Advanced container |
| Loading | Lazy (jab chahiye tab load) | Eager (startup pe sab load) |
| Features | Sirf DI | DI + AOP + i18n + Event Handling |
| Use Case | Rarely used | **Production mein yahi use karo** |

```java
// BeanFactory - lazy loading
BeanFactory factory = new XmlBeanFactory(resource);
UserService service = (UserService) factory.getBean("userService");  // Yahan create hoga

// ApplicationContext - eager loading
ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
// Startup pe sab beans ready
UserService service = context.getBean(UserService.class);
```

**Rule:** Hamesha `ApplicationContext` use karo. `BeanFactory` sirf tab jab memory bahut limited ho.

---

## Q3: Dependency Injection kitne types hain aur kaunsa recommended hai?

**Answer:**

**Dependency Injection (DI)** IoC ka implementation hai jahan Spring class ko dependencies provide (inject) karta hai.

### Three Types:

**1. Constructor Injection (✅ RECOMMENDED)**
```java
@Service
public class OrderService {
    private final PaymentProcessor processor;

    public OrderService(PaymentProcessor processor) {
        this.processor = processor;
    }
}
```

**2. Setter Injection (⚠️ Optional dependencies ke liye)**
```java
@Service
public class OrderService {
    private NotificationService notifier;

    @Autowired(required = false)
    public void setNotifier(NotificationService notifier) {
        this.notifier = notifier;
    }
}
```

**3. Field Injection (❌ AVOID)**
```java
@Service
public class OrderService {
    @Autowired
    private PaymentProcessor processor;  // Bad practice
}
```

### Constructor Injection hi kyun best hai:

| Reason | Explanation |
|--------|-------------|
| Immutability | Dependencies `final` ho sakte hain |
| Required dependencies clear | Constructor dekh ke pata chal jaata hai kya chahiye |
| Testing easy | Test mein directly mocks pass kar do |
| Circular dependency detect | Startup pe hi error aa jaata hai |
| Null safety | Bean kabhi partially initialized nahi ho sakta |

---

## Q4: Beans define karne ke kitne tarike hain aur kab kaunsa use karein?

**Answer:**

**Bean** woh object hai jo Spring container manage karta hai.

### Do Main Tarike:

**1. Stereotype Annotations (@Component, @Service, @Repository, @Controller)**
Apni classes ke liye use hota hai.

```java
@Component       // Generic bean - koi bhi class
public class EmailValidator { }

@Service         // Business logic layer
public class OrderService { }

@Repository      // Data access layer (+ exception translation)
public class UserRepository { }

@Controller      // Web layer - views return karta hai
public class UserController { }

@RestController  // Web layer - JSON/XML return karta hai
public class ApiController { }
```

**2. @Configuration + @Bean**
Third-party classes ke liye ya jab full control chahiye.

```java
@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate template = new RestTemplate();
        template.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        return template;  // Bean creation pe full control
    }
}
```

| Feature | @Component | @Bean |
|---------|-----------|-------|
| Kahan lagta hai | Class pe | Method pe |
| Control | Spring khud object banata hai | Aap manually object return karte ho |
| Use Case | Apni classes ke liye | Third-party classes ke liye |

### Multiple Beans resolve karna:

**@Primary** - Default bean set karta hai:
```java
@Component
@Primary  // Default yehi inject hoga
public class CreditCardProcessor implements PaymentProcessor { }
```

**@Qualifier** - Exact bean specify karta hai:
```java
public OrderService(
    @Qualifier("creditCard") PaymentProcessor credit,
    @Qualifier("debitCard") PaymentProcessor debit
) { }
```

---

## Q5: Spring mein Bean Lifecycle kaise kaam karta hai? Poora explain karo.

**Answer:**

Spring beans ko ek well-defined lifecycle mein manage karta hai:

```
1. Bean Instantiation (Constructor call hota hai)
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

**Complete Example:**
```java
@Component
public class LifecycleDemo {

    public LifecycleDemo() {
        System.out.println("1. Constructor called");
    }

    @Autowired
    public void setDependency(Dependency dep) {
        System.out.println("2. Dependency injected");
    }

    @PostConstruct
    public void init() {
        System.out.println("3. @PostConstruct - Setup complete");
    }

    @PreDestroy
    public void cleanup() {
        System.out.println("4. @PreDestroy - Cleanup");
    }
}
```

**Output:**
```
1. Constructor called
2. Dependency injected
3. @PostConstruct - Setup complete
... (application running) ...
4. @PreDestroy - Cleanup
```

### BeanPostProcessor:
Har bean ko initialization phase mein intercept karta hai. Spring internally `@Transactional`, `@Async` etc. ke liye yahi use karta hai.

```java
@Component
public class LoggingPostProcessor implements BeanPostProcessor {

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

### BeanFactoryPostProcessor:
Bean create hone se **pehle** bean definitions ko modify karta hai. `${...}` property resolution ke liye internally yahi use hota hai.

---

## Q6: Spring mein Bean Scopes kya hain? Har ek ko explain karo.

**Answer:**

**Scope** define karta hai ki kitne bean instances banenge aur kitni der tak accessible rahenge.

### Core Scopes:

**1. Singleton (Default)**
Container mein sirf **ek hi instance** hoga.
```java
@Component  // Default: singleton
public class UserService { }

UserService s1 = context.getBean(UserService.class);
UserService s2 = context.getBean(UserService.class);
System.out.println(s1 == s2);  // true (same instance)
```
**Kab use karein:** Stateless services (90% cases)
**Warning:** Agar mutable state hai to thread-safe nahi hoga automatically.

**2. Prototype**
Har baar `getBean()` call pe **naya instance** milega.
```java
@Component
@Scope("prototype")
public class ReportGenerator { }

ReportGenerator r1 = context.getBean(ReportGenerator.class);
ReportGenerator r2 = context.getBean(ReportGenerator.class);
System.out.println(r1 == r2);  // false (different instances)
```
**Kab use karein:** Stateful beans
**Note:** Prototype beans ke liye `@PreDestroy` automatically call NAHI hota.

### Web Scopes (web app chahiye):

| Scope | Instance | Use Case |
|-------|----------|----------|
| **request** | 1 per HTTP request | Request-specific data |
| **session** | 1 per HTTP session | User session, shopping cart |
| **application** | 1 per ServletContext | Global web app data |

```java
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION,
       proxyMode = ScopedProxyMode.TARGET_CLASS_PROXY)
public class UserSession {
    private List<String> cartItems = new ArrayList<>();
}
```

---

## Q7: Spring Boot mein Auto-Configuration kaise kaam karta hai?

**Answer:**

`@SpringBootApplication` teen annotations ka combination hai:
1. `@Configuration` - Yeh class ek configuration class hai
2. `@EnableAutoConfiguration` - Auto-configuration magic enable karta hai
3. `@ComponentScan` - `@Component`, `@Service`, `@Repository` classes scan karta hai

### Conditional Annotations - Asli Magic:

| Annotation | Matlab |
|------------|--------|
| `@ConditionalOnClass` | Agar yeh class classpath pe hai to configure karo |
| `@ConditionalOnMissingClass` | Agar yeh class nahi hai to configure karo |
| `@ConditionalOnBean` | Agar yeh bean already exist karta hai |
| `@ConditionalOnMissingBean` | Agar yeh bean nahi hai to configure karo |
| `@ConditionalOnProperty` | Agar yeh property set hai |

**Example:**
```java
@Configuration
@ConditionalOnClass(DataSource.class)           // Agar DataSource classpath pe hai
@ConditionalOnMissingBean(DataSource.class)     // Aur user ne khud DataSource nahi banaya
public class DataSourceAutoConfiguration {
    // Spring Boot automatically DataSource bean bana dega
    // using properties from application.properties
}
```

**Kaise kaam karta hai:**
1. Aapne `spring-boot-starter-data-jpa` add kiya
2. `DataSource` class classpath pe aa gayi
3. Aapne `application.properties` mein DB credentials diye
4. Spring Boot automatically DataSource bean bana diya

**Auto-configuration override kaise karein:**
```java
// Method 1: Khud ka bean bana do
@Bean
public DataSource dataSource() {
    return new CustomDataSource();  // Auto-config back off ho jayega
}

// Method 2: Exclude kar do
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
```

---

## Q8: Spring Boot mein configuration ko externalize kaise karte hain?

**Answer:**

### Three Main Approaches:

**1. @Value (1-2 simple properties ke liye)**
```java
@Component
public class AppInfo {

    @Value("${app.name}")
    private String appName;

    @Value("${app.name:DefaultApp}")  // Default value ke saath
    private String appNameWithDefault;

    @Value("#{systemProperties['user.home']}")  // SpEL
    private String userHome;
}
```

**2. @ConfigurationProperties (Type-Safe - ✅ RECOMMENDED for many properties)**
```java
@ConfigurationProperties(prefix = "app")
@Component
public class AppProperties {
    private String name;
    private String version;
    private Feature feature = new Feature();

    // Getters aur setters zaruri hain
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public static class Feature {
        private boolean enabled;
        // Getters and setters
    }
}
```

**Properties file:**
```properties
app.name=MyApp
app.version=1.0.0
app.feature.enabled=true
```

| Feature | @Value | @ConfigurationProperties |
|---------|--------|--------------------------|
| Type-Safe | ❌ No | ✅ Yes |
| Validation | ❌ No | ✅ Yes |
| IDE Autocomplete | ❌ No | ✅ Yes |
| Best For | Kam properties | Zyada related properties |

**Rule:** `@ConfigurationProperties` use karo jab 3+ related properties hon. `@Value` sirf 1-2 values ke liye.

**3. Environment Interface (Programmatic access)**
```java
@Component
public class ConfigService {
    private final Environment env;

    public ConfigService(Environment env) {
        this.env = env;
    }

    public String getAppName() {
        return env.getProperty("app.name", "DefaultName");
    }
}
```

---

## Q9: Spring Profiles kya hain aur kaise use karte hain?

**Answer:**

**Profiles** se aap specific environments (dev, test, prod) ke liye beans ya configurations ko enable/disable kar sakte ho.

### @Profile Annotation:
```java
@Component
@Profile("dev")  // Sirf dev profile mein active
public class DevLogger implements Logger {
    public void log(String msg) {
        System.out.println("DEV LOG: " + msg);
    }
}

@Component
@Profile("prod")  // Sirf prod profile mein active
public class ProdLogger implements Logger {
    public void log(String msg) {
        LoggerFactory.getLogger(getClass()).info(msg);
    }
}
```

### Multiple Profiles:
```java
@Component
@Profile({"dev", "test"})  // Dono mein active hoga
public class TestDataService { }

@Component
@Profile("!prod")  // Prod ke alawa sab mein active
public class DebugService { }
```

### Profiles activate karne ke tarike:

```properties
# Method 1: application.properties
spring.profiles.active=dev
```

```bash
# Method 2: Command line
java -jar app.jar --spring.profiles.active=prod

# Method 3: Environment variable
export SPRING_PROFILES_ACTIVE=prod
```

### Profile-specific Config Files:
```
src/main/resources/
├── application.yml              # Common config (sab ke liye)
├── application-dev.yml          # Development specific
├── application-prod.yml         # Production specific
└── application-test.yml         # Testing specific
```

### Testing mein Profiles:
```java
@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {
    @Test
    void testCreateUser() {
        // Test profile ke beans use honge
    }
}
```

---

## Q10: Conditional Annotations kya hain aur kitne types hain?

**Answer:**

**Conditional annotations** beans ko sirf tab create karte hain jab certain conditions satisfy hon. Yeh Spring Boot ki auto-configuration ki foundation hai.

### Most Common Types:

**1. @ConditionalOnProperty** - Property value ke basis pe
```java
@Bean
@ConditionalOnProperty(name = "app.feature.email.enabled",
                       havingValue = "true",
                       matchIfMissing = false)
public EmailService emailService() {
    return new EmailService();
}
```

**2. @ConditionalOnClass** - Agar class classpath pe hai
```java
@Bean
@ConditionalOnClass(RedisTemplate.class)
public RedisTemplate<String, Object> redisTemplate() {
    return new RedisTemplate<>();
}
```

**3. @ConditionalOnMissingBean** - Agar bean exist nahi karta
```java
@Bean
@ConditionalOnMissingBean(DataSource.class)
public DataSource defaultDataSource() {
    return DataSourceBuilder.create()
        .url("jdbc:h2:mem:testdb")
        .build();
}
```

**4. @ConditionalOnExpression** - SpEL expression
```java
@Bean
@ConditionalOnExpression("${app.feature.advanced.enabled} && ${app.environment} == 'prod'")
public AdvancedService advancedService() {
    return new AdvancedService();
}
```

### @Profile vs @ConditionalOnProperty:

| Feature | @Profile | @ConditionalOnProperty |
|---------|----------|------------------------|
| Granularity | Coarse (entire environment) | Fine-grained (per bean) |
| Use Case | Dev/Test/Prod separation | Feature flags |

**Dono ko saath use karna:**
```java
@Component
@Profile("prod")
@ConditionalOnProperty(name = "app.feature.monitoring", havingValue = "true")
public class MonitoringService {
    // Sirf production mein, aur sirf jab monitoring enabled ho
}
```

---

## Quick Reference Summary

```java
// Annotations
@Component, @Service, @Repository, @Controller, @RestController
@Configuration, @Bean
@Autowired, @Qualifier, @Primary
@PostConstruct, @PreDestroy
@Scope("singleton" | "prototype" | "request" | "session")
@Profile("dev"), @ActiveProfiles("test")
@ConditionalOnProperty, @ConditionalOnClass, @ConditionalOnMissingBean

// Configuration
@Value("${app.name}")
@Value("${app.name:default}")
@ConfigurationProperties(prefix = "app")
Environment env.getProperty("key")

// Component Scanning
@SpringBootApplication  // Auto-includes @ComponentScan
@ComponentScan("com.example")

// Container
ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
MyService service = context.getBean(MyService.class);
```

---

## Yaad Rakhne Wali Baatein

1. **Constructor Injection** hi use karo - Setter/Field Injection avoid karo
2. **Singleton default scope hai** - mutable state mein careful raho
3. **@Configuration + @Bean** third-party classes ke liye use karo
4. **@PostConstruct aur @PreDestroy** lifecycle callbacks ke liye
5. **@ConfigurationProperties** @Value se behtar hai zyada properties ke liye
6. **ApplicationContext = BeanFactory + Extra Features** (hamesha yahi use karo)
7. **Spring Boot auto-config** conditional annotations pe kaam karta hai
8. **@Primary default set karta hai**, **@Qualifier exact bean specify karta hai**
9. **BeanPostProcessor** beans ko intercept karta hai, AOP ke liye use hota hai
10. **Profiles** dev/test/prod configurations alag rakhte hain

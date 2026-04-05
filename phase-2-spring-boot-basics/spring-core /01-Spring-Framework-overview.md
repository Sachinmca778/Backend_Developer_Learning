# Spring Framework Overview

## Status: Not Started

---

## Table of Contents

1. [Spring Modules](#spring-modules)
2. [Spring Boot vs Spring Framework](#spring-boot-vs-spring-framework)
3. [Auto-configuration Magic](#auto-configuration-magic)
4. [Starter Dependencies](#starter-dependencies)
5. [Embedded Servers](#embedded-servers)

---

## Spring Modules

Spring Framework ek modular architecture hai - aapko sirf wahi modules use karne hote hain jo aapko chahiye.

### 1. Core Container

Sabse important layer - yehi Spring ki foundation hai.

#### Spring Core
- **Dependency Injection (DI)** / Inversion of Control (IoC)
  - *Matlab:* Objects ko manually create karne ki jagah, Spring automatically inject kar deta hai
- `BeanFactory` aur `ApplicationContext` - beans manage karta hai
- Bean scopes: singleton (default), prototype, request, session

#### Spring Beans
- Bean definition aur lifecycle management
- `@PostConstruct` (bean create hone ke baad run)
- `@PreDestroy` (bean destroy hone se pehle run)

#### Spring Context
- i18n support (multi-language apps ke liye)
- Event handling
- Property resolution (`.properties` ya `.yml` files se values read karna)

#### Spring Expression Language (SpEL)
- Runtime pe expressions evaluate karta hai
- Example: `@Value("#{systemProperties['user.name']}")`

---

### 2. AOP (Aspect-Oriented Programming)

**AOP kya hai?** Cross-cutting concerns ko alag se handle karna - jaise logging, security, transaction management jo har layer mein chahiye hota hai.

- Proxy-based AOP framework
- **Advice types:**
  - **Before:** Method call se pehle run hoga
  - **After:** Method call ke baad run hoga (chahe exception aaye ya na aaye)
  - **After-returning:** Sirf successful return pe
  - **After-throwing:** Sirf exception aane pe
  - **Around:** Method ke dono taraf (before + after) - sabse powerful

- `@Aspect`, `@Before`, `@After`, `@Around` annotations use hote hain

**Example:**
```java
@Aspect
@Component
public class LoggingAspect {
    
    @Before("execution(* com.example.service.*.*(..))")
    public void logBefore() {
        System.out.println("Method called");
    }
}
```

---

### 3. Data Access/Integration

#### Spring JDBC
- `JdbcTemplate` - boilerplate JDBC code eliminate karta hai
- Manual connection close karne ki zarurat nahi

```java
jdbcTemplate.query("SELECT * FROM users", (rs, row) -> new User(rs.getString("name")));
```

#### Spring ORM
- Hibernate, JPA integration
- `@Transactional` - manually transaction manage nahi karna padta

#### Spring Transactions
- Declarative: `@Transactional` annotation se kaam ho jata hai
- **Propagation behaviors:**
  - `REQUIRED` (default): Existing transaction use karo, nahi to naya banao
  - `REQUIRES_NEW`: Hamesha naya transaction banao
  - `SUPPORTS`: Transaction hai to use karo, nahi to bina transaction ke chalao

---

### 4. Web Layer

#### Spring Web MVC
- `@Controller` - views return karta hai (JSP, Thymeleaf)
- `@RestController` - JSON/XML response return karta hai (APIs ke liye)
- `@RequestMapping`, `@GetMapping`, `@PostMapping` - routes define karne ke liye

```java
@RestController
public class UserController {
    
    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
}
```

#### Spring WebFlux
- Reactive programming model (non-blocking)
- High concurrency scenarios ke liye

---

### 5. Security

- **Authentication:** User kaun hai? (login)
- **Authorization:** User kya kar sakta hai? (roles/permissions)
- `@PreAuthorize("hasRole('ADMIN')")` - method level security
- CSRF protection, JWT, OAuth2 support

```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long id) {
    // sirf admin delete kar sakta hai
}
```

---

### 6. Test

- `@SpringBootTest` - full application context load karta hai
- `@WebMvcTest` - sirf web layer test karta hai
- `@DataJpaTest` - sirf repository layer test karta hai
- `MockMvc` - HTTP requests mock karne ke liye

```java
@SpringBootTest
class ApplicationTests {
    
    @Test
    void contextLoads() {
        // application start ho rahi hai ya nahi
    }
}
```

---

### Module Dependencies (Top to Bottom)

```
Spring Test
    ↓
Spring Security
    ↓
Spring Web MVC / WebFlux
    ↓
Spring AOP
    ↓
Spring Data Access
    ↓
Spring Context
    ↓
Spring Beans
    ↓
Spring Core (Foundation)
```

---

## Spring Boot vs Spring Framework

### Spring Framework (Traditional)

**Problem:** Bahut zyada configuration chahiye hota tha.

```java
// Har cheez manually configure karni padti thi
@Configuration
@EnableWebMvc
@ComponentScan("com.example")
public class WebConfig implements WebMvcConfigurer {
    
    @Bean
    public ViewResolver viewResolver() {
        // manual configuration
    }
    
    @Bean
    public DataSource dataSource() {
        // URL, username, password - sab manually dena padta
    }
}
```

**Deployment:**
1. WAR file build karo
2. External Tomcat mein deploy karo
3. Tomcat start karo

---

### Spring Boot

**Solution:** Auto-configuration + embedded server = zero configuration (almost).

```java
@SpringBootApplication  // Bas ek annotation!
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@RestController
public class HelloController {
    @GetMapping("/hello")
    public String hello() {
        return "Hello!";
    }
}
```

```properties
# application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=pass
```

**Deployment:**
1. JAR file build karo
2. `java -jar myapp.jar` run karo
3. Done! Server automatically start ho jata hai

---

### Comparison Table

| Feature | Spring Framework | Spring Boot |
|---------|------------------|-------------|
| Configuration | Manual (XML/Java) | Auto-configuration |
| Deployment | WAR → External Tomcat | JAR (embedded server) |
| Setup Time | Zyada | Bahut kam |
| Dependencies | Manual version manage | Starter POMs (auto-managed) |
| Production Ready | Extra setup chahiye | Actuator built-in |

---

### Key Formula

```
Spring Boot = Spring Framework
            + Auto-configuration
            + Embedded Server (Tomcat/Jetty/Undertow)
            + Starter Dependencies
            + Production-ready Features (Actuator)
            + Externalized Configuration
```

**Simple bhasha mein:** Spring Boot = Spring Framework ki easy version - sab kuch automatically configure ho jata hai.

---

## Auto-configuration Magic

### Yeh Kaise Kaam Karta Hai?

`@SpringBootApplication` teen annotations ka combination hai:

1. **`@Configuration`** - Yeh class ek configuration class hai
2. **`@EnableAutoConfiguration`** - Spring Boot ko bolta hai: "Mujhe auto-configure karo"
3. **`@ComponentScan`** - `@Component`, `@Service`, `@Repository` classes ko scan karta hai

---

### Conditional Annotations - The Real Magic

Auto-configuration sirf tabhi hota hai jab conditions satisfy hon:

| Annotation | Matlab |
|------------|--------|
| `@ConditionalOnClass` | Agar yeh class classpath pe hai, to configure karo |
| `@ConditionalOnMissingClass` | Agar yeh class nahi hai, to configure karo |
| `@ConditionalOnBean` | Agar yeh bean already exist karta hai |
| `@ConditionalOnMissingBean` | Agar yeh bean nahi hai, to configure karo |
| `@ConditionalOnProperty` | Agar yeh property set hai |

---

### Example: DataSource Auto-configuration

```java
@Configuration
@ConditionalOnClass(DataSource.class)           // Agar DataSource classpath pe hai
@ConditionalOnMissingBean(DataSource.class)     // Aur user ne khud DataSource nahi banaya
public class DataSourceAutoConfiguration {
    
    // Toh Spring Boot automatically DataSource bean bana dega
    // using properties from application.properties
}
```

**Samjho:**
- Aapne `spring-boot-starter-data-jpa` add kiya
- Classpath pe `DataSource` class aa gayi
- Aapne `application.properties` mein DB URL, username, password diya
- Spring Boot ne automatically DataSource bean bana diya
- **Aapko kuch manually configure nahi karna pada!**

---

### Common Auto-configurations

| Starter | What it Auto-configures |
|---------|------------------------|
| `spring-boot-starter-web` | DispatcherServlet, Jackson (JSON), Embedded Tomcat |
| `spring-boot-starter-data-jpa` | DataSource, EntityManager, TransactionManager |
| `spring-boot-starter-security` | Authentication, CSRF, Default login page |
| `spring-boot-starter-thymeleaf` | Template engine, view resolver |

---

### Debug Kaise Karein?

```properties
# application.properties
debug=true
```

Ya run karo:
```bash
java -jar myapp.jar --debug
```

Yeh **Condition Evaluation Report** dikhata hai:
- ✅ Kya configure hua (positive matches)
- ❌ Kya configure nahi hua aur kyun (negative matches)

---

### Auto-configuration Override Kaise Karein?

**Method 1: Khud ka Bean bana do**
```java
@Configuration
public class CustomConfig {
    
    @Bean
    public DataSource dataSource() {
        return new CustomDataSource();  // Auto-configuration back off ho jayega
    }
}
```

**Method 2: Exclude kar do**
```java
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    SecurityAutoConfiguration.class
})
public class Application { }
```

**Method 3: Properties file mein**
```properties
spring.autoconfigure.exclude=\
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

---

## Starter Dependencies

### Starter Kya Hai?

**Problem:** Traditional Spring mein har dependency manually add karna padta tha with correct versions.

**Solution:** Starters - ek dependency daalo, sab transitive dependencies automatically aa jayenge with compatible versions.

---

### Without Starter vs With Starter

```xml
<!-- WITHOUT Starter (bahut zyada kaam) -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-web</artifactId>
    <version>5.3.20</version>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-webmvc</artifactId>
    <version>5.3.20</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.13.3</version>
</dependency>
<!-- ... aur bhi bahut kuch -->

<!-- WITH Starter (bas ek line!) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

**Version management?** `spring-boot-starter-parent` sab versions automatically manage kar deta hai.

---

### Most Common Starters

| Starter | Use Case |
|---------|----------|
| `spring-boot-starter` | Core starter (logging, YAML, auto-config) |
| `spring-boot-starter-web` | REST APIs, web applications |
| `spring-boot-starter-data-jpa` | Database with Hibernate/JPA |
| `spring-boot-starter-data-jdbc` | Simple JDBC without JPA |
| `spring-boot-starter-security` | Authentication & Authorization |
| `spring-boot-starter-test` | Testing (JUnit, Mockito, MockMvc) |
| `spring-boot-starter-actuator` | Production monitoring (health, metrics) |

---

### Database Starters

| Starter | Use Case |
|---------|----------|
| `spring-boot-starter-data-redis` | Redis cache |
| `spring-boot-starter-data-mongodb` | MongoDB |
| `spring-boot-starter-data-elasticsearch` | Elasticsearch |

---

### Common Combinations

**REST API:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

**Secured App:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

**Microservice:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

---

### Dependencies Exclude Kaise Karein

```xml
<!-- Tomcat hatao, Jetty lagao -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jetty</artifactId>
</dependency>
```

---

## Embedded Servers

### Embedded Server Kya Hai?

**Traditional:** WAR file banao → External Tomcat mein deploy karo

**Spring Boot:** JAR file mein server already bundled hai → `java -jar` run karo → Done!

**Fayda:** Alag se server install/configure karne ki zarurat nahi.

---

### Three Supported Servers

| Server | Default? | Best For |
|--------|----------|----------|
| **Tomcat** | ✅ Yes | General purpose (90% apps ke liye yahi kaafi hai) |
| **Jetty** | ❌ No | WebSocket, long-lived connections |
| **Undertow** | ❌ No | High performance, low memory |

---

### 1. Tomcat (Default)

`spring-boot-starter-web` add karte hi automatically Tomcat aa jata hai.

```properties
# application.properties
server.port=8081
server.servlet.context-path=/api
server.tomcat.threads.max=200
```

**Kab use karein:** Standard web apps, REST APIs - yehi default rakhna.

---

### 2. Jetty

Long connections (WebSocket, SSE) ke liye better hai. Memory bhi kam leta hai.

```xml
<!-- Tomcat hatao -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- Jetty lagao -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jetty</artifactId>
</dependency>
```

**Kab use karein:** Chat apps, real-time notifications, WebSocket applications.

---

### 3. Undertow

Sabse fast aur sabse kam memory leta hai.

```xml
<!-- Tomcat hatao -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- Undertow lagao -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-undertow</artifactId>
</dependency>
```

**Kab use karein:** High-throughput microservices, jahan performance critical ho.

---

### Comparison

| Feature | Tomcat | Jetty | Undertow |
|---------|--------|-------|----------|
| Performance | Good | Good | Excellent |
| Memory | Moderate | Low | Lowest |
| WebSocket | ✅ | ✅ Excellent | ✅ |
| JSP Support | ✅ | ✅ | ❌ |
| Default? | ✅ Yes | No | No |

**Rule of thumb:** Default Tomcat rakhna jab tak koi specific need nahi hai.

---

### SSL/HTTPS Configuration

```properties
server.port=8443
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=tomcat
```

---

### Troubleshooting

**Port already in use error?**
```properties
# Random port use karo (testing ke liye)
server.port=0
```

**Check which port is running:**
```java
@Component
public class ServerInfo implements ApplicationListener<WebServerInitializedEvent> {
    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        System.out.println("Server running on port: " + event.getWebServer().getPort());
    }
}
```

---

## Final Summary

```
Spring Boot = Spring Framework + auto-config + embedded server + production-ready features
```

**Yaad rakho:**
- **Spring Modules** = Framework ke building blocks (Core, AOP, Data, Web, Security, Test)
- **Spring Boot** = Spring Framework ka easy version (no manual configuration)
- **Auto-configuration** = Classpath dekh ke automatically beans configure karta hai
- **Starters** = Pre-packaged dependencies (ek line mein sab kuch)
- **Embedded Servers** = App ke andar server bundled (no external Tomcat needed)

---
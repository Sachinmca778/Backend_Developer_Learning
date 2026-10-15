# Level 2: Spring Boot Intermediate - Config, DI & Annotations (Q102 - Q126)

---

### Q102, Q110 & Q123: Auto-Configuration Internals & Overriding
**Answer (Hinglish):**

- **Auto-Configuration Kaise Kaam Karta Hai:**  
  Jab application start hoti hai, `@EnableAutoConfiguration` internally classpath me `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (Spring Boot 3+) ya `spring.factories` (Spring Boot 2.x) file scan karta hai.
  - Har auto-configuration class par **Conditional Annotations** lage hote hain:
    - `@ConditionalOnClass(DataSource.class)`: Agar classpath me yeh class hai tabhi execute hoga.
    - `@ConditionalOnMissingBean(DataSource.class)`: Agar developer ne apna custom bean define NAHI kiya hai, tabhi default bean banega.
    - `@ConditionalOnProperty(name = "feature.enabled", havingValue = "true")`.

- **Auto-Configuration Override Kaise Karein:**
  1. **Exclude via Annotation:**
     ```java
     @SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
     ```
  2. **Exclude via properties file:**
     ```properties
     spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
     ```
  3. **Custom Bean Define Karke:** Sirf apni class me custom `@Bean` declare kar do. Spring Boot ka `@ConditionalOnMissingBean` use dekh kar apna default skip kar dega!

---

### Q103 & Q124: Role of @Configuration and @Bean
**Answer (Hinglish):**

- **`@Configuration`:** Class-level annotation jo Spring ko batata hai ki yeh class Spring beans produce karne ke liye definitions provide karti hai. (CGLIB proxy ke through method caching maintain karti hai taaki beans hamesha singleton rahein).
- **`@Bean`:** Method-level annotation jo method ke return value ko Spring IoC container me ek managed bean ke taur par register karta hai.
- **Kahan Use Hota Hai:** Jab aap third-party library classes (jaise `RestTemplate`, `ObjectMapper`, `ModelMapper`, `AmazonS3`) ko bean banana chahte ho jinke source code par aap `@Component` nahi likh sakte.

```java
@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

---

### Q104: How do Profiles work in Spring Boot (@Profile)?
**Answer (Hinglish):**

Profiles different environments (Dev, QA, Staging, Prod) ke liye configurations aur beans ko segregate karne ke kaam aati hain.

1. **Profile-specific Property Files:**
   - `application-dev.properties` (Local MySQL DB, debug logging)
   - `application-prod.properties` (AWS RDS Aurora, minimal error logging)
2. **Active Profile Set Karna:**
   ```properties
   spring.profiles.active=dev
   ```
   Command line: `java -jar app.jar --spring.profiles.active=prod`
3. **Bean-level Profile:**
   ```java
   @Bean
   @Profile("dev")
   public EmailService mockEmailService() { return new MockEmailService(); }

   @Bean
   @Profile("prod")
   public EmailService awsSesEmailService() { return new AwsSesEmailService(); }
   ```

---

### Q105: What is @Transactional, and what's the benefit?
**Answer (Hinglish):**

- **Definition:** `@Transactional` Spring ka declarative transaction management annotation hai jo AOP proxy ke through methods par lagta hai.
- **Benefits & Internals:**
  1. Method enter hone par automatic DB transaction start (`BEGIN TRANSACTION`) karta hai.
  2. Agar method bina exception ke successfully complete hua, to automatic `COMMIT` karta hai.
  3. Agar koi **`RuntimeException` ya `Error`** aayi, to automatic **`ROLLBACK`** kar deta hai taaki data inconsistent na ho.
- **Key Parameters:**
  - `propagation = Propagation.REQUIRED` (Default: Existing transaction join karo ya nayi banao).
  - `isolation = Isolation.READ_COMMITTED`.
  - `rollbackFor = Exception.class` (Checked exceptions ke liye bhi rollback enforce karne ke liye).

---

### Q106 & Q122: Global Exception Handling (@RestControllerAdvice vs @ExceptionHandler)
**Answer (Hinglish):**

- `@ExceptionHandler`: Specific controller ke andar specific exception ko handle karne ke liye method-level annotation.
- `@ControllerAdvice / @RestControllerAdvice`: Pure application ke sabhi controllers ke liye centralized, cross-cutting exception interception provide karta hai.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        Map<String, Object> body = Map.of(
            "timestamp", LocalDateTime.now(),
            "status", HttpStatus.NOT_FOUND.value(),
            "error", ex.getMessage()
        );
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }
}
```

---

### Q107: Difference between application.properties and application.yml?
**Answer (Hinglish):**

| Feature | `application.properties` | `application.yml` (YAML) |
| :--- | :--- | :--- |
| **Structure** | Flat Key-Value pairs with dot notation (`server.port=8080`). | Hierarchical tree structure with indentation. |
| **Readability** | Repeated prefixes (`spring.datasource.*` bar-bar likhna padta hai). | Clean, DRY (Don't Repeat Yourself), easily readable. |
| **Lists & Maps** | Comma-separated ya indexed (`my.list[0]=a`). | Native dash `- item` syntax. |
| **Multi-Profile** | Multiple files banani padti hain (`application-dev.properties`). | Single file me multiple profiles documents `---` separator se define ho sakte hain. |

---

### Q108: CommandLineRunner vs ApplicationRunner
**Answer (Hinglish):**

Dono interfaces application start hone aur `SpringApplication.run()` complete hone ke baad koi startup code (jaise DB seeding, cache warmup) run karne ke liye use hote hain:
- `CommandLineRunner`: Raw `String[] args` pass karta hai (`run(String... args)`).
- `ApplicationRunner`: Structured `ApplicationArguments` wrapper pass karta hai (`run(ApplicationArguments args)`), jisse optional parameters parse karna easy hota hai (`args.getOptionValues("env")`).

---

### Q109: How does the Embedded Server (Tomcat) work in Spring Boot?
**Answer (Hinglish):**

1. Jab `spring-boot-starter-web` classpath me hota hai, to Spring Boot `TomcatServletWebServerFactory` ko invoke karta hai.
2. Spring Boot application context initialize hote waqt embedded Tomcat server instance ko programmatically create karta hai (`new Tomcat()`), port bind karta hai (default 8080), aur Spring ka `DispatcherServlet` register karta hai.
3. Server background thread par start hota hai bina kisi external deployment step ke.

---

### Q111: How do you handle Circular Dependencies in Spring Boot?
**Answer (Hinglish):**

- **Circular Dependency:** Class A requires Class B, and Class B requires Class A (`A -> B -> A`). Spring startup par `BeanCurrentlyInCreationException` deta hai.
- **Resolution Techniques:**
  1. **Redesign Architecture (Best Way):** Code refactor karke common logic ko ek teesri class `Class C` me nikal lo (`A -> C` and `B -> C`).
  2. **Use `@Lazy` Annotation:** Ek dependency ko lazy initialize hone do taaki pehle doosri bean fully ban sake:
     ```java
     public AService(@Lazy BService bService) { this.bService = bService; }
     ```
  3. **Use Setter Injection instead of Constructor Injection.**
  4. **Property flag (Discouraged):** `spring.main.allow-circular-references=true`.

---

### Q112 - Q117: Dependency Injection Patterns (@Primary vs @Qualifier)
**Answer (Hinglish):**

#### Types of Injection:
1. **Constructor Injection (Recommended):** Immutability guarantee karta hai (`final` fields), test cases me mock karna easy hota hai, aur circular dependency early detect ho jati hai.
2. **Setter Injection:** Optional dependencies ke liye suitable hai.
3. **Field Injection (`@Autowired` on field):** Discouraged because unit testing me bina reflection ke mock inject nahi ho pata aur immutability break hoti hai.

#### Multiple Beans Conflict:
Jab ek hi interface ke 2 implementation beans hon (e.g. `SmsService` aur `EmailService` both implement `NotificationService`):
- **`@Primary`:** Default choice specify karta hai jab koi specific name na diya ho.
- **`@Qualifier("beanName")`:** Specific bean ko explicitly target karta hai by name.
- **Precedence Rule:** Agar dono use hon, to **`@Qualifier` hamesha jeetega (Highest Precedence)**, kyunki `@Qualifier` specific name-based target karta hai jabki `@Primary` sirf fallback default hota hai.

---

### Q118 - Q121: REST Annotations Comparison
**Answer (Hinglish):**

- **`@PathVariable` vs `@RequestParam`:**
  - `@PathVariable`: URI path ka part hota hai (`/users/{id}` -> `/users/101`). Resource identity ke liye.
  - `@RequestParam`: Query string parameter hota hai (`/users?role=admin&page=1`). Filtering, sorting, pagination ke liye.
- **PUT vs PATCH:**
  - `PUT`: Complete resource replacement (Saari fields update karni hoti hain, missing fields null ho sakti hain).
  - `PATCH`: Partial modification (Sirf wahi fields update hoti hain jo payload me pass ki gayi hon).

---

### Q125 & Q126: @Async, @Scheduled, @Cacheable & @CacheEvict
**Answer (Hinglish):**

- **`@Async`:** Method ko background thread pool me asynchronously run karta hai (requires `@EnableAsync`).
- **`@Scheduled(cron = "0 0 * * * *")`:** Method ko periodic interval ya cron expression par automatically execute karta hai (requires `@EnableScheduling`).
- **`@Cacheable(value = "users", key = "#id")`:** Method ka result cache me store karta hai; next time same argument aane par method execute kiye bina cache se direct return karta hai.
- **`@CacheEvict(value = "users", key = "#id")`:** Jab DB me data update ya delete ho, to stale cache entry ko invalidate/remove karta hai.

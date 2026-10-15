# Level 1: Spring Boot & REST API Basics (Q42 - Q57)

---

### Q42 & Q50: What is Spring Boot? Difference between Spring and Spring Boot?
**Answer (Hinglish):**

- **Spring Boot Kya Hai:**  
  Spring Boot ek open-source, microservices-ready Java-based framework hai jo standard Spring Framework ke upar banaya gaya hai. Iska main objective boilerplate XML configuration ko khatam karke **"Production-ready apps quickly develop"** karna hai.

| Criteria | Traditional Spring Framework | Spring Boot |
| :--- | :--- | :--- |
| **Philosophy** | Powerful framework but lots of manual setup & configuration. | **Convention over Configuration** (Out-of-the-box defaults). |
| **Configuration** | Heavy XML config (`applicationContext.xml`) ya manual `@Configuration` classes. | **Auto-Configuration** (`@EnableAutoConfiguration`). |
| **Server Deployment**| External Tomcat / JBoss server install karke WAR file deploy karni padti thi. | **Embedded Server** (Tomcat, Jetty) pre-packaged hota hai. Direct executable JAR banti hai (`java -jar app.jar`). |
| **Dependency Mgmt** | Har dependency ka specific compatible version manually manage karna padta tha. | **Starter POMs** (`spring-boot-starter-web`) saari compatible dependencies automatically pull kar lete hain. |

---

### Q43: What are the advantages of Spring Boot?
**Answer (Hinglish):**

1. **Auto-Configuration:** Classpath dependencies dekh kar Spring Boot khud beans configure kar deta hai (e.g. agar H2 DB classpath me hai, to DataSource automatically create kar dega).
2. **Embedded HTTP Servers:** Tomcat, Jetty ya Undertow already integrated hote hain; alag se web server install karne ki jhanjhat khatam.
3. **Starter POMs:** Complex build configurations simple ho jaate hain.
4. **Production-Ready Features (Actuator):** Health checks, metrics, thread dumps out-of-the-box endpoints (`/actuator/health`, `/actuator/metrics`) par mil jate hain.
5. **No XML Requirement:** 100% pure Java annotations-based configuration.

---

### Q44: What is @SpringBootApplication annotation?
**Answer (Hinglish):**

`@SpringBootApplication` Spring Boot main class par lagne wala primary annotation hai. Yeh internally **3 core annotations ka combination** hai:

```java
@SpringBootApplication
public class MyBackendApp {
    public static void main(String[] args) {
        SpringApplication.run(MyBackendApp.class, args);
    }
}
```

Iske andar 3 annotations hote hain:
1. **`@Configuration`:** Class ko configuration class mark karta hai jisme `@Bean` methods define kiye ja sakte hain.
2. **`@EnableAutoConfiguration`:** Classpath me maujood jars ko dekh kar automatic configuration ko enable karta hai.
3. **`@ComponentScan`:** Current package aur uske sabhi sub-packages ko automatically scan karta hai `@Component`, `@Service`, `@Repository`, `@RestController` beans find karne ke liye.

---

### Q45: Difference between @Component, @Service, and @Repository?
**Answer (Hinglish):**

Teeno Spring managed beans banate hain, lekin architecture layer ke hisaab se inka role divide hai:

```
[ Controller / REST API ] (@RestController)
            ↓
     [ Service Layer ]    (@Service - Business Logic, @Transactional)
            ↓
      [ Data Access ]     (@Repository - DB Operations, Exception Translation)
            ↓
      [ General Beans]    (@Component - Utility, Helper classes)
```

| Annotation | Layer / Purpose | Special Feature |
| :--- | :--- | :--- |
| **`@Component`** | Generic stereotype annotation. Kisi bhi generic helper ya utility class par lagate hain. | Basic Spring managed bean banata hai. |
| **`@Service`** | **Business Logic Layer**. Application ki core calculations aur workflows yahan likhe jaate hain. | Semantic meaning deta hai business layer ko; `@Transactional` ke sath perfectly integrate hota hai. |
| **`@Repository`** | **DAO / Data Access Layer**. Database CRUD operations ke liye use hota hai. | **Automatic Exception Translation:** JDBC/Hibernate specific SQL exceptions ko Spring ke generic `DataAccessException` hierarchy me translate karta hai. |

---

### Q46 & Q51: What is Dependency Injection (DI) and IOC Container?
**Answer (Hinglish):**

- **Inversion of Control (IoC):**  
  Pehle Java me object khud apni dependencies ko `new` keyword se instantiate karta tha (tight coupling). IoC ka matlab hai object creation aur lifecycle management ka control programmer se chheen kar **Spring Framework (IoC Container)** ko de dena.
- **Dependency Injection (DI):**  
  DI ek design pattern hai jo IoC ko implement karta hai. Isme dependent objects (collaborators) ko container runtime par inject karta hai.
- **Spring IoC Container:**  
  Spring ka dil (heart) jo beans ko create karta hai, configure karta hai, aur unki lifecycle manage karta hai.
  - Core interfaces: `BeanFactory` (basic) aur `ApplicationContext` (enterprise features, event handling, AOP ke sath advanced container).

---

### Q47: What is @Autowired?
**Answer (Hinglish):**

- **Definition:** `@Autowired` Spring ko batata hai ki is property, constructor ya setter method me required bean ko **automatically inject** karo (by type).
- **Injection Types:**
  1. **Field Injection (Discouraged in modern Spring):**
     ```java
     @Autowired
     private OrderService orderService;
     ```
  2. **Constructor Injection (Industry Best Practice):**
     ```java
     @Service
     public class OrderController {
         private final OrderService orderService;

         // Java / Spring Boot recommended approach:
         public OrderController(OrderService orderService) {
             this.orderService = orderService;
         }
     }
     ```
  > **Note:** Spring 4.3+ se agar class me single constructor hai, to `@Autowired` likhna bhi optional hai.

---

### Q48: What are Spring Boot starters?
**Answer (Hinglish):**

Spring Boot starters ready-made dependency descriptors hote hain. Agar aapko web API banani hai, to aapko 10 alag-alag jars (Jackson, Tomcat, Spring MVC, Validation) ke exact versions manually dhoondne ki jarurat nahi hai.  
Aap sirf ek starter add karte ho:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```
Yeh starter internally saari required, fully compatible libraries ko auto-import kar leta hai.

---

### Q49: What is application.properties file?
**Answer (Hinglish):**

- **Definition:** Yeh Spring Boot application ka centralized configuration file hota hai jo `src/main/resources/` me rehta hai.
- Iska format key-value pairs (`key=value`) hota hai (ya fir `.yml` format).
- **Common Use Cases:**
  ```properties
  # Server port
  server.port=8080

  # Database configuration
  spring.datasource.url=jdbc:mysql://localhost:3306/mydb
  spring.datasource.username=root
  spring.datasource.password=secret

  # Custom application properties
  app.jwt.secret=MySecretKey123
  ```

---

### Q52 & Q53: HTTP, Common Methods & Status Codes
**Answer (Hinglish):**

- **HTTP (HyperText Transfer Protocol):** Client (Browser/Mobile) aur Server ke beech web par data exchange karne ka stateless protocol hai.
- **Common HTTP Methods:**
  - `GET`: Server se resource fetch karne ke liye (Idempotent & Safe).
  - `POST`: Server par naya resource create karne ke liye (Non-idempotent).
  - `PUT`: Existing resource ko completely replace/update karne ke liye.
  - `PATCH`: Existing resource ko partially update karne ke liye.
  - `DELETE`: Resource ko remove karne ke liye.

- **Key HTTP Status Codes:**
  - **`200 OK`:** Request successfully execute ho gayi.
  - **`201 Created`:** POST request ke baad naya resource create ho gaya.
  - **`400 Bad Request`:** Client ne invalid input ya payload bheja.
  - **`401 Unauthorized`:** User authenticated nahi hai (Login/Token required).
  - **`403 Forbidden`:** User authenticated hai lekin resource access karne ki permission nahi hai.
  - **`404 Not Found`:** Requested URL / Resource server par nahi mila.
  - **`500 Internal Server Error`:** Server side par unhandled exception ya bug crash hua.

---

### Q54 & Q55: What is REST API & JSON?
**Answer (Hinglish):**

- **REST (Representational State Transfer):**  
  Ek architectural style jo standard HTTP protocols, statelessness, uniform interfaces, aur resources (identified by URLs) par kaam karta hai.
- **JSON (JavaScript Object Notation):**  
  Lightweight, human-readable data-interchange format jo REST APIs me communication ke liye universal standard ban chuka hai.
  ```json
  {
    "id": 101,
    "name": "Sachin",
    "roles": ["ADMIN", "DEVELOPER"],
    "active": true
  }
  ```

---

### Q56: What is @RestController vs @Controller?
**Answer (Hinglish):**

| Feature | `@Controller` | `@RestController` |
| :--- | :--- | :--- |
| **Primary Use** | Traditional Spring MVC apps jahan server HTML views (JSP, Thymeleaf) return karta hai. | Modern RESTful Web Services jahan JSON ya XML data return hota hai. |
| **Internal Composition**| Single annotation `@Controller`. | `@Controller + @ResponseBody` ka combination. |
| **Response Handling** | View name (e.g. `"index"`) return karta hai jisko ViewResolver HTML page me map karta hai. | Java objects return karta hai jisko Jackson library automatically **JSON** me serialize kar deti hai. |

---

### Q57: What is the process of creating a REST API (with example)?
**Answer (Hinglish):**

Spring Boot me REST API banane ke standard 4 steps hote hain:

1. **Entity / Model Class:**
   ```java
   public record User(Long id, String name, String email) {}
   ```

2. **Repository / Database Layer:**
   ```java
   @Repository
   public interface UserRepository extends JpaRepository<User, Long> {}
   ```

3. **Service Layer (Business Logic):**
   ```java
   @Service
   public class UserService {
       public User getUserById(Long id) {
           return new User(id, "Sachin Kumar", "sachin@example.com");
       }
   }
   ```

4. **REST Controller Layer:**
   ```java
   @RestController
   @RequestMapping("/api/v1/users")
   public class UserController {

       private final UserService userService;

       public UserController(UserService userService) {
           this.userService = userService;
       }

       @GetMapping("/{id}")
       public ResponseEntity<User> getUser(@PathVariable Long id) {
           User user = userService.getUserById(id);
           return ResponseEntity.ok(user); // returns 200 OK + JSON
       }
   }
   ```
- **Test:** Browser ya Postman me `GET http://localhost:8080/api/v1/users/1` hit karo, aur response me formatted JSON data mil jata hai!

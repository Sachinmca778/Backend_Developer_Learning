# Level 3: Advanced Spring Boot & AOP (Q175 - Q192)

---

### Q175 & Q179 & Q185: Bean Lifecycle & Scopes in Spring
**Answer (Hinglish):**

#### Bean Lifecycle Flow:
```
[ Instantiation: constructor() ] 
       ↓
[ Populate Properties: Dependency Injection ]
       ↓
[ BeanNameAware, BeanFactoryAware, ApplicationContextAware ]
       ↓
[ BeanPostProcessor: postProcessBeforeInitialization() ]
       ↓
[ Initialization: @PostConstruct -> InitializingBean.afterPropertiesSet() -> initMethod ]
       ↓
[ BeanPostProcessor: postProcessAfterInitialization() (AOP Dynamic Proxy wrapped here!) ]
       ↓
[ Ready For Use (Serving Requests) ]
       ↓
[ Destruction: @PreDestroy -> DisposableBean.destroy() -> destroyMethod ]
```

#### Bean Scopes:
- **`singleton` (Default):** Entire Spring container me sirf ek single instance banta hai aur share hota hai.
- **`prototype`:** Har bar injection ya `getBean()` call hone par fresh naya instance banta hai.
- **Web-Aware Scopes:**
  - `request`: Har HTTP request ke liye ek naya instance.
  - `session`: Har HTTP user session ke liye ek instance.
  - `application`: Pure `ServletContext` ke liye ek instance.

---

### Q176: Dynamic Proxies in Spring: JDK Dynamic Proxy vs CGLIB Proxy
**Answer (Hinglish):**

Spring AOP aur `@Transactional` internally Proxies create karte hain:

| Feature | JDK Dynamic Proxy | CGLIB Proxy |
| :--- | :--- | :--- |
| **Prerequisite** | Target class ko **kam se kam ek Interface** implement karna zaroori hai. | Target class ko interface implement karne ki zaroorat nahi. |
| **Mechanism** | `java.lang.reflect.Proxy` use karta hai runtime interface implementation banane ke liye. | Target class ko subclass karke bytecode modify karta hai. |
| **Limitation** | Interface methods hi intercept kar sakta hai. | `final` classes aur `final` methods ko proxy nahi kar sakta. |
| **Spring Boot 2+ / 3 Default**| Spring Boot me default proxy mechanism **CGLIB Proxy** (`proxyTargetClass=true`) hai. |

---

### Q177: @Autowired vs @Inject vs @Resource
**Answer (Hinglish):**

| Annotation | Standard / Origin | Resolution Strategy |
| :--- | :--- | :--- |
| **`@Autowired`** | Spring Framework proprietary. | By **Type** pehle, fir By Name fallback. |
| **`@Inject`** | JSR-330 (Standard Java CDI). | By **Type** pehle, `@Named` ke sath name match. |
| **`@Resource`** | JSR-250 (Java EE standard). | By **Name** pehle, fir By Type fallback. |

---

### Q178: Prototype Bean into Singleton Bean Problem & Solutions
**Answer (Hinglish):**

- **The Problem:** Singleton bean container start hone par sirf ek baar banta hai. Agar aapne usme `@Autowired private PrototypeBean protoBean` likha, to prototype bean bhi sirf **ek hi baar** inject hoga! Har request par naya instance nahi milega.
- **Solutions:**
  1. **`@Lookup` Method (Best Spring Way):**
     ```java
     @Service
     public abstract class OrderService {
         @Lookup
         public abstract OrderTask getOrderTask(); // Spring CGLIB overrides this to return a new prototype instance every time!
     }
     ```
  2. **`ObjectProvider<T>` or `Provider<T>`:**
     ```java
     @Autowired
     private ObjectProvider<OrderTask> taskProvider;
     // Inside method: taskProvider.getObject(); // new instance
     ```
  3. **Scoped Proxy:** Prototype class par `@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)`.

---

### Q181 & Q182: Spring Boot Actuator & Custom Health Indicator
**Answer (Hinglish):**

- **Actuator:** Production monitoring aur management endpoints provide karta hai (`/actuator/health`, `/actuator/metrics`, `/actuator/env`, `/actuator/threaddump`).
- **Custom Health Check Implementation:**
  ```java
  @Component
  public class PaymentGatewayHealthIndicator implements HealthIndicator {

      @Override
      public Health health() {
          boolean isReachable = checkExternalPaymentGateway();
          if (isReachable) {
              return Health.up().withDetail("Gateway", "Stripe API is responding in 40ms").build();
          }
          return Health.down().withDetail("Error", "Timeout connecting to Stripe").build();
      }
  }
  ```

---

### Q187 - Q192: Spring AOP (Aspect-Oriented Programming) Deep Dive
**Answer (Hinglish):**

- **AOP Concept:** Cross-cutting concerns (logging, auditing, security, transaction management) ko core business logic se separate rakhna.
- **Core Terminology:**
  - **Aspect:** Wo class jisme cross-cutting logic likha hai (`@Aspect`).
  - **JoinPoint:** Execution point (jaise method call) jahan aspect hook ho sakta hai.
  - **Pointcut:** Expression jo match karta hai ki kaun se methods intercept honge (e.g. `execution(* com.app.service.*.*(..))`).
  - **Advice:** Action kab run hoga:
    - `@Before`: Target method run hone se pehle.
    - `@AfterReturning`: Method successfully bina error ke return hone par.
    - `@AfterThrowing`: Method me exception throw hone par.
    - `@After`: Finally block ki tarah (success ya exception dono me).
    - `@Around`: **Sabse powerful advice**. Method call ko wrap karta hai (`proceed()`), method ke execution time calculate karne aur arguments/return value modify karne ke kaam aata hai.

#### Real-World Custom Annotation + AOP Interception (Execution Time Tracker):
```java
// 1. Custom Annotation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TrackExecutionTime {}

// 2. AOP Aspect
@Aspect
@Component
@Slf4j
public class ExecutionTimeAspect {

    @Around("@annotation(TrackExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        Object proceed = joinPoint.proceed(); // Real method execute hota hai

        long duration = System.currentTimeMillis() - start;
        log.info("Method {} executed in {} ms", joinPoint.getSignature().toShortString(), duration);
        return proceed;
    }
}
```

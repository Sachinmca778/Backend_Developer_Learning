# Spring-Specific Patterns

## Status: Not Started

---

## Table of Contents

1. [Spring Patterns Kya Hai?](#spring-patterns-kya-hai)
2. [Repository Pattern](#repository-pattern)
3. [Service Layer Pattern](#service-layer-pattern)
4. [DTO Pattern](#dto-pattern)
5. [Specification Pattern](#specification-pattern)
6. [Front Controller Pattern](#front-controller-pattern)
7. [Layered Architecture (Putting It Together)](#layered-architecture-putting-it-together)
8. [Bonus — Other Spring Patterns](#bonus--other-spring-patterns)
9. [Common Pitfalls](#common-pitfalls)
10. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Spring Patterns Kya Hai?

**Matlab:** Patterns jo Spring/Spring Boot world mein **idiomatic** hain — formalized over years of enterprise Java. Most are GoF / DDD patterns adapted for Spring's IoC + AOP world.

### Big Picture

```
Client (Browser, Mobile) ──HTTP──▶
                          [Front Controller — DispatcherServlet]
                                       │
                                       ▼
                          [Controller Layer — @RestController]
                                       │
                                       ▼ (DTOs)
                          [Service Layer — @Service]
                                       │
                                       ▼
                          [Repository Layer — JpaRepository]
                                       │
                                       ▼
                                  [Database]
```

→ Each layer has clear responsibility. Loose coupling via interfaces.

---

## Repository Pattern

**Matlab:** **Abstraction over data access** — domain code talks to repository, not raw DB / ORM.

### Origin (DDD — Eric Evans)

> "A Repository represents all objects of a certain type as a conceptual set."

→ Looks like an in-memory collection, but backed by DB.

### Without Repository (Anti-Pattern)

```java
@Service
public class UserService {
    @PersistenceContext
    private EntityManager em;
    
    public User findById(Long id) {
        return em.find(User.class, id);   // service knows JPA details
    }
    
    public User findByEmail(String email) {
        return em.createQuery("SELECT u FROM User u WHERE u.email = :e", User.class)
            .setParameter("e", email)
            .getSingleResult();   // SQL leakage everywhere
    }
}
```

❌ Service tightly coupled to JPA
❌ Hard to test (mock EntityManager?)
❌ Query logic scattered

### ✅ With Repository

```java
public interface UserRepository {
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    List<User> findByRole(Role role);
    User save(User user);
    void delete(User user);
}

@Service
public class UserService {
    private final UserRepository userRepo;
    
    public UserService(UserRepository repo) { this.userRepo = repo; }
    
    public User getProfile(Long id) {
        return userRepo.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    }
}
```

→ Service has zero JPA knowledge. Easy to mock in tests.

### Spring Data JPA — Repository Made Easy

```java
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);          // method name → query
    List<User> findByRoleAndActive(Role r, boolean a);  // complex parsed
    
    @Query("SELECT u FROM User u WHERE u.lastLogin > :since")
    List<User> findActiveSince(@Param("since") LocalDateTime since);
    
    @Modifying
    @Query("UPDATE User u SET u.active = false WHERE u.lastLogin < :cutoff")
    int deactivateInactive(@Param("cutoff") LocalDateTime cutoff);
}
```

→ Spring **generates implementation** at runtime!
→ No need to write `findByEmail` method body.

### Method Name Conventions

```
findBy / readBy / queryBy / getBy / streamBy
existsBy / countBy
deleteBy / removeBy

Combinators:
  And, Or
  Between, LessThan, GreaterThan
  Like, Containing, StartingWith, EndingWith
  Is, Equals, IsNull, IsNotNull
  OrderBy
  IgnoreCase
```

Examples:
```
findFirstByRoleOrderByCreatedAtDesc(Role role)
countByActiveTrueAndCreatedAtAfter(LocalDateTime date)
existsByEmail(String email)
findTop10ByRoleOrderByLastLoginDesc(Role role)
```

### Beyond JPA — Multiple Backends

```java
public interface UserMongoRepository extends MongoRepository<User, String> { ... }
public interface UserRedisRepository extends CrudRepository<User, String> { ... }
public interface UserElasticsearchRepository extends ElasticsearchRepository<User, String> { ... }
```

→ Same `Repository` abstraction, different storage.

### Custom Repository Implementation

For complex queries beyond Spring's parser:

```java
public interface UserRepositoryCustom {
    List<User> findByCustomCriteria(CustomFilter filter);
}

public class UserRepositoryCustomImpl implements UserRepositoryCustom {
    @PersistenceContext private EntityManager em;
    
    public List<User> findByCustomCriteria(CustomFilter filter) {
        // hand-built JPQL / Criteria
    }
}

public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {
    // inherits both
}
```

### Repository as Facade

`JpaRepository` is also a **facade** over JPA:
- Hides EntityManager
- Hides PersistenceContext lifecycle
- Hides transaction handling

→ Cross-ref: Facade pattern in `02-Structural-Patterns.md`.

---

## Service Layer Pattern

**Matlab:** **Application-specific business logic** lives in service layer — orchestrates repositories, applies business rules, manages transactions.

### Where Service Sits

```
Controllers (HTTP, validation, response shaping)
     ↓
Services (business logic, orchestration, transactions)
     ↓
Repositories (data access)
```

### Responsibilities

✅ Business rules
✅ Orchestrate multiple repositories
✅ Transaction boundaries (`@Transactional`)
✅ Domain event publishing
✅ Validation beyond field-level
✅ External service integration

❌ HTTP / response shaping (controller's job)
❌ SQL / ORM details (repo's job)
❌ Tight coupling to UI

### Example

```java
@Service
@RequiredArgsConstructor
@Transactional   // class-level default; methods can override
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepo;
    private final InventoryRepository inventoryRepo;
    private final PaymentService paymentService;
    private final ApplicationEventPublisher publisher;
    
    public Order placeOrder(PlaceOrderRequest req) {
        // 1. Validate (business rules)
        if (req.getItems().isEmpty()) {
            throw new EmptyOrderException();
        }
        
        // 2. Reserve inventory (atomic w/ TX)
        for (OrderItem item : req.getItems()) {
            inventoryRepo.reserve(item.getProductId(), item.getQuantity());
        }
        
        // 3. Calculate total
        BigDecimal total = req.getItems().stream()
            .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 4. Create order
        Order order = new Order(req.getCustomerId(), req.getItems(), total);
        order = orderRepo.save(order);
        
        // 5. Charge payment (could be in same TX or separate)
        paymentService.charge(req.getCustomerId(), total);
        
        // 6. Publish domain event
        publisher.publishEvent(new OrderPlacedEvent(order));
        
        return order;
    }
    
    @Transactional(readOnly = true)
    public Order getById(Long id) {
        return orderRepo.findById(id)
            .orElseThrow(() -> new OrderNotFoundException(id));
    }
}
```

### Why Separate Service from Controller?

| Without Service | With Service |
|-----------------|--------------|
| Logic scattered across controllers | Centralized |
| Hard to reuse (e.g., scheduled job needs same logic) | Reusable |
| Hard to test (need full HTTP context) | Easy unit tests |
| TX boundaries unclear | Clean `@Transactional` |

### Anemic Domain Model vs Rich Domain Model

#### Anemic (Common in Spring Apps)

```java
public class User {  // just data
    private String name;
    private String email;
    // getters/setters
}

@Service
public class UserService {  // all behavior
    public void changeEmail(User u, String email) {
        if (!email.matches(...)) throw new InvalidEmailException();
        u.setEmail(email);
        userRepo.save(u);
    }
}
```

→ Pragmatic, simple, common in CRUD apps.

#### Rich Domain Model (DDD)

```java
public class User {
    private String email;
    
    public void changeEmail(String email) {
        if (!email.matches(...)) throw new InvalidEmailException();
        this.email = email;
    }
}

@Service
public class UserService {
    public void changeEmail(Long id, String email) {
        User u = userRepo.findById(id).orElseThrow();
        u.changeEmail(email);
        userRepo.save(u);
    }
}
```

→ Behavior with data; harder for ORM mapping but more OO.

→ Both styles valid. Choose based on team + complexity.

---

## DTO Pattern

**Matlab:** **Data Transfer Object** — plain object for moving data between layers, especially across **process boundaries** (HTTP, RPC).

### Why?

```
Entity (DB representation) ≠ API contract (what client needs)

Why?
  - Hide internal fields (passwords, internal IDs)
  - Combine fields from multiple entities
  - Versioning (UserDtoV1, UserDtoV2)
  - Avoid lazy-loading issues (entity → JSON serialization fetches related entities)
  - Decouple API from DB schema changes
```

### Without DTOs (Anti-Pattern)

```java
@RestController
public class UserController {
    @GetMapping("/users/{id}")
    public User get(@PathVariable Long id) {
        return userRepo.findById(id).orElseThrow();
        // ❌ Returns entity directly
        //    - exposes password hash, internal flags
        //    - lazy-loaded relations cause LazyInitializationException
        //    - schema change breaks API
    }
}
```

### ✅ With DTOs

```java
public record UserDto(Long id, String name, String email, LocalDateTime createdAt) {
    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }
}

@GetMapping("/users/{id}")
public UserDto get(@PathVariable Long id) {
    User user = userRepo.findById(id).orElseThrow();
    return UserDto.from(user);
}
```

→ Clean API contract.

### Request DTOs

```java
public record CreateUserRequest(
    @NotBlank @Size(max = 100) String name,
    @Email String email,
    @NotBlank @Size(min = 8) String password
) {}

@PostMapping("/users")
public UserDto create(@Valid @RequestBody CreateUserRequest req) {
    User user = userService.create(req.name(), req.email(), req.password());
    return UserDto.from(user);
}
```

### Mapping — Manual vs MapStruct

#### Manual

```java
public static UserDto from(User u) {
    return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getCreatedAt());
}
```

✅ Explicit, no magic
❌ Boilerplate for many fields

#### MapStruct (Compile-Time Codegen)

```java
@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);
    User toEntity(CreateUserRequest req);
    
    @Mapping(target = "fullName", expression = "java(u.getFirstName() + ' ' + u.getLastName())")
    UserSummaryDto toSummary(User u);
}

// Usage
@Autowired UserMapper mapper;
UserDto dto = mapper.toDto(user);
```

✅ No reflection (compile-time)
✅ Fast, type-safe
❌ Build-time dependency

#### ModelMapper (Reflection — Not Recommended for Production)

```java
ModelMapper mapper = new ModelMapper();
UserDto dto = mapper.map(user, UserDto.class);
```

⚠️ Slow, error-prone (silent field mismatches).

### DTO Variants

| DTO Type | Purpose |
|----------|---------|
| **Request DTO** | Incoming HTTP body |
| **Response DTO** | Outgoing HTTP body |
| **Command** (CQRS write) | Mutation request |
| **Query** (CQRS read) | Read request params |
| **View Model** | UI-shape data |
| **Event** | Cross-service async message |

### Java Records (Modern DTOs)

```java
public record UserDto(Long id, String name, String email) {}
```

✅ Immutable, concise, equality / hashCode / toString auto
✅ Perfect for DTOs

→ Java 14+; preview earlier.

### Lombok Alternative

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String name;
    private String email;
}
```

→ Pre-records era; still valid.

---

## Specification Pattern

**Matlab:** Encapsulate **business rule / criteria** as an object — composable, reusable.

### Origin (DDD)

> "Specification objects represent business rules that can be combined."

### Spring Data JPA Specifications

```java
public interface UserRepository extends JpaRepository<User, Long>,
        JpaSpecificationExecutor<User> {
    // inherits findAll(Specification), findOne(Specification), count(Specification)
}
```

### Build Specifications

```java
public class UserSpecifications {
    
    public static Specification<User> hasRole(Role role) {
        return (root, query, cb) -> cb.equal(root.get("role"), role);
    }
    
    public static Specification<User> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }
    
    public static Specification<User> registeredAfter(LocalDateTime date) {
        return (root, query, cb) -> cb.greaterThan(root.get("createdAt"), date);
    }
    
    public static Specification<User> nameLike(String pattern) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern.toLowerCase());
    }
}
```

### Compose at Query Time

```java
@Service
public class UserSearchService {
    private final UserRepository userRepo;
    
    public List<User> search(SearchRequest req) {
        Specification<User> spec = Specification.where(null);
        
        if (req.getRole() != null) {
            spec = spec.and(UserSpecifications.hasRole(req.getRole()));
        }
        if (req.isActiveOnly()) {
            spec = spec.and(UserSpecifications.isActive());
        }
        if (req.getRegisteredAfter() != null) {
            spec = spec.and(UserSpecifications.registeredAfter(req.getRegisteredAfter()));
        }
        if (req.getName() != null && !req.getName().isBlank()) {
            spec = spec.and(UserSpecifications.nameLike("%" + req.getName() + "%"));
        }
        
        return userRepo.findAll(spec);
    }
}
```

### With Pagination

```java
Page<User> page = userRepo.findAll(spec, PageRequest.of(0, 20, Sort.by("createdAt").descending()));
```

### Combining

```java
Specification<User> spec = Specification
    .where(hasRole(ADMIN))
    .and(isActive())
    .or(hasRole(SUPER_ADMIN));
```

### Why?

✅ Build dynamic queries cleanly
✅ Reusable predicates across services
✅ Avoid 50-method repository (`findByXAndYAndZ...`)
✅ Type-safe (compile-time check on field names via metamodel)

### Type-Safe with Metamodel

```java
@StaticMetamodel(User.class)
public class User_ {
    public static volatile SingularAttribute<User, String> name;
    public static volatile SingularAttribute<User, Role> role;
}

cb.equal(root.get(User_.role), role);   // typo = compile error
```

→ Generated by JPA processor.

### Alternative — QueryDSL

```java
QUser u = QUser.user;
List<User> result = queryFactory.selectFrom(u)
    .where(u.role.eq(role).and(u.active.isTrue()))
    .orderBy(u.createdAt.desc())
    .fetch();
```

→ Type-safe, more fluent than Specifications. Trade-off: extra dependency.

### When?

✅ Search / filter endpoints with many optional criteria
✅ Want reusable rules
✅ Avoid combinatorial explosion of repository methods

---

## Front Controller Pattern

**Matlab:** **Single entry point** for all requests; central place to handle common concerns (auth, logging, routing).

### Spring's `DispatcherServlet`

```
HTTP Request
     │
     ▼
┌───────────────────────────────┐
│      DispatcherServlet         │  ← Front Controller (single entry)
│                                │
│   1. HandlerMapping → find @Controller│
│   2. HandlerAdapter → invoke method   │
│   3. Method executes                  │
│   4. ViewResolver / MessageConverter  │
│   5. Response written                  │
└───────────────────────────────┘
```

### What DispatcherServlet Does

| Concern | Component |
|---------|-----------|
| Map URL → handler | `HandlerMapping` |
| Adapt different handlers | `HandlerAdapter` |
| Argument binding | `HandlerMethodArgumentResolver` |
| Validation | `@Valid` integration |
| Exception handling | `HandlerExceptionResolver`, `@ExceptionHandler` |
| Content negotiation | `ContentNegotiationManager` |
| Response writing | `HttpMessageConverter` |
| View rendering (MVC) | `ViewResolver`, `View` |
| Interceptors | `HandlerInterceptor` |

### Configuration

```java
// SpringBoot: zero config — auto-registered as servlet at "/"

// Customize via WebMvcConfigurer
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoggingInterceptor())
            .addPathPatterns("/api/**");
    }
    
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer cfg) {
        cfg.defaultContentType(MediaType.APPLICATION_JSON);
    }
}
```

### Why Front Controller?

✅ Centralized auth, logging, metrics
✅ Consistent error handling
✅ One place for request lifecycle hooks

### Filter Chain Before DispatcherServlet

```
Request
  ↓
Servlet Filters (CORS, character encoding, security)
  ↓
DispatcherServlet
  ↓
HandlerInterceptors (Spring-level)
  ↓
@Controller method
```

→ Spring Security uses filters; Spring MVC uses interceptors.

### vs Multiple Controllers

In old Java EE (without front controller), each servlet handled its own URL — each had to repeat boilerplate (auth, logging, error handling). Front Controller centralizes.

---

## Layered Architecture (Putting It Together)

```
┌──────────────────────────────────────────────────┐
│           CONTROLLER LAYER (@RestController)       │
│  - HTTP request/response                           │
│  - Validation (@Valid)                             │
│  - DTO conversion                                  │
│  - Exception → HTTP status                         │
└────────────────────┬─────────────────────────────┘
                     │ DTOs (Request/Response)
                     ▼
┌──────────────────────────────────────────────────┐
│           SERVICE LAYER (@Service)                 │
│  - Business logic                                  │
│  - Orchestrate repositories                        │
│  - Transactions (@Transactional)                   │
│  - Domain events                                   │
└────────────────────┬─────────────────────────────┘
                     │ Domain entities
                     ▼
┌──────────────────────────────────────────────────┐
│           REPOSITORY LAYER (Spring Data)           │
│  - Data access                                     │
│  - Query construction (method names, @Query, Specs)│
│  - Persistence operations                          │
└────────────────────┬─────────────────────────────┘
                     ▼
              [Database / Cache / API]
```

### Cross-Cutting Concerns (AOP)

```
Logging, Tracing, Caching, Security, Metrics, Transactions
        ↓ applied via Spring AOP / Filters / Interceptors
```

### Example Stack

```java
// 1. Controller (HTTP)
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    
    @GetMapping("/{id}")
    public UserDto get(@PathVariable Long id) {
        return userService.getProfile(id);
    }
    
    @PostMapping
    public UserDto create(@Valid @RequestBody CreateUserRequest req) {
        return userService.create(req);
    }
}

// 2. Service (business)
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepo;
    private final UserMapper mapper;
    
    public UserDto getProfile(Long id) {
        User user = userRepo.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        return mapper.toDto(user);
    }
    
    public UserDto create(CreateUserRequest req) {
        User user = mapper.toEntity(req);
        user = userRepo.save(user);
        return mapper.toDto(user);
    }
}

// 3. Repository (data)
public interface UserRepository extends JpaRepository<User, Long>,
        JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
}

// 4. Entity (domain)
@Entity
public class User {
    @Id @GeneratedValue private Long id;
    @Column(unique = true) private String email;
    private String name;
    @Enumerated(EnumType.STRING) private Role role;
    private LocalDateTime createdAt;
}

// 5. DTOs
public record UserDto(Long id, String email, String name, LocalDateTime createdAt) {}
public record CreateUserRequest(@Email String email, @NotBlank String name) {}
```

→ Each layer testable in isolation.

---

## Bonus — Other Spring Patterns

### Dependency Injection (Inversion of Control)

```java
@Service
public class OrderService {
    private final UserRepository userRepo;
    
    public OrderService(UserRepository repo) { this.userRepo = repo; }   // constructor injection
}
```

→ Most fundamental Spring pattern.

### Configuration via Annotation / Java Config

```java
@Configuration
public class AppConfig {
    @Bean public DataSource dataSource() { ... }
    @Bean public TransactionManager txManager(DataSource ds) { ... }
}
```

### Aspect-Oriented Programming (Cross-Cutting)

```java
@Aspect
@Component
public class LoggingAspect {
    @Around("execution(* com.example.service.*.*(..))")
    public Object log(ProceedingJoinPoint pjp) throws Throwable {
        log.info("Calling {}", pjp.getSignature());
        return pjp.proceed();
    }
}
```

### Open Session in View (Anti-Pattern, But Common)

```yaml
spring.jpa.open-in-view: false   # ← turn off in production
```

→ Default `true` keeps Hibernate session open during view rendering — convenient but causes N+1 + perf issues. Disable + use DTOs / fetch joins.

### Convention over Configuration

```
Spring Boot defaults:
  src/main/resources/application.yml
  /static for static files
  Component scan from main package downward
  Auto-configuration based on classpath
```

### Auto-Configuration

```
@SpringBootApplication scans → finds spring-boot-starter-jpa on classpath
                            → auto-configures DataSource, EntityManager, etc.
```

→ Reduces boilerplate dramatically.

### CQRS-Lite

Separate read service from write service:

```java
@Service public class UserCommandService { /* writes */ }
@Service public class UserQueryService { /* reads, optimized */ }
```

→ Cross-ref: `Microservices Architecture/05-Saga-Pattern.md` for full CQRS + Event Sourcing.

---

## Common Pitfalls

### 1. Anemic Service / Fat Controller

```java
@RestController
public class UserController {
    public UserDto create(req) {
        // 50 lines of business logic in controller
    }
}
```

→ Move to service.

### 2. Repository Doing Business Logic

```java
@Repository
public class UserRepository {
    public void registerNewUser(...) {
        // sends emails, calls payment, audits — NO!
    }
}
```

→ Repository = data access only. Business logic in service.

### 3. Returning Entities from Controllers

```java
@GetMapping("/users/{id}")
public User get(@PathVariable Long id) { ... }   // ❌
```

- Lazy loading errors
- Internal fields exposed
- Schema change breaks API

→ Always return DTOs.

### 4. Cyclic Dependencies

`A → B → C → A` — Spring fails to wire. Refactor.

### 5. Field Injection (`@Autowired private X x;`)

❌ Hard to test (need reflection to set)
❌ Hides dependencies
❌ Allows incomplete construction

→ Use constructor injection (`@RequiredArgsConstructor` for Lombok).

### 6. `@Transactional` on Private Methods

Spring proxies don't intercept private methods. Use public.

### 7. Self-Invocation Misses Proxy

```java
@Transactional public void outer() { inner(); }   // inner() not transactional
```

→ Refactor to separate class or use injected self-reference.

### 8. Generic JpaRepository for Everything

For complex search → use Specifications / QueryDSL, not 50 method names.

### 9. DTO Mapping Reflection Magic

`ModelMapper` runtime reflection = silent bugs (typos, mismatches). Use MapStruct (compile-time) or manual.

### 10. Specification Without Pagination

Returning all results → OOM on large tables. Paginate.

### 11. Open Session in View

Default ON in Spring Boot. Disable in production.

### 12. Front Controller Override Risks

Customizing `DispatcherServlet` = brittle. Usually configure via `WebMvcConfigurer` instead.

### 13. Mixing API DTO with Entity

```java
@Entity
@JsonIgnore(...) on password
```

→ Brittle. Separate API DTO from JPA entity.

### 14. No `readOnly = true` for Read Methods

```java
@Transactional(readOnly = true)
public User getById(Long id) { ... }
```

→ Hibernate skips dirty checking → faster.

---

## Summary Cheat Sheet

| Pattern | Spring Component | Purpose |
|---------|------------------|---------|
| **Repository** | `JpaRepository` | Abstract data access |
| **Service Layer** | `@Service` | Business logic + transactions |
| **DTO** | Records / `@Data` classes | Cross-boundary data carrier |
| **Specification** | `Specification<T>` | Composable query criteria |
| **Front Controller** | `DispatcherServlet` | Single HTTP entry point |
| **Dependency Injection** | `@Autowired` (constructor) | Inversion of control |
| **Auto-Configuration** | `@EnableAutoConfiguration` | Convention over config |
| **AOP / Proxy** | `@Aspect`, `@Transactional` | Cross-cutting concerns |

| Layer | Annotation | What Lives Here |
|-------|-----------|-----------------|
| Controller | `@RestController` | HTTP, validation, DTO ↔ entity |
| Service | `@Service` | Business logic, transactions, orchestration |
| Repository | `@Repository` (auto via Spring Data) | Data access |
| Entity | `@Entity` | Domain model |
| Configuration | `@Configuration` + `@Bean` | Wiring |

| ✅ Do | ❌ Don't |
|-------|---------|
| Constructor injection | Field injection |
| Return DTOs from controller | Return entities |
| `@Transactional(readOnly = true)` for reads | Default everywhere |
| Service has business logic | Logic in controller / repo |
| Specifications for dynamic queries | 50 method names |
| MapStruct for mapping | ModelMapper reflection |
| Disable open-in-view | Leave on in production |
| Records / `@Builder` for DTOs | Mutable DTOs everywhere |

---

## Practice

1. Build full stack: `Controller → Service → Repository` for `Product` entity with CRUD.
2. Create DTOs (request + response) using Java records; map with MapStruct.
3. Add Specifications for `Product` search (name like, price range, category, in stock).
4. Add pagination + sorting to search endpoint.
5. Customize `DispatcherServlet` — add interceptor logging request duration.
6. Test repository methods (`@DataJpaTest`); test service (mock repo); test controller (`@WebMvcTest`).
7. Compare entity-based response vs DTO response — observe lazy loading errors.
8. Disable `open-in-view`; fix resulting `LazyInitializationException`s with fetch joins.
9. Implement custom repository method that uses native SQL (via `@Query(nativeQuery = true)`).
10. Add `@TransactionalEventListener` for `OrderPlacedEvent` (after commit).
11. Convert anemic `User` to richer model (move `changeEmail` validation into entity).
12. Identify each layer's pattern in a real Spring Boot project (open-source one on GitHub).

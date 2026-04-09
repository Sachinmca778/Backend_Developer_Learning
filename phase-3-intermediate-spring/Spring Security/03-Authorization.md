# Authorization

## Status: Not Started

---

## Table of Contents

1. [Authorization Overview](#authorization-overview)
2. [HttpSecurity Configuration](#httpsecurity-configuration)
3. [requestMatchers](#requestmatchers)
4. [permitAll](#permitall)
5. [authenticated](#authenticated)
6. [hasRole](#hasrole)
7. [hasAuthority](#hasauthority)
8. [hasAnyRole](#hasanyrole)
9. [@PreAuthorize](#preauthorize)
10. [@PostAuthorize](#postauthorize)
11. [@Secured](#secured)
12. [Method-Level Security (@EnableMethodSecurity)](#method-level-security-enablemethodsecurity)
13. [URL-based vs Method-Level Security](#url-based-vs-method-level-security)

---

## Authorization Overview

**Authorization ka matlab:** User authenticated hai — ab woh **kya kar sakta hai** ya **kya access kar sakta hai** yeh decide karna.

### Authentication vs Authorization

```
Authentication = Kaun ho tum? (login verify karna)
Authorization  = Kya kar sakte ho? (permissions check karna)
```

### Authorization Levels

| Level | Where | Use Case |
|-------|-------|----------|
| **URL-based** | SecurityFilterChain config | Coarse-grained (URL patterns pe control) |
| **Method-level** | @PreAuthorize, @Secured on methods | Fine-grained (individual method pe control) |

---

## HttpSecurity Configuration

**Matlab:** Spring Security ka fluent API jo URL-based authorization configure karta hai.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/health").permitAll()

                // Role-based access
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/manager/**").hasRole("MANAGER")

                // Authority-based access
                .requestMatchers("/api/users/read").hasAuthority("user:read")
                .requestMatchers("/api/users/write").hasAuthority("user:write")

                // Multiple roles
                .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "MANAGER")

                // Specific HTTP method
                .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/**").authenticated()

                // Regex matching
                .requestMatchers("/api/.*-secret").hasRole("ADMIN")

                // Default: everything else requires authentication
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults())
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
```

### Important Notes

| Point | Description |
|-------|-------------|
| Order matters | Pehle specific matchers, baad mein general |
| First match wins | Jo pehla matcher match karega, woh apply hoga |
| anyRequest() last | Hamesha end mein hona chahiye (catch-all) |
| HttpSecurity chain | Ek hi chain mein multiple rules |

---

## requestMatchers

**Matlab:** URL patterns match karne ka modern approach. `antMatchers` ka replacement (Spring Security 6+).

### antMatchers vs requestMatchers

```java
// ❌ Old (Spring Security 5.x)
.antMatchers("/api/**")
.antMatchers(HttpMethod.GET, "/api/users")

// ✅ New (Spring Security 6+)
.requestMatchers("/api/**")
.requestMatchers(HttpMethod.GET, "/api/users")
```

### Matching Options

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth

        // Exact match
        .requestMatchers("/health").permitAll()

        // Wildcard: ** = multiple levels
        .requestMatchers("/api/**").authenticated()

        // Wildcard: * = single level
        .requestMatchers("/api/*/info").authenticated()

        // HTTP Method + Path
        .requestMatchers(HttpMethod.GET, "/api/users/**").hasRole("USER")
        .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMIN")
        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")

        // Multiple paths at once
        .requestMatchers("/login", "/register", "/forgot-password").permitAll()

        // Servlet path
        .requestMatchers("/servlet/**").authenticated()

        // Regex matcher
        .requestMatchers(new RegexRequestMatcher("/api/v[0-9]+/.*", null)).authenticated()

        // Custom matcher
        .requestMatchers(new RequestMatcher() {
            @Override
            public boolean matches(HttpServletRequest request) {
                return request.getHeader("X-Internal") != null;
            }
        }).hasRole("INTERNAL")

        .anyRequest().authenticated()
    );

    return http.build();
}
```

### Pattern Examples

| Pattern | Matches | Does NOT Match |
|---------|---------|----------------|
| `/api/**` | `/api/users`, `/api/users/1/posts` | `/api2/users` |
| `/api/*` | `/api/users` | `/api/users/1` |
| `/api/users/{id}` | `/api/users/123` | `/api/users` |
| `/public/**` | `/public/css/style.css` | `/private/` |

---

## permitAll

**Matlab:** Bina authentication ke access allowed. Public endpoints ke liye.

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth

        // Common public endpoints
        .requestMatchers(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/forgot-password",
            "/api/auth/reset-password/**"
        ).permitAll()

        // Static resources
        .requestMatchers(
            "/",
            "/index.html",
            "/css/**",
            "/js/**",
            "/images/**",
            "/favicon.ico"
        ).permitAll()

        // API documentation
        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

        // Health check
        .requestMatchers("/actuator/health").permitAll()

        .anyRequest().authenticated()
    );

    return http.build();
}
```

### Security Warning

`permitAll` ka matlab sirf authorization bypass — CSRF aur security headers abhi bhi apply hote hain.

---

## authenticated

**Matlab:** Koi bhi authenticated user access kar sakta hai. Specific role ki zarurat nahi.

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth

        .requestMatchers("/public/**").permitAll()

        // Koi bhi logged-in user
        .requestMatchers("/api/profile").authenticated()
        .requestMatchers("/api/settings").authenticated()
        .requestMatchers("/dashboard/**").authenticated()

        .anyRequest().authenticated()
    );

    return http.build();
}
```

**Use when:** Sirf yeh confirm karna hai ki user logged-in hai, role matter nahi karta.

---

## hasRole

**Matlab:** User ke paas specific **role** honi chahiye.

```java
.requestMatchers("/api/admin/**").hasRole("ADMIN")
.requestMatchers("/api/manager/**").hasRole("MANAGER")
```

### Important: ROLE_ Prefix

Spring Security automatically `ROLE_` prefix add karta hai.

```java
hasRole("ADMIN")   // Actually checks: "ROLE_ADMIN"
hasRole("USER")    // Actually checks: "ROLE_USER"
```

### Multiple Roles

```java
.requestMatchers("/api/admin/**").hasRole("ADMIN")           // Single role
.requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "MANAGER")  // Any of these
```

### Example Setup

```java
// User with roles
UserDetails admin = User.builder()
    .username("admin")
    .password(encoder.encode("pass"))
    .roles("ADMIN", "USER")     // Creates: ROLE_ADMIN, ROLE_USER
    .build();

UserDetails user = User.builder()
    .username("sachin")
    .password(encoder.encode("pass"))
    .roles("USER")              // Creates: ROLE_USER
    .build();
```

### Access Behavior

| Endpoint | Role: USER | Role: ADMIN |
|----------|------------|-------------|
| `hasRole("USER").requestMatchers("/api/user/**")` | ✅ Access | ✅ Access |
| `hasRole("ADMIN").requestMatchers("/api/admin/**")` | ❌ 403 Forbidden | ✅ Access |

---

## hasAuthority

**Matlab:** User ke paas specific **permission/authority** honi chahiye. Role se zyada **granular** control.

```java
.requestMatchers("/api/users/read").hasAuthority("user:read")
.requestMatchers("/api/users/write").hasAuthority("user:write")
.requestMatchers("/api/users/delete").hasAuthority("user:delete")
```

### Authority Naming Convention

```java
// Domain-based permissions (recommended for fine-grained access)
"user:read"
"user:write"
"user:delete"
"post:create"
"post:publish"
"report:generate"
"report:export"
```

### hasAuthority vs hasRole

```java
// hasRole — checks ROLE_ prefixed
hasRole("ADMIN")              // Checks: ROLE_ADMIN

// hasAuthority — exact match
hasAuthority("user:read")     // Checks: user:read (no prefix)
hasAuthority("ROLE_ADMIN")    // Checks: ROLE_ADMIN (same as hasRole but explicit)
```

### Setup with Authorities

```java
UserDetails editor = User.builder()
    .username("editor")
    .password(encoder.encode("pass"))
    .authorities(
        "post:read",
        "post:write",
        "post:publish",
        "user:read"
    )
    .build();
```

**When to use:** Jab role-based access kaafi broad ho aur fine-grained permissions chahiye.

---

## hasAnyRole

**Matlab:** User ke paas **koi bhi ek role** listed mein se honi chahiye.

```java
.requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "MANAGER")
.requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN", "MANAGER", "ANALYST")
```

### Authority version: hasAnyAuthority

```java
.requestMatchers("/api/data/**").hasAnyAuthority("data:read", "data:write", "data:admin")
```

### Example

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/public/**").permitAll()

        // Users and admins both can access
        .requestMatchers("/api/profile/**").hasAnyRole("USER", "ADMIN")

        // Only admins
        .requestMatchers("/api/admin/**").hasRole("ADMIN")

        // Managers and admins
        .requestMatchers("/api/reports/**").hasAnyRole("MANAGER", "ADMIN")

        .anyRequest().authenticated()
    );

    return http.build();
}
```

---

## @PreAuthorize

**Matlab:** Method execute hone se **pehle** authorization check karta hai. Expression-based, sabse **flexible** approach.

### Enable Method Security

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // @PreAuthorize, @PostAuthorize enable karne ke liye
public class SecurityConfig {
    // ...
}
```

### Basic Usage

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // Sirf ADMIN role wala user call kar sakta hai
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // USER ya ADMIN role wala
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow();
    }

    // Specific authority
    @PreAuthorize("hasAuthority('user:write')")
    public User createUser(UserDto dto) {
        return userRepository.save(mapToUser(dto));
    }
}
```

### Expression Variables

```java
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    // #id = method parameter
    @PreAuthorize("#userId == authentication.principal.id")
    public User getUser(Long userId) {
        // Sirf apna hi user data access kar sakta hai
    }

    // Object property access
    @PreAuthorize("#post.authorId == authentication.principal.id")
    public void updatePost(Post post) {
        // Sirf post ka author hi update kar sakta hai
    }

    // Method return value pe check
    @PreAuthorize("@postService.isOwner(#postId, authentication.principal.id)")
    public void deletePost(Long postId) {
        // Custom bean method call
    }

    // Multiple conditions
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public User getUserDetails(Long userId) {
        // Admin kisi ka bhi dekh sakta hai, user sirf apna
    }
}
```

### Accessing Authentication Object

```java
@Service
public class CommentService {

    // Username check
    @PreAuthorize("authentication.name == #username")
    public Comment getUserComment(String username, Long commentId) {
        // ...
    }

    // Principal object (CustomUserDetails)
    @PreAuthorize("authentication.principal.id == #userId")
    public UserProfile getUserProfile(Long userId) {
        // ...
    }

    // Authorities check
    @PreAuthorize("authentication.authorities.?[authority == 'ROLE_ADMIN'].size() > 0")
    public List<User> getAllUsers() {
        // ...
    }
}
```

### Custom Bean Reference

```java
@Component("securityService")
public class SecurityService {

    public boolean isOwner(Long resourceId, Long userId) {
        // Custom ownership check logic
        return resourceRepository.findById(resourceId)
            .map(r -> r.getOwnerId().equals(userId))
            .orElse(false);
    }

    public boolean isInSameDepartment(Long userId1, Long userId2) {
        // Custom department check
        // ...
    }
}

@Service
public class ResourceService {

    // @beanName.methodName() syntax
    @PreAuthorize("@securityService.isOwner(#resourceId, authentication.principal.id)")
    public void deleteResource(Long resourceId) {
        // ...
    }

    @PreAuthorize("@securityService.isInSameDepartment(#userId, authentication.principal.id)")
    public User getUserDetails(Long userId) {
        // ...
    }
}
```

---

## @PostAuthorize

**Matlab:** Method execute hone ke **baad** authorization check karta hai. Return value pe condition check hoti hai.

```java
@Service
@RequiredArgsConstructor
public class DocumentService {

    // returnObject = method ka return value
    @PostAuthorize("returnObject.ownerId == authentication.principal.id")
    public Document getDocument(Long id) {
        return documentRepository.findById(id).orElseThrow();
    }

    // Admin sab dekh sakta hai, user sirf apna
    @PostAuthorize(
        "hasRole('ADMIN') or returnObject.ownerId == authentication.principal.id"
    )
    public Document getSensitiveDocument(Long id) {
        return documentRepository.findById(id).orElseThrow();
    }

    // Null check bhi handle karo
    @PostAuthorize("returnObject == null or returnObject.isPublic or " +
                   "returnObject.ownerId == authentication.principal.id")
    public Document getDocumentWithPublicCheck(Long id) {
        return documentRepository.findById(id).orElse(null);
    }
}
```

### @PreAuthorize vs @PostAuthorize

| Feature | @PreAuthorize | @PostAuthorize |
|---------|---------------|----------------|
| **When** | Method se pehle | Method ke baad |
| **Check** | Parameters pe | Return value pe |
| **Performance** | Better (method run nahi hota if fail) | Worse (method run hota hai, phir reject) |
| **Access** | `#paramName` | `returnObject` |
| **Use when** | URL/param-based check | Return value-based check |

---

## @Secured

**Matlab:** Older annotation, sirf **role-based** access control. Spring Security legacy.

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)  // @Secured enable karna padta hai
public class SecurityConfig { }

@Service
public class AdminService {

    @Secured("ROLE_ADMIN")
    public void deleteAllUsers() {
        // Sirf ADMIN
    }

    @Secured({"ROLE_ADMIN", "ROLE_MANAGER"})
    public void generateReport() {
        // ADMIN ya MANAGER
    }
}
```

### Limitations

| Limitation | Detail |
|------------|--------|
| No SpEL | Expression language support nahi hai |
| Only roles | Sirf roles check kar sakta hai |
| ROLE_ prefix required | `ROLE_ADMIN` likhna padta hai (automatic nahi) |
| Less flexible | @PreAuthorize se better hai |

**Recommendation:** Naye projects mein `@PreAuthorize` use karo. `@Secured` sirf legacy code ke liye.

---

## Method-Level Security (@EnableMethodSecurity)

**Matlab:** Class-level annotation jo method-level security annotations enable karta hai.

### Enable Options

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
    prePostEnabled = true,    // @PreAuthorize, @PostAuthorize (default: true)
    securedEnabled = true,    // @Secured
    jsr250Enabled = true      // @RolesAllowed (Java standard)
)
public class SecurityConfig { }
```

### All Annotations Together

```java
@Service
public class MixedService {

    // @PreAuthorize — SpEL support (most flexible)
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public User getUser(Long id) {
        return userRepository.findById(id).orElseThrow();
    }

    // @Secured — Simple role check (legacy)
    @Secured("ROLE_ADMIN")
    public void adminOnly() {
        // ...
    }

    // @RolesAllowed — JSR-250 standard
    @RolesAllowed({"ADMIN", "MANAGER"})
    public void managerOrAdmin() {
        // ...
    }

    // @PostAuthorize — Return value check
    @PostAuthorize("returnObject.owner == authentication.principal.username")
    public Document getDocument(Long id) {
        return documentRepository.findById(id).orElseThrow();
    }
}
```

### Method-Level on Controllers

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Controller pe bhi laga sakte ho
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    // Service layer mein bhi (recommended)
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }
}
```

### Service Layer Security (Recommended)

```java
// ✅ Best practice: Service layer pe lagao
@Service
public class UserService {

    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public User getUser(Long id) {
        return userRepository.findById(id).orElseThrow();
    }
}

// ❌ Avoid: Controller pe lagao (mixes concerns)
@RestController
public class UserController {

    @PreAuthorize("hasRole('ADMIN')")  // Yeh nahi
    @GetMapping("/admin/users")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }
}
```

**Why Service Layer?**
- Reusable (multiple controllers use same service)
- Direct service calls bhi secure hote hain
- Separation of concerns

---

## URL-based vs Method-Level Security

### Comparison

| Feature | URL-based (HttpSecurity) | Method-Level (@PreAuthorize) |
|---------|-------------------------|------------------------------|
| **Where** | SecurityConfig | Service methods |
| **Granularity** | Coarse (URL patterns) | Fine (per method, per parameter) |
| **Flexibility** | Limited | SpEL expressions |
| **Ownership logic** | Nahi kar sakte | `#obj.owner == principal.id` |
| **Defense in depth** | First line | Second line |
| **Testability** | Integration tests | Unit tests |

### Combined Approach (Recommended)

```java
// Layer 1: URL-based (coarse gate)
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/public/**", "/api/auth/**").permitAll()
        .requestMatchers("/api/admin/**").hasRole("ADMIN")
        .anyRequest().authenticated()
    );
    return http.build();
}

// Layer 2: Method-level (fine-grained)
@Service
public class UserService {

    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public User getUser(Long id) {
        return userRepository.findById(id).orElseThrow();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PreAuthorize("#user.id == authentication.principal.id or hasRole('ADMIN')")
    public User updateUser(User user) {
        return userRepository.save(user);
    }
}
```

### Defense in Depth

```
HTTP Request
    ↓
[Layer 1] URL-based auth (SecurityFilterChain)
    → /api/admin/** → ROLE_ADMIN required
    ↓
[Layer 2] Method-level auth (@PreAuthorize)
    → getUser(5) → must be ADMIN or user #5
    ↓
Controller → Service → Database
```

---

## Quick Reference

```java
// URL-based authorization
.requestMatchers("/public/**").permitAll()
.requestMatchers("/api/**").authenticated()
.requestMatchers("/admin/**").hasRole("ADMIN")
.requestMatchers("/data/**").hasAuthority("data:read")
.requestMatchers("/reports/**").hasAnyRole("ADMIN", "MANAGER")

// Method-level authorization
@EnableMethodSecurity  // on config class

@PreAuthorize("hasRole('ADMIN')")
@PreAuthorize("hasAuthority('user:read')")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@PreAuthorize("#id == authentication.principal.id")
@PreAuthorize("@securityService.isOwner(#id, authentication.principal.id)")
@PostAuthorize("returnObject.owner == authentication.principal.username")
@Secured("ROLE_ADMIN")
```

---

## Summary

| Feature | Use When |
|---------|----------|
| **permitAll** | Public endpoints (login, register, health) |
| **authenticated** | Any logged-in user (no specific role needed) |
| **hasRole** | Role-based URL access (ADMIN, MANAGER) |
| **hasAuthority** | Fine-grained permission (user:read, post:write) |
| **hasAnyRole** | Multiple roles mein se koi ek kaafi ho |
| **@PreAuthorize** | Service method pe flexible security (SpEL) |
| **@PostAuthorize** | Return value pe condition check |
| **@Secured** | Legacy/simple role check (avoid in new projects) |
| **@EnableMethodSecurity** | Method-level annotations enable karna |

# Security Architecture

## Status: Not Started

---

## Table of Contents

1. [Spring Security Overview](#spring-security-overview)
2. [SecurityFilterChain](#securityfilterchain)
3. [DelegatingFilterProxy](#delegatingfilterproxy)
4. [FilterChainProxy](#filterchainproxy)
5. [Security Filter Order](#security-filter-order)
6. [OncePerRequestFilter](#onceperrequestfilter)
7. [SecurityContextHolder](#securitycontextholder)
8. [SecurityContext](#securitycontext)
9. [Authentication Object](#authentication-object)
10. [GrantedAuthority](#grantedauthority)
11. [Architecture Flow Diagram](#architecture-flow-diagram)

---

## Spring Security Overview

**Spring Security ka matlab:** Application mein authentication (kaun aa raha hai) aur authorization (kya kar sakta hai) handle karne ka framework.

### Core Concepts

| Concept | Description |
|---------|-------------|
| **Authentication** | User kaun hai verify karna (login, credentials check) |
| **Authorization** | User kya access kar sakta hai (roles, permissions) |
| **Filter Chain** | Har request ko process karne wala pipeline |

### Key Components Flow

```
HTTP Request
    ↓
DelegatingFilterProxy (Servlet Filter)
    ↓
FilterChainProxy (Spring Bean)
    ↓
Security Filters (ordered list)
    ↓
Authentication Filter
    ↓
SecurityContextHolder (stores current user)
    ↓
Your Controller
```

---

## SecurityFilterChain

**Matlab:** Ek chain of filters jo decide karta hai ki request secure hai ya nahi. Har HTTP request is chain se guzarti hai.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()      // No auth required
                .requestMatchers("/admin/**").hasRole("ADMIN")  // Admin role required
                .requestMatchers("/api/**").authenticated()     // Any authenticated user
                .anyRequest().authenticated()                   // Default: auth required
            )
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
            )
            .httpBasic(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
```

### Multiple SecurityFilterChain

Aap multiple chains bana sakte ho different URL patterns ke liye.

```java
@Configuration
@EnableWebSecurity
public class MultiSecurityConfig {

    // API requests - JWT based authentication
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    // Web requests - Form login based authentication
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults());

        return http.build();
    }
}
```

**Important:** `@Order` zaruri hai jab multiple chains hon. Lower number = higher priority.

---

## DelegatingFilterProxy

**Matlab:** Servlet container (Tomcat) aur Spring ke beech ka **bridge**. Servlet filter ko Spring beans access karne deta hai.

### How it Works

```
Servlet Container (Tomcat)
    ↓
DelegatingFilterProxy (javax.servlet.Filter interface implement karta hai)
    ↓
Spring ApplicationContext se bean dhoondta hai (by name: "springSecurityFilterChain")
    ↓
Delegation → FilterChainProxy (actual Spring bean)
```

### Behind the Scenes

```java
// Servlet container automatically registers this
public class DelegatingFilterProxy extends GenericFilterBean {

    private String targetBeanName = "springSecurityFilterChain";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        // Spring context se bean get karo
        Filter targetBean = getApplicationContext().getBean(targetBeanName, Filter.class);

        // Actual kaam target bean ko delegate karo
        targetBean.doFilter(request, response, chain);
    }
}
```

### Key Points

| Point | Description |
|-------|-------------|
| Servlet filter hai, Spring bean nahi | Servlet container lifecycle follow karta hai |
| Delegate karta hai Spring bean ko | Actual logic Spring bean (FilterChainProxy) mein hai |
| Auto-registered | `spring-boot-starter-security` add karne pe automatically register ho jata hai |
| Bean name | Default bean name: `springSecurityFilterChain` |

### Manual Registration (Rarely Needed)

```java
// Spring Boot mein usually auto-configured hota hai
// But manually register karna ho toh:

public class AppInitializer extends AbstractSecurityWebApplicationInitializer {
    // Automatically registers DelegatingFilterProxy
}
```

**Why this design?** Servlet container Spring se pehle start hota hai. DelegatingFilterProxy Servlet filter lifecycle ko Spring bean lifecycle se connect karta hai.

---

## FilterChainProxy

**Matlab:** Actual Spring bean jo **multiple SecurityFilterChain** ko manage karta hai aur request ko appropriate chain route karta hai.

### Role

```
DelegatingFilterProxy
    ↓ (delegates to)
FilterChainProxy (Bean name: "springSecurityFilterChain")
    ↓ (selects appropriate chain based on request matcher)
SecurityFilterChain 1 (API requests)
SecurityFilterChain 2 (Web requests)
SecurityFilterChain 3 (Actuator endpoints)
```

### Internal Working

```java
public class FilterChainProxy extends GenericFilterBean {

    private List<SecurityFilterChain> filterChains;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // Find first matching chain
        for (SecurityFilterChain chain : filterChains) {
            if (chain.matches(httpRequest)) {
                // Run all filters in this chain
                VirtualFilterChain virtualChain = new VirtualFilterChain(chain.getFilters());
                virtualChain.doFilter(request, response);
                return;
            }
        }

        // No chain matched, continue with default
    }
}
```

### Key Points

| Point | Description |
|-------|-------------|
| Single entry point | Sara security filtering yahi se control hota hai |
| Multiple chains support | Different URLs ke liye different security rules |
| Request matching | Pehla matching chain execute hota hai |
| Bean name | Hamesha `springSecurityFilterChain` |

---

## Security Filter Order

**Matlab:** Filters ek specific order mein execute hote hain. Order matters kyunki pehle filter ka result baad wale filters use karte hain.

### Default Filter Order (Spring Security 6+)

```
1.  ForceEagerSessionCreationFilter
2.  ChannelProcessingFilter          (HTTP → HTTPS redirect)
3.  WebAsyncManagerIntegrationFilter
4.  SecurityContextPersistenceFilter (SecurityContext load/save)
5.  HeaderWriterFilter               (Security headers: X-Frame-Options, etc.)
6.  CsrfFilter                       (CSRF token validation)
7.  LogoutFilter                     (Handle logout requests)
8.  OAuth2AuthorizationRequestRedirectFilter
9.  Saml2WebSsoAuthenticationRequestFilter
10. X509AuthenticationFilter
11. AbstractPreAuthenticatedProcessingFilter
12. CasAuthenticationFilter
13. OAuth2LoginAuthenticationFilter
14. Saml2WebSsoAuthenticationFilter
15. UsernamePasswordAuthenticationFilter (Form login)
16. DefaultLoginPageGeneratingFilter
17. DefaultLogoutPageGeneratingFilter
18. ConcurrentSessionFilter
19. PasswordManagementFilter
20. OAuth2AuthorizationCodeGrantFilter
21. RequestCacheAwareFilter
22. SecurityContextHolderAwareRequestFilter
23. JaasAuthenticationFilter
24. RememberMeAuthenticationFilter
25. AnonymousAuthenticationFilter
26. OAuth2ResourceServerFilter (JWT validation)
27. SessionManagementFilter
28. ExceptionTranslationFilter       (Handle AccessDeniedException, AuthenticationException)
29. FilterSecurityInterceptor        (Authorization decisions)
30. SwitchUserFilter
```

### Most Important Filters (Must Know)

| Filter | Position | Purpose |
|--------|----------|---------|
| **SecurityContextPersistenceFilter** | Early | SecurityContext ko session se load/save karta hai |
| **UsernamePasswordAuthenticationFilter** | Middle | Form login credentials process karta hai |
| **OAuth2ResourceServerFilter** | Middle-Late | JWT token validate karta hai |
| **ExceptionTranslationFilter** | Late | Auth errors handle karta hai |
| **FilterSecurityInterceptor** | Last | Authorization check (hasRole, hasAuthority) |

### Adding Custom Filter at Specific Position

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .addFilterBefore(new CustomLoggingFilter(), ChannelProcessingFilter.class)
        .addFilterAfter(new CustomRateLimitFilter(), CsrfFilter.class)
        .addFilterBefore(new CustomAuthFilter(), UsernamePasswordAuthenticationFilter.class);

    return http.build();
}

// Custom filter example
public class CustomLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        System.out.println("Request: " + request.getMethod() + " " + request.getRequestURI());

        filterChain.doFilter(request, response);  // Next filter ko call karo
    }
}
```

### Filter Ordering Methods

```java
http
    .addFilterBefore(customFilter, BeforeFilter.class)   // BeforeFilter se pehle
    .addFilterAfter(customFilter, AfterFilter.class)     // AfterFilter ke baad
    .addFilterAt(customFilter, AtFilter.class)           // AtFilter ki jagah
```

**Warning:** `addFilterAt` use karne pe original filter disable nahi hota, sirf position share karte hain. Generally avoid karein.

---

## OncePerRequestFilter

**Matlab:** Abstract filter jo guarantee karta hai ki **har request pe sirf ek baar** execute hoga, even if forward/include ho.

### Why Needed?

Servlet spec ke according, filter multiple baar call ho sakta hai:
- Request forward ho toh
- Include ho toh
- Error dispatch ho toh

`OncePerRequestFilter` isko prevent karta hai using a flag.

### Implementation

```java
public abstract class OncePerRequestFilter extends GenericFilterBean {

    private static final String ALREADY_FILTERED_SUFFIX = ".FILTERED";

    @Override
    public final void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String alreadyFilteredAttr = getClass().getName() + ALREADY_FILTERED_SUFFIX;

        // Check if already filtered this request
        if (request.getAttribute(alreadyFilteredAttr) != null) {
            chain.doFilter(request, response);  // Skip, just continue
            return;
        }

        // Mark as filtered
        request.setAttribute(alreadyFilteredAttr, Boolean.TRUE);

        try {
            doFilterInternal(httpRequest, (HttpServletResponse) response, chain);
        } finally {
            // Remove attribute after filter completes
            request.removeAttribute(alreadyFilteredAttr);
        }
    }

    // Subclass implements this
    protected abstract void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException;
}
```

### Custom Implementation Example

```java
// Request logging filter
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        log.info("{} {} - Started", request.getMethod(), request.getRequestURI());

        try {
            filterChain.doFilter(request, response);  // Next filter

            long duration = System.currentTimeMillis() - startTime;
            log.info("{} {} - Completed in {}ms, Status: {}",
                     request.getMethod(), request.getRequestURI(), duration, response.getStatus());
        } catch (Exception e) {
            log.error("{} {} - Failed: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
            throw e;
        }
    }

    // Skip logging for specific requests
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/");
    }
}
```

### ShouldNotFilter Override

```java
@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    // Return true to skip filtering for this request
    return request.getRequestURI().equals("/health") ||
           request.getRequestURI().startsWith("/public/");
}
```

### Key Points

| Point | Description |
|-------|-------------|
| Guaranteed single execution | Har request pe sirf ek baar run hoga |
| Forward/include safe | Request forward hone pe dobara run nahi hoga |
| shouldNotFilter() | Override karke specific requests skip kar sakte ho |
| Spring Security standard | Sara Spring Security filters isko extend karte hain |

---

## SecurityContextHolder

**Matlab:** Static class jo current thread ka **SecurityContext** store karta hai. Yahi se aapko current user ki info milti hai.

### Storage Strategies

```java
// Default: ThreadLocal (har thread ka alag context)
SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_THREADLOCAL);

// Inheritable: Parent thread se child thread mein pass hoga
SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);

// Global: All threads share same context (rarely used)
SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_GLOBAL);
```

### Usage

```java
@RestController
public class UserController {

    @GetMapping("/me")
    public String getCurrentUser() {
        // SecurityContextHolder se current authentication get karo
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return "Not authenticated";
        }

        String username = authentication.getName();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        return "User: " + username + ", Roles: " + authorities;
    }

    @GetMapping("/check-role")
    public String checkRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return isAdmin ? "You are admin" : "You are not admin";
    }
}
```

### Alternative Ways to Access Current User

```java
// 1. SecurityContextHolder (static access)
Authentication auth1 = SecurityContextHolder.getContext().getAuthentication();

// 2. Method parameter (recommended for controllers)
@GetMapping("/user")
public String getUser(Authentication authentication) {
    return authentication.getName();
}

// 3. @AuthenticationPrincipal (custom user details)
@GetMapping("/profile")
public String getProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
    return userDetails.getEmail();
}

// 4. Principal interface
@GetMapping("/principal")
public String getPrincipal(Principal principal) {
    return principal.getName();
}
```

### Important Notes

| Point | Description |
|-------|-------------|
| ThreadLocal by default | Har HTTP request alag thread pe handle hoti hai |
| Async requests | `MODE_INHERITABLETHREADLOCAL` use karna padta hai |
| Clear automatically | Request complete hone pe Spring clear kar deta hai |
| Manual clear | `SecurityContextHolder.clearContext()` - logout pe |

---

## SecurityContext

**Matlab:** Container jo **Authentication** object hold karta hai. Ek interface hai jo current security state represent karta hai.

### Interface Definition

```java
public interface SecurityContext extends Serializable {

    // Get current Authentication
    Authentication getAuthentication();

    // Set Authentication (usually after successful login)
    void setAuthentication(Authentication authentication);
}
```

### How it Works

```
HTTP Request arrives
    ↓
SecurityContextPersistenceFilter runs
    ↓
Loads SecurityContext from HttpSession (if exists)
    ↓
Sets SecurityContext in SecurityContextHolder
    ↓
Authentication filters populate SecurityContext.setAuthentication(auth)
    ↓
Your controller/access code reads SecurityContextHolder.getContext().getAuthentication()
    ↓
Request completes
    ↓
SecurityContextPersistenceFilter saves SecurityContext to HttpSession
```

### Manual SecurityContext Usage

```java
// After successful login (manual authentication)
Authentication authentication = new UsernamePasswordAuthenticationToken(
    userDetails,              // Principal (user object)
    null,                     // Credentials (clear after auth)
    userDetails.getAuthorities()  // Roles/Authorities
);

// SecurityContext mein set karo
SecurityContext context = SecurityContextHolder.createEmptyContext();
context.setAuthentication(authentication);
SecurityContextHolder.setContext(context);

// Ab user authenticated hai for this request/session
```

### SecurityContextPersistenceFilter Role

```java
public class SecurityContextPersistenceFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {

        try {
            // 1. Load SecurityContext from session
            SecurityContext context = repository.loadContext(request);

            // 2. Set in holder
            SecurityContextHolder.setContext(context);

            // 3. Continue filter chain (your app runs)
            filterChain.doFilter(request, response);

        } finally {
            // 4. Get context after request
            SecurityContext context = SecurityContextHolder.getContext();

            // 5. Save to session (if authenticated)
            repository.saveContext(context, request, response);

            // 6. Clear holder
            SecurityContextHolder.clearContext();
        }
    }
}
```

---

## Authentication Object

**Matlab:** Core interface jo represent karta hai ki **kaun** user hai aur woh **authenticated** hai ya nahi.

### Interface

```java
public interface Authentication extends Principal {

    // User ki roles/permissions
    Collection<? extends GrantedAuthority> getAuthorities();

    // Password (usually cleared after authentication)
    Object getCredentials();

    // Additional details (IP address, session ID, etc.)
    Map<String, Object> getDetails();

    // User identity (username, User object, etc.)
    Object getPrincipal();

    // Is user authenticated?
    boolean isAuthenticated();

    // Set authenticated status (usually called by AuthenticationManager)
    void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException;
}
```

### Common Implementations

```java
// 1. UsernamePasswordAuthenticationToken (Form login)
Authentication auth = new UsernamePasswordAuthenticationToken(
    "sachin",                    // Principal (username)
    "password123",               // Credentials
    List.of(new SimpleGrantedAuthority("ROLE_USER"))  // Authorities
);

// 2. After successful authentication (authenticated = true)
Authentication authenticatedAuth = new UsernamePasswordAuthenticationToken(
    userDetails,                 // Principal (full User object)
    null,                        // Credentials (cleared)
    userDetails.getAuthorities() // Authorities
);
```

### Authentication Flow

```
User sends username + password
    ↓
UsernamePasswordAuthenticationFilter intercepts
    ↓
Creates unauthenticated AuthenticationToken
    ↓
AuthenticationManager.authenticate(token)
    ↓
AuthenticationProvider validates credentials
    ↓
Returns authenticated AuthenticationToken
    ↓
SecurityContextHolder.getContext().setAuthentication(authToken)
    ↓
User is now authenticated
```

### Custom Authentication Implementation

```java
// Custom UserDetailsService
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList())
        );
    }
}

// In controller - accessing authenticated user
@GetMapping("/profile")
public ResponseEntity<?> getProfile() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    // Get UserDetails object
    UserDetails userDetails = (UserDetails) auth.getPrincipal();

    return ResponseEntity.ok(Map.of(
        "username", userDetails.getUsername(),
        "roles", userDetails.getAuthorities()
    ));
}
```

### Authentication Object Structure

```
Authentication {
    principal: "User object or username"
    credentials: "null (cleared after auth)"
    authorities: [ROLE_USER, ROLE_ADMIN]
    details: {
        remoteAddress: "127.0.0.1",
        sessionId: "ABC123"
    }
    authenticated: true
}
```

---

## GrantedAuthority

**Matlab:** Interface jo user ki **permissions** ya **roles** represent karta hai.

### Interface

```java
public interface GrantedAuthority extends Serializable {

    // Authority name (e.g., "ROLE_USER", "ROLE_ADMIN", "READ_PERMISSION")
    String getAuthority();
}
```

### SimpleGrantedAuthority

```java
// Simple string-based authority
GrantedAuthority userRole = new SimpleGrantedAuthority("ROLE_USER");
GrantedAuthority adminRole = new SimpleGrantedAuthority("ROLE_ADMIN");
GrantedAuthority readPerm = new SimpleGrantedAuthority("READ_PERMISSION");
```

### Role vs Authority

```java
// ROLE_ prefix convention (Spring Security default)
GrantedAuthority roleUser = new SimpleGrantedAuthority("ROLE_USER");     // Role
GrantedAuthority roleAdmin = new SimpleGrantedAuthority("ROLE_ADMIN");   // Role

// Without ROLE_ prefix - fine-grained permissions
GrantedAuthority readPerm = new SimpleGrantedAuthority("user:read");     // Permission
GrantedAuthority writePerm = new SimpleGrantedAuthority("user:write");   // Permission
```

### Usage in Security Config

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            // Role-based access
            .requestMatchers("/admin/**").hasRole("ADMIN")           // Checks "ROLE_ADMIN"
            .requestMatchers("/user/**").hasRole("USER")             // Checks "ROLE_USER"

            // Authority-based access (more granular)
            .requestMatchers("/api/users/read").hasAuthority("user:read")
            .requestMatchers("/api/users/write").hasAuthority("user:write")

            // Multiple roles
            .requestMatchers("/reports/**").hasAnyRole("ADMIN", "MANAGER")

            // Multiple authorities
            .requestMatchers("/api/advanced/**").hasAnyAuthority("user:read", "user:write")

            .anyRequest().authenticated()
        );

    return http.build();
}
```

### Custom User with Authorities

```java
@Entity
public class User {
    @Id
    private Long id;
    private String username;
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Role> roles = new HashSet<>();
}

@Entity
public class Role {
    @Id
    private Long id;
    private String name;  // e.g., "USER", "ADMIN"
}

// Convert to GrantedAuthority
List<GrantedAuthority> authorities = user.getRoles().stream()
    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
    .collect(Collectors.toList());

// Create UserDetails
UserDetails userDetails = User.builder()
    .username(user.getUsername())
    .password(user.getPassword())
    .authorities(authorities)
    .build();
```

### Authority Hierarchy

```
ROLE_ADMIN
├── user:read
├── user:write
├── user:delete
├── post:read
├── post:write
└── post:delete

ROLE_USER
├── user:read
└── post:read
```

---

## Architecture Flow Diagram

### Complete Request Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                        HTTP Request                             │
└─────────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────────┐
│                  DelegatingFilterProxy                          │
│         (Servlet Filter → Spring Bean Bridge)                   │
└─────────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────────┐
│                    FilterChainProxy                             │
│         (Selects appropriate SecurityFilterChain)               │
└─────────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────────┐
│                   Security Filter Chain                         │
│  ┌────────────────────────────────────────────────────┐         │
│  │ 1. SecurityContextPersistenceFilter                │         │
│  │    → Load SecurityContext from session              │         │
│  ├────────────────────────────────────────────────────┤         │
│  │ 2. HeaderWriterFilter                             │         │
│  │    → Add security headers (X-Frame-Options, etc.)  │         │
│  ├────────────────────────────────────────────────────┤         │
│  │ 3. CsrfFilter                                     │         │
│  │    → Validate CSRF token                           │         │
│  ├────────────────────────────────────────────────────┤         │
│  │ 4. LogoutFilter                                   │         │
│  │    → Handle logout requests                        │         │
│  ├────────────────────────────────────────────────────┤         │
│  │ 5. UsernamePasswordAuthenticationFilter            │         │
│  │    → Process login form submission                 │         │
│  ├────────────────────────────────────────────────────┤         │
│  │ 6. OAuth2ResourceServerFilter (if JWT)             │         │
│  │    → Validate JWT token                            │         │
│  ├────────────────────────────────────────────────────┤         │
│  │ 7. RequestCacheAwareFilter                        │         │
│  │    → Restore saved request after login             │         │
│  ├────────────────────────────────────────────────────┤         │
│  │ 8. SecurityContextHolderAwareRequestFilter        │         │
│  │    → Wrap request with security methods            │         │
│  ├────────────────────────────────────────────────────┤         │
│  │ 9. AnonymousAuthenticationFilter                  │         │
│  │    → Set anonymous auth if not authenticated       │         │
│  ├────────────────────────────────────────────────────┤         │
│  │ 10. ExceptionTranslationFilter                    │         │
│  │    → Catch auth/access denied exceptions           │         │
│  ├────────────────────────────────────────────────────┤         │
│  │ 11. FilterSecurityInterceptor (Authorization)     │         │
│  │    → Check @PreAuthorize, hasRole, etc.            │         │
│  └────────────────────────────────────────────────────┘         │
└─────────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────────┐
│                   Your Controller                               │
│         (Access authenticated user here)                        │
│                                                                 │
│  Authentication auth =                                          │
│      SecurityContextHolder.getContext().getAuthentication();    │
│                                                                 │
│  String username = auth.getName();                              │
│  Collection<?> roles = auth.getAuthorities();                   │
└─────────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────────┐
│                     HTTP Response                               │
└─────────────────────────────────────────────────────────────────┘
```

### Authentication Flow (Login)

```
┌──────────────┐
│   User       │
│  Login Form  │
└──────┬───────┘
       ↓ POST /login {username, password}
┌──────────────────────────────────────┐
│  UsernamePasswordAuthenticationFilter│
│  1. Extract credentials              │
│  2. Create unauthenticated token     │
└──────┬───────────────────────────────┘
       ↓
┌──────────────────────────────────────┐
│     AuthenticationManager            │
│  (Orchestrates authentication)       │
└──────┬───────────────────────────────┘
       ↓
┌──────────────────────────────────────┐
│    AuthenticationProvider            │
│  1. LoadUserByUsername                │
│  2. Compare passwords                 │
│  3. Build authenticated token        │
└──────┬───────────────────────────────┘
       ↓
┌──────────────────────────────────────┐
│   SecurityContextHolder              │
│   setAuthentication(authToken)       │
└──────┬───────────────────────────────┘
       ↓
┌──────────────────────────────────────┐
│  SecurityContextPersistenceFilter    │
│  Save SecurityContext to Session     │
└──────┬───────────────────────────────┘
       ↓
┌──────────────┐
│  Redirect to │
│  /dashboard  │
└──────────────┘
```

---

## Quick Reference

```java
// Access current user
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String username = auth.getName();
boolean isAuthenticated = auth.isAuthenticated();
Collection<? extends GrantedAuthority> roles = auth.getAuthorities();

// Check specific role
boolean isAdmin = auth.getAuthorities().stream()
    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

// Create custom filter
public class CustomFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {
        // Your logic
        filterChain.doFilter(request, response);
    }
}

// Add filter to chain
http.addFilterBefore(new CustomFilter(), UsernamePasswordAuthenticationFilter.class);

// Authority naming convention
new SimpleGrantedAuthority("ROLE_USER");    // Role
new SimpleGrantedAuthority("user:read");    // Permission
```

---

## Summary

| Component | Purpose |
|-----------|---------|
| **SecurityFilterChain** | Defines which filters run for which URLs |
| **DelegatingFilterProxy** | Bridge between Servlet container and Spring |
| **FilterChainProxy** | Manages multiple SecurityFilterChains |
| **Filter Order** | Specific sequence matters for security |
| **OncePerRequestFilter** | Ensures filter runs only once per request |
| **SecurityContextHolder** | Stores current user's SecurityContext (ThreadLocal) |
| **SecurityContext** | Holds the Authentication object |
| **Authentication** | Represents who the user is and if they're logged in |
| **GrantedAuthority** | User's roles and permissions |

# Spring Security - Top 10 Interview Questions & Answers

> Covering: Security Architecture, Authentication, Authorization, JWT, Password Encoding, CORS, CSRF, OAuth2, and Best Practices

---

## Q1: Spring Security kya hai aur iska architecture kaise kaam karta hai?

**Answer:**

Spring Security ek framework hai jo application mein **authentication** (kaun aa raha hai) aur **authorization** (kya kar sakta hai) handle karta hai.

### Core Concepts

| Concept | Description |
|---------|-------------|
| **Authentication** | User kaun hai verify karna (login, credentials check) |
| **Authorization** | User kya access kar sakta hai (roles, permissions) |
| **Filter Chain** | Har request ko process karne wala pipeline |

### Architecture Flow

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

### Key Components:

**1. DelegatingFilterProxy:**
Servlet container (Tomcat) aur Spring ke beech ka **bridge**. Servlet filter ko Spring beans access karne deta hai.

```
Servlet Container (Tomcat)
    ↓
DelegatingFilterProxy (javax.servlet.Filter interface implement karta hai)
    ↓
Spring ApplicationContext se bean dhoondta hai (by name: "springSecurityFilterChain")
    ↓
Delegation → FilterChainProxy (actual Spring bean)
```

**2. FilterChainProxy:**
Actual Spring bean jo **multiple SecurityFilterChain** ko manage karta hai aur request ko appropriate chain route karta hai.

```
DelegatingFilterProxy
    ↓ (delegates to)
FilterChainProxy (Bean name: "springSecurityFilterChain")
    ↓ (selects appropriate chain based on request matcher)
SecurityFilterChain 1 (API requests)
SecurityFilterChain 2 (Web requests)
SecurityFilterChain 3 (Actuator endpoints)
```

**3. SecurityFilterChain:**
Ek chain of filters jo decide karta hai ki request secure hai ya nahi. Har HTTP request is chain se guzarti hai.

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

**Multiple SecurityFilterChain:**
```java
@Configuration
@EnableWebSecurity
public class MultiSecurityConfig {

    // API requests - JWT based
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    // Web requests - Form login
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

## Q2: Security Filter Chain mein kaunse filters hote hain aur unka order kya hai?

**Answer:**

Filters ek specific order mein execute hote hain. Order matters kyunki pehle filter ka result baad wale filters use karte hain.

### Most Important Filters:

| Filter | Position | Purpose |
|--------|----------|---------|
| **SecurityContextPersistenceFilter** | Early | SecurityContext ko session se load/save karta hai |
| **CsrfFilter** | Middle | CSRF token validation |
| **UsernamePasswordAuthenticationFilter** | Middle | Form login credentials process karta hai |
| **OAuth2ResourceServerFilter** | Middle-Late | JWT token validate karta hai |
| **ExceptionTranslationFilter** | Late | Auth errors handle karta hai |
| **FilterSecurityInterceptor** | Last | Authorization check (hasRole, hasAuthority) |

### Custom Filter Add Karna:

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

### Filter Ordering Methods:

```java
http
    .addFilterBefore(customFilter, BeforeFilter.class)   // BeforeFilter se pehle
    .addFilterAfter(customFilter, AfterFilter.class)     // AfterFilter ke baad
    .addFilterAt(customFilter, AtFilter.class)           // AtFilter ki jagah
```

### OncePerRequestFilter:

Abstract filter jo guarantee karta hai ki **har request pe sirf ek baar** execute hoga, even if forward/include ho.

```java
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        log.info("{} {} - Started", request.getMethod(), request.getRequestURI());

        try {
            filterChain.doFilter(request, response);
            long duration = System.currentTimeMillis() - startTime;
            log.info("{} {} - Completed in {}ms", request.getRequestURI(), duration);
        } catch (Exception e) {
            log.error("Failed: {}", e.getMessage());
            throw e;
        }
    }

    // Skip specific requests
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/");
    }
}
```

---

## Q3: SecurityContextHolder, SecurityContext, Authentication Object aur GrantedAuthority kya hai?

**Answer:**

### SecurityContextHolder:

Static class jo current thread ka **SecurityContext** store karta hai. Yahi se current user ki info milti hai.

```java
// Default: ThreadLocal (har thread ka alag context)
SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_THREADLOCAL);

// Usage
@RestController
public class UserController {

    @GetMapping("/me")
    public String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return "Not authenticated";
        }

        String username = authentication.getName();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        return "User: " + username + ", Roles: " + authorities;
    }
}
```

**Alternative Ways to Access Current User:**
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
```

### SecurityContext:

Container jo **Authentication** object hold karta hai.

```java
public interface SecurityContext extends Serializable {
    Authentication getAuthentication();
    void setAuthentication(Authentication authentication);
}
```

### Authentication Object:

Core interface jo represent karta hai ki **kaun** user hai aur woh **authenticated** hai ya nahi.

```java
public interface Authentication extends Principal {
    Collection<? extends GrantedAuthority> getAuthorities();  // User ki roles/permissions
    Object getCredentials();                                   // Password (usually cleared)
    Object getPrincipal();                                     // User identity (username/User object)
    boolean isAuthenticated();                                 // Authenticated?
}
```

**Authentication Flow:**
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

### GrantedAuthority:

Interface jo user ki **permissions** ya **roles** represent karta hai.

```java
// ROLE_ prefix convention
GrantedAuthority roleUser = new SimpleGrantedAuthority("ROLE_USER");     // Role
GrantedAuthority roleAdmin = new SimpleGrantedAuthority("ROLE_ADMIN");   // Role

// Without ROLE_ prefix - fine-grained permissions
GrantedAuthority readPerm = new SimpleGrantedAuthority("user:read");     // Permission
GrantedAuthority writePerm = new SimpleGrantedAuthority("user:write");   // Permission
```

---

## Q4: Authentication process kaise kaam karta hai? AuthenticationManager, AuthenticationProvider, UserDetailsService aur UserDetails explain karo.

**Answer:**

### Authentication Components Flow:

```
User sends credentials
    ↓
Authentication Filter intercepts
    ↓
Creates AuthenticationToken (unauthenticated)
    ↓
AuthenticationManager.authenticate(token)
    ↓
AuthenticationProvider.validate()
    ↓
UserDetailsService.loadUserByUsername()
    ↓
Returns UserDetails
    ↓
Password match check
    ↓
Returns authenticated AuthenticationToken
    ↓
SecurityContextHolder.setAuthentication()
    ↓
User is authenticated
```

### AuthenticationManager:

Interface jo authentication process ko **orchestrate** karta hai. Yeh ek facade hai - actual kaam AuthenticationProviders ko delegate karta hai.

```java
public interface AuthenticationManager {
    Authentication authenticate(Authentication authentication) throws AuthenticationException;
}
```

**ProviderManager (Default Implementation):**
```java
public class ProviderManager implements AuthenticationManager {
    private List<AuthenticationProvider> providers;

    @Override
    public Authentication authenticate(Authentication authentication) {
        // Har provider ko try karo
        for (AuthenticationProvider provider : providers) {
            if (provider.supports(authentication.getClass())) {
                Authentication result = provider.authenticate(authentication);
                if (result != null) return result;  // Success!
            }
        }
        throw new ProviderNotFoundException("No provider found");
    }
}
```

### AuthenticationProvider:

Interface jo actual authentication logic implement karta hai.

```java
@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication authentication) {
        String username = authentication.getName();
        String password = (String) authentication.getCredentials();

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        List<GrantedAuthority> authorities = user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
            .collect(Collectors.toList());

        return new UsernamePasswordAuthenticationToken(
            user,           // Principal
            null,           // Credentials (cleared)
            authorities     // Roles
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
```

### UserDetailsService:

Interface jo user data fetch karne ka contract define karta hai. Sirf **ek method** hai.

```java
public interface UserDetailsService {
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}
```

**Database Implementation:**
```java
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            user.isEnabled(),
            true,   // accountNonExpired
            true,   // credentialsNonExpired
            true,   // accountNonLocked
            user.getAuthorities()
        );
    }
}
```

### UserDetails:

Interface jo Spring Security ke liye **user contract** define karta hai.

```java
public interface UserDetails extends Serializable {
    Collection<? extends GrantedAuthority> getAuthorities();
    String getPassword();
    String getUsername();
    boolean isAccountNonExpired();
    boolean isAccountNonLocked();
    boolean isCredentialsNonExpired();
    boolean isEnabled();
}
```

**Implementation Option (Recommended): Separate UserDetails**
```java
public class CustomUserDetails implements UserDetails {

    private final User user;
    private final List<GrantedAuthority> authorities;

    public static CustomUserDetails fromUser(User user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
            .collect(Collectors.toList());
        return new CustomUserDetails(user, authorities);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() { return user.getPassword(); }
    @Override
    public String getUsername() { return user.getUsername(); }
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return user.isEnabled(); }

    public Long getId() { return user.getId(); }
}
```

---

## Q5: Authorization kaise kaam karta hai? URL-based aur Method-level security explain karo.

**Answer:**

**Authorization ka matlab:** User authenticated hai — ab woh **kya kar sakta hai** ya **kya access kar sakta hai** yeh decide karna.

```
Authentication = Kaun ho tum? (login verify karna)
Authorization  = Kya kar sakte ho? (permissions check karna)
```

### URL-based Authorization:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/public/**", "/api/auth/**").permitAll()

                // Role-based access
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/manager/**").hasRole("MANAGER")

                // Authority-based access (fine-grained)
                .requestMatchers("/api/users/read").hasAuthority("user:read")
                .requestMatchers("/api/users/write").hasAuthority("user:write")

                // Multiple roles
                .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "MANAGER")

                // HTTP Method specific
                .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")

                // Default
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults());

        return http.build();
    }
}
```

**Important Notes:**
- Order matters: Pehle specific matchers, baad mein general
- First match wins
- `anyRequest()` hamesha end mein

### Method-Level Authorization:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // @PreAuthorize, @PostAuthorize enable karne ke liye
public class SecurityConfig { }
```

**@PreAuthorize (Most Flexible):**
```java
@Service
public class UserService {

    // Simple role check
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Parameter-based check
    @PreAuthorize("#id == authentication.principal.id")
    public User getUser(Long id) {
        return userRepository.findById(id).orElseThrow();
    }

    // Multiple conditions
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public User getUserDetails(Long id) {
        // Admin kisi ka bhi dekh sakta hai, user sirf apna
    }

    // Custom bean call
    @PreAuthorize("@securityService.isOwner(#postId, authentication.principal.id)")
    public void deletePost(Long postId) { }
}
```

**@PostAuthorize (Return value check):**
```java
@Service
public class DocumentService {

    @PostAuthorize("returnObject.ownerId == authentication.principal.id")
    public Document getDocument(Long id) {
        return documentRepository.findById(id).orElseThrow();
    }
}
```

### URL-based vs Method-Level:

| Feature | URL-based (HttpSecurity) | Method-Level (@PreAuthorize) |
|---------|-------------------------|------------------------------|
| **Where** | SecurityConfig | Service methods |
| **Granularity** | Coarse (URL patterns) | Fine (per method, per parameter) |
| **Flexibility** | Limited | SpEL expressions |
| **Ownership logic** | Nahi kar sakte | `#obj.owner == principal.id` |

### Defense in Depth (Recommended):

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
}
```

---

## Q6: JWT authentication kaise implement karte ho? Complete flow explain karo.

**Answer:**

**JWT (JSON Web Token)** ek stateless authentication mechanism hai jo user ki identity ko ek signed token mein encode karta hai.

### Session vs JWT:

| Feature | Session-based | JWT |
|---------|--------------|-----|
| **State** | Server pe store hota hai | Stateless — server kuch store nahi karta |
| **Storage** | Server memory / Redis | Client side (localStorage, cookie) |
| **Scalability** | Sticky sessions needed | Easy horizontal scaling |
| **Logout** | Simple (session invalidate) | Complex (token blacklist chahiye) |
| **Best for** | Monolith, web apps | REST APIs, microservices, SPAs |

### JWT Structure:

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzYWNoaW4iLCJyb2xlIjoiQURNSU4iLCJleHAiOjE3MTIzNDU2Nzh9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
│                          │                                                           │
├────── Header ────────────┤ ├─────────────────── Payload ──────────────────────────────┤ ├────── Signature ───────────────────────────────────────────────────┤
```

**Header:**
```json
{ "alg": "HS256", "typ": "JWT" }
```

**Payload:**
```json
{
  "sub": "sachin",
  "iat": 1712345678,
  "exp": 1712432078,
  "role": "ADMIN",
  "userId": 42
}
```

**Signature:**
```
HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret_key)
```

### JWT Flow:

```
1. User login karta hai (username/password)
    ↓
2. Server credentials verify karta hai
    ↓
3. Server JWT generate karta hai (signed token)
    ↓
4. Token client ko return hota hai
    ↓
5. Client har request mein token bhejta hai (Authorization header)
    ↓
6. Server token validate karta hai (signature + expiry)
    ↓
7. Valid → user authenticated, Invalid → 401
```

### Implementation:

**1. JwtService (Token Generation & Validation):**
```java
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        if (userDetails instanceof CustomUserDetails customUser) {
            extraClaims.put("userId", customUser.getId());
            extraClaims.put("role", customUser.getAuthorities()
                .stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));
        }
        return Jwts.builder()
            .claims(extraClaims)
            .subject(userDetails.getUsername())
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(getSigningKey())
            .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private Key getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

**2. JwtAuthenticationFilter:**
```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            final String username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                        );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Invalid or expired token\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
```

**3. Security Config:**
```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

### Refresh Token Strategy:

```
Access token (15 min) + Refresh token (7 days)

Jab access token expire ho:
Client → POST /api/auth/refresh {refreshToken}
Server → { "accessToken": "new_token", "refreshToken": "new_refresh_token" }
```

### Token Blacklisting (Logout):

```java
@Service
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    public void blacklistToken(String token) {
        Claims claims = extractClaims(token);
        String jti = claims.getId();
        long remainingTime = claims.getExpiration().getTime() - System.currentTimeMillis();

        redisTemplate.opsForValue().set(
            "blacklist:" + jti, "blacklisted",
            remainingTime, TimeUnit.MILLISECONDS
        );
    }

    public boolean isBlacklisted(String token) {
        Claims claims = extractClaims(token);
        String jti = claims.getId();
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + jti));
    }
}
```

---

## Q7: Password encoding kaise kaam karta hai? Kaunsa encoder use karna chahiye?

**Answer:**

**PasswordEncoder** passwords ko hash karne ka interface hai. Plain text passwords ko **encode** karta hai taaki database mein safe store ho.

### Why Encoding?

```
❌ Plain text (database breach = all passwords exposed)
    users: [{email: "sachin@mail.com", password: "mySecret123"}]

✅ Encoded (breach hone pe bhi passwords safe)
    users: [{email: "sachin@mail.com", password: "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG"}]
```

### Password Encoder Comparison:

| Feature | BCrypt | Argon2 | PBKDF2 | NoOp |
|---------|--------|--------|--------|------|
| **Security** | Good ✅ | Excellent 🏆 | Good ✅ | None ❌ |
| **Speed** | Medium (~70ms) | Slower (~280ms) | Medium (~100ms) | Instant |
| **Memory-hard** | No | Yes ✅ | No | No |
| **GPU resistant** | Partial | Yes ✅ | Partial | No |
| **Recommended** | ✅ Production | High security | Enterprise | ❌ Never prod |

### BCryptPasswordEncoder (Recommended for 90% apps):

```java
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);  // Strength 10 for production
    }
}
```

**Strength vs Time:**
| Strength | Iterations | Approx Time | Use Case |
|----------|------------|-------------|----------|
| **4** | 16 | ~1ms | Development only |
| **10** | 1,024 | ~70ms | ✅ Production standard |
| **12** | 4,096 | ~280ms | High security |
| **14** | 16,384 | ~1.1s | Very high security |

**Usage:**
```java
PasswordEncoder encoder = new BCryptPasswordEncoder(10);

// Same password, different hashes (random salt)
String hash1 = encoder.encode("myPassword123");
// $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG

String hash2 = encoder.encode("myPassword123");
// $2a$10$8K1p/a0dUR1AmkVfT5hFp.3bFkXM/ql5RXNlO7XvJzKQYqFqXGvGm

// Both match same password
encoder.matches("myPassword123", hash1);  // true
encoder.matches("wrongPassword", hash1);  // false
```

### DelegatingPasswordEncoder (Migration Support):

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
}
```

**Format:**
```
{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG
{argon2}$argon2id$v=19$m=65536,t=3,p=4$c29tZXNhbHQ$RdescudvJCsgt3ub+b+daw
{pbkdf2}5d9c3b...
{noop}myPlainPassword  // Dev only!
```

### Best Practices:

1. **Never store plain text** — hamesha encode karo
2. **Use appropriate strength** — BCrypt 10-12 for production
3. **Password validation** — min 8 chars, uppercase, lowercase, digit, special char
4. **Check password breach** — Have I Been Pwned API se verify karo

---

## Q8: CORS kya hai aur Spring Security mein kaise configure karte hain?

**Answer:**

**CORS (Cross-Origin Resource Sharing)** browser ki same-origin policy ko relax karne ka mechanism hai. Ek domain ka frontend doosre domain ke backend se baat kar sake.

### Same-Origin Policy:

```
Same Origin = Same Protocol + Same Host + Same Port

Frontend: http://localhost:3000
Backend:  http://localhost:8080

❌ Different ports → Different origins → Browser blocks request (without CORS)
✅ CORS header → Browser allows request
```

**Important:** CORS sirf **browser** ka restriction hai. Server-to-server calls (Postman, curl, microservices) mein CORS apply nahi hota.

### How CORS Works:

**Simple Request:**
```
Browser → Server
  Origin: http://localhost:3000

Server → Browser
  Access-Control-Allow-Origin: http://localhost:3000
  [Response body]
```

**Preflight Request (Complex Requests):**
```
Browser → Server (OPTIONS request)
  Origin: http://localhost:3000
  Access-Control-Request-Method: DELETE
  Access-Control-Request-Headers: Authorization

Server → Browser (OPTIONS response)
  Access-Control-Allow-Origin: http://localhost:3000
  Access-Control-Allow-Methods: DELETE
  Access-Control-Allow-Headers: Authorization
  Access-Control-Max-Age: 3600

Browser → Server (Actual request)
  [Normal request with headers]
```

### Global CORS Configuration (Recommended):

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    config.setAllowedOriginPatterns(List.of(
        "http://localhost:*",        // Dev: all ports
        "https://*.myapp.com",       // Prod: subdomains
        "https://myapp.com"          // Prod: main domain
    ));

    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setExposedHeaders(List.of("X-Total-Count", "X-Page-Number"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());

    return http.build();
}
```

### @CrossOrigin Annotation (Quick):

```java
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:3000", "https://myapp.com"})
public class UserController {

    @CrossOrigin(origins = "*")
    @GetMapping("/public")
    public PublicData getPublicData() { }
}
```

### Common CORS Mistakes:

| Mistake | Problem | Fix |
|---------|---------|-----|
| `allowedOrigins("*")` + `allowCredentials(true)` | Error! | Use `allowedOriginPatterns("*")` |
| Forgetting OPTIONS method | Preflight fail | Include "OPTIONS" in methods |
| Overly permissive CORS | Security risk | Restrict to known origins |

---

## Q9: CSRF kya hai aur kab disable/enable karna chahiye?

**Answer:**

**CSRF (Cross-Site Request Forgery)** ek attack jahan attacker user ko trick karta hai ek malicious request bhejne ke liye — aur user ki authenticated state ka fayda uthata hai.

### CSRF Attack Example:

```
User logged in hai banking app mein → Browser mein session cookie stored hai
User ek malicious site visit karta hai → Wo site automatically banking app ko request bhejti hai
Browser automatically session cookie attach karta hai → Bank request ko legit samajh leta hai
Money transfer ho jata hai → User ko pata bhi nahi chala
```

### Why CSRF Works:

| Condition | Result |
|-----------|--------|
| User authenticated hai | Session cookie browser mein stored |
| Browser auto-sends cookies | Har request mein cookie attach hota hai |
| Request state-changing hai | POST, PUT, DELETE — data modify hota hai |
| No CSRF token | Server ko pata nahi chalta ki request forged hai |

### CSRF Decision Guide:

```
Is your API stateless (JWT in Authorization header)?
├── Yes → csrf.disable()
└── No → Continue
    │
    Is it a web app with session cookies?
    ├── Yes → Keep CSRF enabled
    │   ├── SPA? → CookieCsrfTokenRepository.withHttpOnlyFalse()
    │   └── Server-rendered? → Default HttpSessionCsrfTokenRepository
    │
    └── No → It's stateless → csrf.disable()
```

### Disable CSRF (Stateless JWT API):

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())  // Stateless → no CSRF
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());

    return http.build();
}
```

**Why JWT Doesn't Need CSRF:**
```
Session Cookie Auth:
- Browser auto-attaches cookie → CSRF possible
- Attacker forges request → Browser sends cookie → Server accepts

JWT Auth:
- Token must be manually added to Authorization header
- Browser doesn't auto-attach JWT
- Attacker can't forge Authorization header
- No CSRF needed
```

### Enable CSRF for SPAs (CookieCsrfTokenRepository):

```java
http
    .csrf(csrf -> csrf
        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
    );
```

**Frontend Integration (React):**
```javascript
// Cookie read karo
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}

// Axios interceptor
axios.interceptors.request.use(config => {
    const token = getCookie('XSRF-TOKEN');
    if (token) {
        config.headers['X-XSRF-TOKEN'] = token;
    }
    return config;
});
```

### SameSite Cookie Attribute:

| Value | Same-site | Cross-site Nav | Cross-site API |
|-------|-----------|----------------|----------------|
| **Strict** | ✅ Sent | ❌ Not sent | ❌ Not sent |
| **Lax** | ✅ Sent | ✅ Sent (GET only) | ❌ Not sent |
| **None** | ✅ Sent | ✅ Sent | ✅ Sent |

```java
ResponseCookie cookie = ResponseCookie.from("JWT", token)
    .httpOnly(true)
    .secure(true)
    .sameSite("Lax")  // CSRF protection
    .build();
```

---

## Q10: OAuth2 aur OpenID Connect kya hai? Authorization Code + PKCE aur Client Credentials flow explain karo.

**Answer:**

**OAuth2** ek authorization framework hai jo third-party apps ko limited access deta hai user ke resources pe — without sharing password.

**OpenID Connect (OIDC)** authentication ke liye hai — OAuth2 ke upar build hota hai.

```
OAuth2 = Authorization (access delegation)
    "I allow this app to access my Google Calendar"

OIDC = Authentication (identity verification)
    "Prove you are this Google user"
```

### OAuth2 Roles:

| Role | Example |
|------|---------|
| **Resource Owner (User)** | Woh insaan jiska data access ho raha hai |
| **Client (App)** | Woh app jo resource access karna chahti hai |
| **Authorization Server** | Google, GitHub, Okta jo tokens issue karta hai |
| **Resource Server (API)** | Woh server jiska data access ho raha hai |

### Grant Types:

| Grant Type | Use Case | User Involved? |
|------------|----------|----------------|
| **Authorization Code + PKCE** | SPAs, mobile apps | ✅ Yes |
| **Client Credentials** | Service-to-service (M2M) | ❌ No |
| **Refresh Token** | Token renewal | ❌ No |

### Authorization Code + PKCE Flow:

```
Step 1: Client generates PKCE pair
    code_verifier = randomString(128)
    code_challenge = SHA256(code_verifier) → Base64URL

Step 2: User redirected to Auth Server
    GET /authorize?response_type=code&client_id=myApp
        &code_challenge=E9Melhoa2OwvFrEMTJ...
        &code_challenge_method=S256

Step 3: User logs in → Auth Server redirects back with code
    GET https://myapp.com/callback?code=SplxlOBeZQQY...

Step 4: Client exchanges code + code_verifier for token
    POST /token
      grant_type=authorization_code
      code=SplxlOBeZQQY...
      code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW...

    Response: {
      "access_token": "eyJ...",
      "token_type": "Bearer",
      "expires_in": 3600,
      "refresh_token": "dGhpc..."
    }
```

**Spring Security OAuth2 Client Setup:**
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: your-client-id
            client-secret: your-client-secret
            scope: openid, profile, email
```

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
            );
        return http.build();
    }
}
```

### Client Credentials Grant (Service-to-Service):

```
Order Service → Auth Server (with client_id + client_secret)

    POST /oauth/token
      grant_type=client_credentials
      client_id=order-service
      client_secret=service-secret
      scope=inventory:read

Auth Server → Order Service (access token)

    {
      "access_token": "eyJ...",
      "token_type": "Bearer",
      "expires_in": 3600,
      "scope": "inventory:read"
    }

Order Service → Inventory Service (with Bearer token)

    GET /api/inventory/123
    Authorization: Bearer eyJ...
```

**Resource Server Config:**
```java
@Configuration
@EnableWebSecurity
public class ResourceServerConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/inventory/**").hasAuthority("SCOPE_inventory:read")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
```

---

## Quick Reference Summary

```java
// Security Config
@EnableWebSecurity
@EnableMethodSecurity

// SecurityFilterChain
.requestMatchers("/public/**").permitAll()
.requestMatchers("/api/**").authenticated()
.requestMatchers("/admin/**").hasRole("ADMIN")
.requestMatchers("/data/**").hasAuthority("data:read")

// Method-level
@PreAuthorize("hasRole('ADMIN')")
@PreAuthorize("#id == authentication.principal.id")
@PostAuthorize("returnObject.owner == authentication.principal.username")

// JWT
Jwts.builder().claims().subject().issuedAt().expiration().signWith().compact()
Jwts.parser().verifyWith(key).build().parseSignedClaims(token)

// Password Encoding
new BCryptPasswordEncoder(10)  // Production standard
PasswordEncoderFactories.createDelegatingPasswordEncoder()

// CORS
config.setAllowedOriginPatterns(List.of("http://localhost:*"))
config.setAllowCredentials(true)

// CSRF
http.csrf(csrf -> csrf.disable())  // Stateless API
http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))  // SPA
```

---

## Yaad Rakhne Wali Baatein

1. **SecurityFilterChain** har request ko filter karti hai — order matters
2. **SecurityContextHolder** se current user ki info milti hai (ThreadLocal by default)
3. **AuthenticationManager** orchestrates, **AuthenticationProvider** validates, **UserDetailsService** fetches user
4. **Constructor Injection** recommended hai — field injection avoid karo
5. **@PreAuthorize** most flexible hai — SpEL expressions support karta hai
6. **JWT stateless hai** — CSRF disable karo jab token Authorization header mein ho
7. **BCrypt strength 10-12** production ke liye standard hai
8. **CORS sirf browser restriction hai** — server-to-server calls mein nahi
9. **CSRF session cookies ke liye zaruri hai** — stateless JWT mein nahi
10. **OAuth2 Authorization Code + PKCE** SPAs ke liye best hai
11. **Client Credentials** microservice-to-microservice authentication ke liye
12. **DelegatingPasswordEncoder** migration ke liye best hai — multiple encoders support

# Security Best Practices

## Status: Not Started

---

## Table of Contents

1. [Security Overview](#security-overview)
2. [Security Headers](#security-headers)
3. [Rate Limiting](#rate-limiting)
4. [Account Lockout](#account-lockout)
5. [Audit Logging](#audit-logging)
6. [Principle of Least Privilege](#principle-of-least-privilege)
7. [OWASP Top 10 Awareness](#owasp-top-10-awareness)
8. [Security Checklist](#security-checklist)

---

## Security Overview

**Matlab:** Security ek layer nahi, ek mindset hai. Har layer pe protection lagao — defense in depth.

### Defense in Depth

```
┌─────────────────────────────────────────────────────────────┐
│  Layer 1: Network Level                                     │
│     → HTTPS/TLS, Firewall, IP Whitelisting                   │
├─────────────────────────────────────────────────────────────┤
│  Layer 2: Application Level                                 │
│     → Spring Security, Authentication, Authorization          │
├─────────────────────────────────────────────────────────────┤
│  Layer 3: Input Validation                                   │
│     → SQL Injection, XSS, CSRF prevention                     │
├─────────────────────────────────────────────────────────────┤
│  Layer 4: Data Level                                         │
│     → Password hashing, Encryption at rest                    │
├─────────────────────────────────────────────────────────────┤
│  Layer 5: Monitoring                                         │
│     → Audit logs, Alerting, Anomaly detection                 │
└─────────────────────────────────────────────────────────────┘
```

---

## Security Headers

**Matlab:** HTTP response headers jo browser ko security instructions dete hain. Spring Security most headers by default add karta hai.

### Default Security Headers (Spring Security)

```java
// Spring Security automatically adds these headers:
// (No extra config needed)

X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
Strict-Transport-Security: max-age=31536000; includeSubDomains (HTTPS only)
```

### Custom Security Headers

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .headers(headers -> headers
            // Frame protection (clickjacking prevention)
            .frameOptions(frame -> frame.deny())

            // Content type sniffing prevention
            .contentTypeOptions(Customizer.withDefaults())

            // XSS protection
            .xssProtection(xss -> xss.headerValue(
                XXssProtectionHeaderWriterHeaderValue.BLOCK
            ))

            // HSTS (HTTPS enforcement)
            .httpStrictTransportSecurity(hsts -> hsts
                .includeSubDomains(true)
                .maxAgeInSeconds(31536000)  // 1 year
                .preload(true)
            )

            // Content Security Policy
            .contentSecurityPolicy(csp -> csp
                .policyDirectives("default-src 'self'; " +
                    "script-src 'self' https://trusted-cdn.com; " +
                    "style-src 'self' 'unsafe-inline'; " +
                    "img-src 'self' data: https:; " +
                    "font-src 'self' https://fonts.gstatic.com; " +
                    "connect-src 'self' https://api.myapp.com; " +
                    "frame-ancestors 'none'; " +
                    "form-action 'self'; " +
                    "base-uri 'self'; " +
                    "object-src 'none'")
            )

            // Referrer Policy
            .referrerPolicy(referrer -> referrer
                .policy(ReferrerPolicy.SAME_ORIGIN)
            )

            // Permissions Policy (formerly Feature Policy)
            .permissionsPolicy(permissions -> permissions
                .policy("camera=(), microphone=(), geolocation=(), payment='self'")
            )

            // Cache control for sensitive endpoints
            .cacheControl(Customizer.withDefaults())
        );

    return http.build();
}
```

### Header Reference

| Header | Value | Purpose |
|--------|-------|---------|
| **X-Content-Type-Options** | `nosniff` | Browser MIME type sniff band karo |
| **X-Frame-Options** | `DENY` | iframe mein load nahi hoga (clickjacking prevention) |
| **X-XSS-Protection** | `1; mode=block` | Browser XSS filter enable (legacy browsers) |
| **Strict-Transport-Security** | `max-age=31536000; includeSubDomains` | HTTPS enforce karo |
| **Content-Security-Policy** | Custom directives | Scripts, styles, images sources restrict karo |
| **Referrer-Policy** | `same-origin` | Referrer URL cross-origin requests mein mat bhejo |
| **Permissions-Policy** | `camera=(), microphone=()` | Browser features disable karo |
| **Cache-Control** | `no-store` | Sensitive data cache nahi hoga |

### Content Security Policy (CSP) Examples

```
Strict CSP (recommended for most apps):
    default-src 'self';
    script-src 'self';
    style-src 'self' 'unsafe-inline';
    img-src 'self' data:;
    font-src 'self';
    connect-src 'self';
    frame-ancestors 'none';

With CDN:
    default-src 'self';
    script-src 'self' https://cdn.jsdelivr.net https://cdn.cloudflare.com;
    style-src 'self' https://cdn.jsdelivr.net 'unsafe-inline';
    img-src 'self' data: https:;
    font-src 'self' https://fonts.gstatic.com;
    connect-src 'self' https://api.myapp.com;

Report-only (testing pehli — violations report hongi, block nahi):
    Content-Security-Policy-Report-Only: default-src 'self'; report-uri /csp-report
```

### CSP Violation Reporting

```java
@RestController
public class CspReportController {

    @PostMapping("/csp-report")
    public ResponseEntity<?> reportViolation(@RequestBody Map<String, Object> report) {
        Map<String, Object> cspReport = (Map<String, Object>) report.get("csp-report");

        log.warn("CSP Violation: document-uri={}, violated-directive={}, blocked-uri={}",
            cspReport.get("document-uri"),
            cspReport.get("violated-directive"),
            cspReport.get("blocked-uri")
        );

        return ResponseEntity.ok().build();
    }
}
```

---

## Rate Limiting

**Matlab:** Ek user/IP se aane wale requests ko limit karo taaki brute force ya DDoS attacks se bacha ja sake.

### Why Rate Limiting?

```
Without rate limiting:
    Attacker → 1000 login attempts/second → Brute force password
    Bot → 10000 API calls/second → Server down (DoS)
    Scraper → All data download in minutes → Data theft

With rate limiting:
    User → 5 login attempts/minute → Then blocked temporarily
    API → 100 requests/minute → Then 429 Too Many Requests
```

### Bucket4j Implementation (Recommended)

```xml
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.7.0</version>
</dependency>
```

### In-Memory Rate Limiter

```java
@Service
public class RateLimiterService {

    // IP-based rate limiting
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket createNewBucket() {
        Refill refill = Refill.intervally(10, Duration.ofMinutes(1));  // 10 requests/minute
        Bandwidth limit = Bandwidth.classic(10, refill);  // Burst capacity: 10

        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    public boolean tryConsume(String ip) {
        Bucket bucket = buckets.computeIfAbsent(ip, key -> createNewBucket());
        return bucket.tryConsume(1);
    }
}

// Rate limiting filter
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ip = request.getRemoteAddr();

        if (!rateLimiterService.tryConsume(ip)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.addHeader("X-Rate-Limit-Remaining", "0");
            response.getWriter().write("{\"error\": \"Too many requests. Try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
```

### Login Attempt Rate Limiting

```java
@Service
public class LoginAttemptService {

    // Failed attempts tracker
    private final Map<String, Integer> attempts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lockouts = new ConcurrentHashMap<>();

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 30;

    // Login fail pe increment
    public void loginFailed(String ip) {
        int currentAttempts = attempts.getOrDefault(ip, 0);
        attempts.put(ip, currentAttempts + 1);

        // Max attempts cross kiye → lockout
        if (currentAttempts + 1 >= MAX_ATTEMPTS) {
            lockouts.put(ip, LocalDateTime.now());
        }
    }

    // Login success pe reset
    public void loginSucceeded(String ip) {
        attempts.remove(ip);
        lockouts.remove(ip);
    }

    // Check if IP is locked out
    public boolean isLockedOut(String ip) {
        LocalDateTime lockoutTime = lockouts.get(ip);

        if (lockoutTime == null) {
            return false;  // No lockout
        }

        // Lockout duration check
        if (lockoutTime.plusMinutes(LOCKOUT_DURATION_MINUTES).isAfter(LocalDateTime.now())) {
            return true;  // Still locked
        }

        // Lockout expire ho gaya → clear
        lockouts.remove(ip);
        attempts.remove(ip);
        return false;
    }

    public int getRemainingAttempts(String ip) {
        return Math.max(0, MAX_ATTEMPTS - attempts.getOrDefault(ip, 0));
    }
}
```

### Custom Authentication Failure Handler

```java
@Component
@RequiredArgsConstructor
public class CustomAuthFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) {
        String ip = request.getRemoteAddr();
        loginAttemptService.loginFailed(ip);

        int remaining = loginAttemptService.getRemainingAttempts(ip);

        if (loginAttemptService.isLockedOut(ip)) {
            exception = new LockedException(
                "Account locked. Try again after 30 minutes."
            );
        } else {
            exception = new BadCredentialsException(
                "Invalid credentials. " + remaining + " attempts remaining."
            );
        }

        super.onAuthenticationFailure(request, response, exception);
    }
}

// Register in security config
http.formLogin(form -> form
    .failureHandler(customAuthFailureHandler)
);

// Success handler pe reset
http.formLogin(form -> form
    .successHandler((request, response, authentication) -> {
        loginAttemptService.loginSucceeded(request.getRemoteAddr());
        // Continue...
    })
);
```

### Distributed Rate Limiting (Redis)

```java
@Service
@RequiredArgsConstructor
public class RedisRateLimiter {

    private final RedisTemplate<String, String> redisTemplate;

    public boolean tryConsume(String key, long limit, Duration duration) {
        String redisKey = "rate_limit:" + key;

        // Sliding window counter
        long now = System.currentTimeMillis();
        long windowStart = now - duration.toMillis();

        // Remove old entries
        redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);

        // Count current window
        Long count = redisTemplate.opsForZSet().zCard(redisKey);

        if (count != null && count >= limit) {
            return false;  // Rate limit exceeded
        }

        // Add current request
        redisTemplate.opsForZSet().add(redisKey, String.valueOf(now), now);

        // Set TTL
        redisTemplate.expire(redisKey, duration);

        return true;
    }
}
```

---

## Account Lockout

**Matlab:** Multiple failed login attempts pe account temporarily lock karna. Brute force attacks se bachata hai.

### Spring Security Built-in Lockout

```java
// Account lockout on failed attempts
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .formLogin(form -> form
            .failureHandler((request, response, exception) -> {
                // Failed attempt handle karo
                response.sendRedirect("/login?error=" + exception.getMessage());
            })
        )
        .userDetailsService(userDetailsService());

    return http.build();
}
```

### Database-Backed Account Lockout

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    private Long id;

    private String username;
    private String password;
    private boolean enabled = true;

    // Account lockout fields
    private int failedLoginAttempts = 0;
    private LocalDateTime lockedUntil;

    public boolean isAccountLocked() {
        if (lockedUntil == null) return false;
        return LocalDateTime.now().isBefore(lockedUntil);
    }

    public void recordFailedLogin() {
        this.failedLoginAttempts++;

        if (this.failedLoginAttempts >= 5) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(30);  // 30 min lockout
        }
    }

    public void recordSuccessfulLogin() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    public int getRemainingAttempts() {
        return Math.max(0, 5 - failedLoginAttempts);
    }
}
```

### Updated UserDetailsService

```java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Check if account is locked
        if (user.isAccountLocked()) {
            long minutesLeft = ChronoUnit.MINUTES.between(
                LocalDateTime.now(), user.getLockedUntil()
            );
            throw new LockedException(
                "Account locked. Try again in " + minutesLeft + " minutes."
            );
        }

        // Check if disabled
        if (!user.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }

        return CustomUserDetails.fromUser(user);
    }
}
```

### Auth Event Listeners

```java
@Component
@RequiredArgsConstructor
public class AuthEventListener {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    // Login success
    @EventListener
    public void onLoginSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        userRepository.findByUsername(username).ifPresent(user -> {
            user.recordSuccessfulLogin();
            userRepository.save(user);
        });
    }

    // Login failure
    @EventListener
    public void onLoginFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = event.getAuthentication().getName();
        userRepository.findByUsername(username).ifPresent(user -> {
            user.recordFailedLogin();
            userRepository.save(user);
        });
    }
}
```

### Progressive Lockout

```java
public class User {

    private int failedLoginAttempts = 0;
    private LocalDateTime lockedUntil;

    public void recordFailedLogin() {
        this.failedLoginAttempts++;

        // Progressive lockout: badhate jaao
        if (this.failedLoginAttempts >= 20) {
            this.lockedUntil = LocalDateTime.now().plusDays(1);     // 20 failures → 1 day
        } else if (this.failedLoginAttempts >= 10) {
            this.lockedUntil = LocalDateTime.now().plusHours(1);     // 10 failures → 1 hour
        } else if (this.failedLoginAttempts >= 5) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(30);  // 5 failures → 30 min
        }
    }
}
```

---

## Audit Logging

**Matlab:** Security-related events ko log karna — kaun login kiya, kya change kiya, kya access kiya. Compliance aur forensics ke liye zaruri hai.

### What to Log

| Event | What to Log |
|-------|-------------|
| **Login success/failure** | Username, IP, timestamp, user agent |
| **Password change** | Username, who changed, timestamp |
| **Role/permission change** | Who changed what, old → new, timestamp |
| **Data access (sensitive)** | Who accessed what, timestamp |
| **Account lockout** | Username, failed attempts count, lockout duration |
| **API key creation/deletion** | Who created, what key, timestamp |

### Audit Log Entity

```java
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventType;       // LOGIN_SUCCESS, LOGIN_FAILURE, PASSWORD_CHANGE, etc.

    @Column(nullable = false)
    private String username;        // Kaun

    @Column
    private String ipAddress;       // Kahan se

    @Column
    private String userAgent;       // Kya use kiya

    @Column
    private String resource;        // Kya access/change kiya

    @Column(columnDefinition = "TEXT")
    private String details;         // Extra info (JSON)

    @Column(nullable = false)
    private boolean success;        // Success ya failure

    @Column(nullable = false)
    private LocalDateTime timestamp;  // Kab

    public static AuditLog of(String eventType, String username,
                               HttpServletRequest request, String resource,
                               String details, boolean success) {
        AuditLog log = new AuditLog();
        log.eventType = eventType;
        log.username = username;
        log.ipAddress = getClientIp(request);
        log.userAgent = request.getHeader("User-Agent");
        log.resource = resource;
        log.details = details;
        log.success = success;
        log.timestamp = LocalDateTime.now();
        return log;
    }

    private static String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

### Audit Service

```java
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async  // Async log karo (performance)
    public void log(String eventType, String username,
                    HttpServletRequest request, String resource,
                    String details, boolean success) {

        AuditLog log = AuditLog.of(
            eventType, username, request, resource, details, success
        );

        auditLogRepository.save(log);
    }

    // Query methods
    public List<AuditLog> getLoginFailures(String username, LocalDateTime since) {
        return auditLogRepository.findByEventTypeAndUsernameAndTimestampAfter(
            "LOGIN_FAILURE", username, since
        );
    }

    public List<AuditLog> getUserActivity(String username, LocalDateTime since) {
        return auditLogRepository.findByUsernameAndTimestampAfter(username, since);
    }
}
```

### Event-Based Audit

```java
@Component
@RequiredArgsConstructor
public class SecurityAuditListener {

    private final AuditService auditService;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        HttpServletRequest request = getCurrentRequest();
        auditService.log(
            "LOGIN_SUCCESS",
            event.getAuthentication().getName(),
            request,
            "/login",
            "Authentication method: " + event.getAuthentication().getClass().getSimpleName(),
            true
        );
    }

    @EventListener
    public void onBadCredentials(AuthenticationFailureBadCredentialsEvent event) {
        HttpServletRequest request = getCurrentRequest();
        auditService.log(
            "LOGIN_FAILURE",
            event.getAuthentication().getName(),
            request,
            "/login",
            "Reason: Bad credentials",
            false
        );
    }

    @EventListener
    public void onLockedException(AuthenticationFailureLockedEvent event) {
        HttpServletRequest request = getCurrentRequest();
        auditService.log(
            "LOGIN_FAILURE",
            event.getAuthentication().getName(),
            request,
            "/login",
            "Reason: Account locked",
            false
        );
    }
}
```

### Structured Logging (JSON)

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

```xml
<!-- logback-spring.xml -->
<appender name="AUDIT" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/audit.log</file>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>logs/audit.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
        <maxFileSize>100MB</maxFileSize>
        <maxHistory>90</maxHistory>
    </rollingPolicy>
</appender>

<logger name="com.app.audit" level="INFO" additivity="false">
    <appender-ref ref="AUDIT"/>
</logger>
```

---

## Principle of Least Privilege

**Matlab:** Har user/service ko sirf utna access do jitna usko actually chahiye. Zyada access nahi.

### Implementation

```java
// ❌ Bad: Broad permissions
.requestMatchers("/api/**").hasRole("USER")

// ✅ Good: Granular permissions
.requestMatchers("/api/users/{id}").hasAuthority("user:read")
.requestMatchers("/api/users").hasAuthority("user:write")
.requestMatchers("/api/users/{id}").hasAuthority("user:delete")
```

### Role Hierarchy

```java
// Role hierarchy: Admin > Manager > User
@Bean
public RoleHierarchy roleHierarchy() {
    RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
    hierarchy.setHierarchy("""
        ROLE_ADMIN > ROLE_MANAGER
        ROLE_MANAGER > ROLE_USER
        ROLE_USER > ROLE_GUEST
    """);
    return hierarchy;
}

// Admin automatically gets Manager, User, Guest permissions
// Manager gets User, Guest permissions
// User gets Guest permissions
```

### Method-Level Least Privilege

```java
@Service
public class UserService {

    // Sirf apna data — no admin role needed
    @PreAuthorize("#id == authentication.principal.id")
    public User getMyProfile(Long id) {
        return userRepository.findById(id).orElseThrow();
    }

    // Admin-only operation
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    // Manager or Admin
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public List<User> getTeamMembers(Long managerId) {
        return userRepository.findByManagerId(managerId);
    }
}
```

### Service Account Permissions

```yaml
# Microservice service accounts — minimal permissions
service-accounts:
  order-service:
    scopes:
      - inventory:read        # Sirf read, write nahi
      - payment:create       # Sirf create, delete nahi
      - notification:send     # Sirf send, read nahi

  inventory-service:
    scopes:
      - inventory:read
      - inventory:write
      # No payment access
      # No notification access
```

---

## OWASP Top 10 Awareness

**Matlab:** Web application security ki top 10 vulnerabilities. Har developer ko inko samajhna chahiye.

### OWASP Top 10 (2021)

| # | Vulnerability | Prevention |
|---|--------------|--------------|
| **A01** | Broken Access Control | @PreAuthorize, URL-based auth, role hierarchy |
| **A02** | Cryptographic Failures | BCrypt password hashing, HTTPS, encrypted secrets |
| **A03** | Injection | Parameterized queries, input validation, ORM |
| **A04** | Insecure Design | Threat modeling, secure design patterns |
| **A05** | Security Misconfiguration | Disable debug mode, remove defaults, security headers |
| **A06** | Vulnerable Components | Dependency updates, SCA tools, patching |
| **A07** | Auth Failures | MFA, session management, brute force protection |
| **A08** | Data Integrity Failures | CSP, SRI, signed dependencies |
| **A09** | Logging Failures | Audit logs, monitoring, alerting |
| **A10** | SSRF | URL validation, allowlisting, network segmentation |

### A01: Broken Access Control

```java
// ❌ IDOR (Insecure Direct Object Reference)
@GetMapping("/api/users/{id}")
public User getUser(@PathVariable Long id) {
    return userRepository.findById(id).orElseThrow();  // Koi bhi kisi ka bhi data access kar sakta hai
}

// ✅ Fixed
@GetMapping("/api/users/{id}")
@PreAuthorize("#id == authentication.principal.id or hasRole('ADMIN')")
public User getUser(@PathVariable Long id) {
    return userRepository.findById(id).orElseThrow();
}
```

### A03: Injection (SQL)

```java
// ❌ SQL Injection
@Query(value = "SELECT * FROM users WHERE name = '" + name + "'", nativeQuery = true)

// ✅ Parameterized query
@Query("SELECT u FROM User u WHERE u.name = :name")
List<User> findByName(@Param("name") String name);

// JPA/ORM already prevents SQL injection — but native queries mein careful raho
```

### A05: Security Misconfiguration

```yaml
# ❌ Production mein yeh settings nahi hone chahiye
spring:
  h2:
    console:
      enabled: true  # Database console exposed
  devtools:
    restart:
      enabled: true  # Dev auto-restart

# ✅ Profile-based config
# application-prod.yml
spring:
  h2:
    console:
      enabled: false
  devtools:
    restart:
      enabled: false
```

```java
// ❌ Stack trace expose karna
@ExceptionHandler(Exception.class)
public ResponseEntity<?> handleException(Exception e) {
    return ResponseEntity.status(500).body(Map.of(
        "error", e.getMessage(),
        "stackTrace", Arrays.toString(e.getStackTrace())  // Internal details leak
    ));
}

// ✅ Generic error response
@ExceptionHandler(Exception.class)
public ResponseEntity<?> handleException(Exception e) {
    log.error("Internal error", e);  // Server pe log karo
    return ResponseEntity.status(500).body(Map.of(
        "error", "An internal error occurred"  // Generic message to client
    ));
}
```

### A07: Authentication Failures

```java
// ✅ MFA (Multi-Factor Authentication)
@Service
public class MfaService {

    public String generateSecretKey() {
        return Base32.random().encode(RandomUtils.nextBytes(20));
    }

    public boolean verifyTotp(String secretKey, String code) {
        Totp totp = new Totp(secretKey);
        return totp.verify(code);
    }

    public String generateQrCodeUrl(String secretKey, String username) {
        return "otpauth://totp/MyApp:" + username +
            "?secret=" + secretKey +
            "&issuer=MyApp";
    }
}
```

---

## Security Checklist

### Pre-Production Checklist

```
□ HTTPS enabled everywhere (redirect HTTP → HTTPS)
□ Password hashing: BCrypt (strength 10-12)
□ CSRF protection enabled (if session-based auth)
□ CORS configured (not overly permissive)
□ Security headers set (CSP, HSTS, X-Frame-Options, etc.)
□ Rate limiting on login endpoints
□ Account lockout after N failed attempts
□ Input validation on all endpoints
□ SQL injection prevention (parameterized queries)
□ Sensitive data not logged (passwords, tokens, PII)
□ Debug mode disabled in production
□ Default passwords changed
□ Unused endpoints removed (Swagger, H2 console, Actuator)
□ Dependencies updated (no known CVEs)
□ Environment variables for secrets (no hardcoded secrets)
□ Audit logging enabled
□ Error messages don't leak internal details
```

### Spring Security Defaults (Already Protected)

```
✅ X-Content-Type-Options: nosniff
✅ X-Frame-Options: DENY
✅ X-XSS-Protection: 1; mode=block
✅ Cache-Control: no-cache, no-store
✅ CSRF token (if enabled)
✅ Session fixation protection
✅ Secure cookie flags (with HTTPS)
```

---

## Quick Reference

```java
// Security headers
http.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
    .httpStrictTransportSecurity(hsts -> hsts
        .maxAgeInSeconds(31536000).includeSubDomains(true).preload(true))
);

// Rate limiting
if (!rateLimiter.tryConsume(ip)) {
    return ResponseEntity.status(429).body("Too many requests");
}

// Account lockout
if (user.isAccountLocked()) {
    throw new LockedException("Account locked for 30 minutes");
}

// Audit logging
auditService.log("LOGIN_FAILURE", username, request, "/login", details, false);

// Least privilege
@PreAuthorize("#id == authentication.principal.id or hasRole('ADMIN')")

// Always apply security headers — Spring Security does most by default
```

---

## Summary

| Practice | Why | How |
|----------|-----|-----|
| **Security Headers** | Browser-level protection | Spring Security defaults + CSP |
| **Rate Limiting** | Brute force/DoS prevention | Bucket4j, Redis |
| **Account Lockout** | Credential stuffing prevention | Failed attempt counter + lockout |
| **Audit Logging** | Compliance, forensics | Event listeners, structured logs |
| **Least Privilege** | Minimize blast radius | Fine-grained permissions |
| **OWASP Top 10** | Known vulnerability prevention | Code review, security testing |

**Golden Rule:** Always apply security headers — Spring Security does most by default. Layer your defenses — no single control is sufficient.

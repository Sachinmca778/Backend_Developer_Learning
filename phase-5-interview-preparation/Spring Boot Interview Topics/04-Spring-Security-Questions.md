# Spring Security Questions

## Status: Not Started

---

## Table of Contents

1. [How Spring Security Works (Big Picture)](#how-spring-security-works-big-picture)
2. [Filter Chain Walkthrough](#filter-chain-walkthrough)
3. [Spring Security 6 Lambda Config](#spring-security-6-lambda-config)
4. [Authentication vs Authorization](#authentication-vs-authorization)
5. [JWT Stateless vs Session-Based](#jwt-stateless-vs-session-based)
6. [CSRF — Kab Disable?](#csrf--kab-disable)
7. [`@PreAuthorize` vs `@Secured` vs `@RolesAllowed`](#preauthorize-vs-secured-vs-rolesallowed)
8. [SecurityContextHolder & ThreadLocal](#securitycontextholder--threadlocal)
9. [Async / `@Async` Context Propagation](#async--async-context-propagation)
10. [Common Output Traps](#common-output-traps)
11. [Pitfalls](#pitfalls)
12. [Cheat Sheet](#cheat-sheet)

---

## How Spring Security Works (Big Picture)

```
HTTP Request
   ↓
DelegatingFilterProxy (servlet filter)
   ↓
FilterChainProxy
   ↓
SecurityFilterChain (list of filters)
   ↓
Authentication / Authorization decisions
   ↓
DispatcherServlet → Controller
```

> "Security ek **filter chain** hai jo har request ko intercept karta hai, authentication / authorization decision leta hai, phir agla filter / controller call karta hai."

---

## Filter Chain Walkthrough

Common filters (Spring Security 6 default order):

```
1. DisableEncodeUrlFilter
2. WebAsyncManagerIntegrationFilter
3. SecurityContextHolderFilter           ← context load/save
4. HeaderWriterFilter                     ← X-Frame-Options, HSTS
5. CorsFilter                             ← CORS
6. CsrfFilter                             ← CSRF token check (stateful)
7. LogoutFilter
8. (Auth specific) UsernamePasswordAuthenticationFilter / JwtAuthFilter
9. DefaultLoginPageGeneratingFilter
10. RequestCacheAwareFilter
11. SecurityContextHolderAwareRequestFilter
12. AnonymousAuthenticationFilter         ← if not authenticated, set anonymous
13. ExceptionTranslationFilter            ← AccessDenied / Auth exceptions → HTTP responses
14. AuthorizationFilter                   ← final authorize check
```

→ Har request **all filters** sequentially.

### `DelegatingFilterProxy`

Servlet container is unaware of Spring beans → `DelegatingFilterProxy` bridge.

---

## Spring Security 6 Lambda Config

### Pre-Spring Security 5.7 (deprecated)

```java
@Configuration
public class SecConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
            .antMatchers("/admin").hasRole("ADMIN")
            .anyRequest().authenticated();
    }
}
```

### Modern (Spring Security 5.7+ / 6+)

```java
@Configuration
@EnableMethodSecurity                        // method-level @PreAuthorize
public class SecConfig {

    @Bean
    public SecurityFilterChain api(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())                        // for stateless API
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### Multiple chains

```java
@Bean @Order(1) SecurityFilterChain admin(...) { ... }
@Bean @Order(2) SecurityFilterChain api(...) { ... }
```

---

## Authentication vs Authorization

| | Authentication | Authorization |
|--|----------------|----------------|
| Question | Tu kaun hai? | Kya allowed hai? |
| Output | `Authentication` object (principal + authorities) | Permit / Deny |
| Where | UsernamePasswordAuthFilter / JwtFilter | AuthorizationFilter / `@PreAuthorize` |

---

## JWT Stateless vs Session-Based

### Session-based (default Spring Security)

```
Login → server creates session → JSESSIONID cookie
Subsequent → session lookup (server memory / Redis) → user info
```

**Pros:** Easy invalidation (delete session); CSRF mature defenses; small cookie.
**Cons:** Server-side state; sticky sessions or shared store.

### JWT stateless

```
Login → server returns signed JWT
Subsequent → Authorization: Bearer <token>
Server validates signature; no DB hit
```

**Pros:** Stateless → easy horizontal scale; cross-domain friendly.
**Cons:** Token revocation hard (use short expiry + refresh + blocklist); bigger header; key rotation pain.

### Trade-off cheat

| Need | Pick |
|------|------|
| Server-rendered web app | Session |
| API for SPA / mobile | JWT |
| Need fast invalidation | Session (or JWT + denylist) |
| Cross-service / SSO | JWT (or OAuth2 with reference tokens) |

→ Cross-ref: `phase-4 / Microservices / Spring Cloud Gateway` for OAuth2 patterns.

---

## CSRF — Kab Disable?

### Kya hai?

CSRF (Cross-Site Request Forgery) — attacker dusri site se POST kara dega user ke session ke saath.

### Spring's default

CSRF **enabled** for state-changing requests (POST/PUT/DELETE) — token validation.

### Disable kab?

| Scenario | Disable? |
|----------|---------|
| Stateless REST API + JWT | ✅ Disable (no cookies → no CSRF vector) |
| Session-based browser app | ❌ keep enabled |
| Hybrid (cookie session + AJAX) | ❌ keep + token via header |

### Code

```java
http.csrf(csrf -> csrf.disable());                   // pure API
// or
http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
```

---

## `@PreAuthorize` vs `@Secured` vs `@RolesAllowed`

| | `@PreAuthorize` | `@Secured` | `@RolesAllowed` (JSR-250) |
|--|-----------------|-----------|---------------------------|
| Power | Full SpEL | Basic role list | Basic role list |
| Args use | ✅ `#id`, `principal` | ❌ | ❌ |
| Enable | `@EnableMethodSecurity` (default true) | `securedEnabled=true` | `jsr250Enabled=true` |
| Verbose | More | Less | Less |

### `@PreAuthorize` examples

```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long id) { ... }

@PreAuthorize("hasAuthority('ORDER_WRITE')")
public Order create(...) { ... }

@PreAuthorize("hasRole('USER') and #userId == authentication.principal.id")
public Profile getProfile(Long userId) { ... }

@PreAuthorize("@accessGuard.canEdit(#postId, principal)")
public Post edit(Long postId) { ... }   // delegate to a bean
```

### `@PostAuthorize` (after method, returnObject access)

```java
@PostAuthorize("returnObject.owner == authentication.name")
public Document load(Long id) { ... }
```

### `@PreFilter` / `@PostFilter`

Filter collections by SpEL.

→ **Modern advice:** Default to **`@PreAuthorize`** — most powerful.

---

## SecurityContextHolder & ThreadLocal

```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String user = auth.getName();
```

### Internally — ThreadLocal

```java
private static final ThreadLocal<SecurityContext> contextHolder = new ThreadLocal<>();
```

→ Per-request thread mein authentication store. Different threads ko **automatically inherit nahi**.

### Modes

```java
SecurityContextHolder.setStrategyName(MODE_THREADLOCAL);            // default
SecurityContextHolder.setStrategyName(MODE_INHERITABLETHREADLOCAL); // child threads inherit
SecurityContextHolder.setStrategyName(MODE_GLOBAL);                  // single global (rare)
```

---

## Async / `@Async` Context Propagation

### Trap

```java
@Service
class S {
    @Async
    public void background() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        // a == null in default ThreadLocal mode!
    }
}
```

→ Async thread separate hai → ThreadLocal not inherited.

### Fix 1 — Inheritable ThreadLocal

```java
SecurityContextHolder.setStrategyName(MODE_INHERITABLETHREADLOCAL);
```

⚠️ Pool threads reused → context leak across requests; usually NOT recommended in pooled scenarios.

### Fix 2 — `DelegatingSecurityContextExecutor` (recommended)

```java
@Bean
public Executor taskExecutor() {
    ThreadPoolTaskExecutor base = new ThreadPoolTaskExecutor();
    base.setCorePoolSize(8);
    base.initialize();
    return new DelegatingSecurityContextExecutor(base);
}
```

→ Wrapper context capture + restore + clear correctly.

### Fix 3 — Manual capture

```java
SecurityContext ctx = SecurityContextHolder.getContext();
CompletableFuture.runAsync(() -> {
    SecurityContextHolder.setContext(ctx);
    try { ... } finally { SecurityContextHolder.clearContext(); }
});
```

---

## Common Output Traps

### Q1. CSRF disabled aur cookies allowed

→ Open to CSRF if cookies are credentials.

### Q2. `@PreAuthorize` on private method

```java
@PreAuthorize("hasRole('ADMIN')")
private void delete() { ... }   // ❌ no proxy interception
```

### Q3. `@EnableMethodSecurity` missing

```java
@PreAuthorize(...)   // silently ignored without @EnableMethodSecurity
```

### Q4. Async + missing principal

(See above.)

### Q5. `permitAll()` after `authenticated()`

```java
.requestMatchers("/public/**").authenticated()
.requestMatchers("/public/health").permitAll()    // never reached — first matched wins
```

→ **More specific patterns first.**

---

## Pitfalls

1. **`/public` after `anyRequest().authenticated()`** — order matters; specific first.
2. **CSRF disabled with sessions** — vulnerability.
3. **JWT secret in code/config not rotated** — long-term risk.
4. **Storing JWT in localStorage** — XSS risk; HttpOnly cookie generally safer (with CSRF mitigations).
5. **`@PreAuthorize` without enabling method security** — no-op.
6. **Method-level annotations on private/final/static** — no-op.
7. **Async without context propagation** — null principal.
8. **`MODE_INHERITABLETHREADLOCAL` + thread pool** — context leak.
9. **Using `hasRole('ROLE_ADMIN')`** — Spring auto-prepends `ROLE_`; either use `hasRole('ADMIN')` or `hasAuthority('ROLE_ADMIN')`.
10. **Filter ordering** with custom filters — wrong place breaks chain.

---

## Cheat Sheet

| Topic | Quick |
|-------|-------|
| Filter chain | DelegatingFilterProxy → FilterChainProxy → SecurityFilterChain |
| Disable CSRF | Stateless API only |
| JWT | Stateless, scale-friendly; revocation tricky |
| Session | Stateful, easy invalidation |
| Method sec | `@EnableMethodSecurity` + `@PreAuthorize` |
| Context | `SecurityContextHolder` (ThreadLocal) |
| Async | `DelegatingSecurityContextExecutor` |
| Roles | `hasRole('X')` (auto-prefix) vs `hasAuthority('ROLE_X')` |

| Annotation | Use |
|-----------|-----|
| `@PreAuthorize` | SpEL pre-method |
| `@PostAuthorize` | SpEL post-method (returnObject) |
| `@Secured` | Simple roles |
| `@RolesAllowed` | JSR-250 |

---

## Practice

1. Lambda DSL config + `@PreAuthorize` permit-all/admin/user setup.
2. JWT resource server config (Spring Security 6 OAuth2 RS).
3. Stateless API → CSRF disable confirm via failing curl POST without token.
4. `@Async` method calling `SecurityContextHolder` — null principal demonstrate.
5. `DelegatingSecurityContextExecutor` se fix.
6. Custom filter add via `addFilterBefore` / `addFilterAfter` and inspect order.

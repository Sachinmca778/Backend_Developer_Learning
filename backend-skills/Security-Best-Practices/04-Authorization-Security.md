# Authorization Security

## Status: Not Started

---

## Table of Contents

1. [Authorization Quick Recap](#authorization-quick-recap)
2. [Common Vulnerabilities](#common-vulnerabilities)
3. [Object-Level Authorization (BOLA / IDOR)](#object-level-authorization-bola--idor)
4. [Function-Level Authorization (BFLA)](#function-level-authorization-bfla)
5. [Privilege Escalation](#privilege-escalation)
6. [Models: RBAC / ABAC / PBAC / ReBAC](#models-rbac--abac--pbac--rebac)
7. [Implementation Patterns](#implementation-patterns)
8. [Spring Security Examples](#spring-security-examples)
9. [Testing Authorization](#testing-authorization)
10. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Authorization Quick Recap

**Matlab:** AuthN ke baad — "is identified user **allowed** to do this?".

```
AuthN: "Yeh Rahul hai" (token verified)
AuthZ: "Kya Rahul order #123 dekh sakta hai?" (depends on rules)
```

OWASP A01 (Broken Access Control) is **the #1 most common** web app issue (94% of apps in 2021 dataset).

---

## Common Vulnerabilities

### 1. BOLA / IDOR (Object-Level)

User changes ID → accesses someone else's data.

### 2. BFLA (Function-Level)

User hits admin endpoint without admin role.

### 3. Mass Assignment

User submits `role=admin` field → server applies it.

### 4. Horizontal Privilege Escalation

User-A accesses User-B's data (same role level).

### 5. Vertical Privilege Escalation

User upgrades to admin via flaw.

---

## Object-Level Authorization (BOLA / IDOR)

### Vulnerable

```java
@GetMapping("/orders/{id}")
public Order get(@PathVariable Long id) {
    return orderRepo.findById(id).orElseThrow();
}
```

→ Auth check missing → user 5 fetches `/orders/12345` (user 7's order).

### Fix 1: Filter by Owner in Query

```java
@GetMapping("/orders/{id}")
public Order get(@PathVariable Long id, @AuthenticationPrincipal User u) {
    return orderRepo.findByIdAndUserId(id, u.getId())
                    .orElseThrow(NotFoundException::new);
}
```

✅ DB query itself enforces ownership — no separate check.

### Fix 2: Service-Level Check

```java
public Order get(Long id, User u) {
    Order o = orderRepo.findById(id).orElseThrow(NotFoundException::new);
    if (!o.getUserId().equals(u.getId())) throw new NotFoundException();
    return o;
}
```

⚠️ Return **404** (Not Found), **not 403** (Forbidden) — to avoid revealing existence (info leak).

### Fix 3: Centralized Policy

```java
@PreAuthorize("@orderAccess.canRead(#id, principal)")
@GetMapping("/orders/{id}")
public Order get(@PathVariable Long id) { ... }

@Component
public class OrderAccess {
    public boolean canRead(Long orderId, User principal) {
        return orderRepo.existsByIdAndUserId(orderId, principal.getId())
            || principal.hasRole("ADMIN");
    }
}
```

### Avoid Sequential IDs

`/orders/123` allows enumeration. Use **UUIDs** to make guessing harder (defense in depth — not primary control).

```java
@Id
private UUID id = UUID.randomUUID();
```

---

## Function-Level Authorization (BFLA)

### Vulnerable

```java
@DeleteMapping("/admin/users/{id}")
public void deleteUser(@PathVariable Long id) {
    userRepo.deleteById(id);   // no role check!
}
```

→ Regular user calls this → user deleted.

### Fix

```java
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/admin/users/{id}")
public void deleteUser(@PathVariable Long id) { ... }
```

Or method-level config in `SecurityFilterChain`:

```java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/admin/**").hasRole("ADMIN")
    .requestMatchers("/api/**").authenticated()
    .anyRequest().denyAll()                       // default deny!
);
```

### Default Deny

> If no rule matches → deny.

Many frameworks default to **allow** when no rule → vulnerable. Always explicit deny fallback.

---

## Privilege Escalation

### Vertical (User → Admin)

```java
// Vulnerable
@PutMapping("/users/me")
public User update(@RequestBody User input, @AuthenticationPrincipal User u) {
    u.setName(input.getName());
    u.setRole(input.getRole());          // ← user can set role=ADMIN!
    return userRepo.save(u);
}
```

### Mass Assignment Fix

Use **DTOs** with **only allowed fields**:

```java
public class UpdateProfileRequest {
    @NotBlank
    private String name;
    private String avatarUrl;
    // role intentionally absent
}

@PutMapping("/users/me")
public User update(@Valid @RequestBody UpdateProfileRequest req,
                   @AuthenticationPrincipal User u) {
    u.setName(req.getName());
    u.setAvatarUrl(req.getAvatarUrl());
    return userRepo.save(u);
}
```

→ Different DTO for admin endpoints (where role can be set).

### Horizontal (User-A → User-B same level)

Same fix as BOLA — ownership check on every resource access.

---

## Models: RBAC / ABAC / PBAC / ReBAC

### RBAC — Role-Based Access Control

**Matlab:** Roles assigned to users; permissions assigned to roles.

```
User Rahul → ROLE_EDITOR → can edit articles
User Priya → ROLE_ADMIN → can edit + delete + manage users
```

```sql
users:           id, email
roles:           id, name (USER, EDITOR, ADMIN)
user_roles:      user_id, role_id
permissions:     id, name (article.edit, article.delete)
role_permissions: role_id, permission_id
```

✅ Simple, common
❌ Hard with many fine-grained variations ("editors of Indian articles only")

### ABAC — Attribute-Based Access Control

**Matlab:** Decisions based on attributes of user, resource, action, context.

```
Allow if:
  user.department == resource.department
  AND time is business hours
  AND user.location is whitelisted
```

✅ Fine-grained, context-aware
❌ Complex to debug

### PBAC — Policy-Based

ABAC ka structured cousin — policies in declarative language.

Tools: **OPA (Open Policy Agent)** with Rego, **Cedar** (AWS), **Casbin**.

```rego
package authz

allow {
  input.user.role == "admin"
}

allow {
  input.action == "read"
  input.user.id == input.resource.owner_id
}
```

### ReBAC — Relationship-Based

**Google Zanzibar style** (used by Google Drive sharing). Permissions via relations.

```
user:rahul    is editor of    folder:foo
folder:foo    parent of       file:bar
→ user:rahul can edit file:bar
```

Tools: **SpiceDB**, **OpenFGA**.

✅ Hierarchies, sharing
❌ Complex setup

### Choosing

| Use case | Model |
|----------|-------|
| Simple admin/user/guest | RBAC |
| Multi-tenant fine-grained | ABAC / PBAC |
| Document/folder sharing | ReBAC |
| Compliance-heavy (banking) | PBAC with audit |

---

## Implementation Patterns

### 1. Default Deny

Every endpoint matched against allow rules; unmatched → deny.

### 2. Centralized Policy Decision

Don't scatter `if (user.role == "admin")` everywhere — single decision point.

```java
@Component
public class OrderPolicy {
    public boolean canRead(Order order, User user) { ... }
    public boolean canCancel(Order order, User user) { ... }
    public boolean canRefund(Order order, User user) { ... }
}
```

### 3. Resource-Loaded Authorization

Load resource → check ownership → proceed:

```java
public OrderDto get(Long id, User u) {
    Order o = orderRepo.findById(id).orElseThrow(NotFoundException::new);
    if (!policy.canRead(o, u)) throw new NotFoundException();
    return mapper.toDto(o);
}
```

### 4. Query-Level Filtering

For lists, filter at DB:

```java
// User can only see their orders
orderRepo.findByUserId(user.getId());

// Admin can see all
if (user.hasRole("ADMIN")) {
    orderRepo.findAll();
}
```

### 5. Tenant Isolation

For multi-tenant SaaS — every query MUST filter by tenant.

```java
@Filter(name = "tenant", condition = "tenant_id = :tenantId")
@FilterDef(name = "tenant", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Entity
public class Order { ... }
```

Or row-level security (Postgres RLS):

```sql
CREATE POLICY tenant_isolation ON orders
USING (tenant_id = current_setting('app.tenant_id')::int);
```

---

## Spring Security Examples

### Roles & URL Rules

```java
@Bean
SecurityFilterChain security(HttpSecurity http) throws Exception {
    http
      .authorizeHttpRequests(auth -> auth
          .requestMatchers("/public/**").permitAll()
          .requestMatchers("/admin/**").hasRole("ADMIN")
          .requestMatchers(HttpMethod.POST, "/api/orders").hasAuthority("order.create")
          .anyRequest().authenticated()
      )
      .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
    return http.build();
}
```

### Method Security

```java
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {}
```

```java
@Service
public class OrderService {

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteAll() { ... }

    // SpEL with arguments
    @PreAuthorize("@orderPolicy.canRead(#id, principal)")
    public Order get(Long id) { ... }

    @PostAuthorize("returnObject.userId == principal.id")
    public Order findInternal(Long id) { ... }   // checks AFTER load
}
```

### Custom JWT Authentication Converter

Map JWT claims to Spring authorities:

```java
@Bean
JwtAuthenticationConverter jwtConverter() {
    JwtGrantedAuthoritiesConverter granted = new JwtGrantedAuthoritiesConverter();
    granted.setAuthoritiesClaimName("permissions");
    granted.setAuthorityPrefix("");

    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(granted);
    return converter;
}
```

---

## Testing Authorization

### Unit / Integration Tests

```java
@Test
@WithMockUser(roles = "USER")
void user_cannot_access_admin_endpoint() throws Exception {
    mockMvc.perform(delete("/admin/users/1"))
           .andExpect(status().isForbidden());
}

@Test
@WithMockUser(username = "userA")
void userA_cannot_read_userBs_order() throws Exception {
    mockMvc.perform(get("/orders/" + userBOrderId))
           .andExpect(status().isNotFound());
}
```

### Authorization Test Matrix

For each endpoint, test:

| Scenario | Expected |
|----------|----------|
| Unauthenticated | 401 |
| Authenticated, no permission | 403 / 404 |
| Authenticated, owner | 200 |
| Authenticated, admin override | 200 |
| Cross-tenant access | 404 |

### Continuous

- Every new endpoint → at least one negative auth test
- CI fails if untested endpoint detected (some teams build linters for this)

### Pen Testing

- Burp Suite Authz extension
- Manual testing different users + token swaps

---

## Summary Cheat Sheet

| Concept | Quick Note |
|---------|-----------|
| **AuthN** vs **AuthZ** | Who vs What allowed |
| **Default deny** | Unmatched = deny |
| **BOLA / IDOR** | Always ownership check |
| **BFLA** | Role check on every privileged endpoint |
| **Mass assignment** | DTO with allowed fields only |
| **Return 404 not 403** | For unauthorized access to specific objects |
| **RBAC** | Roles → permissions |
| **ABAC** | Attribute conditions |
| **PBAC** | Declarative policies (OPA) |
| **ReBAC** | Relationship graph |
| **Centralize** | Policy in one place, not scattered |
| **Multi-tenant** | Always filter by tenant |
| **`@PreAuthorize`** | Spring method-level |
| **Test matrix** | Auth scenarios per endpoint |

---

## Practice

1. Pick an endpoint that takes an `id`; add ownership check via repo + 404 on mismatch.
2. Identify mass-assignment risk in your app — convert to DTO.
3. Convert hardcoded `if (role == ADMIN)` to centralized policy class.
4. Implement RBAC schema with `users / roles / permissions / role_permissions`.
5. Write `@PreAuthorize` test cases for both authorized + unauthorized.
6. Try OPA (Rego) as external policy decision point — call from Spring.

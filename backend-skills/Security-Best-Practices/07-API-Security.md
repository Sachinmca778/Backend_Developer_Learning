# API Security

## Status: Not Started

---

## Table of Contents

1. [HTTPS Everywhere](#https-everywhere)
2. [TLS Configuration](#tls-configuration)
3. [Certificate Pinning (Mobile)](#certificate-pinning-mobile)
4. [API Keys vs OAuth2 Tokens](#api-keys-vs-oauth2-tokens)
5. [OAuth2 Flows](#oauth2-flows)
6. [PKCE for Public Clients](#pkce-for-public-clients)
7. [Token Storage (Frontend)](#token-storage-frontend)
8. [CORS — Cross-Origin Resource Sharing](#cors--cross-origin-resource-sharing)
9. [Rate Limiting](#rate-limiting)
10. [Other API Hardening](#other-api-hardening)
11. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## HTTPS Everywhere

**Matlab:** **No exceptions** — all API traffic over TLS.

### Why

- Network sniffing → tokens stolen
- MITM → response/request tampering
- Modern browsers / app stores require HTTPS

### Practices

- ✅ Redirect HTTP → HTTPS (301)
- ✅ HSTS header (force HTTPS even on first visit after redirect)
- ✅ Submit domain to **HSTS preload list** (browser ships preconfigured)

```http
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
```

- ✅ Free certs from Let's Encrypt (auto-renewing)
- ✅ Wildcard / SAN certs for subdomains

---

## TLS Configuration

→ Deep dive in `Networking-&-Protocols/06-SSL-TLS.md`. API perspective summary:

### Minimum

- **TLS 1.2 minimum** (preferably TLS 1.3 only)
- Disable TLS 1.0, 1.1, SSL 3.0 entirely
- Strong cipher suites only (no RC4, 3DES, export ciphers)
- Forward Secrecy (ECDHE, DHE)

### Test Your TLS

- [ssllabs.com/ssltest](https://www.ssllabs.com/ssltest/) — aim for **A+**
- [observatory.mozilla.org](https://observatory.mozilla.org/) — security headers + TLS

### Recommended Configs

[ssl-config.mozilla.org](https://ssl-config.mozilla.org/) — auto-generates configs for nginx / Apache / HAProxy / Java / Python / etc.

---

## Certificate Pinning (Mobile)

**Matlab:** Mobile app **only specific certificate** trust kare — system trust store ke alawa.

### Threat Mitigated

Compromised CA / corporate proxy MITM (with installed root) → app would normally trust → pinning blocks.

### Approaches

#### 1. Cert Pinning

Pin specific cert. Breaks on cert renewal — **not recommended**.

#### 2. SPKI Pinning (Recommended)

Pin **Subject Public Key Info** — survives cert renewal as long as key reused.

```kotlin
// Android (OkHttp)
val pinner = CertificatePinner.Builder()
    .add("api.example.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
    .add("api.example.com", "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=") // backup
    .build()

val client = OkHttpClient.Builder()
    .certificatePinner(pinner)
    .build()
```

```swift
// iOS — TrustKit library or NSURLSession delegate
```

### Best Practices

- Always **2 pins** (current + backup) — avoid bricking app on renewal
- Plan ahead before key rotation
- Have **kill-switch** to disable pinning remotely (config flag)
- Don't pin web apps (browsers ignore)

---

## API Keys vs OAuth2 Tokens

### API Keys

**Matlab:** Static, long-lived keys identifying the **caller (often a service / dev)**.

```
GET /api/v1/data
Authorization: Bearer sk_live_abc123...
```

**Use cases:**
- Service-to-service (internal)
- Developer / SDK access
- Stripe-style account-bound keys

**Pros:**
- Simple, easy to use
- No flow / redirect complexity

**Cons:**
- Long-lived → high blast radius if leaked
- Can't represent end-user identity
- Hard to scope finely

### OAuth2 Access Tokens

**Matlab:** Short-lived tokens issued via OAuth2 flows — represent **delegated user authorization**.

```
GET /api/v1/me
Authorization: Bearer eyJ...JWT...
```

**Use cases:**
- 3rd party app accessing user's data
- Mobile / SPA accessing your API on behalf of user

**Pros:**
- Short-lived (minutes) → low blast radius
- Scoped (read-only, write specific resources)
- Can revoke without changing user creds

### When What?

| Need | Use |
|------|-----|
| Server-to-server, simple | API key (mTLS even better) |
| User context required | OAuth2 |
| Public clients (mobile, SPA) | OAuth2 + PKCE |
| Dev/SDK access | API key |

### API Key Best Practices

- ✅ Random (CSPRNG), 256+ bits entropy
- ✅ Hash before storing (like passwords)
- ✅ Show full key **once** at creation, then prefix only
- ✅ Allow scope: read-only, restricted IPs, expiry
- ✅ Rotation UI (multiple active keys per principal)
- ✅ Audit log (last used, calls per minute)

```
Created key: sk_live_abc123_xyz789
After:       sk_live_abc1...      (only prefix shown)
DB stored:   sha256(full)
```

---

## OAuth2 Flows

OAuth2 = **delegated authorization** standard. OIDC builds on it for **authentication**.

### Roles

- **Resource Owner** — user
- **Client** — app wanting access
- **Authorization Server** — issues tokens
- **Resource Server** — your API serving data

### Flows

#### 1. Authorization Code (with PKCE)

**For:** Web apps, mobile, SPA — most common today.

```
1. User clicks "Sign in with X"
2. Browser → AuthZ Server with code_challenge
3. User authenticates, consents
4. AuthZ Server → redirect with code
5. Client → AuthZ server: exchange code + code_verifier → tokens
6. Client uses access_token to call Resource Server
```

#### 2. Client Credentials

**For:** Service-to-service (no user).

```
Client → AuthZ Server: client_id + client_secret → access_token
Client → API: Bearer token
```

#### 3. Resource Owner Password (Deprecated)

**Don't use.** Username/password posted to OAuth server.

#### 4. Implicit (Deprecated)

Replaced by Auth Code + PKCE.

#### 5. Device Code

**For:** TV, CLI, IoT — devices without browser.

```
Device shows code → user visits URL on phone → enters code → device polls token endpoint
```

### Tokens

- **Access Token** — short-lived (5-60 min), bearer
- **Refresh Token** — long-lived, exchange for new access tokens
- **ID Token** (OIDC) — JWT with user identity claims

---

## PKCE for Public Clients

**Proof Key for Code Exchange** — extension to Auth Code flow for **public clients** (mobile, SPA — no client secret).

### Why?

Public clients can't store secrets. Authorization code interception attack:

```
Bad app intercepts redirect with code → exchanges for tokens
```

PKCE blocks this.

### How PKCE Works

```
1. Client generates code_verifier (random 43-128 chars)
2. Client computes code_challenge = SHA256(code_verifier) base64url
3. Authorize request includes code_challenge
4. AuthZ server stores code_challenge with code
5. Client redeems code + code_verifier
6. AuthZ server: SHA256(verifier) == challenge? → issue tokens
```

Even if attacker gets the code, they don't have the verifier.

### Required For

- Mobile apps (native)
- SPAs (browser JS)
- Any "public" client

OAuth 2.1 makes PKCE **mandatory** for all auth code flows.

### Spring Authorization Server (Issuer)

Configurable; PKCE default in modern setups.

---

## Token Storage (Frontend)

### Options

| Storage | XSS-safe | CSRF-safe | Survives reload | Notes |
|---------|----------|-----------|-----------------|-------|
| **HttpOnly cookie** | ✅ | ❌ (need SameSite/CSRF token) | ✅ | Best balance |
| **localStorage** | ❌ | ✅ | ✅ | XSS = full token theft |
| **sessionStorage** | ❌ | ✅ | ❌ (cleared on tab close) | Same XSS issue |
| **In-memory (JS variable)** | ✅ (best) | ✅ | ❌ | Lost on refresh |

### Recommended Pattern

```
Refresh token → HttpOnly Secure SameSite=Strict cookie
Access token → in-memory only (refreshed via cookie when expired)
```

→ XSS can't steal access token (in memory only); CSRF blocked by SameSite.

### Don't

❌ JWT in localStorage on a site with any XSS surface
❌ Long-lived tokens in cookies without SameSite
❌ "Bearer token" pattern + cookie storage (CSRF-prone)

### CSRF Protection (When Cookies Used)

- `SameSite=Lax/Strict` cookies (most modern protection)
- Double-submit cookie pattern
- CSRF token (synchronizer pattern)
- Custom header (`X-Requested-With`) check (since custom headers trigger CORS preflight)

---

## CORS — Cross-Origin Resource Sharing

**Matlab:** Browser security — frontend at `app.example.com` can't call API at `api.other.com` unless explicitly allowed.

### How CORS Works

#### Simple Request

```
Browser → API: GET /data
                Origin: https://app.example.com
API → Browser: Access-Control-Allow-Origin: https://app.example.com
```

If Origin allowed, browser releases response to JS.

#### Preflight (Complex Requests)

For non-simple methods (PUT, DELETE) or custom headers:

```
Browser → API: OPTIONS /data
                Origin: https://app.example.com
                Access-Control-Request-Method: PUT
                Access-Control-Request-Headers: Authorization

API → Browser: Access-Control-Allow-Origin: https://app.example.com
               Access-Control-Allow-Methods: GET, POST, PUT, DELETE
               Access-Control-Allow-Headers: Authorization, Content-Type
               Access-Control-Max-Age: 600
```

Then actual request.

### Spring CORS

```java
@Bean
public CorsConfigurationSource corsConfig() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(List.of("https://app.example.com"));
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
    cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    cfg.setAllowCredentials(true);     // for cookies/auth headers
    cfg.setMaxAge(3600L);
    
    UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", cfg);
    return src;
}
```

### CORS Mistakes

```java
// ❌ Wide open with credentials → CSRF disaster
cfg.setAllowedOriginPatterns(List.of("*"));
cfg.setAllowCredentials(true);
// → spec actually disallows this combo, but broken implementations accept

// ✅ Explicit origins + credentials
cfg.setAllowedOrigins(List.of("https://app.example.com", "https://admin.example.com"));
cfg.setAllowCredentials(true);
```

### CORS ≠ Auth

CORS is a browser-only protection. Tools like curl, Postman ignore CORS. **Server-side authn/authz still required**.

---

## Rate Limiting

→ Detailed in `API-Design-&-Architecture/06-Rate-Limiting.md`. Security perspective:

### Endpoints to Protect

- Login (brute force)
- Password reset (enumeration / spam)
- Sign up (account farming)
- Token issuance (DoS)
- Search / expensive queries
- File uploads

### Per-Identity vs Per-IP

```
Anonymous → per IP (with header trust caution)
Authenticated → per user (and/or per token)
Tier-based → per plan
```

### 429 Response

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 60
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1715000000
```

---

## Other API Hardening

### 1. Idempotency Keys for Writes

Prevent duplicate from retries (`API-Design/07-Idempotency.md`):

```http
POST /payments
Idempotency-Key: 5cf2d4c0-...
```

### 2. Request Size Limits

```yaml
spring.servlet.multipart.max-request-size: 10MB
server.tomcat.max-http-form-post-size: 1MB
```

### 3. HTTP Method Restrictions

Default deny — only document declared methods. Avoid `OPTIONS` reflection / TRACE.

### 4. Verbose Error Hiding

```java
// ❌ Leaks internals
{"error": "java.sql.SQLException: ORA-01017: invalid credentials"}

// ✅
{"error": "internal_error", "request_id": "abc-123"}
```

### 5. Consistent Auth Errors

`401` for missing/invalid auth, `403` for present but not allowed. **Don't** leak resource existence (`404` vs `403` distinction in BOLA).

### 6. API Versioning

`/v1/`, `/v2/` — security fixes can break clients; versioning gives migration path.

### 7. Webhook Signing

```
HMAC = sha256(secret, body)
X-Signature: sha256=<HMAC>
```

Recipient verifies → only valid if signed with shared secret.

### 8. mTLS for Internal Services

(Networking file.)

### 9. WAF (Web Application Firewall)

- Cloudflare, AWS WAF, Akamai
- Block common attacks (SQLi patterns, XSS, OWASP rule sets)
- DDoS protection

### 10. Bot Protection

- reCAPTCHA, hCaptcha, Cloudflare Turnstile
- Bot Manager services (Cloudflare, DataDome, PerimeterX)

---

## Summary Cheat Sheet

| Concept | Quick Note |
|---------|-----------|
| HTTPS only | HSTS + redirect + preload |
| TLS 1.2+ | Disable old protocols |
| Cert pinning | Mobile only, with backup pin |
| API keys | Hashed, scoped, rotatable |
| OAuth2 | Delegated authz, scoped tokens |
| PKCE | Mandatory for public clients |
| Token storage | HttpOnly cookie or memory |
| `localStorage` for tokens | ❌ XSS risk |
| CORS | Explicit origins, careful with credentials |
| 401 vs 403 | Missing vs forbidden |
| Rate limit | Login, signup, expensive ops |
| Idempotency | Key for unsafe writes |
| Webhook signing | HMAC verification |
| WAF / DDoS | Edge protection |

---

## Practice

1. Run [ssllabs.com](https://www.ssllabs.com/ssltest/) on your API — fix anything below A.
2. Add HSTS header (long max-age + preload).
3. Implement OAuth2 Auth Code + PKCE flow with Spring Authorization Server.
4. Move SPA token storage from localStorage to HttpOnly cookie + memory.
5. Configure CORS with explicit origins (no `*` with credentials).
6. Generate + sign a webhook with HMAC; build verifier endpoint.
7. Set up Cloudflare / AWS WAF basic rule set in front of your API.

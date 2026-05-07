# Authentication Security

## Status: Not Started

---

## Table of Contents

1. [Authentication vs Authorization](#authentication-vs-authorization)
2. [Password Storage](#password-storage)
3. [Brute Force Protection](#brute-force-protection)
4. [Account Enumeration](#account-enumeration)
5. [Session Management](#session-management)
6. [Session Fixation](#session-fixation)
7. [JWT Security](#jwt-security)
8. [HS256 vs RS256](#hs256-vs-rs256)
9. [MFA (Multi-Factor Auth)](#mfa-multi-factor-auth)
10. [Password Reset Flows](#password-reset-flows)
11. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Authentication vs Authorization

| | Authentication (AuthN) | Authorization (AuthZ) |
|--|------------------------|----------------------|
| Question | "Who are you?" | "What can you do?" |
| Example | Login with email + password | Can this user delete this order? |
| File | This one | `04-Authorization-Security.md` |

---

## Password Storage

### Never Do

```
❌ Plaintext             "password123" in DB
❌ Encryption (reversible) AES-encrypted password
❌ MD5                    MD5 broken since 1996
❌ SHA1                   broken
❌ SHA256 alone          fast → brute-forceable
```

### Always Do

**One-way hashing with salt + slow function.**

| Algorithm | Status |
|-----------|--------|
| **bcrypt** | ✅ industry standard (since 1999, still solid) |
| **scrypt** | ✅ memory-hard, good |
| **argon2** | ✅ modern OWASP recommendation (id variant) |
| **PBKDF2** | ✅ FIPS-compliant alternative |

### Why slow + salt?

- **Salt**: per-user random value → rainbow tables defeated
- **Slow** (cost factor): brute force computationally expensive — millions/sec → thousands/sec
- **Cost factor adjustable** as hardware speeds up

### Java with Spring Security

```java
// Bean
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);     // cost = 12 (~250ms per hash)
}

// Hashing on signup
String hashed = passwordEncoder.encode(rawPassword);
userRepo.save(new User(email, hashed));

// Verify on login
boolean matches = passwordEncoder.matches(rawPassword, user.getPasswordHash());
```

### bcrypt Output Format

```
$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
 │ │ │  ├──22 chars salt──┤├──31 chars hash──────────────────┤
 │ │ │
 │ │ └─ cost = 12 (2^12 = 4096 iterations)
 │ └─── version
 └───── identifier
```

### Argon2id (Recommended Today)

```java
// Spring Security
@Bean
public PasswordEncoder argon2() {
    return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
}
```

Tune memory (MB), iterations, parallelism for ~250ms hash time.

### Migration Strategy (Existing System)

1. New signups use new algorithm
2. On login: verify with old, **re-hash** with new, update DB
3. Track per-user algorithm column or use `DelegatingPasswordEncoder` (Spring) auto-detects from prefix

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    // Stores hashes prefixed: {bcrypt}$2a$..., {argon2}$argon2id$...
}
```

### Pepper (Optional Extra)

Server-side secret added before hashing. Compromised DB without pepper → still hard.

```
hash = bcrypt(password + pepper, salt)
```

Pepper stored separately (KMS, Vault) — not in DB.

---

## Brute Force Protection

### Threat

Attacker tries millions of passwords against accounts.

### Defenses (Layered)

#### 1. Rate Limiting

```
Per IP:        max 10 login attempts / minute
Per user:      max 5 failed attempts / 15 min → lock
Per device:    fingerprinted
```

#### 2. Exponential Backoff

```
Attempt 1: instant
Attempt 2: 1s delay
Attempt 3: 2s
Attempt 4: 4s
...
```

#### 3. CAPTCHA

After N failures, show challenge (reCAPTCHA, hCaptcha, Cloudflare Turnstile).

#### 4. Account Lockout

```
After 5 failures: temporary lock 15 min
After 20 failures: permanent until admin/email reset
```

⚠️ Lockout can be **DoS** if attacker tries random emails to lock everyone — pair with email-only lock + CAPTCHA + IP reputation.

#### 5. Notify User

Email after suspicious activity ("New login from Mumbai" / "5 failed attempts").

#### 6. Monitor Aggregate

Spike in failed logins across many users → coordinated attack → escalate.

#### 7. Strong Password Policy + Breach Check

- Min length 12+
- Block top-1000 passwords
- Check **haveibeenpwned** API (k-anonymity SHA1 prefix lookup)

---

## Account Enumeration

**Matlab:** Login responses se attacker pata laga sakta hai ki kaunsa email **registered** hai.

### Bad Patterns

```
"Email not found"        ← reveals which emails exist
"Password incorrect"     ← reveals email exists
"Account locked"         ← exists + has been attacked
```

### Forgot Password

```
"We sent a reset link to that email"   ← always show this
                                          (whether email exists or not)
```

### Signup

```
"Account created"                      ← if email exists, send password reset email
                                          to existing user (silent enumeration block)
```

### Login

```
"Invalid email or password"            ← single generic message
                                          (also: similar timing — see below)
```

### Timing Attacks

Hash comparison time leaks existence (`bcrypt.matches()` skipped if user not found).

```java
// ❌ Different timing
User u = userRepo.findByEmail(email);
if (u == null) return Response.UNAUTHORIZED;     // fast
if (!encoder.matches(pwd, u.getHash())) return UNAUTHORIZED; // ~250ms

// ✅ Constant time-ish
User u = userRepo.findByEmail(email);
String hash = (u == null) ? DUMMY_BCRYPT_HASH : u.getHash();
boolean matches = encoder.matches(pwd, hash);    // always runs hash
if (u == null || !matches) return UNAUTHORIZED;
```

---

## Session Management

### Session Cookie Flags

```
Set-Cookie: session=abc123;
            HttpOnly;                  ← JS can't read (XSS protection)
            Secure;                    ← HTTPS only
            SameSite=Lax;              ← CSRF protection
            Path=/;
            Max-Age=3600
```

### Session ID Properties

- **Random** (CSPRNG, 128+ bits entropy)
- **Stored server-side** (Redis/DB) → opaque token
- **Rotated** on privilege change (login, password change)
- **Expires** (idle + absolute timeout)

### Storage

| Option | Pro | Con |
|--------|-----|-----|
| In-memory (single server) | Fastest | Doesn't scale |
| Redis | Fast, distributed | Extra infra |
| DB | Persistent | Slower |
| Stateless JWT | No server state | Hard to revoke |

### Logout

- **Invalidate server-side** (delete from Redis)
- **Clear cookie** on client
- For JWT: blocklist or short expiry + refresh tokens

---

## Session Fixation

**Matlab:** Attacker pre-creates a session, tricks victim into authenticating into it, then uses that session ID.

### Attack

```
1. Attacker visits site → gets session ID "ABC"
2. Attacker tricks victim to use "ABC" (URL/cookie injection)
3. Victim logs in with "ABC" → server now associates user with ABC
4. Attacker uses "ABC" → impersonates victim
```

### Defense

**Always rotate session ID after login:**

```java
// Spring Security default behavior — yes, it rotates by default
// Can configure:
http.sessionManagement()
    .sessionFixation().migrateSession()      // default — preserves attributes
    .sessionFixation().newSession()          // brand new session
    .sessionFixation().none();               // ❌ vulnerable
```

Also rotate on:
- Password change
- Privilege escalation (sudo mode)
- 2FA verification

---

## JWT Security

**Matlab:** JSON Web Token — signed (sometimes encrypted) token containing claims. Stateless auth common in APIs.

### Format

```
header.payload.signature
```

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0.signature
```

### Header (decoded)

```json
{ "alg": "HS256", "typ": "JWT" }
```

### Payload (decoded — public!)

```json
{
  "sub": "user-123",
  "iat": 1715000000,
  "exp": 1715003600,
  "role": "user"
}
```

### Signature

Computed by server using secret/private key. Verifies integrity.

### Critical Rules

#### 1. Don't Put Sensitive Data in Payload

JWT payload is **base64**, not encrypted. Anyone can decode it.

```
❌ password, full credit card, SSN
✅ user ID, role, expiry
```

#### 2. Strong Secret

For HS256: secret length **>= 256 bits** (32+ random bytes).

```bash
openssl rand -base64 64    # generate strong secret
```

Stored in **secrets manager**, not code.

#### 3. Short Expiry

- Access token: **15 min** typical
- Refresh token: longer (days), stored more securely (HttpOnly cookie)

#### 4. Verify Algorithm

```java
// ❌ Accept whatever algorithm token specifies (alg:none attack!)
JWT.decode(token);

// ✅ Force expected algorithm
JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret))
    .withIssuer("myapp")
    .build();
verifier.verify(token);
```

The infamous **`alg: none`** attack — old libs accepted unsigned tokens.

#### 5. Validate Standard Claims

| Claim | Meaning |
|-------|---------|
| `iss` | Issuer |
| `sub` | Subject (user ID) |
| `aud` | Audience |
| `exp` | Expiry timestamp |
| `nbf` | Not-before |
| `iat` | Issued at |
| `jti` | JWT ID (for blocklisting) |

### Token Storage on Client

| Where | Pros | Cons |
|-------|------|------|
| **HttpOnly cookie** | XSS-safe, auto-sent | CSRF risk → use `SameSite=Lax/Strict` |
| **localStorage** | Easy JS access | XSS = full token theft |
| **sessionStorage** | Cleared on tab close | Same XSS issue |
| **Memory only (JS variable)** | Best XSS resistance | Lost on refresh |

**Common pattern:** Refresh token in HttpOnly cookie + short-lived access token in memory.

### Revocation Problem

JWT is stateless → can't "log out" a token before expiry.

**Mitigations:**
- Short access tokens + revocable refresh tokens
- Blocklist by `jti` in Redis (gives back state, but smaller)
- Token versioning per user — bump on logout-all/password-change

---

## HS256 vs RS256

### HS256 (HMAC + SHA-256)

- **Symmetric** — same secret signs and verifies
- All services validating tokens need the **same** secret
- Simpler

### RS256 (RSA + SHA-256)

- **Asymmetric** — private key signs, **public key** verifies
- Auth server keeps private key secret; resource servers get public key
- **Better** for distributed systems (microservices, OAuth providers)

### When to Use What?

| Scenario | Use |
|----------|-----|
| Single backend | HS256 (simpler) |
| Multiple services validating | RS256 (no shared secret) |
| Third-party validation | RS256 (publish public key) |
| OAuth2 / OIDC | RS256 (standard) |

### Key Distribution (RS256)

JWKS — JSON Web Key Set served at `/.well-known/jwks.json`:

```json
{
  "keys": [
    { "kty": "RSA", "kid": "abc", "use": "sig", "n": "...", "e": "AQAB" }
  ]
}
```

Resource servers fetch + cache.

### Spring Security Resource Server

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: https://auth.example.com/.well-known/jwks.json
```

---

## MFA (Multi-Factor Auth)

**Matlab:** Multiple factors required:
1. **Something you know** (password)
2. **Something you have** (phone, security key)
3. **Something you are** (biometric)

### Common Second Factors

| Method | Strength |
|--------|----------|
| **TOTP** (Google Authenticator) | Strong |
| **WebAuthn / Passkeys / FIDO2** | Strongest |
| **SMS OTP** | ⚠️ SIM swap risk |
| **Email OTP** | Weak (depends on email security) |
| **Push notifications** | Strong (with biometric on phone) |

### TOTP (Time-based OTP)

Standard: RFC 6238. Shared secret generates rotating 6-digit code (every 30s).

```java
// Spring + library
GoogleAuthenticator gAuth = new GoogleAuthenticator();
GoogleAuthenticatorKey key = gAuth.createCredentials();
String secret = key.getKey();
// Show user QR code with `otpauth://totp/...secret=...`

// Verify
boolean valid = gAuth.authorize(secret, userEnteredCode);
```

### When MFA?

- ✅ Login (always preferred)
- ✅ Sensitive operations (password change, large transfer)
- ✅ Admin actions
- Optional for low-risk operations

---

## Password Reset Flows

### Anti-Patterns

❌ Email contains current password (means stored plaintext)
❌ Reset token long-lived (24h+) — should be 15-60 min
❌ Reset token reusable
❌ Reset link doesn't expire after use
❌ Predictable tokens

### Good Pattern

```
1. User clicks "Forgot password" → enters email
2. Same response regardless email exists ("Check your email")
3. If exists: server generates random token (CSPRNG, 256 bits), stores hash + expiry in DB
4. Email link: https://example.com/reset?token=...
5. User clicks → enters new password
6. Server: verify token (not expired, not used) → update password (re-hash) → invalidate token
7. Invalidate all existing sessions for that user
8. Notify user via email "Password changed"
```

### Token Storage

Store **hashed** in DB (SHA-256 sufficient for short-lived tokens).

```java
String token = generateRandomBase64(32);  // 256 bits
String tokenHash = sha256(token);

resetRepo.save(new PasswordReset(userId, tokenHash, Instant.now().plus(30, MINUTES)));
emailService.sendResetLink(user.getEmail(), token);   // raw token in URL only
```

---

## Summary Cheat Sheet

| Topic | Quick Note |
|-------|-----------|
| Password hashing | bcrypt(12) / argon2id |
| Salt | Per-user, automatic in modern libs |
| Brute force | Rate limit + lockout + CAPTCHA |
| Enumeration | Same response for unknown email |
| Timing | Always run hash check |
| Session cookies | HttpOnly, Secure, SameSite |
| Session fixation | Rotate ID on login |
| JWT payload | Public — no secrets |
| JWT alg | Force expected, never trust header |
| `alg: none` | Always reject |
| HS256 | Single backend |
| RS256 | Distributed / OAuth2 |
| Refresh tokens | Long-lived, secure storage |
| Token storage | HttpOnly cookie > localStorage |
| MFA | TOTP / Passkeys for sensitive ops |
| Reset tokens | Random + hashed + short-lived + single-use |

---

## Practice

1. Set up `BCryptPasswordEncoder` (cost=12) in Spring Security; measure hash time.
2. Implement rate limiting (5 attempts / 15 min) for login endpoint.
3. Make login response identical for "user not found" and "wrong password".
4. Configure session cookie flags (`HttpOnly`, `Secure`, `SameSite`).
5. Issue + verify HS256 JWT; then convert to RS256 with JWKS.
6. Implement TOTP using a Google Authenticator-compatible library.
7. Build secure password reset flow with hashed tokens + 30 min expiry.

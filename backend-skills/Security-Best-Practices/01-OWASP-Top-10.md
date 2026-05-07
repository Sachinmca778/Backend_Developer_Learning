# OWASP Top 10 (2021)

## Status: Not Started

---

## Table of Contents

1. [OWASP Top 10 Kya Hai?](#owasp-top-10-kya-hai)
2. [A01 — Broken Access Control](#a01--broken-access-control)
3. [A02 — Cryptographic Failures](#a02--cryptographic-failures)
4. [A03 — Injection](#a03--injection)
5. [A04 — Insecure Design](#a04--insecure-design)
6. [A05 — Security Misconfiguration](#a05--security-misconfiguration)
7. [A06 — Vulnerable & Outdated Components](#a06--vulnerable--outdated-components)
8. [A07 — Identification & Authentication Failures](#a07--identification--authentication-failures)
9. [A08 — Software & Data Integrity Failures](#a08--software--data-integrity-failures)
10. [A09 — Security Logging & Monitoring Failures](#a09--security-logging--monitoring-failures)
11. [A10 — Server-Side Request Forgery (SSRF)](#a10--server-side-request-forgery-ssrf)
12. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## OWASP Top 10 Kya Hai?

**Matlab:** **Open Worldwide Application Security Project** ka 10 sabse common web application risks ka list — every few years updated. 2021 latest official release.

> Goal: developers + AppSec teams jo critical risks awareness ka starting point.

```
Bigger numbers ≠ less critical
A01 = #1 frequency in 2021 data
```

---

## A01 — Broken Access Control

**Matlab:** User wo cheezein access kar leta hai jo nahi karni chahiye. **Most common** issue (94% of apps).

### Examples

```
GET /api/users/123/orders          ← User 456 calls this and sees user 123's orders
POST /admin/delete-user             ← Regular user can hit admin endpoint
GET /api/orders?userId=123          ← Trust client-supplied userId
```

### Common Sub-Issues

#### IDOR (Insecure Direct Object Reference)

```java
// ❌ No ownership check
@GetMapping("/orders/{id}")
public Order getOrder(@PathVariable Long id) {
    return orderRepository.findById(id).orElseThrow();
}

// ✅ Verify ownership
@GetMapping("/orders/{id}")
public Order getOrder(@PathVariable Long id, @AuthenticationPrincipal User user) {
    Order order = orderRepository.findById(id).orElseThrow();
    if (!order.getUserId().equals(user.getId())) {
        throw new ForbiddenException();
    }
    return order;
}
```

#### Force Browsing

```
/admin/users                        ← URL guessed; if no auth check → exposed
```

#### Privilege Escalation

```
PUT /api/users/me {"role": "admin"}  ← role field accepted from input
```

### Prevention

- ✅ **Default deny** — explicit allow on each endpoint
- ✅ **Server-side check** ownership of every resource
- ✅ **Don't trust client-side fields** (`userId`, `role`)
- ✅ **Use centralized authorization** (Spring Security `@PreAuthorize`, policy framework)
- ✅ **Log access denials**

```java
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/admin/users/{id}")
public void deleteUser(@PathVariable Long id) { ... }

@PreAuthorize("@orderService.isOwner(#id, principal)")
@GetMapping("/orders/{id}")
public Order getOrder(@PathVariable Long id) { ... }
```

→ See `04-Authorization-Security.md` for deep dive.

---

## A02 — Cryptographic Failures

**Matlab:** Sensitive data **encryption fail** — plain transport, weak hashing, leaking via errors. Pehle "Sensitive Data Exposure" naam tha.

### Common Mistakes

- HTTP (no TLS) for sensitive data
- Old TLS (1.0, 1.1)
- MD5 / SHA1 for password hashing
- Hardcoded keys in source code
- AES-ECB mode (insecure block mode)
- No encryption at rest for sensitive DB columns
- Logs full of credit cards, SSNs, JWTs

### Examples

```java
// ❌ Plain MD5 — useless against modern attackers
String hash = MessageDigest.getInstance("MD5")
              .digest(password.getBytes()); // BROKEN

// ✅ bcrypt with cost factor
PasswordEncoder encoder = new BCryptPasswordEncoder(12);
String hash = encoder.encode(password);
```

### Prevention

- ✅ **HTTPS everywhere** (HSTS header) — see `07-API-Security.md`
- ✅ **TLS 1.2+ minimum**, prefer 1.3
- ✅ **bcrypt / argon2 / scrypt** for passwords (see `03-Authentication-Security.md`)
- ✅ **AES-GCM** for symmetric encryption (NOT ECB)
- ✅ **Don't roll your own crypto** — use vetted libraries
- ✅ **Encrypt sensitive columns at rest** (PII, financial)
- ✅ **Don't log secrets, tokens, full PAN**
- ✅ **Use secrets manager** (Vault, AWS Secrets Manager)

---

## A03 — Injection

**Matlab:** Untrusted input **interpreted as code/query**. SQL injection famous, but also: NoSQL, OS command, LDAP, XPath, template injection.

### SQL Injection

```java
// ❌ String concatenation
String sql = "SELECT * FROM users WHERE name = '" + userInput + "'";
//   userInput = "x' OR '1'='1"  →  bypasses auth

// ✅ Parameterized
PreparedStatement ps = conn.prepareStatement(
    "SELECT * FROM users WHERE name = ?");
ps.setString(1, userInput);
```

### NoSQL Injection

```javascript
// ❌ MongoDB — passing object directly
db.users.find({ username: req.body.username, password: req.body.password });
// Attacker sends: {"username": "admin", "password": {"$ne": null}}

// ✅ Validate types
const username = String(req.body.username);
const password = String(req.body.password);
db.users.findOne({ username, password: hash(password) });
```

### Command Injection

```java
// ❌ Shell exec with user input
Runtime.getRuntime().exec("ping " + userHost);
// userHost = "; rm -rf /" → catastrophic

// ✅ Avoid shell; use ProcessBuilder with array args
new ProcessBuilder("ping", "-c", "4", userHost).start();
// Plus validate userHost matches a hostname regex
```

### LDAP Injection

```
filter = "(&(uid=" + user + ")(password=" + pass + "))"
user = "*)(uid=*"  → injects into LDAP filter
```

→ Use parameterized LDAP queries, escape special chars.

### Prevention

- ✅ **Parameterized queries** (always)
- ✅ **ORM** (JPA, Hibernate) — but careful with native @Query
- ✅ **Input validation** + allow-lists
- ✅ **Escape** if dynamic SQL truly needed
- ✅ **Least privilege DB user** (no DROP/DELETE for app)

→ Deep dive: `02-SQL-Injection-Prevention.md`

---

## A04 — Insecure Design

**Matlab:** Architecture/design level flaws — coding hi galat ho rahi feature design. **Newer in 2021 list.**

### Examples

- Password reset that sends current password (showing it stored plaintext)
- Token generation predictable (timestamp + counter)
- Lack of rate limiting on critical endpoints
- Business logic flaw: refund larger than purchase
- No email verification → account takeover via signup with victim's email
- Hidden fields in HTML "trusted" by server

### Prevention

- ✅ **Threat modeling** during design (STRIDE method)
- ✅ **Security requirements** in user stories
- ✅ **Secure design patterns** (defense-in-depth, fail-secure)
- ✅ **Security review** before major features
- ✅ **Abuse cases** in test plan ("what if attacker does X?")

---

## A05 — Security Misconfiguration

**Matlab:** Default configs, exposed admin panels, verbose errors, unnecessary features enabled.

### Common Issues

- Default admin/admin credentials unchanged
- Stack traces in 500 responses (leak version, paths)
- Directory listing on web server
- Cloud bucket public
- Spring Boot Actuator endpoints exposed (`/actuator/env`, `/actuator/heapdump`)
- CORS too permissive (`Access-Control-Allow-Origin: *` with credentials)
- HTTP security headers missing
- TLS misconfigured (weak ciphers, expired certs)

### Examples

```yaml
# ❌ Spring Boot dev mode in prod
management.endpoints.web.exposure.include: "*"
spring.profiles.active: dev
debug: true

# ✅
management.endpoints.web.exposure.include: health,info
spring.profiles.active: prod
```

### Prevention

- ✅ **Hardening checklist** for all envs
- ✅ **Same artifact** + env-specific config (no dev features in prod)
- ✅ **Security headers** (HSTS, X-Frame-Options, X-Content-Type-Options, CSP)
- ✅ **Custom error pages** — no stack traces
- ✅ **Periodic config review**
- ✅ **Automated scans** (cloud config, IaC scanners — `tfsec`, `checkov`)

### Recommended Security Headers

```http
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Content-Security-Policy: default-src 'self'
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: geolocation=(), microphone=()
```

---

## A06 — Vulnerable & Outdated Components

**Matlab:** App ki **dependencies** mein known CVEs — Log4Shell-style disasters.

### Examples

- Log4j 2 < 2.17 (CVE-2021-44228 — RCE)
- Spring4Shell (CVE-2022-22965)
- Old jQuery with XSS bugs
- Outdated OS packages in Docker image

### Prevention

- ✅ **Inventory** all dependencies (SBOM)
- ✅ **Automated scanning** (Snyk, Dependabot, OWASP Dependency-Check)
- ✅ **Patch promptly** — especially CVSS High/Critical
- ✅ **Remove unused** dependencies
- ✅ **Subscribe** to security mailing lists for major libs
- ✅ **Image scanning** (Trivy) — see `04-Docker-in-CI-CD.md`

→ Deep dive: `08-Dependency-Security.md`

---

## A07 — Identification & Authentication Failures

**Matlab:** Auth-related issues — weak passwords, brute force, broken sessions, MFA gaps.

### Common Issues

- Weak password policy
- No rate limiting on login → brute force
- Sessions never expire
- Predictable session IDs
- JWT misuse (none algorithm, weak secret)
- Account enumeration via "User not found" message

### Prevention

- ✅ **Strong password policy** + breach check (haveibeenpwned API)
- ✅ **Multi-factor authentication (MFA)** for sensitive ops
- ✅ **Rate limit** login attempts
- ✅ **Lockout** with reasonable thresholds
- ✅ **Secure session cookies** (`HttpOnly`, `Secure`, `SameSite`)
- ✅ **Short JWT expiry** + refresh tokens
- ✅ **Same response** for "user not found" vs "wrong password"

→ Deep dive: `03-Authentication-Security.md`

---

## A08 — Software & Data Integrity Failures

**Matlab:** Code/data integrity verify nahi — supply chain attacks, insecure deserialization.

### Examples

- Java deserialization of untrusted bytes (RCE)
- Unsigned auto-update mechanism (attacker injects malicious update)
- CI pipeline pulls from compromised npm package (typosquatting)
- npm `npm install latest` — surprise breaking + malicious versions

### Prevention

- ✅ **Sign artifacts** (Sigstore, cosign)
- ✅ **Verify signatures** before use
- ✅ **Pin dependency versions** + lockfiles
- ✅ **Avoid** Java native serialization for untrusted data — use JSON
- ✅ **SBOM** for visibility (Syft, CycloneDX)
- ✅ **Internal package mirror** for npm/Maven (proxy known versions)

---

## A09 — Security Logging & Monitoring Failures

**Matlab:** Attacks **detect nahi** ho rahe because logs/monitoring missing or insufficient.

### Examples

- Login failures not logged
- No alerting on anomalies (1000 failed logins per min)
- Logs only stored locally → attacker can wipe
- No SIEM / centralized log analysis
- Long detection time (industry avg ~200+ days for breach detection!)

### Prevention

- ✅ **Log security events**: logins (success + fail), permission denials, input validation failures, sensitive operations
- ✅ **Centralized logging** (ELK, Datadog, Splunk, CloudWatch)
- ✅ **Tamper-resistant** (append-only, ship to remote)
- ✅ **Alerting** on suspicious patterns (rate, geo, lateral movement)
- ✅ **Incident response plan** (runbooks!)
- ✅ **Test** detection — purple team exercises

```java
// Useful logs
log.warn("Failed login attempt", kv("user", username), kv("ip", clientIp));
log.warn("Authorization denied", kv("user", userId), kv("resource", path));
log.info("Sensitive operation", kv("action", "password_change"), kv("user", userId));
```

→ Linked with monitoring: `DevOps-&-CI-CD/07-Monitoring-and-Alerting.md`

---

## A10 — Server-Side Request Forgery (SSRF)

**Matlab:** App user-controlled URL fetch karta hai → attacker se internal services/cloud metadata target karwata hai.

### Classic AWS SSRF

```python
# ❌ App fetches URL given by user
def fetch(url):
    return requests.get(url).text

# Attacker sends: http://169.254.169.254/latest/meta-data/iam/security-credentials/
# → AWS instance metadata endpoint!
# → Returns IAM creds → attacker takes over AWS account
```

### Other Targets

- Internal services (`http://internal-admin/`)
- Cloud metadata (AWS 169.254.169.254, GCP metadata.google.internal)
- File scheme (`file:///etc/passwd`)
- Unintended ports (`http://localhost:6379` → Redis)

### Prevention

- ✅ **Allow-list** of permitted destinations (never deny-list)
- ✅ **Validate URL scheme** (http/https only — block `file://`, `gopher://`)
- ✅ **Resolve DNS, then validate IP** is public (block private ranges, link-local)
- ✅ **No follow-redirects** to private IPs
- ✅ **IMDSv2** for AWS (token-based, kills classic SSRF)
- ✅ **Egress filtering** at network layer (firewall blocks internal IPs)
- ✅ **Disable unused URL schemes** in HTTP client
- ✅ **Timeouts** + size limits on responses

```java
// Safer fetcher (concept)
URL url = new URL(userUrl);
if (!"https".equals(url.getProtocol())) throw new ForbiddenException();
InetAddress addr = InetAddress.getByName(url.getHost());
if (addr.isSiteLocalAddress() || addr.isLoopbackAddress() || addr.isLinkLocalAddress()) {
    throw new ForbiddenException("Private address blocked");
}
// Then fetch with timeouts + size limits
```

---

## Summary Cheat Sheet

| ID | Risk | Quick Fix |
|----|------|-----------|
| **A01** | Broken Access Control | Default deny + ownership checks |
| **A02** | Cryptographic Failures | TLS, bcrypt, AES-GCM, secrets manager |
| **A03** | Injection | Parameterized queries always |
| **A04** | Insecure Design | Threat modeling, abuse cases |
| **A05** | Misconfiguration | Hardening + scanners + custom errors |
| **A06** | Vulnerable Components | Snyk/Dependabot + patching |
| **A07** | Auth Failures | MFA, rate limit, secure sessions |
| **A08** | Integrity Failures | Sign, verify, pin versions |
| **A09** | Logging Failures | Central logs + alerts on anomalies |
| **A10** | SSRF | Allow-list + private IP block + IMDSv2 |

---

## Practice

1. Apne app ke 5 endpoints par A01 ownership checks audit karo.
2. Static analysis tool (Sonar / Snyk Code) chala kar A03 hits dekho.
3. Production-mode Spring Boot config se actuator surface check karo.
4. Deps mein Log4j / Spring versions list — CVE check karo.
5. SSRF safe URL fetcher utility likho jo private IPs block kare.

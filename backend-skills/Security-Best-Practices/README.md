# Security Best Practices

Backend developer ke liye **applied AppSec** — OWASP Top 10 ke practical prevention, SQL injection, authentication / authorization, input validation, secrets, API hardening, aur dependency security. Sab Hinglish mein, Java/Spring + DevOps examples ke saath.

---

## Topics & Status

| # | Topic | File | Status |
|---|-------|------|--------|
| 1 | OWASP Top 10 (2021) | [01-OWASP-Top-10.md](./01-OWASP-Top-10.md) | Not Started |
| 2 | SQL Injection Prevention | [02-SQL-Injection-Prevention.md](./02-SQL-Injection-Prevention.md) | Not Started |
| 3 | Authentication Security | [03-Authentication-Security.md](./03-Authentication-Security.md) | Not Started |
| 4 | Authorization Security | [04-Authorization-Security.md](./04-Authorization-Security.md) | Not Started |
| 5 | Input Validation & Sanitization | [05-Input-Validation-and-Sanitization.md](./05-Input-Validation-and-Sanitization.md) | Not Started |
| 6 | Secrets Management | [06-Secrets-Management.md](./06-Secrets-Management.md) | Not Started |
| 7 | API Security | [07-API-Security.md](./07-API-Security.md) | Not Started |
| 8 | Dependency Security | [08-Dependency-Security.md](./08-Dependency-Security.md) | Not Started |

---

## What's Inside Each File?

### [01 — OWASP Top 10 (2021)](./01-OWASP-Top-10.md)
A01 Broken Access Control (IDOR, force browsing, privilege escalation), A02 Cryptographic Failures (TLS, password hashing, encryption), A03 Injection (SQL/NoSQL/Command/LDAP), A04 Insecure Design (threat modeling), A05 Security Misconfiguration (default deny, security headers), A06 Vulnerable Components, A07 Auth Failures, A08 Integrity (insecure deserialization), A09 Logging/Monitoring Failures, A10 SSRF.

### [02 — SQL Injection Prevention](./02-SQL-Injection-Prevention.md)
Attack types (login bypass, UNION, blind, time-based, stacked), parameterized queries (JDBC, JdbcTemplate, Python, Node, Go), Spring Data JPA (`@Query` with `:param`), native query traps, **dynamic ORDER BY allow-listing**, stored procedures, IN clause, ORM doesn't save you when concatenating, DB-side hardening (least privilege, multi-statement off), sqlmap.

### [03 — Authentication Security](./03-Authentication-Security.md)
Password storage (**bcrypt / argon2** — never MD5/SHA1), brute force protection (rate limit, lockout, CAPTCHA), account enumeration prevention (constant timing), session cookies (HttpOnly/Secure/SameSite), session fixation, **JWT security** (strong secret, short expiry, no secrets in payload, `alg:none` attack), HS256 vs RS256, JWKS, MFA (TOTP/Passkeys), secure password reset flow.

### [04 — Authorization Security](./04-Authorization-Security.md)
**BOLA / IDOR** (object-level), **BFLA** (function-level), mass assignment + DTOs, vertical/horizontal privilege escalation, RBAC vs ABAC vs PBAC vs ReBAC (Zanzibar/SpiceDB/OpenFGA), default deny, centralized policy (`@PreAuthorize`), tenant isolation, Spring Security examples, authorization test matrix.

### [05 — Input Validation & Sanitization](./05-Input-Validation-and-Sanitization.md)
Server-side always (frontend = UX), allow-list > deny-list, **Bean Validation** (`@Valid`, `@Pattern`, `@Size`, custom validators, groups), **XSS** (output encoding per context, Thymeleaf/React auto-escape), **CSP** (directives, strict CSP with nonces, report-only), **XXE** (disable external entities in Java parsers), **SSRF** (allow-list + private IP block + IMDSv2), file upload security (MIME via Tika, UUID filenames, ClamAV), path traversal, SSTI, ReDoS.

### [06 — Secrets Management](./06-Secrets-Management.md)
What's a secret, `.gitignore` essentials, **gitleaks / git-secrets** pre-commit hooks, GitHub push protection, leak cleanup (`git filter-repo`), env vars (pros/cons), **dedicated stores**: HashiCorp Vault (dynamic secrets), AWS Secrets Manager, GCP Secret Manager, Azure Key Vault, **Sealed Secrets / SOPS** for GitOps, **rotation strategies** (time, event, dynamic), encryption at rest vs in transit, KMS + envelope encryption, IAM roles > static keys, logging hygiene (Actuator sanitization).

### [07 — API Security](./07-API-Security.md)
HTTPS everywhere + HSTS preload, TLS minimum (1.2/1.3), **certificate pinning** (mobile, SPKI), API keys vs OAuth2 tokens, OAuth2 flows (Auth Code, Client Creds, Device), **PKCE for public clients**, token storage (HttpOnly cookie + memory, never localStorage), **CORS** (correct origins, credentials gotcha), 401 vs 403, rate limiting, idempotency keys, webhook HMAC signing, WAF, mTLS internal.

### [08 — Dependency Security](./08-Dependency-Security.md)
Why (Equifax/Struts, Log4Shell, xz-utils), direct vs transitive deps, **SCA** tools (OWASP Dependency-Check, Trivy, Grype, Snyk, Dependabot, GitHub Security), **CVE / CVSS** basics, **SBOM** (CycloneDX/SPDX, Syft), patching SLAs (Critical < 72h), choosing libs safely (Snyk Advisor, OpenSSF Scorecard), lockfiles + version pinning, container/OS scanning, **supply chain attacks** (typosquatting, dep confusion, maintainer takeover), CI/CD integration (SARIF upload, periodic re-scan).

---

## Recommended Learning Order

```
1. OWASP Top 10 (01)          ← landscape view
2. Input Validation (05)       ← foundation for many fixes
3. SQL Injection (02)          ← deep dive on #1 historic risk
4. Authentication (03)         ← who you are
5. Authorization (04)          ← what you can do
6. API Security (07)           ← edge hardening
7. Secrets Management (06)     ← protect the keys to the kingdom
8. Dependency Security (08)    ← supply chain
```

---

## Quick Reference

### "Mujhe X karna hai" → kahan dekhun?

| Task | File | Section |
|------|------|---------|
| Hash passwords correctly | 03 | Password Storage |
| Block brute-force login | 03 | Brute Force Protection |
| Stop user-A reading user-B's order | 04 | BOLA / IDOR |
| Prevent role escalation via JSON | 04 | Mass Assignment |
| Prevent SQL injection | 02 | Parameterized Queries |
| Validate request body | 05 | Bean Validation |
| Stop XSS | 05 | XSS / CSP |
| Block SSRF | 05 / 01 | SSRF |
| Disable XXE | 05 | XXE |
| Configure CORS safely | 07 | CORS |
| OAuth2 PKCE for SPA | 07 | PKCE |
| Where to store JWT? | 07 | Token Storage |
| Stop secrets in Git | 06 | Never Commit + gitleaks |
| Rotate AWS keys safely | 06 | Rotation, IAM Roles |
| Find vulnerable libs | 08 | SCA Tools |
| Generate SBOM | 08 | SBOM |
| OWASP A05 hardening | 01 | Misconfig + headers |

---

## Tools Reference

### Static / SCA
- gitleaks, git-secrets (secret scanning)
- OWASP Dependency-Check, Trivy, Snyk, Grype, Dependabot
- SonarQube, Semgrep, CodeQL (SAST)
- Syft (SBOM), cosign (signing)

### Dynamic / Runtime
- OWASP ZAP, Burp Suite, sqlmap
- WAF: Cloudflare, AWS WAF, Akamai
- Bot protection: reCAPTCHA, Cloudflare Turnstile, hCaptcha

### Identity / Tokens
- Spring Security, Keycloak, Auth0, Okta, AWS Cognito
- OAuth2 / OIDC providers
- TOTP libs: java-otp, Google Authenticator

### Secrets
- HashiCorp Vault, AWS Secrets Manager, GCP Secret Manager, Azure Key Vault
- Sealed Secrets (Bitnami), SOPS (Mozilla), age
- Doppler, Infisical

### Headers / Hardening
- [securityheaders.com](https://securityheaders.com/)
- [ssllabs.com/ssltest](https://www.ssllabs.com/ssltest/)
- [observatory.mozilla.org](https://observatory.mozilla.org/)
- [ssl-config.mozilla.org](https://ssl-config.mozilla.org/)

---

## Companion Folders

- [API Design & Architecture](../API-Design-&-Architecture/) — rate limiting, idempotency, pagination
- [Database Mastery](../Database-Mastery/) — query design, transactions
- [Networking & Protocols](../Networking-&-Protocols/) — TLS deep dive, mTLS
- [Code Quality & Best Practices](../Code-Quality-&-Best-Practices/) — clean code, reviews
- [DevOps & CI/CD](../DevOps-&-CI-CD/) — Docker scanning, monitoring/alerting

---

## Status Tracker

```
[ ] 01 — OWASP Top 10 (2021)
[ ] 02 — SQL Injection Prevention
[ ] 03 — Authentication Security
[ ] 04 — Authorization Security
[ ] 05 — Input Validation & Sanitization
[ ] 06 — Secrets Management
[ ] 07 — API Security
[ ] 08 — Dependency Security
```

Topic complete hone par file header aur is README dono mein status update kar lena.

Stay safe out there!

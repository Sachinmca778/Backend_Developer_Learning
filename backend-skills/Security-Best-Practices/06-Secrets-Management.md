# Secrets Management

## Status: Not Started

---

## Table of Contents

1. [What Counts as a Secret?](#what-counts-as-a-secret)
2. [Never Commit to Git](#never-commit-to-git)
3. [Detection & Cleanup if Already Leaked](#detection--cleanup-if-already-leaked)
4. [Environment Variables](#environment-variables)
5. [Dedicated Secret Stores](#dedicated-secret-stores)
6. [Secret Rotation](#secret-rotation)
7. [Encryption at Rest vs in Transit](#encryption-at-rest-vs-in-transit)
8. [Cloud-Native Patterns](#cloud-native-patterns)
9. [Logging Hygiene](#logging-hygiene)
10. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## What Counts as a Secret?

| Type | Examples |
|------|----------|
| **Credentials** | DB password, API tokens, OAuth client secret |
| **Keys** | JWT signing key, SSH private key, GPG key |
| **Encryption keys** | AES master key, KMS data keys |
| **Connection strings** | Postgres URL with password |
| **Webhooks** | Stripe webhook secret, GitHub webhook |
| **Cloud creds** | AWS access keys, GCP service account JSON |
| **PII (treat similarly)** | Customer SSN, payment data |

If leak → bad day → it's a secret.

---

## Never Commit to Git

### `.gitignore` Essentials

```gitignore
.env
.env.*
!.env.example          # commit only template
*.pem
*.key
*.p12
*.pfx
secrets.yaml
config-local.yml
.aws/credentials
.kube/config
.npmrc                 # if has tokens
```

### `.env.example` (Commit This Only)

```env
# .env.example — copy to .env and fill in
DATABASE_URL=postgres://user:pass@localhost/mydb
STRIPE_API_KEY=sk_test_xxx
JWT_SECRET=replace-with-32+-random-bytes
```

### Pre-Commit Hooks

Install hooks that scan **before** commit:

#### `gitleaks`

```bash
brew install gitleaks
gitleaks detect --source . --verbose
```

Pre-commit setup:
```yaml
# .pre-commit-config.yaml
repos:
  - repo: https://github.com/gitleaks/gitleaks
    rev: v8.18.0
    hooks:
      - id: gitleaks
```

#### `git-secrets` (AWS)

```bash
git secrets --install
git secrets --register-aws
```

#### Truffle Hog

Scans repo + history for secrets across many providers.

### Repo Settings

- **GitHub Secret Scanning** (free for public, paid for private — push protection blocks pushes containing detected secrets)
- **Pre-receive hook** at org level

---

## Detection & Cleanup if Already Leaked

### Step 1 — Rotate Immediately

Even if "we'll just delete the commit". Once pushed, **assume compromised**.

### Step 2 — Search History

```bash
# Recent commits
git log -p | grep -i "password\|secret\|api_key"

# Tools
gitleaks detect --source . --log-opts="--all"
trufflehog git file://. --only-verified
```

### Step 3 — Rewrite History (Last Resort)

Tools:
- **`git filter-repo`** (recommended over `filter-branch`)
- **BFG Repo-Cleaner**

```bash
git filter-repo --path config/secrets.yml --invert-paths
git push --force-with-lease  # ⚠️ rewrites public history
```

⚠️ Coordinate with team — everyone needs fresh clone.

### Step 4 — Notify

Cloud provider (AWS abuse), security team, customers if data accessed.

---

## Environment Variables

### Standard Approach

```bash
# Server / container
DATABASE_URL=postgres://...
JWT_SECRET=...
```

```java
@Value("${database.url}") String dbUrl;
// or system: System.getenv("DATABASE_URL")
```

```yaml
# Spring Boot application.yml
database:
  url: ${DATABASE_URL}            # required
  pool-size: ${DB_POOL_SIZE:20}   # with default
```

### Pros / Cons

| Pro | Con |
|-----|-----|
| Universal | Easily logged accidentally |
| Easy to rotate (restart) | Visible in process listings (`ps`, `/proc`) |
| Works everywhere | Plain text in container manifests |

### Container Secrets

```yaml
# Docker Compose (local dev only)
services:
  app:
    image: myapp
    env_file: .env       # never commit .env

# Kubernetes Secret (better than env)
apiVersion: v1
kind: Secret
metadata: { name: db-creds }
type: Opaque
data:
  password: <base64>      # base64 ≠ encrypted!

# Use as env or volume
env:
  - name: DB_PASS
    valueFrom:
      secretKeyRef:
        name: db-creds
        key: password
```

⚠️ K8s Secrets are base64 — **encrypt at rest** with `EncryptionConfiguration` or use external secret operator.

---

## Dedicated Secret Stores

For production: ❌ env vars + Git for everything → ✅ centralized, audited, rotatable.

### HashiCorp Vault

- Self-hosted, multi-cloud
- Dynamic secrets (DB credentials per request, auto-expire)
- Encryption-as-a-service (transit)
- Strong audit log

```bash
vault kv put secret/myapp/db password=...
vault kv get secret/myapp/db
```

App reads via API:
```java
// Spring Cloud Vault
@Value("${db.password}") String password;  // auto-loaded from Vault
```

### AWS Secrets Manager

```bash
aws secretsmanager get-secret-value --secret-id prod/db
```

```java
SecretsManagerClient sm = SecretsManagerClient.create();
GetSecretValueResponse r = sm.getSecretValue(b -> b.secretId("prod/db"));
```

Features:
- Auto-rotation (Lambda hook)
- IAM-based access
- Cross-region replication

### GCP Secret Manager

```bash
gcloud secrets versions access latest --secret=db-password
```

### Azure Key Vault

Similar feature set.

### Spring Cloud Config Server

Centralized config server, often backed by Git + decryption keys.

⚠️ Encrypted-at-rest is the **must**.

### Doppler / Infisical / 1Password Secrets

SaaS friendly tools — sync secrets to dev / CI / prod.

### Comparison

| Tool | Best for |
|------|----------|
| **Vault** | Multi-cloud, dynamic creds, max control |
| **AWS SM / GCP SM / Azure KV** | Cloud-native shops |
| **Sealed Secrets / SOPS / age** | GitOps-friendly encrypted-in-Git |
| **Doppler / Infisical** | Dev-friendly UX |

### Sealed Secrets (Bitnami)

For GitOps — encrypt secret with cluster public key, commit ciphertext.

```bash
echo -n password | kubeseal --raw --namespace=mynamespace --name=db-creds
# → base64 ciphertext, safe to commit
```

### SOPS (Mozilla)

Encrypt entire YAML/JSON with KMS / GPG / age.

```bash
sops -e -i secrets.yaml      # encrypt in place (KMS)
sops -d secrets.yaml         # decrypt to stdout
```

---

## Secret Rotation

**Matlab:** Periodic + on-demand secret change to limit blast radius.

### Why?

- Insider risk (employee leaves)
- Unknown leak (suspect compromise)
- Compliance requirement (PCI, SOC 2)

### Rotation Strategies

#### 1. Time-Based (Easy)

Quarterly rotate API keys / passwords.

#### 2. Event-Based

On suspect leak / personnel change.

#### 3. Dynamic Secrets (Best)

Vault generates short-lived DB creds per session:

```
App requests creds → Vault creates temp DB user (15-min TTL) → app uses → expires
```

→ Long-lived static creds eliminated.

### Zero-Downtime Rotation

Most secrets need overlap during rotation:

```
Time T:    new secret created (both old + new active)
Time T+1:  app reloads / reads new secret
Time T+2:  old secret revoked
```

### Auto-Rotation

- AWS Secrets Manager Lambda rotators
- Vault DB secret engine

---

## Encryption at Rest vs in Transit

### In Transit

Data **moving over network** — TLS.

- HTTPS for all client → server
- mTLS for service → service (see `Networking-&-Protocols/06-SSL-TLS.md`)
- DB connections use SSL (`sslmode=require` Postgres)
- Backups in transit too (S3 over HTTPS)

### At Rest

Data **stored on disk**.

- Disk-level: LUKS, BitLocker, EBS encryption
- DB-level: TDE (Transparent Data Encryption), Postgres pgcrypto
- App-level: encrypt sensitive columns (PII, tokens) before insert
- Object storage: S3 SSE-KMS, GCS CMEK
- Backups encrypted

### Key Management

Use **KMS** — managed key service:

| Provider | Tool |
|----------|------|
| AWS | KMS, CloudHSM |
| GCP | Cloud KMS |
| Azure | Key Vault |
| Self-hosted | Vault Transit, CloudHSM |

Apps don't see raw keys — request KMS to encrypt/decrypt.

```java
// AWS KMS encrypt
kmsClient.encrypt(b -> b
    .keyId("alias/myapp")
    .plaintext(SdkBytes.fromUtf8String(secretValue))
).ciphertextBlob();
```

### Envelope Encryption

For large data:

```
1. Generate Data Key (DEK) — random AES-256 key
2. KMS encrypts DEK with master key (KEK) — store ciphertext alongside data
3. Encrypt data with plaintext DEK
4. Discard plaintext DEK
5. To decrypt: KMS decrypts DEK → use to decrypt data
```

→ Master key never leaves KMS; data keys cheap to generate.

---

## Cloud-Native Patterns

### IAM Roles > Long-Lived Keys

```
❌ App has AWS access key in env
✅ App runs as IAM role (EC2, ECS Task, EKS pod IRSA, Lambda)
   → temporary credentials auto-rotated by AWS
```

GCP: Workload Identity. Azure: Managed Identity.

### Workload Identity Federation

For workloads outside cloud (on-prem, GitHub Actions):

- GitHub OIDC → AWS IAM role (no static keys)
- Tokens minted per workflow

(Covered in `DevOps-&-CI-CD/03-GitHub-Actions.md`.)

### Service Mesh mTLS Auto-Rotation

Istio / Linkerd auto-rotate cert per workload (~24h).

---

## Logging Hygiene

```java
// ❌ NEVER
log.info("User logged in {} {}", email, password);
log.debug("Request body: {}", req);   // might contain secrets

// ✅ Mask or omit
log.info("User logged in", kv("email_hash", hash(email)));
```

### Mask in Frameworks

Spring Boot Actuator hides values for keys matching `password`, `secret`, `key`, `token`. Customize:

```yaml
management.endpoint.env.keys-to-sanitize:
  - password
  - secret
  - key
  - token
  - .*credential.*
```

### MDC / Structured Logs

```java
MDC.put("user_id", user.getId().toString());
log.info("Order placed", kv("order_id", order.getId()));
// NEVER put password / token in MDC
```

### Audit Logs

For compliance, **separate** audit log stream:
- Sensitive operations
- Append-only (immutable)
- Encrypted, tamper-evident
- Retained per compliance period

---

## Summary Cheat Sheet

| Practice | Status |
|----------|--------|
| `.env` in `.gitignore` | ✅ always |
| Pre-commit secret scanning | ✅ gitleaks |
| GitHub push protection | ✅ enable |
| Rotate after leak | ✅ immediate |
| Env vars for local dev | ✅ OK |
| Secrets manager for prod | ✅ Vault / AWS SM |
| IAM roles > static keys | ✅ cloud-native |
| Dynamic DB creds | ✅ Vault |
| TLS everywhere | ✅ in transit |
| KMS-backed encryption | ✅ at rest |
| Sealed Secrets / SOPS | ✅ if GitOps |
| Mask in logs | ✅ no secrets in logs |
| Audit log | ✅ for compliance |

---

## Practice

1. Audit your repo: `gitleaks detect --log-opts="--all"` — find anything?
2. Set up pre-commit hook with gitleaks.
3. Move at least one secret from env file to AWS Secrets Manager (or Vault).
4. Configure Spring Cloud AWS / Vault config to inject secret as `@Value`.
5. Implement automatic rotation for one credential.
6. Sanitize logs: ensure no secret/PII in your INFO/DEBUG logs.
7. For Kubernetes: install Sealed Secrets and migrate one Secret.

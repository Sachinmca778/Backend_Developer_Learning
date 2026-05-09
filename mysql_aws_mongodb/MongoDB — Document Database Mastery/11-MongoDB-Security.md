# MongoDB Security

## Status: Complete

---

## Table of Contents

1. [Threat Model Basics](#threat-model-basics)
2. [Authentication](#authentication)
3. [Authorization & RBAC](#authorization--rbac)
4. [Built-in Roles](#built-in-roles)
5. [Custom Roles](#custom-roles)
6. [Network Exposure](#network-exposure)
7. [TLS / SSL](#tls--ssl)
8. [Encryption at Rest](#encryption-at-rest)
9. [Field-Level Encryption](#field-level-encryption)
10. [Audit Logging](#audit-logging)
11. [Operational Checklist](#operational-checklist)
12. [Pitfalls](#pitfalls)
13. [Cheat Sheet](#cheat-sheet)

---

## Threat Model Basics

Attack surfaces:

| Layer | Risk |
|-------|------|
| Network | Public `mongod` port scan brute-force |
| Identity | Weak passwords shared admin |
| Data in transit | Sniffing LAN/cloud |
| Data at rest | Disk theft snapshot |
| Application | Injection revealing creds |
| Insider | Over-privileged DB users |

---

## Authentication

> "**Who are you?**"

### SCRAM-SHA-256 (default modern)

Salted challenge-response — password **not sent plaintext**.

Create user:

```javascript
use admin
db.createUser({
  user: "appUser",
  pwd: "strong-password",
  roles: [{ role: "readWrite", db: "shop" }]
})
```

Enable auth:

```yaml
security:
  authorization: enabled
```

### Other mechanisms

| Mechanism | Notes |
|-----------|-------|
| **x.509** | Cert-based — mutual TLS identity |
| **LDAP** | Enterprise directory integration |
| **Kerberos** | Enterprise Windows-heavy environments |

---

## Authorization & RBAC

> "**What can you do?** — **Role-Based Access Control**."

Principle of **least privilege**:

- App service account: **`readWrite`** single DB — not **`root`**

---

## Built-in Roles

### Database-level (examples)

| Role | Powers |
|------|--------|
| **read** | Read data |
| **readWrite** | Read + modify data |
| **dbAdmin** | indexes, stats, compaction-ish ops |
| **userAdmin** | manage users on that DB |

### Cluster / admin

| Role | Powers |
|------|--------|
| **clusterAdmin** | sharding, repl maintenance broad |
| **root** | Superuser — **break glass only** |

---

## Custom Roles

```javascript
db.createRole({
  role: "ordersReporter",
  privileges: [
    {
      resource: { db: "shop", collection: "orders" },
      actions: ["find"]
    }
  ],
  roles: []
})
```

Fine-grained actions: **`find`**, **`insert`**, **`update`**, **`remove`**, **`createIndex`**, etc.

---

## Network Exposure

> "**Never expose `mongod` / `mongos` to public internet bare.**"

Do instead:

- **VPC private subnets**
- **Security groups / firewall** allowlist app subnets only
- **SSH tunnel / VPN / bastion** for admin
- Atlas: **IP Access List** + **VPC peering**

---

## TLS / SSL

> "**Encrypt wire protocol** — certs on servers + optionally clients (mTLS)."

```yaml
net:
  tls:
    mode: requireTLS
    certificateKeyFile: /path/mongo.pem
    CAFile: /path/ca.pem
```

Clients connect **`mongodb+srv`** Atlas TLS default.

---

## Encryption at Rest

| Edition | Feature |
|---------|---------|
| **Enterprise / Atlas** | Native storage encryption |
| **Community self-managed** | **Filesystem/disk encryption** (LUKS, cloud volume encryption) |

Also consider:

- Backup encryption
- Snapshot access IAM

---

## Field-Level Encryption

> "**Client-side** encrypt sensitive fields — DB stores ciphertext — keys **KMS** (since 4.2+ drivers Queryable Encryption roadmap evolving)."

Benefits:

- DBAs can't read plaintext certain fields
- Defense in depth DB breach

Tradeoffs:

- Query limitations on encrypted fields (depends scheme — equality queries supported in queryable encryption products)

---

## Audit Logging

> "**Enterprise / Atlas** audit logs — who did what administrative action."

Track:

- Auth failures
- UserAdmin changes
- Schema/index DDL

Forward to SIEM.

---

## Operational Checklist

- [ ] **`authorization: enabled`**
- [ ] Strong unique **`pwd`** stored secrets manager rotation
- [ ] Each microservice **distinct user** + role
- [ ] **TLS** everywhere production
- [ ] **No public bindIp** `0.0.0.0` without firewall story
- [ ] **`mongosh` admin** via bastion not password SSH defaults
- [ ] **encryption at rest** volume-level minimum
- [ ] **Backups** encrypted + restore tested
- [ ] **Disable unused network interfaces**
- [ ] **Rotate certs** before expiry automation

---

## Pitfalls

1. **`--noauth` dev drift** → prod forgot enable.
2. **Single shared root credential** in all apps — blast radius.
3. **LDAP misbind** — auth outage cascade.
4. **TLS hostname mismatch** — driver rejects chain.
5. **Backup plaintext** on open S3 bucket — compliance fail.
6. **Query injection** surfacing as auth bypass in wrapper APIs — sanitize filters.

---

## Cheat Sheet

| Topic | Guidance |
|-------|----------|
| Auth | SCRAM default |
| AuthZ | RBAC least privilege |
| Network | Private + allowlist |
| Transit | TLS |
| Rest | Enterprise native or disk crypto |
| Field enc | Client-side sensitive columns |
| Audit | Enterprise / Atlas |
| Internet | **Never** naked mongod |

---

## Practice

1. Create **`readOnly`** analyst user cannot `insert`.
2. Draw network diagram **app tier → private mongo**.
3. Threat model: stolen backup without encryption impact.
4. Compare **mTLS** vs password SCRAM operational burden.
5. Document **rotation playbook** DB user password with zero downtime deploy.

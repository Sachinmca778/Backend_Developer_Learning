# MySQL Security

## Status: Not Started

---

## Table of Contents

1. [Security Threat Model](#security-threat-model)
2. [Authentication Plugins](#authentication-plugins)
3. [Privilege System](#privilege-system)
4. [Principle of Least Privilege](#principle-of-least-privilege)
5. [SSL/TLS for Connections](#ssltls-for-connections)
6. [Network Security](#network-security)
7. [Audit Plugins](#audit-plugins)
8. [sql_mode](#sql_mode)
9. [SQL Injection Prevention](#sql-injection-prevention)
10. [Encryption at Rest](#encryption-at-rest)
11. [Backup Security](#backup-security)
12. [Compliance Checklist](#compliance-checklist)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## Security Threat Model

| Threat | Mitigation |
|--------|-----------|
| External attacker → DB direct | Bind to localhost / private subnet, firewall |
| SQL injection | Parameterized queries (app layer) |
| Compromised app server | Limited DB user privileges |
| Eavesdropping (MITM) | TLS encryption |
| Backup leak | Encrypted backups |
| Insider threat | Audit logs + role separation |
| Disk theft / cloud breach | Encryption at rest (TDE) |
| Brute force on root | Strong password + IP restriction + rate limiting |
| Unauthorized DDL | `REVOKE` / role limitation |

---

## Authentication Plugins

### Available plugins

| Plugin | When |
|--------|------|
| `mysql_native_password` | Legacy 4.1+ |
| `caching_sha2_password` | **MySQL 8.0+ default** (SHA-256, faster on cache hit) |
| `sha256_password` | Pre-8 SHA |
| `auth_socket` | Unix socket auth (root only) |
| LDAP plugin | Enterprise / external auth |
| Pam plugin | OS-level auth |

### Default in 8.0

```sql
SHOW VARIABLES LIKE 'default_authentication_plugin';   -- caching_sha2_password
```

### Create user with specific plugin

```sql
CREATE USER 'app'@'10.0.0.%' IDENTIFIED WITH caching_sha2_password BY 'StrongP@ss!';
```

### Why `caching_sha2_password`?

- Stronger SHA-256 (vs SHA-1 in `mysql_native_password`)
- Caching for fast subsequent auth
- Requires SSL for first auth (or RSA key)

### Older clients

Some clients don't support `caching_sha2_password`. If migration:

```sql
ALTER USER 'old_user'@'%' IDENTIFIED WITH mysql_native_password BY 'pwd';
```

→ Better: upgrade clients.

### Password policy

```ini
validate_password.policy = STRONG
validate_password.length = 12
validate_password.mixed_case_count = 1
validate_password.number_count = 1
validate_password.special_char_count = 1
```

### Password expiration

```sql
ALTER USER 'app'@'%' PASSWORD EXPIRE INTERVAL 90 DAY;
```

---

## Privilege System

> "Privileges defined at **global / database / table / column** levels."

### Hierarchy

```
GLOBAL          *.*                    SUPER, RELOAD, FILE, etc.
DATABASE        appdb.*                ALL on one DB
TABLE           appdb.users            SELECT, INSERT on table
COLUMN          appdb.users(name)      SELECT on specific columns only
```

### Common privileges

| Privilege | What |
|-----------|------|
| ALL [PRIVILEGES] | Everything |
| SELECT | Read |
| INSERT | Create rows |
| UPDATE | Modify rows |
| DELETE | Delete rows |
| CREATE | Create tables/DBs |
| DROP | Drop tables/DBs |
| ALTER | Modify schema |
| INDEX | Create / drop indexes |
| LOCK TABLES | Explicit table locks |
| EXECUTE | Run stored procedures |
| GRANT OPTION | Grant own privs to others |
| FILE | Read/write OS files (dangerous) |
| SUPER | Many admin ops (dangerous) |
| PROCESS | View other users' queries |
| RELOAD | FLUSH commands |
| REPLICATION SLAVE | Read binlog (replica) |
| REPLICATION CLIENT | Get binlog status |
| SHUTDOWN | Stop server |

### Grant

```sql
-- App user, limited
CREATE USER 'app'@'10.0.0.%' IDENTIFIED BY 'StrongP@ss!';
GRANT SELECT, INSERT, UPDATE, DELETE ON appdb.* TO 'app'@'10.0.0.%';
FLUSH PRIVILEGES;

-- Read-only reporting user
CREATE USER 'reporter'@'%' IDENTIFIED BY 'pwd';
GRANT SELECT ON appdb.* TO 'reporter'@'%';

-- DBA (full)
CREATE USER 'dba'@'localhost' IDENTIFIED BY 'pwd';
GRANT ALL PRIVILEGES ON *.* TO 'dba'@'localhost' WITH GRANT OPTION;
```

### Revoke

```sql
REVOKE INSERT, UPDATE ON appdb.* FROM 'reporter'@'%';
REVOKE ALL PRIVILEGES ON appdb.* FROM 'old_user'@'%';
DROP USER 'old_user'@'%';
```

### Show privileges

```sql
SHOW GRANTS FOR 'app'@'10.0.0.%';
SHOW GRANTS;     -- current user
```

---

## Principle of Least Privilege

> "Give **only what's needed**. Nothing more."

### Application user

```sql
-- App reads + writes business tables; no schema changes
CREATE USER 'app'@'10.0.0.%' IDENTIFIED BY 'pwd';
GRANT SELECT, INSERT, UPDATE, DELETE ON appdb.* TO 'app'@'10.0.0.%';
-- NOT: DROP, ALTER, GRANT, FILE, SUPER
```

### Common mistake

```sql
GRANT ALL ON *.* TO 'app'@'%';   -- ❌ App can DROP DATABASE!
```

→ One SQL injection = catastrophe.

### Multi-user pattern

```
ROLE: app_read         → SELECT
ROLE: app_write        → INSERT/UPDATE/DELETE
ROLE: app_admin        → CREATE/ALTER/DROP

Production app    → app_read + app_write
Migration tool    → app_admin (separate)
Reporting tool    → app_read
```

### Roles (MySQL 8+)

```sql
CREATE ROLE 'app_read', 'app_write';
GRANT SELECT ON appdb.* TO 'app_read';
GRANT INSERT, UPDATE, DELETE ON appdb.* TO 'app_write';

-- Assign
CREATE USER 'app1'@'10.0.0.%' IDENTIFIED BY 'pwd';
GRANT 'app_read', 'app_write' TO 'app1'@'10.0.0.%';
SET DEFAULT ROLE ALL TO 'app1'@'10.0.0.%';
```

### Hostname pattern

```sql
'app'@'localhost'      # only from local socket
'app'@'127.0.0.1'      # local IP
'app'@'10.0.0.%'       # private subnet
'app'@'%'              # ⚠️ anywhere
```

→ **Never `'%'` for production app users**.

---

## SSL/TLS for Connections

> "Encrypt **client → server** traffic."

### Check SSL status

```sql
SHOW VARIABLES LIKE 'have_ssl';        -- YES if compiled in
SHOW VARIABLES LIKE 'ssl%';
```

### Enable in `my.cnf`

```ini
[mysqld]
ssl_ca=/etc/mysql/ca.pem
ssl_cert=/etc/mysql/server-cert.pem
ssl_key=/etc/mysql/server-key.pem
require_secure_transport = ON      # reject non-SSL connections
```

### Generate self-signed certs (dev / internal)

MySQL provides auto-generated certs in `datadir`:

```
/var/lib/mysql/ca.pem
/var/lib/mysql/server-cert.pem
/var/lib/mysql/server-key.pem
```

### Force per-user SSL

```sql
CREATE USER 'app'@'%' REQUIRE SSL IDENTIFIED BY 'pwd';
ALTER USER 'app'@'%' REQUIRE SSL;

-- Stricter:
ALTER USER 'app'@'%' REQUIRE X509;          -- valid client cert
ALTER USER 'app'@'%' REQUIRE SUBJECT '/CN=client01.example.com';
ALTER USER 'app'@'%' REQUIRE CIPHER 'ECDHE-RSA-AES256-GCM-SHA384';
```

### Client connection — JDBC

```
jdbc:mysql://host/db?useSSL=true&verifyServerCertificate=true&trustCertificateKeyStoreUrl=...
```

### `caching_sha2_password` requires TLS

(Or RSA key exchange.) Plain auth requires SSL.

---

## Network Security

### 1. Bind address

```ini
[mysqld]
bind-address = 127.0.0.1     # localhost only
# or
bind-address = 10.0.0.5      # private subnet IP
```

→ **Don't bind 0.0.0.0** unless behind firewall + VPN.

### 2. Firewall (OS / cloud)

- iptables / ufw — only allow 3306 from app subnets
- AWS Security Group — allow 3306 only from app SG

### 3. SSH tunnel for admin

```bash
ssh -L 3306:localhost:3306 user@dbserver
mysql -h 127.0.0.1 -u root -p
```

### 4. VPC peering / private networks

In cloud, DB **never on public subnet**.

### 5. Default user removal

```sql
-- Remove anonymous
DROP USER ''@'localhost';
DROP USER ''@'%';

-- Remove test DB (if exists)
DROP DATABASE test;
```

→ `mysql_secure_installation` script does this automatically.

---

## Audit Plugins

> "**Audit logs** = compliance + forensics."

### MySQL Enterprise Audit (paid)

```ini
plugin-load = audit_log.so
audit_log_format = JSON
audit_log_file = /var/log/mysql/audit.json
```

### MariaDB Audit Plugin (free, works on MySQL too)

```bash
INSTALL PLUGIN server_audit SONAME 'server_audit.so';
```

```ini
server_audit_logging = ON
server_audit_events = CONNECT,QUERY,TABLE
server_audit_file_path = /var/log/mysql/audit.log
```

### What to log?

- All connection attempts (success + failure)
- DDL (CREATE / ALTER / DROP)
- Privilege changes (GRANT / REVOKE)
- Sensitive table access (financial / PII)

→ Don't log all SELECTs at scale (volume).

### Sample log line (JSON)

```json
{
  "ts": "2024-05-08T14:30:22Z",
  "user": "app@10.0.0.5",
  "event": "QUERY",
  "query": "DELETE FROM users WHERE id = 100",
  "result": "success"
}
```

---

## sql_mode

> "**Strict mode** controls — catches data errors instead of silently corrupting."

### Default (8.0)

```
ONLY_FULL_GROUP_BY, STRICT_TRANS_TABLES, NO_ZERO_IN_DATE, NO_ZERO_DATE,
ERROR_FOR_DIVISION_BY_ZERO, NO_ENGINE_SUBSTITUTION
```

### Important modes

| Mode | What |
|------|------|
| **STRICT_TRANS_TABLES** | Error on data truncation / invalid value (vs silent) |
| **STRICT_ALL_TABLES** | Same for non-transactional |
| **ONLY_FULL_GROUP_BY** | All non-aggregated cols in SELECT must be in GROUP BY |
| **NO_ZERO_DATE** | Reject `0000-00-00` |
| **NO_ENGINE_SUBSTITUTION** | Error if requested engine unavailable |

### Without strict mode (legacy)

```sql
INSERT INTO users (age) VALUES (1000);   -- ✅ silently truncated to 127 (TINYINT)
```

### With strict mode

```sql
INSERT INTO users (age) VALUES (1000);
-- ERROR 1264: Out of range value for column 'age' at row 1
```

→ **Always use strict mode in production.** Catches bugs early.

### Set

```sql
SET GLOBAL sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_DATE,...';
```

```ini
[mysqld]
sql_mode = STRICT_TRANS_TABLES,ONLY_FULL_GROUP_BY,...
```

---

## SQL Injection Prevention

> "**App-level concern**, but DB privileges = last line of defense."

### App side — parameterized queries

```java
// ❌ Vulnerable
String sql = "SELECT * FROM users WHERE id = " + userInput;

// ✅ Safe
PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
ps.setLong(1, userInput);
```

→ Cross-ref: `backend-skills / Security-Best-Practices / 02-SQL-Injection-Prevention.md`.

### DB side defense

1. **Limited app user privileges** (no DROP / ALTER) → injection limited
2. **No `FILE` privilege** → no `LOAD_FILE` / `INTO OUTFILE` exploit
3. **No SUPER** → can't disable logs / kill connections
4. **`secure_file_priv`** restricts file IO to specific dir:

```ini
secure_file_priv = /var/lib/mysql-files/
# or
secure_file_priv = NULL          # disable file ops entirely
```

### Stored procedure (limited mitigation)

```sql
CREATE PROCEDURE get_user(IN uid INT)
BEGIN
    SELECT * FROM users WHERE id = uid;
END;
```

→ Type check on parameter; not a full defense.

---

## Encryption at Rest

> "**Transparent Data Encryption (TDE)** — data files encrypted on disk."

### Enable

```ini
[mysqld]
early-plugin-load=keyring_file.so
keyring_file_data=/var/lib/mysql-keyring/keyring
```

### Encrypt tablespace

```sql
ALTER TABLE sensitive_data ENCRYPTION = 'Y';
```

### Encrypt redo + undo logs (8.0)

```ini
innodb_redo_log_encrypt = ON
innodb_undo_log_encrypt = ON
binlog_encryption = ON
```

### Encrypt all new tables by default

```ini
default_table_encryption = ON
```

### Key management

- Default plugin (`keyring_file`) — keys on local disk (less secure)
- Production: AWS KMS, HashiCorp Vault integration

---

## Backup Security

### 1. Encrypt backups

```bash
xtrabackup --backup --encrypt=AES256 \
    --encrypt-key-file=/etc/keys/backup.key
```

### 2. Restrict backup file permissions

```bash
chmod 600 /backup/*.sql
chown mysql:mysql /backup
```

### 3. Encrypted transport

S3 + SSE-S3, secure FTPS, etc.

### 4. Audit backup access

Log who downloaded backups.

→ Cross-ref: `11-Backup-and-Recovery.md`.

---

## Compliance Checklist

### Hardening checklist

```
[ ] Disabled remote root login
[ ] Removed anonymous users
[ ] Removed test DB
[ ] Strong passwords (validate_password)
[ ] caching_sha2_password (default 8.0)
[ ] SSL/TLS enabled, REQUIRE SSL on users
[ ] Bind to private interface only
[ ] Firewall rules
[ ] Least privilege users
[ ] Audit logging enabled
[ ] STRICT sql_mode
[ ] secure_file_priv restricted
[ ] Backup encryption
[ ] TDE for sensitive tables
[ ] Regular patching
[ ] Password rotation policy
[ ] Logged shell access (sudo log)
[ ] CIS MySQL benchmark applied
```

### CIS Benchmark

[CIS MySQL Benchmark](https://www.cisecurity.org) — comprehensive hardening guide.

### PCI DSS / HIPAA / SOC 2

Most regulations require:
- Encryption in transit + at rest
- Access logs
- Least privilege
- Strong authentication
- Backup encryption
- Vulnerability scanning

---

## Pitfalls

1. **`'app'@'%'`** — accept connections from anywhere.
2. **`GRANT ALL ON *.*`** to app user — SQL injection = total compromise.
3. **`require_secure_transport = OFF`** + cloud DB — credentials over plain TCP.
4. **`mysql_native_password`** in 2026 — weaker hash.
5. **No audit logs** — forensics impossible after breach.
6. **Default root password** unchanged.
7. **`bind-address = 0.0.0.0`** without firewall.
8. **No `STRICT_TRANS_TABLES`** — silent data corruption.
9. **`FILE` privilege** for app user — `LOAD_FILE` exploit.
10. **`secure_file_priv` empty** — file IO unrestricted.
11. **Backup files world-readable** (`chmod 644`) — credentials leak.
12. **No password expiration** — same password 5 years.
13. **`SUPER` granted to dev** — can disable logs, modify variables.
14. **`mysql.user` table direct UPDATE** — privileges bypass.

---

## Cheat Sheet

| Practice | Setting |
|----------|---------|
| Auth plugin | `caching_sha2_password` |
| Auth via SSL | `REQUIRE SSL` per user |
| Bind | `bind-address = 127.0.0.1` or private IP |
| Firewall | Only allow 3306 from app subnets |
| App user | Only DML on app DB; no DDL/SUPER/FILE |
| SQL mode | `STRICT_TRANS_TABLES`, `ONLY_FULL_GROUP_BY` |
| Audit | MariaDB Audit / MySQL Enterprise Audit |
| TDE | `ALTER TABLE ... ENCRYPTION='Y'` for sensitive |
| Backup | Encrypt + restrict perms |

| User type | Privileges |
|-----------|-----------|
| App | SELECT, INSERT, UPDATE, DELETE on app DB |
| Migration | + CREATE, ALTER, DROP (separate user) |
| Read-only | SELECT only |
| Replication | REPLICATION SLAVE |
| DBA | ALL on *.* (limited hosts) |

| Don't grant app user | |
|----------------------|--|
| FILE | LOAD_FILE exploit |
| SUPER | Disable logs |
| GRANT OPTION | Privilege escalation |
| CREATE USER | Spawn backdoor users |

---

## Practice

1. Run `mysql_secure_installation` on dev server.
2. Create app user with least privilege; verify CREATE TABLE is denied.
3. Enable SSL; force `REQUIRE SSL` on user; verify connection fails without SSL.
4. Set up MariaDB Audit Plugin; tail audit log while running queries.
5. Enable TDE on a sensitive table; verify file encryption.
6. Audit current users via `SELECT user, host FROM mysql.user;` — remove suspects.

# Flyway

## Status: Not Started

---

## Table of Contents

1. [Flyway Overview](#flyway-overview)
2. [Flyway Dependency](#flyway-dependency)
3. [Naming Convention](#naming-convention)
4. [Configuration Properties](#configuration-properties)
5. [Baseline](#baseline)
6. [Repair](#repair)
7. [Validate](#validate)
8. [Spring Boot Auto-Run](#spring-boot-auto-run)
9. [Common Flyway Commands](#common-flyway-commands)

---

## Flyway Overview

**Matlab:** Database schema ko version control mein manage karna — code ke saath database migrations bhi track hote hain.

### The Problem Without Migrations

```
Developer 1: ALTER TABLE users ADD COLUMN phone VARCHAR(20);  -- Local mein chalaya
Developer 2: ALTER TABLE users ADD COLUMN phone VARCHAR(20);  -- Local mein bhool gaya
Production:  Column exist hi nahi karta — app crash! ❌
```

### With Flyway

```
db/migration/
├── V1__create_users.sql
├── V2__add_phone_to_users.sql
├── V3__create_orders.sql
└── V4__add_status_to_orders.sql

Har environment pe same order mein run honge — consistent schema! ✅
```

### How Flyway Works

```
Application Start
    ↓
Flyway automatically runs
    ↓
Checks flyway_schema_history table
    ↓
Compares available migrations vs applied migrations
    ↓
Pending migrations run karta hai (version order mein)
    ↓
Application starts with up-to-date schema
```

---

## Flyway Dependency

### Maven

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>

<!-- Database driver -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

### Gradle

```groovy
implementation 'org.flywaydb:flyway-core'
runtimeOnly 'com.mysql:mysql-connector-j'
```

### Spring Boot Integration

```xml
<!-- Spring Boot mein already managed -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

**Note:** `spring-boot-starter-data-jpa` + `flyway-core` — Flyway automatically Spring Boot ke saath integrate ho jata hai.

---

## Naming Convention

Flyway file names ka strict pattern follow karta hai.

### Versioned Migrations (V + version)

```
V<version>__<description>.sql

Example:
V1__create_users.sql
V2__add_phone_to_users.sql
V3__create_orders.sql
V4__add_status_to_orders.sql
V5__create_user_roles.sql
V6__insert_default_roles.sql
```

**Rules:**
- `V` (capital) — versioned migration
- Version number — integer ya dotted format (`1`, `1.0`, `1.0.1`)
- `__` (double underscore) — version aur description separator
- `.sql` — file extension
- Description — alphanumeric + underscores (no spaces)

### Version Numbering

```
Sequential:
V1__create_users.sql
V2__add_phone.sql
V3__create_orders.sql

Dotted (major.minor.patch):
V1.0__create_users.sql
V1.1__add_phone.sql
V2.0__create_orders.sql
V2.0.1__fix_order_column.sql
```

### Repeatable Migrations (R__)

```
R__<description>.sql

Example:
R__create_views.sql
R__insert_seed_data.sql
R__update_stored_procedures.sql
```

**Matlab:** Yeh migrations har baar run hongi jab file change hogi — version tracking nahi hoti.

**Use Cases:**
- Views create/update
- Stored procedures
- Seed data (jo latest honi chahiye)

### Undo Migrations (U__)

```
U<version>__<description>.sql

Example:
U2__remove_phone_from_users.sql
U3__drop_orders.sql
```

**⚠️ Note:** Flyway ka undo feature **Flyway Teams (paid)** edition mein hai — open source mein available nahi hai.

### Naming Examples

| File Name | Type | Description |
|-----------|------|-------------|
| `V1__create_users.sql` | Versioned | Users table create karo |
| `V2__add_phone_to_users.sql` | Versioned | Phone column add karo |
| `V3__create_orders.sql` | Versioned | Orders table create karo |
| `R__create_views.sql` | Repeatable | Views update karo |
| `R__update_stored_procs.sql` | Repeatable | Stored procedures update karo |
| `U2__remove_phone.sql` | Undo | V2 ko undo karo (paid only) |

---

## Configuration Properties

```properties
# application.properties

# Migration files ka location
spring.flyway.locations=classpath:db/migration

# Migration files ka encoding
spring.flyway.encoding=UTF-8

# Table name jo migration track karti hai (default: flyway_schema_history)
spring.flyway.table=flyway_schema_history

# Schema name (default: default schema of datasource)
spring.flyway.schemas=public

# Migration fail hone pe application start hoga ya nahi
spring.flyway.fail-on-error=true

# Flyway enable/disable
spring.flyway.enabled=true

# Clean on validation error (development mein useful)
spring.flyway.clean-disabled=true

# Baseline version (existing database ke liye)
spring.flyway.baseline-version=1

# Baseline description
spring.flyway.baseline-description=Baseline

# Out of order migrations allow karna
spring.flyway.out-of-order=false

# Validate migration files checksum
spring.flyway.validate-on-migrate=true

# Placeholder replacement
spring.flyway.placeholder-replacement=true

# SQL migration prefix
spring.flyway.sql-migration-prefix=V

# SQL migration separator
spring.flyway.sql-migration-separator=__

# SQL migration suffix
spring.flyway.sql-migration-suffix=.sql

# Repeatable migrations prefix
spring.flyway.repeatable-sql-migration-prefix=R
```

### flyway.locations

```properties
# Default location
spring.flyway.locations=classpath:db/migration

# Multiple locations
spring.flyway.locations=classpath:db/migration,classpath:db/seed,classpath:db/procedures

# File system location (development)
spring.flyway.locations=filesystem:/path/to/migrations

# Mixed
spring.flyway.locations=classpath:db/migration,filesystem:/home/dev/custom-migrations
```

---

## Baseline

**Matlab:** Existing database ko Flyway ke saath register karna — purane migrations ko "already applied" mark karna.

### Scenario

```
Production database already has tables (without Flyway)
Ab Flyway introduce karna hai — lekin V1__create_users.sql run nahi karna
Kyunki table already exist karti hai!

Solution: Baseline — Flyway ko bolo "yeh migrations already applied hain"
```

### Setup

```properties
# Baseline version — yeh version "already applied" mark hoga
spring.flyway.baseline-version=1

# Already applied migrations ko baseline mark karo
spring.flyway.baseline-on-migrate=true
```

### Baseline Command

```bash
# Maven
mvn flyway:baseline -Dflyway.baselineVersion=1 -Dflyway.baselineDescription="Initial schema"

# Gradle
./gradlew flywayBaseline -Pflyway.baselineVersion=1

# Flyway CLI
flyway baseline -baselineVersion=1 -baselineDescription="Initial schema"
```

**Result:** `flyway_schema_history` table mein entry ban jayegi — V1 "applied" mark ho jayegi.

### When to Use Baseline

| Scenario | Use Baseline? |
|----------|---------------|
| New project — empty database | ❌ No need |
| Existing database — adding Flyway | ✅ Yes |
| Merging databases | ✅ Yes |
| Production with manual changes | ✅ Yes |

---

## Repair

**Matlab:** Flyway ki `flyway_schema_history` table ko fix karna — failed migrations ya checksum mismatches repair karna.

### When to Use Repair

| Issue | Repair Action |
|-------|---------------|
| Failed migration in history | Failed entry remove karta hai |
| Checksum mismatch | Checksum update karta hai |
| Missing migration | History se remove karta hai |
| Applied description change | Description update karta hai |

### Repair Command

```bash
# Maven
mvn flyway:repair

# Gradle
./gradlew flywayRepair

# Flyway CLI
flyway repair
```

**⚠️ Warning:** Repair se pehle database ka backup le lo — destructive operation ho sakti hai.

### Checksum Mismatch Fix

```
Problem: V1__create_users.sql file edit kiya — checksum change ho gaya
Flyway: "Migration V1 checksum mismatch! Validation failed."

Solution 1: Repair (checksum update)
    mvn flyway:repair

Solution 2: Revert file changes (recommended)
    git checkout V1__create_users.sql
```

---

## Validate

**Matlab:** Pending migrations run karne se pehle check karna ki sab kuch sahi hai — checksum verify, missing migrations check.

### Validate Command

```bash
# Maven
mvn flyway:validate

# Gradle
./gradlew flywayValidate

# Flyway CLI
flyway validate
```

### What Validate Checks

| Check | Description |
|-------|-------------|
| **Checksum mismatch** | Applied migration ka checksum vs current file checksum |
| **Missing migrations** | History mein hai lekin file system mein nahi |
| **Duplicate versions** | Same version ke multiple files |
| **Future migrations applied** | Version number order maintain hai |

### Validation Error

```
ERROR: Validate failed:
  - Migration checksum mismatch for migration 1
  - Detected applied migration not resolved locally: 2
  - Missing migration: V3

Fix: mvn flyway:repair (checksum mismatch ke liye)
```

### Auto-Validate on Migrate

```properties
# Default: true — migrate se pehle automatically validate hoga
spring.flyway.validate-on-migrate=true
```

---

## Spring Boot Auto-Run

**Matlab:** Application start hone pe Flyway automatically pending migrations run kar deta hai.

### How It Works

```
SpringApplication.run()
    ↓
FlywayAutoConfiguration activates
    ↓
DataSource se connection banao
    ↓
flyway_schema_history table check karo
    ↓
classpath:db/migration/ se migrations scan karo
    ↓
Pending migrations identify karo (version order mein)
    ↓
Har pending migration run karo
    ↓
flyway_schema_history update karo
    ↓
Application fully starts
```

### Enable/Disable

```properties
# Enable (default)
spring.flyway.enabled=true

# Disable (testing ya specific environments mein)
spring.flyway.enabled=false

# Clean disable (production mein zaruri)
spring.flyway.clean-disabled=true
```

### Disable in Production

```properties
# application-prod.properties
# Production mein migrations manually run karo — auto-run risk hai
spring.flyway.enabled=false
```

### Multiple DataSources

```java
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationInitializer flywayInitializer(Flyway flyway) {
        return new FlywayMigrationInitializer(flyway);
    }

    @Bean
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load();
    }
}
```

---

## Common Flyway Commands

### migrate

```bash
# Pending migrations run karo
mvn flyway:migrate

# Output:
# Current version: 3
# Target version: 5
# Migrating schema "public" to version "4 - create_orders"
# Migrating schema "public" to version "5 - add_status"
# Successfully applied 2 migrations
```

### info

```bash
# Migration status dekho
mvn flyway:info

# Output:
# +-----------+------------------------+---------------------+---------+
# | Category  | Version                | Description         | State   |
# +-----------+------------------------+---------------------+---------+
# | Versioned | 1                      | create users        | Success |
# | Versioned | 2                      | add phone           | Success |
# | Versioned | 3                      | create orders       | Success |
# | Versioned | 4                      | add status          | Pending |
# | Versioned | 5                      | create roles        | Pending |
# +-----------+------------------------+---------------------+---------+
```

### clean

```bash
# ⚠️ WARNING: Sab tables drop kar dega!
mvn flyway:clean

# Production mein disable karo
spring.flyway.clean-disabled=true
```

### undo

```bash
# ⚠️ Flyway Teams (paid) only
mvn flyway:undo -Dflyway.target=2
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Flyway** | Database schema version control |
| **Naming** | `V1__description.sql` (versioned), `R__description.sql` (repeatable) |
| **flyway.locations** | Migration files ka path — default: `classpath:db/migration` |
| **Baseline** | Existing database ko Flyway ke saath register karo |
| **Repair** | History table fix karo — checksum mismatch, failed entries |
| **Validate** | Pending migrations se pehle sanity check |
| **Auto-Run** | Spring Boot startup pe automatically pending migrations run hoti hain |
| **clean-on-validation-error** | Development mein useful — validation fail pe clean karo |
| **flyway.enabled** | Flyway enable/disable — production mein manually run karna better |

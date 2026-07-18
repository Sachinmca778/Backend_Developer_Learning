# Database Migrations - Top 10 Interview Questions & Answers

> Covering: Flyway, Liquibase, Migration Best Practices, Baseline, Rollback, and Zero-Downtime Deployment

---

## Q1: Database Migration kya hai aur production mein kyu zaruri hai?

**Answer:**

Database Migration database schema changes ko version control mein manage karne ka tarika hai — code ke saath database changes bhi track hote hain.

### Problem Without Migrations:

```
Developer 1: ALTER TABLE users ADD COLUMN phone;  -- Local mein chalaya
Developer 2: Column add karna bhool gaya
Production:  Column exist nahi karta — app crash! ❌
```

### With Migrations:

```
db/migration/
├── V1__create_users.sql
├── V2__add_phone_to_users.sql
├── V3__create_orders.sql

Har environment pe same order mein run honge — consistent schema! ✅
```

### How It Works:

```
Application Start
    ↓
Migration tool (Flyway/Liquibase) runs
    ↓
History table check karta hai (flyway_schema_history / DATABASECHANGELOG)
    ↓
Pending migrations identify karta hai
    ↓
Version order mein run karta hai
    ↓
Application starts with up-to-date schema
```

### Benefits:

| Benefit | Description |
|---------|-------------|
| **Version Control** | Schema changes git mein track hote hain |
| **Consistency** | Dev, staging, prod — sab same schema |
| **Automation** | App start pe automatically run |
| **Audit Trail** | Kaunsi migration kab run hui — history table mein |
| **Team Collaboration** | Sab developers same migrations follow karte hain |

---

## Q2: Flyway naming convention aur migration types kya hain?

**Answer:**

Flyway file names ka strict pattern follow karta hai.

### Versioned Migrations:

```
V<version>__<description>.sql

V1__create_users.sql
V2__add_phone_to_users.sql
V3__create_orders.sql
```

**Rules:**
- `V` (capital) — versioned migration
- Version number — integer ya dotted (`1`, `1.0`, `1.0.1`)
- `__` (double underscore) — separator
- Description — alphanumeric + underscores (no spaces)

### Repeatable Migrations:

```
R__<description>.sql

R__create_views.sql
R__insert_seed_data.sql
```

Har baar run hongi jab file change hogi — views, stored procedures, seed data ke liye.

### Migration Types:

| Type | Prefix | Use Case |
|------|--------|----------|
| **Versioned** | `V` | Schema changes (tables, columns) |
| **Repeatable** | `R` | Views, procedures, seed data |
| **Undo** | `U` | Rollback (Flyway Teams — paid only) |

### Example Migration:

```sql
-- V1__create_users.sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- V2__add_phone_to_users.sql
ALTER TABLE users ADD COLUMN phone VARCHAR(20) DEFAULT NULL;
```

---

## Q3: Flyway configuration aur Spring Boot integration kaise hoti hai?

**Answer:**

### Dependency:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

### Configuration:

```properties
# Migration files location
spring.flyway.locations=classpath:db/migration

# Enable/disable
spring.flyway.enabled=true

# Fail on error
spring.flyway.fail-on-error=true

# Validate before migrate
spring.flyway.validate-on-migrate=true

# Production mein clean disable karo
spring.flyway.clean-disabled=true

# History table name
spring.flyway.table=flyway_schema_history
```

### Spring Boot Auto-Run:

```
SpringApplication.run()
    ↓
FlywayAutoConfiguration activates
    ↓
classpath:db/migration/ se migrations scan
    ↓
Pending migrations run (version order)
    ↓
flyway_schema_history update
    ↓
Application fully starts
```

### Common Commands:

```bash
mvn flyway:migrate    # Pending migrations run
mvn flyway:info       # Migration status dekho
mvn flyway:validate   # Checksum verify
mvn flyway:repair     # History table fix
mvn flyway:clean      # ⚠️ Sab tables drop — production mein mat karo!
```

### Production Best Practice:

```properties
# application-prod.properties
spring.flyway.enabled=true
spring.flyway.clean-disabled=true  # Zaruri!
```

---

## Q4: Flyway aur Liquibase mein kya difference hai?

**Answer:**

| Feature | Flyway | Liquibase |
|---------|--------|-----------|
| **Format** | SQL (mostly) | XML, YAML, JSON, SQL |
| **Database Independent** | ❌ SQL database-specific | ✅ Abstract change types |
| **Rollback Support** | Paid (Teams) | ✅ Open source |
| **Preconditions** | Limited | ✅ Rich |
| **Contexts** | Limited | ✅ Environment-specific |
| **Learning Curve** | Easy | Medium |
| **Tracking Table** | `flyway_schema_history` | `DATABASECHANGELOG` |

### Flyway Example:

```sql
-- V1__create_users.sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);
```

### Liquibase Example:

```xml
<changeSet id="1" author="sachin">
    <createTable tableName="users">
        <column name="id" type="BIGINT" autoIncrement="true">
            <constraints primaryKey="true"/>
        </column>
        <column name="name" type="VARCHAR(100)">
            <constraints nullable="false"/>
        </column>
    </createTable>
</changeSet>
```

### When to Use What:

| Scenario | Use |
|----------|-----|
| Simple SQL migrations | Flyway |
| Multi-database support | Liquibase |
| Open source rollback | Liquibase |
| Quick setup, SQL-first team | Flyway |
| Complex preconditions | Liquibase |

**Short Interview Answer:**

Flyway SQL-based, simple, aur popular hai. Liquibase multiple formats support karta hai, database-independent hai, aur open source rollback deta hai.

---

## Q5: Flyway Baseline, Repair aur Validate kya hain?

**Answer:**

### Baseline — Existing Database Register Karna:

Jab production database pehle se tables ke saath exist karti hai aur ab Flyway introduce karna ho.

```properties
spring.flyway.baseline-version=1
spring.flyway.baseline-on-migrate=true
```

```
Problem: Table already exists, V1__create_users.sql run nahi karna
Solution: Baseline — V1 "already applied" mark ho jayegi
```

| Scenario | Use Baseline? |
|----------|---------------|
| New project — empty DB | ❌ No |
| Existing DB — adding Flyway | ✅ Yes |

### Repair — History Table Fix:

```bash
mvn flyway:repair
```

| Issue | Repair Action |
|-------|---------------|
| Failed migration in history | Failed entry remove |
| Checksum mismatch | Checksum update |
| Missing migration | History se remove |

**⚠️ Warning:** Repair se pehle database backup lo.

### Validate — Sanity Check:

```bash
mvn flyway:validate
```

| Check | Description |
|-------|-------------|
| Checksum mismatch | Applied vs current file checksum |
| Missing migrations | History mein hai, file system mein nahi |
| Duplicate versions | Same version ke multiple files |

```properties
spring.flyway.validate-on-migrate=true  # Default: true
```

---

## Q6: Applied migrations ko modify kyu nahi karna chahiye?

**Answer:**

Ek baar migration run ho gayi — uski file ko kabhi edit mat karo. Naya migration banao.

### ❌ Bad Practice:

```sql
-- V1__create_users.sql (already applied in production)
-- Developer ne locally phone column add kiya — checksum mismatch!
```

```
Flyway Error:
  Migration checksum mismatch for V1
  Applied checksum: abc123
  Current checksum:  def456
```

### ✅ Good Practice:

```sql
-- V1__create_users.sql (DO NOT TOUCH)
CREATE TABLE users (...);

-- V2__add_phone_to_users.sql (NEW migration)
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
```

### Why Checksums Matter:

```
Flyway har file ka SHA-256 checksum calculate karta hai
Migration run → checksum history table mein store
Next validation → current vs stored checksum compare
Mismatch → ERROR!
```

### What If You MUST Modify?

```bash
# Development only — fresh start
mvn flyway:clean     # ⚠️ Sab tables drop!
mvn flyway:migrate

# Dangerous — checksum update
mvn flyway:repair

# Proper way — new migration banao
# V3__fix_users_column.sql
```

**Interview Point:** Production mein applied migrations immutable hain — always create new migration for changes.

---

## Q7: Backward-compatible schema changes aur Expand-Contract pattern kya hai?

**Answer:**

Backward-compatible changes existing application code ko break nahi karte — zero-downtime deployment ke liye zaruri.

### ❌ Breaking Change:

```sql
ALTER TABLE users RENAME COLUMN name TO full_name;
```

```
T1: Migration run → column renamed
T2: Old code still running → SELECT name → ERROR!
T3: New code deployed → Works

T1 to T3 window → Application broken! ❌
```

### ✅ Expand-Contract Pattern:

**Phase 1 — Expand:** Naya column add karo, purana bhi rahe.

```sql
-- V5__add_full_name.sql
ALTER TABLE users ADD COLUMN full_name VARCHAR(100);
```

**Phase 2 — Migrate:** Data copy karo.

```sql
-- V6__copy_name_to_full_name.sql
UPDATE users SET full_name = name WHERE full_name IS NULL;
```

**Phase 3 — Contract:** Purana column remove karo.

```sql
-- V7__drop_name_column.sql
ALTER TABLE users DROP COLUMN name;
```

### Safe vs Unsafe Operations:

| Operation | Safe? |
|-----------|-------|
| ADD COLUMN (nullable) | ✅ Yes |
| ADD COLUMN (NOT NULL with default) | ✅ Yes |
| ADD INDEX | ✅ Yes |
| DROP COLUMN | ❌ No |
| RENAME COLUMN | ❌ No |
| CHANGE DATA TYPE | ❌ No |

---

## Q8: Liquibase changeSet, Preconditions aur Rollback kaise kaam karte hain?

**Answer:**

### changeSet — Basic Unit:

Har database change ek `changeSet` mein define hota hai — unique `id` + `author` combination.

```xml
<changeSet id="1" author="sachin">
    <createTable tableName="users">
        <column name="id" type="BIGINT" autoIncrement="true">
            <constraints primaryKey="true"/>
        </column>
        <column name="name" type="VARCHAR(100)"/>
    </createTable>
</changeSet>
```

### Preconditions — Run Se Pehle Check:

```xml
<changeSet id="4" author="sachin">
    <preConditions onFail="HALT">
        <not>
            <tableExists tableName="users"/>
        </not>
    </preConditions>
    <createTable tableName="users">...</createTable>
</changeSet>
```

| onFail | Behavior |
|--------|----------|
| **HALT** | Stop all migrations |
| **WARN** | Warning log, continue |
| **SKIP** | Skip this changeSet |

### Rollback — Open Source Support:

```xml
<changeSet id="7" author="sachin">
    <addColumn tableName="users">
        <column name="phone" type="VARCHAR(20)"/>
    </addColumn>
    <rollback>
        <dropColumn tableName="users" columnName="phone"/>
    </rollback>
</changeSet>
```

```bash
mvn liquibase:rollback -Dliquibase.rollbackCount=1
```

### runOnChange — Views/Procedures:

```xml
<changeSet id="10" author="sachin" runOnChange="true">
    <createView viewName="active_users_view">
        SELECT id, name FROM users WHERE active = true
    </createView>
</changeSet>
```

File change hone pe re-run hoga.

---

## Q9: Liquibase Contexts aur Labels kya hain?

**Answer:**

### Contexts — Environment-Specific Migrations:

```xml
<!-- Sirf dev mein seed data -->
<changeSet id="13" author="sachin" context="dev">
    <insert tableName="users">
        <column name="name" value="Test User"/>
    </insert>
</changeSet>

<!-- Sab environments mein schema -->
<changeSet id="15" author="sachin">
    <createTable tableName="users">...</createTable>
</changeSet>
```

```properties
# application-dev.properties
spring.liquibase.contexts=dev

# application-prod.properties
spring.liquibase.contexts=prod
```

### Labels — Categorization:

```xml
<changeSet id="18" author="sachin" labels="users,core">
    <createTable tableName="users">...</createTable>
</changeSet>

<changeSet id="20" author="sachin" labels="users,performance">
    <createIndex indexName="idx_users_email" tableName="users">
        <column name="email"/>
    </createIndex>
</changeSet>
```

### Contexts vs Labels:

| Feature | Contexts | Labels |
|---------|----------|--------|
| Purpose | Environment filtering | Categorization |
| Execution | Affects which changeSets run | Reporting/filtering |

### Master Changelog Structure:

```
db/changelog/
├── db.changelog-master.xml
└── changes/
    ├── 001-create-users.xml
    ├── 002-create-orders.xml
    └── 003-seed-data.xml
```

```properties
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml
```

---

## Q10: Database migration ke best practices kya hain?

**Answer:**

### 1. Never Modify Applied Migrations:

Naya migration banao — purani file touch mat karo.

### 2. Test on Production Copy:

```
1. Production DB backup
2. Staging pe restore
3. Migrations run
4. Performance measure
5. Rollback test
6. Production deploy
```

### 3. One Logical Change Per Migration:

```sql
-- ✅ Good
-- V5__add_phone_to_users.sql
ALTER TABLE users ADD COLUMN phone VARCHAR(20);

-- ❌ Bad — multiple unrelated changes
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
CREATE TABLE orders (...);
DROP TABLE temp_logs;
```

### 4. Descriptive Names:

```
✅ V5__add_phone_to_users.sql
✅ V6__create_orders_table.sql
❌ V5__update.sql
❌ V6__fix.sql
```

### 5. Document Migrations:

```sql
-- V5__add_phone_to_users.sql
-- Reason: SMS notifications ke liye phone number chahiye
-- JIRA: PROJ-123
-- Rollback: ALTER TABLE users DROP COLUMN phone;

ALTER TABLE users ADD COLUMN phone VARCHAR(20) DEFAULT NULL;
```

### 6. Idempotent Where Possible:

```sql
CREATE TABLE IF NOT EXISTS users (...);
```

### 7. Separate Seed Data:

```
db/migration/     → Schema (V1, V2, V3)
R__seed_data.sql  → Repeatable seed data (dev only)
```

### 8. Online Indexes for Large Tables:

```sql
-- MySQL
CREATE INDEX idx_users_email ON users(email) ALGORITHM=INPLACE, LOCK=NONE;

-- PostgreSQL
CREATE INDEX CONCURRENTLY idx_users_email ON users(email);
```

### 9. Version Control:

```bash
git add db/migration/V5__add_phone_to_users.sql
git commit -m "feat: add phone column for SMS notifications"
```

### 10. Rollback Plan:

Har migration ke saath rollback SQL comment mein likho.

**Short Interview Answer:**

Never modify applied migrations, test on prod copy, one change per migration, descriptive names, document reason/rollback, and use expand-contract for zero-downtime.

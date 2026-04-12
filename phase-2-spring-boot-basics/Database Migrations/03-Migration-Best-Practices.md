# Migration Best Practices

## Status: Not Started

---

## Table of Contents

1. [Never Modify Applied Migrations](#never-modify-applied-migrations)
2. [Test Migrations on Copy of Production DB](#test-migrations-on-copy-of-production-db)
3. [Backward-Compatible Schema Changes](#backward-compatible-schema-changes)
4. [Expand-Contract Pattern](#expand-contract-pattern)
5. [Indexing Strategy During Migration](#indexing-strategy-during-migration)
6. [General Best Practices](#general-best-practices)

---

## Never Modify Applied Migrations

**Matlab:** Ek baar migration run ho gayi — uski file ko kabhi edit mat karo. Naya migration banao changes ke liye.

### ❌ Bad Practice: Modifying Existing Migration

```sql
-- V1__create_users.sql (already applied in production)
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100)
);

-- ❌ Developer ne locally edit kiya — phone column add kiya
-- Ab checksum mismatch hoga!
```

**Problem:**
```
Flyway Error:
  Migration checksum mismatch for V1
  Applied checksum: abc123
  Current checksum:  def456

Fix: Either revert file changes OR run flyway:repair
```

### ✅ Good Practice: New Migration for Changes

```sql
-- V1__create_users.sql (DO NOT TOUCH)
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100)
);

-- V2__add_phone_to_users.sql (NEW migration)
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
```

### Why Checksums Matter

```
Flyway har migration file ka checksum calculate karta hai
Checksum = file content ka hash (SHA-256)
Jab migration run hoti hai → checksum DATABASECHANGELOG mein store hota hai
Next validation pe → current file checksum vs stored checksum compare hota hai
Match nahi hua → ERROR!
```

### What If You MUST Modify?

```bash
# 1. Sirf development mein — jahan abhi koi data nahi
mvn flyway:clean     # ⚠️ Sab tables drop honge!
mvn flyway:migrate   # Fresh migrations run hongi

# 2. Checksum update (dangerous — production mein mat karo)
mvn flyway:repair    # Checksum update ho jayega

# 3. Proper way — new migration banao
# V3__fix_users_column.sql
```

### Git Protection

```bash
# Migrations folder ko protected banao
# .gitattributes
db/migration/*.sql -diff

# Pre-commit hook — migrations edit se bachao
#!/bin/bash
for file in $(git diff --cached --name-only | grep 'db/migration/V.*\.sql'); do
    if git diff --cached $file | grep -v "^+++" | grep -v "^---" | grep -q "^[-+]"; then
        echo "ERROR: Cannot modify existing migration file: $file"
        exit 1
    fi
done
```

---

## Test Migrations on Copy of Production DB

**Matlab:** Migration ko production mein chalane se pehle — production data ke copy pe test karo.

### Why Test on Production Copy?

| Risk | Example |
|------|---------|
| **Large table lock** | 10M rows table pe ALTER TABLE — hours lag sakte hain |
| **Data type mismatch** | VARCHAR → INT convert — existing data invalid ho sakta hai |
| **Constraint violation** | NOT NULL add karo — existing NULL rows fail hongi |
| **Index creation time** | Large tables pe index banana — slow ho sakta hai |

### Testing Workflow

```
1. Production DB ka backup lo
2. Staging/Testing environment mein restore karo
3. Migrations run karo
4. Performance impact measure karo
5. Rollback test karo
6. Sab sahi → Production mein deploy karo
```

### Docker-based Testing

```bash
# Production-like DB spin up karo
docker run -d \
  --name test-db \
  -e MYSQL_ROOT_PASSWORD=pass \
  -e MYSQL_DATABASE=mydb \
  -p 3307:3306 \
  mysql:8.0

# Production data import (sanitized)
docker exec -i test-db mysql -uroot -ppass mydb < prod_backup_sanitized.sql

# Migrations run karo
mvn flyway:migrate -Dspring.datasource.url=jdbc:mysql://localhost:3307/mydb

# App test karo
mvn spring-boot:run -Dspring.profiles.active=test
```

### Data Sanitization

```bash
# Production data se sensitive info hatao
mysqldump -u root -p mydb \
  --ignore-table=mydb.users \
  | sed 's/real_email@test.com/test@test.com/g' \
  | sed 's/real_phone/0000000000/g' \
  > sanitized_backup.sql
```

### Automated Migration Testing

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.flyway.clean-disabled=false",
    "spring.flyway.enabled=true"
})
class MigrationTest {

    @Autowired
    private Flyway flyway;

    @Test
    void testMigrationsApplySuccessfully() {
        // Clean + migrate
        flyway.clean();
        flyway.migrate();

        // Verify tables exist
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        int tableCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'mydb'",
            Integer.class
        );

        assertThat(tableCount).isGreaterThan(0);
    }
}
```

---

## Backward-Compatible Schema Changes

**Matlab:** Aise schema changes jo existing application code ko break nahi karte — zero-downtime deployment.

### ❌ Breaking Change

```sql
-- V5__rename_column.sql
ALTER TABLE users RENAME COLUMN name TO full_name;
```

**Problem:**
```
Deployment timeline:
  T1: New migration run → column renamed to full_name
  T2: Old code still running → SELECT name FROM users → ERROR! (column doesn't exist)
  T3: New code deployed → SELECT full_name FROM users → Works

T1 to T3 window mein → Application broken! ❌
```

### ✅ Backward-Compatible Approach (Expand-Contract)

```sql
-- Step 1: New column add karo (both columns exist)
-- V5__add_full_name.sql
ALTER TABLE users ADD COLUMN full_name VARCHAR(100);

-- Step 2: Code update — both columns read/write karo
-- (Application code mein changes)

-- Step 3: Data migrate karo
-- V6__migrate_name_to_full_name.sql
UPDATE users SET full_name = name WHERE full_name IS NULL;

-- Step 4: Old column remove karo
-- V7__drop_name_column.sql
ALTER TABLE users DROP COLUMN name;
```

### Safe vs Unsafe Operations

| Operation | Safe? | Notes |
|-----------|-------|-------|
| **ADD COLUMN (nullable)** | ✅ Yes | Existing rows NULL hongi |
| **ADD COLUMN (NOT NULL with default)** | ✅ Yes | Existing rows default value lengi |
| **ADD INDEX** | ✅ Yes | Usually online — no lock |
| **DROP COLUMN** | ❌ No | Data loss + code break |
| **RENAME COLUMN** | ❌ No | Code break |
| **CHANGE DATA TYPE** | ❌ No | Existing data incompatible ho sakta |
| **ADD NOT NULL (existing data)** | ❌ No | Existing NULL rows fail hongi |

### Safe Column Add

```sql
-- ✅ Safe — existing code ko affect nahi karega
ALTER TABLE users ADD COLUMN phone VARCHAR(20) DEFAULT NULL;

-- ✅ Safe — default value se existing rows populate hongi
ALTER TABLE users ADD COLUMN active BOOLEAN DEFAULT true;
```

### Safe Column Removal

```sql
-- ❌ Unsafe — direct drop
ALTER TABLE users DROP COLUMN name;

-- ✅ Safe — expand-contract pattern follow karo
-- 1. Code update: Stop writing to 'name' column
-- 2. Data migration: name → full_name copy karo
-- 3. Drop: Ab drop karo (jab code fully deployed ho)
ALTER TABLE users DROP COLUMN name;
```

---

## Expand-Contract Pattern

**Matlab:** Schema changes ko 3 phases mein karna — taaki zero-downtime deployment possible ho.

### Phase 1: Expand

**Naya schema element add karo — purana bhi exist rahega.**

```sql
-- V5__add_full_name_column.sql
ALTER TABLE users ADD COLUMN full_name VARCHAR(100);
```

**Application code update:**
```java
// Old code:
user.getName()

// New code (both columns handle karo):
user.getFullName() != null ? user.getFullName() : user.getName()

// Write to BOTH columns:
user.setName(newName);        // Old column
user.setFullName(newName);    // New column
```

### Phase 2: Migrate Data

**Existing data ko new schema mein copy karo.**

```sql
-- V6__copy_name_to_full_name.sql
UPDATE users SET full_name = name WHERE full_name IS NULL;
```

**Application code update:**
```java
// Ab sirf new column use karo
user.getFullName();  // Sirf full_name
user.setFullName(newName);  // Sirf full_name
```

### Phase 3: Contract

**Purana schema element remove karo.**

```sql
-- V7__drop_name_column.sql
ALTER TABLE users DROP COLUMN name;
```

### Complete Example: Renaming a Column

```
Timeline:
─────────────────────────────────────────────────────────────
Deploy 1:    V5 migration run → full_name column add
             Code: name + full_name dono mein write karo
             Read: full_name prefer karo, fallback to name

Deploy 2:    V6 migration run → data copy name → full_name
             Code: Sirf full_name use karo

Deploy 3:    V7 migration run → name column drop
             Code: Same (sirf full_name)
─────────────────────────────────────────────────────────────
```

### Complete Example: Changing Data Type

```sql
-- Phase 1: New column with new type
-- V10__add_age_numeric.sql
ALTER TABLE users ADD COLUMN age_numeric INT;

-- Phase 2: Migrate data
-- V11__migrate_age_to_numeric.sql
UPDATE users SET age_numeric = CAST(REPLACE(age, ' years', '') AS UNSIGNED)
WHERE age IS NOT NULL;

-- Phase 3: Drop old column
-- V12__drop_age_string.sql
ALTER TABLE users DROP COLUMN age;
```

---

## Indexing Strategy During Migration

**Matlab:** Indexes ko smartly create karna — large tables pe performance impact minimize karna.

### The Problem

```sql
-- ❌ Large table (10M rows) pe index banana
CREATE INDEX idx_users_email ON users(email);

-- Problem:
-- - Table lock ho sakti hai
-- - Minutes ya hours lag sakte hain
-- - Application queries slow/block ho sakte hain
```

### Solution 1: Online DDL (MySQL)

```sql
-- ✅ Online index creation — table lock nahi hoga
CREATE INDEX idx_users_email ON users(email) ALGORITHM=INPLACE, LOCK=NONE;
```

### Solution 2: Concurrent Index (PostgreSQL)

```sql
-- ✅ Concurrent index — reads/writes block nahi honge
CREATE INDEX CONCURRENTLY idx_users_email ON users(email);
```

### Solution 3: Liquibase with SQL

```xml
<changeSet id="index-users.email" author="sachin" runOnChange="true">
    <sql dbms="mysql">
        CREATE INDEX idx_users_email ON users(email) ALGORITHM=INPLACE, LOCK=NONE
    </sql>
    <sql dbms="postgresql">
        CREATE INDEX CONCURRENTLY idx_users_email ON users(email)
    </sql>
    <rollback>
        <dropIndex indexName="idx_users_email" tableName="users"/>
    </rollback>
</changeSet>
```

### Index Creation Best Practices

| Practice | Description |
|----------|-------------|
| **Separate migration** | Index ko alag migration mein banao — table changes se alag |
| **Off-peak hours** | Large indexes off-peak hours mein banao |
| **Monitor progress** | `SHOW PROCESSLIST` (MySQL) se progress dekho |
| **Test on prod copy** | Pehle staging pe measure karo — kitna time lega |
| **Covering indexes** | Sirf zaruri columns — over-indexing avoid karo |

### When to Create Indexes

```sql
-- ✅ Foreign keys pe index
CREATE INDEX idx_orders_user_id ON orders(user_id);

-- ✅ WHERE clause mein frequently used columns
CREATE INDEX idx_users_email ON users(email);

-- ✅ ORDER BY columns
CREATE INDEX idx_posts_created ON posts(created_at DESC);

-- ✅ Composite indexes — query pattern dekh ke
CREATE INDEX idx_orders_status_date ON orders(status, created_at);

-- ❌ Low cardinality columns (gender, boolean)
-- CREATE INDEX idx_users_active ON users(active);  -- Usually not useful

-- ❌ Already indexed columns (duplicate)
-- CREATE INDEX idx_users_email_2 ON users(email);  -- Waste of space
```

### Index Monitoring

```sql
-- Unused indexes dekho (MySQL)
SELECT table_schema, table_name, index_name
FROM sys.schema_unused_indexes
WHERE table_schema = 'mydb';

-- Unused indexes drop karo
DROP INDEX idx_unused ON users;
```

---

## General Best Practices

### 1. One Logical Change Per Migration

```sql
-- ✅ Good — ek migration, ek change
-- V5__add_phone_to_users.sql
ALTER TABLE users ADD COLUMN phone VARCHAR(20);

-- ❌ Bad — multiple unrelated changes ek migration mein
-- V5__multiple_changes.sql
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
CREATE TABLE orders (...);
DROP TABLE temp_logs;
```

### 2. Idempotent Migrations

```sql
-- ✅ Idempotent — multiple baar run kar sakte ho
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100)
);

-- ❌ Not idempotent — second run pe error
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100)
);
```

### 3. Transaction-Aware Migrations

```sql
-- ✅ DDL statements jo transaction support karte hain (PostgreSQL)
BEGIN;
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
ALTER TABLE users ADD COLUMN address VARCHAR(200);
COMMIT;

-- ❌ MySQL mein implicit commit — transaction kaam nahi karega
```

### 4. Document Your Migrations

```sql
-- V5__add_phone_to_users.sql
-- Reason: User phone number collect karna hai for SMS notifications
-- JIRA: PROJ-123
-- Impact: New column — existing queries ko affect nahi karega
-- Rollback: ALTER TABLE users DROP COLUMN phone;

ALTER TABLE users ADD COLUMN phone VARCHAR(20) DEFAULT NULL;
```

### 5. Use Descriptive Names

```
-- ✅ Good
V5__add_phone_to_users.sql
V6__create_orders_table.sql
V7__add_index_on_orders_user_id.sql

-- ❌ Bad
V5__update.sql
V6__changes.sql
V7__fix.sql
```

### 6. Seed Data Separation

```
db/migration/
├── V1__create_users.sql          # Schema migrations
├── V2__create_orders.sql         # Schema migrations
└── R__seed_data.sql              # Repeatable — seed data (dev only)
```

```properties
# Production mein seed data skip karo
spring.liquibase.contexts=!prod
```

### 7. Version Control Everything

```bash
# Migrations ko git mein commit karo
git add db/migration/V5__add_phone_to_users.sql
git commit -m "feat: add phone column to users for SMS notifications (PROJ-123)"
```

### 8. Rollback Plan

```sql
-- V5__add_phone_to_users.sql
-- Rollback: ALTER TABLE users DROP COLUMN phone;

ALTER TABLE users ADD COLUMN phone VARCHAR(20);
```

---

## Summary

| Practice | Key Point |
|----------|-----------|
| **Never modify applied migrations** | Checksum mismatch hoga — naya migration banao |
| **Test on prod copy** | Staging pe production data ke saath test karo |
| **Backward-compatible** | Schema changes jo existing code ko break na karein |
| **Expand-Contract** | Add → Migrate → Remove — zero-downtime deployment |
| **Online indexes** | Large tables pe `ALGORITHM=INPLACE` ya `CONCURRENTLY` use karo |
| **One change per migration** | Ek migration = ek logical change |
| **Descriptive names** | `V5__add_phone_to_users.sql` — clear aur readable |
| **Document migrations** | Reason, impact, rollback — sab comment mein likho |
| **Separate seed data** | Repeatable migrations mein — contexts se control karo |
| **Version control** | Git mein commit karo — audit trail maintain karo |

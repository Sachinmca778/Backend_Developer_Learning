# SQL Injection Prevention

## Status: Not Started

---

## Table of Contents

1. [SQL Injection Kya Hai?](#sql-injection-kya-hai)
2. [Attack Examples](#attack-examples)
3. [Parameterized Queries](#parameterized-queries)
4. [Spring Data JPA](#spring-data-jpa)
5. [Native Queries Carefully](#native-queries-carefully)
6. [Stored Procedures](#stored-procedures)
7. [Input Validation as Defense in Depth](#input-validation-as-defense-in-depth)
8. [ORMs Don't Magically Save You](#orms-dont-magically-save-you)
9. [DB-Side Hardening](#db-side-hardening)
10. [Detection & Testing](#detection--testing)
11. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## SQL Injection Kya Hai?

**Matlab:** User-supplied input ko **string concatenate** karke SQL banaya → attacker control gain karta hai over query.

```
User enters: ' OR '1'='1
Server builds: SELECT * FROM users WHERE name = '' OR '1'='1' AND password = '...'
                                                ↑↑↑ always true!
```

### Why Critical?

- **Auth bypass** (login with `' OR '1'='1` --)
- **Data exfiltration** (UNION SELECT credit_card FROM ...)
- **Data modification** (`'; UPDATE users SET role='admin' WHERE name='attacker'; --`)
- **Schema discovery** (information_schema queries)
- **DB privilege escalation** → sometimes RCE on host

OWASP A03 mein injection #3 — historically #1 for many years.

---

## Attack Examples

### Login Bypass

```sql
-- App code
String sql = "SELECT * FROM users WHERE username = '" + u + "' AND password = '" + p + "'";

-- Attacker:
u = admin'--
p = anything

-- Final query:
SELECT * FROM users WHERE username = 'admin'--' AND password = 'anything'
                                          ↑ rest commented
-- Returns admin row → logged in as admin!
```

### UNION-Based Extraction

```sql
search = ' UNION SELECT credit_card_number, NULL, NULL FROM payments --

SELECT name, email FROM users WHERE name LIKE '%' UNION SELECT credit_card_number, NULL, NULL FROM payments --%'
```

### Blind Boolean Injection

App returns "found" / "not found" only. Attacker probes:

```sql
search = ' AND (SELECT COUNT(*) FROM users WHERE password LIKE 'a%') > 0 --
```

→ Inferential — slow but works.

### Time-Based Blind

```sql
search = '; SELECT pg_sleep(5) --
```

→ Response delays leak data.

### Stacked Queries

```sql
'; DROP TABLE users; --
```

(Most JDBC drivers block multi-statements by default but PHP/MySQL historically allowed.)

---

## Parameterized Queries

**The #1 fix.** Driver bind values **separately** from SQL — values **never** become SQL syntax.

### Java JDBC

```java
// ❌ NEVER
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery(
    "SELECT * FROM users WHERE name = '" + userInput + "'");

// ✅ ALWAYS
PreparedStatement ps = conn.prepareStatement(
    "SELECT * FROM users WHERE name = ?");
ps.setString(1, userInput);   // safely bound
ResultSet rs = ps.executeQuery();
```

### Spring JdbcTemplate

```java
// ❌ String concat
jdbc.queryForList("SELECT * FROM orders WHERE id = " + id);

// ✅ Named or positional params
jdbc.queryForList(
    "SELECT * FROM orders WHERE id = ?",
    id);

// ✅ NamedParameterJdbcTemplate
namedJdbc.queryForList(
    "SELECT * FROM orders WHERE id = :id",
    Map.of("id", id));
```

### Python `psycopg2`

```python
# ❌
cur.execute(f"SELECT * FROM users WHERE name = '{name}'")

# ✅
cur.execute("SELECT * FROM users WHERE name = %s", (name,))
```

### Node `pg`

```javascript
// ❌
client.query(`SELECT * FROM users WHERE name = '${name}'`);

// ✅
client.query("SELECT * FROM users WHERE name = $1", [name]);
```

### Go `database/sql`

```go
// ❌
db.Query("SELECT * FROM users WHERE name = '" + name + "'")

// ✅
db.Query("SELECT * FROM users WHERE name = $1", name)
```

### Why It Works

Driver sends:
1. SQL template (parsed by DB)
2. Parameter values (treated as data, never SQL)

Even if value contains `'; DROP TABLE`, it's just a string in `name` column comparison.

---

## Spring Data JPA

JPA / Hibernate use **parameterized queries** under the hood.

### Repository Methods (Auto-Safe)

```java
public interface UserRepository extends JpaRepository<User, Long> {
    // Method name → safe parameterized query
    Optional<User> findByEmail(String email);
    
    List<User> findByCountryAndAgeGreaterThan(String country, int age);
}
```

### `@Query` with Named/Positional Params

```java
public interface UserRepository extends JpaRepository<User, Long> {
    
    // ✅ Safe — :email is parameterized
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);
    
    // ✅ Safe — positional
    @Query("SELECT u FROM User u WHERE u.email = ?1")
    Optional<User> findByEmailPos(String email);
}
```

### Specifications / Criteria API

Programmatic — also safe (parameters bound).

```java
Specification<User> spec = (root, q, cb) ->
    cb.equal(root.get("email"), email);   // safe binding
userRepo.findAll(spec);
```

---

## Native Queries Carefully

`@Query(nativeQuery = true)` is **as safe as you make it** — same parameter rules apply.

```java
// ✅ Safe — parameterized
@Query(value = "SELECT * FROM users WHERE country = :c", nativeQuery = true)
List<User> findByCountryNative(@Param("c") String country);
```

### ⚠️ String Concatenation Trap

Some folks build SQL dynamically:

```java
// ❌ DANGER — JPA/Hibernate doesn't save you here
@Query(value = "SELECT * FROM users WHERE name LIKE '%" + "#{#name}" + "%'", 
       nativeQuery = true)
// Or worse: building SQL via String.format in service layer
String sql = "SELECT * FROM " + userTable + " WHERE name = '" + name + "'";
em.createNativeQuery(sql).getResultList();
```

→ Even with ORM, **concatenation = injection**. Use parameters.

### Dynamic Sorting / Filtering — Allow-list

Sometimes you need dynamic ORDER BY (can't parameterize column names):

```java
// ❌ User-controlled column name
String sql = "SELECT * FROM users ORDER BY " + sortColumn;

// ✅ Whitelist
Set<String> ALLOWED_SORTS = Set.of("name", "created_at", "email");
if (!ALLOWED_SORTS.contains(sortColumn)) {
    throw new IllegalArgumentException();
}
String sql = "SELECT * FROM users ORDER BY " + sortColumn;  // now safe
```

Same for table names, dynamic IN-list sizes, etc. — **always allow-list**.

### Dynamic IN clause

```java
// ✅ Build placeholders dynamically, bind values
String placeholders = ids.stream().map(i -> "?").collect(Collectors.joining(","));
String sql = "SELECT * FROM users WHERE id IN (" + placeholders + ")";
PreparedStatement ps = conn.prepareStatement(sql);
for (int i = 0; i < ids.size(); i++) {
    ps.setLong(i + 1, ids.get(i));
}
```

---

## Stored Procedures

**Can** be safer (logic in DB) **but** not magically immune.

```sql
-- ❌ Vulnerable stored proc using dynamic SQL
CREATE PROCEDURE FindUser(@name VARCHAR(50))
AS
BEGIN
    EXEC('SELECT * FROM users WHERE name = ''' + @name + '''')  -- still concat!
END
```

```sql
-- ✅ Parameterized inside stored proc
CREATE PROCEDURE FindUser(@name VARCHAR(50))
AS
BEGIN
    SELECT * FROM users WHERE name = @name
END
```

Calling from Java:

```java
CallableStatement cs = conn.prepareCall("{call FindUser(?)}");
cs.setString(1, name);
cs.execute();
```

---

## Input Validation as Defense in Depth

Parameterized queries primary defense. Validation = extra layer.

### Bean Validation (Java)

```java
public class CreateOrderRequest {
    @NotBlank
    @Email
    private String email;
    
    @NotNull
    @Min(1)
    @Max(1000)
    private Integer quantity;
    
    @Pattern(regexp = "^[A-Z]{2,3}$")
    private String currency;
}

@PostMapping("/orders")
public Order create(@Valid @RequestBody CreateOrderRequest req) { ... }
```

### Allow-List > Deny-List

```java
// ❌ Trying to block bad chars (always incomplete)
if (input.contains("'") || input.contains(";") || input.contains("--")) reject;

// ✅ Define what's allowed
if (!input.matches("^[a-zA-Z0-9 ]+$")) reject;
```

### Type Coercion

```java
// User sends "abc" for ID — fail at parsing, never reaches SQL
@GetMapping("/orders/{id}")
public Order get(@PathVariable Long id) { ... }   // 400 Bad Request automatically
```

→ Strong types are free SQL injection prevention.

---

## ORMs Don't Magically Save You

### Real ways ORM still gets injection

#### 1. String concat in HQL/JPQL

```java
// ❌
em.createQuery("FROM User WHERE name = '" + name + "'").getResultList();
```

#### 2. Native queries with concat

(See above.)

#### 3. JpaSpecificationExecutor with `Expression.literal`

Be careful with raw SQL fragments.

#### 4. Repository methods that take partial SQL

Custom DAOs / Repositories doing `String + name`.

### Rule

> Wherever **user input** meets **string-built SQL/JPQL**, it's vulnerable — regardless of framework.

Always: bind parameters or strict allow-lists.

---

## DB-Side Hardening

Even if app has bug, limit blast radius.

### Least Privilege App User

```sql
-- App user — only what's needed
CREATE USER app_user WITH PASSWORD '...';
GRANT SELECT, INSERT, UPDATE ON orders, users TO app_user;
-- NO DROP, NO DDL, NO superuser
```

```sql
-- Read-only replica user
CREATE USER readonly_user WITH PASSWORD '...';
GRANT SELECT ON ALL TABLES IN SCHEMA public TO readonly_user;
```

### Disable Multi-Statement Where Possible

In MySQL JDBC: `allowMultiQueries=false` (default) → blocks `;` chained statements.

### Network ACL

App can only reach DB; DB never exposed to internet.

### Audit Logging

DB-level: log queries, especially DDL/privilege changes.

---

## Detection & Testing

### Static Analysis

- **Snyk Code**, **SonarQube**, **Semgrep** — flag string-concat SQL
- IDE inspections (IntelliJ flags raw concatenation)

### SAST Rule Examples

```yaml
# Semgrep
- pattern: |
    String $SQL = "..." + $X + "...";
    ...
    $JDBC.executeQuery($SQL);
  message: Possible SQL injection
```

### Dynamic Testing

- **OWASP ZAP** — automated scanner
- **Burp Suite** (Pro) — Active Scan + manual fuzz
- **sqlmap** — open-source injection finder
- **Pen testing** by external firm

### Common sqlmap Usage

```bash
sqlmap -u "https://example.com/api/orders?id=1" --batch
sqlmap -u "..." -p id --dbms postgresql
sqlmap -u "..." --cookie="session=abc" --dbs
```

→ Run **only** with permission against your own systems.

---

## Summary Cheat Sheet

| Defense | Priority |
|---------|----------|
| **Parameterized queries** | #1 — always |
| **JPA repos / `@Query` with `:param`** | safe by default |
| **Bean Validation `@Valid`** | type + format |
| **Allow-list dynamic columns/sort** | for cases you can't bind |
| **Native query w/ params** | OK — never concat |
| **Stored proc w/ params** | OK |
| **Least-privilege DB user** | blast radius |
| **Static analysis in CI** | catch regressions |
| **Pen test / sqlmap (auth'd)** | trust but verify |

| ❌ Never | ✅ Always |
|---------|----------|
| String concat user input into SQL | Bind parameters |
| Trust client `id`/`role` | Server-side ownership check |
| Dynamic ORDER BY from input | Allow-list of valid columns |
| Build `IN (...)` from raw input | Generate `?,?,?` then bind |
| Pull native SQL via `String.format` | `EntityManager.createNativeQuery(sql, params)` |

---

## Practice

1. Find one place in your code where SQL/JPQL is built with `+` — convert to parameterized.
2. Write a Spring Data JPA `@Query` using both `:named` and positional params.
3. Build a safe dynamic ORDER BY using an allow-list.
4. Write a JdbcTemplate query that handles `IN (?, ?, ?)` for a variable list.
5. Run sqlmap (locally) against your own test app to confirm parameterized vs vulnerable.
6. Configure two DB users in dev: one for app (limited), one for migrations (DDL).

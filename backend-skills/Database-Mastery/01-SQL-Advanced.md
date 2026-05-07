# SQL Advanced

## Status: Not Started

---

## Table of Contents

1. [Window Functions](#window-functions)
2. [Common Table Expressions (CTEs)](#common-table-expressions-ctes)
3. [Recursive CTEs](#recursive-ctes)
4. [Subquery vs JOIN Performance](#subquery-vs-join-performance)
5. [EXISTS vs IN](#exists-vs-in)
6. [HAVING vs WHERE](#having-vs-where)
7. [UPSERT (INSERT ON CONFLICT)](#upsert-insert-on-conflict)

---

## Window Functions

**Matlab:** Window functions ek "window" (group of rows) par calculation karte hain — but `GROUP BY` ki tarah rows ko collapse nahi karte. Har row apna identity rakhti hai, but uske saath aggregate/ranking values bhi milti hain.

### Difference: `GROUP BY` vs Window Function

```sql
-- GROUP BY → rows collapse ho jaati hain
SELECT department, AVG(salary)
FROM employees
GROUP BY department;
-- Output: 1 row per department

-- Window Function → har employee ki row dikhti hai + uske dept ka avg
SELECT name, department, salary,
       AVG(salary) OVER (PARTITION BY department) AS dept_avg
FROM employees;
-- Output: har employee ki row + uske dept ka avg saath mein
```

### Syntax

```sql
function_name(expression) OVER (
    [PARTITION BY column_list]   -- groups define karta hai
    [ORDER BY column_list]       -- ordering within group
    [frame_clause]               -- ROWS / RANGE
)
```

---

### 1. ROW_NUMBER()

**Matlab:** Har row ko unique sequential number deta hai (1, 2, 3, ...). Ties (tie = same value) hone par bhi alag-alag number milte hain.

```sql
SELECT name, salary,
       ROW_NUMBER() OVER (ORDER BY salary DESC) AS row_num
FROM employees;
```

**Output:**
```
name    | salary | row_num
--------|--------|--------
Rahul   | 100000 | 1
Priya   |  90000 | 2
Amit    |  90000 | 3       ← Same salary but different row_num
Sneha   |  80000 | 4
```

**Use case:** "Top 5 highest paid employees" jaisi queries.

```sql
-- Department-wise top 3 earners
SELECT * FROM (
    SELECT name, department, salary,
           ROW_NUMBER() OVER (PARTITION BY department ORDER BY salary DESC) AS rn
    FROM employees
) t WHERE rn <= 3;
```

---

### 2. RANK()

**Matlab:** Ties ko same rank deta hai, but next rank skip karta hai (gaps banti hain).

```sql
SELECT name, salary,
       RANK() OVER (ORDER BY salary DESC) AS rnk
FROM employees;
```

**Output:**
```
name    | salary | rnk
--------|--------|----
Rahul   | 100000 | 1
Priya   |  90000 | 2
Amit    |  90000 | 2     ← Same rank
Sneha   |  80000 | 4     ← Skip 3! (gap)
```

---

### 3. DENSE_RANK()

**Matlab:** Ties ko same rank, but next rank skip nahi karta (no gaps).

```sql
SELECT name, salary,
       DENSE_RANK() OVER (ORDER BY salary DESC) AS dense_rnk
FROM employees;
```

**Output:**
```
name    | salary | dense_rnk
--------|--------|----------
Rahul   | 100000 | 1
Priya   |  90000 | 2
Amit    |  90000 | 2
Sneha   |  80000 | 3     ← No gap!
```

### Comparison Table

| Function | Ties | Gap | Use Case |
|----------|------|-----|----------|
| `ROW_NUMBER` | Different numbers | No | Pagination, "Nth row" |
| `RANK` | Same rank | Yes | Olympics-style ranking |
| `DENSE_RANK` | Same rank | No | "Top N distinct values" |

---

### 4. LAG() and LEAD()

**Matlab:**
- `LAG(col, n)` → Pichli (previous) row ki value (n rows pehle)
- `LEAD(col, n)` → Aage (next) row ki value (n rows baad)

**Use case:** Day-over-day change calculate karna.

```sql
SELECT date, sales,
       LAG(sales, 1) OVER (ORDER BY date) AS yesterday_sales,
       sales - LAG(sales, 1) OVER (ORDER BY date) AS day_over_day_change
FROM daily_sales;
```

**Output:**
```
date       | sales | yesterday_sales | day_over_day_change
-----------|-------|-----------------|--------------------
2024-01-01 |  1000 | NULL            | NULL
2024-01-02 |  1200 | 1000            | 200
2024-01-03 |  1100 | 1200            | -100
2024-01-04 |  1500 | 1100            | 400
```

---

### 5. FIRST_VALUE() and LAST_VALUE()

**Matlab:** Window mein pehli ya aakhri row ki value.

```sql
SELECT name, department, salary,
       FIRST_VALUE(name) OVER (PARTITION BY department ORDER BY salary DESC) AS top_earner
FROM employees;
```

**Output:**
```
name    | department | salary | top_earner
--------|------------|--------|-----------
Rahul   | Tech       | 150000 | Rahul
Priya   | Tech       | 120000 | Rahul
Amit    | Tech       |  90000 | Rahul
Sneha   | Sales      | 100000 | Sneha
Karan   | Sales      |  80000 | Sneha
```

---

### 6. SUM() OVER (Running Total)

**Matlab:** Running total / cumulative sum.

```sql
SELECT date, sales,
       SUM(sales) OVER (ORDER BY date) AS running_total
FROM daily_sales;
```

**Output:**
```
date       | sales | running_total
-----------|-------|---------------
2024-01-01 |  1000 | 1000
2024-01-02 |  1200 | 2200
2024-01-03 |  1100 | 3300
2024-01-04 |  1500 | 4800
```

**Partition-wise running total:**
```sql
SELECT customer_id, order_date, amount,
       SUM(amount) OVER (PARTITION BY customer_id ORDER BY order_date) AS customer_total
FROM orders;
```

---

### 7. PARTITION BY

**Matlab:** Window ko groups mein todna — har group ke andar function alag se chalta hai.

```sql
-- Bina PARTITION BY → ek hi window
AVG(salary) OVER ()    -- saare employees ka avg

-- PARTITION BY ke saath → har dept ka apna window
AVG(salary) OVER (PARTITION BY department)
```

**Practical Example:** Each employee ki salary vs uski department avg.

```sql
SELECT name, department, salary,
       AVG(salary) OVER (PARTITION BY department) AS dept_avg,
       salary - AVG(salary) OVER (PARTITION BY department) AS diff_from_avg
FROM employees;
```

---

### Window Frame (ROWS BETWEEN)

**Matlab:** Window ke andar bhi ek "frame" define kar sakte ho — kitni rows consider karni hain.

```sql
-- Last 3 rows ka moving average
SELECT date, sales,
       AVG(sales) OVER (
           ORDER BY date
           ROWS BETWEEN 2 PRECEDING AND CURRENT ROW
       ) AS moving_avg_3day
FROM daily_sales;
```

**Frame options:**
- `UNBOUNDED PRECEDING` → window ke shuru se
- `N PRECEDING` → N rows pehle se
- `CURRENT ROW` → abhi wali row
- `N FOLLOWING` → N rows baad tak
- `UNBOUNDED FOLLOWING` → window ke end tak

---

## Common Table Expressions (CTEs)

**Matlab:** CTE ek "temporary named result set" hai jo ek query ke andar use hoti hai. `WITH` clause se banti hai.

### Why CTEs?

1. **Readability** → complex query break ho jaati hai chhoti pieces mein
2. **Reusability** → same subquery multiple jagah use ho sakti hai
3. **Recursion support** → recursive queries bina CTE ke nahi likh sakte

### Basic Syntax

```sql
WITH cte_name AS (
    SELECT ... FROM ...
)
SELECT ... FROM cte_name;
```

### Example: Without CTE (Mess)

```sql
SELECT name, salary
FROM employees
WHERE department_id IN (
    SELECT id FROM departments WHERE location = 'Bangalore'
)
AND salary > (
    SELECT AVG(salary) FROM employees
    WHERE department_id IN (
        SELECT id FROM departments WHERE location = 'Bangalore'
    )
);
```

### Same Query With CTE (Clean)

```sql
WITH bangalore_depts AS (
    SELECT id FROM departments WHERE location = 'Bangalore'
),
bangalore_avg_salary AS (
    SELECT AVG(salary) AS avg_sal
    FROM employees
    WHERE department_id IN (SELECT id FROM bangalore_depts)
)
SELECT name, salary
FROM employees
WHERE department_id IN (SELECT id FROM bangalore_depts)
  AND salary > (SELECT avg_sal FROM bangalore_avg_salary);
```

### Multiple CTEs (Pipeline)

```sql
WITH 
high_value_customers AS (
    SELECT customer_id FROM orders
    GROUP BY customer_id
    HAVING SUM(amount) > 100000
),
recent_orders AS (
    SELECT * FROM orders
    WHERE order_date > NOW() - INTERVAL '30 days'
)
SELECT c.name, COUNT(r.id) AS recent_order_count
FROM high_value_customers h
JOIN customers c ON c.id = h.customer_id
LEFT JOIN recent_orders r ON r.customer_id = h.customer_id
GROUP BY c.name;
```

---

## Recursive CTEs

**Matlab:** Ek CTE jo apne aap ko reference karti hai. Tree/hierarchy traverse karne ke liye perfect.

### Syntax

```sql
WITH RECURSIVE cte_name AS (
    -- Anchor (base case) → starting point
    SELECT ... FROM ...
    
    UNION ALL
    
    -- Recursive part → CTE ko khud reference karti hai
    SELECT ... FROM cte_name JOIN ... ON ...
)
SELECT * FROM cte_name;
```

### Example 1: Numbers 1 to 10

```sql
WITH RECURSIVE numbers AS (
    SELECT 1 AS n              -- Anchor
    UNION ALL
    SELECT n + 1 FROM numbers  -- Recursive
    WHERE n < 10               -- Termination condition (IMPORTANT!)
)
SELECT * FROM numbers;
-- Output: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
```

### Example 2: Employee Hierarchy (Manager Tree)

```sql
-- Table: employees(id, name, manager_id)

WITH RECURSIVE org_chart AS (
    -- Anchor: CEO (manager_id IS NULL)
    SELECT id, name, manager_id, 1 AS level, name AS path
    FROM employees
    WHERE manager_id IS NULL
    
    UNION ALL
    
    -- Recursive: har employee jiska manager already org_chart mein hai
    SELECT e.id, e.name, e.manager_id, oc.level + 1,
           oc.path || ' > ' || e.name
    FROM employees e
    JOIN org_chart oc ON e.manager_id = oc.id
)
SELECT * FROM org_chart ORDER BY level, name;
```

**Output:**
```
id | name   | manager_id | level | path
---|--------|------------|-------|--------------------------
1  | CEO    | NULL       | 1     | CEO
2  | VP1    | 1          | 2     | CEO > VP1
3  | VP2    | 1          | 2     | CEO > VP2
4  | Mgr1   | 2          | 3     | CEO > VP1 > Mgr1
5  | Dev1   | 4          | 4     | CEO > VP1 > Mgr1 > Dev1
```

### ⚠️ Always include termination condition

Bina termination ke recursive CTE infinite loop mein chali jaayegi. Postgres mein default 100 iterations cap hota hai (configurable).

---

## Subquery vs JOIN Performance

### Subquery (Nested Query)

```sql
SELECT name FROM users
WHERE id IN (SELECT user_id FROM orders WHERE amount > 1000);
```

### JOIN (Equivalent)

```sql
SELECT DISTINCT u.name 
FROM users u
JOIN orders o ON u.id = o.user_id
WHERE o.amount > 1000;
```

### Performance Comparison

**Modern DBs (Postgres, MySQL 8+) ke liye:**
- Query optimizer dono ko same execution plan mein convert kar deta hai (most cases mein)
- Performance similar hoti hai

**Lekin general rules:**

| Scenario | Better Option | Reason |
|----------|---------------|--------|
| Bahot saari matching rows | JOIN | DB hash join use kar sakta hai |
| Sirf existence check | `EXISTS` (subquery) | First match milte hi stop |
| Outer table chhoti, inner badi | Subquery + index | Index lookup fast |
| Need columns from both tables | JOIN | Subquery se columns nahi mil sakti |
| Aggregation on inner | Subquery | Cleaner |

### Real-world Tip

```sql
-- ❌ BAD: Subquery in SELECT (correlated → har row ke liye execute)
SELECT u.name,
       (SELECT COUNT(*) FROM orders WHERE user_id = u.id) AS order_count
FROM users u;
-- N+1 queries problem!

-- ✅ GOOD: JOIN with GROUP BY
SELECT u.name, COALESCE(o.cnt, 0) AS order_count
FROM users u
LEFT JOIN (
    SELECT user_id, COUNT(*) AS cnt FROM orders GROUP BY user_id
) o ON u.id = o.user_id;
```

**Hamesha `EXPLAIN ANALYZE` se verify karo.**

---

## EXISTS vs IN

### IN

```sql
SELECT * FROM users
WHERE id IN (SELECT user_id FROM orders);
```

- Pehle subquery execute hoti hai → list of `user_id`s aati hai
- Phir outer query mein match hota hai
- **NULL handling tricky:** `IN (NULL)` se unexpected results

### EXISTS

```sql
SELECT * FROM users u
WHERE EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id);
```

- Har outer row ke liye subquery check karti hai
- Pehla match milte hi `TRUE` return → fast termination
- **NULL safe**

### When to use what?

| Scenario | Use |
|----------|-----|
| Subquery returns small list | `IN` |
| Subquery returns large list / heavy | `EXISTS` |
| Subquery has NULL values | `EXISTS` (safer) |
| Existence check only | `EXISTS` |
| Negation needed | `NOT EXISTS` (NOT IN with NULLs is dangerous!) |

### ⚠️ Trap: `NOT IN` with NULLs

```sql
-- agar subquery mein koi NULL hai...
SELECT * FROM users WHERE id NOT IN (1, 2, NULL);
-- Result: EMPTY! (counter-intuitive)

-- Reason: id NOT IN (1, 2, NULL) 
--       = id != 1 AND id != 2 AND id != NULL
--       = id != 1 AND id != 2 AND UNKNOWN
--       = UNKNOWN (treated as FALSE)

-- ✅ Use NOT EXISTS instead:
SELECT * FROM users u WHERE NOT EXISTS (
    SELECT 1 FROM blocked_users b WHERE b.id = u.id
);
```

---

## HAVING vs WHERE

### WHERE

- **Filtering rows BEFORE aggregation**
- Individual rows par condition lagti hai
- Aggregate functions (COUNT, SUM, AVG) **nahi** use kar sakte

### HAVING

- **Filtering groups AFTER aggregation**
- `GROUP BY` ke baad apply hoti hai
- Aggregate functions use kar sakte hain

### Execution Order (SQL Logical Order)

```
1. FROM
2. WHERE          ← row-level filter
3. GROUP BY       ← grouping
4. HAVING         ← group-level filter
5. SELECT
6. ORDER BY
7. LIMIT
```

### Example

```sql
-- Find departments with more than 10 employees, 
-- but only consider employees with salary > 50000

SELECT department, COUNT(*) AS emp_count, AVG(salary) AS avg_sal
FROM employees
WHERE salary > 50000           -- ✅ row filter (per employee)
GROUP BY department
HAVING COUNT(*) > 10           -- ✅ group filter (per dept)
   AND AVG(salary) > 80000;    -- aggregate condition
```

### Common Mistake

```sql
-- ❌ WRONG: WHERE mein aggregate function
SELECT department, COUNT(*) FROM employees
WHERE COUNT(*) > 10
GROUP BY department;
-- ERROR: aggregate functions are not allowed in WHERE

-- ✅ CORRECT
SELECT department, COUNT(*) FROM employees
GROUP BY department
HAVING COUNT(*) > 10;
```

### Performance Tip

`WHERE` use karo jab possible ho — kyunki ye early filtering karta hai (kam rows aggregation mein jaati hain). `HAVING` sirf aggregate conditions ke liye.

---

## UPSERT (INSERT ON CONFLICT)

**Matlab:** "INSERT or UPDATE" — agar row exist karti hai toh update karo, warna insert karo. Single atomic operation.

### Without UPSERT (Race Condition Risk)

```sql
-- ❌ Race condition!
SELECT id FROM users WHERE email = 'a@b.com';
-- if exists → UPDATE
-- else      → INSERT
-- Concurrent request beech mein insert kar sakti hai → duplicate or error
```

### PostgreSQL `INSERT ON CONFLICT`

```sql
INSERT INTO users (email, name, login_count)
VALUES ('a@b.com', 'Rahul', 1)
ON CONFLICT (email)                          -- conflict on unique column
DO UPDATE SET 
    login_count = users.login_count + 1,
    name = EXCLUDED.name,                    -- new values from INSERT row
    updated_at = NOW();
```

### `EXCLUDED` Pseudo-Table

`EXCLUDED` mein wo values hoti hain jo INSERT karne aayi thi (conflict ki wajah se rejected). `users` (target table) mein purani values hoti hain.

```sql
-- Login counter increment
ON CONFLICT (email) DO UPDATE
SET login_count = users.login_count + 1;     -- purana count + 1

-- Latest data se overwrite
ON CONFLICT (email) DO UPDATE
SET name = EXCLUDED.name, updated_at = NOW();
```

### `DO NOTHING` (Skip if Exists)

```sql
INSERT INTO users (email, name)
VALUES ('a@b.com', 'Rahul')
ON CONFLICT (email) DO NOTHING;
-- Agar email already hai → silently skip (no error)
```

### Conditional UPDATE

```sql
-- Sirf tab update karo jab new login_count bada ho
INSERT INTO user_stats (user_id, last_login, login_count)
VALUES (1, NOW(), 1)
ON CONFLICT (user_id) DO UPDATE
SET last_login = EXCLUDED.last_login,
    login_count = user_stats.login_count + 1
WHERE EXCLUDED.last_login > user_stats.last_login;
```

### MySQL `INSERT ... ON DUPLICATE KEY UPDATE`

```sql
INSERT INTO users (email, name, login_count)
VALUES ('a@b.com', 'Rahul', 1)
ON DUPLICATE KEY UPDATE
    login_count = login_count + 1,
    name = VALUES(name);   -- VALUES() = INSERT ki value (like EXCLUDED in PG)
```

### Bulk UPSERT

```sql
INSERT INTO products (sku, name, price)
VALUES 
    ('A001', 'Apple', 100),
    ('A002', 'Banana', 50),
    ('A003', 'Cherry', 200)
ON CONFLICT (sku) DO UPDATE
SET name = EXCLUDED.name,
    price = EXCLUDED.price,
    updated_at = NOW();
```

### Real-world Use Cases

1. **Counter increments** (page views, login counts)
2. **Cache table updates** (latest value win)
3. **Sync from external source** (dedupe by external_id)
4. **Idempotent APIs** (same request → same result)

---

## Summary Cheat Sheet

| Topic | Quick Note |
|-------|-----------|
| `ROW_NUMBER` | Unique sequential, ties → different |
| `RANK` | Ties → same, gaps yes |
| `DENSE_RANK` | Ties → same, no gaps |
| `LAG/LEAD` | Previous/next row value |
| `FIRST_VALUE/LAST_VALUE` | First/last in window |
| `SUM() OVER` | Running total / cumulative |
| `PARTITION BY` | Window ko groups mein todna |
| CTE | Temporary named result, readable |
| Recursive CTE | Tree/hierarchy traversal |
| `EXISTS` | Existence check, NULL safe, fast on first match |
| `IN` | Small static list, careful with NULLs |
| `WHERE` | Row-level filter (before aggregation) |
| `HAVING` | Group-level filter (after aggregation) |
| `INSERT ON CONFLICT` | Atomic upsert, prevents race condition |

---

## Practice Problems

1. Top 3 highest-paid employees per department.
2. Running total of monthly sales for last 12 months.
3. Day-over-day percentage change in revenue.
4. Hierarchical query: print full reporting chain for a given employee.
5. Find users who have NEVER placed an order (use `NOT EXISTS`).
6. Departments where average salary > company-wide average (`HAVING` + subquery).
7. Atomic counter: increment `views` column for `article_id`, insert if not exists.

# Pagination Patterns

## Status: Not Started

---

## Table of Contents

1. [Why Pagination?](#why-pagination)
2. [Offset-Based Pagination](#offset-based-pagination)
3. [Cursor-Based / Keyset Pagination](#cursor-based--keyset-pagination)
4. [Offset vs Cursor Comparison](#offset-vs-cursor-comparison)
5. [Page Metadata](#page-metadata)
6. [GraphQL Connections Spec](#graphql-connections-spec)

---

## Why Pagination?

**Bina pagination ke problems:**

- **Database load:** 10,000 rows ek saath fetch karna = slow query + high memory usage
- **Network:** Large response = slow transfer
- **Frontend:** Browser crash ho sakta hai itna data render karte waqt
- **UX:** User ko scroll-scroll karna padta hai

**Solution:** Data ko chunks (pages) mein fetch karo.

---

## Offset-Based Pagination

**Matlab:** `LIMIT` aur `OFFSET` use karke data fetch karna. Most common aur simplest approach.

### SQL Query
```sql
-- Page 1 (first 10 items)
SELECT * FROM users ORDER BY id LIMIT 10 OFFSET 0;

-- Page 2 (next 10 items)
SELECT * FROM users ORDER BY id LIMIT 10 OFFSET 10;

-- Page 3
SELECT * FROM users ORDER BY id LIMIT 10 OFFSET 20;

-- General formula: OFFSET = (page_number - 1) * page_size
```

### Spring Data JPA
```java
public interface UserRepository extends JpaRepository<User, Long> {
    // Built-in pagination support
    Page<User> findAll(Pageable pageable);
    
    // Custom query with pagination
    @Query("SELECT u FROM User u WHERE u.status = :status")
    Page<User> findByStatus(@Param("status") String status, Pageable pageable);
}
```

### Controller Usage
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserRepository userRepository;
    
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @GetMapping
    public Page<UserResponse> getUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return userRepository.findAll(pageable).map(this::toResponse);
    }
}
```

### Response Format
```json
{
  "content": [
    { "id": 1, "name": "User 1" },
    { "id": 2, "name": "User 2" },
    { "id": 3, "name": "User 3" }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 150,
  "totalPages": 15,
  "first": true,
  "last": false,
  "numberOfElements": 10
}
```

### Problem: Performance at Scale

**Scenario:** 100,000 users hain, tum page 10,000 fetch kar rahe ho.

```sql
SELECT * FROM users ORDER BY id LIMIT 10 OFFSET 99990;
```

**Kya hota hai internally:**
1. Database pehle 100,000 rows sort karta hai
2. Phir 99,990 rows skip karta hai
3. Finally 10 rows return karta hai

**Result:** Query bahut slow ho jaata hai - **O(N)** operation.

### Benchmark (Approximate)

| Page | Offset | Query Time |
|------|--------|------------|
| 1 | 0 | 5ms |
| 100 | 990 | 10ms |
| 1,000 | 9,990 | 50ms |
| 10,000 | 99,990 | 500ms |
| 100,000 | 999,990 | 5000ms (5 seconds!) |

**Conclusion:** Offset-based pagination small-medium data ke liye theek hai, lekin large scale pe slow ho jaata hai.

---

## Cursor-Based / Keyset Pagination

**Matlab:** Last fetched item ka identifier (cursor) use karke next batch fetch karna.

### SQL Query
```sql
-- Page 1 (first 10 items)
SELECT * FROM users ORDER BY id LIMIT 11;

-- Cursor = last item's id (maano 10 mila)

-- Page 2 (next 10 items, cursor = 10)
SELECT * FROM users WHERE id > 10 ORDER BY id LIMIT 11;

-- Cursor = 20

-- Page 3 (cursor = 20)
SELECT * FROM users WHERE id > 20 ORDER BY id LIMIT 11;
```

**Why `LIMIT 11` instead of `10`?**
11th item check karne ke liye - agar 11th item hai toh `hasNext = true`, nahi toh `hasNext = false`.

### Index Usage

```sql
-- Agar `id` pe PRIMARY KEY index hai (default hai)
-- Yeh query bahut fast hogi: O(log N) instead of O(N)

EXPLAIN SELECT * FROM users WHERE id > 1000 ORDER BY id LIMIT 11;
-- Index Scan using users_pkey (fast!)
```

### Spring Data JPA Implementation

```java
public interface UserRepository extends JpaRepository<User, Long> {
    
    @Query("SELECT u FROM User u WHERE u.id > :cursor ORDER BY u.id ASC")
    List<User> findNextPage(@Param("cursor") Long cursor, Pageable pageable);
}
```

### Controller
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserRepository userRepository;
    
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @GetMapping
    public CursorPageResponse<UserResponse> getUsers(
        @RequestParam(required = false) Long cursor,  // Null = first page
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(0, size + 1, Sort.by("id").ascending());
        
        List<User> users;
        if (cursor == null) {
            users = userRepository.findAll(pageable).getContent();
        } else {
            users = userRepository.findNextPage(cursor, pageable);
        }
        
        boolean hasNext = users.size() > size;
        if (hasNext) {
            users = users.subList(0, size);  // Extra item hata do
        }
        
        String nextCursor = hasNext ? users.get(users.size() - 1).getId().toString() : null;
        
        return new CursorPageResponse<>(
            users.stream().map(this::toResponse).toList(),
            nextCursor,
            hasNext
        );
    }
}
```

### Response Format
```json
{
  "data": [
    { "id": 1, "name": "User 1" },
    { "id": 2, "name": "User 2" },
    { "id": 3, "name": "User 3" }
  ],
  "nextCursor": "3",
  "hasNext": true
}
```

### Compound Cursor (Multiple Sort Columns)

Agar sort multiple columns pe hai toh cursor bhi compound hoga.

```sql
-- Sort: created_at DESC, id DESC
SELECT * FROM posts 
WHERE (created_at, id) < ('2024-01-01', 100)
ORDER BY created_at DESC, id DESC
LIMIT 11;
```

**Cursor format:** `cursor = "2024-01-01_100"` (encode karke bhejo)

### Encoding Cursor (Base64)

**Security:** Direct IDs expose nahi karne - cursor ko encode karo.

```java
// Cursor encode karna
String cursor = Base64.getEncoder().encodeToString("100".getBytes());
// Result: "MTAw"

// Cursor decode karna
String decoded = new String(Base64.getDecoder().decode("MTAw"));
// Result: "100"
```

**Response:**
```json
{
  "data": [...],
  "nextCursor": "MTAw",
  "hasNext": true
}
```

### Benchmark (Same 100,000 rows)

| Page | Cursor Value | Query Time |
|------|-------------|------------|
| 1 | N/A | 5ms |
| 100 | 990 | 5ms |
| 1,000 | 9,990 | 5ms |
| 10,000 | 99,990 | 5ms |
| 100,000 | 999,990 | 5ms |

**Conclusion:** Cursor-based pagination **consistent performance** deta hai chahe kitna bhi bada dataset ho.

---

## Offset vs Cursor Comparison

| Feature | Offset-Based | Cursor-Based |
|---------|-------------|--------------|
| **Syntax** | `LIMIT 10 OFFSET 20` | `WHERE id > cursor LIMIT 10` |
| **Performance** | Slow at scale (O(N)) | Always fast (O(log N)) |
| **Total Count** | ✅ Easy (`COUNT(*)`) | ❌ Extra query chahiye |
| **Page Numbers** | ✅ Easy (Page 1, 2, 3...) | ❌ Nahi (sirf next/prev) |
| **Data Changes** | ⚠️ Inconsistent (naya item add/delete ho toh skip/duplicate) | ✅ Consistent |
| **Complexity** | Simple | Thoda complex |
| **Best For** | Small datasets, numbered pages | Large datasets, infinite scroll |

### Data Change Problem (Offset)

**Scenario:** User page 2 dekh raha hai. Beech mein koi naya item insert ho gaya.

```
Initial: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]  ← Page 1
         [11, 12, 13, 14, 15, ...]        ← Page 2 (expected)

New item inserted at position 3:
[1, 2, NEW, 3, 4, 5, 6, 7, 8, 9]          ← Page 1 (shifted)
[10, 11, 12, 13, 14, ...]                 ← Page 2 (10 duplicate aa gaya!)
```

**Cursor-based mein yeh problem nahi:**
```
Page 1: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]  ← cursor = 10
Page 2: WHERE id > 10 → [11, 12, 13, ...]  ← NEW item kahi bhi ho, affect nahi karega
```

---

## Page Metadata

**Metadata = Pagination ke baare mein extra information.**

### Common Metadata Fields

| Field | Type | Description |
|-------|------|-------------|
| `totalElements` | Long | Total items count (expensive query!) |
| `totalPages` | Integer | Total pages count |
| `currentPage` | Integer | Current page number |
| `pageSize` | Integer | Items per page |
| `hasNext` | Boolean | Next page hai ya nahi |
| `hasPrevious` | Boolean | Previous page hai ya nahi |
| `nextCursor` | String | Next page ka cursor |
| `previousCursor` | String | Previous page ka cursor |

### Offset-Based Metadata
```json
{
  "content": [...],
  "page": {
    "size": 10,
    "number": 0,
    "totalElements": 150,
    "totalPages": 15
  },
  "first": true,
  "last": false
}
```

### Cursor-Based Metadata
```json
{
  "data": [...],
  "pageInfo": {
    "hasNextPage": true,
    "hasPreviousPage": false,
    "startCursor": "MQ==",
    "endCursor": "MTA="
  },
  "nextCursor": "MTA="
}
```

### Total Count - To Fetch or Not to Fetch?

**`COUNT(*)` query expensive hoti hai** (large tables pe).

```java
// Option 1: Always fetch total (accurate but slow)
Page<User> page = userRepository.findAll(pageable);
// SELECT COUNT(*) FROM users  ← Slow for 1M+ rows

// Option 2: Don't fetch total (fast, no total count)
Slice<User> slice = userRepository.findAll(pageable).map(...);
// Sirf current page + hasNext info (no COUNT query)
```

### Slice vs Page (Spring Data)

| Feature | `Page<T>` | `Slice<T>` |
|---------|-----------|------------|
| **Total Count** | ✅ Hai (`COUNT` query) | ❌ Nahi |
| **Performance** | Slow (extra query) | Fast |
| **hasNext** | ✅ | ✅ |
| **totalPages** | ✅ | ❌ |
| **Best For** | Numbered pagination | Infinite scroll / "Load more" |

```java
// Page - total count chahiye (paginated UI)
@GetMapping("/users")
public Page<User> getUsers(Pageable pageable) {
    return userRepository.findAll(pageable);
}

// Slice - total count nahi chahiye (infinite scroll)
@GetMapping("/users/infinite")
public Slice<User> getUsersInfinite(Pageable pageable) {
    return userRepository.findAll(pageable);
}
```

---

## GraphQL Connections Spec

**GraphQL Connections** Relay specification hai - cursor-based pagination ka standard format.

### Why Connections Spec?
- Consistent pagination API across different GraphQL implementations
- Cursor-based pagination (fast at scale)
- Built-in metadata

### Schema Definition
```graphql
type Query {
  users(first: Int, after: String): UserConnection!
  posts(first: Int, after: String, last: Int, before: String): PostConnection!
}

type UserConnection {
  edges: [UserEdge!]!
  pageInfo: PageInfo!
  totalCount: Int
}

type UserEdge {
  node: User!
  cursor: String!
}

type PageInfo {
  hasNextPage: Boolean!
  hasPreviousPage: Boolean!
  startCursor: String
  endCursor: String
}

type User {
  id: ID!
  name: String!
  email: String!
}
```

### GraphQL Query
```graphql
query {
  users(first: 10, after: "cursor123") {
    totalCount
    pageInfo {
      hasNextPage
      hasPreviousPage
      startCursor
      endCursor
    }
    edges {
      cursor
      node {
        id
        name
        email
      }
    }
  }
}
```

### Response
```json
{
  "data": {
    "users": {
      "totalCount": 150,
      "pageInfo": {
        "hasNextPage": true,
        "hasPreviousPage": true,
        "startCursor": "Y3Vyc29yLTEw",
        "endCursor": "Y3Vyc29yLTIw"
      },
      "edges": [
        {
          "cursor": "Y3Vyc29yLTEw",
          "node": {
            "id": "10",
            "name": "User 10",
            "email": "user10@example.com"
          }
        },
        {
          "cursor": "Y3Vyc29yLTEx",
          "node": {
            "id": "11",
            "name": "User 11",
            "email": "user11@example.com"
          }
        }
      ]
    }
  }
}
```

### Key Concepts

| Term | Meaning |
|------|---------|
| **Connection** | Paginated collection (UserConnection, PostConnection) |
| **Edge** | Ek item + uska cursor |
| **Node** | Actual data (User, Post) |
| **cursor** | Opaque string - isse next/prev page fetch karte hain |
| **pageInfo** | Pagination metadata |

### Arguments

| Argument | Direction | Description |
|----------|-----------|-------------|
| `first: Int` | Forward | Next N items |
| `after: String` | Forward | Cursor ke baad |
| `last: Int` | Backward | Previous N items |
| `before: String` | Backward | Cursor se pehle |

### Spring GraphQL Implementation

```java
@Controller
public class UserResolver {
    
    private final UserRepository userRepository;
    
    public UserResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @QueryMapping
    public Connection<User> users(
        @Argument int first,
        @Argument String after
    ) {
        Long cursor = after != null ? decodeCursor(after) : null;
        
        List<User> users = userRepository.findNextPage(cursor, first + 1);
        
        boolean hasNext = users.size() > first;
        if (hasNext) {
            users = users.subList(0, first);
        }
        
        List<Edge<User>> edges = IntStream.range(0, users.size())
            .mapToObj(i -> new Edge<>(
                encodeCursor(users.get(i).getId()),
                users.get(i)
            ))
            .toList();
        
        String startCursor = edges.isEmpty() ? null : edges.get(0).cursor();
        String endCursor = edges.isEmpty() ? null : edges.get(edges.size() - 1).cursor();
        
        PageInfo pageInfo = new PageInfo(
            hasNext,
            after != null,  // hasPrevious (simplified)
            startCursor,
            endCursor
        );
        
        return new Connection<>(edges, pageInfo);
    }
    
    private String encodeCursor(Long id) {
        return Base64.getEncoder().encodeToString(("cursor-" + id).getBytes());
    }
    
    private Long decodeCursor(String encoded) {
        String decoded = new String(Base64.getDecoder().decode(encoded));
        return Long.parseLong(decoded.replace("cursor-", ""));
    }
}
```

### Simplified Version (Without Edges)

Agar edges/cursors ki zarurat nahi hai toh simpler response bhi use kar sakte ho:

```graphql
type Query {
  users(offset: Int, limit: Int): UserPage!
}

type UserPage {
  content: [User!]!
  hasNext: Boolean!
  nextOffset: Int
}
```

```graphql
query {
  users(offset: 0, limit: 10) {
    content {
      id
      name
    }
    hasNext
    nextOffset
  }
}
```

---

## Choosing the Right Pattern

| Scenario | Recommended Pattern |
|----------|---------------------|
| **Simple admin panel** | Offset-based (page numbers chahiye) |
| **Infinite scroll** | Cursor-based / Slice |
| **Large dataset (100K+ rows)** | Cursor-based |
| **REST API** | Offset or Cursor (depends on scale) |
| **GraphQL API** | Connections spec (cursor-based) |
| **"Load More" button** | Cursor-based (`Slice` in Spring) |
| **SEO-friendly URLs** | Offset-based (`/page/1`, `/page/2`) |

---

## Quick Reference

```sql
-- Offset-Based
SELECT * FROM users ORDER BY id LIMIT 10 OFFSET 20;

-- Cursor-Based
SELECT * FROM users WHERE id > 100 ORDER BY id LIMIT 11;
```

```java
// Spring Data
Page<User> page = repo.findAll(PageRequest.of(0, 10));     // With count
Slice<User> slice = repo.findAll(PageRequest.of(0, 10));   // Without count
```

```graphql
# GraphQL Connections
query {
  users(first: 10, after: "cursor") {
    edges { cursor, node { id, name } }
    pageInfo { hasNextPage, endCursor }
  }
}
```

---

## Summary

| Pattern | Best For | Performance |
|---------|----------|-------------|
| **Offset-Based** | Small datasets, page numbers | Slow at scale |
| **Cursor-Based** | Large datasets, infinite scroll | Always fast |
| **GraphQL Connections** | GraphQL APIs | Always fast |

| Metadata | Use When |
|----------|----------|
| **`Page<T>`** | Total count chahiye (paginated UI) |
| **`Slice<T>`** | Sirf "next" chahiye (infinite scroll) |
| **Connections** | GraphQL standard format |

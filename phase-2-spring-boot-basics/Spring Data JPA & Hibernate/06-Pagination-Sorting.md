# Pagination & Sorting

## Status: Not Started

---

## Table of Contents

1. [Pageable Interface](#pageable-interface)
2. [PageRequest.of(page, size, Sort)](#pagerequestofpage-size-sort)
3. [Page<T> vs Slice<T>](#paget-vs-slicet)
4. [Sort.by](#sortby)
5. [Sorting Directions](#sorting-directions)
6. [Custom Pagination Response Wrapper](#custom-pagination-response-wrapper)
7. [findAll(Pageable)](#findallpageable)

---

## Pageable Interface

**Matlab:** Pagination request ko represent karta hai — page number, size, sorting info contain karta hai.

### Key Interfaces

```
Pageable (interface)
    ↑
PageRequest (implementation)
```

### Usage in Repository

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // Pageable accept karne wala method
    Page<User> findAll(Pageable pageable);

    // Custom query with pagination
    @Query("SELECT u FROM User u WHERE u.active = true")
    Page<User> findActiveUsers(Pageable pageable);

    // Custom query with pagination + parameters
    @Query("SELECT u FROM User u WHERE u.name LIKE %:keyword%")
    Page<User> searchByName(@Param("keyword") String keyword, Pageable pageable);
}
```

### Usage in Service

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Page<User> getUsers(int page, int size, String sortBy, String direction) {
        // Pageable create karo
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return userRepository.findAll(pageable);
    }
}
```

---

## PageRequest.of(page, size, Sort)

**Matlab:** Pageable ka concrete implementation — page number, size, aur sorting info set karta hai.

### Basic Usage

```java
// Page 0, 10 items per page
Pageable pageable = PageRequest.of(0, 10);

// With sorting
Pageable pageable = PageRequest.of(0, 10, Sort.by("name"));

// With multiple sort fields
Pageable pageable = PageRequest.of(0, 10, Sort.by("name", "createdAt"));

// With direction
Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

// With multiple sort fields and directions
Pageable pageable = PageRequest.of(0, 10,
    Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("name"))
);
```

### Page Number is Zero-Based

```java
PageRequest.of(0, 10);  // First page (items 0-9)
PageRequest.of(1, 10);  // Second page (items 10-19)
PageRequest.of(2, 10);  // Third page (items 20-29)
```

### Controller Level

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    // Simple pagination
    @GetMapping
    public Page<User> getUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return userRepository.findAll(PageRequest.of(page, size));
    }

    // With sorting
    @GetMapping("/sorted")
    public Page<User> getUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "ASC") String direction
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        return userRepository.findAll(PageRequest.of(page, size, sort));
    }
}
```

---

## Page<T> vs Slice<T>

### Page<T>

**Total count jaanta hai** — metadata ke saath aata hai.

```java
Page<User> page = userRepository.findAll(PageRequest.of(0, 10));

// Metadata
page.getTotalElements();     // Total records in DB (e.g., 150)
page.getTotalPages();        // Total pages (e.g., 15)
page.getNumber();            // Current page number (0)
page.getSize();              // Page size (10)
page.getNumberOfElements();  // Elements in current page (10)
page.hasContent();           // Has content? (true)
page.isFirst();              // First page? (true)
page.isLast();               // Last page? (false)

// Content
List<User> users = page.getContent();

// Navigation
page.hasNext();              // Next page exists? (true)
page.hasPrevious();          // Previous page exists? (false)
page.nextPageable();         // Next Pageable object
page.previousPageable();     // Previous Pageable object
```

**Performance Impact:** `Page<T>` ek extra `COUNT(*)` query fire karta hai — large tables pe slow ho sakta hai.

### Slice<T>

**Total count nahi jaanta** — sirf "next page hai ya nahi" pata hai.

```java
Slice<User> slice = userRepository.findAll(PageRequest.of(0, 10));

// Limited metadata
slice.hasContent();          // Has content? (true)
slice.isFirst();             // First page? (true)
slice.hasNext();             // Next page exists? (true)

// Content
List<User> users = slice.getContent();

// Navigation
slice.nextPageable();        // Next Pageable object
slice.previousPageable();    // Previous Pageable object

// ❌ Not available in Slice
// slice.getTotalElements();  // NOT available
// slice.getTotalPages();     // NOT available
```

**Performance Benefit:** No `COUNT(*)` query — faster for large datasets.

### Page vs Slice Comparison

| Feature | Page<T> | Slice<T> |
|---------|---------|----------|
| **Total Count** | ✅ `getTotalElements()` | ❌ Not available |
| **Total Pages** | ✅ `getTotalPages()` | ❌ Not available |
| **Has Next** | ✅ `hasNext()` | ✅ `hasNext()` |
| **Has Previous** | ✅ `hasPrevious()` | ✅ `hasPrevious()` |
| **Extra Query** | COUNT(*) fire karta hai | No extra query |
| **Performance** | Slower (count query) | Faster |
| **Use When** | Pagination UI (page numbers) | Infinite scroll / "Load More" |

---

## Sort.by

**Matlab:** Sorting criteria define karta hai.

### Basic Usage

```java
// Single field ascending
Sort sort = Sort.by("name");

// Single field with direction
Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

// Multiple fields
Sort sort = Sort.by("name", "createdAt");

// Multiple fields with directions
Sort sort = Sort.by(
    Sort.Order.desc("createdAt"),
    Sort.Order.asc("name")
);

// Null handling
Sort sort = Sort.by(Sort.Direction.DESC, "score")
    .nullsLast();  // NULL values last mein aayenge

// Ignore case
Sort sort = Sort.by("name").ignoreCase();
```

### Usage in Repository

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // With Sort
    List<User> findByActiveTrue(Sort sort);

    // With Pageable (includes Sort)
    Page<User> findByCity(String city, Pageable pageable);
}

// Usage
List<User> users = userRepository.findByActiveTrue(
    Sort.by(Sort.Direction.DESC, "createdAt")
);
```

---

## Sorting Directions

```java
// Ascending (A → Z, 0 → 9)
Sort.by(Sort.Direction.ASC, "name")
Sort.by("name").ascending()

// Descending (Z → A, 9 → 0)
Sort.by(Sort.Direction.DESC, "createdAt")
Sort.by("createdAt").descending()

// Multiple sort criteria
Sort sort = Sort.by(
    Sort.Order.asc("lastName"),
    Sort.Order.desc("createdAt")
);

// Dynamic direction from controller
@GetMapping("/users")
public Page<User> getUsers(
    @RequestParam(defaultValue = "ASC") String direction,
    @RequestParam(defaultValue = "name") String sortBy
) {
    Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
    return userRepository.findAll(PageRequest.of(0, 10, sort));
}
```

### Null Handling

```java
// NULL values last mein
Sort.by(Sort.Direction.ASC, "name").nullsLast();

// NULL values first mein
Sort.by(Sort.Direction.DESC, "score").nullsFirst();
```

---

## Custom Pagination Response Wrapper

**Matlab:** API response mein pagination metadata include karna — client ko navigation info dene ke liye.

### Response DTO

```java
public class PaginatedResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean hasNext;
    private boolean hasPrevious;

    // Constructor
    public PaginatedResponse(Page<T> page) {
        this.content = page.getContent();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.first = page.isFirst();
        this.last = page.isLast();
        this.hasNext = page.hasNext();
        this.hasPrevious = page.hasPrevious();
    }

    // Getters
    public List<T> getContent() { return content; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
    public boolean isFirst() { return first; }
    public boolean isLast() { return last; }
    public boolean isHasNext() { return hasNext; }
    public boolean isHasPrevious() { return hasPrevious; }
}
```

### Usage in Controller

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    public PaginatedResponse<UserResponse> getUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "ASC") String direction
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> userPage = userRepository.findAll(pageable);

        // Entity → DTO mapping
        Page<UserResponse> responsePage = userPage.map(this::toResponse);

        return new PaginatedResponse<>(responsePage);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
```

### Response JSON

```json
{
  "content": [
    { "id": 1, "name": "Sachin", "email": "sachin@example.com" },
    { "id": 2, "name": "Rahul", "email": "rahul@example.com" }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 45,
  "totalPages": 5,
  "first": true,
  "last": false,
  "hasNext": true,
  "hasPrevious": false
}
```

---

## findAll(Pageable)

**Matlab:** JpaRepository ka built-in method — pagination aur sorting ke saath sab entities fetch karta hai.

### Basic Usage

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    public Page<User> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
}
```

**Request:** `GET /api/users?page=0&size=10&sort=name,asc`

Spring automatically request parameters se `Pageable` object bana deta hai!

### Default Pageable Configuration

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // Default pagination settings
        // page=0, size=20, sort=id,asc
    }
}
```

**Default parameters:**
- `page` — default: 0
- `size` — default: 20
- `sort` — default: none

### Custom Query with Pageable

```java
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.name LIKE %:keyword%")
    Page<User> searchByName(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.active = true")
    Page<User> findActiveUsers(Pageable pageable);

    // Multiple parameters + Pageable (always last parameter)
    @Query("SELECT u FROM User u WHERE u.city = :city AND u.age >= :minAge")
    Page<User> findByCityAndMinAge(@Param("city") String city,
                                   @Param("minAge") int minAge,
                                   Pageable pageable);
}
```

### Controller with Multiple Filters + Pagination

```java
@GetMapping("/search")
public PaginatedResponse<UserResponse> searchUsers(
    @RequestParam(required = false) String name,
    @RequestParam(required = false) String city,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "id") String sortBy,
    @RequestParam(defaultValue = "ASC") String direction
) {
    Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
    Pageable pageable = PageRequest.of(page, size, sort);

    Page<User> userPage;

    if (name != null) {
        userPage = userRepository.findByNameContaining(name, pageable);
    } else if (city != null) {
        userPage = userRepository.findByCity(city, pageable);
    } else {
        userPage = userRepository.findAll(pageable);
    }

    Page<UserResponse> responsePage = userPage.map(this::toResponse);
    return new PaginatedResponse<>(responsePage);
}
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Pageable** | Pagination + sorting info ka interface |
| **PageRequest** | Pageable implementation — `PageRequest.of(page, size, sort)` |
| **Page<T>** | Total count jaanta hai — extra COUNT(*) query |
| **Slice<T>** | Total count nahi jaanta — faster, no count query |
| **Sort.by** | Sorting criteria define karo |
| **Sorting Directions** | ASC / DESC, nullsFirst / nullsLast |
| **Page Number** | Zero-based — `PageRequest.of(0, 10)` = first page |
| **PaginatedResponse** | Custom wrapper — client ko metadata dene ke liye |
| **findAll(Pageable)** | JpaRepository built-in method — pagination ke saath sab fetch |

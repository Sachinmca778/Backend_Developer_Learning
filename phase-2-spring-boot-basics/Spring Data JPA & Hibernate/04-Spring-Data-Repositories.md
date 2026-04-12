# Spring Data Repositories

## Status: Not Started

---

## Table of Contents

1. [Repository Hierarchy](#repository-hierarchy)
2. [CrudRepository](#crudrepository)
3. [PagingAndSortingRepository](#pagingandsortingrepository)
4. [JpaRepository](#jparepository)
5. [Method Name Query Derivation](#method-name-query-derivation)
6. [@Query](#query)
7. [@Modifying](#modifying)

---

## Repository Hierarchy

Spring Data repositories ka ek hierarchy hai — har level pe features badhte jaate hain.

```
Repository (Marker Interface)
    ↑
CrudRepository<T, ID>
    ↑
PagingAndSortingRepository<T, ID>
    ↑
JpaRepository<T, ID>  ← Most commonly used
```

| Interface | Features |
|-----------|----------|
| **Repository** | Marker interface — koi method nahi |
| **CrudRepository** | Basic CRUD operations |
| **PagingAndSortingRepository** | Pagination + Sorting |
| **JpaRepository** | CRUD + Pagination + JPA-specific methods |

---

## CrudRepository

**Matlab:** Basic CRUD (Create, Read, Update, Delete) operations provide karta hai.

```java
public interface CrudRepository<T, ID> extends Repository<T, ID> {
    <S extends T> S save(S entity);              // Create / Update
    Optional<T> findById(ID id);                 // Read by ID
    boolean existsById(ID id);                   // Check exists
    Iterable<T> findAll();                       // Read all
    long count();                                // Count all
    void deleteById(ID id);                      // Delete by ID
    void delete(T entity);                       // Delete entity
    void deleteAllById(Iterable<? extends ID> ids); // Delete by IDs
    void deleteAll();                            // Delete all
}
```

### Usage

```java
public interface UserRepository extends CrudRepository<User, Long> {
    // Methods already available (no need to implement)
}

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User createUser(User user) {
        return userRepository.save(user);  // INSERT
    }

    public User getUser(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        userRepository.findAll().forEach(users::add);
        return users;
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
```

---

## PagingAndSortingRepository

**Matlab:** CrudRepository + pagination aur sorting support.

```java
public interface PagingAndSortingRepository<T, ID> extends CrudRepository<T, ID> {
    Iterable<T> findAll(Sort sort);         // Sorted results
    Page<T> findAll(Pageable pageable);     // Paginated results
}
```

### Usage

```java
public interface UserRepository extends PagingAndSortingRepository<User, Long> {
}

// Sorting
List<User> sortedUsers = userRepository.findAll(
    Sort.by(Sort.Direction.DESC, "createdAt")
);

// Pagination
Page<User> page = userRepository.findAll(
    PageRequest.of(0, 10)  // Page 0, 10 items per page
);

// Pagination + Sorting
Page<User> page = userRepository.findAll(
    PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
);
```

---

## JpaRepository

**Matlab:** Most commonly used repository interface. CrudRepository + PagingAndSortingRepository + JPA-specific methods.

```java
public interface JpaRepository<T, ID> extends PagingAndSortingRepository<T, ID> {
    List<T> findAll();                          // Returns List (not Iterable)
    List<T> findAll(Sort sort);                 // Sorted List
    List<T> findAllById(Iterable<ID> ids);      // Find by multiple IDs
    void flush();                               // Flush pending changes
    <S extends T> S saveAndFlush(S entity);     // Save + immediate flush
    void deleteInBatch(Iterable<T> entities);   // Batch delete
    void deleteAllInBatch();                    // Delete all in batch
    void deleteAllByIdInBatch(Iterable<ID> ids);// Delete by IDs in batch
    T getOne(ID id);                            // Get reference (lazy)
    T getById(ID id);                           // Get by ID (Spring Boot 2.5+)
}
```

### Usage

```java
public interface UserRepository extends JpaRepository<User, Long> {
    // All methods already available!
}

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // Find by multiple IDs
    public List<User> getUsersByIds(List<Long> ids) {
        return userRepository.findAllById(ids);
    }

    // Save and flush immediately
    public User saveAndFlush(User user) {
        return userRepository.saveAndFlush(user);
    }

    // Batch delete
    public void deleteUsers(List<User> users) {
        userRepository.deleteInBatch(users);
    }
}
```

---

## Method Name Query Derivation

**Matlab:** Method name dekh ke Spring Data automatically query generate kar deta hai. No `@Query` needed!

### Naming Convention

```
findBy + [Field] + [Keyword] + [Field] + ...

Example: findByNameAndEmail → WHERE name = ? AND email = ?
```

### Basic Queries

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // SELECT * FROM user WHERE name = ?
    List<User> findByName(String name);

    // SELECT * FROM user WHERE email = ?
    User findByEmail(String email);

    // SELECT * FROM user WHERE name = ? AND email = ?
    List<User> findByNameAndEmail(String name, String email);

    // SELECT * FROM user WHERE name = ? OR email = ?
    List<User> findByNameOrEmail(String name, String email);

    // SELECT * FROM user WHERE name = ? ORDER BY createdAt DESC
    List<User> findByNameOrderByCreatedAtDesc(String name);
}
```

### Comparison Keywords

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // SELECT * FROM user WHERE age > ?
    List<User> findByAgeGreaterThan(int age);

    // SELECT * FROM user WHERE age < ?
    List<User> findByAgeLessThan(int age);

    // SELECT * FROM user WHERE age BETWEEN ? AND ?
    List<User> findByAgeBetween(int min, int max);

    // SELECT * FROM user WHERE name LIKE '%?%'
    List<User> findByNameContaining(String name);

    // SELECT * FROM user WHERE name LIKE '?%'
    List<User> findByNameStartingWith(String prefix);

    // SELECT * FROM user WHERE name LIKE '%?'
    List<User> findByNameEndingWith(String suffix);

    // SELECT * FROM user WHERE name IS NULL
    List<User> findByNameIsNull();

    // SELECT * FROM user WHERE name IS NOT NULL
    List<User> findByNameIsNotNull();

    // SELECT * FROM user WHERE age IN (?, ?, ?)
    List<User> findByAgeIn(List<Integer> ages);

    // SELECT * FROM user WHERE age NOT IN (?, ?, ?)
    List<User> findByAgeNotIn(List<Integer> ages);

    // SELECT * FROM user WHERE active = true
    List<User> findByActiveTrue();

    // SELECT * FROM user WHERE active = false
    List<User> findByActiveFalse();
}
```

### Boolean / Exists / Count

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // SELECT COUNT(*) FROM user WHERE name = ?
    long countByName(String name);

    // SELECT EXISTS(SELECT 1 FROM user WHERE email = ?)
    boolean existsByEmail(String email);

    // SELECT EXISTS(SELECT 1 FROM user WHERE name = ? AND active = true)
    boolean existsByNameAndActiveTrue(String name);

    // DELETE FROM user WHERE createdAt < ?
    long deleteByCreatedAtBefore(LocalDateTime date);
}
```

### Nested Properties (Relationships)

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // SELECT * FROM user WHERE address.city = ?
    List<User> findByAddressCity(String city);

    // SELECT * FROM user WHERE address.city = ? AND address.zipCode = ?
    List<User> findByAddressCityAndAddressZipCode(String city, String zipCode);

    // SELECT * FROM user WHERE department.name = ?
    List<User> findByDepartmentName(String deptName);

    // SELECT * FROM post WHERE user.name = ?
    List<Post> findByUser_Name(String userName);
}
```

### First / Top / Distinct

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // SELECT * FROM user ORDER BY createdAt DESC LIMIT 5
    List<User> findFirst5ByOrderByCreatedAtDesc();

    // SELECT * FROM user ORDER BY score DESC LIMIT 10
    List<User> findTop10ByOrderByScoreDesc();

    // SELECT DISTINCT * FROM user WHERE name = ?
    List<User> findDistinctByName(String name);

    // SELECT DISTINCT city FROM user
    List<String> findDistinctCityBy();
}
```

### Complete Keyword Reference

| Keyword | JPQL | SQL |
|---------|------|-----|
| `findBy` | `WHERE x.field = ?` | `WHERE column = ?` |
| `And` | `AND` | `AND` |
| `Or` | `OR` | `OR` |
| `GreaterThan` | `> ?` | `> ?` |
| `LessThan` | `< ?` | `< ?` |
| `Between` | `BETWEEN ? AND ?` | `BETWEEN ? AND ?` |
| `Containing` | `LIKE %?%` | `LIKE %?%` |
| `StartingWith` | `LIKE ?%` | `LIKE ?%` |
| `EndingWith` | `LIKE %?` | `LIKE %?` |
| `IsNull` | `IS NULL` | `IS NULL` |
| `IsNotNull` | `IS NOT NULL` | `IS NOT NULL` |
| `In` | `IN ?` | `IN (?, ?, ?)` |
| `NotIn` | `NOT IN ?` | `NOT IN (?, ?, ?)` |
| `True` | `= true` | `= true` |
| `False` | `= false` | `= false` |
| `OrderBy` | `ORDER BY` | `ORDER BY` |
| `Not` | `<> ?` | `<> ?` |
| `First` / `Top` | `LIMIT` | `LIMIT` |
| `Distinct` | `DISTINCT` | `DISTINCT` |

---

## @Query

**Matlab:** Custom JPQL ya SQL query likhne ke liye jab method name derivation kaafi na ho.

### JPQL Query

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // JPQL — entity/field names use hote hain (not table/column)
    @Query("SELECT u FROM User u WHERE u.name = :name")
    List<User> findByName(@Param("name") String name);

    // Multiple parameters
    @Query("SELECT u FROM User u WHERE u.name = :name AND u.email = :email")
    User findByNameAndEmail(@Param("name") String name, @Param("email") String email);

    // LIKE query
    @Query("SELECT u FROM User u WHERE u.name LIKE %:keyword%")
    List<User> searchByName(@Param("keyword") String keyword);

    // With pagination
    @Query("SELECT u FROM User u WHERE u.active = true")
    Page<User> findActiveUsers(Pageable pageable);
}
```

### Named Parameters (:name)

```java
@Query("SELECT u FROM User u WHERE u.name = :name AND u.age = :age")
List<User> findByNameAndAge(@Param("name") String name, @Param("age") int age);
```

### Positional Parameters (?1)

```java
@Query("SELECT u FROM User u WHERE u.name = ?1 AND u.age = ?2")
List<User> findByNameAndAge(String name, int age);
```

**⚠️ Named parameters recommended hain** — readable aur order-independent.

### SpEL Expressions

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // #{#entityName} = Entity ka name (User)
    @Query("SELECT u FROM #{#entityName} u WHERE u.name = :name")
    List<User> findByName(@Param("name") String name);
}
```

---

## @Modifying

**Matlab:** UPDATE ya DELETE queries run karne ke liye — by default `@Query` sirf SELECT ke liye hota hai.

### UPDATE Query

```java
public interface UserRepository extends JpaRepository<User, Long> {

    @Modifying
    @Query("UPDATE User u SET u.name = :name WHERE u.id = :id")
    int updateName(@Param("id") Long id, @Param("name") String name);

    // Bulk update
    @Modifying
    @Query("UPDATE User u SET u.active = false WHERE u.lastLoginAt < :date")
    int deactivateInactiveUsers(@Param("date") LocalDateTime date);
}
```

### DELETE Query

```java
public interface UserRepository extends JpaRepository<User, Long> {

    @Modifying
    @Query("DELETE FROM User u WHERE u.id = :id")
    int deleteByIdCustom(@Param("id") Long id);

    // Bulk delete
    @Modifying
    @Query("DELETE FROM User u WHERE u.active = false AND u.createdAt < :date")
    int deleteInactiveUsers(@Param("date") LocalDateTime date);
}
```

### @Modifying with flushAutomatically

```java
@Modifying(flushAutomatically = true)
@Query("UPDATE User u SET u.name = :name WHERE u.id = :id")
int updateName(@Param("id") Long id, @Param("name") String name);
```

**Matlab:** Query se pehle pending changes automatically flush ho jayengi.

### @Transactional Requirement

```java
// @Modifying queries @Transactional ke andar run karni padti hain

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public int updateName(Long id, String name) {
        return userRepository.updateName(id, name);
    }
}
```

Ya repository level pe `@Transactional` lagao:

```java
public interface UserRepository extends JpaRepository<User, Long> {

    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.name = :name WHERE u.id = :id")
    int updateName(@Param("id") Long id, @Param("name") String name);
}
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **CrudRepository** | Basic CRUD — save, findById, findAll, delete |
| **PagingAndSortingRepository** | CrudRepository + pagination + sorting |
| **JpaRepository** | Most used — CRUD + pagination + JPA-specific methods |
| **Method Name Derivation** | Method name se query auto-generate — `findByNameAndEmail` |
| **@Query** | Custom JPQL/SQL query jab method name kaafi na ho |
| **@Modifying** | UPDATE / DELETE queries run karne ke liye |
| **:name vs ?1** | Named parameters recommended — readable, order-independent |
| **@Transactional** | @Modifying queries transaction ke andar run karni padti hain |

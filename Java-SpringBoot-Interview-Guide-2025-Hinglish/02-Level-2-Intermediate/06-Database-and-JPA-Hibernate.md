# Level 2: Database & JPA / Hibernate (Q127 - Q136)

---

### Q127 & Q128: ORM, JPA and Difference Between JPA and Hibernate
**Answer (Hinglish):**

- **ORM (Object-Relational Mapping):**  
  Yeh ek technique/paradigm hai jo Object-Oriented programming languages (Java classes & objects) ko Relational Database (Tables, Rows & Columns) ke sath bina raw SQL queries likhe seamlessly map aur synchronize karti hai.

| Criteria | JPA (Jakarta Persistence API) | Hibernate |
| :--- | :--- | :--- |
| **Type** | **Specification / Interface** (Standard guidelines). | **Implementation / Framework** (Actual working engine). |
| **Owner** | Eclipse Foundation / Jakarta EE. | Red Hat / Community. |
| **Analogy** | Rule book / Interface (e.g. `List` interface). | Concrete Class (e.g. `ArrayList` class). |
| **Can it run alone?**| Nahi, JPA ko run karne ke liye ek underlying provider (like Hibernate, EclipseLink) chahiye. | Haan, Hibernate standalone ORM ki tarah bhi direct use ho sakta hai. |

---

### Q129 & Q130: Entity & Core JPA Annotations
**Answer (Hinglish):**

- **Entity:** Java ki aisi class jo database ke ek specific table ko represent karti hai.
- **Core Annotations:**
  - `@Entity`: Class ko JPA managed table entity declare karta hai.
  - `@Table(name = "tbl_users")`: Custom database table name specify karne ke liye.
  - `@Id`: Table ki Primary Key column mark karta hai.
  - `@GeneratedValue(strategy = GenerationType.IDENTITY)`: Auto-increment primary key generation strategy define karta hai (`IDENTITY`, `SEQUENCE`, `AUTO`, `UUID`).
  - `@Column(name = "full_name", nullable = false, length = 100)`: Column constraints configure karne ke liye.

```java
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private Double price;
    // Getters and Setters
}
```

---

### Q131: Difference between persist() and merge()?
**Answer (Hinglish):**

Entity states: *Transient* (New object), *Persistent* (Managed in session), *Detached* (Session closed), *Removed*.

| Feature | `entityManager.persist(entity)` | `entityManager.merge(entity)` |
| :--- | :--- | :--- |
| **Intended State**| **Transient (New)** object ko database me insert karne ke liye. | **Detached** object ko wapas Persistence Context me attach karke update karne ke liye. |
| **Return Value** | `void` (Same passed object persistent ban jata hai). | Managed **New Copy return karta hai** (`T mergedEntity = em.merge(detachedEntity)`). |
| **If ID exists** | Agar entity ke paas already ID hai to `EntityExistsException` throw kar sakta hai. | Agar ID DB me exist karti hai to `UPDATE` query chalata hai, agar nahi milti to `INSERT` chalata hai. |

---

### Q132: What are JPA Relationships (@OneToMany, @ManyToOne, etc.)?
**Answer (Hinglish):**

RDBMS relationships ko Java entities me represent karne ke annotations:
1. **`@OneToOne`:** 1 User ka 1 Profile.
2. **`@OneToMany` & `@ManyToOne` (Most Popular):** 1 Department ke paas Many Employees.
   ```java
   @Entity
   public class Department {
       @Id @GeneratedValue
       private Long id;

       @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
       private List<Employee> employees = new ArrayList<>();
   }

   @Entity
   public class Employee {
       @Id @GeneratedValue
       private Long id;

       @ManyToOne(fetch = FetchType.LAZY)
       @JoinColumn(name = "department_id") // Foreign key column
       private Department department;
   }
   ```
3. **`@ManyToMany`:** Many Students attend Many Courses (Requires `@JoinTable`).

---

### Q133: What is JPQL (Java Persistence Query Language)?
**Answer (Hinglish):**

- **Definition:** JPQL ek platform-independent query language hai.
- **SQL vs JPQL:**
  - SQL direct Database **Tables aur Columns** par queries run karta hai (`SELECT * FROM tbl_user WHERE user_age > 18`).
  - JPQL Java ke **Entities aur Field Names** par queries run karta hai (`SELECT u FROM User u WHERE u.age > 18`).
- **Benefit:** Database change hone par (MySQL se PostgreSQL ya Oracle) queries rewrite nahi karni padti; Hibernate auto-generate karta hai dialect-specific SQL.

---

### Q134: Lazy Loading vs Eager Loading?
**Answer (Hinglish):**

| Feature | Lazy Loading (`FetchType.LAZY`) | Eager Loading (`FetchType.EAGER`) |
| :--- | :--- | :--- |
| **Data Fetch Timing** | Child records tab tak fetch **nahi** hote jab tak unhe code me explicitly access na kiya jaye (`dept.getEmployees()`). | Parent fetch hote hi saare child records **turant SQL JOIN** karke memory me load ho jaate hain. |
| **Performance** | **High & Optimized** (Unnecessary data load nahi hota). | **Risky** (Unwanted massive data load hone se high memory consume hoti hai). |
| **Default In JPA** | `@OneToMany`, `@ManyToMany` by default LAZY hote hain. | `@ManyToOne`, `@OneToOne` by default EAGER hote hain. |
| **Potential Error** | Agar Hibernate Session close hone ke baad child access kiya to **`LazyInitializationException`** aati hai. | Session close hone ke baad bhi data mil jata hai kyunki pehle hi load ho chuka tha. |

> **Best Practice Tip:** Production applications me hamesha **`FetchType.LAZY`** use karein aur required data ko `JOIN FETCH` ya Entity Graphs se load karein.

---

### Q135: save() vs saveAndFlush() in Spring Data JPA?
**Answer (Hinglish):**

- **`save(entity)`:**  
  Entity ko persistence context me save karta hai, lekin database me SQL `INSERT/UPDATE` turant execute nahi hoti. Hibernate flush transaction commit hone par ya zaroorat padne par batch me karta hai (Performance optimized).
- **`saveAndFlush(entity)`:**  
  Entity ko save karne ke **turant baad Database me forcefully `FLUSH`** kar deta hai (Immediate SQL execution). Lekin transaction tab bhi open rehti hai jab tak commit na ho.
- **Use Case for `saveAndFlush`:** Jab aapko usi transaction ke andar kisi native query ya external procedure ko call karna ho jise newly saved row database tables me right away visibly chahiye ho.

---

### Q136: Hibernate get() vs load()
**Answer (Hinglish):**

| Feature | `session.get(Class, id)` | `session.load(Class, id)` |
| :--- | :--- | :--- |
| **Database Hit** | Turant DB par SELECT query chala kar actual object fetch karta hai (Eager). | DB ko turant hit nahi karta; ek **CGLIB Proxy object** return karta hai (Lazy). |
| **If ID Not Found** | Agar record exist nahi karta to **`null`** return karta hai. | Agar record nahi mila aur proxy access hui to **`ObjectNotFoundException`** throw karta hai. |
| **Performance** | Thoda heavy agar sirf foreign key set karne ke liye reference chahiye tha. | Fast, DB roundtrip bachata hai agar sirf association reference assign karni ho. |

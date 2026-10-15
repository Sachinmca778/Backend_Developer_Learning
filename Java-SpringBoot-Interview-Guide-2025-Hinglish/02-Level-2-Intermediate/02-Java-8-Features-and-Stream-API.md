# Level 2: Java 8 Features & Stream API (Q71 - Q79)

---

### Q71: What is a Functional Interface?
**Answer (Hinglish):**

- **Definition:** Ek aisa interface jisme **sirf aur sirf ek hi Single Abstract Method (SAM)** hota hai, use Functional Interface kehte hain.
- Isme multiple `default` ya `static` methods ho sakte hain, aur `java.lang.Object` ke public methods (jaise `equals()`) bhi ho sakte hain, lekin abstract method strictly ek hi hona chahiye.
- **Annotation:** `@FunctionalInterface` (Optional hai lekin likhna recommended hai taaki compiler compile-time par rule enforce kar sake).

**Built-in Functional Interfaces in Java 8 (`java.util.function`):**
1. **`Predicate<T>`:** Takes 1 input, returns `boolean`. Method: `boolean test(T t)`.  
   *Use:* Filter operations (`x -> x > 10`).
2. **`Function<T, R>`:** Takes input of type `T`, returns result of type `R`. Method: `R apply(T t)`.  
   *Use:* Data transformation (`emp -> emp.getName()`).
3. **`Consumer<T>`:** Takes 1 input, returns nothing (`void`). Method: `void accept(T t)`.  
   *Use:* Printing ya DB save (`x -> System.out.println(x)`).
4. **`Supplier<T>`:** Takes no input, returns value of type `T`. Method: `T get()`.  
   *Use:* Factory / Lazy initialization (`() -> UUID.randomUUID().toString()`).

---

### Q72: Explain Java 8 Major Features
**Answer (Hinglish):**

Java 8 Java history ka sabse revolutionary release tha. Iske key features:
1. **Lambda Expressions:** Anonymous functions jo code ko short, readable aur functional banati hain `(args) -> { body }`.
2. **Functional Interfaces:** SAM interfaces jo lambdas ko support karte hain.
3. **Stream API:** Collections ko declarative aur functional style me filter, map, aur reduce karne ke liye.
4. **Default & Static Methods in Interfaces:** Purane implementations ko break kiye bina interfaces me naye methods introduce karne ke liye.
5. **Optional<T> Class:** `NullPointerException` se bachne ke liye container object jo value presence ya absence ko explicitly handle karta hai.
6. **New Date and Time API (`java.time`):** Joda-Time inspired immutable, thread-safe classes (`LocalDate`, `LocalTime`, `LocalDateTime`, `ZonedDateTime`).

---

### Q73: Difference between map() vs flatMap() in Java 8?
**Answer (Hinglish):**

Yeh interview me har ek Java developer se pucha jata hai:

| Feature | `map()` | `flatMap()` |
| :--- | :--- | :--- |
| **Operation** | **Transformation (One-to-One)**. | **Transformation + Flattening (One-to-Many)**. |
| **Input / Output** | Har input element ke badle exactly ek output element deta hai. | Har input element ke badle ek Stream of elements deta hai, aur un sabhi streams ko flatten karke ek single stream bana deta hai. |
| **Data Structure** | Stream of values: `Stream<T>` -> `Stream<R>`. | Nested data structures: `Stream<List<T>>` -> `Stream<T>`. |

**Code Example:**
```java
List<List<String>> nestedList = List.of(
    List.of("Java", "Spring"),
    List.of("Docker", "Kubernetes")
);

// Using flatMap to flatten 2D list into 1D list
List<String> flatList = nestedList.stream()
    .flatMap(List::stream)
    .toList(); // Output: ["Java", "Spring", "Docker", "Kubernetes"]
```

---

### Q74, Q75 & Q76: Stream API, Pipeline, Intermediate vs Terminal Operations
**Answer (Hinglish):**

- **Stream API:** Stream data ka ek sequence hai jo memory me data store **nahi** karta, balki source (Collection/Array) se data lekar computational pipeline ke through process karta hai.
- **Stream Pipeline Structure:**
  ```
  [Source: List/Set] ──> [Intermediate Ops: filter, map, sorted] ──> [Terminal Op: collect, forEach, count]
  ```

| Difference Point | Intermediate Operations | Terminal Operations |
| :--- | :--- | :--- |
| **Return Type** | Ek naya `Stream<T>` return karta hai. | Non-stream result return karta hai (List, long, Optional, void). |
| **Execution Nature** | **Lazy** (Yeh tab tak execute nahi hote jab tak terminal operation call na ho). | **Eager** (Pipeline ko trigger karke results evaluate karta hai). |
| **Quantity in Pipeline**| Chained multiple times ho sakte hain. | Pipeline ke end me **sirf 1** terminal operation hota hai. |
| **Examples** | `filter()`, `map()`, `sorted()`, `distinct()`, `limit()`. | `collect()`, `forEach()`, `reduce()`, `count()`, `findFirst()`. |

---

### Q77: [Coding] Using Stream API, find the 2nd highest salary from Employee list
**Answer (Hinglish):**

Yeh classic interview coding challenge hai:

```java
import java.util.*;
import java.util.stream.Collectors;

public class SecondHighestSalary {
    record Employee(int id, String name, double salary) {}

    public static void main(String[] args) {
        List<Employee> employees = List.of(
            new Employee(1, "Aarav", 50000),
            new Employee(2, "Sachin", 95000),
            new Employee(3, "Priya", 80000),
            new Employee(4, "Rahul", 95000), // Duplicate highest
            new Employee(5, "Neha", 70000)
        );

        // 1. Unique salaries filter karke descending sort karo
        // 2. 1st element skip karo (highest nikal gaya)
        // 3. findFirst() se 2nd element uthao
        Optional<Double> secondHighest = employees.stream()
            .map(Employee::salary)
            .distinct()                              // Duplicates hataye (e.g. dono 95k)
            .sorted(Comparator.reverseOrder())       // Descending order
            .skip(1)                                 // Skip highest
            .findFirst();                            // 2nd highest

        secondHighest.ifPresent(sal -> System.out.println("2nd Highest Salary: " + sal));
        // Output: 80000.0
    }
}
```

---

### Q78: Use of Stream API in Real-world Projects
**Answer (Hinglish):**

Real-world Spring Boot enterprise projects me Stream API kahan use hota hai:
1. **Entity to DTO Conversion:**
   ```java
   List<UserDTO> dtoList = userRepo.findAll().stream()
       .map(userMapper::toDTO)
       .toList();
   ```
2. **Filtering Active Users / Status Checks:**
   ```java
   List<Order> pendingOrders = orders.stream()
       .filter(o -> o.getStatus() == OrderStatus.PENDING)
       .toList();
   ```
3. **Grouping / Aggregations (e.g., Reports by Department):**
   ```java
   Map<String, List<Employee>> byDept = employees.stream()
       .collect(Collectors.groupingBy(Employee::getDepartment));
   ```
4. **Partitioning & Reductions:** Total cart price calculate karna using `reduce()` ya `summingDouble()`.

---

### Q79: Lambda expressions vs Anonymous Inner Classes
**Answer (Hinglish):**

| Aspect | Anonymous Inner Class | Lambda Expression |
| :--- | :--- | :--- |
| **Syntax & Boilerplate** | Verbose (Bahut sara boilerplate code: `new Runnable() { ... }`). | Clean and concise: `() -> { ... }`. |
| **Target Types** | Classes, Abstract Classes, aur Interfaces kisi par bhi lag sakta hai. | **Sirf Functional Interfaces** par lag sakta hai. |
| **`this` Keyword Reference** | `this` us anonymous inner class ke instance ko refer karta hai. | `this` surrounding enclosing class ke instance ko refer karta hai (Lexical scoping). |
| **Compilation Artifact** | Alag `.class` file generate hoti hai (e.g. `Main$1.class`). | Koi nayi class file nahi banti; JVM ka `invokedynamic` bytecode instruction use hota hai (High memory efficiency). |

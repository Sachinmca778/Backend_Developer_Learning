# Comparable & Comparator

## Status: Not Started

---

## Table of Contents

1. [Comparable&lt;T&gt; (Natural Ordering)](#comparablet-natural-ordering)
2. [Comparator&lt;T&gt; (External Ordering)]#comparatort-external-ordering)
3. [Comparator.comparing](#comparatorcomparing)
4. [thenComparing](#thencomparing)
5. [reversed](#reversed)
6. [Lambda Comparators](#lambda-comparators)

---

## Comparable&lt;T&gt; (Natural Ordering)

**Matlab:** Class ke andar natural ordering define karna — `compareTo()` method implement karna padta hai.

### Interface

```java
public interface Comparable<T> {
    int compareTo(T other);
}
```

### Return Values

```java
this.compareTo(other)

< 0  → this < other  (this pehle aayega)
== 0 → this == other (equal — order matter nahi)
> 0  → this > other  (other pehle aayega)
```

### Basic Implementation

```java
class Person implements Comparable<Person> {
    String name;
    int age;

    Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Override
    public int compareTo(Person other) {
        // Age ke basis pe compare karo
        return Integer.compare(this.age, other.age);
    }

    @Override
    public String toString() {
        return name + "(" + age + ")";
    }
}

// Usage
List<Person> people = new ArrayList<>();
people.add(new Person("Sachin", 25));
people.add(new Person("Rahul", 22));
people.add(new Person("Priya", 28));

// Natural ordering — age ke basis pe sort hoga
Collections.sort(people);
System.out.println(people);  // [Rahul(22), Sachin(25), Priya(28)]

// Ya List.sort use karo
people.sort(null);  // null = natural ordering use karo
```

### String Comparison Example

```java
class Product implements Comparable<Product> {
    String name;
    double price;

    Product(String name, double price) {
        this.name = name;
        this.price = price;
    }

    @Override
    public int compareTo(Product other) {
        // Name ke basis pe (alphabetical)
        return this.name.compareTo(other.name);
    }

    @Override
    public String toString() {
        return name + "($" + price + ")";
    }
}

List<Product> products = new ArrayList<>();
products.add(new Product("Laptop", 50000));
products.add(new Product("Phone", 30000));
products.add(new Product("Tablet", 20000));

Collections.sort(products);
System.out.println(products);  // [Laptop($50000), Phone($30000), Tablet($20000)]
```

### ⚠️ compareTo() vs equals() Consistency

```java
// ✅ Consistent — compareTo() == 0 implies equals() == true
class Consistent implements Comparable<Consistent> {
    int value;

    @Override
    public int compareTo(Consistent other) {
        return Integer.compare(this.value, other.value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Consistent)) return false;
        return this.value == ((Consistent) o).value;
    }
}

// ❌ Inconsistent — compareTo() == 0 but equals() == false
// Problem: TreeSet/TreeMap mein issues aa sakte hain
```

**Rule:** `compareTo() == 0` hamesha `equals() == true` ke barabar hona chahiye.

### Built-in Comparable Classes

```java
// Java ke sab wrapper classes already Comparable implement karti hain
String: "Apple".compareTo("Banana")     // < 0 (alphabetical)
Integer: 5.compareTo(10)                 // < 0 (numeric)
Double: 3.14.compareTo(2.71)            // > 0 (numeric)
LocalDate: date1.compareTo(date2)       // chronological
```

---

## Comparator&lt;T&gt; (External Ordering)

**Matlab:** Class ke bahar ordering define karna — multiple sorting strategies possible.

### Interface

```java
public interface Comparator<T> {
    int compare(T o1, T o2);
}
```

### Basic Implementation

```java
class Person {
    String name;
    int age;

    Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Override
    public String toString() {
        return name + "(" + age + ")";
    }
}

// Age comparator
class AgeComparator implements Comparator<Person> {
    @Override
    public int compare(Person p1, Person p2) {
        return Integer.compare(p1.age, p2.age);
    }
}

// Name comparator
class NameComparator implements Comparator<Person> {
    @Override
    public int compare(Person p1, Person p2) {
        return p1.name.compareTo(p2.name);
    }
}

// Usage
List<Person> people = new ArrayList<>();
people.add(new Person("Sachin", 25));
people.add(new Person("Rahul", 22));
people.add(new Person("Priya", 28));

// Sort by age
Collections.sort(people, new AgeComparator());
System.out.println(people);  // [Rahul(22), Sachin(25), Priya(28)]

// Sort by name
Collections.sort(people, new NameComparator());
System.out.println(people);  // [Priya(28), Rahul(22), Sachin(25)]
```

### Anonymous Class Comparator

```java
List<Person> people = new ArrayList<>();
// ... add people

// Sort by age
Collections.sort(people, new Comparator<Person>() {
    @Override
    public int compare(Person p1, Person p2) {
        return Integer.compare(p1.age, p2.age);
    }
});
```

---

## Comparator.comparing

**Matlab:** Key extractor function se comparator banana — clean aur readable syntax.

### Basic Usage

```java
List<Person> people = new ArrayList<>();
// ... add people

// Sort by name
Collections.sort(people, Comparator.comparing(p -> p.name));

// Sort by age
Collections.sort(people, Comparator.comparingInt(p -> p.age));
```

### Comparing Methods

```java
// Object key (natural ordering of key)
Comparator.comparing(Person::getName)

// int key
Comparator.comparingInt(Person::getAge)

// long key
Comparator.comparingLong(Person::getId)

// double key
Comparator.comparingDouble(Person::getSalary)
```

### Complete Example

```java
class Employee {
    String name;
    int age;
    double salary;

    Employee(String name, int age, double salary) {
        this.name = name;
        this.age = age;
        this.salary = salary;
    }

    // Getters
    public String getName() { return name; }
    public int getAge() { return age; }
    public double getSalary() { return salary; }

    @Override
    public String toString() {
        return name + "(" + age + ", $" + salary + ")";
    }
}

List<Employee> employees = new ArrayList<>();
employees.add(new Employee("Sachin", 25, 50000));
employees.add(new Employee("Rahul", 30, 60000));
employees.add(new Employee("Priya", 25, 55000));

// Sort by name
employees.sort(Comparator.comparing(Employee::getName));
System.out.println(employees);
// [Priya(25, $55000), Rahul(30, $60000), Sachin(25, $50000)]

// Sort by age
employees.sort(Comparator.comparingInt(Employee::getAge));
System.out.println(employees);
// [Priya(25, $55000), Sachin(25, $50000), Rahul(30, $60000)]

// Sort by salary
employees.sort(Comparator.comparingDouble(Employee::getSalary));
System.out.println(employees);
// [Sachin(25, $50000), Priya(25, $55000), Rahul(30, $60000)]
```

---

## thenComparing

**Matlab:** Primary comparator ke baad secondary (aur tertiary) comparator apply karna — multi-level sorting.

### Basic Usage

```java
List<Employee> employees = new ArrayList<>();
employees.add(new Employee("Sachin", 25, 50000));
employees.add(new Employee("Rahul", 30, 60000));
employees.add(new Employee("Priya", 25, 55000));
employees.add(new Employee("Ankit", 30, 45000));

// Sort by age, then by name
employees.sort(Comparator
    .comparingInt(Employee::getAge)
    .thenComparing(Employee::getName));

System.out.println(employees);
// [Priya(25, $55000), Sachin(25, $50000), Ankit(30, $45000), Rahul(30, $60000)]

// Sort by age, then by salary
employees.sort(Comparator
    .comparingInt(Employee::getAge)
    .thenComparingDouble(Employee::getSalary));

System.out.println(employees);
// [Sachin(25, $50000), Priya(25, $55000), Ankit(30, $45000), Rahul(30, $60000)]
```

### Multiple Levels

```java
// Age → Name → Salary
employees.sort(Comparator
    .comparingInt(Employee::getAge)
    .thenComparing(Employee::getName)
    .thenComparingDouble(Employee::getSalary));
```

---

## reversed

**Matlab:** Comparator ko reverse karna — ascending → descending ya vice versa.

### Basic Usage

```java
List<Employee> employees = new ArrayList<>();
// ... add employees

// Descending by age
employees.sort(Comparator.comparingInt(Employee::getAge).reversed());

// Ascending by name, then descending by salary
employees.sort(Comparator
    .comparing(Employee::getName)
    .thenComparing(Comparator.comparingDouble(Employee::getSalary).reversed()));
```

### Natural Order Reverse

```java
// Strings — reverse alphabetical
List<String> names = new ArrayList<>(List.of("Sachin", "Rahul", "Priya"));
names.sort(Comparator.reverseOrder());
System.out.println(names);  // [Sachin, Rahul, Priya]

// Integers — descending
List<Integer> nums = new ArrayList<>(List.of(5, 2, 8, 1, 9));
nums.sort(Comparator.reverseOrder());
System.out.println(nums);  // [9, 8, 5, 2, 1]
```

---

## Lambda Comparators

**Matlab:** Lambda expressions se comparators banana — concise syntax.

### Basic Lambda Comparator

```java
List<Person> people = new ArrayList<>();
// ... add people

// Sort by age
people.sort((p1, p2) -> Integer.compare(p1.age, p2.age));

// Sort by name
people.sort((p1, p2) -> p1.name.compareTo(p2.name));

// Sort by name descending
people.sort((p1, p2) -> p2.name.compareTo(p1.name));
```

### Multi-field Lambda

```java
// Age pehle, phir name
people.sort((p1, p2) -> {
    int ageCompare = Integer.compare(p1.age, p2.age);
    if (ageCompare != 0) return ageCompare;
    return p1.name.compareTo(p2.name);
});
```

### Comparator with Method References

```java
// Sort by name
people.sort(Comparator.comparing(Person::getName));

// Sort by age
people.sort(Comparator.comparingInt(Person::getAge));

// Null-safe (nulls first)
people.sort(Comparator.comparing(Person::getName, Comparator.nullsFirst(String::compareTo)));

// Null-safe (nulls last)
people.sort(Comparator.comparing(Person::getName, Comparator.nullsLast(String::compareTo)));
```

### Real-World Example

```java
class Order {
    String customerName;
    LocalDate orderDate;
    double totalAmount;
    String status;  // "PENDING", "SHIPPED", "DELIVERED"

    // ... constructor, getters

    @Override
    public String toString() {
        return customerName + " - " + status + " - $" + totalAmount;
    }
}

List<Order> orders = new ArrayList<>();
orders.add(new Order("Sachin", LocalDate.of(2024, 1, 15), 5000, "DELIVERED"));
orders.add(new Order("Rahul", LocalDate.of(2024, 1, 10), 3000, "PENDING"));
orders.add(new Order("Priya", LocalDate.of(2024, 1, 12), 7000, "SHIPPED"));

// Sort by status (custom order), then by date
Map<String, Integer> statusPriority = Map.of(
    "PENDING", 1, "SHIPPED", 2, "DELIVERED", 3
);

orders.sort(Comparator
    .comparingInt((Order o) -> statusPriority.getOrDefault(o.status, 999))
    .thenComparing(Order::getOrderDate));

System.out.println(orders);
// Rahul - PENDING - $3000
// Priya - SHIPPED - $7000
// Sachin - DELIVERED - $5000
```

---

## Comparable vs Comparator

| Feature | Comparable | Comparator |
|---------|-----------|------------|
| **Package** | `java.lang` | `java.util` |
| **Method** | `compareTo(T)` | `compare(T, T)` |
| **Location** | Class ke andar | Class ke bahar (ya lambda) |
| **Ordering** | Natural ordering | Multiple orderings possible |
| **Modification** | Class modify karni padti hai | Existing class pe bhi use kar sakte ho |
| **When to Use** | Default sorting strategy | Custom/alternative sorting |

### When to Use What

```java
// ✅ Use Comparable when:
// - Class ka ek natural ordering hai
// - Class ko modify kar sakte ho
// - Sorting ka ek hi way hai

class Product implements Comparable<Product> {
    @Override
    public int compareTo(Product other) {
        return this.name.compareTo(other.name);  // Natural: alphabetical
    }
}

// ✅ Use Comparator when:
// - Multiple sorting strategies chahiye
// - Existing class ko modify nahi kar sakte
// - Third-party class ko sort karna hai

List<String> strings = new ArrayList<>();
strings.sort(Comparator.comparingInt(String::length));  // Length-based
strings.sort(Comparator.reverseOrder());                // Reverse alphabetical
strings.sort(String.CASE_INSENSITIVE_ORDER);            // Case-insensitive
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Comparable&lt;T&gt;** | Natural ordering — `compareTo()` method implement karo |
| **Comparator&lt;T&gt;** | External ordering — `compare()` method ya lambda use karo |
| **Comparator.comparing** | Key extractor se comparator banao — clean syntax |
| **thenComparing** | Multi-level sorting — primary, secondary, tertiary |
| **reversed** | Comparator ko reverse karo — ascending ↔ descending |
| **Lambda Comparators** | `(a, b) -> a.field.compareTo(b.field)` — concise |
| **nullsFirst/nullsLast** | Null values ko safely handle karo |
| **Comparable vs Comparator** | Comparable = natural ordering, Comparator = custom ordering |

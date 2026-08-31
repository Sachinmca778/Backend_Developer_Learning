# Generics - Top 10 Interview Questions & Answers

> Covering: Generic Classes/Methods, Bounded Type Parameters, Wildcards, PECS, Type Erasure, Raw Types, Heap Pollution, Bridge Methods, and Generic Interfaces (Comparable)

---

## Q1: Generics kyun use karte hain? Type safety ka problem kya tha?

**Answer:**

Java 5 se pehle collections `Object` type use karte the — compile-time pe type check nahi hoti thi, runtime pe `ClassCastException` aata tha.

### Problem Without Generics:

```java
// ❌ Without Generics — type safety nahi
class Box {
    private Object content;

    public void setContent(Object content) { this.content = content; }
    public Object getContent() { return content; }
}

Box box = new Box();
box.setContent("Hello");
String str = (String) box.getContent();  // Manual casting

box.setContent(123);  // ❌ Koi bhi object daal sakta hai
String wrong = (String) box.getContent();  // ❌ ClassCastException at runtime!
```

### Solution With Generics:

```java
// ✅ With Generics — compile-time type safety
class Box<T> {
    private T content;

    public void setContent(T content) { this.content = content; }
    public T getContent() { return content; }
}

Box<String> stringBox = new Box<>();
stringBox.setContent("Hello");
String str = stringBox.getContent();  // No casting!

// stringBox.setContent(123);  // ❌ Compile-time error!
```

### Key Benefits:

| Benefit | Explanation |
|---------|-------------|
| **Type Safety** | Galat type compile-time pe catch ho jaati hai |
| **No Casting** | Code clean aur readable rehta hai |
| **Reusability** | Ek class/method multiple types ke liye use kar sakte ho |
| **Better APIs** | Collections, Streams, Optional sab generics pe based hain |

**Short Interview Answer:**

Generics compile-time type safety dete hain — casting ki zarurat nahi, aur galat type daalne pe compile error milta hai instead of runtime `ClassCastException`.

---

## Q2: Generic Class vs Generic Method — difference kya hai?

**Answer:**

**Generic Class** — poori class type-parameterized hoti hai (`Box<T>`).  
**Generic Method** — class generic na ho, phir bhi method generic ho sakta hai (`<T> T getFirst(T[] arr)`).

### Generic Class:

```java
class Repository<T> {
    private List<T> items = new ArrayList<>();

    public void add(T item) { items.add(item); }
    public T get(int index) { return items.get(index); }
}

Repository<User> userRepo = new Repository<>();
userRepo.add(new User("Sachin", 25));
User user = userRepo.get(0);  // No casting
```

### Generic Method (Non-Generic Class mein bhi):

```java
class Utility {
    // Generic method — <T> method level pe declare hota hai
    public static <T> T getFirst(T[] array) {
        return array.length > 0 ? array[0] : null;
    }

    public static <T, R> List<R> map(List<T> list, Function<T, R> mapper) {
        List<R> result = new ArrayList<>();
        for (T item : list) {
            result.add(mapper.apply(item));
        }
        return result;
    }
}

String[] strings = {"Apple", "Banana"};
String first = Utility.getFirst(strings);  // T inferred as String

Integer[] nums = {1, 2, 3};
Integer firstNum = Utility.getFirst(nums);  // T inferred as Integer
```

### Multiple Type Parameters:

```java
class Pair<K, V> {
    private K key;
    private V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() { return key; }
    public V getValue() { return value; }
}

Pair<String, Integer> age = new Pair<>("Sachin", 25);
```

### Diamond Operator (Java 7+):

```java
// ✅ Compiler type infer kar leta hai
List<String> list = new ArrayList<>();
Map<String, List<Integer>> map = new HashMap<>();
Pair<String, Integer> pair = new Pair<>("Sachin", 25);
```

**Important:**

Generic method ka type parameter `<T>` return type se **pehle** likhna padta hai. Class generic nahi ho toh bhi method generic ho sakta hai — yeh flexibility bahut useful hai utility methods ke liye.

---

## Q3: Type Erasure kya hai? Runtime pe generics exist karte hain ya nahi?

**Answer:**

**Type Erasure** matlab: compile-time pe generic type information remove ho jaati hai — bytecode mein sirf raw types ya `Object` rehte hain. **Runtime pe generics exist nahi karte.**

### Compile Time vs Runtime:

```java
// Compile time
List<String> stringList = new ArrayList<>();
List<Integer> intList = new ArrayList<>();

// Runtime — dono same type hain!
System.out.println(stringList.getClass() == intList.getClass());  // true
System.out.println(stringList.getClass());  // class java.util.ArrayList (NOT ArrayList<String>)
```

### Erasure Example:

```java
// Source code
class Box<T> {
    private T content;
    public T getContent() { return content; }
}

// After erasure (conceptually)
class Box {
    private Object content;           // T → Object
    public Object getContent() { return content; }
}
```

### Bounded Type Erasure:

```java
// Source
class Calculator<T extends Number> {
    public double add(T a, T b) {
        return a.doubleValue() + b.doubleValue();
    }
}

// After erasure
class Calculator {
    public double add(Number a, Number b) {
        return a.doubleValue() + b.doubleValue();
    }
}
```

### Implications:

| Limitation | Reason |
|------------|--------|
| `new T()` nahi kar sakte | Runtime pe T ka type pata nahi |
| `instanceof List<String>` invalid | Erased type check nahi ho sakti |
| Generic array `T[]` risky hai | Heap pollution possible |
| Overloading `<T> void m(T t)` vs `void m(String s)` conflict | Erasure ke baad same signature |

```java
// ❌ Cannot create generic array directly
// T[] array = new T[10];  // Compile error

// ❌ instanceof with parameterized type
// if (list instanceof List<String>) {}  // Compile error

// ✅ Workaround — pass Class<T> explicitly
public <T> T createInstance(Class<T> clazz) throws Exception {
    return clazz.getDeclaredConstructor().newInstance();
}
```

**Short Interview Answer:**

Type erasure means generic type info compile-time pe remove ho jaati hai for backward compatibility. Runtime pe `List<String>` aur `List<Integer>` dono sirf `List` hain — isliye `new T()`, generic `instanceof`, aur runtime type checks possible nahi.

---

## Q4: Bounded Type Parameters kya hain? Upper bound vs Lower bound?

**Answer:**

Bounded types type parameter ko restrict karte hain — sirf certain types ya unke subtypes/supertypes allowed hote hain.

### Upper Bound (`<T extends Number>`):

```java
// T sirf Number ya uske subtypes ho sakta hai
class Calculator<T extends Number> {
    public double add(T a, T b) {
        return a.doubleValue() + b.doubleValue();  // ✅ Number ka method
    }
}

Calculator<Integer> intCalc = new Calculator<>();   // ✅
Calculator<Double> doubleCalc = new Calculator<>(); // ✅
// Calculator<String> strCalc = new Calculator<>();  // ❌ Compile error

// Generic method with upper bound
public static <T extends Number> double sum(List<T> numbers) {
    double total = 0;
    for (T num : numbers) {
        total += num.doubleValue();
    }
    return total;
}
```

### Lower Bound (`<? super Integer>`):

```java
// ? = Integer ya uska supertype (Number, Object)
public static void addNumbers(List<? super Integer> list, int count) {
    for (int i = 1; i <= count; i++) {
        list.add(i);  // ✅ Integer add kar sakte ho
    }
}

List<Integer> intList = new ArrayList<>();
addNumbers(intList, 5);  // ✅

List<Number> numList = new ArrayList<>();
addNumbers(numList, 3);  // ✅

List<Object> objList = new ArrayList<>();
addNumbers(objList, 2);  // ✅
```

### Multiple Bounds:

```java
// T ko Comparable AND Serializable dono implement karna padega
public static <T extends Comparable<T> & Serializable> void saveAndSort(List<T> list) {
    Collections.sort(list);
    // serialize logic...
}

// First bound class ho sakta hai, baaki interfaces
class Processor<T extends Number & Comparable<T>> {
    public int compare(T a, T b) {
        return a.compareTo(b);
    }
}
```

| Bound Type | Syntax | Use Case |
|------------|--------|----------|
| **Upper** | `<T extends X>` | Type pe specific methods call karna |
| **Lower** | `<? super T>` | Collection mein write karna |
| **Multiple** | `<T extends A & B>` | Multiple contracts enforce karna |

**Important:**

Upper bound mein `extends` use hota hai (class + interface dono ke liye). Lower bound wildcards mein `super` use hota hai — type parameters mein lower bound directly nahi likhte.

---

## Q5: Wildcards kya hain? PECS rule explain karo (? extends vs ? super)

**Answer:**

Wildcards (`?`) unknown type represent karte hain — flexibility dete hain jab exact type matter nahi karta ya covariance/contravariance chahiye.

### Three Types of Wildcards:

| Wildcard | Read As | Write | Use Case |
|----------|---------|-------|----------|
| `<?>` | Object | Nothing (except null) | Type irrelevant |
| `<? extends T>` | T | Nothing (except null) | **Producer** — read data |
| `<? super T>` | Object | T (or subtype) | **Consumer** — write data |

### PECS — Producer Extends, Consumer Super:

```java
// PRODUCER — List se read kar rahe ho → ? extends T
public static double sum(List<? extends Number> numbers) {
    double total = 0;
    for (Number n : numbers) {  // ✅ Read as Number
        total += n.doubleValue();
    }
    return total;
}

List<Integer> ints = List.of(1, 2, 3);
List<Double> doubles = List.of(1.1, 2.2);
sum(ints);     // ✅
sum(doubles);  // ✅

// numbers.add(10);  // ❌ Write nahi kar sakte — actual type unknown

// CONSUMER — List mein write kar rahe ho → ? super T
public static void addNumbers(List<? super Integer> list, int count) {
    for (int i = 1; i <= count; i++) {
        list.add(i);  // ✅ Write kar sakte ho
    }
}

List<Number> numList = new ArrayList<>();
addNumbers(numList, 3);  // ✅
```

### Why Can't We Write to `? extends T`?

```java
List<Integer> intList = new ArrayList<>();
List<? extends Number> numbers = intList;

// Agar allow hota:
// numbers.add(3.14);  // Double add karne ki koshish
// Lekin actual list List<Integer> hai — type safety break!
```

### Copy Example (PECS in action):

```java
public static <T> void copy(List<? extends T> source, List<? super T> dest) {
    for (T item : source) {  // source = Producer (extends)
        dest.add(item);       // dest = Consumer (super)
    }
}

List<Integer> source = List.of(1, 2, 3);
List<Number> dest = new ArrayList<>();
copy(source, dest);  // ✅ Works perfectly
```

**Short Interview Answer:**

PECS: **Producer Extends, Consumer Super**. Data read karna ho toh `? extends T`, write karna ho toh `? super T`. Dono read+write chahiye toh `<T>` type parameter use karo, wildcard nahi.

---

## Q6: Raw Types kya hain? Heap Pollution ka risk kya hai?

**Answer:**

**Raw Type** = generic class bina type argument ke use karna (`List list` instead of `List<String>`). Type safety completely lost ho jaati hai.

### Raw Type Problem:

```java
// ✅ Parameterized — type-safe
List<String> stringList = new ArrayList<>();
stringList.add("Hello");

// ❌ Raw type — type safety lost
List rawList = stringList;  // Raw type reference
rawList.add(123);  // ❌ Integer add ho gaya — compile error nahi!

for (String s : stringList) {  // ❌ ClassCastException at runtime!
    System.out.println(s.toUpperCase());
}
```

### Heap Pollution:

**Heap Pollution** tab hoti hai jab runtime pe collection mein galat type ka object store ho jaata hai — compile-time pe catch nahi hota.

```java
// Heap pollution example
List<String> strings = new ArrayList<>();
List raw = strings;       // Raw type se pollution
raw.add(42);              // Integer heap pe store ho gaya

String s = strings.get(1); // ClassCastException — heap polluted!
```

### @SafeVarargs and Varargs:

```java
// Varargs + generics = heap pollution risk
public static <T> void addToList(List<T> list, T... elements) {
    for (T element : elements) {
        list.add(element);
    }
}

// Object[] internally ban sakta hai — unsafe
```

### Best Practices:

```java
// ✅ Always use parameterized types
List<String> list = new ArrayList<>();
Map<String, Integer> map = new HashMap<>();

// ❌ Never use raw types in new code
List rawList = new ArrayList();  // Compiler warning

// Legacy code mein unavoidable ho toh @SuppressWarnings carefully use karo
```

**Important:**

Raw types sirf legacy Java 5 se pehle ke code ke saath compatibility ke liye hain. Naye code mein hamesha parameterized types use karo — interview mein yeh clearly bolo ki raw types avoid karte ho.

---

## Q7: Generic Interfaces kaise implement karte hain? Comparable<T> example do.

**Answer:**

Generic interfaces type parameter define karte hain — implementing class concrete type specify karti hai ya apna type parameter maintain karti hai.

### Generic Interface Declaration:

```java
interface Repository<T> {
    void save(T entity);
    T findById(Long id);
    List<T> findAll();
    void delete(T entity);
}
```

### Concrete Type Implementation:

```java
class UserRepository implements Repository<User> {
    private List<User> users = new ArrayList<>();

    @Override
    public void save(User user) { users.add(user); }

    @Override
    public User findById(Long id) {
        return users.stream()
            .filter(u -> u.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<User> findAll() { return new ArrayList<>(users); }

    @Override
    public void delete(User user) { users.remove(user); }
}
```

### Generic Implementation (Type Parameter Maintain):

```java
class InMemoryRepository<T> implements Repository<T> {
    private List<T> items = new ArrayList<>();

    @Override
    public void save(T entity) { items.add(entity); }

    @Override
    public T findById(Long id) { return items.isEmpty() ? null : items.get(0); }

    @Override
    public List<T> findAll() { return new ArrayList<>(items); }

    @Override
    public void delete(T entity) { items.remove(entity); }
}

Repository<User> userRepo = new InMemoryRepository<>();
Repository<Product> productRepo = new InMemoryRepository<>();
```

### Comparable<T> — Built-in Generic Interface:

```java
// Comparable — natural ordering define karta hai
class Employee implements Comparable<Employee> {
    private String name;
    private int salary;

    public Employee(String name, int salary) {
        this.name = name;
        this.salary = salary;
    }

    @Override
    public int compareTo(Employee other) {
        return Integer.compare(this.salary, other.salary);
    }
}

// Generic sorting — Comparable bound
public static <T extends Comparable<T>> T findMax(List<T> list) {
    T max = list.get(0);
    for (T item : list) {
        if (item.compareTo(max) > 0) {
            max = item;
        }
    }
    return max;
}

List<Employee> employees = List.of(
    new Employee("Sachin", 50000),
    new Employee("Rahul", 70000)
);
Employee highestPaid = findMax(new ArrayList<>(employees));
```

**Short Interview Answer:**

Generic interface implement karte waqt ya toh concrete type specify karo (`implements Repository<User>`) ya generic implementation banao (`class InMemoryRepository<T> implements Repository<T>`). `Comparable<T>` sabse common example hai — sorting ke liye natural order define karta hai.

---

## Q8: Wildcard Capture kya hai? Kab use karte hain?

**Answer:**

**Wildcard Capture** tab hota hai jab compiler `List<?>` ko internally ek specific type `T` mein convert karta hai — helper generic method ke through.

### Problem — Wildcard Directly Use Nahi Kar Sakte:

```java
// ❌ Wildcard list pe generic operations nahi kar sakte
public static void reverse(List<?> list) {
    // T temp = list.get(i);  // ❌ ? capture nahi ho sakta directly
    reverseHelper(list);  // ❌ Without helper — won't compile
}
```

### Solution — Helper Method:

```java
// ✅ Public method — wildcard accept karta hai
public static void reverse(List<?> list) {
    reverseHelper(list);  // ✅ Compiler wildcard capture karta hai
}

// ✅ Private helper — specific type T capture karta hai
private static <T> void reverseHelper(List<T> list) {
    for (int i = 0; i < list.size() / 2; i++) {
        int j = list.size() - 1 - i;
        T temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }
}

// Usage
List<String> names = new ArrayList<>(List.of("A", "B", "C", "D"));
reverse(names);
System.out.println(names);  // [D, C, B, A]
```

### When Capture Fails:

```java
// ❌ Two independent wildcards — capture nahi hota
public static void merge(List<?> list1, List<?> list2) {
    // mergeHelper(list1, list2);  // ❌ Error! Two different ? types
}

// ✅ Solution — same type parameter
public static <T> void merge(List<T> list1, List<T> list2) {
    list1.addAll(list2);
}
```

### Decision Guide:

```
Wildcard chahiye?
├── Type matter nahi, sirf read (Object methods)? → <?>
├── Data read/produce (T ya subtype)?             → <? extends T>
├── Data write/consume (T ya supertype)?          → <? super T>
└── Dono read + write?                            → <T> (type parameter)
```

---

## Q9: Bridge Methods kya hain? Type erasure se kaise related hain?

**Answer:**

**Bridge Methods** compiler-generated synthetic methods hain jo type erasure ke baad method overriding ko preserve karte hain.

### Problem Without Bridge Methods:

```java
class Node<T> {
    public T data;
    public void setData(T data) { this.data = data; }
}

class MyNode extends Node<String> {
    @Override
    public void setData(String data) {  // Override setData(T) where T=String
        this.data = data;
    }
}

// After erasure:
// Node: setData(Object data)
// MyNode: setData(String data)  ← Different signature! Override nahi hua!
```

### Compiler Solution — Bridge Method:

```java
// Compiler generates (conceptually):
class MyNode extends Node {
    public void setData(String data) { this.data = data; }

    // Bridge method — synthetic, compiler-generated
    public void setData(Object data) {
        setData((String) data);  // Delegates to actual method
    }
}
```

### Generic Interface Example:

```java
interface Comparator<T> {
    int compare(T o1, T o2);
}

class StringComparator implements Comparator<String> {
    @Override
    public int compare(String o1, String o2) {
        return o1.compareTo(o2);
    }
    // Bridge: int compare(Object o1, Object o2) { return compare((String)o1, (String)o2); }
}
```

**Important:**

Bridge methods automatically generate hote hain — tumhe manually likhne ki zarurat nahi. Interview mein bas itna bolo: type erasure ke baad generic signatures erase ho jaate hain, isliye compiler bridge methods banata hai taaki polymorphism break na ho.

**Short Interview Answer:**

Bridge methods are compiler-generated synthetic methods that preserve polymorphism after type erasure. Example: `MyNode extends Node<String>` gets a bridge `setData(Object)` that delegates to `setData(String)`.

---

## Q10: Generics interview mein common mistakes aur best practices kya hain?

**Answer:**

### Common Mistakes:

| Mistake | Problem | Fix |
|---------|---------|-----|
| Raw types use karna | Heap pollution, ClassCastException | Always parameterized types |
| `? extends` mein write karna | Compile error | PECS — Consumer Super |
| `List<Object>` vs `List<?>` confuse karna | `List<Object>` accepts only Object | `List<?>` accepts any List |
| Generic array banana | Heap pollution | `ArrayList` use karo |
| Overusing wildcards | Unnecessary complexity | `<T>` when read+write both needed |

### List<Object> vs List<?>:

```java
List<Object> objectList = new ArrayList<>();
objectList.add("Hello");  // ✅
objectList.add(123);      // ✅
// List<String> strings = objectList;  // ❌ Compile error — not compatible

List<?> wildcardList = new ArrayList<String>();
// wildcardList.add("Hello");  // ❌ Cannot add anything (except null)
Object obj = wildcardList.get(0);  // ✅ Can only read as Object
```

### Best Practices:

```java
// ✅ PECS for method parameters
public static <T> void copy(List<? extends T> src, List<? super T> dest) { ... }

// ✅ Bounded types when methods needed
public static <T extends Comparable<T>> T max(List<T> list) { ... }

// ✅ Generic methods for utilities
public static <T> List<T> filter(List<T> list, Predicate<T> predicate) { ... }

// ✅ Avoid raw types completely
List<String> names = new ArrayList<>();  // ✅
// List names = new ArrayList();         // ❌

// ✅ Use @SuppressWarnings only when unavoidable (legacy interop)
```

### Quick Revision Table:

| Concept | Key Point |
|---------|-----------|
| **Generic Class `<T>`** | Type parameterized class — compile-time safety |
| **Generic Method `<T> T method()`** | Class generic na ho toh bhi method generic ho sakta hai |
| **Type Erasure** | Runtime pe generic info nahi — backward compatibility |
| **Upper Bound `<T extends X>`** | Restrict to X and subtypes |
| **PECS** | Producer Extends, Consumer Super |
| **Raw Types** | Avoid — type safety lost, heap pollution |
| **Bridge Methods** | Compiler-generated — erasure + polymorphism preserve |
| **Comparable<T>** | Most common generic interface — natural ordering |

**Short Interview Answer:**

Always use parameterized types, follow PECS for wildcards, use bounded types when you need type-specific methods, understand type erasure limitations (`new T()`, generic instanceof), and never use raw types in new code.

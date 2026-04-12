# Generic Interfaces

## Status: Not Started

---

## Table of Contents

1. [Implementing Generic Interfaces](#implementing-generic-interfaces)
2. [Functional Interfaces with Generics](#functional-interfaces-with-generics)
3. [Bounded Generic Interfaces](#bounded-generic-interfaces)
4. [Real-World Examples](#real-world-examples)

---

## Implementing Generic Interfaces

**Matlab:** Generic interface banana aur usko implement karna — type parameter implementation class ke paas specify hota hai.

### Generic Interface Declaration

```java
// Generic interface
interface Repository<T> {
    void save(T entity);
    T findById(Long id);
    List<T> findAll();
    void delete(T entity);
}
```

### Implementation with Specific Type

```java
// Implementation with concrete type
class UserRepository implements Repository<User> {
    private List<User> users = new ArrayList<>();

    @Override
    public void save(User user) {
        users.add(user);
    }

    @Override
    public User findById(Long id) {
        return users.stream()
            .filter(u -> u.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users);
    }

    @Override
    public void delete(User user) {
        users.remove(user);
    }
}

// Implementation with another type
class ProductRepository implements Repository<Product> {
    private Map<Long, Product> products = new HashMap<>();

    @Override
    public void save(Product product) {
        products.put(product.getId(), product);
    }

    @Override
    public Product findById(Long id) {
        return products.get(id);
    }

    @Override
    public List<Product> findAll() {
        return new ArrayList<>(products.values());
    }

    @Override
    public void delete(Product product) {
        products.remove(product.getId());
    }
}
```

### Implementation with Type Parameter

```java
// Generic implementation — type parameter maintain karo
class InMemoryRepository<T> implements Repository<T> {
    private List<T> items = new ArrayList<>();

    @Override
    public void save(T entity) {
        items.add(entity);
    }

    @Override
    public T findById(Long id) {
        // Generic — specific ID logic nahi hai
        return items.stream()
            .filter(item -> {
                try {
                    Method getId = item.getClass().getMethod("getId");
                    Long itemId = (Long) getId.invoke(item);
                    return itemId.equals(id);
                } catch (Exception e) {
                    return false;
                }
            })
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(items);
    }

    @Override
    public void delete(T entity) {
        items.remove(entity);
    }
}

// Usage
Repository<User> userRepo = new InMemoryRepository<>();
userRepo.save(new User("Sachin", 25));

Repository<Product> productRepo = new InMemoryRepository<>();
productRepo.save(new Product("Laptop", 50000));
```

### Multiple Type Parameters

```java
interface Cache<K, V> {
    void put(K key, V value);
    V get(K key);
    boolean containsKey(K key);
    void remove(K key);
    int size();
}

class SimpleCache<K, V> implements Cache<K, V> {
    private Map<K, V> map = new HashMap<>();

    @Override
    public void put(K key, V value) {
        map.put(key, value);
    }

    @Override
    public V get(K key) {
        return map.get(key);
    }

    @Override
    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    @Override
    public void remove(K key) {
        map.remove(key);
    }

    @Override
    public int size() {
        return map.size();
    }
}

// Usage
Cache<String, User> userCache = new SimpleCache<>();
userCache.put("sachin", new User("Sachin", 25));
User user = userCache.get("sachin");

Cache<Integer, Product> productCache = new SimpleCache<>();
productCache.put(1, new Product("Laptop", 50000));
Product product = productCache.get(1);
```

---

## Functional Interfaces with Generics

**Matlab:** Generic interfaces jisme sirf ek abstract method ho — lambda expressions ke saath use hote hain.

### Built-in Functional Interfaces

```java
// Function<T, R> — T input, R output
Function<String, Integer> parse = Integer::parseInt;
Integer num = parse.apply("123");  // 123

// Consumer<T> — T input, no return
Consumer<String> print = System.out::println;
print.accept("Hello");

// Supplier<T> — no input, T output
Supplier<Double> random = Math::random;
Double value = random.get();

// Predicate<T> — T input, boolean output
Predicate<String> isEmpty = String::isEmpty;
boolean result = isEmpty.test("");  // true

// UnaryOperator<T> — T input, T output
UnaryOperator<Integer> doubleIt = n -> n * 2;
Integer doubled = doubleIt.apply(5);  // 10

// BinaryOperator<T> — T, T input, T output
BinaryOperator<Integer> add = (a, b) -> a + b;
Integer sum = add.apply(5, 10);  // 15
```

### Custom Generic Functional Interface

```java
@FunctionalInterface
interface Converter<F, T> {
    T convert(F from);
}

// Usage with lambda
Converter<String, Integer> stringToInt = Integer::parseInt;
Integer num = stringToInt.convert("456");  // 456

Converter<Double, String> doubleToString = d -> String.format("%.2f", d);
String str = doubleToString.convert(3.14159);  // "3.14"

// Method reference
Converter<String, LocalDate> stringToDate = LocalDate::parse;
LocalDate date = stringToDate.convert("2024-01-15");
```

### Custom Functional Interface with Bounds

```java
@FunctionalInterface
interface Validator<T> {
    boolean validate(T value);
}

// Usage
Validator<String> notEmpty = s -> s != null && !s.isBlank();
Validator<Integer> positive = n -> n > 0;
Validator<User> adult = u -> u != null && u.getAge() >= 18;

// Combined validators
Validator<String> emailValidator = s -> s != null && s.contains("@");
Validator<String> passwordValidator = s -> s != null && s.length() >= 8;

// Chain validators
Validator<String> combined = s -> notEmpty.validate(s) && emailValidator.validate(s);
```

### Factory Pattern with Generics

```java
@FunctionalInterface
interface Factory<T> {
    T create();
}

// Usage
Factory<User> userFactory = () -> new User("Unknown", 0);
Factory<List<String>> listFactory = ArrayList::new;
Factory<Random> randomFactory = Random::new;

User user = userFactory.create();
List<String> list = listFactory.create();
Random random = randomFactory.create();
```

### Transformer Pattern

```java
@FunctionalInterface
interface Transformer<T, R> {
    R transform(T input);
}

// Usage
Transformer<String, Integer> lengthTransformer = String::length;
Transformer<User, String> nameExtractor = User::getName;
Transformer<Product, Double> priceExtractor = Product::getPrice;

// Chain transformers
Transformer<String, Integer> strToInt = Integer::parseInt;
Transformer<Integer, String> intToBinary = n -> Integer.toBinaryString(n);

// Compose
Transformer<String, String> composed = s -> intToBinary.transform(strToInt.transform(s));
String binary = composed.transform("10");  // "1010"
```

---

## Bounded Generic Interfaces

**Matlab:** Interface ke type parameters pe bounds lagana — implementors ko specific contracts follow karne honge.

### Interface with Upper Bound

```java
// Interface with bounded type parameter
interface SortableList<T extends Comparable<T>> {
    void add(T item);
    void sort();
    List<T> getSortedList();
}

// Implementation
class MySortableList<T extends Comparable<T>> implements SortableList<T> {
    private List<T> items = new ArrayList<>();

    @Override
    public void add(T item) {
        items.add(item);
    }

    @Override
    public void sort() {
        Collections.sort(items);  // ✅ sort() ke liye Comparable chahiye
    }

    @Override
    public List<T> getSortedList() {
        return new ArrayList<>(items);
    }
}

// Usage
SortableList<String> stringList = new MySortableList<>();
stringList.add("Banana");
stringList.add("Apple");
stringList.add("Cherry");
stringList.sort();
System.out.println(stringList.getSortedList());  // [Apple, Banana, Cherry]

SortableList<Integer> intList = new MySortableList<>();
intList.add(5);
intList.add(2);
intList.add(8);
intList.sort();
System.out.println(intList.getSortedList());  // [2, 5, 8]
```

### Interface with Multiple Bounds

```java
// Interface with multiple bounds on type parameter
interface SerializableProcessor<T extends Serializable & Comparable<T>> {
    T process(T input);
    byte[] serialize(T item) throws IOException;
}

// Implementation
class StringProcessor implements SerializableProcessor<String> {
    @Override
    public String process(String input) {
        return input.toUpperCase();
    }

    @Override
    public byte[] serialize(String item) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(item);
            return baos.toByteArray();
        }
    }
}
```

### Bounded Functional Interface

```java
@FunctionalInterface
interface BoundedFunction<T extends Number, R> {
    R apply(T value);
}

// Usage
BoundedFunction<Integer, String> intToString = n -> "Number: " + n;
String result = intToString.apply(42);  // "Number: 42"

BoundedFunction<Double, Boolean> isPositive = d -> d > 0;
Boolean positive = isPositive.apply(3.14);  // true
```

---

## Real-World Examples

### Generic Repository Pattern

```java
// Generic repository interface
interface BaseRepository<T, ID extends Serializable> {
    Optional<T> findById(ID id);
    List<T> findAll();
    T save(T entity);
    void deleteById(ID id);
    long count();
    boolean existsById(ID id);
}

// Implementation
class JpaBaseRepository<T, ID extends Serializable> implements BaseRepository<T, ID> {
    private final EntityManager em;
    private final Class<T> entityClass;

    public JpaBaseRepository(EntityManager em, Class<T> entityClass) {
        this.em = em;
        this.entityClass = entityClass;
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(em.find(entityClass, id));
    }

    @Override
    public List<T> findAll() {
        return em.createQuery("SELECT e FROM " + entityClass.getSimpleName() + " e", entityClass)
            .getResultList();
    }

    @Override
    public T save(T entity) {
        if (em.contains(entity)) {
            return em.merge(entity);
        } else {
            em.persist(entity);
            return entity;
        }
    }

    @Override
    public void deleteById(ID id) {
        findById(id).ifPresent(em::remove);
    }

    @Override
    public long count() {
        return em.createQuery("SELECT COUNT(e) FROM " + entityClass.getSimpleName() + " e", Long.class)
            .getSingleResult();
    }

    @Override
    public boolean existsById(ID id) {
        return findById(id).isPresent();
    }
}

// Usage
BaseRepository<User, Long> userRepo = new JpaBaseRepository<>(em, User.class);
BaseRepository<Product, Long> productRepo = new JpaBaseRepository<>(em, Product.class);
```

### Generic Strategy Pattern

```java
// Generic strategy interface
interface SortingStrategy<T extends Comparable<T>> {
    List<T> sort(List<T> list);
}

// Implementations
class QuickSortStrategy<T extends Comparable<T>> implements SortingStrategy<T> {
    @Override
    public List<T> sort(List<T> list) {
        List<T> sorted = new ArrayList<>(list);
        quickSort(sorted, 0, sorted.size() - 1);
        return sorted;
    }

    private void quickSort(List<T> list, int low, int high) {
        if (low < high) {
            int pi = partition(list, low, high);
            quickSort(list, low, pi - 1);
            quickSort(list, pi + 1, high);
        }
    }

    private int partition(List<T> list, int low, int high) {
        T pivot = list.get(high);
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (list.get(j).compareTo(pivot) <= 0) {
                i++;
                T temp = list.get(i);
                list.set(i, list.get(j));
                list.set(j, temp);
            }
        }
        T temp = list.get(i + 1);
        list.set(i + 1, list.get(high));
        list.set(high, temp);
        return i + 1;
    }
}

class MergeSortStrategy<T extends Comparable<T>> implements SortingStrategy<T> {
    @Override
    public List<T> sort(List<T> list) {
        List<T> sorted = new ArrayList<>(list);
        mergeSort(sorted, 0, sorted.size() - 1);
        return sorted;
    }

    private void mergeSort(List<T> list, int left, int right) {
        if (left < right) {
            int mid = (left + right) / 2;
            mergeSort(list, left, mid);
            mergeSort(list, mid + 1, right);
            merge(list, left, mid, right);
        }
    }

    private void merge(List<T> list, int left, int mid, int right) {
        List<T> temp = new ArrayList<>();
        int i = left, j = mid + 1;
        while (i <= mid && j <= right) {
            if (list.get(i).compareTo(list.get(j)) <= 0) {
                temp.add(list.get(i++));
            } else {
                temp.add(list.get(j++));
            }
        }
        while (i <= mid) temp.add(list.get(i++));
        while (j <= right) temp.add(list.get(j++));
        for (int k = 0; k < temp.size(); k++) {
            list.set(left + k, temp.get(k));
        }
    }
}

// Context class
class Sorter<T extends Comparable<T>> {
    private SortingStrategy<T> strategy;

    public Sorter(SortingStrategy<T> strategy) {
        this.strategy = strategy;
    }

    public void setStrategy(SortingStrategy<T> strategy) {
        this.strategy = strategy;
    }

    public List<T> sort(List<T> list) {
        return strategy.sort(list);
    }
}

// Usage
Sorter<Integer> sorter = new Sorter<>(new QuickSortStrategy<>());
List<Integer> nums = List.of(5, 2, 8, 1, 9);
System.out.println(sorter.sort(new ArrayList<>(nums)));  // [1, 2, 5, 8, 9]

sorter.setStrategy(new MergeSortStrategy<>());
System.out.println(sorter.sort(new ArrayList<>(nums)));  // [1, 2, 5, 8, 9]
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Generic Interface** | Interface with type parameter — implementation class type specify karti hai |
| **Implementation with Type** | `class UserRepository implements Repository<User>` |
| **Generic Implementation** | `class InMemoryRepository<T> implements Repository<T>` |
| **Functional Interfaces** | Single abstract method + generics — lambda friendly |
| **Bounded Generic Interface** | Type parameter pe bounds — implementors ko contracts follow karne honge |
| **PECS Rule** | Producer Extends, Consumer Super — wildcards ke liye |
| **Strategy Pattern** | Generic interfaces se reusable, type-safe strategies bana sakte ho |
| **Repository Pattern** | Generic repository se DRY code — sab entities ke liye same interface |

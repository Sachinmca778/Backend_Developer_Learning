# Comparable vs Comparator

## Status: Not Started

---

## Table of Contents

1. [Sorting Frameworks Overview](#sorting-frameworks-overview)
2. [`Comparable` — Natural Ordering](#comparable--natural-ordering)
3. [`Comparator` — External Ordering](#comparator--external-ordering)
4. [`Comparator.comparing()` Fluent](#comparatorcomparing-fluent)
5. [`thenComparing` & `reversed`](#thencomparing--reversed)
6. [Null Handling](#null-handling)
7. [Primitives Specializations](#primitives-specializations)
8. [`Arrays.sort` vs `Collections.sort` vs `List.sort`](#arrayssort-vs-collectionssort-vs-listsort)
9. [Stable Sort & Algorithms](#stable-sort--algorithms)
10. [`compareTo` vs `equals` Consistency](#compareto-vs-equals-consistency)
11. [Common Interview Qs](#common-interview-qs)
12. [Pitfalls](#pitfalls)
13. [Cheat Sheet](#cheat-sheet)

---

## Sorting Frameworks Overview

Java mein 2 abstractions:

| | Comparable | Comparator |
|--|-----------|------------|
| Defined | Inside class | Outside class |
| Method | `int compareTo(T)` | `int compare(T, T)` |
| Ordering | Single (natural) | Many possible |
| Use | "Default" sort | Per-context sort |
| Common types | `String`, `Integer`, `Date` | Custom rules |

---

## `Comparable` — Natural Ordering

```java
public class Employee implements Comparable<Employee> {
    private final String name;
    private final int age;
    
    @Override
    public int compareTo(Employee o) {
        return Integer.compare(this.age, o.age);
    }
}

List<Employee> list = ...;
Collections.sort(list);    // uses Employee.compareTo
```

### Contract

| Property | Meaning |
|----------|---------|
| **Sign**: positive / zero / negative | this > o, equal, this < o |
| **Reverse**: `sgn(a.compareTo(b)) == -sgn(b.compareTo(a))` |
| **Transitive**: a > b > c → a > c |
| **Consistent**: stable across calls |
| **Equals consistency** (recommended): `a.compareTo(b) == 0` ↔ `a.equals(b)` |

### `Integer.compare` instead of subtraction

```java
return Integer.compare(this.age, o.age);   // ✅
return this.age - o.age;                    // ❌ overflow risk on huge ints
```

---

## `Comparator` — External Ordering

```java
Comparator<Employee> byName = (a, b) -> a.getName().compareTo(b.getName());

list.sort(byName);
```

→ Class ko modify nahi karna padta. Multiple orderings define kar sakte ho per use-site.

### When?

- Class control nahi (`String`, library types)
- Multiple sort criteria
- Reusable ordering objects

---

## `Comparator.comparing()` Fluent

```java
Comparator<Employee> byName = Comparator.comparing(Employee::getName);
Comparator<Employee> byAge  = Comparator.comparingInt(Employee::getAge);
Comparator<Employee> bySal  = Comparator.comparingDouble(Employee::getSalary);
```

→ Method reference se bahut readable + null-friendly extensions.

---

## `thenComparing` & `reversed`

```java
Comparator<Employee> cmp = Comparator
    .comparing(Employee::getDepartment)
    .thenComparing(Employee::getName)
    .thenComparingInt(Employee::getAge)
    .reversed();             // reverses entire chain
```

→ `reversed()` **whole comparator** flip. Ek field ko reverse karna ho:

```java
.thenComparing(Comparator.comparing(Employee::getName).reversed())
```

---

## Null Handling

```java
Comparator<String> nullsFirst = Comparator.nullsFirst(Comparator.naturalOrder());
Comparator<String> nullsLast  = Comparator.nullsLast(Comparator.naturalOrder());

List<String> list = new ArrayList<>(Arrays.asList("b", null, "a"));
list.sort(nullsFirst);   // [null, a, b]
```

→ Bina wrap kiye `null` ho sorted list mein → NPE inside `compareTo`.

---

## Primitives Specializations

| Method | Use |
|--------|-----|
| `comparingInt` | int-returning extractor |
| `comparingLong` | long |
| `comparingDouble` | double |

→ Avoid autoboxing in hot loops.

```java
list.sort(Comparator.comparingInt(Employee::getAge));
```

---

## `Arrays.sort` vs `Collections.sort` vs `List.sort`

```java
Arrays.sort(arr);                       // primitives or Object[]
Arrays.sort(arr, comparator);           // Object[] + comparator

Collections.sort(list);                 // natural order
Collections.sort(list, comparator);     // delegates to list.sort

list.sort(null);                        // natural order (Java 8+)
list.sort(comparator);
```

### Algorithms

- **Primitives**: Dual-Pivot Quicksort (`Arrays.sort(int[])`)
- **Objects**: TimSort (stable, adaptive)

---

## Stable Sort & Algorithms

**Stable** = equal elements ka **relative order preserved**.

| Method | Stable? |
|--------|---------|
| `Arrays.sort(int[])` (Quicksort) | ❌ |
| `Arrays.sort(Object[])` (TimSort) | ✅ |
| `Collections.sort` / `List.sort` | ✅ |
| `Stream.sorted()` | ✅ |

### Why care?

Multi-key sorts: agar primary equal hai, **previous sort order preserve** hota — chain ke liye crucial.

---

## `compareTo` vs `equals` Consistency

> "If `a.compareTo(b) == 0`, ideally `a.equals(b) == true`."

### Trap with `TreeSet` / `TreeMap`

These use `compareTo` (or comparator) to determine "equal":

```java
class Point implements Comparable<Point> {
    int x, y;
    public int compareTo(Point p) { return Integer.compare(x, p.x); }   // only x!
}

Set<Point> s = new TreeSet<>();
s.add(new Point(1, 1));
s.add(new Point(1, 2));   // not added — compareTo == 0!
s.size();                  // 1
```

→ `TreeSet` me "equal" === `compareTo == 0`. Surprise behaviour for users expecting `equals` semantics.

→ Solution: `compareTo` should be **consistent with equals** — break ties on additional fields.

---

## Common Interview Qs

### Q1. Comparable banae ya Comparator?

| Situation | Choice |
|-----------|--------|
| Class apni hai + ek natural order | Comparable |
| Class third-party | Comparator |
| Multiple orderings same class | Comparator(s) |

### Q2. Subtraction kyon avoid?

```java
int compareTo(Other o) { return this.age - o.age; }   // overflow if extreme values
```

→ `Integer.compare(a, b)` overflow-safe.

### Q3. Reverse sort?

```java
list.sort(Comparator.reverseOrder());                  // natural reverse
list.sort(Comparator.comparingInt(E::getAge).reversed());
```

### Q4. Null-safe comparator?

```java
Comparator.nullsFirst(Comparator.naturalOrder());
```

### Q5. Sort stability importance?

Multi-pass sort (e.g., first by age, then by salary) → stability ensures inner order preserved.

### Q6. `TreeSet` sort with comparator?

```java
new TreeSet<>(Comparator.comparing(Person::getName));
```

→ Comparator overrides natural ordering for that set.

---

## Pitfalls

1. **Subtraction overflow** — use `Integer.compare`.
2. **Inconsistent with equals** + `TreeSet` → silent dedup.
3. **Mutating sort key** post-add → broken ordering.
4. **`reversed()` on chain** vs single field — read carefully.
5. **`null` in comparator without wrap** → NPE.
6. **Sorting unmodifiable list** (`List.of(...)`) → `UnsupportedOperationException`.
7. **Comparator violating contract** (asymmetric, non-transitive) — `IllegalArgumentException` ("Comparison method violates its general contract!") at runtime.
8. **Mixing `int` extractor with `comparing` not `comparingInt`** — autoboxing perf.

---

## Cheat Sheet

| Need | API |
|------|-----|
| Natural order | `Comparable.compareTo` |
| External order | `Comparator.compare` |
| Field-based | `Comparator.comparing(...)` |
| Multi-key | `.thenComparing(...)` |
| Reverse | `.reversed()` / `Comparator.reverseOrder()` |
| Null-safe | `Comparator.nullsFirst/last` |
| Primitive | `comparingInt/Long/Double` |
| Stable sort | `Arrays.sort(Object[])` / `List.sort` |

| Sort | Stable? |
|------|---------|
| Object[] / List | ✅ TimSort |
| primitives | ❌ Quicksort |
| Stream sorted | ✅ |

---

## Practice

1. `Employee` class — Comparable by age; Comparator by name; chained dept→name→age.
2. Show subtraction overflow with `Integer.MAX_VALUE` and `MIN_VALUE`.
3. TreeSet with `compareTo` only on `x` field — observe missing element.
4. Sort with `nullsFirst`; remove wrap → NPE.
5. Stable sort demo: sort by primary then secondary; flip order; observe.

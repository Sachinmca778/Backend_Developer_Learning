# Equals & HashCode Contract

## Status: Not Started

---

## Table of Contents

1. [Yeh Kyon Asked Hai?](#yeh-kyon-asked-hai)
2. [`equals` — 5 Rules](#equals--5-rules)
3. [`hashCode` Contract](#hashcode-contract)
4. [`equals` Skeleton](#equals-skeleton)
5. [`hashCode` Skeleton](#hashcode-skeleton)
6. [Inheritance Trap (Liskov)](#inheritance-trap-liskov)
7. [Mutable Keys = Disaster](#mutable-keys--disaster)
8. [Records (Java 14+)](#records-java-14)
9. [Lombok `@EqualsAndHashCode`](#lombok-equalsandhashcode)
10. [JPA Entities — Special Care](#jpa-entities--special-care)
11. [Common Interview Outputs](#common-interview-outputs)
12. [Pitfalls](#pitfalls)
13. [Cheat Sheet](#cheat-sheet)

---

## Yeh Kyon Asked Hai?

`equals` / `hashCode` galat → `HashSet`, `HashMap`, `distinct()`, dedup logic — **sab break**. Real production bugs from this.

---

## `equals` — 5 Rules

For non-null `x`, `y`, `z`:

| Rule | Meaning |
|------|---------|
| **Reflexive** | `x.equals(x) == true` |
| **Symmetric** | `x.equals(y) == y.equals(x)` |
| **Transitive** | `x.equals(y) && y.equals(z)` → `x.equals(z)` |
| **Consistent** | Repeated calls — same result if no fields changed |
| **Null** | `x.equals(null) == false` |

---

## `hashCode` Contract

1. Same object multiple calls → same hash (consistent within run; not across JVMs).
2. **Equal objects → equal hashCode** (mandatory).
3. Unequal objects **may** have equal hashCode (collisions allowed).

> "Override `equals` → override `hashCode` together. Always." — Effective Java.

---

## `equals` Skeleton

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof User u)) return false;     // pattern matching (Java 16+)
    return Objects.equals(email, u.email);        // null-safe
}
```

### Steps (interview narration)

1. Same reference → fast true
2. `null` / wrong type → false
3. Cast (or pattern match)
4. Compare significant fields with `Objects.equals` / `==`

### `instanceof` vs `getClass()`

| Choice | Pro | Con |
|--------|-----|-----|
| `instanceof` | Allows subclass equality | Symmetry tricky |
| `getClass() ==` | Strict — symmetric | Subclasses considered different |

→ For **value objects**: `getClass()` strict often safer. For sealed hierarchies: `instanceof` fine.

---

## `hashCode` Skeleton

```java
@Override
public int hashCode() {
    return Objects.hash(email);
}
```

For multi-field:

```java
return Objects.hash(firstName, lastName, dob);
```

### Manual (faster, less garbage in hot loops)

```java
int result = 17;
result = 31 * result + (email == null ? 0 : email.hashCode());
result = 31 * result + age;
return result;
```

(`Objects.hash(...)` internally creates a varargs array.)

---

## Inheritance Trap (Liskov)

```java
class Point {
    int x, y;
    public boolean equals(Object o) {
        if (!(o instanceof Point p)) return false;
        return p.x == x && p.y == y;
    }
}

class ColorPoint extends Point {
    Color c;
    // also overrides equals comparing color too...
}
```

```java
Point p = new Point(1, 1);
ColorPoint cp = new ColorPoint(1, 1, RED);

p.equals(cp);    // true (Point only checks x,y)
cp.equals(p);    // false (ColorPoint checks color)
// SYMMETRY broken!
```

### Fix Options

1. **`getClass()`** instead of `instanceof` — but breaks Liskov if accept Point everywhere.
2. **Composition** instead of inheritance — favour HAS-A.
3. **Abstract `Point` + concrete subclasses with `final`** equals.

→ Effective Java Item 10: "There is no way to extend an instantiable class and add a value component while preserving equals contract."

---

## Mutable Keys = Disaster

```java
class Box { int n; ...   // hashCode based on n }

Box b = new Box(1);
Map<Box, String> m = new HashMap<>();
m.put(b, "X");
b.n = 2;             // mutated key!
m.get(b);            // null — bucket different now
m.containsKey(b);    // false
```

→ **Hash-keyed objects ko effectively immutable** rakho. Cross-ref `02-HashMap-Internals.md`.

---

## Records (Java 14+)

```java
public record Point(int x, int y) {}
```

→ Auto-generated:
- Canonical constructor
- Accessors (`x()`, `y()`)
- `equals` / `hashCode` based on **all components**
- `toString`

**Pro:** Boilerplate khatm; **immutable** by default.

```java
record User(String email, String name) {}

new User("a@x", "A").equals(new User("a@x", "A"));  // true
```

⚠️ Records implicitly final — extension nahi.

---

## Lombok `@EqualsAndHashCode`

### Default

```java
@EqualsAndHashCode
public class User {
    String email;
    String name;
}
```

→ All non-static, non-transient fields used.

### Selective

```java
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {
    @EqualsAndHashCode.Include String email;
    String tempCache;   // ignored
}
```

### `@Data` includes `@EqualsAndHashCode` (and getters/setters/`@ToString`/etc.) — be careful with mutable + hash keys.

### `callSuper`

```java
@EqualsAndHashCode(callSuper = true)
class Manager extends Employee { ... }
```

Default `false` — explicit set karo agar inheritance involved.

---

## JPA Entities — Special Care

Common bug: default Lombok / IDE-generated `equals`/`hashCode` based on **all fields including `id`**.

### Problems

1. `id` `null` before persist → two transient entities both `id=null` may collide / equal accidentally.
2. After persist, hashCode changes (`id` filled) → if entity already in `HashSet` → "lost".
3. Lazy collections → `equals` on a field can trigger lazy load + `LazyInitializationException`.

### Recommended

- Override `equals` based on **business key** (e.g., `email`, `naturalId`).
- OR — equals based on `id` but stable: assign UUID **at construction** (not by DB).

```java
@Entity
public class User {
    @Id @GeneratedValue private UUID id = UUID.randomUUID();
    private String email;
    
    @Override
    public boolean equals(Object o) {
        return o instanceof User u && id.equals(u.id);
    }
    @Override
    public int hashCode() { return id.hashCode(); }
}
```

→ Cross-ref: `phase-4 / Database Mastery` for JPA depth.

---

## Common Interview Outputs

### Q1. `HashSet` losing element

```java
Set<Box> s = new HashSet<>();
Box b = new Box(1);
s.add(b);
b.n = 2;
s.contains(b);   // false
s.remove(b);     // false — but element still in set!
```

### Q2. equals without hashCode

```java
class Foo {
    int v;
    public boolean equals(Object o) { return o instanceof Foo f && f.v == v; }
}

Set<Foo> s = new HashSet<>();
s.add(new Foo(1));
s.contains(new Foo(1));   // false! (Object.hashCode != for two new objects)
```

### Q3. `null` field

```java
Objects.equals(null, null);   // true
Objects.hash((Object) null);  // works (uses Arrays.hashCode)
```

---

## Pitfalls

1. **Override `equals`, forget `hashCode`** — Set/Map breaks.
2. **All-field equals on JPA entity** — id null + lazy fields = pain.
3. **Lombok `@Data` on entity** — same trap.
4. **Mutable hash key** — ghost entries.
5. **Symmetry violation** with inheritance.
6. **`hashCode = constant`** — all collide; technically valid but performance dead.
7. **Floating-point fields** — `==` on `double` use `Double.compare`.
8. **Arrays in equals** — `arr1 == arr2` reference; use `Arrays.equals` (and `Arrays.hashCode`).

---

## Cheat Sheet

| Rule | Meaning |
|------|---------|
| Reflexive | `x.equals(x)` |
| Symmetric | both directions |
| Transitive | chain holds |
| Consistent | repeatable |
| Null | `x.equals(null) = false` |

| Override | Always Pair |
|----------|-------------|
| `equals` | with `hashCode` |
| `hashCode` | with `equals` |
| `compareTo` | usually consistent with `equals` |

| Modern tool | Use |
|-------------|-----|
| `record` | Value objects |
| Lombok `@EqualsAndHashCode(onlyExplicitlyIncluded)` | Mutable beans |
| `Objects.equals` / `Objects.hash` | Null-safe |

---

## Practice

1. Implement bad `equals` (no hashCode) → demonstrate Set lookup fail.
2. Implement Liskov-broken inheritance + show symmetry violation.
3. Mutable key → ghost map entry.
4. Record vs Lombok class — same equals/hashCode result.
5. JPA entity: id-based equals vs field-based; observe pre/post-persist behavior.

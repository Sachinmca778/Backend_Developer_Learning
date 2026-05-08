# Java 8 Deep Questions

## Status: Not Started

---

## Table of Contents

1. [Why Java 8 Matters Still](#why-java-8-matters-still)
2. [Streams Are Lazy — Kaise?](#streams-are-lazy--kaise)
3. [Short-Circuit Operations](#short-circuit-operations)
4. [Stream Pitfalls](#stream-pitfalls)
5. [Parallel Stream Pitfalls](#parallel-stream-pitfalls)
6. [`Optional` Anti-Patterns](#optional-anti-patterns)
7. [Method Reference — 4 Types](#method-reference--4-types)
8. [Functional Interfaces Quick Map](#functional-interfaces-quick-map)
9. [Default & Static Interface Methods](#default--static-interface-methods)
10. [`Collectors` Highlights](#collectors-highlights)
11. [Streams vs Loops](#streams-vs-loops)
12. [Common Output Traps](#common-output-traps)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## Why Java 8 Matters Still

- Lambdas + streams = **most asked** modern Java topic.
- Optional + functional interfaces design choices test depth.
- Spring (WebFlux Mono/Flux, Reactor) ka mental model lambdas pe bana hai.

---

## Streams Are Lazy — Kaise?

### Mental model

```
Source → Intermediate ops (lazy) → ... → Terminal op (triggers)
```

### Intermediate ops

`filter`, `map`, `flatMap`, `peek`, `sorted`, `distinct`, `limit`, `skip` — sirf **pipeline definition** banate hain.

### Terminal ops

`forEach`, `collect`, `reduce`, `count`, `toList`, `findFirst`, `anyMatch` — yahan **actual evaluation** hota hai.

### Demo

```java
Stream.of(1, 2, 3, 4, 5)
    .peek(i -> System.out.println("seen: " + i))
    .filter(i -> i % 2 == 0)
    .findFirst();
```

```
seen: 1
seen: 2     ← stops here (short-circuit)
```

→ Lazy + short-circuit. Saare elements process nahi hote.

### Stream re-use illegal

```java
Stream<Integer> s = Stream.of(1,2,3);
s.count();
s.count();   // ❌ IllegalStateException — already consumed
```

---

## Short-Circuit Operations

| Op | Stop when |
|----|-----------|
| `findFirst` / `findAny` | First match |
| `anyMatch` | First match |
| `allMatch` / `noneMatch` | First counter-example |
| `limit(n)` | n elements |

→ Infinite streams par bhi safe (`Stream.iterate`, `Stream.generate`).

---

## Stream Pitfalls

### 1. Side effects in pipeline

```java
List<Integer> out = new ArrayList<>();
list.parallelStream().forEach(out::add);   // ❌ race condition
```

→ Use `collect(toList())`.

### 2. Modify source while streaming

```java
list.stream().forEach(list::remove);   // CME / undefined
```

### 3. Streams not collections

Cannot index, cannot multi-iterate without re-creating.

### 4. Order assumptions in unordered streams

`Set.stream()` — encounter order may differ from expectation.

---

## Parallel Stream Pitfalls

### Where execution happens

`ForkJoinPool.commonPool()` — **shared** across all parallel streams in JVM. One slow task can starve others.

### Pitfalls

1. **Shared mutable state** — race conditions.
2. **Operations order matters** — `findFirst` becomes `findAny`-ish unless ordered (`forEachOrdered` slower).
3. **Tiny work per element** — overhead dominates; sequential faster.
4. **IO inside parallel stream** — common pool pollute.
5. **Custom executor needed**:

```java
ForkJoinPool pool = new ForkJoinPool(4);
pool.submit(() -> list.parallelStream().map(...).collect(toList())).get();
```

→ For interview: parallel stream = "data-bound CPU-heavy, stateless, large list" me hi worth.

---

## `Optional` Anti-Patterns

### 1. Use as method parameter

```java
public void search(Optional<String> name) { ... }   // ❌
```

→ Use overload / nullable + Javadoc.

### 2. Use as field

```java
class User {
    private Optional<String> nick;   // ❌ not Serializable, weird semantics
}
```

→ Use nullable; Optional in **return types** primarily.

### 3. `get()` without `isPresent()`

```java
opt.get();   // can throw NoSuchElementException
```

→ Use `orElse`, `orElseThrow`, `ifPresent`, `map`.

### 4. `if (opt.isPresent()) opt.get()`

→ Functional style:

```java
opt.ifPresent(this::handle);
opt.map(this::transform).orElse(default);
```

### 5. `Optional.of(null)` — NPE

→ Use `Optional.ofNullable(...)`.

### 6. Boxing primitives

```java
Optional<Integer>   // boxes
OptionalInt         // ✅ primitive specialization
```

---

## Method Reference — 4 Types

| Type | Example | Equivalent lambda |
|------|---------|-------------------|
| Static | `Integer::parseInt` | `s -> Integer.parseInt(s)` |
| Bound instance | `system.out::println` | `x -> system.out.println(x)` |
| Unbound instance | `String::toLowerCase` | `s -> s.toLowerCase()` |
| Constructor | `ArrayList::new` | `() -> new ArrayList<>()` |

### Trap (unbound vs bound)

```java
list.stream().map(String::trim);    // unbound — applies on stream element
List<Integer> l = ...; 
... .forEach(l::add);                // bound — uses l, takes element
```

---

## Functional Interfaces Quick Map

| Interface | Signature |
|-----------|-----------|
| `Function<T,R>` | `R apply(T)` |
| `BiFunction<T,U,R>` | `R apply(T,U)` |
| `Predicate<T>` | `boolean test(T)` |
| `Consumer<T>` | `void accept(T)` |
| `Supplier<T>` | `T get()` |
| `UnaryOperator<T>` | `T apply(T)` |
| `BinaryOperator<T>` | `T apply(T,T)` |
| `Runnable` | `void run()` |
| `Callable<V>` | `V call() throws Exception` |

---

## Default & Static Interface Methods

```java
public interface Shape {
    double area();
    
    default String describe() { return "Shape with area " + area(); }
    
    static Shape unit() { return () -> 1.0; }
}
```

### Diamond rule

If two interfaces give same default method → implementing class **must override**.

```java
interface A { default String hi() { return "A"; } }
interface B { default String hi() { return "B"; } }

class C implements A, B {
    @Override public String hi() { return A.super.hi(); }
}
```

---

## `Collectors` Highlights

```java
Collectors.toList();
Collectors.toUnmodifiableList();
Collectors.toSet();
Collectors.toMap(k, v);                                   // throws on duplicate
Collectors.toMap(k, v, mergeFn);                          // duplicate handler
Collectors.groupingBy(classifier);
Collectors.groupingBy(classifier, counting());
Collectors.partitioningBy(predicate);
Collectors.joining(", ", "[", "]");
Collectors.summingInt(...);
Collectors.averagingDouble(...);
Collectors.reducing(...);
Collectors.mapping(mapper, downstream);
```

### Java 16+

```java
list.stream().toList();                  // unmodifiable
```

→ Shorter, no Collectors import.

---

## Streams vs Loops

| Aspect | Loop | Stream |
|--------|------|--------|
| Readability | Imperative | Declarative |
| Perf (tight loops) | Slightly faster | Marginal overhead |
| Parallelism | Manual | Easy (`.parallel()`) |
| Side effects | Easy | Discouraged |
| Debug | Easy step | Pipeline harder (use `peek`) |

→ Choose for clarity; perf-critical hot paths sometimes loop wins.

---

## Common Output Traps

### Q1. Stream re-use

```java
Stream<Integer> s = Stream.of(1,2,3);
s.forEach(System.out::println);
s.forEach(System.out::println);   // ❌ IllegalStateException
```

### Q2. Lazy without terminal

```java
Stream.of(1,2,3).filter(i -> { System.out.println(i); return true; });
// prints NOTHING — no terminal op
```

### Q3. `toMap` duplicate

```java
Stream.of("a", "a")
    .collect(Collectors.toMap(Function.identity(), s -> 1));
// IllegalStateException: Duplicate key a
// Fix: (a, b) -> a (merge function)
```

### Q4. Optional lazy?

```java
Optional.ofNullable(getUser()).orElse(loadDefault());   // loadDefault() ALWAYS called
Optional.ofNullable(getUser()).orElseGet(this::loadDefault);  // ✅ lazy
```

---

## Pitfalls

1. **Side effects in stream ops** — race / surprises.
2. **`parallelStream` blindly** — common pool pollution.
3. **`Optional.of(null)`** → NPE; use `ofNullable`.
4. **Stream re-use** — IllegalStateException.
5. **`forEach` over `collect`** — for transformation.
6. **No `mergeFn` in toMap** — duplicate key crash.
7. **`peek` for side effect** — JIT may skip.
8. **Boxing** — `Stream<Integer>` vs `IntStream`.
9. **`Optional` as field** — scope misuse.
10. **`Collectors.toList()` is mutable; `toUnmodifiableList` if you need immutable**.

---

## Cheat Sheet

| Concept | Note |
|---------|------|
| Lazy | Intermediate ops |
| Eager trigger | Terminal op |
| Short-circuit | findFirst, anyMatch, limit |
| Re-use stream | ❌ |
| Parallel pool | common ForkJoinPool |
| Optional | Return-type only mostly |
| Method ref types | Static / bound / unbound / constructor |
| `peek` | Debug only |
| `toList()` (Java 16+) | Unmodifiable |
| `orElseGet` vs `orElse` | Lazy vs eager |

---

## Practice

1. Lazy proof: `peek` in pipeline without terminal — observe nothing prints.
2. Re-use stream → catch `IllegalStateException`.
3. Parallel stream with shared `ArrayList::add` — observe corruption.
4. Optional anti-pattern: `Optional<String>` parameter → refactor.
5. `groupingBy(... counting())` for word frequency.
6. Custom executor for parallel stream.

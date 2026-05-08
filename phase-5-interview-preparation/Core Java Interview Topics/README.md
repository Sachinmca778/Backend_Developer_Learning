# Core Java Interview Topics

Backend interviews ke **Core Java** rapid-fire topics — String internals, HashMap internals, memory/GC, equals/hashCode, Comparable/Comparator, exceptions, Java 8 deep stuff, multithreading. **Hinglish**, **Q&A style**, code snippets + interview traps + cheat sheets.

---

## Topics & Status

| # | Topic | File | Status |
|---|-------|------|--------|
| 1 | String Internals | [01-String-Internals.md](./01-String-Internals.md) | Not Started |
| 2 | HashMap Internals | [02-HashMap-Internals.md](./02-HashMap-Internals.md) | Not Started |
| 3 | Java Memory & GC | [03-Java-Memory-and-GC.md](./03-Java-Memory-and-GC.md) | Not Started |
| 4 | Equals & HashCode Contract | [04-Equals-and-HashCode.md](./04-Equals-and-HashCode.md) | Not Started |
| 5 | Comparable vs Comparator | [05-Comparable-vs-Comparator.md](./05-Comparable-vs-Comparator.md) | Not Started |
| 6 | Exception Interview Qs | [06-Exception-Interview-Qs.md](./06-Exception-Interview-Qs.md) | Not Started |
| 7 | Java 8 Deep Questions | [07-Java-8-Deep-Questions.md](./07-Java-8-Deep-Questions.md) | Not Started |
| 8 | Multithreading Questions | [08-Multithreading-Questions.md](./08-Multithreading-Questions.md) | Not Started |

---

## What's Inside Each File?

### [01 — String Internals](./01-String-Internals.md)
**Immutability** ke 4 reasons (security, thread-safety, hashing, caching), **String pool** (kahan baithi hai modern JVM mein — heap), `new String("a")` ka actual behaviour, **`intern()`**, **`==` vs `equals()`** with examples, `+` ka **`StringBuilder` rewrite** + loop trap, **`char[]` for passwords** kyon (immutability + GC + heap dump).

### [02 — HashMap Internals](./02-HashMap-Internals.md)
Internal **`Node<K,V>[] table`**, default **capacity 16**, **load factor 0.75**, **threshold = capacity × loadFactor**, hash perturbation (`(h = key.hashCode()) ^ (h >>> 16)`), **collision** → linked list → **TREEIFY_THRESHOLD = 8** + `MIN_TREEIFY_CAPACITY = 64`, rehashing, **`ConcurrentModificationException`** ka fail-fast iterator, `HashMap` vs `LinkedHashMap` vs `TreeMap` vs `ConcurrentHashMap`.

### [03 — Java Memory & GC](./03-Java-Memory-and-GC.md)
**Stack vs Heap**, primitives kahan rehte hain (locals → stack frame, fields → heap), Method Area / Metaspace, **memory leaks** (unclosed streams, static collections, listeners not unregistered, ThreadLocal not cleared, inner classes), **GC roots**, **Strong / Soft / Weak / Phantom** references with code, escape analysis hint.

### [04 — Equals & HashCode Contract](./04-Equals-and-HashCode.md)
`equals` ka **reflexive / symmetric / transitive / consistent / null** contract, `hashCode` contract (equal objects → equal hashCode, unequal **may** collide), inheritance pitfalls (Liskov violation), **immutable fields** for hash, **records** auto, **Lombok `@EqualsAndHashCode`** — `onlyExplicitlyIncluded`, `callSuper`, JPA entity warning, IntelliJ generator notes.

### [05 — Comparable vs Comparator](./05-Comparable-vs-Comparator.md)
**Natural ordering** (`Comparable.compareTo`) vs **external ordering** (`Comparator.compare`), **`Comparator.comparing().thenComparing().reversed()`** chains, `nullsFirst` / `nullsLast`, primitive specializations (`comparingInt`), `Arrays.sort` / `Collections.sort` / `List.sort` overloads, **stable sort**, `compareTo` vs `equals` consistency (TreeSet trap).

### [06 — Exception Interview Qs](./06-Exception-Interview-Qs.md)
**Checked vs unchecked**, `Throwable` hierarchy (`Error` vs `Exception`), can you catch `Error`? — yes but don't, **`finally` vs `return`** (override trap), **try-with-resources** vs traditional finally, **suppressed** exceptions, **rethrowing + chaining**, custom exceptions design, common Spring exception translation hint.

### [07 — Java 8 Deep Questions](./07-Java-8-Deep-Questions.md)
**Streams lazy** kaise hain (intermediate ops register karte hain, terminal trigger karta hai), **short-circuit** (`findFirst`, `anyMatch`, `limit`), **parallel stream** ke pitfalls (mutable state, ordering, common ForkJoinPool, work characteristics), **Optional anti-patterns** (param, field, `get()`, `isPresent + get`), **method reference types** (4 types), `Collectors` highlights, default vs static interface methods.

### [08 — Multithreading Questions](./08-Multithreading-Questions.md)
**`volatile` vs `synchronized`** (visibility vs visibility+atomicity), **deadlock** ke 4 conditions + prevention, **livelock** / **starvation** / **race condition**, **thread-safe singleton** (DCL with volatile, Bill Pugh, enum), **producer-consumer** (`BlockingQueue`), **thread pool sizing** (CPU-bound vs IO-bound formulas), `ThreadLocal`, `CompletableFuture` quick hits.

---

## Recommended Order

```
1. String Internals       ← fundamentals
2. Equals & HashCode      ← prereq for HashMap
3. HashMap Internals      ← classic question
4. Comparable vs Comparator
5. Exception Q&A
6. Memory & GC
7. Java 8 Deep
8. Multithreading
```

---

## Interview Strategy

| Format | Approach |
|--------|----------|
| Theory | 1-line answer → details → example → trap |
| Code reasoning | Compile / runtime / output sequence |
| Whiteboard | Hash collision diagram, deadlock graph |
| Follow-ups | "Why?", "What if?", "How does Spring use it?" |

**Pro tip:** "I don't know but I'd think it works like X because Y" >> blank stare.

---

## Companion Folders

- [Phase-1 Java Fundamentals](../../phase-1-java-fundamentals/) — basics refresher
- [Performance & Optimization](../../phase-4-advanced-topics/Performance%20%26%20Optimization/) — memory tuning + GC
- [Design Patterns in Java](../../phase-4-advanced-topics/Design%20Patterns%20in%20Java/) — singleton, equals/hashCode usage
- [Concurrency](../../phase-1-java-fundamentals/) — if dedicated folder exists

---

## Status Tracker

```
[ ] 01 — String Internals
[ ] 02 — HashMap Internals
[ ] 03 — Java Memory & GC
[ ] 04 — Equals & HashCode Contract
[ ] 05 — Comparable vs Comparator
[ ] 06 — Exception Interview Qs
[ ] 07 — Java 8 Deep Questions
[ ] 08 — Multithreading Questions
```

> Interviews mein **"kyon"** ka jawab toh saath rakho — "kya" sab ko pata hota hai.

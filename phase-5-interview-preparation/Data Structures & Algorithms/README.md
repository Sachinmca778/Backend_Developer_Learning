# Data Structures & Algorithms

DSA interview preparation — **patterns + problems + complexity** in Hinglish. Java-first solutions with explanations, edge cases, and template code.

---

## Topics & Status

| # | Topic | File | Status |
|---|-------|------|--------|
| 1 | Array & String Problems | [01-Array-and-String-Problems.md](./01-Array-and-String-Problems.md) | Not Started |
| 2 | Linked List Problems | [02-Linked-List-Problems.md](./02-Linked-List-Problems.md) | Not Started |
| 3 | Stack & Queue Problems | [03-Stack-and-Queue-Problems.md](./03-Stack-and-Queue-Problems.md) | Not Started |
| 4 | Tree Problems | [04-Tree-Problems.md](./04-Tree-Problems.md) | Not Started |
| 5 | Graph Problems | [05-Graph-Problems.md](./05-Graph-Problems.md) | Not Started |
| 6 | Dynamic Programming | [06-Dynamic-Programming.md](./06-Dynamic-Programming.md) | Not Started |
| 7 | Sorting & Searching | [07-Sorting-and-Searching.md](./07-Sorting-and-Searching.md) | Not Started |

---

## What's Inside Each File?

### [01 — Array & String Problems](./01-Array-and-String-Problems.md)
**Two pointers** (opposite ends, fast-slow), **sliding window** (fixed + variable), **prefix sum**, **Kadane's algorithm** (max subarray sum), **anagram checks** (sort vs frequency map), **palindrome**, **string manipulation** (reverse, substring, char count), **matrix traversal** (spiral, diagonal, in-place rotation), **binary search on arrays** (rotated, peak element, first/last occurrence).

### [02 — Linked List Problems](./02-Linked-List-Problems.md)
**Reverse linked list** (iterative + recursive), **cycle detection** (Floyd's tortoise-hare + cycle start), **find middle** (slow-fast), **merge two sorted lists** (iterative + recursive), **remove Nth node from end** (two-pointer gap), **LRU cache** implementation (HashMap + DLL), reverse in groups of K, intersection of two lists, palindrome linked list.

### [03 — Stack & Queue Problems](./03-Stack-and-Queue-Problems.md)
**Valid parentheses** (stack-based), **stack using queues** + **queue using stacks**, **monotonic stack** (next greater/smaller), **sliding window maximum** (deque-based O(n)), **BFS using queue**, **level-order traversal**, min-stack design, evaluate RPN, daily temperatures, largest rectangle in histogram.

### [04 — Tree Problems](./04-Tree-Problems.md)
**Inorder/Preorder/Postorder** (recursive + iterative with stack), **BFS level-order** (queue), **tree height/depth**, **LCA** (Lowest Common Ancestor — binary tree + BST), **BST operations** (search, insert, delete, validate), **balanced tree check**, **path sum** (root-to-leaf, any path), tree to linked list flattening, serialize/deserialize, diameter, vertical order traversal.

### [05 — Graph Problems](./05-Graph-Problems.md)
**BFS / DFS** templates (adj list + matrix), **topological sort** (Kahn's BFS + DFS-based), **cycle detection** (directed + undirected), **Union-Find** (DSU with path compression + union by rank), **Dijkstra** (PriorityQueue), **Bellman-Ford** (negative weights), **number of islands** (BFS/DFS/Union-Find), **course schedule**, clone graph, word ladder, network delay time.

### [06 — Dynamic Programming](./06-Dynamic-Programming.md)
**Memoization vs tabulation**, top-down recursion + memo, bottom-up table, **1D DP** (Fibonacci, climbing stairs, house robber, max product subarray), **2D DP** (unique paths, edit distance, LCS, longest palindromic subsequence), **knapsack** (0/1, unbounded), **coin change** (min coins, total ways), **DP on strings** (regex matching, wildcard, word break), state transition cheat sheet.

### [07 — Sorting & Searching](./07-Sorting-and-Searching.md)
**QuickSort** (Lomuto + Hoare partition, randomization, in-place), **MergeSort** (stable, O(n log n) guarantee), **HeapSort**, complexity comparison, **binary search variations** (first/last occurrence, search in rotated sorted array, peak element, search insert position), **counting sort**, **radix sort**, when to use each, Java's `Arrays.sort` (TimSort vs Dual-Pivot QuickSort).

---

## Recommended Order

```
1. Array & String        ← foundation patterns (two pointers, sliding window)
2. Linked List           ← pointer manipulation
3. Stack & Queue         ← linear DS + monotonic patterns
4. Tree                  ← recursion + traversals
5. Graph                 ← BFS/DFS extensions + Union-Find
6. Dynamic Programming   ← optimization patterns (hardest)
7. Sorting & Searching   ← classic algos + complexity
```

→ DP last because it builds on recursion + understanding of optimal substructure.

---

## Pattern → Problem Cheat Sheet

| Pattern | When to use | Examples |
|---------|-------------|----------|
| Two pointers | Sorted array / palindrome / pair sum | 2Sum sorted, container with most water |
| Sliding window | Subarray/substring with constraint | Longest substring no-repeat, max sum size-K |
| Prefix sum | Range queries | Subarray sum equals K |
| Fast/slow pointer | Cycle / middle | LL cycle, middle of LL |
| Monotonic stack | Next greater/smaller | Daily temperatures, largest rectangle |
| BFS | Shortest path unweighted, level-order | Word ladder, level traversal |
| DFS | All paths, connected components | Number of islands |
| Topological sort | Dependency ordering, DAG | Course schedule |
| Union-Find | Connectivity / grouping | Number of provinces, MST |
| Dijkstra | Shortest path, non-negative weights | Network delay |
| DP (1D) | Decision per index | House robber, climb stairs |
| DP (2D) | Two-string / grid | Edit distance, unique paths |
| Knapsack DP | Subset / capacity | Coin change, subset sum |
| Heap | Top-K, streaming | K largest, merge K sorted |
| Trie | Prefix queries | Autocomplete, word search |

---

## Complexity Cheat Sheet

| Algorithm | Time | Space |
|-----------|------|-------|
| Linear scan | O(n) | O(1) |
| Binary search | O(log n) | O(1) |
| Sorting (comparison) | O(n log n) | O(log n) – O(n) |
| BFS / DFS | O(V + E) | O(V) |
| Dijkstra (heap) | O((V+E) log V) | O(V) |
| Bellman-Ford | O(V × E) | O(V) |
| DP (1D) | O(n) | O(n) |
| DP (2D) | O(n × m) | O(n × m) |
| Sliding window | O(n) | O(k) |
| Two pointers | O(n) | O(1) |
| Heap operations | O(log n) | O(n) |
| HashMap ops | O(1) avg | O(n) |
| Trie ops | O(m) m=word len | O(N×Σ) |

---

## Interview Strategy

| Step | Action |
|------|--------|
| 1 | **Clarify** — input range, edge cases, sorted? |
| 2 | **Brute force first** — state explicitly + complexity |
| 3 | **Optimize** — identify pattern (two ptr / sliding / DP / graph) |
| 4 | **Trace example** before coding |
| 5 | **Code** clean — meaningful var names |
| 6 | **Test** — edge cases (empty, single, all-same, max) |
| 7 | **Complexity** — both time + space |

---

## Companion Folders

- [Core Java Interview Topics](../Core%20Java%20Interview%20Topics/) — collections internals + threading
- [Spring Boot Interview Topics](../Spring%20Boot%20Interview%20Topics/) — framework round
- [Performance & Optimization](../../phase-4-advanced-topics/Performance%20%26%20Optimization/) — practical perf
- [System Design Interview Prep](../System%20Design%20Interview%20Prep/) — high-level design

---

## Status Tracker

```
[ ] 01 — Array & String Problems
[ ] 02 — Linked List Problems
[ ] 03 — Stack & Queue Problems
[ ] 04 — Tree Problems
[ ] 05 — Graph Problems
[ ] 06 — Dynamic Programming
[ ] 07 — Sorting & Searching
```

> "DSA = pattern recognition + clean implementation + complexity reasoning. Practice 5-10 problems per pattern → 70% interviews crackable."

# Sorting & Searching

## Status: Not Started

---

## Table of Contents

1. [Sorting Overview](#sorting-overview)
2. [QuickSort](#quicksort)
3. [MergeSort](#mergesort)
4. [HeapSort](#heapsort)
5. [Counting Sort](#counting-sort)
6. [Radix Sort](#radix-sort)
7. [Java's Built-in Sort](#javas-built-in-sort)
8. [Stable vs Unstable Sort](#stable-vs-unstable-sort)
9. [Binary Search Variations](#binary-search-variations)
10. [Search in Rotated Sorted Array](#search-in-rotated-sorted-array)
11. [First/Last Occurrence](#firstlast-occurrence)
12. [Find Peak Element](#find-peak-element)
13. [Common Output Traps](#common-output-traps)
14. [Pitfalls](#pitfalls)
15. [Cheat Sheet](#cheat-sheet)

---

## Sorting Overview

| Algorithm | Best | Average | Worst | Space | Stable | In-place |
|-----------|------|---------|-------|-------|--------|----------|
| QuickSort | O(n log n) | O(n log n) | O(n²) | O(log n) | ❌ | ✅ |
| MergeSort | O(n log n) | O(n log n) | O(n log n) | O(n) | ✅ | ❌ |
| HeapSort | O(n log n) | O(n log n) | O(n log n) | O(1) | ❌ | ✅ |
| InsertionSort | O(n) | O(n²) | O(n²) | O(1) | ✅ | ✅ |
| BubbleSort | O(n) | O(n²) | O(n²) | O(1) | ✅ | ✅ |
| Counting Sort | O(n + k) | O(n + k) | O(n + k) | O(k) | ✅ | ❌ |
| Radix Sort | O(d × (n + k)) | — | — | O(n + k) | ✅ | ❌ |

→ k = range, d = digits.

### When to use what?

| Need | Algorithm |
|------|-----------|
| General purpose, fast avg | QuickSort |
| Guaranteed O(n log n), stable | MergeSort |
| Small n / nearly sorted | InsertionSort |
| Bounded integer range | Counting / Radix |
| In-place + worst-case bound | HeapSort |
| External sort (large data) | MergeSort |

---

## QuickSort

### Idea

Pick **pivot**, partition array around it (smaller left, larger right), recursively sort halves.

### Lomuto partition (simpler)

```java
void quickSort(int[] arr, int lo, int hi) {
    if (lo >= hi) return;
    int p = partition(arr, lo, hi);
    quickSort(arr, lo, p - 1);
    quickSort(arr, p + 1, hi);
}

int partition(int[] arr, int lo, int hi) {
    int pivot = arr[hi];   // pick last
    int i = lo - 1;
    for (int j = lo; j < hi; j++) {
        if (arr[j] <= pivot) {
            i++;
            swap(arr, i, j);
        }
    }
    swap(arr, i + 1, hi);
    return i + 1;
}
```

### Hoare partition (slightly faster)

Two pointers from both ends; swap until cross.

### Random pivot (avoids O(n²) on sorted)

```java
int pivot = lo + (int)(Math.random() * (hi - lo + 1));
swap(arr, pivot, hi);
```

### Why O(n²) worst-case?

Already-sorted array + last element pivot → unbalanced partitions. Random pivot fixes.

### Quickselect (kth largest in O(n))

Same idea, recurse into one half only.

```java
int kthLargest(int[] arr, int k) {
    int target = arr.length - k;     // (n-k)th smallest
    int lo = 0, hi = arr.length - 1;
    while (lo <= hi) {
        int p = partition(arr, lo, hi);
        if (p == target) return arr[p];
        if (p < target) lo = p + 1;
        else hi = p - 1;
    }
    return -1;
}
```

→ Avg **O(n)**, worst **O(n²)**.

---

## MergeSort

### Idea

Divide array in half, recursively sort, **merge two sorted halves**.

```java
void mergeSort(int[] arr, int lo, int hi) {
    if (lo >= hi) return;
    int mid = lo + (hi - lo) / 2;
    mergeSort(arr, lo, mid);
    mergeSort(arr, mid + 1, hi);
    merge(arr, lo, mid, hi);
}

void merge(int[] arr, int lo, int mid, int hi) {
    int[] tmp = new int[hi - lo + 1];
    int i = lo, j = mid + 1, k = 0;
    while (i <= mid && j <= hi) {
        tmp[k++] = arr[i] <= arr[j] ? arr[i++] : arr[j++];
    }
    while (i <= mid) tmp[k++] = arr[i++];
    while (j <= hi) tmp[k++] = arr[j++];
    System.arraycopy(tmp, 0, arr, lo, tmp.length);
}
```

### Properties

- **Stable** — `<=` in merge preserves order
- **O(n log n) guaranteed** — divide always halves
- **O(n)** extra space
- Used in Java's `Arrays.sort` for objects (TimSort variant)

### External sort

Sort 100GB on 4GB RAM:

1. Split into 4GB chunks
2. Sort each chunk + write to disk
3. K-way merge using priority queue

---

## HeapSort

### Idea

1. Build max-heap from array
2. Repeatedly swap root with last + heapify shrinking heap

```java
void heapSort(int[] arr) {
    int n = arr.length;
    for (int i = n / 2 - 1; i >= 0; i--) heapify(arr, n, i);
    
    for (int i = n - 1; i > 0; i--) {
        swap(arr, 0, i);
        heapify(arr, i, 0);
    }
}

void heapify(int[] arr, int n, int i) {
    int largest = i;
    int l = 2 * i + 1, r = 2 * i + 2;
    if (l < n && arr[l] > arr[largest]) largest = l;
    if (r < n && arr[r] > arr[largest]) largest = r;
    if (largest != i) {
        swap(arr, i, largest);
        heapify(arr, n, largest);
    }
}
```

→ Time **O(n log n)** all cases. Space **O(1)**. Not stable.

---

## Counting Sort

**For bounded integer range only.**

```java
int[] countingSort(int[] arr, int max) {
    int[] count = new int[max + 1];
    for (int n : arr) count[n]++;
    
    int idx = 0;
    for (int i = 0; i <= max; i++) {
        while (count[i]-- > 0) arr[idx++] = i;
    }
    return arr;
}
```

→ Time **O(n + k)**, space **O(k)** where k = max value.

→ Useful for **age sort**, **frequency sort**, **histogram**.

→ Stable variant uses prefix sum + back fill.

---

## Radix Sort

**For non-negative integers / fixed-width keys.**

Sort by least-significant digit → most-significant digit using **stable counting sort** at each step.

```java
void radixSort(int[] arr) {
    int max = Arrays.stream(arr).max().getAsInt();
    for (int exp = 1; max / exp > 0; exp *= 10) {
        countingSortByDigit(arr, exp);
    }
}

void countingSortByDigit(int[] arr, int exp) {
    int n = arr.length;
    int[] output = new int[n];
    int[] count = new int[10];
    
    for (int x : arr) count[(x / exp) % 10]++;
    for (int i = 1; i < 10; i++) count[i] += count[i - 1];
    for (int i = n - 1; i >= 0; i--) {
        int d = (arr[i] / exp) % 10;
        output[--count[d]] = arr[i];
    }
    System.arraycopy(output, 0, arr, 0, n);
}
```

→ Time **O(d × (n + k))**, space **O(n + k)**.

→ Beats O(n log n) for fixed-width integers.

---

## Java's Built-in Sort

### Primitives — `Arrays.sort(int[] arr)`

Java 7+ → **Dual-Pivot QuickSort** (better than classic — fewer comparisons).

```java
int[] arr = {5, 2, 8, 1};
Arrays.sort(arr);
```

→ Avg **O(n log n)**, worst **O(n²)** (rare).

### Objects — `Arrays.sort(Object[])`, `Collections.sort(List)`

→ **TimSort** (hybrid Merge + Insertion). Stable. Adaptive (faster on partially sorted).

```java
List<String> list = ...;
Collections.sort(list);
list.sort(Comparator.naturalOrder());
```

### Custom comparator

```java
Arrays.sort(arr, (a, b) -> a.value - b.value);   // ⚠️ overflow risk
Arrays.sort(arr, Comparator.comparingInt(o -> o.value));   // safer
```

→ Cross-ref `../Core Java Interview Topics/05-Comparable-vs-Comparator.md`.

---

## Stable vs Unstable Sort

**Stable** — equal elements keep their relative order.

| Stable | Unstable |
|--------|----------|
| MergeSort | QuickSort |
| InsertionSort | HeapSort |
| BubbleSort | Selection Sort |
| Counting / Radix | — |
| TimSort (Java objects) | Dual-pivot QS (Java primitives) |

### When matters?

Sorting list of records by one field while preserving previous order in another (multi-key sort).

```java
// First sort by name, then by age — only stable preserves name order within same age
list.sort(Comparator.comparing(P::getName));
list.sort(Comparator.comparingInt(P::getAge));
```

---

## Binary Search Variations

### Standard

```java
int search(int[] arr, int t) {
    int l = 0, r = arr.length - 1;
    while (l <= r) {
        int m = l + (r - l) / 2;
        if (arr[m] == t) return m;
        if (arr[m] < t) l = m + 1;
        else r = m - 1;
    }
    return -1;
}
```

### Search insert position (lower bound)

```java
int searchInsert(int[] arr, int t) {
    int l = 0, r = arr.length;
    while (l < r) {
        int m = l + (r - l) / 2;
        if (arr[m] < t) l = m + 1;
        else r = m;
    }
    return l;
}
```

### Generic "find smallest x where f(x) is true"

```java
int lo = lowest, hi = highest;
while (lo < hi) {
    int mid = lo + (hi - lo) / 2;
    if (f(mid)) hi = mid;
    else lo = mid + 1;
}
return lo;
```

→ Powerful template for "minimize / maximize" problems with monotonic property.

---

## Search in Rotated Sorted Array

```java
int search(int[] arr, int t) {
    int l = 0, r = arr.length - 1;
    while (l <= r) {
        int m = l + (r - l) / 2;
        if (arr[m] == t) return m;
        
        if (arr[l] <= arr[m]) {       // left half sorted
            if (arr[l] <= t && t < arr[m]) r = m - 1;
            else l = m + 1;
        } else {                       // right half sorted
            if (arr[m] < t && t <= arr[r]) l = m + 1;
            else r = m - 1;
        }
    }
    return -1;
}
```

→ Time **O(log n)**.

→ With duplicates → may degrade to O(n) (worst case all same).

---

## First/Last Occurrence

```java
int firstOccurrence(int[] arr, int t) {
    int l = 0, r = arr.length - 1, ans = -1;
    while (l <= r) {
        int m = l + (r - l) / 2;
        if (arr[m] == t) { ans = m; r = m - 1; }
        else if (arr[m] < t) l = m + 1;
        else r = m - 1;
    }
    return ans;
}

int lastOccurrence(int[] arr, int t) {
    int l = 0, r = arr.length - 1, ans = -1;
    while (l <= r) {
        int m = l + (r - l) / 2;
        if (arr[m] == t) { ans = m; l = m + 1; }
        else if (arr[m] < t) l = m + 1;
        else r = m - 1;
    }
    return ans;
}
```

→ Count of t = `last - first + 1`.

---

## Find Peak Element

**Q:** arr[i] > arr[i-1] AND arr[i] > arr[i+1]. Find any peak.

```java
int findPeak(int[] arr) {
    int l = 0, r = arr.length - 1;
    while (l < r) {
        int m = l + (r - l) / 2;
        if (arr[m] > arr[m + 1]) r = m;       // peak in left half (incl m)
        else l = m + 1;                        // peak in right half
    }
    return l;
}
```

→ Time **O(log n)**.

---

## Common Output Traps

### Q1. Mid overflow

```java
int mid = (l + r) / 2;        // ❌ overflow for large l + r
int mid = l + (r - l) / 2;    // ✅
```

### Q2. Comparator overflow

```java
Arrays.sort(arr, (a, b) -> a - b);    // ❌ if a, b near INT_MIN/MAX
Arrays.sort(arr, Integer::compare);   // ✅
```

### Q3. `<` vs `<=` in binary search loop

Both work; consistency with shrink direction matters.

### Q4. Java's `Arrays.binarySearch`

```java
int idx = Arrays.binarySearch(arr, t);
// not found → -(insertion_point) - 1
// always check `idx >= 0`
```

### Q5. Recursion limit on QuickSort with sorted input

→ Random pivot or Iterative.

---

## Pitfalls

1. **Overflow** in pivot / mid / comparator.
2. **Modifying input** when not allowed.
3. **`Arrays.sort` on `int[]`** = primitive (Dual-Pivot QS, unstable); on `Integer[]` = TimSort (stable). Match expectation.
4. **Stable sort assumption** — verify which Java sort applies.
5. **Comparator anti-symmetry** broken — `IllegalArgumentException`.
6. **Binary search on unsorted** — wrong answer.
7. **`HashSet`/`HashMap` no order** — use `LinkedHashMap` for insertion order.
8. **Counting sort with negatives** — shift by `min`.
9. **Recursion depth** in QuickSort — pathological case.
10. **Quickselect** worst-case O(n²) — use median-of-medians for guaranteed O(n) (rarely needed).

---

## Cheat Sheet

| Algorithm | Time | Space | Stable | Use |
|-----------|------|-------|--------|-----|
| QuickSort | O(n log n) avg | O(log n) | ❌ | General |
| MergeSort | O(n log n) | O(n) | ✅ | Stable / external |
| HeapSort | O(n log n) | O(1) | ❌ | In-place, worst case |
| Insertion | O(n²) | O(1) | ✅ | Small n / nearly sorted |
| Counting | O(n+k) | O(k) | ✅ | Bounded range |
| Radix | O(d(n+k)) | O(n+k) | ✅ | Fixed-width int |

| Search | Time |
|--------|------|
| Binary search | O(log n) |
| First / last occurrence | O(log n) |
| Search in rotated | O(log n) |
| Peak element | O(log n) |

| Java | Algorithm | Stable |
|------|-----------|--------|
| `Arrays.sort(int[])` | Dual-Pivot QuickSort | ❌ |
| `Arrays.sort(Object[])` | TimSort | ✅ |
| `Collections.sort(List)` | TimSort | ✅ |

---

## Practice (LeetCode)

| # | Problem | Topic |
|---|---------|-------|
| 215 | Kth Largest Element | Quickselect / Heap |
| 912 | Sort an Array | Sort |
| 88 | Merge Sorted Array | Merge |
| 75 | Sort Colors | Dutch flag (3-way) |
| 148 | Sort List | MergeSort on LL |
| 33 | Search in Rotated Array | Binary search |
| 81 | Search in Rotated II (dups) | Binary search |
| 153 | Find Min in Rotated | Binary search |
| 162 | Find Peak Element | Binary search |
| 34 | First & Last Position | Binary search |
| 704 | Binary Search | Binary search |
| 35 | Search Insert Position | Lower bound |
| 875 | Koko Eating Bananas | Binary search on answer |
| 410 | Split Array Largest Sum | Binary search on answer |
| 4 | Median of Two Sorted Arrays | Binary search |

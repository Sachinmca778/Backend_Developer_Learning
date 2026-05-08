# Linked List Problems

## Status: Not Started

---

## Table of Contents

1. [Linked List Refresher](#linked-list-refresher)
2. [Reverse Linked List](#reverse-linked-list)
3. [Cycle Detection (Floyd)](#cycle-detection-floyd)
4. [Find Middle Node](#find-middle-node)
5. [Merge Two Sorted Lists](#merge-two-sorted-lists)
6. [Remove Nth Node from End](#remove-nth-node-from-end)
7. [LRU Cache](#lru-cache)
8. [Reverse in Groups of K](#reverse-in-groups-of-k)
9. [Intersection of Two Lists](#intersection-of-two-lists)
10. [Palindrome Linked List](#palindrome-linked-list)
11. [Common Output Traps](#common-output-traps)
12. [Pitfalls](#pitfalls)
13. [Cheat Sheet](#cheat-sheet)

---

## Linked List Refresher

```java
class ListNode {
    int val;
    ListNode next;
    ListNode(int v) { val = v; }
}
```

### Why dummy node?

```java
ListNode dummy = new ListNode(0);
dummy.next = head;
```

→ Avoids special handling for head node modifications.

### Common patterns

| Pattern | Use |
|---------|-----|
| Two pointers | Cycle / middle / Nth from end |
| Dummy head | Modifications at start |
| Reverse | In-place rewiring |
| Merge | Two sorted lists |

---

## Reverse Linked List

### Iterative (most asked)

```java
ListNode reverse(ListNode head) {
    ListNode prev = null, curr = head;
    while (curr != null) {
        ListNode next = curr.next;     // save
        curr.next = prev;               // rewire
        prev = curr;                    // advance
        curr = next;
    }
    return prev;
}
```

→ Time **O(n)**, space **O(1)**.

### Recursive

```java
ListNode reverse(ListNode head) {
    if (head == null || head.next == null) return head;
    ListNode rest = reverse(head.next);
    head.next.next = head;
    head.next = null;
    return rest;
}
```

→ Space **O(n)** call stack.

### Reverse between positions m and n

```java
ListNode reverseBetween(ListNode head, int m, int n) {
    ListNode dummy = new ListNode(0); dummy.next = head;
    ListNode prev = dummy;
    for (int i = 1; i < m; i++) prev = prev.next;
    
    ListNode curr = prev.next;
    for (int i = 0; i < n - m; i++) {
        ListNode next = curr.next;
        curr.next = next.next;
        next.next = prev.next;
        prev.next = next;
    }
    return dummy.next;
}
```

---

## Cycle Detection (Floyd)

### Detect cycle (Tortoise & Hare)

```java
boolean hasCycle(ListNode head) {
    ListNode slow = head, fast = head;
    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;
        if (slow == fast) return true;
    }
    return false;
}
```

→ Time **O(n)**, space **O(1)**.

### Find cycle start node

After detection, reset one pointer to head; move both 1 step until meet.

```java
ListNode detectCycleStart(ListNode head) {
    ListNode slow = head, fast = head;
    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;
        if (slow == fast) {
            ListNode p = head;
            while (p != slow) { p = p.next; slow = slow.next; }
            return p;        // cycle start
        }
    }
    return null;
}
```

### Math behind it

Distance from head to cycle = distance from meeting point to cycle (going forward in cycle). Floyd's elegant proof.

---

## Find Middle Node

```java
ListNode middle(ListNode head) {
    ListNode slow = head, fast = head;
    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;
    }
    return slow;
}
```

→ For even count: returns **second middle** (1→2→3→4 returns 3).

→ Variation: return first middle → check `fast.next.next != null` instead.

---

## Merge Two Sorted Lists

### Iterative

```java
ListNode merge(ListNode a, ListNode b) {
    ListNode dummy = new ListNode(0), tail = dummy;
    while (a != null && b != null) {
        if (a.val <= b.val) { tail.next = a; a = a.next; }
        else { tail.next = b; b = b.next; }
        tail = tail.next;
    }
    tail.next = (a != null) ? a : b;
    return dummy.next;
}
```

### Recursive

```java
ListNode merge(ListNode a, ListNode b) {
    if (a == null) return b;
    if (b == null) return a;
    if (a.val <= b.val) { a.next = merge(a.next, b); return a; }
    else { b.next = merge(a, b.next); return b; }
}
```

### Merge K sorted lists (priority queue)

```java
ListNode mergeKLists(ListNode[] lists) {
    PriorityQueue<ListNode> pq = new PriorityQueue<>((a, b) -> a.val - b.val);
    for (ListNode l : lists) if (l != null) pq.offer(l);
    
    ListNode dummy = new ListNode(0), tail = dummy;
    while (!pq.isEmpty()) {
        ListNode min = pq.poll();
        tail.next = min;
        tail = tail.next;
        if (min.next != null) pq.offer(min.next);
    }
    return dummy.next;
}
```

→ Time **O(N log K)** where N = total nodes, K = lists.

---

## Remove Nth Node from End

### Two-pointer gap technique

```java
ListNode removeNthFromEnd(ListNode head, int n) {
    ListNode dummy = new ListNode(0); dummy.next = head;
    ListNode fast = dummy, slow = dummy;
    
    for (int i = 0; i <= n; i++) fast = fast.next;
    
    while (fast != null) {
        slow = slow.next;
        fast = fast.next;
    }
    slow.next = slow.next.next;
    return dummy.next;
}
```

→ Single pass **O(n)**.

→ Dummy head essential — handles "remove head" case.

---

## LRU Cache

**Q:** Implement Least-Recently-Used cache with `O(1)` get/put.

### Structure

- **HashMap** key → DLL node
- **Doubly Linked List** for ordering (most recent at head)

```java
class LRUCache {
    class Node {
        int key, val;
        Node prev, next;
        Node(int k, int v) { key = k; val = v; }
    }
    
    private final int cap;
    private final Map<Integer, Node> map = new HashMap<>();
    private final Node head = new Node(0, 0), tail = new Node(0, 0);
    
    public LRUCache(int capacity) {
        cap = capacity;
        head.next = tail; tail.prev = head;
    }
    
    public int get(int k) {
        if (!map.containsKey(k)) return -1;
        Node n = map.get(k);
        moveToHead(n);
        return n.val;
    }
    
    public void put(int k, int v) {
        if (map.containsKey(k)) {
            Node n = map.get(k);
            n.val = v;
            moveToHead(n);
        } else {
            if (map.size() == cap) {
                Node lru = tail.prev;
                remove(lru);
                map.remove(lru.key);
            }
            Node n = new Node(k, v);
            map.put(k, n);
            addToHead(n);
        }
    }
    
    private void addToHead(Node n) {
        n.next = head.next;
        n.prev = head;
        head.next.prev = n;
        head.next = n;
    }
    
    private void remove(Node n) {
        n.prev.next = n.next;
        n.next.prev = n.prev;
    }
    
    private void moveToHead(Node n) {
        remove(n);
        addToHead(n);
    }
}
```

→ Both `get` and `put` **O(1)**.

### Quick alternative — `LinkedHashMap`

```java
class LRU<K, V> extends LinkedHashMap<K, V> {
    private final int cap;
    public LRU(int cap) {
        super(cap, 0.75f, true);   // accessOrder = true
        this.cap = cap;
    }
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > cap;
    }
}
```

→ For interviews, **HashMap + DLL** preferred (shows internals understanding).

---

## Reverse in Groups of K

```java
ListNode reverseKGroup(ListNode head, int k) {
    ListNode curr = head;
    int count = 0;
    while (curr != null && count < k) { curr = curr.next; count++; }
    
    if (count == k) {
        curr = reverseKGroup(curr, k);
        while (count-- > 0) {
            ListNode next = head.next;
            head.next = curr;
            curr = head;
            head = next;
        }
        head = curr;
    }
    return head;
}
```

→ Time **O(n)**.

---

## Intersection of Two Lists

### Length difference approach

```java
ListNode intersect(ListNode a, ListNode b) {
    int la = len(a), lb = len(b);
    while (la > lb) { a = a.next; la--; }
    while (lb > la) { b = b.next; lb--; }
    while (a != b) { a = a.next; b = b.next; }
    return a;
}
int len(ListNode n) { int c = 0; while (n != null) { c++; n = n.next; } return c; }
```

### Elegant two-pointer

```java
ListNode intersect(ListNode a, ListNode b) {
    ListNode pa = a, pb = b;
    while (pa != pb) {
        pa = (pa == null) ? b : pa.next;
        pb = (pb == null) ? a : pb.next;
    }
    return pa;
}
```

→ When pointer hits null, jump to other list's head. After 2 cycles, they align. **O(la + lb)**.

---

## Palindrome Linked List

### Find middle + reverse second half + compare

```java
boolean isPalindrome(ListNode head) {
    if (head == null) return true;
    
    ListNode mid = middle(head);
    ListNode second = reverse(mid);
    
    ListNode p1 = head, p2 = second;
    while (p2 != null) {
        if (p1.val != p2.val) return false;
        p1 = p1.next; p2 = p2.next;
    }
    return true;
}
```

→ Time **O(n)**, space **O(1)** (in-place reverse).

---

## Common Output Traps

### Q1. NullPointerException on `fast.next.next`

```java
while (fast != null && fast.next != null) { ... }   // ✅ both checks
```

### Q2. Lost reference

```java
curr.next = prev;
prev = curr;
curr = curr.next;     // ❌ already overwritten
```

### Q3. Cycle = infinite loop

Always check / detect cycles before traversing.

### Q4. Even-length middle ambiguity

Two definitions; clarify in interview.

### Q5. Modifying head requires dummy

Otherwise return value tricky.

---

## Pitfalls

1. **Forgot `dummy.next`** — return `dummy` (zero) instead of `dummy.next`.
2. **Order of pointer rewiring** — save `next` before modifying.
3. **NullPointerException** in fast pointer — check both `fast` & `fast.next`.
4. **Recursion depth limit** — for huge lists prefer iterative.
5. **Mutating while iterating** — restructure first then traverse.
6. **Cycle detection without check** — infinite loop.
7. **Comparing references** vs values — use `==` for nodes, `.val` for content.
8. **Wrong middle definition** — even-length 1→2→3→4 → first middle (2) vs second (3); ask.
9. **Off-by-one in Nth from end** — `for (i = 0; i <= n; ...)` (one extra step from dummy).
10. **Java vs C/C++** — no manual free; GC handles.

---

## Cheat Sheet

| Problem | Pattern | Time |
|---------|---------|------|
| Reverse | Iterate + rewire | O(n) |
| Cycle | Floyd's tortoise-hare | O(n) |
| Middle | Slow-fast | O(n) |
| Merge | Two pointer | O(n+m) |
| Nth from end | Gap of N | O(n) |
| LRU | HashMap + DLL | O(1) |
| Palindrome | Reverse half | O(n) |
| Intersection | Two-pass alignment | O(la+lb) |

---

## Practice (LeetCode)

| # | Problem |
|---|---------|
| 21 | Merge Two Sorted Lists |
| 23 | Merge K Sorted Lists |
| 19 | Remove Nth from End |
| 141 | Linked List Cycle |
| 142 | Linked List Cycle II (start) |
| 146 | LRU Cache |
| 206 | Reverse Linked List |
| 92 | Reverse Linked List II |
| 25 | Reverse Nodes in K-Group |
| 234 | Palindrome Linked List |
| 160 | Intersection of Two Linked Lists |
| 138 | Copy List with Random Pointer |
| 86 | Partition List |
| 143 | Reorder List |

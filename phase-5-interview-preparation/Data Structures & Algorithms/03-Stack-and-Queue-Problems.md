# Stack & Queue Problems

## Status: Not Started

---

## Table of Contents

1. [Stack & Queue Refresher](#stack--queue-refresher)
2. [Valid Parentheses](#valid-parentheses)
3. [Stack using Queues](#stack-using-queues)
4. [Queue using Stacks](#queue-using-stacks)
5. [Min Stack](#min-stack)
6. [Monotonic Stack Pattern](#monotonic-stack-pattern)
7. [Next Greater Element](#next-greater-element)
8. [Daily Temperatures](#daily-temperatures)
9. [Largest Rectangle in Histogram](#largest-rectangle-in-histogram)
10. [Sliding Window Maximum](#sliding-window-maximum)
11. [Evaluate Reverse Polish Notation](#evaluate-reverse-polish-notation)
12. [BFS using Queue](#bfs-using-queue)
13. [Common Output Traps](#common-output-traps)
14. [Pitfalls](#pitfalls)
15. [Cheat Sheet](#cheat-sheet)

---

## Stack & Queue Refresher

### Java's choices

| DS | Implementation | Notes |
|----|----------------|-------|
| Stack | `Deque<Integer>` (`ArrayDeque`) | Avoid legacy `Stack` (synchronized + slow) |
| Queue | `Queue<Integer>` (`ArrayDeque` / `LinkedList`) | `ArrayDeque` faster |
| Deque (both ends) | `Deque<Integer>` (`ArrayDeque`) | Used for monotonic + sliding window |
| Priority Queue | `PriorityQueue<>` | Min-heap default |

```java
Deque<Integer> stack = new ArrayDeque<>();
stack.push(1); stack.pop(); stack.peek();

Queue<Integer> q = new ArrayDeque<>();
q.offer(1); q.poll(); q.peek();
```

⚠️ `Stack` (java.util) — synchronized, slower. Use `ArrayDeque`.

---

## Valid Parentheses

```java
boolean isValid(String s) {
    Deque<Character> stack = new ArrayDeque<>();
    for (char c : s.toCharArray()) {
        if (c == '(' || c == '[' || c == '{') stack.push(c);
        else {
            if (stack.isEmpty()) return false;
            char top = stack.pop();
            if (c == ')' && top != '(') return false;
            if (c == ']' && top != '[') return false;
            if (c == '}' && top != '{') return false;
        }
    }
    return stack.isEmpty();
}
```

→ Time **O(n)**, space **O(n)**.

### Cleaner with Map

```java
Map<Character, Character> pair = Map.of(')', '(', ']', '[', '}', '{');
for (char c : s.toCharArray()) {
    if (pair.containsValue(c)) stack.push(c);
    else if (stack.isEmpty() || stack.pop() != pair.get(c)) return false;
}
```

---

## Stack using Queues

### Two-queue (push O(1), pop O(n))

```java
class MyStack {
    Queue<Integer> q1 = new ArrayDeque<>(), q2 = new ArrayDeque<>();
    
    public void push(int x) { q1.offer(x); }
    
    public int pop() {
        while (q1.size() > 1) q2.offer(q1.poll());
        int top = q1.poll();
        Queue<Integer> t = q1; q1 = q2; q2 = t;
        return top;
    }
    
    public int top() { ... similar ... }
}
```

### One-queue (push O(n), pop O(1))

```java
class MyStack {
    Queue<Integer> q = new ArrayDeque<>();
    
    public void push(int x) {
        q.offer(x);
        for (int i = 0; i < q.size() - 1; i++) q.offer(q.poll());
    }
    public int pop() { return q.poll(); }
    public int top() { return q.peek(); }
}
```

---

## Queue using Stacks

```java
class MyQueue {
    Deque<Integer> in = new ArrayDeque<>(), out = new ArrayDeque<>();
    
    public void push(int x) { in.push(x); }
    
    public int pop() {
        peek();
        return out.pop();
    }
    
    public int peek() {
        if (out.isEmpty())
            while (!in.isEmpty()) out.push(in.pop());
        return out.peek();
    }
    
    public boolean empty() { return in.isEmpty() && out.isEmpty(); }
}
```

→ Amortized **O(1)** per operation.

---

## Min Stack

**Q:** Stack that supports push, pop, top, **getMin in O(1)**.

```java
class MinStack {
    Deque<int[]> stack = new ArrayDeque<>();   // [val, currMin]
    
    public void push(int x) {
        int min = stack.isEmpty() ? x : Math.min(x, stack.peek()[1]);
        stack.push(new int[]{x, min});
    }
    public void pop() { stack.pop(); }
    public int top() { return stack.peek()[0]; }
    public int getMin() { return stack.peek()[1]; }
}
```

→ Each node carries running min — **O(1)**.

---

## Monotonic Stack Pattern

**Pattern:** Stack maintains **monotonic order** (increasing or decreasing). Used for "next greater/smaller" problems.

### Template

```java
Deque<Integer> stack = new ArrayDeque<>();   // store indices
int[] result = new int[n];
for (int i = 0; i < n; i++) {
    while (!stack.isEmpty() && arr[stack.peek()] < arr[i]) {
        result[stack.pop()] = arr[i];
    }
    stack.push(i);
}
```

→ Each element pushed + popped once → **O(n)**.

---

## Next Greater Element

```java
int[] nextGreater(int[] arr) {
    int n = arr.length;
    int[] res = new int[n];
    Arrays.fill(res, -1);
    Deque<Integer> stack = new ArrayDeque<>();
    
    for (int i = 0; i < n; i++) {
        while (!stack.isEmpty() && arr[stack.peek()] < arr[i]) {
            res[stack.pop()] = arr[i];
        }
        stack.push(i);
    }
    return res;
}
```

### Circular array variant

Iterate `2n` times, take `i % n`.

---

## Daily Temperatures

```java
int[] dailyTemperatures(int[] T) {
    int n = T.length;
    int[] res = new int[n];
    Deque<Integer> stack = new ArrayDeque<>();
    
    for (int i = 0; i < n; i++) {
        while (!stack.isEmpty() && T[stack.peek()] < T[i]) {
            int idx = stack.pop();
            res[idx] = i - idx;
        }
        stack.push(i);
    }
    return res;
}
```

→ For each day, find days until warmer.

---

## Largest Rectangle in Histogram

**Classic monotonic stack problem.**

```java
int largestRectangleArea(int[] h) {
    Deque<Integer> stack = new ArrayDeque<>();
    int max = 0;
    for (int i = 0; i <= h.length; i++) {
        int curr = (i == h.length) ? 0 : h[i];
        while (!stack.isEmpty() && h[stack.peek()] >= curr) {
            int top = stack.pop();
            int width = stack.isEmpty() ? i : i - stack.peek() - 1;
            max = Math.max(max, h[top] * width);
        }
        stack.push(i);
    }
    return max;
}
```

→ Time **O(n)**, space **O(n)**.

→ Sentinel (`0` at end) flushes remaining stack.

---

## Sliding Window Maximum

**Q:** Max in every window of size K.

### Brute force O(n × k) — too slow.

### Monotonic deque (decreasing) — O(n)

```java
int[] maxSlidingWindow(int[] arr, int k) {
    Deque<Integer> dq = new ArrayDeque<>();   // store indices
    int n = arr.length;
    int[] res = new int[n - k + 1];
    
    for (int i = 0; i < n; i++) {
        // Remove indices out of window
        while (!dq.isEmpty() && dq.peekFirst() <= i - k) dq.pollFirst();
        
        // Maintain decreasing deque
        while (!dq.isEmpty() && arr[dq.peekLast()] < arr[i]) dq.pollLast();
        
        dq.offerLast(i);
        
        if (i >= k - 1) res[i - k + 1] = arr[dq.peekFirst()];
    }
    return res;
}
```

→ **O(n)** — each index entered + removed once from deque.

---

## Evaluate Reverse Polish Notation

```java
int evalRPN(String[] tokens) {
    Deque<Integer> stack = new ArrayDeque<>();
    for (String t : tokens) {
        if ("+-*/".contains(t)) {
            int b = stack.pop(), a = stack.pop();
            switch (t) {
                case "+" -> stack.push(a + b);
                case "-" -> stack.push(a - b);
                case "*" -> stack.push(a * b);
                case "/" -> stack.push(a / b);
            }
        } else {
            stack.push(Integer.parseInt(t));
        }
    }
    return stack.pop();
}
```

→ Time **O(n)**, space **O(n)**.

---

## BFS using Queue

### Generic BFS template

```java
void bfs(Node start) {
    Queue<Node> q = new ArrayDeque<>();
    Set<Node> visited = new HashSet<>();
    q.offer(start);
    visited.add(start);
    
    while (!q.isEmpty()) {
        Node curr = q.poll();
        // process curr
        for (Node nei : curr.neighbors()) {
            if (visited.add(nei)) q.offer(nei);
        }
    }
}
```

### Level-order traversal (binary tree)

```java
List<List<Integer>> levelOrder(TreeNode root) {
    List<List<Integer>> res = new ArrayList<>();
    if (root == null) return res;
    Queue<TreeNode> q = new ArrayDeque<>();
    q.offer(root);
    
    while (!q.isEmpty()) {
        int size = q.size();           // freeze level size
        List<Integer> level = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            TreeNode n = q.poll();
            level.add(n.val);
            if (n.left != null) q.offer(n.left);
            if (n.right != null) q.offer(n.right);
        }
        res.add(level);
    }
    return res;
}
```

→ **Frozen size** key — separates levels.

→ Cross-ref `04-Tree-Problems.md`.

---

## Common Output Traps

### Q1. `Stack` legacy class

```java
Stack<Integer> s = new Stack<>();   // ⚠️ slow synchronized
```

→ Prefer `Deque<Integer> s = new ArrayDeque<>();`.

### Q2. `null` in `ArrayDeque`

```java
Deque<Integer> d = new ArrayDeque<>();
d.offer(null);   // ❌ NPE — ArrayDeque doesn't allow null
```

→ Use `LinkedList` if `null` allowed. Better — design without null.

### Q3. `peek()` on empty

```java
stack.peek();   // returns null (Deque) — check `isEmpty()` first
```

### Q4. Forgetting frozen level size

```java
while (!q.isEmpty()) {
    for (int i = 0; i < q.size(); i++) { ... }   // ❌ size changes mid-loop
}
```

### Q5. Monotonic stack direction confusion

For **next greater** → maintain **decreasing** stack (pop when curr is greater).

---

## Pitfalls

1. **Legacy `Stack`** — use `ArrayDeque`.
2. **`null` in `ArrayDeque`** — NPE.
3. **Mutating queue while iterating** — frozen size first.
4. **`peek()` on empty** — null returned (silent bug).
5. **Wrong monotonic direction** — verify with example.
6. **Stack overflow** in recursion-heavy code — iterative DFS via stack.
7. **Storing values vs indices** in stack — indices for distance/width.
8. **`Queue.add` vs `offer`** — `add` throws on capacity (rare); prefer `offer`.
9. **`Queue.remove` vs `poll`** — `poll` returns null on empty; `remove` throws.
10. **Forgot sentinel** in histogram — last bars not flushed.

---

## Cheat Sheet

| Problem | Pattern | Time |
|---------|---------|------|
| Valid parentheses | Stack | O(n) |
| Min stack | Stack of pair | O(1) all |
| Next greater | Monotonic stack | O(n) |
| Largest rectangle | Monotonic + sentinel | O(n) |
| Sliding window max | Monotonic deque | O(n) |
| BFS | Queue | O(V+E) |
| Stack via queues | One-queue trick | push O(n) |
| Queue via stacks | Two stacks | amortized O(1) |
| Eval RPN | Stack | O(n) |

| Java | Use |
|------|-----|
| `ArrayDeque` | Stack / Queue / Deque |
| `PriorityQueue` | Heap |
| `LinkedList` | If null elements |

---

## Practice (LeetCode)

| # | Problem |
|---|---------|
| 20 | Valid Parentheses |
| 71 | Simplify Path |
| 84 | Largest Rectangle in Histogram |
| 85 | Maximal Rectangle |
| 150 | Evaluate RPN |
| 155 | Min Stack |
| 225 | Stack using Queues |
| 232 | Queue using Stacks |
| 239 | Sliding Window Maximum |
| 394 | Decode String |
| 496 | Next Greater Element I |
| 503 | Next Greater Element II (circular) |
| 739 | Daily Temperatures |
| 102 | Binary Tree Level Order |
| 994 | Rotting Oranges (BFS) |

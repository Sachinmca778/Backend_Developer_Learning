# Tree Problems

## Status: Not Started

---

## Table of Contents

1. [Tree Refresher](#tree-refresher)
2. [Tree Traversals — Recursive](#tree-traversals--recursive)
3. [Tree Traversals — Iterative](#tree-traversals--iterative)
4. [BFS Level-Order Traversal](#bfs-level-order-traversal)
5. [Tree Height & Depth](#tree-height--depth)
6. [Diameter of Binary Tree](#diameter-of-binary-tree)
7. [Lowest Common Ancestor](#lowest-common-ancestor)
8. [BST Operations](#bst-operations)
9. [Validate BST](#validate-bst)
10. [Balanced Tree Check](#balanced-tree-check)
11. [Path Sum Variants](#path-sum-variants)
12. [Serialize / Deserialize](#serialize--deserialize)
13. [Common Output Traps](#common-output-traps)
14. [Pitfalls](#pitfalls)
15. [Cheat Sheet](#cheat-sheet)

---

## Tree Refresher

```java
class TreeNode {
    int val;
    TreeNode left, right;
    TreeNode(int v) { val = v; }
}
```

### Tree types

| Type | Property |
|------|----------|
| Binary tree | ≤ 2 children |
| BST | Left < root < right |
| Balanced | Height differ by ≤ 1 |
| Complete | All levels filled except possibly last (left to right) |
| Full | Every node has 0 or 2 children |
| Perfect | All internal nodes 2 children + all leaves at same depth |

---

## Tree Traversals — Recursive

```java
void inorder(TreeNode n) {       // Left, Root, Right
    if (n == null) return;
    inorder(n.left);
    visit(n);
    inorder(n.right);
}

void preorder(TreeNode n) {      // Root, Left, Right
    if (n == null) return;
    visit(n);
    preorder(n.left);
    preorder(n.right);
}

void postorder(TreeNode n) {     // Left, Right, Root
    if (n == null) return;
    postorder(n.left);
    postorder(n.right);
    visit(n);
}
```

### When?

| Order | Use case |
|-------|----------|
| Inorder | BST → sorted output |
| Preorder | Copy / serialize |
| Postorder | Delete tree / dependency resolution |
| Level-order | BFS / shortest path / right view |

---

## Tree Traversals — Iterative

### Inorder iterative

```java
List<Integer> inorder(TreeNode root) {
    List<Integer> res = new ArrayList<>();
    Deque<TreeNode> stack = new ArrayDeque<>();
    TreeNode curr = root;
    
    while (curr != null || !stack.isEmpty()) {
        while (curr != null) {
            stack.push(curr);
            curr = curr.left;
        }
        curr = stack.pop();
        res.add(curr.val);
        curr = curr.right;
    }
    return res;
}
```

### Preorder iterative

```java
List<Integer> preorder(TreeNode root) {
    List<Integer> res = new ArrayList<>();
    if (root == null) return res;
    Deque<TreeNode> stack = new ArrayDeque<>();
    stack.push(root);
    
    while (!stack.isEmpty()) {
        TreeNode n = stack.pop();
        res.add(n.val);
        if (n.right != null) stack.push(n.right);   // right first
        if (n.left != null) stack.push(n.left);
    }
    return res;
}
```

### Postorder iterative — two stacks (or reverse preorder with right-first)

```java
List<Integer> postorder(TreeNode root) {
    LinkedList<Integer> res = new LinkedList<>();
    if (root == null) return res;
    Deque<TreeNode> stack = new ArrayDeque<>();
    stack.push(root);
    
    while (!stack.isEmpty()) {
        TreeNode n = stack.pop();
        res.addFirst(n.val);                      // prepend
        if (n.left != null) stack.push(n.left);   // left first now
        if (n.right != null) stack.push(n.right);
    }
    return res;
}
```

→ Trick: **modified preorder (root, right, left), then reverse** = postorder.

---

## BFS Level-Order Traversal

```java
List<List<Integer>> levelOrder(TreeNode root) {
    List<List<Integer>> res = new ArrayList<>();
    if (root == null) return res;
    Queue<TreeNode> q = new ArrayDeque<>();
    q.offer(root);
    
    while (!q.isEmpty()) {
        int size = q.size();          // freeze level size
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

### Variations

- **Right side view** — last element of each level
- **Zigzag** — alternate level reverse
- **Bottom-up** — `Collections.reverse(res)` at end

---

## Tree Height & Depth

```java
int height(TreeNode n) {
    if (n == null) return 0;
    return 1 + Math.max(height(n.left), height(n.right));
}
```

→ Time **O(n)**.

→ **Depth** = root to node distance; **Height** = node to deepest leaf.

→ Empty tree → height 0 (some defs say -1; clarify).

---

## Diameter of Binary Tree

**Diameter** = longest path between any two nodes (in edges).

```java
int diameter = 0;

public int diameterOfBinaryTree(TreeNode root) {
    height(root);
    return diameter;
}

int height(TreeNode n) {
    if (n == null) return 0;
    int l = height(n.left), r = height(n.right);
    diameter = Math.max(diameter, l + r);   // path through n
    return 1 + Math.max(l, r);
}
```

→ Time **O(n)**, space **O(h)**.

---

## Lowest Common Ancestor

### Binary tree (general)

```java
TreeNode lca(TreeNode root, TreeNode p, TreeNode q) {
    if (root == null || root == p || root == q) return root;
    TreeNode l = lca(root.left, p, q);
    TreeNode r = lca(root.right, p, q);
    if (l != null && r != null) return root;     // p and q on different sides
    return l != null ? l : r;
}
```

→ Time **O(n)**.

### BST (faster — leverages ordering)

```java
TreeNode lcaBST(TreeNode root, TreeNode p, TreeNode q) {
    while (root != null) {
        if (p.val < root.val && q.val < root.val) root = root.left;
        else if (p.val > root.val && q.val > root.val) root = root.right;
        else return root;
    }
    return null;
}
```

→ Time **O(h)**, space **O(1)**.

---

## BST Operations

### Search

```java
TreeNode search(TreeNode root, int v) {
    while (root != null) {
        if (root.val == v) return root;
        root = v < root.val ? root.left : root.right;
    }
    return null;
}
```

→ Time **O(h)**.

### Insert

```java
TreeNode insert(TreeNode root, int v) {
    if (root == null) return new TreeNode(v);
    if (v < root.val) root.left = insert(root.left, v);
    else if (v > root.val) root.right = insert(root.right, v);
    return root;
}
```

### Delete (3 cases)

```java
TreeNode delete(TreeNode root, int v) {
    if (root == null) return null;
    if (v < root.val) root.left = delete(root.left, v);
    else if (v > root.val) root.right = delete(root.right, v);
    else {
        // Case 1 & 2: 0 or 1 child
        if (root.left == null) return root.right;
        if (root.right == null) return root.left;
        
        // Case 3: 2 children — find inorder successor (min of right subtree)
        TreeNode succ = root.right;
        while (succ.left != null) succ = succ.left;
        root.val = succ.val;
        root.right = delete(root.right, succ.val);
    }
    return root;
}
```

---

## Validate BST

### Wrong approach (only direct children check)

```java
// ❌ Doesn't catch: 5 with right child 6, but 6's left is 4
return n.left.val < n.val && n.val < n.right.val;
```

### Correct — pass min/max bounds

```java
boolean isValidBST(TreeNode root) {
    return valid(root, Long.MIN_VALUE, Long.MAX_VALUE);
}

boolean valid(TreeNode n, long min, long max) {
    if (n == null) return true;
    if (n.val <= min || n.val >= max) return false;
    return valid(n.left, min, n.val) && valid(n.right, n.val, max);
}
```

### Inorder check (alternative)

Inorder of valid BST is **strictly increasing**.

```java
TreeNode prev = null;
boolean isValidBST(TreeNode n) {
    if (n == null) return true;
    if (!isValidBST(n.left)) return false;
    if (prev != null && prev.val >= n.val) return false;
    prev = n;
    return isValidBST(n.right);
}
```

---

## Balanced Tree Check

**Height of left & right differ by ≤ 1, recursively.**

```java
boolean isBalanced(TreeNode root) {
    return height(root) != -1;
}

int height(TreeNode n) {
    if (n == null) return 0;
    int l = height(n.left);
    if (l == -1) return -1;
    int r = height(n.right);
    if (r == -1) return -1;
    if (Math.abs(l - r) > 1) return -1;
    return 1 + Math.max(l, r);
}
```

→ **-1** propagates "not balanced" → **O(n)** instead of O(n²).

---

## Path Sum Variants

### Root-to-leaf with target sum (path exists?)

```java
boolean hasPathSum(TreeNode n, int target) {
    if (n == null) return false;
    if (n.left == null && n.right == null) return n.val == target;
    return hasPathSum(n.left, target - n.val) || hasPathSum(n.right, target - n.val);
}
```

### All root-to-leaf paths summing to target

```java
List<List<Integer>> pathSum(TreeNode root, int target) {
    List<List<Integer>> res = new ArrayList<>();
    dfs(root, target, new ArrayList<>(), res);
    return res;
}

void dfs(TreeNode n, int target, List<Integer> path, List<List<Integer>> res) {
    if (n == null) return;
    path.add(n.val);
    if (n.left == null && n.right == null && target == n.val) {
        res.add(new ArrayList<>(path));
    }
    dfs(n.left, target - n.val, path, res);
    dfs(n.right, target - n.val, path, res);
    path.remove(path.size() - 1);   // backtrack
}
```

### Path sum III (any node to any node)

→ Prefix sum in DFS — O(n).

### Max path sum (any node to any node)

```java
int maxSum = Integer.MIN_VALUE;

int maxPathSum(TreeNode root) {
    gain(root);
    return maxSum;
}

int gain(TreeNode n) {
    if (n == null) return 0;
    int l = Math.max(gain(n.left), 0);
    int r = Math.max(gain(n.right), 0);
    maxSum = Math.max(maxSum, n.val + l + r);
    return n.val + Math.max(l, r);
}
```

---

## Serialize / Deserialize

```java
String serialize(TreeNode root) {
    StringBuilder sb = new StringBuilder();
    ser(root, sb);
    return sb.toString();
}
void ser(TreeNode n, StringBuilder sb) {
    if (n == null) { sb.append("#,"); return; }
    sb.append(n.val).append(",");
    ser(n.left, sb);
    ser(n.right, sb);
}

TreeNode deserialize(String s) {
    Queue<String> q = new LinkedList<>(Arrays.asList(s.split(",")));
    return des(q);
}
TreeNode des(Queue<String> q) {
    String t = q.poll();
    if (t.equals("#")) return null;
    TreeNode n = new TreeNode(Integer.parseInt(t));
    n.left = des(q);
    n.right = des(q);
    return n;
}
```

→ Preorder + null marker.

---

## Common Output Traps

### Q1. `validateBST` only checking direct children

(Covered above — pass bounds.)

### Q2. `Integer.MIN_VALUE` as bound

If node value can be `Integer.MIN_VALUE`, use `Long` bounds.

### Q3. Forgetting backtrack in path problems

```java
path.add(n.val);
... recurse ...
// missing: path.remove(path.size() - 1);
```

### Q4. Recursive height giving O(n²)

For `isBalanced`, naive O(n²) common. Use `-1` sentinel for O(n).

### Q5. Mutating shared state without thread-safety

Recursion + class field — single-thread fine, multi-thread bug.

---

## Pitfalls

1. **Stack overflow** for deep / skewed tree — iterative or tail recursion (Java doesn't optimize tail).
2. **Equality on TreeNode** — reference equality unless `equals` overridden.
3. **`null` checks missing** — NPE in left/right access.
4. **Confusing depth vs height** — clarify in interview.
5. **Path problem without backtracking** — wrong results.
6. **BST validation wrong** — sub-tree bounds.
7. **Iterative postorder** tricky — use modified preorder + reverse.
8. **Frozen level size** in BFS — recompute mid-loop = bug.
9. **Serialize without null marker** — can't reconstruct.
10. **Wrong recursion base** — return null vs return 0 distinction.

---

## Cheat Sheet

| Traversal | Order | Use |
|-----------|-------|-----|
| Inorder | L-Root-R | BST sorted |
| Preorder | Root-L-R | Copy / serialize |
| Postorder | L-R-Root | Delete / dependency |
| Level | BFS | Shortest / view |

| Operation | Recursive | Iterative | Time |
|-----------|-----------|-----------|------|
| Traverse | ✅ | ✅ (stack) | O(n) |
| Height | trivial | post-stack | O(n) |
| LCA (BST) | recursive | iterative | O(h) |
| Validate BST | bounds | inorder | O(n) |

---

## Practice (LeetCode)

| # | Problem |
|---|---------|
| 94 | Inorder Traversal |
| 144 | Preorder Traversal |
| 145 | Postorder Traversal |
| 102 | Level Order Traversal |
| 103 | Zigzag Level Order |
| 104 | Maximum Depth |
| 110 | Balanced Binary Tree |
| 124 | Maximum Path Sum |
| 199 | Right Side View |
| 226 | Invert Binary Tree |
| 235 | LCA of BST |
| 236 | LCA of Binary Tree |
| 297 | Serialize / Deserialize |
| 543 | Diameter |
| 98 | Validate BST |
| 105 | Build Tree from Preorder + Inorder |
| 112 | Path Sum |
| 113 | Path Sum II |
| 437 | Path Sum III |
| 230 | Kth Smallest in BST |

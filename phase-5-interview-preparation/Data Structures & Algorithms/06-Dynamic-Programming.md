# Dynamic Programming

## Status: Not Started

---

## Table of Contents

1. [DP Mindset](#dp-mindset)
2. [Memoization vs Tabulation](#memoization-vs-tabulation)
3. [DP Identification Checklist](#dp-identification-checklist)
4. [1D DP — Fibonacci](#1d-dp--fibonacci)
5. [Climbing Stairs](#climbing-stairs)
6. [House Robber](#house-robber)
7. [Max Product Subarray](#max-product-subarray)
8. [2D DP — Unique Paths](#2d-dp--unique-paths)
9. [Edit Distance](#edit-distance)
10. [Longest Common Subsequence](#longest-common-subsequence)
11. [0/1 Knapsack](#01-knapsack)
12. [Unbounded Knapsack — Coin Change](#unbounded-knapsack--coin-change)
13. [DP on Strings — Word Break](#dp-on-strings--word-break)
14. [Common Output Traps](#common-output-traps)
15. [Pitfalls](#pitfalls)
16. [Cheat Sheet](#cheat-sheet)

---

## DP Mindset

> "**DP = Recursion + Memoization**. Recursion explore karta hai sub-problems; memoization repeat avoid karta hai."

### Two requirements

1. **Optimal substructure** — sub-problem ka optimal answer → bigger problem ka optimal answer
2. **Overlapping sub-problems** — same sub-problem multiple times solve hota

### Steps to solve

1. **Define state** — what does `dp[i]` / `dp[i][j]` represent?
2. **Recurrence relation** — `dp[i] = ?` from previous states
3. **Base case** — smallest input
4. **Order** — top-down (memo) or bottom-up (tab)
5. **Space optimize** — keep only needed previous states

---

## Memoization vs Tabulation

### Memoization (Top-Down)

```java
int[] memo;
int fib(int n) {
    if (n <= 1) return n;
    if (memo[n] != -1) return memo[n];
    return memo[n] = fib(n - 1) + fib(n - 2);
}
```

**Pros:** Easy to write (recursion). Solve only needed states.
**Cons:** Stack space; recursion overhead.

### Tabulation (Bottom-Up)

```java
int fib(int n) {
    if (n <= 1) return n;
    int[] dp = new int[n + 1];
    dp[0] = 0; dp[1] = 1;
    for (int i = 2; i <= n; i++) dp[i] = dp[i - 1] + dp[i - 2];
    return dp[n];
}
```

**Pros:** No recursion / stack overflow risk. Easy to space-optimize.
**Cons:** Computes all states (sometimes wasteful).

| | Memo | Tab |
|--|------|-----|
| Stack | O(n) | O(1) |
| Cache | HashMap / array | Array |
| All states computed | ❌ | ✅ |
| Easier to write | ✅ | (depends) |

→ Both **same time complexity**, different style.

---

## DP Identification Checklist

Look for:

- "Find minimum / maximum / count of ways"
- "Optimal" anywhere
- Recursion with overlap (same params reappear)
- Choices at each step (take / skip / split)

If problem says "find any one valid path" → may be DFS / BFS, not DP.

---

## 1D DP — Fibonacci

```java
int fib(int n) {
    if (n <= 1) return n;
    int prev2 = 0, prev1 = 1;
    for (int i = 2; i <= n; i++) {
        int curr = prev1 + prev2;
        prev2 = prev1;
        prev1 = curr;
    }
    return prev1;
}
```

→ Time **O(n)**, space **O(1)** (rolling).

---

## Climbing Stairs

**Q:** Reach Nth stair, take 1 or 2 steps at a time. Number of ways?

### Recurrence

```
ways(n) = ways(n-1) + ways(n-2)
```

(Same as Fibonacci.)

```java
int climbStairs(int n) {
    if (n <= 2) return n;
    int prev2 = 1, prev1 = 2;
    for (int i = 3; i <= n; i++) {
        int curr = prev1 + prev2;
        prev2 = prev1; prev1 = curr;
    }
    return prev1;
}
```

### Variants

- 1, 2, 3 steps allowed → `dp[i] = dp[i-1] + dp[i-2] + dp[i-3]`
- Min cost climbing stairs — at each step pick min(prev, prev2) + cost

---

## House Robber

**Q:** Cannot rob two adjacent houses. Maximize loot.

### State

`dp[i]` = max loot up to house i.

### Recurrence

```
dp[i] = max(dp[i-1],            // skip current
            dp[i-2] + nums[i])  // rob current
```

```java
int rob(int[] nums) {
    int prev2 = 0, prev1 = 0;
    for (int n : nums) {
        int curr = Math.max(prev1, prev2 + n);
        prev2 = prev1; prev1 = curr;
    }
    return prev1;
}
```

→ Time **O(n)**, space **O(1)**.

### Variants

- House Robber II (circular) — solve excluding first OR last; take max
- House Robber III (binary tree) — DP on tree

---

## Max Product Subarray

**Trick:** Track both max + min (negative × negative = positive).

```java
int maxProduct(int[] arr) {
    int max = arr[0], min = arr[0], result = arr[0];
    for (int i = 1; i < arr.length; i++) {
        int curr = arr[i];
        if (curr < 0) { int t = max; max = min; min = t; }
        max = Math.max(curr, max * curr);
        min = Math.min(curr, min * curr);
        result = Math.max(result, max);
    }
    return result;
}
```

→ Time **O(n)**, space **O(1)**.

---

## 2D DP — Unique Paths

**Q:** Robot at top-left of m×n grid, move right or down. Number of paths to bottom-right.

### State

`dp[i][j]` = paths to (i, j).

### Recurrence

```
dp[i][j] = dp[i-1][j] + dp[i][j-1]
```

```java
int uniquePaths(int m, int n) {
    int[][] dp = new int[m][n];
    for (int i = 0; i < m; i++) dp[i][0] = 1;
    for (int j = 0; j < n; j++) dp[0][j] = 1;
    
    for (int i = 1; i < m; i++)
        for (int j = 1; j < n; j++)
            dp[i][j] = dp[i-1][j] + dp[i][j-1];
    return dp[m-1][n-1];
}
```

### Space optimize → 1D

```java
int[] dp = new int[n];
Arrays.fill(dp, 1);
for (int i = 1; i < m; i++)
    for (int j = 1; j < n; j++)
        dp[j] += dp[j-1];
return dp[n-1];
```

→ Space **O(n)**.

### Variant — with obstacles

`dp[i][j] = 0` if obstacle.

---

## Edit Distance

**Q:** Min operations to convert s1 to s2 (insert, delete, replace).

### State

`dp[i][j]` = edit distance between s1[0..i] and s2[0..j].

### Recurrence

```
if s1[i-1] == s2[j-1]: dp[i][j] = dp[i-1][j-1]
else: dp[i][j] = 1 + min(dp[i-1][j-1],   // replace
                          dp[i-1][j],     // delete
                          dp[i][j-1])     // insert
```

```java
int editDistance(String s1, String s2) {
    int m = s1.length(), n = s2.length();
    int[][] dp = new int[m + 1][n + 1];
    
    for (int i = 0; i <= m; i++) dp[i][0] = i;
    for (int j = 0; j <= n; j++) dp[0][j] = j;
    
    for (int i = 1; i <= m; i++) {
        for (int j = 1; j <= n; j++) {
            if (s1.charAt(i - 1) == s2.charAt(j - 1)) dp[i][j] = dp[i-1][j-1];
            else dp[i][j] = 1 + Math.min(dp[i-1][j-1], Math.min(dp[i-1][j], dp[i][j-1]));
        }
    }
    return dp[m][n];
}
```

→ Time **O(m × n)**, space **O(m × n)**.

→ Space optimize: 2 rows.

---

## Longest Common Subsequence

**Q:** Length of longest common subsequence between s1 and s2.

### Recurrence

```
if s1[i-1] == s2[j-1]: dp[i][j] = dp[i-1][j-1] + 1
else: dp[i][j] = max(dp[i-1][j], dp[i][j-1])
```

```java
int lcs(String s1, String s2) {
    int m = s1.length(), n = s2.length();
    int[][] dp = new int[m + 1][n + 1];
    
    for (int i = 1; i <= m; i++)
        for (int j = 1; j <= n; j++) {
            if (s1.charAt(i - 1) == s2.charAt(j - 1)) dp[i][j] = dp[i-1][j-1] + 1;
            else dp[i][j] = Math.max(dp[i-1][j], dp[i][j-1]);
        }
    return dp[m][n];
}
```

→ Time **O(m × n)**.

### Variants

- **Longest palindromic subsequence** = LCS(s, reverse(s))
- **Shortest common supersequence** = m + n - LCS(s1, s2)

---

## 0/1 Knapsack

**Q:** Items with weights[i] + values[i]. Pick subset with total weight ≤ W; maximize value. Each item used **at most once**.

### State

`dp[i][w]` = max value using first i items with capacity w.

### Recurrence

```
dp[i][w] = max(dp[i-1][w],                                  // skip
               dp[i-1][w - weights[i-1]] + values[i-1])     // take
```

```java
int knapsack(int[] weights, int[] values, int W) {
    int n = weights.length;
    int[][] dp = new int[n + 1][W + 1];
    
    for (int i = 1; i <= n; i++) {
        for (int w = 0; w <= W; w++) {
            dp[i][w] = dp[i-1][w];
            if (w >= weights[i-1]) {
                dp[i][w] = Math.max(dp[i][w], dp[i-1][w - weights[i-1]] + values[i-1]);
            }
        }
    }
    return dp[n][W];
}
```

### Space optimize → 1D (reverse iteration)

```java
int[] dp = new int[W + 1];
for (int i = 0; i < n; i++) {
    for (int w = W; w >= weights[i]; w--) {
        dp[w] = Math.max(dp[w], dp[w - weights[i]] + values[i]);
    }
}
return dp[W];
```

→ **Reverse iteration** prevents using item twice (0/1 constraint).

---

## Unbounded Knapsack — Coin Change

### Coin change — min coins

```java
int coinChange(int[] coins, int amount) {
    int[] dp = new int[amount + 1];
    Arrays.fill(dp, amount + 1);
    dp[0] = 0;
    
    for (int i = 1; i <= amount; i++) {
        for (int c : coins) {
            if (i - c >= 0) dp[i] = Math.min(dp[i], dp[i - c] + 1);
        }
    }
    return dp[amount] > amount ? -1 : dp[amount];
}
```

### Coin change — number of ways

```java
int change(int amount, int[] coins) {
    int[] dp = new int[amount + 1];
    dp[0] = 1;
    for (int c : coins) {                  // outer = coins (avoid double counting)
        for (int i = c; i <= amount; i++) {
            dp[i] += dp[i - c];
        }
    }
    return dp[amount];
}
```

→ Outer loop coins → combinations. Outer loop amount → permutations.

---

## DP on Strings — Word Break

**Q:** Can string s be broken into dictionary words?

```java
boolean wordBreak(String s, List<String> dict) {
    Set<String> set = new HashSet<>(dict);
    int n = s.length();
    boolean[] dp = new boolean[n + 1];
    dp[0] = true;
    
    for (int i = 1; i <= n; i++) {
        for (int j = 0; j < i; j++) {
            if (dp[j] && set.contains(s.substring(j, i))) {
                dp[i] = true;
                break;
            }
        }
    }
    return dp[n];
}
```

→ Time **O(n² × m)** (m = avg word length).

### Other DP-on-strings

- **Regex matching** (`*`, `.`)
- **Wildcard matching** (`*`, `?`)
- **Distinct subsequences**

---

## Common Output Traps

### Q1. Stack overflow in deep memo

For n = 10⁶ Fibonacci recursion → blow stack.

→ Iterative tabulation.

### Q2. Initialization wrong

```java
int[] dp = new int[n];      // default 0
// for "min" problems, want INF
Arrays.fill(dp, Integer.MAX_VALUE);
```

### Q3. Knapsack 1D — wrong iteration order

```java
for (int w = weights[i]; w <= W; w++)   // ❌ allows item twice (unbounded)
for (int w = W; w >= weights[i]; w--)   // ✅ 0/1
```

### Q4. Coin change — combinations vs permutations

Outer loop matters.

### Q5. Off-by-one

`dp[i]` = answer for first i (0-indexed string s[i-1]) — common confusion.

---

## Pitfalls

1. **Wrong state definition** — debug by writing example transition.
2. **Base case missing** → wrong answer / infinite recursion.
3. **Space O(n²) when O(n) possible** — interviewer expects optimization.
4. **Forgetting integer overflow** — use `long` for products / large sums.
5. **Memo with `Integer` boxed** — slower; use array.
6. **Memoization key** — must include all changing params.
7. **Iterative order wrong** — bottom-up needs sub-problems computed first.
8. **Combinations vs permutations** in counting problems — outer loop choice.
9. **`Arrays.fill` skipped** — default 0 doesn't suit min problems.
10. **Using HashMap vs int[]** — array faster (no autobox).

---

## Cheat Sheet

| Pattern | Recurrence | Examples |
|---------|------------|----------|
| Fibonacci-like | dp[i] = dp[i-1] + dp[i-2] | Climbing stairs |
| Pick / skip | dp[i] = max(dp[i-1], dp[i-2] + a[i]) | House Robber |
| Grid paths | dp[i][j] = dp[i-1][j] + dp[i][j-1] | Unique paths |
| LCS-style | dp[i][j] from dp[i-1][j-1], dp[i-1][j], dp[i][j-1] | LCS, edit distance |
| 0/1 Knapsack | dp[w] = max(skip, take) — reverse iter | Subset sum |
| Unbounded knapsack | dp[w] = sum/min — forward iter | Coin change |
| State machine | dp[i][s] for state s | Stock buy/sell with cooldown |

| Approach | Pros | Cons |
|----------|------|------|
| Memoization | Easy to write | Stack risk |
| Tabulation | No recursion | All states |

---

## Practice (LeetCode)

| # | Problem | Pattern |
|---|---------|---------|
| 70 | Climbing Stairs | 1D Fibonacci |
| 198 | House Robber | Pick/skip |
| 213 | House Robber II | Circular |
| 53 | Maximum Subarray | Kadane |
| 152 | Maximum Product Subarray | Track max+min |
| 322 | Coin Change | Unbounded knapsack |
| 518 | Coin Change 2 | Counting |
| 416 | Partition Equal Subset Sum | 0/1 knapsack |
| 62 | Unique Paths | 2D grid |
| 63 | Unique Paths II (obstacles) | 2D |
| 64 | Min Path Sum | 2D grid |
| 1143 | LCS | LCS |
| 72 | Edit Distance | LCS-style |
| 5 | Longest Palindromic Substring | DP / expand |
| 516 | Longest Palindromic Subsequence | LCS |
| 139 | Word Break | DP on strings |
| 300 | LIS | 1D |
| 121 | Best Time Buy/Sell I | 1D |
| 188 | Best Time Buy/Sell IV | State machine |
| 10 | Regex Matching | DP on strings |
| 44 | Wildcard Matching | DP on strings |

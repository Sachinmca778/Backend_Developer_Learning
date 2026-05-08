# Array & String Problems

## Status: Not Started

---

## Table of Contents

1. [Two Pointers](#two-pointers)
2. [Sliding Window](#sliding-window)
3. [Prefix Sum](#prefix-sum)
4. [Kadane's Algorithm](#kadanes-algorithm)
5. [Anagram Checks](#anagram-checks)
6. [Palindrome](#palindrome)
7. [String Manipulation](#string-manipulation)
8. [Matrix Traversal](#matrix-traversal)
9. [Binary Search on Array](#binary-search-on-array)
10. [Common Output Traps](#common-output-traps)
11. [Pitfalls](#pitfalls)
12. [Cheat Sheet](#cheat-sheet)

---

## Two Pointers

**Pattern:** Do pointers — opposite ends ya same direction (fast/slow). Reduce O(n²) → O(n).

### Type 1: Opposite ends (sorted array / palindrome)

#### Pair sum in sorted array

```java
int[] twoSumSorted(int[] arr, int target) {
    int l = 0, r = arr.length - 1;
    while (l < r) {
        int sum = arr[l] + arr[r];
        if (sum == target) return new int[]{l, r};
        if (sum < target) l++;
        else r--;
    }
    return new int[]{-1, -1};
}
```

→ Time **O(n)**, space **O(1)**.

#### Container with most water

```java
int maxArea(int[] h) {
    int l = 0, r = h.length - 1, max = 0;
    while (l < r) {
        int area = Math.min(h[l], h[r]) * (r - l);
        max = Math.max(max, area);
        if (h[l] < h[r]) l++;
        else r--;
    }
    return max;
}
```

### Type 2: Same direction (fast-slow)

#### Remove duplicates from sorted array (in-place)

```java
int removeDuplicates(int[] arr) {
    if (arr.length == 0) return 0;
    int slow = 0;
    for (int fast = 1; fast < arr.length; fast++) {
        if (arr[fast] != arr[slow]) {
            slow++;
            arr[slow] = arr[fast];
        }
    }
    return slow + 1;
}
```

#### Move zeros to end

```java
void moveZeros(int[] arr) {
    int slow = 0;
    for (int fast = 0; fast < arr.length; fast++) {
        if (arr[fast] != 0) {
            int t = arr[slow]; arr[slow] = arr[fast]; arr[fast] = t;
            slow++;
        }
    }
}
```

---

## Sliding Window

**Pattern:** Subarray / substring with **constraint** — maintain window L..R, expand R, shrink L when violates.

### Fixed-size window — Max sum of size K

```java
int maxSumK(int[] arr, int k) {
    int sum = 0, max;
    for (int i = 0; i < k; i++) sum += arr[i];
    max = sum;
    for (int i = k; i < arr.length; i++) {
        sum += arr[i] - arr[i - k];
        max = Math.max(max, sum);
    }
    return max;
}
```

### Variable window — Longest substring without repeating

```java
int lengthOfLongestSubstring(String s) {
    Map<Character, Integer> last = new HashMap<>();
    int l = 0, max = 0;
    for (int r = 0; r < s.length(); r++) {
        char c = s.charAt(r);
        if (last.containsKey(c) && last.get(c) >= l) {
            l = last.get(c) + 1;
        }
        last.put(c, r);
        max = Math.max(max, r - l + 1);
    }
    return max;
}
```

→ Time **O(n)**.

### Variable window — Min size subarray sum ≥ target

```java
int minSubArrayLen(int target, int[] arr) {
    int l = 0, sum = 0, min = Integer.MAX_VALUE;
    for (int r = 0; r < arr.length; r++) {
        sum += arr[r];
        while (sum >= target) {
            min = Math.min(min, r - l + 1);
            sum -= arr[l++];
        }
    }
    return min == Integer.MAX_VALUE ? 0 : min;
}
```

### Template

```java
int l = 0;
for (int r = 0; r < n; r++) {
    // expand: add arr[r] to window
    while (windowInvalid()) {
        // shrink: remove arr[l]; l++
    }
    // update result
}
```

---

## Prefix Sum

**Pattern:** Pre-compute cumulative sum → answer **range queries in O(1)**.

```java
int[] prefix = new int[n + 1];
for (int i = 0; i < n; i++) prefix[i + 1] = prefix[i] + arr[i];

// Range sum [l, r] = prefix[r+1] - prefix[l]
```

### Subarray sum equals K (count)

```java
int subarraySum(int[] arr, int k) {
    Map<Integer, Integer> map = new HashMap<>();
    map.put(0, 1);
    int sum = 0, count = 0;
    for (int n : arr) {
        sum += n;
        count += map.getOrDefault(sum - k, 0);
        map.merge(sum, 1, Integer::sum);
    }
    return count;
}
```

→ Trick: `sum - k` already seen → subarray exists. **O(n)**.

---

## Kadane's Algorithm

**Pattern:** Max subarray sum in **O(n)**.

```java
int maxSubArray(int[] arr) {
    int curMax = arr[0], allMax = arr[0];
    for (int i = 1; i < arr.length; i++) {
        curMax = Math.max(arr[i], curMax + arr[i]);
        allMax = Math.max(allMax, curMax);
    }
    return allMax;
}
```

### Logic

At each index: "extend previous subarray OR start fresh".

### Variants

- **Max product subarray** — track max + min (negative * negative)
- **Circular array Kadane** — `total - minSubarray` OR normal max

---

## Anagram Checks

### Approach 1 — Sort

```java
boolean isAnagram(String a, String b) {
    if (a.length() != b.length()) return false;
    char[] x = a.toCharArray(), y = b.toCharArray();
    Arrays.sort(x); Arrays.sort(y);
    return Arrays.equals(x, y);
}
```

→ **O(n log n)**.

### Approach 2 — Frequency count

```java
boolean isAnagram(String a, String b) {
    if (a.length() != b.length()) return false;
    int[] freq = new int[26];
    for (int i = 0; i < a.length(); i++) {
        freq[a.charAt(i) - 'a']++;
        freq[b.charAt(i) - 'a']--;
    }
    for (int f : freq) if (f != 0) return false;
    return true;
}
```

→ **O(n)** time, **O(1)** space (assuming lowercase a-z).

### Group anagrams

```java
List<List<String>> groupAnagrams(String[] strs) {
    Map<String, List<String>> map = new HashMap<>();
    for (String s : strs) {
        char[] c = s.toCharArray();
        Arrays.sort(c);
        map.computeIfAbsent(new String(c), k -> new ArrayList<>()).add(s);
    }
    return new ArrayList<>(map.values());
}
```

---

## Palindrome

### Standard check

```java
boolean isPalindrome(String s) {
    int l = 0, r = s.length() - 1;
    while (l < r) {
        if (s.charAt(l) != s.charAt(r)) return false;
        l++; r--;
    }
    return true;
}
```

### Valid palindrome (alphanumeric only, ignore case)

```java
boolean isPalindrome(String s) {
    int l = 0, r = s.length() - 1;
    while (l < r) {
        while (l < r && !Character.isLetterOrDigit(s.charAt(l))) l++;
        while (l < r && !Character.isLetterOrDigit(s.charAt(r))) r--;
        if (Character.toLowerCase(s.charAt(l)) != Character.toLowerCase(s.charAt(r)))
            return false;
        l++; r--;
    }
    return true;
}
```

### Longest palindromic substring (Expand around center)

```java
String longestPalindrome(String s) {
    if (s.isEmpty()) return "";
    int start = 0, end = 0;
    for (int i = 0; i < s.length(); i++) {
        int l1 = expand(s, i, i);       // odd length
        int l2 = expand(s, i, i + 1);   // even length
        int len = Math.max(l1, l2);
        if (len > end - start) {
            start = i - (len - 1) / 2;
            end = i + len / 2;
        }
    }
    return s.substring(start, end + 1);
}
int expand(String s, int l, int r) {
    while (l >= 0 && r < s.length() && s.charAt(l) == s.charAt(r)) { l--; r++; }
    return r - l - 1;
}
```

→ **O(n²)** time, **O(1)** space. (Manacher's algorithm gives O(n) — rarely asked.)

---

## String Manipulation

### Reverse string in-place

```java
void reverse(char[] s) {
    int l = 0, r = s.length - 1;
    while (l < r) {
        char t = s[l]; s[l++] = s[r]; s[r--] = t;
    }
}
```

### Reverse words in string

```java
String reverseWords(String s) {
    String[] parts = s.trim().split("\\s+");
    Collections.reverse(Arrays.asList(parts));
    return String.join(" ", parts);
}
```

### First unique character

```java
int firstUniqChar(String s) {
    int[] freq = new int[26];
    for (char c : s.toCharArray()) freq[c - 'a']++;
    for (int i = 0; i < s.length(); i++)
        if (freq[s.charAt(i) - 'a'] == 1) return i;
    return -1;
}
```

### String to integer (atoi-lite)

```java
int myAtoi(String s) {
    s = s.strip();
    if (s.isEmpty()) return 0;
    int sign = 1, i = 0;
    if (s.charAt(0) == '-' || s.charAt(0) == '+') {
        sign = s.charAt(0) == '-' ? -1 : 1;
        i++;
    }
    long res = 0;
    while (i < s.length() && Character.isDigit(s.charAt(i))) {
        res = res * 10 + (s.charAt(i) - '0');
        if (sign * res > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (sign * res < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        i++;
    }
    return (int)(sign * res);
}
```

---

## Matrix Traversal

### Spiral order

```java
List<Integer> spiral(int[][] m) {
    List<Integer> res = new ArrayList<>();
    int top = 0, bot = m.length - 1, l = 0, r = m[0].length - 1;
    while (top <= bot && l <= r) {
        for (int j = l; j <= r; j++) res.add(m[top][j]);
        top++;
        for (int i = top; i <= bot; i++) res.add(m[i][r]);
        r--;
        if (top <= bot) {
            for (int j = r; j >= l; j--) res.add(m[bot][j]);
            bot--;
        }
        if (l <= r) {
            for (int i = bot; i >= top; i--) res.add(m[i][l]);
            l++;
        }
    }
    return res;
}
```

### Rotate matrix 90° clockwise (in-place)

```java
void rotate(int[][] m) {
    int n = m.length;
    // Transpose
    for (int i = 0; i < n; i++)
        for (int j = i + 1; j < n; j++) {
            int t = m[i][j]; m[i][j] = m[j][i]; m[j][i] = t;
        }
    // Reverse each row
    for (int[] row : m) {
        int l = 0, r = n - 1;
        while (l < r) { int t = row[l]; row[l++] = row[r]; row[r--] = t; }
    }
}
```

### Set matrix zeros (O(1) space)

Use first row + col as marker.

---

## Binary Search on Array

### Standard binary search

```java
int binarySearch(int[] arr, int t) {
    int l = 0, r = arr.length - 1;
    while (l <= r) {
        int m = l + (r - l) / 2;     // avoid overflow
        if (arr[m] == t) return m;
        if (arr[m] < t) l = m + 1;
        else r = m - 1;
    }
    return -1;
}
```

### First occurrence

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
```

### Last occurrence — same template, `l = m + 1` on equality.

### Search in rotated sorted array

```java
int searchRotated(int[] arr, int t) {
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

### Find peak element

```java
int findPeak(int[] arr) {
    int l = 0, r = arr.length - 1;
    while (l < r) {
        int m = l + (r - l) / 2;
        if (arr[m] > arr[m + 1]) r = m;
        else l = m + 1;
    }
    return l;
}
```

### Java's `Arrays.binarySearch` — beware

Returns `-(insertion_point) - 1` if not found. Don't compare with -1 directly.

---

## Common Output Traps

### Q1. `int mid = (l + r) / 2` overflow

```java
int mid = (l + r) / 2;        // ❌ if l + r > Integer.MAX_VALUE
int mid = l + (r - l) / 2;    // ✅
```

### Q2. String concatenation in loop

```java
String s = "";
for (int i = 0; i < n; i++) s += i;   // ❌ O(n²)
```

→ Use `StringBuilder`.

### Q3. `==` for strings

```java
"abc" == new String("abc")     // false (reference)
```

→ Use `.equals()`.

### Q4. Two-pointer skip duplicate (3Sum trap)

After finding triplet, skip duplicate values:

```java
while (l < r && arr[l] == arr[l+1]) l++;
```

### Q5. Modifying while iterating

`ConcurrentModificationException` — use iterator's `remove()` or copy.

---

## Pitfalls

1. **Off-by-one** in window boundaries — `r - l + 1` for length.
2. **Mid overflow** — always `l + (r - l) / 2`.
3. **String == comparison** — use `equals`.
4. **`StringBuilder` not used** — quadratic concatenation.
5. **Forgetting empty/single edge** — `length() == 0`, `length() == 1`.
6. **Sliding window not shrinking** — infinite loop / wrong window.
7. **Hash collision** for large constraints — `HashMap` works fine; trust it.
8. **Char indexing** — `c - 'a'` for lowercase, `c - 'A'` for uppercase, `c - '0'` for digits.
9. **`Arrays.sort` mutates** — don't expect immutability.
10. **Substring O(n)** in Java — `s.substring(i, j)` copies (Java 7+).

---

## Cheat Sheet

| Pattern | Time | Trigger |
|---------|------|---------|
| Two pointers | O(n) | Sorted / palindrome |
| Sliding window | O(n) | Subarray with constraint |
| Prefix sum | O(n) build, O(1) query | Range queries |
| Kadane | O(n) | Max subarray sum |
| Binary search | O(log n) | Sorted (or modified sorted) |

| String trick | Code |
|--------------|------|
| Char count | `int[26]` |
| In-place reverse | Two-pointer swap |
| Build big string | `StringBuilder` |
| Lowercase | `Character.toLowerCase` |

---

## Practice (LeetCode)

| # | Problem | Pattern |
|---|---------|---------|
| 1 | Two Sum | HashMap |
| 11 | Container With Most Water | Two Pointers |
| 53 | Maximum Subarray | Kadane |
| 76 | Minimum Window Substring | Sliding Window |
| 121 | Best Time to Buy/Sell Stock | One Pass |
| 153 | Find Min in Rotated Array | Binary Search |
| 167 | Two Sum II Sorted | Two Pointers |
| 209 | Min Size Subarray Sum | Sliding Window |
| 238 | Product Except Self | Prefix |
| 242 | Valid Anagram | Frequency |
| 344 | Reverse String | Two Pointers |
| 560 | Subarray Sum K | Prefix + Map |
| 5 | Longest Palindromic Substring | Expand |
| 49 | Group Anagrams | HashMap |
| 33 | Search in Rotated | Binary Search |
| 48 | Rotate Image | Matrix |
| 54 | Spiral Matrix | Matrix |

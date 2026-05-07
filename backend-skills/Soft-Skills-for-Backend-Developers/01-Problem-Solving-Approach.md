# Problem-Solving Approach

## Status: Not Started

---

## Table of Contents

1. [Why Approach Matters](#why-approach-matters)
2. [Step 1 — Understand the Problem](#step-1--understand-the-problem)
3. [Step 2 — Ask Clarifying Questions](#step-2--ask-clarifying-questions)
4. [Step 3 — Break It Down](#step-3--break-it-down)
5. [Step 4 — Identify Unknowns](#step-4--identify-unknowns)
6. [Step 5 — MVP Thinking](#step-5--mvp-thinking)
7. [Step 6 — Edge Cases](#step-6--edge-cases)
8. [Step 7 — Algorithm & Data Structure Choice](#step-7--algorithm--data-structure-choice)
9. [Step 8 — Time / Space Complexity](#step-8--time--space-complexity)
10. [Step 9 — Code, Test, Iterate](#step-9--code-test-iterate)
11. [Common Pitfalls](#common-pitfalls)
12. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Why Approach Matters

**Matlab:** Senior dev vs junior dev ka **biggest visible** difference — kaise sochte hain problem ke baare mein, **before** coding shuru.

> Junior: "Code chalu kar do, dekha jaayega."
> Senior: "Pehle samjho — kya banana hai, kyun, kaun use karega, kya tooot sakta hai?"

### Cost of Skipping

```
Bad assumption found in:
- 5 min discussion → 5 min cost
- After coding 1 day → 1 day cost
- After deploy to prod → days of damage + on-call pain
```

Sochne ka time **never wasted**.

---

## Step 1 — Understand the Problem

### Restate in Your Own Words

Stakeholder ne jo bola, **wapas explain karo**:

> "Toh aapko chahiye — orders API mein filter add karna ho user ke region ke hisaab se, aur sirf last 30 din ke orders dikhayein. Sahi samjha?"

→ Mismatched understanding **early** pakad mein aata hai.

### Identify the Goal vs Implementation

User asks: *"Add Redis cache to orders endpoint"*

→ Better question: *"Endpoint slow kyun lag raha hai? Kya P99 latency target hai? Kya bottleneck DB hai ya something else?"*

Often the **stated solution** isn't the right one. Get to the **underlying problem**.

### Inputs / Outputs / Constraints

```
Input:    GET /orders?region=IN&from=2024-01-01
Output:   List of orders, sorted by date desc, paginated
Constraints:
  - Auth required (JWT)
  - Only orders user can see (BOLA check)
  - P95 < 200ms
  - Pagination required (max 100 per page)
  - Timezone? (UTC vs user TZ?)
```

---

## Step 2 — Ask Clarifying Questions

### Categories to Probe

#### Functional

- "What if region is missing? Default to all?"
- "Empty result — return `[]` or 404?"
- "Date range max — last 1 year? Lifetime?"
- "Sort order — newest first?"

#### Non-Functional

- "Expected QPS?"
- "Acceptable latency?"
- "Eventual consistency okay or strong required?"
- "Audit log needed?"

#### Edge Cases

- "What if user has 1M orders?"
- "What if region = empty string?"
- "Timezone — input UTC or user-local?"

#### Integration

- "Who calls this? Mobile app? Web? Both?"
- "Existing endpoints to align with?"
- "Versioning — `/v1` already exists?"

### Pro Tip: Ask in Writing

Slack/Jira mein questions likho — interpretation later **debate-able**. Verbal can be forgotten.

### Don't Be Afraid to Ask

Junior fear: "Stupid question lagega." Reality: **everyone** needs context. Asking = caring about correctness.

---

## Step 3 — Break It Down

Big problem = scary. Small chunks = doable.

### Decomposition Example

**Task:** "Build user notifications system."

```
Notifications System
├── Domain Model
│   ├── Notification entity
│   ├── Channel enum (email, push, in-app)
│   └── Status (pending, sent, read)
├── Storage
│   ├── DB schema
│   └── Index design
├── Sending
│   ├── Email integration (SES)
│   ├── Push integration (FCM)
│   └── Retry logic
├── API
│   ├── List user's notifications
│   ├── Mark as read
│   └── Update preferences
├── Background
│   └── Worker for sending
└── Observability
    ├── Metrics (sent, failed, latency)
    └── Logs (success, failures)
```

→ Each leaf = 1-2 day task. Estimable, testable, reviewable.

### MECE Principle

**Mutually Exclusive, Collectively Exhaustive** — no overlap, no gap.

```
✅ Bad: { read, unread, important }     ← important overlaps with read+unread
✅ Good: { read, unread } × { important, normal }
```

---

## Step 4 — Identify Unknowns

> "What do I **not** know?"

### Common Unknowns

- Third-party API behavior (rate limits, failure modes)
- Data shape ("Will user IDs be sequential or UUIDs?")
- Performance characteristics ("How fast is this query at 10M rows?")
- Library quirks ("Does Spring Data handle this?")

### Strategies

#### 1. Spike / POC (Time-Boxed)

```
"I'll spike this for 2 hours to see if approach X works.
After 2h I'll either continue or report findings."
```

→ Bound the unknown.

#### 2. Read Documentation / Source

For libraries / APIs — actually read.

#### 3. Ask Someone Who Knows

Senior dev / domain expert / vendor support.

#### 4. Test in Isolation

Reproduce in minimal repo before integrating.

### Don't Pretend to Know

```
Junior trap: "Haan haan kar liyenge" then 3 days later stuck.
Pro: "I'm not sure how X behaves under load. Let me test/research."
```

---

## Step 5 — MVP Thinking

**Matlab:** Smallest version that **delivers value** + can be improved.

### Why MVP First?

- Get **feedback early** before over-engineering
- Reduce **risk** of wrong direction
- Ship something **incrementally**

### MVP Scoping

```
Full vision: User can see notifications, filter by type, mark read,
             archive, snooze, delete, preferences per channel...

MVP: User can see notifications + mark read.
     (filter/archive/snooze/preferences = phase 2)
```

→ MVP works end-to-end **shippable**, even if minimal.

### What MVP Is Not

❌ Half-done features that crash
❌ No tests / no monitoring
❌ Hardcoded values forever

### YAGNI

**You Aren't Gonna Need It** — don't build for hypothetical future.

```
Junior: "Let's add a plugin system in case we need it later."
Pro:    "What's the actual use case? If not now, skip."
```

(Cross-ref: `Code-Quality-&-Best-Practices/01-Clean-Code-Principles.md`.)

---

## Step 6 — Edge Cases

> "What can go wrong?"

### Categories

#### 1. Empty Inputs

- Empty string `""`
- Empty list `[]`
- `null`
- Missing optional field

#### 2. Boundary Values

- 0
- Negative numbers
- Max int
- Single character / single element list
- Maximum allowed size

#### 3. Bad Inputs

- Wrong type (string where int expected)
- SQL injection chars
- Unicode (emoji, RTL)
- Very long strings
- Whitespace-only

#### 4. Concurrent Access

- Two users edit same row → last-write-wins? Optimistic locking?
- Two instances run cron at once?
- Race condition on counter?

#### 5. Failure Modes

- Network timeout
- DB connection lost
- 3rd-party API 500
- Disk full
- Out of memory
- Retried duplicate request

#### 6. Time / Date

- Timezone mismatch
- DST transition
- Year 2038
- Leap year (Feb 29)
- Date in future / past

#### 7. Data Volume

- 1 record (works)
- 1M records (still works?)
- 0 records?

### Edge Case Checklist Example

For "list orders" endpoint:

```
[ ] User has 0 orders
[ ] User has 1 order
[ ] User has 10K orders (pagination)
[ ] Region filter not provided
[ ] Region filter empty string
[ ] Region filter unknown value
[ ] Date range > 1 year
[ ] Date `from` > `to`
[ ] Date in future
[ ] DB connection slow (timeout)
[ ] User exists but not owner of any orders
[ ] User auth expires mid-pagination
```

---

## Step 7 — Algorithm & Data Structure Choice

### Common Patterns

| Need | Choose |
|------|--------|
| Lookup by key | HashMap (O(1)) |
| Ordered iteration | TreeMap / sorted list |
| Membership test | HashSet |
| FIFO | Queue (LinkedList / ArrayDeque) |
| LIFO | Stack / Deque |
| Priority extract | PriorityQueue / heap |
| Range queries | Sorted array / BST / segment tree |
| Top-K | Min-heap of size K |
| Frequent items | HashMap + sort / count-min sketch |
| Path / hierarchy | Tree / graph |
| Caching | LRU (LinkedHashMap or Caffeine) |
| Streaming dedupe | Bloom filter |

### Algorithm Selection Criteria

- **Input size** — small N? bigger O acceptable
- **Update vs read frequency** — index trade-off
- **Memory constraint** — in-memory? streaming?
- **Distribution** — sorted? random?
- **Mutability** — append-only? edits?

### Pragmatic Default

```
Don't optimize until you measure.
Most APIs: simple Map / List + DB index = sufficient.
Reach for fancy DS only when proven need.
```

---

## Step 8 — Time / Space Complexity

### Big-O Quick Reference

```
O(1)        constant    HashMap.get
O(log n)    logarithmic binary search, balanced tree
O(n)        linear      single loop
O(n log n)  log-linear  sort, merge
O(n²)       quadratic   nested loops
O(2ⁿ)       exponential subsets, brute combinatorial
O(n!)       factorial   permutations
```

### Practical Implications

```
n = 10,000:
  O(n)       10K ops    < 1ms
  O(n log n) ~130K ops  < 10ms
  O(n²)      100M ops   ~1s   ← borderline
  O(n³)      10¹² ops   ~hours ❌
```

### Hidden Costs

```java
// Looks O(n)
for (int i = 0; i < list.size(); i++) {
    if (list.contains(target)) ...   // ❌ O(n) inside → overall O(n²)
}

// Better
Set<T> set = new HashSet<>(list);    // O(n) once
for (...) {
    if (set.contains(target)) ...   // O(1)
}
```

### N+1 Queries (DB Hidden Cost)

```java
List<Order> orders = orderRepo.findAll();   // 1 query
for (Order o : orders) {
    o.getCustomer().getName();              // N more queries (lazy load)
}
```

→ Use `JOIN FETCH` / batching. (Cross-ref: `Database-Mastery/04-Query-Optimization.md`)

### Space Trade-offs

```
Time vs Space:
  Caching        more space, less time
  Memoization    same idea
  Compression    less space, more time

Default: optimize for clarity → measure → optimize what's hot.
```

---

## Step 9 — Code, Test, Iterate

### Workflow

```
1. Write smallest test for happy path
2. Implement minimum to pass
3. Add edge case tests
4. Refactor for clarity
5. Code review
6. Iterate
```

### Pseudocode First (For Complex)

```
function processOrders(userId):
    orders = fetch from DB filtered by user
    if empty: return []
    enriched = for each order, attach customer + items
    sorted = order by date desc
    return paginated sorted
```

→ Translate to Java/Python after logic is sound.

### Don't Code in Silence

- Pseudocode in PR description
- Whiteboard / Excalidraw with peer for design
- Async write-up before deep work (forces clarity)

### Iterate

First version = ugly but works. Second = clean. Third = optimal **only if needed**.

---

## Common Pitfalls

### 1. Coding Before Understanding

> "Let me start coding and figure it out as I go."

→ Half a day later, you've built the wrong thing.

### 2. Over-Engineering

> "Let me make it generic for unknown future requirements."

→ Complex code that nobody uses (but has to maintain).

### 3. Ignoring Edge Cases

> "Empty list won't happen in practice."

→ Two days after launch: NullPointerException at 3am.

### 4. Premature Optimization

> "Let me cache and shard from day 1."

→ Slows shipping, adds bugs, often not even bottleneck.

### 5. Not Asking Questions

> "I'll just assume what they meant."

→ Build wrong feature → throw away.

### 6. Solo Heroics

> "I got this — don't need to discuss."

→ Miss obvious approach. Code review reveals needed redesign.

### 7. Not Time-Boxing Spikes

> "Let me figure out this one bug…"

→ 3 days later still stuck. **Always set timer**.

---

## Worked Example

**Problem:** *"Build a function: given list of N transactions, return top-3 spenders this month."*

### Bad Approach

```java
List<Tx> txs = ...;
// Sort by user → group → sum → sort by total → take 3
// O(n log n) with multiple passes — fine for N=1000.
```

### Pro Walk-Through

**Clarify:**
- "What's N typically?" → 10M
- "Top-3 by sum or count?" → sum
- "Real-time or batch?" → daily batch
- "Tie-break?" → arbitrary
- "Filter month — month start UTC?" → UTC

**Constraints:**
- 10M txs / month
- Memory budget tight on worker

**Approach:**
- Stream txs (not load all)
- Maintain `Map<userId, Long>` running sum
- Then top-3: maintain min-heap of size 3 → O(N log 3) = O(N)

**Edge cases:**
- Less than 3 unique users? Return all
- Empty input? Return empty
- DB connection drops mid-stream? Resumable / retryable

**Test cases:**
- 0 users
- < 3 users
- Tied totals
- Single user with many txs
- Very large totals (overflow Long?)

---

## Summary Cheat Sheet

| Step | Question |
|------|----------|
| 1 | What's the actual goal? |
| 2 | What do I need to clarify? |
| 3 | How do I split into small parts? |
| 4 | What don't I know? |
| 5 | What's the MVP? |
| 6 | What can break? (edge cases) |
| 7 | Right data structure / algorithm? |
| 8 | Time / space? Will it scale? |
| 9 | Smallest test → implement → iterate |

| Pitfall | Antidote |
|---------|----------|
| Code-before-think | Restate problem |
| Over-engineer | YAGNI |
| Ignore edge cases | Checklist |
| Premature optimize | Measure first |
| No questions | Ask early, in writing |
| Solo hero | Whiteboard with peer |
| Endless spike | Time-box (2-4h) |

---

## Practice

1. Next ticket: write 5 clarifying questions before coding.
2. For the same ticket: list 10 edge cases.
3. Decompose a "big" task into ≤2 day subtasks.
4. Time-box a spike: 2h max — note findings.
5. After implementing, do Big-O analysis of your solution.
6. Pseudocode a non-trivial function before coding it.

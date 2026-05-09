# Schema Design Patterns

## Status: Complete

---

## Table of Contents

1. [Design Goals](#design-goals)
2. [Embedding vs Referencing](#embedding-vs-referencing)
3. [When to Embed](#when-to-embed)
4. [When to Reference](#when-to-reference)
5. [Extended Reference Pattern](#extended-reference-pattern)
6. [Subset Pattern](#subset-pattern)
7. [Outlier Pattern](#outlier-pattern)
8. [Bucket Pattern](#bucket-pattern)
9. [Document Size & Growth](#document-size--growth)
10. [Pitfalls](#pitfalls)
11. [Cheat Sheet](#cheat-sheet)

---

## Design Goals

> "MongoDB mein **no silver JOIN** — schema **access patterns** ke around design hota hai (query-driven), normalization worship optional."

Ask:

1. Data **ek saath read** hota hai ya alag?
2. Child ka **independent lifecycle** hai?
3. Cardinality **1-to-few** vs **unbounded**?
4. **Write contention** same document pe?
5. **16 MB** document limit boundary?

---

## Embedding vs Referencing

### Embed (denormalize)

```javascript
{
  _id: ObjectId(),
  userName: "Sachin",
  addresses: [
    { city: "Delhi", zip: "110001" },
    { city: "Mumbai", zip: "400001" }
  ]
}
```

### Reference (normalize)

```javascript
// users
{ _id: u1, name: "Sachin" }

// addresses
{ _id: a1, userId: u1, city: "Delhi" }
```

| Embed | Reference |
|-------|-----------|
| Single read round-trip | Multiple queries or `$lookup` |
| Atomic update within doc | Cross-doc consistency needs TX or care |
| Duplication risk | Single source of truth |

---

## When to Embed

Embed when:

- **Always together** accessed (parent-child tight)
- Child ka **no independent queries** / reporting separate rarely
- **1-to-few** relationship — bounded small arrays
- **Strong locality** — UX shows nested structure

Example: **Order + few line items** (bounded N, e.g. < 50) sometimes embedded if always fetched together.

---

## When to Reference

Reference when:

- Child accessed **independently** ("all addresses updated today")
- **1-to-many unbounded** — millions of comments per post
- **Many-to-many** — shared entities (tags reused across posts)
- **High write churn** on child — avoid huge parent doc rewrite
- Need **separate indexing** strategies per entity

Example: **Post + comments** — millions → separate `comments` collection with `postId`.

---

## Extended Reference Pattern

> "**Duplication + pointer** — frequently read fields embed karo, canonical doc reference rakho."

```javascript
// orders collection
{
  _id: oid,
  userId: ObjectId("..."),
  userDisplayName: "Sachin",     // denormalized for list UI
  userAvatarUrl: "https://..."
}
```

### Sync strategy

- App-level on profile update (batch)
- Or eventual consistency acceptable for display-only fields
- **Transactions** if critical consistency window small

→ Saves **`$lookup`** on order history listing.

---

## Subset Pattern

> "**Huge array ka sirf recent/top-K embed** — baaki separate collection."

```javascript
{
  _id: userId,
  recentOrders: [ /* last 5 summaries */ ],
  orderCount: 12450
}
// full orders in orders collection
```

Use cases:

- Social feeds — cache recent N in user doc
- Game leaderboard preview + full scores elsewhere

---

## Outlier Pattern

> "**99% docs simple shape; exceptional docs flagged + richer structure.**"

```javascript
// normal product
{ sku: "P1", name: "Mug", attributes: { color: "red" } }

// outlier product — thousands of attributes
{ sku: "P999", name: "Enterprise SKU", isOutlier: true, attributesDocId: ObjectId("...") }
```

→ Keeps majority docs small/index-friendly; outliers stored extended.

---

## Bucket Pattern

> "**Time-series** — har event alag doc expensive → **bucket per hour/day** with array of measurements."

```javascript
{
  sensorId: "S1",
  date: ISODate("2024-05-08T00:00:00Z"),   // bucket hour/day start
  measurements: [
    { t: ISODate("2024-05-08T10:01:02Z"), temp: 36.5 },
    { t: ISODate("2024-05-08T10:02:11Z"), temp: 36.7 }
  ],
  count: 2,
  minTemp: 36.5,
  maxTemp: 36.7
}
```

### Benefits

- Fewer docs → less index overhead
- Analytics pre-aggregated fields (`min`, `max`, `sum`)

### Tradeoffs

- Bucket **growth** — cap measurements per doc or roll new bucket
- Partial bucket reads — still OK if indexed by sensor+date

---

## Document Size & Growth

| Constraint | Impact |
|------------|--------|
| **16 MB max** per BSON doc | No infinite embed |
| WiredTiger update | Large doc rewrite cost |
| Replication | Large doc churn → oplog/network pressure |

### Anti-pattern

Unbounded **`events: []`** embedded array on user → someday **failure**.

---

## Pitfalls

1. **RDBMS brain** — over-normalizing → JOIN hell / latency.
2. **Over-embedding** — unbounded arrays.
3. **Stale denormalized fields** — no invalidation strategy.
4. **Hot documents** — single doc updated 10k/sec → contention shard inside doc (actually single doc serializes writes).
5. **Sharding later shock** — wrong **shard key** because schema ignored access paths.

---

## Cheat Sheet

| Pattern | Idea |
|---------|------|
| Embed | Together, bounded, few |
| Reference | Independent, huge N |
| Extended Reference | Embed hot fields + `userId` |
| Subset | Top-K embedded, rest external |
| Outlier | Flag exceptions |
| Bucket | Time-series grouping |

---

## Practice

1. E-commerce: **`orders`** embedded line items vs separate **`order_items`** — decide with boundaries.
2. Design **comments on blog** at 10 vs 10M scale.
3. Sketch **Extended Reference** for `posts` listing author name.
4. Implement **bucket** schema for IoT 1 reading/sec per device.
5. Identify **outlier** case in your domain (SKU with 5000 optional specs).

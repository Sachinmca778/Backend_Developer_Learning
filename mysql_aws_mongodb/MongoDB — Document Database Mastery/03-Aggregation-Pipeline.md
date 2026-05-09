# Aggregation Pipeline

## Status: Complete

---

## Table of Contents

1. [Pipeline Mental Model](#pipeline-mental-model)
2. [$match](#match)
3. [$group](#group)
4. [$project](#project)
5. [$sort, $limit, $skip](#sort-limit-skip)
6. [$lookup](#lookup)
7. [$unwind](#unwind)
8. [$facet](#facet)
9. [$bucket / $bucketAuto](#bucket--bucketauto)
10. [$graphLookup](#graphlookup)
11. [$merge / $out](#merge--out)
12. [Performance Tips](#performance-tips)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## Pipeline Mental Model

> "**Stages chain** — har stage ka output next stage ka input. SQL ke multiple CTEs / subqueries jaisa feel."

```javascript
db.orders.aggregate([
  { $match: { status: "COMPLETED" } },
  { $group: { _id: "$userId", totalSpent: { $sum: "$amount" } } },
  { $sort: { totalSpent: -1 } },
  { $limit: 10 }
])
```

### vs find()

| find() | aggregate() |
|--------|---------------|
| Single query shape | Multi-stage transforms |
| Limited joins | **`$lookup`** |
| Simple sorts/filters | Full pipeline algebra |

---

## $match

> "**Filter early** — kam documents aage stages ko pass hon — **biggest perf win**."

```javascript
{ $match: { createdAt: { $gte: ISODate("2024-01-01") }, region: "IN" } }
```

### Tips

- Push **`$match`** as **first** when possible (uses indexes before heavy stages)
- Avoid **`$regex`** heavy patterns without index support

---

## $group

> "**GROUP BY** + accumulators."

```javascript
db.orders.aggregate([
  { $match: { status: "COMPLETED" } },
  {
    $group: {
      _id: { userId: "$userId", month: { $month: "$createdAt" } },
      orderCount: { $sum: 1 },
      revenue: { $sum: "$amount" },
      avgOrder: { $avg: "$amount" },
      minOrder: { $min: "$amount" },
      maxOrder: { $max: "$amount" },
      items: { $push: "$orderId" },
      distinctSkus: { $addToSet: "$sku" }
    }
  }
])
```

### Common accumulators

| Accumulator | Role |
|-------------|------|
| **`$sum`** | Total |
| **`$avg`** | Average |
| **`$min` / `$max`** | Extremes |
| **`$first` / `$last`** | After `$sort` inside group |
| **`$push`** | Array of values |
| **`$addToSet`** | Unique values |
| **`$count`** | Count (also standalone `$count` stage exists) |

### Global group

```javascript
{ $group: { _id: null, total: { $sum: "$amount" } } }
```

---

## $project

> "**Reshape** — pick fields, rename, compute."

```javascript
{
  $project: {
    userId: 1,
    discounted: { $multiply: ["$price", 0.9] },
    fullName: { $concat: ["$first", " ", "$last"] },
    _id: 0
  }
}
```

### $addFields vs $project

- **`$addFields`**: merge new fields into existing doc shape
- **`$project`**: explicit inclusion/exclusion

---

## $sort, $limit, $skip

```javascript
{ $sort: { revenue: -1, userId: 1 } },
{ $skip: 20 },
{ $limit: 10 }
```

⚠️ **`$skip` large values** — expensive; prefer **range pagination** on indexed field.

### $sort memory limit

Large sorts may need **allowDiskUse: true**:

```javascript
db.orders.aggregate([...], { allowDiskUse: true })
```

---

## $lookup

> "**Left outer join** to another collection."

### Basic (equality)

```javascript
db.orders.aggregate([
  {
    $lookup: {
      from: "users",
      localField: "userId",
      foreignField: "_id",
      as: "user"
    }
  },
  { $unwind: { path: "$user", preserveNullAndEmptyArrays: true } }
])
```

### Pipeline form (more control)

```javascript
{
  $lookup: {
    from: "order_items",
    let: { oid: "$_id" },
    pipeline: [
      { $match: { $expr: { $eq: ["$orderId", "$$oid"] } } },
      { $sort: { lineNo: 1 } },
      { $limit: 100 }
    ],
    as: "items"
  }
}
```

→ Nested arrays huge ho sakte hain — **limit** in sub-pipeline.

---

## $unwind

> "**Array ko flatten** — har element alag document."

```javascript
{ $unwind: "$tags" }

// preserve docs with empty/missing array
{ $unwind: { path: "$tags", preserveNullAndEmptyArrays: true } }
```

### includeArrayIndex

```javascript
{ $unwind: { path: "$items", includeArrayIndex: "lineIdx" } }
```

---

## $facet

> "**Parallel sub-pipelines** — ek hi input pe multiple summaries."

```javascript
db.orders.aggregate([
  { $match: { year: 2024 } },
  {
    $facet: {
      byStatus: [
        { $group: { _id: "$status", c: { $sum: 1 } } }
      ],
      topUsers: [
        { $group: { _id: "$userId", spent: { $sum: "$amount" } } },
        { $sort: { spent: -1 } },
        { $limit: 5 }
      ]
    }
  }
])
```

→ Dashboard APIs ke liye useful — **one round-trip**.

---

## $bucket / $bucketAuto

### $bucket — histogram with boundaries

```javascript
{
  $bucket: {
    groupBy: "$price",
    boundaries: [0, 50, 100, 200, 500],
    default: "500+",
    output: { count: { $sum: 1 } }
  }
}
```

### $bucketAuto — auto boundaries

```javascript
{
  $bucketAuto: {
    groupBy: "$score",
    buckets: 5,
    output: { count: { $sum: 1 } }
  }
}
```

---

## $graphLookup

> "**Recursive graph traversal** — social follows, org tree, referrals."

```javascript
db.users.aggregate([
  { $match: { _id: rootId } },
  {
    $graphLookup: {
      from: "users",
      startWith: "$_id",
      connectFromField: "_id",
      connectToField: "referrerId",
      as: "downline",
      maxDepth: 5,
      depthField: "depth"
    }
  }
])
```

⚠️ **Expensive** — depth/unbounded graphs carefully.

---

## $merge / $out

### $out — write results to collection (**replace** collection)

```javascript
[
  { $match: {} },
  { $group: { _id: "$day", total: { $sum: "$amount" } } },
  { $out: "daily_revenue" }
]
```

### $merge — upsert into existing collection

```javascript
{
  $merge: {
    into: "daily_revenue",
    on: "_id",
    whenMatched: "merge",
    whenNotMatched: "insert"
  }
}
```

→ **`$merge`** production **ETL** ke liye better — incremental refresh.

---

## Performance Tips

1. **`$match` first** — index-friendly filter upfront.
2. **`$project` early** — drop fat fields before heavy joins.
3. **`$lookup`** — index **`foreignField`** + **`localField`**.
4. Avoid **`$unwind`** giant arrays — pre-aggregate or limit.
5. Use **`explain("executionStats")`** on aggregation (same spirit as find).
6. **`allowDiskUse`** only when needed — disk slower than RAM.

---

## Pitfalls

1. **`$group` memory** — huge cardinality `_id` → RAM pressure.
2. **`$lookup` array explosion** — cartesian-style blowups.
3. **`$skip` pagination** — bad at scale.
4. **`$out`** drops target — wrong DB wipe horror stories.
5. **16 MB BSON limit** — single stage output doc can't exceed.
6. **Collation** mismatch — join keys case-sensitive surprises.

---

## Cheat Sheet

| Stage | Role |
|-------|------|
| `$match` | Filter |
| `$group` | Aggregate |
| `$project` / `$addFields` | Shape |
| `$sort` | Order |
| `$limit` / `$skip` | Window |
| `$lookup` | Join |
| `$unwind` | Flatten arrays |
| `$facet` | Parallel buckets |
| `$bucket` / `$bucketAuto` | Histogram |
| `$graphLookup` | Graph |
| `$merge` / `$out` | Materialize |

---

## Practice

1. Pipeline: revenue per user last 30 days, top 10.
2. `$lookup` + `$unwind` with **preserveNullAndEmptyArrays**.
3. `$facet`: counts by status + avg order value same query.
4. `$bucketAuto` on latency field into 10 buckets.
5. Explain difference **`$out`** vs **`$merge`**.

# CRUD Operations & Query Operators

## Status: Complete

---

## Table of Contents

1. [Insert](#insert)
2. [Read — find / findOne](#read--find--findone)
3. [Update Operators](#update-operators)
4. [replaceOne](#replaceone)
5. [Delete](#delete)
6. [findOneAndUpdate / findOneAndReplace](#findoneandupdate--findoneandreplace)
7. [Comparison Query Operators](#comparison-query-operators)
8. [Logical Operators](#logical-operators)
9. [Element Operators](#element-operators)
10. [Array Operators](#array-operators)
11. [Projection](#projection)
12. [Cursor Basics](#cursor-basics)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## Insert

### insertOne

```javascript
db.users.insertOne({
  name: "Sachin",
  email: "s@example.com",
  createdAt: new Date()
})
// returns { acknowledged: true, insertedId: ObjectId(...) }
```

### insertMany

```javascript
db.users.insertMany([
  { name: "A" },
  { name: "B" }
], { ordered: false })   // continue on duplicate error if possible
```

### Ordered vs unordered

- **`ordered: true`** (default): stop on first error
- **`ordered: false`**: parallel-friendly bulk — best effort all docs

---

## Read — find / findOne

### findOne

```javascript
db.users.findOne({ email: "s@example.com" })
```

### find — returns **cursor**

```javascript
db.users.find({ status: "ACTIVE" }).limit(20).sort({ createdAt: -1 })
```

### Cursor methods (chain)

```javascript
db.orders.find({ userId: uid })
  .projection({ total: 1, status: 1 })
  .sort({ createdAt: -1 })
  .skip(40)
  .limit(20)
```

### count (modern)

```javascript
db.orders.countDocuments({ status: "PENDING" })      // accurate filter
db.orders.estimatedDocumentCount()                   // metadata fast estimate
```

---

## Update Operators

### updateOne / updateMany

```javascript
db.users.updateOne(
  { _id: ObjectId("...") },
  { $set: { lastLogin: new Date() }, $inc: { loginCount: 1 } }
)

db.users.updateMany(
  { country: "IN" },
  { $set: { region: "APAC" } }
)
```

### Common operators

| Operator | Effect |
|----------|--------|
| **`$set`** | Set field (create if missing) |
| **`$unset`** | Remove field |
| **`$inc`** | Increment numbers |
| **`$mul`** | Multiply |
| **`$min` / `$max`** | Compare & keep min/max |
| **`$rename`** | Rename field |
| **`$currentDate`** | Set date |
| **`$push`** | Append to array |
| **`$pull`** | Remove matching elements from array |
| **`$pullAll`** | Remove many values |
| **`$addToSet`** | Push if not already present |
| **`$pop`** | Remove first/last array element |
| **`$each`** | Used with `$push` for multiple |
| **`$position`** | Insert at index in array |
| **`$slice`** | Trim array after push |

### $push + $each + $sort

```javascript
db.users.updateOne(
  { _id: id },
  {
    $push: {
      scores: {
        $each: [{ v: 90, t: new Date() }],
        $sort: { v: -1 },
        $slice: 10          // keep top 10
      }
    }
  }
)
```

### arrayFilters — update matched array elements

```javascript
db.orders.updateOne(
  { _id: oid },
  { $set: { "items.$[elem].status": "SHIPPED" } },
  { arrayFilters: [{ "elem.sku": "SKU-1" }] }
)
```

---

## replaceOne

> "**Whole document replace** — `_id` stays same; other fields replaced."

```javascript
db.users.replaceOne(
  { _id: id },
  { _id: id, name: "NewName", email: "x@y.com" }   // must include _id if you keep same
)
```

⚠️ Easy to **accidentally drop fields** — prefer `$set` for partial updates.

---

## Delete

```javascript
db.users.deleteOne({ email: "spam@x.com" })
db.users.deleteMany({ inactive: true })
```

---

## findOneAndUpdate / findOneAndReplace

> "**Atomic read-modify-return** — single round-trip."

```javascript
db.counters.findOneAndUpdate(
  { _id: "orders" },
  { $inc: { seq: 1 } },
  { upsert: true, returnDocument: "after" }   // Node driver style
)
```

Shell (legacy):

```javascript
db.counters.findOneAndUpdate(
  { _id: "orders" },
  { $inc: { seq: 1 } },
  { upsert: true, returnNewDocument: true }
)
```

### Options

| Option | Meaning |
|--------|---------|
| `upsert` | Create if not exists |
| `returnDocument: "before"/"after"` | Which version to return |
| `sort` | Tie-breaker which doc to update |
| `projection` | Limit returned fields |

→ Use for **idempotent counters**, **claim next job** patterns.

---

## Comparison Query Operators

```javascript
{ age: { $eq: 25 } }           // same as { age: 25 }
{ age: { $ne: 25 } }
{ age: { $gt: 18, $lte: 65 } }
{ status: { $in: ["A", "B"] } }
{ status: { $nin: ["X"] } }
```

---

## Logical Operators

```javascript
{
  $and: [
    { price: { $gte: 10 } },
    { price: { $lte: 100 } }
  ]
}

{ $or: [ { status: "A" }, { status: "B" } ] }

{ price: { $not: { $gt: 100 } } }

{
  $nor: [
    { discontinued: true },
    { stock: { $lte: 0 } }
  ]
}
```

### Implicit AND

```javascript
{ status: "ACTIVE", country: "IN" }   // AND of conditions
```

---

## Element Operators

```javascript
{ middleName: { $exists: true } }
{ middleName: { $exists: false } }

{ age: { $type: "int" } }              // or BSON type number
{ tags: { $type: "array" } }

{ name: { $regex: /^sa/i } }            // index-friendly if anchored prefix + right collation

{
  $where: "this.age > 18"               // ⚠️ slow — executes JS; avoid in prod
}
```

---

## Array Operators

```javascript
{ tags: { $all: ["mongo", "backend"] } }     // has all elements

{
  orders: {
    $elemMatch: { sku: "X", qty: { $gte: 2 } }
  }
}

{ tags: { $size: 3 } }
```

---

## Projection

> "**Shape** of returned documents."

### Include only listed fields (`1`)

```javascript
db.users.find({}, { name: 1, email: 1, _id: 0 })
```

### Exclude fields (`0`)

```javascript
db.users.find({}, { passwordHash: 0, internalNotes: 0 })
```

### Rule

> "**Don't mix** inclusion (`1`) and exclusion (`0`) **except `_id`**."

✅ `{ name: 1, email: 1, _id: 0 }`

❌ `{ name: 1, password: 0 }` → error

### Positional projection

```javascript
db.orders.find(
  { userId: uid, "items.sku": "SKU-1" },
  { "items.$": 1 }
)
```

### Array slice / elemMatch in projection

```javascript
db.users.find({}, { scores: { $slice: -5 } })   // last 5
```

---

## Cursor Basics

```javascript
var c = db.orders.find({}).batchSize(500)
while (c.hasNext()) printjson(c.next())
```

- **Lazy evaluation** — query runs as you iterate
- **`toArray()`** in drivers loads all — careful memory

---

## Pitfalls

1. **`update` without operator** (legacy) — replaces whole doc — dangerous.
2. **`multi: false` default** in old shell — only first match; use `updateMany`.
3. **`$where`** — full collection scan + JS execution.
4. **Projection mix** — invalid combinations.
5. **Floating money** — use **Decimal128** not binary float.
6. **`find().count()` removed** — use `countDocuments`.
7. **Case-sensitive regex** — huge COLLSCAN without proper index / constraint.
8. **Large `skip`** for pagination — slow; use **range queries** on indexed field.

---

## Cheat Sheet

| Op | Use |
|----|-----|
| insertOne / insertMany | Create |
| find / findOne | Read |
| updateOne / updateMany | Partial update |
| replaceOne | Full replace |
| deleteOne / deleteMany | Delete |
| findOneAndUpdate | Atomic RMW |

| Update | |
|--------|--|
| `$set` | Add/change field |
| `$unset` | Remove field |
| `$inc` | Counter |
| `$push` / `$pull` | Arrays |
| `$addToSet` | Unique push |
| arrayFilters | Update array elements |

| Query | |
|-------|--|
| `$in` / `$nin` | Membership |
| `$elemMatch` | Array object match |
| `$regex` | Pattern |
| `$exists` | Field presence |

---

## Practice

1. Model `users` with optional `phone`; query `$exists` vs `$type`.
2. Implement cart line-item update with **arrayFilters**.
3. Atomic increment counter with **findOneAndUpdate** + upsert.
4. Write query with `$elemMatch` for nested array objects.
5. Explain why `{ a: 1, b: 0 }` projection fails.

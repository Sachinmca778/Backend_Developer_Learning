# CRUD Operations & Query Operators

## Status: Not Started

---

## Table of Contents

1. [CRUD Overview](#crud-overview)
2. [Insert Operations](#insert-operations)
3. [Find / Read Operations](#find--read-operations)
4. [Update Operations](#update-operations)
5. [Update Operators (`$set`, `$inc`, `$push`, ...)](#update-operators-set-inc-push-)
6. [Array Update Operators](#array-update-operators)
7. [Delete Operations](#delete-operations)
8. [`findOneAndUpdate` (Atomic Read-Modify)](#findoneandupdate-atomic-read-modify)
9. [Comparison Query Operators](#comparison-query-operators)
10. [Logical Operators](#logical-operators)
11. [Element & String Operators](#element--string-operators)
12. [Array Query Operators](#array-query-operators)
13. [Projection Rules](#projection-rules)
14. [Cursor Methods](#cursor-methods)
15. [Pitfalls](#pitfalls)
16. [Cheat Sheet](#cheat-sheet)

---

## CRUD Overview

| Op | Methods |
|----|---------|
| **C**reate | `insertOne`, `insertMany` |
| **R**ead | `findOne`, `find`, `aggregate`, `countDocuments` |
| **U**pdate | `updateOne`, `updateMany`, `replaceOne`, `findOneAndUpdate`, `bulkWrite` |
| **D**elete | `deleteOne`, `deleteMany`, `findOneAndDelete` |

→ All operations on a collection: `db.collection.<method>()`.

---

## Insert Operations

### `insertOne`

```js
db.users.insertOne({
  name: "Sachin",
  email: "sachin@example.com",
  age: 28
})

// Result
{
  acknowledged: true,
  insertedId: ObjectId("64f...")
}
```

### `insertMany`

```js
db.users.insertMany([
  { name: "A", age: 25 },
  { name: "B", age: 30 },
  { name: "C", age: 28 }
])
```

### Ordered vs Unordered

```js
db.users.insertMany(docs, { ordered: false })
```

| | `ordered: true` (default) | `ordered: false` |
|--|--------------------------|-----------------|
| Behaviour | Stops at first error | Continues; reports all errors |
| Speed | Sequential | Parallel-ish |
| Use | Strict ordering | Bulk imports |

→ `unordered` faster + tolerant for batch.

### `_id` auto-injected

```js
db.users.insertOne({ name: "X" })
// MongoDB adds: _id: ObjectId(...)
```

---

## Find / Read Operations

### `findOne`

```js
db.users.findOne({ name: "Sachin" })
// returns single doc or null
```

### `find` — returns cursor

```js
db.users.find({ age: { $gt: 25 } })
```

→ Cursor = lazy iterator. Iterate or convert:

```js
db.users.find().toArray()     // load all into array
db.users.find().forEach(doc => print(doc.name))
```

### Cursor methods

```js
db.users.find({ ... })
    .sort({ age: -1 })          // descending
    .skip(10)
    .limit(5)
    .project({ name: 1, _id: 0 })
```

### Count

```js
db.users.countDocuments({ age: { $gt: 25 } })   // accurate
db.users.estimatedDocumentCount()                // fast metadata-based
```

→ `countDocuments` runs query; `estimatedDocumentCount` reads metadata (faster, less accurate).

---

## Update Operations

### `updateOne`

```js
db.users.updateOne(
  { _id: ObjectId("...") },                    // filter
  { $set: { name: "Sachin S" } }                // update
)

// Result
{
  acknowledged: true,
  matchedCount: 1,
  modifiedCount: 1,
  upsertedId: null
}
```

### `updateMany`

```js
db.users.updateMany(
  { active: false },
  { $set: { archived: true, archivedAt: new Date() } }
)
```

### `replaceOne` — replace entire document

```js
db.users.replaceOne(
  { _id: ObjectId("...") },
  { name: "New", email: "new@example.com" }    // entirely new doc (keeps _id)
)
```

⚠️ **All other fields lost** if not in replacement!

### Upsert — insert if not exists

```js
db.users.updateOne(
  { email: "sachin@example.com" },
  { $set: { name: "Sachin", lastLogin: new Date() } },
  { upsert: true }
)
```

→ If filter matches: update. If not: insert (filter + update merged).

### Bulk write

```js
db.users.bulkWrite([
  { insertOne: { document: { name: "A" } } },
  { updateOne: { filter: { name: "B" }, update: { $set: { age: 30 } } } },
  { deleteOne: { filter: { name: "C" } } }
], { ordered: false })
```

→ Multiple operations in one round trip.

---

## Update Operators (`$set`, `$inc`, `$push`, ...)

### `$set` — set field value

```js
db.users.updateOne({ _id: 1 }, { $set: { name: "X", age: 30 } })
```

### `$unset` — remove field

```js
db.users.updateOne({ _id: 1 }, { $unset: { tempField: "" } })
```

### `$inc` — increment numeric

```js
db.products.updateOne({ _id: 1 }, { $inc: { stock: -1 } })
db.users.updateOne({ _id: 1 }, { $inc: { loginCount: 1, score: 10 } })
```

→ Atomic counter — no read-modify-write race.

### `$mul` — multiply

```js
db.products.updateMany({}, { $mul: { price: 1.1 } })   // 10% increase
```

### `$rename`

```js
db.users.updateMany({}, { $rename: { "userName": "name" } })
```

### `$min` / `$max`

```js
db.scores.updateOne({ _id: 1 }, { $max: { highScore: 999 } })   // sets if 999 > existing
```

### `$currentDate` — set to current time

```js
db.users.updateOne({ _id: 1 }, { $currentDate: { lastModified: true } })
db.users.updateOne({ _id: 1 }, { $currentDate: { lastModified: { $type: "timestamp" } } })
```

### `$setOnInsert` — only on upsert insert

```js
db.users.updateOne(
  { email: "x@x.com" },
  {
    $set: { lastLogin: new Date() },           // always
    $setOnInsert: { createdAt: new Date() }    // only on first insert
  },
  { upsert: true }
)
```

---

## Array Update Operators

### `$push` — append

```js
db.users.updateOne(
  { _id: 1 },
  { $push: { hobbies: "skiing" } }
)
```

### `$push` with `$each`

```js
db.users.updateOne(
  { _id: 1 },
  { $push: { hobbies: { $each: ["a", "b", "c"] } } }
)
```

### `$push` with options

```js
{
  $push: {
    scores: {
      $each: [95, 80, 75],
      $sort: -1,         // sort descending
      $slice: 5          // keep top 5 only
    }
  }
}
```

→ "Top-N pattern" — bounded array.

### `$addToSet` — add if not present (set semantics)

```js
db.users.updateOne(
  { _id: 1 },
  { $addToSet: { hobbies: "coding" } }       // skip if already in array
)
```

### `$pull` — remove matching

```js
db.users.updateOne(
  { _id: 1 },
  { $pull: { hobbies: "skiing" } }
)

db.users.updateOne(
  { _id: 1 },
  { $pull: { scores: { $gte: 90 } } }        // remove all scores >= 90
)
```

### `$pop` — remove first / last

```js
{ $pop: { hobbies: 1 } }      // last
{ $pop: { hobbies: -1 } }     // first
```

### `$pullAll`

```js
{ $pullAll: { hobbies: ["a", "b"] } }
```

### Update specific array element by index

```js
{ $set: { "hobbies.0": "newValue" } }
{ $set: { "address.city": "Mumbai" } }
```

→ Dot notation for nested.

### `$` — positional operator (matched element)

```js
db.users.updateOne(
  { _id: 1, "hobbies": "old" },
  { $set: { "hobbies.$": "new" } }
)
```

### `$[]` — all elements

```js
db.users.updateMany(
  {},
  { $inc: { "scores.$[]": 5 } }     // add 5 to every score in every user
)
```

### `$[<identifier>]` with `arrayFilters` — conditional element

```js
db.users.updateOne(
  { _id: 1 },
  { $set: { "scores.$[low]": 0 } },
  { arrayFilters: [{ "low": { $lt: 50 } }] }
)
// Sets all scores < 50 to 0
```

→ Powerful for conditional array updates.

---

## Delete Operations

### `deleteOne`

```js
db.users.deleteOne({ _id: ObjectId("...") })

// Result
{ acknowledged: true, deletedCount: 1 }
```

### `deleteMany`

```js
db.users.deleteMany({ active: false })
db.users.deleteMany({})           // ⚠️ delete ALL documents (collection still exists)
db.users.drop()                    // delete collection itself
```

### `findOneAndDelete` — atomic, returns deleted doc

```js
const deleted = db.users.findOneAndDelete({ _id: 1 })
print(deleted.name)
```

---

## `findOneAndUpdate` (Atomic Read-Modify)

> "**Atomic** — read + update + return in single op."

```js
db.counters.findOneAndUpdate(
  { _id: "users" },
  { $inc: { sequence: 1 } },
  { returnDocument: "after", upsert: true }    // 'after' = return updated; 'before' (default)
)
```

### Use case — auto-increment counter

```js
function nextId(name) {
  return db.counters.findOneAndUpdate(
    { _id: name },
    { $inc: { seq: 1 } },
    { returnDocument: "after", upsert: true }
  ).seq
}

const orderId = nextId("orders")
```

→ Race-safe.

### Options

```js
{
  returnDocument: "after",      // 'before' (default) | 'after'
  upsert: true,
  projection: { name: 1 },
  sort: { priority: -1 },
  arrayFilters: [...]
}
```

---

## Comparison Query Operators

| Op | Meaning | Example |
|----|---------|---------|
| `$eq` | Equal | `{age: {$eq: 25}}` (same as `{age: 25}`) |
| `$ne` | Not equal | `{age: {$ne: 25}}` |
| `$gt` | Greater than | `{age: {$gt: 18}}` |
| `$gte` | Greater than equal | `{age: {$gte: 21}}` |
| `$lt` | Less than | `{age: {$lt: 65}}` |
| `$lte` | Less than equal | `{age: {$lte: 65}}` |
| `$in` | Value in array | `{country: {$in: ['IN', 'US']}}` |
| `$nin` | Value not in array | `{status: {$nin: ['failed']}}` |

### Combining

```js
db.users.find({ age: { $gte: 18, $lte: 65 } })
```

---

## Logical Operators

### `$and`

```js
db.users.find({
  $and: [
    { age: { $gte: 18 } },
    { country: "IN" }
  ]
})

// Equivalent (implicit AND)
db.users.find({ age: { $gte: 18 }, country: "IN" })
```

### `$or`

```js
db.users.find({
  $or: [
    { age: { $lt: 18 } },
    { country: "IN" }
  ]
})
```

### `$not`

```js
db.users.find({ age: { $not: { $gte: 18 } } })
```

### `$nor`

```js
db.users.find({
  $nor: [
    { age: { $gte: 18 } },
    { country: "IN" }
  ]
})
// Neither condition true
```

---

## Element & String Operators

### `$exists`

```js
db.users.find({ phone: { $exists: true } })       // has phone field
db.users.find({ phone: { $exists: false } })      // missing
```

### `$type`

```js
db.users.find({ age: { $type: "int" } })
db.users.find({ age: { $type: ["int", "long"] } })
```

| Type | Alias |
|------|-------|
| 1 | "double" |
| 2 | "string" |
| 3 | "object" |
| 4 | "array" |
| 7 | "objectId" |
| 8 | "bool" |
| 9 | "date" |
| 10 | "null" |
| 16 | "int" |
| 18 | "long" |

### `$regex` — regular expression

```js
db.users.find({ name: { $regex: /^Sa/, $options: "i" } })
db.users.find({ email: /@example\.com$/i })

// Case-insensitive without index → slow on large collections
```

→ Use **anchored prefix** (`^`) for index usage. Otherwise full scan.

→ Cross-ref `04-Indexing-Strategy.md` (text index for full-text).

### `$where` — JS execution (avoid)

```js
db.users.find({ $where: "this.age > 18" })
```

⚠️ Slow + security risk + can't use index. **Don't use.**

---

## Array Query Operators

### Plain match — any element matches

```js
db.users.find({ hobbies: "coding" })       // hobbies array contains "coding"
```

### `$all` — all elements present

```js
db.users.find({ hobbies: { $all: ["coding", "reading"] } })
// Both must be in array
```

### `$elemMatch` — single element matches multiple criteria

```js
// users with at least one score: 80 <= s <= 90
db.users.find({
  scores: { $elemMatch: { $gte: 80, $lte: 90 } }
})
```

→ vs without `$elemMatch`:

```js
db.users.find({ scores: { $gte: 80, $lte: 90 } })
// Different! Match if ANY score >= 80 AND ANY score <= 90 (could be different scores!)
```

### `$size`

```js
db.users.find({ hobbies: { $size: 3 } })       // exactly 3 hobbies
```

→ No `$size: { $gte: 3 }` — use aggregation for that.

---

## Projection Rules

### Include fields (1)

```js
db.users.find({}, { name: 1, email: 1 })
// Returns _id (always) + name + email
```

### Exclude fields (0)

```js
db.users.find({}, { password: 0, tokens: 0 })
// Returns everything except password, tokens
```

### Mixing

❌
```js
{ name: 1, password: 0 }      // ERROR: cannot mix include + exclude
```

✅
```js
{ name: 1, email: 1, _id: 0 }   // exception: _id can always be excluded
```

### Nested

```js
{ "address.city": 1 }
```

### Array slicing

```js
{ hobbies: { $slice: 5 } }              // first 5 elements
{ hobbies: { $slice: -3 } }              // last 3
{ hobbies: { $slice: [10, 5] } }         // skip 10, take 5
```

### `$elemMatch` in projection

```js
{ scores: { $elemMatch: { $gte: 90 } } }
// Returns scores array with only matching elements
```

---

## Cursor Methods

| Method | What |
|--------|------|
| `.sort({field: 1})` | Ascending; `-1` descending |
| `.limit(n)` | Limit |
| `.skip(n)` | Skip (slow at scale — use range queries) |
| `.toArray()` | Materialize all into array |
| `.forEach(fn)` | Iterate |
| `.hasNext()` / `.next()` | Manual iteration |
| `.count()` | (deprecated; use countDocuments) |
| `.hint(index)` | Force index |
| `.explain()` | Plan |
| `.batchSize(n)` | Cursor batch size |
| `.maxTimeMS(ms)` | Query timeout |

### Pagination — bad

```js
db.posts.find().sort({_id: -1}).skip(10000).limit(20)
// Skips 10K docs server-side; slow at scale
```

### Pagination — keyset (good)

```js
// Last seen _id from previous page
db.posts.find({_id: {$lt: lastId}}).sort({_id: -1}).limit(20)
// Uses index, constant time
```

→ Cross-ref: `phase-4 / Performance & Optimization / 03-Query-Optimization.md`.

---

## Pitfalls

1. **`replaceOne` instead of `updateOne`** — wipes other fields.
2. **Forgetting `upsert: true`** when expected → `matchedCount: 0`.
3. **`$elemMatch` confusion** with array conditions.
4. **`{name: 1, password: 0}`** — projection mix error.
5. **`$where` JS** — slow + insecure.
6. **`$regex` without anchor** — full collection scan.
7. **`skip(N)` for pagination** — slow on big N.
8. **`updateMany({}, ...)`** — touches everything; expensive on large collection.
9. **No index on filter** in update — slow + locks docs.
10. **`bulkWrite` ordered: true** swallowing parallelism.
11. **Atomic counter via separate read+update** instead of `$inc` — race condition.
12. **Float `_id`** — surprising sort behaviour.

---

## Cheat Sheet

| CRUD | Methods |
|------|---------|
| Create | `insertOne` / `insertMany` |
| Read | `findOne` / `find` / `aggregate` |
| Update | `updateOne` / `updateMany` / `findOneAndUpdate` |
| Delete | `deleteOne` / `deleteMany` |

| Update Op | Use |
|-----------|-----|
| `$set` | Set field |
| `$unset` | Remove field |
| `$inc` | Atomic counter |
| `$push` | Append array |
| `$addToSet` | Add unique to array |
| `$pull` | Remove matching |
| `$pop` | Remove first/last |
| `$[<id>]` + arrayFilters | Conditional array element |

| Query Op | Use |
|----------|-----|
| `$gt/$gte/$lt/$lte` | Range |
| `$in / $nin` | Value list |
| `$and / $or / $not / $nor` | Logical |
| `$exists / $type` | Element |
| `$regex` (anchored) | String pattern |
| `$elemMatch` | Single array element matches multiple |
| `$all` | All values present |

| Projection | |
|-----------|--|
| `{f: 1}` | Include |
| `{f: 0}` | Exclude |
| `{_id: 0}` | Always allowed |

---

## Practice

1. Atomic counter — implement `nextId()` using `findOneAndUpdate` + `$inc`.
2. Top-N scores in user array — `$push` with `$each`, `$sort`, `$slice`.
3. Conditional array update — set scores < 50 to 0 with `arrayFilters`.
4. Compare `find({a:1, b:1})` vs `find({$and: [...]})` — same plan?
5. Pagination — switch from `skip` to keyset; benchmark difference.
6. Bulk write — insert 10K docs with `ordered: false`; observe throughput.

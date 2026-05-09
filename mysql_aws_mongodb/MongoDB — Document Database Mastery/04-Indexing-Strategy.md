# Indexing Strategy

## Status: Complete

---

## Table of Contents

1. [Why Indexes](#why-indexes)
2. [Single Field Index](#single-field-index)
3. [Compound Index & ESR Rule](#compound-index--esr-rule)
4. [Multikey Index](#multikey-index)
5. [Text Index](#text-index)
6. [Geospatial — 2dsphere](#geospatial--2dsphere)
7. [Hashed Index](#hashed-index)
8. [Index Properties](#index-properties)
9. [Covered Queries](#covered-queries)
10. [explain() Quick Read](#explain-quick-read)
11. [Pitfalls](#pitfalls)
12. [Cheat Sheet](#cheat-sheet)

---

## Why Indexes

> "**Index = ordered pointer structure** — queries fast; writes slightly slower + disk extra."

| Without index | With index |
|---------------|------------|
| **COLLSCAN** full collection | **IXSCAN** seek + scan range |
| O(n) docs examined | O(log n) seek + subset |

---

## Single Field Index

```javascript
db.users.createIndex({ email: 1 })           // ascending
db.users.createIndex({ createdAt: -1 })      // descending
```

### Unique single field

```javascript
db.users.createIndex({ email: 1 }, { unique: true })
```

---

## Compound Index & ESR Rule

```javascript
db.orders.createIndex({ userId: 1, status: 1, createdAt: -1 })
```

### ESR Rule (order of keys)

> **E**quality → **S**ort → **R**ange

| Order | Put fields that are… |
|-------|----------------------|
| **E** | **`$eq` / `$in` (small)** — exact match |
| **S** | **Sort** field in query |
| **R** | **Range** (`$gt`, `$lt`, `$regex` prefix sometimes) last |

### Example query

```javascript
db.orders.find({ userId: x, status: "SHIPPED" }).sort({ createdAt: -1 })
```

Good index: **`{ userId: 1, status: 1, createdAt: -1 }`**

### Left-prefix rule

Index `{ a: 1, b: 1, c: 1 }` supports:

- `{ a }`
- `{ a, b }`
- `{ a, b, c }`

But **not** efficiently `{ b }` alone or `{ c }` alone.

---

## Multikey Index

> "**Array field pe index** → multikey — separate index entries per array element."

```javascript
db.products.createIndex({ tags: 1 })
```

### Behavior

- One doc with 100 tags → **many** index entries
- Compound multikey rules: **at most one** array field can drive multikey index in a compound index (multikey limitations — check version docs for edge cases)

### Memory / selectivity

- High cardinality tags OK
- Low cardinality (`status` few values) → less useful

---

## Text Index

> "**Full-text search** — tokenization, stems, language."

```javascript
db.articles.createIndex({ title: "text", body: "text" }, {
  weights: { title: 10, body: 1 },
  default_language: "english",
  name: "textIdx"
})
```

### Query

```javascript
db.articles.find({ $text: { $search: "mongodb indexing" } })
```

### Sort by relevance

```javascript
db.articles.find(
  { $text: { $search: "mongo" } },
  { score: { $meta: "textScore" } }
).sort({ score: { $meta: "textScore" } })
```

### Limits

- **One text index** per collection (classic model)
- Case/language stemming surprises — test queries

---

## Geospatial — 2dsphere

```javascript
db.places.createIndex({ loc: "2dsphere" })

// GeoJSON
db.places.insertOne({
  name: "Hub",
  loc: { type: "Point", coordinates: [77.2090, 28.6139] }
})
```

### Queries

```javascript
db.places.find({
  loc: {
    $near: {
      $geometry: { type: "Point", coordinates: [77.2, 28.6] },
      $maxDistance: 5000    // meters
    }
  }
})

db.places.find({
  loc: {
    $geoWithin: {
      $geometry: {
        type: "Polygon",
        coordinates: [ /* ... */ ]
      }
    }
  }
})
```

---

## Hashed Index

```javascript
db.users.createIndex({ userId: "hashed" })
```

> "**Sharding hashed shard key** + equality lookups — **not** for range sorts."

- Good: **even distribution** when shard key would hotspot (monotonic `_id`)
- Bad: range queries on hashed field — scatter

---

## Index Properties

### unique

```javascript
db.users.createIndex({ phone: 1 }, { unique: true })
```

Duplicate insert → **E11000** error.

### sparse

```javascript
db.users.createIndex({ passport: 1 }, { sparse: true })
```

> "**Skip docs** where indexed field **missing**."

- Useful optional unique fields — multiple docs without field OK

### TTL

```javascript
db.sessions.createIndex({ expiresAt: 1 }, { expireAfterSeconds: 0 })
```

> "**Background thread** deletes docs where **`expiresAt`** < now."

- Field must be **Date**
- Granularity ~ **60 sec**
- Not for "instant" expiry guarantees

### partial

```javascript
db.orders.createIndex(
  { userId: 1 },
  { partialFilterExpression: { status: "OPEN" } }
)
```

> "**Smaller index** — only OPEN orders — faster writes + RAM."

---

## Covered Queries

> "**Projection only indexed fields** → data served **from index** without fetching doc."

```javascript
db.users.find({ email: "x@y.com" }, { _id: 0, email: 1 })
// needs index including email; _id excluded or included in index
```

Check **`explain`** → **`totalDocsExamined: 0`** ideal case.

---

## explain() Quick Read

```javascript
db.orders.find({ userId: x }).explain("executionStats")
```

Watch:

| Field | Good |
|-------|------|
| **`executionStage`** | **IXSCAN** not COLLSCAN |
| **`totalDocsExamined`** | Close to **`nReturned`** |
| **`totalKeysExamined`** | Reasonable |

---

## Pitfalls

1. **Too many indexes** — write amplification + RAM.
2. **Wrong compound order** — ESR ignore → IXSCAN partial / in-memory sort.
3. **Regex middle wildcard** `.*foo` — often can't use index efficiently.
4. **`$where` / heavy `$expr`** — index bypass.
5. **Multikey index bloat** on massive arrays.
6. **TTL precision** — minute-level, not millisecond SLA.
7. **Unique + sparse** semantics — understand duplicate missing-field behavior.
8. **Case insensitive** — needs **collation** on index + query match.

---

## Cheat Sheet

| Index type | Use |
|------------|-----|
| Single | One filter/sort field |
| Compound | Multi-field — **ESR** order |
| Multikey | Array fields |
| Text | `$text` search |
| 2dsphere | Geo queries |
| Hashed | Shard key / equality |

| Property | Meaning |
|----------|---------|
| unique | No dup values |
| sparse | Skip missing |
| TTL | Auto-delete by date |
| partial | Index subset of docs |

---

## Practice

1. Given queries, pick compound index order with **ESR**.
2. Create **partial** index for `status: "CART"` only.
3. `explain()` a query — identify COLLSCAN vs IXSCAN.
4. Text search with **weights** on title vs body.
5. When **hashed** vs **ranged** index for shard key?

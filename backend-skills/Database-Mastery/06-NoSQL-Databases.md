# NoSQL Databases

## Status: Not Started

---

## Table of Contents

1. [NoSQL Kya Hai?](#nosql-kya-hai)
2. [MongoDB (Document DB)](#mongodb-document-db)
3. [Redis (Key-Value / Data Structure Store)](#redis-key-value--data-structure-store)
4. [Elasticsearch (Search Engine)](#elasticsearch-search-engine)
5. [Comparison & When to Use What](#comparison--when-to-use-what)

---

## NoSQL Kya Hai?

**Matlab:** "Not Only SQL" — relational schema ke alawa data store karne ke models. CAP theorem ke trade-offs alag se handle karte hain.

### Categories

| Type | Examples | Use Case |
|------|---------|----------|
| **Document** | MongoDB, CouchDB | Flexible schema, JSON-like docs |
| **Key-Value** | Redis, DynamoDB | Cache, sessions, simple lookups |
| **Wide-Column** | Cassandra, HBase | Massive scale, time-series |
| **Graph** | Neo4j, ArangoDB | Relationships, social networks |
| **Search** | Elasticsearch, Solr | Full-text search, analytics |

### When NoSQL > SQL?

✅ Schema flexible / evolving
✅ Massive horizontal scale
✅ Specific access patterns (cache, search, graph)
✅ High write throughput

### When SQL > NoSQL?

✅ Complex transactions (ACID)
✅ Strong relationships (joins)
✅ Reporting / analytics on structured data
✅ Strong consistency needs

---

## MongoDB (Document DB)

**Matlab:** Documents (JSON-like BSON) collections mein store. Schema-flexible. Per-document atomic.

### Document Model

```javascript
// users collection
{
  "_id": ObjectId("65a..."),
  "name": "Rahul",
  "email": "rahul@x.com",
  "address": {                    // nested document
    "city": "Bangalore",
    "pincode": "560001"
  },
  "tags": ["dev", "music"],       // array
  "created_at": ISODate("...")
}
```

### CRUD Operations

```javascript
// Create
db.users.insertOne({ name: "Rahul", age: 25 });
db.users.insertMany([{...}, {...}]);

// Read
db.users.findOne({ email: "rahul@x.com" });
db.users.find({ age: { $gte: 18 } });

// Update
db.users.updateOne(
    { _id: ObjectId("...") },
    { $set: { age: 26 }, $push: { tags: "newtag" } }
);

// Delete
db.users.deleteOne({ _id: ObjectId("...") });
```

### Query Operators

```javascript
{ age: { $gt: 18 } }           // > 18
{ age: { $gte: 18, $lte: 65 } } // 18-65
{ status: { $in: ["a", "b"] } }
{ name: { $regex: /^Rah/ } }
{ tags: { $all: ["dev", "music"] } }
{ $or: [{ a: 1 }, { b: 2 }] }
{ $and: [{ a: 1 }, { b: 2 }] }
```

### Aggregation Pipeline

**Matlab:** Stages of transformations — pipeline ki tarah.

```javascript
db.orders.aggregate([
    // Stage 1: filter
    { $match: { status: "delivered" } },
    
    // Stage 2: group
    { $group: {
        _id: "$user_id",
        total: { $sum: "$amount" },
        count: { $sum: 1 }
    }},
    
    // Stage 3: sort
    { $sort: { total: -1 } },
    
    // Stage 4: limit
    { $limit: 10 },
    
    // Stage 5: lookup (join with users)
    { $lookup: {
        from: "users",
        localField: "_id",
        foreignField: "_id",
        as: "user"
    }},
    
    // Stage 6: project (select fields)
    { $project: { 
        user_name: { $first: "$user.name" },
        total: 1, 
        count: 1 
    }}
]);
```

### Common Stages

| Stage | Like SQL |
|-------|----------|
| `$match` | `WHERE` |
| `$group` | `GROUP BY` |
| `$sort` | `ORDER BY` |
| `$limit` / `$skip` | `LIMIT` / `OFFSET` |
| `$lookup` | `LEFT JOIN` |
| `$project` | `SELECT` |
| `$unwind` | flatten array |
| `$count` | `COUNT(*)` |

### Indexes

```javascript
// Single field
db.users.createIndex({ email: 1 });   // 1 = ASC, -1 = DESC

// Compound
db.users.createIndex({ country: 1, age: -1 });

// Unique
db.users.createIndex({ email: 1 }, { unique: true });

// Partial
db.users.createIndex(
    { email: 1 },
    { partialFilterExpression: { is_active: true } }
);

// Text
db.articles.createIndex({ title: "text", body: "text" });
db.articles.find({ $text: { $search: "mongodb tutorial" } });

// Geospatial
db.places.createIndex({ location: "2dsphere" });

// TTL (auto-delete)
db.sessions.createIndex({ expires_at: 1 }, { expireAfterSeconds: 0 });
```

### Sharding (Horizontal Scaling)

**Matlab:** Data ko multiple servers (shards) par split — based on **shard key**.

```javascript
sh.enableSharding("mydb");
sh.shardCollection("mydb.orders", { user_id: "hashed" });
```

**Strategies:**
- **Hashed sharding** — even distribution
- **Ranged sharding** — range queries efficient

### Replica Sets (HA)

**Matlab:** Multiple copies of data — primary + secondaries.

```
Primary  ←  writes
   ↓
   ├──→ Secondary 1  (sync replication)
   └──→ Secondary 2

If primary fails → automatic election → secondary becomes primary
```

### Transactions (4.0+)

```javascript
const session = db.getMongo().startSession();
session.startTransaction();
try {
    db.accounts.updateOne({ id: "A" }, { $inc: { balance: -100 } }, { session });
    db.accounts.updateOne({ id: "B" }, { $inc: { balance: 100 } }, { session });
    session.commitTransaction();
} catch (err) {
    session.abortTransaction();
}
```

⚠️ Cross-shard transactions slow — try to model around them.

---

## Redis (Key-Value / Data Structure Store)

**Matlab:** In-memory data store. Insanely fast (sub-ms). Rich data structures.

### Why Redis?

- ⚡ **In-memory** — RAM speed
- 🧩 **Rich data structures** — not just strings
- 🔄 **Persistence options** — RDB / AOF
- 📡 **Pub-Sub built-in**

### Data Structures

#### 1. String

```redis
SET key "value"
GET key
INCR counter
EXPIRE key 3600           # TTL in seconds
SETEX key 3600 "value"    # set + expire
```

**Use case:** Cache, counters, simple values.

#### 2. Hash (Object)

```redis
HSET user:1 name "Rahul" email "r@x.com" age 25
HGET user:1 name
HGETALL user:1
HINCRBY user:1 age 1
```

**Use case:** Object storage (user profile).

#### 3. List (Linked List)

```redis
LPUSH queue "task1"        # push to head
RPUSH queue "task2"        # push to tail
LPOP queue                 # pop from head
BRPOP queue 5              # blocking pop (5s timeout)
LRANGE queue 0 -1          # get all
```

**Use case:** Queue, recent items list.

#### 4. Set (Unique values)

```redis
SADD tags "dev"
SADD tags "music"
SISMEMBER tags "dev"       # 1 (yes)
SMEMBERS tags
SINTER tags1 tags2         # intersection
SUNION tags1 tags2         # union
```

**Use case:** Unique tags, distinct counts, follower sets.

#### 5. Sorted Set (ZSet) — by score

```redis
ZADD leaderboard 100 "Rahul"
ZADD leaderboard 95 "Priya"
ZADD leaderboard 200 "Amit"

ZRANGE leaderboard 0 -1 WITHSCORES        # asc
ZREVRANGE leaderboard 0 9 WITHSCORES      # top 10
ZRANK leaderboard "Rahul"                 # position
ZINCRBY leaderboard 10 "Rahul"            # increase score
```

**Use case:** Leaderboards, time-series, priority queue.

#### 6. Stream (Append-only log)

```redis
XADD events * action login user_id 42
XREAD COUNT 10 STREAMS events 0
XGROUP CREATE events workers $
XREADGROUP GROUP workers worker1 COUNT 10 STREAMS events >
```

**Use case:** Event sourcing, message queue (Kafka-lite).

#### 7. Other

- **HyperLogLog** — approximate distinct count (uses 12KB!)
- **Bitmap** — bit-level operations
- **Geo** — geospatial sorted sets

### Persistence

#### RDB (Snapshot)

- Periodic dump of memory to disk (binary)
- Fast restart, smaller files
- ❌ Data loss between snapshots

```
save 900 1     # snapshot if 1 change in 15 min
save 300 10    # snapshot if 10 changes in 5 min
```

#### AOF (Append-Only File)

- Every write logged to file
- Rebuild by replaying log
- ✅ Less data loss
- ❌ Bigger files, slower

```
appendonly yes
appendfsync everysec   # fsync every second (good balance)
```

#### Hybrid (recommended)

Both RDB + AOF — best of both.

### Pub-Sub

```redis
# Subscriber
SUBSCRIBE notifications

# Publisher
PUBLISH notifications "Hello"
```

⚠️ No persistence — offline subscribers miss messages. Use **Streams** for durable events.

### Lua Scripting (Atomic)

```lua
-- Atomic check-and-set
EVAL "
  if redis.call('GET', KEYS[1]) == ARGV[1] then
    return redis.call('SET', KEYS[1], ARGV[2])
  else
    return 0
  end
" 1 mykey "expected" "newvalue"
```

✅ Multi-command atomicity — entire script runs without interruption.

### Redis Cluster (Sharding)

**Matlab:** Built-in horizontal scaling. Keys hashed across 16384 slots, slots distributed across nodes.

```
Cluster: 6 nodes (3 master, 3 replica)
Master 1: slots 0-5460
Master 2: slots 5461-10922
Master 3: slots 10923-16383

Each master has a replica for HA
```

### Redis Sentinel (HA without Cluster)

**Matlab:** Single master + replicas + sentinels jo monitor karte hain.

```
Master ←── Replica 1
       ←── Replica 2
       
Sentinel 1, 2, 3 (odd quorum) watch master
If master down → elect new master from replicas
```

### Common Patterns

#### Cache-Aside

```javascript
async function getUser(id) {
    const cached = await redis.get(`user:${id}`);
    if (cached) return JSON.parse(cached);
    
    const user = await db.query('SELECT ...', [id]);
    await redis.setex(`user:${id}`, 3600, JSON.stringify(user));
    return user;
}
```

#### Distributed Lock (Redlock)

```redis
SET lock:resource1 token123 NX PX 30000
# NX: only if not exists
# PX: 30s expiry
```

#### Rate Limiting

```redis
INCR rate:user:42:minute
EXPIRE rate:user:42:minute 60
# If > limit → reject
```

---

## Elasticsearch (Search Engine)

**Matlab:** Distributed search & analytics engine. Inverted index ke through full-text search lightning fast.

### Why Elasticsearch?

- 🔍 Full-text search with relevance ranking
- 📊 Aggregations (analytics)
- 📈 Horizontal scaling out-of-box
- 🌐 RESTful JSON API

### Inverted Index (Core Concept)

```
Doc 1: "The quick brown fox"
Doc 2: "The brown dog jumps"

Inverted Index:
  the   → [1, 2]
  quick → [1]
  brown → [1, 2]
  fox   → [1]
  dog   → [2]
  jumps → [2]

Query "brown" → instant lookup → [1, 2]
```

### Documents and Indexes

```json
PUT /products/_doc/1
{
  "name": "iPhone 15",
  "price": 80000,
  "tags": ["phone", "apple"],
  "description": "Latest Apple phone"
}
```

- **Index** = like SQL "table"
- **Document** = like SQL "row"
- **Field** = like SQL "column"
- **Mapping** = like SQL schema

### Mapping (Schema)

```json
PUT /products
{
  "mappings": {
    "properties": {
      "name":        { "type": "text", "analyzer": "english" },
      "name_kw":     { "type": "keyword" },     // exact match
      "price":       { "type": "integer" },
      "tags":        { "type": "keyword" },
      "created_at":  { "type": "date" },
      "location":    { "type": "geo_point" }
    }
  }
}
```

### Field Types

| Type | Use |
|------|-----|
| `text` | Full-text search (analyzed) |
| `keyword` | Exact match, aggregations |
| `integer`, `long`, `double` | Numbers |
| `date` | Date/time |
| `boolean` | True/false |
| `geo_point` | Lat/lon |
| `nested` | Array of objects (independent docs) |

### Analyzers

**Matlab:** Text ko tokens mein todna + lowercase + stem etc.

```
Input: "The Quick Brown Foxes"
   ↓ Standard analyzer
Tokens: [the, quick, brown, fox]   (foxes → fox stemming)
```

Built-in: `standard`, `english`, `whitespace`, `keyword` (no analysis).

### Query DSL

#### Match (Full-text)

```json
GET /products/_search
{
  "query": {
    "match": { "name": "apple phone" }
  }
}
```

#### Term (Exact)

```json
GET /products/_search
{
  "query": {
    "term": { "tags": "apple" }
  }
}
```

#### Bool (Combine)

```json
GET /products/_search
{
  "query": {
    "bool": {
      "must":     [{ "match": { "name": "phone" } }],
      "filter":   [{ "range": { "price": { "lte": 100000 } } }],
      "must_not": [{ "term": { "tags": "refurbished" } }],
      "should":   [{ "match": { "tags": "apple" } }]
    }
  }
}
```

| Clause | Effect |
|--------|--------|
| `must` | AND, contributes to score |
| `filter` | AND, no scoring (cached, fast) |
| `must_not` | NOT |
| `should` | OR (boosts score) |

### Aggregations (Analytics)

```json
GET /orders/_search
{
  "size": 0,
  "aggs": {
    "by_status": {
      "terms": { "field": "status" },
      "aggs": {
        "avg_amount": { "avg": { "field": "amount" } }
      }
    },
    "total_revenue": { "sum": { "field": "amount" } }
  }
}
```

### Sharding & Replication

```
Index "products" with 3 shards, 1 replica:

Node A:  Shard 0 (primary)   |  Shard 2 (replica)
Node B:  Shard 1 (primary)   |  Shard 0 (replica)
Node C:  Shard 2 (primary)   |  Shard 1 (replica)

Search query → all 3 primary shards in parallel → merge results
Write → primary shard, then replicate
```

### When NOT to Use Elasticsearch

❌ Primary database (no ACID, eventual consistency)
❌ Frequent updates of same doc (immutable docs internally)
❌ Strong relational data with joins

✅ Search, log analytics, observability (ELK stack)

---

## Comparison & When to Use What

### Quick Decision Matrix

| Need | Use |
|------|-----|
| Strong relational + transactions | PostgreSQL / MySQL |
| Flexible schema, document storage | MongoDB |
| Caching, sessions, leaderboards | Redis |
| Full-text search, analytics | Elasticsearch |
| Massive write scale, time-series | Cassandra |
| Graph relationships | Neo4j |

### CAP Theorem Quick Look

```
CAP: pick 2 of 3
  C - Consistency
  A - Availability
  P - Partition tolerance (network split)
```

| DB | Trade-off |
|----|-----------|
| Postgres | CP (consistency + partition) |
| MongoDB | CP (default), tunable |
| Redis (single) | CP |
| Cassandra | AP (availability + partition) |
| Elasticsearch | AP |

### Real-World Stack Example

```
PostgreSQL → source of truth (orders, users)
Redis      → cache + session + queue
Elasticsearch → product search + logs
MongoDB    → activity feed (flexible schema)
```

---

## Summary Cheat Sheet

| DB | Model | Best For | Persistence |
|----|-------|----------|-------------|
| **MongoDB** | Document | Flexible schema, hierarchical data | Disk |
| **Redis** | Key-Value++ | Speed (cache, queue, leaderboard) | RDB/AOF |
| **Elasticsearch** | Search | Full-text, analytics | Disk + replicas |

### MongoDB Cheatsheet
- Aggregation pipeline > regex chains
- Compound index leftmost prefix (like SQL)
- Replica set = HA, Sharding = scale
- Transactions slow on multi-shard

### Redis Cheatsheet
- Strings → cache, counters
- Hashes → objects
- Sorted sets → leaderboards
- Streams → durable events
- Use AOF for less data loss
- Cluster for sharding, Sentinel for HA without sharding

### Elasticsearch Cheatsheet
- `text` for search, `keyword` for exact/agg
- Use `filter` (cached) over `must` (scored) when no relevance needed
- Aggregations powerful — slice and dice metrics
- NOT a primary DB

---

## Practice

1. MongoDB: model a blog (posts, comments) — choose embed vs reference.
2. Redis: build a simple rate limiter using sorted sets (sliding window).
3. Redis: implement a leaderboard with top 10 + user's rank.
4. Elasticsearch: index 1000 products, build a search UI with filters.
5. MongoDB: write aggregation for "top 5 customers by revenue last month".
6. Redis: build a job queue using Streams + consumer groups.

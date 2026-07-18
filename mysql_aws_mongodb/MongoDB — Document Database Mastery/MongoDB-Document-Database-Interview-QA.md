# MongoDB — Document Database Mastery - Top 10 Interview Questions & Answers

> Covering: MongoDB Architecture, BSON, CRUD Operators, Aggregation Pipeline, Indexing, Schema Design, Transactions, Replica Sets, Sharding, Performance Monitoring, Change Streams, and Security

---

## Q1: MongoDB kya hai aur RDBMS se kaise different hai?

**Answer:**

MongoDB ek **document database** hai jo JSON-like documents store karta hai. Internally BSON (Binary JSON) use hota hai.

### RDBMS vs MongoDB Mapping:

| RDBMS | MongoDB |
|-------|---------|
| Database | Database |
| Table | Collection |
| Row | Document |
| Column | Field |
| Primary Key | `_id` |
| Join | `$lookup` / embedding / references |

### Example Document:

```javascript
{
  _id: ObjectId("65abc..."),
  name: "Sachin",
  email: "sachin@example.com",
  addresses: [
    { type: "home", city: "Pune" },
    { type: "office", city: "Mumbai" }
  ]
}
```

### BSON vs JSON:

| Feature | JSON | BSON |
|---------|------|------|
| Format | Text | Binary |
| Extra types | Limited | ObjectId, Date, Decimal128, BinData |
| Traversal | Slower | Faster for DB engine |

**Important:**

MongoDB schema-less nahi, **schema-flexible** hai. Production systems mein schema discipline still required hota hai, warna inconsistent documents query/indexing ko painful bana dete hain.

**Short Interview Answer:**

MongoDB is a document database that stores BSON documents in collections. It is schema-flexible and best when data is naturally document-shaped, access patterns favor embedding, and horizontal scaling is important.

---

## Q2: ObjectId kya hota hai? `_id` ka role kya hai?

**Answer:**

Har MongoDB document mein `_id` field mandatory hoti hai. Agar app provide nahi karta, MongoDB ObjectId generate karta hai.

### ObjectId Structure (12 bytes):

```text
4 bytes timestamp
5 bytes random/process unique
3 bytes counter
```

### Benefits:

- Globally unique enough
- Creation time embedded hota hai
- Client-side generate ho sakta hai

```javascript
db.users.insertOne({
  name: "Amit",
  email: "amit@example.com"
});

// MongoDB auto-generates _id
```

**Important:**

ObjectId roughly increasing hota hai timestamp ke basis par, but business ordering ke liye always explicit `createdAt` field use karo.

---

## Q3: MongoDB CRUD operations aur query operators kaise use karte hain?

**Answer:**

### Create:

```javascript
db.users.insertOne({ name: "Amit", age: 25 });

db.users.insertMany([
  { name: "Riya", age: 22 },
  { name: "Kabir", age: 30 }
]);
```

### Read:

```javascript
db.users.find({ age: { $gte: 18 } });
db.users.findOne({ email: "amit@example.com" });
```

### Update:

```javascript
db.users.updateOne(
  { email: "amit@example.com" },
  { $set: { city: "Pune" }, $inc: { loginCount: 1 } }
);
```

### Delete:

```javascript
db.users.deleteOne({ email: "amit@example.com" });
```

### Common Operators:

| Operator | Use |
|----------|-----|
| `$eq`, `$ne` | equality |
| `$gt`, `$gte`, `$lt`, `$lte` | comparison |
| `$in`, `$nin` | list matching |
| `$and`, `$or`, `$not` | logical |
| `$exists`, `$type` | field checks |
| `$regex` | pattern matching |
| `$elemMatch` | array element matching |

**Short Interview Answer:**

MongoDB CRUD uses `insertOne/Many`, `find/findOne`, `updateOne/Many`, `deleteOne/Many`. Updates use atomic operators like `$set`, `$inc`, `$push`, `$pull`, `$addToSet`. Query operators provide comparison, logical, array, and regex matching.

---

## Q4: Aggregation pipeline kya hai? `$match`, `$group`, `$lookup` kaise kaam karte hain?

**Answer:**

Aggregation pipeline data processing stages ka sequence hai. Har stage input documents transform/filter/group karta hai.

```javascript
db.orders.aggregate([
  { $match: { status: "PAID" } },
  { $group: { _id: "$userId", total: { $sum: "$amount" } } },
  { $sort: { total: -1 } },
  { $limit: 10 }
]);
```

### Common Stages:

| Stage | Purpose |
|-------|---------|
| `$match` | Filter documents |
| `$project` | Shape fields |
| `$group` | Aggregate by key |
| `$sort` | Sort results |
| `$limit` / `$skip` | Pagination |
| `$lookup` | Left outer join |
| `$unwind` | Array flatten |
| `$facet` | Multiple pipelines in one |

### `$lookup` Example:

```javascript
db.orders.aggregate([
  {
    $lookup: {
      from: "users",
      localField: "userId",
      foreignField: "_id",
      as: "user"
    }
  }
]);
```

**Important:**

`$match` as early as possible rakho so downstream stages fewer documents process karein. `$sort` + `$group` heavy ho sakte hain; indexes and `allowDiskUse` consider karo.

---

## Q5: MongoDB indexing strategy kaise design karte ho?

**Answer:**

Index query performance ka foundation hai. MongoDB mein wrong/no index = `COLLSCAN`.

### Index Types:

| Index | Use |
|-------|-----|
| Single-field | One field queries |
| Compound | Multiple fields |
| Multikey | Array fields |
| Text | Full-text search |
| 2dsphere | Geo queries |
| Hashed | Sharding/equality |
| TTL | Auto-expire documents |
| Partial | Subset indexing |

### Compound Index ESR Rule:

**E → S → R**

```text
Equality fields first
Sort fields next
Range fields last
```

Example:

```javascript
db.orders.createIndex({ userId: 1, status: 1, createdAt: -1 });
```

Good for:

```javascript
db.orders.find({ userId: 1, status: "PAID" })
         .sort({ createdAt: -1 });
```

### Explain:

```javascript
db.orders.find({ userId: 1 }).explain("executionStats");
```

Look for:
- `IXSCAN` good
- `COLLSCAN` bad for large collections
- `keysExamined` vs `docsExamined`

**Short Interview Answer:**

I design MongoDB indexes from query patterns. For compound indexes I follow ESR: Equality, Sort, Range. I verify using `explain("executionStats")` and avoid indexes that are unused or too expensive for writes.

---

## Q6: MongoDB schema design mein embed vs reference kaise decide karte ho?

**Answer:**

MongoDB mein schema design query pattern driven hota hai, not normalization-first.

### Embed when:
- Data together read hota hai
- One-to-few relationship
- Child lifecycle parent ke saath tied hai

```javascript
{
  _id: 1,
  name: "Amit",
  addresses: [
    { type: "home", city: "Pune" }
  ]
}
```

### Reference when:
- One-to-many unbounded
- Child independently update/query hota hai
- Document size grow risk

```javascript
// orders collection
{ _id: 101, userId: 1, amount: 500 }
```

### Common Patterns:

| Pattern | Use |
|---------|-----|
| Extended Reference | Frequently needed fields duplicate |
| Subset | Most recent N items embed |
| Outlier | Rare huge documents separate |
| Bucket | Time-series/events grouped |

**Important:**

Unbounded arrays anti-pattern hain. MongoDB document size limit 16MB hai. “Embed everything” scale par painful ho sakta hai.

---

## Q7: MongoDB transactions aur consistency kaise kaam karte hain?

**Answer:**

MongoDB single-document writes atomic hote hain. Multi-document ACID transactions 4.0+ mein available hain.

### Transaction Example:

```javascript
const session = client.startSession();

try {
  session.startTransaction();

  await accounts.updateOne(
    { _id: from },
    { $inc: { balance: -100 } },
    { session }
  );

  await accounts.updateOne(
    { _id: to },
    { $inc: { balance: 100 } },
    { session }
  );

  await session.commitTransaction();
} catch (e) {
  await session.abortTransaction();
} finally {
  await session.endSession();
}
```

### Read Concern:

| Concern | Meaning |
|---------|---------|
| `local` | Fast, may rollback |
| `majority` | Majority committed data |
| `linearizable` | Strongest for primary reads |

### Write Concern:

```javascript
{ writeConcern: { w: "majority", j: true } }
```

**Important:**

Transactions MongoDB mein available hain, but expensive hain. Schema design aisa rakho ki most writes single-document atomicity mein fit ho jayein.

---

## Q8: Replica set kya hota hai? Elections, oplog, read preference kaise work karte hain?

**Answer:**

Replica set MongoDB high availability ka core hai.

```text
Replica Set:
Primary    → accepts writes
Secondary  → replicates oplog
Secondary  → can become primary
```

### Oplog:

Primary har write operation oplog mein record karta hai. Secondaries oplog tail karke changes apply karte hain.

### Election:

Primary fail hone par secondaries election run karte hain. Majority vote se new primary elect hota hai.

### Read Preference:

| Preference | Use |
|------------|-----|
| primary | Strong consistency default |
| primaryPreferred | Primary if available |
| secondary | Read scale, stale possible |
| nearest | Lowest latency |

**Short Interview Answer:**

A replica set has one primary and multiple secondaries. Writes go to primary, secondaries replicate via oplog. If primary fails, election chooses a new primary. Read preference controls whether reads go primary or secondary, trading freshness for scale/latency.

---

## Q9: MongoDB sharding kaise kaam karta hai? Shard key kaise choose karte ho?

**Answer:**

Sharding large data ko multiple shards across distribute karta hai.

### Components:

| Component | Role |
|-----------|------|
| `mongos` | Query router |
| Shards | Data store |
| Config servers | Cluster metadata |
| Balancer | Chunk movement |

```text
Client → mongos → correct shard(s)
              ↓
        config metadata
```

### Shard Key Criteria:

| Factor | Why important |
|--------|---------------|
| High cardinality | Enough distinct values |
| Even frequency | Avoid hot shards |
| Non-monotonic | Avoid all writes to last chunk |
| Query-aligned | Targeted queries possible |

### Range vs Hashed:

| Type | Pros | Cons |
|------|------|------|
| Ranged | Range queries efficient | Hotspot risk |
| Hashed | Even distribution | Range queries scatter |

**Important:**

Bad shard key almost irreversible pain hota hai. Monotonic keys like timestamp can create hot shard unless combined/hashed.

---

## Q10: MongoDB performance monitoring, change streams aur security best practices kya hain?

**Answer:**

### Performance:

```javascript
db.collection.find(query).explain("executionStats");
db.currentOp();
db.killOp(opid);
```

Tools:
- `mongostat`
- `mongotop`
- Atlas Performance Advisor
- `collStats`

### Change Streams:

Change Streams real-time changes observe karne ke liye:

```javascript
db.orders.watch([
  { $match: { "operationType": "insert" } }
]);
```

Use cases:
- Cache invalidation
- Audit log
- Real-time notifications
- Search index sync

Requires replica set/sharded cluster.

### Security:

- Enable authentication (SCRAM-SHA-256)
- RBAC least privilege
- TLS in transit
- Encryption at rest
- IP allowlist / private network
- Never expose `mongod` publicly

```javascript
db.createUser({
  user: "app_user",
  pwd: "strong-password",
  roles: [{ role: "readWrite", db: "appdb" }]
});
```

**Short Interview Answer:**

For MongoDB production, I monitor with `explain`, `currentOp`, mongostat/mongotop, use change streams for real-time event workflows, and secure with auth, RBAC, TLS, encryption, private networking, and least privilege.


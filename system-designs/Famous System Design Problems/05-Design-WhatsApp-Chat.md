# Design: WhatsApp / Chat System

## Status: Complete

---

## Quick Walkthrough (Hinglish)

> "WhatsApp = **real-time messaging** at billions-of-users scale. Core requirements: 1:1 + group chat, **delivery + read receipts** (✓✓ blue), **online presence** ('last seen'), **media** (photos/videos), aur **end-to-end encryption**."

**Mental model**:

- **Real-time transport** = **WebSocket** (persistent TCP, both-direction). Long polling fallback corporate proxies ke liye.
- **Storage** = **Cassandra** — write-heavy, time-series; partition key = `chat_id`. Itne messages SQL pe scale nahi karenge.
- **Receipts**: server message persist hote hi `sent`, recipient device ACK pe `delivered`, user open kare to `read`.
- **Group chat**: message **ek baar persist** hota hai (chat_id pe), fan-out **online members** ko notification ke liye — duplicate rows mat banao.
- **Presence**: Redis key `presence:{user_id}` with **TTL ~60s**, client har 30s heartbeat bheje. TTL expire = offline.
- **Media**: client **S3 pre-signed URL** se direct upload kare, message mein sirf URL/key. Server bandwidth bach jata hai.
- **E2EE** (Signal Protocol): server ke paas sirf **ciphertext** — decrypt karne ka power nahi. Trade-off: server-side search / moderation impossible.

**Connection routing problem**:

- 100M users connected via WebSocket — message kaunse gateway pod pe deliver karein?
- **Connection registry** (Redis): `user_id → gateway_instance` mapping. Message aaye → registry dekho → right pod ko forward.

> "**Interview soundbite**: 'WebSocket sticky session enough nahi — distributed pub/sub layer chahiye taaki kisi bhi gateway pe message land kare to right user ko reach ho jaye.'"

---

## Table of Contents

1. [Requirements](#requirements)
2. [Real-Time Transport](#real-time-transport)
3. [Message Storage](#message-storage)
4. [Message Lifecycle & Receipts](#message-lifecycle--receipts)
5. [Group Chat](#group-chat)
6. [Online Presence](#online-presence)
7. [Media Messages](#media-messages)
8. [Encryption Note](#encryption-note)
9. [High-Level Architecture](#high-level-architecture)
10. [Out-of-Order & Delivery](#out-of-order--delivery)
11. [Pitfalls](#pitfalls)
12. [Cheat Sheet](#cheat-sheet)

---

## Requirements

- 1:1 and **group** chat
- **Real-time** delivery + **persistent** history
- **Sent → Delivered → Read** receipts
- **Typing indicators**, **presence** (online / last seen)
- **Media** (images, voice, video)
- Scale: billions of messages/day

---

## Real-Time Transport

### WebSocket (preferred)

```
Client ◄════ persistent TCP ════► Chat Gateway
         bidirectional frames
```

✅ Low latency, server push, multiplexing  
❌ Stateful connections → **gateway fleet** + sticky routing or shared session store

### Long polling

```
Client GET /messages?cursor=…
Server holds until message or timeout → repeat
```

✅ Works through strict proxies  
❌ Higher latency + server load vs WebSocket

### Interview soundbite

> "**Primary WebSocket** + **fallback long polling** where corporate firewalls block WS."

### Connection layer

- **MQTT / custom binary protocol** on mobile for battery + bandwidth (WhatsApp historically uses optimized stacks)
- TLS everywhere

---

## Message Storage

> "**Write-heavy + time-ordered** per conversation."

### Partition key

```
PRIMARY KEY ((chat_id), message_ts, message_id)
```

- **Cassandra / Scylla** ideal — append-only, horizontal scale
- Alternative: **DynamoDB** `(chat_id PK, SK=timestamp+msg_id)`

### Why not SQL single table?

Sheer write volume + partition isolation per chat.

### Secondary needs

- **User inbox** projection if listing all chats by user (separate table keyed by `user_id`)
- **Elasticsearch** optional for full-text search inside chats

---

## Message Lifecycle & Receipts

States:

```
SENDING → SERVER_ACK (sent) → DELIVERED → READ
```

Implementation ideas:

- **Sent**: server persisted + seq assigned
- **Delivered**: recipient device ACK per message id (batch)
- **Read**: client sends read cursor `last_read_msg_id` per chat

Store:

```
message table: delivery_status per recipient (groups), or separate receipts table
```

Sync via:

- WebSocket push of receipt events
- Offline clients catch up via REST `GET /messages?since=cursor`

---

## Group Chat

```
chat_id → member list (Redis SET / DB table)
New message → fan-out write:
   ├── Persist once in messages(chat_id)
   └── Notify each online member via connection manager
```

**Fan-out notifications** not duplicate message rows (single source of truth).

Large groups: **read receipts** batched / summarized ("8 read").

---

## Online Presence

```
Redis: presence:{user_id} = "online"
TTL: 60s
Heartbeat from client every ~30s refreshes TTL
```

- **Pub/Sub** channel `presence:{friend_id}` for subscribers who care
- **Last seen** stored in DB when going offline

Privacy settings: hide last seen / online status.

---

## Media Messages

```
Client uploads file → pre-signed URL → **S3 / object storage**
Server stores metadata: chat_id, message_id, **s3_key**, mime, size
Thumbnail pipeline async (Lambda / worker)
Message payload references URL clients fetch via CDN
```

End-to-end encrypted apps: **encrypt blob client-side** before upload (server sees ciphertext only).

---

## Encryption Note

**Signal Protocol** (Double Ratchet) — WhatsApp-style **E2EE**:

- Server routes **opaque ciphertext**
- Key negotiation client-side
- Interview: "**Server can't read messages** — trade-off: no server-side search / moderation without client assistance"

---

## High-Level Architecture

```
Clients → DNS → Geo-routed Chat Gateway (WebSocket)
              │
              ├── Presence Service (Redis)
              ├── Message Service → Cassandra (by chat_id)
              ├── Media Service → S3 presign + CDN
              └── Push Notify offline users (FCM/APNS)

Cross-region: multi-region Cassandra / Dynamo global tables + regional gateways
```

**Connection service** tracks `user_id → gateway_instance` mapping in Redis for targeted push.

---

## Out-of-Order & Delivery

- Assign **monotonic sequence per chat** from server (single leader per chat partition or CRDT careful ordering)
- Client sorts by `(seq, sender_ts)`
- **Exactly-once illusion**: idempotent `client_msg_id` dedup at server

---

## Pitfalls

1. **WebSocket stickiness wrong** → messages routed to wrong pod — need shared subscription layer
2. **Partition hot chat** (viral group) — shard **within** chat difficult — mitigations: rate limit, upgrade infra
3. **Receipt storms** — batch ACKs
4. **Media scanning vs E2EE** — product tension
5. **Push notification leaks preview** — careful payload encryption flags

---

## Cheat Sheet

| Piece | Choice |
|-------|--------|
| Real-time | **WebSocket** + fallback |
| Storage | **Cassandra** / Dynamo by `chat_id` |
| Presence | **Redis SET + TTL** |
| Media | **S3 + CDN** |
| E2EE | **Signal Protocol** (conceptual) |

---

## Practice

1. Schema for **message edits** + **delete for everyone** under E2EE constraints.
2. How to implement **typing indicator** without flooding server?
3. Design migration from monolith chat DB to Cassandra without downtime.

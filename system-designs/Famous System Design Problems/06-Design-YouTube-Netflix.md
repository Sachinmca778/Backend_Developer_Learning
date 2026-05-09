# Design: YouTube / Netflix (Video Streaming)

## Status: Complete

---

## Quick Walkthrough (Hinglish)

> "Video platform = **upload pipeline** + **transcoding** + **global delivery**. Three completely different problems jo ek system mein milte hain."

**Mental model — 4 stages**:

1. **Upload**: Bade files (GBs) — chunked / resumable upload to **S3 multipart**. Network blip ho to whole file dobara nahi bhejna.
2. **Transcode**: Source video → multiple resolutions (360p / 720p / 1080p / 4K) + multiple codecs (H.264, VP9, AV1). **Async** worker fleet (FFmpeg / Elastic Transcoder) job queue se pull karke process kare. Ek video → kai jobs parallel.
3. **Package**: **HLS / DASH** — video ko chhote **segments** (~6 sec) mein todo + manifest file (`.m3u8` / `.mpd`). Adaptive bitrate ka foundation.
4. **Deliver**: **CDN** sab kuch handle kare. Origin pe load nahi aana chahiye for popular videos. **Cache hit ratio = 95%+** target.

**ABR (Adaptive Bitrate Streaming)**:

- Player network speed monitor karta hai → bandwidth gir gaya = automatic 1080p → 720p → 360p switch
- Wi-Fi mein wapas 4K — manifest mein saari variants listed hoti hain

**Hot complications**:

| Component | Choice |
|-----------|--------|
| **Metadata** (title, description) | PostgreSQL |
| **Search** (full-text, autocomplete) | Elasticsearch |
| **View count** | **Approximate** — Redis INCR + batch flush. Exact count = hot row disaster. |
| **Recommendations** | Offline ML pipeline (Spark/Flink) → user/video vectors → ANN serving |
| **DRM** (Netflix paid content) | Widevine / FairPlay / PlayReady + license server |

> "**Interview soundbite**: 'Public view count exact nahi hota — YouTube bhi rounded dikhata hai (1.2M views). Counter ko Redis pe sharded rakho, batch job DB mein flush kare.'"

---

## Table of Contents

1. [Requirements](#requirements)
2. [Upload Pipeline](#upload-pipeline)
3. [Transcoding](#transcoding)
4. [Storage Layout](#storage-layout)
5. [Playback & CDN](#playback--cdn)
6. [Adaptive Bitrate (ABR)](#adaptive-bitrate-abr)
7. [Metadata & Search](#metadata--search)
8. [Recommendations](#recommendations)
9. [View Count](#view-count)
10. [DRM & Geo](#drm--geo)
11. [High-Level Diagram](#high-level-diagram)
12. [Pitfalls](#pitfalls)
13. [Cheat Sheet](#cheat-sheet)

---

## Requirements

- Upload large files reliably (**resume** support)
- **Transcode** multiple resolutions / codecs
- **Low-latency start**, smooth playback globally
- **Search** + metadata browsing
- **Recommendations** personalized
- **View counts** at huge scale (often approximate publicly)

---

## Upload Pipeline

### Chunked / resumable upload

```
Client ─► Upload API (+ auth)
    ├── Initializes upload session → upload_id
    └── PUT chunks with byte ranges → **Object storage (S3 multipart)**

On complete → emit **video.uploaded** event → transcoding pipeline
```

Benefits:

- Retry individual parts
- Parallel chunk uploads saturate bandwidth

---

## Transcoding

```
Kafka / SQS: video.uploaded
    │
    ▼
Transcode Worker fleet (ECS/Batch/Elastic Transcoder / FFmpeg workers)
    ├── Generate renditions: 360p, 720p, 1080p, 4K (if source permits)
    ├── Codecs: H.264, VP9, AV1 (business dependent)
    ├── Audio tracks / subtitles ingestion
    └── Package outputs → **HLS (.m3u8 + .ts) / DASH (.mpd)**

Publish **video.ready** + CDN purge warmup for manifest URLs
```

**Fan-out jobs**: one video → many renditions processed in parallel tasks.

---

## Storage Layout

```
s3://videos/{video_id}/source/raw.mp4
s3://videos/{video_id}/h264/720p/segment_00001.ts
s3://videos/{video_id}/h264/720p/index.m3u8
...
```

- Source kept for re-transcode
- Cold tier possible for masters (**Glacier**) after processing

---

## Playback & CDN

```
Client requests manifest: https://cdn.example.com/{video_id}/master.m3u8
CDN edge cache (high hit ratio for popular videos)
On miss → origin S3 / dedicated origin shield
```

- **Signed URLs** or **token auth** for private/paid content
- **Geo-blocking** at CDN edge

---

## Adaptive Bitrate (ABR)

**HLS / DASH**:

- Manifest lists variant streams (bitrate, resolution)
- Player monitors buffer → switches quality (**ABR algorithm** — throughput-based)

Benefits:

- Mobile congested network → drop to 360p
- Wi-Fi fiber → 4K

---

## Metadata & Search

**PostgreSQL / MySQL**:

- `videos` table: title, description, owner, duration, status (processing/ready), thumbnail keys

**Elasticsearch / OpenSearch**:

- Full-text search, facets (category, duration bucket), autocomplete

**Thumbnail**:

- Generated frame @ t=10s or uploaded custom → CDN URL stored

---

## Recommendations

Offline / near-line pipelines:

```
Watch history stream → Kafka → Spark/Flink feature extraction
        │
        ▼
Training pipeline (batch) → model artifacts
        │
        ▼
Serving: approximate nearest neighbors (vector DB: Milvus / Pinecone / ES dense_vector)
        OR two-tower model online scoring candidate pool
```

Interview level: **candidate generation** (popular, similar, subscribed) + **light ranker** + **heavy model** async refresh.

---

## View Count

Naive: `UPDATE videos SET views=views+1` → **hot row** disaster.

Better:

```
Each play → Redis INCR video:{id}:views (sharded key optional)
Periodic aggregator (Flink / batch job) → flush deltas to Postgres / OLAP
Display **rounded** public count (YouTube-style lag acceptable)
```

Fraud: dedupe by session / signed beacon; bot filtering.

---

## DRM & Geo

- **Widevine / FairPlay / PlayReady** for premium encumbered content
- License server separate from CDN
- **Geo compliance** — CDN rules + manifest filtering

---

## High-Level Diagram

```
Upload ─► API ─► S3 multipart ─► Event ─► Transcode workers ─► Segments to S3
                                                      │
Playback ◄── CDN ◄── manifests + segments ◄───────────┘

Metadata DB ◄── API      Search ◄── Indexer ◄── CDC/events
Reco service ◄── Features store ◄── ML offline pipeline
```

---

## Pitfalls

1. **Synchronous transcoding in API** — never; always async queue
2. **Single-region S3** — latency + DR; multi-region replication for library
3. **Ignoring CDN cache key** — personalize breaks hit ratio (signed URLs per user carefully)
4. **Exact view count** obsession — costly; clarify product tolerance
5. **Copyright / content ID** — legal pipeline parallel (out of scope but mention)

---

## Cheat Sheet

| Piece | Choice |
|-------|--------|
| Upload | **Multipart S3** |
| Processing | **Queue + worker fleet** |
| Playback | **HLS/DASH + CDN** |
| Metadata | **Postgres + Elasticsearch** |
| Views | **Redis aggregate + batch** |
| Reco | **Two-tower + ANN** (high level) |

---

## Practice

1. Resume upload after app kill — state machine on client + server.
2. Cost optimization: when to delete source raw after transcoding?
3. Design **live streaming** variant (RTMP ingest → HLS low-latency chunk size trade-offs).

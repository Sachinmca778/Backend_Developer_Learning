# S3 — Object Storage

## Status: Complete

---

## Table of Contents

1. [What is S3](#what-is-s3)
2. [Buckets & Objects](#buckets--objects)
3. [Multipart Upload](#multipart-upload)
4. [Storage Classes](#storage-classes)
5. [Lifecycle Policies](#lifecycle-policies)
6. [Versioning & MFA Delete](#versioning--mfa-delete)
7. [Static Website Hosting](#static-website-hosting)
8. [Pre-Signed URLs](#pre-signed-urls)
9. [Transfer Acceleration](#transfer-acceleration)
10. [Object Lock (WORM)](#object-lock-worm)
11. [Security Essentials](#security-essentials)
12. [Pitfalls](#pitfalls)
13. [Cheat Sheet](#cheat-sheet)

---

## What is S3

> "**Simple Storage Service** — durable (11 9's), virtually unlimited **object storage**. Backbone for backups, data lakes, static sites, log dumps, ML datasets, and AWS internals (RDS snapshots, CloudTrail logs etc.)."

Key properties:

- **Durability**: 99.999999999% (11 nines) within region
- **Availability**: 99.99% (Standard)
- **Object size**: 0 → **5 TB max** per object
- **Strong read-after-write consistency** (since Dec 2020) — no more "list inconsistency"

---

## Buckets & Objects

### Bucket

- **Globally unique name** (DNS-style: `my-app-prod-logs`)
- Lives in **one region**
- Soft limit **100 buckets per account** (request increase up to 1000)

### Object

- Key (path-like string), value (bytes), metadata, version ID
- **No real folders** — `/` in key is just a separator (UI shows folders for UX)

```
s3://my-app-prod-logs/2024/05/08/api-server-1.log.gz
                       └────── flat key, just convention ──────┘
```

### ARN format

```
arn:aws:s3:::my-bucket
arn:aws:s3:::my-bucket/path/to/object
```

---

## Multipart Upload

> "**>100 MB** objects use **multipart upload** — parallel parts + retry-friendly + resumable."

```
file (5 GB) → split into 50 parts of 100 MB
            → upload parts in parallel
            → CompleteMultipartUpload
```

### Why mandatory for big files

- Single PUT max **5 GB**
- Multipart supports up to **5 TB**
- **Failed part = retry that part only**, not whole file
- Faster — saturates bandwidth via parallelism

### Watch

- **Abort incomplete multipart uploads** lifecycle rule — orphan parts charge silently

```json
{
  "Rules": [{
    "Status": "Enabled",
    "AbortIncompleteMultipartUpload": { "DaysAfterInitiation": 7 }
  }]
}
```

---

## Storage Classes

| Class | Latency | Min duration | Cost | Use |
|-------|---------|--------------|------|-----|
| **Standard** | ms | none | baseline | Frequently accessed, hot |
| **Standard-IA** | ms | 30 d | ~50% cheaper, **per-GB retrieval** fee | Infrequent but instant |
| **One Zone-IA** | ms | 30 d | 20% cheaper than IA | Reproducible / non-critical |
| **Intelligent-Tiering** | ms | none | small monitoring fee, **auto-moves** | Unknown / variable patterns |
| **Glacier Instant Retrieval** | ms | 90 d | very cheap | Archive but instant access |
| **Glacier Flexible Retrieval** | minutes-hours | 90 d | cheaper still | Backups rarely accessed |
| **Glacier Deep Archive** | 12 h | 180 d | cheapest | Compliance retention 7+ yrs |

### Decision guide

- **Don't know access pattern** → **Intelligent-Tiering** (let AWS decide)
- **Compliance archive** (financial/legal) → **Glacier Deep Archive**
- **Backup with quick restore needed sometimes** → **Glacier Instant**
- **Hot website assets** → **Standard** (or Standard-IA + CloudFront)

---

## Lifecycle Policies

> "**Auto-transition or expire objects** based on age / prefix / tags."

```json
{
  "Rules": [
    {
      "ID": "logs-archive",
      "Filter": { "Prefix": "logs/" },
      "Status": "Enabled",
      "Transitions": [
        { "Days": 30,  "StorageClass": "STANDARD_IA" },
        { "Days": 90,  "StorageClass": "GLACIER" },
        { "Days": 365, "StorageClass": "DEEP_ARCHIVE" }
      ],
      "Expiration": { "Days": 2555 }
    }
  ]
}
```

### Wins

- **Massive cost savings** without code changes
- Forget-it-and-it-works

### Watch

- **Min duration penalties** — moving to IA at day 1 then deleting at day 5 charges 30 days
- **Plan tiers** before enabling

---

## Versioning & MFA Delete

### Versioning

- Each PUT creates a **new version**, old version retained
- Delete = **delete marker** (object hidden, not gone)
- Recover by removing delete marker

```bash
aws s3api list-object-versions --bucket my-bucket --prefix file.txt
aws s3api get-object --bucket my-bucket --key file.txt --version-id <id> out.txt
```

### Combine with lifecycle

```json
{
  "NoncurrentVersionTransitions": [
    { "NoncurrentDays": 30, "StorageClass": "GLACIER" }
  ],
  "NoncurrentVersionExpiration": { "NoncurrentDays": 365 }
}
```

→ Otherwise **versions = forever bill**.

### MFA Delete

- Requires **root account + MFA** to permanently delete versions / disable versioning
- High-security buckets only (regulatory)

---

## Static Website Hosting

> "**S3 + CloudFront** = cheap, scalable static site (SPA, marketing site)."

Steps:

1. Bucket → **Static website hosting** ON
2. Index document `index.html`, error doc `404.html`
3. Bucket policy → public read **OR** restrict to **CloudFront Origin Access Identity / OAC**
4. Front with **CloudFront** + **ACM cert** for HTTPS + custom domain via **Route 53**

### Modern best practice

- Make bucket **private** + **Origin Access Control (OAC)** — only CloudFront can read
- Don't enable public S3 website endpoint (no HTTPS, no custom domain native)

---

## Pre-Signed URLs

> "**Time-limited signed URL** giving the holder permission to GET/PUT a specific object — without AWS creds."

```python
import boto3
s3 = boto3.client('s3')

url = s3.generate_presigned_url(
    'get_object',
    Params={'Bucket': 'my-bucket', 'Key': 'invoice.pdf'},
    ExpiresIn=300   # seconds
)
```

### Use cases

- **User uploads direct to S3** (skip your app server bandwidth)
- **Time-limited downloads** (paid content)
- **Webhooks** sharing a one-time file
- **Mobile clients** uploading photos

### Notes

- URL inherits the **signer's** permissions
- Max expiry: **7 days** (signature v4)
- Use **POST policy** form for size/type restrictions on uploads

---

## Transfer Acceleration

> "**Upload via nearest CloudFront edge** → AWS backbone → bucket region. Faster cross-continent uploads."

- Enable per-bucket
- Endpoint: `bucket.s3-accelerate.amazonaws.com`
- **Per-GB extra charge** — useful only if measurable benefit (test first)
- Mostly for large files from globally distributed users

---

## Object Lock (WORM)

> "**Write Once Read Many** — object can't be deleted/overwritten for retention period. Compliance: SEC 17a-4, financial/legal."

Two modes:

| Mode | Behavior |
|------|----------|
| **Governance** | Privileged users (`s3:BypassGovernanceRetention`) can override |
| **Compliance** | **Nobody** can delete (not even root) until retention ends |

Plus **Legal Hold** — indefinite lock independent of retention.

### Use carefully

- Compliance mode + 100-year retention = **immortal bill**
- Test in non-prod first

---

## Security Essentials

- **Block Public Access** — account-level + bucket-level. **Always ON** unless intentional public.
- **Bucket policy** + **ACL** — prefer policies, ACLs legacy
- **SSE-S3** (default), **SSE-KMS** (customer-managed key, audit), **SSE-C** (customer-supplied key)
- **Default encryption** ON for new buckets (account setting)
- **Access Logs** to another bucket (or CloudTrail data events for object-level audit)
- **VPC Gateway Endpoint** for in-VPC access — no NAT charges, no internet path

---

## Pitfalls

1. **Public bucket** by accident — every breach headline ever.
2. **No lifecycle on versions** — versioned bucket grows forever.
3. **Multipart parts orphaned** — invisible bill.
4. **IA storage for tiny short-lived objects** — penalty fees > savings.
5. **Pre-signed URL with super-broad signer permissions** — leak = whole-bucket access.
6. **No CloudFront** in front of static site — paying full S3 GET rate + slow globally.
7. **Cross-region copy** for "backup" — expensive egress; use **CRR** instead.
8. **Object Lock in Compliance mode** mistake — undeletable.
9. **No `BlockPublicAccess`** at account level — single misconfigured bucket → public.
10. **Listing huge buckets** without prefix → thousands of API calls + slow.

---

## Cheat Sheet

| Need | Feature |
|------|---------|
| Big upload | Multipart |
| Cheaper for old data | Lifecycle → IA → Glacier |
| Unknown pattern | Intelligent-Tiering |
| Compliance archive | Deep Archive + Object Lock |
| Recover deletes | Versioning + MFA Delete |
| Direct user upload | Pre-signed URL POST |
| Global fast upload | Transfer Acceleration |
| Static site | S3 + CloudFront + OAC |
| Private VPC access | Gateway Endpoint |
| Audit | CloudTrail data events |

---

## Practice

1. Create bucket → enable **versioning + lifecycle** for noncurrent versions.
2. Upload 1 GB file with **`aws s3 cp`** (auto multipart) — observe parts.
3. Generate **pre-signed PUT URL** + upload from `curl`.
4. Configure lifecycle to move `logs/` to IA at 30 d, Glacier at 90 d.
5. Set up **CloudFront + OAC** in front of a private S3 site.
6. Enable **block public access** at account level — verify can't make public bucket.

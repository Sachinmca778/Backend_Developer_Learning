# Backup & Recovery

## Status: Not Started

---

## Table of Contents

1. [Why Backup?](#why-backup)
2. [Backup Types](#backup-types)
3. [Logical vs Physical](#logical-vs-physical)
4. [mysqldump](#mysqldump)
5. [mysqlpump](#mysqlpump)
6. [mydumper / myloader](#mydumper--myloader)
7. [Percona XtraBackup](#percona-xtrabackup)
8. [Point-in-Time Recovery (PITR)](#point-in-time-recovery-pitr)
9. [Binary Log Backup](#binary-log-backup)
10. [Backup Strategies (3-2-1)](#backup-strategies-3-2-1)
11. [Encryption at Rest](#encryption-at-rest)
12. [Test Restores](#test-restores)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## Why Backup?

> "**Untested backup = no backup**. Disasters: hardware fail, human error (DROP table), malware, software bugs."

### Real reasons

- Hardware failure (disk crash)
- Software bug / data corruption
- Accidental `DELETE`/`UPDATE`/`DROP` (most common)
- Ransomware / security breach
- Compliance / legal requirement (audit trail)
- Migration / DR

### What recovery looks like

```
1. Restore most recent full backup
2. Apply incremental backups (if any)
3. Replay binlogs up to target time (PITR)
4. Verify data
```

→ Time matters. **RTO** target?

---

## Backup Types

| Type | What |
|------|------|
| **Full** | Complete copy of DB |
| **Incremental** | Only changes since last full / incremental |
| **Differential** | Changes since last **full** |
| **Continuous** | Stream (binlog backup) |

### Full vs Incremental — example

```
Day 1: Full backup (100 GB)
Day 2: Incremental (5 GB — only changes)
Day 3: Incremental (5 GB)
...
Day 7: Incremental (5 GB)
Day 8: Full backup (100 GB)
```

→ **Restore Day 5**: full Day 1 + incrementals Day 2,3,4,5.

→ Faster backups, slower restores.

### Hot vs Cold

| | Hot (online) | Cold (offline) |
|--|--------------|----------------|
| DB running | ✅ Yes | ❌ No |
| Locking | Minimal/none | Stop DB |
| Tools | XtraBackup, mysqldump --single-transaction | File copy after stopping mysqld |

→ Modern: hot backups always.

---

## Logical vs Physical

| | Logical | Physical |
|--|---------|---------|
| Format | SQL statements | Raw data files (.ibd) |
| Tools | mysqldump, mysqlpump, mydumper | XtraBackup, MySQL Enterprise Backup |
| Size | Larger (text) | Smaller (binary) |
| Speed | Slow | Fast |
| Portable | ✅ Cross-version, cross-engine | ❌ Same version mostly |
| Recovery | Slow (replay SQL) | Fast (copy files) |
| Use | Small DBs, migrations | Large prod DBs |

### When logical?

- Migration to different version / engine
- Single table / DB extraction
- Small DBs (< 100 GB)
- Compliance text-readable need

### When physical?

- Large DBs (> 100 GB)
- Fastest backup + restore
- Production HA setups
- Daily / scheduled backups

---

## mysqldump

> "Built-in. Logical backup. **Slow but universal.**"

### Basic

```bash
mysqldump -u root -p mydb > backup.sql
```

### Full database with options

```bash
mysqldump -u root -p \
    --single-transaction \         # consistent without lock (InnoDB)
    --routines \                    # stored procedures
    --triggers \
    --events \
    --master-data=2 \               # binlog position as comment
    --default-character-set=utf8mb4 \
    --skip-add-locks \
    mydb > backup.sql
```

### Multiple DBs

```bash
mysqldump --databases db1 db2 db3 > backup.sql
mysqldump --all-databases > all.sql
```

### Restore

```bash
mysql -u root -p mydb < backup.sql
# or
mysql -u root -p < backup.sql      # if backup includes CREATE DATABASE
```

### Pros

- Built-in (no install)
- Cross-version compatible
- Easy to inspect / edit (text)

### Cons

- **Slow**: 100GB DB = hours
- Single-threaded
- Restore = even slower (each statement parsed + executed)

→ OK for **small DBs (<10 GB)** or migrations.

---

## mysqlpump

> "**Parallel** version of mysqldump. Came in 5.7."

```bash
mysqlpump -u root -p \
    --default-parallelism=4 \
    --include-databases=mydb \
    --result-file=backup.sql
```

### Pros

- Parallel
- Compression options

### Cons

- **Restore not parallel** (use `mysql` client = serial)
- Less popular than `mydumper`

→ Niche; **skip in favor of mydumper** for parallel.

---

## mydumper / myloader

> "**Best logical backup tool** — parallel dump + parallel load."

### Install

```bash
apt install mydumper
```

### Backup

```bash
mydumper \
    --user=root --password=pwd \
    --database=mydb \
    --threads=8 \
    --outputdir=/backup/mydb \
    --compress \
    --rows=1000000              # split tables into chunks
```

### Restore (parallel)

```bash
myloader \
    --user=root --password=pwd \
    --threads=8 \
    --directory=/backup/mydb
```

### Pros

- **Parallel** dump + restore (10× faster than mysqldump)
- Per-table files (selective restore easy)
- Compression

### Cons

- Needs install
- Logical (still slower than physical for huge DBs)

---

## Percona XtraBackup

> "**The standard physical backup tool**. Hot, no downtime, incremental support."

### Install (Percona repo)

```bash
apt install percona-xtrabackup-80
```

### Full backup

```bash
xtrabackup --backup --target-dir=/backup/full \
    --user=root --password=pwd
```

### Prepare (apply transactions to make consistent)

```bash
xtrabackup --prepare --target-dir=/backup/full
```

### Restore

```bash
# 1. Stop MySQL
systemctl stop mysql

# 2. Empty data dir
rm -rf /var/lib/mysql/*

# 3. Copy back
xtrabackup --copy-back --target-dir=/backup/full

# 4. Fix ownership
chown -R mysql:mysql /var/lib/mysql

# 5. Start
systemctl start mysql
```

### Incremental backup

```bash
# Day 1 — full
xtrabackup --backup --target-dir=/backup/day1

# Day 2 — incremental from day 1
xtrabackup --backup --target-dir=/backup/day2 \
    --incremental-basedir=/backup/day1

# Day 3 — incremental from day 2
xtrabackup --backup --target-dir=/backup/day3 \
    --incremental-basedir=/backup/day2
```

### Restore from incremental chain

```bash
# 1. Prepare full (with --apply-log-only to keep redo log usable)
xtrabackup --prepare --apply-log-only --target-dir=/backup/day1

# 2. Apply incremental day 2
xtrabackup --prepare --apply-log-only \
    --target-dir=/backup/day1 \
    --incremental-dir=/backup/day2

# 3. Apply incremental day 3 (final, no --apply-log-only)
xtrabackup --prepare \
    --target-dir=/backup/day1 \
    --incremental-dir=/backup/day3

# 4. Restore as before
```

### Pros

- **Hot** — no downtime
- **Fast** (raw file copy)
- **Incremental** support
- Parallel
- Stream to S3 / pipe

### Cons

- Same MySQL version restore
- Setup learning curve

→ **Production standard**.

---

## Point-in-Time Recovery (PITR)

> "Restore database to **exact moment in time** (e.g., right before bad DROP)."

### Requirements

1. Recent **full backup** (logical or physical)
2. **All binlogs** since backup time

### Steps

```
T0: Full backup
T0 to T1: binlogs accumulate
T1: User runs DROP TABLE accidentally
T2: You realize, target T1 - 1 second
```

#### 1. Restore full backup

```bash
xtrabackup --copy-back --target-dir=/backup/full
# Or mysql < backup.sql
```

#### 2. Apply binlogs up to target time

```bash
mysqlbinlog \
    --start-datetime='2024-05-08 10:00:00' \
    --stop-datetime='2024-05-08 14:29:59' \
    binlog.000001 binlog.000002 binlog.000003 \
    | mysql -u root -p
```

→ Stops just before T1.

### Find specific position

```bash
mysqlbinlog binlog.000003 | grep -B 5 "DROP TABLE"
# Look for log position before DROP
mysqlbinlog --stop-position=12345 binlog.000003 | mysql -u root -p
```

### GTID-based

```bash
mysqlbinlog --include-gtids='SERVER_UUID:1-100' binlog.000001
```

### Practical tip

Run on **isolated host** first to verify before applying to production.

---

## Binary Log Backup

> "**Continuous backup** = binlogs streamed to remote storage."

### Why?

- PITR requires unbroken binlog history
- Don't lose binlogs to disk failure / rotation

### Tool — `mysqlbinlog --read-from-remote-server`

```bash
mysqlbinlog \
    --read-from-remote-server \
    --raw \
    --stop-never \
    --result-file=/backup/binlogs/ \
    --host=mysql_primary --user=repl --password=pwd \
    binlog.000001
```

→ Continuously streams binlogs as they're produced.

### Retention

```ini
binlog_expire_logs_seconds = 604800     # 7 days
# or
expire_logs_days = 7
```

→ MySQL purges automatically. **Backup before purge.**

---

## Backup Strategies (3-2-1)

> "**3 copies of data** on **2 different media** with **1 offsite**."

### Example

```
Copy 1: Live DB on primary disk
Copy 2: Daily XtraBackup on local backup server
Copy 3: Weekly snapshot to S3 (different region)
```

→ Survives: disk fail, server fail, datacenter loss.

### Daily / Weekly schedule

```
Sunday 02:00:    Full backup
Mon-Sat 02:00:   Incremental backup
Continuous:      Binlog streaming

Retention:
   Daily:    7 days
   Weekly:   4 weeks
   Monthly:  12 months
   Yearly:   Compliance
```

### Cloud examples

| AWS | Service |
|-----|---------|
| RDS automated backups | Daily snapshots, PITR up to 35 days |
| Aurora continuous | PITR up to 35 days, ~5 min RPO |
| Custom EC2 + XtraBackup → S3 | DIY |

---

## Encryption at Rest

> "Encrypt backup files. **Compliance + ransomware resistance**."

### XtraBackup encrypted

```bash
xtrabackup --backup \
    --encrypt=AES256 \
    --encrypt-key-file=/etc/keys/backup.key \
    --target-dir=/backup/full
```

### S3 encryption

S3 buckets enable SSE-S3 or SSE-KMS by default.

### MySQL transparent data encryption (TDE)

```ini
innodb_redo_log_encrypt = ON
innodb_undo_log_encrypt = ON
```

```sql
ALTER TABLE sensitive ENCRYPTION = 'Y';
```

→ Cross-ref `12-MySQL-Security.md`.

---

## Test Restores

> "**Untested backup = no backup.** Period."

### Checklist

```
[ ] Quarterly: full restore test on isolated host
[ ] Monthly: PITR drill (restore to specific time)
[ ] After schema migration: verify backup compatibility
[ ] After config change: re-test
```

### Document

Runbook with exact steps + commands. **People panic in disasters** — runbook saves time.

### Drill scenarios

- "Primary disk fails" → restore from backup
- "Bad UPDATE deleted data" → PITR to before
- "DC outage" → restore in another region from S3

→ Practice these in dev/staging.

---

## Pitfalls

1. **No backup at all** — happens more than you think.
2. **Untested backups** — corrupt / wrong data discovered during real disaster.
3. **Backup on same disk** — disk fail loses both.
4. **No offsite** — DC loss = total loss.
5. **`mysqldump` of 1TB DB** — takes day, restore takes 3 days.
6. **Binlog purge before backup** — PITR broken.
7. **No encryption** — backup leaks credentials / PII.
8. **No retention policy** — disk fills up.
9. **Backup runs without monitoring** — silent failures for weeks.
10. **No `--single-transaction`** in `mysqldump` — non-consistent backup of InnoDB.
11. **Restoring to wrong version** — schema incompatibility.
12. **Hot backup tools missing** for non-InnoDB tables — MyISAM tables not consistent.

---

## Cheat Sheet

| Tool | Type | Best for |
|------|------|----------|
| mysqldump | Logical | Small DBs, migration |
| mysqlpump | Logical parallel | Niche |
| **mydumper** | Logical parallel | Mid-size logical |
| **XtraBackup** | Physical | Production large DBs |
| Snapshot (LVM/EBS) | Physical | Cloud-managed |

| Backup Strategy |
|-----------------|
| Full = weekly |
| Incremental = daily |
| Binlog = continuous |
| Test restore = quarterly |
| 3-2-1 rule |

| PITR Steps |
|------------|
| 1. Restore full |
| 2. Apply incrementals |
| 3. Replay binlog up to target |

| Critical settings |
|-------------------|
| `binlog_format = ROW` |
| `expire_logs_days = 7+` |
| `binlog_row_image = FULL` (for safety) or MINIMAL (for size) |
| `sync_binlog = 1` |

---

## Practice

1. Take `mysqldump` of test DB. Restore to fresh server. Verify.
2. Set up XtraBackup full + incremental chain. Restore from chain.
3. Practice PITR — restore + replay binlog up to point before "bad" DROP.
4. Set up cron-based daily XtraBackup → S3.
5. Test backup restore on staging quarterly. Document time.
6. Encrypt backup with AES256; verify decryption.

# Module 07: Cron Jobs & Real-World Production Automation Scripts ⏰

Aap 24/7 server ke samne baith kar raat ko 2 baje backup nahi le sakte ya har 5 minute me API ping nahi kar sakte. Linux me scheduled background tasks ke liye **Cron** use hota hai.

---

## 1. Cron Daemon & Crontab Syntax 📅

Cron Linux ka built-in background scheduler hai jo har minute check karta hai ki koi task execute hone ke liye due hai ya nahi.

### Commands:
```bash
crontab -e      # Edit current user's cron schedule
crontab -l      # List all active cron jobs
crontab -r      # Delete all cron jobs (Dhayan se use karein!)
sudo crontab -e # Edit ROOT user's cron schedule
```

---

## 2. Deciphering the 5-Star Cron Expression ⭐

```
┌───────────── Minute (0 - 59)
│ ┌─────────── Hour (0 - 23, 24-hour format)
│ │ ┌───────── Day of Month (1 - 31)
│ │ │ ┌─────── Month (1 - 12)
│ │ │ │ ┌───── Day of Week (0 - 6, where 0 = Sunday)
│ │ │ │ │
* * * * *  /path/to/script.sh
```

### Common Production Schedules:
| Schedule Expression | Meaning | Typical Use Case |
| :--- | :--- | :--- |
| `*/5 * * * *` | Har 5 minute me | Health check monitor ping |
| `0 * * * *` | Har ghante ke start me | Metrics aggregation |
| `0 2 * * *` | Roz raat ko 2:00 AM | Database daily backup |
| `0 3 * * 0` | Har Sunday raat 3:00 AM | Weekly vacuum & index rebuild |
| `0 0 1 * *` | Har mahine ki 1 tareekh ko | Monthly billing invoice generator |

---

## 3. 🚨 3 Deadly Cron Gotchas (Sabhi Developers Yahan Phaste Hain!)

1. **Gotcha 1: The Minimal `$PATH` Issue:**
   - Cron aapka `.bashrc` ya `.zshrc` load **nahi** karta! Cron ka environment path bohot basic hota hai (`/usr/bin:/bin`).
   - Isliye cron me kabhi bhi simple `python app.py` ya `node script.js` mat likhein. **Hamesha full absolute path dein:**  
     `/usr/bin/python3 /opt/scripts/app.py`.
2. **Gotcha 2: Unhandled Output & Silent Failures:**
   - Agar cron script koi output ya error fekegi, toh OS use local mail box (`/var/mail/user`) me bhej deta hai jo koi check nahi karta.
   - Hamesha STDOUT aur STDERR ko log file me redirect karein:  
     `0 2 * * * /opt/scripts/backup.sh >> /var/log/backup.log 2>&1`
3. **Gotcha 3: Script Permissions:**
   - Make sure script par execute permission ho: `chmod +x /opt/scripts/backup.sh`.

---

## 4. Production Script 1: Database Backup & Cloud S3 Upload 💾

Yeh script roz raat ko chal kar PostgreSQL DB ka dump leti hai, use gzip se compress karti hai, AWS S3 par upload karti hai aur 7 din se puraane local backups delete kar deti hai.

File: `/opt/scripts/db_backup.sh`

```bash
#!/bin/bash
set -euo pipefail

# Configuration
BACKUP_DIR="/var/backups/postgres"
DATE=$(date +"%Y-%m-%d_%H-%M-%S")
DB_NAME="production_db"
DB_USER="postgres"
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_${DATE}.sql.gz"
S3_BUCKET="s3://mycompany-db-backups"
RETENTION_DAYS=7

# Step 1: Ensure backup directory exists
mkdir -p "${BACKUP_DIR}"

echo "[$(date)] Starting Database Backup for ${DB_NAME}..."

# Step 2: Take PostgreSQL Dump and compress on the fly
pg_dump -U "${DB_USER}" "${DB_NAME}" | gzip > "${BACKUP_FILE}"

echo "[$(date)] Backup created successfully: ${BACKUP_FILE} ($(du -h "${BACKUP_FILE}" | awk '{print $1}'))"

# Step 3: Sync to AWS S3 (Optional - if aws cli installed)
if command -v aws &> /dev/null; then
    echo "[$(date)] Uploading backup to S3 bucket..."
    aws s3 cp "${BACKUP_FILE}" "${S3_BUCKET}/" --quiet
    echo "[$(date)] S3 upload completed!"
fi

# Step 4: Cleanup local backups older than 7 days
echo "[$(date)] Cleaning up local backups older than ${RETENTION_DAYS} days..."
find "${BACKUP_DIR}" -type f -name "${DB_NAME}_*.sql.gz" -mtime +"${RETENTION_DAYS}" -exec rm -f {} \;

echo "[$(date)] All tasks finished successfully!"
```

Crontab entry:
```cron
0 2 * * * /opt/scripts/db_backup.sh >> /var/log/cron_db_backup.log 2>&1
```

---

## 5. Production Script 2: Log Rotation & Disk Space Watchdog 🧹

Agar disk 85% se upar ho jaye, toh puraane logs clean karo aur alert trigger karo:

File: `/opt/scripts/disk_cleanup.sh`

```bash
#!/bin/bash
set -euo pipefail

ALERT_THRESHOLD=85
LOG_DIR="/var/log/spring-boot"

CURRENT_USAGE=$(df / | awk 'NR==2 {print $5}' | tr -d '%')

echo "Current Root Disk Usage: ${CURRENT_USAGE}%"

if [ "${CURRENT_USAGE}" -gt "${ALERT_THRESHOLD}" ]; then
    echo "⚠️ Disk usage above ${ALERT_THRESHOLD}%! Starting aggressive cleanup..."
    
    # 14 din se purani compressed log files delete karo:
    if [ -d "${LOG_DIR}" ]; then
        find "${LOG_DIR}" -type f -name "*.log.gz" -mtime +14 -delete
        echo "Old compressed logs deleted."
    fi
    
    # Clean apt cache
    apt-get clean
    
    # Check new usage
    NEW_USAGE=$(df / | awk 'NR==2 {print $5}' | tr -d '%')
    echo "New Disk Usage after cleanup: ${NEW_USAGE}%"
fi
```

---

## 6. Production Script 3: API Health Check & Auto-Restart Watchdog 🩺

Aapka backend API (`localhost:8080/actuator/health`) har 2 minute me ping hoga. Agar 3 baar lagataar timeout ya 500 error aaye, toh Systemd service restart ho jayegi aur Slack par notification jayega:

File: `/opt/scripts/health_watchdog.sh`

```bash
#!/bin/bash

HEALTH_URL="http://127.0.0.1:8080/actuator/health"
SERVICE_NAME="backend-api.service"
SLACK_WEBHOOK_URL="https://hooks.slack.com/services/T00/B00/XXXX"

# HTTP status code nikalna (Max 5 second timeout)
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "${HEALTH_URL}")

if [ "${HTTP_CODE}" -ne 200 ]; then
    echo "[$(date)] Health check failed with HTTP code: ${HTTP_CODE}. Restarting service..."
    
    # Restart service
    sudo systemctl restart "${SERVICE_NAME}"
    
    # Send Slack alert
    if [ -n "${SLACK_WEBHOOK_URL}" ]; then
        PAYLOAD="{\"text\": \"🚨 *Alert*: ${SERVICE_NAME} on $(hostname) failed health check (HTTP ${HTTP_CODE}). Auto-restart executed!\"}"
        curl -s -X POST -H 'Content-type: application/json' --data "${PAYLOAD}" "${SLACK_WEBHOOK_URL}" > /dev/null
    fi
else
    echo "[$(date)] Health check OK (HTTP 200)"
fi
```

Crontab entry:
```cron
*/2 * * * * /opt/scripts/health_watchdog.sh >> /var/log/watchdog.log 2>&1
```

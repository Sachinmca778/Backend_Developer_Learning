# Module 06: Systemd, Service Management & Journalctl 🚀

Production me aap kabhi bhi `java -jar app.jar &` ya terminal me command chala kar nahi chhodte. Agar server reboot ho gaya, ya app out-of-memory hokar crash ho gayi, toh use automatically kaun restart karega? **Systemd!**

---

## 1. Systemd Kya Hai? (The Linux Init System)

`systemd` modern Linux distributions (Ubuntu 16+, Debian 8+, RHEL 7+) ka default system aur service manager hai (PID 1).

### Why Systemd is powerful:
- **Parallel Boot:** System start hote waqt saari independent services ko ek saath parallel me start karta hai (Fast boot time).
- **Auto-Restart:** Agar application kisi bug ya memory issue se crash ho jaye, toh systemd use instantly automatically restart kar deta hai.
- **Dependency Ordering:** Define kar sakte hain ki *"Backend app tabhi start ho jab Database aur Network successfully ready ho chuke hon."*
- **Resource Limits:** Har service ko CPU aur RAM limit assign kar sakte hain using Linux cgroups.

---

## 2. Managing Services with `systemctl` 🎛️

```bash
# 1. Service Status Check Karna:
sudo systemctl status nginx

# 2. Start / Stop / Restart:
sudo systemctl start nginx
sudo systemctl stop nginx
sudo systemctl restart nginx

# 3. Graceful Reload (Zero-Downtime Config Change):
# Service stop nahi hogi, sirf config file reload hogi!
sudo systemctl reload nginx

# 4. Boot-time Auto-start Enable / Disable:
sudo systemctl enable nginx    # Server reboot hote hi automatic start hoga
sudo systemctl disable nginx   # Reboot par start nahi hoga

# 5. Quick Health Checks in Bash Scripts:
systemctl is-active --quiet nginx && echo "Nginx is Running"
systemctl is-failed --quiet nginx && echo "Nginx is Down!"
```

---

## 3. Writing a Production-Grade Systemd Unit File 📝

Chaliye apne backend application (Spring Boot, Node.js ya Python) ke liye ek complete production service file banate hain.

File location: `/etc/systemd/system/backend-api.service`

```ini
[Unit]
Description=Backend REST API Spring Boot Service
# Database aur Network ready hone ke baad hi start karo:
After=network.target postgresql.service
Requires=postgresql.service

[Service]
Type=simple

# Security: Dedicated non-root user se run karein!
User=appuser
Group=appuser

# Application ki working directory:
WorkingDirectory=/opt/backend

# Environment Variables load karna (.env file se):
EnvironmentFile=/opt/backend/.env

# Exact Command jo run karni hai (Hamesha full absolute path dein!):
ExecStart=/usr/bin/java -Xms512m -Xmx2048m -Dspring.profiles.active=prod -jar /opt/backend/app.jar

# 🌟 CRASH RECOVERY: Agar app crash ho, automatically restart karo!
Restart=always
RestartSec=5s

# File descriptor limits (High traffic microservices ke liye zaroori):
LimitNOFILE=65535

# Linux Security Hardening:
NoNewPrivileges=true
ProtectSystem=full

[Install]
# Standard multi-user runlevel (OS boot hone par load karein):
WantedBy=multi-user.target
```

---

## 4. Activating the New Service (Step-by-Step) ⚡

Service file save karne ke baad:

```bash
# Step 1: Systemd ko batayein ki nayi/modified service file aayi hai:
sudo systemctl daemon-reload

# Step 2: System reboot par auto-start enable karein:
sudo systemctl enable backend-api.service

# Step 3: Service ko start karein:
sudo systemctl start backend-api.service

# Step 4: Status verify karein:
sudo systemctl status backend-api.service
```

---

## 5. Centralized Logging with `journalctl` 📜

Systemd sabhi services ke logs ko ek secure binary format me manage karta hai jise **`journald`** kehte hain. Logs inspect karne ke liye `journalctl` command use hoti hai:

```bash
# 1. 🌟 LIVE STREAM LOGS (Production Favorite - Like tail -f):
sudo journalctl -u backend-api.service -f

# 2. Pichhle 50 lines print karna:
sudo journalctl -u backend-api.service -n 50

# 3. Time filter lagana:
sudo journalctl -u backend-api.service --since "1 hour ago"
sudo journalctl -u backend-api.service --since "2026-10-27 14:00:00" --until "2026-10-27 15:30:00"

# 4. Sirf Errors aur Critical logs filter karna (-p flag = Priority):
# Levels: emerg, alert, crit, err, warning, notice, info, debug
sudo journalctl -u backend-api.service -p err

# 5. System Boot level failures inspect karna:
sudo journalctl -xe
# -x: Explanatory error details add karta hai
# -e: Jump directly to the end of pager
```

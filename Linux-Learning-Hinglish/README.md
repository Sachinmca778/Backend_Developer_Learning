# 🐧 Complete Linux Mastery & Production Engineering Guide (Hinglish 🇮🇳)

Yeh repository aapki **Linux Fundamentals se lekar Production Server Troubleshooting & DevOps Mastery** tak ki comprehensive 360° guide hai. Isme har ek concept ko simple, visual aur practical **Hinglish** (Hindi + English) me explain kiya gaya hai saath me **real-world production commands, architecture diagrams, shell scripts, security best practices aur Top 50 Interview Questions** shamil hain.

---

## 🗺️ Learning Roadmap: Beginner to Production DevOps

```
[ Phase 1: Foundations ] 
  ├── 01. Linux Basics & Kernel Architecture
  └── 02. Filesystem Hierarchy Standard (FHS) & Inodes
          │
[ Phase 2: Daily Developer Superpowers ]
  ├── 03. Navigation & File Operations (ls, find, less, tail -f)
  └── 04. Text Processing Mastery (grep, awk, sed, pipes, redirection)
          │
[ Phase 3: Administration & Security ]
  ├── 05. User & Group Management, Sudoers & visudo
  └── 06. File Permissions (chmod, chown, SUID, SGID, Sticky Bit, Umask)
          │
[ Phase 4: Production Operations & Performance ]
  ├── 07. Process Management & Linux Signals (SIGTERM vs SIGKILL)
  └── 08. System Performance Monitoring (CPU Load Average, RAM, Disk I/O, OOM)
          │
[ Phase 5: Cloud Networking & Automation ]
  ├── 09. Linux Networking (ss, lsof, curl, UFW firewall)
  ├── 10. SSH Hardening, Key Auth, Rsync & Bastion Tunneling
  ├── 11. Package Managers (APT, DNF, Alpine APK) & Tar Archives
  ├── 12. Systemd Service Management & Centralized Logging (journalctl)
  ├── 13. Shell Scripting (Strict Mode, Conditionals, Loops, Functions)
  └── 14. Cron Jobs & Production Automation Scripts
          │
[ Phase 6: Enterprise & Interview Prep ]
  ├── 15. Linux Internals for Docker (Namespaces, Cgroups) & Hardening
  └── 16. Top 50 Linux Interview Q&A + Disaster Recovery Runbooks
```

---

## 📑 Complete Syllabus & Directory Navigation

### 🟢 [Module 01: Linux Basics & Architecture](file:///Users/sachin/Backend_Developer_Learning/Linux-Learning-Hinglish/01-Linux-Basics-and-Architecture/)
- [**01. Introduction & Kernel Architecture**](file:///Users/sachin/Backend_Developer_Learning/Linux-Learning-Hinglish/01-Linux-Basics-and-Architecture/01-Introduction-and-Architecture.md): What is Linux?, Kernel vs OS, Monolithic vs Microkernel, System Calls API, Debian vs RedHat vs Alpine, Linux vs Windows comparison.
- [**02. Linux File System Hierarchy Standard (FHS) & Inodes**](file:///Users/sachin/Backend_Developer_Learning/Linux-Learning-Hinglish/01-Linux-Basics-and-Architecture/02-Linux-File-System-Hierarchy-FHS.md): "Everything is a file", Directory tree breakdown (`/etc`, `/var`, `/opt`, `/proc`, `/dev`), Inode mechanics, Hard Links vs Soft (Symbolic) Links (`ln` vs `ln -s`), Inode exhaustion troubleshooting.

---

### 🟡 [Module 02: Essential Commands & Text Processing](file:///Users/sachin/Backend_Developer_Learning/Linux-Learning-Hinglish/02-Essential-Commands-and-File-Management/)
- [**01. Navigation & File Operations**](file:///Users/sachin/Backend_Developer_Learning/Linux-Learning-Hinglish/02-Essential-Commands-and-File-Management/01-Navigation-and-File-Operations.md): `cd -` pro shortcuts, `ls -ltr` flags breakdown, `mkdir -p`, `cp -rp`, `rm -rf` danger zone, Log inspection with `less` and `tail -F`, finding files with `find` (size, time, exec).
- [**02. Text Processing, Pipes & Redirection**](file:///Users/sachin/Backend_Developer_Learning/Linux-Learning-Hinglish/02-Essential-Commands-and-File-Management/02-Text-Processing-and-Piping.md): STDIN, STDOUT, STDERR (0, 1, 2), Redirection (`>`, `>>`, `2>&1`, `&>`), Pipes (`|`), `grep` context flags (`-B`, `-A`, `-C`), `awk` column extraction & math, `sed` in-place search & replace, `sort`, `uniq -c`, `tee`, `xargs`, Nginx log analysis one-liners.

---

### 🟠 [Module 03: User Management & Permissions](file:///Users/sachin/Backend_Developer_Learning/Linux-Learning-Hinglish/03-User-Management-and-Permissions/)
- [**01. Users, Groups & Sudo**](file:///Users/sachin/Backend_Developer_Learning/Linux-Learning-Hinglish/03-User-Management-and-Permissions/01-Users-Groups-and-Sudo.md): Root vs Service vs Regular users, `/etc/passwd` & `/etc/shadow` format, `useradd`, `usermod -aG`, `su` vs `su -` vs `sudo -i`, `/etc/sudoers` safety and `visudo`.
- [**02. File Permissions & Ownership**](file:///Users/sachin/Backend_Developer_Learning/Linux-Learning-Hinglish/03-User-Management-and-Permissions/02-File-Permissions-and-Ownership.md): Read/Write/Execute on Files vs Directories, Octal notation (755, 644, 600, 400), `chmod` & `chown -R`, Special permissions: SUID (4xxx), SGID (2xxx), Sticky Bit (1xxx), `umask` calculation.

---

### 🔵 [Module 04: Process & Resource Monitoring](file:///Users/sachin/Backend_Developer_Learning/Linux-Learning-Hinglish/04-Process-and-Resource-Monitoring/)
- [**01. Process Management & Signals**](file:///Users/sachin/Backend_Developer_Learning/Linux-Learning-Hinglish/04-Process-and-Resource-Monitoring/01-Process-Management.md): Process lifecycle, PID 1 Systemd, Process states (Running, Sleep, Zombie, Orphan), `ps aux` vs `ps -ef`, `pgrep`, `nohup` & background job control, Linux Signals (`SIGTERM 15` vs `SIGKILL 9`, `SIGHUP 1`).
- [**02. System Performance & Troubleshooting**](file:///Users/sachin/Backend_Developer_Learning/Linux-Learning-Hinglish/04-Process-and-Resource-Monitoring/02-System-Resource-Monitoring-and-Performance.md): CPU Load Average math (1, 5, 15 min), `top` breakdown (`%us`, `%sy`, `%wa`, `%st`), `htop`, RAM `buff/cache` vs `available`, Swap & OOM Killer investigation, `df -h`, `du -sh`, `iostat`, Production 100% CPU Emergency Runbook.

---

### 🟣 [Module 05: Networking & Remote Access](file:///Users/sachin/Backend_Developer_Learning/Linux-Learning-Hinglish/05-Networking-and-Remote-Access/)
- [**01. Linux Networking & Firewalls**](file:///Users/sachin/Backend_Developer_Learning/Linux-Learning-Hinglish/05-Networking-and-Remote-Access/01-Linux-Networking-Commands.md): `ip addr` & interfaces, `0.0.0.0` vs `127.0.0.1`, Listening sockets with `ss -tulnp`, Port conflict triage with `lsof -i :8080`, `curl -v` API testing, `dig` DNS lookups, Netcat `nc`, UFW firewall rules.
- [**02. SSH, Rsync & Port Forwarding**](file:///Users/sachin/Backend_Developer_Learning/Linux-Learning-Hinglish/05-Networking-and-Remote-Access/02-SSH-and-Secure-Remote-Access.md): Asymmetric cryptography, Ed25519 key generation, strict permission checks, `~/.ssh/config` shortcuts, `scp` vs `rsync -avzP`, Local Port Forwarding Bastion Jump (`-L`), Server hardening checklist.

---

### 📦 [Module 06: Package Management & Systemd](file:///Users/sachin/Backend_Developer_Learning/Linux-Learning-Hinglish/06-Package-Management-and-Systemd-Services/)
- [**01. Package Managers & Tar Archives**](file:///Users/sachin/Backend_Developer_Learning/Linux-Learning-Hinglish/06-Package-Management-and-Systemd-Services/01-Package-Managers.md): APT lifecycle (`update`, `upgrade`, `purge`, `autoremove`), Third-party GPG keyrings, DNF/YUM, Alpine `apk --no-cache` for Docker, `tar -czvf` and `tar -xzvf` compression.
- [**02. Systemd Service Management**](file:///Users/sachin/Backend_Developer_Learning/Linux-Learning-Hinglish/06-Package-Management-and-Systemd-Services/02-Systemd-and-Service-Management.md): SysVinit vs Systemd, `systemctl` commands, writing production systemd unit file with auto-restart and security flags, Centralized logging with `journalctl -u app -f`.

---

### ⚙️ [Module 07: Shell Scripting & Automation](file:///Users/sachin/Backend_Developer_Learning/Linux-Learning-Hinglish/07-Shell-Scripting-and-Automation/)
- [**01. Bash Scripting Fundamentals**](file:///Users/sachin/Backend_Developer_Learning/Linux-Learning-Hinglish/07-Shell-Scripting-and-Automation/01-Bash-Scripting-Fundamentals.md): Shebang, variables, command substitution `$()`, Exit status `$?`, Bash Strict Mode (`set -euo pipefail`), arguments (`$1`, `$@`), conditionals `[[ ... ]]`, file tests, loops, modular functions.
- [**02. Cron Jobs & Production Scripts**](file:///Users/sachin/Backend_Developer_Learning/Linux-Learning-Hinglish/07-Shell-Scripting-and-Automation/02-Automation-Cron-Jobs-and-Real-Scripts.md): 5-star crontab syntax, 3 deadly cron pitfalls, Production Scripts: (1) DB Backup & S3 upload, (2) Disk watchdog cleanup, (3) API health check & auto-restart watchdog with Slack alerts.

---

### 🚀 [Module 08: DevOps, Docker & Interview Prep](file:///Users/sachin/Backend_Developer_Learning/Linux-Learning-Hinglish/08-DevOps-Backend-Production-Guide/)
- [**01. Linux Internals for Docker & Hardening**](file:///Users/sachin/Backend_Developer_Learning/Linux-Learning-Hinglish/08-DevOps-Backend-Production-Guide/01-Linux-for-Backend-and-Docker.md): Docker under the hood: Namespaces (PID, NET, MNT, IPC, UTS, USER) & Cgroups, Environment variables (`$PATH`, `/etc/environment`), Persistent disk mounting (`/etc/fstab`, `lsblk`), Fail2ban server hardening.
- [**02. Top 50 Linux Interview Q&A & Disaster Recovery**](file:///Users/sachin/Backend_Developer_Learning/Linux-Learning-Hinglish/08-DevOps-Backend-Production-Guide/02-Top-50-Linux-Interview-Questions-and-Troubleshooting.md): 50 high-yield questions categorized by architecture, filesystem, performance, networking, and 10 production disaster scenarios (Disk 100% full with deleted files open, Inode exhaustion, OOM killer triage, Port clashes, Zero-downtime reloads).

---

## ⚡ Master Quick Reference Cheatsheet

| Task | Command | Real World Example |
| :--- | :--- | :--- |
| **Check Current Folder** | `pwd` | `pwd` |
| **Go back to previous folder** | `cd -` | Switch between 2 directories |
| **List latest modified files** | `ls -ltr` | `ls -ltr /var/log/` |
| **Create nested directories** | `mkdir -p` | `mkdir -p backend/src/main/java` |
| **Real-time live log follow** | `tail -f` | `tail -f /var/log/nginx/access.log` |
| **View huge file without RAM freeze** | `less` | `less +F app.log` |
| **Search text in files** | `grep -rn` | `grep -rn "NullPointerException" ./logs/` |
| **Extract column from output** | `awk` | `awk '{print $1, $9}' access.log` |
| **In-place text replace** | `sed -i` | `sed -i 's/localhost/10.0.0.5/g' .env` |
| **Count lines** | `wc -l` | `grep "ERROR" app.log \| wc -l` |
| **Check Port holder process** | `lsof -i` | `sudo lsof -i :8080` |
| **List listening TCP/UDP ports** | `ss -tulnp` | `sudo ss -tulnp` |
| **Kill process gracefully** | `kill -15` | `kill -15 <PID>` |
| **Force kill frozen process** | `kill -9` | `kill -9 <PID>` |
| **Check CPU & Memory dashboard** | `top` / `htop` | Press `P` (CPU sort), `M` (RAM sort) |
| **Check available RAM** | `free -h` | Focus on **available** column |
| **Check Disk space** | `df -h` | Check root `/` filesystem |
| **Find folder sizes** | `du -sh *` | `du -sh /var/log/* \| sort -rh` |
| **Find huge files (>100M)** | `find` | `find / -type f -size +100M` |
| **Change File Permissions** | `chmod` | `chmod 755 run.sh && chmod 600 key.pem` |
| **Change Ownership** | `chown` | `sudo chown -R appuser:appuser /opt/app` |
| **Service Status** | `systemctl` | `sudo systemctl status backend-api` |
| **Service Live Logs** | `journalctl` | `sudo journalctl -u backend-api -f` |
| **Test API endpoint** | `curl -v` | `curl -v http://localhost:8080/health` |
| **Copy files fast** | `rsync -avzP` | `rsync -avzP ./dist/ prod:/var/www/` |

# Module 04: System Resource Monitoring & Performance Troubleshooting 📊

Jab production server slow ho jata hai ya APIs 504 Gateway Timeout dene lagti hain, toh sabse pehle system engineer ya backend developer ko server ke 4 pillars check karne hote hain: **CPU, Memory (RAM), Disk I/O, aur Network**.

---

## 1. CPU Monitoring & Load Average Deep Dive 📈

### 1. `uptime` Command
```bash
uptime
# 16:30:15 up 45 days,  2:14,  3 users,  load average: 1.25, 2.80, 4.10
```

### 🧠 Load Average Numbers ka Sach (1 min, 5 min, 15 min)
Load average ka matlab sirf CPU % nahi hota! **Load Average = Running processes + CPU Run Queue me wait kar rahe processes + Disk I/O wait me phanse processes.**

Kaise interpret karein?
- **Pehle check karein aapke system me kitne CPU Cores hain:**
  ```bash
  nproc
  # Output: 4 (Matlab 4 Cores hain)
  ```
- **Rule of Thumb:**
  - Agar **4 Core** CPU par Load Average **4.00** hai: Matlab CPU 100% full capacity par perfectly utilized hai (koi wait nahi).
  - Agar Load Average **< 4.00** (e.g. 1.25): System mast chill kar raha hai, capacity bachi hai.
  - Agar Load Average **> 4.00** (e.g. 12.00): Iska matlab 8 processes line me khade hokar CPU ka intezar kar rahe hain! Server overload hai.
  - **Trend Analysis:** Agar numbers `0.50, 1.80, 4.20` hain, matlab pichhle 15 minute me load kam ho raha hai (Recovery). Agar `8.50, 3.20, 1.10` hain, toh achanak huge traffic spike aaya hai!

---

### 2. `top` Command Decoded

`top` run karne par terminal par real-time dashboard aata hai:

```
top - 16:35:10 up 10 days, 2 users, load average: 0.85, 1.12, 0.95
Tasks: 185 total,   1 running, 184 sleeping,   0 stopped,   0 zombie
%Cpu(s): 12.5 us,  3.2 sy,  0.0 ni, 82.1 id,  1.8 wa,  0.0 hi,  0.4 si,  0.0 st
MiB Mem :   7948.5 total,   1250.2 free,   4520.1 used,   2178.2 buff/cache
MiB Swap:   2048.0 total,   1850.5 free,    197.5 used.   3120.4 avail Mem
```

#### `%Cpu(s)` Breakdown:
- **`%us` (User Time):** Aapka application code (Java, Python, Nginx) kitna CPU kha raha hai.
- **`%sy` (System / Kernel Time):** Linux kernel system calls execute karne me kitna busy hai.
- **`%id` (Idle Time):** CPU kitna free baitha hai (Jitna zyada utna achha).
- **`%wa` (I/O Wait) 🚨 CRITICAL:** CPU disk se read/write hone ka wait kar raha hai! Agar `%wa` > 20% hai, iska matlab CPU slow nahi hai, aapki **Hard Disk / SSD bottleneck ban chuki hai** (Slow database queries, heavy logs).
- **`%st` (Steal Time) ☁️:** Cloud VMs (AWS EC2, GCP) me dikhta hai. Agar `%st` high hai, iska matlab cloud provider ne aapka physical hardware kisi doosre noisy neighbor ko allocate kar diya hai!

**Shortcuts inside `top`:**
- `P`: Sort processes by CPU % (Highest CPU process upar)
- `M`: Sort processes by Memory % (Highest RAM process upar)
- `k`: Process ko directly PID dekar kill karna
- `q`: Exit

---

### 3. `htop` (Modern Colorful Alternative) 🌈
Agar server par installed nahi hai toh: `sudo apt install htop`
- Mouse click support
- Easy search (`F3`)
- Easy process kill (`F9`)
- Per-core CPU visual meters

---

## 2. Memory (RAM) & Swap Management 🧠

### `free -h` Command
```bash
free -h
#               total        used        free      shared  buff/cache   available
# Mem:          7.8Gi       3.2Gi       1.1Gi       250Mi       3.5Gi       4.3Gi
# Swap:         2.0Gi       200Mi       1.8Gi
```

### ⚠️ Myth: "My Linux server has only 1.1GB Free RAM, Server is out of memory?!"
**NO!** Linux ka design Windows se alag hota hai. Linux ka rule hai: *"Unused RAM is wasted RAM."*
Isliye Linux bachi hui free memory ko automatically Hard Disk se aane wale data ko cache karne ke liye **`buff/cache`** me use kar leta hai, taaki app super fast chale.

👉 **Golden Metric:** Hamesha **`available`** column dekhein! Agar aapki application ko aur RAM chahiye hogi, toh Linux immediately `buff/cache` ko drop karke app ko available memory de dega. Jab `available` 0 ke kareeb pahuche, tab real memory crisis hoti hai.

### Swap & The OOM (Out Of Memory) Killer 💀
- **Swap:** Hard disk par banayi gayi virtual RAM. Jab RAM full hone lagti hai, Linux inactive memory pages ko Swap me push kar deta hai.
- **Swappiness:** Kernel setting (`/proc/sys/vm/swappiness`, value 0 to 100). Production servers par isse `10` set karte hain taaki unnecessary disk thrashing na ho.
- **OOM Killer:** Jab RAM aur Swap dono 100% full ho jate hain, toh kernel system ko crash hone se bachane ke liye sabse heavy memory-hogging process (usually aapka Java app ya MySQL) ko `SIGKILL` bhej kar terminate kar deta hai!
- Check if OOM killed your app:
  ```bash
  dmesg -T | grep -i oom
  # Output: Out of memory: Killed process 3491 (java) total-vm:4294967kB
  ```

---

## 3. Disk Space & I/O Monitoring 💾

### 1. `df -h` (Disk Filesystem Free)
```bash
df -h
# Filesystem      Size  Used Avail Use% Mounted on
# /dev/sda1        50G   47G  1.5G  97% /
```
Agar `Use%` > 90% ho, immediate alert triggered!

### 2. `du` (Disk Usage - Folder-wise breakdown)
Kaun sa folder space kha raha hai:
```bash
# Current directory me sabhi top-level folders ka size:
du -sh *

# Top 10 sabse badi files/directories dhundo:
du -ah /var/log | sort -rh | head -n 10
```

### 3. `iostat` (Disk I/O Latency)
Check if disks are overloaded (`sudo apt install sysstat`):
```bash
iostat -xz 1
# '%util' column dekhein: Agar %util 100% ke paas hai, toh disk completely saturated hai!
```

---

## 4. Production Emergency Runbook: "Server 100% CPU Alert" 🚨

Jab PagerDuty alert bajta hai, follow this exact 5-step playbook:

```
[ Step 1: Login & Run uptime ]
  -> Check Load Average (Spike hai ya gradual?)
  -> Check nproc (Total cores)

[ Step 2: Open top or htop ]
  -> Press 'P' (Identify process consuming high CPU)
  -> Check %us vs %sy vs %wa (App problem vs Disk I/O bottleneck?)

[ Step 3: Inspect the Culprit Process ]
  -> Agar Java app hai: PID note karo.
  -> Thread dump lo before killing:
     jstack <PID> > /tmp/thread_dump.txt

[ Step 4: Check Logs for Anomalies ]
  -> tail -n 100 -f /var/log/nginx/error.log
  -> tail -n 100 -f /var/log/application.log

[ Step 5: Immediate Recovery Action ]
  -> Graceful restart: sudo systemctl restart myapp
  -> Agar unresponsive hai: sudo kill -9 <PID> && sudo systemctl start myapp
```

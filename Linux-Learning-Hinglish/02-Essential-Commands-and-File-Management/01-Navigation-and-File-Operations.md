# Module 02: Navigation & File Operations (Commands & Deep Dive) 🧭

Backend developers ko daily production servers par SSH karke logs check karne hote hain, configurations edit karni hoti hain aur files manage karni hoti hain. Yeh guide un sabhi commands ko cover karegi jo roz kaam aati hain.

---

## 1. Directory Navigation Commands

### `pwd` (Print Working Directory)
Aap is waqt system ke kis folder me baithe hain:
```bash
pwd
# Output: /home/sachin/Backend_Developer_Learning
```

### `cd` (Change Directory) & Pro Shortcuts
```bash
cd /var/log            # Absolute path par jana
cd projects/java-app   # Current directory ke andar jana (Relative)
cd ..                  # Ek step peeche (parent folder me) jana
cd ../..               # Do level upar jana
cd ~                   # Apne personal home directory me jana (/home/username)
cd                     # Sirf 'cd' likhne se bhi home directory me chala jata hai
cd -                   # 🚀 PRO TIP: Previous directory me wapas switch karna (Like Alt+Tab)
```

---

## 2. Directory Listing: `ls` Flags Explained

Simple `ls` sirf filenames print karta hai. Production me flags ke combination use hote hain:

```bash
ls -l       # Long listing (Permissions, Owner, Group, Size, Timestamp)
ls -a       # All files (Hidden files bhi dikhata hai jo '.' se start hoti hain jaise .bashrc, .env)
ls -h       # Human readable sizes (Bytes ki jagah KB, MB, GB me dikhata hai)
ls -t       # Sort by Modification Time (Sabse latest modified file sabse upar)
ls -r       # Reverse order me sort karna
ls -S       # Sort by File Size (Sabse badi file sabse pehle)
ls -la      # Long listing + Hidden files
ls -lh      # Long listing + Human readable file size
ls -lt      # Long listing sorted by newest first
ls -ltr     # 🚀 DEVOPS FAVORITE: Sabse latest file/log sabse NEECHE dikhegi terminal me!
```

Example Output breakdown:
```
-rw-r--r-- 1 sachin developers  4096 Oct 27 15:30 application.properties
│▲▲ ▲▲ ▲▲  │ │      │           │    │            │
││  │  │   │ │      │           │    │            └─ File Name
││  │  │   │ │      │           │    └────────────── Timestamp
││  │  │   │ │      │           └─────────────────── File Size (bytes)
││  │  │   │ │      └─────────────────────────────── Group
││  │  │   │ └────────────────────────────────────── Owner User
││  │  │   └──────────────────────────────────────── Hard Link Count
││  │  └──────────────────────────────────────────── Others Permissions (Read)
││  └─────────────────────────────────────────────── Group Permissions (Read)
│└────────────────────────────────────────────────── User/Owner Permissions (Read, Write)
└─────────────────────────────────────────────────── File Type (- = Regular file, d = Directory, l = Symlink)
```

---

## 3. Creating & Managing Files and Directories

### `mkdir` (Make Directory)
```bash
mkdir myproject

# 🚀 PRO TIP: Nested subfolders ek baar me banana (-p flag for parents)
mkdir -p backend/src/main/java/com/app
# Yeh bina error ke backend, src, main, java, com, app sabhi folders ek sath bana dega!
```

### `touch` (Create Empty File / Update Timestamp)
```bash
touch Dockerfile
touch file1.txt file2.txt file3.txt
# Agar file already exist karti hai, toh yeh content ko bina chhede uska modification time update kar deta hai.
```

### `cp` (Copy Files and Folders)
```bash
cp source.txt destination.txt

# Folder copy karna (-r / -R = Recursive):
cp -r src/ backup-src/

# 🚀 PRO TIP: Permissions aur Timestamps preserve karke copy karna (-p flag):
cp -rp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.backup
# Isse backup file ki permissions aur owner original jaise hi rehte hain.
```

### `mv` (Move & Rename)
Linux me rename ke liye alag command nahi hoti; `mv` hi move aur rename dono karta hai:
```bash
mv old_name.txt new_name.txt        # Rename
mv app.jar /opt/deployments/        # Move to another directory
mv app.jar /opt/deployments/api.jar # Move aur rename dono ek sath
```

### `rm` (Remove / Delete) & ⚠️ The Danger Zone
```bash
rm file.txt             # File delete
rm -f file.txt          # Force delete (bina confirmation puche)
rm -r myfolder/         # Folder aur uske andar ke sabhi files delete
rm -rf myfolder/        # Forcefully recursive delete (Production me bahut dhyaan se use karein!)

# 🚨 DEADLY COMMAND:
# rm -rf /          --> Poora OS delete!
# rm -rf ./*        --> Current directory ka sab kuch delete!
```

> **Safe Practice:** Production me alias bana kar rakhein ya delete karne se pehle `ls` karke verify karein:
> `ls /tmp/test*` verify karne ke baad `rm -rf /tmp/test*`.

---

## 4. Reading & Viewing Files (Logs Inspection)

Backend developer ke life ka 50% time logs dekhne me jata hai.

### 1. `cat` (Concatenate & Print)
Chhoti files ko pura terminal par print karne ke liye:
```bash
cat application.yml
cat -n application.yml    # Line numbers ke sath print karega
```
> ⚠️ **Warning:** Kabhi bhi 1GB ki log file par `cat` mat chalao! Terminal hang ho jayega aur buffer freeze ho jayega.

### 2. `less` (Paginated Interactive Viewer) 🥇
Badi log files (100MB - 10GB) ko dekhne ka standard tool. Yeh file ko ek saath RAM me load nahi karta, page by page scroll karta hai.
```bash
less /var/log/application.log
```
**`less` ke andar navigation shortcuts:**
- `Spacebar` ya `Page Down`: Ek page aage
- `b` ya `Page Up`: Ek page peeche
- `/NullPointerException`: Aage search karega (Search forward)
- `?DatabaseTimeout`: Peeche search karega (Search backward)
- `n`: Next match par jayega
- `N`: Previous match par jayega
- `G` (Capital G): File ke bilkul aakhri (bottom) par jump karega
- `g` (Small g): File ke bilkul shuru (top) par jump karega
- `q`: Exit karke terminal par wapas aana

### 3. `head` (View Top Lines)
```bash
head /var/log/syslog         # Pehli 10 lines dikhayega
head -n 25 /var/log/syslog    # Pehli 25 lines dikhayega
```

### 4. `tail` (View Bottom Lines & Live Logs) 🚀
```bash
tail -n 50 app.log           # Last ki 50 lines print karega

# 🌟 MUST-KNOW FOR BACKEND: Live Stream Logs (-f = Follow)
tail -f /var/log/nginx/access.log
# Jaise hi website par request aayegi, real-time terminal me log line aati dikhegi!

# 🚀 PRO TIP: Agar logrotate file ko recreate kar deta hai, toh -F use karein:
tail -F /var/log/spring-boot/app.log
# -F follow karta rehta hai chahe file rename hokar nayi file ban jaye.
```

---

## 5. Searching Files: `find` vs `locate` vs `which`

### 1. `find` (The Powerhouse) 🔍
`find` disk par real-time traverse karta hai aur conditions ke base par search karta hai.

```bash
# 1. Name ke basis par search (Case-insensitive: -iname)
find /var/log -name "*.log"
find . -iname "*spring*"

# 2. File Type ke basis par (-type f for file, -type d for directory)
find /home/sachin -type f -name "*.java"
find /etc -type d -name "*nginx*"

# 3. File Size ke basis par (Production disk cleanup me bahut kaam aata hai)
find /var/log -type f -size +100M       # 100MB se badi files
find / -type f -size +1G                # 1GB se badi files dhundo

# 4. Modification Time ke basis par (-mtime)
find /var/log/app -name "*.log" -mtime +30   # 30 din se puraani files
find . -type f -mmin -10                     # Pichhle 10 minute me modify hui files

# 5. Search karke directly Action lena (-exec)
# 30 din se purani sabhi .log files ko automatically delete karna:
find /var/log/myapp -name "*.log" -mtime +30 -exec rm -f {} \;
```

### 2. `which` & `whereis`
Koi software/executable binary kahan installed hai:
```bash
which java
# Output: /usr/bin/java

which docker
# Output: /usr/bin/docker

whereis nginx
# Output: nginx: /usr/sbin/nginx /etc/nginx /usr/share/nginx
```

---

## Quick Reference Summary Table

| Command | Purpose | Production Use Case |
| :--- | :--- | :--- |
| `pwd` | Show current directory path | Scripts me current working directory check karna |
| `cd -` | Switch to last visited folder | Do folders ke beech baar-baar switch karte waqt |
| `ls -ltr` | List files sorted by time | Sabse newly generated log file identify karna |
| `mkdir -p a/b/c` | Create nested folders | Project directory skeleton create karna |
| `cp -rp` | Copy with permissions | Config files ka exact replica backup banana |
| `tail -f app.log` | Real-time log streaming | Live testing aur API debugging |
| `less +F app.log` | View & stream logs | Large files me scrolling + live updates |
| `find / -size +1G` | Find huge files | "Disk Full 100%" alert fix karte waqt |

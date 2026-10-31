# Module 01: Linux File System Hierarchy Standard (FHS) & Inodes 📂

Windows me hard drives alag-alag letters me bati hoti hain: `C:\`, `D:\`, `E:\`.
Lekin Linux me koi drive letters nahi hote! **Linux me sirf ek hi single root directory hoti hai: `/` (Forward Slash).** Chahe aapke computer me 10 alag physical hard drives ya SSDs lagi ho, sab `/` ke kisi na kisi folder ke andar mount hoti hain.

---

## 1. "Everything is a File" Concept 💡

Linux aur Unix philosophy ka sabse important rule hai:
> **"In Linux, everything is a file."**

Agar koi cheez file nahi hai, toh woh ek running process hai!
- **Normal file:** `.txt`, `.java`, `.pdf`, `.mp4`
- **Directory (Folder):** Linux ke liye folder bhi ek special file hai jisme doosri files ke naam aur Inode numbers ki list hoti hai.
- **Hardware Devices:** Aapka keyboard, mouse, hard disk (`/dev/sda1`), pendrive sab files ke roop me represent hote hain.
- **Network Sockets:** TCP/IP connections bhi sockets (files) ke roop me treat hote hain.
- **Running Processes:** CPU aur RAM ke running processes `/proc/` directory ke andar virtual files bankar dikhte hain.

---

## 2. Linux Directory Tree Structure (FHS)

```
/ (Root Directory)
├── bin -> usr/bin          (Essential User Binaries: ls, cat, cp, mkdir)
├── sbin -> usr/sbin        (System Binaries for Admin/Root: iptables, fdisk, reboot)
├── etc                     (System-wide Configuration Files: nginx.conf, hosts, passwd)
├── var                     (Variable Data: logs, databases, cache)
│   ├── log                 (System & Application logs: syslog, auth.log, nginx/)
│   ├── www                 (Web application files: html, php)
│   └── lib                 (Package state & database data: mysql, docker)
├── opt                     (Optional / 3rd Party software: /opt/java, /opt/google)
├── usr                     (User System Resources: secondary hierarchy)
│   ├── bin                 (Non-essential standard user commands)
│   ├── lib                 (Libraries for binaries)
│   └── local               (Locally compiled software by admin)
├── home                    (Regular Users' Personal Home Directories: /home/sachin)
├── root                    (Superuser / Root user's Home Directory)
├── tmp                     (Temporary files, auto-cleared on reboot)
├── dev                     (Device files: /dev/null, /dev/random, /dev/sda)
├── proc                    (Virtual filesystem: Process & Kernel info in memory)
├── sys                     (Virtual filesystem: Kernel hardware subsystem)
├── mnt                     (Temporary mount point for external drives)
└── media                   (Removable media: USB drives, CD-ROMs)
```

---

## 3. Directory by Directory In-Depth Guide

### 1. `/` (The Root)
Pura file system yahin se shuru hota hai. Superuser (root) ke alawa koi bhi user directly yahan file write nahi kar sakta.

### 2. `/etc` (Configuration Central) ⚙️
Production servers par sabse zyada visit kiya jane wala folder:
- `/etc/passwd`: System ke sabhi users ki list.
- `/etc/shadow`: Users ke encrypted passwords.
- `/etc/group`: User groups ki details.
- `/etc/hosts`: Local DNS mapping (IP to domain names).
- `/etc/ssh/sshd_config`: SSH server ka configuration.
- `/etc/nginx/` ya `/etc/apache2/`: Web server configs.
- `/etc/systemd/system/`: Custom background services (jaise aapka Spring Boot service unit file).

### 3. `/var` (Variable Data) 📈
Aisi files jo system chalte waqt continuously grow hoti hain:
- `/var/log/`: **Production troubleshooting ka sabse bada dost!**
  - `/var/log/syslog` ya `/var/log/messages`: OS level messages.
  - `/var/log/auth.log` ya `/var/log/secure`: SSH login attempts, failed passwords, brute-force attacks.
  - `/var/log/nginx/access.log` & `/var/log/nginx/error.log`: HTTP traffic & 502/500 errors.
- `/var/lib/docker/`: Docker images, containers aur volumes ka physical storage.
- `/var/lib/mysql/` ya `/var/lib/postgresql/`: Database storage files.

### 4. `/opt` (Optional Add-on Packages) 📦
Jab aap koi commercial ya custom pre-compiled software install karte hain (jaise custom JDK `/opt/jdk-17`, Tomcat, Prometheus, Splunk agent), unhe standard conventions ke mutabik `/opt` me rakha jata hai.

### 5. `/home` vs `/root`
- `/home/<username>`: Regular users ka personal playground (e.g. `/home/sachin`). User ke pass yahan read/write/delete ki full permission hoti hai. Shortcut: `~` (tilde).
- `/root`: Yeh root (Super Admin) user ka personal home folder hai (`/home/root` nahi hota, directly `/root` hota hai).

### 6. `/tmp` (Temporary Storage)
Koi bhi process temporary files create karne ke liye ise use karti hai. Linux distributions reboot par ya periodic cron job se `/tmp` ko automatic clean karte rehte hain. Kabhi bhi important permanent data yahan na rakhein!

### 7. `/dev` (Special & Device Files) 🔌
Yahan hard drives (`/dev/nvme0n1`, `/dev/sda`), partitions (`/dev/sda1`) hote hain.
Special virtual devices:
- `/dev/null`: **The Black Hole.** Jo bhi output isme redirect karoge, woh silently gayab ho jayega. (Used in scripts: `command > /dev/null 2>&1`).
- `/dev/zero`: Infinite null bytes provide karta hai (swap file create karne me use hota hai).
- `/dev/urandom`: Cryptographically secure random data generator (tokens, keys generate karne me use hota hai).

### 8. `/proc` (The Kernel's Window into Memory) 🧠
Yeh disk par physical space occupy nahi karta. Yeh **RAM me rehta hai (Virtual Filesystem)**.
- `/proc/cpuinfo`: CPU model, cores, cache size.
- `/proc/meminfo`: Detailed RAM and Swap usage.
- `/proc/<PID>/`: Har running process ka ek folder hota hai! Jaise agar aapki Java app ka PID 2489 hai, toh `/proc/2489/status`, `/proc/2489/cmdline`, `/proc/2489/fd` (file descriptors) me uski complete live state mil jayegi.

---

## 4. Paths: Absolute Path vs Relative Path

```
Current Location: /home/sachin/projects
Target File: /var/log/nginx/access.log
```

- **Absolute Path:** Always starts from the root `/`. Yeh kahin se bhi run karo, hamesha exact same file par point karega.
  ```bash
  cat /var/log/nginx/access.log
  ```
- **Relative Path:** Starts from your current working directory (`pwd`).
  - `.` (single dot) = Current directory
  - `..` (double dot) = Parent directory (ek level upar)
  ```bash
  cd ../../var/log/nginx
  ```

---

## 5. Inodes & Links (Hard Links vs Soft Links) 🔗

Linux me file ka naam sirf ek insaan ke samajhne ke liye label hai. Under the hood, Linux filesystem file ko ek **Inode (Index Node) Number** se pehchanta hai.

### Inode kya store karta hai?
- File type (regular, directory, socket, etc.)
- File permissions (rwxr-xr-x)
- Owner UID & Group GID
- File size (bytes me)
- Timestamps: `atime` (access time), `mtime` (modification time), `ctime` (metadata change time)
- Pointers to disk data blocks (data physically kahan pada hai)
> ⚠️ **Note:** Inode file ka content aur file ka **NAAM** store nahi karta! Directory file naam aur inode number ka map store karti hai.

Check inode number of any file:
```bash
ls -i myfile.txt
# Output: 1441829 myfile.txt
```

---

### Hard Link vs Soft Link (Symbolic Link)

```
[ Soft Link (Symlink) ] ---> [ Original File Name ] ---> [ Inode #1234 ] ---> [ Data Blocks on Disk ]
                                                                 ^
[ Hard Link ] ---------------------------------------------------+
```

| Feature | Soft Link (Symbolic / Symlink) | Hard Link |
| :--- | :--- | :--- |
| **Command** | `ln -s original.txt symlink.txt` | `ln original.txt hardlink.txt` |
| **Inode Number** | Ek **naya alag inode** banta hai jo original file ke path ko point karta hai. | Original file ka **same exact inode number** share karta hai. |
| **Original File Deleted?** | **Broken link!** Link exist karega lekin file open nahi hogi (Red color me dikhega). | **Data safe rahega!** Content access hoga jab tak link count > 0 hai. |
| **Across Filesystems?** | Haan, alag partitions/disks ke beech ban sakta hai. | Nahi, same filesystem/partition me hona zaroori hai. |
| **Directory linking?** | Directories par link ban sakta hai (e.g. `/bin -> usr/bin`). | Directories par hard link allow nahi hota (filesystem loops se bachne ke liye). |

### Practical Example:
```bash
# File create karte hain
echo "Backend Learning" > test.txt

# Soft Link banate hain
ln -s test.txt soft_test.txt

# Hard Link banate hain
ln test.txt hard_test.txt

# Check inodes and link count
ls -li *_test.txt
# 10234 -rw-r--r-- 2 sachin sachin 17 Oct 27 16:00 hard_test.txt
# 10234 -rw-r--r-- 2 sachin sachin 17 Oct 27 16:00 test.txt
# 10235 lrwxrwxrwx 1 sachin sachin  8 Oct 27 16:01 soft_test.txt -> test.txt

# Delete original file
rm test.txt

cat hard_test.txt # ✅ Chalega! (Data block delete nahi hua)
cat soft_test.txt # ❌ Error: No such file or directory (Dangling symlink)
```

---

## Real World Production Tips 💡
1. **"No space left on device" error jab disk me space bachi ho:**
   - Agar aap `df -h` run karte hain aur disk 40% free dikhati hai, lekin phir bhi file write nahi hoti — toh problem hoti hai **Inodes exhaust ho jana**!
   - Lakhon chhote-chhote session files ya cache files create hone par Inodes khatam ho jate hain.
   - Command to check inodes: `df -i`.
2. **Log directories me Symlink ka use:**
   - Log files root partition ko full na karein, isliye production me `/var/log` ko alag large disk partition par mount kiya jata hai ya symlinks banaye jate hain.

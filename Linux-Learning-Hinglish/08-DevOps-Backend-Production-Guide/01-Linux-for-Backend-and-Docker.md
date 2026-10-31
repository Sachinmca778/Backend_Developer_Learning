# Module 08: Linux Internals for Backend, Docker & Production Hardening 🐳

Docker koi magical virtual machine nahi hai! **Docker is just a standard Linux process running with Kernel Namespaces and Control Groups (cgroups).** Agar aapko Linux ke internals samajh aate hain, toh containerization aur cloud debugging aapke liye baye hath ka khel ban jati hai.

---

## 1. How Docker Works Under the Hood: Namespaces & Cgroups 🏗️

Virtual Machines (jaise VMware, VirtualBox) poora Guest OS, alag Kernel aur Virtual Hardware emulate karti hain (High RAM/CPU overhead).  
**Docker containers host machine ka same Linux Kernel share karte hain!**

```
+-----------------------------------------------------------+
|                   Docker Engine / Containerd              |
+-----------------------------------------------------------+
| Container 1 (Spring App)        | Container 2 (Postgres)  |
| - Own isolated PID 1            | - Own isolated PID 1    |
| - Own isolated IP 172.17.0.2    | - Own isolated IP .3    |
| - Limited to 1GB RAM            | - Limited to 2GB RAM    |
+---------------------------------+-------------------------+
|                  HOST LINUX KERNEL                        |
|   ┌─────────────────────────────────────────────────┐     |
|   │ Namespaces (Isolation) + Cgroups (Resource Limit)│    |
|   └─────────────────────────────────────────────────┘     |
+-----------------------------------------------------------+
|                      PHYSICAL HARDWARE                    |
+-----------------------------------------------------------+
```

### 1. Linux Namespaces (What a Container can SEE 👁️)
Namespaces processes ko isolate karti hain taaki ek container doosre container ya host ko na dekh sake:
- **PID Namespace:** Container ke andar aapki Java app **PID 1** bankar chalti hai, jabki host server par uska ek alag PID (e.g. 5240) hota hai!
- **NET Namespace:** Har container ko apna alag virtual network card (`eth0`), routing table aur IP address milta hai.
- **MNT (Mount) Namespace:** Container ko sirf apna isolated file system dikhta hai (Host ka `/` root nahi dikhta).
- **IPC Namespace:** Shared memory aur inter-process communication isolate karta hai.
- **UTS Namespace:** Container ko apna unique Hostname rakhne deta hai.
- **USER Namespace:** Container ke andar root (UID 0) ko host machine ke standard unprivileged user (UID 10001) par map karta hai (Super Secure!).

### 2. Linux Cgroups - Control Groups (What a Container can USE 🛑)
Cgroups ensure karte hain ki koi ek container poore server ka CPU ya RAM khatam karke baaki services ko crash na kar de:
```bash
# Docker run karte waqt jo limits aap lagate hain:
docker run -d --memory="1g" --cpus="1.5" my-backend-image
# Under the hood, Linux kernel ke /sys/fs/cgroup/memory/ me limits enforce hoti hain!
```

---

## 2. Environment Variables & Path Configuration 🌐

Backend applications configuration ke liye environment variables par rely karti hain (`DATABASE_URL`, `JWT_SECRET`, `SPRING_PROFILES_ACTIVE`).

### Shell Variable vs Environment Variable:
```bash
# 1. Local Shell Variable (Sirf current bash prompt me visible):
PORT=8080
# Nayi child process ya script me accessible NAHI hoga!

# 2. Environment Variable ('export' keyword):
export PORT=8080
# Current shell aur uske andar se run hone wale sabhi scripts/processes me pass hoga!
```

### Files Jisme Variables Define Hote Hain:
- **`~/.bashrc`:** Current user ke non-login interactive shells ke liye. Aliases aur personal `$PATH` yahan set karte hain.
- **`~/.bash_profile` ya `~/.profile`:** SSH login karte waqt ek baar execute hota hai.
- **`/etc/environment`:** System-wide static key-value pairs (sab users aur services ke liye).

### Modifying `$PATH` (The Executable Search Path):
Jab aap `java` ya `mvn` type karte hain, toh Linux `$PATH` me list kiye folders me binary dhundta hai:
```bash
echo $PATH
# /usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

# Naya path append karna (e.g., custom JDK 21):
export PATH=$PATH:/opt/jdk-21/bin
# Permanent banane ke liye is line ko ~/.bashrc ke end me add karein!
```

---

## 3. Disks, Partitioning & Persistent Mounting (`/etc/fstab`) 💾

Cloud (AWS EBS) par jab aap naya 100GB volume attach karte hain:

```bash
# 1. Sabhi block devices list karein:
lsblk
# NAME    MAJ:MIN RM  SIZE RO TYPE MOUNTPOINTS
# sda       8:0    0   50G  0 disk 
# └─sda1    8:1    0   50G  0 part /
# sdb       8:16   0  100G  0 disk   <-- Naya Unformatted Volume!

# 2. Filesystem format karein (ext4 ya XFS):
sudo mkfs.ext4 /dev/sdb

# 3. Mount point banayein aur mount karein:
sudo mkdir -p /data
sudo mount /dev/sdb /data

# 4. Verify mount:
df -h /data
```

### 🚨 Reboot Persistent Banana: `/etc/fstab`
Agar aapne sirf `mount` kiya, toh server reboot hote hi mount hat jayega! Permanent karne ke liye disk ka **UUID** `/etc/fstab` me daalte hain:

```bash
# UUID nikalna:
sudo blkid /dev/sdb
# /dev/sdb: UUID="4bf7a8e2-9b21-4f11-9230-abcd1234efgh" TYPE="ext4"

# /etc/fstab me entry add karein:
# UUID                                  MountPoint  Type   Options  Dump Pass
UUID=4bf7a8e2-9b21-4f11-9230-abcd1234efgh /data       ext4   defaults   0    2

# Test mount without rebooting (Safe check!):
sudo mount -a
```

---

## 4. Production Server Hardening Checklist 🛡️

Naye Linux instance ko production-ready banane ke liye essential security steps:

```
┌────────────────────────────────────────────────────────────────────────┐
│               Production Linux Server Security Baseline                │
├────────────────────────────────────────────────────────────────────────┤
│ 1. User Safety: Direct root SSH disabled (PermitRootLogin no).         │
│ 2. Auth: Password login disabled; Ed25519 SSH keys only.               │
│ 3. Firewall: UFW enabled, only Ports 22, 80, 443 open.                 │
│ 4. Brute Force Protection: Fail2ban installed (Auto-ban attacking IPs).│
│ 5. Auto Security Updates: unattended-upgrades package active.          │
│ 6. App Isolation: Dedicated non-root service users for each app.       │
│ 7. Audit: Periodic check of open ports using ss -tulnp.                │
└────────────────────────────────────────────────────────────────────────┘
```

### Install Fail2ban in 1 Minute:
```bash
sudo apt update && sudo apt install -y fail2ban
sudo systemctl enable --now fail2ban
# Agar koi 5 baar wrong SSH password daalega, toh uska IP 10 minute ke liye firewall me block ho jayega!
```

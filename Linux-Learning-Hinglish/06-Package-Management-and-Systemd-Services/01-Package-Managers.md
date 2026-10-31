# Module 06: Package Managers (APT, DNF, APK) & Archives (Tar) 📦

Linux me software install karne ke liye Windows ki tarah `.exe` download karke "Next-Next-Finish" nahi karna hota. Linux distributions centralized, cryptographically-signed **Software Repositories** use karti hain.

---

## 1. Package Managers Overview

```
Distro Family       Package Format    Low-Level Tool    High-Level Manager (Resolves Dependencies)
Debian / Ubuntu     .deb              dpkg              apt / apt-get
RHEL / CentOS       .rpm              rpm               dnf / yum
Alpine Linux        .apk              apk               apk
Arch Linux          .pkg.tar.zst      pacman            pacman
```

---

## 2. APT (Debian & Ubuntu Standard) 🐧

`apt` (Advanced Package Tool) dependencies ko automatically calculate aur resolve karta hai.

### Daily Lifecycle Commands:
```bash
# 1. Update Package Index (Pehle hamesha yeh karein!):
# Yeh internet repositories se latest software versions ki list download karta hai (Software install nahi karta)
sudo apt update

# 2. Installed software ko latest versions par upgrade karna:
sudo apt upgrade -y

# 3. Naya software install karna:
sudo apt install -y openjdk-17-jdk nginx git curl

# 4. Software Uninstall Karna:
sudo apt remove nginx           # Binaries delete karega, lekin configuration files chhod dega
sudo apt purge nginx            # Binaries + Config files sab kuch clean wipe karega!

# 5. Leftover Unused Dependencies Clean Karna:
sudo apt autoremove -y          # Purane Linux kernels aur orphan packages delete karta hai
sudo apt clean                  # /var/cache/apt/archives se downloaded .deb cache delete karta hai

# 6. Package search karna:
apt search postgresql-16
```

### Adding Third-Party Repositories & GPG Keys (e.g. Official Docker Repo)
Kayi baar Ubuntu ke default repos me purana software version hota hai. Official vendor repos add karne ka modern standard:
```bash
# 1. Vendor ka GPG key download karke keyring me save karein (Tamper-proofing):
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# 2. Repo URL sources.list.d me add karein:
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 3. Update & install:
sudo apt update
sudo apt install docker-ce
```

### Low-Level `.deb` Handling with `dpkg`:
Agar aapne manual `.deb` file download ki hai:
```bash
sudo dpkg -i package_name.deb    # Install
sudo dpkg -l | grep java         # Check installed packages
sudo dpkg -r package_name        # Remove
```

---

## 3. DNF / YUM (RedHat, Rocky Linux, CentOS) 🎩

RHEL 8+ me `dnf` default package manager hai:

```bash
sudo dnf check-update            # Check for updates
sudo dnf install -y nginx        # Install
sudo dnf update -y               # System-wide upgrade
sudo dnf remove -y nginx         # Remove
sudo dnf clean all               # Cache cleanup
```

---

## 4. APK (Alpine Linux - Docker Container Superpower) 🏔️

Dockerfiles ko chhota (10x smaller) rakhne ke liye hum Alpine Linux base images use karte hain.

```dockerfile
FROM alpine:3.19

# ⚠️ Docker Pro Tip: Hamesha '--no-cache' flag use karein!
# Isse package index disk par save nahi hota aur Docker image ka size 20MB kam ho jata hai.
RUN apk update && apk add --no-cache curl openjdk17-jre-headless
```

---

## 5. Archiving & Compression with `tar` 🗜️

Linux me software tarballs (`.tar.gz`) me distribute hota hai aur backups bhi tar me bante hain.

```
.tar       = Tape Archive (Bina compression ke sabhi files ko ek bundle me baandhna)
.tar.gz    = Gzip se compress kiya hua tar bundle (Size 80% chhota ho jata hai)
```

### The 4 Magic Flags in `tar`:
- `-c`: **Create** a new archive
- `-x`: **Extract** an archive
- `-z`: Use **Gzip** compression
- `-v`: **Verbose** (Terminal par file names print karo)
- `-f`: Specify **Filename** (Archive ka naam)

```bash
# 1. Compress a folder into .tar.gz (Backup banana):
tar -czvf my-app-backup.tar.gz /var/www/my-app/

# 2. Extract a .tar.gz file into current directory:
tar -xzvf jdk-17_linux-x64_bin.tar.gz

# 3. Extract into a specific destination directory (-C flag):
sudo tar -xzvf apache-tomcat-10.tar.gz -C /opt/

# 4. Extract standard .zip files:
unzip project.zip -d /opt/myproject/
```

# Module 03: Linux User & Group Management and Sudo 👥

Linux ek multi-user operating system hai. Enterprise servers par multiple developers, devops engineers aur automated services (jaise Nginx, PostgreSQL, Jenkins) simultaneously kaam karte hain. System ki security ke liye users aur unke privileges ko control karna bohot zaroori hai.

---

## 1. Types of Users in Linux 👤

Linux me 3 categories ke users hote hain:

```
+--------------------------------------------------------------------------+
| 1. Root User (UID 0)                                                     |
|    - The Superuser / God mode. Har file, process aur hardware par full   |
|      control. Koi bhi restriction nahi hoti.                             |
+--------------------------------------------------------------------------+
| 2. System / Service Users (UID 1 - 999)                                  |
|    - Background daemons aur services ke liye (nginx, mysql, www-data).   |
|    - Inke paas interactive shell nahi hota (/bin/false ya /usr/sbin/nologin)
+--------------------------------------------------------------------------+
| 3. Regular Users (UID 1000+)                                             |
|    - Humans jo SSH login karte hain (e.g., sachin, developer).           |
|    - Sirf apne /home directory aur assigned resources ka access hota hai.|
+--------------------------------------------------------------------------+
```

> ⚠️ **Production Security Rule:** Kabhi bhi application (Spring Boot, Node.js, Python) ko **Root user** se run nahi karna chahiye! Agar app me koi vulnerability (RCE - Remote Code Execution) aayi, toh attacker poore server ka root access pa lega. Always use a dedicated non-root service user.

---

## 2. Core User Configuration Files

### 1. `/etc/passwd` (Public User Database)
Sabhi users ki list hoti hai. Yeh readable by all users hota hai:
```bash
cat /etc/passwd | grep sachin
# Output: sachin:x:1001:1001:Sachin Dev:/home/sachin:/bin/bash
```
Breakdown:
1. `sachin`: Username
2. `x`: Password marker (actual encrypted password `/etc/shadow` me hota hai)
3. `1001`: User ID (UID)
4. `1001`: Primary Group ID (GID)
5. `Sachin Dev`: Comment / Full Name
6. `/home/sachin`: Home Directory
7. `/bin/bash`: Default Login Shell

### 2. `/etc/shadow` (Encrypted Passwords) 🔐
Sirf root user hi ise read kar sakta hai (`-rw-r-----`). Isme password ke hashes aur expiration dates hote hain.
```bash
sudo head -n 2 /etc/shadow
# Format: username:$6$salt$hashed_password:last_changed:min:max:warn:inactive:expire
# $6$ = SHA-512 encryption algorithm
```

### 3. `/etc/group` (Group Database)
```bash
cat /etc/group | grep developers
# developers:x:1002:sachin,rahul,priya
```

---

## 3. User Management Commands

### 1. User Create Karna (`useradd`)
```bash
# Naya user create karna (-m = create home directory, -s = specify shell):
sudo useradd -m -s /bin/bash appuser

# Password set karna:
sudo passwd appuser
```

### 2. User Modify Karna (`usermod`)
```bash
# User ko naye secondary group (e.g., 'docker' ya 'sudo') me add karna:
# ⚠️ -aG ka dhyaan rakhein: -a = append, -G = secondary group.
# Agar bina -a ke sirf -G diya toh purane sabhi groups se user remove ho jayega!
sudo usermod -aG sudo sachin
sudo usermod -aG docker sachin

# User ki default shell badalna:
sudo usermod -s /bin/zsh sachin

# User account ko lock / unlock karna (Security Incident par):
sudo usermod -L baduser    # Lock account
sudo usermod -U baduser    # Unlock account
```

### 3. User Delete Karna (`userdel`)
```bash
sudo userdel appuser          # Sirf user account delete karega
sudo userdel -r appuser       # -r flag: Home folder aur mail spool sab clean karega!
```

---

## 4. Group Management Commands

```bash
sudo groupadd backend-team          # Group create karna
sudo groupdel backend-team          # Group delete karna
id sachin                           # Current user ka UID, GID aur sabhi groups check karna
groups sachin                       # Sirf user ke groups dikhana
```

---

## 5. `su` vs `su -` vs `sudo` vs `sudo -i` (Interview Trap! 🎯)

Yeh sawaal aksar Linux aur DevOps interviews me pucha jata hai:

| Command | Full Form / Meaning | Environment State | Best Practice |
| :--- | :--- | :--- | :--- |
| `sudo <command>` | SuperUser DO | Current user ke context me single command ko root privilege se chalata hai. Command khatam hote hi root power chali jati hai. | **Best & Safest.** Production standard. Audit log `/var/log/auth.log` me maintain rehta hai. |
| `su <user>` | Switch User | User change karta hai lekin **current user ke environment variables ($PATH, $HOME) retain rehte hain**. | Messy, conflicts create karta hai. |
| `su - <user>` | Switch User with Login Shell | Target user ke saare environment variables, `$PATH`, aur `.bashrc` load karke fresh login karta hai. | Use jab kisi doosre user ke environment me poora session chahiye. |
| `sudo -i` | Sudo Interactive Shell | Root user ka complete login shell open kar deta hai (`$HOME = /root`). Root password ki zaroorat nahi hoti, user ka apna sudo password lagta hai. | Administration tasks ke liye quick root access. |

---

## 6. `/etc/sudoers` & `visudo` Command 🛡️

Kaun sa user `sudo` run kar sakta hai aur kaun si commands execute kar sakta hai, yeh `/etc/sudoers` file define karti hai.

> 🚨 **GOLDEN RULE:** `/etc/sudoers` ko kabhi bhi `nano` ya `vim` se directly edit mat karna!
> Hamesha **`sudo visudo`** command use karein.
> `visudo` file save hone se pehle syntax checking karta hai. Agar aapse ek comma ya space ki galti hui, toh directly edit karne par system crash ho sakta hai aur aap root se permanently lockout ho sakte hain!

### Common Sudoers Rules:
Open with: `sudo visudo`

```sudoers
# 1. Standard Root privilege:
root    ALL=(ALL:ALL) ALL

# 2. 'sudo' group ke sabhi users ko full access:
%sudo   ALL=(ALL:ALL) ALL

# 3. Specific developer ko bina password ke restart command ki permission:
# (CI/CD Deployment pipelines me bohot use hota hai):
deployer ALL=(ALL) NOPASSWD: /usr/bin/systemctl restart myapp

# 4. Developer ko specific script run karne dena without full root:
john ALL=(ALL) NOPASSWD: /opt/scripts/backup.sh
```

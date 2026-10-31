# Module 03: File Permissions, Ownership & Special Bits 🔒

Linux me file security ka main foundation permissions aur ownership system hai. "Permission Denied" error backend developers aur DevOps engineers ki zindagi me sabse zyada aane wala error hai! Is chapter me hum permissions ko mathematical aur practical dono tareeqe se samjhenge.

---

## 1. Anatomy of File Permissions 📊

Jab aap `ls -l` command chalate hain, toh pehla column file permissions ko represent karta hai:

```
        -   r w x   r - x   r - -
        │   │───│   │───│   │───│
Type ───┘     │       │       │
              │       │       └── Others (World: koi bhi doosra user)
              │       └────────── Group (Jo log file ke group me hain)
              └────────────────── Owner / User (Jisne file banayi)
```

### File Type Symbols:
- `-` : Regular file (text file, binary, image)
- `d` : Directory (Folder)
- `l` : Symbolic Link (Shortcut)
- `c` : Character device file (Keyboard, terminal)
- `b` : Block device file (Hard disk, SSD)
- `s` : Unix Domain Socket (Inter-process communication)

---

## 2. File vs Directory Permissions (Big Difference! 💡)

Permissions ka matlab Files aur Directories ke liye thoda alag hota hai:

| Permission | File par iska matlab | Directory (Folder) par iska matlab |
| :---: | :--- | :--- |
| **`r` (Read)** | File ka content view kar sakte hain (`cat`, `less`). | Folder ke andar ki files ki list dekh sakte hain (`ls`). |
| **`w` (Write)** | File me data add, edit ya replace kar sakte hain. | Folder ke andar **nayi files create ya delete kar sakte hain**! |
| **`x` (Execute)** | File ko program/script ki tarah run kar sakte hain (`./app.sh`). | Folder ke andar enter/navigate kar sakte hain (`cd folder/`). |

> ⚠️ **Common Bug:** Agar kisi directory par execute (`x`) permission nahi hai, toh bhale hi aapke paas read (`r`) permission ho, aap us directory me `cd` nahi kar payenge aur files access nahi hongi!

---

## 3. Numeric (Octal) Permission Representation

Har permission ko ek numeric weight diya gaya hai:

```
Read (r)    = 4
Write (w)   = 2
Execute (x) = 1
None (-)    = 0
```

Teen digits ka total calculate hota hai: `[User][Group][Others]`

| Value | Permissions | Symbolic | Kahan use hota hai? |
| :---: | :---: | :---: | :--- |
| **7** | 4 + 2 + 1 | `rwx` | Read, Write & Execute |
| **6** | 4 + 2 + 0 | `rw-` | Read & Write (Standard files) |
| **5** | 4 + 0 + 1 | `r-x` | Read & Execute (Scripts/Directories) |
| **4** | 4 + 0 + 0 | `r--` | Read Only |
| **0** | 0 + 0 + 0 | `---` | No Access |

### Production Standard Combinations:
- `755` (`rwxr-xr-x`): Directories aur executable scripts ke liye. Owner ko full control, group aur others sirf read & execute kar sakte hain.
- `644` (`rw-r--r--`): Application code, config files, HTML/CSS. Owner edit karega, baki sab sirf read karenge.
- `600` (`rw-------`): **Secrets & Private Keys!** (e.g., SSH Private Key `~/.ssh/id_rsa`, `.env` files). Sirf owner read/write kar sakta hai.
- `400` (`r--------`): AWS EC2 `.pem` key file. Sirf owner read kar sakta hai.

---

## 4. `chmod` (Change Mode / Permissions)

### Method A: Numeric Mode (Octal)
```bash
chmod 755 deploy.sh           # Executable script banana
chmod 644 config.properties   # Standard file permissions
chmod 600 .env                # Secure secrets
chmod 400 my-key.pem          # AWS Key pair security

# Recursive (-R flag): Sabhi subfolders aur files par apply karna
chmod -R 755 /var/www/html
```

### Method B: Symbolic Mode (Human Readable)
- `u` = user/owner, `g` = group, `o` = others, `a` = all
- `+` = permission add karna, `-` = permission remove karna, `=` = exact set karna

```bash
chmod +x script.sh            # Sabhi ke liye execute add karo
chmod u+x script.sh           # Sirf owner ke liye execute add karo
chmod g-w report.txt          # Group se write permission cheen lo
chmod o=r data.txt            # Others ko sirf read-only bana do
chmod a-w sensitive.log       # Sabhi se write permission hata do
```

---

## 5. `chown` & `chgrp` (Change Ownership)

Har file ka ek Owner User aur ek Owner Group hota hai:

```bash
# 1. Sirf Owner change karna:
sudo chown sachin app.jar

# 2. Owner aur Group dono ek sath change karna (user:group):
sudo chown sachin:developers app.jar

# 3. Web server directory ka ownership web service user ko dena (Recursive):
sudo chown -R www-data:www-data /var/www/html

# 4. Sirf Group change karna (chgrp):
sudo chgrp devops /opt/app/
```

---

## 6. Special Permissions: SUID, SGID & Sticky Bit 🌟

Normal permissions ke aage ek 4th special digit hota hai:

### 1. SUID (Set User ID - Value: 4xxx)
Jab koi user yeh file execute karega, toh yeh program **file ke owner ke privileges ke sath chalega**, na ki us user ke sath jisne ise run kiya!
- Real Example: `/usr/bin/passwd` command.
  - Jab normal user apna password change karta hai, toh `/etc/shadow` file update hoti hai jo sirf root ki hoti hai.
  - SUID ki wajah se `passwd` program temporary root ban kar password save kar deta hai!
```bash
ls -l /usr/bin/passwd
# -rwsr-xr-x 1 root root (Notice the 's' in user place)
chmod 4755 /usr/local/bin/special_tool
```

### 2. SGID (Set Group ID - Value: 2xxx)
- **Files par:** File run hote waqt group ke privileges leti hai.
- **Directories par (DevOps Favorite):** Is directory ke andar jo bhi naya file ya folder banega, woh automatically **parent directory ke group ko inherit karega**, chahe kisi bhi user ne file banayi ho! Shared team directories me bohot use hota hai.
```bash
chmod 2775 /var/shared-project/
# drwxrwsr-x (Notice 's' in group place)
```

### 3. Sticky Bit (Value: 1xxx)
Agar kisi directory par full write permission ho (777), toh koi bhi user doosre user ki files delete kar sakta hai!
**Sticky Bit lagane ke baad, sirf file ka actual owner (ya root) hi us file ko delete ya rename kar sakta hai.**
- Real Example: `/tmp` directory!
```bash
ls -ld /tmp
# drwxrwxrwt 1 root root (Notice the 't' at the end!)
chmod 1777 /my-shared-uploads/
chmod +t /my-shared-uploads/
```

---

## 7. Understanding `umask` (Default Permissions) 🎭

Jab aap `touch newfile.txt` ya `mkdir newdir` karte hain, toh Linux uski permissions kaise decide karta hai? **`umask`** se!

- Linux me base permissions hoti hain:
  - Nayi Files ke liye: `666` (`rw-rw-rw-`)
  - Naye Folders ke liye: `777` (`rwxrwxrwx`)
- Default system `umask` aamtaur par hota hai: `022`

### The Math: `Base Permission - Umask = Final Permission`
- **File:** `666 - 022 = 644` (`rw-r--r--`)
- **Directory:** `777 - 022 = 755` (`rwxr-xr-x`)

Check current umask:
```bash
umask
# Output: 0022
```
Agar aap chahte hain ki aapki banayi files koi doosra user touch bhi na kar sake, toh umask ko `077` set kar sakte hain (`666 - 077 = 600`).

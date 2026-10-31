# Module 05: SSH (Secure Shell), Rsync & Port Forwarding 🔑

Enterprise backend infrastructure AWS, GCP, DigitalOcean ya On-Premise data centers me hoti hai. Servers ko physically touch kiye bina safely manage karne ka ek hi universal gateway hai: **SSH (Port 22)**.

---

## 1. How SSH Works: Public-Key Cryptography 🔐

SSH asymmetric encryption par kaam karta hai jisme 2 keys ka pair banta hai:

```
[ Your Local Laptop ]                                     [ Remote Cloud Server ]
         │                                                           │
   +-----+-----+                                                     │
   │ Key Pair  │                                                     │
   +-----+-----+                                                     │
   │                                                                 │
   ├── 1. Private Key (~/.ssh/id_ed25519) [Keep Secret!]             │
   │      (Kabhi kisi ko share nahi karna)                           │
   │                                                                 │
   └── 2. Public Key (~/.ssh/id_ed25519.pub) ──────────────────────> │
          (Har server par copy kar sakte hain)                       └── ~/.ssh/authorized_keys
```

Jab aap login karte hain:
1. Server public key se ek random encrypted challenge generate karta hai.
2. Aapka local machine private key se use decrypt karke signature wapas bhejta hai.
3. Server verify karta hai aur bina password maange **instant secure access** de deta hai!

---

## 2. Generating & Deploying Modern SSH Keys

Puraani `rsa` keys (2048-bit) ab purani ho chuki hain. Modern standard hai **`ed25519`** (Elliptic Curve Cryptography - super fast, highly secure, short keys).

### Step 1: Key Generate Karein (Local Laptop par)
```bash
ssh-keygen -t ed25519 -C "sachin@backend-laptop"
# Enter dabayein default location ke liye (~/.ssh/id_ed25519)
# Optional: Passphrase daal sakte hain extra security ke liye
```

### Step 2: Public Key Server par Copy Karein
```bash
# Method A: Automated command (Fastest)
ssh-copy-id -i ~/.ssh/id_ed25519.pub sachin@65.2.140.22

# Method B: Manual (Agar ssh-copy-id na ho)
cat ~/.ssh/id_ed25519.pub | ssh sachin@65.2.140.22 "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys"
```

### 🚨 Strict Permissions Requirement (Linux Security Check!)
Agar permissions galat hui, toh SSH server bina koi error bataye **Connection refused ya Password prompt** de dega!
Server par yeh permissions hamesha verify karein:
```bash
chmod 700 ~/.ssh
chmod 600 ~/.ssh/authorized_keys
```
Aur aapke local laptop par:
```bash
chmod 600 ~/.ssh/id_ed25519
```

---

## 3. SSH Config File (`~/.ssh/config`): Work Like a Pro 🧙‍♂️

Har baar itna bada command type karne ke bajaye:  
`ssh -i ~/.ssh/my-aws-key.pem -p 2222 ubuntu@ec2-65-1-2-3.compute.amazonaws.com`

Aap apne local machine me file banayein: `nano ~/.ssh/config`

```ssh-config
# Production Web Server
Host prod
    HostName 65.1.2.3
    User ubuntu
    Port 2222
    IdentityFile ~/.ssh/my-aws-key.pem

# Staging Database Server
Host stage-db
    HostName 10.0.1.50
    User devops
    IdentityFile ~/.ssh/id_ed25519
```

**Magic Result:** Ab aapko sirf yeh type karna hai:
```bash
ssh prod
```
Aapka terminal automatically right user, port, IP aur key pair ke sath server se connect ho jayega!

---

## 4. File Transfer: `scp` vs `rsync` 📦

### 1. `scp` (Secure Copy - Simple Single File)
```bash
# Local se Remote server par bhejna:
scp target/app.jar prod:/opt/deployments/

# Remote server se local par download karna:
scp prod:/var/log/nginx/error.log ~/Desktop/
```

### 2. `rsync` (Remote Sync - The DevOps Gold Standard) 🏆
`scp` har baar poori file dobara transfer karta hai. `rsync` **Delta Transfer Algorithm** use karta hai: sirf woh parts transfer karta hai jo modify hue hain!

```bash
rsync -avzP ./build/ prod:/var/www/my-app/
```
**Flags Decoded:**
- `-a` (Archive): Directories, file permissions, ownership, symlinks sab preserve karta hai.
- `-v` (Verbose): Screen par transferred files dikhata hai.
- `-z` (Compress): Network par data compress karke bhejta hai (bandwidth bachata hai).
- `-P` (Progress & Partial): Transfer progress bar dikhata hai aur internet cut hone par **resume** allow karta hai!

```bash
# Folder sync karte waqt remote se extra files delete karna:
rsync -avzP --delete ./dist/ prod:/var/www/html/
```

---

## 5. SSH Tunneling / Port Forwarding (Bastion Host Jump) 🚇

### Scenario: Private Database ko Local Laptop se Access Karna
Cloud (AWS) me aapka PostgreSQL/MySQL Database Private VPC ke andar hai jiska koi Public IP nahi hai. Bahar sirf ek Bastion/Jump Server hai jiska Public IP hai.

```
[ Laptop: DBeaver GUI ] ───> [ Bastion Server (Public) ] ───> [ Private RDS Postgres ]
  (localhost:5433)                    (Port 22)                    (10.0.1.20:5432)
```

### Local Port Forwarding (`-L` Flag):
```bash
ssh -L 5433:10.0.1.20:5432 ubuntu@bastion.mycompany.com -N
```
**Explanation:**
- Local laptop ke port `5433` par jo traffic aayega, woh SSH tunnel ke through Bastion server par jayega aur Bastion use `10.0.1.20:5432` par forward kar dega!
- Ab aap apne DBeaver/PgAdmin me connection host `localhost` aur port `5433` daal kar private DB inspect kar sakte hain!
- `-N` flag: Command run nahi karta, sirf tunnel open rakhta hai.

---

## 6. Production SSH Server Hardening Checklist 🛡️

Naye Linux server par login karne ke baad `/etc/ssh/sshd_config` me yeh changes zaroor karein:

```bash
sudo nano /etc/ssh/sshd_config
```

1. **Root Direct Login Band Karein:**
   ```sshd-config
   PermitRootLogin no
   ```
2. **Password Authentication Band Karein (Sirf SSH Keys allow karein):**
   ```sshd-config
   PasswordAuthentication no
   ```
3. **Brute Force Attacks se bachne ke liye Default Port 22 change karein (e.g. 2222 ya 49152):**
   ```sshd-config
   Port 2222
   ```

Restart SSH Service to apply changes:
```bash
sudo systemctl restart sshd
```
> ⚠️ **Pro Safety Tip:** Restart karne ke baad current terminal band mat karein! Naya terminal window open karke pehle test login karein taaki lock out hone ka chance na ho.

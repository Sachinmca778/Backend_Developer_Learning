# Module 08: Top 50 Linux Interview Questions & Production Disaster Recovery 🎯

Yeh comprehensive interview guide specifically Backend Developers, DevOps Engineers aur SREs ke liye design ki gayi hai. Har question ka crisp, technically solid answer Hinglish me diya gaya hai.

---

## 📑 Categorized Index

- **Section A:** Core Linux Architecture & Fundamentals (Q1 - Q10)
- **Section B:** File System, Links & Permissions (Q11 - Q20)
- **Section C:** Process, Memory & CPU Performance (Q21 - Q30)
- **Section D:** Networking, Ports & Remote Access (Q31 - Q40)
- **Section E:** Production Disaster Recovery Scenarios (Q41 - Q50)

---

## 🟢 Section A: Core Architecture & Fundamentals

### Q1: Linux Kernel aur Operating System me kya antar hai?
> **Answer:** Linux technically sirf ek **Kernel** hai (Hardware resource manager). Jab is kernel ke sath GNU core utilities (`ls`, `cat`, bash, gcc compilers) aur system libraries add kiye jaate hain, toh use poora **GNU/Linux Operating System** kaha jata hai.

### Q2: User Space aur Kernel Space me kya difference hai?
> **Answer:** Kernel Space memory ka privileged zone (**Ring 0**) hota hai jahan kernel code aur drivers chalte hain jinke paas direct hardware access hota hai. User Space unprivileged zone (**Ring 3**) hota hai jahan hamare applications (Java, Node, Nginx) chalte hain. Dono ke beech communication **System Calls** ke through hota hai.

### Q3: Monolithic Kernel vs Microkernel me kya farak hai?
> **Answer:** Linux ek **Monolithic Kernel** hai jahan sabhi core services (VFS, IPC, Network stack, Drivers) single kernel memory address space me super-fast run hoti hain. Microkernel me sirf basic scheduling kernel me rehta hai aur baaki drivers user space me hote hain.

### Q4: Linux me Boot Process ke steps kya hote hain?
> **Answer:**
> 1. **BIOS / UEFI:** Hardware POST (Power-On Self-Test) run karta hai aur bootloader dhundta hai.
> 2. **MBR / GPT:** Partition table read karta hai.
> 3. **GRUB2:** Linux kernel image (`vmlinuz`) aur `initramfs` ko RAM me load karta hai.
> 4. **Kernel:** Hardware initialize karta hai aur root filesystem mount karta hai.
> 5. **systemd (PID 1):** System services aur targets (multi-user.target) ko parallel me start karta hai.

### Q5: "Everything is a file" concept ka kya significance hai?
> **Answer:** Unix philosophy ke mutabik hardware devices (`/dev/sda`), network sockets, running processes (`/proc`), aur normal text files sabhi ko ek uniform API (System calls: `open()`, `read()`, `write()`, `close()`) se access kiya ja sakta hai.

### Q6: Debian based (Ubuntu) aur RHEL based (CentOS/Rocky) distros me main differences kya hain?
> **Answer:** Ubuntu me `.deb` packages aur `apt` package manager hota hai, jabki RHEL/Rocky me `.rpm` packages aur `dnf`/`yum` manager hota hai. Ubuntu cloud startups me zyada common hai, jabki RHEL banking aur legacy enterprise me standard hai.

### Q7: Bash Shell me Shebang `#!/bin/bash` ka kya role hai?
> **Answer:** Yeh script ki pehli line hoti hai jo kernel ke `execve()` system call ko batati hai ki is script text file ko interpret karne ke liye `/bin/bash` binary engine ka use karein.

### Q8: CLI me `source script.sh` vs `./script.sh` me kya farak hai?
> **Answer:** `./script.sh` ek **naya subshell (child process)** spawn karta hai; script khatam hone par uske variables gayab ho jaate hain. `source script.sh` (ya `. script.sh`) **current shell** me execute karta hai, jisse variables aur aliases current terminal me preserve rehte hain.

### Q9: `PATH` environment variable kya karta hai?
> **Answer:** Yeh colon (`:`) se separated directories ki list hoti hai jahan OS executable commands dhundta hai jab aap bina absolute path ke koi command (e.g. `java`, `git`) type karte hain.

### Q10: Swappiness parameter kya represent karta hai?
> **Answer:** `/proc/sys/vm/swappiness` (0 se 100) kernel ko batata hai ki kitni aggressively RAM ke data ko disk swap me move karna hai. Lower value (10) enterprise servers par preferred hoti hai latency prevent karne ke liye.

---

## 🟡 Section B: File System, Links & Permissions

### Q11: Inode (Index Node) kya hota hai?
> **Answer:** Inode filesystem ka metadata record hota hai jo file ka size, permissions, owner, timestamps aur data block pointers store karta hai. Inode file ka content aur file ka **NAAM** store nahi karta.

### Q12: Hard Link aur Soft Link (Symlink) me difference kya hai?
> **Answer:**
> - **Soft Link:** Naya inode banata hai jo original file ke path par point karta hai. Agar original file delete ho jaye, toh link broken/dangling ho jata hai.
> - **Hard Link:** Original file ka same inode share karta hai. Original file delete karne par bhi data disk par safe rehta hai jab tak link count 0 na ho.

### Q13: File par `chmod 755` aur `chmod 644` ka exact binary meaning kya hai?
> **Answer:** 4=Read, 2=Write, 1=Execute.
> - `755`: Owner = 7 (rwx), Group = 5 (r-x), Others = 5 (r-x).
> - `644`: Owner = 6 (rw-), Group = 4 (r--), Others = 4 (r--).

### Q14: Directory par Execute (`x`) permission ka kya role hota hai?
> **Answer:** Directory par `x` permission hone par hi user us directory ke andar `cd` kar sakta hai aur uske files ke metadata ko access kar sakta hai. Bina `x` ke sirf `ls` chal sakta hai lekin file open nahi ho sakti.

### Q15: Sticky Bit kya hota hai aur kahan use hota hai?
> **Answer:** Sticky Bit (numeric 1000, `t`) shared directories par lagaya jata hai (jaise `/tmp`). Isse koi bhi user sirf apni banayi file ko delete ya rename kar sakta hai, doosre user ki files delete nahi kar sakta.

### Q16: SUID aur SGID ka real world use case kya hai?
> **Answer:** SUID (`s` in user) file ko file owner ke privileges ke sath run karta hai (e.g. `/usr/bin/passwd`). SGID directory par lagane se naye created files parent folder ka group inherit karti hain.

### Q17: `umask` default file permissions kaise decide karta hai?
> **Answer:** Files ki base permission `666` aur folders ki `777` hoti hai. Default umask `022` subtract hone par: File banti hai `644` aur folder banta hai `755`.

### Q18: `/dev/null` aur `/dev/zero` me kya antar hai?
> **Answer:** `/dev/null` bit bucket (black hole) hai jo saara input discard karta hai. `/dev/zero` continuous null bytes (`\0`) generate karta hai jo dummy files ya swap files create karne me use hota hai.

### Q19: File me modification time (`mtime`), change time (`ctime`), aur access time (`atime`) me kya farak hai?
> **Answer:**
> - `mtime`: File ka actual CONTENT modify hua.
> - `ctime`: File ka METADATA (permissions, ownership) change hua.
> - `atime`: File ko read/access kiya gaya.

### Q20: `find` command se 100MB se badi sabhi `.log` files kaise dhundenge?
> **Answer:**
> `find /var/log -type f -name "*.log" -size +100M`

---

## 🟠 Section C: Process, Memory & CPU Performance

### Q21: Zombie Process aur Orphan Process me kya difference hai?
> **Answer:**
> - **Zombie:** Child process execute hokar exit ho chuki hai, lekin parent ne abhi tak `wait()` call nahi kiya. RAM nahi leti, sirf PID table slot leti hai.
> - **Orphan:** Parent process mar chuki hai, lekin child zinda chal rahi hai. PID 1 (`systemd`) orphan child ko adopt kar leta hai.

### Q22: `kill -9` aur `kill -15` me kya difference hai?
> **Answer:**
> - `kill -15` (`SIGTERM`): Graceful shutdown request. Application open connections, buffers aur threads clean karke band hoti hai.
> - `kill -9` (`SIGKILL`): Forceful immediate termination by kernel. App koi cleanup nahi kar sakti; data corruption ka risk hota hai.

### Q23: `top` output me `%wa` (I/O Wait) high hone ka kya matlab hai?
> **Answer:** CPU idle baith kar Hard Disk / Storage subsystem se data read/write hone ka wait kar raha hai. Problem CPU me nahi, slow disk ya unindexed database queries me hai.

### Q24: Load Average numbers (1, 5, 15 min) ko kaise read karte hain?
> **Answer:** Load average running + waiting processes ka count hota hai. Agar 4-core CPU par load 4.00 hai toh 100% saturation hai. Agar 8.00 hai toh processes queue me phas rahi hain.

### Q25: `free -m` me "Free" aur "Available" memory me kya farak hai?
> **Answer:** "Free" memory woh RAM hai jo bilkul unused hai. "Available" memory = Free RAM + Buffers/Cache jo nayi application start hone par Linux instantly reclaim karke de sakta hai. Real health ke liye hamesha **Available** check karein.

### Q26: Linux OOM (Out Of Memory) Killer kab trigger hota hai?
> **Answer:** Jab system ki RAM aur Swap dono exhaust ho jaate hain aur kernel nayi allocations nahi kar pata, toh system crash se bachne ke liye OOM killer highest `oom_score` wale memory-heavy process ko SIGKILL kar deta hai.

### Q27: `nohup` command background tasks me kyun zaroori hoti hai?
> **Answer:** SSH session close hone par terminal apne child processes ko `SIGHUP` signal bhejta hai jisse processes kill ho jaati hain. `nohup` (No Hang Up) is signal ko ignore kar deta hai jisse process chalti rehti hai.

### Q28: `ps aux` aur `ps -ef` flags ka meaning kya hai?
> **Answer:** `ps aux` (BSD style) user and memory/cpu % dikhata hai. `ps -ef` (POSIX style) standard format me Parent PID (PPID) dikhata hai jo process tree identify karne me kaam aata hai.

### Q29: Linux me `nice` aur `renice` values kya karti hain?
> **Answer:** Process ki CPU priority set karti hain (-20 highest priority se +19 lowest priority). High negative value dene se process ko CPU shares zyada milte hain.

### Q30: PID 1 ke roop me `systemd` ke advantages kya hain?
> **Answer:** SysVinit sequential boot karta tha. Systemd parallel dependency-aware service startup, unified logging (`journald`), aur auto-recovery provide karta hai.

---

## 🔵 Section D: Networking, Ports & Remote Access

### Q31: `ss -tulnp` aur `netstat` me kaun sa behtar hai aur kyun?
> **Answer:** `ss` behtar hai kyunki yeh Linux kernel ke netlink interface se direct socket information fetch karta hai, jabki puraana `netstat` `/proc` files parse karta tha jo heavy traffic servers par bohot slow ho jata tha.

### Q32: Port `0.0.0.0:8080` aur `127.0.0.1:8080` binding me kya farak hai?
> **Answer:** `127.0.0.1` sirf local machine se accessible hota hai (Private). `0.0.0.0` server ke sabhi available network interfaces (LAN + Public Internet) par listen karta hai.

### Q33: "Address already in use" error aane par port holder process kaise kill karenge?
> **Answer:**
> ```bash
> sudo kill -9 $(sudo lsof -t -i :8080)
> ```

### Q34: `curl -v` aur `curl -I` me kya difference hai?
> **Answer:** `curl -I` sirf HTTP HEAD headers fetch karta hai. `curl -v` (verbose) TLS handshake, request headers, IP address aur response body sab detail me dikhata hai.

### Q35: SSH me RSA vs Ed25519 keys me kaun si better hai?
> **Answer:** **Ed25519** better hai. Yeh Elliptic Curve Cryptography use karti hai, RSA 4096-bit se zyada secure hai, key size short hoti hai aur handshake super fast hota hai.

### Q36: SSH Key authentication fail hone par permissions kya honi chahiye?
> **Answer:**
> - `~/.ssh` directory: `700` (`drwx------`)
> - `~/.ssh/authorized_keys`: `600` (`-rw-------`)
> - `~/.ssh/id_ed25519` (Private key): `600` (`-rw-------`)

### Q37: SSH Local Port Forwarding (`-L`) ka real use-case kya hai?
> **Answer:** Cloud me private subnet ke database (jiska public IP nahi hai) ko local machine ke DBeaver/GUI tool se connect karna via Bastion host:
> `ssh -L 5433:private-db.internal:5432 user@bastion-ip`

### Q38: `scp` vs `rsync` me `rsync` kyun prefer kiya jata hai?
> **Answer:** `rsync` delta-transfer algorithm use karta hai (sirf modified chunks transfer karta hai), interrupted transfer resume karta hai (`-P`), aur file permissions preserve karta hai (`-a`).

### Q39: `/etc/resolv.conf` aur `/etc/hosts` me priority kiski hoti hai?
> **Answer:** `/etc/nsswitch.conf` file define karti hai, jisme standard rule `hosts: files dns` hota hai. Iska matlab **`/etc/hosts` file pehle check hoti hai**, aur agar match na mile tab DNS servers (`/etc/resolv.conf`) query hote hain.

### Q40: UFW firewall me specific IP ko MySQL access kaise dete hain?
> **Answer:**
> `sudo ufw allow from 192.168.1.50 to any port 3306 proto tcp`

---

## 🔴 Section E: Production Disaster Recovery Scenarios

### Q41: SCENARIO: Disk 100% Full alert aaya, aapne 10GB ki log file `rm` kar di, lekin `df -h` me abhi bhi 100% full dikh raha hai! Problem kya hai?
> **Answer & Fix:**
> - **Cause:** Kisi running process (e.g. Java ya Nginx) ke paas abhi bhi us deleted file ka **File Descriptor open** hai! Linux me jab tak open FD close nahi hota, disk data blocks free nahi hote.
> - **Detect:**
>   ```bash
>   sudo lsof +L1 | grep deleted
>   ```
> - **Fix:** Culprit process ko gracefully reload ya restart karein (`sudo systemctl reload nginx`), FD release hote hi disk space instantly reclaim ho jayegi!

### Q42: SCENARIO: Server par "No space left on device" error aa raha hai, lekin `df -h` dikhata hai ki 40GB free hai. Kaise solve karoge?
> **Answer & Fix:**
> - **Cause:** Disk space bachi hai lekin **Inodes exhaust** ho chuke hain! (Millions of small files jaise session files ya cache files create ho gayi hain).
> - **Verify:** `df -i` run karein (`IUse%` 100% dikhega).
> - **Fix:** Target folder dhund kar delete karein:
>   ```bash
>   find /tmp/sessions -type f -delete
>   ```

### Q43: SCENARIO: Production server par CPU 100% pahunch gaya aur server unresponsive ho raha hai. Aapka step-by-step triage kya hoga?
> **Answer:**
> 1. `uptime` check karke Load average trend dekhein.
> 2. `top` run karke `P` dabayein (Identify highest CPU consumer PID).
> 3. `%us` vs `%sy` vs `%wa` check karein (App issue vs Kernel issue vs Disk bottleneck).
> 4. Agar Java application hai: Kill karne se pehle **Thread Dump** lein:
>    `jstack <PID> > /tmp/threaddump.txt`
> 5. Application logs live inspect karein: `tail -n 100 -f /var/log/app.log`.
> 6. Service restart karein: `sudo systemctl restart myapp`.

### Q44: SCENARIO: Java application crash ho rahi hai aur log me "java.io.IOException: Too many open files" error aa raha hai. Fix kya hai?
> **Answer & Fix:**
> - **Cause:** Process ne OS-level file descriptor (FD) limit reach kar li hai (har open socket connection bhi ek file descriptor hota hai).
> - **Check current limit:** `ulimit -n`
> - **Fix:** `/etc/security/limits.conf` me limits badhayein:
>   ```
>   *    soft    nofile    65535
>   *    hard    nofile    65535
>   ```
> - Systemd service me `LimitNOFILE=65535` add karein.

### Q45: SCENARIO: Cron job script terminal se manually chalane par perfectly execute hoti hai, lekin cron scheduler se silently fail ho jati hai. Kyun?
> **Answer:**
> - **Cause:** Cron minimal `$PATH` ke sath chalta hai aur user ke environment variables load nahi karta.
> - **Fix:**
>   1. Script ke andar sabhi commands ka full absolute path dein (`/usr/bin/curl`, `/usr/bin/python3`).
>   2. Cron entry me STDOUT/STDERR redirect karein taaki error dikhe:  
>      `* * * * * /path/to/script.sh >> /var/log/cron.log 2>&1`

### Q46: SCENARIO: Nginx access log se top 5 error producing URLs nikalne hain. Command kya hogi?
> **Answer:**
> ```bash
> awk '$9 >= 400 {print $7}' /var/log/nginx/access.log | sort | uniq -c | sort -nr | head -n 5
> ```

### Q47: SCENARIO: Server SSH connection timeout de raha hai, lekin ping aa raha hai. Kaise troubleshoot karoge?
> **Answer:**
> 1. Ping ICMP par chalta hai aur SSH TCP Port 22 par. Ping ka matlab physical network zinda hai.
> 2. `nc -zv <server_ip> 22` se check karein ki Port 22 open hai ya firewall ne block kiya hai.
> 3. Cloud security group / AWS NACL me Port 22 check karein.
> 4. Agar SSH daemon crash ho gaya hai, toh cloud web console se login karke `sudo systemctl status sshd` check karein.

### Q48: SCENARIO: Docker container ke andar se host machine ke database ko connect karna hai. IP kya use karenge?
> **Answer:**
> `localhost` use nahi kar sakte kyunki container ka apna isolated network namespace hota hai.
> - Linux Docker me: `172.17.0.1` (Docker default gateway IP) ya container run karte waqt `--add-host=host.docker.internal:host-gateway`.

### Q49: SCENARIO: Ek script likhni hai jo Nginx process down hone par automatically restart kare.
> **Answer:**
> ```bash
> #!/bin/bash
> if ! systemctl is-active --quiet nginx; then
>     echo "[$(date)] Nginx down! Restarting..." >> /var/log/nginx_watchdog.log
>     sudo systemctl start nginx
> fi
> ```

### Q50: SCENARIO: Production Nginx me configuration change ki hai, bina active users ka connection tode kaise apply karenge?
> **Answer:**
> Pehle config syntax verify karein: `sudo nginx -t`.  
> Agar successful aaye, toh restart nahi, **`sudo systemctl reload nginx`** (ya `nginx -s reload`) karein. Isse master process purane worker processes ko current connections finish karne deti hai aur naye workers nayi config ke sath start karti hai (Zero Downtime).

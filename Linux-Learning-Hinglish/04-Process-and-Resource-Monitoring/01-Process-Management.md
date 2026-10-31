# Module 04: Process Management, Jobs & Signals ⚙️

Backend applications jaise Spring Boot, Node.js, Python, Nginx ya PostgreSQL jab server par run hoti hain, toh Linux unhe **Processes** ke roop me manage karta hai. Jab koi app crash hoti hai ya CPU 100% khati hai, toh process management commands hi jaan bachati hain.

---

## 1. Program vs Process & Process Tree 🌳

- **Program:** Hard disk ya SSD par rakha hua passive code ya binary (e.g., `/usr/bin/java`, `app.jar`).
- **Process:** Jab program RAM me load hokar CPU dwara execute ho raha hota hai, use **Active Process** kehte hain.

Har process ko Linux Kernel 2 important IDs assign karta hai:
1. **PID (Process ID):** Har process ka unique number.
2. **PPID (Parent Process ID):** Jis process ne ise paida (fork) kiya.

### The Ancestor: PID 1 (systemd / init)
Jab computer boot hota hai, Linux Kernel sabse pehle **`systemd` (PID 1)** ko start karta hai. Baaki system ke saare services, daemons aur background processes PID 1 ke bachhe (children) hote hain!

```
                         [ Linux Kernel ]
                                │
                                ▼
                         [ systemd (PID 1) ]
                                │
        ┌───────────────────────┼───────────────────────┐
        ▼                       ▼                       ▼
    sshd (PID 540)        nginx (PID 820)       systemd-journald (PID 400)
        │                       │
        ▼                       ▼
  bash (PID 1205)         worker (PID 821)
        │
        ▼
java -jar app.jar (PID 2490)
```

---

## 2. Process States (Lifecycle) 🔄

Process apne pure life me in states se guzarti hai:

| State Code | Name | Meaning |
| :---: | :--- | :--- |
| **`R`** | Running / Runnable | CPU par actively execute ho rahi hai ya CPU run queue me wait kar rahi hai. |
| **`S`** | Interruptible Sleep | Kisi event, user input ya network socket I/O ka wait kar rahi hai (Most processes isme hoti hain). |
| **`D`** | Uninterruptible Sleep | Hardware/Disk I/O ka wait kar rahi hai. Is process ko `kill -9` se bhi kill nahi kiya ja sakta jab tak I/O complete na ho! |
| **`T`** | Stopped / Suspended | User ne `Ctrl+Z` daba kar pause kar diya hai. |
| **`Z`** | **Zombie (Defunct)** | Process apna kaam khatam karke mar chuki hai, lekin uske Parent ne abhi tak `wait()` system call karke uska exit status read nahi kiya! RAM/CPU use nahi karti, sirf PID table slot block karti hai. |

### Zombie vs Orphan Process (Interview Favorite! 🎯)
- **Zombie Process:** Child process mar chuki hai, lekin parent ne reap nahi kiya. Solution: Parent process ko restart/kill karo, taaki zombie PID 1 ko adopt ho aur PID 1 use clean kare.
- **Orphan Process:** Parent process mar gayi jabki Child abhi bhi zinda chal rahi hai! Linux me orphan hote hi **PID 1 (systemd)** immediately us child process ka parent ban jata hai aur use properly nurture karta hai.

---

## 3. Viewing & Searching Processes

### `ps aux` vs `ps -ef`
- `ps aux` (BSD style):
  - `a`: Sabhi users ke processes
  - `u`: Process ka owner/user aur memory/CPU % dikhao
  - `x`: Background processes jo kisi terminal se attach nahi hain
- `ps -ef` (System V style): PPID column dekhne ke liye best.

```bash
ps aux | grep java
# sachin  2490  12.4  8.5  3548900 142000 ?  Sl  14:20  1:15 java -jar app.jar
```

### Process Tree: `pstree`
Hierarchy visually dekhne ke liye:
```bash
pstree -p
# systemd(1)───sshd(540)───sshd(1200)───bash(1205)───java(2490)
```

### Quick Process Search: `pgrep`
Process ID direct nikalne ke liye:
```bash
pgrep -l nginx            # Name aur PID print karega
pgrep -f "spring-boot"    # Full command line arguments match karega
```

---

## 4. Foreground, Background Jobs & `nohup` 🏃‍♂️

Agar aap SSH terminal me `java -jar app.jar` run karte hain, toh terminal lock ho jata hai aur jaise hi aap SSH window band karte hain, application band ho jati hai! Is problem ko solve karne ke 3 tareeqe:

### 1. Job Control: `&`, `Ctrl+Z`, `jobs`, `bg`, `fg`
```bash
# 1. Command ko direct background me start karna:
java -jar app.jar &
# Output: [1] 3421 (Job ID: 1, PID: 3421)

# 2. Agar koi command foreground me chal rahi hai:
# Press 'Ctrl + Z' (Pause ho jayegi)
# [1]+  Stopped    java -jar app.jar

# 3. Use background me resume karne ke liye:
bg %1

# 4. Current running background jobs dekhne ke liye:
jobs -l

# 5. Wapas screen ke samne (foreground) lane ke liye:
fg %1
```

### 2. `nohup` (No Hang Up) 🛡️
Jab aap SSH disconnect karte hain, toh OS sabhi child processes ko **`SIGHUP` (Hangup Signal)** bhej kar terminate kar deta hai. `nohup` signal ko block kar deta hai taaki SSH close karne par bhi process chalti rahe!

```bash
nohup java -jar backend.jar > app.log 2>&1 &
# Output: Process detached! Aap SSH band kar sakte hain, backend chalta rahega.
```

---

## 5. Linux Signals & Terminating Processes 🛑

Processes aapas me communicate karne ke liye Signals ka use karti hain.

| Signal Number | Signal Name | Description | Catchable by App? |
| :---: | :--- | :--- | :---: |
| **`15`** | **`SIGTERM`** | **Graceful Shutdown (Polite request).** App ko resources clean karne, open database connections close karne aur buffers flush karne ka mauka milta hai. | ✅ Haan |
| **`9`** | **`SIGKILL`** | **Force Kill (Death sentence).** Kernel process ko immediately RAM se kick kar deta hai. App koi cleanup nahi kar sakti. Data corruption ka risk hota hai. | ❌ Nahi (Kernel direct kills) |
| **`1`** | **`SIGHUP`** | **Hangup / Reload.** Nginx ya config reload karne ke liye (Zero downtime restart). | ✅ Haan |
| **`2`** | **`SIGINT`** | **Interrupt.** Keyboard par `Ctrl + C` dabane par yahi signal jata hai. | ✅ Haan |
| **`19`** | **`SIGSTOP`** | **Pause.** `Ctrl + Z` dabane par process freeze ho jati hai. | ❌ Nahi |

### How to Kill Processes:
```bash
# 1. Graceful kill (Pehle hamesha yahi try karein):
kill -15 2490
# ya simply:
kill 2490       # Default signal 15 hi hota hai

# 2. Force kill (Jab application hang ho jaye aur 15 se na mare):
kill -9 2490

# 3. Process Name se kill karna:
killall nginx
pkill -f "my-spring-app"

# 4. Graceful Reload (Zero-Downtime Config Reload for Nginx):
kill -HUP $(pgrep -f "nginx: master")
```

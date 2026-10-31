# Module 02: Text Processing, Pipes & Redirection (DevOps Power Tools) ⚡

Backend systems me 90% information plain text formats me hoti hai (Logs, CSVs, JSONs, Configs). Linux text processing tools (`grep`, `awk`, `sed`, `sort`, `uniq`) sikh kar aap GBs ke logs ko seconds me parse kar sakte hain bina kisi Python script likhe!

---

## 1. Standard Streams: STDIN, STDOUT & STDERR 🌊

Har Linux process ke shuru hone par OS uske liye 3 standard input/output channels (File Descriptors) open karta hai:

```
                  +---------------------------+
  [ Keyboard ] ─> | 0: STDIN (Standard Input) | ─> [ Linux Process ]
                  +---------------------------+             │
                                                            ├──> [ 1: STDOUT ] ─> [ Screen / Terminal ]
                                                            │
                                                            └──> [ 2: STDERR ] ─> [ Screen (Error Log) ]
```

| Stream Name | File Descriptor (FD) | Default Source / Destination | Purpose |
| :--- | :--- | :--- | :--- |
| **STDIN** | `0` | Keyboard | Process me input bhejna |
| **STDOUT** | `1` | Terminal Screen | Normal output print karna |
| **STDERR** | `2` | Terminal Screen | Error messages print karna |

---

## 2. Redirection Operators (`>`, `>>`, `2>`, `&>`)

Output ko screen par dikhane ke bajaye kisi file me store karna ya ignore karna:

```bash
# 1. Overwrite STDOUT (Purana content clear karke naya likhega)
echo "Server Online" > status.txt

# 2. Append STDOUT (Purane content ke neeche naya append karega)
echo "New Log Entry" >> status.txt

# 3. Only Redirect STDERR (Sirf errors ko file me save karna)
find / -name "*.conf" 2> errors.log

# 4. Discard errors (Black hole me bhej do)
find / -name "*.conf" 2> /dev/null

# 5. STDOUT aur STDERR dono ko alag files me bhejna:
java -jar app.jar > output.log 2> error.log

# 6. STDOUT aur STDERR dono ko SAME file me merge karke save karna:
# (Method 1 - Classic)
java -jar app.jar > app.log 2>&1

# (Method 2 - Modern Bash)
java -jar app.jar &> app.log

# 7. Input Redirection (<): File se input lena
mysql -u root -p mydatabase < backup.sql
```

---

## 3. The Pipe (`|`) Operator 🔗

Pipe ka matlab hota hai: **Pehle command ka STDOUT agle command ka STDIN ban jata hai!**

```
[ Command 1: cat app.log ] 
           │ (STDOUT)
           ▼
     [ Pipe | ]
           │ (STDIN)
           ▼
[ Command 2: grep "ERROR" ]
           │ (STDOUT)
           ▼
     [ Pipe | ]
           │ (STDIN)
           ▼
[ Command 3: wc -l ] ──> Output: Total error count!
```

---

## 4. `grep` (Global Regular Expression Print) 🔍

Log files me se specific patterns search karne ke liye `grep` sabse tez tool hai.

### Important Flags:
```bash
grep "NullPointerException" app.log      # Exact case match
grep -i "error" app.log                  # -i: Case-insensitive (Error, ERROR, error sab match)
grep -v "DEBUG" app.log                  # -v: Invert match (DEBUG lines ko chhod kar baki sab dikhao)
grep -c "Exception" app.log              # -c: Count matches (Total kitni baar aaya)
grep -n "OutOfMemory" app.log            # -n: Line number ke saath dikhao
grep -r "TODO" ./src/                    # -r: Subdirectories me recursively search karo
grep -E "(404|500|502)" access.log       # -E: Extended Regex (OR condition)
```

### Context Flags (Production Favorite) 🎯
Exception ke aage aur peeche ki lines dekhna (Stack Trace inspect karne ke liye):
```bash
grep -B 3 -A 10 "NullPointerException" app.log
# -B 3  : Match se pehle ki 3 lines (Before)
# -A 10 : Match ke baad ki 10 lines (After)
# -C 5  : 5 lines before aur 5 lines after (Context)
```

---

## 5. `awk` (The Column Extraction & Pattern Engine) 🧮

`awk` table-like structured data (space, tab ya comma se separated files) ko process karne ka king hai. Default delimiter: **Space ya Tab**.

### Essential Variables in `awk`:
- `$0` = Poori current line
- `$1` = First column
- `$2` = Second column
- `$NF` = Last column (Number of Fields)
- `NR` = Current line number (Number of Records)

```bash
# 1. Sirf specific columns print karna
# Format: IP - - [Date] "GET /url" Status Size
awk '{print $1, $7, $9}' access.log
# Output: 192.168.1.10 /api/v1/users 200

# 2. Delimiter change karna (-F flag)
# /etc/passwd file me colon ':' separator hota hai
awk -F':' '{print $1, $6}' /etc/passwd
# Output: sachin /home/sachin

# 3. Filtering Conditions (Logic)
# Sirf woh requests dikhao jinka HTTP status code >= 500 hai:
awk '$9 >= 500 {print $1, $7, $9}' access.log

# 4. Total Calculation (Sum of values)
# Nginx log se total bandwidth (column 10 = bytes) calculate karna:
awk '{sum += $10} END {print "Total MB: ", sum/(1024*1024)}' access.log
```

---

## 6. `sed` (Stream Editor - Search & Replace) ✏️

`sed` file ko open kiye bina terminal se hi text search and replace karne ke kaam aata hai.

### Syntax: `sed 's/search_term/replace_term/flags'`

```bash
# 1. Output me replace karke terminal par print karna (Original file modify nahi hogi):
sed 's/localhost/192.168.1.50/g' config.env
# 's' = substitute, 'g' = global (line ke sabhi occurrences)

# 2. In-Place Edit (-i flag): Original file ko modify karna!
sed -i 's/DEBUG/INFO/g' application.properties

# 3. Specific lines delete karna:
sed -i '5d' myfile.txt          # Line 5 delete
sed -i '/^#/d' config.conf      # Comments (# se start hone wali lines) delete karo
sed -i '/^$/d' config.conf      # Empty blank lines delete karo
```

---

## 7. Supporting Data Wrangling Tools

### `sort` & `uniq`
```bash
sort names.txt                 # Alphabetical sort
sort -n numbers.txt            # Numeric sort (1, 2, 10, 100)
sort -r numbers.txt            # Reverse sort
sort -u names.txt              # Sort + Remove duplicates

# ⚠️ VERY IMPORTANT FOR UNIQ:
# 'uniq' sirf ADJACENT (lagataar) duplicate lines ko hatata hai.
# Isliye pehle 'sort' karna zaroori hai!
cat ips.txt | sort | uniq -c
# -c flag: Har unique item kitni baar repeat hua uska count print karta hai.
```

### `wc` (Word Count)
```bash
wc -l access.log               # Total lines count
wc -w doc.txt                  # Total words count
wc -c doc.txt                  # Total bytes count
```

### `cut` (Slice by Delimiter)
```bash
# Comma separated file se column 1 aur 3 nikalna:
cut -d',' -f1,3 data.csv
```

### `tee` (Terminal + File dono me ek sath likhna) 🔀
Normal redirection (`>`) screen par output nahi dikhata. Agar aap screen par bhi live dekhna chahte hain aur file me bhi record karna chahte hain:
```bash
./deploy.sh | tee deployment.log
./deploy.sh | tee -a deployment.log  # -a for append
```

### `xargs` (Piped output ko command arguments me convert karna)
```bash
# Sabhi .tmp files dhund kar ek baar me rm ko pass karna:
find . -name "*.tmp" | xargs rm -f
```

---

## 8. Real-World Production One-Liners 🚀

### Scenario 1: Nginx Access Log se Top 10 Most Active IP Addresses nikalna
```bash
awk '{print $1}' /var/log/nginx/access.log | sort | uniq -c | sort -nr | head -n 10
```
**Step-by-step Explanation:**
1. `awk '{print $1}'`: Access log se pehla column (IP address) nikala.
2. `sort`: Sabhi IPs ko order me arrange kiya (taaki duplicates paas aa jayein).
3. `uniq -c`: Duplicates count kiye (e.g., `452 192.168.1.15`).
4. `sort -nr`: Numeric descending order me sort kiya (highest requests upar).
5. `head -n 10`: Top 10 heavy IPs print kar diye! (DDoS identify karne ke liye standard trick).

### Scenario 2: Spring Boot Log me se sabhi 500/502 Error requests extract karna
```bash
grep -E "HTTP/1\.[01]\" (500|502)" access.log | awk '{print $4, $7, $9}' | head -n 20
```

### Scenario 3: Port 8080 par chalne wale process ko dhund kar kill karna
```bash
lsof -ti :8080 | xargs kill -9
```

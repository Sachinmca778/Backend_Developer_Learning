# Shell Scripting Complete Learning Notes

# What is Shell Scripting?

Shell Scripting means writing commands in a file so the operating system can execute them automatically.

Instead of typing commands one by one in terminal, you write them in a script file.

Mostly used in Linux/Unix systems.

---

# What is Shell?

Shell is a command interpreter between user and OS.

Examples:

* bash
* sh
* zsh
* ksh

Most common:

* Bash (GNU Bourne Again Shell)

---

# Why Use Shell Scripting?

Shell scripting is used for:

* Automation
* Server management
* Deployment
* Backup
* Monitoring
* Cron jobs
* File handling
* DevOps tasks

Example:

* Daily backup automatically
* Restart server automatically
* Process CSV files
* Monitor memory usage

---

# First Shell Script

Create file:

```bash
nano hello.sh
```

Write:

```bash
#!/bin/bash

echo "Hello World"
```

Run:

```bash
chmod +x hello.sh
./hello.sh
```

---

# Understanding This

## Shebang

```bash
#!/bin/bash
```

Tells system:
Use Bash shell to execute this script.

---

# Variables

```bash
#!/bin/bash

name="Sachin"

echo $name
```

Output:

```bash
Sachin
```

---

# User Input

```bash
#!/bin/bash

echo "Enter name:"
read name

echo "Welcome $name"
```

---

# Conditions (if else)

```bash
#!/bin/bash

num=10

if [ $num -gt 5 ]
then
    echo "Greater"
else
    echo "Smaller"
fi
```

---

# Important Comparison Operators

| Operator | Meaning       |
| -------- | ------------- |
| -eq      | equal         |
| -ne      | not equal     |
| -gt      | greater than  |
| -lt      | less than     |
| -ge      | greater/equal |
| -le      | less/equal    |

---

# Loops

## For Loop

```bash
#!/bin/bash

for i in 1 2 3 4 5
 do
    echo $i
 done
```

---

## While Loop

```bash
#!/bin/bash

count=1

while [ $count -le 5 ]
 do
    echo $count
    count=$((count+1))
 done
```

---

# Functions

```bash
#!/bin/bash

function greet() {
    echo "Hello $1"
}

greet Sachin
```

---

# Arguments

```bash
#!/bin/bash

echo $1
echo $2
```

Run:

```bash
./test.sh hello world
```

Output:

```bash
hello
world
```

---

# Important Special Variables

| Variable | Meaning             |
| -------- | ------------------- |
| $0       | script name         |
| $1 $2    | arguments           |
| $#       | total arguments     |
| $?       | last command status |
| $$       | process ID          |

---

# File Operations

## Check file exists

```bash
if [ -f file.txt ]
then
    echo "File exists"
fi
```

---

# Common Commands Used

| Command | Purpose           |
| ------- | ----------------- |
| grep    | search text       |
| awk     | text processing   |
| sed     | replace/edit text |
| cut     | split columns     |
| sort    | sorting           |
| uniq    | remove duplicates |
| find    | search files      |
| curl    | API requests      |
| scp     | copy files        |
| tar     | archive           |

---

# Example: Backup Script

```bash
#!/bin/bash

SOURCE="/home/data"
DEST="/backup"

tar -czf backup.tar.gz $SOURCE

mv backup.tar.gz $DEST

echo "Backup completed"
```

---

# Example: Memory Monitoring

```bash
#!/bin/bash

free -m
```

---

# Example: Disk Space Alert

```bash
#!/bin/bash

usage=$(df / | tail -1 | awk '{print $5}' | sed 's/%//')

if [ $usage -gt 80 ]
then
    echo "Disk usage high"
fi
```

---

# Cron Job + Shell Script

Automate scripts using cron.

Open cron:

```bash
crontab -e
```

Example:

```bash
0 2 * * * /home/backup.sh
```

Meaning:
Run daily at 2 AM.

---

# Real Life DevOps Usage

Shell scripting is heavily used with:

* AWS
* Docker
* Kubernetes
* Jenkins
* Linux servers
* CI/CD pipelines

---

# Important Interview Questions

## Difference between sh and bash

* sh = older shell
* bash = advanced shell with more features

---

## Difference between = and ==

```bash
a=5
```

Assignment.

```bash
[ "$a" == "5" ]
```

Comparison.

---

## What is chmod +x?

Gives execute permission to script.

---

# Best Practices

* Use meaningful variable names
* Add comments
* Handle errors
* Use functions
* Avoid hardcoded paths
* Always test scripts

---

# Learning Roadmap

## Beginner

* Commands
* Variables
* Conditions
* Loops

## Intermediate

* awk
* sed
* grep
* cron jobs
* log parsing

## Advanced

* Server automation
* Deployment scripts
* Monitoring scripts
* CI/CD automation
* DevOps scripting

---

# Most Important Commands to Practice

```bash
ls
cd
pwd
grep
awk
sed
find
cut
sort
uniq
xargs
curl
scp
ssh
tar
```

---

# Practice Project Ideas

1. Automatic backup system
2. Log analyzer
3. Disk monitoring tool
4. CSV processor
5. Deployment script
6. Health check script
7. Server monitoring system
8. S3 upload automation with shell script

---

# Quick Revision Cheatsheet

## Create Script

```bash
nano test.sh
```

## Give Permission

```bash
chmod +x test.sh
```

## Run Script

```bash
./test.sh
```

## Run with Bash

```bash
bash test.sh
```

---

# Real World Example: API Calling Script

```bash
#!/bin/bash

response=$(curl -X GET https://jsonplaceholder.typicode.com/posts/1)

echo $response
```

---

# Real World Example: Loop Through Files

```bash
#!/bin/bash

for file in *.txt
 do
    echo $file
 done
```

---

# Real World Example: Read CSV

```bash
#!/bin/bash

while IFS="," read id name email
 do
    echo "$id - $name - $email"
 done < users.csv
```

---

# Final Advice

Practice daily on Linux terminal.

Most backend developers, DevOps engineers, cloud engineers, and system admins use shell scripting heavily.

If you master:

* grep
* awk
* sed
* cron
* loops
* conditions
* file handling

Then automation becomes very powerful.

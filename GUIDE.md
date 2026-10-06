# 📖 HuntLog — Beginner & User Guide

Welcome to **HuntLog**! This guide is designed to get you up and running in minutes, explain how to use every feature end-to-end, and clarify important details about how your data is stored and managed.

> 💡 **Looking for technical architecture, database schemas, or state machine internals?** Check out [ARCHITECTURE.md](file:///c:/projects/Huntlog/ARCHITECTURE.md).

---

## 📌 1. Important Things You Should Know

Before running the app, here are a few key concepts:

### Where is the Data Stored?
- HuntLog uses a **MySQL 8.0** database.
- When running via Docker, data is saved inside a persistent Docker volume named **`mysql_data`**.
- This means your data **survives container restarts** — closing or stopping Docker will NOT delete your accounts or job applications.

### Why Port 3307 instead of 3306?
- Many computers (especially Windows) already have local MySQL or dev tools using port `3306`.
- We map Docker MySQL to port **`3307`** on your host machine to prevent port collisions, while inside Docker the container communicates normally on `3306`.

### How Authentication Works:
- When you register or login, the API returns a signed **JWT token** (a long text string).
- For all job application actions, you must send this token in the request header:
  ```http
  Authorization: Bearer <your-token-here>
  ```
- Your applications are completely private. Another user cannot see, edit, or delete your applications.

---

## 🧰 2. Prerequisites

Make sure you have these tools installed on your computer:

1. **Java JDK 21+** (`java -version`)
2. **Maven 3.9+** (`mvn -version`)
3. **Docker Desktop** running in the background (`docker --version`)

---

## 🚀 3. How to Run HuntLog

### Method A: Full Stack with Docker Compose (Fastest & Recommended)

This starts both the MySQL database and the Spring Boot application inside Docker.

```powershell
# Step 1: Build the latest JAR file
mvn clean package -DskipTests

# Step 2: Start both MySQL and API containers
docker compose up --build -d

# Step 3: Check that containers are running
docker compose ps
```

The API will be live at: **`http://localhost:8080`**

To stop the containers when you're done:
```powershell
docker compose down
```

---

### Method B: Local Development Mode

If you are modifying code and want instant reloading:

```powershell
# 1. Start only the MySQL database in Docker
docker compose up mysql -d

# 2. Run the Spring Boot app directly on your machine
mvn spring-boot:run
```

---

## 💡 4. End-to-End Walkthrough (Step-by-Step)

Open PowerShell and follow these steps to see the entire app in action:

---

### Step 1: Register a New User Account

Create your account with your name, email, and a password (minimum 8 characters):

```powershell
'{"name":"Alice Dev","email":"alice@test.com","password":"password123"}' | `
  curl.exe -s -X POST http://localhost:8080/api/v1/auth/register `
  -H "Content-Type: application/json" -d "@-"
```

**What you receive back (`201 Created`)**:
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhbGljZUB0ZXN0LmNvbSIsInVzZXJJZCI6MSwibmFtZSI6IkFsaWNlIERldiIsImlhdCI6MTc5MTExNDc0NCwiZXhwIjoxNzkxMjAxMTQ0fQ...",
  "email": "alice@test.com",
  "name": "Alice Dev"
}
```

---

### Step 2: Login and Save Your Token in a Variable

Login to get your JWT access token and store it in PowerShell variable `$token`:

```powershell
$auth = '{"email":"alice@test.com","password":"password123"}' | `
  curl.exe -s -X POST http://localhost:8080/api/v1/auth/login `
  -H "Content-Type: application/json" -d "@-" | ConvertFrom-Json

$token = $auth.token
Write-Host "Logged in successfully! Token starts with: $($token.Substring(0, 20))..."
```

---

### Step 3: Create a Job Application

Add an application you just submitted:

```powershell
'{"company":"Google","role":"Staff Software Engineer","jobUrl":"https://careers.google.com/123","location":"Mountain View, CA","notes":"Referred by Alex"}' | `
  curl.exe -s -X POST http://localhost:8080/api/v1/applications `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer $token" `
  -d "@-"
```

**Response (`201 Created`)**:
```json
{
  "id": 1,
  "company": "Google",
  "role": "Staff Software Engineer",
  "status": "APPLIED",
  "appliedDate": "2026-10-04",
  "lastUpdated": "2026-10-04T11:53:43.489361",
  "jobUrl": "https://careers.google.com/123",
  "notes": "Referred by Alex",
  "location": "Mountain View, CA",
  "allowedNextStatuses": [
    "SCREENING",
    "REJECTED",
    "WITHDRAWN"
  ]
}
```
> Notice `allowedNextStatuses` tells you exactly which stages this application can move to next!

---

### Step 4: View and Filter Your Applications

View all applications you have created:

```powershell
# 1. Get all your applications (Page 0, 10 items per page)
curl.exe -s -X GET "http://localhost:8080/api/v1/applications?page=0&size=10" `
  -H "Authorization: Bearer $token"

# 2. Filter by company name (e.g. Google)
curl.exe -s -X GET "http://localhost:8080/api/v1/applications?company=Google" `
  -H "Authorization: Bearer $token"

# 3. Filter by status (e.g. APPLIED)
curl.exe -s -X GET "http://localhost:8080/api/v1/applications?status=APPLIED" `
  -H "Authorization: Bearer $token"
```

---

### Step 5: Advance Application Through the Hiring Pipeline

As you make progress in your hiring process, update the application status:

#### Stage 1: Recruiter Phone Screening
```powershell
'{"status":"SCREENING","notes":"Passed initial screening, scheduling tech round"}' | `
  curl.exe -s -X PUT http://localhost:8080/api/v1/applications/1 `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer $token" `
  -d "@-"
```

#### Stage 2: Technical Interview
```powershell
'{"status":"INTERVIEW","notes":"Completed System Design & Coding rounds"}' | `
  curl.exe -s -X PUT http://localhost:8080/api/v1/applications/1 `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer $token" `
  -d "@-"
```

#### Stage 3: Job Offer!
```powershell
'{"status":"OFFER","notes":"Received written offer letter with equity package"}' | `
  curl.exe -s -X PUT http://localhost:8080/api/v1/applications/1 `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer $token" `
  -d "@-"
```

#### Stage 4: Accept Offer (Terminal State)
```powershell
'{"status":"ACCEPTED","notes":"Offer signed! Start date in November."}' | `
  curl.exe -s -X PUT http://localhost:8080/api/v1/applications/1 `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer $token" `
  -d "@-"
```

---

### Step 6: Test State Machine Validation (422 Guard)

Try to make an illegal leap (for example, attempting to jump directly from `APPLIED` to `OFFER`):

```powershell
'{"status":"OFFER"}' | `
  curl.exe -s -X PUT http://localhost:8080/api/v1/applications/1 `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer $token" `
  -d "@-"
```

**What the API returns (`422 Unprocessable Entity`)**:
```json
{
  "timestamp": "2026-10-04T11:54:05.927998749",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Cannot transition from APPLIED to OFFER. Allowed transitions: [SCREENING, REJECTED, WITHDRAWN]",
  "path": "/api/v1/applications/1"
}
```

---

### Step 7: View Your Job Hunt Analytics

Get real-time statistics, response rates, and company application breakdown:

```powershell
curl.exe -s -X GET http://localhost:8080/api/v1/analytics `
  -H "Authorization: Bearer $token"
```

**Response (`200 OK`)**:
```json
{
  "totalApplications": 3,
  "byStatus": {
    "APPLIED": 1,
    "SCREENING": 1,
    "INTERVIEW": 0,
    "OFFER": 0,
    "ACCEPTED": 1,
    "REJECTED": 0,
    "WITHDRAWN": 0
  },
  "appliedThisWeek": 3,
  "appliedThisMonth": 3,
  "responseRate": 66.7,
  "averageDaysToFirstUpdate": 4.5,
  "oldestPendingDays": 12,
  "topCompaniesByApplications": [
    {
      "company": "Google",
      "count": 2
    },
    {
      "company": "Amazon",
      "count": 1
    }
  ]
}
```

---

### Step 8: Delete an Application

When you want to remove an application:

```powershell
curl.exe -s -X DELETE http://localhost:8080/api/v1/applications/1 `
  -H "Authorization: Bearer $token"
```
**Response**: `204 No Content` (Success)

If you try to retrieve it again, it returns `404 Not Found`.

---

## ⚡ 5. Quick Automated Test Script

We have included a pre-written test script that automatically executes all 10 verification steps (Register, Login, Create, List, Multi-User Isolation, Update, Invalid Jump, Delete) in one go:

```powershell
.\test_phase2.ps1
```

---

## ❓ 6. Helpful Tips & Troubleshooting

### Q: Why do we use `| curl.exe ... -d "@-"` in PowerShell?
**A**: PowerShell automatically strips double quotes from inline strings like `'{"name":"Alice"}'`. Using the pipe `... | curl.exe ... -d "@-"` feeds the exact JSON through standard input without any quote corruption.

### Q: How do I completely wipe and start fresh with an empty database?
**A**: Run:
```powershell
docker compose down -v
docker compose up --build -d
```
The `-v` flag deletes the MySQL volume so all Flyway migrations run fresh.

### Q: What if I forget my password?
**A**: Simply register a new test email (e.g. `user2@test.com`) during development, or wipe the volume with `docker compose down -v`.

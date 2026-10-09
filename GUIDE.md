# HuntLog — User and Beginner Guide

This guide provides end-to-end instructions for running HuntLog, using the web dashboard, configuring automated email reminders, interacting with the REST API, and managing data.

For technical architecture and database design, see [ARCHITECTURE.md](file:///c:/projects/Huntlog/ARCHITECTURE.md).
For cloud deployment, see [DEPLOYMENT.md](file:///c:/projects/Huntlog/DEPLOYMENT.md).

---

## 1. Key Concepts

### Data Persistence
- HuntLog stores application data in a PostgreSQL 16 database.
- When running with Docker, data persists inside a Docker volume named `huntlog-postgres-data`. Stopping or restarting containers does not erase your data.

### Database Port Mapping
- Docker maps the PostgreSQL database to host port `5432` for local development.
- In production on Railway, connection parameters are supplied seamlessly via the `DATABASE_URL` environment variable.

### Authentication and Data Isolation
- Authentication uses JSON Web Tokens (JWT).
- All job application data is strictly scoped to the authenticated user ID. Users can only access and modify their own applications.

### Automated Follow-Up Reminders
- HuntLog runs a background scheduled job that identifies pending applications (in APPLIED, SCREENING, or INTERVIEW) that have not been updated in 7 or more days.
- A single consolidated email is sent to each user summarizing all their stale applications.

---

## 2. Prerequisites

Verify that the following tools are installed:

1. **Java JDK 21+**: Verify with `java -version`
2. **Maven 3.9+**: Verify with `mvn -version`
3. **Docker Desktop**: Verify with `docker --version`

---

## 3. How to Run HuntLog

### Method A: Docker Compose (Recommended)

Starts both the PostgreSQL database and the Spring Boot application container.

```powershell
# 1. Package the application JAR
mvn clean package -DskipTests

# 2. Build and start containers in the background
docker compose up --build -d

# 3. Verify running containers
docker compose ps
```

The application is accessible in your browser at:
**http://localhost:8080**

To stop containers:
```powershell
docker compose down
```

### Method B: Local Development Mode

Runs the Spring Boot application locally while connecting to PostgreSQL in Docker.

```powershell
# 1. Start only the PostgreSQL database
docker compose up postgres -d

# 2. Run the Spring Boot application
mvn spring-boot:run
```

---

## 4. Email Reminders Setup and Testing

HuntLog includes an automated reminder system powered by Spring Boot Scheduling and JavaMailSender.

### Setting Up Mailtrap for Local Testing

Mailtrap is a safe SMTP sandbox for testing emails without sending messages to real addresses.

1. Create a free account at https://mailtrap.io
2. Navigate to **Email Testing** > **Inboxes** > **SMTP Settings**
3. Select **Java / Spring Boot** or copy the credentials:
   - Host: `sandbox.smtp.mailtrap.io`
   - Port: `2525`
   - Username and Password
4. Create a `.env` file in the project root:
   ```env
   MAILTRAP_USERNAME=your_mailtrap_username
   MAILTRAP_PASSWORD=your_mailtrap_password
   ```

### Using Real Gmail SMTP in Production

For production on Railway, see [DEPLOYMENT.md](file:///c:/projects/Huntlog/DEPLOYMENT.md) for generating a 16-character Google App Password.

### Disabling Email Reminders

#### Method 1: Per-User Preference (Recommended)
Users can disable reminder emails via the preferences API:

```powershell
'{"emailRemindersEnabled":false}' | `
  curl.exe -s -X PUT http://localhost:8080/api/v1/users/me/preferences `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer $token" `
  -d "@-"
```

#### Method 2: System-Wide Config
Disable the scheduler globally by setting the environment variable:
```env
REMINDERS_ENABLED=false
```

---

## 5. Using the Web Dashboard

1. **Register an Account**: Open `http://localhost:8080` and switch to the "Register" tab. Enter your name, email, and password.
2. **View Live Analytics**: The dashboard displays real-time statistics including total applications, applications submitted this week, response rate percentage, and oldest pending application age.
3. **Create Applications**: Click "+ New Application" to add company, role, applied date, location, job URL, and notes.
4. **Update Status**: Click the "Status" button next to any row. The dropdown only permits transitions allowed by the state machine (e.g. APPLIED can only transition to SCREENING, REJECTED, or WITHDRAWN).
5. **Real-time Search and Filter**: Use the company search box to filter instantly on the client side, or filter by specific application status using the dropdown.
6. **Mobile View**: The layout automatically adapts to mobile screens, converting the sidebar into a collapsible top navigation.

---

## 6. End-to-End API Walkthrough

If you prefer to interact directly with the REST API using PowerShell and curl:

### Step 1: Register User

```powershell
'{"name":"Alice Dev","email":"alice@test.com","password":"password123"}' | `
  curl.exe -s -X POST http://localhost:8080/api/v1/auth/register `
  -H "Content-Type: application/json" -d "@-"
```

Response (`201 Created`):
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "email": "alice@test.com",
  "name": "Alice Dev"
}
```

### Step 2: Login and Save Token

```powershell
$auth = '{"email":"alice@test.com","password":"password123"}' | `
  curl.exe -s -X POST http://localhost:8080/api/v1/auth/login `
  -H "Content-Type: application/json" -d "@-" | ConvertFrom-Json

$token = $auth.token
```

### Step 3: Create a Job Application

```powershell
'{"company":"Google","role":"Staff Software Engineer","jobUrl":"https://careers.google.com/123","location":"Mountain View, CA","notes":"Referred by team"}' | `
  curl.exe -s -X POST http://localhost:8080/api/v1/applications `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer $token" `
  -d "@-"
```

Response (`201 Created`):
```json
{
  "id": 1,
  "company": "Google",
  "role": "Staff Software Engineer",
  "status": "APPLIED",
  "appliedDate": "2026-10-06",
  "lastUpdated": "2026-10-06T12:00:00.000",
  "jobUrl": "https://careers.google.com/123",
  "notes": "Referred by team",
  "location": "Mountain View, CA",
  "allowedNextStatuses": [
    "SCREENING",
    "REJECTED",
    "WITHDRAWN"
  ]
}
```

### Step 4: List and Filter Applications

```powershell
# List all applications (paginated)
curl.exe -s -X GET "http://localhost:8080/api/v1/applications?page=0&size=10" `
  -H "Authorization: Bearer $token"

# Filter by status
curl.exe -s -X GET "http://localhost:8080/api/v1/applications?status=APPLIED" `
  -H "Authorization: Bearer $token"
```

### Step 5: Advance Status Through Hiring Pipeline

```powershell
# Transition from APPLIED to SCREENING
'{"status":"SCREENING","notes":"Recruiter phone screen scheduled"}' | `
  curl.exe -s -X PUT http://localhost:8080/api/v1/applications/1 `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer $token" `
  -d "@-"
```

### Step 6: Verify State Machine Validation

Attempting an illegal jump (e.g. `APPLIED` directly to `OFFER`) results in a `422 Unprocessable Entity`:

```powershell
'{"status":"OFFER"}' | `
  curl.exe -s -X PUT http://localhost:8080/api/v1/applications/1 `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer $token" `
  -d "@-"
```

Response (`422 Unprocessable Entity`):
```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Cannot transition from APPLIED to OFFER. Allowed transitions: [SCREENING, REJECTED, WITHDRAWN]",
  "path": "/api/v1/applications/1"
}
```

### Step 7: Manage User Email Preferences

```powershell
# 1. Fetch current preference
curl.exe -s -X GET http://localhost:8080/api/v1/users/me/preferences `
  -H "Authorization: Bearer $token"

# 2. Update preference to false
'{"emailRemindersEnabled":false}' | `
  curl.exe -s -X PUT http://localhost:8080/api/v1/users/me/preferences `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer $token" `
  -d "@-"
```

### Step 8: Retrieve Analytics

```powershell
curl.exe -s -X GET http://localhost:8080/api/v1/analytics `
  -H "Authorization: Bearer $token"
```

Response (`200 OK`):
```json
{
  "totalApplications": 1,
  "byStatus": {
    "APPLIED": 0,
    "SCREENING": 1,
    "INTERVIEW": 0,
    "OFFER": 0,
    "ACCEPTED": 0,
    "REJECTED": 0,
    "WITHDRAWN": 0
  },
  "appliedThisWeek": 1,
  "appliedThisMonth": 1,
  "responseRate": 100.0,
  "averageDaysToFirstUpdate": 0.0,
  "oldestPendingDays": 0,
  "topCompaniesByApplications": [
    {
      "company": "Google",
      "count": 1
    }
  ]
}
```

### Step 9: Delete Application

```powershell
curl.exe -s -X DELETE http://localhost:8080/api/v1/applications/1 `
  -H "Authorization: Bearer $token"
```

Response: `204 No Content`

---

## 7. Troubleshooting

### Why use `| curl.exe ... -d "@-"` in PowerShell?
PowerShell command parsing can strip double quotes from inline JSON string arguments. Piping JSON strings directly to `curl.exe -d "@-"` preserves formatting reliably.

### How to Reset Database and Start Clean
Run:
```powershell
docker compose down -v
docker compose up --build -d
```
The `-v` flag removes the persistent PostgreSQL volume, allowing all Flyway migrations to run from scratch.

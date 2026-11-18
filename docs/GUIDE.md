# HuntLog — User and Beginner Guide

This guide provides end-to-end instructions for running HuntLog, using the web dashboard, configuring automated email reminders, interacting with the REST API, and managing data.

For technical architecture and database design, see [ARCHITECTURE.md](ARCHITECTURE.md).
For cloud deployment, see [DEPLOYMENT.md](DEPLOYMENT.md).

---

## 1. Key Concepts

### Data Persistence
- HuntLog stores application data in a PostgreSQL 16 database.
- When running with Docker, data persists inside a Docker volume named `huntlog-postgres-data`. Stopping or restarting containers does not erase your data.

### Database Port Mapping
- Docker maps the PostgreSQL database to host port `5432` for local development.
- In production, connection parameters are supplied through the configured database environment variables.

### Authentication and Data Isolation
- Authentication uses JSON Web Tokens (JWT).
- All job application data is strictly scoped to the authenticated user ID. Users can only access and modify their own applications.

### Automated Follow-Up Reminders
- HuntLog scans pending applications (APPLIED, SCREENING, or INTERVIEW) that have not been updated in 7 or more days.
- The scan runs after startup and hourly by default, then sends one consolidated email per user via SMTP.
- Successful reminders are tracked so the same application is not emailed again every hour.

### Automated Mailbox Synchronization
- HuntLog connects to your mailbox via IMAP over TLS/SSL (port 993) using a 16-character Google App Password.
- An intelligent parser scans incoming emails from top Applicant Tracking Systems (Greenhouse, Lever, Workday, HiBob, Keka, Gem, iCIMS, HackerRank, Ashby, and direct recruiters).
- Detected applications are automatically imported with status `APPLIED` and tagged with an `EMAIL_SYNC` source badge.
- Status updates (such as interview invitations, assessments, and rejections) are matched against existing applications and advance the pipeline in strict compliance with the state machine.
- Applications are never duplicated or accidentally regressed; every sync run updates a persistent timestamp checkpoint.

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

### Method B: Local Development Mode (Docker Postgres)

Runs the Spring Boot application locally while connecting to PostgreSQL in Docker.

```powershell
# 1. Start only the PostgreSQL database
docker compose up postgres -d

# 2. Run the Spring Boot application
mvn spring-boot:run
```

### Method C: Native Java Mode (Zero Docker / WSL Required)

If Docker or WSL is not installed or temporarily unavailable, you can run HuntLog natively on Windows or Linux by pointing directly to your cloud Neon PostgreSQL database over SSL:

```powershell
# Set database environment variables in your current PowerShell session
$env:SPRING_DATASOURCE_URL="<jdbc-postgresql-url>"
$env:DB_USERNAME="<database-user>"
$env:DB_PASSWORD="<database-password>"
$env:JWT_SECRET="<random-secret-at-least-32-bytes>"

# Start the Spring Boot application
mvn spring-boot:run
```

The application starts immediately on `http://localhost:8080`, applying Flyway migrations and serving the full SPA and API using cloud Neon storage.


---

## 4. Email Reminders and Mailbox Synchronization Setup

HuntLog integrates with your email in two distinct directions:
1. **Outbound SMTP**: Dispatches consolidated follow-up reminder digests to you for stale pending applications.
2. **Inbound IMAP**: Connects to your mailbox over TLS/SSL (port 993) to read and parse job applications and interview updates automatically.

---

### 4.1 Follow-Up Email Reminders (SMTP)

#### Setting Up Mailtrap for Local Testing

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

#### Using Real Gmail SMTP in Production

For production on Render, see [DEPLOYMENT.md](DEPLOYMENT.md) for generating a 16-character Google App Password.

#### Disabling Email Reminders

##### Method 1: Per-User Preference (Recommended)
Users can disable reminder emails via the preferences API:

```powershell
'{"emailRemindersEnabled":false}' | `
  curl.exe -s -X PUT http://localhost:8080/api/v1/users/me/preferences `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer $token" `
  -d "@-"
```

##### Method 2: System-Wide Config
Disable the scheduler globally by setting the environment variable:
```env
REMINDERS_ENABLED=false
```

#### Test Reminders Immediately

After logging in, call the authenticated endpoint below to send reminders without waiting for the scheduled scan:

```bash
curl -X POST http://localhost:8080/api/v1/users/me/reminders/send \
  -H "Authorization: Bearer <your-jwt>"
```

The response contains the number of applications notified. Check application logs for SMTP failures.

---

### 4.2 Automated Mailbox Synchronization (IMAP)

HuntLog can periodically inspect your mailbox over IMAP (port 993) and automatically register applications or update pipeline statuses based on emails received from recruiters and Applicant Tracking Systems.

#### Step 1: Generate a Google App Password
For Gmail accounts with 2-Step Verification enabled, Google requires an App Password rather than your account password:

1. Sign in to your [Google Account](https://myaccount.google.com/).
2. Select **Security** from the left navigation.
3. Under *How you sign in to Google*, ensure **2-Step Verification** is turned on.
4. Search for **App passwords** in the top search bar (or navigate directly to [https://myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords)).
5. Enter an app name (such as `HuntLog Email Sync`) and click **Create**.
6. Copy the generated 16-character code (for example: `abcd efgh ijkl mnop`).

#### Step 2: Configure via the Web Dashboard
1. Log in to HuntLog at `http://localhost:8080`.
2. Click **Settings** in the left sidebar menu (or navigate to `http://localhost:8080#view-settings`).
3. Locate the **Email Auto-Sync** card:
   - Toggle **Enable Email Auto-Sync** on.
   - **IMAP Server Host**: `imap.gmail.com`
   - **IMAP Port**: `993`
   - **Email Address**: Your Gmail address (e.g. `you@gmail.com`).
   - **Google App Password**: Paste the 16-character App Password generated in Step 1.
4. Click **Save Configuration**. The password is saved securely and is never re-exposed to the client interface.
5. Click **Sync Now** (or the **Sync Emails** button in the top navigation bar) to execute an on-demand scan.

#### Step 3: Automated Background Scanning
- When enabled, HuntLog runs a background scan every hour (`app.email-sync.scan-interval-ms=3600000`).
- The initial scan evaluates messages received over the past 14 days (`app.email-sync.days-to-scan=14`).
- Subsequent scans only process emails received after the recorded `lastEmailSyncedAt` timestamp.

#### Supported ATS Providers and Detection Heuristics
The parsing engine identifies confirmation and status emails from:
- **Greenhouse** (`no-reply@greenhouse.io`, `greenhouse-mail.io`)
- **Lever** (`no-reply@hire.lever.co`, `jobs.lever.co`)
- **Workday** (`myworkday.com`, `myworkdayjobs.com`)
- **HiBob** (`hibob.com`)
- **Keka** (`keka.com`, `kekacareers`)
- **Gem** (`gem.com`)
- **iCIMS** (`icims.com`)
- **HackerRank** (`hackerrank.com`)
- **Ashby** (`ashbyhq.com`, `jobs.ashbyhq.com`)
- **Direct Recruiter Emails** (Subject matching patterns: `Thank you for applying to <Company>`, `Interview with <Company>`)

---

## 5. Using the Web Dashboard

1. **Register an Account**: Open `http://localhost:8080` and switch to the "Register" tab. Enter your name, email, and password.
2. **View Live Analytics**: The dashboard displays real-time statistics including total applications, applications submitted this week, response rate percentage, and oldest pending application age.
3. **Trigger Email Sync**: Click the **Sync Emails** button in the top navigation bar at any time to scan your configured mailbox. Applications imported via email sync will display a distinct **Sync** badge.
4. **Create Applications Manually**: Click "+ New Application" to manually record company, role, applied date, location, job URL, and notes.
5. **Update Status**: Click the "Status" button next to any row. The dropdown only permits transitions allowed by the state machine (e.g. APPLIED can only transition to SCREENING, REJECTED, or WITHDRAWN).
6. **Real-time Search and Filter**: Use the company search box to filter instantly on the client side, or filter by specific application status using the dropdown.
7. **Configure Settings**: Navigate to **Settings** in the sidebar to toggle email reminders, manage IMAP email auto-sync credentials, inspect the latest sync timestamp, or delete your account.
8. **Mobile View**: The layout automatically adapts to mobile screens, converting the sidebar into a collapsible top navigation.

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

### Step 7: Manage User Email and Auto-Sync Preferences

```powershell
# 1. Fetch current preferences
curl.exe -s -X GET http://localhost:8080/api/v1/users/me/preferences `
  -H "Authorization: Bearer $token"
```

Response (`200 OK`):
```json
{
  "emailRemindersEnabled": true,
  "emailSyncEnabled": false,
  "imapHost": "imap.gmail.com",
  "imapPort": 993,
  "imapUsername": null,
  "hasImapPassword": false,
  "lastEmailSyncedAt": null
}
```

```powershell
# 2. Update preferences: enable email sync and configure IMAP credentials
'{"emailRemindersEnabled":true,"emailSyncEnabled":true,"imapHost":"imap.gmail.com","imapPort":993,"imapUsername":"applicant@gmail.com","imapPassword":"your-16-char-app-pass"}' | `
  curl.exe -s -X PUT http://localhost:8080/api/v1/users/me/preferences `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer $token" `
  -d "@-"
```

Response (`200 OK`):
```json
{
  "emailRemindersEnabled": true,
  "emailSyncEnabled": true,
  "imapHost": "imap.gmail.com",
  "imapPort": 993,
  "imapUsername": "applicant@gmail.com",
  "hasImapPassword": true,
  "lastEmailSyncedAt": null
}
```

### Step 8: Trigger On-Demand Mailbox Synchronization

```powershell
# 1. Check sync status
curl.exe -s -X GET http://localhost:8080/api/v1/sync/status `
  -H "Authorization: Bearer $token"

# 2. Trigger mailbox scan
curl.exe -s -X POST http://localhost:8080/api/v1/sync/emails `
  -H "Authorization: Bearer $token"
```

Response (`200 OK`):
```json
{
  "scannedCount": 8,
  "createdCount": 2,
  "updatedCount": 1
}
```

Applications imported from your mailbox are returned by `GET /api/v1/applications` with `"source": "EMAIL_SYNC"`:
```json
{
  "id": 2,
  "company": "Stripe",
  "role": "Backend Engineer",
  "status": "APPLIED",
  "source": "EMAIL_SYNC",
  "appliedDate": "2026-11-18",
  "jobUrl": "https://jobs.lever.co/stripe/abc-123",
  "allowedNextStatuses": ["SCREENING", "REJECTED", "WITHDRAWN"]
}
```

### Step 9: Retrieve Analytics

```powershell
curl.exe -s -X GET http://localhost:8080/api/v1/analytics `
  -H "Authorization: Bearer $token"
```

Response (`200 OK`):
```json
{
  "totalApplications": 2,
  "byStatus": {
    "APPLIED": 1,
    "SCREENING": 1,
    "INTERVIEW": 0,
    "OFFER": 0,
    "ACCEPTED": 0,
    "REJECTED": 0,
    "WITHDRAWN": 0
  },
  "appliedThisWeek": 2,
  "appliedThisMonth": 2,
  "responseRate": 50.0,
  "averageDaysToFirstUpdate": 0.0,
  "oldestPendingDays": 0,
  "topCompaniesByApplications": [
    {
      "company": "Google",
      "count": 1
    },
    {
      "company": "Stripe",
      "count": 1
    }
  ]
}
```

### Step 10: Delete Application

```powershell
curl.exe -s -X DELETE http://localhost:8080/api/v1/applications/1 `
  -H "Authorization: Bearer $token"
```

Response: `204 No Content`

### Step 11: Delete User Account (Danger Zone)

Permanently removes the authenticated user and all their associated applications:

```powershell
curl.exe -s -X DELETE http://localhost:8080/api/v1/users/me `
  -H "Authorization: Bearer $token"
```

Response: `204 No Content`

Subsequent requests using the old token will return `401 Unauthorized`.


---

## 7. Interactive API Documentation (Swagger UI)

HuntLog provides an interactive Swagger UI dashboard powered by Springdoc OpenAPI:
- Local URL: http://localhost:8080/swagger-ui.html
- Production URL: https://huntlog-coga.onrender.com/swagger-ui.html
- OpenAPI Spec: https://huntlog-coga.onrender.com/api-docs

### How to Test Protected Endpoints via Swagger UI:
1. Open http://localhost:8080/swagger-ui.html in your browser.
2. Expand the `Authentication` group and run `POST /api/v1/auth/login` (or `/register`) using **Try it out**.
3. Copy the returned `token` string from the response.
4. Click the green **Authorize** button at the top right of the Swagger UI page.
5. Paste your token into the Value box and click **Authorize**.
6. All protected endpoints (Job Applications, Analytics, User Preferences) will now send your JWT automatically when tested with **Try it out**.

---

## 8. Troubleshooting

### Why use `| curl.exe ... -d "@-"` in PowerShell?
PowerShell command parsing can strip double quotes from inline JSON string arguments. Piping JSON strings directly to `curl.exe -d "@-"` preserves formatting reliably.

### How to Reset Database and Start Clean
Run:
```powershell
docker compose down -v
docker compose up --build -d
```
The `-v` flag removes the persistent PostgreSQL volume, allowing all Flyway migrations to run from scratch.

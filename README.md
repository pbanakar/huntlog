# HuntLog

HuntLog is a full-stack job application tracker with lifecycle validation, per-user data isolation, real-time analytics, automated follow-up email reminders, and interactive OpenAPI documentation. Built with Java 21, Spring Boot 3, PostgreSQL 16, Chart.js, and a vanilla JavaScript single-page frontend.

Live Demo: https://huntlog-coga.onrender.com (Deployed on Render + Neon PostgreSQL)

---

## Documentation

- [Production Deployment Guide](file:///c:/projects/Huntlog/DEPLOYMENT.md): Step-by-step guide to deploying on Render with Neon PostgreSQL, HTTPS, and Gmail/Mailtrap SMTP.
- [User and Beginner Guide](file:///c:/projects/Huntlog/GUIDE.md): Local development, dashboard walkthrough, email reminder setup, and API examples.
- [Architecture and Technical Design](file:///c:/projects/Huntlog/ARCHITECTURE.md): System architecture, finite state machine, security isolation, and database schemas.

---

## Features and Project Phases

- [x] Phase 1: Core CRUD and Finite State Machine (APPLIED -> SCREENING -> INTERVIEW -> OFFER -> ACCEPTED / REJECTED / WITHDRAWN)
- [x] Phase 2: Stateless JWT Authentication and Per-User Data Isolation
- [x] Phase 3a: Pipeline Analytics (Response rate, velocity, status breakdown, oldest pending)
- [x] Phase 3b: Single-Page Application (Vanilla JavaScript, HTML5, CSS3)
- [x] Phase 4a: Automated Email Reminders and User Preferences (Spring Mail, Cron Scheduler)
- [x] Phase 4b: Cloud Deployment (Render Docker Web Service + Neon PostgreSQL)
- [x] Phase 5a: Interactive OpenAPI Documentation (Springdoc OpenAPI / Swagger UI)
- [x] Phase 5b: Complete Frontend Feature Parity (Chart.js analytics, settings, edit modal, date filters, toasts, mobile drawer)

---

## Frontend Capabilities (Phase 5b)

- **Application Management**:
  - Full CRUD operations with instant client-side table rendering
  - Status transition modal enforcing allowed state machine moves with stale duration indicator
  - Dedicated Edit modal for updating company, role, location, job URL, and notes
  - Truncated notes preview with full-text hover tooltips
  - One-click "Copy URL" button with clipboard feedback and direct "View" link
  - Days-since-update counter with visual warning highlights for stale entries (> 14 days)
- **Advanced Filtering and Search**:
  - Real-time text search across company, role, location, and notes
  - Status filter dropdown
  - Date range filters ("Applied From" and "Applied To") with instant client-side evaluation
- **Interactive Analytics View**:
  - Pipeline stat cards: Total Applications, Applied This Week, Response Rate, Oldest Pending
  - Chart.js horizontal bar chart visualizing applications across all pipeline stages with badge color harmony
  - Top Companies ranking table with application counts and empty states
- **User Settings and Preferences**:
  - Pure CSS toggle switch for enabling/disabling automated 7-day stale follow-up email reminders
  - Read-only profile overview displaying name, email, and registration date
  - Danger Zone account deletion with confirmation dialog and cascade deletion
- **UX Polish and Responsiveness**:
  - Toast notification system for success, error, and info events with auto-dismissal
  - CSS keyframe loading spinners for asynchronous table and analytics loading
  - Informative empty states with direct call-to-action buttons
  - Fully mobile-responsive layout featuring a sliding drawer sidebar, hamburger toggle, and backdrop overlay

---

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.4.1, Spring Security (Stateless JWT), Spring Data JPA, Spring Mail, Spring Boot Actuator, Flyway Migrations, Springdoc OpenAPI 2.8.5
- **Frontend**: Vanilla HTML5, CSS3, ES6+ JavaScript, Chart.js 4.4.0 (Static Spring Boot resources, zero build step required)
- **Database**: PostgreSQL 16/18 with automated Flyway versioning (Hosted on Neon)
- **Cloud and Hosting**: Docker, Docker Compose, Render (Web Service, Auto-HTTPS), Neon (Serverless PostgreSQL), UptimeRobot (24/7 Keep-Alive)

---

## Quick Start (Local Development)

### 1. Build and Package

```powershell
mvn clean package -DskipTests
```

### 2. Configure Environment (Optional for Email Testing)

Copy `.env.example` to `.env` and supply Mailtrap or Gmail credentials:

```powershell
cp .env.example .env
```

### 3. Start Application Stack

#### With Docker:
```powershell
docker compose up --build -d
```

#### Without Docker (Direct Java + Cloud Neon PostgreSQL):
```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://ep-fancy-cake-b3nmhm4h-pooler.c-4.ap-southeast-1.aws.neon.tech/neondb?sslmode=require"
$env:DB_USERNAME="neondb_owner"
$env:DB_PASSWORD="npg_vz69XlYewrsB"
$env:JWT_SECRET="404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
mvn spring-boot:run
```

### 4. Access HuntLog

- Web Application: http://localhost:8080
- REST API Base: http://localhost:8080/api/v1
- Actuator Health: http://localhost:8080/actuator/health
- Swagger UI (Local): http://localhost:8080/swagger-ui.html
- Swagger UI (Live): https://huntlog-coga.onrender.com/swagger-ui.html

---

## API Documentation

Interactive API documentation is available via Swagger UI:
- Local: http://localhost:8080/swagger-ui.html
- Live: https://huntlog-coga.onrender.com/swagger-ui.html
- OpenAPI Specification: https://huntlog-coga.onrender.com/api-docs

Paste your JWT token into the Authorize dialog to test protected endpoints interactively.

---

## Deploy Your Own (Zero-Cost Production Stack)

HuntLog is pre-configured for zero-cost production deployment on Render and Neon PostgreSQL:
- **Neon.tech**: Free serverless PostgreSQL database (persistent storage, zero expiration).
- **Render.com**: Free Docker Web Service deployment directly from GitHub.
- **UptimeRobot**: Free keep-alive ping on `/actuator/health` to prevent idle sleep.

### Environment Variables:
- `SPRING_DATASOURCE_URL`: Neon JDBC URL (`jdbc:postgresql://<host>/neondb?sslmode=require`)
- `DB_USERNAME`: Neon database user
- `DB_PASSWORD`: Neon database password
- `JWT_SECRET`: 256-bit secret key for authentication tokens
- `MAIL_HOST`: `sandbox.smtp.mailtrap.io` or `smtp.gmail.com`
- `MAIL_PORT`: `587`
- `MAIL_USERNAME`: Mail account username
- `MAIL_PASSWORD`: Mail account password
- `MAIL_FROM`: Sender email address
- `REMINDERS_ENABLED`: `true`
- `REMINDER_STALE_DAYS`: `7`
- `REMINDER_CRON`: `0 0 9 * * *`
- `PORT`: `8080`

For full deployment instructions, see [DEPLOYMENT.md](file:///c:/projects/Huntlog/DEPLOYMENT.md).

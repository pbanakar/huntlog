# HuntLog

HuntLog is a full-stack job application tracker with lifecycle validation, per-user data isolation, real-time analytics, and automated follow-up email reminders. Built with Java 21, Spring Boot 3, PostgreSQL 16, and a vanilla JavaScript single-page frontend.

Live Demo: https://huntlog-production.up.railway.app (Deployed on Railway)

---

## Documentation

- [Production Deployment Guide](file:///c:/projects/Huntlog/DEPLOYMENT.md): Step-by-step guide to deploying on Railway with PostgreSQL, HTTPS, and Gmail SMTP.
- [User and Beginner Guide](file:///c:/projects/Huntlog/GUIDE.md): Local development, dashboard walkthrough, email reminder setup, and API examples.
- [Architecture and Technical Design](file:///c:/projects/Huntlog/ARCHITECTURE.md): System architecture, finite state machine, security isolation, and database schemas.

---

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.4.1, Spring Security (Stateless JWT), Spring Data JPA, Spring Mail, Spring Boot Actuator, Flyway Migrations
- **Frontend**: Vanilla HTML5, CSS3, ES6+ JavaScript (Served statically by Spring Boot, no build step)
- **Database**: PostgreSQL 16 with automated Flyway versioning
- **Cloud & Deployment**: Docker, Docker Compose, Railway (Managed PostgreSQL, Auto-HTTPS)

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

```powershell
docker compose up --build -d
```

### 4. Access HuntLog Locally

- Web Application: http://localhost:8080
- REST API Base: http://localhost:8080/api/v1
- Actuator Health: http://localhost:8080/actuator/health

---

## Deploy Your Own

HuntLog is pre-configured for one-click deployment on Railway using `railway.toml` and Docker.

### Environment Variables for Railway:
- `DATABASE_URL`: Automatically provided by Railway PostgreSQL plugin
- `JWT_SECRET`: 256-bit secret key for authentication tokens
- `MAIL_HOST`: `smtp.gmail.com`
- `MAIL_PORT`: `587`
- `MAIL_USERNAME`: Your Gmail address
- `MAIL_PASSWORD`: 16-character Google App Password (never commit to git)
- `REMINDERS_ENABLED`: `true`
- `REMINDER_STALE_DAYS`: `7`
- `REMINDER_CRON`: `0 0 9 * * *`
- `SPRING_PROFILES_ACTIVE`: `prod`

For full deployment instructions, see [DEPLOYMENT.md](file:///c:/projects/Huntlog/DEPLOYMENT.md).

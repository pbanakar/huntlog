# HuntLog

HuntLog is a full-stack job application tracker with lifecycle validation, per-user data isolation, real-time analytics, and automated follow-up email reminders. Built with Java 21, Spring Boot 3, PostgreSQL 16, and a vanilla JavaScript single-page frontend.

Live Demo: https://huntlog-coga.onrender.com (Deployed on Render + Neon PostgreSQL)

---

## Documentation

- [Production Deployment Guide](file:///c:/projects/Huntlog/DEPLOYMENT.md): Step-by-step guide to deploying on Render with Neon PostgreSQL, HTTPS, and Gmail/Mailtrap SMTP.
- [User and Beginner Guide](file:///c:/projects/Huntlog/GUIDE.md): Local development, dashboard walkthrough, email reminder setup, and API examples.
- [Architecture and Technical Design](file:///c:/projects/Huntlog/ARCHITECTURE.md): System architecture, finite state machine, security isolation, and database schemas.

---

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.4.1, Spring Security (Stateless JWT), Spring Data JPA, Spring Mail, Spring Boot Actuator, Flyway Migrations
- **Frontend**: Vanilla HTML5, CSS3, ES6+ JavaScript (Served statically by Spring Boot, no build step)
- **Database**: PostgreSQL 16/18 with automated Flyway versioning (Hosted on Neon)
- **Cloud & Deployment**: Docker, Docker Compose, Render (Web Service, Auto-HTTPS), Neon (Serverless PostgreSQL), UptimeRobot (24/7 Keep-Alive)

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

## Deploy Your Own (100% Free Production Stack)

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

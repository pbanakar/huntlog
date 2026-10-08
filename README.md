# HuntLog

HuntLog is a full-stack job application tracker with lifecycle validation, per-user isolation, real-time analytics, and automated follow-up email reminders. Built with Java 21, Spring Boot 3, MySQL 8, and a vanilla JavaScript single-page frontend.

---

## Documentation

- [User and Beginner Guide](file:///c:/projects/Huntlog/GUIDE.md): Step-by-step setup, UI usage, email reminder configuration, API examples, and troubleshooting.
- [Architecture and Technical Design](file:///c:/projects/Huntlog/ARCHITECTURE.md): System architecture, finite state machine, security model, reminder scheduling pipeline, and database schemas.

---

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.4.1, Spring Security (Stateless JWT), Spring Data JPA, Spring Mail, Flyway Migrations
- **Frontend**: Vanilla HTML5, CSS3, ES6+ JavaScript (Served statically by Spring Boot, no build step)
- **Database**: MySQL 8.0 with automated migrations
- **Deployment**: Docker and Docker Compose

---

## Quick Start

### 1. Build and Package

```powershell
mvn clean package -DskipTests
```

### 2. Configure Environment (Optional for Email Testing)

Copy `.env.example` to `.env` and supply your free Mailtrap SMTP credentials:

```powershell
cp .env.example .env
```

### 3. Start Application Stack

```powershell
docker compose up --build -d
```

### 4. Access HuntLog

- Web Application: http://localhost:8080
- REST API Base: http://localhost:8080/api/v1

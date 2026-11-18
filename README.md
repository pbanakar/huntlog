# HuntLog

HuntLog is a full-stack job application tracker designed for managing job pipelines, tracking hiring lifecycle transitions with a deterministic state machine, analyzing search metrics, receiving automated follow-up reminders, and automatically synchronizing applications from mailbox confirmations via IMAP.

**Stack:** Java 21, Spring Boot 3.4, PostgreSQL 16, Flyway, JWT (HMAC-SHA512), Jakarta Mail (IMAP over TLS/SSL), and a vanilla JavaScript single-page frontend.

**Live demo:** [huntlog-coga.onrender.com](https://huntlog-coga.onrender.com)

## Core Capabilities

- **Application Management**: Track company, role, applied date, location, job links, source, and interview notes.
- **Deterministic State Machine**: Enforces hiring pipeline transitions (`APPLIED` -> `SCREENING` -> `INTERVIEW` -> `OFFER` -> `ACCEPTED` / `REJECTED` / `WITHDRAWN`).
- **Automated Mailbox Synchronization**: Connects to mailbox via IMAP over SSL (port 993) to detect job applications, interview invitations, and status updates across common ATS platforms (Greenhouse, Lever, Workday, HiBob, Keka, Gem, iCIMS, HackerRank, Ashby, direct recruiting).
- **Follow-up Reminders**: Background scheduler scans stale applications and sends consolidated email digests via SMTP.
- **Search and Metrics**: Real-time response rate calculation, status breakdowns, top applied companies, and client-side instant search.
- **Multi-Tenant Security**: Per-user data isolation, BCrypt password hashing, and stateless JWT authentication with rate limiting.

## Run Locally

### Option A: Local Development with Docker PostgreSQL

1. Start the PostgreSQL database:
```bash
docker compose up -d postgres
```

2. Run the Spring Boot application:
```bash
mvn spring-boot:run
```

### Option B: Full Docker Compose Stack

```bash
mvn clean package -DskipTests
docker compose up --build
```

Access the application in your browser at [http://localhost:8080](http://localhost:8080).
Interactive Swagger API documentation is available at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).

## Documentation

- [User and Beginner Guide](docs/GUIDE.md): Local setup, email sync configuration, Google App Passwords, web dashboard usage, and end-to-end API walkthrough.
- [System Architecture and Technical Design](docs/ARCHITECTURE.md): Architecture diagrams, state machine mechanics, email synchronization pipeline, reminder subsystem, database ERD, and migration history.
- [Production Deployment Guide](docs/DEPLOYMENT.md): Step-by-step production setup on Render, Neon PostgreSQL, UptimeRobot, and environment variables.
- [Environment Configuration](.env.example): Reference of application environment variables and default values.

Never commit production credentials. Generate a new `JWT_SECRET` and provide database, SMTP, and IMAP credentials through environment variables or secure application settings.

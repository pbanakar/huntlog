# HuntLog

HuntLog is a full-stack job application tracker for managing applications, hiring-pipeline transitions, analytics, and follow-up reminders.

**Stack:** Java 21, Spring Boot, PostgreSQL, Flyway, JWT, Spring Mail, and a vanilla JavaScript frontend.

**Live demo:** [huntlog-coga.onrender.com](https://huntlog-coga.onrender.com)

## Run locally

```bash
cp .env.example .env
mvn spring-boot:run
```

Or start the complete local stack:

```bash
mvn clean package -DskipTests
docker compose up --build
```

Open [http://localhost:8080](http://localhost:8080). API documentation is available at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).

## Documentation

- [User guide](docs/GUIDE.md)
- [Deployment guide](docs/DEPLOYMENT.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Environment variables](.env.example)

Never commit production credentials. Generate a new `JWT_SECRET` and provide database and SMTP credentials through the deployment environment.

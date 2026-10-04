# HuntLog

Job Application Tracker REST API built with **Java 21**, **Spring Boot 3.4.1**, **Spring Security (JWT)**, **Spring Data JPA**, **Flyway**, and **MySQL 8.0**.

---

## 📚 Documentation

- 🚀 **[User & Beginner Guide](file:///c:/projects/Huntlog/GUIDE.md)**: How to run the application, full end-to-end API walkthroughs, PowerShell/cURL examples, and troubleshooting.
- 🏛️ **[Architecture & Technical Design](file:///c:/projects/Huntlog/ARCHITECTURE.md)**: Request flow diagrams, state machine mechanics, security & data isolation models, and database ER schemas.

---

## ⚡ Quick Start

```powershell
# 1. Package the application
mvn clean package -DskipTests

# 2. Start MySQL and Spring Boot in Docker
docker compose up --build -d

# 3. Run the automated test script
.\test_phase2.ps1
```

The API is accessible at: `http://localhost:8080`

---

## 📋 Status Tracking

- [x] **Phase 1** — Core CRUD + State Machine
- [x] **Phase 2** — JWT Authentication & Per-User Data Isolation
- [ ] **Phase 3** — Email Notifications & Scheduling
- [ ] **Phase 4** — API Documentation (Swagger/OpenAPI)
- [ ] **Phase 5** — Frontend Dashboard

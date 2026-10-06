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
```

The API is accessible at: `http://localhost:8080`

---

## 📊 Analytics Example

Retrieve real-time metrics and insights about your job hunt:

```powershell
curl.exe -s -X GET http://localhost:8080/api/v1/analytics `
  -H "Authorization: Bearer $token"
```

**Response (`200 OK`)**:
```json
{
  "totalApplications": 42,
  "byStatus": {
    "APPLIED": 15,
    "SCREENING": 10,
    "INTERVIEW": 8,
    "OFFER": 3,
    "ACCEPTED": 1,
    "REJECTED": 4,
    "WITHDRAWN": 1
  },
  "appliedThisWeek": 5,
  "appliedThisMonth": 18,
  "responseRate": 63.1,
  "averageDaysToFirstUpdate": 8.4,
  "oldestPendingDays": 21,
  "topCompaniesByApplications": [
    {
      "company": "Google",
      "count": 3
    },
    {
      "company": "Microsoft",
      "count": 2
    }
  ]
}
```

---

## 📋 Status Tracking

- [x] **Phase 1** — Core CRUD + State Machine
- [x] **Phase 2** — JWT Authentication & Per-User Data Isolation
- [x] **Phase 3a** — User Analytics & Metrics (`GET /api/v1/analytics`)
- [ ] **Phase 3b** — Email Notifications & Reminders
- [ ] **Phase 4** — API Documentation (Swagger/OpenAPI)
- [ ] **Phase 5** — Frontend Dashboard

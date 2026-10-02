# HuntLog

Job Application Tracker REST API built with Spring Boot 3, Java 21, and MySQL.

> 📘 **Looking for complete setup instructions, state machine diagrams, and cURL/PowerShell examples? Check out the [Comprehensive User & Developer Guide](file:///c:/projects/Huntlog/GUIDE.md).**

## Quick Start

```bash
# 1. Clone and navigate
git clone https://github.com/pbanakar/huntlog.git && cd huntlog

# 2. Start MySQL + app via Docker Compose
docker compose up --build -d

# 3. Verify the API is running
curl http://localhost:8080/api/v1/applications
```

## Example: Create a New Application

```bash
curl -s -X POST http://localhost:8080/api/v1/applications \
  -H "Content-Type: application/json" \
  -d '{
    "company": "Google",
    "role": "Software Engineer",
    "jobUrl": "https://careers.google.com/jobs/123",
    "location": "Mountain View, CA",
    "notes": "Referred by a friend"
  }' | jq
```

**Response** (201 Created):
```json
{
  "id": 1,
  "company": "Google",
  "role": "Software Engineer",
  "status": "APPLIED",
  "appliedDate": "2026-10-01",
  "lastUpdated": "2026-10-01T12:00:00.000000",
  "jobUrl": "https://careers.google.com/jobs/123",
  "notes": "Referred by a friend",
  "location": "Mountain View, CA",
  "allowedNextStatuses": ["SCREENING", "REJECTED", "WITHDRAWN"]
}
```

The `allowedNextStatuses` field tells the client exactly which state transitions are valid next — no guessing required.

## Phase Status

- [x] **Phase 1** — Core CRUD + State Machine
- [ ] **Phase 2** — Authentication & Authorization
- [ ] **Phase 3** — Email Notifications & Scheduling
- [ ] **Phase 4** — API Documentation (Swagger/OpenAPI)
- [ ] **Phase 5** — Frontend Dashboard

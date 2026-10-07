# HuntLog — System Architecture and Technical Design

This document details the architectural decisions, design patterns, state machine mechanics, security model, database schema, and frontend architecture for HuntLog.

---

## 1. High-Level Architecture and Request Flow

HuntLog follows a layered, domain-driven Spring Boot architecture with separation of concerns. The frontend is served directly as static SPA files without a server-side rendering template engine.

```mermaid
flowchart TD
    Client(["Client (Browser SPA / cURL)"])
    
    subgraph SpringSecurity ["Security and Authentication Layer"]
        JwtFilter["JwtAuthenticationFilter\n(Extracts and Validates Bearer Token)"]
        SecurityCtx["SecurityContextHolder\n(Stores Authenticated User ID and Email)"]
    end

    subgraph ControllerLayer ["Controller Layer"]
        AuthCtrl["AuthController\n(/api/v1/auth/**)"]
        AppCtrl["JobApplicationController\n(/api/v1/applications/**)"]
        AnalyticsCtrl["AnalyticsController\n(/api/v1/analytics)"]
    end

    subgraph ServiceLayer ["Business Logic and Domain"]
        AuthService["AuthService\n(BCrypt Hashing, Token Generation)"]
        AppService["JobApplicationService\n(User Scoping and Pipeline Validation)"]
        AnalyticsService["AnalyticsService\n(Metrics, Response Rates, Projections)"]
        StateMachine["ApplicationStateMachine\n(Enum State Transition Validator)"]
    end

    subgraph PersistenceLayer ["Persistence Layer"]
        UserRepo[("UserRepository\n(users table)")]
        AppRepo[("JobApplicationRepository\n(job_applications table)")]
        MySQL[("MySQL 8.0 Database\n(Flyway V1 and V2 Migrations)")]
    end

    Client -->|Public Static Files or Auth| AuthCtrl
    Client -->|Authenticated Request with Bearer Token| JwtFilter
    JwtFilter -->|Populates| SecurityCtx
    JwtFilter -->|Routes Request| AppCtrl
    JwtFilter -->|Routes Request| AnalyticsCtrl
    
    AuthCtrl --> AuthService
    AuthService --> UserRepo
    AuthService -->|Issues JWT| Client
    
    AppCtrl --> AppService
    AppService --> SecurityCtx
    AppService --> StateMachine
    AppService --> AppRepo
    
    AnalyticsCtrl --> AnalyticsService
    AnalyticsService --> SecurityCtx
    AnalyticsService --> AppRepo
    
    UserRepo --> MySQL
    AppRepo --> MySQL
```

---

## 2. State Machine Pipeline

HuntLog enforces hiring lifecycle rules using a deterministic finite state machine to prevent illegal status transitions (such as jumping directly from `APPLIED` to `OFFER`).

```mermaid
stateDiagram-v2
    [*] --> APPLIED : Application Created
    
    APPLIED --> SCREENING : Screening Call
    APPLIED --> REJECTED : Direct Rejection
    APPLIED --> WITHDRAWN : Candidate Withdraws

    SCREENING --> INTERVIEW : Passed Screening
    SCREENING --> REJECTED : Failed Screening
    SCREENING --> WITHDRAWN : Candidate Withdraws

    INTERVIEW --> OFFER : Passed Interviews
    INTERVIEW --> REJECTED : Failed Interview
    INTERVIEW --> WITHDRAWN : Candidate Withdraws

    OFFER --> ACCEPTED : Accepted Offer
    OFFER --> REJECTED : Offer Rescinded
    OFFER --> WITHDRAWN : Declined / Withdrawn

    ACCEPTED --> [*] : Terminal State
    REJECTED --> [*] : Terminal State
    WITHDRAWN --> [*] : Terminal State
```

### Transition Matrix

| Current Status | Allowed Next Statuses | Terminal State |
|---|---|---|
| `APPLIED` | `SCREENING`, `REJECTED`, `WITHDRAWN` | No |
| `SCREENING` | `INTERVIEW`, `REJECTED`, `WITHDRAWN` | No |
| `INTERVIEW` | `OFFER`, `REJECTED`, `WITHDRAWN` | No |
| `OFFER` | `ACCEPTED`, `REJECTED`, `WITHDRAWN` | No |
| `ACCEPTED` | *None* | Yes |
| `REJECTED` | *None* | Yes |
| `WITHDRAWN` | *None* | Yes |

### Implementation Detail
The state machine is implemented via an immutable `Map<ApplicationStatus, Set<ApplicationStatus>>` in `ApplicationStateMachine.java`. Lookups operate in constant time O(1).

Every API response embeds `allowedNextStatuses: [...]`, enabling client frontends to dynamically render next actions without duplicating transition rules in JavaScript.

---

## 3. Frontend Architecture

The frontend is implemented as a lightweight Single Page Application (SPA) without third-party frameworks:
- **Location**: `src/main/resources/static/` (`index.html`, `styles.css`, `app.js`).
- **Routing**: Hash-based routing (`#login`, `#register`, `#dashboard`) with immediate redirection if unauthenticated.
- **State Management**: Lightweight client-side application caching for real-time search and filter without redundant network queries.
- **Security**: JWT stored in `localStorage` for simple client persistence, sent via standard `Authorization: Bearer <token>` headers.

---

## 4. Analytics and Metrics Pipeline

The `AnalyticsService` executes user-scoped aggregate queries to compute real-time metrics:

- **Database Aggregation**: Utilizes Spring Data JPA projections (`CompanyCount`) to calculate top company distribution in MySQL without loading entire entity collections into memory.
- **Response Rate Formula**:
  $$\text{Response Rate} = \frac{\text{Total} - \text{APPLIED} - \text{WITHDRAWN}}{\text{Total}} \times 100$$
- **Velocity Metrics**: Calculates average days from application submission to first status update for active candidates.

---

## 5. Security and Multi-Tenant Data Isolation

### Stateless Authentication
- **Algorithm**: HMAC-SHA512 (`HS512`) with 256+ bit secret keys.
- **Payload Claims**: `sub` (email), `userId` (Long), `name` (String), `iat` (issued at), `exp` (expiration).
- **Password Storage**: Encrypted with `BCryptPasswordEncoder` (salted, 10 rounds).

### Per-User Data Isolation
Every application record is associated with a `user_id` foreign key.

```mermaid
sequenceDiagram
    autonumber
    actor Alice as Alice (User ID 1)
    actor Bob as Bob (User ID 2)
    participant API as HuntLog REST API
    participant DB as MySQL Database

    Alice->>API: POST /api/v1/applications (Google SWE) + Alice JWT
    API->>DB: INSERT INTO job_applications (user_id=1, company='Google', status='APPLIED')
    DB-->>API: Created record (id = 42)
    API-->>Alice: 201 Created

    Bob->>API: GET /api/v1/applications/42 + Bob JWT
    API->>DB: SELECT * FROM job_applications WHERE id = 42 AND user_id = 2
    DB-->>API: 0 records found
    API-->>Bob: 404 Not Found (Information Leak Prevention)
```

Attempting to access or modify another user's application produces a `404 Not Found` rather than `403 Forbidden`, preventing unauthorized discovery of entity IDs.

---

## 6. Database Schema and Migrations

Database schema versioning is managed via Flyway migrations under `src/main/resources/db/migration`:

### Entity Relationship Diagram

```mermaid
erDiagram
    USERS ||--o{ JOB_APPLICATIONS : "owns"
    
    USERS {
        BIGINT id PK "AUTO_INCREMENT"
        VARCHAR(150) email "NOT NULL, UNIQUE"
        VARCHAR(255) password "NOT NULL (BCrypt hash)"
        VARCHAR(100) name "NOT NULL"
        DATETIME(6) created_at "NOT NULL"
    }

    JOB_APPLICATIONS {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT user_id FK "NOT NULL, INDEXED"
        VARCHAR(100) company "NOT NULL"
        VARCHAR(100) role "NOT NULL"
        VARCHAR(20) status "NOT NULL"
        DATE applied_date "NOT NULL"
        DATETIME(6) last_updated "NOT NULL"
        VARCHAR(500) job_url "NULLABLE"
        VARCHAR(100) location "NULLABLE"
        TEXT notes "NULLABLE"
    }
```

### Migrations Timeline
- `V1__create_job_applications_table.sql`: Creates initial `job_applications` table.
- `V2__create_users_and_link_applications.sql`: Creates `users` table, adds `user_id` foreign key constraint, and indexes `idx_job_applications_user_id`.

---

## 7. Global Exception and Error Handling

All controller errors are processed by `GlobalExceptionHandler` and returned in a standard RFC 7807 format:

| Exception | HTTP Status | Meaning |
|---|---|---|
| `DuplicateEmailException` | `409 Conflict` | Email is already registered |
| `BadCredentialsException` | `401 Unauthorized` | Invalid email or password |
| `ResourceNotFoundException` | `404 Not Found` | Application not found or belongs to another user |
| `InvalidStatusTransitionException` | `422 Unprocessable Entity` | Illegal state machine status transition |
| `MethodArgumentNotValidException` | `400 Bad Request` | DTO validation failure (e.g. invalid email) |
| `HttpMessageNotReadableException` | `400 Bad Request` | Malformed JSON in request body |

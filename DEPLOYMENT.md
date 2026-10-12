# HuntLog — Production Deployment Guide (Render + Neon)

This guide walks through deploying HuntLog to production for permanent, zero-cost hosting using Render for application compute, Neon for serverless PostgreSQL, UptimeRobot for 24/7 keep-alive monitoring, and Mailtrap or Gmail SMTP for email reminders.

---

## 1. Production Architecture Overview

HuntLog uses an architecture that separates the web server from the database to guarantee zero data loss and avoid cloud vendor lock-in or trial expirations:

- **Compute**: Render.com Free Web Service (Docker container built from source via multi-stage Dockerfile).
- **Database**: Neon.tech Serverless PostgreSQL (Persistent storage, no expiration, automated backups).
- **Keep-Alive**: UptimeRobot HTTP monitor pinging `/actuator/health` every 5-10 minutes to prevent Render idle spin-down.
- **Email Delivery**: SMTP via Mailtrap (development/testing) or Gmail App Password (production).
- **Cost**: $0.00 / month indefinitely.

Note: Earlier deployment on Railway was retired to avoid the 30-day trial limitation and credit expiration.

---

## 2. Prerequisites

Before beginning deployment, ensure you have:

1. A GitHub account with the `pbanakar/huntlog` repository.
2. A free account at [Neon.tech](https://neon.tech) (no credit card required).
3. A free account at [Render.com](https://render.com) (no credit card required).
4. A free account at [UptimeRobot.com](https://uptimerobot.com) (no credit card required).
5. SMTP credentials (Mailtrap Sandbox credentials or a 16-character Google App Password).

---

## 3. Step 1: Provision Neon PostgreSQL Database

1. Log in to [Neon.tech](https://neon.tech) using your GitHub account.
2. Click **Create Project**:
   - Project Name: `huntlog`
   - Region: Select a region close to your users (e.g. `AWS Asia Pacific 1 (Singapore)` or `AWS US East`).
   - PostgreSQL Version: `16` or newer.
3. Click the green **Connect** button on the project dashboard.
4. Select the **Spring Boot** or **Parameters only** option from the dropdown.
5. Note the following values:
   - **JDBC URL**: `jdbc:postgresql://<host>/neondb?sslmode=require`
   - **Database User**: `neondb_owner` (or configured user)
   - **Password**: Your database password

---

## 4. Step 2: Deploy Spring Boot Web Service on Render

1. Log in to [Render.com](https://render.com) using your GitHub account.
2. Click **New +** > **Web Service**.
3. Select **Build and deploy from a Git repository** and connect `pbanakar/huntlog`.
4. Configure the web service:
   - **Name**: `huntlog`
   - **Region**: Same region as your Neon database (e.g. `Singapore` or `US East`).
   - **Runtime**: `Docker`
   - **Instance Type**: `Free` ($0/month)
5. Scroll down to **Environment Variables** and enter the following keys and values:

| Variable Name | Value | Description |
|---|---|---|
| `PORT` | `8080` | Internal Spring Boot HTTP port |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<neon-host>/neondb?sslmode=require` | Neon JDBC connection string |
| `DB_USERNAME` | `<neon-username>` | Database username |
| `DB_PASSWORD` | `<neon-password>` | Database password |
| `JWT_SECRET` | `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970` | 256-bit secret key for signing tokens |
| `MAIL_HOST` | `sandbox.smtp.mailtrap.io` | SMTP host (`smtp.gmail.com` for Gmail) |
| `MAIL_PORT` | `587` | SMTP port with STARTTLS |
| `MAIL_USERNAME` | `<smtp-username>` | Mailtrap username or Gmail address |
| `MAIL_PASSWORD` | `<smtp-password>` | Mailtrap password or Google App Password |
| `MAIL_FROM` | `no-reply@huntlog.dev` | Sender address |
| `REMINDERS_ENABLED` | `true` | Enables background daily reminder scanner |
| `REMINDER_STALE_DAYS` | `7` | Days threshold to flag pending applications |
| `REMINDER_CRON` | `0 0 9 * * *` | Daily scan cron schedule |

6. Click **Create Web Service**.
7. Render will build the multi-stage Docker container using the Google Cloud Maven Central mirror and start the service.
8. Once deployment displays **Live**, note your public HTTPS URL (e.g. `https://huntlog-coga.onrender.com`).

---

## 5. Step 3: Configure 24/7 Keep-Alive via UptimeRobot

Render free tier instances automatically spin down after 15 minutes of inactivity. To keep the instance warm and eliminate cold-start delays:

1. Log in to [UptimeRobot.com](https://uptimerobot.com).
2. Click **Add New Monitor**.
3. Fill in the monitor settings:
   - **Monitor Type**: `HTTP(s)`
   - **Friendly Name**: `HuntLog Health`
   - **URL to monitor**: `https://<your-render-domain>.onrender.com/actuator/health`
   - **Monitoring Interval**: `5 minutes` or `10 minutes`
4. Click **Create Monitor**.

UptimeRobot will continuously ping the lightweight Actuator endpoint, keeping the container active 24/7 without charging any compute fees.

---

## 6. Verification and Health Checks

1. **Dashboard UI**:
   Open `https://<your-render-domain>.onrender.com` in your browser.
   - Register a new account.
   - Verify login, adding applications, and analytics cards.

2. **Health Check Endpoint**:
   ```bash
   curl https://<your-render-domain>.onrender.com/actuator/health
   ```
   Expected response:
   ```json
   {"status":"UP"}
   ```

3. **Swagger UI (Interactive API Documentation)**:
   Open `https://<your-render-domain>.onrender.com/swagger-ui.html` in your browser.
   - Verify all endpoints are grouped under Authentication, Job Applications, Analytics, and User Preferences.
   - Click Authorize, paste your JWT token, and test protected endpoints directly.

4. **Database Persistence**:
   Flyway migrations (`V1`, `V2`, `V3`) run automatically on startup against Neon PostgreSQL. Tables and records persist permanently across any container redeployments.


# HuntLog — Production Deployment Guide (Railway)

This guide walks through deploying HuntLog to Railway with managed PostgreSQL, automated HTTPS, environment variable configuration, and real Gmail SMTP for email reminders.

---

## 1. Prerequisites

Before beginning deployment, ensure you have:

1. A GitHub account with the `pbanakar/huntlog` repository pushed.
2. A free account at [Railway.app](https://railway.app).
3. A Google/Gmail account with 2-Factor Authentication (2FA) enabled (for email reminders).

---

## 2. Generating a Gmail App Password (for Production Reminders)

To send real reminder emails from your Gmail account without exposing your master account password:

1. Log in to your Google Account at https://myaccount.google.com/
2. In the left menu, select **Security**.
3. Under *How you sign in to Google*, ensure **2-Step Verification** is turned ON.
4. Search for **App passwords** in the top search bar (or go directly to https://myaccount.google.com/apppasswords).
5. Enter an app name (e.g. `HuntLog Production`) and click **Create**.
6. Google will generate a 16-character password (e.g. `abcd efgh ijkl mnop`).
7. Copy this password (without spaces). This will be your `MAIL_PASSWORD` in Railway.

---

## 3. Step-by-Step Railway Deployment

### Step 1: Create a New Project on Railway
1. Log in to [Railway](https://railway.app).
2. Click **+ New Project**.
3. Select **Deploy from GitHub repo**.
4. Choose `pbanakar/huntlog` from your repository list.

### Step 2: Provision Managed PostgreSQL Database
1. Inside your new Railway project canvas, click **+ Create** or **+ New Service**.
2. Select **Database** > **PostgreSQL**.
3. Railway automatically provisions a PostgreSQL 16 instance and generates an internal connection variable `DATABASE_URL`.

### Step 3: Configure Environment Variables
1. Click on the **huntlog** application service block.
2. Navigate to the **Variables** tab.
3. Click **+ New Variable** (or **Raw Editor**) and add the following variables:

| Variable Name | Value | Description |
|---|---|---|
| `DATABASE_URL` | `${{Postgres.DATABASE_URL}}` | Auto-linked to the Railway PostgreSQL database |
| `JWT_SECRET` | `your_generated_256_bit_random_secret_string` | Secret key for signing authentication tokens |
| `MAIL_HOST` | `smtp.gmail.com` | Production SMTP host |
| `MAIL_PORT` | `587` | SMTP port with STARTTLS |
| `MAIL_USERNAME` | `your.email@gmail.com` | Your Gmail address |
| `MAIL_PASSWORD` | `your_16_char_gmail_app_password` | App password generated in Section 2 |
| `REMINDERS_ENABLED` | `true` | Enables background daily reminder scanner |
| `REMINDER_STALE_DAYS` | `7` | Threshold in days to flag pending applications |
| `REMINDER_CRON` | `0 0 9 * * *` | Daily schedule at 9:00 AM UTC |
| `SPRING_PROFILES_ACTIVE` | `prod` | Activates production Spring profile |

> Note: Railway automatically maps `${{Postgres.DATABASE_URL}}` into `DATABASE_URL`, which Spring Boot reads directly.

### Step 4: Generate Public Domain & Deploy
1. In the **huntlog** service, navigate to **Settings** > **Networking** > **Public Networking**.
2. Click **Generate Domain** (e.g. `huntlog-production.up.railway.app`).
3. Railway will build the `Dockerfile`, run the Actuator health checks (`/actuator/health`), and serve the app over HTTPS.

---

## 4. Verification After Deployment

Once the build finishes and status displays **Active**:

1. Open your public Railway URL in a browser:
   `https://huntlog-production.up.railway.app`
2. **Register**: Create your production admin account.
3. **Add Applications**: Add an active application (e.g. Google, Microsoft).
4. **Inspect Analytics**: Confirm stats cards load from `/api/v1/analytics`.
5. **Health Check**: Verify `https://huntlog-production.up.railway.app/actuator/health` returns `{"status":"UP"}`.

---

## 5. Maintenance and Troubleshooting

### Database Migrations
Flyway runs automatically on container boot against the PostgreSQL database. All tables (`users`, `job_applications`) and indices are created cleanly on first launch.

### Viewing Logs
To view live runtime logs:
1. Open your project on Railway.
2. Click the **huntlog** service > **Deployments** > **View Logs**.
3. Watch scheduler scan logs:
   `Sent reminder to your.email@gmail.com for X stale applications`

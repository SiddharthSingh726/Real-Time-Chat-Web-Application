# Deploying PulseChat to Render.com (Free)

This guide deploys PulseChat publicly for **$0** using Render's free tier.

## What You Get

| Service     | Render Plan | Limit                                       |
| ----------- | ----------- | ------------------------------------------- |
| Web Service | Free        | Spins down after 15 min idle; 750 hrs/month |
| PostgreSQL  | Free        | 256 MB storage, 90 days then recreate       |
| Redis       | Free        | 25 MB memory                                |

> Free web services go to sleep after inactivity. The first request after sleep takes ~30 seconds to cold-start.

---

## Prerequisites

1. A **GitHub** (or GitLab) account
2. A **Render.com** account (sign up free at https://render.com)

---

## Step-by-Step Deployment

### 1. Push your code to GitHub

If your project is not on GitHub yet:

```bash
git init
git add .
git commit -m "Initial commit"
```

Create a new repository on GitHub, then:

```bash
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
git branch -M main
git push -u origin main
```

### 2. Deploy via Render Blueprint (Recommended — One Click)

1. Go to https://render.com and log in
2. Click **"New" → "Blueprint"**
3. Connect your GitHub repo
4. Render auto-detects the `render.yaml` file and shows 3 services:
   - **pulsechat** (Web Service)
   - **pulsechat-db** (PostgreSQL)
   - **pulsechat-redis** (Redis)
5. Click **"Apply"**
6. Wait for the build to complete (~3-5 minutes for first build)

### 3. Access Your App

After deployment, Render gives you a URL like:

```
https://pulsechat-xxxx.onrender.com
```

Open this URL — PulseChat is live!

---

## Manual Setup (Alternative)

If you prefer to set up services individually:

### Create PostgreSQL Database

1. Dashboard → **New → PostgreSQL**
2. Name: `pulsechat-db`, Plan: **Free**, Database: `chatapp`, User: `chat`
3. Note the **Internal Connection String**

### Create Redis Instance

1. Dashboard → **New → Redis**
2. Name: `pulsechat-redis`, Plan: **Free**
3. Note the **Internal URL**

### Create Web Service

1. Dashboard → **New → Web Service**
2. Connect your GitHub repo
3. Settings:
   - **Name**: `pulsechat`
   - **Runtime**: Docker
   - **Plan**: Free
   - **Health Check Path**: `/actuator/health`
4. Environment Variables:

| Variable                     | Value                                                        |
| ---------------------------- | ------------------------------------------------------------ |
| `SPRING_PROFILES_ACTIVE`     | `render`                                                     |
| `SPRING_DATASOURCE_URL`      | `jdbc:postgresql://HOST:5432/chatapp` (from DB Internal URL) |
| `SPRING_DATASOURCE_USERNAME` | `chat`                                                       |
| `SPRING_DATASOURCE_PASSWORD` | (from DB dashboard)                                          |
| `REDIS_URL`                  | (Redis Internal URL)                                         |
| `APP_SECURITY_MODE`          | `dev`                                                        |
| `APP_MESSAGING_PROVIDER`     | `local`                                                      |
| `APP_AI_ENABLED`             | `false`                                                      |

5. Click **"Create Web Service"**

---

## After Deployment

### Test It

1. Open your Render URL
2. Register two accounts in separate browser tabs
3. Create a conversation and start chatting in real-time!

### Enable AI (Optional)

Add these environment variables in Render dashboard:

- `APP_AI_ENABLED` = `true`
- `APP_AI_OPENAI_API_KEY` = `sk-your-key-here`

### Custom Domain (Optional)

1. In Render dashboard → your web service → **Settings → Custom Domains**
2. Add your domain and configure DNS as instructed
3. Update the `APP_WEBSOCKET_ALLOWED_ORIGIN_PATTERNS_0` env var to `https://yourdomain.com`

---

## Troubleshooting

| Issue                   | Solution                                                                  |
| ----------------------- | ------------------------------------------------------------------------- |
| App takes 30s to load   | Free tier cold-starts. Wait for it to wake up.                            |
| WebSocket errors        | Check the Render URL matches allowed origins in config.                   |
| DB errors after 90 days | Render free PostgreSQL expires. Create a new one and redeploy.            |
| Build fails             | Check Render logs. Ensure Dockerfile builds locally with `docker build .` |

### View Logs

Render Dashboard → Your Service → **Logs** tab

### Health Check

Visit `https://your-app.onrender.com/actuator/health` — should return `{"status":"UP"}`.

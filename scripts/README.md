# ROBOPILOT Development & Deployment Scripts

This directory contains helper scripts used during local development, internal release validation, and future deployment automation.

---

# Directory Structure

```
scripts/
├── README.md
├── aws_connect.sh
├── start-local.sh
├── start-internal.sh
├── start-ai-tunnel.sh
└── start-mqtt-tunnel.sh
```

---

# Prerequisites

Before using any script, ensure:

- AWS CLI is installed
- Session Manager Plugin is installed
- AWS SSO login is configured
- Docker (future deployment)
- Java 17
- Maven

---

# Environment Files

The backend uses environment files located at the repository root.

```
.env.example
.env.local
.env.internal
```

| File | Purpose |
|------|---------|
| `.env.example` | Template for required environment variables |
| `.env.local` | Local development configuration |
| `.env.internal` | Internal release validation configuration |

---

# Local Development Workflow

## Step 1 - AWS Login

The helper scripts automatically execute:

```bash
source scripts/aws_connect.sh -login
```

This authenticates the AWS SSO profile (`robopilot-dev`).

---

## Step 2 - Start AI Tunnel

Open Terminal 1.

```bash
./scripts/start-ai-tunnel.sh
```

This creates an SSM port-forward:

```
localhost:7879
        ↓
FPT AI Service
```

---

## Step 3 - Start MQTT Tunnel

Open Terminal 2.

```bash
./scripts/start-mqtt-tunnel.sh
```

This creates an SSM port-forward:

```
localhost:1883
        ↓
FPT EMQX Broker
```

---

## Step 4 - Start Backend (Local)

Open Terminal 3.

```bash
./scripts/start-local.sh
```

Uses:

```
.env.local
SPRING_PROFILES_ACTIVE=dev
```

Current connectivity:

```
Backend (Local)

↓

Local PostgreSQL

↓

Local Redis

↓

FPT MQTT (Tunnel)

↓

FPT AI Service (Tunnel)
```

---

# Internal Release Validation

Run:

```bash
./scripts/start-internal.sh
```

Uses:

```
.env.internal
SPRING_PROFILES_ACTIVE=prod
```

Current connectivity:

```
Backend (Local)

↓

FPT PostgreSQL

↓

FPT Redis

↓

FPT MQTT (Tunnel)

↓

FPT AI Service (Tunnel)
```

---

# Current Architecture

```
Developer Machine
        │
        │
        ├─────────────── PostgreSQL (Local / Internal)
        │
        ├─────────────── Redis (Local / Internal)
        │
        ├── SSM Tunnel ─► EMQX MQTT
        │
        ├── SSM Tunnel ─► AI Service
        │
        └─────────────── MediaMTX / CloudFront
```

---

# Future Roadmap

Current Phase

- Local Development
- Internal Release Validation

Next

- Docker Compose Deployment
- Release Tag Automation

Later

- GitHub Actions CI/CD
- ECS Deployment
- AWS Secrets Manager
- Complete Service Separation

---

# Notes

Do not commit:

```
.env.local
.env.internal
```

Only commit:

```
.env.example
```

Sensitive values such as:

- Database passwords
- JWT secret
- Slack Bot Token
- MQTT password

must remain outside Git.
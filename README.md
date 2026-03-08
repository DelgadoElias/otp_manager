# OTP Manager

A production-ready **TOTP (Time-based One-Time Password) microservice** built with Spring Boot and Kotlin. Designed as a plug-and-play MFA backend that any application can integrate with — deploy in minutes on Railway, AWS ECS, or locally via Docker.

## Features

- **Secret generation** — creates TOTP secrets and returns a QR code (base64 PNG) ready to scan with Google Authenticator, Authy, or any TOTP-compatible app
- **OTP validation** — validates 6-digit codes against stored secrets
- **Audit log** — every validation attempt is recorded with timestamp and result
- **Rate limiting** — per-IP token bucket (configurable requests/minute and requests/hour)
- **CORS** — configurable allowed origins for cross-origin frontends
- **Swagger UI** — interactive API documentation at `/swagger-ui.html`
- **Flyway migrations** — schema versioning, no `ddl-auto=update` in production
- **Custom error pages** — clean 404/500 pages, no Spring whitelabel
- **Health check** — `/actuator/health` endpoint for load balancers and orchestrators

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 1.9 |
| Framework | Spring Boot 3.2 |
| Database | PostgreSQL (any provider) |
| Migrations | Flyway |
| ORM | Hibernate / Spring Data JPA |
| TOTP | Google Authenticator library |
| QR Code | QRGen (ZXing-based) |
| Rate Limiting | Bucket4j + Caffeine |
| Runtime | JVM 17 (eclipse-temurin) |

## API Reference

### `POST /otp/generate-secret`

Generates a new TOTP secret for a user. Fails with `409` if the user already has one.

**Query params:** `username` (string, required)

```json
// Response 200
{
  "secret": "JBSWY3DPEHPK3PXP",
  "qrCode": "data:image/png;base64,..."
}
```

### `POST /otp/validate`

Validates a 6-digit TOTP code.

**Query params:** `username` (string), `otp` (integer)

```json
// Response 200
{
  "valid": true
}
```

Full interactive documentation: `/swagger-ui.html` · OpenAPI JSON: `/api-docs`

## Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `DATABASE_URL` | ✅ | — | JDBC URL (e.g. `jdbc:postgresql://host:5432/db`) |
| `DATABASE_USERNAME` | ✅ | — | Database user |
| `DATABASE_PASSWORD` | ✅ | — | Database password |
| `OTP_APP_NAME` | | `MyApp` | Issuer name shown in the authenticator app |
| `OTP_SECRET_EXPIRATION_DAYS` | | `30` | Days until a secret expires |
| `CORS_ALLOWED_ORIGINS` | | `http://localhost:4321` | Comma-separated list of allowed origins |
| `RATE_LIMIT_PER_MINUTE` | | `5` | Max requests per IP per minute |
| `RATE_LIMIT_PER_HOUR` | | `20` | Max requests per IP per hour |

Copy `.env.example` to `.env` for local development.

---

## Running Locally

### With Docker Compose (recommended)

Requires Docker Desktop or Docker Engine with Compose v2.

```bash
git clone https://github.com/DelgadoElias/otp_manager.git
cd otp_manager
cp .env.example .env          # edit .env with your values
docker compose up --build
```

The app starts at `http://localhost:8080`.

### With Gradle directly

Requires JDK 17+ and a running PostgreSQL instance.

```bash
cp .env.example .env
export $(cat .env | xargs)
./gradlew bootRun
```

---

## Deploying on Railway

Railway auto-detects the `Dockerfile` via `railway.toml`. No extra config files needed.

**1. Create a new project**

```
railway init
```

Or create it from the Railway dashboard and connect your GitHub repo.

**2. Set up a database**

You can use Railway's PostgreSQL plugin (add it from the dashboard) or an external provider like [Neon](https://neon.tech).

- **Railway plugin**: use `${{PGHOST}}`, `${{PGPORT}}`, `${{PGDATABASE}}`, `${{PGUSER}}`, `${{PGPASSWORD}}` to build the JDBC URL
- **Neon / external**: use the connection string directly (without `channel_binding=require` — the JDBC driver does not support it)

**3. Add environment variables**

In Railway → your service → **Variables**:

```
DATABASE_URL=jdbc:postgresql://<host>:<port>/<database>?sslmode=require
DATABASE_USERNAME=<user>
DATABASE_PASSWORD=<password>
OTP_APP_NAME=YourAppName
CORS_ALLOWED_ORIGINS=https://your-frontend.com
```

**4. Deploy**

```bash
railway up
```

Or push to the connected branch — Railway redeploys automatically.

The health check is configured at `/actuator/health` in `railway.toml`.

---

## Deploying on AWS ECS

### Prerequisites

- AWS CLI configured
- An ECR repository created
- A PostgreSQL instance (RDS, Aurora Serverless, or Neon)

### 1. Build and push the image to ECR

```bash
# Authenticate
aws ecr get-login-password --region us-east-1 \
  | docker login --username AWS --password-stdin \
    <account-id>.dkr.ecr.us-east-1.amazonaws.com

# Build
docker build -t otp-manager .

# Tag and push
docker tag otp-manager:latest \
  <account-id>.dkr.ecr.us-east-1.amazonaws.com/otp-manager:latest

docker push \
  <account-id>.dkr.ecr.us-east-1.amazonaws.com/otp-manager:latest
```

### 2. Create an ECS Task Definition

Create a file `task-definition.json`:

```json
{
  "family": "otp-manager",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::<account-id>:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "otp-manager",
      "image": "<account-id>.dkr.ecr.us-east-1.amazonaws.com/otp-manager:latest",
      "portMappings": [{ "containerPort": 8080, "protocol": "tcp" }],
      "environment": [
        { "name": "OTP_APP_NAME", "value": "YourAppName" },
        { "name": "CORS_ALLOWED_ORIGINS", "value": "https://your-frontend.com" },
        { "name": "RATE_LIMIT_PER_MINUTE", "value": "10" },
        { "name": "RATE_LIMIT_PER_HOUR", "value": "50" }
      ],
      "secrets": [
        { "name": "DATABASE_URL",      "valueFrom": "arn:aws:ssm:...:DATABASE_URL" },
        { "name": "DATABASE_USERNAME", "valueFrom": "arn:aws:ssm:...:DATABASE_USERNAME" },
        { "name": "DATABASE_PASSWORD", "valueFrom": "arn:aws:ssm:...:DATABASE_PASSWORD" }
      ],
      "healthCheck": {
        "command": ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"],
        "interval": 30,
        "timeout": 10,
        "retries": 3,
        "startPeriod": 60
      },
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/otp-manager",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

> **Security**: store sensitive values (database credentials) in **AWS SSM Parameter Store** or **Secrets Manager**, not as plain `environment` entries.

### 3. Register and deploy

```bash
# Register task definition
aws ecs register-task-definition \
  --cli-input-json file://task-definition.json

# Create or update the service
aws ecs create-service \
  --cluster your-cluster \
  --service-name otp-manager \
  --task-definition otp-manager \
  --desired-count 1 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx],securityGroups=[sg-xxx],assignPublicIp=ENABLED}"
```

### Recommended ECS resources

| Environment | CPU | Memory | Notes |
|---|---|---|---|
| Development | 256 | 512 MB | May be slow on first request (lazy init) |
| Production | 512 | 1024 MB | Comfortable headroom for JVM + connection pool |

---

## Live Demo

A test UI is available at **[https://delgadoelias.github.io/otp_manager](https://delgadoelias.github.io/otp_manager)** — enter your deployed API URL, register an email, scan the QR code, and verify your first TOTP code end-to-end.

---

## Security Recommendation — Do Not Expose Publicly

> **This microservice is intended to be called internally by a trusted Auth Service, not directly by end users or browsers.**

In a production architecture, the OTP Manager should sit behind an Auth Service that:

- Verifies the caller's identity before proxying requests (active session, JWT, etc.)
- Applies business policies (e.g. can this user enroll MFA? do they already have one?)
- Is the **only** service that knows the OTP Manager's internal URL

```
[Client / Frontend]
        ↓
[Auth Service]         ← public-facing, handles identity
        ↓  private network / VPC
[OTP Manager]          ← never exposed to the internet
```

**Railway**: enable Private Networking on the OTP Manager service so it only accepts traffic from within the project network.

**AWS ECS**: place the OTP Manager in a private subnet with no internet gateway. Restrict the security group to only allow inbound traffic from the Auth Service task.

When running behind an internal Auth Service, the per-IP rate limiting and CORS configuration in this service become redundant and can be disabled.

A future improvement is to add an internal API Key (`X-Internal-Key` header) so the OTP Manager rejects any request that does not come from the expected Auth Service, as a defense-in-depth measure.

---

## License

MIT

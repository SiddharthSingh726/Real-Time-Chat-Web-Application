# Realtime Chat Web Application (Java + AI)

Production-oriented starter for a real-time chat platform with:

- REST + WebSocket/STOMP gateway
- Durable message persistence (PostgreSQL + Flyway)
- Redis-backed presence, membership caching, and rate limiting
- Broker-backed event transport (Kafka or RabbitMQ) for horizontal gateway fanout
- Async AI worker pipeline (Java-based integration, OpenAI-compatible)
- Metrics via Actuator + Micrometer + Prometheus endpoint
- Docker + Compose + CI workflow

## Stack

- Java 21
- Spring Boot 3
- Spring WebSocket (STOMP)
- Spring Security (dev mode + OAuth2/JWT mode)
- PostgreSQL
- Redis
- Kafka / RabbitMQ
- Flyway

## Quick Start

### 1) Run dependencies

```bash
docker compose up -d postgres redis kafka rabbitmq
```

### 2) Start app

```bash
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080) for the dev STOMP chat console.

## Local Run Without Docker

You can run the app directly on your machine if PostgreSQL/Redis (and optional Kafka/RabbitMQ) are installed locally.

### PowerShell helper script

Use `run-local.ps1`:

```powershell
# Local mode (no Kafka/RabbitMQ required)
powershell -ExecutionPolicy Bypass -File .\run-local.ps1 -MessagingProvider local
```

```powershell
# Kafka mode
powershell -ExecutionPolicy Bypass -File .\run-local.ps1 -MessagingProvider kafka -KafkaBootstrapServers localhost:9092
```

```powershell
# RabbitMQ mode
powershell -ExecutionPolicy Bypass -File .\run-local.ps1 -MessagingProvider rabbitmq -RabbitHost localhost -RabbitPort 5672
```

### 3) Create conversation (dev auth)

Use a local auth token in dev mode.

```bash
TOKEN="<token returned by /api/auth/login or /api/auth/register>"

curl -X POST http://localhost:8080/api/conversations \
  -H "Content-Type: application/json" \
  -H "X-Auth-Token: ${TOKEN}" \
  -d '{
    "title": "Team Room",
    "type": "GROUP",
    "memberIds": ["user-2", "user-3"]
  }'
```

### 4) Connect WebSocket/STOMP

- Endpoint: `/ws`
- CONNECT header in dev mode: `x-auth-token: <token>`
- Send destination: `/app/chat.send`
- Conversation stream: `/topic/conversations.{conversationId}`
- User ACK queue: `/user/queue/acks`
- User error queue: `/user/queue/errors`

## Message Lifecycle

1. Client sends `ChatSendRequest` over STOMP.
2. Server authenticates session, rate-limits, validates membership.
3. Message is persisted in PostgreSQL with idempotency key (`clientMessageId`).
4. `ChatMessageEvent` is published to an internal app event bus (for AI triggers) and an external broker.
5. Gateway consumers read from Kafka/RabbitMQ and fan out to `/topic/conversations.{id}`.
6. Sender receives ACK on `/user/queue/acks`.
7. Optional AI worker listens to internal events and posts AI messages as regular chat events.

## Broker Modes

Configure the broker mode with `app.messaging.provider`.

```yaml
app:
  messaging:
    provider: kafka # local | kafka | rabbitmq
```

### Kafka mode

```yaml
app:
  messaging:
    provider: kafka
    kafka:
      topic: chat.messages
      gateway-group-id: ${spring.application.name}-${random.uuid}
```

- Producer key: `conversationId` to preserve per-conversation ordering semantics.
- Each gateway uses a unique consumer group id so all gateway instances receive events.

### RabbitMQ mode

```yaml
app:
  messaging:
    provider: rabbitmq
    rabbit:
      exchange: chat.messages.exchange
      gateway-queue-prefix: chat.gateway
```

- Uses a fanout exchange.
- Each gateway instance binds its own durable queue (`<prefix>.<hostname>`) so every gateway gets every event.

## Security Modes

### Dev mode (default)

`application.yml`:

```yaml
app:
  security:
    mode: dev
    allow-header-impersonation: true
```

- REST auth via `X-Auth-Token`.
- STOMP auth via `x-auth-token` CONNECT header.
- Header impersonation can stay enabled locally for testing, but it should be disabled for any public deployment.

### OAuth2 mode (production)

`application-prod.yml`:

```yaml
app:
  security:
    mode: oauth2
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://your-idp/realms/chat
```

- REST expects JWT bearer token.
- STOMP CONNECT expects `Authorization: Bearer <token>` header.

## AI Integration

AI worker is disabled by default.

Enable with env/properties:

```yaml
app:
  ai:
    enabled: true
    provider: openai
    trigger-prefix: "@ai"
    openai:
      api-key: ${OPENAI_API_KEY}
      model: gpt-4.1-mini
```

When a message starts with `@ai`, worker calls provider asynchronously and persists AI reply as a normal chat message.

## Observability

- Health: `GET /actuator/health`
- Prometheus metrics: `GET /actuator/prometheus`

Key metrics emitted:

- `chat.messages.delivered`
- `chat.messages.rejected`
- `chat.ai.requests`
- `chat.ai.failures`
- `chat.ai.generated_messages`

## Running Full Stack in Containers

```bash
docker compose up --build
```

By default, the containerized app runs with `APP_MESSAGING_PROVIDER=kafka`.

## Tests

```bash
mvn test
```

Added integration coverage:

- `KafkaBrokerWebSocketIntegrationTest`: Postgres + Redis + Kafka + real STOMP WebSocket E2E.
- `RabbitMqBrokerWebSocketIntegrationTest`: Postgres + Redis + RabbitMQ + real STOMP WebSocket E2E.
- Both verify broker-backed cross-user fanout and sender ACK.

## Production Hardening Checklist

- Switch to OAuth2/JWT mode and enforce trusted origins.
- Add dead-letter handling for async workers.
- Add integration/load/security tests (Testcontainers, k6/Gatling, ZAP).
- Set SLOs and alerting for ACK latency, error rate, broker lag.

## Public Deployment (VPS + Domain + HTTPS)

This repo now includes `compose.public.yaml` and `Caddyfile` to publish the app with TLS.

### 1) Prerequisites

- A Linux VPS with Docker + Docker Compose installed.
- A domain/subdomain (for example `chat.example.com`) pointing to your VPS public IP.
- Ports `80` and `443` open in firewall/security group.

### 2) Copy project to server

```bash
git clone <your-repo-url>
cd realtime-chat
```

### 3) Create public env file

```bash
cp .env.public.example .env.public
```

Edit `.env.public`:

- Set `APP_DOMAIN` to your real domain.
- Set strong passwords.
- Use `APP_MESSAGING_PROVIDER=local` for a single VPS.
- Switch to `kafka` or `rabbitmq` only when you run multiple app instances behind a load balancer.
- Keep `APP_SECURITY_MODE=dev` if you want to use the built-in local account system.
- Keep `APP_SECURITY_ALLOW_HEADER_IMPERSONATION=false` so users cannot spoof another user ID.

### 4) Start public stack

```bash
docker compose --env-file .env.public -f compose.public.yaml up -d --build postgres redis app caddy
```

If you later scale to multiple app instances, also start your broker service:

```bash
# Kafka
docker compose --env-file .env.public -f compose.public.yaml up -d kafka

# RabbitMQ
docker compose --env-file .env.public -f compose.public.yaml up -d rabbitmq
```

### 5) Verify

- App: `https://<APP_DOMAIN>`
- Health: `https://<APP_DOMAIN>/actuator/health`

### 6) Create first user

```bash
curl -X POST https://<APP_DOMAIN>/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "displayName":"Admin",
    "userId":"admin",
    "password":"change-this-password"
  }'
```

Copy the returned `authToken`.

### 7) Create first conversation

```bash
curl -X POST https://<APP_DOMAIN>/api/conversations \
  -H "Content-Type: application/json" \
  -H "X-Auth-Token: <authToken>" \
  -d '{
    "title":"Public Room",
    "type":"GROUP",
    "memberIds":["user-2","user-3"]
  }'
```

Users can now open `https://<APP_DOMAIN>`, create their own accounts, sign in, and use the web UI.

### Important security note

If `APP_SECURITY_ALLOW_HEADER_IMPERSONATION=false`, the built-in local account system is acceptable for a small self-hosted public deployment.
For a larger or multi-tenant deployment, switch to OAuth2 (`APP_SECURITY_MODE=oauth2`) and integrate a real identity provider.

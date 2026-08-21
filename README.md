# 🚀 Webhook Delivery Service

A reliable, multi‑tenant webhook delivery engine I built with Spring Boot and PostgreSQL. It ingests events from internal systems, fans them out to tenant‑configured endpoints, and retries failed deliveries with exponential backoff until they succeed or get dead‑lettered.

---

## Features

| Feature | Description |
|---------|-------------|
| 🔐 **Multi-Tenant** | Full tenant isolation using `X-Tenant-Id` header |
| 📥 **Idempotent Ingestion** | Duplicate `eventId` submissions are rejected via DB constraint |
| ⚡ **Async Delivery** | Events are processed asynchronously – `202 Accepted` response |
| 🔒 **SKIP LOCKED Worker** | Prevents duplicate delivery processing across concurrent workers |
| 🔑 **HMAC-SHA256 Signing** | Secure webhooks with per-endpoint secrets |
| 🔄 **Exponential Backoff** | Retries with jitter – `2^attempt * (0.8 + 0.4 * random)` |
| 💀 **Dead-Lettering** | After 8 failed attempts, delivery stops automatically |
| 📊 **Visibility APIs** | Track delivery history with status codes and error snippets |
| 🔁 **Manual Redrive** | Retry dead-lettered deliveries on demand |

---

## Quick Start

```bash
# Clone the repository
git clone https://github.com/0818Abhishek/webhook-delivery.git

# Navigate to project
cd webhook-delivery

# Start the application (PostgreSQL + Spring Boot)
docker compose up --build
```

The service will be available at: `http://localhost:8080`

> **Note:** All API calls require the header `X-Tenant-Id: 1` for testing.

---

## What I Built

### 1. Ingestion & Idempotency

`POST /api/v1/events` accepts an `eventId`, `type`, and `payload`.

I used a **unique database constraint** on `(tenant_id, event_id_external)` to guarantee idempotency. If the same `eventId` is sent again, the system returns the existing event's status without creating duplicates.

### 2. Fan-out

For each active endpoint that subscribes to the event's type, the system creates a `PENDING` delivery record. The response to the client is `202 Accepted` and the actual delivery happens asynchronously.

### 3. Delivery Worker

A scheduled job runs **every second**. It claims **one pending delivery per tenant** using PostgreSQL's `SELECT ... FOR UPDATE SKIP LOCKED`:

```sql
SELECT * FROM deliveries 
WHERE tenant_id = :tenantId 
  AND status = 'PENDING' 
  AND next_attempt_at <= NOW() 
ORDER BY next_attempt_at ASC 
LIMIT 1 
FOR UPDATE SKIP LOCKED
```

I chose this approach because it's **simple, database-native**, and avoids the complexity of external locking (like Redis) while still providing strong concurrency guarantees.

### 4. Retry & Dead-Lettering

On failure (non-2xx response or timeout), the worker increments the attempt count and schedules the next retry using **exponential backoff with jitter**:

```
delay = 2^attempt * (0.8 + 0.4 * random) seconds
```

This spreads out retries and prevents a thundering herd when many endpoints fail at the same time.

After **8 failed attempts**, the delivery is marked `DEAD_LETTERED` and stops retrying automatically.

### 5. Signing & Security

Each endpoint has a **unique HMAC-SHA256 secret** generated at registration time.

Outgoing webhooks include:
- `X-Webhook-Signature` – HMAC-SHA256 of payload + timestamp
- `X-Webhook-Timestamp` – Current epoch seconds

Secrets are stored in the database and **never hardcoded or logged**.

### 6. Visibility APIs

I exposed two endpoints to check delivery history:

| Endpoint | Description |
|----------|-------------|
| `GET /api/v1/events/{id}/deliveries` | All attempts for a given event |
| `GET /api/v1/endpoints/{id}/deliveries` | All deliveries for a given endpoint |

Each record includes the HTTP status code and a truncated response snippet (to avoid logging sensitive data).

### 7. Manual Redrive

I added `POST /api/v1/deliveries/{id}/redrive` to manually re-queue a dead-lettered delivery. It resets the status to `PENDING` and updates `nextAttemptAt` so the worker can try again.

---

## API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/endpoints` | Register a new webhook endpoint |
| `GET` | `/api/v1/endpoints` | List all active endpoints for the tenant |
| `DELETE` | `/api/v1/endpoints/{id}` | Soft-delete an endpoint (stops future deliveries) |
| `POST` | `/api/v1/events` | Ingest an event (idempotent) |
| `GET` | `/api/v1/events/{id}/deliveries` | View all deliveries for an event |
| `GET` | `/api/v1/endpoints/{id}/deliveries` | View all deliveries for an endpoint |
| `POST` | `/api/v1/deliveries/{id}/redrive` | Retry a dead-lettered delivery |

> **All endpoints require the `X-Tenant-Id` header for tenant isolation.**

---

## How I Guarantee At-Least-Once Delivery

| Mechanism | How It Works |
|-----------|--------------|
| **Idempotent ingestion** | Duplicate `eventId` submissions are ignored, so no extra deliveries are created |
| **SKIP LOCKED claiming** | Exactly one worker claims each delivery at a time |
| **Retries until success or dead-letter** | The worker keeps trying (with backoff) until the endpoint returns `2xx` or the attempt limit is reached |

> ⚠️ **Theoretical duplicate scenario:** If a worker crashes *after* sending the webhook but *before* updating the status, the delivery remains `PENDING` and will be retried – acceptable for at-least-once semantics.

---

## Known Limitations

| Limitation | What I'd Do With More Time |
|------------|---------------------------|
| **Redrive `attemptCount` reset** | The manual redrive sets status to `PENDING` and updates `nextAttemptAt`, but due to transaction timing the `attemptCount` and `lastResponseCode` are not reset. I would use a native update query with `RETURNING` to ensure atomic reset. |
| **No circuit breaker** | I would add Resilience4j to pause retries for consistently failing endpoints. |
| **No tests yet** | I would add Testcontainers integration tests to cover concurrency, idempotency, and tenant isolation. |
| **No correlation IDs** | I would add a `X-Correlation-Id` filter for end-to-end tracing. |

---

## What I Would Do with One More Weeks

- [ ] Implement circuit breakers per endpoint (Resilience4j)
- [ ] Add strict FIFO delivery ordering per endpoint (optional mode)
- [ ] Build a replay tool to resend all events of a given type within a date range
- [ ] Create a lightweight admin dashboard showing delivery success rates per tenant/endpoint
- [ ] Write full Testcontainers test suite and achieve 60%+ coverage

---

## One Thing That Surprised Me

I was surprised how well `FOR UPDATE SKIP LOCKED` works for distributed queue processing – I didn't need Redis or any external broker to prevent duplicate processing. It made the system simpler, cheaper, and more reliable.

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| **Language** | Java 21 LTS |
| **Framework** | Spring Boot 4.x |
| **Database** | PostgreSQL 16 |
| **Migrations** | Flyway |
| **Container** | Docker Compose |
| **Build Tool** | Maven |

---

## Project Structure

```
webhook-delivery/
├── src/main/java/com/webhook/delivery/
│   ├── controller/      # REST endpoints
│   ├── service/         # Business logic + interfaces
│   ├── serviceimpl/     # Service implementations
│   ├── repository/      # Spring Data JPA (SKIP LOCKED)
│   ├── entity/          # JPA entities
│   ├── dto/             # Request/Response DTOs
│   ├── filter/          # Tenant filter (X-Tenant-Id)
│   └── context/         # ThreadLocal tenant context
├── src/main/resources/
│   ├── db/migration/    # Flyway migrations (V1, V2)
│   └── application.yml  # Spring Boot config
├── docker-compose.yml   # PostgreSQL + App
├── pom.xml              # Maven dependencies
└── README.md            # This file
```

---

## Author

**Abhishek Kishor**  
Java Developer | 3 YOE  
[GitHub](https://github.com/0818Abhishek) • [LinkedIn](https://www.linkedin.com/in/abhishek-kishor0818/)
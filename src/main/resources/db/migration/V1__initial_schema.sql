-- V1__initial_schema.sql

-- 1. Tenants table
CREATE TABLE IF NOT EXISTS tenants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Endpoints table
CREATE TABLE IF NOT EXISTS endpoints (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    url VARCHAR(500) NOT NULL,
    secret VARCHAR(255) NOT NULL, -- HMAC signing secret
    subscribed_event_types TEXT[], -- Array of event types, e.g. {'invoice.paid', 'user.created'}
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, DISABLED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_endpoints_tenant_id ON endpoints(tenant_id);

-- 3. Events table (ingested events)
CREATE TABLE IF NOT EXISTS events (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    event_id_external VARCHAR(255) NOT NULL, -- Client-provided idempotency key
    type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- Unique constraint for idempotency (tenant + external id)
    CONSTRAINT uk_event_tenant_external UNIQUE (tenant_id, event_id_external)
);

CREATE INDEX idx_events_tenant_id ON events(tenant_id);

-- 4. Deliveries table (the heart of the system)
CREATE TABLE IF NOT EXISTS deliveries (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    endpoint_id BIGINT NOT NULL REFERENCES endpoints(id) ON DELETE CASCADE,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, PROCESSING, SUCCESS, FAILED, DEAD_LETTERED
    attempt_count INT DEFAULT 0,
    next_attempt_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    locked_by VARCHAR(100), -- optional: hostname or worker id
    locked_until TIMESTAMP,
    last_response_code INT,
    last_response_snippet TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- CRITICAL INDEX: Supports the SKIP LOCKED query
CREATE INDEX idx_deliveries_claim ON deliveries (tenant_id, status, next_attempt_at)
WHERE status = 'PENDING' OR status = 'PROCESSING';

-- 5. Delivery attempts history (for observability)
CREATE TABLE IF NOT EXISTS delivery_attempts (
    id BIGSERIAL PRIMARY KEY,
    delivery_id BIGINT NOT NULL REFERENCES deliveries(id) ON DELETE CASCADE,
    attempt_number INT NOT NULL,
    response_code INT,
    latency_ms INT,
    error TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_attempts_delivery_id ON delivery_attempts(delivery_id);
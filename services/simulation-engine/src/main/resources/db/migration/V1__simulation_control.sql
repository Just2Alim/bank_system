CREATE TABLE simulation_state (
    singleton BOOLEAN PRIMARY KEY DEFAULT TRUE CHECK (singleton),
    status VARCHAR(24) NOT NULL,
    profile VARCHAR(16) NOT NULL,
    seed BIGINT NOT NULL,
    speed INTEGER NOT NULL CHECK (speed IN (1, 10, 60, 360)),
    simulation_time TIMESTAMPTZ NOT NULL,
    wall_anchor TIMESTAMPTZ NOT NULL,
    revision BIGINT NOT NULL CHECK (revision >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO simulation_state (
    singleton, status, profile, seed, speed, simulation_time, wall_anchor, revision
) VALUES (
    TRUE, 'STOPPED', 'DEMO', 20260904, 1, '2026-09-01T00:00:00Z', CURRENT_TIMESTAMP, 0
);

CREATE TABLE outbox_event (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    event_version INTEGER NOT NULL CHECK (event_version > 0),
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id VARCHAR(128) NOT NULL,
    aggregate_version BIGINT NOT NULL CHECK (aggregate_version > 0),
    occurred_at TIMESTAMPTZ NOT NULL,
    simulation_time TIMESTAMPTZ NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    available_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    last_error_code VARCHAR(80),
    UNIQUE (aggregate_id, aggregate_version)
);

CREATE INDEX outbox_event_pending_idx
    ON outbox_event (available_at, occurred_at)
    WHERE status = 'PENDING';

CREATE TABLE simulation_fault (
    fault_id VARCHAR(128) PRIMARY KEY CHECK (fault_id LIKE 'SIM-FLT-NPP-%'),
    fault_type VARCHAR(48) NOT NULL,
    target_service VARCHAR(80) NOT NULL,
    activated_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL CHECK (expires_at > activated_at),
    cleared_at TIMESTAMPTZ,
    activated_by VARCHAR(128) NOT NULL,
    revision BIGINT NOT NULL DEFAULT 1 CHECK (revision > 0)
);

CREATE TABLE simulation_audit_event (
    audit_id UUID PRIMARY KEY,
    action VARCHAR(80) NOT NULL,
    actor_id VARCHAR(128) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    previous_revision BIGINT,
    resulting_revision BIGINT,
    correlation_id UUID,
    safe_metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

REVOKE UPDATE, DELETE ON outbox_event FROM PUBLIC;
REVOKE UPDATE, DELETE ON simulation_audit_event FROM PUBLIC;

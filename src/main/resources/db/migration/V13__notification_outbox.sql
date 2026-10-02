-- The business transaction owns enqueue; delivery and acknowledgement commit together.
CREATE TABLE sys_notification_outbox (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES sys_tenant(id),
    receiver_id BIGINT NOT NULL,
    type VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content VARCHAR(2000),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'DELIVERED', 'FAILED')),
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    delivered_at TIMESTAMPTZ,
    last_error VARCHAR(128),
    CONSTRAINT fk_notification_outbox_receiver FOREIGN KEY (tenant_id, receiver_id) REFERENCES sys_user(tenant_id, id)
);
CREATE INDEX idx_notification_outbox_pending ON sys_notification_outbox(next_attempt_at, created_at)
    WHERE status = 'PENDING';

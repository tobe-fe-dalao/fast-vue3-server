-- Keep command claims until an explicit business retention policy permits deletion.
-- Claim insertion and command effects share the same PostgreSQL transaction.
CREATE TABLE sys_idempotency (
    tenant_id BIGINT NOT NULL REFERENCES sys_tenant(id),
    user_id BIGINT NOT NULL REFERENCES sys_user(id),
    operation VARCHAR(128) NOT NULL,
    request_key VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id, user_id, operation, request_key)
);
CREATE INDEX idx_approval_pending_actor ON wf_approval_step(tenant_id, approver_id, approval_request_id)
    WHERE status = 'PENDING';
CREATE INDEX idx_user_role_role ON sys_user_role(tenant_id, role_id, user_id);

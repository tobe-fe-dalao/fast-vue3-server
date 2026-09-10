-- Enterprise reference domains: tenant isolation, organization, projects/tasks,
-- approvals, notifications and request audit. Existing V1-V8 migrations stay immutable.

CREATE TABLE sys_tenant (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    code        VARCHAR(64)  NOT NULL UNIQUE,
    status      VARCHAR(16)  NOT NULL DEFAULT 'active',
    plan        VARCHAR(32)  NOT NULL DEFAULT 'standard',
    expired_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
INSERT INTO sys_tenant (id, name, code, status, plan)
VALUES (1, 'Fast Vue3 Default Organization', 'default', 'active', 'enterprise');
SELECT setval('sys_tenant_id_seq', 1);

ALTER TABLE sys_user ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 REFERENCES sys_tenant(id);
ALTER TABLE sys_role ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 REFERENCES sys_tenant(id);
ALTER TABLE sys_user_role ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 REFERENCES sys_tenant(id);
ALTER TABLE sys_role_permission ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 REFERENCES sys_tenant(id);
ALTER TABLE sys_role_menu ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 REFERENCES sys_tenant(id);
ALTER TABLE content_category ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 REFERENCES sys_tenant(id);
ALTER TABLE content_article ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 REFERENCES sys_tenant(id);
ALTER TABLE site_blog_comment ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 REFERENCES sys_tenant(id);
ALTER TABLE site_payment_order ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 REFERENCES sys_tenant(id);

ALTER TABLE sys_user DROP CONSTRAINT sys_user_username_key;
ALTER TABLE sys_role DROP CONSTRAINT sys_role_code_key;
ALTER TABLE content_category DROP CONSTRAINT content_category_slug_key;
CREATE UNIQUE INDEX uq_user_tenant_username ON sys_user(tenant_id, username) WHERE deleted = false;
CREATE UNIQUE INDEX uq_role_tenant_code ON sys_role(tenant_id, code) WHERE deleted = false;
CREATE UNIQUE INDEX uq_category_tenant_slug ON content_category(tenant_id, slug) WHERE deleted = false;
CREATE INDEX idx_user_tenant ON sys_user(tenant_id);
CREATE INDEX idx_article_tenant_status ON content_article(tenant_id, status);
CREATE INDEX idx_payment_tenant_created ON site_payment_order(tenant_id, created_at DESC);

CREATE TABLE org_organization (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL UNIQUE REFERENCES sys_tenant(id),
    name        VARCHAR(128) NOT NULL,
    code        VARCHAR(64) NOT NULL,
    status      VARCHAR(16) NOT NULL DEFAULT 'active',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, code)
);
INSERT INTO org_organization(tenant_id, name, code) VALUES (1, 'Fast Vue3', 'FASTVUE3');

CREATE TABLE org_department (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES sys_tenant(id),
    organization_id BIGINT NOT NULL REFERENCES org_organization(id),
    parent_id       BIGINT REFERENCES org_department(id),
    name            VARCHAR(128) NOT NULL,
    code            VARCHAR(64) NOT NULL,
    leader_id       BIGINT REFERENCES sys_user(id),
    sort            INTEGER NOT NULL DEFAULT 0,
    status          VARCHAR(16) NOT NULL DEFAULT 'active',
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      BIGINT,
    updated_by      BIGINT
);
CREATE UNIQUE INDEX uq_department_tenant_code ON org_department(tenant_id, code) WHERE deleted = false;
CREATE INDEX idx_department_tenant_parent ON org_department(tenant_id, parent_id, sort);

CREATE TABLE org_department_member (
    tenant_id       BIGINT NOT NULL REFERENCES sys_tenant(id),
    department_id   BIGINT NOT NULL REFERENCES org_department(id),
    user_id         BIGINT NOT NULL REFERENCES sys_user(id),
    joined_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id, department_id, user_id)
);
CREATE INDEX idx_department_member_user ON org_department_member(tenant_id, user_id);
CREATE UNIQUE INDEX uq_department_member_user ON org_department_member(tenant_id, user_id);

CREATE TABLE biz_project (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL REFERENCES sys_tenant(id),
    name        VARCHAR(160) NOT NULL,
    code        VARCHAR(64) NOT NULL,
    description TEXT,
    owner_id    BIGINT NOT NULL REFERENCES sys_user(id),
    status      VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    start_date  DATE,
    end_date    DATE,
    version     INTEGER NOT NULL DEFAULT 0,
    deleted     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT
);
CREATE UNIQUE INDEX uq_project_tenant_code ON biz_project(tenant_id, code) WHERE deleted = false;
CREATE INDEX idx_project_tenant_status ON biz_project(tenant_id, status, created_at DESC);

CREATE TABLE biz_project_member (
    tenant_id  BIGINT NOT NULL REFERENCES sys_tenant(id),
    project_id BIGINT NOT NULL REFERENCES biz_project(id),
    user_id    BIGINT NOT NULL REFERENCES sys_user(id),
    role       VARCHAR(24) NOT NULL DEFAULT 'MEMBER',
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id, project_id, user_id)
);
CREATE INDEX idx_project_member_user ON biz_project_member(tenant_id, user_id, project_id);

CREATE TABLE biz_project_activity (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL REFERENCES sys_tenant(id),
    project_id  BIGINT NOT NULL REFERENCES biz_project(id),
    actor_id    BIGINT NOT NULL REFERENCES sys_user(id),
    action      VARCHAR(64) NOT NULL,
    detail      TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_project_activity_project ON biz_project_activity(tenant_id, project_id, created_at DESC);

CREATE TABLE biz_task (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL REFERENCES sys_tenant(id),
    project_id  BIGINT NOT NULL REFERENCES biz_project(id),
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    assignee_id BIGINT REFERENCES sys_user(id),
    reporter_id BIGINT NOT NULL REFERENCES sys_user(id),
    priority    VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
    status      VARCHAR(24) NOT NULL DEFAULT 'TODO',
    due_date    DATE,
    version     INTEGER NOT NULL DEFAULT 0,
    deleted     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT
);
CREATE INDEX idx_task_tenant_project ON biz_task(tenant_id, project_id, status);
CREATE INDEX idx_task_tenant_assignee ON biz_task(tenant_id, assignee_id, status);

CREATE TABLE biz_task_comment (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL REFERENCES sys_tenant(id),
    task_id     BIGINT NOT NULL REFERENCES biz_task(id),
    author_id   BIGINT NOT NULL REFERENCES sys_user(id),
    content     VARCHAR(2000) NOT NULL,
    deleted     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT
);
CREATE INDEX idx_task_comment_task ON biz_task_comment(tenant_id, task_id, created_at);

CREATE TABLE biz_task_activity (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL REFERENCES sys_tenant(id),
    task_id     BIGINT NOT NULL REFERENCES biz_task(id),
    actor_id    BIGINT NOT NULL REFERENCES sys_user(id),
    action      VARCHAR(64) NOT NULL,
    field_name  VARCHAR(64),
    old_value   TEXT,
    new_value   TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_task_activity_task ON biz_task_activity(tenant_id, task_id, created_at DESC);

CREATE TABLE wf_approval_request (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES sys_tenant(id),
    type            VARCHAR(64) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    business_key    VARCHAR(128),
    applicant_id    BIGINT NOT NULL REFERENCES sys_user(id),
    department_id   BIGINT REFERENCES org_department(id),
    status          VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    current_step    INTEGER NOT NULL DEFAULT 0,
    payload         TEXT,
    version         INTEGER NOT NULL DEFAULT 0,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      BIGINT,
    updated_by      BIGINT
);
CREATE INDEX idx_approval_tenant_status ON wf_approval_request(tenant_id, status, created_at DESC);

CREATE TABLE wf_approval_step (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL REFERENCES sys_tenant(id),
    approval_request_id BIGINT NOT NULL REFERENCES wf_approval_request(id),
    step_order          INTEGER NOT NULL,
    approver_type       VARCHAR(32) NOT NULL,
    approver_id         BIGINT,
    status              VARCHAR(24) NOT NULL DEFAULT 'WAITING',
    acted_at            TIMESTAMPTZ,
    UNIQUE(tenant_id, approval_request_id, step_order)
);

CREATE TABLE wf_approval_action (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL REFERENCES sys_tenant(id),
    approval_request_id BIGINT NOT NULL REFERENCES wf_approval_request(id),
    step_id             BIGINT REFERENCES wf_approval_step(id),
    actor_id            BIGINT NOT NULL REFERENCES sys_user(id),
    action              VARCHAR(24) NOT NULL,
    comment             VARCHAR(1000),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_approval_action_request ON wf_approval_action(tenant_id, approval_request_id, created_at);

CREATE TABLE sys_notification (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL REFERENCES sys_tenant(id),
    type        VARCHAR(64) NOT NULL,
    title       VARCHAR(255) NOT NULL,
    content     VARCHAR(2000),
    receiver_id BIGINT NOT NULL REFERENCES sys_user(id),
    read        BOOLEAN NOT NULL DEFAULT FALSE,
    deleted     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT
);
CREATE INDEX idx_notification_receiver ON sys_notification(tenant_id, receiver_id, read, created_at DESC);

CREATE TABLE sys_operation_log (
    id          BIGSERIAL PRIMARY KEY,
    request_id  VARCHAR(64) NOT NULL,
    tenant_id   BIGINT REFERENCES sys_tenant(id),
    user_id     BIGINT,
    method      VARCHAR(12) NOT NULL,
    path        VARCHAR(512) NOT NULL,
    ip          VARCHAR(64),
    user_agent  VARCHAR(512),
    duration    BIGINT NOT NULL,
    status      INTEGER NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_operation_tenant_created ON sys_operation_log(tenant_id, created_at DESC);
CREATE INDEX idx_operation_request ON sys_operation_log(request_id);

INSERT INTO sys_permission(code, name, description) VALUES
 ('tenant:list', '租户查询', '跨租户查询（仅超级管理员）'),
 ('department:list', '部门查询', '查看组织与部门'),
 ('department:manage', '部门管理', '维护组织与部门成员'),
 ('project:list', '项目查询', '查看可访问项目'),
 ('project:create', '项目创建', '创建项目'),
 ('project:update', '项目更新', '更新和归档项目'),
 ('task:create', '任务创建', '在项目内创建任务'),
 ('task:update', '任务更新', '编辑、指派和流转任务'),
 ('approval:create', '审批创建', '创建并提交审批'),
 ('approval:action', '审批处理', '同意或拒绝待办审批'),
 ('audit:view', '审计查看', '查看系统操作审计'),
 ('file:upload', '文件上传', '上传受控业务附件');

INSERT INTO sys_role_permission(tenant_id, role_id, permission_id)
SELECT 1, 1, id FROM sys_permission
WHERE code IN ('tenant:list','department:list','department:manage','project:list','project:create',
 'project:update','task:create','task:update','approval:create','approval:action','audit:view','file:upload');

-- Prevent accidental tenant fallback in raw SQL after legacy rows have been backfilled.
ALTER TABLE sys_user ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE sys_role ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE sys_user_role ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE sys_role_permission ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE sys_role_menu ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE content_category ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE content_article ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE site_blog_comment ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE site_payment_order ALTER COLUMN tenant_id DROP DEFAULT;

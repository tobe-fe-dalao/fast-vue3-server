-- =============================================================
-- V1: 基础表结构（用户 / 角色 / 权限 / 菜单 及关联表）
-- =============================================================

CREATE TABLE sys_user (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(64)  NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,
    nickname    VARCHAR(64),
    email       VARCHAR(128),
    phone       VARCHAR(32),
    status      VARCHAR(16)  NOT NULL DEFAULT 'active',
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT
);

COMMENT ON COLUMN sys_user.status IS 'active / disabled';
COMMENT ON COLUMN sys_user.deleted IS '逻辑删除标记';

CREATE TABLE sys_role (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(64)  NOT NULL UNIQUE,
    name        VARCHAR(64)  NOT NULL,
    description VARCHAR(255),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT
);

CREATE TABLE sys_permission (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(128) NOT NULL UNIQUE,
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT
);

COMMENT ON COLUMN sys_permission.code IS '权限字符串，如 user:list';

CREATE TABLE sys_menu (
    id          BIGSERIAL PRIMARY KEY,
    parent_id   BIGINT       NOT NULL DEFAULT 0,
    name        VARCHAR(64)  NOT NULL,
    path        VARCHAR(255),
    component   VARCHAR(255),
    icon        VARCHAR(64),
    sort        INTEGER      NOT NULL DEFAULT 0,
    visible     BOOLEAN      NOT NULL DEFAULT TRUE,
    permission  VARCHAR(128),
    type        VARCHAR(16)  NOT NULL DEFAULT 'menu',
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT
);

COMMENT ON COLUMN sys_menu.type IS 'directory / menu / button';

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL REFERENCES sys_user (id),
    role_id BIGINT NOT NULL REFERENCES sys_role (id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE sys_role_permission (
    role_id       BIGINT NOT NULL REFERENCES sys_role (id),
    permission_id BIGINT NOT NULL REFERENCES sys_permission (id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL REFERENCES sys_role (id),
    menu_id BIGINT NOT NULL REFERENCES sys_menu (id),
    PRIMARY KEY (role_id, menu_id)
);

CREATE INDEX idx_user_role_user ON sys_user_role (user_id);
CREATE INDEX idx_role_permission_role ON sys_role_permission (role_id);
CREATE INDEX idx_role_menu_role ON sys_role_menu (role_id);

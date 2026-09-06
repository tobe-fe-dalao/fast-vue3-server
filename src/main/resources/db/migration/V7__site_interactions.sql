-- Authenticated site interactions: blog comments and payment checkout orders.

CREATE TABLE site_blog_comment (
    id          BIGSERIAL PRIMARY KEY,
    article_id  BIGINT       NOT NULL,
    user_id     BIGINT       NOT NULL REFERENCES sys_user (id),
    username    VARCHAR(64)  NOT NULL,
    content     VARCHAR(1000) NOT NULL,
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT
);

CREATE INDEX idx_site_comment_article ON site_blog_comment (article_id, created_at DESC);

CREATE TABLE site_payment_order (
    id           BIGSERIAL PRIMARY KEY,
    order_no     VARCHAR(40)  NOT NULL UNIQUE,
    user_id      BIGINT       NOT NULL REFERENCES sys_user (id),
    plan_id      BIGINT       NOT NULL,
    plan_name    VARCHAR(64)  NOT NULL,
    amount_cents INTEGER      NOT NULL,
    currency     VARCHAR(8)   NOT NULL DEFAULT 'CNY',
    channel      VARCHAR(16)  NOT NULL,
    status       VARCHAR(16)  NOT NULL DEFAULT 'pending',
    expires_at   TIMESTAMPTZ  NOT NULL,
    deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by   BIGINT,
    updated_by   BIGINT
);

CREATE INDEX idx_site_payment_user ON site_payment_order (user_id, created_at DESC);
COMMENT ON COLUMN site_payment_order.status IS 'pending / paid / cancelled / expired';
COMMENT ON COLUMN site_payment_order.channel IS 'alipay / wechat / card';

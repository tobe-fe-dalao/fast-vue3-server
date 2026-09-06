-- =============================================================
-- V4: 内容管理模块（文章 / 分类）
-- =============================================================

CREATE TABLE content_category (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(64)  NOT NULL,
    slug        VARCHAR(64)  NOT NULL UNIQUE,
    description VARCHAR(255),
    status      VARCHAR(16)  NOT NULL DEFAULT 'active',
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT
);

COMMENT ON COLUMN content_category.status IS 'active / inactive';
COMMENT ON COLUMN content_category.deleted IS '逻辑删除标记';

CREATE TABLE content_article (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    author      VARCHAR(64),
    summary     VARCHAR(512),
    content     TEXT,
    cover       VARCHAR(255),
    category_id BIGINT REFERENCES content_category (id),
    status      VARCHAR(16)  NOT NULL DEFAULT 'draft',
    tags        TEXT,
    date        VARCHAR(32),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT
);

COMMENT ON COLUMN content_article.status IS 'draft / published';
COMMENT ON COLUMN content_article.content IS 'JSON 数组，存储正文段落';
COMMENT ON COLUMN content_article.tags IS 'JSON 数组，存储标签';
COMMENT ON COLUMN content_article.deleted IS '逻辑删除标记';

CREATE INDEX idx_content_article_category ON content_article (category_id);
CREATE INDEX idx_content_article_status ON content_article (status);

-- 分类种子数据（至少 4 个）
INSERT INTO content_category (id, name, slug, description, status) VALUES
    (1, '产品动态', 'product', '产品功能更新与版本发布动态', 'active'),
    (2, '技术博客', 'tech', '技术实践、架构与工程化分享', 'active'),
    (3, '团队生活', 'team', '团队活动、成长与文化建设', 'active'),
    (4, '公告', 'announcement', '官方公告与重要通知', 'active');

-- 文章种子数据（至少 8 篇，使用真实中文业务文案）
INSERT INTO content_article (id, title, author, summary, content, cover, category_id, status, tags, date) VALUES
    (1, '内容管理模块上线：让运营自助发布文章', '张明',
     '新增内容管理模块，运营同学无需研发介入即可发布与维护文章。',
     '["过去文章的发布往往要排期等待研发改库，效率低且容易出错。",
       "内容管理模块把文章与分类抽象为标准 CRUD，并对接统一权限。",
       "运营在后台即可完成撰写、分类、上下线与统计，研发只负责能力沉淀。"]',
     '', 1, 'published', '["产品", "内容管理"]', '2026-08-20'),
    (2, '如何用 Fast Vue3 在一天内搭好企业后台', '李雪',
     '从脚手架到权限、内容管理，带你走完一条可投产的最短路径。',
     '["很多企业后台的痛点不是功能，而是从 0 到 1 的启动成本。",
       "本文以订单管理后台为例：先生成 Monorepo 骨架，再启用文件路由。",
       "当你跑通第一条列表到详情的链路，剩下的页面都只是同一套模式的复制。"]',
     '', 2, 'published', '["教程", "后台", "CLI"]', '2026-08-12'),
    (3, '7 套 UI 框架，同一套业务能力的秘密', '王浩',
     '在不牺牲各框架原生体验的前提下，让页面能力完全对齐。',
     '["功能对齐是 Fast Vue3 的核心约束：无论使用哪套 UI，后台都拥有相同的信息架构。",
       "业务逻辑沉淀在共享包里，UI 层只负责把同一份数据结构渲染成原生组件。"]',
     '', 2, 'published', '["架构", "UI", "Design System"]', '2026-08-06'),
    (4, '文件路由让新增页面变成建文件', '陈思',
     '告别手写路由表，目录即路由，动态参数用方括号表达。',
     '["在大型后台里，路由表往往是最容易腐化的文件。文件路由把声明交还给目录结构。",
       "动态参数用方括号文件名表达，配合侧边栏目录分组，路由与导航始终一致。"]',
     '', 2, 'published', '["路由", "工程化"]', '2026-07-29'),
    (5, '从 PR 到独立负责模块：实习生培养路径', '周凯',
     '小步反馈加可信任的脚手架，让新人逐步交付完整模块。',
     '["新人从小改动开始，逐步过渡到列表筛选，再到独立负责完整模块。",
       "持续的小步反馈与可靠脚手架同样重要，能显著降低上手门槛。"]',
     '', 3, 'published', '["成长", "招聘"]', '2026-06-30'),
    (6, '开源协议变更意味着什么', '王浩',
     '更明确的专利授权与商业保障，使用方式保持不变。',
     '["核心共享包采用 Apache-2.0，提供更明确的专利授权与商业保障。",
       "个人与商业项目均可继续使用，无需担心协议突变。"]',
     '', 4, 'published', '["开源", "协议"]', '2026-06-12'),
    (7, '组件市场来了：把业务区块装进一行命令', '李雪',
     '生产验证的业务区块，安装后自动适配所选 UI 框架。',
     '["组件市场覆盖高级搜索、可拖拽看板和角色权限矩阵等高频场景。",
       "一行命令安装，并自动适配框架设计语言，避免重复造轮子。"]',
     '', 1, 'published', '["组件市场", "效率"]', '2026-06-22'),
    (8, '暗色模式不只是换肤：设计 Token 的正确打开方式', '陈思',
     '把颜色抽象为语义 Token，让组件在明暗主题间无缝切换。',
     '["暗色模式翻车，多半是因为把颜色写死在了组件里。",
       "切换主题时只改语义变量，组件便可全部响应，无需逐一套餐修改。"]',
     '', 2, 'published', '["主题", "CSS", "Design Token"]', '2026-07-21'),
    (9, '真实中文内容：为什么我们不写 Lorem Ipsum', '赵敏',
     '占位文本会掩盖真实的排版与信息密度问题。',
     '["占位文本会掩盖中文与英文在行高、断行和标点上的真实差异。",
       "所以模板坚持使用真实中文业务内容，帮助设计更早暴露问题。"]',
     '', 3, 'draft', '["内容", "团队"]', '2026-07-08');

-- 修正自增序列起点，避免与显式 id 冲突
SELECT setval('content_category_id_seq', (SELECT max(id) FROM content_category));
SELECT setval('content_article_id_seq', (SELECT max(id) FROM content_article));

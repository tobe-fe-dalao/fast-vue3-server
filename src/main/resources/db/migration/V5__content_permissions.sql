-- =============================================================
-- V5: 内容管理权限字符串与菜单（挂在系统管理目录下）
-- =============================================================

-- 权限
INSERT INTO sys_permission (code, name, description) VALUES
    ('content:list',   '内容查询', '查看文章与分类'),
    ('content:create', '内容创建', '新增文章与分类'),
    ('content:update', '内容更新', '修改文章与分类'),
    ('content:delete', '内容删除', '删除文章与分类');

-- 菜单：内容管理目录（挂在系统管理目录 id=3 下）
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort, visible, type) VALUES
    (16, 3, '内容管理', '/content', NULL, 'FileTextOutlined', 4, TRUE, 'directory'),
    (17, 16, '文章管理', '/content/article', 'content/article/index', 'FileOutlined', 1, TRUE, 'menu'),
    (18, 16, '分类管理', '/content/category', 'content/category/index', 'TagsOutlined', 2, TRUE, 'menu');

-- 按钮权限（文章管理下）
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort, visible, permission, type) VALUES
    (19, 17, '文章新增', NULL, NULL, NULL, 1, TRUE, 'content:create', 'button'),
    (20, 17, '文章编辑', NULL, NULL, NULL, 2, TRUE, 'content:update', 'button'),
    (21, 17, '文章删除', NULL, NULL, NULL, 3, TRUE, 'content:delete', 'button'),
    (22, 18, '分类新增', NULL, NULL, NULL, 1, TRUE, 'content:create', 'button'),
    (23, 18, '分类编辑', NULL, NULL, NULL, 2, TRUE, 'content:update', 'button'),
    (24, 18, '分类删除', NULL, NULL, NULL, 3, TRUE, 'content:delete', 'button');

-- 为超级管理员角色（id=1）赋予内容管理权限与菜单
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission
WHERE code IN ('content:list', 'content:create', 'content:update', 'content:delete');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id BETWEEN 16 AND 24;

-- 修正自增序列起点，避免与显式 id 冲突
SELECT setval('sys_menu_id_seq', (SELECT max(id) FROM sys_menu));
SELECT setval('sys_permission_id_seq', (SELECT max(id) FROM sys_permission));

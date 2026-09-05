-- =============================================================
-- V2: 初始化权限字符串与菜单
-- =============================================================

-- 权限
INSERT INTO sys_permission (code, name, description) VALUES
    ('user:list',   '用户查询', '查看用户列表'),
    ('user:create', '用户创建', '新增用户'),
    ('user:update', '用户更新', '修改用户'),
    ('user:delete', '用户删除', '删除用户'),
    ('role:list',   '角色查询', '查看角色列表'),
    ('role:create', '角色创建', '新增角色'),
    ('role:update', '角色更新', '修改角色'),
    ('role:delete', '角色删除', '删除角色'),
    ('menu:list',   '菜单查询', '查看菜单'),
    ('menu:create', '菜单创建', '新增菜单'),
    ('menu:update', '菜单更新', '修改菜单'),
    ('menu:delete', '菜单删除', '删除菜单');

-- 菜单树
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort, visible, type) VALUES
    (1,  0, '首页',       '/home',     'home/index',     'HomeOutlined',     1, TRUE, 'menu'),
    (2,  0, '仪表盘',     '/dashboard', 'dashboard/index', 'DashboardOutlined', 2, TRUE, 'menu'),
    (3,  0, '系统管理',   '/system',   NULL,             'SettingOutlined',  3, TRUE, 'directory'),
    (4,  3, '用户管理',   '/system/user', 'system/user/index', 'UserOutlined', 1, TRUE, 'menu'),
    (5,  3, '角色管理',   '/system/role', 'system/role/index', 'LockOutlined', 2, TRUE, 'menu'),
    (6,  3, '菜单管理',   '/system/menu', 'system/menu/index', 'MenuOutlined', 3, TRUE, 'menu');

-- 按钮权限（挂在父菜单下，visible 用于前端按钮显隐）
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort, visible, permission, type) VALUES
    (7,  4, '用户新增', NULL, NULL, NULL, 1, TRUE, 'user:create', 'button'),
    (8,  4, '用户编辑', NULL, NULL, NULL, 2, TRUE, 'user:update', 'button'),
    (9,  4, '用户删除', NULL, NULL, NULL, 3, TRUE, 'user:delete', 'button'),
    (10, 5, '角色新增', NULL, NULL, NULL, 1, TRUE, 'role:create', 'button'),
    (11, 5, '角色编辑', NULL, NULL, NULL, 2, TRUE, 'role:update', 'button'),
    (12, 5, '角色删除', NULL, NULL, NULL, 3, TRUE, 'role:delete', 'button'),
    (13, 6, '菜单新增', NULL, NULL, NULL, 1, TRUE, 'menu:create', 'button'),
    (14, 6, '菜单编辑', NULL, NULL, NULL, 2, TRUE, 'menu:update', 'button'),
    (15, 6, '菜单删除', NULL, NULL, NULL, 3, TRUE, 'menu:delete', 'button');

-- 修正自增序列起点，避免与显式 id 冲突
SELECT setval('sys_menu_id_seq', (SELECT max(id) FROM sys_menu));
SELECT setval('sys_permission_id_seq', (SELECT max(id) FROM sys_permission));

-- =============================================================
-- V3: 初始化默认管理员与角色绑定
-- =============================================================

-- 默认管理员密码为占位符，实际密码由应用启动时通过环境变量注入
-- （见 AdminInitializer），此处仅保证 Flyway 迁移可独立执行。
-- 该 bcrypt 值对应明文 "admin123"，仅供迁移脚本内参考，应用启动后
-- 若配置了 ADMIN_PASSWORD 环境变量会覆盖它。
INSERT INTO sys_user (id, username, password, nickname, email, status) VALUES
    (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'Administrator', 'admin@fast-vue3.com', 'active');

-- 超级管理员角色
INSERT INTO sys_role (id, code, name, description) VALUES
    (1, 'admin', '超级管理员', '拥有系统所有权限');

-- 绑定用户与角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 为超级管理员角色赋予全部权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;

-- 为超级管理员角色赋予全部菜单（目录 / 菜单 / 按钮）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;

SELECT setval('sys_user_id_seq', (SELECT max(id) FROM sys_user));
SELECT setval('sys_role_id_seq', (SELECT max(id) FROM sys_role));

-- 管理端聚合页面权限，供角色授权页完整持久化。
INSERT INTO sys_permission (code, name, description) VALUES
    ('dashboard:view',  '仪表盘查看', '查看仪表盘'),
    ('role:permission', '角色授权',   '配置角色权限'),
    ('log:view',        '日志查看',   '查看系统日志'),
    ('settings:view',   '系统设置查看', '查看系统设置');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission
WHERE code IN ('dashboard:view', 'role:permission', 'log:view', 'settings:view');

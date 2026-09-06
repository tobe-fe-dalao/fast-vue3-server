-- Fine-grained permissions for business analytics that may hold real operating data.
INSERT INTO sys_permission (code, name, description) VALUES
    ('analytics:view', '数据分析查看', '查看经营分析与趋势数据'),
    ('data:view',      '数据中心查看', '查看经营数据中心');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id
FROM sys_permission
WHERE code IN ('analytics:view', 'data:view');

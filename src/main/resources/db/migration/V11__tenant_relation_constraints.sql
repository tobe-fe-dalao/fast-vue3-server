-- Defense in depth: a valid ID alone must not allow a cross-tenant relation.
ALTER TABLE sys_user ADD CONSTRAINT uq_user_tenant_id UNIQUE (tenant_id, id);
ALTER TABLE sys_role ADD CONSTRAINT uq_role_tenant_id UNIQUE (tenant_id, id);
ALTER TABLE biz_project ADD CONSTRAINT uq_project_tenant_id UNIQUE (tenant_id, id);
ALTER TABLE biz_task ADD CONSTRAINT uq_task_tenant_id UNIQUE (tenant_id, id);
ALTER TABLE org_department ADD CONSTRAINT uq_department_tenant_id UNIQUE (tenant_id, id);
ALTER TABLE sys_user_role ADD CONSTRAINT fk_user_role_tenant_user FOREIGN KEY (tenant_id, user_id) REFERENCES sys_user(tenant_id, id);
ALTER TABLE sys_user_role ADD CONSTRAINT fk_user_role_tenant_role FOREIGN KEY (tenant_id, role_id) REFERENCES sys_role(tenant_id, id);
ALTER TABLE sys_role_permission ADD CONSTRAINT fk_role_permission_tenant_role FOREIGN KEY (tenant_id, role_id) REFERENCES sys_role(tenant_id, id);
ALTER TABLE sys_role_menu ADD CONSTRAINT fk_role_menu_tenant_role FOREIGN KEY (tenant_id, role_id) REFERENCES sys_role(tenant_id, id);
ALTER TABLE biz_project_member ADD CONSTRAINT fk_project_member_tenant_project FOREIGN KEY (tenant_id, project_id) REFERENCES biz_project(tenant_id, id);
ALTER TABLE biz_project_member ADD CONSTRAINT fk_project_member_tenant_user FOREIGN KEY (tenant_id, user_id) REFERENCES sys_user(tenant_id, id);
ALTER TABLE biz_project ADD CONSTRAINT fk_project_tenant_owner FOREIGN KEY (tenant_id, owner_id) REFERENCES sys_user(tenant_id, id);
ALTER TABLE biz_task ADD CONSTRAINT fk_task_tenant_project FOREIGN KEY (tenant_id, project_id) REFERENCES biz_project(tenant_id, id);
ALTER TABLE biz_task ADD CONSTRAINT fk_task_tenant_assignee FOREIGN KEY (tenant_id, assignee_id) REFERENCES sys_user(tenant_id, id);
ALTER TABLE org_department_member ADD CONSTRAINT fk_department_member_tenant_department FOREIGN KEY (tenant_id, department_id) REFERENCES org_department(tenant_id, id);
ALTER TABLE org_department_member ADD CONSTRAINT fk_department_member_tenant_user FOREIGN KEY (tenant_id, user_id) REFERENCES sys_user(tenant_id, id);
ALTER TABLE sys_idempotency ADD CONSTRAINT fk_idempotency_tenant_user FOREIGN KEY (tenant_id, user_id) REFERENCES sys_user(tenant_id, id);

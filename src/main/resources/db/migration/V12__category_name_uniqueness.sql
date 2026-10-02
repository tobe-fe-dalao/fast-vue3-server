-- Enforce the service's existing tenant-scoped, live-category name invariant.
-- Existing duplicate live names must be resolved explicitly before deployment;
-- CREATE UNIQUE INDEX fails atomically instead of silently renaming business data.
CREATE UNIQUE INDEX uq_category_tenant_name_live
    ON content_category (tenant_id, name) WHERE deleted = false;

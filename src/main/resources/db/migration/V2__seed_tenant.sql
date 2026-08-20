-- V2__seed_tenant.sql
INSERT INTO tenants (name, created_at)
SELECT 'default-tenant', NOW()
WHERE NOT EXISTS (SELECT 1 FROM tenants WHERE name = 'default-tenant');
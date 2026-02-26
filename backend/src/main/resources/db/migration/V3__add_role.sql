-- ============================================================
-- V3__add_role.sql
-- Menambahkan kolom role ke tabel users untuk RBAC
-- ============================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Jadikan test@example.com sebagai admin untuk testing
UPDATE users SET role = 'ADMIN' WHERE email = 'test@example.com';

-- ============================================================
-- V2__add_digest_features.sql
-- Menambahkan fitur yang dibutuhkan untuk email digest
-- ============================================================

-- Tambah kolom digest preferences ke tabel users
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS last_digest_sent_at TIMESTAMP,     -- Kapan terakhir email dikirim
    ADD COLUMN IF NOT EXISTS digest_unsubscribe_token VARCHAR(255) UNIQUE; -- Token untuk unsubscribe

-- Tabel untuk tracking email yang sudah dikirim (mencegah duplikasi)
CREATE TABLE IF NOT EXISTS digest_logs (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status          VARCHAR(50) NOT NULL,        -- SENT, FAILED, SKIPPED
    recipient_email VARCHAR(255) NOT NULL,
    articles_count  INT DEFAULT 0,               -- Berapa artikel yang dikirim
    error_message   TEXT,
    sent_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_digest_logs_user_id ON digest_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_digest_logs_sent_at ON digest_logs(sent_at DESC);

-- Insert user contoh untuk testing
INSERT INTO users (email, password_hash, full_name, is_active, email_verified, digest_enabled, digest_frequency)
VALUES
    ('test@example.com', '$2a$10$example_hash', 'User Test', true, true, true, 'DAILY'),
    ('weekly@example.com', '$2a$10$example_hash', 'User Weekly', true, true, true, 'WEEKLY')
ON CONFLICT (email) DO NOTHING;

-- Insert preferensi kategori untuk user test
INSERT INTO user_category_preferences (user_id, category)
SELECT u.id, 'teknologi' FROM users u WHERE u.email = 'test@example.com'
ON CONFLICT DO NOTHING;

INSERT INTO user_category_preferences (user_id, category)
SELECT u.id, 'bisnis' FROM users u WHERE u.email = 'test@example.com'
ON CONFLICT DO NOTHING;
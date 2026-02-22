-- ============================================================
-- V1__init.sql
-- Migration pertama: membuat semua tabel dasar aplikasi
-- Dijalankan otomatis oleh Flyway saat aplikasi pertama kali start
-- ============================================================

-- ============================================================
-- TABEL: sources
-- Menyimpan daftar sumber RSS yang akan di-crawl
-- ============================================================
CREATE TABLE sources (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,               -- Nama sumber, contoh: "CNN Indonesia"
    url         VARCHAR(500) NOT NULL UNIQUE,        -- URL RSS feed, harus unik
    website_url VARCHAR(500),                        -- URL website aslinya
    category    VARCHAR(100) NOT NULL,               -- Kategori: teknologi, bisnis, olahraga, dll
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,       -- Apakah sumber ini aktif di-crawl?
    last_crawled_at TIMESTAMP,                       -- Kapan terakhir kali berhasil di-crawl
    crawl_status    VARCHAR(50) DEFAULT 'PENDING',   -- Status: PENDING, SUCCESS, ERROR
    error_message   TEXT,                            -- Pesan error jika crawl gagal
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE sources IS 'Daftar sumber RSS feed yang akan di-crawl secara berkala';
COMMENT ON COLUMN sources.crawl_status IS 'PENDING=belum pernah crawl, SUCCESS=berhasil, ERROR=gagal';

-- ============================================================
-- TABEL: articles
-- Menyimpan artikel berita hasil crawl dari RSS feed
-- ============================================================
CREATE TABLE articles (
    id              BIGSERIAL PRIMARY KEY,
    source_id       BIGINT NOT NULL REFERENCES sources(id) ON DELETE CASCADE,
    title           VARCHAR(500) NOT NULL,           -- Judul artikel
    url             VARCHAR(500) NOT NULL UNIQUE,    -- URL artikel asli, harus unik (untuk cek duplikat)
    guid            VARCHAR(500),                    -- GUID dari RSS feed (identifier unik dari sumber)
    description     TEXT,                            -- Ringkasan/excerpt artikel
    content         TEXT,                            -- Konten lengkap artikel (hasil scraping)
    thumbnail_url   VARCHAR(500),                    -- URL gambar thumbnail
    author          VARCHAR(255),                    -- Nama penulis artikel
    category        VARCHAR(100),                    -- Kategori artikel
    published_at    TIMESTAMP,                       -- Waktu artikel diterbitkan di sumber asli
    is_scraped      BOOLEAN NOT NULL DEFAULT FALSE,  -- Apakah konten lengkap sudah di-scrape?
    view_count      BIGINT NOT NULL DEFAULT 0,       -- Jumlah view (untuk sorting popularitas)
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE articles IS 'Artikel berita hasil crawl dari berbagai sumber RSS';
COMMENT ON COLUMN articles.url IS 'URL unik untuk mencegah duplikasi artikel';
COMMENT ON COLUMN articles.guid IS 'GUID dari RSS feed, alternatif identifier dari sumber';

-- ============================================================
-- TABEL: users
-- Menyimpan data pengguna aplikasi
-- ============================================================
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,           -- Password yang sudah di-hash (BCrypt)
    full_name       VARCHAR(255),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    digest_enabled  BOOLEAN NOT NULL DEFAULT FALSE,  -- Apakah user mau terima email digest?
    digest_frequency VARCHAR(20) DEFAULT 'DAILY',    -- DAILY atau WEEKLY
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE users IS 'Data pengguna aplikasi';

-- ============================================================
-- TABEL: user_category_preferences
-- Menyimpan preferensi kategori berita per user
-- Relasi many-to-many antara user dan kategori
-- ============================================================
CREATE TABLE user_category_preferences (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category    VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, category)                        -- Satu user tidak boleh duplikat kategori
);

-- ============================================================
-- TABEL: bookmarks
-- Menyimpan artikel yang di-bookmark oleh user
-- ============================================================
CREATE TABLE bookmarks (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    article_id  BIGINT NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, article_id)                      -- Satu artikel hanya bisa di-bookmark sekali per user
);

-- ============================================================
-- TABEL: crawl_logs
-- Menyimpan log setiap kali crawl dijalankan (untuk monitoring)
-- ============================================================
CREATE TABLE crawl_logs (
    id              BIGSERIAL PRIMARY KEY,
    source_id       BIGINT NOT NULL REFERENCES sources(id) ON DELETE CASCADE,
    status          VARCHAR(50) NOT NULL,             -- SUCCESS atau FAILED
    articles_found  INT DEFAULT 0,                   -- Jumlah artikel ditemukan di feed
    articles_saved  INT DEFAULT 0,                   -- Jumlah artikel baru yang berhasil disimpan
    error_message   TEXT,                            -- Pesan error jika gagal
    duration_ms     BIGINT,                          -- Durasi crawl dalam millisecond
    crawled_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE crawl_logs IS 'Log history setiap kali proses crawl dijalankan';

-- ============================================================
-- INDEXES
-- Dibuat untuk mempercepat query yang sering dipakai
-- ============================================================

-- Index untuk filter artikel berdasarkan kategori (query paling sering)
CREATE INDEX idx_articles_category ON articles(category);

-- Index untuk sorting artikel berdasarkan tanggal publish (halaman utama)
CREATE INDEX idx_articles_published_at ON articles(published_at DESC);

-- Index untuk filter artikel berdasarkan sumber
CREATE INDEX idx_articles_source_id ON articles(source_id);

-- Index untuk mencegah duplikat saat insert (cek URL sudah ada atau belum)
CREATE INDEX idx_articles_url ON articles(url);

-- Index untuk filter sumber yang aktif
CREATE INDEX idx_sources_active ON sources(is_active) WHERE is_active = TRUE;

-- Index untuk query bookmark per user
CREATE INDEX idx_bookmarks_user_id ON bookmarks(user_id);

-- Index untuk query crawl log per sumber (monitoring)
CREATE INDEX idx_crawl_logs_source_id ON crawl_logs(source_id);
CREATE INDEX idx_crawl_logs_crawled_at ON crawl_logs(crawled_at DESC);

-- ============================================================
-- SEED DATA
-- Data awal untuk development & testing
-- ============================================================
INSERT INTO sources (name, url, website_url, category, is_active) VALUES
('CNN Indonesia - Teknologi', 'https://www.cnnindonesia.com/teknologi/rss', 'https://cnnindonesia.com', 'teknologi', true),
('Kompas Tekno', 'https://tekno.kompas.com/rss/2014/02/23/rssindex', 'https://tekno.kompas.com', 'teknologi', true),
('Detik Finance', 'https://finance.detik.com/rss', 'https://finance.detik.com', 'bisnis', true),
('Kompas Bisnis', 'https://money.kompas.com/rss/2014/02/23/rssindex', 'https://money.kompas.com', 'bisnis', true),
('CNN Indonesia - Olahraga', 'https://www.cnnindonesia.com/olahraga/rss', 'https://cnnindonesia.com', 'olahraga', true);
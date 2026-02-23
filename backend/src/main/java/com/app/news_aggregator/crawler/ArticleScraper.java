package com.app.news_aggregator.crawler;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * ArticleScraper bertanggung jawab untuk:
 * 1. Fetch halaman HTML artikel dari URL aslinya
 * 2. Ekstrak konten teks lengkap dari HTML
 * 3. Ekstrak thumbnail jika belum ada dari RSS feed
 *
 * Tantangan scraping: setiap website punya struktur HTML berbeda.
 * Kita pakai pendekatan heuristik: coba selector yang umum dipakai,
 * fallback ke elemen dengan teks terpanjang.
 */
@Slf4j
@Component
public class ArticleScraper {

    private static final int TIMEOUT_MS = 15_000;      // Timeout HTTP request
    private static final int MIN_CONTENT_LENGTH = 100; // Konten dianggap valid jika > 100 karakter

    // User-Agent agar tidak diblokir oleh website sebagai bot
    private static final String USER_AGENT =
        "Mozilla/5.0 (compatible; NewsAggBot/1.0; +https://newsagg.com/bot)";

    /**
     * Scrape konten lengkap dari URL artikel.
     *
     * @param url URL artikel yang akan di-scrape
     * @return ScrapeResult berisi konten dan thumbnail (jika ditemukan)
     */
    public ScrapeResult scrape(String url) {
        log.debug("Mulai scraping: {}", url);

        try {
            // ---- Step 1: Fetch HTML ----
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .get();

            // ---- Step 2: Ekstrak konten ----
            String content = extractContent(doc);

            // ---- Step 3: Ekstrak thumbnail ----
            String thumbnail = extractThumbnail(doc, url);

            log.debug("Scraping berhasil: {} karakter konten diekstrak dari {}",
                    content != null ? content.length() : 0, url);

            return new ScrapeResult(content, thumbnail, true);

        } catch (Exception e) {
            log.warn("Gagal scraping URL '{}': {}", url, e.getMessage());
            return new ScrapeResult(null, null, false);
        }
    }

    /**
     * Ekstrak konten utama artikel dari HTML.
     *
     * Strategi (dari yang paling spesifik ke paling umum):
     * 1. Cek meta tag Open Graph (og:description) — biasanya ringkasan terbaik
     * 2. Cek tag <article> — standar HTML5 untuk konten artikel
     * 3. Cek selector umum yang dipakai CMS populer (WordPress, dll)
     * 4. Fallback: ambil paragraf terpanjang dari halaman
     */
    private String extractContent(Document doc) {
        // Strategi 1: Coba selector HTML5 <article> tag
        Element articleTag = doc.selectFirst("article");
        if (articleTag != null) {
            String text = articleTag.text();
            if (text.length() > MIN_CONTENT_LENGTH) {
                return cleanContent(text);
            }
        }

        // Strategi 2: Coba selector umum untuk content area
        String[] contentSelectors = {
            "[class*='article-body']",
            "[class*='article-content']",
            "[class*='post-content']",
            "[class*='entry-content']",
            "[class*='story-body']",
            "[class*='content-body']",
            "[class*='read-more']",
            "main",
            "#content",
            ".content"
        };

        for (String selector : contentSelectors) {
            Element element = doc.selectFirst(selector);
            if (element != null) {
                String text = element.text();
                if (text.length() > MIN_CONTENT_LENGTH) {
                    log.debug("Konten ditemukan dengan selector: {}", selector);
                    return cleanContent(text);
                }
            }
        }

        // Strategi 3: Kumpulkan semua paragraf dan ambil yang cukup panjang
        Elements paragraphs = doc.select("p");
        StringBuilder content = new StringBuilder();
        for (Element p : paragraphs) {
            String text = p.text().trim();
            if (text.length() > 50) { // Abaikan paragraf pendek (navigasi, footer, dll)
                content.append(text).append("\n\n");
            }
        }

        if (content.length() > MIN_CONTENT_LENGTH) {
            return cleanContent(content.toString().trim());
        }

        // Jika semua strategi gagal, return null
        log.debug("Tidak bisa ekstrak konten dari halaman");
        return null;
    }

    /**
     * Ekstrak URL thumbnail dari HTML.
     *
     * Strategi:
     * 1. og:image (Open Graph) — paling reliable, dipakai untuk social sharing
     * 2. twitter:image — Twitter Card image
     * 3. Gambar pertama yang cukup besar di dalam artikel
     */
    private String extractThumbnail(Document doc, String pageUrl) {
        // Strategi 1: Open Graph image
        Element ogImage = doc.selectFirst("meta[property='og:image']");
        if (ogImage != null) {
            String content = ogImage.attr("content");
            if (!content.isBlank()) {
                return resolveUrl(content, pageUrl);
            }
        }

        // Strategi 2: Twitter Card image
        Element twitterImage = doc.selectFirst("meta[name='twitter:image']");
        if (twitterImage != null) {
            String content = twitterImage.attr("content");
            if (!content.isBlank()) {
                return resolveUrl(content, pageUrl);
            }
        }

        // Strategi 3: Gambar pertama di dalam article tag
        Element articleImg = doc.selectFirst("article img");
        if (articleImg != null) {
            String src = articleImg.attr("src");
            if (!src.isBlank()) {
                return resolveUrl(src, pageUrl);
            }
        }

        return null;
    }

    /**
     * Resolve relative URL ke absolute URL.
     * Contoh: "/images/foto.jpg" → "https://example.com/images/foto.jpg"
     */
    private String resolveUrl(String url, String baseUrl) {
        if (url.startsWith("http")) {
            return url; // sudah absolute
        }
        try {
            java.net.URL base = new java.net.URL(baseUrl);
            return new java.net.URL(base, url).toString();
        } catch (Exception e) {
            return url;
        }
    }

    /**
     * Bersihkan teks konten: hapus whitespace berlebih.
     */
    private String cleanContent(String text) {
        if (text == null) return null;
        // Hapus multiple whitespace/newline berlebih
        return text.replaceAll("\\s{3,}", "\n\n").trim();
    }

    /**
     * Result class untuk encapsulate hasil scraping.
     * Menggunakan record (Java 16+) agar immutable dan ringkas.
     */
    public record ScrapeResult(
        String content,       // Konten teks lengkap artikel
        String thumbnailUrl,  // URL thumbnail (jika ditemukan)
        boolean success       // Apakah scraping berhasil?
    ) {}
}
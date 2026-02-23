package com.app.news_aggregator.crawler;

import com.app.news_aggregator.model.Article;
import com.app.news_aggregator.model.Source;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * RssFeedParser bertanggung jawab untuk:
 * 1. Fetch RSS feed dari URL sumber
 * 2. Parse XML feed menjadi list SyndEntry (format internal Rome)
 * 3. Konversi SyndEntry ke entity Article
 *
 * Rome library mendukung semua format feed populer:
 * - RSS 0.9, 1.0, 2.0
 * - Atom 0.3, 1.0
 *
 * @Component berarti class ini dikelola oleh Spring sebagai bean.
 */
@Slf4j
@Component
public class RssFeedParser {

    // Timeout koneksi ke RSS feed dalam millisecond
    private static final int CONNECTION_TIMEOUT_MS = 10_000;

    /**
     * Method utama: fetch dan parse RSS feed dari sumber.
     *
     * @param source Sumber RSS yang akan di-parse
     * @return List artikel hasil parsing (belum disimpan ke DB)
     */
    public List<Article> parseFeed(Source source) {
        log.info("Mulai parsing feed dari sumber: {} ({})", source.getName(), source.getUrl());
        List<Article> articles = new ArrayList<>();

        try {
            // ---- Step 1: Fetch XML dari URL ----
            // SyndFeedInput adalah parser utama dari Rome library
            // XmlReader menangani encoding dan format XML secara otomatis
            URL feedUrl = new URL(source.getUrl());
            SyndFeedInput input = new SyndFeedInput();

            // XmlReader membuka koneksi HTTP dan membaca XML feed
            try (XmlReader reader = new XmlReader(feedUrl)) {
                SyndFeed feed = input.build(reader);

                log.debug("Feed '{}' berhasil difetch, jumlah entry: {}",
                        source.getName(), feed.getEntries().size());

                // ---- Step 2: Konversi setiap entry ke Article ----
                for (SyndEntry entry : feed.getEntries()) {
                    try {
                        Article article = convertEntryToArticle(entry, source);
                        if (article != null) {
                            articles.add(article);
                        }
                    } catch (Exception e) {
                        // Jika satu entry gagal dikonversi, skip dan lanjut ke entry berikutnya
                        // Jangan stop semua karena satu entry bermasalah
                        log.warn("Gagal konversi entry '{}': {}", entry.getTitle(), e.getMessage());
                    }
                }
            }

            log.info("Selesai parsing feed '{}': {} artikel berhasil diparse",
                    source.getName(), articles.size());

        } catch (Exception e) {
            log.error("Gagal fetch/parse feed dari '{}': {}", source.getUrl(), e.getMessage());
            // Re-throw agar CrawlerService bisa tangkap dan catat ke CrawlLog
            throw new RuntimeException("Gagal parse feed dari " + source.getUrl(), e);
        }

        return articles;
    }

    /**
     * Konversi satu SyndEntry (format Rome) ke entity Article.
     *
     * SyndEntry adalah representasi satu item/entry di RSS feed.
     * Setiap entry biasanya berisi: title, link, description, publishedDate, author.
     *
     * @return Article entity, atau null jika entry tidak valid (tidak ada URL)
     */
    private Article convertEntryToArticle(SyndEntry entry, Source source) {
        // URL adalah identifier unik artikel — wajib ada
        String url = entry.getLink();
        if (url == null || url.isBlank()) {
            log.debug("Entry dilewati karena tidak ada URL: {}", entry.getTitle());
            return null;
        }

        // ---- Ekstrak judul ----
        String title = entry.getTitle();
        if (title == null || title.isBlank()) {
            log.debug("Entry dilewati karena tidak ada judul");
            return null;
        }

        // ---- Ekstrak deskripsi/excerpt ----
        // RSS feed biasanya menyediakan deskripsi dalam format HTML
        // Kita bersihkan tag HTML-nya agar jadi teks biasa
        String description = null;
        if (entry.getDescription() != null) {
            String rawDesc = entry.getDescription().getValue();
            // Jsoup.parse().text() menghapus semua tag HTML dan decode HTML entities
            description = cleanHtml(rawDesc);
            // Batasi panjang deskripsi maksimal 500 karakter
            if (description.length() > 500) {
                description = description.substring(0, 497) + "...";
            }
        }

        // ---- Ekstrak tanggal publish ----
        LocalDateTime publishedAt = null;
        Date pubDate = entry.getPublishedDate();
        if (pubDate == null) {
            pubDate = entry.getUpdatedDate(); // fallback ke updated date
        }
        if (pubDate != null) {
            publishedAt = pubDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        } else {
            publishedAt = LocalDateTime.now(); // fallback ke waktu sekarang
        }

        // ---- Ekstrak penulis ----
        String author = null;
        if (entry.getAuthor() != null && !entry.getAuthor().isBlank()) {
            author = entry.getAuthor();
        }

        // ---- Ekstrak GUID ----
        // GUID adalah identifier unik dari sisi sumber RSS
        String guid = entry.getUri();
        if (guid == null || guid.isBlank()) {
            guid = url; // fallback ke URL jika tidak ada GUID
        }

        // ---- Ekstrak thumbnail ----
        // Beberapa RSS feed menyertakan gambar via media:thumbnail atau enclosure
        String thumbnailUrl = extractThumbnail(entry);

        // ---- Build Article entity ----
        return Article.builder()
                .source(source)
                .title(title.trim())
                .url(url.trim())
                .guid(guid)
                .description(description)
                .thumbnailUrl(thumbnailUrl)
                .author(author)
                .category(source.getCategory()) // inherit kategori dari sumber
                .publishedAt(publishedAt)
                .isScraped(false) // konten lengkap belum di-scrape
                .viewCount(0L)
                .build();
    }

    /**
     * Ekstrak URL thumbnail dari RSS entry.
     * Feed berbeda-beda cara menyimpan gambar:
     * - media:thumbnail (format Media RSS)
     * - enclosure (format podcast/media)
     */
    private String extractThumbnail(SyndEntry entry) {
        // Coba dari media:content atau media:thumbnail via foreign markup
        // Rome menyimpan extension elements di getModules()
        try {
            // Cek via enclosure (gambar yang dilampirkan ke entry)
            if (entry.getEnclosures() != null && !entry.getEnclosures().isEmpty()) {
                var enclosure = entry.getEnclosures().get(0);
                if (enclosure.getType() != null && enclosure.getType().startsWith("image/")) {
                    return enclosure.getUrl();
                }
            }

            // Cek via media module (MediaEntryModule)
            var mediaModule = entry.getModule("http://search.yahoo.com/mrss/");
            if (mediaModule instanceof com.rometools.modules.mediarss.MediaEntryModule media) {
                if (media.getMediaContents() != null && media.getMediaContents().length > 0) {
                    var content = media.getMediaContents()[0];
                    if (content.getReference() != null) {
                        return content.getReference().toString();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Tidak bisa ekstrak thumbnail: {}", e.getMessage());
        }

        return null; // tidak ada thumbnail
    }

    /**
     * Bersihkan HTML dari string: hapus semua tag HTML, decode HTML entities.
     * Contoh: "<p>Hello &amp; World</p>" → "Hello & World"
     */
    private String cleanHtml(String html) {
        if (html == null) return null;
        return Jsoup.parse(html).text();
    }
}
package com.app.news_aggregator.service;

import com.app.news_aggregator.crawler.RssFeedParser;
import com.app.news_aggregator.model.Article;
import com.app.news_aggregator.model.CrawlLog;
import com.app.news_aggregator.model.Source;
import com.app.news_aggregator.repository.ArticleRepository;
import com.app.news_aggregator.repository.CrawlLogRepository;
import com.app.news_aggregator.repository.SourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CrawlerService adalah orchestrator utama proses crawling.
 *
 * Alur kerja untuk satu sumber:
 * 1. Fetch dan parse RSS feed → dapat daftar artikel
 * 2. Filter duplikat (cek URL/GUID yang sudah ada di DB)
 * 3. Simpan artikel baru ke database
 * 4. Update status source (lastCrawledAt, crawlStatus)
 * 5. Catat hasil crawl ke CrawlLog
 *
 * @Async memungkinkan crawlSource() dipanggil secara asinkron
 * (tidak memblokir thread pemanggil).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerService {

    private final RssFeedParser rssFeedParser;
    private final ArticleRepository articleRepository;
    private final SourceRepository sourceRepository;
    private final CrawlLogRepository crawlLogRepository;

    /**
     * Crawl SEMUA sumber aktif.
     * Dipanggil oleh RssCrawlerScheduler setiap 15 menit.
     * Setiap sumber di-crawl secara asinkron agar tidak saling menunggu.
     */
    public void crawlAllActiveSources() {
        List<Source> activeSources = sourceRepository.findByIsActiveTrue();
        log.info("Memulai crawl untuk {} sumber aktif", activeSources.size());

        for (Source source : activeSources) {
            // Panggil asinkron: setiap source diproses di thread terpisah
            // Sehingga crawl 50 sumber tidak harus menunggu satu per satu
            crawlSourceAsync(source);
        }
    }

    /**
     * Crawl satu sumber secara ASINKRON.
     * @Async memastikan method ini dijalankan di thread pool terpisah.
     * "crawlerExecutor" adalah nama thread pool yang kita definisikan di AppConfig.
     */
    @Async("crawlerExecutor")
    public void crawlSourceAsync(Source source) {
        crawlSource(source);
    }

    /**
     * Crawl satu sumber secara SINKRON.
     * Bisa dipanggil langsung dari controller untuk trigger manual.
     * Juga dipanggil oleh crawlSourceAsync().
     *
     * @return CrawlLog hasil crawl (untuk keperluan response API)
     */
    @Transactional
    public CrawlLog crawlSource(Source source) {
        log.info("Memulai crawl sumber: {} (ID: {})", source.getName(), source.getId());
        long startTime = System.currentTimeMillis();

        // Objek untuk mencatat hasil crawl
        CrawlLog.CrawlLogBuilder logBuilder = CrawlLog.builder().source(source);

        try {
            // ---- Step 1: Parse RSS feed ----
            List<Article> parsedArticles = rssFeedParser.parseFeed(source);
            logBuilder.articlesFound(parsedArticles.size());

            // ---- Step 2: Filter duplikat dan simpan artikel baru ----
            int savedCount = saveNewArticles(parsedArticles);
            logBuilder.articlesSaved(savedCount);

            // ---- Step 3: Update status source ----
            source.setLastCrawledAt(LocalDateTime.now());
            source.setCrawlStatus(Source.CrawlStatus.SUCCESS);
            source.setErrorMessage(null); // clear error sebelumnya
            sourceRepository.save(source);

            // ---- Step 4: Catat log sukses ----
            long duration = System.currentTimeMillis() - startTime;
            CrawlLog crawlLog = logBuilder
                    .status(CrawlLog.CrawlStatus.SUCCESS)
                    .durationMs(duration)
                    .build();

            CrawlLog saved = crawlLogRepository.save(crawlLog);

            log.info("Crawl selesai untuk '{}': {} artikel ditemukan, {} artikel baru disimpan ({}ms)",
                    source.getName(), parsedArticles.size(), savedCount, duration);

            return saved;

        } catch (Exception e) {
            // ---- Handle error ----
            long duration = System.currentTimeMillis() - startTime;
            log.error("Crawl gagal untuk '{}': {}", source.getName(), e.getMessage());

            // Update status source ke ERROR
            source.setCrawlStatus(Source.CrawlStatus.ERROR);
            source.setErrorMessage(e.getMessage());
            sourceRepository.save(source);

            // Catat log gagal
            CrawlLog crawlLog = logBuilder
                    .status(CrawlLog.CrawlStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .durationMs(duration)
                    .build();

            return crawlLogRepository.save(crawlLog);
        }
    }

    /**
     * Filter duplikat dan simpan hanya artikel baru.
     *
     * Cek duplikat berdasarkan:
     * 1. URL artikel (primary check)
     * 2. GUID dari RSS feed (secondary check)
     *
     * @return Jumlah artikel baru yang berhasil disimpan
     */
    private int saveNewArticles(List<Article> articles) {
        List<Article> newArticles = new ArrayList<>();

        for (Article article : articles) {
            boolean isDuplicate = false;

            // Cek duplikat via URL
            if (article.getUrl() != null && articleRepository.existsByUrl(article.getUrl())) {
                isDuplicate = true;
            }

            // Cek duplikat via GUID (jika URL belum ada tapi GUID sudah ada)
            if (!isDuplicate && article.getGuid() != null
                    && !article.getGuid().equals(article.getUrl())
                    && articleRepository.existsByGuid(article.getGuid())) {
                isDuplicate = true;
            }

            if (!isDuplicate) {
                newArticles.add(article);
            } else {
                log.debug("Artikel duplikat dilewati: {}", article.getUrl());
            }
        }

        if (!newArticles.isEmpty()) {
            // saveAll lebih efisien daripada save satu per satu (batch insert)
            articleRepository.saveAll(newArticles);
            log.debug("Berhasil menyimpan {} artikel baru", newArticles.size());
        }

        return newArticles.size();
    }
}
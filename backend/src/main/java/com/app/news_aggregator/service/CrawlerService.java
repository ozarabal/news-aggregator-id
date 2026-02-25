package com.app.news_aggregator.service;

import com.app.news_aggregator.crawler.RssFeedParser;
import com.app.news_aggregator.model.Article;
import com.app.news_aggregator.model.CrawlLog;
import com.app.news_aggregator.model.Source;
import com.app.news_aggregator.repository.ArticleRepository;
import com.app.news_aggregator.repository.CrawlLogRepository;
import com.app.news_aggregator.repository.SourceRepository;
import com.app.news_aggregator.queue.CrawlProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CrawlerService adalah orchestrator utama proses crawling.
 *
 * Perubahan di Phase 3 (RabbitMQ):
 * - crawlAllActiveSources() → kirim pesan ke queue via CrawlProducer
 * - crawlSource() → tetap sinkron, dipanggil oleh CrawlConsumer (Worker)
 * - setelah simpan artikel → kirim task scraping ke scrape.article.queue
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerService {

    private final RssFeedParser rssFeedParser;
    private final ArticleRepository articleRepository;
    private final SourceRepository sourceRepository;
    private final CrawlLogRepository crawlLogRepository;
    private final CrawlProducer crawlProducer;
    private final ArticleService articleService;

    /**
     * Enqueue crawl semua sumber aktif ke RabbitMQ.
     * Dipanggil oleh Scheduler setiap 15 menit.
     * Di Phase 3: tidak crawl langsung, tapi kirim pesan ke queue.
     */
    public void crawlAllActiveSources() {
        List<Source> activeSources = sourceRepository.findByIsActiveTrue();
        log.info("Mengirim {} task crawl ke RabbitMQ queue", activeSources.size());
        crawlProducer.enqueueCrawlSources(activeSources);
    }

    /**
     * Crawl satu sumber secara sinkron.
     * Dipanggil oleh CrawlConsumer (Worker) saat consume pesan dari queue.
     * Juga bisa dipanggil langsung dari CrawlerController (manual trigger).
     */
    @Transactional
    public CrawlLog crawlSource(Source source) {
        log.info("Crawl sumber: {} (ID: {})", source.getName(), source.getId());
        long startTime = System.currentTimeMillis();
        CrawlLog.CrawlLogBuilder logBuilder = CrawlLog.builder().source(source);

        try {
            // Step 1: Parse RSS feed
            List<Article> parsedArticles = rssFeedParser.parseFeed(source);
            logBuilder.articlesFound(parsedArticles.size());

            // Step 2: Filter duplikat dan simpan artikel baru
            // Sekarang return List<Article> (bukan int) karena butuh object untuk enqueue
            List<Article> savedArticles = saveNewArticles(parsedArticles);
            logBuilder.articlesSaved(savedArticles.size());

            // Step 3: Kirim task scraping ke queue (BUKAN langsung scrape)
            // ScrapeConsumer yang akan proses satu per satu secara async via RabbitMQ
            if (!savedArticles.isEmpty()) {
                articleService.invalidateArticleCache();
                crawlProducer.enqueueScrapeArticles(savedArticles);
            }

            // Step 4: Update status source
            source.setLastCrawledAt(LocalDateTime.now());
            source.setCrawlStatus(Source.CrawlStatus.SUCCESS);
            source.setErrorMessage(null);
            sourceRepository.save(source);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Crawl selesai '{}': {} ditemukan, {} baru ({}ms)",
                    source.getName(), parsedArticles.size(), savedArticles.size(), duration);

            return crawlLogRepository.save(logBuilder
                    .status(CrawlLog.CrawlStatus.SUCCESS)
                    .durationMs(duration).build());

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Crawl gagal '{}': {}", source.getName(), e.getMessage(), e);
            source.setCrawlStatus(Source.CrawlStatus.ERROR);
            source.setErrorMessage(e.getMessage());
            sourceRepository.save(source);

            return crawlLogRepository.save(logBuilder
                    .status(CrawlLog.CrawlStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .durationMs(duration).build());
        }
    }

    /**
     * Filter duplikat, simpan artikel baru, return list artikel yang tersimpan.
     * Return List<Article> (bukan int) karena butuh ID artikel untuk enqueue scraping.
     */
    private List<Article> saveNewArticles(List<Article> articles) {
        List<Article> newArticles = new ArrayList<>();

        for (Article article : articles) {
            boolean isDuplicate = article.getUrl() != null
                    && articleRepository.existsByUrl(article.getUrl());

            if (!isDuplicate && article.getGuid() != null
                    && !article.getGuid().equals(article.getUrl())) {
                isDuplicate = articleRepository.existsByGuid(article.getGuid());
            }

            if (!isDuplicate) newArticles.add(article);
        }

        if (!newArticles.isEmpty()) {
            newArticles = articleRepository.saveAll(newArticles); // return dengan ID terisi
        }

        log.debug("{} artikel baru dari {} total di feed", newArticles.size(), articles.size());
        return newArticles;
    }
}
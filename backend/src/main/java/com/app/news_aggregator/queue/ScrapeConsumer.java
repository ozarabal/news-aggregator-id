package com.app.news_aggregator.queue;

import com.app.news_aggregator.config.RabbitMQConfig;
import com.app.news_aggregator.crawler.ArticleScraper;
import com.app.news_aggregator.exception.ResourceNotFoundException;
import com.app.news_aggregator.model.Article;
import com.app.news_aggregator.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * ScrapeConsumer adalah Worker yang mengkonsumsi pesan dari scrape.article.queue.
 *
 * Setiap kali ada artikel baru masuk dari RSS crawl, CrawlProducer mengirim
 * pesan ke scrape.article.queue. ScrapeConsumer yang kemudian scrape
 * konten lengkap artikel tersebut dari website aslinya.
 *
 * Dengan queue, scraping tidak memblokir proses crawl RSS:
 * - Crawl RSS berjalan cepat → simpan artikel (tanpa konten) → kirim ke queue
 * - ScrapeConsumer memproses satu per satu di background dengan rate yang aman
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScrapeConsumer {

    private final ArticleScraper articleScraper;
    private final ArticleRepository articleRepository;

    /**
     * Listen ke scrape.article.queue.
     *
     * Setiap pesan berisi articleId yang perlu di-scrape.
     * Consumer mengambil artikel dari DB, scrape kontennya, lalu update DB.
     */
    @Transactional
    @RabbitListener(queues = RabbitMQConfig.QUEUE_SCRAPE_ARTICLE)
    public void consumeScrapeArticle(CrawlMessage.ScrapeArticleMessage message) {
        log.debug("[WORKER] Menerima task scrape untuk artikel ID: {}, URL: {}",
                message.getArticleId(), message.getArticleUrl());

        try {
            // Ambil artikel dari database
            Article article = articleRepository.findById(message.getArticleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Article", message.getArticleId()));

            // Skip jika sudah di-scrape (bisa terjadi jika pesan duplikat di queue)
            if (article.getIsScraped()) {
                log.debug("[WORKER] Artikel ID {} sudah di-scrape sebelumnya, skip",
                        message.getArticleId());
                return;
            }

            // Scrape konten dari URL artikel
            ArticleScraper.ScrapeResult result = articleScraper.scrape(article.getUrl());

            // Update artikel dengan hasil scraping
            if (result.success() && result.content() != null) {
                article.setContent(result.content());
            }

            // Update thumbnail jika belum ada dari RSS feed
            if (article.getThumbnailUrl() == null && result.thumbnailUrl() != null) {
                article.setThumbnailUrl(result.thumbnailUrl());
            }

            // Tandai sudah di-scrape (berhasil maupun tidak)
            article.setIsScraped(true);
            articleRepository.save(article);

            // Delay kecil agar tidak terlalu agresif hit website sumber
            // Rate limiting sederhana: 500ms per request scraping
            Thread.sleep(500);

            log.debug("[WORKER] Scrape selesai untuk artikel ID: {}", message.getArticleId());

        } catch (ResourceNotFoundException e) {
            // Artikel tidak ditemukan (mungkin sudah dihapus) — tidak perlu retry
            log.warn("[WORKER] Artikel ID {} tidak ditemukan, task diabaikan",
                    message.getArticleId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[WORKER] Scraping diinterupsi untuk artikel ID: {}", message.getArticleId());

        } catch (Exception e) {
            log.error("[WORKER] Gagal scrape artikel ID {}: {}",
                    message.getArticleId(), e.getMessage());
            // Throw agar pesan masuk DLQ setelah retry habis
            throw new RuntimeException("Gagal scrape artikel: " + message.getArticleUrl(), e);
        }
    }
}
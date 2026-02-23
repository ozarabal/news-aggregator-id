package com.app.news_aggregator.service;

import com.app.news_aggregator.crawler.*;
import com.app.news_aggregator.model.Article;
import com.app.news_aggregator.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ArticleScraperService mengelola proses scraping konten lengkap artikel.
 *
 * Kenapa dipisah dari CrawlerService?
 * - Crawl RSS harus cepat (berjalan setiap 15 menit)
 * - Scraping konten bisa lambat (buka satu halaman = 1-5 detik)
 * - Jika 50 artikel baru, scraping langsung = 50-250 detik blocking
 *
 * Solusinya: simpan artikel dulu tanpa konten (isScraped=false),
 * lalu job ini akan scrape secara bertahap di background.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleScraperService {

    private final ArticleScraper articleScraper;
    private final ArticleRepository articleRepository;

    // Berapa artikel yang di-scrape per satu kali jalan
    private static final int BATCH_SIZE = 10;

    /**
     * Scrape batch artikel yang belum punya konten lengkap.
     * Dipanggil oleh RssCrawlerScheduler setiap 5 menit.
     *
     * Mengambil BATCH_SIZE artikel dengan isScraped=false,
     * lalu scrape satu per satu dengan delay kecil antar request
     * agar tidak terdeteksi sebagai bot agresif.
     */
    @Transactional
    public void scrapeUnscrapedArticles() {
        // Ambil artikel yang belum di-scrape (batch kecil)
        List<Article> unscraped = articleRepository.findUnscrapedArticles(
                PageRequest.of(0, BATCH_SIZE)
        );

        if (unscraped.isEmpty()) {
            log.debug("Tidak ada artikel yang perlu di-scrape");
            return;
        }

        log.info("Memulai scraping {} artikel yang belum punya konten lengkap", unscraped.size());
        int successCount = 0;

        for (Article article : unscraped) {
            try {
                // Scrape konten dari URL artikel
                ArticleScraper.ScrapeResult result = articleScraper.scrape(article.getUrl());

                if (result.success()) {
                    // Update artikel dengan konten hasil scraping
                    if (result.content() != null) {
                        article.setContent(result.content());
                    }

                    // Update thumbnail jika belum ada dari RSS feed
                    if (article.getThumbnailUrl() == null && result.thumbnailUrl() != null) {
                        article.setThumbnailUrl(result.thumbnailUrl());
                    }

                    successCount++;
                }

                // Tandai sebagai sudah di-scrape (baik berhasil maupun gagal)
                // agar tidak terus dicoba berulang untuk artikel yang memang tidak bisa di-scrape
                article.setIsScraped(true);
                articleRepository.save(article);

                // Delay kecil antar request: 500ms - 1500ms (random)
                // Mencegah website mendeteksi pola scraping otomatis
                Thread.sleep(500 + (long) (Math.random() * 1000));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Scraping diinterupsi");
                break;
            } catch (Exception e) {
                log.warn("Gagal scrape artikel ID {}: {}", article.getId(), e.getMessage());
                // Tetap tandai sebagai scraped agar tidak retry terus
                article.setIsScraped(true);
                articleRepository.save(article);
            }
        }

        log.info("Scraping selesai: {}/{} artikel berhasil di-scrape", successCount, unscraped.size());
    }
}
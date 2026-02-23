package com.app.news_aggregator.crawler;


import com.app.news_aggregator.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * RssCrawlerScheduler mengatur jadwal semua background job di aplikasi.
 *
 * @Scheduled memiliki dua mode:
 * 1. fixedRate  : jalankan setiap X millisecond (dari awal ke awal)
 * 2. fixedDelay : jalankan X millisecond SETELAH job sebelumnya selesai
 * 3. cron       : jadwal format cron ("second minute hour day month weekday")
 *
 * Perbedaan fixedRate vs fixedDelay:
 * - fixedRate  : jika crawl butuh 5 menit, job baru dimulai setelah 15 menit dari start
 * - fixedDelay : jika crawl butuh 5 menit, job baru dimulai 15 menit setelah selesai (total 20 menit)
 *
 * Untuk crawler kita pakai fixedDelay agar tidak overlap jika crawl lama.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RssCrawlerScheduler {

    private final CrawlerService crawlerService;
    private final ArticleScraperService articleScraperService;

    /**
     * Crawl semua sumber RSS aktif.
     *
     * Jadwal: setiap 15 menit SETELAH job sebelumnya selesai.
     * initialDelay: tunggu 30 detik setelah aplikasi start sebelum crawl pertama.
     *               Memberi waktu Spring untuk fully initialized.
     *
     * 15 menit = 15 * 60 * 1000 = 900_000 millisecond
     */
    @Scheduled(fixedDelay = 900_000, initialDelay = 30_000)
    public void scheduledRssCrawl() {
        log.info("=== [SCHEDULER] Memulai crawl RSS pada {} ===",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        try {
            crawlerService.crawlAllActiveSources();
        } catch (Exception e) {
            // Tangkap exception agar Scheduler tidak berhenti karena error satu kali jalan
            log.error("[SCHEDULER] Error saat crawl: {}", e.getMessage(), e);
        }

        log.info("=== [SCHEDULER] Crawl RSS selesai diinisiasi ===");
    }

    /**
     * Scrape konten lengkap artikel yang belum di-scrape.
     *
     * Jadwal: setiap 5 menit, dimulai 60 detik setelah aplikasi start.
     * Lebih sering dari crawl karena scraping batch kecil (10 artikel per run).
     *
     * 5 menit = 5 * 60 * 1000 = 300_000 millisecond
     */
    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void scheduledArticleScraping() {
        log.debug("[SCHEDULER] Memulai scraping artikel belum ter-scrape");

        try {
            articleScraperService.scrapeUnscrapedArticles();
        } catch (Exception e) {
            log.error("[SCHEDULER] Error saat scraping artikel: {}", e.getMessage(), e);
        }
    }

    /**
     * Cleanup artikel lama untuk menjaga ukuran database.
     *
     * Jadwal: setiap hari jam 02.00 pagi (waktu sepi traffic).
     * Cron format: "detik menit jam hari bulan hari-minggu"
     * "0 0 2 * * *" = jam 02:00:00 setiap hari
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledCleanup() {
        log.info("[SCHEDULER] Memulai cleanup artikel lama");
        // Akan diimplementasi: hapus artikel > 30 hari
        // articleService.deleteOldArticles(30);
        log.info("[SCHEDULER] Cleanup selesai");
    }
}
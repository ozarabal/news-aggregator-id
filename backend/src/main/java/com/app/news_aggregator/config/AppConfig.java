package com.app.news_aggregator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * AppConfig berisi konfigurasi infrastruktur aplikasi.
 *
 * @Configuration berarti class ini berisi @Bean definitions —
 * Spring akan membaca dan mendaftarkan semua bean yang ada di sini.
 */
@Configuration
public class AppConfig {

    /**
     * Thread pool untuk background job crawling.
     *
     * Kenapa perlu thread pool khusus?
     * - @Async("crawlerExecutor") merujuk ke bean ini
     * - Membatasi jumlah thread agar tidak overload server
     * - Jika ada 100 sumber RSS, tidak akan membuat 100 thread sekaligus
     *
     * Konfigurasi:
     * - corePoolSize  : jumlah thread yang selalu siap (idle threads)
     * - maxPoolSize   : maksimal thread yang bisa dibuat saat beban tinggi
     * - queueCapacity : antrian task jika semua thread sedang sibuk
     * - threadNamePrefix : prefix nama thread untuk mudah debugging di log
     */
    @Bean(name = "crawlerExecutor")
    public Executor crawlerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(3);        // 3 thread selalu siap
        executor.setMaxPoolSize(10);        // Maksimal 10 thread paralel
        executor.setQueueCapacity(50);      // Tampung 50 task dalam antrian
        executor.setThreadNamePrefix("crawler-");
        executor.setWaitForTasksToCompleteOnShutdown(true);  // Tunggu task selesai saat shutdown
        executor.setAwaitTerminationSeconds(30);              // Maksimal tunggu 30 detik

        executor.initialize();
        return executor;
    }
}
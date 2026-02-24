package com.app.news_aggregator.queue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * CrawlMessage adalah "amplop" pesan yang dikirim ke RabbitMQ.
 *
 * Kenapa tidak kirim langsung ID saja (Long)?
 * - Pesan yang informatif lebih mudah di-debug di RabbitMQ Management UI
 * - Bisa tambahkan metadata: kapan dikirim, prioritas, retry count, dll
 * - Jackson bisa serialisasi/deserilisasi class ini ke/dari JSON dengan mudah
 *
 * Contoh JSON yang dikirim ke queue:
 * {
 *   "sourceId": 1,
 *   "sourceName": "CNN Indonesia",
 *   "enqueuedAt": "2024-01-01T10:00:00",
 *   "retryCount": 0
 * }
 */
public class CrawlMessage {

    /**
     * Pesan untuk task crawl RSS feed satu sumber.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrawlRssMessage {
        private Long sourceId;
        private String sourceName;
        private String sourceUrl;

        @Builder.Default
        private LocalDateTime enqueuedAt = LocalDateTime.now();

        @Builder.Default
        private int retryCount = 0;
    }

    /**
     * Pesan untuk task scraping konten artikel.
     * Dikirim setelah artikel baru berhasil disimpan dari RSS feed.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScrapeArticleMessage {
        private Long articleId;
        private String articleUrl;
        private String articleTitle;

        @Builder.Default
        private LocalDateTime enqueuedAt = LocalDateTime.now();

        @Builder.Default
        private int retryCount = 0;
    }
}
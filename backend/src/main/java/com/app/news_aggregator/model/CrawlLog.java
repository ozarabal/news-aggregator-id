package com.app.news_aggregator.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity CrawlLog menyimpan history setiap kali proses crawl dijalankan.
 * Berguna untuk monitoring: apakah crawl berhasil? berapa artikel ditemukan? dll.
 */
@Entity
@Table(name = "crawl_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CrawlStatus status;

    @Column(name = "articles_found")
    @Builder.Default
    private Integer articlesFound = 0;      // Jumlah artikel ditemukan di feed

    @Column(name = "articles_saved")
    @Builder.Default
    private Integer articlesSaved = 0;      // Jumlah artikel baru yang berhasil disimpan

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "duration_ms")
    private Long durationMs;                // Durasi crawl dalam millisecond

    @Column(name = "crawled_at", nullable = false)
    @Builder.Default
    private LocalDateTime crawledAt = LocalDateTime.now();

    public enum CrawlStatus {
        SUCCESS, FAILED
    }
}
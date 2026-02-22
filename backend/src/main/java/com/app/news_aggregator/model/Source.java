package com.app.news_aggregator.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sources")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Source extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;                    // Nama sumber, contoh: "CNN Indonesia"

    @Column(nullable = false, unique = true)
    private String url;                     // URL RSS feed

    @Column(name = "website_url")
    private String websiteUrl;             // URL website aslinya

    @Column(nullable = false)
    private String category;               // Kategori: teknologi, bisnis, olahraga, dll

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;       // Apakah sumber ini aktif di-crawl?

    @Column(name = "last_crawled_at")
    private LocalDateTime lastCrawledAt;   // Kapan terakhir kali berhasil di-crawl

    @Column(name = "crawl_status")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CrawlStatus crawlStatus = CrawlStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @OneToMany(mappedBy = "source", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @Builder.Default
    private List<Article> articles = new ArrayList<>();

    public enum CrawlStatus {
        PENDING,    
        SUCCESS,    
        ERROR
    }
}

package com.app.news_aggregator.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity Article merepresentasikan tabel 'articles' di database.
 * Setiap Article adalah satu berita hasil crawl dari RSS feed.
 *
 * Relasi: Banyak Article dimiliki oleh satu Source (Many-to-One).
 */
@Entity
@Table(name = "articles",
       indexes = {
           @Index(name = "idx_articles_category", columnList = "category"),
           @Index(name = "idx_articles_published_at", columnList = "published_at DESC"),
           @Index(name = "idx_articles_source_id", columnList = "source_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Article extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Relasi Many-to-One ke Source.
     * FetchType.LAZY = Source TIDAK langsung di-load saat Article di-fetch.
     * @JoinColumn menentukan nama kolom foreign key di tabel articles.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @Column(nullable = false, length = 500)
    private String title;                   

    @Column(nullable = false, unique = true, length = 500)
    private String url;                     // URL artikel asli (UNIK - untuk cek duplikat)

    @Column(length = 500)
    private String guid;                    // GUID dari RSS feed

    @Column(columnDefinition = "TEXT")
    private String description;             // Ringkasan/excerpt

    @Column(columnDefinition = "TEXT")
    private String content;                 // Konten lengkap (hasil scraping)

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;            // URL gambar thumbnail

    @Column(length = 255)
    private String author;                  // Nama penulis

    @Column(length = 100)
    private String category;               // Kategori artikel

    @Column(name = "published_at")
    private LocalDateTime publishedAt;      // Waktu artikel diterbitkan

    @Column(name = "is_scraped", nullable = false)
    @Builder.Default
    private Boolean isScraped = false;      // Apakah konten lengkap sudah di-scrape?

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Long viewCount = 0L;            // Jumlah view artikel
}
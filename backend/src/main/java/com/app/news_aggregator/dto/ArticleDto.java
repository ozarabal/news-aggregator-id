package com.app.news_aggregator.dto;

import com.app.news_aggregator.model.Article;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO untuk entity Article.
 * Dibedakan antara Summary (untuk list) dan Detail (untuk single article).
 * Ini penting untuk performa: list artikel tidak perlu kirim 'content' yang bisa sangat panjang.
 */
public class ArticleDto {

    /**
     * Summary dipakai untuk tampilkan list artikel (halaman utama, search hasil).
     * Tidak mengandung field 'content' yang bisa sangat panjang.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Ringkasan artikel untuk ditampilkan di list")
    public static class Summary {

        @Schema(description = "ID unik artikel", example = "1")
        private Long id;

        @Schema(description = "Judul artikel", example = "Google Luncurkan AI Terbaru")
        private String title;

        @Schema(description = "URL artikel asli", example = "https://cnnindonesia.com/teknologi/...")
        private String url;

        @Schema(description = "Deskripsi singkat artikel", nullable = true, example = "Google hari ini mengumumkan...")
        private String description;

        @Schema(description = "URL thumbnail/gambar artikel", nullable = true, example = "https://cdn.cnn.com/image.jpg")
        private String thumbnailUrl;

        @Schema(description = "Nama penulis artikel", nullable = true, example = "John Doe")
        private String author;

        @Schema(description = "Kategori artikel", example = "teknologi")
        private String category;

        @Schema(description = "Nama sumber RSS", example = "CNN Indonesia - Teknologi")
        private String sourceName;

        @Schema(description = "ID sumber RSS", example = "1")
        private Long sourceId;

        @Schema(description = "Waktu publikasi artikel (ISO 8601)", example = "2024-01-15T08:30:00")
        private LocalDateTime publishedAt;

        @Schema(description = "Jumlah total kunjungan artikel", example = "42")
        private Long viewCount;

        public static Summary from(Article article) {
            return Summary.builder()
                    .id(article.getId())
                    .title(article.getTitle())
                    .url(article.getUrl())
                    .description(article.getDescription())
                    .thumbnailUrl(article.getThumbnailUrl())
                    .author(article.getAuthor())
                    .category(article.getCategory())
                    .sourceName(article.getSource() != null ? article.getSource().getName() : null)
                    .sourceId(article.getSource() != null ? article.getSource().getId() : null)
                    .publishedAt(article.getPublishedAt())
                    .viewCount(article.getViewCount())
                    .build();
        }
    }

    /**
     * Detail dipakai untuk tampilkan satu artikel lengkap.
     * Mengandung semua field termasuk 'content'.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Detail lengkap artikel termasuk konten hasil scraping")
    public static class Detail {

        @Schema(description = "ID unik artikel", example = "1")
        private Long id;

        @Schema(description = "Judul artikel", example = "Google Luncurkan AI Terbaru")
        private String title;

        @Schema(description = "URL artikel asli", example = "https://cnnindonesia.com/teknologi/...")
        private String url;

        @Schema(description = "Deskripsi singkat artikel", nullable = true)
        private String description;

        @Schema(description = "Konten lengkap artikel hasil scraping. Null jika belum di-scrape (isScraped=false)", nullable = true)
        private String content;

        @Schema(description = "URL thumbnail/gambar artikel", nullable = true)
        private String thumbnailUrl;

        @Schema(description = "Nama penulis artikel", nullable = true, example = "John Doe")
        private String author;

        @Schema(description = "Kategori artikel", example = "teknologi")
        private String category;

        @Schema(description = "Nama sumber RSS", example = "CNN Indonesia - Teknologi")
        private String sourceName;

        @Schema(description = "URL website sumber RSS", nullable = true, example = "https://cnnindonesia.com")
        private String sourceWebsiteUrl;

        @Schema(description = "ID sumber RSS", example = "1")
        private Long sourceId;

        @Schema(description = "Waktu publikasi artikel (ISO 8601)", example = "2024-01-15T08:30:00")
        private LocalDateTime publishedAt;

        @Schema(description = "Jumlah total kunjungan artikel", example = "42")
        private Long viewCount;

        @Schema(description = "True jika konten artikel sudah berhasil di-scrape", example = "true")
        private Boolean isScraped;

        @Schema(description = "Waktu artikel pertama kali disimpan ke database", example = "2024-01-15T08:35:00")
        private LocalDateTime createdAt;

        public static Detail from(Article article) {
            return Detail.builder()
                    .id(article.getId())
                    .title(article.getTitle())
                    .url(article.getUrl())
                    .description(article.getDescription())
                    .content(article.getContent())
                    .thumbnailUrl(article.getThumbnailUrl())
                    .author(article.getAuthor())
                    .category(article.getCategory())
                    .sourceName(article.getSource() != null ? article.getSource().getName() : null)
                    .sourceWebsiteUrl(article.getSource() != null ? article.getSource().getWebsiteUrl() : null)
                    .sourceId(article.getSource() != null ? article.getSource().getId() : null)
                    .publishedAt(article.getPublishedAt())
                    .viewCount(article.getViewCount())
                    .isScraped(article.getIsScraped())
                    .createdAt(article.getCreatedAt())
                    .build();
        }
    }
}

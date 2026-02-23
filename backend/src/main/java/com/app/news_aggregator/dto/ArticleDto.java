package com.app.news_aggregator.dto;

import com.app.news_aggregator.model.Article;
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
    public static class Summary {
        private Long id;
        private String title;
        private String url;
        private String description;
        private String thumbnailUrl;
        private String author;
        private String category;
        private String sourceName;
        private Long sourceId;
        private LocalDateTime publishedAt;
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
    public static class Detail {
        private Long id;
        private String title;
        private String url;
        private String description;
        private String content;
        private String thumbnailUrl;
        private String author;
        private String category;
        private String sourceName;
        private String sourceWebsiteUrl;
        private Long sourceId;
        private LocalDateTime publishedAt;
        private Long viewCount;
        private Boolean isScraped;
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
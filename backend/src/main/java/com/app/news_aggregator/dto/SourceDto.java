package com.app.news_aggregator.dto;

import com.app.news_aggregator.model.Source;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO (Data Transfer Object) untuk entity Source.
 *
 * Kita pisahkan menjadi inner class:
 * - Request: data yang diterima dari client (POST/PUT)
 * - Response: data yang dikirim ke client (GET)
 *
 * Kenapa tidak langsung pakai entity Source?
 * 1. Entity punya field List<Article> yang bisa menyebabkan infinite loop saat serialisasi JSON
 * 2. Kita ingin kontrol penuh atas apa yang dikirim/diterima
 * 3. Validasi input (@NotBlank, dll) lebih baik di DTO, bukan entity
 */
public class SourceDto {

    /**
     * DTO untuk CREATE/UPDATE sumber RSS.
     * Hanya berisi field yang boleh diisi oleh client.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Request body untuk membuat atau mengupdate sumber RSS")
    public static class Request {

        @NotBlank(message = "Nama sumber tidak boleh kosong")
        @Size(max = 255, message = "Nama maksimal 255 karakter")
        @Schema(description = "Nama sumber RSS", example = "CNN Indonesia - Teknologi", requiredMode = Schema.RequiredMode.REQUIRED)
        private String name;

        @NotBlank(message = "URL RSS feed tidak boleh kosong")
        @Size(max = 500, message = "URL maksimal 500 karakter")
        @Schema(description = "URL RSS feed", example = "https://www.cnnindonesia.com/teknologi/rss", requiredMode = Schema.RequiredMode.REQUIRED)
        private String url;

        @Size(max = 500)
        @Schema(description = "URL website utama sumber (opsional)", example = "https://www.cnnindonesia.com", nullable = true)
        private String websiteUrl;

        @NotBlank(message = "Kategori tidak boleh kosong")
        @Pattern(regexp = "^[a-z]+$", message = "Kategori hanya boleh huruf kecil")
        @Schema(description = "Kategori sumber, hanya huruf kecil tanpa spasi", example = "teknologi", requiredMode = Schema.RequiredMode.REQUIRED)
        private String category;

        @Builder.Default
        @Schema(description = "Status aktif sumber. Default true", example = "true")
        private Boolean isActive = true;
    }

    /**
     * DTO untuk response data sumber RSS.
     * Berisi semua informasi yang aman untuk dikirim ke client.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Data sumber RSS")
    public static class Response {

        @Schema(description = "ID unik sumber", example = "1")
        private Long id;

        @Schema(description = "Nama sumber RSS", example = "CNN Indonesia - Teknologi")
        private String name;

        @Schema(description = "URL RSS feed", example = "https://www.cnnindonesia.com/teknologi/rss")
        private String url;

        @Schema(description = "URL website utama sumber", nullable = true, example = "https://www.cnnindonesia.com")
        private String websiteUrl;

        @Schema(description = "Kategori sumber", example = "teknologi")
        private String category;

        @Schema(description = "Apakah sumber aktif dan akan di-crawl secara otomatis", example = "true")
        private Boolean isActive;

        @Schema(description = "Waktu terakhir sumber berhasil di-crawl", nullable = true, example = "2024-01-15T07:00:00")
        private LocalDateTime lastCrawledAt;

        @Schema(description = "Status crawl terakhir", nullable = true, allowableValues = {"PENDING", "SUCCESS", "ERROR"}, example = "SUCCESS")
        private String crawlStatus;

        @Schema(description = "Waktu sumber ditambahkan ke sistem", example = "2024-01-01T00:00:00")
        private LocalDateTime createdAt;

        /**
         * Static factory method: konversi dari entity Source ke SourceDto.Response.
         * Pola ini memudahkan konversi dan menghindari dependency ke mapper library.
         */
        public static Response from(Source source) {
            return Response.builder()
                    .id(source.getId())
                    .name(source.getName())
                    .url(source.getUrl())
                    .websiteUrl(source.getWebsiteUrl())
                    .category(source.getCategory())
                    .isActive(source.getIsActive())
                    .lastCrawledAt(source.getLastCrawledAt())
                    .crawlStatus(source.getCrawlStatus() != null ? source.getCrawlStatus().name() : null)
                    .createdAt(source.getCreatedAt())
                    .build();
        }
    }
}

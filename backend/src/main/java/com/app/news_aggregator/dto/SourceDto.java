package com.app.news_aggregator.dto;

import com.app.news_aggregator.model.Source;
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
    public static class Request {

        @NotBlank(message = "Nama sumber tidak boleh kosong")
        @Size(max = 255, message = "Nama maksimal 255 karakter")
        private String name;

        @NotBlank(message = "URL RSS feed tidak boleh kosong")
        @Size(max = 500, message = "URL maksimal 500 karakter")
        private String url;

        @Size(max = 500)
        private String websiteUrl;

        @NotBlank(message = "Kategori tidak boleh kosong")
        @Pattern(regexp = "^[a-z]+$", message = "Kategori hanya boleh huruf kecil")
        private String category;

        @Builder.Default
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
    public static class Response {
        private Long id;
        private String name;
        private String url;
        private String websiteUrl;
        private String category;
        private Boolean isActive;
        private LocalDateTime lastCrawledAt;
        private String crawlStatus;
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
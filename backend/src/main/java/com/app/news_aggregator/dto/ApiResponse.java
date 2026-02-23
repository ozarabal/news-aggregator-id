package com.app.news_aggregator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ApiResponse adalah wrapper standar untuk semua response API.
 *
 * Tujuannya agar semua response punya format yang konsisten:
 * {
 *   "success": true,
 *   "message": "Berhasil mengambil artikel",
 *   "data": { ... },
 *   "timestamp": "2024-01-01T10:00:00"
 * }
 *
 * Generic type <T> memungkinkan 'data' berisi tipe apa saja:
 * ApiResponse<ArticleDto.Detail>, ApiResponse<Page<ArticleDto.Summary>>, dll.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // ---- Static Factory Methods ----

    /** Response sukses dengan data */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /** Response sukses tanpa data (untuk DELETE, dll) */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    /** Response error */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
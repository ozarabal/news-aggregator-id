package com.app.news_aggregator.controller;

import com.app.news_aggregator.config.RedisConfig;
import com.app.news_aggregator.dto.ApiResponse;
import com.app.news_aggregator.service.ArticleService;
import com.app.news_aggregator.service.SourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * CacheController menyediakan endpoint untuk:
 * 1. Lihat status cache (berapa key yang disimpan di Redis)
 * 2. Manual evict cache tertentu
 * 3. Flush semua cache
 *
 * Endpoint:
 * GET    /api/v1/cache/stats          - Statistik semua cache
 * DELETE /api/v1/cache/{cacheName}    - Hapus semua entri cache tertentu
 * DELETE /api/v1/cache                - Flush semua cache
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ArticleService articleService;
    private final SourceService sourceService;

    /**
     * GET /api/v1/cache/stats
     * Menampilkan jumlah key per cache di Redis.
     *
     * Berguna untuk monitoring: apakah cache terisi? berapa key?
     * Jika jumlah key = 0 padahal sudah ada request → ada masalah konfigurasi.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();

        // Daftar semua cache yang kita definisikan
        List<String> cacheNames = List.of(
            RedisConfig.CACHE_ARTICLES,
            RedisConfig.CACHE_ARTICLE_DETAIL,
            RedisConfig.CACHE_SOURCES,
            RedisConfig.CACHE_CATEGORIES,
            RedisConfig.CACHE_SEARCH
        );

        for (String cacheName : cacheNames) {
            // Hitung jumlah key di Redis untuk cache ini
            // Key format di Redis: "cacheName::*"
            Set<String> keys = redisTemplate.keys(cacheName + "::*");
            int keyCount = keys != null ? keys.size() : 0;

            Map<String, Object> cacheInfo = new HashMap<>();
            cacheInfo.put("keyCount", keyCount);
            cacheInfo.put("keys", keys != null ? new ArrayList<>(keys) : new ArrayList<>());

            stats.put(cacheName, cacheInfo);
        }

        return ResponseEntity.ok(ApiResponse.success("Statistik cache", stats));
    }

    /**
     * DELETE /api/v1/cache/articles
     * Hapus semua cache artikel.
     * Berguna jika artikel di DB diubah secara langsung (bypass aplikasi).
     */
    @DeleteMapping("/articles")
    public ResponseEntity<ApiResponse<Void>> evictArticleCache() {
        articleService.invalidateAllCache();
        log.info("Manual evict: semua cache artikel dihapus");
        return ResponseEntity.ok(ApiResponse.success("Cache artikel berhasil dihapus"));
    }

    /**
     * DELETE /api/v1/cache/sources
     * Hapus cache sumber RSS.
     */
    @DeleteMapping("/sources")
    public ResponseEntity<ApiResponse<Void>> evictSourceCache() {
        Objects.requireNonNull(cacheManager.getCache(RedisConfig.CACHE_SOURCES)).clear();
        Objects.requireNonNull(cacheManager.getCache(RedisConfig.CACHE_CATEGORIES)).clear();
        log.info("Manual evict: cache sources dan categories dihapus");
        return ResponseEntity.ok(ApiResponse.success("Cache sumber berhasil dihapus"));
    }

    /**
     * DELETE /api/v1/cache
     * Flush semua cache — nuclear option.
     * Pakai dengan hati-hati di production karena semua request berikutnya
     * akan hit DB sampai cache terisi kembali (cold start).
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> evictAllCache() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) cache.clear();
        });
        log.warn("Manual evict: SEMUA cache dihapus (flush all)");
        return ResponseEntity.ok(ApiResponse.success("Semua cache berhasil dihapus"));
    }
}
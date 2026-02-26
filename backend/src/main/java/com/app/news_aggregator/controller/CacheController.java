package com.app.news_aggregator.controller;

import com.app.news_aggregator.config.RedisConfig;
import com.app.news_aggregator.dto.ApiResponse;
import com.app.news_aggregator.service.ArticleService;
import com.app.news_aggregator.service.SourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
@Tag(name = "Cache", description = "Monitor dan kelola Redis cache — lihat statistik dan evict cache secara manual")
public class CacheController {

    private final CacheManager cacheManager;
    private final ArticleService articleService;
    private final SourceService sourceService;

    // Optional: hanya tersedia saat spring.cache.type=redis
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * GET /api/v1/cache/stats
     * Menampilkan jumlah key per cache di Redis.
     *
     * Berguna untuk monitoring: apakah cache terisi? berapa key?
     * Jika jumlah key = 0 padahal sudah ada request → ada masalah konfigurasi.
     */
    @GetMapping("/stats")
    @Operation(
        summary = "Statistik Redis cache",
        description = """
            Menampilkan jumlah key yang tersimpan di setiap cache Redis.

            Cache yang dipantau:
            - `articles` — daftar artikel (list, filter, pagination)
            - `article-detail` — detail satu artikel
            - `sources` — daftar sumber RSS
            - `categories` — daftar kategori
            - `search` — hasil pencarian artikel

            Jika `keyCount = 0` padahal sudah ada request → kemungkinan ada masalah konfigurasi Redis.
            """
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();

        if (redisTemplate == null) {
            // Redis tidak aktif (spring.cache.type=simple)
            stats.put("mode", "simple (in-memory)");
            stats.put("info", "Redis tidak aktif. Set spring.cache.type=redis untuk menggunakan Redis.");
            return ResponseEntity.ok(ApiResponse.success("Cache mode: simple (in-memory)", stats));
        }

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
    @Operation(
        summary = "Evict cache artikel",
        description = "Menghapus semua entri cache artikel (`articles`, `article-detail`, `search`). Berguna jika data artikel diubah langsung di database"
    )
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
    @Operation(
        summary = "Evict cache sumber RSS",
        description = "Menghapus semua entri cache sumber RSS (`sources` dan `categories`)"
    )
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
    @Operation(
        summary = "Flush semua cache",
        description = """
            Menghapus **semua** entri dari semua cache sekaligus (*nuclear option*).

            ⚠️ **Perhatian**: Setelah operasi ini, semua request berikutnya akan langsung hit database
            sampai cache terisi kembali (cold start). Gunakan dengan hati-hati di production.
            """
    )
    public ResponseEntity<ApiResponse<Void>> evictAllCache() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) cache.clear();
        });
        log.warn("Manual evict: SEMUA cache dihapus (flush all)");
        return ResponseEntity.ok(ApiResponse.success("Semua cache berhasil dihapus"));
    }
}

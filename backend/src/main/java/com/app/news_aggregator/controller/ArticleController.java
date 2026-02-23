package com.app.news_aggregator.controller;

import com.app.news_aggregator.dto.ApiResponse;
import com.app.news_aggregator.dto.ArticleDto;
import com.app.news_aggregator.service.ArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ArticleController menangani semua HTTP request untuk artikel.
 *
 * Endpoint yang tersedia:
 * GET /api/v1/articles                   - Semua artikel (dengan pagination)
 * GET /api/v1/articles/{id}              - Detail satu artikel
 * GET /api/v1/articles?category=teknologi - Filter by kategori
 * GET /api/v1/articles?search=keyword    - Search by keyword
 * GET /api/v1/articles?sourceId=1        - Filter by sumber
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    /**
     * GET /api/v1/articles
     *
     * Query params:
     * - page: nomor halaman (default: 0)
     * - size: jumlah artikel per halaman (default: 20)
     * - category: filter by kategori (opsional)
     * - search: keyword pencarian (opsional)
     * - sourceId: filter by sumber (opsional)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ArticleDto.Summary>>> getArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long sourceId) {

        Page<ArticleDto.Summary> articles;

        if (search != null && !search.isBlank()) {
            // Mode pencarian
            articles = articleService.searchArticles(search, page, size);
        } else if (category != null && !category.isBlank()) {
            // Mode filter kategori
            articles = articleService.getArticlesByCategory(category, page, size);
        } else if (sourceId != null) {
            // Mode filter by sumber
            articles = articleService.getArticlesBySource(sourceId, page, size);
        } else {
            // Mode default: semua artikel
            articles = articleService.getAllArticles(page, size);
        }

        return ResponseEntity.ok(
            ApiResponse.success("Berhasil mengambil daftar artikel", articles)
        );
    }

    /**
     * GET /api/v1/articles/{id} - Ambil detail satu artikel
     * View count akan otomatis diincrement saat endpoint ini dipanggil.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ArticleDto.Detail>> getArticleById(@PathVariable Long id) {
        ArticleDto.Detail article = articleService.getArticleById(id);
        return ResponseEntity.ok(
            ApiResponse.success("Berhasil mengambil detail artikel", article)
        );
    }
}
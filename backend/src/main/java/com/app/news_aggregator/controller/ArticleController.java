package com.app.news_aggregator.controller;

import com.app.news_aggregator.dto.ApiResponse;
import com.app.news_aggregator.dto.ArticleDto;
import com.app.news_aggregator.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Articles", description = "Endpoint untuk mengambil dan mencari artikel berita")
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
    @Operation(
        summary = "Ambil daftar artikel",
        description = """
            Mengambil daftar artikel dengan pagination. Mendukung tiga mode:
            - **Default**: semua artikel terbaru
            - **Filter kategori**: tambahkan `?category=teknologi`
            - **Filter sumber**: tambahkan `?sourceId=1`
            - **Pencarian**: tambahkan `?search=keyword` (prioritas tertinggi)

            Hanya satu mode yang aktif per request. Urutan prioritas: `search` > `category` > `sourceId` > default.
            """
    )
    public ResponseEntity<ApiResponse<Page<ArticleDto.Summary>>> getArticles(
            @Parameter(description = "Nomor halaman (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Jumlah artikel per halaman", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Filter berdasarkan kategori, misal: teknologi, bisnis, olahraga")
            @RequestParam(required = false) String category,

            @Parameter(description = "Kata kunci pencarian pada judul dan deskripsi artikel")
            @RequestParam(required = false) String search,

            @Parameter(description = "Filter berdasarkan ID sumber RSS")
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
    @Operation(
        summary = "Ambil detail artikel",
        description = """
            Mengambil detail lengkap satu artikel berdasarkan ID, termasuk konten hasil scraping (jika tersedia).

            **Catatan:** Setiap kali endpoint ini dipanggil, `viewCount` artikel akan otomatis bertambah 1.

            Jika `isScraped = false`, field `content` akan bernilai `null`.
            Dalam kondisi ini, tampilkan `description` sebagai fallback.
            """
    )
    public ResponseEntity<ApiResponse<ArticleDto.Detail>> getArticleById(
            @Parameter(description = "ID unik artikel", example = "1", required = true)
            @PathVariable Long id) {
        ArticleDto.Detail article = articleService.getArticleById(id);
        return ResponseEntity.ok(
            ApiResponse.success("Berhasil mengambil detail artikel", article)
        );
    }
}

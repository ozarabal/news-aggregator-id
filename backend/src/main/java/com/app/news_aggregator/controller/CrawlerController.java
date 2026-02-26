package com.app.news_aggregator.controller;

import com.app.news_aggregator.service.*;
import com.app.news_aggregator.dto.ApiResponse;
import com.app.news_aggregator.exception.ResourceNotFoundException;
import com.app.news_aggregator.model.CrawlLog;
import com.app.news_aggregator.model.Source;
import com.app.news_aggregator.repository.CrawlLogRepository;
import com.app.news_aggregator.repository.SourceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CrawlerController menyediakan endpoint untuk:
 * 1. Trigger crawl manual (untuk testing atau saat tambah sumber baru)
 * 2. Melihat history crawl log
 * 3. Melihat statistik crawl
 *
 * Endpoint:
 * POST /api/v1/crawler/crawl-all          - Crawl semua sumber aktif
 * POST /api/v1/crawler/crawl/{sourceId}   - Crawl satu sumber tertentu
 * GET  /api/v1/crawler/logs/{sourceId}    - Riwayat crawl satu sumber
 * GET  /api/v1/crawler/stats              - Statistik crawl hari ini
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/crawler")
@RequiredArgsConstructor
@Tag(name = "Crawler", description = "Trigger crawl RSS manual dan monitoring riwayat crawl")
public class CrawlerController {

    private final CrawlerService crawlerService;
    private final SourceRepository sourceRepository;
    private final CrawlLogRepository crawlLogRepository;

    /**
     * POST /api/v1/crawler/crawl-all
     * Trigger crawl semua sumber aktif secara asinkron.
     * Response langsung dikembalikan tanpa menunggu crawl selesai.
     */
    @PostMapping("/crawl-all")
    @Operation(
        summary = "Crawl semua sumber aktif (async)",
        description = """
            Menginisiasi crawl untuk semua sumber RSS yang berstatus aktif secara **asinkron**.

            Response dikembalikan **segera** tanpa menunggu proses crawl selesai.
            Proses crawl berjalan di background menggunakan thread pool.

            Gunakan `GET /api/v1/crawler/stats` untuk memantau hasilnya.
            """
    )
    public ResponseEntity<ApiResponse<String>> crawlAll() {
        log.info("Trigger manual: crawl semua sumber aktif");
        // Jalankan asinkron — tidak blocking
        crawlerService.crawlAllActiveSources();
        return ResponseEntity.ok(
            ApiResponse.success("Crawl semua sumber aktif berhasil diinisiasi. Proses berjalan di background.")
        );
    }

    /**
     * POST /api/v1/crawler/crawl/{sourceId}
     * Trigger crawl satu sumber secara SINKRON.
     * Response MENUNGGU hingga crawl selesai dan return hasilnya.
     * Berguna untuk testing atau jika admin ingin lihat hasilnya langsung.
     */
    @PostMapping("/crawl/{sourceId}")
    @Operation(
        summary = "Crawl satu sumber (sync)",
        description = """
            Menjalankan crawl untuk satu sumber RSS secara **sinkron**.

            Response **menunggu** hingga crawl selesai dan mengembalikan hasil lengkap:
            - Jumlah artikel yang ditemukan di feed
            - Jumlah artikel baru yang disimpan ke database
            - Durasi proses crawl (milidetik)
            - Pesan error jika gagal

            Berguna untuk testing atau saat admin menambahkan sumber baru dan ingin langsung melihat hasilnya.
            """
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> crawlOne(
            @Parameter(description = "ID sumber RSS yang akan di-crawl", example = "1", required = true)
            @PathVariable Long sourceId) {
        log.info("Trigger manual: crawl sumber ID {}", sourceId);

        Source source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Source", sourceId));

        // Sinkron: tunggu hingga crawl selesai
        CrawlLog result = crawlerService.crawlSource(source);

        // Buat response informatif
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("status", result.getStatus().name());
        resultData.put("sourceName", source.getName());
        resultData.put("articlesFound", result.getArticlesFound());
        resultData.put("articlesSaved", result.getArticlesSaved());
        resultData.put("durationMs", result.getDurationMs());
        if (result.getErrorMessage() != null) {
            resultData.put("errorMessage", result.getErrorMessage());
        }

        boolean success = result.getStatus() == CrawlLog.CrawlStatus.SUCCESS;
        String message = success
            ? "Crawl berhasil: " + result.getArticlesSaved() + " artikel baru disimpan"
            : "Crawl gagal: " + result.getErrorMessage();

        return ResponseEntity.ok(ApiResponse.success(message, resultData));
    }

    /**
     * GET /api/v1/crawler/logs/{sourceId}
     * Ambil riwayat crawl untuk sumber tertentu.
     */
    @GetMapping("/logs/{sourceId}")
    @Operation(
        summary = "Riwayat crawl satu sumber",
        description = "Mengambil riwayat crawl (crawl log) untuk sumber RSS tertentu, diurutkan dari yang terbaru"
    )
    public ResponseEntity<ApiResponse<List<CrawlLog>>> getCrawlLogs(
            @Parameter(description = "ID sumber RSS", example = "1", required = true)
            @PathVariable Long sourceId,

            @Parameter(description = "Nomor halaman (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Jumlah log per halaman", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        // Validasi source exists
        if (!sourceRepository.existsById(sourceId)) {
            throw new ResourceNotFoundException("Source", sourceId);
        }

        List<CrawlLog> logs = crawlLogRepository
                .findBySourceIdOrderByCrawledAtDesc(sourceId, PageRequest.of(page, size))
                .getContent();

        return ResponseEntity.ok(
            ApiResponse.success("Berhasil mengambil riwayat crawl", logs)
        );
    }

    /**
     * GET /api/v1/crawler/stats
     * Statistik crawl hari ini.
     */
    @GetMapping("/stats")
    @Operation(
        summary = "Statistik crawl hari ini",
        description = "Mengambil statistik crawl untuk hari ini: total artikel yang berhasil disimpan dan jumlah sumber aktif"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCrawlStats() {
        Map<String, Object> stats = new HashMap<>();

        Long articlesSavedToday = crawlLogRepository.countArticlesSavedToday();
        long totalActiveSources = sourceRepository.findByIsActiveTrue().size();

        stats.put("articlesSavedToday", articlesSavedToday != null ? articlesSavedToday : 0);
        stats.put("totalActiveSources", totalActiveSources);

        return ResponseEntity.ok(
            ApiResponse.success("Statistik crawl hari ini", stats)
        );
    }
}

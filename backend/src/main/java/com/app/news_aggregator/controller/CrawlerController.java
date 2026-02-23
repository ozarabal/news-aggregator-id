package com.app.news_aggregator.controller;

import com.app.news_aggregator.service.*;
import com.app.news_aggregator.dto.ApiResponse;
import com.app.news_aggregator.exception.ResourceNotFoundException;
import com.app.news_aggregator.model.CrawlLog;
import com.app.news_aggregator.model.Source;
import com.app.news_aggregator.repository.CrawlLogRepository;
import com.app.news_aggregator.repository.SourceRepository;
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> crawlOne(@PathVariable Long sourceId) {
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
    public ResponseEntity<ApiResponse<List<CrawlLog>>> getCrawlLogs(
            @PathVariable Long sourceId,
            @RequestParam(defaultValue = "0") int page,
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
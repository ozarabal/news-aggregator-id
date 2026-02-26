package com.app.news_aggregator.controller;

import com.app.news_aggregator.dto.ApiResponse;
import com.app.news_aggregator.dto.SourceDto;
import com.app.news_aggregator.service.SourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SourceController menangani semua HTTP request yang berkaitan dengan sumber RSS.
 *
 * @RestController = @Controller + @ResponseBody (semua method return JSON)
 * @RequestMapping("/api/v1/sources") = base URL untuk semua endpoint di controller ini
 *
 * Endpoint yang tersedia:
 * GET    /api/v1/sources              - Ambil semua sumber
 * GET    /api/v1/sources/{id}         - Ambil satu sumber berdasarkan ID
 * GET    /api/v1/sources/categories   - Ambil semua kategori
 * POST   /api/v1/sources              - Tambah sumber baru
 * PUT    /api/v1/sources/{id}         - Update sumber
 * PATCH  /api/v1/sources/{id}/toggle  - Toggle status aktif/nonaktif
 * DELETE /api/v1/sources/{id}         - Hapus sumber
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/sources")
@RequiredArgsConstructor
@Tag(name = "Sources", description = "Manajemen sumber RSS feed (CRUD)")
public class SourceController {

    private final SourceService sourceService;

    /** GET /api/v1/sources - Ambil semua sumber RSS */
    @GetMapping
    @Operation(summary = "Ambil semua sumber RSS", description = "Mengambil daftar lengkap semua sumber RSS yang terdaftar di sistem")
    public ResponseEntity<ApiResponse<List<SourceDto.Response>>> getAllSources() {
        List<SourceDto.Response> sources = sourceService.getAllSources();
        return ResponseEntity.ok(
            ApiResponse.success("Berhasil mengambil daftar sumber RSS", sources)
        );
    }

    /** GET /api/v1/sources/categories - Ambil semua kategori yang tersedia */
    @GetMapping("/categories")
    @Operation(summary = "Ambil semua kategori", description = "Mengambil daftar kategori unik dari seluruh sumber RSS yang terdaftar")
    public ResponseEntity<ApiResponse<List<String>>> getAllCategories() {
        List<String> categories = sourceService.getAllCategories();
        return ResponseEntity.ok(
            ApiResponse.success("Berhasil mengambil daftar kategori", categories)
        );
    }

    /** GET /api/v1/sources/{id} - Ambil satu sumber berdasarkan ID */
    @GetMapping("/{id}")
    @Operation(summary = "Ambil sumber RSS by ID", description = "Mengambil detail satu sumber RSS berdasarkan ID-nya")
    public ResponseEntity<ApiResponse<SourceDto.Response>> getSourceById(
            @Parameter(description = "ID unik sumber RSS", example = "1", required = true)
            @PathVariable Long id) {
        SourceDto.Response source = sourceService.getSourceById(id);
        return ResponseEntity.ok(
            ApiResponse.success("Berhasil mengambil sumber RSS", source)
        );
    }

    /** GET /api/v1/sources?category=teknologi - Ambil sumber berdasarkan kategori */
    @GetMapping(params = "category")
    @Operation(summary = "Filter sumber RSS by kategori", description = "Mengambil daftar sumber RSS yang termasuk dalam kategori tertentu")
    public ResponseEntity<ApiResponse<List<SourceDto.Response>>> getSourcesByCategory(
            @Parameter(description = "Nama kategori, misal: teknologi, bisnis, olahraga", example = "teknologi", required = true)
            @RequestParam String category) {
        List<SourceDto.Response> sources = sourceService.getSourcesByCategory(category);
        return ResponseEntity.ok(
            ApiResponse.success("Berhasil mengambil sumber RSS kategori " + category, sources)
        );
    }

    /**
     * POST /api/v1/sources - Tambah sumber RSS baru
     * @Valid memicu validasi dari annotation di SourceDto.Request (@NotBlank, dll)
     */
    @PostMapping
    @Operation(summary = "Tambah sumber RSS baru", description = "Menambahkan sumber RSS baru ke sistem. Field `name`, `url`, dan `category` wajib diisi")
    public ResponseEntity<ApiResponse<SourceDto.Response>> createSource(
            @Valid @RequestBody SourceDto.Request request) {
        SourceDto.Response created = sourceService.createSource(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Sumber RSS berhasil ditambahkan", created));
    }

    /** PUT /api/v1/sources/{id} - Update sumber RSS */
    @PutMapping("/{id}")
    @Operation(summary = "Update sumber RSS", description = "Memperbarui data sumber RSS yang sudah ada berdasarkan ID")
    public ResponseEntity<ApiResponse<SourceDto.Response>> updateSource(
            @Parameter(description = "ID unik sumber RSS", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody SourceDto.Request request) {
        SourceDto.Response updated = sourceService.updateSource(id, request);
        return ResponseEntity.ok(
            ApiResponse.success("Sumber RSS berhasil diupdate", updated)
        );
    }

    /** PATCH /api/v1/sources/{id}/toggle - Toggle status aktif/nonaktif */
    @PatchMapping("/{id}/toggle")
    @Operation(
        summary = "Toggle status aktif sumber",
        description = "Mengubah status aktif/nonaktif sumber RSS. Sumber yang nonaktif tidak akan di-crawl secara otomatis"
    )
    public ResponseEntity<ApiResponse<SourceDto.Response>> toggleStatus(
            @Parameter(description = "ID unik sumber RSS", example = "1", required = true)
            @PathVariable Long id) {
        SourceDto.Response updated = sourceService.toggleSourceStatus(id);
        return ResponseEntity.ok(
            ApiResponse.success("Status sumber RSS berhasil diubah", updated)
        );
    }

    /** DELETE /api/v1/sources/{id} - Hapus sumber RSS */
    @DeleteMapping("/{id}")
    @Operation(summary = "Hapus sumber RSS", description = "Menghapus sumber RSS dari sistem secara permanen")
    public ResponseEntity<ApiResponse<Void>> deleteSource(
            @Parameter(description = "ID unik sumber RSS", example = "1", required = true)
            @PathVariable Long id) {
        sourceService.deleteSource(id);
        return ResponseEntity.ok(
            ApiResponse.success("Sumber RSS berhasil dihapus")
        );
    }
}

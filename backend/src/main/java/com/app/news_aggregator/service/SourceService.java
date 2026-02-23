package com.app.news_aggregator.service;

import com.app.news_aggregator.dto.SourceDto;
import com.app.news_aggregator.exception.DuplicateResourceException;
import com.app.news_aggregator.exception.ResourceNotFoundException;
import com.app.news_aggregator.model.Source;
import com.app.news_aggregator.repository.SourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SourceService berisi semua business logic yang berkaitan dengan Source (sumber RSS).
 *
 * @Service memberitahu Spring bahwa ini adalah komponen service layer.
 * @RequiredArgsConstructor dari Lombok otomatis membuat constructor untuk field 'final'.
 * @Slf4j menyediakan objek 'log' untuk logging.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SourceService {

    private final SourceRepository sourceRepository;

    /**
     * Ambil semua sumber RSS.
     * @Transactional(readOnly = true) = transaksi read-only, lebih optimal untuk query.
     */
    @Transactional(readOnly = true)
    public List<SourceDto.Response> getAllSources() {
        log.debug("Mengambil semua sumber RSS");
        return sourceRepository.findAll()
                .stream()
                .map(SourceDto.Response::from)
                .collect(Collectors.toList());
    }

    /**
     * Ambil sumber RSS berdasarkan ID.
     * Lempar ResourceNotFoundException jika tidak ditemukan.
     */
    @Transactional(readOnly = true)
    public SourceDto.Response getSourceById(Long id) {
        log.debug("Mengambil sumber RSS dengan ID: {}", id);
        Source source = findSourceOrThrow(id);
        return SourceDto.Response.from(source);
    }

    /**
     * Ambil sumber RSS berdasarkan kategori.
     */
    @Transactional(readOnly = true)
    public List<SourceDto.Response> getSourcesByCategory(String category) {
        return sourceRepository.findByCategoryAndIsActiveTrue(category)
                .stream()
                .map(SourceDto.Response::from)
                .collect(Collectors.toList());
    }

    /**
     * Ambil semua kategori yang tersedia.
     */
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return sourceRepository.findAllActiveCategories();
    }

    /**
     * Tambah sumber RSS baru.
     * Validasi: URL tidak boleh duplikat.
     */
    @Transactional
    public SourceDto.Response createSource(SourceDto.Request request) {
        log.info("Menambahkan sumber RSS baru: {}", request.getName());

        // Validasi duplikat URL
        if (sourceRepository.existsByUrl(request.getUrl())) {
            throw new DuplicateResourceException(
                "Sumber RSS dengan URL '" + request.getUrl() + "' sudah terdaftar"
            );
        }

        // Konversi DTO ke entity menggunakan builder pattern
        Source source = Source.builder()
                .name(request.getName())
                .url(request.getUrl())
                .websiteUrl(request.getWebsiteUrl())
                .category(request.getCategory().toLowerCase())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        Source saved = sourceRepository.save(source);
        log.info("Sumber RSS berhasil ditambahkan dengan ID: {}", saved.getId());

        return SourceDto.Response.from(saved);
    }

    /**
     * Update sumber RSS yang sudah ada.
     */
    @Transactional
    public SourceDto.Response updateSource(Long id, SourceDto.Request request) {
        log.info("Mengupdate sumber RSS dengan ID: {}", id);

        Source source = findSourceOrThrow(id);

        // Jika URL berubah, cek apakah URL baru sudah dipakai sumber lain
        if (!source.getUrl().equals(request.getUrl()) &&
            sourceRepository.existsByUrl(request.getUrl())) {
            throw new DuplicateResourceException(
                "URL '" + request.getUrl() + "' sudah digunakan sumber lain"
            );
        }

        // Update field
        source.setName(request.getName());
        source.setUrl(request.getUrl());
        source.setWebsiteUrl(request.getWebsiteUrl());
        source.setCategory(request.getCategory().toLowerCase());
        if (request.getIsActive() != null) {
            source.setIsActive(request.getIsActive());
        }

        Source updated = sourceRepository.save(source);
        log.info("Sumber RSS berhasil diupdate: {}", updated.getId());

        return SourceDto.Response.from(updated);
    }

    /**
     * Aktifkan atau nonaktifkan sumber RSS.
     * Sumber yang nonaktif tidak akan di-crawl oleh Scheduler.
     */
    @Transactional
    public SourceDto.Response toggleSourceStatus(Long id) {
        Source source = findSourceOrThrow(id);
        source.setIsActive(!source.getIsActive());
        Source updated = sourceRepository.save(source);

        log.info("Status sumber RSS ID {} diubah menjadi: {}",
                id, updated.getIsActive() ? "AKTIF" : "NONAKTIF");

        return SourceDto.Response.from(updated);
    }

    /**
     * Hapus sumber RSS.
     * Artikel yang berasal dari sumber ini juga akan terhapus (CASCADE di DB).
     */
    @Transactional
    public void deleteSource(Long id) {
        log.warn("Menghapus sumber RSS dengan ID: {}", id);
        Source source = findSourceOrThrow(id);
        sourceRepository.delete(source);
        log.info("Sumber RSS berhasil dihapus: {}", id);
    }

    /**
     * Helper method: ambil Source atau lempar exception jika tidak ditemukan.
     * Private karena hanya dipakai di dalam service ini.
     */
    private Source findSourceOrThrow(Long id) {
        return sourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Source", id));
    }
}
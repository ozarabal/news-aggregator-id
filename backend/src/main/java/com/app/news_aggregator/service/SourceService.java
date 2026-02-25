package com.app.news_aggregator.service;

import com.app.news_aggregator.config.RedisConfig;
import com.app.news_aggregator.dto.SourceDto;
import com.app.news_aggregator.exception.DuplicateResourceException;
import com.app.news_aggregator.exception.ResourceNotFoundException;
import com.app.news_aggregator.model.Source;
import com.app.news_aggregator.repository.SourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
 * 
 * SourceService dengan Redis Caching
 * 
 * Cache "sources" menyimpan daftar sumber RSS yang sangat jarang berubah.
 * TLL  30 menit (dikonfigurasi di RedisConfig)
 * 
 * pola invalidasi :
 * - Create/Update/Delete source -> @CacheEvict("sources") + @CacheEvict("categories")
 * - sehingga request berikutnya akan fresh dari DB
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
    @Cacheable(value= RedisConfig.CACHE_SOURCES, key= "'all'")
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
    @Cacheable(value= RedisConfig.CACHE_SOURCES, key="'id_'+ #id")
    @Transactional(readOnly = true)
    public SourceDto.Response getSourceById(Long id) {
        log.debug("Mengambil sumber RSS dengan ID: {}", id);
        Source source = findSourceOrThrow(id);
        return SourceDto.Response.from(source);
    }

    /**
     * Ambil sumber RSS berdasarkan kategori.
     */
    @Cacheable(value= RedisConfig.CACHE_SOURCES, key="'cat_' + #category")
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
    @Cacheable(value= RedisConfig.CACHE_CATEGORIES, key = "'all'")
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return sourceRepository.findAllActiveCategories();
    }

    /**
     * Tambah sumber RSS baru.
     * Validasi: URL tidak boleh duplikat.
     * 
     * @CacheEvict : setelah tambah sumber baru, hapus cache "sources" dan "categories"
     * agar request selanjutnya bisa memperbarui data 
     * 
     * allEntries = true: hapus Semua entri dalam cache tersebut
     */
    @Caching(evict = {
        @CacheEvict(value = RedisConfig.CACHE_SOURCES, allEntries = true),
        @CacheEvict(value = RedisConfig.CACHE_CATEGORIES, allEntries = true)
    })
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
    @Caching(evict = {
        @CacheEvict(value = RedisConfig.CACHE_SOURCES, allEntries = true),
        @CacheEvict(value = RedisConfig.CACHE_CATEGORIES, allEntries = true)}
    )
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
    @CacheEvict(value = RedisConfig.CACHE_SOURCES, allEntries = true)
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
    @Caching(evict = {
        @CacheEvict(value = RedisConfig.CACHE_SOURCES, allEntries = true),
        @CacheEvict(value = RedisConfig.CACHE_CATEGORIES, allEntries = true)
    }
    )
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
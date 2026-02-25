package com.app.news_aggregator.service;

import com.app.news_aggregator.config.RedisConfig;
import com.app.news_aggregator.dto.ArticleDto;
import com.app.news_aggregator.exception.ResourceNotFoundException;
import com.app.news_aggregator.model.Article;
import com.app.news_aggregator.repository.ArticleRepository;
import com.app.news_aggregator.util.RestPage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ArticleService dengan Redis Caching.
 *
 * Strategi caching yang dipakai:
 *
 * @Cacheable   → Ambil dari cache jika ada, jalankan method jika tidak ada
 * @CacheEvict  → Hapus cache saat data berubah (artikel baru masuk)
 * @Caching     → Gabungkan beberapa cache annotation sekaligus
 *
 * Format cache key:
 * "articles::all_0_20"         → semua artikel, halaman 0, size 20
 * "articles::cat_teknologi_0"  → artikel kategori teknologi, halaman 0
 * "article_detail::1"          → detail artikel ID 1
 * "search::spring_boot_0"      → hasil search "spring boot", halaman 0
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;

    /**
     * Ambil semua artikel dengan pagination.
     *
     * @Cacheable:
     * - value = nama cache di Redis (key prefix)
     * - key   = cache key unik (SpEL expression)
     * - Hasil Page<ArticleDto.Summary> disimpan ke Redis dengan key "articles::all_0_20"
     *
     * Request berikutnya dengan page=0,size=20 → Redis HIT, tidak query DB.
     * Request dengan page=1,size=20 → Redis MISS, query DB, simpan ke Redis.
     */
    @Cacheable(value = RedisConfig.CACHE_ARTICLES, key = "'all_' + #page + '_' + #size")
    @Transactional(readOnly = true)
    public RestPage<ArticleDto.Summary> getAllArticles(int page, int size) {
        log.debug("[CACHE MISS] getAllArticles - query ke database (page={}, size={})", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        Page<ArticleDto.Summary> rpage = articleRepository.findAll(pageable).map(ArticleDto.Summary::from);
        return new RestPage<>(rpage.getContent(), rpage.getNumber(), rpage.getSize(), rpage.getTotalElements());
    }

    /**
     * Ambil artikel berdasarkan kategori.
     * Cache key berbeda per kategori dan halaman.
     * Contoh key: "articles::cat_teknologi_0_20"
     */
    @Cacheable(value = RedisConfig.CACHE_ARTICLES, key = "'cat_' + #category + '_' + #page + '_' + #size")
    @Transactional(readOnly = true)
    public Page<ArticleDto.Summary> getArticlesByCategory(String category, int page, int size) {
        log.debug("[CACHE MISS] getArticlesByCategory - query ke database (category={})", category);

        Pageable pageable = PageRequest.of(page, size);
        return articleRepository
                .findByCategoryOrderByPublishedAtDesc(category.toLowerCase(), pageable)
                .map(ArticleDto.Summary::from);
    }

    /**
     * Ambil artikel dari sumber tertentu.
     * Cache key: "articles::src_1_0_20"
     */
    @Cacheable(value = RedisConfig.CACHE_ARTICLES, key = "'src_' + #sourceId + '_' + #page + '_' + #size")
    @Transactional(readOnly = true)
    public Page<ArticleDto.Summary> getArticlesBySource(Long sourceId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return articleRepository
                .findBySourceIdOrderByPublishedAtDesc(sourceId, pageable)
                .map(ArticleDto.Summary::from);
    }

    /**
     * Ambil detail satu artikel berdasarkan ID.
     * Sekaligus increment view count.
     *
     * khusus method ini tidak dilakukan cache agar view count selalu akurat
     * namun detail content di cache secara terpisah via getArticleDetailCached()
     */
    @Transactional
    public ArticleDto.Detail getArticleById(Long id) {
        log.debug("Mengambil detail artikel ID: {}", id);

        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article", id));

        // Increment view count setiap kali artikel dibuka
        articleRepository.incrementViewCount(id);
        article.setViewCount(article.getViewCount() + 1); // Update objek lokal juga

        return ArticleDto.Detail.from(article);
    }

    /**
     * Cari artikel berdasarkan keyword.
     * Cache key: "search::java_0_20"
     * TTL lebih pendek (2 menit) karena hasil search lebih dinamis.
     */
    @Cacheable(value = RedisConfig.CACHE_SEARCH, key = "#keyword.toLowerCase() + '_' + #page + '_' + #size")
    @Transactional(readOnly = true)
    public Page<ArticleDto.Summary> searchArticles(String keyword, int page, int size) {
        log.debug("Mencari artikel dengan keyword: '{}'", keyword);

        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllArticles(page, size);
        }

        Pageable pageable = PageRequest.of(page, size);
        return articleRepository.searchByKeyword(keyword.trim(), pageable)
                .map(ArticleDto.Summary::from);
    }

    /**
     * Invalidasi cache artikel saat ada artikel baru masuk.
     * Dipanggil oleh CrawlerService setelah saveNewArticles() selesai.
     *
     * @Caching memungkinkan kita pakai multiple @CacheEvict sekaligus:
     * - Hapus semua entri di cache "articles" (list artikel)
     * - Hapus semua entri di cache "search"
     *
     * Kenapa tidak hapus "article_detail"?
     * Karena detail artikel yang sudah ada tidak berubah, hanya ada artikel BARU.
     */
    @Caching(evict = {
        @CacheEvict(value = RedisConfig.CACHE_ARTICLES, allEntries = true),
        @CacheEvict(value = RedisConfig.CACHE_SEARCH, allEntries = true)
    })
    public void invalidateArticleCache() {
        log.info("[CACHE EVICT] Cache artikel dan search dihapus karena ada data baru");
    }

    /**
     * Invalidasi semua cache — dipakai saat crawl selesai atau data besar berubah.
     */
    @Caching(evict = {
        @CacheEvict(value = RedisConfig.CACHE_ARTICLES,       allEntries = true),
        @CacheEvict(value = RedisConfig.CACHE_ARTICLE_DETAIL, allEntries = true),
        @CacheEvict(value = RedisConfig.CACHE_SEARCH,         allEntries = true)
    })
    public void invalidateAllCache() {
        log.info("[CACHE EVICT] Semua cache artikel dihapus");
    }
}
package com.app.news_aggregator.service;

import com.app.news_aggregator.dto.ArticleDto;
import com.app.news_aggregator.exception.ResourceNotFoundException;
import com.app.news_aggregator.model.Article;
import com.app.news_aggregator.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ArticleService berisi business logic untuk operasi artikel.
 * Di Phase 4, method-method di sini akan ditambahkan @Cacheable untuk caching.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;

    /**
     * Ambil semua artikel dengan pagination.
     * defaultPage = 0 (Spring Data JPA berbasis 0-index).
     *
     * Di Phase 4, akan ditambahkan:
     * @Cacheable(value = "articles", key = "'all_' + #page + '_' + #size")
     */
    @Transactional(readOnly = true)
    public Page<ArticleDto.Summary> getAllArticles(int page, int size) {
        log.debug("Mengambil semua artikel, halaman: {}, ukuran: {}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        return articleRepository.findAll(pageable)
                .map(ArticleDto.Summary::from);
    }

    /**
     * Ambil artikel berdasarkan kategori dengan pagination.
     *
     * Di Phase 4, akan ditambahkan:
     * @Cacheable(value = "articles", key = "#category + '_' + #page")
     */
    @Transactional(readOnly = true)
    public Page<ArticleDto.Summary> getArticlesByCategory(String category, int page, int size) {
        log.debug("Mengambil artikel kategori '{}', halaman: {}", category, page);

        Pageable pageable = PageRequest.of(page, size);
        return articleRepository
                .findByCategoryOrderByPublishedAtDesc(category.toLowerCase(), pageable)
                .map(ArticleDto.Summary::from);
    }

    /**
     * Ambil artikel dari sumber tertentu.
     */
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
     * Di Phase 4, akan ditambahkan:
     * @Cacheable(value = "article_detail", key = "#id")
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
     *
     * Di Phase 4, akan ditambahkan:
     * @Cacheable(value = "search", key = "#keyword + '_' + #page")
     */
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
}
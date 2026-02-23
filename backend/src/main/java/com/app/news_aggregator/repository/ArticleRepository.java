package com.app.news_aggregator.repository;

import com.app.news_aggregator.model.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
// import java.util.Optional;

/**
 * Repository untuk entity Article.
 * Menggunakan Page<Article> untuk mendukung pagination di API.
 */
@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    /**
     * Cek apakah artikel dengan URL tersebut sudah ada.
     * Dipakai saat crawl untuk mencegah duplikasi artikel.
     */
    boolean existsByUrl(String url);

    /**
     * Cek apakah artikel dengan GUID tersebut sudah ada.
     * GUID adalah identifier unik dari RSS feed.
     */
    boolean existsByGuid(String guid);

    /**
     * Ambil semua artikel berdasarkan kategori, diurutkan dari terbaru.
     * Menggunakan Pageable untuk pagination (halaman, ukuran halaman, sorting).
     */
    Page<Article> findByCategoryOrderByPublishedAtDesc(String category, Pageable pageable);

    /**
     * Ambil semua artikel dari sumber tertentu, diurutkan dari terbaru.
     */
    Page<Article> findBySourceIdOrderByPublishedAtDesc(Long sourceId, Pageable pageable);

    /**
     * Cari artikel berdasarkan keyword di judul atau deskripsi.
     * LOWER() untuk case-insensitive search.
     * LIKE '%keyword%' untuk partial match.
     */
    @Query("""
           SELECT a FROM Article a
           WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
           ORDER BY a.publishedAt DESC
           """)
    Page<Article> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Ambil artikel terpopuler berdasarkan view count.
     * Dipakai untuk generate email digest.
     */
    @Query("""
           SELECT a FROM Article a
           WHERE a.category IN :categories
             AND a.publishedAt >= :since
           ORDER BY a.viewCount DESC, a.publishedAt DESC
           """)
    Page<Article> findPopularByCategories(
        @Param("categories") java.util.List<String> categories,
        @Param("since") LocalDateTime since,
        Pageable pageable
    );

    /**
     * Increment view count artikel (saat user membuka artikel).
     * @Modifying + @Query dipakai karena ini adalah UPDATE statement, bukan SELECT.
     * nativeQuery = false karena kita pakai JPQL (bukan SQL biasa).
     */
    @Modifying
    @Query("UPDATE Article a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
    void incrementViewCount(@Param("id") Long id);

    /**
     * Ambil semua artikel yang belum di-scrape konten lengkapnya.
     * Dipakai oleh background job scraper (Phase 2).
     */
    @Query("SELECT a FROM Article a WHERE a.isScraped = false ORDER BY a.createdAt DESC")
    java.util.List<Article> findUnscrapedArticles(Pageable pageable);
}
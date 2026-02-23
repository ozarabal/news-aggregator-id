package com.app.news_aggregator.repository;

import com.app.news_aggregator.model.CrawlLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
// import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CrawlLogRepository extends JpaRepository<CrawlLog, Long> {

    /**
     * Ambil log crawl terbaru untuk sumber tertentu.
     */
    Optional<CrawlLog> findFirstBySourceIdOrderByCrawledAtDesc(Long sourceId);

    /**
     * Ambil semua log crawl untuk sumber tertentu, diurutkan dari terbaru.
     */
    Page<CrawlLog> findBySourceIdOrderByCrawledAtDesc(Long sourceId, Pageable pageable);

    /**
     * Statistik crawl: total artikel yang berhasil disimpan hari ini.
     */
    @Query("""
           SELECT SUM(c.articlesSaved) FROM CrawlLog c
           WHERE DATE(c.crawledAt) = CURRENT_DATE
             AND c.status = 'SUCCESS'
           """)
    Long countArticlesSavedToday();
}
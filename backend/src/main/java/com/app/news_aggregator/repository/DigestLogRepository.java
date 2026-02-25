package com.app.news_aggregator.repository;

import com.app.news_aggregator.model.DigestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public interface DigestLogRepository extends JpaRepository<DigestLog, Long> {

    long countByStatus(DigestLog.DigestStatus status);

    /**
     * Statistik pengiriman digest hari ini.
     */
    @Query("""
           SELECT COUNT(d) FROM DigestLog d
           WHERE cast(d.sentAt as LocalDate) = local date
             AND d.status = 'SENT'
           """)
    long countSentToday();

    @Query("""
           SELECT COUNT(d) FROM DigestLog d
           WHERE cast(d.sentAt as LocalDate) = local date
             AND d.status = 'FAILED'
           """)
    long countFailedToday();
}
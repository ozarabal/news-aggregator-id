package com.app.news_aggregator.repository;

import com.app.news_aggregator.model.Source;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
// import java.util.Optional;

/**
 * Repository untuk entity Source.
 *
 * JpaRepository<Source, Long> sudah menyediakan method CRUD dasar:
 * - findAll(), findById(), save(), delete(), count(), dll
 *
 * Kita hanya perlu tambahkan method custom sesuai kebutuhan.
 * Spring Data JPA akan otomatis generate query-nya berdasarkan nama method.
 */
@Repository
public interface SourceRepository extends JpaRepository<Source, Long> {

    /**
     * Ambil semua sumber yang aktif (untuk di-crawl oleh Scheduler).
     * Spring translate ini ke: SELECT * FROM sources WHERE is_active = true
     */
    List<Source> findByIsActiveTrue();

    /**
     * Cek apakah URL sudah terdaftar (mencegah duplikat sumber).
     */
    boolean existsByUrl(String url);

    /**
     * Ambil sumber berdasarkan kategori.
     */
    List<Source> findByCategoryAndIsActiveTrue(String category);

    /**
     * Ambil semua kategori yang tersedia (distinct).
     * Menggunakan @Query karena tidak bisa pakai naming convention untuk DISTINCT.
     */
    @Query("SELECT DISTINCT s.category FROM Source s WHERE s.isActive = true")
    List<String> findAllActiveCategories();

    /**
     * Cari sumber berdasarkan nama (case-insensitive, partial match).
     */
    List<Source> findByNameContainingIgnoreCase(String name);
}
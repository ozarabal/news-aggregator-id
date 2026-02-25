package com.app.news_aggregator.repository;

import com.app.news_aggregator.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByDigestUnsubscribeToken(String token);

    /**
     * Ambil semua user yang perlu menerima digest hari ini.
     * Kondisi:
     * - digest aktif
     * - email sudah diverifikasi
     * - akun aktif
     * - frekuensi DAILY
     */
    @Query("""
           SELECT u FROM User u
           WHERE u.digestEnabled = true
             AND u.emailVerified = true
             AND u.isActive = true
             AND u.digestFrequency = 'DAILY'
           """)
    List<User> findUsersForDailyDigest();

    /**
     * Ambil user untuk weekly digest (dikirim setiap Senin).
     */
    @Query("""
           SELECT u FROM User u
           WHERE u.digestEnabled = true
             AND u.emailVerified = true
             AND u.isActive = true
             AND u.digestFrequency = 'WEEKLY'
           """)
    List<User> findUsersForWeeklyDigest();
}
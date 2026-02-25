package com.app.news_aggregator.controller;

import com.app.news_aggregator.dto.ApiResponse;
import com.app.news_aggregator.model.User;
import com.app.news_aggregator.queue.DigestProducer;
import com.app.news_aggregator.repository.DigestLogRepository;
import com.app.news_aggregator.repository.UserRepository;
import com.app.news_aggregator.service.DigestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DigestController — endpoint untuk manajemen email digest.
 *
 * Endpoint:
 * POST /api/v1/digest/trigger-all         - Trigger digest semua user (manual)
 * POST /api/v1/digest/trigger/{userId}    - Trigger digest satu user (testing)
 * GET  /api/v1/digest/unsubscribe         - Unsubscribe via link email
 * GET  /api/v1/digest/stats               - Statistik pengiriman hari ini
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/digest")
@RequiredArgsConstructor
public class DigestController {

    private final DigestService digestService;
    private final DigestProducer digestProducer;
    private final UserRepository userRepository;
    private final DigestLogRepository digestLogRepository;

    /**
     * POST /api/v1/digest/trigger-all
     * Trigger kirim digest ke semua user yang subscribe.
     * Berguna untuk testing atau jika scheduler gagal jalan.
     */
    @PostMapping("/trigger-all")
    public ResponseEntity<ApiResponse<String>> triggerAll() {
        List<User> users = userRepository.findUsersForDailyDigest();

        if (users.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("Tidak ada user yang perlu menerima digest"));
        }

        digestProducer.enqueueDigestBatch(users);
        return ResponseEntity.ok(
            ApiResponse.success(users.size() + " task digest berhasil di-enqueue ke queue")
        );
    }

    /**
     * POST /api/v1/digest/trigger/{userId}
     * Trigger kirim digest ke satu user secara SINKRON.
     * Berguna untuk testing: langsung lihat hasilnya tanpa tunggu Scheduler.
     */
    @PostMapping("/trigger/{userId}")
    public ResponseEntity<ApiResponse<String>> triggerOne(@PathVariable Long userId) {
        log.info("Manual trigger digest untuk user ID: {}", userId);

        // Sinkron: tunggu hingga email terkirim, return hasilnya
        digestService.sendDigestToUser(userId);

        return ResponseEntity.ok(ApiResponse.success("Digest berhasil dikirim ke user ID: " + userId));
    }

    /**
     * GET /api/v1/digest/unsubscribe?token=xxx
     * Endpoint unsubscribe yang diklik user dari link di email.
     *
     * Setiap user punya token unik (UUID) yang disertakan di email.
     * Saat diklik, kita cari user berdasarkan token dan nonaktifkan digest.
     *
     * Keamanan: token unik per user, tidak bisa ditebak.
     */
    @GetMapping("/unsubscribe")
    public ResponseEntity<ApiResponse<String>> unsubscribe(@RequestParam String token) {
        User user = userRepository.findByDigestUnsubscribeToken(token).orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Token unsubscribe tidak valid"));
        }

        user.setDigestEnabled(false);
        userRepository.save(user);

        log.info("User {} berhasil unsubscribe dari digest", user.getEmail());
        return ResponseEntity.ok(
            ApiResponse.success("Anda berhasil unsubscribe dari News Digest. " +
                               "Email: " + user.getEmail())
        );
    }

    /**
     * GET /api/v1/digest/stats
     * Statistik pengiriman digest hari ini.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("sentToday",   digestLogRepository.countSentToday());
        stats.put("failedToday", digestLogRepository.countFailedToday());
        stats.put("totalUsers",  userRepository.findUsersForDailyDigest().size());

        return ResponseEntity.ok(ApiResponse.success("Statistik digest hari ini", stats));
    }
}
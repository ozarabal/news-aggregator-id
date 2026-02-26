package com.app.news_aggregator.controller;

import com.app.news_aggregator.dto.ApiResponse;
import com.app.news_aggregator.model.User;
import com.app.news_aggregator.queue.DigestProducer;
import com.app.news_aggregator.repository.DigestLogRepository;
import com.app.news_aggregator.repository.UserRepository;
import com.app.news_aggregator.service.DigestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Digest", description = "Manajemen email digest harian — trigger pengiriman dan monitoring statistik")
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
    @Operation(
        summary = "Trigger digest semua user",
        description = """
            Menginisiasi pengiriman email digest ke semua user yang aktif berlangganan.

            Proses berjalan secara **asinkron** melalui RabbitMQ — setiap user di-enqueue sebagai
            task terpisah sehingga kegagalan satu user tidak mempengaruhi user lain.

            Berguna untuk trigger manual jika scheduler gagal jalan atau untuk testing.
            """
    )
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
    @Operation(
        summary = "Trigger digest satu user (sync)",
        description = """
            Mengirim email digest ke satu user secara **sinkron** — response menunggu hingga email terkirim.

            Berguna untuk testing: bisa langsung melihat hasil pengiriman tanpa menunggu scheduler.
            """
    )
    public ResponseEntity<ApiResponse<String>> triggerOne(
            @Parameter(description = "ID user yang akan menerima digest", example = "1", required = true)
            @PathVariable Long userId) {
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
    @Operation(
        summary = "Unsubscribe dari email digest",
        description = """
            Menonaktifkan langganan email digest untuk user berdasarkan token unik.

            Endpoint ini diklik oleh user melalui link unsubscribe di email digest mereka.
            Setiap user memiliki token UUID unik yang tidak dapat ditebak.

            Mengembalikan `400 Bad Request` jika token tidak valid atau sudah kadaluarsa.
            """
    )
    public ResponseEntity<ApiResponse<String>> unsubscribe(
            @Parameter(description = "Token unsubscribe unik dari email digest user", required = true)
            @RequestParam String token) {
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
    @Operation(
        summary = "Statistik digest hari ini",
        description = "Mengambil statistik pengiriman email digest untuk hari ini: berhasil terkirim, gagal, dan total user berlangganan"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("sentToday",   digestLogRepository.countSentToday());
        stats.put("failedToday", digestLogRepository.countFailedToday());
        stats.put("totalUsers",  userRepository.findUsersForDailyDigest().size());

        return ResponseEntity.ok(ApiResponse.success("Statistik digest hari ini", stats));
    }
}

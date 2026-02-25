package com.app.news_aggregator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DigestMessage adalah pesan yang dikirim ke queue email.digest.queue.
 * Setiap pesan mewakili satu email yang harus dikirim ke satu user.
 *
 * Scheduler enqueue satu pesan per user,
 * DigestConsumer (Worker) yang kemudian kirim email satu per satu.
 *
 * Dengan queue, jika ada 10.000 user:
 * - Scheduler selesai dalam < 1 detik (hanya enqueue)
 * - Worker kirim email bertahap, tidak overload SMTP sekaligus
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigestMessage {
    private Long userId;
    private String userEmail;
    private String userName;

    @Builder.Default
    private LocalDateTime enqueuedAt = LocalDateTime.now();
}
package com.app.news_aggregator.queue;

import com.app.news_aggregator.config.RabbitMQConfig;
import com.app.news_aggregator.model.User;
import com.app.news_aggregator.dto.DigestMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DigestProducer mengirim task pengiriman email ke queue.
 * Setiap user yang perlu menerima digest di-enqueue sebagai satu pesan terpisah.
 *
 * Keuntungan pola ini vs kirim langsung:
 * - Scheduler selesai sangat cepat (hanya enqueue, tidak kirim email)
 * - Pengiriman berjalan bertahap via Worker
 * - Jika SMTP down, pesan tersimpan di queue dan di-retry otomatis
 * - Bisa scale: tambah lebih banyak Worker untuk kirim lebih cepat
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DigestProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Enqueue task kirim digest untuk satu user.
     */
    public void enqueueDigest(User user) {
        DigestMessage message = DigestMessage.builder()
                .userId(user.getId())
                .userEmail(user.getEmail())
                .userName(user.getFullName())
                .build();

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY_DIGEST,
                    message
            );
            log.debug("[DIGEST] Task enqueued untuk user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("[DIGEST] Gagal enqueue task untuk {}: {}", user.getEmail(), e.getMessage());
        }
    }

    /**
     * Enqueue task digest untuk banyak user sekaligus.
     */
    public void enqueueDigestBatch(List<User> users) {
        log.info("[DIGEST] Mengirim {} task email ke queue...", users.size());
        users.forEach(this::enqueueDigest);
        log.info("[DIGEST] Selesai enqueue {} task email", users.size());
    }
}
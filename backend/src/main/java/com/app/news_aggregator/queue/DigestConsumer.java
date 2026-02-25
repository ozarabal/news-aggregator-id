package com.app.news_aggregator.queue;

import com.app.news_aggregator.config.RabbitMQConfig;
import com.app.news_aggregator.service.DigestService;
import com.app.news_aggregator.dto.DigestMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * DigestConsumer adalah Worker yang mengkonsumsi pesan dari email.digest.queue.
 *
 * Setiap pesan berisi userId yang perlu dikirim digestnya.
 * Worker memanggil DigestService yang akan:
 * 1. Fetch artikel sesuai preferensi user
 * 2. Render template email
 * 3. Kirim email via SMTP
 * 4. Catat hasil ke DigestLog
 *
 * Dengan prefetchCount=1, setiap Worker hanya ambil 1 pesan sekaligus.
 * Ini memastikan pengiriman email tidak terlalu agresif ke SMTP server.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DigestConsumer {

    private final DigestService digestService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_EMAIL_DIGEST)
    public void consumeDigest(DigestMessage message) {
        log.info("[WORKER] Memproses digest untuk user: {} (ID: {})",
                message.getUserEmail(), message.getUserId());

        try {
            digestService.sendDigestToUser(message.getUserId());
        } catch (Exception e) {
            log.error("[WORKER] Gagal kirim digest ke {}: {}",
                    message.getUserEmail(), e.getMessage());
            // Throw agar Spring NACK dan pesan di-retry / ke DLQ
            throw new RuntimeException("Gagal kirim digest ke " + message.getUserEmail(), e);
        }
    }
}
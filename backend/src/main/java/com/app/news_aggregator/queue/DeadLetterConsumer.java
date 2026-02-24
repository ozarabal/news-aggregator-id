package com.app.news_aggregator.queue;

import com.app.news_aggregator.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * DeadLetterConsumer memonitor Dead Letter Queue (DLQ).
 *
 * Pesan masuk ke DLQ ketika:
 * 1. Consumer throw exception berkali-kali (melebihi retry limit)
 * 2. Pesan expired (TTL habis sebelum diproses)
 * 3. Queue penuh (x-max-length tercapai)
 *
 * Consumer ini TIDAK mencoba re-proses pesan — hanya logging.
 * Tujuannya untuk visibilitas: admin bisa tahu ada sesuatu yang bermasalah
 * dan bisa investigate via log atau RabbitMQ Management UI.
 *
 * Untuk production, bisa ditambahkan:
 * - Kirim alert ke Slack/email saat ada pesan masuk DLQ
 * - Simpan pesan ke database untuk audit trail
 * - Dashboard monitoring
 */
@Slf4j
@Component
public class DeadLetterConsumer {

    @RabbitListener(queues = RabbitMQConfig.QUEUE_DEAD_LETTER)
    public void consumeDeadLetter(Message message) {
        // Ekstrak informasi dari pesan yang gagal
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        String originalQueue = (String) message.getMessageProperties()
                .getHeaders().get("x-original-queue");
        String deathReason = extractDeathReason(message);

        log.error("===== [DEAD LETTER] Pesan gagal diproses =====");
        log.error("Original Queue : {}", originalQueue);
        log.error("Alasan Gagal   : {}", deathReason);
        log.error("Isi Pesan      : {}", body);
        log.error("==============================================");

        // TODO untuk production: kirim alert ke monitoring system
        // slackNotifier.sendAlert("Pesan masuk DLQ: " + body);
    }

    private String extractDeathReason(Message message) {
        try {
            var deaths = message.getMessageProperties().getXDeathHeader();
            if (deaths != null && !deaths.isEmpty()) {
                return deaths.get(0).get("reason").toString();
            }
        } catch (Exception ignored) {}
        return "unknown";
    }
}
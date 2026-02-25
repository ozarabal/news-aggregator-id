package com.app.news_aggregator.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DigestLog mencatat setiap kali email digest dikirim.
 * Berguna untuk:
 * - Audit trail: siapa yang sudah terima email?
 * - Monitoring: berapa % pengiriman sukses?
 * - Debugging: error apa yang terjadi saat gagal kirim?
 */
@Entity
@Table(name = "digest_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DigestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DigestStatus status;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "articles_count")
    @Builder.Default
    private Integer articlesCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sent_at", nullable = false)
    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now();

    public enum DigestStatus {
        SENT,    // Email berhasil dikirim
        FAILED,  // Email gagal dikirim (error SMTP, dll)
        SKIPPED  // Dilewati (tidak ada artikel sesuai preferensi)
    }
}
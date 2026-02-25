package com.app.news_aggregator.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "digest_enabled", nullable = false)
    @Builder.Default
    private Boolean digestEnabled = false;  // Apakah user mau terima email digest?

    @Column(name = "digest_frequency")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DigestFrequency digestFrequency = DigestFrequency.DAILY;

    @Column(name= "last_digest_sent_at")
    private LocalDateTime lastDigestSentAt;

    @Column(name= "digest_unsubscribe_token", unique=true)
    private String digestUnsubscribeToken;


    /**
     * Relasi One-to-Many ke UserCategoryPreference.
     * ElementCollection dipakai karena preferensi kategori hanya berupa String sederhana,
     * tidak perlu entity tersendiri.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_category_preferences",
                     joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "category")
    @Builder.Default
    private List<String> categoryPreferences = new ArrayList<>();

    public enum DigestFrequency {
        DAILY, WEEKLY
    }

    /**
     * generate token unsubscribe unik saat pertama kali register.
    */
    public void generateUnsubscribeToken(){
        this.digestUnsubscribeToken = UUID.randomUUID().toString();
    }

    /**
     * cek apakah perlu menerima digest sekarang berdasarkan frekuensi.
     */
    public boolean isDueForDigest(){
        if(!Boolean.TRUE.equals(digestEnabled) || !Boolean.TRUE.equals(emailVerified)){
            return false;
        }
        if (lastDigestSentAt == null) return true;

        return switch(digestFrequency){
            case DAILY -> lastDigestSentAt.isBefore(LocalDateTime.now().minusHours(23));
            case WEEKLY -> lastDigestSentAt.isBefore(LocalDateTime.now().minusDays(6));
        };
    }
}
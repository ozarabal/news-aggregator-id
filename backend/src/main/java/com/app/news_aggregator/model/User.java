package com.app.news_aggregator.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * Relasi One-to-Many ke UserCategoryPreference.
     * ElementCollection dipakai karena preferensi kategori hanya berupa String sederhana,
     * tidak perlu entity tersendiri.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_category_preferences",
                     joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "category")
    @Builder.Default
    private List<String> categoryPreferences = new ArrayList<>();

    public enum DigestFrequency {
        DAILY, WEEKLY
    }
}
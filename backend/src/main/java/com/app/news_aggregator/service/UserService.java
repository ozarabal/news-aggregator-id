package com.app.news_aggregator.service;

import com.app.news_aggregator.config.JwtUtil;
import com.app.news_aggregator.dto.AuthDto;
import com.app.news_aggregator.dto.UserDto;
import com.app.news_aggregator.exception.DuplicateResourceException;
import com.app.news_aggregator.exception.ResourceNotFoundException;
import com.app.news_aggregator.model.User;
import com.app.news_aggregator.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Daftarkan user baru dan return JWT token.
     */
    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new DuplicateResourceException("Email sudah terdaftar: " + req.email());
        }

        User.DigestFrequency freq = User.DigestFrequency.DAILY;
        if (req.digestFrequency() != null) {
            try {
                freq = User.DigestFrequency.valueOf(req.digestFrequency().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid value — fallback to DAILY
            }
        }

        List<String> categories = req.categories() != null ? req.categories() : new ArrayList<>();

        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .isActive(true)
                .emailVerified(false)
                .digestEnabled(Boolean.TRUE.equals(req.digestEnabled()))
                .digestFrequency(freq)
                .categoryPreferences(new ArrayList<>(categories))
                .build();

        user.generateUnsubscribeToken();
        user = userRepository.save(user);

        log.info("User baru terdaftar: {} (ID: {})", user.getEmail(), user.getId());
        return buildAuthResponse(user);
    }

    /**
     * Login dengan email + password dan return JWT token.
     */
    public AuthDto.AuthResponse login(AuthDto.LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new BadCredentialsException("Email atau password salah"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Email atau password salah");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadCredentialsException("Akun tidak aktif");
        }

        log.info("User login: {} (ID: {})", user.getEmail(), user.getId());
        return buildAuthResponse(user);
    }

    /**
     * Ambil User entity berdasarkan ID.
     */
    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User dengan ID " + userId + " tidak ditemukan"));
    }

    /**
     * Update preferensi kategori dan digest user.
     */
    @Transactional
    public User updatePreferences(Long userId, UserDto.PreferenceRequest req) {
        User user = getById(userId);

        user.getCategoryPreferences().clear();
        if (req.categories() != null) {
            user.getCategoryPreferences().addAll(req.categories());
        }

        user.setDigestEnabled(req.digestEnabled());

        if (req.digestFrequency() != null) {
            try {
                user.setDigestFrequency(User.DigestFrequency.valueOf(req.digestFrequency().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Keep existing frequency if invalid value
            }
        }

        return userRepository.save(user);
    }

    private AuthDto.AuthResponse buildAuthResponse(User user) {
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        return new AuthDto.AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                Boolean.TRUE.equals(user.getDigestEnabled()),
                user.getDigestFrequency() != null ? user.getDigestFrequency().name() : "DAILY",
                new ArrayList<>(user.getCategoryPreferences()),
                user.getRole() != null ? user.getRole().name() : "USER"
        );
    }
}

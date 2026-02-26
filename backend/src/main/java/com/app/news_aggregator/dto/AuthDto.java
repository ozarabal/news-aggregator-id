package com.app.news_aggregator.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTOs untuk endpoint autentikasi.
 */
public class AuthDto {

    /** Request body untuk POST /api/v1/auth/register */
    public record RegisterRequest(
            @NotBlank(message = "Email wajib diisi")
            @Email(message = "Format email tidak valid")
            String email,

            @NotBlank(message = "Password wajib diisi")
            @Size(min = 8, message = "Password minimal 8 karakter")
            String password,

            @NotBlank(message = "Nama lengkap wajib diisi")
            String fullName,

            /** Kategori berita yang diminati, contoh: ["teknologi", "bisnis"] */
            List<String> categories,

            /** Apakah user mau terima email digest? Default false */
            Boolean digestEnabled,

            /** DAILY atau WEEKLY. Default DAILY */
            String digestFrequency
    ) {}

    /** Request body untuk POST /api/v1/auth/login */
    public record LoginRequest(
            @NotBlank(message = "Email wajib diisi")
            @Email(message = "Format email tidak valid")
            String email,

            @NotBlank(message = "Password wajib diisi")
            String password
    ) {}

    /** Response untuk register & login — berisi JWT token dan info user */
    public record AuthResponse(
            String token,
            Long userId,
            String email,
            String fullName,
            boolean digestEnabled,
            String digestFrequency,
            List<String> categories,
            String role
    ) {}
}

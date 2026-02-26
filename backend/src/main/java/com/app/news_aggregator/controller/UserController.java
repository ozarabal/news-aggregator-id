package com.app.news_aggregator.controller;

import com.app.news_aggregator.dto.ApiResponse;
import com.app.news_aggregator.dto.UserDto;
import com.app.news_aggregator.model.User;
import com.app.news_aggregator.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "Profil & preferensi pengguna (butuh JWT)")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Profil user saat ini",
               description = "Ambil data profil dan preferensi user yang sedang login",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto.UserProfile>> getMe(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                ApiResponse.success("Profil berhasil diambil", UserDto.UserProfile.from(currentUser)));
    }

    @Operation(summary = "Update preferensi user",
               description = "Ubah kategori yang diminati dan pengaturan email digest",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/me/preferences")
    public ResponseEntity<ApiResponse<UserDto.UserProfile>> updatePreferences(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UserDto.PreferenceRequest req) {
        User updated = userService.updatePreferences(currentUser.getId(), req);
        return ResponseEntity.ok(
                ApiResponse.success("Preferensi berhasil diperbarui", UserDto.UserProfile.from(updated)));
    }
}

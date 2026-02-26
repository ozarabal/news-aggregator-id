package com.app.news_aggregator.dto;

import com.app.news_aggregator.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * DTOs untuk endpoint user profile dan preferensi.
 */
public class UserDto {

    /** Request body untuk PUT /api/v1/users/me/preferences */
    public record PreferenceRequest(
            List<String> categories,
            boolean digestEnabled,
            String digestFrequency
    ) {}

    /** Response untuk GET /api/v1/users/me */
    public record UserProfile(
            Long id,
            String email,
            String fullName,
            boolean digestEnabled,
            String digestFrequency,
            List<String> categories,
            String role
    ) {
        public static UserProfile from(User user) {
            return new UserProfile(
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
}

package com.app.news_aggregator.config;

import com.app.news_aggregator.filter.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration.
 *
 * - Public endpoints: semua GET artikel/sumber, auth, admin ops (admin page tetap berfungsi tanpa login)
 * - Protected: /api/v1/bookmarks/** dan /api/v1/users/** (butuh JWT)
 * - Stateless session (JWT-based, tidak pakai HttpSession)
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Auth endpoints — public
                .requestMatchers("/api/v1/auth/**").permitAll()

                // Articles — public read
                .requestMatchers(HttpMethod.GET, "/api/v1/articles/**").permitAll()

                // Sources — public read, admin write (admin page works without login)
                .requestMatchers("/api/v1/sources/**").permitAll()

                // Crawler & cache — admin ops, keep public
                .requestMatchers("/api/v1/crawler/**").permitAll()
                .requestMatchers("/api/v1/cache/**").permitAll()

                // Digest — public (trigger for testing, unsubscribe link from email)
                .requestMatchers("/api/v1/digest/**").permitAll()

                // Swagger/OpenAPI
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                // User profile & bookmarks — require JWT
                .requestMatchers("/api/v1/users/**").authenticated()
                .requestMatchers("/api/v1/bookmarks/**").authenticated()

                // Anything else — require auth
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS configuration — mirrors WebConfig.java allowed origins.
     * Must be declared here so Spring Security applies it before the CORS preflight check.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:5174",
                "http://localhost:4173"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}

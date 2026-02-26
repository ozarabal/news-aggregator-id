package com.app.news_aggregator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("News Aggregator API")
                        .version("1.0.0")
                        .description("""
                                REST API untuk aplikasi **News Aggregator** — mengumpulkan berita dari berbagai sumber RSS secara otomatis.

                                ## Fitur Utama
                                - **Articles**: Ambil, filter, dan cari artikel berita
                                - **Sources**: Kelola sumber RSS feed
                                - **Crawler**: Trigger crawl manual dan lihat riwayat crawl
                                - **Cache**: Monitor dan evict Redis cache
                                - **Digest**: Kirim email digest harian ke user

                                ## Format Response
                                Semua response dibungkus dalam `ApiResponse<T>`:
                                ```json
                                {
                                  "success": true,
                                  "message": "Pesan status",
                                  "data": { ... },
                                  "timestamp": "2024-01-15T10:00:00"
                                }
                                ```

                                ## Pagination
                                Endpoint list menggunakan Spring `Page<T>` — halaman dimulai dari `0` (0-indexed).
                                """)
                        .contact(new Contact()
                                .name("News Aggregator")
                                .email("r.ardhinto@gmail.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server")
                ));
    }
}

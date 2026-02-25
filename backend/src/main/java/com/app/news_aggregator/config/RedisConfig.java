package com.app.news_aggregator.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * RedisConfig mengkonfigurasi Redis sebagai cache store.
 *
 * @EnableCaching mengaktifkan Spring Cache abstraction.
 * Setelah ini, annotation @Cacheable, @CacheEvict, @CachePut akan berfungsi.
 *
 * Kenapa perlu konfigurasi khusus?
 * - Default Spring Redis pakai Java serialization → tidak readable di Redis CLI
 * - Kita mau JSON agar mudah debug via redis-cli atau RedisInsight
 * - Kita mau TTL berbeda untuk setiap jenis cache
 */
@Configuration
@EnableCaching
public class RedisConfig {

    // =============================================
    // NAMA-NAMA CACHE (konstanta)
    // Dipakai di @Cacheable(value = "...")
    // =============================================
    public static final String CACHE_ARTICLES = "articles";
    public static final String CACHE_ARTICLE_DETAIL = "article_detail";
    public static final String CACHE_SOURCES = "sources";
    public static final String CACHE_CATEGORIES = "categories";
    public static final String CACHE_SEARCH = "search";

    /**
     * ObjectMapper khusus untuk Redis serialization.
     *
     * Kenapa tidak pakai ObjectMapper default?
     * - Redis perlu tahu TYPE dari object yang disimpan agar bisa deserilisasi
     * - Kita aktifkan DefaultTyping: Redis akan simpan nama class bersama datanya
     * - JavaTimeModule diperlukan agar LocalDateTime bisa di-serialisasi ke JSON
     */
    @Bean(name = "redisObjectMapper")
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Handle LocalDateTime, LocalDate, dll
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Simpan type info agar deserilisasi bisa tahu class yang tepat
        mapper.activateDefaultTyping(
                mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }

    /**
     * RedisTemplate: dipakai untuk operasi Redis manual (get, set, delete).
     * Key: String, Value: JSON.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key disimpan sebagai String biasa
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value disimpan sebagai JSON
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * CacheManager: mengelola semua cache dalam aplikasi.
     *
     * Kita set TTL berbeda untuk setiap cache:
     * - articles      : 5 menit (data sering berubah karena crawl tiap 15 menit)
     * - article_detail: 10 menit (detail artikel jarang berubah)
     * - sources       : 30 menit (sumber RSS jarang berubah)
     * - categories    : 60 menit (kategori sangat jarang berubah)
     * - search        : 2 menit (hasil search lebih dinamis)
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        // Konfigurasi default untuk cache yang tidak punya setting khusus
        RedisCacheConfiguration defaultConfig = buildCacheConfig(Duration.ofMinutes(5));

        // Konfigurasi spesifik per cache dengan TTL berbeda
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put(CACHE_ARTICLES,       buildCacheConfig(Duration.ofMinutes(5)));
        cacheConfigs.put(CACHE_ARTICLE_DETAIL, buildCacheConfig(Duration.ofMinutes(10)));
        cacheConfigs.put(CACHE_SOURCES,        buildCacheConfig(Duration.ofMinutes(30)));
        cacheConfigs.put(CACHE_CATEGORIES,     buildCacheConfig(Duration.ofMinutes(60)));
        cacheConfigs.put(CACHE_SEARCH,         buildCacheConfig(Duration.ofMinutes(2)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    /**
     * Helper: buat RedisCacheConfiguration dengan TTL tertentu.
     * Semua cache pakai JSON serializer agar readable.
     */
    private RedisCacheConfiguration buildCacheConfig(Duration ttl) {
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                // Key format: "cacheName::cacheKey" (contoh: "articles::teknologi_0")
                .computePrefixWith(cacheName -> cacheName + "::")
                // Serialisasi key sebagai String
                .serializeKeysWith(
                    RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                // Serialisasi value sebagai JSON
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer))
                // Jangan cache null value (hindari NullPointerException)
                .disableCachingNullValues();
    }
}
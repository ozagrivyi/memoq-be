package com.memoq.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.memoq.backend.dto.CategoryDto;
import com.memoq.backend.dto.QuestionDto;
import com.memoq.backend.dto.QuestionPage;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String QUESTIONS_CACHE = "questions";
    public static final String QUESTION_PAGES_CACHE = "questionPages";
    public static final String CATEGORIES_CACHE = "categories";

    /**
     * Local/dev cache manager. Under k8s, set spring.cache.type=redis (see application-prod.yml)
     * so Spring Boot's own RedisAutoConfiguration provides the CacheManager instead.
     */
    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "caffeine", matchIfMissing = true)
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager manager =
                new CaffeineCacheManager(QUESTIONS_CACHE, QUESTION_PAGES_CACHE, CATEGORIES_CACHE);
        manager.setCaffeine(Caffeine.newBuilder().maximumSize(2_000).expireAfterWrite(10, TimeUnit.MINUTES));
        return manager;
    }

    /**
     * Only takes effect when spring.cache.type=redis (application-prod.yml) and Spring Boot
     * builds its own RedisCacheManager. The default RedisCacheManager uses JDK serialization for
     * cache values, which fails for our DTO records (not Serializable) — and Spring Cache's
     * generic (type-erased) API means a single shared JSON serializer can't recover the right
     * element type for a cached {@code List<CategoryDto>} without embedding polymorphic type
     * metadata, which is its own source of version-fragile mismatches. Simplest reliable fix:
     * one serializer per cache name, each bound to that cache's exact concrete value type via an
     * explicit {@link JavaType} — no polymorphism needed since each cache only ever holds one
     * shape of value.
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(ObjectMapper objectMapper) {
        JavaType categoryListType = objectMapper.getTypeFactory().constructCollectionType(List.class, CategoryDto.class);
        JavaType questionType = objectMapper.getTypeFactory().constructType(QuestionDto.class);
        JavaType questionPageType = objectMapper.getTypeFactory().constructType(QuestionPage.class);

        RedisCacheConfiguration categoriesConfig = cacheConfigFor(objectMapper, categoryListType);
        RedisCacheConfiguration questionsConfig = cacheConfigFor(objectMapper, questionType);
        RedisCacheConfiguration questionPagesConfig = cacheConfigFor(objectMapper, questionPageType);

        return builder -> builder.withCacheConfiguration(CATEGORIES_CACHE, categoriesConfig)
                .withCacheConfiguration(QUESTIONS_CACHE, questionsConfig)
                .withCacheConfiguration(QUESTION_PAGES_CACHE, questionPagesConfig);
    }

    private static RedisCacheConfiguration cacheConfigFor(ObjectMapper objectMapper, JavaType type) {
        SerializationPair<Object> serialization =
                SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<Object>(objectMapper, type));
        return RedisCacheConfiguration.defaultCacheConfig().serializeValuesWith(serialization);
    }
}

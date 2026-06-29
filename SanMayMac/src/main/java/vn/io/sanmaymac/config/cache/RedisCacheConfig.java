package vn.io.sanmaymac.config.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
public class RedisCacheConfig implements CachingConfigurer {
    private static final Logger log = LoggerFactory.getLogger(RedisCacheConfig.class);

    /** Bump when serializer format changes — tránh đọc key Redis cũ không tương thích. */
    private static final String CACHE_KEY_PREFIX = "sanmaymac:v4:";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith(CACHE_KEY_PREFIX)
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                "catalog:categories", defaultConfig.entryTtl(Duration.ofHours(24)),
                "catalog:newest-products", defaultConfig.entryTtl(Duration.ofMinutes(10)),
                "catalog:product-detail", defaultConfig.entryTtl(Duration.ofMinutes(15)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn(
                        "Cache GET failed for cache={} key={}. Evicting stale entry and falling back to DB.",
                        cache != null ? cache.getName() : "unknown",
                        key,
                        exception);
                if (cache != null && key != null) {
                    try {
                        cache.evict(key);
                    } catch (RuntimeException evictEx) {
                        log.warn(
                                "Failed to evict stale cache entry cache={} key={}",
                                cache.getName(),
                                key,
                                evictEx);
                    }
                }
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                log.warn("Cache PUT failed for cache={} key={}. Skip cache write.", cache != null ? cache.getName() : "unknown", key, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Cache EVICT failed for cache={} key={}. Continue request.", cache != null ? cache.getName() : "unknown", key, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                log.warn("Cache CLEAR failed for cache={}. Continue request.", cache != null ? cache.getName() : "unknown", exception);
            }
        };
    }

    /**
     * Chỉ cấu hình JavaTime — KHÔNG dùng activateDefaultTyping.
     * GenericJackson2JsonRedisSerializer tự gắn @class khi serialize/deserialize.
     */
    private static ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}

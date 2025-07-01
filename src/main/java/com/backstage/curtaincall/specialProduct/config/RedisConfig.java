package com.backstage.curtaincall.specialProduct.config;

import com.backstage.curtaincall.specialProduct.dto.SpecialProductDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    private final ObjectMapper objectMapper;

    // ObjectMapper 주입 받기 (JacksonConfiguration에서 생성된 Bean 사용)
    public RedisConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        // Lettuce라는 라이브러리를 활용해 Redis 연결을 관리하는 객체를 생성하고
        // Redis 서버에 대한 정보(host, port)를 설정한다.
        return new LettuceConnectionFactory(new RedisStandaloneConfiguration(host, port));
    }

    @Bean
    public RedisTemplate<String, SpecialProductDto> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, SpecialProductDto> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // Key 직렬화
        template.setKeySerializer(new StringRedisSerializer());

        // Value 직렬화 (ObjectMapper 적용)
        Jackson2JsonRedisSerializer<SpecialProductDto> serializer = new Jackson2JsonRedisSerializer<>(SpecialProductDto.class);
        serializer.setObjectMapper(objectMapper);  // 기존 ObjectMapper 사용

        template.setValueSerializer(serializer);
        template.afterPropertiesSet();

        return template;
    }
}
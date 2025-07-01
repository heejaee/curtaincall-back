package com.backstage.curtaincall.specialProduct.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfiguration {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        // Jackson을 이용한 Redis 직렬화 설정
        ObjectMapper mapper = new ObjectMapper();
        // JavaTimeModule을 등록합니다.
        mapper.registerModule(new JavaTimeModule());
        // LocalDate, LocalDateTime 등의 타입을 ISO-8601 문자열로 직렬화
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}


package com.backstage.curtaincall.recommend.service;

import com.backstage.curtaincall.product.dto.ProductResponseDto;
import com.backstage.curtaincall.product.entity.Product;
import com.backstage.curtaincall.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRecommendService {
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ProductRepository productRepository;

    private static final String LAST_VIEWED_PRODUCT_KEY = "user:lastViewed:";

    // 사용자의 클릭 이벤트 저장
    @Transactional
    public void saveUserClick(Long userId, Long productId) {
        kafkaTemplate.send("user-click-events", userId + "," + productId);
    }

    // 연쇄 클릭 이벤트 저장 (이전 상품 A → 현재 상품 B)
    @Transactional
    public void saveUserClickSequence(Long userId, Long currentProductId) {
        String redisKey = LAST_VIEWED_PRODUCT_KEY + userId;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();

        String lastViewedProductId = ops.get(redisKey);

        // 만약 이전에 봤던 상품이 있다면, A → B 관계 저장
        if (lastViewedProductId != null && !lastViewedProductId.equals(currentProductId.toString())) {
            Long previousProductId = Long.parseLong(lastViewedProductId);
            kafkaTemplate.send("related-product-events", userId + "," + previousProductId + "," + currentProductId);
        }

        // Redis에 현재 상품을 최신값으로 갱신
        ops.set(redisKey, currentProductId.toString());
    }

    // 인기 장르 추출
    @Transactional
    public Long getMostClickedCategory(Long userId) {
        String redisKey = "user:clicks:categories:" + userId;

        // Redis에서 가장 많이 클릭된 카테고리 찾기
        Map<Object, Object> categoryClicks = redisTemplate.opsForHash().entries(redisKey);

        return categoryClicks.entrySet().stream()
                .max(Comparator.comparingLong(e -> Long.parseLong(e.getValue().toString())))
                .map(entry -> Long.parseLong(entry.getKey().toString()))
                .orElse(null);
    }

    // 가장 많이 클릭한 카테고리의 인기 작품 추출
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getRecommendedProductsByCategory(Long userId) {
        // 가장 많이 클릭한 카테고리 가져오기
        Long mostClickedCategory = getMostClickedCategory(userId);

        if (mostClickedCategory == null) {
            return Collections.emptyList(); // 클릭한 카테고리가 없으면 빈 리스트 반환
        }

        // 해당 카테고리에서 판매량이 높은 상품을 가져오기
        return productRepository.findTop5ByCategoryIdOrderBySalesCountDesc(mostClickedCategory).stream()
                .map(ProductResponseDto::fromEntity) // 트랜잭션 안에서 DTO로 변환
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getRecommendedProductsByChain(Long userId) {
        // 사용자가 가장 많이 클릭한 상품 찾기
        String userClicksKey = "user:clicks:products:" + userId;
        Map<Object, Object> clickedProducts = redisTemplate.opsForHash().entries(userClicksKey);

        if (clickedProducts.isEmpty()) {
            return Collections.emptyList(); // 클릭 기록이 없으면 빈 리스트 반환
        }

        // 가장 많이 클릭한 상품 찾기
        Long mostClickedProductId = clickedProducts.entrySet().stream()
                .max(Comparator.comparingLong(entry -> Long.parseLong(entry.getValue().toString())))
                .map(entry -> Long.parseLong(entry.getKey().toString()))
                .orElse(null);

        if (mostClickedProductId == null) {
            return Collections.emptyList();
        }

        // 해당 상품의 연관 상품 ID 목록 가져오기
        String redisKey = "related:clicks:" + mostClickedProductId;
        Set<String> recommendedProductIds = redisTemplate.opsForZSet()
                .reverseRange(redisKey, 0, 4); // 상위 5개 추천

        if (recommendedProductIds == null || recommendedProductIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 연관 상품 ID 리스트를 Product 엔티티로 변환
        List<Long> productIds = recommendedProductIds.stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());

        return productRepository.findAllById(productIds).stream()
                .map(ProductResponseDto::fromEntity)
                .collect(Collectors.toList());
    }




}

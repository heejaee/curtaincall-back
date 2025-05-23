package com.backstage.curtaincall.specialProduct.service;

import static com.backstage.curtaincall.global.exception.CustomErrorCode.PRODUCT_NOT_FOUND;
import static com.backstage.curtaincall.global.exception.CustomErrorCode.SPECIAL_PRODUCT_NOT_FOUND;
import static com.backstage.curtaincall.global.exception.CustomErrorCode.UPCOMING_SPECIAL_PRODUCT_NOT_FOUND;

import com.backstage.curtaincall.global.exception.CustomException;
import com.backstage.curtaincall.product.entity.Product;
import com.backstage.curtaincall.product.repository.ProductRepository;
import com.backstage.curtaincall.specialProduct.dto.SpecialProductDto;
import com.backstage.curtaincall.specialProduct.entity.SpecialProduct;
import com.backstage.curtaincall.specialProduct.repository.SpecialProductRepository;
import com.backstage.curtaincall.specialProduct.validator.SpecialProductValidator;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@Service
@Slf4j
@RequiredArgsConstructor
public class SpecialProductService {

    private final SpecialProductRepository specialProductRepository;
    private final ProductRepository productRepository; // Product 조회용
    private final RedisTemplate<String, SpecialProductDto> redisTemplate;
    private final SpecialProductValidator specialProductValidator;


    //단건조회(삭제되지 않은 것만)
    public SpecialProduct findById(Long id){
        return specialProductRepository.findById(id)
                .orElseThrow(() -> new CustomException(SPECIAL_PRODUCT_NOT_FOUND));
    }

    // Redis에서 캐시된 ACTIVE 특가상품 가져오기
    public List<SpecialProductDto> getActiveSpecialProducts() {
        ValueOperations<String, SpecialProductDto> valueOps = redisTemplate.opsForValue();

        // Redis에서 모든 활성화된 특가 상품 키 가져오기
        Set<String> keys = redisTemplate.keys("specialProductCache::specialProduct:*");

        if (!keys.isEmpty()) {
            List<SpecialProductDto> cachedProducts = keys.stream()
                    .map(valueOps::get)
                    .filter(Objects::nonNull)
                    .toList();

            if (!cachedProducts.isEmpty()) {
                return cachedProducts; // 캐시에 데이터가 있으면 반환
            }
        }

        // 캐시가 비어 있으면 DB에서 조회
        List<SpecialProduct> activeProducts = specialProductRepository.findAllActive();
        List<SpecialProductDto> activeProductsDto = activeProducts.stream()
                .map(SpecialProduct::toDto)
                .toList();

        // Redis에 저장 (TTL 24시간 설정)
        for (SpecialProductDto dto : activeProductsDto) {
            valueOps.set("specialProductCache::specialProduct:" + dto.getSpecialProductId(), dto, Duration.ofHours(24));
        }


        return activeProductsDto;
    }


    // 이름 검색 및 페이지네이션을 적용한 전체 조회
    public Page<SpecialProductDto> getSpecialProducts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SpecialProduct> spPage = specialProductRepository.findAll(keyword, pageable);
        return spPage.map(SpecialProduct::toDto);
    }

    // 삭제된것만 전체 조회
    public List<SpecialProductDto> findAllDeleted() {
        List<SpecialProduct> specialProducts = specialProductRepository.findAllDeleted();
        return specialProducts.stream()
                .map(SpecialProduct::toDto)
                .toList();
    }

    //상품id와 관련된 모든 특가상품 가져오기
    public List<SpecialProduct> findAllByProductId(Long productId){
        return specialProductRepository.findAllByProductId(productId);
    }


    // 단건 생성
    public SpecialProductDto save(SpecialProductDto dto) {
        // 통합 검증 메서드
        specialProductValidator.validate(dto);

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new CustomException(PRODUCT_NOT_FOUND));

        SpecialProduct sp = SpecialProduct.of(product, dto);
        specialProductRepository.save(sp);
        return sp.toDto();
    }

    private final PlatformTransactionManager transactionManager;

    public <T> T executeInTransaction(Supplier<T> action) {
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());

        log.info("[트랜잭션 시작]");

        try {
            T result = action.get(); // 실제 작업
            log.info("[비즈니스 로직 실행됨]");
            transactionManager.commit(status);
            log.info("[커밋 완료]");
            return result;
        } catch (RuntimeException | Error e) {
            transactionManager.rollback(status);
            log.info("[롤백 발생]");
            throw e;
        }
    }

    // 수정: 캐시 반영 O
    @CachePut(cacheNames = "specialProductCache", key = "'specialProduct:' + #dto.specialProductId", cacheManager = "cacheManager")
    public SpecialProductDto updateWithCache(SpecialProduct sp, SpecialProductDto dto) {
        return executeInTransaction(() -> {
            sp.update(dto);
            specialProductRepository.update(dto);
            return sp.toDto();
        });
    }

    // 수정: 캐시 업데이트 반영 X
    public void updateWithOutCache(SpecialProduct sp, SpecialProductDto dto) {
        executeInTransaction(() -> {
            sp.update(dto);
            specialProductRepository.update(dto);
            return null;
        });
    }


    // 수정: 캐시 반영 O
//    @Transactional
//    @CachePut(cacheNames = "specialProductCache", key = "'specialProduct:' + #dto.specialProductId", cacheManager = "cacheManager")
//    public SpecialProductDto updateWithCache(SpecialProduct sp, SpecialProductDto dto) {
//        sp.update(dto);
//        return sp.toDto();
//    }
//
//    // 수정: 캐시 업데이트 반영 X
//    @Transactional
//    public void updateWithOutCache(SpecialProduct sp, SpecialProductDto dto) {
//        sp.update(dto);
//    }

    // Soft 삭제 : 캐시 반영 O
    @Transactional
    @CacheEvict(cacheNames = "specialProductCache", key = "'specialProduct:' + #sp.id", cacheManager = "cacheManager")
    public SpecialProductDto deleteWithCache(SpecialProduct sp) {
        sp.delete();
        return sp.toDto();
    }

    // Soft 삭제 : 캐시 반영 X
    @Transactional
    public void deleteWithOutCache(SpecialProduct sp) {
        sp.delete();
    }

    // 승인: 캐시에 복구된 엔티티 업데이트
    @Transactional
    @CachePut(cacheNames = "specialProductCache", key = "'specialProduct:' + #id", cacheManager = "cacheManager")
    public SpecialProductDto approve(Long id) {
        SpecialProduct sp = specialProductRepository.findByIdUpcoming(id)
                .orElseThrow(() -> new CustomException(UPCOMING_SPECIAL_PRODUCT_NOT_FOUND));

        // 이미 같은 Product에 ACTIVE 상태의 특가 상품이 있는지 확인
        specialProductValidator.alreadyActiveProduct(sp.getProduct().getProductId());

        //할인 시작일이나 할인 종료일이 오늘보다 적으면 오류발생
        specialProductValidator.discountExpired(sp.getStartDate(),sp.getEndDate());

        sp.approve();
        return sp.toDto();
    }


    //승인 취소
    @Transactional
    @CacheEvict(cacheNames = "specialProductCache", key = "'specialProduct:' + #id", cacheManager = "cacheManager")
    public SpecialProductDto approveCancel(Long id) {
        SpecialProduct sp = specialProductRepository.findByIdActive(id)
                .orElseThrow(() -> new CustomException(SPECIAL_PRODUCT_NOT_FOUND));

        sp.approveCancel();// 다시 할인 예정 상태로 변경
        return sp.toDto();
    }
}
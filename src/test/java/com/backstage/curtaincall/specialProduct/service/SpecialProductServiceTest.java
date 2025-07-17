package com.backstage.curtaincall.specialProduct.service;

import com.backstage.curtaincall.global.exception.CustomErrorCode;
import com.backstage.curtaincall.global.exception.CustomException;
import com.backstage.curtaincall.product.entity.Product;
import com.backstage.curtaincall.product.entity.ProductImage;
import com.backstage.curtaincall.product.repository.ProductRepository;
import com.backstage.curtaincall.specialProduct.dto.SpecialProductDto;
import com.backstage.curtaincall.specialProduct.entity.SpecialProduct;
import com.backstage.curtaincall.specialProduct.entity.SpecialProductStatus;
import com.backstage.curtaincall.specialProduct.repository.SpecialProductRepository;
import com.backstage.curtaincall.specialProduct.validator.SpecialProductValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecialProductServiceTest {

    @InjectMocks
    private SpecialProductService service;

    @Mock private SpecialProductRepository specialProductRepository;
    @Mock private ProductRepository productRepository;
    @Mock private RedisTemplate<String, SpecialProductDto> redisTemplate;
    @Mock private ValueOperations<String, SpecialProductDto> valueOperations;
    @Mock private SpecialProductValidator specialProductValidator;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private TransactionStatus transactionStatus;

    private SpecialProductDto upCommingDto;
    private SpecialProductDto deletedDto;
    private SpecialProductDto activeDto;
    private Product product;
    private SpecialProduct upcommingSpecialProduct;
    private SpecialProduct deletedSpecialProduct;
    private SpecialProduct activeSpecialProduct;

    @BeforeEach
    void setup() {
        // ProductImage mock 생성
        ProductImage mockImage = mock(ProductImage.class);

        product = Product.builder()
                .productId(1L)
                .productName("테스트상품")
                .price(5000)
                .startDate(LocalDate.of(2025, 7, 10))
                .endDate(LocalDate.of(2025, 7, 30))
                .productImage(mockImage)
                .build();

        upCommingDto = SpecialProductDto.builder()
                .productId(1L)
                .discountRate(10)
                .discountStartDate(LocalDate.of(2025, 7, 15))
                .discountEndDate(LocalDate.of(2025, 7, 20))
                .status(SpecialProductStatus.UPCOMING)
                .startDate(product.getStartDate())
                .endDate(product.getEndDate())
                .specialProductId(99L)
                .build();

        activeDto = SpecialProductDto.builder()
                .productId(1L)
                .discountRate(10)
                .discountStartDate(LocalDate.of(2025, 7, 15))
                .discountEndDate(LocalDate.of(2025, 7, 20))
                .status(SpecialProductStatus.ACTIVE)
                .startDate(product.getStartDate())
                .endDate(product.getEndDate())
                .specialProductId(99L)
                .build();


        deletedDto = SpecialProductDto.builder()
                .productId(1L)
                .discountRate(10)
                .discountStartDate(LocalDate.of(2025, 7, 15))
                .discountEndDate(LocalDate.of(2025, 7, 20))
                .status(SpecialProductStatus.DELETED)
                .startDate(product.getStartDate())
                .endDate(product.getEndDate())
                .specialProductId(98L)
                .build();

        upcommingSpecialProduct = SpecialProduct.of(product, upCommingDto);
        deletedSpecialProduct = SpecialProduct.of(product, deletedDto);
        activeSpecialProduct = SpecialProduct.of(product, activeDto);
    }


    @Test
    @DisplayName("특가상품 ID로 조회 성공")
    void findById_success() {
        // Given
        when(specialProductRepository.findById(99L)).thenReturn(Optional.of(upcommingSpecialProduct));

        // When
        SpecialProduct result = service.findById(99L);

        // Then
        assertThat(result).isEqualTo(upcommingSpecialProduct);
        verify(specialProductRepository).findById(99L);
    }

    @Test
    @DisplayName("특가상품 ID로 조회 실패 - 존재하지 않음")
    void findById_notFound() {
        // Given
        when(specialProductRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        CustomException ex = assertThrows(CustomException.class, () -> service.findById(1L));
        assertThat(ex.getCustomErrorCode()).isEqualTo(CustomErrorCode.SPECIAL_PRODUCT_NOT_FOUND);
        verify(specialProductRepository).findById(1L);
    }

    @Test
    @DisplayName("Redis 캐시에 데이터가 있을 때 ACTIVE 특가상품은 캐시에서 조회")
    void getActiveSpecialProducts_cacheHit() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.keys("specialProductCache::specialProduct:*"))
                .thenReturn(Set.of("specialProductCache::specialProduct:99"));
        when(valueOperations.get("specialProductCache::specialProduct:99"))
                .thenReturn(upCommingDto);

        // When
        List<SpecialProductDto> result = service.getActiveSpecialProducts();

        // Then
        assertThat(result).hasSize(1).first().isEqualTo(upCommingDto);
        // DB 조회가 호출되지 않았는지 확인
        verify(specialProductRepository, never()).findAllActive();
    }

    @Test
    @DisplayName("Redis 캐시에 없을 때 ACTIVE 특가상품은 DB 조회 후 캐시 저장")
    void getActiveSpecialProducts_cacheMiss() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.keys("specialProductCache::specialProduct:*"))
                .thenReturn(Collections.emptySet());
        when(specialProductRepository.findAllActive()).thenReturn(List.of(upcommingSpecialProduct));

        // When
        List<SpecialProductDto> result = service.getActiveSpecialProducts();

        // Then
        assertThat(result).hasSize(1);
        //DB 조회됐는지 확인
        verify(specialProductRepository).findAllActive();
        // Redis에 저장했는지 확인
        verify(valueOperations).set(eq("specialProductCache::specialProduct:" + upcommingSpecialProduct.getId()), any(), any());
    }

    @Test
    @DisplayName("삭제되지 않은 특가상품 이름 검색 성공")
    void getSpecialProducts_success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<SpecialProduct> page = new PageImpl<>(List.of(upcommingSpecialProduct));
        when(specialProductRepository.findAll("상품", pageable)).thenReturn(page);

        // When
        Page<SpecialProductDto> result = service.getSpecialProducts("상품", 0, 10);

        // Then
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("삭제된 특가상품 전체 조회 성공")
    void findAllDeleted_success() {
        // Given
        when(specialProductRepository.findAllDeleted()).thenReturn(List.of(deletedSpecialProduct));
        // When
        List<SpecialProductDto> result = service.findAllDeleted();
        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("상품 ID로 관련 특가상품 모두 조회")
    void findAllByProductId_success() {
        // Given
        when(specialProductRepository.findAllByProductId(1L)).thenReturn(List.of(upcommingSpecialProduct,deletedSpecialProduct));
        // When
        List<SpecialProduct> result = service.findAllByProductId(1L);
        // Then
        assertThat(result).hasSize(2);
    }

    @DisplayName("특가상품 저장 성공")
    @Test
    void save_success() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        doNothing().when(specialProductRepository).save(any(SpecialProduct.class));

        // When
        SpecialProductDto result = service.save(upCommingDto);

        // Then
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(upcommingSpecialProduct.toDto());
        verify(specialProductValidator).validateSave(upCommingDto);
        verify(specialProductRepository).save(any(SpecialProduct.class));
    }

    @Test
    @DisplayName("상품이 존재하지 않으면 예외 발생")
    void save_productNotFound() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        CustomException ex = assertThrows(CustomException.class, () -> service.save(upCommingDto));
        assertThat(ex.getCustomErrorCode()).isEqualTo(CustomErrorCode.PRODUCT_NOT_FOUND);
    }


    @Test
    @DisplayName("updateWithCache 성공")
    void updateWithCache_success() {
        // Given
        SpecialProductDto changeDto = SpecialProductDto.builder()
                .productId(1L)
                .discountRate(20)// 변경
                .discountStartDate(LocalDate.of(2025, 7, 15))
                .discountEndDate(LocalDate.of(2025, 7, 20))
                .status(SpecialProductStatus.UPCOMING)
                .startDate(product.getStartDate())
                .endDate(product.getEndDate())
                .specialProductId(99L)
                .build();

        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        // When
        SpecialProductDto result = service.updateWithCache(upcommingSpecialProduct, changeDto);
        // Then
        assertThat(result.getDiscountRate()).isEqualTo(changeDto.getDiscountRate());
        verify(specialProductRepository).update(changeDto);
    }

    @Test
    @DisplayName("updateWithOutCache 성공")
    void updateWithOutCache_success() {
        // Given
        SpecialProductDto changeDto = SpecialProductDto.builder()
                .productId(1L)
                .discountRate(20)// 변경
                .discountStartDate(LocalDate.of(2025, 7, 15))
                .discountEndDate(LocalDate.of(2025, 7, 20))
                .status(SpecialProductStatus.UPCOMING)
                .startDate(product.getStartDate())
                .endDate(product.getEndDate())
                .specialProductId(99L)
                .build();
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        // When
        service.updateWithOutCache(upcommingSpecialProduct, changeDto);
        // Then
        verify(specialProductRepository).update(changeDto);
    }

    @Test
    @DisplayName("deleteWithCache 성공")
    void deleteWithCache_success() {
        // Given
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        // When
        SpecialProductDto result = service.deleteWithCache(upcommingSpecialProduct);
        // Then
        assertThat(result.getStatus()).isEqualTo(SpecialProductStatus.DELETED);
        verify(specialProductRepository).delete(upcommingSpecialProduct);
        verify(redisTemplate).delete("specialProductCache::specialProduct:" + upcommingSpecialProduct.getId());
    }

    @Test
    @DisplayName("deleteWithOutCache 성공")
    void deleteWithOutCache_success() {
        // Given
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        // When
        service.deleteWithOutCache(upcommingSpecialProduct);
        // Then
        verify(specialProductRepository).delete(upcommingSpecialProduct);
    }

    @Test
    @DisplayName("UPCOMING 특가상품 승인")
    void approve_success() {
        // Given
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        when(specialProductRepository.findByIdUpcoming(1L)).thenReturn(Optional.of(upcommingSpecialProduct));

        // When
        SpecialProductDto result = service.approve(1L);

        // Then
        assertThat(result.getStatus()).isEqualTo(SpecialProductStatus.ACTIVE);
        verify(specialProductRepository).approve(upcommingSpecialProduct);
        verify(specialProductValidator).alreadyActiveProduct(product.getProductId());
        verify(specialProductValidator).discountExpired(upcommingSpecialProduct.getStartDate(), upcommingSpecialProduct.getEndDate());
    }


    @Test
    @DisplayName("approveCancel 성공")
    void approveCancel_success() {
        // Given
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        when(specialProductRepository.findByIdActive(1L)).thenReturn(Optional.of(activeSpecialProduct));
        // When
        SpecialProductDto result = service.approveCancel(1L);
        // Then
        assertThat(result.getStatus()).isEqualTo(SpecialProductStatus.UPCOMING);
        verify(specialProductRepository).approveCancel(activeSpecialProduct);
    }

    @Test
    @DisplayName("approveCancel 실패 - ACTIVE 특가상품 없음")
    void approveCancel_notFound() {
        // Given
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        when(specialProductRepository.findByIdActive(1L)).thenReturn(Optional.empty());

        // When & Then
        CustomException ex = assertThrows(CustomException.class, () -> service.approveCancel(1L));
        assertThat(ex.getCustomErrorCode()).isEqualTo(CustomErrorCode.SPECIAL_PRODUCT_NOT_FOUND);
    }
}

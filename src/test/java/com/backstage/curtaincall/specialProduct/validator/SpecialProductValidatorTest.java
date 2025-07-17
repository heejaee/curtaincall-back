package com.backstage.curtaincall.specialProduct.validator;

import com.backstage.curtaincall.global.exception.CustomErrorCode;
import com.backstage.curtaincall.global.exception.CustomException;
import com.backstage.curtaincall.specialProduct.dto.SpecialProductDto;
import com.backstage.curtaincall.specialProduct.entity.SpecialProduct;
import com.backstage.curtaincall.specialProduct.repository.SpecialProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SpecialProductValidatorTest {

    private SpecialProductValidator validator;
    private SpecialProductRepository repository;

    @BeforeEach
    void setup() {
        repository = mock(SpecialProductRepository.class);
        validator = new SpecialProductValidator(repository);
    }

    @Test
    @DisplayName("할인 종료일이 시작일보다 빠르면 예외 발생")
    void endDateBeforeStart() {
        // Given
        SpecialProductDto dto = SpecialProductDto.builder()
                .discountStartDate(LocalDate.of(2025, 7, 15))
                .discountEndDate(LocalDate.of(2025, 7, 10))
                .build();

        // When & Then
        assertThatThrownBy(() -> validator.validateSave(dto))
                .isInstanceOf(CustomException.class)
                .extracting("customErrorCode")
                .isEqualTo(CustomErrorCode.DISCOUNT_END_DATE_BEFORE_START);
    }

    @Test
    @DisplayName("할인 기간이 과거면 예외 발생")
    void discountExpired() {
        // Given
        LocalDate pastStart = LocalDate.of(2020, 1, 1);
        LocalDate pastEnd = LocalDate.of(2020, 1, 2);

        SpecialProductDto dto = SpecialProductDto.builder()
                .discountStartDate(pastStart)
                .discountEndDate(pastEnd)
                .build();

        // When & Then
        assertThatThrownBy(() -> validator.validateSave(dto))
                .isInstanceOf(CustomException.class)
                .extracting("customErrorCode")
                .isEqualTo(CustomErrorCode.CANNOT_APPLY_DISCOUNT_FOR_PAST_DATE);
    }

    @Test
    @DisplayName("할인 기간이 공연 기간을 벗어나면 예외 발생")
    void overDate() {
        // Given
        SpecialProductDto dto = SpecialProductDto.builder()
                .discountStartDate(LocalDate.of(2026, 7, 13))
                .discountEndDate(LocalDate.of(2026, 7, 15))
                .startDate(LocalDate.of(2026, 7, 10))
                .endDate(LocalDate.of(2026, 7, 12))
                .build();

        // When & Then
        assertThatThrownBy(() -> validator.validateSave(dto))
                .isInstanceOf(CustomException.class)
                .extracting("customErrorCode")
                .isEqualTo(CustomErrorCode.DISCOUNT_PERIOD_OUT_OF_PRODUCT_RANGE);
    }

    @Test
    @DisplayName("중복 할인 기간이 존재하면 예외 발생")
    void overlappingDiscount() {
        // Given
        SpecialProductDto dto = SpecialProductDto.builder()
                .productId(1L)
                .specialProductId(99L)
                .discountStartDate(LocalDate.of(2026, 7, 15))
                .discountEndDate(LocalDate.of(2026, 7, 20))
                .startDate(LocalDate.of(2026, 7, 10))
                .endDate(LocalDate.of(2026, 7, 30))
                .build();

        when(repository.findAllOverlappingDates(anyLong(), anyLong(), any(), any()))
                .thenReturn(Collections.singletonList(mock(SpecialProduct.class)));

        // When & Then
        assertThatThrownBy(() -> validator.validateSave(dto))
                .isInstanceOf(CustomException.class)
                .extracting("customErrorCode")
                .isEqualTo(CustomErrorCode.OVERLAPPING_SPECIAL_PRODUCT_DISCOUNT);
    }
}

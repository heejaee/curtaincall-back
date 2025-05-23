package com.backstage.curtaincall.specialProduct.validator;

import com.backstage.curtaincall.global.exception.CustomErrorCode;
import com.backstage.curtaincall.global.exception.CustomException;
import com.backstage.curtaincall.specialProduct.dto.SpecialProductDto;
import com.backstage.curtaincall.specialProduct.entity.SpecialProduct;
import com.backstage.curtaincall.specialProduct.repository.SpecialProductRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpecialProductValidator {

    private final SpecialProductRepository specialProductRepository;

    public void validateUpdate(SpecialProductDto dto) {
        // 할인 종료일이 할인 시작일보다 이전이면 오류발생
        endDateBeforeStart(dto);
        //할인 종료일이 오늘보다 적으면 오류발생
        discountExpired(dto.getDiscountEndDate());
        // 할인 날짜가 공연날짜 범위를 벗어나면 오류발생
        overDate(dto);
        //한 상품에 2개의 할인적용 날짜가 겹치면 오류발생
        overLappingDate(dto);
    }

    public void validateSave(SpecialProductDto dto) {
        // 할인 종료일이 할인 시작일보다 이전이면 오류발생
        endDateBeforeStart(dto);
        //할인 시작일이나 할인 종료일이 오늘보다 적으면 오류발생
        discountExpired(dto.getDiscountStartDate(),dto.getDiscountEndDate());
        // 할인 날짜가 공연날짜 범위를 벗어나면 오류발생
        overDate(dto);
        //한 상품에 2개의 할인적용 날짜가 겹치면 오류발생
        overLappingDate(dto);
    }

    public void alreadyActiveProduct(Long productId) {
        boolean isAlreadyActive = specialProductRepository.existsByProductIdAndStatus(productId);
        if (isAlreadyActive) {
            throw new CustomException(CustomErrorCode.ALREADY_ACTIVE_SPECIAL_PRODUCT_EXISTS);
        }
    }

    public void discountExpired(LocalDate discountStartDate, LocalDate discountEndDate) {
        LocalDate now = LocalDate.now();
        if (now.isAfter(discountStartDate) || now.isAfter(discountEndDate)) {
            throw new CustomException(CustomErrorCode.CANNOT_APPLY_DISCOUNT_FOR_PAST_DATE);
        }
    }

    public void discountExpired(LocalDate discountEndDate) {
        LocalDate now = LocalDate.now();
        if (now.isAfter(discountEndDate)) {
            throw new CustomException(CustomErrorCode.CANNOT_APPLY_DISCOUNT_FOR_PAST_DATE);
        }
    }

    public void overDate(SpecialProductDto dto) {
        if (dto.getStartDate().isAfter(dto.getDiscountStartDate()) ||
                dto.getEndDate().isBefore(dto.getDiscountEndDate())) {
            throw new CustomException(CustomErrorCode.DISCOUNT_PERIOD_OUT_OF_PRODUCT_RANGE);
        }
    }

    private void overLappingDate(SpecialProductDto dto) {
        List<SpecialProduct> overlappingProducts = specialProductRepository.findAllOverlappingDates(
                dto.getProductId(),
                dto.getSpecialProductId(),
                dto.getDiscountStartDate(),
                dto.getDiscountEndDate()
        );
        if (!overlappingProducts.isEmpty()) {
            throw new CustomException(CustomErrorCode.OVERLAPPING_SPECIAL_PRODUCT_DISCOUNT);
        }
    }

    private void endDateBeforeStart(SpecialProductDto dto) {
        if (dto.getDiscountEndDate().isBefore(dto.getDiscountStartDate())) {
            throw new CustomException(CustomErrorCode.DISCOUNT_END_DATE_BEFORE_START);
        }
    }
}

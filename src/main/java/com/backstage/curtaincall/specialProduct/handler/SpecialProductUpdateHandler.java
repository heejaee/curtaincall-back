package com.backstage.curtaincall.specialProduct.handler;

import static com.backstage.curtaincall.global.exception.CustomErrorCode.CANNOT_UPDATE_DELETED_SPECIAL_PRODUCT;

import com.backstage.curtaincall.global.exception.CustomException;
import com.backstage.curtaincall.product.entity.Product;
import com.backstage.curtaincall.specialProduct.dto.SpecialProductDto;
import com.backstage.curtaincall.specialProduct.entity.SpecialProduct;
import com.backstage.curtaincall.specialProduct.entity.SpecialProductStatus;
import com.backstage.curtaincall.specialProduct.service.SpecialProductService;
import com.backstage.curtaincall.specialProduct.validator.SpecialProductValidator;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpecialProductUpdateHandler {

    private final SpecialProductService specialProductService;
    private final SpecialProductValidator specialProductValidator;

    // 단건 변경
    @Transactional
    public void update(SpecialProductDto dto) {

        specialProductValidator.validate(dto);
        SpecialProduct sp = specialProductService.findById(dto.getSpecialProductId());

        if (dto.getStatus() == SpecialProductStatus.ACTIVE) {
            // 캐시 반영하여 변경
            specialProductService.updateWithCache(sp,dto);
        } else if (dto.getStatus() == SpecialProductStatus.UPCOMING) {
            // 캐시를 조회하지 않고 변경
            specialProductService.updateWithOutCache(sp, dto);
        } else {
            // 삭제된 특가상품을 변경하는 경우 예외발생
            throw new CustomException(CANNOT_UPDATE_DELETED_SPECIAL_PRODUCT);
        }
    }


    // 상품 변경시 연결된 모든 특가 상품을 업데이트
    @Transactional
    public void updateAllByProduct(Long productId, Product updatedProduct) {
        List<SpecialProduct> specialProducts = specialProductService.findAllByProductId(productId);

        for (SpecialProduct sp : specialProducts) {
            SpecialProductDto updatedDto = sp.toUpdatedDto(updatedProduct);
            specialProductValidator.overDate(updatedDto);

            if (sp.getStatus() == SpecialProductStatus.ACTIVE) {
                // 캐시 반영하여 변경
                specialProductService.updateWithCache(sp, updatedDto);
            } else if (sp.getStatus() == SpecialProductStatus.UPCOMING) {
                // 캐시를 조회하지 않고 변경
                specialProductService.updateWithOutCache(sp, updatedDto);
            }

            //삭제된 특가상품을 가진 상품이 변경될 수 있으므로 예외 처리 X
        }
    }
}

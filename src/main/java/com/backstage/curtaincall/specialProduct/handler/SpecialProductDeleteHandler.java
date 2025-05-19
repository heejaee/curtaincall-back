package com.backstage.curtaincall.specialProduct.handler;

import com.backstage.curtaincall.specialProduct.entity.SpecialProduct;
import com.backstage.curtaincall.specialProduct.entity.SpecialProductStatus;
import com.backstage.curtaincall.specialProduct.service.SpecialProductService;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpecialProductDeleteHandler {

    private final SpecialProductService specialProductService;

    // 단건 삭제
    @Transactional
    public void delete(Long specialProductId) {
        SpecialProduct sp = specialProductService.findById(specialProductId);
        if (sp.getStatus() == SpecialProductStatus.ACTIVE) {
            // 캐시 반영해서 삭제
            specialProductService.deleteWithCache(sp);
        }  else if (sp.getStatus() == SpecialProductStatus.UPCOMING) {
            // 캐시를 조회하지 않고 삭제
            specialProductService.deleteWithOutCache(sp);
        }
    }

    // 상품 삭제시 연결된 모든 특가 상품을 삭제
    @Transactional
    public void deleteAllByProduct(Long productId) {
        List<SpecialProduct> specialProducts = specialProductService.findAllByProductId(productId);
        for (SpecialProduct sp : specialProducts) {
            if (sp.getStatus() == SpecialProductStatus.ACTIVE) {
                // 캐시 반영해서 삭제
                specialProductService.deleteWithCache(sp);
            }
            else if (sp.getStatus() == SpecialProductStatus.UPCOMING) {
                // 캐시를 조회하지 않고 삭제
                specialProductService.deleteWithOutCache(sp);
            }
        }
    }
}

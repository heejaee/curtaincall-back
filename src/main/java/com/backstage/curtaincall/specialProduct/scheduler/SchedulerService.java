package com.backstage.curtaincall.specialProduct.scheduler;

import com.backstage.curtaincall.specialProduct.entity.SpecialProduct;
import com.backstage.curtaincall.specialProduct.handler.SpecialProductDeleteHandler;
import com.backstage.curtaincall.specialProduct.repository.SpecialProductRepository;
import com.backstage.curtaincall.specialProduct.service.SpecialProductService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SchedulerService {

    private final SpecialProductRepository specialProductRepository;
    private final SpecialProductService specialProductService;
    private final SpecialProductDeleteHandler specialProductDeleteHandler;

//    @Transactional
    public void deleteExpiredSpecialProducts() {
        LocalDate today = LocalDate.now();

        // 1. 만료된 특가 상품 조회
        List<Long> expiredSpecialProductIds = specialProductRepository.findExpiredSpecialProductIds(today);

        if (!expiredSpecialProductIds.isEmpty()) {
            // 2. 만료된 상품 삭제
            for (Long spId : expiredSpecialProductIds) {
                try {
                    specialProductDeleteHandler.delete(spId);
                } catch (Exception e) {
                    // 실패한 상품은 넘어가고, 다른 상품은 계속 삭제하도록 처리
                    log.warn("특가 상품 삭제 실패 (ID: {}), 이유: {}", spId, e.getMessage());
                }
            }
        }
    }

//    @Transactional
    public void approveStartingSpecialProducts() {
        LocalDate today = LocalDate.now();
        // 각 productId별 삭제되지 않은 특가상품 중 종료일이 가장 빠른 것이 상태가 할인 예정일때만 가져옴
        List<SpecialProduct> startingProducts = specialProductRepository.findAllStartingSpecialProducts(today);

        for (SpecialProduct sp : startingProducts) {
            try {
                specialProductService.approve(sp.getId());
            } catch (Exception e) {
                // 실패한 상품은 넘어가고, 다른 상품은 계속 승인하도록 처리
                log.warn("특가 상품 승인 실패 (ID: {}), 이유: {}", sp.getId(), e.getMessage());
            }
        }
    }
}

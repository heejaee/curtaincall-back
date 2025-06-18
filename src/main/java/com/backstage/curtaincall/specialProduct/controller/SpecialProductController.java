package com.backstage.curtaincall.specialProduct.controller;

import com.backstage.curtaincall.specialProduct.dto.SpecialProductDto;
import com.backstage.curtaincall.specialProduct.handler.SpecialProductDeleteHandler;
import com.backstage.curtaincall.specialProduct.scheduler.SchedulerService;
import com.backstage.curtaincall.specialProduct.service.SpecialProductService;
import com.backstage.curtaincall.specialProduct.handler.SpecialProductUpdateHandler;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/specialProduct")
@RequiredArgsConstructor
public class SpecialProductController {

    private final SpecialProductService specialProductService;
    private final SpecialProductUpdateHandler specialProductUpdateHandler;
    private final SpecialProductDeleteHandler specialProductDeleteHandler;
    private final SchedulerService schedulerService;


    // 메인화면
    // 캐싱된 특가상품 가져오기
    @GetMapping("/active")
    public List<SpecialProductDto> getActiveSpecialProducts() {
        return specialProductService.getActiveSpecialProducts();
    }

    // 관리자화면
    // 페이지네이션과 이름 검색이 적용된 조회 API
    @GetMapping("/search")
    public Page<SpecialProductDto> getSpecialProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return specialProductService.getSpecialProducts(keyword, page, size);
    }

    // 삭제 된 것만 전체 조회
    @GetMapping("findAllDeleted")
    public List<SpecialProductDto> findAllDeleted(){
        return specialProductService.findAllDeleted();
    }


    // 생성
    @PostMapping
    public SpecialProductDto save(@Valid @RequestBody SpecialProductDto dto) {
        return specialProductService.save(dto);
    }

    // 수정
    @PutMapping
    public void update(@Valid @RequestBody SpecialProductDto dto) {
        specialProductUpdateHandler.update(dto);
    }

    // 삭제 (소프트 삭제)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        specialProductDeleteHandler.delete(id);
    }

    // 승인
    @PutMapping("/approve/{id}")
    public void approve(@PathVariable Long id) {
        specialProductService.approve(id);
    }

    @PutMapping("/approveCancel/{id}")
    public void approveCancel(@PathVariable Long id) {
        specialProductService.approveCancel(id);
    }

    //== 스케줄러 기능 ==
    // 만료된 특가상품 삭제
    @PostMapping("/deleteExpired")
    public void deleteExpiredSpecialProducts() {
        schedulerService.deleteExpiredSpecialProducts();
    }

    // 시작할 특가상품 승인
    @PostMapping("/approveStarting")
    public void approveStartingSpecialProducts() {
        schedulerService.approveStartingSpecialProducts();
    }
}

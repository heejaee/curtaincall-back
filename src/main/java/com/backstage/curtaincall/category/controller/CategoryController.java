package com.backstage.curtaincall.category.controller;

import com.backstage.curtaincall.category.dto.CategoryDto;
import com.backstage.curtaincall.category.service.CategoryService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    // 삭제되지 않은 것만 전체 조회
    @GetMapping
    public List<CategoryDto> findAllNotDeleted() {
        return categoryService.findAllNotDeleted();
    }

    // 삭제된 것만 전체 조회
    @GetMapping("findAllDeleted")
    public List<CategoryDto> findAllDeleted() {
        return categoryService.findAllDeleted();
    }

    // 생성
    @PostMapping
    public CategoryDto create(@Valid @RequestBody CategoryDto categoryDto) {
        return categoryService.save(categoryDto.getName(), categoryDto.getParentId());
    }

    // 수정
    @PutMapping
    public CategoryDto update(@Valid @RequestBody CategoryDto categoryDto) {
        return categoryService.update(categoryDto.getName(), categoryDto.getId());
    }

    // 삭제
    @DeleteMapping("{categoryId}")
    @ResponseStatus(HttpStatus.OK) // 본문 없음, 상태만 반환
    public void delete(@PathVariable("categoryId") Long categoryId) {
        categoryService.delete(categoryId);
    }

    // 복구
    @PutMapping("restore/{categoryId}")
    @ResponseStatus(HttpStatus.OK)
    public void restore(@PathVariable("categoryId") Long categoryId) {
        categoryService.restore(categoryId);
    }
}

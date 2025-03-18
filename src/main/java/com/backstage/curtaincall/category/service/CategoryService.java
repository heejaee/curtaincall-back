package com.backstage.curtaincall.category.service;

import com.backstage.curtaincall.category.domain.Category;
import com.backstage.curtaincall.category.dto.CategoryDto;
import com.backstage.curtaincall.category.repository.CategoryRepository;
import com.backstage.curtaincall.global.exception.CustomErrorCode;
import com.backstage.curtaincall.global.exception.CustomException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
@Transactional
public class CategoryService {
    //
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryDto> findAllNotDeleted() {
        List<Category> categories = categoryRepository.findAllNotDeleted();
        List<CategoryDto> categoryDtos = categories.stream()
                .map(Category::toDto)
                .toList();
        return categoryDtos;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> findAllDeleted() {
        List<Category> categories = categoryRepository.findAllDeleted();
        List<CategoryDto> categoryDtos = categories.stream()
                .map(Category::toDto)
                .toList();
        return categoryDtos;
    }

    public CategoryDto save(String name, Long parentId) {
        // 이스케이프 처리
        String escapedName = StringEscapeUtils.escapeHtml4(name);

        validateCategoryName(escapedName);

        Category category = Category.from(escapedName);

        // 자식 카테고리인 경우 부모 설정
        if (parentId != null) {
            Category parent = categoryRepository.findByIdNotDeleted(parentId)
                    .orElseThrow(() -> new CustomException(CustomErrorCode.CATEGORY_NOT_FOUND));

            // 루트 카테고리가 아닌 곳에서 카테고리를 추가하는 경우 오류 발생
            if (!parent.isRootCategory()) {
                throw new CustomException(CustomErrorCode.INVALID_CATEGORY_OPERATION);
            }

            parent.addChild(category);
        }

        return categoryRepository.save(category).toDto();
    }

    public CategoryDto update(String name, Long id) {
        // 이스케이프 처리
        String escapedName = StringEscapeUtils.escapeHtml4(name);

        validateCategoryName(escapedName);

        Category category = categoryRepository.findByIdNotDeleted(id)
                .orElseThrow(() -> new CustomException(CustomErrorCode.CATEGORY_NOT_FOUND));

        category.updateName(escapedName);
        return categoryRepository.save(category).toDto();
    }

    private void validateCategoryName(String escapedName) {
        if (escapedName == null || escapedName.trim().isEmpty()) {
            throw new CustomException(CustomErrorCode.INVALID_CATEGORY_NAME);
        }

        // 이미 이름이 있다면 중복 예외 발생
        if (categoryRepository.existsByName(escapedName)) {
            throw new CustomException(CustomErrorCode.DUPLICATED_CATEGORY_NAME);
        }
    }

    public void delete(Long id) {
        Category category = categoryRepository.findByIdNotDeleted(id)
                .orElseThrow(() -> new CustomException(CustomErrorCode.CATEGORY_NOT_FOUND));

        // 자신 삭제
        category.delete();

        // 부모 카테고리면 자식 카테고리 삭제
        if (category.isRootCategory()) {
            categoryRepository.softDeleteChildren(id);
        }
    }

    public void restore(Long id) {
        Category category = categoryRepository.findByIdDeleted(id)
                .orElseThrow(() -> new CustomException(CustomErrorCode.CATEGORY_NOT_FOUND));

        category.restore();
        if (category.isRootCategory()) {
            categoryRepository.restoreChildren(id);
        }
    }
}

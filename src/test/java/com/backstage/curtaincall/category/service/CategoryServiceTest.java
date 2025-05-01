package com.backstage.curtaincall.category.service;

import com.backstage.curtaincall.category.domain.Category;
import com.backstage.curtaincall.category.dto.CategoryDto;
import com.backstage.curtaincall.category.repository.CategoryRepository;
import com.backstage.curtaincall.global.exception.CustomErrorCode;
import com.backstage.curtaincall.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @InjectMocks
    private CategoryService service;

    @Mock
    private CategoryRepository repository;

    @Test
    @DisplayName("삭제되지 않은 카테고리 전체 조회")
    void findAllNotDeleted() {
        // Given
        Category c1 = Category.builder().id(1L).name("A").deleted(false).parent(null).build();
        Category c2 = Category.builder().id(2L).name("B").deleted(false).parent(null).build();
        when(repository.findAllNotDeleted()).thenReturn(List.of(c1, c2));

        // When
        List<CategoryDto> dtos = service.findAllNotDeleted();

        // Then
        assertThat(dtos).hasSize(2).extracting(CategoryDto::getName).containsExactly("A", "B");
        verify(repository).findAllNotDeleted();
    }

    @Test
    @DisplayName("삭제된 카테고리 전체 조회")
    void findAllDeleted() {
        // Given
        Category c = Category.builder().id(3L).name("X").deleted(true).build();
        when(repository.findAllDeleted()).thenReturn(List.of(c));

        // When
        List<CategoryDto> dtos = service.findAllDeleted();

        // Then
        assertThat(dtos).hasSize(1).first().extracting(CategoryDto::getName).isEqualTo("X");
        verify(repository).findAllDeleted();
    }

    @Test
    @DisplayName("루트 카테고리 저장 - 성공")
    void save_rootCategory_success() {
        // Given
        String name = "공연";
        when(repository.existsByName(name)).thenReturn(false);
        Category category = Category.builder().id(10L).name(name).deleted(false).build();
        when(repository.save(any(Category.class))).thenReturn(category);

        // When
        CategoryDto dto = service.save(name, null);

        // Then
        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getName()).isEqualTo(name);
        assertThat(dto.isDeleted()).isFalse();
        assertThat(dto.getParentId()).isNull();
        verify(repository).save(any(Category.class));
    }

    @Test
    @DisplayName("카테고리 이름이 공백이면 저장 시 예외 발생")
    void save_blankName_throwsInvalidCategoryName() {
        // When & Then
        CustomException ex = assertThrows(CustomException.class, () -> service.save("   ", null));
        assertThat(ex.getCustomErrorCode()).isEqualTo(CustomErrorCode.INVALID_CATEGORY_NAME);
    }

    @Test
    @DisplayName("중복된 카테고리 이름 저장 시 예외 발생")
    void save_duplicatedName_throwsDuplicatedCategoryName() {
        // Given
        String name = "뮤지컬";
        when(repository.existsByName(name)).thenReturn(true);

        // When & Then
        CustomException ex = assertThrows(CustomException.class, () -> service.save(name, null));
        assertThat(ex.getCustomErrorCode()).isEqualTo(CustomErrorCode.DUPLICATED_CATEGORY_NAME);
    }

    @Test
    @DisplayName("루트가 아닌 부모 카테고리에 추가 시 예외 발생")
    void save_nonRootParent_throwsInvalidCategoryOperation() {
        // Given
        String name = "연극";
        Long parentId = 1L;
        Category parent = mock(Category.class);
        when(repository.existsByName(name)).thenReturn(false);
        when(repository.findByIdNotDeleted(parentId)).thenReturn(Optional.of(parent));
        when(parent.isRootCategory()).thenReturn(false);

        // When & Then
        CustomException ex = assertThrows(CustomException.class, () -> service.save(name, parentId));
        assertThat(ex.getCustomErrorCode()).isEqualTo(CustomErrorCode.INVALID_CATEGORY_OPERATION);
    }

    @Test
    @DisplayName("카테고리 이름 수정 - 성공")
    void update_success() {
        // Given
        Long id = 42L;
        String newName = "뮤지컬";
        Category category = spy(Category.builder().id(id).name("OldName").deleted(false).build());
        when(repository.findByIdNotDeleted(id)).thenReturn(Optional.of(category));
        when(repository.existsByName(newName)).thenReturn(false);
        when(repository.save(category)).thenReturn(category);

        // When
        CategoryDto dto = service.update(newName, id);

        // Then
        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getName()).isEqualTo(newName);
        verify(category).updateName(newName);
        verify(repository).save(category);
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 수정 시 예외 발생")
    void update_notFound_throwsCategoryNotFound() {
        // Given
        when(repository.findByIdNotDeleted(99L)).thenReturn(Optional.empty());

        // When & Then
        CustomException ex = assertThrows(CustomException.class, () -> service.update("A", 99L));
        assertThat(ex.getCustomErrorCode()).isEqualTo(CustomErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    @DisplayName("카테고리 이름 수정 시 중복되면 예외 발생")
    void update_duplicatedName_throwsDuplicatedCategoryName() {
        // Given
        Long id = 5L;
        String name = "중복";
        when(repository.existsByName(name)).thenReturn(true);

        // When & Then
        CustomException ex = assertThrows(CustomException.class, () -> service.update(name, id));
        assertThat(ex.getCustomErrorCode()).isEqualTo(CustomErrorCode.DUPLICATED_CATEGORY_NAME);
    }

    @Test
    @DisplayName("루트 카테고리 삭제 시 자식까지 soft-delete")
    void delete_root_deletesChildren() {
        // Given
        Long id = 7L;
        Category category = mock(Category.class);
        when(repository.findByIdNotDeleted(id)).thenReturn(Optional.of(category));
        when(category.isRootCategory()).thenReturn(true);

        // When
        service.delete(id);

        // Then
        verify(category).delete();
        verify(repository).softDeleteChildren(id);
    }

    @Test
    @DisplayName("자식 카테고리 삭제 시 자신만 soft-delete")
    void delete_nonRoot_onlyDeletesSelf() {
        // Given
        Long id = 8L;
        Category category = mock(Category.class);
        when(repository.findByIdNotDeleted(id)).thenReturn(Optional.of(category));
        when(category.isRootCategory()).thenReturn(false);

        // When
        service.delete(id);

        // Then
        verify(category).delete();
        verify(repository, never()).softDeleteChildren(any());
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 삭제 시 예외 발생")
    void delete_notFound_throwsCategoryNotFound() {
        // Given
        when(repository.findByIdNotDeleted(123L)).thenReturn(Optional.empty());

        // When & Then
        CustomException ex = assertThrows(CustomException.class, () -> service.delete(123L));
        assertThat(ex.getCustomErrorCode()).isEqualTo(CustomErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    @DisplayName("루트 카테고리 복원 시 자식까지 restore")
    void restore_root_restoresChildren() {
        // Given
        Long id = 9L;
        Category category = mock(Category.class);
        when(repository.findByIdDeleted(id)).thenReturn(Optional.of(category));
        when(category.isRootCategory()).thenReturn(true);

        // When
        service.restore(id);

        // Then
        verify(category).restore();
        verify(repository).restoreChildren(id);
    }

    @Test
    @DisplayName("자식 카테고리 복원 시 자신만 restore")
    void restore_nonRoot_onlyRestoresSelf() {
        // Given
        Long id = 10L;
        Category category = mock(Category.class);
        when(repository.findByIdDeleted(id)).thenReturn(Optional.of(category));
        when(category.isRootCategory()).thenReturn(false);

        // When
        service.restore(id);

        // Then
        verify(category).restore();
        verify(repository, never()).restoreChildren(any());
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 복원 시 예외 발생")
    void restore_notFound_throwsCategoryNotFound() {
        // Given
        when(repository.findByIdDeleted(777L)).thenReturn(Optional.empty());

        // When & Then
        CustomException ex = assertThrows(CustomException.class, () -> service.restore(777L));
        assertThat(ex.getCustomErrorCode()).isEqualTo(CustomErrorCode.CATEGORY_NOT_FOUND);
    }
}

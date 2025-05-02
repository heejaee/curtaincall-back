package com.backstage.curtaincall.category.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backstage.curtaincall.category.dto.CategoryDto;
import com.backstage.curtaincall.category.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = CategoryController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    @DisplayName("삭제되지 않은 카테고리 전체 조회")
    void findAllNotDeleted() throws Exception {
        // Given
        CategoryDto dto1 = CategoryDto.builder().id(1L).parentId(null).name("Category1").deleted(false).build();
        CategoryDto dto2 = CategoryDto.builder().id(2L).parentId(1L).name("SubCategory").deleted(false).build();
        given(categoryService.findAllNotDeleted()).willReturn(List.of(dto1, dto2));

        // When & Then
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("Category1")))
                .andExpect(jsonPath("$[1].parentId", is(1)));
    }

    @Test
    @DisplayName("삭제된 카테고리 전체 조회")
    void findAllDeleted_returnsList() throws Exception {
        // Given
        CategoryDto dto = CategoryDto.builder().id(3L).parentId(null).name("DeletedCategory").deleted(true).build();
        given(categoryService.findAllDeleted()).willReturn(List.of(dto));

        // When & Then
        mockMvc.perform(get("/api/v1/categories/findAllDeleted"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].deleted", is(true)));
    }

    @Test
    @DisplayName("카테고리 생성 성공")
    void create_returnsCreatedCategory() throws Exception {
        // Given
        CategoryDto input = new CategoryDto(null, null, "NewCategory", false);
        CategoryDto saved = CategoryDto.builder().id(10L).parentId(null).name("NewCategory").deleted(false).build();
        given(categoryService.save("NewCategory", null)).willReturn(saved);

        // When & Then
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.name", is("NewCategory")));
    }

    @Test
    @DisplayName("카테고리 생성 시 이름 누락으로 유효성 검증 실패")
    void create_withInvalidName_returnsBadRequest() throws Exception {
        // Given
        CategoryDto invalidDto = new CategoryDto(null, null, "", false);

        // When & Then
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("카테고리 수정 성공")
    void update_returnsUpdatedCategory() throws Exception {
        // Given
        CategoryDto input = new CategoryDto(10L, null, "UpdatedName", false);
        CategoryDto updated = CategoryDto.builder().id(10L).parentId(null).name("UpdatedName").deleted(false).build();
        given(categoryService.update("UpdatedName", 10L)).willReturn(updated);

        // When & Then
        mockMvc.perform(put("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("UpdatedName")));
    }

    @Test
    @DisplayName("카테고리 삭제 호출")
    void delete_returnsOk() throws Exception {
        // Given
        willDoNothing().given(categoryService).delete(5L);

        // When & Then
        mockMvc.perform(delete("/api/v1/categories/{categoryId}", 5L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("카테고리 복구 호출")
    void restore_returnsOk() throws Exception {
        // Given
        willDoNothing().given(categoryService).restore(7L);

        // When & Then
        mockMvc.perform(put("/api/v1/categories/restore/{categoryId}", 7L))
                .andExpect(status().isOk());
    }

}

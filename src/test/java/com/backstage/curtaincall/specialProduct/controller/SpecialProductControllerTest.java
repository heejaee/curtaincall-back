package com.backstage.curtaincall.specialProduct.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backstage.curtaincall.specialProduct.dto.SpecialProductDto;
import com.backstage.curtaincall.specialProduct.handler.SpecialProductDeleteHandler;
import com.backstage.curtaincall.specialProduct.handler.SpecialProductUpdateHandler;
import com.backstage.curtaincall.specialProduct.scheduler.SchedulerService;
import com.backstage.curtaincall.specialProduct.service.SpecialProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = SpecialProductController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
class SpecialProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SpecialProductService specialProductService;

    @MockBean
    private SpecialProductUpdateHandler specialProductUpdateHandler;

    @MockBean
    private SpecialProductDeleteHandler specialProductDeleteHandler;

    @MockBean
    private SchedulerService schedulerService;

    @MockBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("활성 특가상품 조회")
    void getActiveSpecialProducts() throws Exception {
        // Given
        SpecialProductDto dto1 = SpecialProductDto.builder()
                .specialProductId(1L)
                .productId(100L)
                .discountRate(10)
                .discountStartDate(LocalDate.of(2025,7,1))
                .discountEndDate(LocalDate.of(2025,7,10))
                .expiring(false)
                .build();
        SpecialProductDto dto2 = SpecialProductDto.builder()
                .specialProductId(2L)
                .productId(101L)
                .discountRate(20)
                .discountStartDate(LocalDate.of(2025,7,5))
                .discountEndDate(LocalDate.of(2025,7,15))
                .expiring(true)
                .build();
        given(specialProductService.getActiveSpecialProducts())
                .willReturn(List.of(dto1, dto2));

        // When & Then
        mockMvc.perform(get("/api/v1/specialProduct/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].specialProductId", is(1)))
                .andExpect(jsonPath("$[1].expiring", is(true)));
    }

    @Test
    @DisplayName("관리자용 특가상품 검색 및 페이지 조회")
    void searchSpecialProducts() throws Exception {
        // Given
        SpecialProductDto dto1 = SpecialProductDto.builder().specialProductId(3L).build();
        SpecialProductDto dto2 = SpecialProductDto.builder().specialProductId(4L).build();
        Page<SpecialProductDto> page = new PageImpl<>(
                List.of(dto1, dto2), PageRequest.of(0, 10), 2
        );
        given(specialProductService.getSpecialProducts("test", 0, 10))
                .willReturn(page);

        // When & Then
        mockMvc.perform(get("/api/v1/specialProduct/search")
                        .param("keyword", "test")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page.totalElements", is(2))); // ← 여기 수정

    }

    @Test
    @DisplayName("삭제된 특가상품 전체 조회")
    void findAllDeletedSpecialProducts() throws Exception {
        // Given
        SpecialProductDto dto = SpecialProductDto.builder()
                .specialProductId(5L).build();
        given(specialProductService.findAllDeleted())
                .willReturn(List.of(dto));

        // When & Then
        mockMvc.perform(get("/api/v1/specialProduct/findAllDeleted"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].specialProductId", is(5)));
    }

    @Test
    @DisplayName("특가상품 생성 성공")
    void createSpecialProduct() throws Exception {
        // Given
        SpecialProductDto input = SpecialProductDto.builder()
                .productId(200L)
                .discountRate(15)
                .discountStartDate(LocalDate.of(2025,7,12))
                .discountEndDate(LocalDate.of(2025,7,20))
                .build();
        SpecialProductDto saved = SpecialProductDto.builder()
                .specialProductId(10L)
                .productId(200L)
                .discountRate(15)
                .discountStartDate(LocalDate.of(2025,7,12))
                .discountEndDate(LocalDate.of(2025,7,20))
                .expiring(false)
                .build();
        given(specialProductService.save(refEq(input))).willReturn(saved);

        // When & Then
        mockMvc.perform(post("/api/v1/specialProduct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialProductId", is(10)))
                .andExpect(jsonPath("$.productId", is(200)));
    }

    //여기부터
    @Test
    @DisplayName("특가상품 수정 호출")
    void updateSpecialProduct() throws Exception {
        // Given
        SpecialProductDto dto = SpecialProductDto.builder()
                .specialProductId(10L)
                .discountRate(25)
                .build();
        willDoNothing().given(specialProductUpdateHandler).update(dto);

        // When & Then
        mockMvc.perform(put("/api/v1/specialProduct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("특가상품 삭제 호출")
    void deleteSpecialProduct() throws Exception {
        // Given
        willDoNothing().given(specialProductDeleteHandler).delete(7L);

        // When & Then
        mockMvc.perform(delete("/api/v1/specialProduct/{id}", 7L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("특가상품 승인 호출")
    void approveSpecialProduct() throws Exception {
        // Given
        SpecialProductDto result = SpecialProductDto.builder()
                .specialProductId(8L)
                .productId(200L)
                .discountRate(10)
                .build();
        given(specialProductService.approve(8L)).willReturn(result);

        // When & Then
        mockMvc.perform(put("/api/v1/specialProduct/approve/{id}", 8L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("특가상품 승인 취소 호출")
    void approveCancelSpecialProduct() throws Exception {
        // Given
        SpecialProductDto result = SpecialProductDto.builder()
                .specialProductId(9L)
                .productId(200L)
                .discountRate(10)
                .build();

        given(specialProductService.approveCancel(9L)).willReturn(result);

        // When & Then
        mockMvc.perform(put("/api/v1/specialProduct/approveCancel/{id}", 9L))
                .andExpect(status().isOk());
    }


    @Test
    @DisplayName("스케줄러: 만료된 특가상품 삭제 호출")
    void deleteExpiredScheduler() throws Exception {
        // Given
        willDoNothing().given(schedulerService).deleteExpiredSpecialProducts();

        // When & Then
        mockMvc.perform(post("/api/v1/specialProduct/deleteExpired"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("스케줄러: 시작할 특가상품 승인 호출")
    void approveStartingScheduler() throws Exception {
        // Given
        willDoNothing().given(schedulerService).approveStartingSpecialProducts();

        // When & Then
        mockMvc.perform(post("/api/v1/specialProduct/approveStarting"))
                .andExpect(status().isOk());
    }
}

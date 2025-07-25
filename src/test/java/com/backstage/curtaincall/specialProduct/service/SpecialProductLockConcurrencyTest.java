package com.backstage.curtaincall.specialProduct.service;

import com.backstage.curtaincall.category.domain.Category;
import com.backstage.curtaincall.category.repository.CategoryRepository;
import com.backstage.curtaincall.product.entity.Product;
import com.backstage.curtaincall.product.entity.ProductImage;
import com.backstage.curtaincall.product.repository.ProductImageRepository;
import com.backstage.curtaincall.product.repository.ProductRepository;
import com.backstage.curtaincall.specialProduct.dto.SpecialProductDto;
import com.backstage.curtaincall.specialProduct.entity.SpecialProduct;
import com.backstage.curtaincall.specialProduct.repository.SpecialProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=dummy:9092",
        "spring.kafka.listener.auto-startup=false"
})
public class SpecialProductLockConcurrencyTest {

    @MockBean
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    private SpecialProductService specialProductService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private SpecialProductRepository specialProductRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    @DisplayName("특가상품 저장 성공")
    void saveSpecialProduct() {
        // 카테고리 저장
        Category category = categoryRepository.save(Category.builder()
                .name("연극")
                .build());

        // 상품 저장
        Product product = productRepository.save(Product.builder()
                .productName("테스트 상품")
                .category(category)
                .place("테스트 장소")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .runningTime(120)
                .price(10000)
                .casting("출연진")
                .notice("공지사항")
                .build());

        // 이미지 저장 및 연관관계 설정
        ProductImage image = productImageRepository.save(ProductImage.builder()
                .imageUrl("http://test.com/test.jpg")
                .product(product)
                .build());

        product.updateImage(image);  // 연관관계 설정
        productRepository.save(product); // product에 image 반영

        // DTO 생성
        SpecialProductDto baseDto = SpecialProductDto.of(product);
        SpecialProductDto dto = SpecialProductDto.builder()
                .productId(baseDto.getProductId())
                .productName(baseDto.getProductName())
                .price(baseDto.getPrice())
                .startDate(baseDto.getStartDate())
                .endDate(baseDto.getEndDate())
                .place(baseDto.getPlace())
                .runningTime(baseDto.getRunningTime())
                .casting(baseDto.getCasting())
                .notice(baseDto.getNotice())
                .imageUrl(baseDto.getImageUrl())
                .discountRate(20)
                .discountStartDate(LocalDate.now().plusDays(1))
                .discountEndDate(LocalDate.now().plusDays(3))
                .build();

        // when
        SpecialProductDto saved = specialProductService.save(dto);
        entityManager.flush();
        entityManager.clear();

        // then
        SpecialProduct result = specialProductRepository.findById(saved.getSpecialProductId())
                .orElseThrow();
        assertThat(result.getDiscountRate()).isEqualTo(20);
        assertThat(result.getProduct().getProductImage().getImageUrl()).isEqualTo("http://test.com/test.jpg");
    }
}

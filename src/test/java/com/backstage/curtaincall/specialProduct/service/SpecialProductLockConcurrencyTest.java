package com.backstage.curtaincall.specialProduct.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.backstage.curtaincall.category.domain.Category;
import com.backstage.curtaincall.category.repository.CategoryRepository;
import com.backstage.curtaincall.product.entity.Product;
import com.backstage.curtaincall.product.entity.ProductImage;
import com.backstage.curtaincall.product.repository.ProductImageRepository;
import com.backstage.curtaincall.product.repository.ProductRepository;
import com.backstage.curtaincall.specialProduct.dto.SpecialProductDto;
import com.backstage.curtaincall.specialProduct.entity.SpecialProduct;
import com.backstage.curtaincall.specialProduct.entity.SpecialProductStatus;
import com.backstage.curtaincall.specialProduct.repository.SpecialProductRepository;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

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

    private SpecialProductDto dto;


    @BeforeEach
    void setUp() {
        Category category = categoryRepository.save(Category.builder()
                .name("연극")
                .build());

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

        dto = SpecialProductDto.builder()
                .productId(product.getProductId())
                .discountRate(20)
                .discountStartDate(LocalDate.now().plusDays(1))
                .discountEndDate(LocalDate.now().plusDays(3))
                .build();

        // DTO 생성
        SpecialProductDto baseDto = SpecialProductDto.of(product);
        dto = baseDto.toBuilder()
                .discountRate(20)
                .discountStartDate(LocalDate.now().plusDays(1))
                .discountEndDate(LocalDate.now().plusDays(3))
                .build();

    }

    @Test
    @DisplayName("동시에 여러 쓰레드가 updateWithOutCache를 호출하면 모두 동시에 성공하지는 않아야 한다")
    void concurrencyTest_updateWithOutCache() throws InterruptedException {
        // given
        SpecialProductDto savedDto = specialProductService.save(dto);
        SpecialProduct sp = specialProductRepository.findById(savedDto.getSpecialProductId()).orElseThrow();
        // 테스트시  updateWithOutCache()에 Thread.sleep(8000)을 추가해야합니다!

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();

        // when
        for (int i = 0; i < threadCount; i++) {
            int threadNum = i;
            executor.submit(() -> {
                latch.countDown();
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                try {
                    SpecialProductDto updateDto = savedDto.toBuilder()
                            .notice("공지사항 업데이트 - " + threadNum)
                            .status(SpecialProductStatus.UPCOMING)
                            .build();
                    specialProductService.updateWithOutCache(sp, updateDto);
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // then
        assertThat(successCount.get())
                .as("동시 update 시 분산 락이 동작하여 모두 동시에 성공하면 안 됨")
                .isLessThan(threadCount);
    }
}

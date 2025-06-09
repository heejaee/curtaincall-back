package com.backstage.curtaincall.specialProduct.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.backstage.curtaincall.category.domain.Category;
import com.backstage.curtaincall.category.repository.CategoryRepository;
import com.backstage.curtaincall.product.entity.Product;
import com.backstage.curtaincall.product.repository.ProductRepository;
import com.backstage.curtaincall.specialProduct.dto.SpecialProductDto;
import com.backstage.curtaincall.specialProduct.entity.SpecialProduct;
import com.backstage.curtaincall.specialProduct.entity.SpecialProductStatus;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import(SpecialProductRepository.class)
@ActiveProfiles("test")
public class SpecialProductRepositoryTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private SpecialProductRepository specialProductRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;


    private Category category;
    private Product product;

    @BeforeEach
    void setUp() {
        // 1) 테스트용 Category 저장
        Category category = Category.from("테스트 카테고리");
        this.category = categoryRepository.save(category);

        // 2) 테스트용 Product 저장 (Category 필수)
        Product product = Product.builder()
                .productName("테스트 상품")
                .category(this.category)
                .place("테스트 장소")
                .runningTime(120)
                .price(30000)
                .casting("테스트 캐스팅")
                .notice("테스트 공지")
                .build();
        this.product = productRepository.save(product);

        // 영속성 컨텍스트 초기화
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("ACTIVE 상태의 특가상품만 전부 조회")
    void findAllActive() {
        // given: ACTIVE 상태 2개, DELETED 상태 1개 저장
        SpecialProduct activeSp = SpecialProduct.builder()
                .product(product)
                .discountRate(10)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .status(SpecialProductStatus.ACTIVE)
                .build();

        SpecialProduct activeSp2 = SpecialProduct.builder()
                .product(product)
                .discountRate(20)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .status(SpecialProductStatus.ACTIVE)
                .build();

        SpecialProduct deletedSp = SpecialProduct.builder()
                .product(product)
                .discountRate(20)
                .startDate(LocalDate.now().minusDays(2))
                .endDate(LocalDate.now().minusDays(1))
                .status(SpecialProductStatus.DELETED)
                .build();

        em.persist(activeSp);
        em.persist(activeSp2);
        em.persist(deletedSp);
        em.flush();
        em.clear();

        // when
        List<SpecialProduct> result = specialProductRepository.findAllActive();

        // then: ACTIVE 상태만 반환
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStatus()).isEqualTo(SpecialProductStatus.ACTIVE);
        assertThat(result.get(1).getStatus()).isEqualTo(SpecialProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("키워드와 페이지네이션을 적용한 조회")
    void findAll() {
        // given: 서로 다른 productName을 가진 Product 세 건 저장하여 SpecialProduct 연결
        Product productA = Product.builder()
                .productName("productA")
                .category(category)
                .place("장소A")
                .runningTime(90)
                .price(20000)
                .casting("캐스팅A")
                .notice("공지A")
                .build();
        productA = productRepository.save(productA);

        Product productB = Product.builder()
                .productName("productB")
                .category(category)
                .place("장소B")
                .runningTime(100)
                .price(25000)
                .casting("캐스팅B")
                .notice("공지B")
                .build();
        productB = productRepository.save(productB);

        Product alphaP = Product.builder()
                .productName("alphaP")
                .category(category)
                .place("장소C")
                .runningTime(110)
                .price(30000)
                .casting("캐스팅C")
                .notice("공지C")
                .build();
        alphaP = productRepository.save(alphaP);

        em.persist(SpecialProduct.builder()
                .product(productA)
                .discountRate(5)
                .startDate(LocalDate.now().minusDays(2))
                .endDate(LocalDate.now().plusDays(2))
                .status(SpecialProductStatus.ACTIVE)
                .build());

        em.persist(SpecialProduct.builder()
                .product(productB)
                .discountRate(10)
                .startDate(LocalDate.now().minusDays(4))
                .endDate(LocalDate.now().plusDays(4))
                .status(SpecialProductStatus.ACTIVE)
                .build());

        em.persist(SpecialProduct.builder()
                .product(alphaP)
                .discountRate(20)
                .startDate(LocalDate.now().minusDays(6))
                .endDate(LocalDate.now().plusDays(6))
                .status(SpecialProductStatus.ACTIVE)
                .build());

        em.flush();
        em.clear();

        // when: "product" 키워드, 페이지 크기 2, 0번 페이지
        Pageable pageable = PageRequest.of(0, 2);
        Page<SpecialProduct> page = specialProductRepository.findAll("product", pageable);

        // then: 총 2개 (productA → productB 순)
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(sp -> sp.getProduct().getProductName())
                .containsExactly("productA", "productB");
    }

    @Test
    @DisplayName("DELETED 상태의 특가상품만 전부 조회")
    void findAllDeleted() {
        // given: DELETED 상태 SpecialProduct 두 건 저장
        SpecialProduct sp1 = SpecialProduct.builder()
                .product(product)
                .discountRate(30)
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().minusDays(3))
                .status(SpecialProductStatus.DELETED)
                .build();

        SpecialProduct sp2 = SpecialProduct.builder()
                .product(product)
                .discountRate(40)
                .startDate(LocalDate.now().minusDays(4))
                .endDate(LocalDate.now().minusDays(2))
                .status(SpecialProductStatus.DELETED)
                .build();

        em.persist(sp1);
        em.persist(sp2);
        em.flush();
        em.clear();

        // when
        List<SpecialProduct> deletedList = specialProductRepository.findAllDeleted();

        // then: 둘 다 DELETED 상태
        assertThat(deletedList).hasSize(2)
                .allMatch(sp -> sp.getStatus() == SpecialProductStatus.DELETED);
    }

    @Test
    @DisplayName("productId로 조회 시 DELETED 제외하고 반환")
    void findAllByProductId() {
        // given: 같은 productId로 두 건 저장 (하나는 DELETED, 하나는 UPCOMING)
        SpecialProduct sp1 = SpecialProduct.builder()
                .product(product)
                .discountRate(5)
                .startDate(LocalDate.now().minusDays(3))
                .endDate(LocalDate.now().minusDays(1))
                .status(SpecialProductStatus.DELETED)
                .build();

        SpecialProduct sp2 = SpecialProduct.builder()
                .product(product)
                .discountRate(35)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .status(SpecialProductStatus.UPCOMING)
                .build();

        em.persist(sp1);
        em.persist(sp2);
        em.flush();
        em.clear();

        // when
        List<SpecialProduct> specialProducts = specialProductRepository.findAllByProductId(product.getProductId());

        // then: DELETED가 아닌 것만 반환 (sp2)
        assertThat(specialProducts).hasSize(1)
                .first()
                .extracting(sp -> sp.getStatus())
                .isEqualTo(SpecialProductStatus.UPCOMING);
    }

    @Test
    @DisplayName("특정 기간이 기존 특가상품과 겹치는지 확인")
    void findAllOverlappingDates() {
        // given: 한 건 저장 (2025-06-01 ~ 2025-06-05, UPCOMING)
        SpecialProduct specialProduct = SpecialProduct.builder()
                .product(product)
                .discountRate(25)
                .startDate(LocalDate.of(2025, 6, 1))
                .endDate(LocalDate.of(2025, 6, 5))
                .status(SpecialProductStatus.UPCOMING)
                .build();

        em.persist(specialProduct);
        em.flush();
        em.clear();

        // when: 겹침 (2025-06-04 ~ 2025-06-07)
        List<SpecialProduct> overlaps = specialProductRepository.findAllOverlappingDates(
                product.getProductId(),
                null,
                LocalDate.of(2025, 6, 4),
                LocalDate.of(2025, 6, 7)
        );

        // then: 한 건 반환
        assertThat(overlaps).hasSize(1)
                .first()
                .extracting(SpecialProduct::getId)
                .isEqualTo(specialProduct.getId());
    }

    @Test
    @DisplayName("특정 ID의 UPCOMING 상태 특가상품 조회")
    void findByIdUpcoming() {
        // given
        SpecialProduct upcomingSp = SpecialProduct.builder()
                .product(product)
                .discountRate(15)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .status(SpecialProductStatus.UPCOMING)
                .build();
        em.persist(upcomingSp);
        em.flush();
        em.clear();

        // when
        Optional<SpecialProduct> result = specialProductRepository.findByIdUpcoming(upcomingSp.getId());

        // then
        assertThat(result).isPresent()
                .get()
                .extracting(SpecialProduct::getStatus)
                .isEqualTo(SpecialProductStatus.UPCOMING);
    }

    @Test
    @DisplayName("특정 ID의 ACTIVE 상태 특가상품 조회")
    void findByIdActive() {
        // given
        SpecialProduct activeSp = SpecialProduct.builder()
                .product(product)
                .discountRate(25)
                .startDate(LocalDate.now().minusDays(2))
                .endDate(LocalDate.now().plusDays(2))
                .status(SpecialProductStatus.ACTIVE)
                .build();
        em.persist(activeSp);
        em.flush();
        em.clear();

        // when
        Optional<SpecialProduct> result = specialProductRepository.findByIdActive(activeSp.getId());

        // then
        assertThat(result).isPresent()
                .get()
                .extracting(SpecialProduct::getStatus)
                .isEqualTo(SpecialProductStatus.ACTIVE);
    }


    @Test
    @DisplayName("각 상품별 삭제되지 않은 특가상품 중 종료일이 가장 빠른 것이 상태가 할인 예정일때 조회 성공")
    void findAllStartingSpecialProducts_success() {
        // given
        SpecialProduct spEarly = SpecialProduct.builder()
                .product(product)
                .discountRate(10)
                .startDate(LocalDate.of(2025, 6, 5))
                .endDate(LocalDate.of(2025, 6, 10))
                .status(SpecialProductStatus.UPCOMING)
                .build();

        SpecialProduct spLater = SpecialProduct.builder()
                .product(product)
                .discountRate(20)
                .startDate(LocalDate.of(2025, 6, 6))
                .endDate(LocalDate.of(2025, 6, 15))
                .status(SpecialProductStatus.UPCOMING)
                .build();

        em.persist(spEarly);
        em.persist(spLater);
        em.flush();
        em.clear();

        // when
        LocalDate today = LocalDate.of(2025, 6, 9);
        List<SpecialProduct> result = specialProductRepository.findAllStartingSpecialProducts(today);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(spEarly.getId());
    }

    @Test
    @DisplayName("각 상품별 삭제되지 않은 특가상품 중 종료일이 가장 빠른 것이 상태가 할인 예정이 아닐때 조회 실패")
    void findAllStartingSpecialProducts_fail() {
        // given
        SpecialProduct activeSp = SpecialProduct.builder()
                .product(product)
                .discountRate(30)
                .startDate(LocalDate.of(2025, 6, 1))
                .endDate(LocalDate.of(2025, 6, 5))
                .status(SpecialProductStatus.ACTIVE)
                .build();

        SpecialProduct upcomingSp = SpecialProduct.builder()
                .product(product)
                .discountRate(15)
                .startDate(LocalDate.of(2025, 6, 6))
                .endDate(LocalDate.of(2025, 6, 10))
                .status(SpecialProductStatus.UPCOMING)
                .build();

        em.persist(activeSp);
        em.persist(upcomingSp);
        em.flush();
        em.clear();

        // when
        LocalDate today = LocalDate.of(2025, 6, 4);
        List<SpecialProduct> result = specialProductRepository.findAllStartingSpecialProducts(today);

        // then
        assertThat(result).isEmpty();
    }


    @Test
    @DisplayName("해당 상품에 ACTIVE 상태의 특가상품이 존재하는지 확인")
    void existsByProductIdAndStatus() {
        // given: ACTIVE 상태 한 건 저장
        SpecialProduct activeSp = SpecialProduct.builder()
                .product(product)
                .discountRate(18)
                .startDate(LocalDate.now().minusDays(2))
                .endDate(LocalDate.now().plusDays(2))
                .status(SpecialProductStatus.ACTIVE)
                .build();

        em.persist(activeSp);
        em.flush();
        em.clear();

        // when
        boolean exists = specialProductRepository.existsByProductIdAndStatus(product.getProductId());

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("할인 종료일이 오늘보다 이전인 특가상품 ID 조회")
    void findExpiredSpecialProductIds() {
        // given: 두 건 저장 (하나는 만료, 하나는 만료 아님)
        SpecialProduct expired = SpecialProduct.builder()
                .product(product)
                .discountRate(12)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().minusDays(5))
                .status(SpecialProductStatus.ACTIVE)
                .build();

        SpecialProduct notExpired = SpecialProduct.builder()
                .product(product)
                .discountRate(22)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .status(SpecialProductStatus.ACTIVE)
                .build();

        em.persist(expired);
        em.persist(notExpired);
        em.flush();
        em.clear();

        // when: today = LocalDate.now()
        LocalDate today = LocalDate.now();
        List<Long> expiredIds = specialProductRepository.findExpiredSpecialProductIds(today);

        // then: 만료된 ID만 포함
        assertThat(expiredIds).containsExactly(expired.getId());
    }



    // ===== JDBC 기반 메서드 Tests =====

    @Test
    @DisplayName("JdbcTemplate을 통한 특가상품 저장")
    void save() {
        // given: JPA로 저장된 개수 조회
        Long beforeCount = em.createQuery("SELECT COUNT(sp) FROM SpecialProduct sp", Long.class)
                .getSingleResult();

        // when: JdbcTemplate 기반 save 호출
        SpecialProductDto spDto = SpecialProductDto.builder()
                .productId(product.getProductId())
                .discountRate(50)
                .discountStartDate(LocalDate.of(2025, 7, 1))
                .discountEndDate(LocalDate.of(2025, 7, 5))
                .status(SpecialProductStatus.UPCOMING)
                .build();

        SpecialProduct sp = SpecialProduct.of(product, spDto);
        specialProductRepository.save(sp);
        em.flush();
        em.clear();

        // then: 개수 증가 확인
        Long afterCount = em.createQuery("SELECT COUNT(sp) FROM SpecialProduct sp", Long.class)
                .getSingleResult();
        assertThat(afterCount).isEqualTo(beforeCount + 1);
    }

    @Test
    @DisplayName("JdbcTemplate을 통한 특가상품 수정")
    void update() {
        // given: 먼저 JPA로 한 건 저장
        SpecialProduct sp = SpecialProduct.builder()
                .product(product)
                .discountRate(30)
                .startDate(LocalDate.of(2025, 8, 1))
                .endDate(LocalDate.of(2025, 8, 10))
                .status(SpecialProductStatus.UPCOMING)
                .build();
        em.persist(sp);
        em.flush();
        Long targetId = sp.getId();
        em.clear();

        // when: DTO로 업데이트 후 JdbcTemplate.update 호출
        SpecialProductDto dto = SpecialProductDto.builder()
                .specialProductId(targetId)
                .discountRate(80)
                .discountStartDate(LocalDate.of(2025, 8, 5))
                .discountEndDate(LocalDate.of(2025, 8, 15))
                .status(SpecialProductStatus.ACTIVE)
                .build();

        specialProductRepository.update(dto);

        // then: DB에서 읽어와 필드가 반영되었는지 확인
        SpecialProduct updated = em.find(SpecialProduct.class, targetId);
        assertThat(updated.getDiscountRate()).isEqualTo(80);
        assertThat(updated.getStartDate()).isEqualTo(LocalDate.of(2025, 8, 5));
        assertThat(updated.getEndDate()).isEqualTo(LocalDate.of(2025, 8, 15));
        assertThat(updated.getStatus()).isEqualTo(SpecialProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("JdbcTemplate을 통한 특가상품 soft삭제 → DELETED로 변경")
    void delete() {
        // given: JPA로 한 건 저장
        SpecialProduct sp = SpecialProduct.builder()
                .product(product)
                .discountRate(22)
                .startDate(LocalDate.now().minusDays(3))
                .endDate(LocalDate.now().plusDays(3))
                .status(SpecialProductStatus.UPCOMING)
                .build();
        em.persist(sp);
        em.flush();
        Long targetId = sp.getId();
        em.clear();

        // when: JdbcTemplate.delete 호출
        SpecialProduct toDelete = em.find(SpecialProduct.class, targetId);
        specialProductRepository.delete(toDelete);
        em.flush();
        em.clear();

        // then: 상태가 DELETED로 변경됨
        SpecialProduct deleted = em.find(SpecialProduct.class, targetId);
        assertThat(deleted.getStatus()).isEqualTo(SpecialProductStatus.DELETED);
    }

    @Test
    @DisplayName("JdbcTemplate으로 특가상품 상태를 ACTIVE로 변경")
    void approve() {
        // given: UPCOMING 상태의 특가상품 저장
        SpecialProduct sp = SpecialProduct.builder()
                .product(product)
                .discountRate(20)
                .startDate(LocalDate.of(2025, 9, 1))
                .endDate(LocalDate.of(2025, 9, 10))
                .status(SpecialProductStatus.UPCOMING)
                .build();
        em.persist(sp);
        em.flush();
        Long id = sp.getId();
        em.clear();

        // when
        SpecialProduct target = em.find(SpecialProduct.class, id);
        specialProductRepository.approve(target);
        em.clear();

        // then: 상태가 ACTIVE로 변경되었는지 확인
        SpecialProduct updated = em.find(SpecialProduct.class, id);
        assertThat(updated.getStatus()).isEqualTo(SpecialProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("JdbcTemplate으로 특가상품 상태를 UPCOMING으로 되돌림")
    void approveCancel() {
        // given: ACTIVE 상태의 특가상품 저장
        SpecialProduct sp = SpecialProduct.builder()
                .product(product)
                .discountRate(20)
                .startDate(LocalDate.of(2025, 9, 1))
                .endDate(LocalDate.of(2025, 9, 10))
                .status(SpecialProductStatus.ACTIVE)
                .build();
        em.persist(sp);
        em.flush();
        Long id = sp.getId();
        em.clear();

        // when
        SpecialProduct target = em.find(SpecialProduct.class, id);
        specialProductRepository.approveCancel(target);
        em.clear();

        // then: 상태가 UPCOMING으로 변경되었는지 확인
        SpecialProduct updated = em.find(SpecialProduct.class, id);
        assertThat(updated.getStatus()).isEqualTo(SpecialProductStatus.UPCOMING);
    }
}

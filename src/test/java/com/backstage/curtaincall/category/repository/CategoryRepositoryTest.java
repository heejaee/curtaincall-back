package com.backstage.curtaincall.category.repository;

import com.backstage.curtaincall.category.domain.Category;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Slf4j
@ActiveProfiles("test")
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("삭제되지 않은 Category 전체 조회")
    void findAllNotDeleted() {
        // given
        Category category1 = Category.builder().name("Book").deleted(false).build();
        Category category2 = Category.builder().name("Movie").deleted(true).build(); // deleted
        categoryRepository.save(category1);
        categoryRepository.save(category2);

        // when
        List<Category> categories = categoryRepository.findAllNotDeleted();

        // then
        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).getName()).isEqualTo("Book");
    }

    @Test
    @DisplayName("삭제되지 않은 Category 단건 조회")
    void findByIdNotDeleted() {
        // given
        Category category = Category.builder().name("Music").deleted(false).build();
        Category saved = categoryRepository.save(category);

        // when
        Optional<Category> found = categoryRepository.findByIdNotDeleted(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Music");
    }

    @Test
    @DisplayName("삭제된 Category 전체 조회")
    void findAllDeleted() {
        // given
        Category category = Category.builder().name("Drama").deleted(true).build();
        categoryRepository.save(category);

        // when
        List<Category> categories = categoryRepository.findAllDeleted();

        // then
        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).getName()).isEqualTo("Drama");
    }

    @Test
    @DisplayName("삭제된 Category 단건 조회")
    void findByIdDeleted() {
        // given
        Category category = Category.builder().name("Comedy").deleted(true).build();
        Category saved = categoryRepository.save(category);

        // when
        Optional<Category> found = categoryRepository.findByIdDeleted(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Comedy");
    }

//    @Test
//    @DisplayName("Category 이름으로 존재 여부 확인")
//    void existsByName() {
//        // given
//        Category category = Category.builder().name("Travel").deleted(false).build();
//        categoryRepository.save(category);
//
//        long startTime = System.currentTimeMillis(); // 시간 측정 시작
//
//        // when
//        boolean exists = categoryRepository.existsByName("Travel");
//
//        long endTime = System.currentTimeMillis(); // 시간 측정 끝
//
//        // then
//        assertThat(exists).isTrue();
//
//        log.info("existsByName() 실행 시간: {}ms", (endTime - startTime)); // 로그 출력
//    }
//
//    @Test
//    @DisplayName("Category 이름으로 존재 여부 확인")
//    void existsByName2() {
//        // given
//        Category category = Category.builder().name("Travel").deleted(false).build();
//        categoryRepository.save(category);
//
//        long startTime = System.currentTimeMillis(); // 시간 측정 시작
//
//        // when
//        boolean exists = categoryRepository.existsByName2("Travel");
//
//        long endTime = System.currentTimeMillis(); // 시간 측정 끝
//
//        // then
//        assertThat(exists).isTrue();
//
//        log.info("existsByName() 실행 시간: {}ms", (endTime - startTime)); // 로그 출력
//    }

    @Test
    @DisplayName("Category 이름으로 존재 여부 비교 테스트")
    void existsByNameComparison() {
        // given
        // 5만개 데이터 삽입
//        List<Category> categoryList = new ArrayList<>();
//        for (int i = 0; i < 50_000; i++) {
//            categoryList.add(Category.builder()
//                    .name("Category_" + i)
//                    .deleted(false)
//                    .build());
//        }
//        categoryRepository.saveAll(categoryList);
//        entityManager.flush();
//        entityManager.clear();
//
//        // 추가로 "Travel" 하나 저장
//        Category category = Category.builder()
//                .name("Travel")
//                .deleted(false)
//                .build();
//        categoryRepository.save(category);
//
//        entityManager.flush();
//        entityManager.clear();
//
//        // existsByName() 시간 측정
//        long start1 = System.currentTimeMillis();
//        boolean exists1 = categoryRepository.existsByName("Travel");
//        long end1 = System.currentTimeMillis();
//        log.info("existsByName (count(*) 기반) 실행 시간: {}ms", (end1 - start1));
//        assertThat(exists1).isTrue();
//
//        // existsByName2() 시간 측정
//        long start2 = System.currentTimeMillis();
//        boolean exists2 = categoryRepository.existsByName2("Travel");
//        long end2 = System.currentTimeMillis();
//        log.info("existsByName2 (SELECT EXISTS 기반) 실행 시간: {}ms", (end2 - start2));
//        assertThat(exists2).isTrue();
    }


    @Test
    @DisplayName("Category 이름으로 존재하지 않는 경우 확인")
    void notExistsByName() {
        // when
        boolean exists = categoryRepository.existsByName("NonExisting");

        // then
        assertThat(exists).isFalse();
    }

//    @Test
//    @DisplayName("부모 ID로 하위 Category soft delete")
//    void softDeleteChildren() {
//        // given
//        Category parent = Category.builder().name("Parent").deleted(false).build();
//        Category savedParent = categoryRepository.save(parent);
//
//        Category child = Category.builder().name("Child").deleted(false).build();
//        child.setParent(savedParent);
//        categoryRepository.save(child);
//
//        // when
//        categoryRepository.softDeleteChildren(savedParent.getId());
//        entityManager.flush();
//        entityManager.clear();
//
//        // then
//        List<Category> children = categoryRepository.findAll();
//        assertThat(children.get(1).isDeleted()).isTrue();
//    }
//
//    @Test
//    @DisplayName("부모 ID로 하위 Category 복구")
//    void restoreChildren() {
//        // given
//        Category parent = Category.builder().name("Parent").deleted(false).build();
//        Category savedParent = categoryRepository.save(parent);
//
//        Category child = Category.builder().name("Child").deleted(true).build();
//        child.setParent(savedParent);
//        categoryRepository.save(child);
//
//        // when
//        categoryRepository.restoreChildren(savedParent.getId());
//        entityManager.flush();
//        entityManager.clear();
//
//        // then
//        List<Category> children = categoryRepository.findAll();
//        assertThat(children.get(1).isDeleted()).isFalse();
//    }

    @Test
    @DisplayName("Category 이름으로 단건 조회")
    void findByName() {
        // given
        Category category = Category.builder().name("Fashion").deleted(false).build();
        categoryRepository.save(category);

        // when
        Optional<Category> found = categoryRepository.findByName("Fashion");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Fashion");
    }
}

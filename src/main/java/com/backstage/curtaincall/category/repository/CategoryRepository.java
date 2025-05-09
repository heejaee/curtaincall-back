package com.backstage.curtaincall.category.repository;

import com.backstage.curtaincall.category.domain.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category,Long> {


    // 삭제되지않는 데이터만 전체조회
    @Query("SELECT c FROM Category c WHERE c.deleted = false")
    List<Category> findAllNotDeleted();

    // 삭제되지 않은 데이터만 단일조회
    @Query("SELECT c from Category c where c.id =:id and c.deleted = false")
    Optional<Category> findByIdNotDeleted(Long id);


    // 삭제된 데이터만 전체조회
    @Query("SELECT c FROM Category c WHERE c.deleted = true")
    List<Category> findAllDeleted();

    @Query("SELECT c from Category c where c.id =:id and c.deleted = true")
    Optional<Category> findByIdDeleted(Long id);

    // 이름으로 Category 존재 여부 확인
    @Query("select count(*)>0 from Category c where c.name =:name")
    boolean existsByName(@Param("name") String name);

//    @Query(value = "SELECT EXISTS (SELECT 1 FROM categories WHERE name = :name AND is_deleted = false)", nativeQuery = true)
//    boolean existsByName(@Param("name") String name);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Category c SET c.deleted = true WHERE c.parent.id = :parentId")
    void softDeleteChildren(@Param("parentId") Long parentId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Category c SET c.deleted = false WHERE c.parent.id = :parentId")
    void restoreChildren(@Param("parentId") Long parentId);

    Optional<Category> findByName(String name);


}

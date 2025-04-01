package com.backstage.curtaincall.specialProduct.repository;

import com.backstage.curtaincall.specialProduct.entity.SpecialProduct;
import com.backstage.curtaincall.specialProduct.entity.SpecialProductStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SpecialProductRepository {

    @PersistenceContext
    EntityManager em;

    public List<SpecialProduct> findAll() {
        return em.createQuery(
                        "SELECT sp FROM SpecialProduct sp JOIN FETCH sp.product p " +
                                "WHERE sp.status != :deleted", SpecialProduct.class)
                .setParameter("deleted", SpecialProductStatus.DELETED)
                .getResultList();
    }

    // 활성 (ACTIVE) 상태의 특가상품 조회
    public List<SpecialProduct> findAllActive() {
        return em.createQuery(
                        "SELECT sp FROM SpecialProduct sp JOIN FETCH sp.product p " +
                                "LEFT JOIN FETCH p.productImage pi " +
                                "WHERE sp.status = :active", SpecialProduct.class)
                .setParameter("active", SpecialProductStatus.ACTIVE)
                .getResultList();
    }



    // 이름 검색 및 페이지네이션을 적용한 전체 조회
    public Page<SpecialProduct> findAll(String keyword, Pageable pageable) {
        StringBuilder jpql = new StringBuilder(
                "SELECT sp FROM SpecialProduct sp " +
                        "JOIN FETCH sp.product p " +
                        "LEFT JOIN FETCH p.productImage pi " +
                        "WHERE sp.status != :deleted ");
        StringBuilder countJpql = new StringBuilder(
                "SELECT COUNT(sp) FROM SpecialProduct sp JOIN sp.product p " +
                        "WHERE sp.status != :deleted ");

        if (keyword != null && !keyword.trim().isEmpty()) {
            jpql.append("AND p.productName LIKE :keyword ");
            countJpql.append("AND p.productName LIKE :keyword ");
        }

        // 상품 이름 순으로 정렬하고, 동일한 이름 내에서는 종료일이 작은 순으로 정렬
        jpql.append("ORDER BY p.productName ASC, sp.endDate ASC");

        TypedQuery<SpecialProduct> query = em.createQuery(jpql.toString(), SpecialProduct.class);
        TypedQuery<Long> countQuery = em.createQuery(countJpql.toString(), Long.class);

        query.setParameter("deleted", SpecialProductStatus.DELETED);
        countQuery.setParameter("deleted", SpecialProductStatus.DELETED);

        if (keyword != null && !keyword.trim().isEmpty()) {
            query.setParameter("keyword", "%" + keyword + "%");
            countQuery.setParameter("keyword", "%" + keyword + "%");
        }

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        List<SpecialProduct> content = query.getResultList();
        Long total = countQuery.getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    // 삭제된 것만 전체 조회
    public List<SpecialProduct> findAllDeleted() {
        return em.createQuery(
                        "SELECT sp FROM SpecialProduct sp JOIN FETCH sp.product p " +
                                "LEFT JOIN FETCH p.productImage pi " +
                                "WHERE sp.status = :deleted", SpecialProduct.class)
                .setParameter("deleted", SpecialProductStatus.DELETED)
                .getResultList();
    }

    public Optional<SpecialProduct> findById(Long id) {
        return em.createQuery(
                        "SELECT sp FROM SpecialProduct sp " +
                                "WHERE sp.id = :id AND sp.status != :deleted", SpecialProduct.class)
                .setParameter("id", id)
                .setParameter("deleted", SpecialProductStatus.DELETED)
                .getResultStream()
                .findFirst();
    }

    public List<SpecialProduct> findAllByProductId(Long productId){
        return em.createQuery(
                        "SELECT sp FROM SpecialProduct sp " +
                                "WHERE sp.product.productId = :productId AND sp.status != :deleted", SpecialProduct.class)
                .setParameter("productId", productId)
                .setParameter("deleted", SpecialProductStatus.DELETED)
                .getResultList();
    }

    public List<SpecialProduct> findAllOverlappingDates(Long productId, Long excludeId, LocalDate newStartDate, LocalDate newEndDate) {
        return em.createQuery(
                        "SELECT sp FROM SpecialProduct sp " +
                                "WHERE sp.product.id = :productId " +
                                "AND sp.status != :deleted " +
                                "AND (:excludeId IS NULL OR sp.id <> :excludeId) " +
                                "AND ((sp.startDate BETWEEN :newStartDate AND :newEndDate) " +
                                "OR (sp.endDate BETWEEN :newStartDate AND :newEndDate) " +
                                "OR (sp.startDate <= :newStartDate AND sp.endDate >= :newEndDate) " +
                                "OR (sp.startDate >= :newStartDate AND sp.endDate <= :newEndDate))", SpecialProduct.class)
                .setParameter("productId", productId)
                .setParameter("excludeId", excludeId)
                .setParameter("newStartDate", newStartDate)
                .setParameter("newEndDate", newEndDate)
                .setParameter("deleted", SpecialProductStatus.DELETED)
                .getResultList();
    }

    public Optional<SpecialProduct> findByIdWithProduct(Long id) {
        return em.createQuery(
                        "SELECT sp FROM SpecialProduct sp " +
                                "JOIN FETCH sp.product p " +
                                "WHERE sp.id = :id AND sp.status <> :deleted", SpecialProduct.class)
                .setParameter("id", id)
                .setParameter("deleted", SpecialProductStatus.DELETED)
                .getResultStream()
                .findFirst();
    }


    public Optional<SpecialProduct> findByIdUpcoming(Long id) {
        return em.createQuery(
                        "SELECT sp FROM SpecialProduct sp " +
                                "WHERE sp.id = :id AND sp.status = :upcoming", SpecialProduct.class)
                .setParameter("id", id)
                .setParameter("upcoming", SpecialProductStatus.UPCOMING)
                .getResultStream()
                .findFirst();
    }

    public Optional<SpecialProduct> findByIdActive(Long id) {
        return em.createQuery(
                        "SELECT sp FROM SpecialProduct sp " +
                                "WHERE sp.id = :id AND sp.status = :active", SpecialProduct.class)
                .setParameter("id", id)
                .setParameter("active", SpecialProductStatus.ACTIVE)
                .getResultStream()
                .findFirst();
    }

    public void save(SpecialProduct specialProduct) {
        em.persist(specialProduct);
    }


    public List<SpecialProduct> findAllStartingSpecialProducts(LocalDate today) {
        return em.createQuery(
                        "SELECT sp FROM SpecialProduct sp " +
                                "WHERE sp.endDate = ( " +
                                "    SELECT MIN(sp2.endDate) FROM SpecialProduct sp2 " +
                                "    WHERE sp2.product.id = sp.product.id " +
                                "    AND sp2.endDate >= :today " +
                                "    AND sp2.status != :deleted " +
                                ") " +
                                "AND sp.status = :upcoming",
                        SpecialProduct.class)
                .setParameter("today", today)
                .setParameter("deleted", SpecialProductStatus.DELETED)
                .setParameter("upcoming", SpecialProductStatus.UPCOMING)
                .getResultList();
    }


    public boolean existsByProductIdAndStatus(Long productId) {
        return em.createQuery(
                        "SELECT CASE WHEN EXISTS (" +
                                "    SELECT 1 FROM SpecialProduct sp " +
                                "    WHERE sp.product.id = :productId " +
                                "    AND sp.status = :active" +
                                ") THEN true ELSE false END FROM SpecialProduct sp",
                        Boolean.class)
                .setParameter("productId", productId)
                .setParameter("active", SpecialProductStatus.ACTIVE)
                .getSingleResult();
    }


    public List<Long> findExpiredSpecialProductIds(LocalDate today) {
        return em.createQuery(
                        "SELECT sp.id FROM SpecialProduct sp " +
                                "WHERE sp.endDate < :today AND sp.status != :deleted", Long.class)
                .setParameter("today", today)
                .setParameter("deleted", SpecialProductStatus.DELETED)
                .getResultList();
    }
}

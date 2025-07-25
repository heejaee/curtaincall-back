package com.backstage.curtaincall.specialProduct.entity;

import com.backstage.curtaincall.global.entity.BaseEntity;
import com.backstage.curtaincall.product.entity.Product;
import com.backstage.curtaincall.specialProduct.dto.SpecialProductDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Table(name ="special_products")
@Builder
public class SpecialProduct extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "special_product_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private int discountRate; // 할인율 (0~100, 정수만 허용)

    private LocalDate startDate; // 할인 시작 날짜

    private LocalDate endDate; // 할인 종료 날짜

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SpecialProductStatus status;

    public static SpecialProduct of(Product product, SpecialProductDto dto) {
        return SpecialProduct.builder()
                .product(product)
                .discountRate(dto.getDiscountRate())
                .startDate(dto.getDiscountStartDate())
                .endDate(dto.getDiscountEndDate())
                .status(SpecialProductStatus.UPCOMING)
                .build();
    }


    public SpecialProductDto toDto() {
        return SpecialProductDto.builder()
                // SpecialProduct 정보
                .specialProductId(this.id)
                .discountRate(this.discountRate)
                .discountStartDate(this.startDate)
                .discountEndDate(this.endDate)
                .status(this.status)

                // Product 정보
                .productId(this.product.getProductId())
                .productName(this.product.getProductName())
                .price(this.product.getPrice())
                .startDate(this.product.getStartDate())
                .endDate(this.product.getEndDate())
                .place(this.product.getPlace())
                .runningTime(this.product.getRunningTime())
                .casting(this.product.getCasting())
                .notice(this.product.getNotice())

                // ProductImage 정보
                .imageUrl(this.product.getProductImage().getImageUrl())

                .build();
    }

    public SpecialProductDto toUpdatedDto(Product updatedProduct) {
        return SpecialProductDto.builder()
                // SpecialProduct 정보
                .specialProductId(this.id)
                .discountRate(this.discountRate)
                .discountStartDate(this.startDate)
                .discountEndDate(this.endDate)
                .status(this.status)

                // 업데이트된 Product 정보 반영
                .productId(updatedProduct.getProductId())
                .productName(updatedProduct.getProductName())
                .price(updatedProduct.getPrice())
                .startDate(updatedProduct.getStartDate())
                .endDate(updatedProduct.getEndDate())
                .place(updatedProduct.getPlace())
                .runningTime(updatedProduct.getRunningTime())
                .casting(updatedProduct.getCasting())
                .notice(updatedProduct.getNotice())

                // ProductImage 정보
                .imageUrl(updatedProduct.getProductImage() != null ? updatedProduct.getProductImage().getImageUrl() : null)

                .build();
    }

    public void save(Long id){
        this.id =id;
    }

    public void update(SpecialProductDto dto) {
        this.discountRate = dto.getDiscountRate();
        this.startDate = dto.getDiscountStartDate();
        this.endDate  = dto.getDiscountEndDate();
        this.status = dto.getStatus();
    }

    public void delete() {
        this.status = SpecialProductStatus.DELETED;
    }

    public void approve() {
        this.status = SpecialProductStatus.ACTIVE;
    }

    public void approveCancel() {
        this.status = SpecialProductStatus.UPCOMING;
    }
}

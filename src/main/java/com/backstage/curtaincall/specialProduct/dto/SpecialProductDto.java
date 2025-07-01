package com.backstage.curtaincall.specialProduct.dto;

import com.backstage.curtaincall.product.entity.Product;
import com.backstage.curtaincall.specialProduct.entity.SpecialProductStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialProductDto {

    // Product 정보
    private Long productId;
    private String productName;
    private int price;
    private LocalDate startDate;
    private LocalDate endDate;
    //추가
    private String place;
    private int runningTime;
    private String casting;
    private String notice;

    // SpecialProduct 정보
    private Long specialProductId;
    private boolean expiring;

    @Min(value = 0, message = "할인율은 {value}% 이상이어야 합니다.")
    @Max(value = 100, message = "할인율은 {value}% 이하이어야 합니다.")
    private int discountRate;
    private LocalDate discountStartDate; // 할인 시작일시
    private LocalDate discountEndDate;   // 할인 종료일시
    private SpecialProductStatus status;

    //ProductImage 정보
    private String imageUrl;


    
     //Product 엔티티를 SpecialProductDto로 변환하는 메서드
    public static SpecialProductDto of(Product product) {
        return SpecialProductDto.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .price(product.getPrice())
                .startDate(product.getStartDate())
                .endDate(product.getEndDate())
                .place(product.getPlace())
                .runningTime(product.getRunningTime())
                .build();
    }
}

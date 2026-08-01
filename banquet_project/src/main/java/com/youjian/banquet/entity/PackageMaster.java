package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "package_master")
@IdClass(PackageMaster.PackageMasterId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackageMaster {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PackageMasterId implements Serializable {
        private String packageId;
        private Long storeId;
    }

    @Id
    @Column(name = "package_id", length = 20)
    private String packageId;

    @Id
    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "package_name", length = 100)
    private String packageName;

    @Column(name = "package_total_price", precision = 10, scale = 2)
    private BigDecimal packageTotalPrice;

    @Column(name = "package_cost_price", precision = 10, scale = 2)
    private BigDecimal packageCostPrice;

    @Column(name = "cost_rate", precision = 5, scale = 2)
    private BigDecimal costRate;

    @Column(name = "dish_count")
    private Integer dishCount;

    @Column(name = "suggest_guests")
    private Integer suggestGuests;

    @Column(name = "occasion_type", length = 20)
    private String occasionType;

    @Column(name = "package_series", length = 20)
    private String packageSeries;

    @Column(name = "is_active")
    private Integer isActive;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "category", length = 255)
    private String category;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "original_price", precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "min_guests")
    private Integer minGuests;

    @Column(name = "max_guests")
    private Integer maxGuests;

    @Column(name = "status", length = 255)
    private String status;

    @Column(name = "tags", length = 255)
    private String tags;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

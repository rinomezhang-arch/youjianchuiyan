package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dish_master")
@IdClass(DishMaster.DishMasterId.class)
public class DishMaster {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DishMasterId implements Serializable {
        private String dishId;
        private Long storeId;
    }

    @Id
    @Column(name = "dish_id")
    private String dishId;

    @Id
    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "dish_name")
    private String dishName;

    @Column(name = "dish_category")
    private String dishCategory;

    @Column(name = "spicy_level")
    private Integer spicyLevel;

    @Column(name = "main_ingredient_type")
    private String mainIngredientType;

    @Column(name = "main_ingredient")
    private String mainIngredient;

    @Column(name = "english_name")
    private String englishName;

    @Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "sale_price", precision = 10, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "cost_rate", precision = 5, scale = 2)
    private BigDecimal costRate;

    @Column(name = "cooking_time")
    private Integer cookingTime;

    @Column(name = "servings")
    private Integer servings;

    // ===== 宴席场合名称字段族 =====
    @Column(name = "birthday_name")
    private String birthdayName;

    @Column(name = "wedding_name")
    private String weddingName;

    @Column(name = "house_move_name")
    private String houseMoveName;

    @Column(name = "promotion_name")
    private String promotionName;

    @Column(name = "reunion_name")
    private String reunionName;

    @Column(name = "thanksgiving_name")
    private String thanksgivingName;

    @Column(name = "year_end_name")
    private String yearEndName;

    @Column(name = "baby_born_name")
    private String babyBornName;

    @Column(name = "festive_name")
    private String festiveName;

    @Column(name = "business_name")
    private String businessName;

    @Column(name = "spring_name")
    private String springName;

    @Column(name = "school_name")
    private String schoolName;

    @Column(name = "victory_name")
    private String victoryName;

    @Column(name = "opening_name")
    private String openingName;

    @Column(name = "comrade_name")
    private String comradeName;

    @Column(name = "teacher_name")
    private String teacherName;

    @Column(name = "adult_name")
    private String adultName;
    // ===== 场合名称字段族结束 =====

    @Column(name = "is_active")
    private Integer isActive;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "usage_type")
    private String usageType;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "dish_intro", columnDefinition = "TEXT")
    private String dishIntro;

    @Column(name = "tiktok_recommend", columnDefinition = "TEXT")
    private String tiktokRecommend;

    @Column(name = "menu_type")
    private String menuType;

    @Column(name = "category")
    private String category;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "cooking_method")
    private String cookingMethod;

    @Column(name = "dish_code")
    private String dishCode;

    @Column(name = "dish_name_en")
    private String dishNameEn;

    @Column(name = "is_seasonal")
    private Integer isSeasonal;

    @Column(name = "is_specialty")
    private Integer isSpecialty;

    @Column(name = "main_ingredients", columnDefinition = "TEXT")
    private String mainIngredients;

    @Column(name = "taste")
    private String taste;

    @Column(name = "unit")
    private String unit;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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

/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.DishMaster
 *  com.youjian.banquet.entity.DishMaster$DishMasterId
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Id
 *  jakarta.persistence.IdClass
 *  jakarta.persistence.PrePersist
 *  jakarta.persistence.PreUpdate
 *  jakarta.persistence.Table
 */
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
@Table(name="dish_master")
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
    @Column(name="dish_id")
    private String dishId;
    @Id
    @Column(name="store_id")
    private Long storeId;
    @Column(name="dish_name")
    private String dishName;
    @Column(name="dish_category")
    private String dishCategory;
    @Column(name="spicy_level")
    private Integer spicyLevel;
    @Column(name="main_ingredient_type")
    private String mainIngredientType;
    @Column(name="main_ingredient")
    private String mainIngredient;
    @Column(name="english_name")
    private String englishName;
    @Column(name="cost_price", precision=10, scale=2)
    private BigDecimal costPrice;
    @Column(name="sale_price", precision=10, scale=2)
    private BigDecimal salePrice;
    @Column(name="cost_rate", precision=5, scale=2)
    private BigDecimal costRate;
    @Column(name="cooking_time")
    private Integer cookingTime;
    @Column(name="servings")
    private Integer servings;
    @Column(name="birthday_name")
    private String birthdayName;
    @Column(name="wedding_name")
    private String weddingName;
    @Column(name="house_move_name")
    private String houseMoveName;
    @Column(name="promotion_name")
    private String promotionName;
    @Column(name="reunion_name")
    private String reunionName;
    @Column(name="thanksgiving_name")
    private String thanksgivingName;
    @Column(name="year_end_name")
    private String yearEndName;
    @Column(name="baby_born_name")
    private String babyBornName;
    @Column(name="festive_name")
    private String festiveName;
    @Column(name="is_active")
    private Integer isActive;
    @Column(name="sort_order")
    private Integer sortOrder;
    @Column(name="usage_type")
    private String usageType;
    @Column(name="image_url")
    private String imageUrl;
    @Column(name="dish_intro")
    private String dishIntro;
    @Column(name="tiktok_recommend")
    private String tiktokRecommend;
    @Column(name="create_time")
    private LocalDateTime createdAt;
    @Column(name="update_time")
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

    public String getDishId() {
        return this.dishId;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getDishName() {
        return this.dishName;
    }

    public String getDishCategory() {
        return this.dishCategory;
    }

    public Integer getSpicyLevel() {
        return this.spicyLevel;
    }

    public String getMainIngredientType() {
        return this.mainIngredientType;
    }

    public String getMainIngredient() {
        return this.mainIngredient;
    }

    public String getEnglishName() {
        return this.englishName;
    }

    public BigDecimal getCostPrice() {
        return this.costPrice;
    }

    public BigDecimal getSalePrice() {
        return this.salePrice;
    }

    public BigDecimal getCostRate() {
        return this.costRate;
    }

    public Integer getCookingTime() {
        return this.cookingTime;
    }

    public Integer getServings() {
        return this.servings;
    }

    public String getBirthdayName() {
        return this.birthdayName;
    }

    public String getWeddingName() {
        return this.weddingName;
    }

    public String getHouseMoveName() {
        return this.houseMoveName;
    }

    public String getPromotionName() {
        return this.promotionName;
    }

    public String getReunionName() {
        return this.reunionName;
    }

    public String getThanksgivingName() {
        return this.thanksgivingName;
    }

    public String getYearEndName() {
        return this.yearEndName;
    }

    public String getBabyBornName() {
        return this.babyBornName;
    }

    public String getFestiveName() {
        return this.festiveName;
    }

    public Integer getIsActive() {
        return this.isActive;
    }

    public Integer getSortOrder() {
        return this.sortOrder;
    }

    public String getUsageType() {
        return this.usageType;
    }

    public String getImageUrl() {
        return this.imageUrl;
    }

    public String getDishIntro() {
        return this.dishIntro;
    }

    public String getTiktokRecommend() {
        return this.tiktokRecommend;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setDishId(String dishId) {
        this.dishId = dishId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public void setDishName(String dishName) {
        this.dishName = dishName;
    }

    public void setDishCategory(String dishCategory) {
        this.dishCategory = dishCategory;
    }

    public void setSpicyLevel(Integer spicyLevel) {
        this.spicyLevel = spicyLevel;
    }

    public void setMainIngredientType(String mainIngredientType) {
        this.mainIngredientType = mainIngredientType;
    }

    public void setMainIngredient(String mainIngredient) {
        this.mainIngredient = mainIngredient;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public void setCostRate(BigDecimal costRate) {
        this.costRate = costRate;
    }

    public void setCookingTime(Integer cookingTime) {
        this.cookingTime = cookingTime;
    }

    public void setServings(Integer servings) {
        this.servings = servings;
    }

    public void setBirthdayName(String birthdayName) {
        this.birthdayName = birthdayName;
    }

    public void setWeddingName(String weddingName) {
        this.weddingName = weddingName;
    }

    public void setHouseMoveName(String houseMoveName) {
        this.houseMoveName = houseMoveName;
    }

    public void setPromotionName(String promotionName) {
        this.promotionName = promotionName;
    }

    public void setReunionName(String reunionName) {
        this.reunionName = reunionName;
    }

    public void setThanksgivingName(String thanksgivingName) {
        this.thanksgivingName = thanksgivingName;
    }

    public void setYearEndName(String yearEndName) {
        this.yearEndName = yearEndName;
    }

    public void setBabyBornName(String babyBornName) {
        this.babyBornName = babyBornName;
    }

    public void setFestiveName(String festiveName) {
        this.festiveName = festiveName;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setUsageType(String usageType) {
        this.usageType = usageType;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setDishIntro(String dishIntro) {
        this.dishIntro = dishIntro;
    }

    public void setTiktokRecommend(String tiktokRecommend) {
        this.tiktokRecommend = tiktokRecommend;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DishMaster)) {
            return false;
        }
        DishMaster other = (DishMaster)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$storeId = this.getStoreId();
        Long other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !((Object)this$storeId).equals(other$storeId)) {
            return false;
        }
        Integer this$spicyLevel = this.getSpicyLevel();
        Integer other$spicyLevel = other.getSpicyLevel();
        if (this$spicyLevel == null ? other$spicyLevel != null : !((Object)this$spicyLevel).equals(other$spicyLevel)) {
            return false;
        }
        Integer this$cookingTime = this.getCookingTime();
        Integer other$cookingTime = other.getCookingTime();
        if (this$cookingTime == null ? other$cookingTime != null : !((Object)this$cookingTime).equals(other$cookingTime)) {
            return false;
        }
        Integer this$servings = this.getServings();
        Integer other$servings = other.getServings();
        if (this$servings == null ? other$servings != null : !((Object)this$servings).equals(other$servings)) {
            return false;
        }
        Integer this$isActive = this.getIsActive();
        Integer other$isActive = other.getIsActive();
        if (this$isActive == null ? other$isActive != null : !((Object)this$isActive).equals(other$isActive)) {
            return false;
        }
        Integer this$sortOrder = this.getSortOrder();
        Integer other$sortOrder = other.getSortOrder();
        if (this$sortOrder == null ? other$sortOrder != null : !((Object)this$sortOrder).equals(other$sortOrder)) {
            return false;
        }
        String this$dishId = this.getDishId();
        String other$dishId = other.getDishId();
        if (this$dishId == null ? other$dishId != null : !this$dishId.equals(other$dishId)) {
            return false;
        }
        String this$dishName = this.getDishName();
        String other$dishName = other.getDishName();
        if (this$dishName == null ? other$dishName != null : !this$dishName.equals(other$dishName)) {
            return false;
        }
        String this$dishCategory = this.getDishCategory();
        String other$dishCategory = other.getDishCategory();
        if (this$dishCategory == null ? other$dishCategory != null : !this$dishCategory.equals(other$dishCategory)) {
            return false;
        }
        String this$mainIngredientType = this.getMainIngredientType();
        String other$mainIngredientType = other.getMainIngredientType();
        if (this$mainIngredientType == null ? other$mainIngredientType != null : !this$mainIngredientType.equals(other$mainIngredientType)) {
            return false;
        }
        String this$mainIngredient = this.getMainIngredient();
        String other$mainIngredient = other.getMainIngredient();
        if (this$mainIngredient == null ? other$mainIngredient != null : !this$mainIngredient.equals(other$mainIngredient)) {
            return false;
        }
        String this$englishName = this.getEnglishName();
        String other$englishName = other.getEnglishName();
        if (this$englishName == null ? other$englishName != null : !this$englishName.equals(other$englishName)) {
            return false;
        }
        BigDecimal this$costPrice = this.getCostPrice();
        BigDecimal other$costPrice = other.getCostPrice();
        if (this$costPrice == null ? other$costPrice != null : !((Object)this$costPrice).equals(other$costPrice)) {
            return false;
        }
        BigDecimal this$salePrice = this.getSalePrice();
        BigDecimal other$salePrice = other.getSalePrice();
        if (this$salePrice == null ? other$salePrice != null : !((Object)this$salePrice).equals(other$salePrice)) {
            return false;
        }
        BigDecimal this$costRate = this.getCostRate();
        BigDecimal other$costRate = other.getCostRate();
        if (this$costRate == null ? other$costRate != null : !((Object)this$costRate).equals(other$costRate)) {
            return false;
        }
        String this$birthdayName = this.getBirthdayName();
        String other$birthdayName = other.getBirthdayName();
        if (this$birthdayName == null ? other$birthdayName != null : !this$birthdayName.equals(other$birthdayName)) {
            return false;
        }
        String this$weddingName = this.getWeddingName();
        String other$weddingName = other.getWeddingName();
        if (this$weddingName == null ? other$weddingName != null : !this$weddingName.equals(other$weddingName)) {
            return false;
        }
        String this$houseMoveName = this.getHouseMoveName();
        String other$houseMoveName = other.getHouseMoveName();
        if (this$houseMoveName == null ? other$houseMoveName != null : !this$houseMoveName.equals(other$houseMoveName)) {
            return false;
        }
        String this$promotionName = this.getPromotionName();
        String other$promotionName = other.getPromotionName();
        if (this$promotionName == null ? other$promotionName != null : !this$promotionName.equals(other$promotionName)) {
            return false;
        }
        String this$reunionName = this.getReunionName();
        String other$reunionName = other.getReunionName();
        if (this$reunionName == null ? other$reunionName != null : !this$reunionName.equals(other$reunionName)) {
            return false;
        }
        String this$thanksgivingName = this.getThanksgivingName();
        String other$thanksgivingName = other.getThanksgivingName();
        if (this$thanksgivingName == null ? other$thanksgivingName != null : !this$thanksgivingName.equals(other$thanksgivingName)) {
            return false;
        }
        String this$yearEndName = this.getYearEndName();
        String other$yearEndName = other.getYearEndName();
        if (this$yearEndName == null ? other$yearEndName != null : !this$yearEndName.equals(other$yearEndName)) {
            return false;
        }
        String this$babyBornName = this.getBabyBornName();
        String other$babyBornName = other.getBabyBornName();
        if (this$babyBornName == null ? other$babyBornName != null : !this$babyBornName.equals(other$babyBornName)) {
            return false;
        }
        String this$festiveName = this.getFestiveName();
        String other$festiveName = other.getFestiveName();
        if (this$festiveName == null ? other$festiveName != null : !this$festiveName.equals(other$festiveName)) {
            return false;
        }
        String this$usageType = this.getUsageType();
        String other$usageType = other.getUsageType();
        if (this$usageType == null ? other$usageType != null : !this$usageType.equals(other$usageType)) {
            return false;
        }
        String this$imageUrl = this.getImageUrl();
        String other$imageUrl = other.getImageUrl();
        if (this$imageUrl == null ? other$imageUrl != null : !this$imageUrl.equals(other$imageUrl)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt)) {
            return false;
        }
        LocalDateTime this$updatedAt = this.getUpdatedAt();
        LocalDateTime other$updatedAt = other.getUpdatedAt();
        return !(this$updatedAt == null ? other$updatedAt != null : !((Object)this$updatedAt).equals(other$updatedAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof DishMaster;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : ((Object)$storeId).hashCode());
        Integer $spicyLevel = this.getSpicyLevel();
        result = result * 59 + ($spicyLevel == null ? 43 : ((Object)$spicyLevel).hashCode());
        Integer $cookingTime = this.getCookingTime();
        result = result * 59 + ($cookingTime == null ? 43 : ((Object)$cookingTime).hashCode());
        Integer $servings = this.getServings();
        result = result * 59 + ($servings == null ? 43 : ((Object)$servings).hashCode());
        Integer $isActive = this.getIsActive();
        result = result * 59 + ($isActive == null ? 43 : ((Object)$isActive).hashCode());
        Integer $sortOrder = this.getSortOrder();
        result = result * 59 + ($sortOrder == null ? 43 : ((Object)$sortOrder).hashCode());
        String $dishId = this.getDishId();
        result = result * 59 + ($dishId == null ? 43 : $dishId.hashCode());
        String $dishName = this.getDishName();
        result = result * 59 + ($dishName == null ? 43 : $dishName.hashCode());
        String $dishCategory = this.getDishCategory();
        result = result * 59 + ($dishCategory == null ? 43 : $dishCategory.hashCode());
        String $mainIngredientType = this.getMainIngredientType();
        result = result * 59 + ($mainIngredientType == null ? 43 : $mainIngredientType.hashCode());
        String $mainIngredient = this.getMainIngredient();
        result = result * 59 + ($mainIngredient == null ? 43 : $mainIngredient.hashCode());
        String $englishName = this.getEnglishName();
        result = result * 59 + ($englishName == null ? 43 : $englishName.hashCode());
        BigDecimal $costPrice = this.getCostPrice();
        result = result * 59 + ($costPrice == null ? 43 : ((Object)$costPrice).hashCode());
        BigDecimal $salePrice = this.getSalePrice();
        result = result * 59 + ($salePrice == null ? 43 : ((Object)$salePrice).hashCode());
        BigDecimal $costRate = this.getCostRate();
        result = result * 59 + ($costRate == null ? 43 : ((Object)$costRate).hashCode());
        String $birthdayName = this.getBirthdayName();
        result = result * 59 + ($birthdayName == null ? 43 : $birthdayName.hashCode());
        String $weddingName = this.getWeddingName();
        result = result * 59 + ($weddingName == null ? 43 : $weddingName.hashCode());
        String $houseMoveName = this.getHouseMoveName();
        result = result * 59 + ($houseMoveName == null ? 43 : $houseMoveName.hashCode());
        String $promotionName = this.getPromotionName();
        result = result * 59 + ($promotionName == null ? 43 : $promotionName.hashCode());
        String $reunionName = this.getReunionName();
        result = result * 59 + ($reunionName == null ? 43 : $reunionName.hashCode());
        String $thanksgivingName = this.getThanksgivingName();
        result = result * 59 + ($thanksgivingName == null ? 43 : $thanksgivingName.hashCode());
        String $yearEndName = this.getYearEndName();
        result = result * 59 + ($yearEndName == null ? 43 : $yearEndName.hashCode());
        String $babyBornName = this.getBabyBornName();
        result = result * 59 + ($babyBornName == null ? 43 : $babyBornName.hashCode());
        String $festiveName = this.getFestiveName();
        result = result * 59 + ($festiveName == null ? 43 : $festiveName.hashCode());
        String $usageType = this.getUsageType();
        result = result * 59 + ($usageType == null ? 43 : $usageType.hashCode());
        String $imageUrl = this.getImageUrl();
        result = result * 59 + ($imageUrl == null ? 43 : $imageUrl.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        LocalDateTime $updatedAt = this.getUpdatedAt();
        result = result * 59 + ($updatedAt == null ? 43 : ((Object)$updatedAt).hashCode());
        return result;
    }

    public String toString() {
        return "DishMaster(dishId=" + this.getDishId() + ", storeId=" + this.getStoreId() + ", dishName=" + this.getDishName() + ", dishCategory=" + this.getDishCategory() + ", spicyLevel=" + this.getSpicyLevel() + ", mainIngredientType=" + this.getMainIngredientType() + ", mainIngredient=" + this.getMainIngredient() + ", englishName=" + this.getEnglishName() + ", costPrice=" + String.valueOf(this.getCostPrice()) + ", salePrice=" + String.valueOf(this.getSalePrice()) + ", costRate=" + String.valueOf(this.getCostRate()) + ", cookingTime=" + this.getCookingTime() + ", servings=" + this.getServings() + ", birthdayName=" + this.getBirthdayName() + ", weddingName=" + this.getWeddingName() + ", houseMoveName=" + this.getHouseMoveName() + ", promotionName=" + this.getPromotionName() + ", reunionName=" + this.getReunionName() + ", thanksgivingName=" + this.getThanksgivingName() + ", yearEndName=" + this.getYearEndName() + ", babyBornName=" + this.getBabyBornName() + ", festiveName=" + this.getFestiveName() + ", isActive=" + this.getIsActive() + ", sortOrder=" + this.getSortOrder() + ", usageType=" + this.getUsageType() + ", imageUrl=" + this.getImageUrl() + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ", updatedAt=" + String.valueOf(this.getUpdatedAt()) + ")";
    }

    public DishMaster() {
    }

    public DishMaster(String dishId, Long storeId, String dishName, String dishCategory, Integer spicyLevel, String mainIngredientType, String mainIngredient, String englishName, BigDecimal costPrice, BigDecimal salePrice, BigDecimal costRate, Integer cookingTime, Integer servings, String birthdayName, String weddingName, String houseMoveName, String promotionName, String reunionName, String thanksgivingName, String yearEndName, String babyBornName, String festiveName, Integer isActive, Integer sortOrder, String usageType, String imageUrl, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.dishId = dishId;
        this.storeId = storeId;
        this.dishName = dishName;
        this.dishCategory = dishCategory;
        this.spicyLevel = spicyLevel;
        this.mainIngredientType = mainIngredientType;
        this.mainIngredient = mainIngredient;
        this.englishName = englishName;
        this.costPrice = costPrice;
        this.salePrice = salePrice;
        this.costRate = costRate;
        this.cookingTime = cookingTime;
        this.servings = servings;
        this.birthdayName = birthdayName;
        this.weddingName = weddingName;
        this.houseMoveName = houseMoveName;
        this.promotionName = promotionName;
        this.reunionName = reunionName;
        this.thanksgivingName = thanksgivingName;
        this.yearEndName = yearEndName;
        this.babyBornName = babyBornName;
        this.festiveName = festiveName;
        this.isActive = isActive;
        this.sortOrder = sortOrder;
        this.usageType = usageType;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}


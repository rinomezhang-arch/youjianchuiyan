package com.youjian.banquet.dto;

import java.math.BigDecimal;

public class DishDTO {
    private String dishId;
    private String storeId;
    private String dishName;
    private String category;
    private String dishCategory;
    private BigDecimal price;
    private BigDecimal costPrice;
    private BigDecimal salePrice;
    private BigDecimal costRate;
    private String unit;
    private String description;
    private String imageUrl;
    private String status;
    private String tags;
    private Integer sortOrder;
    private String mainIngredient;
    private String mainIngredientType;
    private String englishName;
    private Integer cookingTime;
    private Integer servings;
    private Integer spicyLevel;
    private String festiveName;
    private String usageType;
    private String dishIntro;
    private String tiktokRecommend;

    public String getDishId() { return this.dishId; }
    public String getStoreId() { return this.storeId; }
    public String getDishName() { return this.dishName; }
    public String getCategory() { return this.category; }
    public String getDishCategory() { return this.dishCategory; }
    public BigDecimal getPrice() { return this.price; }
    public BigDecimal getCostPrice() { return this.costPrice; }
    public BigDecimal getSalePrice() { return this.salePrice; }
    public BigDecimal getCostRate() { return this.costRate; }
    public String getUnit() { return this.unit; }
    public String getDescription() { return this.description; }
    public String getImageUrl() { return this.imageUrl; }
    public String getStatus() { return this.status; }
    public String getTags() { return this.tags; }
    public Integer getSortOrder() { return this.sortOrder; }
    public String getMainIngredient() { return this.mainIngredient; }
    public String getMainIngredientType() { return this.mainIngredientType; }
    public String getEnglishName() { return this.englishName; }
    public Integer getCookingTime() { return this.cookingTime; }
    public Integer getServings() { return this.servings; }
    public Integer getSpicyLevel() { return this.spicyLevel; }
    public String getFestiveName() { return this.festiveName; }
    public String getUsageType() { return this.usageType; }
    public String getDishIntro() { return this.dishIntro; }
    public String getTiktokRecommend() { return this.tiktokRecommend; }

    public void setDishId(String dishId) { this.dishId = dishId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
    public void setDishName(String dishName) { this.dishName = dishName; }
    public void setCategory(String category) { this.category = category; }
    public void setDishCategory(String dishCategory) { this.dishCategory = dishCategory; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public void setCostRate(BigDecimal costRate) { this.costRate = costRate; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setDescription(String description) { this.description = description; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setStatus(String status) { this.status = status; }
    public void setTags(String tags) { this.tags = tags; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public void setMainIngredient(String mainIngredient) { this.mainIngredient = mainIngredient; }
    public void setMainIngredientType(String mainIngredientType) { this.mainIngredientType = mainIngredientType; }
    public void setEnglishName(String englishName) { this.englishName = englishName; }
    public void setCookingTime(Integer cookingTime) { this.cookingTime = cookingTime; }
    public void setServings(Integer servings) { this.servings = servings; }
    public void setSpicyLevel(Integer spicyLevel) { this.spicyLevel = spicyLevel; }
    public void setFestiveName(String festiveName) { this.festiveName = festiveName; }
    public void setUsageType(String usageType) { this.usageType = usageType; }
    public void setDishIntro(String dishIntro) { this.dishIntro = dishIntro; }
    public void setTiktokRecommend(String tiktokRecommend) { this.tiktokRecommend = tiktokRecommend; }
}

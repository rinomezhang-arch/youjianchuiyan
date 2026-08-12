/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.dto.PackageDTO
 */
package com.youjian.banquet.dto;

import java.math.BigDecimal;

public class PackageDTO {
    private String packageId;
    private String storeId;
    private String packageName;
    private String englishName;
    private String usageLocation;
    private String category;
    private BigDecimal price;
    private BigDecimal packageTotalPrice;
    private BigDecimal originalPrice;
    private BigDecimal discount;
    private String description;
    private String imageUrl;
    private Integer minGuests;
    private Integer maxGuests;
    private String status;
    private String tags;
    private Integer sortOrder;
    private String creator;
    private String createdAt;

    public String getPackageId() {
        return this.packageId;
    }

    public String getStoreId() {
        return this.storeId;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public String getCategory() {
        return this.category;
    }

    public BigDecimal getPrice() {
        return this.price;
    }

    public BigDecimal getOriginalPrice() {
        return this.originalPrice;
    }

    public String getDescription() {
        return this.description;
    }

    public String getImageUrl() {
        return this.imageUrl;
    }

    public Integer getMinGuests() {
        return this.minGuests;
    }

    public Integer getMaxGuests() {
        return this.maxGuests;
    }

    public String getStatus() {
        return this.status;
    }

    public String getTags() {
        return this.tags;
    }

    public Integer getSortOrder() {
        return this.sortOrder;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getEnglishName() {
        return this.englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public String getUsageLocation() {
        return this.usageLocation;
    }

    public void setUsageLocation(String usageLocation) {
        this.usageLocation = usageLocation;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getPackageTotalPrice() {
        return this.packageTotalPrice;
    }

    public void setPackageTotalPrice(BigDecimal packageTotalPrice) {
        this.packageTotalPrice = packageTotalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public BigDecimal getDiscount() {
        return this.discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setMinGuests(Integer minGuests) {
        this.minGuests = minGuests;
    }

    public void setMaxGuests(Integer maxGuests) {
        this.maxGuests = maxGuests;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getCreator() {
        return this.creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PackageDTO)) {
            return false;
        }
        PackageDTO other = (PackageDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$minGuests = this.getMinGuests();
        Integer other$minGuests = other.getMinGuests();
        if (this$minGuests == null ? other$minGuests != null : !((Object)this$minGuests).equals(other$minGuests)) {
            return false;
        }
        Integer this$maxGuests = this.getMaxGuests();
        Integer other$maxGuests = other.getMaxGuests();
        if (this$maxGuests == null ? other$maxGuests != null : !((Object)this$maxGuests).equals(other$maxGuests)) {
            return false;
        }
        Integer this$sortOrder = this.getSortOrder();
        Integer other$sortOrder = other.getSortOrder();
        if (this$sortOrder == null ? other$sortOrder != null : !((Object)this$sortOrder).equals(other$sortOrder)) {
            return false;
        }
        String this$packageId = this.getPackageId();
        String other$packageId = other.getPackageId();
        if (this$packageId == null ? other$packageId != null : !this$packageId.equals(other$packageId)) {
            return false;
        }
        String this$storeId = this.getStoreId();
        String other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) {
            return false;
        }
        String this$packageName = this.getPackageName();
        String other$packageName = other.getPackageName();
        if (this$packageName == null ? other$packageName != null : !this$packageName.equals(other$packageName)) {
            return false;
        }
        String this$englishName = this.getEnglishName();
        String other$englishName = other.getEnglishName();
        if (this$englishName == null ? other$englishName != null : !this$englishName.equals(other$englishName)) {
            return false;
        }
        String this$usageLocation = this.getUsageLocation();
        String other$usageLocation = other.getUsageLocation();
        if (this$usageLocation == null ? other$usageLocation != null : !this$usageLocation.equals(other$usageLocation)) {
            return false;
        }
        String this$category = this.getCategory();
        String other$category = other.getCategory();
        if (this$category == null ? other$category != null : !this$category.equals(other$category)) {
            return false;
        }
        BigDecimal this$price = this.getPrice();
        BigDecimal other$price = other.getPrice();
        if (this$price == null ? other$price != null : !((Object)this$price).equals(other$price)) {
            return false;
        }
        BigDecimal this$originalPrice = this.getOriginalPrice();
        BigDecimal other$originalPrice = other.getOriginalPrice();
        if (this$originalPrice == null ? other$originalPrice != null : !((Object)this$originalPrice).equals(other$originalPrice)) {
            return false;
        }
        BigDecimal this$packageTotalPrice = this.getPackageTotalPrice();
        BigDecimal other$packageTotalPrice = other.getPackageTotalPrice();
        if (this$packageTotalPrice == null ? other$packageTotalPrice != null : !((Object)this$packageTotalPrice).equals(other$packageTotalPrice)) {
            return false;
        }
        BigDecimal this$discount = this.getDiscount();
        BigDecimal other$discount = other.getDiscount();
        if (this$discount == null ? other$discount != null : !((Object)this$discount).equals(other$discount)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) {
            return false;
        }
        String this$imageUrl = this.getImageUrl();
        String other$imageUrl = other.getImageUrl();
        if (this$imageUrl == null ? other$imageUrl != null : !this$imageUrl.equals(other$imageUrl)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$tags = this.getTags();
        String other$tags = other.getTags();
        return !(this$tags == null ? other$tags != null : !this$tags.equals(other$tags));
    }

    protected boolean canEqual(Object other) {
        return other instanceof PackageDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $minGuests = this.getMinGuests();
        result = result * 59 + ($minGuests == null ? 43 : ((Object)$minGuests).hashCode());
        Integer $maxGuests = this.getMaxGuests();
        result = result * 59 + ($maxGuests == null ? 43 : ((Object)$maxGuests).hashCode());
        Integer $sortOrder = this.getSortOrder();
        result = result * 59 + ($sortOrder == null ? 43 : ((Object)$sortOrder).hashCode());
        String $packageId = this.getPackageId();
        result = result * 59 + ($packageId == null ? 43 : $packageId.hashCode());
        String $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : $storeId.hashCode());
        String $packageName = this.getPackageName();
        result = result * 59 + ($packageName == null ? 43 : $packageName.hashCode());
        String $englishName = this.getEnglishName();
        result = result * 59 + ($englishName == null ? 43 : $englishName.hashCode());
        String $usageLocation = this.getUsageLocation();
        result = result * 59 + ($usageLocation == null ? 43 : $usageLocation.hashCode());
        String $category = this.getCategory();
        result = result * 59 + ($category == null ? 43 : $category.hashCode());
        BigDecimal $price = this.getPrice();
        result = result * 59 + ($price == null ? 43 : ((Object)$price).hashCode());
        BigDecimal $originalPrice = this.getOriginalPrice();
        result = result * 59 + ($originalPrice == null ? 43 : ((Object)$originalPrice).hashCode());
        BigDecimal $packageTotalPrice = this.getPackageTotalPrice();
        result = result * 59 + ($packageTotalPrice == null ? 43 : ((Object)$packageTotalPrice).hashCode());
        BigDecimal $discount = this.getDiscount();
        result = result * 59 + ($discount == null ? 43 : ((Object)$discount).hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        String $imageUrl = this.getImageUrl();
        result = result * 59 + ($imageUrl == null ? 43 : $imageUrl.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $tags = this.getTags();
        result = result * 59 + ($tags == null ? 43 : $tags.hashCode());
        return result;
    }

    public String toString() {
        return "PackageDTO(packageId=" + this.getPackageId() + ", storeId=" + this.getStoreId() + ", packageName=" + this.getPackageName() + ", englishName=" + this.getEnglishName() + ", usageLocation=" + this.getUsageLocation() + ", category=" + this.getCategory() + ", price=" + String.valueOf(this.getPrice()) + ", packageTotalPrice=" + String.valueOf(this.getPackageTotalPrice()) + ", originalPrice=" + String.valueOf(this.getOriginalPrice()) + ", discount=" + String.valueOf(this.getDiscount()) + ", description=" + this.getDescription() + ", imageUrl=" + this.getImageUrl() + ", minGuests=" + this.getMinGuests() + ", maxGuests=" + this.getMaxGuests() + ", status=" + this.getStatus() + ", tags=" + this.getTags() + ", sortOrder=" + this.getSortOrder() + ")";
    }

    public PackageDTO() {
    }

    public PackageDTO(String packageId, String storeId, String packageName, String englishName, String usageLocation, String category, BigDecimal price, BigDecimal packageTotalPrice, BigDecimal originalPrice, BigDecimal discount, String description, String imageUrl, Integer minGuests, Integer maxGuests, String status, String tags, Integer sortOrder) {
        this.packageId = packageId;
        this.storeId = storeId;
        this.packageName = packageName;
        this.englishName = englishName;
        this.usageLocation = usageLocation;
        this.category = category;
        this.price = price;
        this.packageTotalPrice = packageTotalPrice;
        this.originalPrice = originalPrice;
        this.discount = discount;
        this.description = description;
        this.imageUrl = imageUrl;
        this.minGuests = minGuests;
        this.maxGuests = maxGuests;
        this.status = status;
        this.tags = tags;
        this.sortOrder = sortOrder;
    }
}


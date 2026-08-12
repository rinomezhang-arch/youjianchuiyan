/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.TemplateDishRel
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.Table
 */
package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="template_dish_rel")
public class TemplateDishRel {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Integer id;
    @Column(name="template_id")
    private Integer templateId;
    @Column(name="dish_id")
    private String dishId;
    @Column(name="store_id")
    private Long storeId;
    @Column(name="menu_category_id")
    private Integer menuCategoryId;
    @Column(name="special_price", precision=10, scale=2)
    private BigDecimal specialPrice;
    @Column(name="sort_order")
    private Integer sortOrder;
    @Column(name="create_time")
    private LocalDateTime createdAt;

    public Integer getId() {
        return this.id;
    }

    public Integer getTemplateId() {
        return this.templateId;
    }

    public String getDishId() {
        return this.dishId;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public Integer getMenuCategoryId() {
        return this.menuCategoryId;
    }

    public BigDecimal getSpecialPrice() {
        return this.specialPrice;
    }

    public Integer getSortOrder() {
        return this.sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setTemplateId(Integer templateId) {
        this.templateId = templateId;
    }

    public void setDishId(String dishId) {
        this.dishId = dishId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public void setMenuCategoryId(Integer menuCategoryId) {
        this.menuCategoryId = menuCategoryId;
    }

    public void setSpecialPrice(BigDecimal specialPrice) {
        this.specialPrice = specialPrice;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TemplateDishRel)) {
            return false;
        }
        TemplateDishRel other = (TemplateDishRel)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$id = this.getId();
        Integer other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Integer this$templateId = this.getTemplateId();
        Integer other$templateId = other.getTemplateId();
        if (this$templateId == null ? other$templateId != null : !((Object)this$templateId).equals(other$templateId)) {
            return false;
        }
        Long this$storeId = this.getStoreId();
        Long other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !((Object)this$storeId).equals(other$storeId)) {
            return false;
        }
        Integer this$menuCategoryId = this.getMenuCategoryId();
        Integer other$menuCategoryId = other.getMenuCategoryId();
        if (this$menuCategoryId == null ? other$menuCategoryId != null : !((Object)this$menuCategoryId).equals(other$menuCategoryId)) {
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
        BigDecimal this$specialPrice = this.getSpecialPrice();
        BigDecimal other$specialPrice = other.getSpecialPrice();
        if (this$specialPrice == null ? other$specialPrice != null : !((Object)this$specialPrice).equals(other$specialPrice)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        return !(this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof TemplateDishRel;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Integer $templateId = this.getTemplateId();
        result = result * 59 + ($templateId == null ? 43 : ((Object)$templateId).hashCode());
        Long $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : ((Object)$storeId).hashCode());
        Integer $menuCategoryId = this.getMenuCategoryId();
        result = result * 59 + ($menuCategoryId == null ? 43 : ((Object)$menuCategoryId).hashCode());
        Integer $sortOrder = this.getSortOrder();
        result = result * 59 + ($sortOrder == null ? 43 : ((Object)$sortOrder).hashCode());
        String $dishId = this.getDishId();
        result = result * 59 + ($dishId == null ? 43 : $dishId.hashCode());
        BigDecimal $specialPrice = this.getSpecialPrice();
        result = result * 59 + ($specialPrice == null ? 43 : ((Object)$specialPrice).hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        return result;
    }

    public String toString() {
        return "TemplateDishRel(id=" + this.getId() + ", templateId=" + this.getTemplateId() + ", dishId=" + this.getDishId() + ", storeId=" + this.getStoreId() + ", menuCategoryId=" + this.getMenuCategoryId() + ", specialPrice=" + String.valueOf(this.getSpecialPrice()) + ", sortOrder=" + this.getSortOrder() + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ")";
    }

    public TemplateDishRel() {
    }

    public TemplateDishRel(Integer id, Integer templateId, String dishId, Long storeId, Integer menuCategoryId, BigDecimal specialPrice, Integer sortOrder, LocalDateTime createdAt) {
        this.id = id;
        this.templateId = templateId;
        this.dishId = dishId;
        this.storeId = storeId;
        this.menuCategoryId = menuCategoryId;
        this.specialPrice = specialPrice;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
    }
}


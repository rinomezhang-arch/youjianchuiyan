/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.dto.RecipeItemDTO
 *  com.youjian.banquet.dto.RecipeSaveDTO
 */
package com.youjian.banquet.dto;

import com.youjian.banquet.dto.RecipeItemDTO;
import java.util.List;

public class RecipeSaveDTO {
    private String dishId;
    private String storeId;
    private List<RecipeItemDTO> items;

    public String getDishId() {
        return this.dishId;
    }

    public String getStoreId() {
        return this.storeId;
    }

    public List<RecipeItemDTO> getItems() {
        return this.items;
    }

    public void setDishId(String dishId) {
        this.dishId = dishId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public void setItems(List<RecipeItemDTO> items) {
        this.items = items;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof RecipeSaveDTO)) {
            return false;
        }
        RecipeSaveDTO other = (RecipeSaveDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        String this$dishId = this.getDishId();
        String other$dishId = other.getDishId();
        if (this$dishId == null ? other$dishId != null : !this$dishId.equals(other$dishId)) {
            return false;
        }
        String this$storeId = this.getStoreId();
        String other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) {
            return false;
        }
        List this$items = this.getItems();
        List other$items = other.getItems();
        return !(this$items == null ? other$items != null : !((Object)this$items).equals(other$items));
    }

    protected boolean canEqual(Object other) {
        return other instanceof RecipeSaveDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $dishId = this.getDishId();
        result = result * 59 + ($dishId == null ? 43 : $dishId.hashCode());
        String $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : $storeId.hashCode());
        List $items = this.getItems();
        result = result * 59 + ($items == null ? 43 : ((Object)$items).hashCode());
        return result;
    }

    public String toString() {
        return "RecipeSaveDTO(dishId=" + this.getDishId() + ", storeId=" + this.getStoreId() + ", items=" + String.valueOf(this.getItems()) + ")";
    }

    public RecipeSaveDTO() {
    }

    public RecipeSaveDTO(String dishId, String storeId, List<RecipeItemDTO> items) {
        this.dishId = dishId;
        this.storeId = storeId;
        this.items = items;
    }
}


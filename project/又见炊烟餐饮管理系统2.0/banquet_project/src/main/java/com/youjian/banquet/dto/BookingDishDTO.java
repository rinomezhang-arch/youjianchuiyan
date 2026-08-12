/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.dto.BookingDishDTO
 */
package com.youjian.banquet.dto;

import java.math.BigDecimal;

public class BookingDishDTO {
    private String dishId;
    private String dishName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private String notes;

    public String getDishId() {
        return this.dishId;
    }

    public String getDishName() {
        return this.dishName;
    }

    public Integer getQuantity() {
        return this.quantity;
    }

    public BigDecimal getUnitPrice() {
        return this.unitPrice;
    }

    public BigDecimal getSubtotal() {
        return this.subtotal;
    }

    public String getNotes() {
        return this.notes;
    }

    public void setDishId(String dishId) {
        this.dishId = dishId;
    }

    public void setDishName(String dishName) {
        this.dishName = dishName;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof BookingDishDTO)) {
            return false;
        }
        BookingDishDTO other = (BookingDishDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$quantity = this.getQuantity();
        Integer other$quantity = other.getQuantity();
        if (this$quantity == null ? other$quantity != null : !((Object)this$quantity).equals(other$quantity)) {
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
        BigDecimal this$unitPrice = this.getUnitPrice();
        BigDecimal other$unitPrice = other.getUnitPrice();
        if (this$unitPrice == null ? other$unitPrice != null : !((Object)this$unitPrice).equals(other$unitPrice)) {
            return false;
        }
        BigDecimal this$subtotal = this.getSubtotal();
        BigDecimal other$subtotal = other.getSubtotal();
        if (this$subtotal == null ? other$subtotal != null : !((Object)this$subtotal).equals(other$subtotal)) {
            return false;
        }
        String this$notes = this.getNotes();
        String other$notes = other.getNotes();
        return !(this$notes == null ? other$notes != null : !this$notes.equals(other$notes));
    }

    protected boolean canEqual(Object other) {
        return other instanceof BookingDishDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $quantity = this.getQuantity();
        result = result * 59 + ($quantity == null ? 43 : ((Object)$quantity).hashCode());
        String $dishId = this.getDishId();
        result = result * 59 + ($dishId == null ? 43 : $dishId.hashCode());
        String $dishName = this.getDishName();
        result = result * 59 + ($dishName == null ? 43 : $dishName.hashCode());
        BigDecimal $unitPrice = this.getUnitPrice();
        result = result * 59 + ($unitPrice == null ? 43 : ((Object)$unitPrice).hashCode());
        BigDecimal $subtotal = this.getSubtotal();
        result = result * 59 + ($subtotal == null ? 43 : ((Object)$subtotal).hashCode());
        String $notes = this.getNotes();
        result = result * 59 + ($notes == null ? 43 : $notes.hashCode());
        return result;
    }

    public String toString() {
        return "BookingDishDTO(dishId=" + this.getDishId() + ", dishName=" + this.getDishName() + ", quantity=" + this.getQuantity() + ", unitPrice=" + String.valueOf(this.getUnitPrice()) + ", subtotal=" + String.valueOf(this.getSubtotal()) + ", notes=" + this.getNotes() + ")";
    }

    public BookingDishDTO() {
    }

    public BookingDishDTO(String dishId, String dishName, Integer quantity, BigDecimal unitPrice, BigDecimal subtotal, String notes) {
        this.dishId = dishId;
        this.dishName = dishName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
        this.notes = notes;
    }
}


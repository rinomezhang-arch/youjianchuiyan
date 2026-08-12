package com.youjian.banquet.projection;

import java.math.BigDecimal;

public interface InventorySummaryProjection {
    String getIngredientId();
    String getIngredientName();
    BigDecimal getTotalQuantity();
    BigDecimal getTotalCost();
    BigDecimal getAvgUnitPrice();
}

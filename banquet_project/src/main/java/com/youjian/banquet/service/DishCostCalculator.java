package com.youjian.banquet.service;

import com.youjian.banquet.entity.DishRecipe;
import com.youjian.banquet.entity.IngredientMaster;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** One costing formula shared by recipe recalculation and cost cards. Does not set selling prices. */
public final class DishCostCalculator {
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private DishCostCalculator() {}

    public static BigDecimal calculateLine(DishRecipe recipe, IngredientMaster ingredient) {
        if (ingredient == null) throw new IllegalArgumentException("配方引用的原料不存在，请补全原料档案");
        BigDecimal quantity = recipe.getQuantity();
        if (quantity == null || quantity.signum() <= 0 || quantity.stripTrailingZeros().scale() > 3)
            throw new IllegalArgumentException("配方用量必须大于0，最多三位小数");
        BigDecimal price = ingredient.getUnitPrice();
        if (price == null || price.signum() < 0) throw new IllegalArgumentException("原料缺少有效采购单价");
        String purchaseUnit = clean(ingredient.getPurchaseUnit(), ingredient.getUnit());
        String usageUnit = clean(ingredient.getUsageUnit(), purchaseUnit);
        String recipeUnit = clean(recipe.getUnit(), usageUnit);
        if (purchaseUnit == null || recipeUnit == null) throw new IllegalArgumentException("请补全原料采购单位和配方使用单位");
        BigDecimal conversion;
        if (Objects.equals(recipeUnit, purchaseUnit)) conversion = BigDecimal.ONE;
        else if (Objects.equals(recipeUnit, usageUnit)) {
            conversion = ingredient.getConversionRate();
            if (conversion == null || conversion.signum() <= 0) throw new IllegalArgumentException("采购单位与配方单位不同，请填写有效换算率");
        } else throw new IllegalArgumentException("配方单位与原料档案不一致，请先确认单位换算");
        BigDecimal yield = recipe.getYieldRate();
        if (yield == null && recipe.getWastageRate() != null) yield = HUNDRED.subtract(recipe.getWastageRate());
        if (yield == null) yield = ingredient.getYieldRate();
        if (yield == null || yield.signum() <= 0 || yield.compareTo(new BigDecimal("999.99")) > 0)
            throw new IllegalArgumentException("请填写有效出成率；零出成率不能计算净料成本");
        // A measured yield may exceed 100% for hydrated ingredients. Do not count yield and wastage twice.
        BigDecimal netPrice = price.multiply(HUNDRED).divide(conversion.multiply(yield), 8, RoundingMode.HALF_UP);
        BigDecimal lineCost = netPrice.multiply(quantity).setScale(4, RoundingMode.HALF_UP);
        recipe.setIngredientName(ingredient.getIngredientName());
        recipe.setUnit(recipeUnit);
        recipe.setUnitPrice(netPrice);
        recipe.setNetUnitPrice(netPrice);
        recipe.setTotalCost(lineCost);
        recipe.setLastEntryDate(ingredient.getLastEntryDate());
        return lineCost;
    }

    public static BigDecimal costRate(BigDecimal cost, BigDecimal sellingPrice) {
        if (sellingPrice == null || sellingPrice.signum() <= 0) return null;
        BigDecimal rate = cost.multiply(HUNDRED).divide(sellingPrice, 2, RoundingMode.HALF_UP);
        if (rate.compareTo(new BigDecimal("999.99")) > 0)
            throw new IllegalArgumentException("成本率超出字段范围，请核对售价、原料单价和单位换算");
        return rate;
    }

    private static String clean(String value, String fallback) {
        String result = value == null || value.isBlank() ? fallback : value;
        return result == null || result.isBlank() ? null : result.trim();
    }
}

package com.youjian.banquet.controller;

import com.youjian.banquet.config.ApiResponse;
import com.youjian.banquet.dto.IngredientDTO;
import com.youjian.banquet.service.IngredientService;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping(value={"/api/ingredients", "/api/menu-api/ingredients"})
@CrossOrigin
public class IngredientController {
    @Autowired
    private IngredientService ingredientService;

    @Autowired
    private JdbcTemplate jdbc;

    @GetMapping
    public ApiResponse<List<IngredientDTO>> getAllIngredients(@RequestParam String storeId) {
        try {
            UserContext.assertStoreAccess(storeId);
            final long sid = Long.parseLong(storeId);
            final String sql =
                "SELECT i.ingredient_id, i.store_id, i.ingredient_name, i.category, i.unit, " +
                "i.current_stock, i.min_stock, i.unit_price, i.supplier_id, i.status, " +
                "s.supplier_name AS supplierName " +
                "FROM ingredient_master i LEFT JOIN supplier_master s " +
                "ON s.supplier_id = i.supplier_id AND s.store_id = i.store_id " +
                "WHERE i.store_id = ? ORDER BY i.ingredient_name LIMIT 500";
            List<IngredientDTO> list = jdbc.query(sql, new Object[]{sid}, (rs, row) -> {
                IngredientDTO d = new IngredientDTO();
                d.setIngredientId(rs.getString("ingredient_id"));
                d.setStoreId(String.valueOf(rs.getLong("store_id")));
                d.setIngredientName(rs.getString("ingredient_name"));
                d.setCategory(rs.getString("category"));
                d.setUnit(rs.getString("unit"));
                d.setCurrentStock(rs.getBigDecimal("current_stock"));
                d.setMinStock(rs.getBigDecimal("min_stock"));
                d.setUnitPrice(rs.getBigDecimal("unit_price"));
                int sId = rs.getInt("supplier_id");
                d.setSupplierId(rs.wasNull() ? null : String.valueOf(sId));
                d.setStatus(rs.getString("status"));
                d.setSupplierName(rs.getString("supplierName"));
                return d;
            });
            return ApiResponse.success(list);
        } catch (Exception e) {
            return ApiResponse.success(Collections.emptyList());
        }
    }

    @GetMapping(value={"/{ingredientId}"})
    public ApiResponse<IngredientDTO> getIngredient(@PathVariable String ingredientId, @RequestParam String storeId) {
        try {
            UserContext.assertStoreAccess(storeId);
            return ApiResponse.success(this.ingredientService.getIngredient(ingredientId, storeId));
        } catch (Exception e) {
            return ApiResponse.success(null);
        }
    }

    @GetMapping(value={"/low-stock"})
    public ApiResponse<List<IngredientDTO>> getLowStockIngredients(@RequestParam String storeId) {
        try {
            UserContext.assertStoreAccess(storeId);
            return ApiResponse.success(this.ingredientService.getLowStockIngredients(storeId));
        } catch (Exception e) {
            return ApiResponse.success(Collections.emptyList());
        }
    }

    @PostMapping
    public ApiResponse<IngredientDTO> createIngredient(@RequestBody IngredientDTO dto) {
        try {
            if (dto.getStoreId() != null) {
                UserContext.assertStoreAccess(dto.getStoreId());
            } else if (!UserContext.isGeneralManager()) {
                Long current = UserContext.currentStoreId();
                if (current == null || current == 0L) throw new IllegalArgumentException("缺少 storeId 参数");
                dto.setStoreId(String.valueOf(current));
            }
            return ApiResponse.success(this.ingredientService.createIngredient(dto));
        } catch (Exception e) {
            return ApiResponse.success(dto);
        }
    }

    @PutMapping(value={"/{ingredientId}"})
    public ApiResponse<IngredientDTO> updateIngredient(@PathVariable String ingredientId, @RequestParam String storeId, @RequestBody IngredientDTO dto) {
        try {
            UserContext.assertStoreAccess(storeId);
            if (dto.getStoreId() != null && !dto.getStoreId().equals(storeId)) UserContext.assertStoreAccess(dto.getStoreId());
            return ApiResponse.success(this.ingredientService.updateIngredient(ingredientId, storeId, dto));
        } catch (Exception e) {
            return ApiResponse.success(dto);
        }
    }

    @DeleteMapping(value={"/{ingredientId}"})
    public ApiResponse<Void> deleteIngredient(@PathVariable String ingredientId, @RequestParam String storeId) {
        try {
            UserContext.assertStoreAccess(storeId);
            this.ingredientService.deleteIngredient(ingredientId, storeId);
        } catch (Exception ignore) {}
        return ApiResponse.success();
    }
}

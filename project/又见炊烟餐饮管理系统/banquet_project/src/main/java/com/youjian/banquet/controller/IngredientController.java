/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.youjian.banquet.config.ApiResponse
 *  com.youjian.banquet.controller.IngredientController
 *  com.youjian.banquet.dto.IngredientDTO
 *  com.youjian.banquet.service.IngredientService
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.web.bind.annotation.CrossOrigin
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.PutMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.youjian.banquet.controller;

import com.youjian.banquet.config.ApiResponse;
import com.youjian.banquet.dto.IngredientDTO;
import com.youjian.banquet.service.IngredientService;
import com.youjian.banquet.util.UserContext;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/menu-api/ingredients"})
@CrossOrigin
public class IngredientController {
    @Autowired
    private IngredientService ingredientService;

    @GetMapping
    public ApiResponse<List<IngredientDTO>> getAllIngredients(@RequestParam String storeId) {
        UserContext.assertStoreAccess(storeId);
        return ApiResponse.success(this.ingredientService.getAllIngredients(storeId));
    }

    @GetMapping(value={"/{ingredientId}"})
    public ApiResponse<IngredientDTO> getIngredient(@PathVariable String ingredientId, @RequestParam String storeId) {
        UserContext.assertStoreAccess(storeId);
        return ApiResponse.success(this.ingredientService.getIngredient(ingredientId, storeId));
    }

    @GetMapping(value={"/low-stock"})
    public ApiResponse<List<IngredientDTO>> getLowStockIngredients(@RequestParam String storeId) {
        UserContext.assertStoreAccess(storeId);
        return ApiResponse.success(this.ingredientService.getLowStockIngredients(storeId));
    }

    @PostMapping
    public ApiResponse<IngredientDTO> createIngredient(@RequestBody IngredientDTO dto) {
        if (dto.getStoreId() != null) {
            UserContext.assertStoreAccess(dto.getStoreId());
        } else if (!UserContext.isGeneralManager()) {
            Long current = UserContext.currentStoreId();
            if (current == null || current == 0L) {
                throw new IllegalArgumentException("缺少 storeId 参数");
            }
            dto.setStoreId(String.valueOf(current));
        }
        return ApiResponse.success(this.ingredientService.createIngredient(dto));
    }

    @PutMapping(value={"/{ingredientId}"})
    public ApiResponse<IngredientDTO> updateIngredient(@PathVariable String ingredientId, @RequestParam String storeId, @RequestBody IngredientDTO dto) {
        UserContext.assertStoreAccess(storeId);
        if (dto.getStoreId() != null && !dto.getStoreId().equals(storeId)) {
            UserContext.assertStoreAccess(dto.getStoreId());
        }
        return ApiResponse.success(this.ingredientService.updateIngredient(ingredientId, storeId, dto));
    }

    @DeleteMapping(value={"/{ingredientId}"})
    public ApiResponse<Void> deleteIngredient(@PathVariable String ingredientId, @RequestParam String storeId) {
        UserContext.assertStoreAccess(storeId);
        this.ingredientService.deleteIngredient(ingredientId, storeId);
        return ApiResponse.success();
    }
}

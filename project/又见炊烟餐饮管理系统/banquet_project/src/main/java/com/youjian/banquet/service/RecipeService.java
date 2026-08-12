/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.dto.RecipeItemDTO
 *  com.youjian.banquet.dto.RecipeSaveDTO
 *  com.youjian.banquet.entity.DishRecipe
 *  com.youjian.banquet.repository.DishRecipeRepository
 *  com.youjian.banquet.repository.IngredientMasterRepository
 *  com.youjian.banquet.service.RecipeService
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.youjian.banquet.service;

import com.youjian.banquet.dto.RecipeItemDTO;
import com.youjian.banquet.dto.RecipeSaveDTO;
import com.youjian.banquet.entity.DishRecipe;
import com.youjian.banquet.repository.DishRecipeRepository;
import com.youjian.banquet.repository.IngredientMasterRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecipeService {
    @Autowired
    private DishRecipeRepository dishRecipeRepository;
    @Autowired
    private IngredientMasterRepository ingredientMasterRepository;

    public List<RecipeItemDTO> getRecipe(String dishId, String storeId) {
        Long storeIdLong = Long.parseLong(storeId);
        return this.dishRecipeRepository.findByDishIdAndStoreId(dishId, storeIdLong).stream().map(r -> {
            RecipeItemDTO dto = new RecipeItemDTO();
            dto.setIngredientId(r.getIngredientId());
            dto.setQuantity(r.getQuantity());
            dto.setUnit(r.getUnit());
            dto.setNotes(r.getNotes());
            this.ingredientMasterRepository.findByIngredientIdAndStoreId(r.getIngredientId(), storeIdLong).ifPresent(i -> dto.setIngredientName(i.getIngredientName()));
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void saveRecipe(RecipeSaveDTO saveDTO) {
        String dishId = saveDTO.getDishId();
        Long storeIdLong = Long.parseLong(saveDTO.getStoreId());
        this.dishRecipeRepository.deleteByDishIdAndStoreId(dishId, storeIdLong);
        if (saveDTO.getItems() != null) {
            for (RecipeItemDTO item : saveDTO.getItems()) {
                DishRecipe recipe = new DishRecipe();
                recipe.setDishId(dishId);
                recipe.setStoreId(storeIdLong);
                recipe.setIngredientId(item.getIngredientId());
                recipe.setQuantity(item.getQuantity());
                recipe.setUnit(item.getUnit());
                recipe.setNotes(item.getNotes());
                this.dishRecipeRepository.save(recipe);
            }
        }
    }

    public List<String> getDishesWithRecipe(String storeId) {
        return this.dishRecipeRepository.findDistinctDishIdsByStoreId(Long.valueOf(Long.parseLong(storeId)));
    }

    @Transactional
    public void recalcAll(String storeId) {
        List recipes = this.dishRecipeRepository.findByStoreId(Long.valueOf(Long.parseLong(storeId)));
    }
}


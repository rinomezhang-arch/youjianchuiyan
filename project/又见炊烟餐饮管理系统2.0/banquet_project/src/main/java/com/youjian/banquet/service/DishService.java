package com.youjian.banquet.service;

import com.youjian.banquet.dto.DishDTO;
import com.youjian.banquet.entity.DishMaster;
import com.youjian.banquet.repository.DishMasterRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DishService {
    @Autowired
    private DishMasterRepository dishMasterRepository;

    public List<DishDTO> getAllDishes(String storeId) {
        return this.dishMasterRepository.findByStoreId(Long.valueOf(Long.parseLong(storeId))).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<DishDTO> getDishesByStatus(String storeId, String status) {
        Integer isActive = "active".equals(status) ? 1 : 0;
        return this.dishMasterRepository.findByStoreIdAndIsActive(Long.valueOf(Long.parseLong(storeId)), isActive).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<DishDTO> getDishesByCategory(String storeId, String category) {
        return this.dishMasterRepository.findByStoreIdAndDishCategory(Long.valueOf(Long.parseLong(storeId)), category).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public DishDTO getDish(String dishId, String storeId) {
        return this.dishMasterRepository.findByDishIdAndStoreId(dishId, Long.valueOf(Long.parseLong(storeId))).map(this::toDTO).orElseThrow(() -> new IllegalArgumentException("Dish not found: " + dishId));
    }

    public List<DishDTO> searchDishes(String storeId, String keyword) {
        return this.dishMasterRepository.searchByKeyword(Long.valueOf(Long.parseLong(storeId)), keyword).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<String> getCategories(String storeId) {
        return this.dishMasterRepository.findDistinctCategoriesByStoreId(Long.valueOf(Long.parseLong(storeId)));
    }

    @Transactional
    public DishDTO createDish(DishDTO dto) {
        DishMaster dish = new DishMaster();
        dish.setDishId(dto.getDishId() != null ? dto.getDishId() : "DISH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        dish.setStoreId(Long.valueOf(Long.parseLong(dto.getStoreId())));
        dish.setDishName(dto.getDishName());
        dish.setDishCategory(dto.getDishCategory() != null ? dto.getDishCategory() : dto.getCategory());
        dish.setSpicyLevel(dto.getSpicyLevel());
        dish.setMainIngredientType(dto.getMainIngredientType());
        dish.setMainIngredient(dto.getMainIngredient());
        dish.setEnglishName(dto.getEnglishName());
        dish.setCostPrice(dto.getCostPrice());
        dish.setSalePrice(dto.getSalePrice() != null ? dto.getSalePrice() : dto.getPrice());
        dish.setCostRate(dto.getCostRate());
        dish.setCookingTime(dto.getCookingTime());
        dish.setServings(dto.getServings());
        dish.setFestiveName(dto.getFestiveName());
        dish.setUsageType(dto.getUsageType());
        dish.setImageUrl(dto.getImageUrl());
        dish.setDishIntro(dto.getDishIntro());
        dish.setTiktokRecommend(dto.getTiktokRecommend());
        dish.setIsActive("active".equals(dto.getStatus()) ? 1 : 0);
        dish.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        this.dishMasterRepository.save(dish);
        return this.toDTO(dish);
    }

    @Transactional
    public DishDTO updateDish(String dishId, String storeId, DishDTO dto) {
        DishMaster dish = this.dishMasterRepository.findByDishIdAndStoreId(dishId, Long.valueOf(Long.parseLong(storeId))).orElseThrow(() -> new IllegalArgumentException("Dish not found: " + dishId));
        if (dto.getDishName() != null) dish.setDishName(dto.getDishName());
        if (dto.getDishCategory() != null) dish.setDishCategory(dto.getDishCategory());
        else if (dto.getCategory() != null) dish.setDishCategory(dto.getCategory());
        if (dto.getSpicyLevel() != null) dish.setSpicyLevel(dto.getSpicyLevel());
        if (dto.getMainIngredientType() != null) dish.setMainIngredientType(dto.getMainIngredientType());
        if (dto.getMainIngredient() != null) dish.setMainIngredient(dto.getMainIngredient());
        if (dto.getEnglishName() != null) dish.setEnglishName(dto.getEnglishName());
        if (dto.getCostPrice() != null) dish.setCostPrice(dto.getCostPrice());
        if (dto.getSalePrice() != null) dish.setSalePrice(dto.getSalePrice());
        else if (dto.getPrice() != null) dish.setSalePrice(dto.getPrice());
        if (dto.getCostRate() != null) dish.setCostRate(dto.getCostRate());
        if (dto.getCookingTime() != null) dish.setCookingTime(dto.getCookingTime());
        if (dto.getServings() != null) dish.setServings(dto.getServings());
        if (dto.getFestiveName() != null) dish.setFestiveName(dto.getFestiveName());
        if (dto.getUsageType() != null) dish.setUsageType(dto.getUsageType());
        if (dto.getImageUrl() != null) dish.setImageUrl(dto.getImageUrl());
        if (dto.getDishIntro() != null) dish.setDishIntro(dto.getDishIntro());
        if (dto.getTiktokRecommend() != null) dish.setTiktokRecommend(dto.getTiktokRecommend());
        if (dto.getStatus() != null) dish.setIsActive("active".equals(dto.getStatus()) ? 1 : 0);
        if (dto.getSortOrder() != null) dish.setSortOrder(dto.getSortOrder());
        this.dishMasterRepository.save(dish);
        return this.toDTO(dish);
    }

    @Transactional
    public void deleteDish(String dishId, String storeId) {
        this.dishMasterRepository.deleteByDishIdAndStoreId(dishId, Long.valueOf(Long.parseLong(storeId)));
    }

    private DishDTO toDTO(DishMaster e) {
        DishDTO dto = new DishDTO();
        dto.setDishId(e.getDishId());
        dto.setStoreId(String.valueOf(e.getStoreId()));
        dto.setDishName(e.getDishName());
        dto.setDishCategory(e.getDishCategory());
        dto.setCategory(e.getDishCategory());
        dto.setSpicyLevel(e.getSpicyLevel());
        dto.setMainIngredientType(e.getMainIngredientType());
        dto.setMainIngredient(e.getMainIngredient());
        dto.setEnglishName(e.getEnglishName());
        dto.setCostPrice(e.getCostPrice());
        dto.setSalePrice(e.getSalePrice());
        dto.setPrice(e.getSalePrice());
        dto.setCostRate(e.getCostRate());
        dto.setCookingTime(e.getCookingTime());
        dto.setServings(e.getServings());
        dto.setFestiveName(e.getFestiveName());
        dto.setUsageType(e.getUsageType());
        dto.setImageUrl(e.getImageUrl());
        dto.setDishIntro(e.getDishIntro());
        dto.setTiktokRecommend(e.getTiktokRecommend());
        dto.setStatus(e.getIsActive() != null && e.getIsActive() == 1 ? "active" : "inactive");
        dto.setSortOrder(e.getSortOrder());
        return dto;
    }
}

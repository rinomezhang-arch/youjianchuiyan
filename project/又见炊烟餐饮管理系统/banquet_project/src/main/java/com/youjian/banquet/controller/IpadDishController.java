package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.DishMaster;
import com.youjian.banquet.entity.MenuCategory;
import com.youjian.banquet.entity.PackageMaster;
import com.youjian.banquet.entity.PackageDishDetail;
import com.youjian.banquet.entity.BanquetTemplate;
import com.youjian.banquet.entity.TemplateDishRel;
import com.youjian.banquet.repository.DishMasterRepository;
import com.youjian.banquet.repository.MenuCategoryRepository;
import com.youjian.banquet.repository.PackageMasterRepository;
import com.youjian.banquet.repository.PackageDishDetailRepository;
import com.youjian.banquet.repository.BanquetTemplateRepository;
import com.youjian.banquet.repository.TemplateDishRelRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ipad")
@CrossOrigin(origins = "*")
public class IpadDishController {

    @Autowired
    private DishMasterRepository dishRepo;

    @Autowired
    private MenuCategoryRepository categoryRepo;

    @Autowired
    private PackageMasterRepository packageRepo;

    @Autowired
    private PackageDishDetailRepository packageDishRepo;

    @Autowired
    private BanquetTemplateRepository templateRepo;

    @Autowired
    private TemplateDishRelRepository templateDishRepo;

    @GetMapping("/dish/category")
    public Result<List<Map<String, Object>>> getDishCategories(HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");
            List<MenuCategory> categories = categoryRepo.findAll();

            List<Map<String, Object>> result = categories.stream().map(cat -> {
                Map<String, Object> map = new HashMap<>();
                map.put("category_id", cat.getId());
                map.put("dish_category", cat.getCategoryName());
                map.put("sort_order", cat.getSortOrder());
                return map;
            }).sorted(Comparator.comparingInt(a -> (Integer) a.get("sort_order")))
              .collect(Collectors.toList());

            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "查询菜品分类失败：" + e.getMessage());
        }
    }

    @GetMapping("/dish/list")
    public Result<List<Map<String, Object>>> getDishList(
            @RequestParam(required = false) String category_id,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");
            List<DishMaster> dishes = dishRepo.findByStoreId(storeId);

            // 关键词搜索
            if (keyword != null && !keyword.isEmpty()) {
                dishes = dishes.stream()
                        .filter(d -> d.getDishName() != null && d.getDishName().contains(keyword))
                        .collect(Collectors.toList());
            }

            // 过滤未激活菜品
            dishes = dishes.stream()
                    .filter(d -> d.getIsActive() == null || d.getIsActive() == 1)
                    .collect(Collectors.toList());

            List<Map<String, Object>> result = dishes.stream().map(dish -> {
                Map<String, Object> map = new HashMap<>();
                map.put("dish_id", dish.getDishId());
                map.put("dish_name", dish.getDishName());
                map.put("english_name", dish.getEnglishName());
                map.put("dish_category", dish.getDishCategory());
                map.put("sale_price", dish.getSalePrice());
                map.put("cost_price", dish.getCostPrice());
                map.put("spicy_level", dish.getSpicyLevel());
                map.put("cooking_time", dish.getCookingTime());
                map.put("servings", dish.getServings());
                map.put("main_ingredient", dish.getMainIngredient());
                map.put("usage_type", dish.getUsageType());
                map.put("dish_intro", dish.getDishIntro());
                map.put("tiktok_recommend", dish.getTiktokRecommend());
                map.put("birthday_name", dish.getBirthdayName());
                map.put("wedding_name", dish.getWeddingName());
                map.put("house_move_name", dish.getHouseMoveName());
                map.put("promotion_name", dish.getPromotionName());
                map.put("reunion_name", dish.getReunionName());
                map.put("thanksgiving_name", dish.getThanksgivingName());
                map.put("year_end_name", dish.getYearEndName());
                map.put("baby_born_name", dish.getBabyBornName());
                map.put("festive_name", dish.getFestiveName());
                map.put("is_active", dish.getIsActive());
                map.put("image_url", dish.getImageUrl());
                map.put("sort_order", dish.getSortOrder());
                return map;
            }).collect(Collectors.toList());

            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "查询菜品列表失败：" + e.getMessage());
        }
    }

    @GetMapping("/dish/detail/{dish_id}")
    public Result<Map<String, Object>> getDishDetail(
            @PathVariable String dish_id,
            HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");
            DishMaster dish = dishRepo.findByDishIdAndStoreId(dish_id, storeId)
                    .orElseThrow(() -> new RuntimeException("菜品不存在"));

            Map<String, Object> data = new HashMap<>();
            data.put("dish_id", dish.getDishId());
            data.put("dish_name", dish.getDishName());
            data.put("english_name", dish.getEnglishName());
            data.put("dish_category", dish.getDishCategory());
            data.put("sale_price", dish.getSalePrice());
            data.put("cost_price", dish.getCostPrice());
            data.put("spicy_level", dish.getSpicyLevel());
            data.put("cooking_time", dish.getCookingTime());
            data.put("servings", dish.getServings());
            data.put("main_ingredient", dish.getMainIngredient());
            data.put("usage_type", dish.getUsageType());
            data.put("dish_intro", dish.getDishIntro());
            data.put("tiktok_recommend", dish.getTiktokRecommend());
            data.put("birthday_name", dish.getBirthdayName());
            data.put("wedding_name", dish.getWeddingName());
            data.put("house_move_name", dish.getHouseMoveName());
            data.put("promotion_name", dish.getPromotionName());
            data.put("reunion_name", dish.getReunionName());
            data.put("thanksgiving_name", dish.getThanksgivingName());
            data.put("year_end_name", dish.getYearEndName());
            data.put("baby_born_name", dish.getBabyBornName());
            data.put("festive_name", dish.getFestiveName());
            data.put("image_url", dish.getImageUrl());
            data.put("is_active", dish.getIsActive());

            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "查询菜品详情失败：" + e.getMessage());
        }
    }

    @GetMapping("/package/list")
    public Result<List<Map<String, Object>>> getPackageList(HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");
            List<PackageMaster> packages = packageRepo.findByStoreIdOrderBySortOrderAsc(String.valueOf(storeId));

            List<Map<String, Object>> result = packages.stream().map(pkg -> {
                Map<String, Object> map = new HashMap<>();
                map.put("package_id", pkg.getPackageId());
                map.put("package_name", pkg.getPackageName());
                map.put("package_price", pkg.getPackageTotalPrice());

                // 查询套餐菜品明细
                List<PackageDishDetail> details = packageDishRepo.findByPackageIdAndStoreId(pkg.getPackageId(), storeId);
                List<Map<String, Object>> dishList = details.stream().map(detail -> {
                    Map<String, Object> dishMap = new HashMap<>();
                    dishMap.put("dish_id", detail.getDishId());
                    dishMap.put("dish_name", detail.getCustomName());
                    dishMap.put("dish_quantity", detail.getDishQuantity());
                    return dishMap;
                }).collect(Collectors.toList());

                map.put("dish_list", dishList);
                return map;
            }).collect(Collectors.toList());

            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "查询套餐列表失败：" + e.getMessage());
        }
    }

    @GetMapping("/template/list")
    public Result<List<Map<String, Object>>> getTemplateList(
            @RequestParam(required = false) String banquet_type_id,
            HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");
            List<BanquetTemplate> templates = templateRepo.findAll();

            // 按宴会类型筛选
            if (banquet_type_id != null && !banquet_type_id.isEmpty()) {
                templates = templates.stream()
                        .filter(t -> banquet_type_id.equals(t.getTemplateType()))
                        .collect(Collectors.toList());
            }

            List<Map<String, Object>> result = templates.stream().map(template -> {
                Map<String, Object> map = new HashMap<>();
                map.put("template_id", template.getId());
                map.put("template_name", template.getTemplateName());
                map.put("total_price", template.getBasePrice());

                // 查询模板菜品关联
                List<TemplateDishRel> rels = templateDishRepo.findByTemplateIdAndStoreId(template.getId(), storeId);
                List<Map<String, Object>> dishList = rels.stream().map(rel -> {
                    Map<String, Object> dishMap = new HashMap<>();
                    dishMap.put("dish_id", rel.getDishId());
                    dishMap.put("dish_name", "");
                    dishMap.put("dish_quantity", 1);
                    dishMap.put("unit_price", rel.getSpecialPrice());
                    return dishMap;
                }).collect(Collectors.toList());

                map.put("dish_list", dishList);
                return map;
            }).collect(Collectors.toList());

            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "查询宴席模板失败：" + e.getMessage());
        }
    }

    @GetMapping("/dish/search")
    public Result<List<Map<String, Object>>> searchDishes(
            @RequestParam String keyword,
            HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");
            List<DishMaster> dishes = dishRepo.findByStoreId(storeId);

            // 关键词搜索
            dishes = dishes.stream()
                    .filter(d -> d.getDishName() != null && d.getDishName().contains(keyword))
                    .filter(d -> d.getIsActive() == null || d.getIsActive() == 1)
                    .collect(Collectors.toList());

            List<Map<String, Object>> result = dishes.stream().map(dish -> {
                Map<String, Object> map = new HashMap<>();
                map.put("dish_id", dish.getDishId());
                map.put("dish_name", dish.getDishName());
                map.put("english_name", dish.getEnglishName());
                map.put("dish_category", dish.getDishCategory());
                map.put("sale_price", dish.getSalePrice());
                map.put("spicy_level", dish.getSpicyLevel());
                map.put("cooking_time", dish.getCookingTime());
                map.put("servings", dish.getServings());
                map.put("main_ingredient", dish.getMainIngredient());
                map.put("usage_type", dish.getUsageType());
                map.put("dish_intro", dish.getDishIntro());
                map.put("tiktok_recommend", dish.getTiktokRecommend());
                map.put("birthday_name", dish.getBirthdayName());
                map.put("wedding_name", dish.getWeddingName());
                map.put("house_move_name", dish.getHouseMoveName());
                map.put("promotion_name", dish.getPromotionName());
                map.put("reunion_name", dish.getReunionName());
                map.put("thanksgiving_name", dish.getThanksgivingName());
                map.put("year_end_name", dish.getYearEndName());
                map.put("baby_born_name", dish.getBabyBornName());
                map.put("festive_name", dish.getFestiveName());
                map.put("image_url", dish.getImageUrl());
                return map;
            }).collect(Collectors.toList());

            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "搜索菜品失败：" + e.getMessage());
        }
    }
}

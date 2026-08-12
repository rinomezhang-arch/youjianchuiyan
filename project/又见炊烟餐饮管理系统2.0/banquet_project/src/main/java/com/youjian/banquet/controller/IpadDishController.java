package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BtDishInfo;
import com.youjian.banquet.entity.BtDishType;
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
import com.youjian.banquet.service.BtDishInfoService;
import com.youjian.banquet.service.BtDishTypeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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

    @Autowired
    private BtDishInfoService btDishInfoService;

    @Autowired
    private BtDishTypeService btDishTypeService;

    private Map<String, Object> btDishToIpadMap(BtDishInfo b, int sortOrder) {
        Map<String, Object> map = new HashMap<>();
        map.put("dish_id", "bt_" + b.getId());
        map.put("dish_name", b.getCaipinmingcheng());
        map.put("english_name", "");
        map.put("dish_category", b.getCaipinleixing());
        map.put("sale_price", b.getPrice() != null ? b.getPrice().doubleValue() : 0.0);
        map.put("cost_price", 0.0);
        map.put("spicy_level", b.getKouwei());
        map.put("cooking_time", b.getYujishijian());
        map.put("servings", 1);
        map.put("main_ingredient", "");
        map.put("usage_type", "零点");
        map.put("dish_intro", b.getCaipinjieshao());
        map.put("tiktok_recommend", "");
        map.put("birthday_name", "");
        map.put("wedding_name", "");
        map.put("house_move_name", "");
        map.put("promotion_name", "");
        map.put("reunion_name", "");
        map.put("thanksgiving_name", "");
        map.put("year_end_name", "");
        map.put("baby_born_name", "");
        map.put("festive_name", "");
        map.put("is_active", 1);
        map.put("image_url", b.getTupian());
        map.put("sort_order", sortOrder);
        return map;
    }

    @GetMapping("/dish/category")
    public Result<List<Map<String, Object>>> getDishCategories(HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");
            List<MenuCategory> categories = categoryRepo.findAll();

            List<Map<String, Object>> result = categories.stream().map(cat -> {
                Map<String, Object> map = new HashMap<>();
                map.put("category_id", cat.getId());
                map.put("dish_category", cat.getCategoryName());
                map.put("sort_order", cat.getSortOrder() != null ? cat.getSortOrder() : 0);
                return map;
            }).sorted(Comparator.comparingInt(a -> (Integer) a.get("sort_order")))
              .collect(Collectors.toList());

            Set<String> existNames = result.stream()
                    .map(m -> String.valueOf(m.get("dish_category")))
                    .collect(Collectors.toSet());

            List<BtDishType> btTypes = btDishTypeService.listAll(storeId);
            int sortBase = result.size();
            for (BtDishType t : btTypes) {
                if (t.getCaipinleixing() == null) continue;
                if (existNames.contains(t.getCaipinleixing())) continue;
                Map<String, Object> map = new HashMap<>();
                map.put("category_id", "bt_" + t.getId());
                map.put("dish_category", t.getCaipinleixing());
                map.put("sort_order", sortBase++);
                result.add(map);
            }

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
            Set<String> seenIds = new HashSet<>();

            List<Map<String, Object>> result = new ArrayList<>();

            List<Map<String, Object>> primaryResult = dishes.stream()
                    .filter(d -> d.getIsActive() == null || d.getIsActive() == 1)
                    .filter(d -> {
                        if (keyword == null || keyword.isEmpty()) return true;
                        return d.getDishName() != null && d.getDishName().contains(keyword);
                    })
                    .map(dish -> {
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
                        map.put("sort_order", dish.getSortOrder() != null ? dish.getSortOrder() : 999);
                        seenIds.add(String.valueOf(dish.getDishId()));
                        return map;
                    }).collect(Collectors.toList());
            result.addAll(primaryResult);

            try {
                Page<BtDishInfo> btPage = btDishInfoService.page(1, 500, storeId);
                int order = 1000;
                for (BtDishInfo b : btPage.getContent()) {
                    if (b.getCaipinmingcheng() == null) continue;
                    String catFilter = category_id != null && category_id.startsWith("bt_") ? null : category_id;
                    if (catFilter != null && !catFilter.isEmpty() && !catFilter.equals(b.getCaipinleixing())) continue;
                    if (keyword != null && !keyword.isEmpty()
                            && (b.getCaipinmingcheng() == null || !b.getCaipinmingcheng().contains(keyword))) continue;
                    Map<String, Object> mapped = btDishToIpadMap(b, order++);
                    if (seenIds.add(String.valueOf(mapped.get("dish_id")))) {
                        result.add(mapped);
                    }
                }
            } catch (Exception btEx) {
            }

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
            if (dish_id != null && dish_id.startsWith("bt_")) {
                Long bid = Long.parseLong(dish_id.substring(3));
                BtDishInfo b = btDishInfoService.getById(bid)
                        .orElseThrow(() -> new RuntimeException("菜品不存在"));
                return Result.success(btDishToIpadMap(b, 0));
            }
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
            List<PackageMaster> packages = packageRepo.findByStoreIdOrderBySortOrderAsc(storeId);

            List<Map<String, Object>> result = packages.stream().map(pkg -> {
                Map<String, Object> map = new HashMap<>();
                map.put("package_id", pkg.getPackageId());
                map.put("package_name", pkg.getPackageName());
                map.put("package_price", pkg.getPackageTotalPrice());

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
            Set<String> seenIds = new HashSet<>();

            List<Map<String, Object>> result = dishes.stream()
                    .filter(d -> d.getDishName() != null && d.getDishName().contains(keyword))
                    .filter(d -> d.getIsActive() == null || d.getIsActive() == 1)
                    .map(dish -> {
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
                        seenIds.add(String.valueOf(dish.getDishId()));
                        return map;
                    }).collect(Collectors.toList());

            try {
                Page<BtDishInfo> btPage = btDishInfoService.searchWithPrice(keyword, null, null, null, 1, 200);
                int order = 1000;
                for (BtDishInfo b : btPage.getContent()) {
                    Map<String, Object> mapped = btDishToIpadMap(b, order++);
                    if (seenIds.add(String.valueOf(mapped.get("dish_id")))) {
                        result.add(mapped);
                    }
                }
            } catch (Exception btEx) {
            }

            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "搜索菜品失败：" + e.getMessage());
        }
    }
}

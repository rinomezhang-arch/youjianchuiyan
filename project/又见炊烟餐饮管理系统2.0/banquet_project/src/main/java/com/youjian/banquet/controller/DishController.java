/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.config.ApiResponse
 *  com.youjian.banquet.controller.DishController
 *  com.youjian.banquet.dto.DishDTO
 *  com.youjian.banquet.service.DishService
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
import com.youjian.banquet.dto.DishDTO;
import com.youjian.banquet.service.DishService;
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
@RequestMapping(value={"/api/dishes"})
@CrossOrigin
public class DishController {
    @Autowired
    private DishService dishService;

    /**
     * 查询接口门店过滤：店长强制查询本店，总经理可查询任意门店。
     * <p>GET 请求由 {@code StoreDataScopeAspect} 已填充 UserContext 并设置 dataScopeAll 标记，
     * 本方法据此覆盖客户端传入的 storeId，防止店长越权查询其他门店菜品。
     */
    private String resolveQueryStoreId(String requestStoreId) {
        Long currentStoreId = UserContext.getCurrentStoreId();
        if (!UserContext.isDataScopeAll() && currentStoreId != null) {
            return String.valueOf(currentStoreId);
        }
        return requestStoreId;
    }

    @GetMapping
    public ApiResponse<List<DishDTO>> getAllDishes(@RequestParam String storeId) {
        storeId = resolveQueryStoreId(storeId);
        return ApiResponse.success(this.dishService.getAllDishes(storeId));
    }

    @GetMapping(value={"/{dishId}"})
    public ApiResponse<DishDTO> getDish(@PathVariable String dishId, @RequestParam String storeId) {
        storeId = resolveQueryStoreId(storeId);
        return ApiResponse.success(this.dishService.getDish(dishId, storeId));
    }

    @GetMapping(value={"/categories"})
    public ApiResponse<List<String>> getCategories(@RequestParam String storeId) {
        storeId = resolveQueryStoreId(storeId);
        return ApiResponse.success(this.dishService.getCategories(storeId));
    }

    @GetMapping(value={"/search"})
    public ApiResponse<List<DishDTO>> searchDishes(@RequestParam String storeId, @RequestParam String keyword) {
        storeId = resolveQueryStoreId(storeId);
        return ApiResponse.success(this.dishService.searchDishes(storeId, keyword));
    }

    @PostMapping
    public ApiResponse<DishDTO> createDish(@RequestBody DishDTO dto) {
        UserContext.ensureDataScopeFromStoreId();
        if (!UserContext.isDataScopeAll()) {
            return ApiResponse.error(403, "无权限：菜品基础信息（名称/成本/售价/配方）仅总经理可新增");
        }
        return ApiResponse.success(this.dishService.createDish(dto));
    }

    @PutMapping(value={"/{dishId}"})
    public ApiResponse<DishDTO> updateDish(@PathVariable String dishId, @RequestParam String storeId, @RequestBody DishDTO dto) {
        UserContext.ensureDataScopeFromStoreId();
        if (!UserContext.isDataScopeAll()) {
            return ApiResponse.error(403, "无权限：菜品基础信息（名称/成本/售价/配方）仅总经理可编辑");
        }
        return ApiResponse.success(this.dishService.updateDish(dishId, storeId, dto));
    }

    @DeleteMapping(value={"/{dishId}"})
    public ApiResponse<Void> deleteDish(@PathVariable String dishId, @RequestParam(defaultValue = "1") String storeId) {
        UserContext.ensureDataScopeFromStoreId();
        if (!UserContext.isDataScopeAll()) {
            return ApiResponse.error(403, "无权限：菜品基础信息仅总经理可删除");
        }
        // 存在性检查：删除不存在的菜品返回 404，避免静默删除 0 行或连接异常
        try {
            this.dishService.getDish(dishId, storeId);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(404, "菜品不存在: " + dishId);
        }
        this.dishService.deleteDish(dishId, storeId);
        return ApiResponse.success();
    }
}


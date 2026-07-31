/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.config.ApiResponse
 *  com.youjian.banquet.controller.PackageController
 *  com.youjian.banquet.dto.PackageDTO
 *  com.youjian.banquet.service.PackageService
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
import com.youjian.banquet.dto.PackageDTO;
import com.youjian.banquet.entity.PackageDishDetail;
import com.youjian.banquet.service.PackageService;
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
@RequestMapping(value={"/api/packages"})
@CrossOrigin
public class PackageController {
    @Autowired
    private PackageService packageService;

    /**
     * 查询接口门店过滤：店长强制查询本店，总经理可查询任意门店。
     * <p>GET 请求由 {@code StoreDataScopeAspect} 已填充 UserContext 并设置 dataScopeAll 标记，
     * 本方法据此覆盖客户端传入的 storeId，防止店长越权查询其他门店套餐。
     */
    private String resolveQueryStoreId(String requestStoreId) {
        Long currentStoreId = UserContext.getCurrentStoreId();
        if (!UserContext.isDataScopeAll() && currentStoreId != null) {
            return String.valueOf(currentStoreId);
        }
        return requestStoreId;
    }

    @GetMapping
    public ApiResponse<List<PackageDTO>> getAllPackages(@RequestParam String storeId) {
        storeId = resolveQueryStoreId(storeId);
        return ApiResponse.success(this.packageService.getAllPackages(storeId));
    }

    @GetMapping(value={"/{packageId}"})
    public ApiResponse<PackageDTO> getPackage(@PathVariable String packageId, @RequestParam String storeId) {
        storeId = resolveQueryStoreId(storeId);
        return ApiResponse.success(this.packageService.getPackage(packageId, storeId));
    }

    @PostMapping
    public ApiResponse<PackageDTO> createPackage(@RequestBody PackageDTO dto) {
        UserContext.ensureDataScopeFromStoreId();
        if (!UserContext.isDataScopeAll()) {
            return ApiResponse.error(403, "无权限：套餐基础信息仅总经理可新增");
        }
        return ApiResponse.success(this.packageService.createPackage(dto));
    }

    @PutMapping(value={"/{packageId}"})
    public ApiResponse<PackageDTO> updatePackage(@PathVariable String packageId, @RequestParam String storeId, @RequestBody PackageDTO dto) {
        UserContext.ensureDataScopeFromStoreId();
        if (!UserContext.isDataScopeAll()) {
            return ApiResponse.error(403, "无权限：套餐基础信息仅总经理可编辑");
        }
        return ApiResponse.success(this.packageService.updatePackage(packageId, storeId, dto));
    }

    @DeleteMapping(value={"/{packageId}"})
    public ApiResponse<Void> deletePackage(@PathVariable String packageId, @RequestParam String storeId) {
        UserContext.ensureDataScopeFromStoreId();
        if (!UserContext.isDataScopeAll()) {
            return ApiResponse.error(403, "无权限：套餐基础信息仅总经理可删除");
        }
        this.packageService.deletePackage(packageId, storeId);
        return ApiResponse.success();
    }

    // 获取套餐菜品明细
    @GetMapping(value={"/{packageId}/dishes"})
    public ApiResponse<List<PackageDishDetail>> getPackageDishes(@PathVariable String packageId, @RequestParam String storeId) {
        storeId = resolveQueryStoreId(storeId);
        return ApiResponse.success(this.packageService.getPackageDishes(packageId, Long.valueOf(storeId)));
    }

    // 添加菜品到套餐
    @PostMapping(value={"/{packageId}/dishes"})
    public ApiResponse<PackageDishDetail> addDishToPackage(@PathVariable String packageId, @RequestParam String storeId, @RequestBody PackageDishDetail detail) {
        UserContext.ensureDataScopeFromStoreId();
        if (!UserContext.isDataScopeAll()) {
            return ApiResponse.error(403, "无权限：套餐菜品仅总经理可编辑");
        }
        return ApiResponse.success(this.packageService.addDishToPackage(packageId, Long.valueOf(storeId), detail.getDishId(), detail.getDishQuantity(), detail.getDishOrder(), detail.getCustomName(), detail.getNote()));
    }

    // 批量添加菜品到套餐
    @PostMapping(value={"/{packageId}/dishes/batch"})
    public ApiResponse<List<PackageDishDetail>> addDishesToPackage(@PathVariable String packageId, @RequestParam String storeId, @RequestBody List<PackageDishDetail> details) {
        UserContext.ensureDataScopeFromStoreId();
        if (!UserContext.isDataScopeAll()) {
            return ApiResponse.error(403, "无权限：套餐菜品仅总经理可编辑");
        }
        return ApiResponse.success(this.packageService.addDishesToPackage(packageId, Long.valueOf(storeId), details));
    }

    // 删除套餐中的菜品
    @DeleteMapping(value={"/{packageId}/dishes/{dishId}"})
    public ApiResponse<Void> removeDishFromPackage(@PathVariable String packageId, @PathVariable String dishId, @RequestParam String storeId) {
        UserContext.ensureDataScopeFromStoreId();
        if (!UserContext.isDataScopeAll()) {
            return ApiResponse.error(403, "无权限：套餐菜品仅总经理可编辑");
        }
        this.packageService.removeDishFromPackage(packageId, Long.valueOf(storeId), dishId);
        return ApiResponse.success();
    }
}


/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.youjian.banquet.config.ApiResponse
 *  com.youjian.banquet.controller.SupplierController
 *  com.youjian.banquet.dto.SupplierDTO
 *  com.youjian.banquet.service.SupplierService
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
import com.youjian.banquet.dto.SupplierDTO;
import com.youjian.banquet.service.SupplierService;
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
@RequestMapping(value={"/api/menu-api/suppliers"})
@CrossOrigin
public class SupplierController {
    @Autowired
    private SupplierService supplierService;

    @GetMapping
    public ApiResponse<List<SupplierDTO>> getAllSuppliers(@RequestParam String storeId) {
        // 店长只读：可查看本店供应商
        UserContext.assertStoreAccess(storeId);
        return ApiResponse.success(this.supplierService.getAllSuppliers(storeId));
    }

    @GetMapping(value={"/{supplierId}"})
    public ApiResponse<SupplierDTO> getSupplier(@PathVariable String supplierId, @RequestParam String storeId) {
        // 店长只读：可查看本店供应商
        UserContext.assertStoreAccess(storeId);
        return ApiResponse.success(this.supplierService.getSupplier(supplierId, storeId));
    }

    @PostMapping
    public ApiResponse<SupplierDTO> createSupplier(@RequestBody SupplierDTO dto) {
        // 仅总经理可新增供应商
        UserContext.assertGeneralManager();
        if (dto.getStoreId() != null) {
            UserContext.assertStoreAccess(dto.getStoreId());
        }
        return ApiResponse.success(this.supplierService.createSupplier(dto));
    }

    @PutMapping(value={"/{supplierId}"})
    public ApiResponse<SupplierDTO> updateSupplier(@PathVariable String supplierId, @RequestParam String storeId, @RequestBody SupplierDTO dto) {
        // 仅总经理可修改供应商
        UserContext.assertGeneralManager();
        UserContext.assertStoreAccess(storeId);
        return ApiResponse.success(this.supplierService.updateSupplier(supplierId, storeId, dto));
    }

    @DeleteMapping(value={"/{supplierId}"})
    public ApiResponse<Void> deleteSupplier(@PathVariable String supplierId, @RequestParam String storeId) {
        // 仅总经理可删除供应商
        UserContext.assertGeneralManager();
        UserContext.assertStoreAccess(storeId);
        this.supplierService.deleteSupplier(supplierId, storeId);
        return ApiResponse.success();
    }
}

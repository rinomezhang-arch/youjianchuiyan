package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.UserAddress;
import com.youjian.banquet.service.UserAddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户地址控制器
 */
@RestController
@RequestMapping("/api/dish/address")
@CrossOrigin(origins = "*")
public class UserAddressController {

    @Autowired
    private UserAddressService userAddressService;

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(defaultValue = "id") String sortField,
                                            @RequestParam(defaultValue = "desc") String sortOrder,
                                            @RequestParam(required = false) Long storeId) {
        Page<UserAddress> pageResult = userAddressService.page(page, size, sortField, sortOrder, storeId);
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "size", size
        ));
    }

    @GetMapping("/list")
    public Result<List<UserAddress>> list(@RequestParam(required = false) Long storeId) {
        return Result.success(userAddressService.listAll(storeId));
    }

    @GetMapping("/info/{id}")
    public Result<UserAddress> info(@PathVariable Long id) {
        return userAddressService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "用户地址不存在"));
    }

    @PostMapping("/save")
    public Result<UserAddress> save(@RequestBody UserAddress entity) {
        return Result.success(userAddressService.save(entity));
    }

    @PutMapping("/update")
    public Result<UserAddress> update(@RequestBody UserAddress entity) {
        return Result.success(userAddressService.update(entity));
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        userAddressService.deleteBatch(List.of(ids));
        return Result.success("删除成功");
    }

    @GetMapping("/remind/{columnName}/{type}")
    public Result<Map<String, Object>> remindCount(@PathVariable String columnName,
                                                   @PathVariable String type,
                                                   @RequestParam Map<String, Object> map,
                                                   @RequestParam(required = false) Long storeId) {
        long count = userAddressService.count(storeId);
        return Result.success(Map.of("count", count));
    }
}
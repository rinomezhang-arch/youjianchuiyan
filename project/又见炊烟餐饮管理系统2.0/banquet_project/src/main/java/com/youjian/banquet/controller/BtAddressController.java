package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BtAddress;
import com.youjian.banquet.service.BtAddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 地址控制器
 * 来源：点餐系统 AddressController
 */
@RestController
@RequestMapping("/api/bt/address")
@CrossOrigin(origins = "*")
public class BtAddressController {

    @Autowired
    private BtAddressService btAddressService;

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(defaultValue = "id") String sortField,
                                            @RequestParam(defaultValue = "desc") String sortOrder,
                                            @RequestParam(required = false) Long userid,
                                            @RequestParam(required = false) Long storeId) {
        Page<BtAddress> pageResult;
        if (userid != null) {
            pageResult = btAddressService.pageByUser(userid, page, size, storeId);
        } else {
            pageResult = btAddressService.page(page, size, sortField, sortOrder, storeId);
        }
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "size", size
        ));
    }

    @GetMapping("/list")
    public Result<List<BtAddress>> list(@RequestParam(required = false) Long userid,
                                         @RequestParam(required = false) Long storeId) {
        if (userid != null) {
            return Result.success(btAddressService.pageByUser(userid, 1, 100, storeId).getContent());
        }
        return Result.success(List.of());
    }

    @GetMapping("/info/{id}")
    public Result<BtAddress> info(@PathVariable Long id) {
        return btAddressService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "地址不存在"));
    }

    @GetMapping("/detail/{id}")
    public Result<BtAddress> detail(@PathVariable Long id) {
        return btAddressService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "地址不存在"));
    }

    @GetMapping("/default")
    public Result<BtAddress> defaultAddress(@RequestParam Long userid) {
        return btAddressService.getDefaultAddress(userid)
                .map(Result::success)
                .orElse(Result.error(404, "没有默认地址"));
    }

    @PostMapping("/save")
    public Result<BtAddress> save(@RequestBody BtAddress entity) {
        return Result.success(btAddressService.save(entity));
    }

    @PostMapping("/add")
    public Result<BtAddress> add(@RequestBody BtAddress entity) {
        return Result.success(btAddressService.save(entity));
    }

    @PutMapping("/update")
    public Result<BtAddress> update(@RequestBody BtAddress entity) {
        return Result.success(btAddressService.update(entity));
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        btAddressService.deleteBatch(List.of(ids));
        return Result.success("删除成功");
    }

    @GetMapping("/remind/{columnName}/{type}")
    public Result<Map<String, Object>> remindCount(@PathVariable String columnName,
                                                   @PathVariable String type,
                                                   @RequestParam Map<String, Object> map,
                                                   @RequestParam(required = false) Long storeId) {
        long count = btAddressService.count(storeId);
        return Result.success(Map.of("count", count));
    }
}
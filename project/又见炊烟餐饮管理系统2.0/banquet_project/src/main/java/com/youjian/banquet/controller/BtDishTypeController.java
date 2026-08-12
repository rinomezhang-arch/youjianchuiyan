package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BtDishType;
import com.youjian.banquet.service.BtDishTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 菜品类型控制器
 * 来源：点餐系统 CaipinleixingController
 */
@RestController
@RequestMapping("/api/bt/dish-type")
@CrossOrigin(origins = "*")
public class BtDishTypeController {

    @Autowired
    private BtDishTypeService btDishTypeService;

    /**
     * 分页列表
     */
    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(defaultValue = "id") String sortField,
                                            @RequestParam(defaultValue = "desc") String sortOrder,
                                            @RequestParam(required = false) Long storeId) {
        Page<BtDishType> pageResult = btDishTypeService.page(page, size, sortField, sortOrder, storeId);
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "size", size
        ));
    }

    /**
     * 全部列表
     */
    @GetMapping("/list")
    public Result<List<BtDishType>> list(@RequestParam(required = false) Long storeId) {
        return Result.success(btDishTypeService.listAll(storeId));
    }

    /**
     * 详情
     */
    @GetMapping("/info/{id}")
    public Result<BtDishType> info(@PathVariable Long id) {
        return btDishTypeService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "菜品类型不存在"));
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<BtDishType> save(@RequestBody BtDishType entity) {
        return Result.success(btDishTypeService.save(entity));
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result<BtDishType> update(@RequestBody BtDishType entity) {
        return Result.success(btDishTypeService.update(entity));
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        btDishTypeService.deleteBatch(List.of(ids));
        return Result.success("删除成功");
    }

    /**
     * 提醒接口（按日期列统计）
     */
    @GetMapping("/remind/{columnName}/{type}")
    public Result<Map<String, Object>> remindCount(@PathVariable String columnName,
                                                   @PathVariable String type,
                                                   @RequestParam Map<String, Object> map,
                                                   @RequestParam(required = false) Long storeId) {
        long count = btDishTypeService.count(storeId);
        return Result.success(Map.of("count", count));
    }
}
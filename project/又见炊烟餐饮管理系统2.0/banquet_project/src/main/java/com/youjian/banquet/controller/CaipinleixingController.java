package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BtDishType;
import com.youjian.banquet.service.BtDishTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/caipinleixing")
@CrossOrigin(origins = "*")
public class CaipinleixingController {

    @Autowired
    private BtDishTypeService btDishTypeService;

    @RequestMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam Map<String, Object> params,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int limit,
                                            @RequestParam(defaultValue = "id") String sort,
                                            @RequestParam(defaultValue = "desc") String order,
                                            @RequestParam(required = false) Long storeId) {
        Page<BtDishType> pageResult = btDishTypeService.page(page, limit, sort, order, storeId);
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "limit", limit
        ));
    }

    @RequestMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam Map<String, Object> params,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int limit,
                                            @RequestParam(required = false) Long storeId) {
        Page<BtDishType> pageResult = btDishTypeService.page(page, limit, storeId);
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "limit", limit
        ));
    }

    @RequestMapping("/lists")
    public Result<List<BtDishType>> lists(BtDishType query) {
        return Result.success(btDishTypeService.listAll(null));
    }

    @RequestMapping("/query")
    public Result<BtDishType> query(BtDishType query) {
        if (query.getId() != null) {
            return btDishTypeService.getById(query.getId())
                    .map(Result::success)
                    .orElse(Result.error(404, "菜品类型不存在"));
        }
        return Result.success(null);
    }

    @RequestMapping("/info/{id}")
    public Result<BtDishType> info(@PathVariable("id") Long id) {
        return btDishTypeService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "菜品类型不存在"));
    }

    @RequestMapping("/detail/{id}")
    public Result<BtDishType> detail(@PathVariable("id") Long id) {
        return btDishTypeService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "菜品类型不存在"));
    }

    @RequestMapping("/save")
    public Result<BtDishType> save(@RequestBody BtDishType entity) {
        if (entity.getId() == null) {
            entity.setId(System.currentTimeMillis() + (long)(Math.random() * 1000));
        }
        return Result.success(btDishTypeService.save(entity));
    }

    @RequestMapping("/add")
    public Result<BtDishType> add(@RequestBody BtDishType entity) {
        if (entity.getId() == null) {
            entity.setId(System.currentTimeMillis() + (long)(Math.random() * 1000));
        }
        return Result.success(btDishTypeService.save(entity));
    }

    @RequestMapping("/update")
    public Result<BtDishType> update(@RequestBody BtDishType entity) {
        return Result.success(btDishTypeService.update(entity));
    }

    @RequestMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        btDishTypeService.deleteBatch(List.of(ids));
        return Result.success("删除成功");
    }

    @RequestMapping("/remind/{columnName}/{type}")
    public Result<Map<String, Object>> remindCount(@PathVariable("columnName") String columnName,
                                                   @PathVariable("type") String type,
                                                   @RequestParam Map<String, Object> map,
                                                   @RequestParam(required = false) Long storeId) {
        long count = btDishTypeService.count(storeId);
        return Result.success(Map.of("count", count));
    }
}

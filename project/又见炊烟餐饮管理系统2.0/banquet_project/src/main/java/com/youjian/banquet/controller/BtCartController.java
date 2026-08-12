package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BtCart;
import com.youjian.banquet.service.BtCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 购物车控制器
 * 来源：点餐系统 CartController
 */
@RestController
@RequestMapping("/api/bt/cart")
@CrossOrigin(origins = "*")
public class BtCartController {

    @Autowired
    private BtCartService btCartService;

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(defaultValue = "id") String sortField,
                                            @RequestParam(defaultValue = "desc") String sortOrder,
                                            @RequestParam(required = false) Long userid,
                                            @RequestParam(required = false) Long storeId) {
        Page<BtCart> pageResult;
        if (userid != null) {
            pageResult = btCartService.pageByUser(userid, page, size, storeId);
        } else {
            pageResult = btCartService.page(page, size, sortField, sortOrder, storeId);
        }
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "size", size
        ));
    }

    @GetMapping("/list")
    public Result<List<BtCart>> list(@RequestParam(required = false) Long userid) {
        if (userid != null) {
            return Result.success(btCartService.listByUser(userid));
        }
        return Result.success(List.of());
    }

    @GetMapping("/info/{id}")
    public Result<BtCart> info(@PathVariable Long id) {
        return btCartService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "购物车记录不存在"));
    }

    @GetMapping("/detail/{id}")
    public Result<BtCart> detail(@PathVariable Long id) {
        return btCartService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "购物车记录不存在"));
    }

    @PostMapping("/save")
    public Result<BtCart> save(@RequestBody BtCart entity) {
        return Result.success(btCartService.save(entity));
    }

    @PostMapping("/add")
    public Result<BtCart> add(@RequestBody BtCart entity) {
        return Result.success(btCartService.save(entity));
    }

    @PutMapping("/update")
    public Result<BtCart> update(@RequestBody BtCart entity) {
        return Result.success(btCartService.update(entity));
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        btCartService.deleteBatch(List.of(ids));
        return Result.success("删除成功");
    }

    @GetMapping("/remind/{columnName}/{type}")
    public Result<Map<String, Object>> remindCount(@PathVariable String columnName,
                                                   @PathVariable String type,
                                                   @RequestParam Map<String, Object> map,
                                                   @RequestParam(required = false) Long storeId) {
        long count = btCartService.count(storeId);
        return Result.success(Map.of("count", count));
    }
}
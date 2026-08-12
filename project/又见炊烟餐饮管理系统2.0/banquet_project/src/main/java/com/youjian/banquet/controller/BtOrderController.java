package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BtOrder;
import com.youjian.banquet.service.BtOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 订单控制器
 * 来源：点餐系统 OrdersController
 */
@RestController
@RequestMapping("/api/bt/order")
@CrossOrigin(origins = "*")
public class BtOrderController {

    @Autowired
    private BtOrderService btOrderService;

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(defaultValue = "id") String sortField,
                                            @RequestParam(defaultValue = "desc") String sortOrder,
                                            @RequestParam(required = false) Long userid,
                                            @RequestParam(required = false) Long storeId) {
        Page<BtOrder> pageResult;
        if (userid != null) {
            pageResult = btOrderService.pageByUser(userid, page, size, storeId);
        } else {
            pageResult = btOrderService.page(page, size, sortField, sortOrder, storeId);
        }
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "size", size
        ));
    }

    @GetMapping("/list")
    public Result<List<BtOrder>> list(@RequestParam(required = false) Long userid) {
        if (userid != null) {
            return Result.success(btOrderService.listByUser(userid));
        }
        return Result.success(List.of());
    }

    @GetMapping("/info/{id}")
    public Result<BtOrder> info(@PathVariable Long id) {
        return btOrderService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "订单不存在"));
    }

    @GetMapping("/detail/{id}")
    public Result<BtOrder> detail(@PathVariable Long id) {
        return btOrderService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "订单不存在"));
    }

    @PostMapping("/save")
    public Result<BtOrder> save(@RequestBody BtOrder entity) {
        return Result.success(btOrderService.save(entity));
    }

    @PostMapping("/add")
    public Result<BtOrder> add(@RequestBody BtOrder entity) {
        return Result.success(btOrderService.save(entity));
    }

    @PutMapping("/update")
    public Result<BtOrder> update(@RequestBody BtOrder entity) {
        return Result.success(btOrderService.update(entity));
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        btOrderService.deleteBatch(List.of(ids));
        return Result.success("删除成功");
    }

    @GetMapping("/remind/{columnName}/{type}")
    public Result<Map<String, Object>> remindCount(@PathVariable String columnName,
                                                   @PathVariable String type,
                                                   @RequestParam Map<String, Object> map,
                                                   @RequestParam(required = false) Long storeId) {
        long count = btOrderService.count(storeId);
        return Result.success(Map.of("count", count));
    }

    /**
     * 按值统计（X/Y 轴）
     */
    @GetMapping("/value/{xColumnName}/{yColumnName}")
    public Result<List<Map<String, Object>>> value(@PathVariable String yColumnName,
                                                   @PathVariable String xColumnName) {
        return Result.success(btOrderService.selectValue(xColumnName, yColumnName));
    }

    /**
     * 按时间类型统计
     */
    @GetMapping("/value/{xColumnName}/{yColumnName}/{timeStatType}")
    public Result<List<Map<String, Object>>> valueDay(@PathVariable String yColumnName,
                                                      @PathVariable String xColumnName,
                                                      @PathVariable String timeStatType) {
        return Result.success(btOrderService.selectTimeStatValue(xColumnName, yColumnName, timeStatType));
    }

    /**
     * 分组统计
     */
    @GetMapping("/group/{columnName}")
    public Result<List<Map<String, Object>>> group(@PathVariable String columnName) {
        return Result.success(btOrderService.selectGroup(columnName));
    }
}
package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BtOrder;
import com.youjian.banquet.service.BtOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrdersController {

    @Autowired
    private BtOrderService btOrderService;

    @RequestMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam Map<String, Object> params,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int limit,
                                            @RequestParam(defaultValue = "id") String sort,
                                            @RequestParam(defaultValue = "desc") String order,
                                            @RequestParam(required = false) Long userid,
                                            @RequestParam(required = false) Long storeId) {
        Page<BtOrder> pageResult;
        if (userid != null) {
            pageResult = btOrderService.pageByUser(userid, page, limit, storeId);
        } else {
            pageResult = btOrderService.page(page, limit, sort, order, storeId);
        }
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
                                            @RequestParam(required = false) Long userid,
                                            @RequestParam(required = false) Long storeId) {
        Page<BtOrder> pageResult;
        if (userid != null) {
            pageResult = btOrderService.pageByUser(userid, page, limit, storeId);
        } else {
            pageResult = btOrderService.page(page, limit, storeId);
        }
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "limit", limit
        ));
    }

    @RequestMapping("/lists")
    public Result<List<BtOrder>> lists(BtOrder query) {
        if (query.getUserid() != null) {
            return Result.success(btOrderService.listByUser(query.getUserid()));
        }
        return Result.success(List.of());
    }

    @RequestMapping("/query")
    public Result<BtOrder> query(BtOrder query) {
        if (query.getId() != null) {
            return btOrderService.getById(query.getId())
                    .map(Result::success)
                    .orElse(Result.error(404, "订单不存在"));
        }
        if (query.getOrderid() != null) {
            BtOrder o = btOrderService.getByOrderid(query.getOrderid());
            return o != null ? Result.success(o) : Result.error(404, "订单不存在");
        }
        return Result.success(null);
    }

    @RequestMapping("/info/{id}")
    public Result<BtOrder> info(@PathVariable("id") Long id) {
        return btOrderService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "订单不存在"));
    }

    @RequestMapping("/detail/{id}")
    public Result<BtOrder> detail(@PathVariable("id") Long id) {
        return btOrderService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "订单不存在"));
    }

    @RequestMapping("/save")
    public Result<BtOrder> save(@RequestBody BtOrder entity,
                                @RequestParam(required = false) Long userid) {
        if (entity.getId() == null) {
            entity.setId(System.currentTimeMillis() + (long)(Math.random() * 1000));
        }
        if (entity.getUserid() == null && userid != null) {
            entity.setUserid(userid);
        }
        return Result.success(btOrderService.save(entity));
    }

    @RequestMapping("/add")
    public Result<BtOrder> add(@RequestBody BtOrder entity) {
        if (entity.getId() == null) {
            entity.setId(System.currentTimeMillis() + (long)(Math.random() * 1000));
        }
        return Result.success(btOrderService.save(entity));
    }

    @RequestMapping("/update")
    public Result<BtOrder> update(@RequestBody BtOrder entity) {
        return Result.success(btOrderService.update(entity));
    }

    @RequestMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        btOrderService.deleteBatch(List.of(ids));
        return Result.success("删除成功");
    }

    @RequestMapping("/remind/{columnName}/{type}")
    public Result<Map<String, Object>> remindCount(@PathVariable("columnName") String columnName,
                                                   @PathVariable("type") String type,
                                                   @RequestParam Map<String, Object> map,
                                                   @RequestParam(required = false) Long storeId) {
        long count = btOrderService.count(storeId);
        return Result.success(Map.of("count", count));
    }

    @RequestMapping("/value/{xColumnName}/{yColumnName}")
    public Result<List<Map<String, Object>>> value(@PathVariable("yColumnName") String yColumnName,
                                                   @PathVariable("xColumnName") String xColumnName) {
        return Result.success(btOrderService.selectValue(xColumnName, yColumnName));
    }

    @RequestMapping("/value/{xColumnName}/{yColumnName}/{timeStatType}")
    public Result<List<Map<String, Object>>> valueDay(@PathVariable("yColumnName") String yColumnName,
                                                      @PathVariable("xColumnName") String xColumnName,
                                                      @PathVariable("timeStatType") String timeStatType) {
        return Result.success(btOrderService.selectTimeStatValue(xColumnName, yColumnName, timeStatType));
    }

    @RequestMapping("/group/{columnName}")
    public Result<List<Map<String, Object>>> group(@PathVariable("columnName") String columnName) {
        return Result.success(btOrderService.selectGroup(columnName));
    }
}

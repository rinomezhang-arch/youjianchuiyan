package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BtCart;
import com.youjian.banquet.service.BtCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
public class CartController {

    @Autowired
    private BtCartService btCartService;

    @RequestMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam Map<String, Object> params,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int limit,
                                            @RequestParam(defaultValue = "id") String sort,
                                            @RequestParam(defaultValue = "desc") String order,
                                            @RequestParam(required = false) Long userid,
                                            @RequestParam(required = false) Long storeId) {
        Page<BtCart> pageResult;
        if (userid != null) {
            pageResult = btCartService.pageByUser(userid, page, limit, storeId);
        } else {
            pageResult = btCartService.page(page, limit, sort, order, storeId);
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
        Page<BtCart> pageResult;
        if (userid != null) {
            pageResult = btCartService.pageByUser(userid, page, limit, storeId);
        } else {
            pageResult = btCartService.page(page, limit, storeId);
        }
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "limit", limit
        ));
    }

    @RequestMapping("/lists")
    public Result<List<BtCart>> lists(BtCart query) {
        if (query.getUserid() != null) {
            return Result.success(btCartService.listByUser(query.getUserid()));
        }
        return Result.success(List.of());
    }

    @RequestMapping("/query")
    public Result<BtCart> query(BtCart query) {
        if (query.getId() != null) {
            return btCartService.getById(query.getId())
                    .map(Result::success)
                    .orElse(Result.error(404, "购物车记录不存在"));
        }
        return Result.success(null);
    }

    @RequestMapping("/info/{id}")
    public Result<BtCart> info(@PathVariable("id") Long id) {
        return btCartService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "购物车记录不存在"));
    }

    @RequestMapping("/detail/{id}")
    public Result<BtCart> detail(@PathVariable("id") Long id) {
        return btCartService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "购物车记录不存在"));
    }

    @RequestMapping("/save")
    public Result<BtCart> save(@RequestBody BtCart entity,
                               @RequestParam(required = false) Long userid) {
        if (entity.getId() == null) {
            entity.setId(System.currentTimeMillis() + (long)(Math.random() * 1000));
        }
        if (entity.getUserid() == null && userid != null) {
            entity.setUserid(userid);
        }
        return Result.success(btCartService.save(entity));
    }

    @RequestMapping("/add")
    public Result<BtCart> add(@RequestBody BtCart entity) {
        if (entity.getId() == null) {
            entity.setId(System.currentTimeMillis() + (long)(Math.random() * 1000));
        }
        return Result.success(btCartService.save(entity));
    }

    @RequestMapping("/update")
    public Result<BtCart> update(@RequestBody BtCart entity) {
        return Result.success(btCartService.update(entity));
    }

    @RequestMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        btCartService.deleteBatch(List.of(ids));
        return Result.success("删除成功");
    }

    @RequestMapping("/remind/{columnName}/{type}")
    public Result<Map<String, Object>> remindCount(@PathVariable("columnName") String columnName,
                                                   @PathVariable("type") String type,
                                                   @RequestParam Map<String, Object> map,
                                                   @RequestParam(required = false) Long storeId) {
        long count = btCartService.count(storeId);
        return Result.success(Map.of("count", count));
    }
}

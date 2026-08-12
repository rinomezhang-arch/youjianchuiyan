package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BtTableInfo;
import com.youjian.banquet.service.BtTableInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/canzhuoxinxi")
@CrossOrigin(origins = "*")
public class CanzhuoxinxiController {

    @Autowired
    private BtTableInfoService btTableInfoService;

    @RequestMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam Map<String, Object> params,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int limit,
                                            @RequestParam(defaultValue = "id") String sort,
                                            @RequestParam(defaultValue = "desc") String order,
                                            @RequestParam(required = false) Long storeId) {
        Page<BtTableInfo> pageResult = btTableInfoService.page(page, limit, sort, order, storeId);
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
        Page<BtTableInfo> pageResult = btTableInfoService.page(page, limit, storeId);
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "limit", limit
        ));
    }

    @RequestMapping("/lists")
    public Result<List<BtTableInfo>> lists(BtTableInfo query) {
        return Result.success(btTableInfoService.listAll(null));
    }

    @RequestMapping("/query")
    public Result<BtTableInfo> query(BtTableInfo query) {
        if (query.getId() != null) {
            return btTableInfoService.getById(query.getId())
                    .map(Result::success)
                    .orElse(Result.error(404, "餐桌信息不存在"));
        }
        return Result.success(null);
    }

    @RequestMapping("/info/{id}")
    public Result<BtTableInfo> info(@PathVariable("id") Long id) {
        return btTableInfoService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "餐桌信息不存在"));
    }

    @RequestMapping("/detail/{id}")
    public Result<BtTableInfo> detail(@PathVariable("id") Long id) {
        return btTableInfoService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "餐桌信息不存在"));
    }

    @RequestMapping("/save")
    public Result<BtTableInfo> save(@RequestBody BtTableInfo entity) {
        if (entity.getId() == null) {
            entity.setId(System.currentTimeMillis() + (long)(Math.random() * 1000));
        }
        return Result.success(btTableInfoService.save(entity));
    }

    @RequestMapping("/add")
    public Result<BtTableInfo> add(@RequestBody BtTableInfo entity) {
        if (entity.getId() == null) {
            entity.setId(System.currentTimeMillis() + (long)(Math.random() * 1000));
        }
        return Result.success(btTableInfoService.save(entity));
    }

    @RequestMapping("/update")
    public Result<BtTableInfo> update(@RequestBody BtTableInfo entity) {
        return Result.success(btTableInfoService.update(entity));
    }

    @RequestMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        btTableInfoService.deleteBatch(List.of(ids));
        return Result.success("删除成功");
    }

    @RequestMapping("/remind/{columnName}/{type}")
    public Result<Map<String, Object>> remindCount(@PathVariable("columnName") String columnName,
                                                   @PathVariable("type") String type,
                                                   @RequestParam Map<String, Object> map,
                                                   @RequestParam(required = false) Long storeId) {
        long count = btTableInfoService.count(storeId);
        return Result.success(Map.of("count", count));
    }
}

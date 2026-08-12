package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BtTableInfo;
import com.youjian.banquet.service.BtTableInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 餐桌信息控制器
 * 来源：点餐系统 CanzhuoxinxiController
 */
@RestController
@RequestMapping("/api/bt/table-info")
@CrossOrigin(origins = "*")
public class BtTableInfoController {

    @Autowired
    private BtTableInfoService btTableInfoService;

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(defaultValue = "id") String sortField,
                                            @RequestParam(defaultValue = "desc") String sortOrder,
                                            @RequestParam(required = false) Long storeId) {
        Page<BtTableInfo> pageResult = btTableInfoService.page(page, size, sortField, sortOrder, storeId);
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "size", size
        ));
    }

    @GetMapping("/list")
    public Result<List<BtTableInfo>> list(@RequestParam(required = false) Long storeId) {
        return Result.success(btTableInfoService.listAll(storeId));
    }

    @GetMapping("/info/{id}")
    public Result<BtTableInfo> info(@PathVariable Long id) {
        return btTableInfoService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "餐桌信息不存在"));
    }

    @GetMapping("/detail/{id}")
    public Result<BtTableInfo> detail(@PathVariable Long id) {
        return btTableInfoService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "餐桌信息不存在"));
    }

    @PostMapping("/save")
    public Result<BtTableInfo> save(@RequestBody BtTableInfo entity) {
        return Result.success(btTableInfoService.save(entity));
    }

    @PostMapping("/add")
    public Result<BtTableInfo> add(@RequestBody BtTableInfo entity) {
        return Result.success(btTableInfoService.save(entity));
    }

    @PutMapping("/update")
    public Result<BtTableInfo> update(@RequestBody BtTableInfo entity) {
        return Result.success(btTableInfoService.update(entity));
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        btTableInfoService.deleteBatch(List.of(ids));
        return Result.success("删除成功");
    }

    @GetMapping("/remind/{columnName}/{type}")
    public Result<Map<String, Object>> remindCount(@PathVariable String columnName,
                                                   @PathVariable String type,
                                                   @RequestParam Map<String, Object> map,
                                                   @RequestParam(required = false) Long storeId) {
        long count = btTableInfoService.count(storeId);
        return Result.success(Map.of("count", count));
    }
}
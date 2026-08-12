package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.TableUsage;
import com.youjian.banquet.service.TableUsageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 餐桌使用记录控制器
 */
@RestController
@RequestMapping("/api/dish/table-usage")
@CrossOrigin(origins = "*")
public class TableUsageController {

    @Autowired
    private TableUsageService tableUsageService;

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(defaultValue = "id") String sortField,
                                            @RequestParam(defaultValue = "desc") String sortOrder,
                                            @RequestParam(required = false) Long storeId) {
        Page<TableUsage> pageResult = tableUsageService.page(page, size, sortField, sortOrder, storeId);
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "size", size
        ));
    }

    @GetMapping("/list")
    public Result<List<TableUsage>> list(@RequestParam(required = false) Long storeId) {
        return Result.success(tableUsageService.listAll(storeId));
    }

    @GetMapping("/info/{id}")
    public Result<TableUsage> info(@PathVariable Long id) {
        return tableUsageService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "餐桌使用记录不存在"));
    }

    @PostMapping("/save")
    public Result<TableUsage> save(@RequestBody TableUsage entity) {
        return Result.success(tableUsageService.save(entity));
    }

    @PutMapping("/update")
    public Result<TableUsage> update(@RequestBody TableUsage entity) {
        return Result.success(tableUsageService.update(entity));
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        tableUsageService.deleteBatch(List.of(ids));
        return Result.success("删除成功");
    }

    @GetMapping("/remind/{columnName}/{type}")
    public Result<Map<String, Object>> remindCount(@PathVariable String columnName,
                                                   @PathVariable String type,
                                                   @RequestParam Map<String, Object> map,
                                                   @RequestParam(required = false) Long storeId) {
        long count = tableUsageService.count(storeId);
        return Result.success(Map.of("count", count));
    }
}
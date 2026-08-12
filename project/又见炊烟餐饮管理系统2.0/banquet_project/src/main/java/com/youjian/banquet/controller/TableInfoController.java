package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.TableInfo;
import com.youjian.banquet.service.TableInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 餐桌信息控制器
 */
@RestController
@RequestMapping("/api/dish/table-info")
@CrossOrigin(origins = "*")
public class TableInfoController {

    @Autowired
    private TableInfoService tableInfoService;

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(defaultValue = "id") String sortField,
                                            @RequestParam(defaultValue = "desc") String sortOrder,
                                            @RequestParam(required = false) Long storeId) {
        Page<TableInfo> pageResult = tableInfoService.page(page, size, sortField, sortOrder, storeId);
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "size", size
        ));
    }

    @GetMapping("/list")
    public Result<List<TableInfo>> list(@RequestParam(required = false) Long storeId) {
        return Result.success(tableInfoService.listAll(storeId));
    }

    @GetMapping("/info/{id}")
    public Result<TableInfo> info(@PathVariable Long id) {
        return tableInfoService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "餐桌信息不存在"));
    }

    @PostMapping("/save")
    public Result<TableInfo> save(@RequestBody TableInfo entity) {
        return Result.success(tableInfoService.save(entity));
    }

    @PutMapping("/update")
    public Result<TableInfo> update(@RequestBody TableInfo entity) {
        return Result.success(tableInfoService.update(entity));
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        tableInfoService.deleteBatch(List.of(ids));
        return Result.success("删除成功");
    }

    @GetMapping("/remind/{columnName}/{type}")
    public Result<Map<String, Object>> remindCount(@PathVariable String columnName,
                                                   @PathVariable String type,
                                                   @RequestParam Map<String, Object> map,
                                                   @RequestParam(required = false) Long storeId) {
        long count = tableInfoService.count(storeId);
        return Result.success(Map.of("count", count));
    }
}
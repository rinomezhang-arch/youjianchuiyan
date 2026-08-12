package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BtNews;
import com.youjian.banquet.service.BtNewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 餐厅资讯控制器
 * 来源：点餐系统 NewsController
 */
@RestController
@RequestMapping("/api/bt/news")
@CrossOrigin(origins = "*")
public class BtNewsController {

    @Autowired
    private BtNewsService btNewsService;

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(defaultValue = "id") String sortField,
                                            @RequestParam(defaultValue = "desc") String sortOrder,
                                            @RequestParam(required = false) String title,
                                            @RequestParam(required = false) Long storeId) {
        Page<BtNews> pageResult;
        if (title != null && !title.isEmpty()) {
            pageResult = btNewsService.search(title, page, size, storeId);
        } else {
            pageResult = btNewsService.page(page, size, sortField, sortOrder, storeId);
        }
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "size", size
        ));
    }

    @GetMapping("/list")
    public Result<List<BtNews>> list(@RequestParam(required = false) Long storeId) {
        return Result.success(btNewsService.listAll(storeId));
    }

    @GetMapping("/info/{id}")
    public Result<BtNews> info(@PathVariable Long id) {
        return btNewsService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "资讯不存在"));
    }

    @GetMapping("/detail/{id}")
    public Result<BtNews> detail(@PathVariable Long id) {
        return btNewsService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "资讯不存在"));
    }

    @PostMapping("/save")
    public Result<BtNews> save(@RequestBody BtNews entity) {
        return Result.success(btNewsService.save(entity));
    }

    @PostMapping("/add")
    public Result<BtNews> add(@RequestBody BtNews entity) {
        return Result.success(btNewsService.save(entity));
    }

    @PutMapping("/update")
    public Result<BtNews> update(@RequestBody BtNews entity) {
        return Result.success(btNewsService.update(entity));
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        btNewsService.deleteBatch(List.of(ids));
        return Result.success("删除成功");
    }

    @GetMapping("/remind/{columnName}/{type}")
    public Result<Map<String, Object>> remindCount(@PathVariable String columnName,
                                                   @PathVariable String type,
                                                   @RequestParam Map<String, Object> map,
                                                   @RequestParam(required = false) Long storeId) {
        long count = btNewsService.count(storeId);
        return Result.success(Map.of("count", count));
    }
}
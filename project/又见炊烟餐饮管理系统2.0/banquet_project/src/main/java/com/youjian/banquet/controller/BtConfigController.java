package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BtConfig;
import com.youjian.banquet.service.BtConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 配置控制器
 * 来源：点餐系统 ConfigController
 */
@RestController
@RequestMapping("/api/bt-config")
@CrossOrigin(origins = "*")
public class BtConfigController {

    @Autowired
    private BtConfigService btConfigService;

    /**
     * 分页列表
     */
    @GetMapping("/page")
    public Result<Page<BtConfig>> page(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(defaultValue = "id") String sortField,
                                       @RequestParam(defaultValue = "desc") String sortOrder,
                                       @RequestParam(required = false) Long storeId) {
        return Result.success(btConfigService.page(page, size, sortField, sortOrder, storeId));
    }

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<List<BtConfig>> list(@RequestParam(required = false) Long storeId) {
        return Result.success(btConfigService.listAll(storeId));
    }

    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<BtConfig> info(@PathVariable Long id) {
        return btConfigService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "配置不存在"));
    }

    /**
     * 详情
     */
    @GetMapping("/detail/{id}")
    public Result<BtConfig> detail(@PathVariable Long id) {
        return btConfigService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "配置不存在"));
    }

    /**
     * 根据name获取信息
     */
    @GetMapping("/info")
    public Result<BtConfig> infoByName(@RequestParam String name) {
        return btConfigService.getByName(name)
                .map(Result::success)
                .orElse(Result.error(404, "配置不存在"));
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<String> save(@RequestBody BtConfig config) {
        btConfigService.save(config);
        return Result.success("保存成功");
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result<String> update(@RequestBody BtConfig config) {
        btConfigService.update(config);
        return Result.success("修改成功");
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        btConfigService.deleteBatch(Arrays.asList(ids));
        return Result.success("删除成功");
    }
}
package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.HrSocCity;
import com.youjian.banquet.service.HrSocCityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 参保城市管理控制器 (HR系统复刻)
 * 来源: HR系统 CityController
 * 路径: /api/hr/soc-city
 */
@RestController
@RequestMapping("/api/hr-admin/soc-city")
@CrossOrigin(origins = "*")
public class HrSocCityController {

    @Autowired
    private HrSocCityService cityService;

    /**
     * 新增参保城市
     */
    @PostMapping
    public Result<HrSocCity> add(@RequestBody HrSocCity city,
                                  @RequestAttribute(value = "store_id", required = false) Long storeId) {
        if (storeId == null) storeId = 1L;
        city.setStoreId(storeId);
        return cityService.add(city);
    }

    /**
     * 逻辑删除
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        return cityService.deleteById(id);
    }

    /**
     * 批量逻辑删除
     */
    @DeleteMapping("/batch")
    public Result<String> deleteBatch(@RequestBody List<Integer> ids) {
        return cityService.deleteBatch(ids);
    }

    /**
     * 编辑更新
     */
    @PutMapping
    public Result<HrSocCity> edit(@RequestBody HrSocCity city) {
        return cityService.edit(city);
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public Result<HrSocCity> findById(@PathVariable Integer id) {
        return cityService.findById(id);
    }

    /**
     * 查询所有参保城市
     */
    @GetMapping("/all")
    public Result<List<HrSocCity>> findAll(@RequestAttribute(value = "store_id", required = false) Long storeId) {
        if (storeId == null) storeId = 1L;
        return cityService.findAll(storeId);
    }

    /**
     * 分页条件查询
     */
    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") Integer current,
                                             @RequestParam(defaultValue = "10") Integer size,
                                             @RequestAttribute(value = "store_id", required = false) Long storeId,
                                             @RequestParam(required = false) String name) {
        if (storeId == null) storeId = 1L;
        return cityService.list(current, size, storeId, name);
    }

    /**
     * 根据城市名称查询
     */
    @GetMapping("/by-name")
    public Result<HrSocCity> findByName(@RequestAttribute(value = "store_id", required = false) Long storeId,
                                         @RequestParam String name) {
        if (storeId == null) storeId = 1L;
        return cityService.findByName(storeId, name);
    }
}
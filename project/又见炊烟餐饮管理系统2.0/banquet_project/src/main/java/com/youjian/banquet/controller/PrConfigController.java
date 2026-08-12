package com.youjian.banquet.controller;

import com.youjian.banquet.entity.PrConfig;
import com.youjian.banquet.service.PrConfigService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 登录相关
 */
@RestController
@RequestMapping("/config")
public class PrConfigController {

    @Autowired
    private PrConfigService prConfigService;

    /**
     * 列表
     */
    @RequestMapping("/page")
    public Map<String, Object> page(@RequestParam Map<String, Object> params, PrConfig entity,
                                     @RequestParam(required = false) Long storeId) {
        if (storeId != null) {
            entity.setStoreId(storeId);
        }
        Page<PrConfig> page = prConfigService.queryPage(params, entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", page.getContent());
        result.put("total", page.getTotalElements());
        return result;
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, Object> params, PrConfig entity,
                                     @RequestParam(required = false) Long storeId) {
        if (storeId != null) {
            entity.setStoreId(storeId);
        }
        Page<PrConfig> page = prConfigService.queryPage(params, entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", page.getContent());
        result.put("total", page.getTotalElements());
        return result;
    }

    @RequestMapping("/info/{id}")
    public Map<String, Object> info(@PathVariable("id") String id) {
        PrConfig data = prConfigService.selectById(Long.valueOf(id)).orElse(null);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    @RequestMapping("/detail/{id}")
    public Map<String, Object> detail(@PathVariable("id") String id) {
        PrConfig data = prConfigService.selectById(Long.valueOf(id)).orElse(null);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    /**
     * 根据name获取信息
     */
    @RequestMapping("/info")
    public Map<String, Object> infoByName(@RequestParam String name) {
        Specification<PrConfig> spec = (root, query, cb) ->
                cb.equal(root.get("name"), "faceFile");
        PrConfig data = prConfigService.selectOne(spec);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    @PostMapping("/save")
    public Map<String, Object> save(@RequestBody PrConfig entity) {
        prConfigService.insert(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @RequestMapping("/update")
    public Map<String, Object> update(@RequestBody PrConfig entity) {
        prConfigService.updateById(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @RequestMapping("/delete")
    public Map<String, Object> delete(@RequestBody Long[] ids) {
        prConfigService.deleteBatchIds(Arrays.asList(ids));
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }
}
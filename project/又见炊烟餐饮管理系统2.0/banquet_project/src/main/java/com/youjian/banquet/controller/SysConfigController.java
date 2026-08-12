package com.youjian.banquet.controller;

import com.youjian.banquet.entity.SysConfig;
import com.youjian.banquet.service.SysConfigService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/purchase/config")
public class SysConfigController {

    @Autowired
    private SysConfigService sysConfigService;

    @RequestMapping("/page")
    public Map<String, Object> page(@RequestParam Map<String, Object> params, SysConfig entity,
                                     @RequestParam(required = false) Long storeId) {
        if (storeId != null) {
            entity.setStoreId(storeId);
        }
        Page<SysConfig> page = sysConfigService.queryPage(params, entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", page.getContent());
        result.put("total", page.getTotalElements());
        return result;
    }

    @RequestMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, Object> params, SysConfig entity,
                                     @RequestParam(required = false) Long storeId) {
        if (storeId != null) {
            entity.setStoreId(storeId);
        }
        Page<SysConfig> page = sysConfigService.queryPage(params, entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", page.getContent());
        result.put("total", page.getTotalElements());
        return result;
    }

    @RequestMapping("/lists")
    public Map<String, Object> lists(SysConfig entity) {
        List<SysConfig> list = sysConfigService.selectList(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", list);
        return result;
    }

    @RequestMapping("/query")
    public Map<String, Object> query(SysConfig entity) {
        Specification<SysConfig> spec = buildEqSpecification(entity);
        SysConfig data = sysConfigService.selectOne(spec);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("msg", "查询系统配置成功");
        result.put("data", data);
        return result;
    }

    @RequestMapping("/info/{id}")
    public Map<String, Object> info(@PathVariable("id") Long id) {
        SysConfig data = sysConfigService.selectById(id).orElse(null);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    @RequestMapping("/detail/{id}")
    public Map<String, Object> detail(@PathVariable("id") Long id) {
        SysConfig data = sysConfigService.selectById(id).orElse(null);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    @RequestMapping("/save")
    public Map<String, Object> save(@RequestBody SysConfig entity) {
        entity.setId(System.currentTimeMillis());
        sysConfigService.insert(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @RequestMapping("/add")
    public Map<String, Object> add(@RequestBody SysConfig entity) {
        entity.setId(System.currentTimeMillis());
        sysConfigService.insert(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @RequestMapping("/update")
    @Transactional
    public Map<String, Object> update(@RequestBody SysConfig entity) {
        sysConfigService.updateById(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @RequestMapping("/delete")
    public Map<String, Object> delete(@RequestBody Long[] ids) {
        sysConfigService.deleteBatchIds(Arrays.asList(ids));
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @RequestMapping("/remind/{columnName}/{type}")
    public Map<String, Object> remindCount(@PathVariable("columnName") String columnName,
                                            @PathVariable("type") String type,
                                            @RequestParam Map<String, Object> map) {
        map.put("column", columnName);
        map.put("type", type);
        if ("2".equals(type)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Calendar c = Calendar.getInstance();
            if (map.get("remindstart") != null) {
                Integer remindStart = Integer.parseInt(map.get("remindstart").toString());
                c.setTime(new Date());
                c.add(Calendar.DAY_OF_MONTH, remindStart);
                map.put("remindstart", sdf.format(c.getTime()));
            }
            if (map.get("remindend") != null) {
                Integer remindEnd = Integer.parseInt(map.get("remindend").toString());
                c.setTime(new Date());
                c.add(Calendar.DAY_OF_MONTH, remindEnd);
                map.put("remindend", sdf.format(c.getTime()));
            }
        }
        Specification<SysConfig> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (map.get("remindstart") != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(columnName), (Comparable) map.get("remindstart")));
            }
            if (map.get("remindend") != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(columnName), (Comparable) map.get("remindend")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        long count = sysConfigService.selectCount(spec);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("count", count);
        return result;
    }

    private Specification<SysConfig> buildEqSpecification(SysConfig entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getName() != null && !entity.getName().isEmpty()) {
                predicates.add(cb.equal(root.get("name"), entity.getName()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
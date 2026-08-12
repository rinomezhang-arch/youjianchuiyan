package com.youjian.banquet.controller;

import com.youjian.banquet.entity.Supplier;
import com.youjian.banquet.service.SupplierService;
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
 * 供应商
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/purchase/supplier")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    /**
     * 后端列表
     */
    @RequestMapping("/page")
    public Map<String, Object> page(@RequestParam Map<String, Object> params, Supplier entity,
                                     @RequestParam(required = false) Long storeId) {
        if (storeId != null) {
            entity.setStoreId(storeId);
        }
        Page<Supplier> page = supplierService.queryPage(params, entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", page.getContent());
        result.put("total", page.getTotalElements());
        return result;
    }

    /**
     * 前端列表
     */
    @RequestMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, Object> params, Supplier entity,
                                     @RequestParam(required = false) Long storeId) {
        if (storeId != null) {
            entity.setStoreId(storeId);
        }
        Page<Supplier> page = supplierService.queryPage(params, entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", page.getContent());
        result.put("total", page.getTotalElements());
        return result;
    }

    @RequestMapping("/lists")
    public Map<String, Object> lists(Supplier entity) {
        List<Supplier> list = supplierService.selectList(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", list);
        return result;
    }

    @RequestMapping("/query")
    public Map<String, Object> query(Supplier entity) {
        Specification<Supplier> spec = buildEqSpecification(entity);
        Supplier data = supplierService.selectOne(spec);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("msg", "查询供应商成功");
        result.put("data", data);
        return result;
    }

    @RequestMapping("/info/{id}")
    public Map<String, Object> info(@PathVariable("id") Long id) {
        Supplier data = supplierService.selectById(id).orElse(null);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    @RequestMapping("/detail/{id}")
    public Map<String, Object> detail(@PathVariable("id") Long id) {
        Supplier data = supplierService.selectById(id).orElse(null);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    @RequestMapping("/save")
    public Map<String, Object> save(@RequestBody Supplier entity) {
        Map<String, Object> result = new HashMap<>();
        Specification<Supplier> spec = (root, query, cb) ->
                cb.equal(root.get("supplierAccount"), entity.getSupplierAccount());
        Supplier u = supplierService.selectOne(spec);
        if (u != null) {
            result.put("code", 1);
            result.put("msg", "供应商账号已存在");
            return result;
        }
        entity.setId(System.currentTimeMillis());
        supplierService.insert(entity);
        result.put("code", 0);
        return result;
    }

    @RequestMapping("/add")
    public Map<String, Object> add(@RequestBody Supplier entity) {
        Map<String, Object> result = new HashMap<>();
        Specification<Supplier> spec = (root, query, cb) ->
                cb.equal(root.get("supplierAccount"), entity.getSupplierAccount());
        Supplier u = supplierService.selectOne(spec);
        if (u != null) {
            result.put("code", 1);
            result.put("msg", "供应商账号已存在");
            return result;
        }
        entity.setId(System.currentTimeMillis());
        supplierService.insert(entity);
        result.put("code", 0);
        return result;
    }

    @RequestMapping("/update")
    @Transactional
    public Map<String, Object> update(@RequestBody Supplier entity) {
        supplierService.updateById(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @RequestMapping("/delete")
    public Map<String, Object> delete(@RequestBody Long[] ids) {
        supplierService.deleteBatchIds(Arrays.asList(ids));
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
        Specification<Supplier> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (map.get("remindstart") != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(columnName), (Comparable) map.get("remindstart")));
            }
            if (map.get("remindend") != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(columnName), (Comparable) map.get("remindend")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        long count = supplierService.selectCount(spec);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("count", count);
        return result;
    }

    private Specification<Supplier> buildEqSpecification(Supplier entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getSupplierAccount() != null && !entity.getSupplierAccount().isEmpty()) {
                predicates.add(cb.equal(root.get("supplierAccount"), entity.getSupplierAccount()));
            }
            if (entity.getSupplierName() != null && !entity.getSupplierName().isEmpty()) {
                predicates.add(cb.equal(root.get("supplierName"), entity.getSupplierName()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
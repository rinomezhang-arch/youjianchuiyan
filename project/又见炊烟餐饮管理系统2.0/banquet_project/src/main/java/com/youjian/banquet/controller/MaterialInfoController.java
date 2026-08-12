package com.youjian.banquet.controller;

import com.youjian.banquet.entity.MaterialInfo;
import com.youjian.banquet.service.MaterialInfoService;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/purchase/material-info")
public class MaterialInfoController {

    @Autowired
    private MaterialInfoService materialInfoService;

    @RequestMapping("/page")
    public Map<String, Object> page(@RequestParam Map<String, Object> params, MaterialInfo entity,
                                     @RequestParam(required = false) Long storeId,
                                     @RequestParam(required = false) Double pricestart,
                                     @RequestParam(required = false) Double priceend) {
        if (storeId != null) {
            entity.setStoreId(storeId);
        }
        Page<MaterialInfo> page = materialInfoService.queryPage(params, entity);
        List<MaterialInfo> content = page.getContent();
        if (pricestart != null || priceend != null) {
            content = new ArrayList<>();
            for (MaterialInfo m : page.getContent()) {
                boolean include = true;
                if (pricestart != null && m.getPrice() != null && m.getPrice() < pricestart.floatValue()) {
                    include = false;
                }
                if (priceend != null && m.getPrice() != null && m.getPrice() > priceend.floatValue()) {
                    include = false;
                }
                if (include) content.add(m);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", content);
        result.put("total", page.getTotalElements());
        return result;
    }

    @RequestMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, Object> params, MaterialInfo entity,
                                     @RequestParam(required = false) Long storeId,
                                     @RequestParam(required = false) Double pricestart,
                                     @RequestParam(required = false) Double priceend) {
        if (storeId != null) {
            entity.setStoreId(storeId);
        }
        Page<MaterialInfo> page = materialInfoService.queryPage(params, entity);
        List<MaterialInfo> content = page.getContent();
        if (pricestart != null || priceend != null) {
            content = new ArrayList<>();
            for (MaterialInfo m : page.getContent()) {
                boolean include = true;
                if (pricestart != null && m.getPrice() != null && m.getPrice() < pricestart.floatValue()) {
                    include = false;
                }
                if (priceend != null && m.getPrice() != null && m.getPrice() > priceend.floatValue()) {
                    include = false;
                }
                if (include) content.add(m);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", content);
        result.put("total", page.getTotalElements());
        return result;
    }

    @RequestMapping("/lists")
    public Map<String, Object> lists(MaterialInfo entity) {
        List<MaterialInfo> list = materialInfoService.selectList(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", list);
        return result;
    }

    @RequestMapping("/query")
    public Map<String, Object> query(MaterialInfo entity) {
        Specification<MaterialInfo> spec = buildEqSpecification(entity);
        MaterialInfo data = materialInfoService.selectOne(spec);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("msg", "查询材料信息成功");
        result.put("data", data);
        return result;
    }

    @RequestMapping("/info/{id}")
    public Map<String, Object> info(@PathVariable("id") Long id) {
        MaterialInfo data = materialInfoService.selectById(id).orElse(null);
        if (data != null) {
            data.setClickTime(LocalDateTime.now());
            materialInfoService.updateById(data);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    @RequestMapping("/detail/{id}")
    public Map<String, Object> detail(@PathVariable("id") Long id) {
        MaterialInfo data = materialInfoService.selectById(id).orElse(null);
        if (data != null) {
            data.setClickTime(LocalDateTime.now());
            materialInfoService.updateById(data);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    @RequestMapping("/save")
    public Map<String, Object> save(@RequestBody MaterialInfo entity) {
        entity.setId(System.currentTimeMillis());
        materialInfoService.insert(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @RequestMapping("/add")
    public Map<String, Object> add(@RequestBody MaterialInfo entity) {
        entity.setId(System.currentTimeMillis());
        materialInfoService.insert(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @RequestMapping("/update")
    @Transactional
    public Map<String, Object> update(@RequestBody MaterialInfo entity) {
        materialInfoService.updateById(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @RequestMapping("/delete")
    public Map<String, Object> delete(@RequestBody Long[] ids) {
        materialInfoService.deleteBatchIds(Arrays.asList(ids));
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
        Specification<MaterialInfo> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (map.get("remindstart") != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(columnName), (Comparable) map.get("remindstart")));
            }
            if (map.get("remindend") != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(columnName), (Comparable) map.get("remindend")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        long count = materialInfoService.selectCount(spec);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("count", count);
        return result;
    }

    private Specification<MaterialInfo> buildEqSpecification(MaterialInfo entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getStoreId() != null) {
                predicates.add(cb.equal(root.get("storeId"), entity.getStoreId()));
            }
            if (entity.getMaterialName() != null && !entity.getMaterialName().isEmpty()) {
                predicates.add(cb.equal(root.get("materialName"), entity.getMaterialName()));
            }
            if (entity.getCategory() != null && !entity.getCategory().isEmpty()) {
                predicates.add(cb.equal(root.get("category"), entity.getCategory()));
            }
            if (entity.getSupplierAccount() != null && !entity.getSupplierAccount().isEmpty()) {
                predicates.add(cb.equal(root.get("supplierAccount"), entity.getSupplierAccount()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

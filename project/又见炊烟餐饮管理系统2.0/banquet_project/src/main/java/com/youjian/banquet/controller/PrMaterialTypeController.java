package com.youjian.banquet.controller;

import com.youjian.banquet.entity.PrMaterialType;
import com.youjian.banquet.service.PrMaterialTypeService;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
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
 * 材料种类
 * 后端接口
 */
@RestController
@RequestMapping("/cailiaozhonglei")
public class PrMaterialTypeController {

    @Autowired
    private PrMaterialTypeService prMaterialTypeService;

    /**
     * 后端列表
     */
    @RequestMapping("/page")
    public Map<String, Object> page(@RequestParam Map<String, Object> params, PrMaterialType entity) {
        Page<PrMaterialType> page = prMaterialTypeService.queryPage(params, entity);
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
    public Map<String, Object> list(@RequestParam Map<String, Object> params, PrMaterialType entity,
                                     @RequestParam(required = false) Long storeId) {
        if (storeId != null) {
            entity.setStoreId(storeId);
        }
        Page<PrMaterialType> page = prMaterialTypeService.queryPage(params, entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", page.getContent());
        result.put("total", page.getTotalElements());
        return result;
    }

    /**
     * 列表
     */
    @RequestMapping("/lists")
    public Map<String, Object> lists(PrMaterialType entity) {
        List<PrMaterialType> list = prMaterialTypeService.selectList(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", list);
        return result;
    }

    /**
     * 查询
     */
    @RequestMapping("/query")
    public Map<String, Object> query(PrMaterialType entity) {
        Specification<PrMaterialType> spec = buildEqSpecification(entity);
        PrMaterialType data = prMaterialTypeService.selectOne(spec);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("msg", "查询材料种类成功");
        result.put("data", data);
        return result;
    }

    /**
     * 后端详情
     */
    @RequestMapping("/info/{id}")
    public Map<String, Object> info(@PathVariable("id") Long id) {
        PrMaterialType data = prMaterialTypeService.selectById(id).orElse(null);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    /**
     * 前端详情
     */
    @RequestMapping("/detail/{id}")
    public Map<String, Object> detail(@PathVariable("id") Long id) {
        PrMaterialType data = prMaterialTypeService.selectById(id).orElse(null);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    /**
     * 后端保存
     */
    @RequestMapping("/save")
    public Map<String, Object> save(@RequestBody PrMaterialType entity) {
        entity.setId(System.currentTimeMillis() + (long) (Math.random() * 1000));
        prMaterialTypeService.insert(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    /**
     * 前端保存
     */
    @RequestMapping("/add")
    public Map<String, Object> add(@RequestBody PrMaterialType entity) {
        entity.setId(System.currentTimeMillis() + (long) (Math.random() * 1000));
        prMaterialTypeService.insert(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @Transactional
    public Map<String, Object> update(@RequestBody PrMaterialType entity) {
        prMaterialTypeService.updateById(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Map<String, Object> delete(@RequestBody Long[] ids) {
        prMaterialTypeService.deleteBatchIds(Arrays.asList(ids));
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    /**
     * 提醒接口
     */
    @RequestMapping("/remind/{columnName}/{type}")
    public Map<String, Object> remindCount(@PathVariable("columnName") String columnName,
                                            HttpServletRequest request,
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

        Specification<PrMaterialType> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (map.get("remindstart") != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(columnName), (Comparable) map.get("remindstart")));
            }
            if (map.get("remindend") != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(columnName), (Comparable) map.get("remindend")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        long count = prMaterialTypeService.selectCount(spec);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("count", count);
        return result;
    }

    private Specification<PrMaterialType> buildEqSpecification(PrMaterialType entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getCailiaozhonglei() != null && !entity.getCailiaozhonglei().isEmpty()) {
                predicates.add(cb.equal(root.get("cailiaozhonglei"), entity.getCailiaozhonglei()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
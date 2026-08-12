package com.youjian.banquet.controller;

import com.youjian.banquet.entity.PrStoreup;
import com.youjian.banquet.service.PrStoreupService;
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
 * 收藏表
 * 后端接口
 */
@RestController
@RequestMapping("/storeup")
public class PrStoreupController {

    @Autowired
    private PrStoreupService prStoreupService;

    /**
     * 后端列表
     */
    @RequestMapping("/page")
    public Map<String, Object> page(@RequestParam Map<String, Object> params, PrStoreup entity,
                                     @RequestParam(required = false) Long storeId,
                                     HttpServletRequest request) {
        if (storeId != null) {
            entity.setStoreId(storeId);
        }
        Object role = request.getSession().getAttribute("role");
        if (role != null && !"管理员".equals(role.toString())) {
            entity.setUserid((Long) request.getSession().getAttribute("userId"));
        }
        Page<PrStoreup> page = prStoreupService.queryPage(params, entity);
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
    public Map<String, Object> list(@RequestParam Map<String, Object> params, PrStoreup entity,
                                     @RequestParam(required = false) Long storeId) {
        if (storeId != null) {
            entity.setStoreId(storeId);
        }
        Page<PrStoreup> page = prStoreupService.queryPage(params, entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", page.getContent());
        result.put("total", page.getTotalElements());
        return result;
    }

    @RequestMapping("/lists")
    public Map<String, Object> lists(PrStoreup entity) {
        List<PrStoreup> list = prStoreupService.selectList(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", list);
        return result;
    }

    @RequestMapping("/query")
    public Map<String, Object> query(PrStoreup entity) {
        Specification<PrStoreup> spec = buildEqSpecification(entity);
        PrStoreup data = prStoreupService.selectOne(spec);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("msg", "查询收藏表成功");
        result.put("data", data);
        return result;
    }

    @RequestMapping("/info/{id}")
    public Map<String, Object> info(@PathVariable("id") Long id) {
        PrStoreup data = prStoreupService.selectById(id).orElse(null);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    @RequestMapping("/detail/{id}")
    public Map<String, Object> detail(@PathVariable("id") Long id) {
        PrStoreup data = prStoreupService.selectById(id).orElse(null);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    @RequestMapping("/save")
    public Map<String, Object> save(@RequestBody PrStoreup entity, HttpServletRequest request) {
        entity.setId(System.currentTimeMillis() + Double.valueOf(Math.floor(Math.random() * 1000)).longValue());
        entity.setUserid((Long) request.getSession().getAttribute("userId"));
        prStoreupService.insert(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @RequestMapping("/add")
    public Map<String, Object> add(@RequestBody PrStoreup entity, HttpServletRequest request) {
        entity.setId(System.currentTimeMillis() + Double.valueOf(Math.floor(Math.random() * 1000)).longValue());
        prStoreupService.insert(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @RequestMapping("/update")
    @Transactional
    public Map<String, Object> update(@RequestBody PrStoreup entity) {
        prStoreupService.updateById(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @RequestMapping("/delete")
    public Map<String, Object> delete(@RequestBody Long[] ids) {
        prStoreupService.deleteBatchIds(Arrays.asList(ids));
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
        Specification<PrStoreup> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (map.get("remindstart") != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(columnName), (Comparable) map.get("remindstart")));
            }
            if (map.get("remindend") != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(columnName), (Comparable) map.get("remindend")));
            }
            Object role = request.getSession().getAttribute("role");
            if (role != null && !"管理员".equals(role.toString())) {
                predicates.add(cb.equal(root.get("userid"), (Long) request.getSession().getAttribute("userId")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        long count = prStoreupService.selectCount(spec);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("count", count);
        return result;
    }

    private Specification<PrStoreup> buildEqSpecification(PrStoreup entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getUserid() != null) {
                predicates.add(cb.equal(root.get("userid"), entity.getUserid()));
            }
            if (entity.getRefid() != null) {
                predicates.add(cb.equal(root.get("refid"), entity.getRefid()));
            }
            if (entity.getTablename() != null && !entity.getTablename().isEmpty()) {
                predicates.add(cb.equal(root.get("tablename"), entity.getTablename()));
            }
            if (entity.getName() != null && !entity.getName().isEmpty()) {
                predicates.add(cb.equal(root.get("name"), entity.getName()));
            }
            if (entity.getType() != null && !entity.getType().isEmpty()) {
                predicates.add(cb.equal(root.get("type"), entity.getType()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
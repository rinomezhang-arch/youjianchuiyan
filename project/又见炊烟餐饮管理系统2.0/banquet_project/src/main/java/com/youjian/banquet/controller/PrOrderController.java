package com.youjian.banquet.controller;

import com.youjian.banquet.entity.PrOrder;
import com.youjian.banquet.service.PrOrderService;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单
 * 后端接口
 */
@RestController
@RequestMapping("/orders")
public class PrOrderController {

    @Autowired
    private PrOrderService prOrderService;

    /**
     * 后端列表
     */
    @RequestMapping("/page")
    public Map<String, Object> page(@RequestParam Map<String, Object> params, PrOrder entity,
                                     @RequestParam(required = false) Long storeId,
                                     HttpServletRequest request) {
        if (storeId != null) {
            entity.setStoreId(storeId);
        }
        String role = (String) request.getSession().getAttribute("role");
        if (!"管理员".equals(role)) {
            entity.setUserid((Long) request.getSession().getAttribute("userId"));
        }
        String tableName = (String) request.getSession().getAttribute("tableName");
        if ("gongyingshang".equals(tableName)) {
            entity.setGongyingshangzhanghao((String) request.getSession().getAttribute("username"));
            if (entity.getUserid() != null) {
                entity.setUserid(null);
            }
        }
        Page<PrOrder> page = prOrderService.queryPage(params, entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", page.getContent());
        result.put("total", page.getTotalElements());
        return result;
    }

    @RequestMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, Object> params, PrOrder entity,
                                     @RequestParam(required = false) Long storeId) {
        if (storeId != null) {
            entity.setStoreId(storeId);
        }
        Page<PrOrder> page = prOrderService.queryPage(params, entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", page.getContent());
        result.put("total", page.getTotalElements());
        return result;
    }

    @RequestMapping("/lists")
    public Map<String, Object> lists(PrOrder entity) {
        List<PrOrder> list = prOrderService.selectList(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", list);
        return result;
    }

    @RequestMapping("/query")
    public Map<String, Object> query(PrOrder entity) {
        Specification<PrOrder> spec = buildEqSpecification(entity);
        PrOrder data = prOrderService.selectOne(spec);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("msg", "查询订单成功");
        result.put("data", data);
        return result;
    }

    @RequestMapping("/info/{id}")
    public Map<String, Object> info(@PathVariable("id") Long id) {
        PrOrder data = prOrderService.selectById(id).orElse(null);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    @RequestMapping("/detail/{id}")
    public Map<String, Object> detail(@PathVariable("id") Long id) {
        PrOrder data = prOrderService.selectById(id).orElse(null);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    @RequestMapping("/save")
    public Map<String, Object> save(@RequestBody PrOrder entity, HttpServletRequest request) {
        entity.setId(System.currentTimeMillis() + (long) (Math.random() * 1000));
        entity.setUserid((Long) request.getSession().getAttribute("userId"));
        prOrderService.insert(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @RequestMapping("/add")
    public Map<String, Object> add(@RequestBody PrOrder entity) {
        entity.setId(System.currentTimeMillis() + (long) (Math.random() * 1000));
        prOrderService.insert(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @RequestMapping("/update")
    @Transactional
    public Map<String, Object> update(@RequestBody PrOrder entity) {
        prOrderService.updateById(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @RequestMapping("/delete")
    public Map<String, Object> delete(@RequestBody Long[] ids) {
        prOrderService.deleteBatchIds(Arrays.asList(ids));
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

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
        Specification<PrOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (map.get("remindstart") != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(columnName), (Comparable) map.get("remindstart")));
            }
            if (map.get("remindend") != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(columnName), (Comparable) map.get("remindend")));
            }
            String role = (String) request.getSession().getAttribute("role");
            if (!"管理员".equals(role)) {
                predicates.add(cb.equal(root.get("userid"), request.getSession().getAttribute("userId")));
            }
            String tableName = (String) request.getSession().getAttribute("tableName");
            if ("gongyingshang".equals(tableName)) {
                predicates.add(cb.equal(root.get("gongyingshangzhanghao"), request.getSession().getAttribute("username")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        long count = prOrderService.selectCount(spec);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("count", count);
        return result;
    }

    private Specification<PrOrder> buildEqSpecification(PrOrder entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getOrderid() != null && !entity.getOrderid().isEmpty()) {
                predicates.add(cb.equal(root.get("orderid"), entity.getOrderid()));
            }
            if (entity.getUserid() != null) {
                predicates.add(cb.equal(root.get("userid"), entity.getUserid()));
            }
            if (entity.getGongyingshangzhanghao() != null && !entity.getGongyingshangzhanghao().isEmpty()) {
                predicates.add(cb.equal(root.get("gongyingshangzhanghao"), entity.getGongyingshangzhanghao()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
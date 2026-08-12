package com.youjian.banquet.controller;

import com.youjian.banquet.entity.PrAddress;
import com.youjian.banquet.service.PrAddressService;
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
 * 地址
 * 后端接口
 */
@RestController
@RequestMapping("/address")
public class PrAddressController {

    @Autowired
    private PrAddressService prAddressService;

    @RequestMapping("/page")
    public Map<String, Object> page(@RequestParam Map<String, Object> params, PrAddress entity,
                                     HttpServletRequest request) {
        String role = (String) request.getSession().getAttribute("role");
        if (!"管理员".equals(role)) {
            entity.setUserid((Long) request.getSession().getAttribute("userId"));
        }
        Page<PrAddress> page = prAddressService.queryPage(params, entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", page.getContent());
        result.put("total", page.getTotalElements());
        return result;
    }

    @RequestMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, Object> params, PrAddress entity,
                                     @RequestParam(required = false) Long storeId,
                                     HttpServletRequest request) {
        if (storeId != null) {
            entity.setStoreId(storeId);
        }
        String role = (String) request.getSession().getAttribute("role");
        if (!"管理员".equals(role)) {
            entity.setUserid((Long) request.getSession().getAttribute("userId"));
        }
        Page<PrAddress> page = prAddressService.queryPage(params, entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", page.getContent());
        result.put("total", page.getTotalElements());
        return result;
    }

    @RequestMapping("/lists")
    public Map<String, Object> lists(PrAddress entity) {
        List<PrAddress> list = prAddressService.selectList(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", list);
        return result;
    }

    @RequestMapping("/query")
    public Map<String, Object> query(PrAddress entity) {
        Specification<PrAddress> spec = buildEqSpecification(entity);
        PrAddress data = prAddressService.selectOne(spec);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("msg", "查询地址成功");
        result.put("data", data);
        return result;
    }

    @RequestMapping("/info/{id}")
    public Map<String, Object> info(@PathVariable("id") Long id) {
        PrAddress data = prAddressService.selectById(id).orElse(null);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    @RequestMapping("/detail/{id}")
    public Map<String, Object> detail(@PathVariable("id") Long id) {
        PrAddress data = prAddressService.selectById(id).orElse(null);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    @RequestMapping("/save")
    public Map<String, Object> save(@RequestBody PrAddress entity, HttpServletRequest request) {
        entity.setId(System.currentTimeMillis() + (long) (Math.random() * 1000));
        Long userId = (Long) request.getSession().getAttribute("userId");
        if ("是".equals(entity.getIsdefault())) {
            prAddressService.updateForSet("isdefault='否'", userId);
        }
        entity.setUserid(userId);
        prAddressService.insert(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @RequestMapping("/add")
    public Map<String, Object> add(@RequestBody PrAddress entity, HttpServletRequest request) {
        entity.setId(System.currentTimeMillis() + (long) (Math.random() * 1000));
        Long userId = (Long) request.getSession().getAttribute("userId");
        if ("是".equals(entity.getIsdefault())) {
            prAddressService.updateForSet("isdefault='否'", userId);
        }
        entity.setUserid(userId);
        prAddressService.insert(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @RequestMapping("/update")
    @Transactional
    public Map<String, Object> update(@RequestBody PrAddress entity, HttpServletRequest request) {
        if ("是".equals(entity.getIsdefault())) {
            prAddressService.updateForSet("isdefault='否'", (Long) request.getSession().getAttribute("userId"));
        }
        prAddressService.updateById(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    /**
     * 获取默认地址
     */
    @RequestMapping("/default")
    public Map<String, Object> defaultAddress(HttpServletRequest request) {
        Specification<PrAddress> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("isdefault"), "是"),
                cb.equal(root.get("userid"), request.getSession().getAttribute("userId"))
        );
        PrAddress address = prAddressService.selectOne(spec);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", address);
        return result;
    }

    @RequestMapping("/delete")
    public Map<String, Object> delete(@RequestBody Long[] ids) {
        prAddressService.deleteBatchIds(Arrays.asList(ids));
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
        Specification<PrAddress> spec = (root, query, cb) -> {
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
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        long count = prAddressService.selectCount(spec);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("count", count);
        return result;
    }

    private Specification<PrAddress> buildEqSpecification(PrAddress entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getUserid() != null) {
                predicates.add(cb.equal(root.get("userid"), entity.getUserid()));
            }
            if (entity.getName() != null && !entity.getName().isEmpty()) {
                predicates.add(cb.equal(root.get("name"), entity.getName()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
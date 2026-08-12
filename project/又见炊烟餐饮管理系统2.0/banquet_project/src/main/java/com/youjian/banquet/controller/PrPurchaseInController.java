package com.youjian.banquet.controller;

import com.youjian.banquet.entity.PrPurchaseIn;
import com.youjian.banquet.service.PrPurchaseInService;
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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 采购入库
 * 后端接口
 */
@RestController
@RequestMapping("/caigouruku")
public class PrPurchaseInController {

    @Autowired
    private PrPurchaseInService prPurchaseInService;

    /**
     * 后端列表
     */
    @RequestMapping("/page")
    public Map<String, Object> page(@RequestParam Map<String, Object> params, PrPurchaseIn entity,
                                     @RequestParam(required = false) Long storeId,
                                     HttpServletRequest request) {
        if (storeId != null) {
            entity.setStoreId(storeId);
        }
        // 供应商登录时过滤自己的数据
        String tableName = (String) request.getSession().getAttribute("tableName");
        if ("gongyingshang".equals(tableName)) {
            entity.setGongyingshangzhanghao((String) request.getSession().getAttribute("username"));
        }
        Page<PrPurchaseIn> page = prPurchaseInService.queryPage(params, entity);
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
    public Map<String, Object> list(@RequestParam Map<String, Object> params, PrPurchaseIn entity,
                                     @RequestParam(required = false) Long storeId) {
        if (storeId != null) {
            entity.setStoreId(storeId);
        }
        Page<PrPurchaseIn> page = prPurchaseInService.queryPage(params, entity);
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
    public Map<String, Object> lists(PrPurchaseIn entity) {
        List<PrPurchaseIn> list = prPurchaseInService.selectList(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", list);
        return result;
    }

    /**
     * 查询
     */
    @RequestMapping("/query")
    public Map<String, Object> query(PrPurchaseIn entity) {
        Specification<PrPurchaseIn> spec = buildEqSpecification(entity);
        PrPurchaseIn data = prPurchaseInService.selectOne(spec);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("msg", "查询采购入库成功");
        result.put("data", data);
        return result;
    }

    /**
     * 后端详情
     */
    @RequestMapping("/info/{id}")
    public Map<String, Object> info(@PathVariable("id") Long id) {
        PrPurchaseIn data = prPurchaseInService.selectById(id).orElse(null);
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
        PrPurchaseIn data = prPurchaseInService.selectById(id).orElse(null);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    /**
     * 后端保存
     */
    @RequestMapping("/save")
    public Map<String, Object> save(@RequestBody PrPurchaseIn entity) {
        entity.setId(System.currentTimeMillis() + (long) (Math.random() * 1000));
        prPurchaseInService.insert(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    /**
     * 前端保存
     */
    @RequestMapping("/add")
    public Map<String, Object> add(@RequestBody PrPurchaseIn entity) {
        entity.setId(System.currentTimeMillis() + (long) (Math.random() * 1000));
        prPurchaseInService.insert(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @Transactional
    public Map<String, Object> update(@RequestBody PrPurchaseIn entity) {
        prPurchaseInService.updateById(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Map<String, Object> delete(@RequestBody Long[] ids) {
        prPurchaseInService.deleteBatchIds(Arrays.asList(ids));
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

        Specification<PrPurchaseIn> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (map.get("remindstart") != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(columnName),
                        LocalDateTime.ofInstant(((Date) map.get("remindstart")).toInstant(), ZoneId.systemDefault())));
            }
            if (map.get("remindend") != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(columnName),
                        LocalDateTime.ofInstant(((Date) map.get("remindend")).toInstant(), ZoneId.systemDefault())));
            }
            String tableName = (String) request.getSession().getAttribute("tableName");
            if ("gongyingshang".equals(tableName)) {
                predicates.add(cb.equal(root.get("gongyingshangzhanghao"),
                        request.getSession().getAttribute("username")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        long count = prPurchaseInService.selectCount(spec);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("count", count);
        return result;
    }

    private Specification<PrPurchaseIn> buildEqSpecification(PrPurchaseIn entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getCailiaomingcheng() != null && !entity.getCailiaomingcheng().isEmpty()) {
                predicates.add(cb.equal(root.get("cailiaomingcheng"), entity.getCailiaomingcheng()));
            }
            if (entity.getCailiaozhonglei() != null && !entity.getCailiaozhonglei().isEmpty()) {
                predicates.add(cb.equal(root.get("cailiaozhonglei"), entity.getCailiaozhonglei()));
            }
            if (entity.getCailiaoguige() != null && !entity.getCailiaoguige().isEmpty()) {
                predicates.add(cb.equal(root.get("cailiaoguige"), entity.getCailiaoguige()));
            }
            if (entity.getGongyingshangzhanghao() != null && !entity.getGongyingshangzhanghao().isEmpty()) {
                predicates.add(cb.equal(root.get("gongyingshangzhanghao"), entity.getGongyingshangzhanghao()));
            }
            if (entity.getGongyingshangmingcheng() != null && !entity.getGongyingshangmingcheng().isEmpty()) {
                predicates.add(cb.equal(root.get("gongyingshangmingcheng"), entity.getGongyingshangmingcheng()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
package com.youjian.banquet.controller;

import com.youjian.banquet.entity.PrSupplier;
import com.youjian.banquet.service.PrSupplierService;
import com.youjian.banquet.service.PrTokenService;
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
 * 供应商
 * 后端接口
 */
@RestController
@RequestMapping("/gongyingshang")
public class PrSupplierController {

    @Autowired
    private PrSupplierService prSupplierService;

    @Autowired
    private PrTokenService prTokenService;

    /**
     * 登录
     */
    @RequestMapping("/login")
    public Map<String, Object> login(String username, String password, String captcha, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        Specification<PrSupplier> spec = (root, query, cb) ->
                cb.equal(root.get("gongyingshangzhanghao"), username);
        PrSupplier u = prSupplierService.selectOne(spec);
        if (u == null || !u.getMima().equals(password)) {
            result.put("code", 1);
            result.put("msg", "账号或密码不正确");
            return result;
        }
        String token = prTokenService.generateToken(u.getId(), username, "gongyingshang", "供应商");
        result.put("code", 0);
        result.put("token", token);
        return result;
    }

    /**
     * 注册
     */
    @RequestMapping("/register")
    public Map<String, Object> register(@RequestBody PrSupplier entity) {
        Map<String, Object> result = new HashMap<>();
        Specification<PrSupplier> spec = (root, query, cb) ->
                cb.equal(root.get("gongyingshangzhanghao"), entity.getGongyingshangzhanghao());
        PrSupplier u = prSupplierService.selectOne(spec);
        if (u != null) {
            result.put("code", 1);
            result.put("msg", "注册用户已存在");
            return result;
        }
        entity.setId(System.currentTimeMillis());
        prSupplierService.insert(entity);
        result.put("code", 0);
        return result;
    }

    /**
     * 退出
     */
    @RequestMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        request.getSession().invalidate();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("msg", "退出成功");
        return result;
    }

    /**
     * 获取用户的session用户信息
     */
    @RequestMapping("/session")
    public Map<String, Object> getCurrUser(HttpServletRequest request) {
        Long id = (Long) request.getSession().getAttribute("userId");
        PrSupplier u = prSupplierService.selectById(id).orElse(null);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", u);
        return result;
    }

    /**
     * 密码重置
     */
    @RequestMapping("/resetPass")
    public Map<String, Object> resetPass(String username, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        Specification<PrSupplier> spec = (root, query, cb) ->
                cb.equal(root.get("gongyingshangzhanghao"), username);
        PrSupplier u = prSupplierService.selectOne(spec);
        if (u == null) {
            result.put("code", 1);
            result.put("msg", "账号不存在");
            return result;
        }
        u.setMima("123456");
        prSupplierService.updateById(u);
        result.put("code", 0);
        result.put("msg", "密码已重置为：123456");
        return result;
    }

    /**
     * 后端列表
     */
    @RequestMapping("/page")
    public Map<String, Object> page(@RequestParam Map<String, Object> params, PrSupplier entity,
                                     @RequestParam(required = false) Long storeId) {
        if (storeId != null) {
            entity.setStoreId(storeId);
        }
        Page<PrSupplier> page = prSupplierService.queryPage(params, entity);
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
    public Map<String, Object> list(@RequestParam Map<String, Object> params, PrSupplier entity,
                                     @RequestParam(required = false) Long storeId) {
        if (storeId != null) {
            entity.setStoreId(storeId);
        }
        Page<PrSupplier> page = prSupplierService.queryPage(params, entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", page.getContent());
        result.put("total", page.getTotalElements());
        return result;
    }

    @RequestMapping("/lists")
    public Map<String, Object> lists(PrSupplier entity) {
        List<PrSupplier> list = prSupplierService.selectList(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", list);
        return result;
    }

    @RequestMapping("/query")
    public Map<String, Object> query(PrSupplier entity) {
        Specification<PrSupplier> spec = buildEqSpecification(entity);
        PrSupplier data = prSupplierService.selectOne(spec);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("msg", "查询供应商成功");
        result.put("data", data);
        return result;
    }

    @RequestMapping("/info/{id}")
    public Map<String, Object> info(@PathVariable("id") Long id) {
        PrSupplier data = prSupplierService.selectById(id).orElse(null);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    @RequestMapping("/detail/{id}")
    public Map<String, Object> detail(@PathVariable("id") Long id) {
        PrSupplier data = prSupplierService.selectById(id).orElse(null);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    @RequestMapping("/save")
    public Map<String, Object> save(@RequestBody PrSupplier entity) {
        Map<String, Object> result = new HashMap<>();
        Specification<PrSupplier> spec = (root, query, cb) ->
                cb.equal(root.get("gongyingshangzhanghao"), entity.getGongyingshangzhanghao());
        PrSupplier u = prSupplierService.selectOne(spec);
        if (u != null) {
            result.put("code", 1);
            result.put("msg", "用户已存在");
            return result;
        }
        entity.setId(System.currentTimeMillis());
        prSupplierService.insert(entity);
        result.put("code", 0);
        return result;
    }

    @RequestMapping("/add")
    public Map<String, Object> add(@RequestBody PrSupplier entity) {
        Map<String, Object> result = new HashMap<>();
        Specification<PrSupplier> spec = (root, query, cb) ->
                cb.equal(root.get("gongyingshangzhanghao"), entity.getGongyingshangzhanghao());
        PrSupplier u = prSupplierService.selectOne(spec);
        if (u != null) {
            result.put("code", 1);
            result.put("msg", "用户已存在");
            return result;
        }
        entity.setId(System.currentTimeMillis());
        prSupplierService.insert(entity);
        result.put("code", 0);
        return result;
    }

    @RequestMapping("/update")
    @Transactional
    public Map<String, Object> update(@RequestBody PrSupplier entity) {
        prSupplierService.updateById(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @RequestMapping("/delete")
    public Map<String, Object> delete(@RequestBody Long[] ids) {
        prSupplierService.deleteBatchIds(Arrays.asList(ids));
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
        Specification<PrSupplier> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (map.get("remindstart") != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(columnName), (Comparable) map.get("remindstart")));
            }
            if (map.get("remindend") != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(columnName), (Comparable) map.get("remindend")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        long count = prSupplierService.selectCount(spec);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("count", count);
        return result;
    }

    private Specification<PrSupplier> buildEqSpecification(PrSupplier entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
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
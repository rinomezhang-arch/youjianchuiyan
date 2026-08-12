package com.youjian.banquet.controller;

import com.youjian.banquet.entity.PrUser;
import com.youjian.banquet.service.PrTokenService;
import com.youjian.banquet.service.PrUserService;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/users")
public class PrUserController {

    @Autowired
    private PrUserService prUserService;

    @Autowired
    private PrTokenService prTokenService;

    /**
     * 登录
     */
    @RequestMapping("/login")
    public Map<String, Object> login(String username, String password, String captcha, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        Specification<PrUser> spec = (root, query, cb) ->
                cb.equal(root.get("username"), username);
        PrUser user = prUserService.selectOne(spec);
        if (user == null || !user.getPassword().equals(password)) {
            result.put("code", 1);
            result.put("msg", "账号或密码不正确");
            return result;
        }
        String token = prTokenService.generateToken(user.getId(), username, "users", user.getRole());
        result.put("code", 0);
        result.put("token", token);
        return result;
    }

    /**
     * 注册
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody PrUser user) {
        Map<String, Object> result = new HashMap<>();
        Specification<PrUser> spec = (root, query, cb) ->
                cb.equal(root.get("username"), user.getUsername());
        PrUser existing = prUserService.selectOne(spec);
        if (existing != null) {
            result.put("code", 1);
            result.put("msg", "用户已存在");
            return result;
        }
        prUserService.insert(user);
        result.put("code", 0);
        return result;
    }

    /**
     * 退出
     */
    @GetMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        request.getSession().invalidate();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("msg", "退出成功");
        return result;
    }

    /**
     * 密码重置
     */
    @RequestMapping("/resetPass")
    public Map<String, Object> resetPass(String username, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        Specification<PrUser> spec = (root, query, cb) ->
                cb.equal(root.get("username"), username);
        PrUser user = prUserService.selectOne(spec);
        if (user == null) {
            result.put("code", 1);
            result.put("msg", "账号不存在");
            return result;
        }
        user.setPassword("123456");
        prUserService.updateById(user);
        result.put("code", 0);
        result.put("msg", "密码已重置为：123456");
        return result;
    }

    /**
     * 列表
     */
    @RequestMapping("/page")
    public Map<String, Object> page(@RequestParam Map<String, Object> params, PrUser entity) {
        Page<PrUser> page = prUserService.queryPage(params, entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", page.getContent());
        result.put("total", page.getTotalElements());
        return result;
    }

    @RequestMapping("/list")
    public Map<String, Object> list(PrUser entity) {
        List<PrUser> list = prUserService.selectList(entity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", list);
        return result;
    }

    @RequestMapping("/info/{id}")
    public Map<String, Object> info(@PathVariable("id") String id) {
        PrUser data = prUserService.selectById(Long.valueOf(id)).orElse(null);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    /**
     * 获取用户的session用户信息
     */
    @RequestMapping("/session")
    public Map<String, Object> getCurrUser(HttpServletRequest request) {
        Long id = (Long) request.getSession().getAttribute("userId");
        PrUser user = prUserService.selectById(id).orElse(null);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", user);
        return result;
    }

    @PostMapping("/save")
    public Map<String, Object> save(@RequestBody PrUser entity) {
        Map<String, Object> result = new HashMap<>();
        Specification<PrUser> spec = (root, query, cb) ->
                cb.equal(root.get("username"), entity.getUsername());
        PrUser existing = prUserService.selectOne(spec);
        if (existing != null) {
            result.put("code", 1);
            result.put("msg", "用户已存在");
            return result;
        }
        prUserService.insert(entity);
        result.put("code", 0);
        return result;
    }

    @RequestMapping("/update")
    public Map<String, Object> update(@RequestBody PrUser entity) {
        Map<String, Object> result = new HashMap<>();
        Specification<PrUser> spec = (root, query, cb) ->
                cb.equal(root.get("username"), entity.getUsername());
        PrUser u = prUserService.selectOne(spec);
        if (u != null && !u.getId().equals(entity.getId()) && u.getUsername().equals(entity.getUsername())) {
            result.put("code", 1);
            result.put("msg", "用户名已存在。");
            return result;
        }
        prUserService.updateById(entity);
        result.put("code", 0);
        return result;
    }

    @RequestMapping("/delete")
    public Map<String, Object> delete(@RequestBody Long[] ids) {
        prUserService.deleteBatchIds(Arrays.asList(ids));
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }
}
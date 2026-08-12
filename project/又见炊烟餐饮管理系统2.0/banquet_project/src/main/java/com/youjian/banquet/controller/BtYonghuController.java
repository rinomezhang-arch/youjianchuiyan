package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BtYonghu;
import com.youjian.banquet.service.BtTokenService;
import com.youjian.banquet.service.BtYonghuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

/**
 * 普通用户控制器
 * 来源：点餐系统 YonghuController
 */
@RestController
@RequestMapping("/api/bt-yonghu")
@CrossOrigin(origins = "*")
public class BtYonghuController {

    @Autowired
    private BtYonghuService btYonghuService;

    @Autowired
    private BtTokenService btTokenService;

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<Map<String, String>> login(@RequestParam String username, @RequestParam String password) {
        BtYonghu user = btYonghuService.login(username, password);
        if (user == null) {
            return Result.error(401, "账号或密码不正确");
        }
        String token = btTokenService.generateToken(user.getId(), username, "yonghu", "用户");
        return Result.success(Map.of("token", token));
    }

    /**
     * 注册
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody BtYonghu yonghu) {
        if (btYonghuService.existsByUsername(yonghu.getYonghuming())) {
            return Result.error(400, "注册用户已存在");
        }
        yonghu.setId(new Date().getTime());
        btYonghuService.save(yonghu);
        return Result.success("注册成功");
    }

    /**
     * 退出
     */
    @GetMapping("/logout")
    public Result<String> logout() {
        return Result.success("退出成功");
    }

    /**
     * 密码重置
     */
    @PostMapping("/resetPass")
    public Result<String> resetPass(@RequestParam String username) {
        boolean success = btYonghuService.resetPassword(username);
        if (!success) {
            return Result.error(400, "账号不存在");
        }
        return Result.success("密码已重置为：123456");
    }

    /**
     * 后端分页列表
     */
    @GetMapping("/page")
    public Result<Page<BtYonghu>> page(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(defaultValue = "id") String sortField,
                                       @RequestParam(defaultValue = "desc") String sortOrder,
                                       @RequestParam(required = false) Long storeId) {
        return Result.success(btYonghuService.page(page, size, sortField, sortOrder, storeId));
    }

    /**
     * 前端列表
     */
    @GetMapping("/list")
    public Result<Page<BtYonghu>> list(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "100") int size,
                                       @RequestParam(required = false) Long storeId) {
        return Result.success(btYonghuService.page(page, size, storeId));
    }

    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<BtYonghu> info(@PathVariable Long id) {
        return btYonghuService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "用户不存在"));
    }

    /**
     * 详情
     */
    @GetMapping("/detail/{id}")
    public Result<BtYonghu> detail(@PathVariable Long id) {
        return btYonghuService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "用户不存在"));
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<String> save(@RequestBody BtYonghu yonghu) {
        btYonghuService.save(yonghu);
        return Result.success("保存成功");
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result<String> update(@RequestBody BtYonghu yonghu) {
        btYonghuService.update(yonghu);
        return Result.success("修改成功");
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        btYonghuService.deleteBatch(Arrays.asList(ids));
        return Result.success("删除成功");
    }
}
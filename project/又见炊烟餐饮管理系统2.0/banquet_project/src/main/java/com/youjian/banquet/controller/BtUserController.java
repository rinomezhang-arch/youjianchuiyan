package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BtUser;
import com.youjian.banquet.service.BtTokenService;
import com.youjian.banquet.service.BtUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

/**
 * 管理员用户控制器
 * 来源：点餐系统 UsersController
 */
@RestController
@RequestMapping("/api/bt-user")
@CrossOrigin(origins = "*")
public class BtUserController {

    @Autowired
    private BtUserService btUserService;

    @Autowired
    private BtTokenService btTokenService;

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<Map<String, String>> login(@RequestParam String username, @RequestParam String password) {
        BtUser user = btUserService.login(username, password);
        if (user == null) {
            return Result.error(401, "账号或密码不正确");
        }
        String token = btTokenService.generateToken(user.getId(), username, "users", user.getRole());
        return Result.success(Map.of("token", token));
    }

    /**
     * 注册
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody BtUser user) {
        if (btUserService.existsByUsername(user.getUsername())) {
            return Result.error(400, "用户已存在");
        }
        btUserService.save(user);
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
        boolean success = btUserService.resetPassword(username);
        if (!success) {
            return Result.error(400, "账号不存在");
        }
        return Result.success("密码已重置为：123456");
    }

    /**
     * 分页列表
     */
    @GetMapping("/page")
    public Result<Page<BtUser>> page(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     @RequestParam(defaultValue = "id") String sortField,
                                     @RequestParam(defaultValue = "desc") String sortOrder,
                                     @RequestParam(required = false) Long storeId) {
        return Result.success(btUserService.page(page, size, sortField, sortOrder, storeId));
    }

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<Page<BtUser>> list(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "100") int size,
                                     @RequestParam(required = false) Long storeId) {
        return Result.success(btUserService.page(page, size, storeId));
    }

    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<BtUser> info(@PathVariable Long id) {
        return btUserService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "用户不存在"));
    }

    /**
     * 详情
     */
    @GetMapping("/detail/{id}")
    public Result<BtUser> detail(@PathVariable Long id) {
        return btUserService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "用户不存在"));
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<String> save(@RequestBody BtUser user) {
        btUserService.save(user);
        return Result.success("保存成功");
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result<String> update(@RequestBody BtUser user) {
        btUserService.update(user);
        return Result.success("修改成功");
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        btUserService.deleteBatch(Arrays.asList(ids));
        return Result.success("删除成功");
    }
}
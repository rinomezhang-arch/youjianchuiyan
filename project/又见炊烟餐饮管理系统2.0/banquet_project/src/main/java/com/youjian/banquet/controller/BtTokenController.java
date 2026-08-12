package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BtToken;
import com.youjian.banquet.service.BtTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Token控制器
 * 来源：点餐系统 token 管理逻辑
 */
@RestController
@RequestMapping("/api/bt-token")
@CrossOrigin(origins = "*")
public class BtTokenController {

    @Autowired
    private BtTokenService btTokenService;

    /**
     * 生成token
     */
    @PostMapping("/generate")
    public Result<String> generate(@RequestParam Long userid,
                                   @RequestParam String username,
                                   @RequestParam String tableName,
                                   @RequestParam String role) {
        String token = btTokenService.generateToken(userid, username, tableName, role);
        return Result.success(token);
    }

    /**
     * 验证token
     */
    @GetMapping("/verify")
    public Result<BtToken> verify(@RequestParam String token) {
        BtToken tokenEntity = btTokenService.getTokenEntity(token);
        if (tokenEntity == null) {
            return Result.error(401, "token无效或已过期");
        }
        return Result.success(tokenEntity);
    }
}
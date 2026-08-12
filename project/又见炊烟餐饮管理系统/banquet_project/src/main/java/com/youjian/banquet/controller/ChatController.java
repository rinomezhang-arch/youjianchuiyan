package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * ChatController - 通用聊天辅助接口
 * 注意：本控制器路径前缀为 /api/chat，独立于 AIController 的 /api/ai
 * 纳入 JWT 全局鉴权体系（/api/** 由 JwtAuthInterceptor 统一拦截）
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin
public class ChatController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 欢迎语：根据当前登录员工返回个性化问候 */
    @GetMapping("/greeting")
    public Result<Map<String, Object>> greeting(HttpServletRequest request) {
        Map<String, Object> data = new HashMap<>();
        Long staffId = resolveStaffId(request);
        if (staffId != null) {
            try {
                List<Map<String, Object>> staff = jdbcTemplate.queryForList(
                    "SELECT staff_name, department FROM staff_master WHERE staff_id = ? LIMIT 1", staffId);
                if (!staff.isEmpty()) {
                    String name = (String) staff.get(0).get("staff_name");
                    data.put("name", name);
                    data.put("greeting", "你好，" + name + "！我是炊小助，又见炊烟的AI助理。有什么可以帮你的？");
                } else {
                    data.put("greeting", "你好！我是炊小助，又见炊烟的AI助理。有什么需要？");
                }
            } catch (Exception e) {
                data.put("greeting", "你好！我是炊小助，又见炊烟的AI助理。有什么需要？");
            }
        } else {
            data.put("greeting", "你好！我是炊小助，又见炊烟的AI助理。有什么需要？");
        }
        return Result.success(data);
    }

    /**
     * 从 JWT 拦截器注入的 request 属性中解析 staffId。
     */
    private Long resolveStaffId(HttpServletRequest request) {
        Object staffId = request.getAttribute("jwt_staff_id");
        if (staffId instanceof Long) {
            return (Long) staffId;
        }
        if (staffId instanceof Number) {
            return ((Number) staffId).longValue();
        }
        return null;
    }
}

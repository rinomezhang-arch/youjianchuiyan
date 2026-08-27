package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 官网首页"门店选择"用的门店信息。免登录公开接口，见 WebMvcConfig 放行配置。
 * 只返回地址/电话/营业时间这几个本来就该公开的字段。
 */
@RestController
@RequestMapping("/api/public/stores")
@CrossOrigin(origins = "*")
public class PublicStoreController {

    @Autowired
    private JdbcTemplate jdbc;

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT store_id, store_name, store_short_name, address, phone, business_hours " +
                "FROM store_info WHERE status = 'open' ORDER BY sort_order, store_id");
        return Result.success(rows);
    }
}

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
        // latitude / longitude 这两列**在实体和全量 schema 里都不存在**，
        // 查它们会让整个接口在真实库上直接 Unknown column 报错——H5 首页因此打不开。
        // 这里只查确实存在的字段。地图定位需要经纬度的话，得先有列，属独立任务，不在此处臆造。
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT store_id, store_name, store_short_name, address, phone, business_hours " +
                "FROM store_info WHERE status = 'open' ORDER BY sort_order, store_id");
        return Result.success(rows);
    }
}

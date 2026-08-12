package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.HrStaff;
import com.youjian.banquet.service.HrStaffService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * HR员工管理控制器
 * 复刻自HR系统 StaffController，@RequestMapping 改为 /api/hr/staff
 *
 * @author cow
 * @since 2022-01-27
 */
@RestController
@RequestMapping("/api/hr-admin/staff")
@CrossOrigin(origins = "*")
public class HrStaffController {

    @Autowired
    private HrStaffService hrStaffService;

    /**
     * 新增员工
     */
    @PostMapping
    public Result<HrStaff> add(@RequestBody HrStaff staff) {
        return hrStaffService.add(staff);
    }

    /**
     * 逻辑删除
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteById(@PathVariable Integer id) {
        return hrStaffService.deleteById(id);
    }

    /**
     * 批量逻辑删除
     */
    @DeleteMapping("/batch/{ids}")
    public Result<String> deleteBatch(@PathVariable List<Integer> ids) {
        return hrStaffService.deleteBatch(ids);
    }

    /**
     * 编辑更新
     */
    @PutMapping
    public Result<HrStaff> edit(@RequestBody HrStaff staff) {
        return hrStaffService.edit(staff);
    }

    /**
     * 按ID查询
     */
    @GetMapping("/{id}")
    public Result<HrStaff> findById(@PathVariable Integer id) {
        return hrStaffService.findById(id);
    }

    /**
     * 查询员工详细信息（含部门名称）
     */
    @GetMapping("/info/{id}")
    public Result<Map<String, Object>> findInfoById(@PathVariable Integer id) {
        return hrStaffService.findInfoById(id);
    }

    /**
     * 多条件分页查询
     */
    @PostMapping("/page")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestBody HrStaff staff) {
        return hrStaffService.list(current, size, staff);
    }

    /**
     * 数据导出（CSV格式）
     */
    @GetMapping("/export")
    public Result<String> export(HttpServletResponse response) throws IOException {
        return hrStaffService.export(response);
    }

    /**
     * 数据导入（CSV格式）
     */
    @PostMapping("/import")
    public Result<String> imp(@RequestParam("file") MultipartFile file) throws IOException {
        return hrStaffService.imp(file);
    }

    /**
     * 检查员工的密码
     */
    @GetMapping("/check/{pwd}/{id}")
    public Result<String> checkPassword(@PathVariable String pwd, @PathVariable Integer id) {
        return hrStaffService.checkPassword(pwd, id);
    }

    /**
     * 更新密码
     */
    @PutMapping("/pwd")
    public Result<String> updatePassword(@RequestBody HrStaff staff) {
        return hrStaffService.updatePassword(staff);
    }
}
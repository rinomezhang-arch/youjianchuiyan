package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.HrDept;
import com.youjian.banquet.service.HrDeptService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * HR部门管理控制器
 * 复刻自HR系统 DeptController，@RequestMapping 改为 /api/hr/dept
 *
 * @author cow
 * @since 2022-03-07
 */
@RestController
@RequestMapping("/api/hr-admin/dept")
@CrossOrigin(origins = "*")
public class HrDeptController {

    @Autowired
    private HrDeptService hrDeptService;

    /**
     * 新增部门
     */
    @PostMapping
    public Result<HrDept> add(@RequestBody HrDept dept) {
        return hrDeptService.add(dept);
    }

    /**
     * 逻辑删除
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        return hrDeptService.deleteById(id);
    }

    /**
     * 批量逻辑删除
     */
    @DeleteMapping("/batch/{ids}")
    public Result<String> deleteBatch(@PathVariable List<Integer> ids) {
        return hrDeptService.deleteBatch(ids);
    }

    /**
     * 编辑更新
     */
    @PutMapping
    public Result<HrDept> edit(@RequestBody HrDept dept) {
        return hrDeptService.edit(dept);
    }

    /**
     * 按ID查询
     */
    @GetMapping("/{id}")
    public Result<HrDept> findById(@PathVariable Integer id) {
        return hrDeptService.findById(id);
    }

    /**
     * 查询所有部门（树形结构）
     */
    @GetMapping("/all")
    public Result<List<HrDept>> findAll() {
        return hrDeptService.findAll();
    }

    /**
     * 查询所有子部门
     */
    @GetMapping("/all/sub")
    public Result<List<HrDept>> findAllSubDept() {
        return hrDeptService.findAllSubDept();
    }

    /**
     * 分页条件查询
     */
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name) {
        return hrDeptService.list(current, size, name);
    }

    /**
     * 数据导出（CSV格式）
     */
    @GetMapping("/export")
    public Result<String> export(HttpServletResponse response) throws IOException {
        return hrDeptService.export(response);
    }

    /**
     * 数据导入（CSV格式）
     */
    @PostMapping("/import")
    public Result<String> imp(@RequestParam("file") MultipartFile file) throws IOException {
        return hrDeptService.imp(file);
    }
}
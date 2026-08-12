package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.HrDocs;
import com.youjian.banquet.service.HrDocsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * HR文件管理控制器
 * 复刻自HR系统 DocsController，@RequestMapping 改为 /api/hr/docs
 *
 * @author cow
 * @since 2022-02-24
 */
@RestController
@RequestMapping("/api/hr-admin/docs")
@CrossOrigin(origins = "*")
public class HrDocsController {

    @Autowired
    private HrDocsService hrDocsService;

    /**
     * 新增文件记录
     */
    @PostMapping
    public Result<HrDocs> add(@RequestBody HrDocs docs) {
        return hrDocsService.add(docs);
    }

    /**
     * 逻辑删除
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteById(@PathVariable Integer id) {
        return hrDocsService.deleteById(id);
    }

    /**
     * 批量逻辑删除
     */
    @DeleteMapping("/batch/{ids}")
    public Result<String> deleteBatch(@PathVariable List<Integer> ids) {
        return hrDocsService.deleteBatch(ids);
    }

    /**
     * 编辑更新
     */
    @PutMapping
    public Result<HrDocs> edit(@RequestBody HrDocs docs) {
        return hrDocsService.edit(docs);
    }

    /**
     * 按ID查询
     */
    @GetMapping("/{id}")
    public Result<HrDocs> findById(@PathVariable Integer id) {
        return hrDocsService.findById(id);
    }

    /**
     * 分页条件查询
     */
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String oldName,
            @RequestParam(required = false) String staffName) {
        return hrDocsService.list(current, size, oldName, staffName);
    }

    /**
     * 数据导出（CSV格式）
     */
    @GetMapping("/export")
    public Result<String> export(HttpServletResponse response) throws IOException {
        return hrDocsService.export(response);
    }

    /**
     * 数据导入（CSV格式）
     */
    @PostMapping("/import")
    public Result<String> imp(@RequestParam("file") MultipartFile file) throws IOException {
        return hrDocsService.imp(file);
    }

    /**
     * 文件上传
     */
    @PostMapping("/upload")
    public Result<HrDocs> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Integer staffId) throws Exception {
        return hrDocsService.upload(file, storeId, staffId);
    }

    /**
     * 文件下载
     */
    @GetMapping("/download/{filename}")
    public Result<String> download(@PathVariable String filename, HttpServletResponse response) throws Exception {
        return hrDocsService.download(filename, response);
    }
}
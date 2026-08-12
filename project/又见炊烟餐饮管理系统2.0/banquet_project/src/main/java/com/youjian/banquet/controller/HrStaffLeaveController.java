package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.HrStaffLeave;
import com.youjian.banquet.service.HrStaffLeaveService;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 员工请假控制器
 * 对应参考系统: StaffLeaveController
 * 完整保留参考系统的所有API接口
 */
@RestController
@RequestMapping("/api/hr-admin/staff-leave")
@CrossOrigin(origins = "*")
public class HrStaffLeaveController {

    @Autowired
    private HrStaffLeaveService hrStaffLeaveService;

    /**
     * 新增员工请假
     * 对应参考系统：POST /staff-leave → add
     */
    @PostMapping
    public Result<HrStaffLeave> add(@RequestBody HrStaffLeave staffLeave) {
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) storeId = 1L;
        staffLeave.setStoreId(storeId);
        return hrStaffLeaveService.add(staffLeave);
    }

    /**
     * 逻辑删除
     * 对应参考系统：DELETE /staff-leave/{id} → delete
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        return hrStaffLeaveService.deleteById(id);
    }

    /**
     * 批量逻辑删除
     * 对应参考系统：DELETE /staff-leave/batch/{ids} → deleteBatch
     */
    @DeleteMapping("/batch/{ids}")
    public Result<Void> deleteBatch(@PathVariable List<Integer> ids) {
        return hrStaffLeaveService.deleteBatch(ids);
    }

    /**
     * 编辑更新（审核通过后自动设置考勤状态为休假）
     * 对应参考系统：PUT /staff-leave → edit
     */
    @PutMapping
    public Result<HrStaffLeave> edit(@RequestBody HrStaffLeave staffLeave) {
        return hrStaffLeaveService.edit(staffLeave);
    }

    /**
     * 根据ID查询
     * 对应参考系统：GET /staff-leave/{id} → findById
     */
    @GetMapping("/detail/{id}")
    public Result<HrStaffLeave> findById(@PathVariable Integer id) {
        return hrStaffLeaveService.findById(id);
    }

    /**
     * 分页条件查询
     * 对应参考系统：GET /staff-leave?current=&size=&name=&deptId= → list
     */
    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") Integer current,
                                            @RequestParam(defaultValue = "10") Integer size,
                                            @RequestParam(required = false) String name,
                                            @RequestParam(required = false) Integer deptId) {
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) storeId = 1L;
        return hrStaffLeaveService.list(storeId, current, size, name, deptId);
    }

    /**
     * 数据导出接口
     * 对应参考系统：GET /staff-leave/export → export
     */
    @GetMapping("/export")
    public Result<Void> export(HttpServletResponse response) throws IOException {
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) storeId = 1L;
        return hrStaffLeaveService.export(response, storeId);
    }

    /**
     * 数据导入接口
     * 对应参考系统：POST /staff-leave/import → imp
     */
    @PostMapping("/import")
    public Result<Void> imp(@RequestParam("file") MultipartFile file) throws IOException {
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) storeId = 1L;
        return hrStaffLeaveService.imp(file, storeId);
    }

    /**
     * 根据员工ID分页查询请假记录
     * 对应参考系统：GET /staff-leave/staff?current=&size=&id= → findByStaffId
     */
    @GetMapping("/staff")
    public Result<Map<String, Object>> findByStaffId(@RequestParam(defaultValue = "1") Integer current,
                                                     @RequestParam(defaultValue = "10") Integer size,
                                                     @RequestParam(required = false) Integer id) {
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) storeId = 1L;
        return hrStaffLeaveService.findByStaffId(storeId, current, size, id);
    }

    /**
     * 查询未被审核的请假申请
     * 对应参考系统：GET /staff-leave/staff/{id} → findUnauditedByStaffId
     */
    @GetMapping("/staff/{id}")
    public Result<Void> findUnauditedByStaffId(@PathVariable Integer id) {
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) storeId = 1L;
        return hrStaffLeaveService.findUnauditedByStaffId(storeId, id);
    }

    /**
     * 获取所有审核状态枚举
     * 对应参考系统：GET /staff-leave/all → findAll
     */
    @GetMapping("/all")
    public Result<List<Map<String, Object>>> findAll() {
        return hrStaffLeaveService.findAll();
    }
}
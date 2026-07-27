package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.Staff;
import com.youjian.banquet.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hr")
@CrossOrigin(origins = "*")
public class StaffController {

    @Autowired
    private StaffRepository staffRepository;

    /**
     * 获取全部在职员工列表（考勤日历下拉用）
     * GET /api/hr/staff
     */
    @GetMapping("/staff")
    public Result<List<Staff>> getAllStaff() {
        try {
            List<Staff> list = staffRepository.findByEmploymentStatus("active");
            if (list.isEmpty()) {
                list = staffRepository.findAll();
            }
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "获取员工列表失败: " + e.getMessage());
        }
    }

    /** POST /api/hr/staff — create staff */
    @PostMapping("/staff")
    public Result<Staff> createStaff(@RequestBody Staff staff) {
        try {
            if (staff.getStoreId() == null) staff.setStoreId(1L);
            if (staff.getEmploymentStatus() == null) staff.setEmploymentStatus("active");
            staff.setCreatedAt(null);
            staff.setUpdatedAt(null);
            Staff saved = staffRepository.save(staff);
            return Result.success(saved);
        } catch (Exception e) {
            return Result.error(500, "创建员工失败: " + e.getMessage());
        }
    }

    /** PUT /api/hr/staff/{id} — update staff */
    @PutMapping("/staff/{id}")
    public Result<Staff> updateStaff(@PathVariable Integer id, @RequestBody Staff staff) {
        try {
            Staff existing = staffRepository.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "员工不存在");
            if (staff.getStaffName() != null) existing.setStaffName(staff.getStaffName());
            if (staff.getStaffGender() != null) existing.setStaffGender(staff.getStaffGender());
            if (staff.getStaffPhone() != null) existing.setStaffPhone(staff.getStaffPhone());
            if (staff.getStaffPosition() != null) existing.setStaffPosition(staff.getStaffPosition());
            if (staff.getDepartment() != null) existing.setDepartment(staff.getDepartment());
            if (staff.getDeptId() != null) existing.setDeptId(staff.getDeptId());
            if (staff.getStaffRank() != null) existing.setStaffRank(staff.getStaffRank());
            if (staff.getEmploymentType() != null) existing.setEmploymentType(staff.getEmploymentType());
            if (staff.getHireDate() != null) existing.setHireDate(staff.getHireDate());
            if (staff.getBasicSalary() != null) existing.setBasicSalary(staff.getBasicSalary());
            if (staff.getPerformanceSalary() != null) existing.setPerformanceSalary(staff.getPerformanceSalary());
            if (staff.getSubsidy() != null) existing.setSubsidy(staff.getSubsidy());
            if (staff.getBonus() != null) existing.setBonus(staff.getBonus());
            if (staff.getSocialInsurance() != null) existing.setSocialInsurance(staff.getSocialInsurance());
            if (staff.getHousingFund() != null) existing.setHousingFund(staff.getHousingFund());
            if (staff.getBankName() != null) existing.setBankName(staff.getBankName());
            if (staff.getBankAccount() != null) existing.setBankAccount(staff.getBankAccount());
            if (staff.getEducation() != null) existing.setEducation(staff.getEducation());
            if (staff.getMajor() != null) existing.setMajor(staff.getMajor());
            if (staff.getEmail() != null) existing.setEmail(staff.getEmail());
            if (staff.getWechat() != null) existing.setWechat(staff.getWechat());
            if (staff.getNation() != null) existing.setNation(staff.getNation());
            if (staff.getBirthDate() != null) existing.setBirthDate(staff.getBirthDate());
            if (staff.getNativePlace() != null) existing.setNativePlace(staff.getNativePlace());
            if (staff.getMaritalStatus() != null) existing.setMaritalStatus(staff.getMaritalStatus());
            if (staff.getPoliticalStatus() != null) existing.setPoliticalStatus(staff.getPoliticalStatus());
            if (staff.getHomeAddress() != null) existing.setHomeAddress(staff.getHomeAddress());
            if (staff.getEmergencyContact() != null) existing.setEmergencyContact(staff.getEmergencyContact());
            if (staff.getEmergencyPhone() != null) existing.setEmergencyPhone(staff.getEmergencyPhone());
            if (staff.getEmploymentStatus() != null) existing.setEmploymentStatus(staff.getEmploymentStatus());
            if (staff.getRole() != null) existing.setRole(staff.getRole());
            if (staff.getPermissionLevel() != null) existing.setPermissionLevel(staff.getPermissionLevel());
            if (staff.getRemark() != null) existing.setRemark(staff.getRemark());
            existing.setUpdatedAt(null);
            Staff saved = staffRepository.save(existing);
            return Result.success(saved);
        } catch (Exception e) {
            return Result.error(500, "更新员工失败: " + e.getMessage());
        }
    }

    /** DELETE /api/hr/staff/{id} — soft-delete staff */
    @DeleteMapping("/staff/{id}")
    public Result<?> deleteStaff(@PathVariable Integer id) {
        try {
            Staff existing = staffRepository.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "员工不存在");
            existing.setEmploymentStatus("resigned");
            staffRepository.save(existing);
            return Result.success("员工已标记离职");
        } catch (Exception e) {
            return Result.error(500, "删除员工失败: " + e.getMessage());
        }
    }
}

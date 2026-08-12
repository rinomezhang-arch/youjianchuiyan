package com.youjian.banquet.service;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.HrStaffRole;
import com.youjian.banquet.repository.HrStaffRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * HR员工角色关系服务
 * 完整保留原系统 StaffRoleService 的RBAC权限逻辑（员工设置角色、获取员工角色）
 * 原框架：MyBatis Plus → 现框架：JPA
 */
@Service
public class HrStaffRoleService {

    @Autowired
    private HrStaffRoleRepository staffRoleRepository;

    /**
     * 为员工设置角色
     * 完整保留原系统 StaffRoleService.setRole(Integer, List<Integer>) 逻辑：
     * 1. 先禁用不需要的角色（将status设为0）
     * 2. 再启用或新增需要的角色（saveOrUpdate逻辑）
     *
     * @param staffId 员工ID
     * @param roleIds 需要启用的角色ID列表
     * @param storeId 门店ID（多租户隔离）
     */
    @Transactional
    public Result<Void> setRole(Integer staffId, List<Integer> roleIds, Long storeId) {
        // 查询该员工当前所有角色关联（含禁用的）
        List<HrStaffRole> existingList = staffRoleRepository.findByStoreIdAndStaffId(storeId, staffId);

        // 先禁用不需要的角色
        for (HrStaffRole staffRole : existingList) {
            if (roleIds.contains(staffRole.getRoleId())) {
                staffRole.setStatus(1); // 启用
            } else {
                staffRole.setStatus(0); // 禁用
            }
            staffRoleRepository.save(staffRole);
        }

        // 根据条件添加或更新（saveOrUpdate逻辑）
        for (Integer roleId : roleIds) {
            HrStaffRole existing = staffRoleRepository.findByStoreIdAndStaffIdAndRoleId(storeId, staffId, roleId);
            if (existing != null) {
                // 已存在，确保启用
                existing.setStatus(1);
                staffRoleRepository.save(existing);
            } else {
                // 不存在，新增
                HrStaffRole staffRole = new HrStaffRole();
                staffRole.setStoreId(storeId);
                staffRole.setStaffId(staffId);
                staffRole.setRoleId(roleId);
                staffRole.setStatus(1);
                staffRoleRepository.save(staffRole);
            }
        }

        return Result.success();
    }

    /**
     * 获取员工的角色（仅返回启用的）
     * 完整保留原系统 StaffRoleService.getRole(Integer) 逻辑
     *
     * @param staffId 员工ID
     * @param storeId 门店ID
     */
    public Result<List<HrStaffRole>> getRole(Integer staffId, Long storeId) {
        List<HrStaffRole> list = staffRoleRepository.findByStoreIdAndStaffIdAndStatus(storeId, staffId, 1);
        return Result.success(list);
    }

    /**
     * 获取员工的角色ID列表（仅返回启用的角色ID）
     * 用于前端回显员工已分配的角色
     */
    public Result<List<Integer>> getRoleIds(Integer staffId, Long storeId) {
        List<HrStaffRole> list = staffRoleRepository.findByStoreIdAndStaffIdAndStatus(storeId, staffId, 1);
        List<Integer> roleIds = list.stream()
                .map(HrStaffRole::getRoleId)
                .toList();
        return Result.success(roleIds);
    }
}
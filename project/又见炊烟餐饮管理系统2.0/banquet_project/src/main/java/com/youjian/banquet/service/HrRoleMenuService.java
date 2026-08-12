package com.youjian.banquet.service;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.HrRoleMenu;
import com.youjian.banquet.repository.HrRoleMenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * HR角色菜单关系服务
 * 完整保留原系统 RoleMenuService 的RBAC权限逻辑（角色设置菜单、获取角色菜单）
 * 原框架：MyBatis Plus → 现框架：JPA
 */
@Service
public class HrRoleMenuService {

    @Autowired
    private HrRoleMenuRepository roleMenuRepository;

    /**
     * 为角色设置菜单
     * 完整保留原系统 RoleMenuService.setMenu(Integer, List<Integer>) 逻辑：
     * 1. 先禁用不需要的菜单（将status设为0）
     * 2. 再启用或新增需要的菜单（saveOrUpdate逻辑）
     *
     * @param roleId  角色ID
     * @param menuIds 需要启用的菜单ID列表
     * @param storeId 门店ID（多租户隔离）
     */
    @Transactional
    public Result<Void> setMenu(Integer roleId, List<Integer> menuIds, Long storeId) {
        // 查询该角色当前所有菜单关联（含禁用的）
        List<HrRoleMenu> existingList = roleMenuRepository.findByStoreIdAndRoleId(storeId, roleId);

        // 先禁用不需要的菜单
        for (HrRoleMenu roleMenu : existingList) {
            if (menuIds.contains(roleMenu.getMenuId())) {
                roleMenu.setStatus(1); // 启用
            } else {
                roleMenu.setStatus(0); // 禁用
            }
            roleMenuRepository.save(roleMenu);
        }

        // 根据条件添加或更新（saveOrUpdate逻辑）
        for (Integer menuId : menuIds) {
            HrRoleMenu existing = roleMenuRepository.findByStoreIdAndRoleIdAndMenuId(storeId, roleId, menuId);
            if (existing != null) {
                // 已存在，确保启用
                existing.setStatus(1);
                roleMenuRepository.save(existing);
            } else {
                // 不存在，新增
                HrRoleMenu roleMenu = new HrRoleMenu();
                roleMenu.setStoreId(storeId);
                roleMenu.setRoleId(roleId);
                roleMenu.setMenuId(menuId);
                roleMenu.setStatus(1);
                roleMenuRepository.save(roleMenu);
            }
        }

        return Result.success();
    }

    /**
     * 获取角色的菜单（仅返回启用的）
     * 完整保留原系统 RoleMenuService.getMenu(Integer) 逻辑
     *
     * @param roleId  角色ID
     * @param storeId 门店ID
     */
    public Result<List<HrRoleMenu>> getMenu(Integer roleId, Long storeId) {
        List<HrRoleMenu> list = roleMenuRepository.findByStoreIdAndRoleIdAndStatus(storeId, roleId, 1);
        return Result.success(list);
    }

    /**
     * 获取角色的菜单ID列表（仅返回启用的菜单ID）
     * 用于前端回显角色已分配的菜单
     */
    public Result<List<Integer>> getMenuIds(Integer roleId, Long storeId) {
        List<HrRoleMenu> list = roleMenuRepository.findByStoreIdAndRoleIdAndStatus(storeId, roleId, 1);
        List<Integer> menuIds = list.stream()
                .map(HrRoleMenu::getMenuId)
                .toList();
        return Result.success(menuIds);
    }
}
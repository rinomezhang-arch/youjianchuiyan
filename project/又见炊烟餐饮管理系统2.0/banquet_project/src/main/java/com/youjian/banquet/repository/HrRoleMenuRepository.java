package com.youjian.banquet.repository;

import com.youjian.banquet.entity.HrRoleMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * HR角色菜单关系 Repository
 */
@Repository
public interface HrRoleMenuRepository extends JpaRepository<HrRoleMenu, Integer>, JpaSpecificationExecutor<HrRoleMenu> {

    /**
     * 按门店ID和角色ID查询，且状态为启用
     */
    List<HrRoleMenu> findByStoreIdAndRoleIdAndStatus(Long storeId, Integer roleId, Integer status);

    /**
     * 按门店ID和角色ID查询所有（含禁用）
     */
    List<HrRoleMenu> findByStoreIdAndRoleId(Long storeId, Integer roleId);

    /**
     * 按门店ID、角色ID和菜单ID查询
     */
    HrRoleMenu findByStoreIdAndRoleIdAndMenuId(Long storeId, Integer roleId, Integer menuId);

    /**
     * 按门店ID和状态查询
     */
    List<HrRoleMenu> findByStoreIdAndStatus(Long storeId, Integer status);
}
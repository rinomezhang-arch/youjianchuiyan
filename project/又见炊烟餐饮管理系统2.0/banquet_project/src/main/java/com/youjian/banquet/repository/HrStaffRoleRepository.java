package com.youjian.banquet.repository;

import com.youjian.banquet.entity.HrStaffRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * HR员工角色关系 Repository
 */
@Repository
public interface HrStaffRoleRepository extends JpaRepository<HrStaffRole, Integer>, JpaSpecificationExecutor<HrStaffRole> {

    /**
     * 按门店ID和员工ID查询，且状态为启用
     */
    List<HrStaffRole> findByStoreIdAndStaffIdAndStatus(Long storeId, Integer staffId, Integer status);

    /**
     * 按门店ID和员工ID查询所有（含禁用）
     */
    List<HrStaffRole> findByStoreIdAndStaffId(Long storeId, Integer staffId);

    /**
     * 按门店ID、员工ID和角色ID查询
     */
    HrStaffRole findByStoreIdAndStaffIdAndRoleId(Long storeId, Integer staffId, Integer roleId);

    /**
     * 按门店ID和状态查询
     */
    List<HrStaffRole> findByStoreIdAndStatus(Long storeId, Integer status);
}
package com.youjian.banquet.repository;

import com.youjian.banquet.entity.HrRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * HR角色 Repository
 */
@Repository
public interface HrRoleRepository extends JpaRepository<HrRole, Integer>, JpaSpecificationExecutor<HrRole> {

    /**
     * 按门店ID查询所有角色
     */
    List<HrRole> findByStoreId(Long storeId);

    /**
     * 按门店ID和编码查询
     */
    HrRole findByStoreIdAndCode(Long storeId, String code);
}
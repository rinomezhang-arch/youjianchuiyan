package com.youjian.banquet.repository;

import com.youjian.banquet.entity.HrMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * HR菜单 Repository
 */
@Repository
public interface HrMenuRepository extends JpaRepository<HrMenu, Integer>, JpaSpecificationExecutor<HrMenu> {

    /**
     * 按门店ID查询所有菜单
     */
    List<HrMenu> findByStoreId(Long storeId);

    /**
     * 按门店ID和父菜单ID查询
     */
    List<HrMenu> findByStoreIdAndParentId(Long storeId, Integer parentId);

    /**
     * 按门店ID和编码查询
     */
    HrMenu findByStoreIdAndCode(Long storeId, String code);
}
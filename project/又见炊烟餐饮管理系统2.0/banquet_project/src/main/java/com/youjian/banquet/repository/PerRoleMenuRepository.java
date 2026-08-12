package com.youjian.banquet.repository;

import com.youjian.banquet.entity.PerRoleMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerRoleMenuRepository extends JpaRepository<PerRoleMenu, Integer>, JpaSpecificationExecutor<PerRoleMenu> {

    List<PerRoleMenu> findByStoreIdAndIsDeleted(Long storeId, Integer isDeleted);

    List<PerRoleMenu> findByStoreIdAndIsDeletedOrderByCreateTimeDesc(Long storeId, Integer isDeleted);

    List<PerRoleMenu> findByStoreIdAndRoleIdAndIsDeleted(Long storeId, Integer roleId, Integer isDeleted);

    List<PerRoleMenu> findByStoreIdAndMenuIdAndIsDeleted(Long storeId, Integer menuId, Integer isDeleted);
}
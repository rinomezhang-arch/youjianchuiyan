package com.youjian.banquet.repository;

import com.youjian.banquet.entity.PerMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerMenuRepository extends JpaRepository<PerMenu, Integer>, JpaSpecificationExecutor<PerMenu> {

    List<PerMenu> findByStoreIdAndIsDeleted(Long storeId, Integer isDeleted);

    List<PerMenu> findByStoreIdAndIsDeletedOrderByCreateTimeDesc(Long storeId, Integer isDeleted);

    List<PerMenu> findByStoreIdAndParentIdAndIsDeleted(Long storeId, Integer parentId, Integer isDeleted);

    List<PerMenu> findByStoreIdAndParentIdAndIsDeletedOrderByCreateTimeAsc(Long storeId, Integer parentId, Integer isDeleted);
}
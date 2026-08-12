package com.youjian.banquet.repository;

import com.youjian.banquet.entity.PerRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerRoleRepository extends JpaRepository<PerRole, Integer>, JpaSpecificationExecutor<PerRole> {

    List<PerRole> findByStoreIdAndIsDeleted(Long storeId, Integer isDeleted);

    List<PerRole> findByStoreIdAndIsDeletedOrderByCreateTimeDesc(Long storeId, Integer isDeleted);
}
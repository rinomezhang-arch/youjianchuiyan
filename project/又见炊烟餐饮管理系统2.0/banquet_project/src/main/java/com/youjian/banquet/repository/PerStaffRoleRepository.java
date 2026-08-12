package com.youjian.banquet.repository;

import com.youjian.banquet.entity.PerStaffRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerStaffRoleRepository extends JpaRepository<PerStaffRole, Integer>, JpaSpecificationExecutor<PerStaffRole> {

    List<PerStaffRole> findByStoreIdAndIsDeleted(Long storeId, Integer isDeleted);

    List<PerStaffRole> findByStoreIdAndIsDeletedOrderByCreateTimeDesc(Long storeId, Integer isDeleted);

    List<PerStaffRole> findByStoreIdAndStaffIdAndIsDeleted(Long storeId, Integer staffId, Integer isDeleted);

    List<PerStaffRole> findByStoreIdAndRoleIdAndIsDeleted(Long storeId, Integer roleId, Integer isDeleted);
}
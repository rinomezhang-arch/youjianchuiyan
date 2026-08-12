package com.youjian.banquet.repository;

import com.youjian.banquet.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType, Integer>, JpaSpecificationExecutor<LeaveType> {

    List<LeaveType> findByStoreIdAndIsDeleted(Long storeId, Integer isDeleted);

    List<LeaveType> findByStoreIdAndIsDeletedOrderByCreateTimeDesc(Long storeId, Integer isDeleted);

    List<LeaveType> findByStoreIdAndDeptIdAndIsDeleted(Long storeId, Integer deptId, Integer isDeleted);

    Optional<LeaveType> findByStoreIdAndDeptIdAndTypeNumAndIsDeleted(Long storeId, Integer deptId, Integer typeNum, Integer isDeleted);
}
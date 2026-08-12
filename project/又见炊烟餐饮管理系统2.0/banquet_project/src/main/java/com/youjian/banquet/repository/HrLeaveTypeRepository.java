package com.youjian.banquet.repository;

import com.youjian.banquet.entity.HrLeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 请假类型配置 Repository
 * 对应参考系统: LeaveMapper
 */
@Repository
public interface HrLeaveTypeRepository extends JpaRepository<HrLeaveType, Integer>, JpaSpecificationExecutor<HrLeaveType> {

    List<HrLeaveType> findByStoreIdAndIsDeleted(Long storeId, Integer isDeleted);

    List<HrLeaveType> findByStoreIdAndIsDeletedOrderByCreateTimeDesc(Long storeId, Integer isDeleted);

    List<HrLeaveType> findByDeptIdAndIsDeleted(Integer deptId, Integer isDeleted);

    List<HrLeaveType> findByStoreIdAndDeptIdAndIsDeleted(Long storeId, Integer deptId, Integer isDeleted);

    Optional<HrLeaveType> findByStoreIdAndDeptIdAndTypeNumAndIsDeleted(Long storeId, Integer deptId, Integer typeNum, Integer isDeleted);

    List<HrLeaveType> findByStoreIdAndDeptIdAndTypeNumAndIsDeletedOrderByCreateTimeDesc(Long storeId, Integer deptId, Integer typeNum, Integer isDeleted);
}
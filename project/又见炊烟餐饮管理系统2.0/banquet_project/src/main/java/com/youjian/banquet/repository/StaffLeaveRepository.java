package com.youjian.banquet.repository;

import com.youjian.banquet.entity.StaffLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffLeaveRepository extends JpaRepository<StaffLeave, Integer>, JpaSpecificationExecutor<StaffLeave> {

    List<StaffLeave> findByStoreIdAndIsDeleted(Long storeId, Integer isDeleted);

    List<StaffLeave> findByStoreIdAndIsDeletedOrderByCreateTimeDesc(Long storeId, Integer isDeleted);

    List<StaffLeave> findByStoreIdAndStaffIdAndIsDeleted(Long storeId, Integer staffId, Integer isDeleted);

    Optional<StaffLeave> findByStoreIdAndStaffIdAndTypeNumAndIsDeleted(Long storeId, Integer staffId, Integer typeNum, Integer isDeleted);
}
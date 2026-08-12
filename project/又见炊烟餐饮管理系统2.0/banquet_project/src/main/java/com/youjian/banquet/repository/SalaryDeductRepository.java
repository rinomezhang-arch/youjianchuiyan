package com.youjian.banquet.repository;

import com.youjian.banquet.entity.SalaryDeduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryDeductRepository extends JpaRepository<SalaryDeduct, Integer>, JpaSpecificationExecutor<SalaryDeduct> {

    List<SalaryDeduct> findByStoreIdAndIsDeleted(Long storeId, Integer isDeleted);

    List<SalaryDeduct> findByStoreIdAndIsDeletedOrderByCreateTimeDesc(Long storeId, Integer isDeleted);

    List<SalaryDeduct> findByStoreIdAndDeptIdAndIsDeleted(Long storeId, Integer deptId, Integer isDeleted);

    Optional<SalaryDeduct> findByStoreIdAndDeptIdAndTypeNumAndIsDeleted(Long storeId, Integer deptId, Integer typeNum, Integer isDeleted);

    Optional<SalaryDeduct> findByStoreIdAndDeptIdAndTypeNum(Long storeId, Integer deptId, Integer typeNum);
}
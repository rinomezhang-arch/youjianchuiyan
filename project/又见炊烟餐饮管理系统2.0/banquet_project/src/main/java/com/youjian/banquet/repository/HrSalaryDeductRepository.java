package com.youjian.banquet.repository;

import com.youjian.banquet.entity.HrSalaryDeduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HrSalaryDeductRepository extends JpaRepository<HrSalaryDeduct, Integer> {

    List<HrSalaryDeduct> findByStoreId(Long storeId);

    List<HrSalaryDeduct> findByStoreIdAndDeptId(Long storeId, Integer deptId);

    Optional<HrSalaryDeduct> findByStoreIdAndDeptIdAndTypeNum(Long storeId, Integer deptId, Integer typeNum);

    List<HrSalaryDeduct> findByStoreIdAndTypeNum(Long storeId, Integer typeNum);
}
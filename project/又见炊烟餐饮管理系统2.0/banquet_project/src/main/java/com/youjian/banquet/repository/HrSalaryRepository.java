package com.youjian.banquet.repository;

import com.youjian.banquet.entity.HrSalary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HrSalaryRepository extends JpaRepository<HrSalary, Integer>, JpaSpecificationExecutor<HrSalary> {

    List<HrSalary> findByStoreId(Long storeId);

    List<HrSalary> findByStoreIdAndMonth(Long storeId, String month);

    Optional<HrSalary> findByStoreIdAndStaffIdAndMonth(Long storeId, Integer staffId, String month);

    Optional<HrSalary> findByStaffIdAndMonth(Integer staffId, String month);
}
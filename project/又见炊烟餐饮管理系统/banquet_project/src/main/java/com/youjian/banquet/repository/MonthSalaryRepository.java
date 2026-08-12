package com.youjian.banquet.repository;

import com.youjian.banquet.entity.MonthSalary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonthSalaryRepository extends JpaRepository<MonthSalary, Long> {

    List<MonthSalary> findByStoreId(Long storeId);

    List<MonthSalary> findByStoreIdAndSalaryMonth(Long storeId, String salaryMonth);

    Optional<MonthSalary> findByStaffIdAndSalaryMonth(Long staffId, String salaryMonth);

    List<MonthSalary> findByStaffIdAndSalaryMonthIn(Long staffId, List<String> salaryMonths);

    List<MonthSalary> findByStoreIdAndSalaryMonthAndStatus(Long storeId, String salaryMonth, Integer status);
}

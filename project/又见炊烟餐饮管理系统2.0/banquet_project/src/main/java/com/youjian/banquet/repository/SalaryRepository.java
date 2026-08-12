package com.youjian.banquet.repository;

import com.youjian.banquet.entity.Salary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryRepository extends JpaRepository<Salary, Integer>, JpaSpecificationExecutor<Salary> {

    List<Salary> findByStoreId(Long storeId);

    List<Salary> findByStoreIdAndIsDeleted(Long storeId, Integer isDeleted);

    List<Salary> findByStoreIdAndIsDeletedOrderByCreateTimeDesc(Long storeId, Integer isDeleted);

    List<Salary> findByStoreIdAndMonth(Long storeId, String month);

    Optional<Salary> findByStoreIdAndStaffIdAndMonth(Long storeId, Integer staffId, String month);

    Optional<Salary> findByStaffIdAndMonth(Integer staffId, String month);
}
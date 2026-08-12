package com.youjian.banquet.repository;

import com.youjian.banquet.entity.HrSocInsurance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HrSocInsuranceRepository extends JpaRepository<HrSocInsurance, Integer>, JpaSpecificationExecutor<HrSocInsurance> {

    List<HrSocInsurance> findByStoreIdAndIsDeleted(Long storeId, Integer isDeleted);

    List<HrSocInsurance> findByStoreIdAndStaffIdAndIsDeleted(Long storeId, Integer staffId, Integer isDeleted);

    Optional<HrSocInsurance> findByStoreIdAndStaffIdAndCityIdAndIsDeleted(Long storeId, Integer staffId, Integer cityId, Integer isDeleted);

    List<HrSocInsurance> findByStoreIdAndStaffIdAndPayMonthAndIsDeleted(Long storeId, Integer staffId, String payMonth, Integer isDeleted);

    List<HrSocInsurance> findByStoreIdAndPayMonthAndIsDeleted(Long storeId, String payMonth, Integer isDeleted);

    List<HrSocInsurance> findByStoreIdAndStatusAndIsDeleted(Long storeId, Integer status, Integer isDeleted);
}
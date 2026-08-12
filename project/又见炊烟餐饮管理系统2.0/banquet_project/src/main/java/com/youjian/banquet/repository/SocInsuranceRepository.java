package com.youjian.banquet.repository;

import com.youjian.banquet.entity.SocInsurance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SocInsuranceRepository extends JpaRepository<SocInsurance, Integer>, JpaSpecificationExecutor<SocInsurance> {

    List<SocInsurance> findByStoreIdAndIsDeleted(Long storeId, Integer isDeleted);

    List<SocInsurance> findByStoreIdAndIsDeletedOrderByCreateTimeDesc(Long storeId, Integer isDeleted);

    List<SocInsurance> findAllByStoreIdAndStaffIdAndIsDeleted(Long storeId, Integer staffId, Integer isDeleted);

    Optional<SocInsurance> findByStoreIdAndStaffIdAndIsDeleted(Long storeId, Integer staffId, Integer isDeleted);

    List<SocInsurance> findByStoreIdAndCityIdAndIsDeleted(Long storeId, Integer cityId, Integer isDeleted);
}
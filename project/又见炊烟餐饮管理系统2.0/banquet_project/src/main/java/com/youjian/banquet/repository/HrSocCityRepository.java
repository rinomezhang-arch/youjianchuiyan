package com.youjian.banquet.repository;

import com.youjian.banquet.entity.HrSocCity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HrSocCityRepository extends JpaRepository<HrSocCity, Integer>, JpaSpecificationExecutor<HrSocCity> {

    List<HrSocCity> findByStoreIdAndIsDeleted(Long storeId, Integer isDeleted);

    List<HrSocCity> findByStoreIdAndIsDeletedOrderByName(Long storeId, Integer isDeleted);

    Optional<HrSocCity> findByStoreIdAndNameAndIsDeleted(Long storeId, String name, Integer isDeleted);

    List<HrSocCity> findByStoreIdAndNameContainingAndIsDeleted(Long storeId, String name, Integer isDeleted);
}
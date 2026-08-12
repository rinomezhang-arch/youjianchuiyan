package com.youjian.banquet.repository;

import com.youjian.banquet.entity.SocCity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SocCityRepository extends JpaRepository<SocCity, Integer>, JpaSpecificationExecutor<SocCity> {

    List<SocCity> findByStoreIdAndIsDeleted(Long storeId, Integer isDeleted);

    List<SocCity> findByStoreIdAndIsDeletedOrderByCreateTimeDesc(Long storeId, Integer isDeleted);

    List<SocCity> findByNameAndIsDeleted(String name, Integer isDeleted);
}
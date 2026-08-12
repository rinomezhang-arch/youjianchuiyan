package com.youjian.banquet.repository;

import com.youjian.banquet.entity.MaterialInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MaterialInfoRepository extends JpaRepository<MaterialInfo, Long>, JpaSpecificationExecutor<MaterialInfo> {

    Optional<MaterialInfo> findByMaterialName(String materialName);

    Optional<MaterialInfo> findBySupplierAccount(String supplierAccount);
}
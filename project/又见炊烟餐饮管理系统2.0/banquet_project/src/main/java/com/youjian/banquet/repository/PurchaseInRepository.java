package com.youjian.banquet.repository;

import com.youjian.banquet.entity.PurchaseIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PurchaseInRepository extends JpaRepository<PurchaseIn, Long>, JpaSpecificationExecutor<PurchaseIn> {

    Optional<PurchaseIn> findByMaterialName(String materialName);

    Optional<PurchaseIn> findBySupplierAccount(String supplierAccount);
}
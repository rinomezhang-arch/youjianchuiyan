package com.youjian.banquet.repository;

import com.youjian.banquet.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long>, JpaSpecificationExecutor<Supplier> {

    Optional<Supplier> findBySupplierAccount(String supplierAccount);

    Optional<Supplier> findBySupplierName(String supplierName);
}
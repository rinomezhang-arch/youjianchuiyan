package com.youjian.banquet.repository;

import com.youjian.banquet.entity.PrMaterialType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrMaterialTypeRepository extends JpaRepository<PrMaterialType, Long>, JpaSpecificationExecutor<PrMaterialType> {

    Optional<PrMaterialType> findByCailiaozhonglei(String cailiaozhonglei);
}
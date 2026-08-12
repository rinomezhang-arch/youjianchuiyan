package com.youjian.banquet.repository;

import com.youjian.banquet.entity.PrConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrConfigRepository extends JpaRepository<PrConfig, Long>, JpaSpecificationExecutor<PrConfig> {

    Optional<PrConfig> findByName(String name);
}
package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BtConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 配置 Repository
 */
@Repository
public interface BtConfigRepository extends JpaRepository<BtConfig, Long>, JpaSpecificationExecutor<BtConfig> {

    Optional<BtConfig> findByName(String name);
}
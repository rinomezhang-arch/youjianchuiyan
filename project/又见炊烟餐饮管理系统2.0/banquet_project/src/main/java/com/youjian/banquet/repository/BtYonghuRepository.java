package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BtYonghu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 普通用户 Repository
 */
@Repository
public interface BtYonghuRepository extends JpaRepository<BtYonghu, Long>, JpaSpecificationExecutor<BtYonghu> {

    Optional<BtYonghu> findByYonghuming(String yonghuming);

    boolean existsByYonghuming(String yonghuming);

    Page<BtYonghu> findByYonghumingContaining(String yonghuming, Pageable pageable);

    Page<BtYonghu> findByXingmingContaining(String xingming, Pageable pageable);
}
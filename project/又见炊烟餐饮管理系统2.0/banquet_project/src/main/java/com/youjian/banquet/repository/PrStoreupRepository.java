package com.youjian.banquet.repository;

import com.youjian.banquet.entity.PrStoreup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrStoreupRepository extends JpaRepository<PrStoreup, Long>, JpaSpecificationExecutor<PrStoreup> {

    List<PrStoreup> findByUserid(Long userid);

    List<PrStoreup> findByRefid(Long refid);

    List<PrStoreup> findByUseridAndRefid(Long userid, Long refid);
}
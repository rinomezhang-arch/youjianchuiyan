package com.youjian.banquet.repository;

import com.youjian.banquet.entity.PrCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrCartRepository extends JpaRepository<PrCart, Long>, JpaSpecificationExecutor<PrCart> {

    List<PrCart> findByUserid(Long userid);

    List<PrCart> findByGoodid(Long goodid);
}
package com.youjian.banquet.repository;

import com.youjian.banquet.entity.PrSupplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrSupplierRepository extends JpaRepository<PrSupplier, Long>, JpaSpecificationExecutor<PrSupplier> {

    Optional<PrSupplier> findByGongyingshangzhanghao(String gongyingshangzhanghao);

    Optional<PrSupplier> findByGongyingshangmingcheng(String gongyingshangmingcheng);
}
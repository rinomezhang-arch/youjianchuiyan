package com.youjian.banquet.repository;

import com.youjian.banquet.entity.PrOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrOrderRepository extends JpaRepository<PrOrder, Long>, JpaSpecificationExecutor<PrOrder> {

    Optional<PrOrder> findByOrderid(String orderid);

    List<PrOrder> findByUserid(Long userid);

    List<PrOrder> findByGongyingshangzhanghao(String gongyingshangzhanghao);

    List<PrOrder> findByStatus(String status);
}
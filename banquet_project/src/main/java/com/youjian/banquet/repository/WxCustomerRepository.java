package com.youjian.banquet.repository;

import com.youjian.banquet.entity.WxCustomer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WxCustomerRepository extends JpaRepository<WxCustomer, Long> {
    Optional<WxCustomer> findByOpenId(String openId);
}

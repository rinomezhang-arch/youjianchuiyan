package com.youjian.banquet.repository;

import com.youjian.banquet.entity.PrAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrAddressRepository extends JpaRepository<PrAddress, Long>, JpaSpecificationExecutor<PrAddress> {

    List<PrAddress> findByUserid(Long userid);

    Optional<PrAddress> findByUseridAndIsdefault(Long userid, String isdefault);
}
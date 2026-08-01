package com.youjian.banquet.repository;

import com.youjian.banquet.entity.StoreInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 门店信息 Repository
 */
@Repository
public interface StoreInfoRepository extends JpaRepository<StoreInfo, Long> {

    Optional<StoreInfo> findByStoreCode(String storeCode);

    List<StoreInfo> findByStatusOrderBySortOrder(String status);

    List<StoreInfo> findAllByOrderBySortOrder();
}

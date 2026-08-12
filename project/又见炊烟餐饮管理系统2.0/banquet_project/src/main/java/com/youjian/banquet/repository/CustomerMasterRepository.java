package com.youjian.banquet.repository;

import com.youjian.banquet.entity.CustomerMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerMasterRepository extends JpaRepository<CustomerMaster, Integer> {

    /** 按门店列出全部客户 */
    List<CustomerMaster> findByStoreId(Long storeId);

    /** 按 customer_id + 门店查找（CustomerService 使用） */
    Optional<CustomerMaster> findByCustomerIdAndStoreId(Integer customerId, Long storeId);

    /** 按电话+门店查找客户（下单时查重） */
    Optional<CustomerMaster> findByCustomerPhoneAndStoreId(String customerPhone, Long storeId);

    /** 按姓名+门店查找客户（电话为空时的降级查重） */
    Optional<CustomerMaster> findByCustomerNameAndStoreId(String customerName, Long storeId);

    /** 关键字搜索（姓名/电话） */
    @Query("SELECT c FROM CustomerMaster c WHERE c.storeId = :storeId AND " +
           "(c.customerName LIKE %:keyword% OR c.customerPhone LIKE %:keyword%)")
    List<CustomerMaster> searchByKeyword(@Param("storeId") Long storeId, @Param("keyword") String keyword);
}

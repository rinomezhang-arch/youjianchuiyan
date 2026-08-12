/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.CustomerMaster
 *  com.youjian.banquet.repository.CustomerMasterRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 *  org.springframework.stereotype.Repository
 */
package com.youjian.banquet.repository;

import com.youjian.banquet.entity.CustomerMaster;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerMasterRepository
extends JpaRepository<CustomerMaster, Integer>,
JpaSpecificationExecutor<CustomerMaster> {
    public List<CustomerMaster> findByStoreId(Long var1);

    public List<CustomerMaster> findByStoreIdAndIsActive(Long var1, Integer var2);

    public Optional<CustomerMaster> findByCustomerIdAndStoreId(Integer var1, Long var2);

    @Query(value="SELECT c FROM CustomerMaster c WHERE c.storeId = :storeId AND (c.customerName LIKE %:keyword% OR c.customerPhone LIKE %:keyword%)")
    public List<CustomerMaster> searchByKeyword(@Param(value="storeId") Long var1, @Param(value="keyword") String var2);

    public Optional<CustomerMaster> findByCustomerPhoneAndStoreId(String var1, Long var2);

    public void deleteByCustomerIdAndStoreId(Integer var1, Long var2);

    public boolean existsByCustomerPhoneAndStoreId(String var1, Long var2);
}


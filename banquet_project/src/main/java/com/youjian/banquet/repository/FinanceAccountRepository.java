package com.youjian.banquet.repository;

import com.youjian.banquet.entity.FinanceAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FinanceAccountRepository extends JpaRepository<FinanceAccount, Long> {

    List<FinanceAccount> findByStoreIdOrderByAccountIdDesc(Long storeId);

    List<FinanceAccount> findByStoreIdAndIsActiveOrderByAccountIdDesc(Long storeId, Boolean isActive);

    List<FinanceAccount> findByStoreIdAndAccountTypeOrderByAccountIdDesc(Long storeId, String accountType);
}

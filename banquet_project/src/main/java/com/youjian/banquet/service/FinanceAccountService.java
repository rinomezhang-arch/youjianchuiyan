package com.youjian.banquet.service;

import com.youjian.banquet.entity.FinanceAccount;
import com.youjian.banquet.repository.FinanceAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 资金账户服务。对应 GET/POST/PUT /api/finance/accounts。
 */
@Service
public class FinanceAccountService {

    @Autowired
    private FinanceAccountRepository financeAccountRepository;

    public List<FinanceAccount> list(Long storeId, String accountType, String status) {
        boolean hasType = accountType != null && !accountType.isEmpty();
        boolean hasStatus = status != null && !status.isEmpty();
        if (hasType && hasStatus) {
            // JPA 无同时按 type+status 的派生方法，退化为按 type 查后在内存过滤
            List<FinanceAccount> list = financeAccountRepository
                    .findByStoreIdAndAccountTypeOrderByAccountIdDesc(storeId, accountType);
            list.removeIf(a -> !status.equals(a.getStatus()));
            return list;
        }
        if (hasType) {
            return financeAccountRepository.findByStoreIdAndAccountTypeOrderByAccountIdDesc(storeId, accountType);
        }
        if (hasStatus) {
            return financeAccountRepository.findByStoreIdAndStatusOrderByAccountIdDesc(storeId, status);
        }
        return financeAccountRepository.findByStoreIdOrderByAccountIdDesc(storeId);
    }

    @Transactional
    public FinanceAccount create(FinanceAccount account) {
        if (account.getAccountId() == null) {
            account.setAccountId(System.currentTimeMillis());
        }
        if (account.getOpeningBalance() == null) {
            account.setOpeningBalance(BigDecimal.ZERO);
        }
        if (account.getCurrentBalance() == null) {
            account.setCurrentBalance(account.getOpeningBalance());
        }
        return financeAccountRepository.save(account);
    }

    @Transactional
    public FinanceAccount update(Long accountId, FinanceAccount body) {
        FinanceAccount existing = financeAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("资金账户不存在: " + accountId));
        if (body.getAccountName() != null) existing.setAccountName(body.getAccountName());
        if (body.getAccountType() != null) existing.setAccountType(body.getAccountType());
        if (body.getOpeningBalance() != null) existing.setOpeningBalance(body.getOpeningBalance());
        if (body.getCurrentBalance() != null) existing.setCurrentBalance(body.getCurrentBalance());
        if (body.getBankName() != null) existing.setBankName(body.getBankName());
        if (body.getBankAccount() != null) existing.setBankAccount(body.getBankAccount());
        if (body.getStatus() != null) existing.setStatus(body.getStatus());
        if (body.getRemark() != null) existing.setRemark(body.getRemark());
        return financeAccountRepository.save(existing);
    }
}

package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.FinanceAccount;
import com.youjian.banquet.service.FinanceAccountService;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 资金账户管理。对应 GET/POST/PUT /api/finance/accounts。
 * <p>与 FinanceController 的 /api/finance/account(JdbcTemplate 版) 共存，路径不冲突（单复数不同）。
 */
@RestController
@RequestMapping("/api/finance/accounts")
@CrossOrigin(origins = "*")
public class FinanceAccountController {

    @Autowired
    private FinanceAccountService financeAccountService;

    /** 当前操作用户的 storeId（非总经理时取 JWT 中的 storeId，兜底 1） */
    private Long storeId() {
        Long sid = UserContext.currentStoreId();
        return (sid == null || sid == 0L) ? 1L : sid;
    }

    @GetMapping
    public Result<List<FinanceAccount>> list(
            @RequestParam(defaultValue = "1") Long storeId,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String status) {
        Long sid = UserContext.isGeneralManager() ? storeId : storeId();
        return Result.success(financeAccountService.list(sid, accountType, status));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody FinanceAccount body) {
        body.setStoreId(storeId());
        FinanceAccount saved = financeAccountService.create(body);
        return Result.success(Map.of("accountId", saved.getAccountId()));
    }

    @PutMapping("/{id}")
    public Result<FinanceAccount> update(@PathVariable Long id, @RequestBody FinanceAccount body) {
        return Result.success(financeAccountService.update(id, body));
    }
}

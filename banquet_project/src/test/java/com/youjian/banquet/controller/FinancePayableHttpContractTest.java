package com.youjian.banquet.controller;

import com.youjian.banquet.service.FinancePayableService;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import com.youjian.banquet.util.UserContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HTTP contract with a mock service, not authentication or browser E2E. */
class FinancePayableHttpContractTest {
    FinancePayableService service;
    MockMvc mvc;
    @BeforeEach void setup(){
        service=mock(FinancePayableService.class);
        var controller=new FinancePayableController();
        ReflectionTestUtils.setField(controller,"financePayableService",service);
        mvc=MockMvcBuilders.standaloneSetup(controller).build();
    }
    @Test void invalidAmountReturnsAnActionable400() throws Exception {
        when(service.settle(eq(1L),eq(new BigDecimal("101")),anyString()))
                .thenThrow(new IllegalArgumentException("结算金额不能超过待付金额"));
        mvc.perform(post("/api/finance/payables").contentType("application/json")
                .content("{\"payableId\":1,\"settleAmount\":101,\"requestId\":\"REQ-1\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("结算金额不能超过待付金额"));
    }
    @Test void wrongStoreReturns403() throws Exception {
        when(service.settle(eq(1L),eq(BigDecimal.ONE),anyString()))
                .thenThrow(new FinancePayableService.PayableAccessDeniedException());
        mvc.perform(post("/api/finance/payables").contentType("application/json")
                .content("{\"payableId\":1,\"settleAmount\":1,\"requestId\":\"REQ-2\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
    }
    @Test void missingRequestIdReturns400() throws Exception {
        // 新契约：结算必须带幂等键。没带时服务抛可解释错误，接口回 400。
        when(service.settle(eq(1L),eq(BigDecimal.ONE),isNull()))
                .thenThrow(new IllegalArgumentException("缺少 requestId：结算必须带幂等键"));
        mvc.perform(post("/api/finance/payables").contentType("application/json")
                .content("{\"payableId\":1,\"settleAmount\":1}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
    }
    @Test void malformedNumberDoesNotReachSettlement() throws Exception {
        mvc.perform(post("/api/finance/payables").contentType("application/json")
                .content("{\"payableId\":\"invalid\",\"settleAmount\":1}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("单据编号或金额格式不正确"));
        verifyNoInteractions(service);
    }

    // ==================== 幂等冲突必须是 409，不是 400 ====================

    @AfterEach void clearIdentity(){ UserContext.clear(); }

    private void loginAs(Long storeId, boolean gm){
        UserContext.set(new UserContext.CurrentUser(9L, storeId, gm ? "gm" : "store_manager", "synthetic"));
        UserContext.setDataScopeAll(gm);
    }

    @Test void idempotencyConflictReturns409NotBadRequest() throws Exception {
        // 400 的含义是"请求本身写错了"，幂等冲突是"请求没错，但和已发生的另一笔撞上了"，
        // 调用方处置方式完全不同。这两个异常继承自 IllegalArgumentException，
        // 若没有单独的处理器就会被 400 处理器吃掉——这条用例就是防这个回归。
        when(service.settle(eq(1L), eq(BigDecimal.ONE), anyString()))
                .thenThrow(new FinancePayableService.SettlementConflictException("该 requestId 已被另一笔结算占用"));
        mvc.perform(post("/api/finance/payables").contentType("application/json")
                        .content("{\"payableId\":1,\"settleAmount\":1,\"requestId\":\"REQ-C\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test void concurrentLockContentionAlsoReturns409() throws Exception {
        when(service.settle(eq(1L), eq(BigDecimal.ONE), anyString()))
                .thenThrow(new FinancePayableService.SettlementRetryableException());
        mvc.perform(post("/api/finance/payables").contentType("application/json")
                        .content("{\"payableId\":1,\"settleAmount\":1,\"requestId\":\"REQ-R\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("重试")));
    }

    // ==================== 门店解析 fail-closed ====================

    @Test void listWithoutResolvableStoreIsRejected() throws Exception {
        // 非总经理解析不出门店：原实现回退成 1 号店，等于把别人的应付单给了他。
        loginAs(null, false);
        mvc.perform(get("/api/finance/payables"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
        verifyNoInteractions(service);
    }

    @Test void generalManagerMustNameAStore() throws Exception {
        // 总经理原来带 defaultValue=1，不传就静默按 1 号店查。改为必须显式指定。
        loginAs(0L, true);
        mvc.perform(get("/api/finance/payables"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(service);
    }

    @Test void generalManagerIllegalStoreIsRejected() throws Exception {
        loginAs(0L, true);
        for (String bad : new String[]{"0", "-1"}) {
            mvc.perform(get("/api/finance/payables").param("storeId", bad))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        }
        verifyNoInteractions(service);
    }

    @Test void storeManagerCannotQueryAnotherStore() throws Exception {
        loginAs(1L, false);
        mvc.perform(get("/api/finance/payables").param("storeId", "2"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
        verifyNoInteractions(service);
    }

    @Test void storeManagerQueriesOwnStoreOnly() throws Exception {
        loginAs(1L, false);
        when(service.list(eq(1L), isNull(), isNull())).thenReturn(java.util.List.of());
        mvc.perform(get("/api/finance/payables")).andExpect(status().isOk());
        // 只能用自己门店去查，不会被参数改写成别的门店
        verify(service).list(eq(1L), isNull(), isNull());
    }

    @Test void invalidExplicitStoreIsNotSilentlyIgnoredForStoreManager() throws Exception {
        loginAs(1L, false);
        for (String bad : new String[]{"0", "-1"}) {
            mvc.perform(get("/api/finance/payables").param("storeId", bad))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        }
        verifyNoInteractions(service);
    }

    @Test void absentIdentityCannotUseResidualAllStoreFlag() throws Exception {
        UserContext.clear();
        UserContext.setDataScopeAll(true);
        mvc.perform(get("/api/finance/payables").param("storeId", "1"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test void settlementsOfAnotherStoreIsRejected() throws Exception {
        loginAs(1L, false);
        when(service.settlements(eq(77L)))
                .thenThrow(new FinancePayableService.PayableAccessDeniedException());
        mvc.perform(get("/api/finance/payables/77/settlements"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
    }

    // ==================== 手工新增的幂等契约 ====================

    @Test void createWithoutRequestIdReturns400() throws Exception {
        // 控制器现在自己解析并校验门店，没有身份一律 403，用例必须先登录
        loginAs(1L,false);
        // 新增分支（body 不含 payableId）同样必须带幂等键，否则超时重发会多建一张单。
        when(service.create(org.mockito.ArgumentMatchers.any(
                        com.youjian.banquet.entity.FinancePayable.class), isNull()))
                .thenThrow(new IllegalArgumentException("缺少 requestId：手工新增应付必须带幂等键"));
        mvc.perform(post("/api/finance/payables").contentType("application/json")
                        .content("{\"supplierName\":\"供应商甲\",\"totalAmount\":100}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
    }

    @Test void createConflictReturns409() throws Exception {
        // 控制器现在自己解析并校验门店，没有身份一律 403，用例必须先登录
        loginAs(1L,false);
        // 同键改参数：冲突语义用 409，不能被 400 处理器吃掉。
        when(service.create(org.mockito.ArgumentMatchers.any(
                        com.youjian.banquet.entity.FinancePayable.class), anyString()))
                .thenThrow(new FinancePayableService.SettlementConflictException(
                        "该 requestId 此前已创建过一张应付单"));
        mvc.perform(post("/api/finance/payables").contentType("application/json")
                        .content("{\"supplierName\":\"供应商甲\",\"totalAmount\":100,\"requestId\":\"REQ-X\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(409));
    }

    @Test void createInFlightReturns409() throws Exception {
        // 控制器现在自己解析并校验门店，没有身份一律 403，用例必须先登录
        loginAs(1L,false);
        when(service.create(org.mockito.ArgumentMatchers.any(
                        com.youjian.banquet.entity.FinancePayable.class), anyString()))
                .thenThrow(new FinancePayableService.CreateInFlightException());
        mvc.perform(post("/api/finance/payables").contentType("application/json")
                        .content("{\"supplierName\":\"供应商甲\",\"totalAmount\":100,\"requestId\":\"REQ-Y\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("重试")));
    }

    @Test void createReceiptCarriesFieldsFrontendCanVerify() throws Exception {
        // 控制器现在自己解析并校验门店，没有身份一律 403，用例必须先登录
        loginAs(1L,false);
        var payable = new com.youjian.banquet.entity.FinancePayable();
        payable.setPayableId(42L);
        payable.setPayableNo("PY-42");
        payable.setStoreId(1L);
        payable.setTotalAmount(new BigDecimal("100.00"));
        payable.setStatus("unpaid");
        when(service.create(org.mockito.ArgumentMatchers.any(
                        com.youjian.banquet.entity.FinancePayable.class), anyString()))
                .thenReturn(new FinancePayableService.CreateResult(payable, "REQ-Z", true));
        mvc.perform(post("/api/finance/payables").contentType("application/json")
                        .content("{\"supplierName\":\"供应商甲\",\"totalAmount\":100,\"requestId\":\"REQ-Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestId").value("REQ-Z"))
                .andExpect(jsonPath("$.data.payableId").value(42))
                .andExpect(jsonPath("$.data.payableNo").value("PY-42"))
                // replayed 让前端知道这次是重试取回，不要再提示一次"创建成功"
                .andExpect(jsonPath("$.data.replayed").value(true));
    }
}

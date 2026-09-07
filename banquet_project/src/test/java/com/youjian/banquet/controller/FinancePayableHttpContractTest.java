package com.youjian.banquet.controller;

import com.youjian.banquet.service.FinancePayableService;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import static org.mockito.Mockito.*;
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
        when(service.settle(1L,new BigDecimal("101"))).thenThrow(new IllegalArgumentException("结算金额不能超过待付金额"));
        mvc.perform(post("/api/finance/payables").contentType("application/json")
                .content("{\"payableId\":1,\"settleAmount\":101}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("结算金额不能超过待付金额"));
    }
    @Test void wrongStoreReturns403() throws Exception {
        when(service.settle(1L,BigDecimal.ONE)).thenThrow(new FinancePayableService.PayableAccessDeniedException());
        mvc.perform(post("/api/finance/payables").contentType("application/json")
                .content("{\"payableId\":1,\"settleAmount\":1}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
    }
    @Test void malformedNumberDoesNotReachSettlement() throws Exception {
        mvc.perform(post("/api/finance/payables").contentType("application/json")
                .content("{\"payableId\":\"invalid\",\"settleAmount\":1}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("单据编号或金额格式不正确"));
        verifyNoInteractions(service);
    }
}

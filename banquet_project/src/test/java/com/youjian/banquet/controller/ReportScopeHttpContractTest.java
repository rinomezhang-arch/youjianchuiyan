package com.youjian.banquet.controller;

import com.youjian.banquet.service.FinanceReportService;
import com.youjian.banquet.util.UserContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real MVC dispatch with mocked business dependencies; not JWT/authentication or business E2E. */
class ReportScopeHttpContractTest {
    private static final List<String> ROUTES = List.of("/api/report/daily-summary", "/api/report/revenue",
            "/api/report/dish-sales", "/api/report/department-cost", "/api/report/staff-kpi",
            "/api/report/overview", "/api/finance/profit-report", "/api/finance/balance-report");
    JdbcTemplate jdbc;
    FinanceReportService service;
    MockMvc mvc;

    @BeforeEach void setup() {
        UserContext.clear();
        jdbc = mock(JdbcTemplate.class);
        service = mock(FinanceReportService.class);
        var report = new ReportController();
        var finance = new FinanceReportController();
        ReflectionTestUtils.setField(report, "jdbc", jdbc);
        ReflectionTestUtils.setField(finance, "financeReportService", service);
        mvc = MockMvcBuilders.standaloneSetup(report, finance).build();
    }
    @AfterEach void cleanup() { UserContext.clear(); }
    void user(Long store, String role) { UserContext.set(new UserContext.CurrentUser(10L, store, role, "synthetic")); }
    void rejected(String store, int code) throws Exception {
        for (String route : ROUTES) {
            var request = get(route);
            if (store != null) request.param("storeId", store);
            mvc.perform(request).andExpect(status().is(code)).andExpect(jsonPath("$.code").value(code));
        }
        verifyNoInteractions(jdbc, service);
    }
    @Test void noIdentityEvenWithStaleGlobalScopeCannotQuery() throws Exception {
        UserContext.setDataScopeAll(true);
        rejected(null, 403);
    }
    @Test void missingStoreCannotDefaultToAllOrOne() throws Exception { user(null, "manager"); rejected(null, 403); }
    @Test void invalidContextStoreCannotQuery() throws Exception { user(-1L, "manager"); rejected("1", 403); }
    @ParameterizedTest @ValueSource(strings={"0","-1","abc","1.5","9223372036854775808","+1"})
    void invalidRequestedStoreCannotBecomeAll(String value) throws Exception {
        user(2L,"manager"); rejected(value,400);
        user(0L,"gm"); rejected(value,400);
    }
    @ParameterizedTest @ValueSource(strings={"1","all"})
    void otherStoreOrAggregateIsForbidden(String value) throws Exception { user(2L,"manager"); rejected(value,403); }
    @ParameterizedTest @ValueSource(strings={"2",""," "})
    void legitimateStoreUsesScopeInEveryQuery(String value) throws Exception {
        user(2L,"manager");
        for(String route:ROUTES) mvc.perform(get(route).param("storeId",value)).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        verify(service).profitReport(eq(2L), anyString());
        verify(service).balanceReport(eq(2L), anyString());
        var calls=mockingDetails(jdbc).getInvocations();
        assertFalse(calls.isEmpty());
        for(var call:calls) {
            assertTrue(call.getArgument(0,String.class).contains("store_id = ?"));
            assertTrue(Arrays.asList(call.getArguments()).contains(2L), "bound store must be passed to JDBC");
        }
    }
    @ParameterizedTest @ValueSource(strings={"all",""," "})
    void gmAggregateExplicitOrEmptyRemainsPermitted(String value) throws Exception {
        user(0L,"gm");
        for(String route:ROUTES) mvc.perform(get(route).param("storeId",value)).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        verify(service).profitReport(isNull(), anyString());
        verify(service).balanceReport(isNull(), anyString());
        for(var call:mockingDetails(jdbc).getInvocations()) assertFalse(call.getArgument(0,String.class).contains("store_id = ?"));
    }
    @Test void gmOmittedParameterRemainsPermitted() throws Exception {
        user(1L,"gm");
        for(String route:ROUTES) mvc.perform(get(route)).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        verify(service).profitReport(isNull(),anyString());
    }
    @Test void backendFailuresNeverExposeSql() throws Exception {
        user(2L,"manager");
        when(service.profitReport(eq(2L),anyString())).thenThrow(new IllegalStateException("SELECT secret FROM private_table"));
        mvc.perform(get("/api/finance/profit-report")).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.message").value("报表查询失败"));
        doThrow(new IllegalStateException("SELECT secret FROM private_table")).when(jdbc).queryForList(anyString(),any(Object[].class));
        mvc.perform(get("/api/report/revenue")).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.message").value("报表查询失败"));
    }
}

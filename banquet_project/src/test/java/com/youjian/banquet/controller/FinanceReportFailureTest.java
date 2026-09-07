package com.youjian.banquet.controller;

import com.youjian.banquet.service.FinanceReportService;
import com.youjian.banquet.util.UserContext;
import org.junit.jupiter.api.*;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.time.YearMonth;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class FinanceReportFailureTest {
    JdbcTemplate jdbc;
    FinanceReportService service;
    FinanceReportController controller;
    @BeforeEach void setup() {
        jdbc=mock(JdbcTemplate.class); service=new FinanceReportService();
        ReflectionTestUtils.setField(service,"jdbc",jdbc);
        controller=new FinanceReportController();
        ReflectionTestUtils.setField(controller,"financeReportService",service);
        UserContext.set(new UserContext.CurrentUser(1L,1L,"store_manager","synthetic"));
    }
    @AfterEach void clear(){UserContext.clear();}

    @Test void emptyAggregatesStillReturnValidZero() {
        when(jdbc.queryForObject(anyString(),eq(BigDecimal.class),any(Object[].class))).thenReturn(BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO,service.profitReport(1L,"2026-09").get("revenue"));
        assertEquals(BigDecimal.ZERO,service.balanceReport(1L,"2026-09").get("totalAssets"));
    }
    @Test void invalidMonthNeverQueriesAnotherPeriod() throws Exception {
        for(String month:new String[]{"2026-13","2026-9","garbage","2026-09-01"}) {
            assertThrows(IllegalArgumentException.class,()->service.profitReport(1L,month));
            assertThrows(IllegalArgumentException.class,()->service.balanceReport(1L,month));
        }
        var response=MockMvcBuilders.standaloneSetup(controller).build()
            .perform(get("/api/finance/profit-report").param("month","2026-13")).andReturn().getResponse();
        assertEquals(400,response.getStatus());
        verifyNoInteractions(jdbc);
    }
    @Test void omittedMonthIsConsistentWithReportedPeriod() {
        String expected=YearMonth.now().toString();
        assertEquals(expected,service.profitReport(1L,null).get("month"));
        assertEquals(expected,service.balanceReport(1L,"").get("month"));
    }
    @Test void databaseFailureCannotBecomeSuccessfulZeroReport() throws Exception {
        when(jdbc.queryForObject(anyString(),eq(BigDecimal.class),any(Object[].class)))
            .thenThrow(new DataAccessResourceFailureException("synthetic-private-sql-marker"));
        var mvc=MockMvcBuilders.standaloneSetup(controller).build();
        for(String route:new String[]{"profit-report","balance-report"}) {
            var response=mvc.perform(get("/api/finance/"+route).param("month","2026-09")).andReturn().getResponse();
            assertEquals(500,response.getStatus());
            assertFalse(response.getContentAsString().contains("synthetic-private-sql-marker"));
        }
    }
}

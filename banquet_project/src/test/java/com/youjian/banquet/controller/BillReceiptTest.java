package com.youjian.banquet.controller;

import com.youjian.banquet.service.BillReceiptService;
import com.youjian.banquet.util.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TR-RECEIPT-REAL-24 小票接口针对性契约测试。
 * <p>
 * 不启动 Spring 上下文、不连数据库：JdbcTemplate 打桩，身份用 UserContext 直接构造，
 * 覆盖 401（无身份）/ 403（普通员工跨店）/ 400（GM 缺门店）/ 404（未知单）/ 200（快照装配）。
 * 真实 HTTP + 真实数据库 + 真实浏览器链路由 scripts/release/receipt-real-24 的可重放驱动覆盖。
 */
class BillReceiptTest {

    private JdbcTemplate jdbc;
    private MockMvc mvc;

    private List<Map<String, Object>> masterRows;
    private List<Map<String, Object>> dishRows;

    @BeforeEach
    void setup() {
        jdbc = mock(JdbcTemplate.class);
        BillReceiptService service = new BillReceiptService();
        ReflectionTestUtils.setField(service, "jdbc", jdbc);
        BillController controller = new BillController();
        ReflectionTestUtils.setField(controller, "jdbc", jdbc);
        ReflectionTestUtils.setField(controller, "billReceiptService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();

        masterRows = new ArrayList<>();
        Map<String, Object> master = new LinkedHashMap<>();
        master.put("booking_id", "COPRINT23-BK-001");
        master.put("store_id", 1L);
        master.put("guest_count", 4);
        master.put("total_amount", new BigDecimal("100.00"));
        master.put("final_amount", new BigDecimal("100.00"));
        master.put("payment_status", "paid");
        master.put("booking_status", "completed");
        master.put("booking_date", Date.valueOf("2026-09-09"));
        master.put("store_name", "COPRINT23合成门店");
        masterRows.add(master);

        dishRows = new ArrayList<>();
        dishRows.add(dish("COPRINT23红烧肉", "2.00", "35.00", "70.00"));
        dishRows.add(dish("COPRINT23时蔬", "1.00", "30.00", "30.00"));

        lenient().when(jdbc.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("FROM booking_dish_detail")) {
                return dishRows;
            }
            return masterRows;
        });
    }

    private Map<String, Object> dish(String name, String qty, String price, String subtotal) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("dish_name", name);
        d.put("dish_quantity", new BigDecimal(qty));
        d.put("unit_price", new BigDecimal(price));
        d.put("subtotal", new BigDecimal(subtotal));
        return d;
    }

    @AfterEach
    void clearIdentity() {
        UserContext.clear();
    }

    private void loginAs(Long staffId, Long storeId, String role, boolean gm) {
        UserContext.set(new UserContext.CurrentUser(staffId, storeId, role, "synthetic"));
        UserContext.setDataScopeAll(gm);
    }

    private String receiptPath(String storeId) {
        return "/api/bills/COPRINT23-BK-001/receipt" + (storeId == null ? "" : "?storeId=" + storeId);
    }

    // ==================== 身份与门店范围 ====================

    @Test
    void noIdentityReturns401() throws Exception {
        mvc.perform(get(receiptPath("1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void gmMissingStoreReturns400() throws Exception {
        loginAs(1L, 1L, "gm", true);
        mvc.perform(get("/api/bills/COPRINT23-BK-001/receipt"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void gmZeroStoreReturns400() throws Exception {
        loginAs(1L, 1L, "gm", true);
        mvc.perform(get(receiptPath("0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void gmAllStoreReturns400() throws Exception {
        loginAs(1L, 1L, "gm", true);
        mvc.perform(get(receiptPath("all")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void gmInvalidStoreReturns400() throws Exception {
        loginAs(1L, 1L, "gm", true);
        mvc.perform(get(receiptPath("abc")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void staffCrossStoreReturns403() throws Exception {
        loginAs(923001L, 1L, "store_manager", false);
        mvc.perform(get(receiptPath("2")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void staffAllStoreReturns403() throws Exception {
        loginAs(923001L, 1L, "store_manager", false);
        mvc.perform(get(receiptPath("all")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void staffWithoutOwnStoreReturns403() throws Exception {
        loginAs(77L, null, "staff", false);
        mvc.perform(get(receiptPath("1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void unknownOrderReturns404() throws Exception {
        loginAs(1L, 1L, "gm", true);
        masterRows.clear();
        mvc.perform(get(receiptPath("1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ==================== 成功快照装配 ====================

    @Test
    void staffOwnStoreReturnsReceiptSnapshot() throws Exception {
        loginAs(923001L, 1L, "store_manager", false);
        mvc.perform(get(receiptPath("1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderNo").value("COPRINT23-BK-001"))
                .andExpect(jsonPath("$.data.storeId").value(1))
                .andExpect(jsonPath("$.data.storeName").value("COPRINT23合成门店"))
                .andExpect(jsonPath("$.data.dishes.length()").value(2))
                .andExpect(jsonPath("$.data.dishes[0].dishName").value("COPRINT23红烧肉"))
                .andExpect(jsonPath("$.data.dishes[1].dishName").value("COPRINT23时蔬"))
                .andExpect(jsonPath("$.data.status").value("settled"))
                .andExpect(jsonPath("$.data.amountNote").exists())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"finalAmount\":100.00")));
    }

    @Test
    void gmExplicitStoreReturnsReceiptSnapshot() throws Exception {
        loginAs(2L, 1L, "gm", true);
        mvc.perform(get(receiptPath("1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderNo").value("COPRINT23-BK-001"));
    }

    @Test
    void gmCanTargetOtherStoreExplicitly() throws Exception {
        loginAs(2L, 1L, "gm", true);
        Map<String, Object> other = new LinkedHashMap<>(masterRows.get(0));
        other.put("booking_id", "TR24-BK-STORE2");
        other.put("store_id", 2L);
        masterRows.clear();
        masterRows.add(other);
        mvc.perform(get("/api/bills/TR24-BK-STORE2/receipt?storeId=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeId").value(2));
    }
}

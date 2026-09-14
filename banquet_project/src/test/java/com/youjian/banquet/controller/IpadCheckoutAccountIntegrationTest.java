package com.youjian.banquet.controller;

import com.youjian.banquet.entity.BookingDishDetail;
import com.youjian.banquet.repository.BookingDishDetailRepository;
import com.youjian.banquet.repository.BookingMasterRepository;
import com.youjian.banquet.repository.BookingTableRepository;
import com.youjian.banquet.repository.DishMasterRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class IpadCheckoutAccountIntegrationTest {
    private static final long STORE_ID = 7L;
    private static final long STAFF_ID = 23L;
    private static final long ACCOUNT_ID = 41L;
    private static final String BOOKING_ID = "SYNTHETIC-IPAD-ACCOUNT-63";
    private static final String IDEMPOTENCY_KEY = "SYNTHETIC-IDEMPOTENCY-63";

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final BookingMasterRepository bookingRepository = mock(BookingMasterRepository.class);
    private final BookingTableRepository tableRepository = mock(BookingTableRepository.class);
    private final BookingDishDetailRepository detailRepository = mock(BookingDishDetailRepository.class);
    private final DishMasterRepository dishRepository = mock(DishMasterRepository.class);
    private final IpadCheckoutController controller = new IpadCheckoutController(
            jdbc, bookingRepository, tableRepository, detailRepository, dishRepository);

    @Test
    void financeInsertCarriesAccountIdAndRelatedIdTogether() {
        stubUnpaidBooking();
        when(jdbc.queryForObject(contains("FROM finance_account"), eq(Integer.class), eq(ACCOUNT_ID), eq(STORE_ID)))
                .thenReturn(1);
        BookingDishDetail detail = mock(BookingDishDetail.class);
        when(detail.getKitchenStatus()).thenReturn("served");
        when(detail.getSubtotal()).thenReturn(new BigDecimal("100.00"));
        when(detailRepository.findByBookingIdAndStoreId(BOOKING_ID, STORE_ID)).thenReturn(List.of(detail));

        var result = controller.pay(paymentBody(ACCOUNT_ID), IDEMPOTENCY_KEY, device());

        assertEquals(200, result.getCode());
        verify(jdbc).update(
                argThat(sql -> sql.startsWith("INSERT INTO finance_transaction")
                        && sql.contains("related_type,related_id,related_no")
                        && sql.contains("payment_method,account_id,operator_id")),
                eq(STORE_ID), startsWith("POS" + STORE_ID), eq(88L), eq(BOOKING_ID),
                eq(new BigDecimal("100.00")), eq("cash"), eq(ACCOUNT_ID), eq((int) STAFF_ID), isNull());
    }

    @Test
    void missingAccountRejectsBeforeAnyWrite() {
        Map<String, Object> body = paymentBody(ACCOUNT_ID);
        body.remove("account_id");

        assertEquals("请选择收款账户",
                assertThrows(IllegalArgumentException.class,
                        () -> controller.pay(body, IDEMPOTENCY_KEY, device())).getMessage());
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void crossStoreAccountRejectsBeforeAnyWrite() {
        assertUnavailableAccountRejected(901L);
    }

    @Test
    void disabledAccountRejectsBeforeAnyWrite() {
        assertUnavailableAccountRejected(902L);
    }

    @Test
    void accountListQueriesOnlyWhitelistedFieldsForAuthenticatedStore() {
        List<Map<String, Object>> rows = List.of(Map.of(
                "account_id", ACCOUNT_ID, "account_name", "Synthetic Cash", "account_type", "cash"));
        when(jdbc.queryForList(anyString(), eq(STORE_ID))).thenReturn(rows);

        var result = controller.settlementAccounts(device());

        assertEquals(rows, result.getData());
        verify(jdbc).queryForList(
                eq("SELECT account_id, account_name, account_type FROM finance_account "
                        + "WHERE store_id=? AND is_active=1 ORDER BY sort_order ASC, account_id ASC"),
                eq(STORE_ID));
    }

    @Test
    void payMethodRemainsTransactional() throws Exception {
        var method = IpadCheckoutController.class.getMethod(
                "pay", Map.class, String.class, HttpServletRequest.class);
        assertNotNull(method.getAnnotation(Transactional.class));
    }

    private void assertUnavailableAccountRejected(long accountId) {
        stubUnpaidBooking();
        when(jdbc.queryForObject(contains("FROM finance_account"), eq(Integer.class), eq(accountId), eq(STORE_ID)))
                .thenReturn(0);

        var error = assertThrows(IllegalArgumentException.class,
                () -> controller.pay(paymentBody(accountId), IDEMPOTENCY_KEY, device()));

        assertEquals("收款账户不存在、不属于当前门店或已停用", error.getMessage());
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    private void stubUnpaidBooking() {
        when(jdbc.queryForList(contains("FROM ipad_payment_request"), eq(STORE_ID), eq(IDEMPOTENCY_KEY)))
                .thenReturn(List.of());
        when(jdbc.queryForList(contains("FROM booking_master"), eq(BOOKING_ID), eq(STORE_ID)))
                .thenReturn(List.of(Map.of(
                        "id", 88L,
                        "payment_status", "unpaid",
                        "deposit_amount", BigDecimal.ZERO,
                        "booking_status", "dining")));
    }

    private Map<String, Object> paymentBody(long accountId) {
        Map<String, Object> body = new HashMap<>();
        body.put("booking_id", BOOKING_ID);
        body.put("pay_type", "cash");
        body.put("pay_amount", "120.00");
        body.put("account_id", accountId);
        return body;
    }

    private MockHttpServletRequest device() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("ipad_store_id", STORE_ID);
        request.setAttribute("ipad_staff_id", STAFF_ID);
        return request;
    }
}

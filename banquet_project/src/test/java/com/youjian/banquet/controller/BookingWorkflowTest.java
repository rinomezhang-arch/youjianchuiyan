package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BookingMaster;
import com.youjian.banquet.repository.*;
import com.youjian.banquet.util.UserContext;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BookingWorkflowTest {
    BookingController controller;
    JdbcTemplate jdbc;
    BookingMasterRepository bookings;
    BookingTableRepository tables;
    BookingDishDetailRepository dishes;

    @BeforeEach void setup() {
        controller = new BookingController();
        jdbc = mock(JdbcTemplate.class);
        bookings = mock(BookingMasterRepository.class);
        tables = mock(BookingTableRepository.class);
        dishes = mock(BookingDishDetailRepository.class);
        ReflectionTestUtils.setField(controller, "jdbc", jdbc);
        ReflectionTestUtils.setField(controller, "bookingMasterRepo", bookings);
        ReflectionTestUtils.setField(controller, "bookingTableRepo", tables);
        ReflectionTestUtils.setField(controller, "bookingDishDetailRepo", dishes);
        UserContext.set(new UserContext.CurrentUser(1L, 1L, "store_manager", "test-operator"));
        when(bookings.save(any(BookingMaster.class))).thenAnswer(i -> i.getArgument(0));
    }
    @AfterEach void cleanup() { UserContext.clear(); }

    Map<String, Object> request() {
        Map<String, Object> body = new HashMap<>();
        body.put("customerName", "验收客户");
        body.put("customerPhone", "13800000000");
        body.put("bookingDate", LocalDate.now().plusDays(1).toString());
        body.put("bookingTime", "18:00");
        return body;
    }

    @Test void standardDepositFieldMustBePersisted() {
        Map<String, Object> body = request();
        body.put("depositAmount", "300.50");
        Result<BookingMaster> result = controller.create(body).getBody();
        assertEquals(200, result.getCode());
        assertEquals(new BigDecimal("300.50"), result.getData().getDepositAmount());
    }

    @Test void invalidDepositMustRejectBeforeAnyWrite() {
        Map<String, Object> body = request();
        body.put("deposit", "三百");
        assertEquals(400, controller.create(body).getBody().getCode());
        verifyNoInteractions(bookings, tables, dishes);
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test void negativeDepositMustRejectBeforeAnyWrite() {
        Map<String, Object> body = request();
        body.put("depositAmount", "-0.01");
        assertEquals(400, controller.create(body).getBody().getCode());
        verifyNoInteractions(bookings, tables, dishes);
    }

    @Test void pastBookingDateMustRejectBeforeAnyWrite() {
        Map<String, Object> body = request();
        body.put("bookingDate", LocalDate.now().minusDays(1).toString());
        assertEquals(400, controller.create(body).getBody().getCode());
        verifyNoInteractions(bookings, tables, dishes);
    }

    void existingBooking(String status, String payment, BigDecimal deposit) {
        when(jdbc.queryForList(contains("FROM booking_master"), any(Object[].class)))
            .thenReturn(List.of(Map.of("booking_status", status, "payment_status", payment,
                "deposit_amount", deposit)));
    }

    @Test void cancellationMustKeepBookingAndDishHistory() {
        existingBooking("confirmed", "unpaid", BigDecimal.ZERO);
        Result<?> result = (Result<?>) controller.delete("BK-TEST", 1L).getBody();
        assertEquals(200, result.getCode());
        verifyNoInteractions(bookings, tables, dishes);
        verify(jdbc).update(contains("booking_status='cancelled'"), eq("BK-TEST"), eq(1L));
    }

    @Test void unknownCancellationMustReturnNotFound() {
        Result<?> result = (Result<?>) controller.delete("BK-MISSING", 1L).getBody();
        assertEquals(404, result.getCode());
        verifyNoInteractions(bookings, tables, dishes);
    }

    @Test void settledBookingCannotBeCancelledAsUnpaid() {
        existingBooking("completed", "paid", BigDecimal.ZERO);
        Result<?> result = (Result<?>) controller.delete("BK-PAID", 1L).getBody();
        assertEquals(409, result.getCode());
        verifyNoInteractions(bookings, tables, dishes);
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test void cancellationCannotCrossStores() {
        Result<?> result = (Result<?>) controller.delete("BK-OTHER", 2L).getBody();
        assertEquals(403, result.getCode());
        verifyNoInteractions(jdbc, bookings, tables, dishes);
    }

    @Test void duplicateTableSelectionCannotCreatePartialBooking() {
        Map<String, Object> body = request();
        body.put("tableIds", List.of(7, 7));
        assertEquals(400, controller.create(body).getBody().getCode());
        verifyNoInteractions(bookings, tables, dishes);
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    void availableTable() {
        when(jdbc.queryForList(contains("FROM table_master"), any(Object[].class)))
            .thenReturn(List.of(Map.of("table_id", 7, "table_name", "测试桌", "table_number", "T7", "table_status", "idle")));
    }

    @Test void occupiedPeriodMustFailBeforeCustomerOrBookingWrites() {
        availableTable();
        when(jdbc.queryForObject(contains("SELECT COUNT(*) FROM booking_table"), eq(Integer.class), any(Object[].class)))
            .thenReturn(1);
        Map<String, Object> body = request();
        body.put("tableIds", List.of(7));
        assertEquals(409, controller.create(body).getBody().getCode());
        verifyNoInteractions(bookings, tables, dishes);
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test void unknownOrOtherStoreTableMustBeRejected() {
        Map<String, Object> body = request();
        body.put("tableIds", List.of(7));
        assertEquals(400, controller.create(body).getBody().getCode());
        verifyNoInteractions(bookings, tables, dishes);
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test void reservationMustReserveRatherThanSeatTheGuest() {
        availableTable();
        Map<String, Object> body = request();
        body.put("tableIds", List.of(7));
        assertEquals(200, controller.create(body).getBody().getCode());
        verify(tables).save(argThat(t -> t.getTableId() == 7 && t.getStoreId() == 1L));
        verify(jdbc).update(contains("table_status='reserved'"), eq(7), eq(1L));
    }

    @Test void cancellationWithDepositMustRequireRefundWorkflow() {
        existingBooking("confirmed", "unpaid", new BigDecimal("300.00"));
        Result<?> result = (Result<?>) controller.delete("BK-DEPOSIT", 1L).getBody();
        assertEquals(409, result.getCode());
        verifyNoInteractions(bookings, tables, dishes);
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test void repeatedCancellationIsIdempotent() {
        existingBooking("cancelled", "unpaid", BigDecimal.ZERO);
        Result<?> result = (Result<?>) controller.delete("BK-CANCELLED", 1L).getBody();
        assertEquals(200, result.getCode());
        verifyNoInteractions(bookings, tables, dishes);
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test void legacyAvailableTableMustBecomeReserved() {
        when(jdbc.queryForList(contains("FROM table_master"), any(Object[].class)))
            .thenReturn(List.of(Map.of("table_id", 7, "table_name", "测试桌", "table_number", "T7", "table_status", "available")));
        Map<String, Object> body = request();
        body.put("bookingDate", LocalDate.now().toString());
        body.put("tableIds", List.of(7));
        Result<BookingMaster> result = controller.create(body).getBody();
        assertEquals(200, result.getCode());
        assertEquals(0, result.getData().getGuestConfirmed());
        verify(tables).save(argThat(t -> t.getTableId() == 7 && t.getStoreId() == 1L));
    }

    void cancellationTable(int remaining) {
        existingBooking("confirmed", "unpaid", BigDecimal.ZERO);
        when(jdbc.queryForList(contains("SELECT DISTINCT tm.table_id"), any(Object[].class)))
            .thenReturn(List.of(Map.of("table_id", 7)));
        when(jdbc.queryForObject(contains("SELECT COUNT(*) FROM booking_table"), eq(Integer.class), any(Object[].class)))
            .thenReturn(remaining);
    }

    @Test void lastCancellationReleasesOnlyReservedTableAndPreservesHistory() {
        cancellationTable(0);
        assertEquals(200, ((Result<?>) controller.delete("BK-TEST", 1L).getBody()).getCode());
        verify(jdbc).update(contains("booking_status='cancelled'"), eq("BK-TEST"), eq(1L));
        verify(jdbc).queryForObject(contains("bm.booking_status NOT IN ('cancelled','completed')"),
            eq(Integer.class), eq(7L), eq(1L));
        verify(jdbc).update(contains("AND table_status='reserved'"), eq(7L), eq(1L));
        verifyNoInteractions(bookings, tables, dishes);
    }

    @Test void cancellationKeepsTableReservedWhenAnotherActiveBookingRemains() {
        cancellationTable(1);
        assertEquals(200, ((Result<?>) controller.delete("BK-TEST", 1L).getBody()).getCode());
        verify(jdbc, never()).update(contains("table_status='idle'"), any(Object[].class));
        verifyNoInteractions(bookings, tables, dishes);
    }
}

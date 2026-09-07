package com.youjian.banquet.controller;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class IpadOrderScopeTest {
    @Test void absentStoreCannotReadOrBatchWriteDefaultStore() {
        var controller = new IpadOrderController();
        var jdbc = mock(JdbcTemplate.class);
        ReflectionTestUtils.setField(controller, "jdbcTemplate", jdbc);
        var request = new MockHttpServletRequest();
        assertEquals(401, controller.getCurrentOrder("1", request).getCode());
        assertEquals(401, controller.addDishesBatch(Map.of("booking_id", "synthetic"), request).getCode());
        verifyNoInteractions(jdbc);
    }

    @Test void absentStaffCannotCreateOrderAsEmployeeOne() {
        var controller = new IpadOrderController();
        var request = new MockHttpServletRequest();
        request.setAttribute("ipad_store_id", 1L);
        assertEquals(401, controller.addDish(Map.of("table_id", "1"), request).getCode());
        request.setAttribute("ipad_staff_id", 0L);
        assertEquals(401, controller.addDish(Map.of("table_id", "1"), request).getCode());
    }

    @Test void noActiveBookingReturnsNoHistoricDishes() {
        var controller = new IpadOrderController();
        var jdbc = mock(JdbcTemplate.class);
        ReflectionTestUtils.setField(controller, "jdbcTemplate", jdbc);
        when(jdbc.queryForList(anyString(), any(Integer.class), any(Long.class))).thenReturn(List.of());
        var request = new MockHttpServletRequest();
        request.setAttribute("ipad_store_id", 7L);
        var result = controller.getCurrentOrder("19", request);
        assertEquals(200, result.getCode());
        assertTrue(result.getData().isEmpty());
        verify(jdbc).queryForList(contains("booking_date=CURRENT_DATE"), eq(19), eq(7L));
    }

    @Test void ambiguousActiveBookingsReturnAllDishesNotArbitrary() {
        var controller = new IpadOrderController();
        var jdbc = mock(JdbcTemplate.class);
        ReflectionTestUtils.setField(controller, "jdbcTemplate", jdbc);
        when(jdbc.queryForList(anyString(), any(Integer.class), any(Long.class))).thenReturn(List.of(Map.of("dish_id", "A"), Map.of("dish_id", "B")));
        var request = new MockHttpServletRequest();
        request.setAttribute("ipad_store_id", 7L);
        var result = controller.getCurrentOrder("19", request);
        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().size());
    }
}

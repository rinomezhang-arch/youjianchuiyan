package com.youjian.banquet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youjian.banquet.config.IpadInterceptor;
import com.youjian.banquet.dto.NotifyEvent;
import com.youjian.banquet.entity.*;
import com.youjian.banquet.repository.*;
import com.youjian.banquet.service.NotifyPublisher;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Regression rejection checks after CHAIN02. Original vulnerable source snapshot is retained in personal evidence.
 * Real interceptor, controller and JDBC binding lookup. Downstream JPA/notification are doubles.
 * No full app, no persisted orders, no employee login simulated as successful authorization.
 */
class IpadAuthChainIsolationTest {
    String schema;
    JdbcTemplate jdbc;
    MockMvc mvc;
    ObjectMapper json = new ObjectMapper();
    BookingMasterRepository bookings = mock(BookingMasterRepository.class);
    DishMasterRepository dishes = mock(DishMasterRepository.class);
    BookingDishDetailRepository details = mock(BookingDishDetailRepository.class);
    NotifyPublisher notifications = mock(NotifyPublisher.class);

    @BeforeEach void setup() throws Exception {
        schema = "ipad_auth_" + UUID.randomUUID().toString().replace("-", "");
        String host = "jdbc:mysql://127.0.0.1:13317/", options = "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
        new JdbcTemplate(new DriverManagerDataSource(host + options, "root", "")).execute("CREATE DATABASE " + schema + " CHARACTER SET utf8mb4");
        jdbc = new JdbcTemplate(new DriverManagerDataSource(host + schema + options, "root", ""));
        String captured;
        try (var in = new ClassPathResource("restaurant-production-schema-20260906.sql").getInputStream()) {
            captured = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (String marker : List.of("CREATE TABLE staff_master (", "CREATE TABLE `store_info` (")) {
            int start = captured.indexOf(marker); assertTrue(start >= 0);
            jdbc.execute(captured.substring(start, captured.indexOf(';', start)));
        }
        // Explicit fixture adaptation only: original migration refers to nonexistent id columns.
        String ddl = Files.readString(Path.of("../scripts/migrations/ipad_device_binding_migration_v1.sql"))
            .replace("staff_id BIGINT NULL", "staff_id INT NULL")
            .replace("REFERENCES store_info(id)", "REFERENCES store_info(store_id)")
            .replace("REFERENCES staff_master(id)", "REFERENCES staff_master(staff_id)");
        jdbc.execute(ddl);
        jdbc.update("INSERT INTO store_info(store_id,store_code,store_name) VALUES(1,'SYN-IPAD','Synthetic')");
        jdbc.update("INSERT INTO staff_master(staff_id) VALUES(11)");
        jdbc.update("INSERT INTO ipad_device_binding(device_sn,store_id,staff_id) VALUES('SYN-EMPTY',1,NULL),('SYN-BOUND',1,11)");
        BookingMaster booking = new BookingMaster(); booking.setBookingId("SYN-BOOK"); booking.setStoreId(1L); booking.setBookingStatus("confirmed");
        when(bookings.findForOrderUpdate("SYN-BOOK", 1L)).thenReturn(Optional.of(booking));
        when(bookings.findByBookingIdAndStoreId("SYN-BOOK", 1L)).thenReturn(Optional.of(booking));
        DishMaster dish = new DishMaster(); dish.setDishId("SYN-DISH"); dish.setStoreId(1L); dish.setDishName("Synthetic"); dish.setSalePrice(new BigDecimal("12.50"));
        when(dishes.findByDishIdAndStoreId("SYN-DISH", 1L)).thenReturn(Optional.of(dish));
        when(details.save(any(BookingDishDetail.class))).thenAnswer(inv -> { BookingDishDetail d = inv.getArgument(0); d.setDishBookingId(81L); return d; });
        var controller = new IpadOrderController();
        ReflectionTestUtils.setField(controller,"jdbcTemplate",jdbc);
        ReflectionTestUtils.setField(controller,"bookingRepo",bookings);
        ReflectionTestUtils.setField(controller,"dishRepo",dishes);
        ReflectionTestUtils.setField(controller,"dishDetailRepo",details);
        ReflectionTestUtils.setField(controller,"notifyPublisher",notifications);
        ReflectionTestUtils.setField(controller,"batchAuthorization",new com.youjian.banquet.service.IpadBatchAuthorizationService());
        mvc = MockMvcBuilders.standaloneSetup(controller).addInterceptors(new IpadInterceptor(jdbc)).build();
    }
    @AfterEach void retained() { System.out.println("SYNTHETIC_SCHEMA_RETAINED=" + schema); }
    MockHttpServletRequestBuilder req(String route, String device, long staff, long store, Map<String,Object> body) throws Exception {
        return post("/api/ipad/"+route).header("X-Client-Type","ipad").header("X-Device-Sn",device)
            .header("X-Store-Id",store).header("X-Staff-Id",staff).contentType("application/json").content(json.writeValueAsBytes(body));
    }
    Map<String,Object> batch() { return new HashMap<>(Map.of("client_request_id","SYNTHETIC-AUTH-REQUEST-0001","booking_id","SYN-BOOK","dishes",List.of(Map.of("dish_id","SYN-DISH","dish_quantity",2)))); }
    void successful(MvcResult result) throws Exception {
        assertEquals(200,result.getResponse().getStatus());
        assertEquals(200,json.readTree(result.getResponse().getContentAsByteArray()).path("code").asInt());
        var write = ArgumentCaptor.forClass(BookingDishDetail.class); verify(details).save(write.capture());
        assertEquals(1L,write.getValue().getStoreId()); assertEquals("SYN-BOOK",write.getValue().getBookingId());
        assertEquals("SYN-DISH",write.getValue().getDishId());
    }
    @Test void unboundStaffHeaderIsRejectedWithoutEmployeePromotionOrWrite() throws Exception {
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM staff_master WHERE staff_id=900001",Integer.class));
        var r=mvc.perform(req("order/dish/add","SYN-EMPTY",900001,1,Map.of("booking_id","SYN-BOOK","dish_id","SYN-DISH","dish_quantity",1))).andReturn();
        assertEquals(403,r.getResponse().getStatus()); assertNull(r.getRequest().getAttribute("ipad_staff_id"));
        verifyNoInteractions(bookings,dishes,details,notifications);
    }
    @Test void batchRejectsForgedBodyStaffWithoutWrites() throws Exception {
        var body=batch(); body.put("staff_id",900002);
        var r=mvc.perform(req("order/add-dishes","SYN-BOUND",11,1,body)).andReturn();
        assertEquals(400,json.readTree(r.getResponse().getContentAsByteArray()).path("code").asInt());
        assertNull(r.getRequest().getAttribute("ipad_staff_id")); verifyNoInteractions(bookings,dishes,details,notifications);
    }
    @Test void batchMissingAuthorizationRejectsWithoutWrites() throws Exception {
        var r=mvc.perform(req("order/add-dishes","SYN-BOUND",11,1,batch())).andReturn();
        assertEquals(403,json.readTree(r.getResponse().getContentAsByteArray()).path("code").asInt());
        verifyNoInteractions(bookings,dishes,details,notifications);
    }
    @Test void boundStaffMismatchIsActuallyRejectedBeforeControllerWrites() throws Exception {
        var r=mvc.perform(req("order/dish/add","SYN-BOUND",900001,1,batch())).andReturn();
        assertEquals(403,r.getResponse().getStatus()); verifyNoInteractions(bookings,dishes,details,notifications);
        assertNull(r.getRequest().getAttribute("ipad_staff_id"));
    }
    @Test void storeMismatchIsActuallyRejectedBeforeControllerWrites() throws Exception {
        var r=mvc.perform(req("order/add-dishes","SYN-EMPTY",900001,2,batch())).andReturn();
        assertEquals(403,r.getResponse().getStatus()); verifyNoInteractions(bookings,dishes,details,notifications);
    }
    @Test void nonexistentDeviceIsActuallyRejectedBeforeControllerWrites() throws Exception {
        var r=mvc.perform(req("order/add-dishes","SYN-NONE",900001,1,batch())).andReturn();
        assertEquals(401,r.getResponse().getStatus()); verifyNoInteractions(bookings,dishes,details,notifications);
    }
}

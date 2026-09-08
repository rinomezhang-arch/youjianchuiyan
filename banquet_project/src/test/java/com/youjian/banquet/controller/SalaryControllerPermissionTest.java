package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.MonthSalary;
import com.youjian.banquet.entity.SalaryTemplate;
import com.youjian.banquet.service.SalaryService;
import com.youjian.banquet.util.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TL-OPS-PAYROLL-PAYOUT-GUARD-11 契约测试（单元级，不依赖生产库）。
 * <p>
 * 验证 SalaryController 补齐 HR 权限 + 门店范围后的关键行为：
 * 未授权拒绝、跨店拒绝、总经理范围。
 * <p>
 * 用 UserContext.set() 注入登录上下文，用 Mockito mock JdbcTemplate 的
 * staff_master 权限查询，不碰生产库、不读真实员工数据。
 */
class SalaryControllerPermissionTest {

    private SalaryController controller;

    @Mock private SalaryService salaryService;
    @Mock private JdbcTemplate jdbc;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        controller = new SalaryController();
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "salaryService", salaryService);
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "jdbc", jdbc);
    }

    @AfterEach
    void tearDown() throws Exception {
        UserContext.clear();
        mocks.close();
    }

    /** 未登录（无 staffId）→ 403 拒绝 */
    @Test
    void listTemplate_rejectsWhenNotLoggedIn() {
        UserContext.set(new UserContext.CurrentUser(null, 1L, "manager", "x"));
        Result<List<SalaryTemplate>> r = controller.listTemplates(1L);
        assertEquals(403, r.getCode());
    }

    /** 非 HR（can_manage_hr=0）→ 403 拒绝 */
    @Test
    void list_rejectsWhenNotHr() {
        UserContext.set(new UserContext.CurrentUser(99L, 1L, "staff", "putong"));
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(
                Map.of("store_id", 1L, "can_view_all_stores", 0, "can_manage_hr", 0)));
        Result<List<MonthSalary>> r = controller.list(1L, "2026-08");
        assertEquals(403, r.getCode());
    }

    /** 店长(HR)跨店访问他人门店 → 403 拒绝 */
    @Test
    void list_rejectsWhenCrossStore() {
        UserContext.set(new UserContext.CurrentUser(2L, 1L, "manager", "dianzhang"));
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(
                Map.of("store_id", 1L, "can_view_all_stores", 0, "can_manage_hr", 1)));
        Result<List<MonthSalary>> r = controller.list(2L, "2026-08"); // 请求 2 店，但本店是 1
        assertEquals(403, r.getCode());
        verify(salaryService, never()).listSalary(anyLong(), any());
    }

    /** 店长(HR)访问本店 → 放行，且强制用本店 storeId（不信任请求参数） */
    @Test
    void list_allowsOwnStoreAndForcesOwnStoreId() {
        UserContext.set(new UserContext.CurrentUser(2L, 1L, "manager", "dianzhang"));
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(
                Map.of("store_id", 1L, "can_view_all_stores", 0, "can_manage_hr", 1)));
        when(salaryService.listSalary(anyLong(), any())).thenReturn(Collections.emptyList());
        Result<List<MonthSalary>> r = controller.list(1L, "2026-08");
        assertEquals(200, r.getCode());
        verify(salaryService).listSalary(eq(1L), eq("2026-08"));
    }

    /** 总经理(全店)访问任意门店 → 放行 */
    @Test
    void list_allowsGeneralManagerAnyStore() {
        UserContext.set(new UserContext.CurrentUser(1L, 0L, "gm", "zongjingli"));
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(
                Map.of("store_id", 0L, "can_view_all_stores", 1, "can_manage_hr", 1)));
        when(salaryService.listSalary(anyLong(), any())).thenReturn(Collections.emptyList());
        Result<List<MonthSalary>> r = controller.list(2L, "2026-08");
        assertEquals(200, r.getCode());
        verify(salaryService).listSalary(eq(2L), eq("2026-08"));
    }

    /** 删除模板：非全店用户删他人门店模板 → 403 */
    @Test
    void deleteTemplate_rejectsCrossStoreTemplate() {
        UserContext.set(new UserContext.CurrentUser(2L, 1L, "manager", "dianzhang"));
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(
                List.of(Map.of("store_id", 1L, "can_view_all_stores", 0, "can_manage_hr", 1)), // checkHrAccess
                        List.of(Map.of("store_id", 2L))); // 目标模板在 2 店
        Result<Void> r = controller.deleteTemplate(10L);
        assertEquals(403, r.getCode());
        verify(salaryService, never()).deleteTemplate(anyLong());
    }

    /** 缺口1修复：非1号店 HR 省略 storeId → 强制用本店，而非报403 */
    @Test
    void list_omittedStoreIdForcesOwnStore() {
        UserContext.set(new UserContext.CurrentUser(2L, 2L, "manager", "dianzhang2")); // 本店=2
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(
                Map.of("store_id", 2L, "can_view_all_stores", 0, "can_manage_hr", 1)));
        when(salaryService.listSalary(anyLong(), any())).thenReturn(Collections.emptyList());
        Result<List<MonthSalary>> r = controller.list(null, "2026-08"); // 省略 storeId
        assertEquals(200, r.getCode());
        verify(salaryService).listSalary(eq(2L), eq("2026-08")); // 强制本店 2
    }

    /** 缺口2修复：updateTemplate 跨店定位他人模板 id → 403 且不调 service */
    @Test
    void updateTemplate_rejectsCrossStoreExistingTemplate() {
        UserContext.set(new UserContext.CurrentUser(2L, 1L, "manager", "dianzhang"));
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(
                List.of(Map.of("store_id", 1L, "can_view_all_stores", 0, "can_manage_hr", 1)), // checkHrAccess
                        List.of(Map.of("store_id", 2L))); // 已存在模板在 2 店
        SalaryTemplate t = new SalaryTemplate();
        t.setStoreId(1L);
        Result<SalaryTemplate> r = controller.updateTemplate(10L, t);
        assertEquals(403, r.getCode());
        verify(salaryService, never()).updateTemplate(anyLong(), any());
    }

    /** 缺口2修复：updateTemplate 本店模板 → 放行 */
    @Test
    void updateTemplate_allowsOwnStoreExistingTemplate() {
        UserContext.set(new UserContext.CurrentUser(2L, 1L, "manager", "dianzhang"));
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(
                List.of(Map.of("store_id", 1L, "can_view_all_stores", 0, "can_manage_hr", 1)), // checkHrAccess
                        List.of(Map.of("store_id", 1L))); // 已存在模板在本店
        when(salaryService.updateTemplate(anyLong(), any())).thenReturn(new SalaryTemplate());
        SalaryTemplate t = new SalaryTemplate();
        t.setStoreId(1L);
        Result<SalaryTemplate> r = controller.updateTemplate(10L, t);
        assertEquals(200, r.getCode());
        verify(salaryService).updateTemplate(eq(10L), any());
    }
}

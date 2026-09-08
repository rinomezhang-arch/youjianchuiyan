package com.youjian.banquet.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TL-OPS-PAYROLL-PAYOUT-GUARD-11 契约测试（单元级，不依赖生产库/隔离库）。
 * <p>
 * 覆盖 PayrollService 发放记账链路的关键契约：
 * 未审批禁发、重复发放台账唯一性、坏数据整批回滚（事务回滚触发点）。
 * <p>
 * 用 Mockito mock JdbcTemplate，不碰生产库、不读真实员工工资数据。
 */
class PayrollPayoutGuardTest {

    private PayrollService service;

    @Mock private JdbcTemplate jdbc;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new PayrollService();
        ReflectionTestUtils.setField(service, "jdbc", jdbc);
    }

    /** 未审批发放拒绝：仍有 status=1 未审批记录 → PayrollRejectedException */
    @Test
    void payout_rejectsWhenPendingUnapproved() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(Object[].class)))
                .thenAnswer(inv -> {
                    Object[] all = inv.getArguments();
                    // all[0]=sql, all[1]=Integer.class, all[2..]=varargs {month, status, storeId}
                    int n = all.length - 2;
                    if (n == 3 && ((Number) all[3]).intValue() == 1) return 3; // pending
                    return 0;
                });
        PayrollService.PayrollContext ctx = new PayrollService.PayrollContext(false, 1L, 2L, "zhangjing");
        assertThrows(PayrollService.PayrollRejectedException.class,
                () -> service.payout("2026-08", ctx));
    }

    /** 重复发放台账唯一性：已发放(status=3)再次发放 → alreadyRecorded=true，不写新台账 */
    @Test
    void payout_repeatedDoesNotWriteAgain() {
        // pending(status=1)=0, batch(status=2 FOR UPDATE)空, total>0, paidAlready(status=3)>0
        // pending/paidAlready 的 SQL 字符串相同，用参数值区分：
        //   变量 varargs = {month, status?, storeId}（total 无 status，长度2）
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(Object[].class)))
                .thenAnswer(inv -> {
                    Object[] all = inv.getArguments();
                    int n = all.length - 2;
                    if (n == 2) return 5;                                        // total={month, storeId}
                    if (n == 3 && ((Number) all[3]).intValue() == 1) return 0;   // pending={month,1,storeId}
                    if (n == 3 && ((Number) all[3]).intValue() == 3) return 5;   // paidAlready={month,3,storeId}
                    return 0;
                });
        when(jdbc.queryForList(contains("FOR UPDATE"), any(Object[].class)))
                .thenReturn(Collections.emptyList());

        PayrollService.PayrollContext ctx = new PayrollService.PayrollContext(false, 1L, 2L, "zhangjing");
        Map<String, Object> r = service.payout("2026-08", ctx);

        assertEquals(0, r.get("paid"));
        assertEquals(true, r.get("alreadyRecorded"));
        assertEquals(5, r.get("previouslyRecorded"));
        // 未写新台账（不调用 INSERT INTO payroll_payout_record）
        verify(jdbc, never()).update(any(org.springframework.jdbc.core.PreparedStatementCreator.class), any());
        verify(jdbc, never()).update(contains("payroll_payout_record"), any(Object[].class));
    }

    /** 坏数据整批回滚触发点：save 传入负金额 → PayrollRejectedException，整批拒绝 */
    @Test
    void save_rejectsNegativeAmountWholeBatch() {
        when(jdbc.queryForList(contains("SELECT staff_id, store_id FROM staff_master"), any(Object[].class)))
                .thenReturn(List.of(Map.of("staff_id", 1, "store_id", 1L)));
        PayrollService.PayrollContext ctx = new PayrollService.PayrollContext(true, null, 1L, "zhangjing");
        List<Map<String, Object>> rows = List.of(
                Map.of("emp_id", 1, "base_salary", new BigDecimal("-5")));
        assertThrows(PayrollService.PayrollRejectedException.class,
                () -> service.save("2026-08", rows, ctx));
        // 未写库
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    /** 坏数据：跨店员工整批拒绝 */
    @Test
    void save_rejectsCrossStoreWholeBatch() {
        when(jdbc.queryForList(contains("SELECT staff_id, store_id FROM staff_master"), any(Object[].class)))
                .thenReturn(List.of(Map.of("staff_id", 1, "store_id", 2L))); // 员工在2店
        PayrollService.PayrollContext ctx = new PayrollService.PayrollContext(false, 1L, 2L, "zhangjing"); // 店长在1店
        List<Map<String, Object>> rows = List.of(
                Map.of("emp_id", 1, "base_salary", "5000"));
        assertThrows(PayrollService.PayrollRejectedException.class,
                () -> service.save("2026-08", rows, ctx));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }
}

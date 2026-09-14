package com.youjian.banquet.controller;

import com.youjian.banquet.config.ApprovalAuthorityInterceptor;
import com.youjian.banquet.service.PayrollService;
import com.youjian.banquet.util.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * TL-PAYROLL-DENIAL-REGRESSION-76：工资审批/付款越权拒绝聚焦回归（MockMvc，零数据库）。
 *
 * <p>与既有用例的分工（不重复造测试）：
 * <ul>
 *   <li>{@code PayrollHttpMysqlFlowTest#signedNonApproverCannotApprove}（L141）已覆盖「非批复人 approve 被拒(400)」，
 *       但它的 MockMvc 只挂了 {@code JwtAuthInterceptor}（L79），<b>未接入真实 ApprovalAuthorityInterceptor</b>，
 *       且依赖真实 MySQL，只断言单号/台账未新增，<b>没有</b>断言审批/付款服务零调用。</li>
 *   <li>{@code PayrollHttpMysqlFlowTest#missingJwtIsRejectedBeforeAnyPayrollOrAuditWrites}（L131）已覆盖 save/approve/payout
 *       缺 JWT 的 401——本类<b>不重复</b>该场景。</li>
 *   <li>本类新增：真实 Controller + 真实审批白名单拦截器在链上，非白名单员工打 /approve、/pay、/payout
 *       一律被拦且 {@code PayrollService}/{@code JdbcTemplate} 零交互；并给出一个白名单合成角色的正向对照，
 *       证明不是所有请求都被夹具误拦。不连库、不起真实 HTTP、不报成真实 JWT 全链。</li>
 * </ul>
 */
class PayrollDeniedActionRegressionTest {

    private static final String MONTH = "2026-08";
    /** 白名单内（合成夹具）：approval.approvers 只放这一个。 */
    private static final String WHITELISTED = "synthetic_approver";
    /** 白名单外（合成夹具）：在册在职但不是批复人。 */
    private static final String NOT_WHITELISTED = "synthetic_worker";
    /** 拦截器真实拒绝文案（ApprovalAuthorityInterceptor#reject）。 */
    private static final String DENY_MESSAGE = "当前仅张婧、张晓秋有审批批复权限，请转交他们处理";

    private JdbcTemplate jdbc;
    private PayrollService payrollService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        payrollService = mock(PayrollService.class);

        ApprovalAuthorityInterceptor interceptor = new ApprovalAuthorityInterceptor();
        ReflectionTestUtils.setField(interceptor, "enabled", true);
        ReflectionTestUtils.setField(interceptor, "approversRaw", WHITELISTED);
        ReflectionTestUtils.setField(interceptor, "jdbcTemplate", jdbc);

        PayrollController controller = new PayrollController();
        ReflectionTestUtils.setField(controller, "jdbc", jdbc);
        ReflectionTestUtils.setField(controller, "payrollService", payrollService);

        mvc = MockMvcBuilders.standaloneSetup(controller).addInterceptors(interceptor).build();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    /** 非批复人打真实 approve 路由：被拦、返回 403 业务错误、审批与库均零调用。 */
    @Test
    void nonWhitelistedEmployeeCannotApproveAndServiceStaysUntouched() throws Exception {
        assertDeniedAndNoServiceCall("/api/hr/payroll/approve");
    }

    /** 非批复人打真实 pay 路由：同样被拦且付款服务零调用。 */
    @Test
    void nonWhitelistedEmployeeCannotPayAndServiceStaysUntouched() throws Exception {
        assertDeniedAndNoServiceCall("/api/hr/payroll/pay");
    }

    /** 非批复人打 pay 的语义别名 payout：同样被拦。 */
    @Test
    void nonWhitelistedEmployeeCannotPayoutAndServiceStaysUntouched() throws Exception {
        assertDeniedAndNoServiceCall("/api/hr/payroll/payout");
    }

    /** 正向对照：白名单合成角色请求可达控制器并真实调用一次审批服务（证明夹具未误拦所有请求）。 */
    @Test
    void whitelistedApproverReachesControllerAndCallsApproveOnce() throws Exception {
        UserContext.set(new UserContext.CurrentUser(1L, 1L, "store_manager", WHITELISTED));
        when(jdbc.queryForList(anyString(), Mockito.any(Object.class))).thenReturn(List.of(
                Map.of("store_id", 1, "can_view_all_stores", 1, "can_manage_hr", 1)));
        when(payrollService.approve(eq(MONTH), any())).thenReturn(Map.of("approved", 0));

        String body = mvc.perform(post("/api/hr/payroll/approve")
                        .param("month", MONTH)
                        .requestAttr("jwt_subject", WHITELISTED))
                .andReturn().getResponse().getContentAsString();

        assertFalse(body.contains(DENY_MESSAGE), "白名单角色不应被审批拦截器拒绝，实际响应：" + body);
        verify(payrollService, times(1)).approve(eq(MONTH), any());
    }

    private void assertDeniedAndNoServiceCall(String path) throws Exception {
        String body = mvc.perform(post(path)
                        .param("month", MONTH)
                        .requestAttr("jwt_subject", NOT_WHITELISTED))
                .andReturn().getResponse().getContentAsString();

        assertTrue(body.contains(DENY_MESSAGE), "应被审批白名单拦截，实际响应：" + body);
        assertTrue(body.contains("403"), "应返回 403 业务错误，实际响应：" + body);
        verifyNoInteractions(payrollService, jdbc);
        Mockito.verifyNoMoreInteractions(payrollService);
    }
}

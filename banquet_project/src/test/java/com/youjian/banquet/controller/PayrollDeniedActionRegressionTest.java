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

import java.nio.charset.StandardCharsets;
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
 * TL-PAYROLL-DENIAL-REGRESSION-76：工资审批/付款越权拒绝聚焦回归（MockMvc，零数据库、零真实 HTTP）。
 *
 * <p>与既有用例的分工（引用原用例，不重复造）：
 * <ul>
 *   <li>{@code PayrollHttpMysqlFlowTest#signedNonApproverCannotApprove}（L141）已覆盖「非批复人 approve 被拒(400)」，
 *       但其 MockMvc 只挂 {@code JwtAuthInterceptor}（L79），未接入真实 {@code ApprovalAuthorityInterceptor}，
 *       且依赖真实 MySQL，未断言服务零调用。</li>
 *   <li>{@code PayrollHttpMysqlFlowTest#missingJwtIsRejectedBeforeAnyPayrollOrAuditWrites}（L131）覆盖 save/approve/payout
 *       缺 JWT 的 401——本类<b>不重复</b>。</li>
 * </ul>
 *
 * <p><b>本类覆盖两条互相独立的拒绝语义，报告里必须分开陈述：</b>
 * <ol>
 *   <li><b>/approve</b>：真实 {@code ApprovalAuthorityInterceptor}（张婧/张晓秋白名单）拦截非白名单员工；
 *       该两条路由的拒绝属「审批批复权」控制。</li>
 *   <li><b>/pay、/payout</b>：该两路径<b>不匹配</b>拦截器的动作正则（只认 approve|reject），
 *       所以拒绝来自 {@code PayrollController.checkPayrollAccess()} 的 RBAC 判定（已登录但
 *       {@code can_manage_hr}≠1 → 403「无权访问薪酬数据」），<b>不是</b>付款白名单。</li>
 * </ol>
 * <p><b>不宣称</b>：本类不宣称「付款已纳入张婧/张晓秋白名单」，也不宣称已验证真实 JWT 全链或登录员工 RBAC 全矩阵。
 */
class PayrollDeniedActionRegressionTest {

    private static final String MONTH = "2026-08";
    /** 白名单内（合成夹具）：approval.approvers 只放这一个。 */
    private static final String WHITELISTED = "synthetic_approver";
    /** 白名单外（合成夹具）：在册在职但不是批复人。 */
    private static final String NOT_WHITELISTED = "synthetic_worker";
    /** 拦截器真实拒绝文案（ApprovalAuthorityInterceptor#reject）。 */
    private static final String INTERCEPTOR_DENY_MESSAGE = "当前仅张婧、张晓秋有审批批复权限，请转交他们处理";
    /** 控制器 RBAC 拒绝文案（PayrollController#checkPayrollAccess）。 */
    private static final String RBAC_DENY_MESSAGE = "无权访问薪酬数据";

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

    /**
     * 非批复人打真实 approve 路由：被真实白名单拦截器拒绝，审批服务与库均零调用。
     * 语义 = 审批批复权（张婧/张晓秋白名单）。
     */
    @Test
    void nonWhitelistedEmployeeCannotApproveAndServiceStaysUntouched() throws Exception {
        String body = mvc.perform(post("/api/hr/payroll/approve")
                        .param("month", MONTH)
                        .requestAttr("jwt_subject", NOT_WHITELISTED))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertTrue(body.contains(INTERCEPTOR_DENY_MESSAGE), "应被审批白名单拦截，实际响应：" + body);
        assertTrue(body.contains("403"), "应返回 403 业务错误，实际响应：" + body);
        verifyNoInteractions(payrollService, jdbc);
        Mockito.verifyNoMoreInteractions(payrollService);
    }

    /**
     * 已登录但无薪酬权限的合成员工打 /pay：403 且付款服务零调用。
     * 语义 = Controller RBAC（can_manage_hr≠1）；<b>不</b>等于付款白名单已验证。
     */
    @Test
    void loggedInEmployeeWithoutPayrollPermissionCannotPayAndServiceStaysUntouched() throws Exception {
        assertRbacDeniedAndNoServiceCall("/api/hr/payroll/pay");
    }

    /** 同上，覆盖 /pay 的语义别名 /payout。 */
    @Test
    void loggedInEmployeeWithoutPayrollPermissionCannotPayoutAndServiceStaysUntouched() throws Exception {
        assertRbacDeniedAndNoServiceCall("/api/hr/payroll/payout");
    }

    /**
     * 正向对照一：白名单合成角色打 /approve 可达控制器并真实调用一次审批服务。
     * 证明夹具没有把所有请求一律拦死。
     */
    @Test
    void whitelistedApproverReachesControllerAndCallsApproveOnce() throws Exception {
        UserContext.set(new UserContext.CurrentUser(1L, 1L, "store_manager", WHITELISTED));
        when(jdbc.queryForList(anyString(), Mockito.any(Object.class))).thenReturn(List.of(
                Map.of("store_id", 1, "can_view_all_stores", 1, "can_manage_hr", 1)));
        when(payrollService.approve(eq(MONTH), any())).thenReturn(Map.of("approved", 0));

        String body = mvc.perform(post("/api/hr/payroll/approve")
                        .param("month", MONTH)
                        .requestAttr("jwt_subject", WHITELISTED))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertFalse(body.contains(INTERCEPTOR_DENY_MESSAGE), "白名单角色不应被审批拦截器拒绝，实际响应：" + body);
        assertFalse(body.contains(RBAC_DENY_MESSAGE), "有薪酬权限的角色不应被 RBAC 拒绝，实际响应：" + body);
        verify(payrollService, times(1)).approve(eq(MONTH), any());
    }

    /**
     * 已登录（合成身份 staffId=2，在册在职、非白名单、can_manage_hr=0）打 payment 路由：
     * 断言 403 + 付款服务零调用，且拒绝文案是 RBAC 的「无权访问薪酬数据」而不是「未登录」，
     * 以证明这是登录员工被权限拒绝，而非仅未认证被拒。
     */
    private void assertRbacDeniedAndNoServiceCall(String path) throws Exception {
        UserContext.set(new UserContext.CurrentUser(2L, 1L, "store_manager", NOT_WHITELISTED));
        when(jdbc.queryForList(anyString(), Mockito.any(Object.class))).thenReturn(List.of(
                Map.of("store_id", 1, "can_view_all_stores", 0, "can_manage_hr", 0)));

        String body = mvc.perform(post(path)
                        .param("month", MONTH)
                        .requestAttr("jwt_subject", NOT_WHITELISTED))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertTrue(body.contains("403"), "应返回 403 业务错误，实际响应：" + body);
        assertTrue(body.contains(RBAC_DENY_MESSAGE), "拒绝应来自 RBAC（已登录但无薪酬权限），实际响应：" + body);
        verifyNoInteractions(payrollService);
    }
}

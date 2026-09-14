package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CL-AUTH-BLANK-PASSWORD-GUARD-70 专属合成测试。
 * <p>
 * 纯 MockMvc + Mockito 合成夹具，不连数据库、不读真实账号口令、不发 JWT。
 * 覆盖两端 null/空串/纯空白密码均不查库不获 token；有效密码路径（BCrypt/历史明文）
 * 保留不受影响，且未被 trim 改动。
 */
class AuthBlankPasswordGuardTest {

    private JdbcTemplate mockJdbc(AuthController c) {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ReflectionTestUtils.setField(c, "jdbcTemplate", jdbc);
        ReflectionTestUtils.setField(c, "jwtSecret", "synthetic-test-secret-at-least-32-bytes-long");
        ReflectionTestUtils.setField(c, "jwtExpiration", 3600000L);
        return jdbc;
    }

    private JdbcTemplate mockIpadJdbc(IpadAuthController c) {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ReflectionTestUtils.setField(c, "jdbc", jdbc);
        return jdbc;
    }

    // ---------- AuthController：PC 端 ----------

    @Test
    void authRejectsNullPasswordWithoutQueryingDb() throws Exception {
        AuthController controller = new AuthController();
        JdbcTemplate jdbc = mockJdbc(controller);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        Map<String, String> body = new HashMap<>();
        body.put("username", "synthetic_user");
        body.put("password", null);

        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(jdbc);
    }

    @Test
    void authRejectsEmptyAndBlankPasswordWithoutQueryingDb() throws Exception {
        AuthController controller = new AuthController();
        JdbcTemplate jdbc = mockJdbc(controller);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();

        for (String blank : new String[]{"", "   ", "\t\n"}) {
            Map<String, String> body = new HashMap<>();
            body.put("username", "synthetic_user");
            body.put("password", blank);
            mvc.perform(post("/api/auth/login").contentType("application/json")
                            .content(om.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        }
        verifyNoInteractions(jdbc);
    }

    @Test
    void authValidBcryptPasswordStillLogsInAndPasswordIsNotTrimmedBeforeMatch() throws Exception {
        AuthController controller = new AuthController();
        JdbcTemplate jdbc = mockJdbc(controller);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        // 密码故意带首尾空格，验证不会被 trim 掉再比对——BCrypt 编码时就是这个带空格的原始值。
        String rawPasswordWithSpaces = " s3cret! ";
        String bcryptHash = new BCryptPasswordEncoder().encode(rawPasswordWithSpaces);
        Map<String, Object> staff = new HashMap<>();
        staff.put("staff_id", 1);
        staff.put("store_id", 1);
        staff.put("role", "waiter");
        staff.put("staff_name", "合成员工");
        staff.put("staff_account", "synthetic_user");
        staff.put("staff_password", bcryptHash);
        // 用更明确的 varargs 匹配方式；之前用 (Object[]) any() 混合 anyString() 没能正确
        // 匹配到 4 个 String 参数的重载调用，导致查询命中空桩，误判成"用户不存在"。
        lenient().when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(staff));

        Map<String, String> body = new HashMap<>();
        body.put("username", "synthetic_user");
        body.put("password", rawPasswordWithSpaces);

        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(jdbc, atLeastOnce()).queryForList(anyString(), (Object[]) any());
    }

    // ---------- IpadAuthController：iPad 端 ----------

    @Test
    void ipadRejectsNullAndBlankPasswordWithoutQueryingDb() throws Exception {
        IpadAuthController controller = new IpadAuthController();
        JdbcTemplate jdbc = mockIpadJdbc(controller);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();

        for (String blank : new String[]{null, "", "  "}) {
            Map<String, String> body = new HashMap<>();
            body.put("phone", "13800000000");
            if (blank != null) body.put("password", blank);
            mvc.perform(post("/api/ipad/login").contentType("application/json")
                            .content(om.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        }
        verifyNoInteractions(jdbc);
    }

    @Test
    void ipadValidLegacyPlaintextPasswordStillLogsIn() throws Exception {
        IpadAuthController controller = new IpadAuthController();
        JdbcTemplate jdbc = mockIpadJdbc(controller);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        Map<String, Object> staff = new HashMap<>();
        staff.put("staff_id", 2);
        staff.put("store_id", 1);
        staff.put("role", "cashier");
        staff.put("staff_name", "合成收银员");
        staff.put("staff_phone", "13800000000");
        staff.put("staff_password", "plainpass123");
        lenient().when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(staff));

        Map<String, String> body = new HashMap<>();
        body.put("phone", "13800000000");
        body.put("password", "plainpass123");

        mvc.perform(post("/api/ipad/login").contentType("application/json")
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}

package com.youjian.banquet.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youjian.banquet.controller.AuthController;
import com.youjian.banquet.controller.IpadAuthController;
import com.youjian.banquet.controller.IpadOrderController;
import com.youjian.banquet.util.UserContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Exercises the real advice and captured JDBC write; no database or login-success claim. */
class AuthAuditPrivacyTest {
    @AfterEach void clear() { UserContext.clear(); }

    @Test void loginAndAuthorizationOmitCredentialsOnSuccessAndException() throws Throwable {
        for (Class<?> type : new Class<?>[]{AuthController.class, IpadAuthController.class, IpadOrderController.class}) {
            for (String method : type == IpadOrderController.class
                    ? new String[]{"authVerify", "addDishesBatch"} : new String[]{"login"}) {
                for (boolean fail : new boolean[]{false, true}) {
                    String syntheticSecret = "SYN-private-never-persist";
                    Map<String, Object> body = Map.of("password", syntheticSecret,
                            "nested", Map.of("authorization_token", syntheticSecret));
                    AtomicReference<String> stored = new AtomicReference<>();
                    var pjp = joinPoint(type, method, body);
                    RuntimeException failure = new RuntimeException(syntheticSecret);
                    Object response = new Object();
                    if (fail) when(pjp.proceed()).thenThrow(failure);
                    else when(pjp.proceed()).thenReturn(response);
                    var aspect = aspect(stored);
                    if (fail) assertSame(failure, assertThrows(RuntimeException.class,
                            () -> aspect.aroundWriteOperation(pjp)));
                    else assertSame(response, aspect.aroundWriteOperation(pjp));
                    assertNotNull(stored.get());
                    assertFalse(stored.get().contains(syntheticSecret));
                    var json = new ObjectMapper().readTree(stored.get());
                    assertEquals(fail ? "error" : "success", json.get("result").asText());
                    assertTrue(json.has("elapsedMs"));
                    assertSame(body, pjp.getArgs()[0]);
                    verify(pjp, times(1)).proceed();
                }
            }
        }
    }

    @Test void nonCredentialOperationPreservesExistingAuditDetail() throws Throwable {
        AtomicReference<String> stored = new AtomicReference<>();
        var pjp = joinPoint(IpadOrderController.class, "sendToKitchen", Map.of("booking_id", "SYN-BOOKING"));
        aspect(stored).aroundWriteOperation(pjp);
        var json = new ObjectMapper().readTree(stored.get());
        assertEquals("SYN-BOOKING", json.get("args").get(0).get("booking_id").asText());
    }

    private ProceedingJoinPoint joinPoint(Class<?> type, String method, Object body) {
        var pjp = mock(ProceedingJoinPoint.class);
        var signature = mock(Signature.class);
        when(signature.getDeclaringType()).thenReturn(type);
        when(signature.getName()).thenReturn(method);
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(new Object[]{body});
        return pjp;
    }

    private AuditLogAspect aspect(AtomicReference<String> stored) {
        var jdbc = mock(JdbcTemplate.class);
        doAnswer(call -> {
            stored.set((String) call.getArguments()[4]);
            return 1;
        }).when(jdbc).update(anyString(), any(Object[].class));
        var aspect = new AuditLogAspect();
        ReflectionTestUtils.setField(aspect, "jdbcTemplate", jdbc);
        return aspect;
    }
}

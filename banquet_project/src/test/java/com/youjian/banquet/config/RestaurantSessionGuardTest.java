package com.youjian.banquet.config;

import com.youjian.banquet.auth.StaffRealtimeGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestaurantSessionGuardTest {
    StaffRealtimeGuard guard;
    RestaurantSessionGuardInterceptor interceptor;
    MockHttpServletRequest request;
    MockHttpServletResponse response;
    @BeforeEach void setup() {
        guard=mock(StaffRealtimeGuard.class); interceptor=new RestaurantSessionGuardInterceptor(guard);
        request=new MockHttpServletRequest("GET", "/api/stock-takes");response=new MockHttpServletResponse();
        request.setAttribute("jwt_staff_id", 101L);request.setAttribute("jwt_store_id", 1L);request.setAttribute("jwt_role", "manager");
    }
    void current(boolean active, long store, String role) { when(guard.verify(101L)).thenReturn(new StaffRealtimeGuard.Verdict(active,store,role)); }
    boolean run() throws Exception { return interceptor.preHandle(request,response,new Object()); }
    @Test void activeSameIdentityPassesWithoutRewritingClaims() throws Exception {current(true,1,"manager");assertTrue(run());assertEquals(1L,request.getAttribute("jwt_store_id"));}
    @Test void departedStaffRejected() throws Exception {current(false,1,"manager");assertFalse(run());assertEquals(401,response.getStatus());}
    @Test void transferredStaffMustLoginAgain() throws Exception {current(true,2,"manager");assertFalse(run());assertEquals(1L,request.getAttribute("jwt_store_id"));}
    @Test void demotedStaffMustLoginAgain() throws Exception {current(true,1,"staff");assertFalse(run());}
    @Test void rolePromotionAlsoRequiresNewLogin() throws Exception {current(true,1,"gm");assertFalse(run());}
    @Test void convertedToLawyerCannotKeepRestaurantToken() throws Exception {current(true,1,"lawyer");assertFalse(run());}
    @Test void databaseFailureDoesNotReachController() throws Exception {when(guard.verify(101L)).thenThrow(new RuntimeException("private error"));assertFalse(run());assertFalse(response.getContentAsString().contains("private error"));}
    @Test void missingStaffIdRejectedBeforeLookup() throws Exception {request.removeAttribute("jwt_staff_id");assertFalse(run());verifyNoInteractions(guard);}
    @Test void legalRoutesNeverQueryStaffHere() throws Exception {request.setRequestURI("/api/legal/case");assertTrue(run());verifyNoInteractions(guard);}
    @Test void lawyersExistingMeContractUntouched() throws Exception {request.setRequestURI("/api/auth/me");request.setAttribute("jwt_role","lawyer");assertTrue(run());verifyNoInteractions(guard);}
    @Test void publicAndSelfServiceRoutesStayExcluded() throws Exception {for(String path:new String[]{"/api/public/menu","/api/auth/login","/api/hr/self-service/submit","/api/hr/job-postings/open","/api/bookings/confirm/test"}){request.setRequestURI(path);assertTrue(run());}verifyNoInteractions(guard);}
    @Test void lookalikePathsAreNotExempt() throws Exception {for(String path:new String[]{"/api/legal-export","/api/public-private","/api/bookings/confirm/test/extra"})assertFalse(RestaurantSessionGuardInterceptor.excluded(path));}
    @Test void preflightDoesNotQueryStaff() throws Exception {request.setMethod("OPTIONS");assertTrue(run());verifyNoInteractions(guard);}
    void ipad() {request.setRequestURI("/api/ipad/orders");request.removeAttribute("jwt_role");request.setAttribute("ipad_staff_id",101L);request.setAttribute("ipad_store_id",1L);}
    @Test void ipadBindingMustMatchCurrentStaffStore() throws Exception {ipad();current(true,2,"staff");assertFalse(run());}
    @Test void ipadValidStaffPasses() throws Exception {ipad();current(true,1,"staff");assertTrue(run());}
    @Test void ipadGlobalStaffRemainsBoundByEarlierDeviceCheck() throws Exception {ipad();current(true,0,"gm");assertTrue(run());}
    @Test void ipadLawyerIsRejected() throws Exception {ipad();current(true,1,"lawyer");assertFalse(run());}
    @RestController static class Endpoint {int writes;@GetMapping("/api/stock-takes")String read(){writes++;return "ok";}}
    @Test void mvcDoesNotInvokeBusinessEndpointForStaleToken() throws Exception {
        current(true,2,"manager");Endpoint endpoint=new Endpoint();
        MockMvc mvc=MockMvcBuilders.standaloneSetup(endpoint).addInterceptors(interceptor).build();
        mvc.perform(get("/api/stock-takes").requestAttr("jwt_staff_id",101L).requestAttr("jwt_store_id",1L).requestAttr("jwt_role","manager")).andExpect(status().isUnauthorized());
        assertEquals(0,endpoint.writes);
    }
}

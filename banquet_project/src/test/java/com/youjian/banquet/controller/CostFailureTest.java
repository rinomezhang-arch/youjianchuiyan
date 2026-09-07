package com.youjian.banquet.controller;
import com.youjian.banquet.util.UserContext;
import org.junit.jupiter.api.*;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class CostFailureTest {
    @AfterEach void clear(){UserContext.clear();}
    @Test void databaseFailureCannotLookLikeSuccessfulZeroCostOrEmptyRanking(){
        UserContext.set(new UserContext.CurrentUser(1L,1L,"store_manager","合成验收员"));
        JdbcTemplate jdbc=mock(JdbcTemplate.class, invocation->{throw new DataAccessResourceFailureException("synthetic unavailable database");});
        CostController controller=new CostController();ReflectionTestUtils.setField(controller,"jdbc",jdbc);
        assertEquals(500,controller.getSummary("1").getCode());
        assertEquals(500,controller.getCategories("1").getCode());
        assertEquals(500,controller.getRanking("1",null,null,"rate",1,50).getCode());
    }
}

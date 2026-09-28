package com.miniproject1.ai.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DatabaseHealthController.class)
class DatabaseHealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // 테스트에서는 실제 DB 대신 JdbcTemplate의 응답만 대체합니다.
    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @Test
    void databaseHealthReturnsUp() throws Exception {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

        mockMvc.perform(get("/api/health/database"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.database").value("MariaDB"))
                .andExpect(jsonPath("$.status").value("UP"));
    }
}

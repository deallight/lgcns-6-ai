package com.miniproject1.ai.program;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProgramController.class)
class ProgramControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // 컨트롤러 테스트에서는 서비스 응답만 대체합니다.
    @MockitoBean
    private ProgramService programService;

    @Test
    void getProgramReturnsOneProgram() throws Exception {
        String pblancId = "PBLN_TEST";
        ProgramResponse response = new ProgramResponse(
                pblancId,
                "테스트 공고",
                "https://example.com/program.pdf");

        when(programService.findByPblancId(pblancId)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/programs/{pblancId}", pblancId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pblancId").value(pblancId))
                .andExpect(jsonPath("$.title").value("테스트 공고"))
                .andExpect(jsonPath("$.printFilePathUrl").value("https://example.com/program.pdf"));
    }

    @Test
    void getProgramReturnsNotFound() throws Exception {
        String pblancId = "PBLN_NOT_FOUND";
        when(programService.findByPblancId(pblancId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/programs/{pblancId}", pblancId))
                .andExpect(status().isNotFound());
    }
}

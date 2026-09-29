package com.miniproject1.ai.document.batch;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DocumentBatchController.class)
class DocumentBatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // 컨트롤러 테스트에서는 실제 다운로드와 OpenAI 호출을 실행하지 않습니다.
    @MockitoBean
    private DocumentBatchService documentBatchService;

    @Test
    void startsAllProgramBatchByDefault() throws Exception {
        DocumentBatchStatusResponse response =
                new DocumentBatchStatusResponse("RUNNING", 1500, 1500, 0, 0, 0, null);
        when(documentBatchService.start(1500)).thenReturn(response);

        mockMvc.perform(post("/api/documents/batch/start"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.limit").value(1500))
                .andExpect(jsonPath("$.total").value(1500));
    }

    @Test
    void returnsCurrentBatchStatus() throws Exception {
        DocumentBatchStatusResponse response =
                new DocumentBatchStatusResponse("COMPLETED", 5, 5, 5, 4, 1, null);
        when(documentBatchService.getStatus()).thenReturn(response);

        mockMvc.perform(get("/api/documents/batch/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.processed").value(5))
                .andExpect(jsonPath("$.success").value(4))
                .andExpect(jsonPath("$.failed").value(1));
    }
}

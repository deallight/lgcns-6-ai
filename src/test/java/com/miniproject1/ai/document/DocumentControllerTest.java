package com.miniproject1.ai.document;

import com.miniproject1.ai.document.parser.hwp.HwpParseResponse;
import com.miniproject1.ai.document.parser.pdf.PdfParseResponse;
import com.miniproject1.ai.document.parser.hwpx.HwpxParseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // 컨트롤러 테스트에서는 실제 파일을 다운로드하지 않습니다.
    @MockitoBean
    private DocumentService documentService;

    // 파싱 API 테스트에서는 실제 파일과 DB를 사용하지 않습니다.
    @MockitoBean
    private DocumentParseService documentParseService;

    @Test
    void downloadReturnsSavedFileInformation() throws Exception {
        String pblancId = "PBLN_TEST";
        DocumentDownloadResponse response = new DocumentDownloadResponse(
                pblancId,
                "테스트 공고.pdf",
                "pdf",
                "downloads/pdf/PBLN_TEST/테스트 공고.pdf",
                1024L);

        when(documentService.download(pblancId)).thenReturn(response);

        mockMvc.perform(post("/api/documents/{pblancId}/download", pblancId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pblancId").value(pblancId))
                .andExpect(jsonPath("$.extension").value("pdf"))
                .andExpect(jsonPath("$.savedPath").value("downloads/pdf/PBLN_TEST/테스트 공고.pdf"));
    }

    @Test
    void parsePdfReturnsExtractedText() throws Exception {
        String pblancId = "PBLN_TEST";
        PdfParseResponse response = new PdfParseResponse(
                pblancId,
                "테스트 공고.pdf",
                2,
                8,
                "추출된 테스트");

        when(documentParseService.parsePdf(pblancId)).thenReturn(response);

        mockMvc.perform(post("/api/documents/{pblancId}/parse/pdf", pblancId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pblancId").value(pblancId))
                .andExpect(jsonPath("$.pageCount").value(2))
                .andExpect(jsonPath("$.characterCount").value(8))
                .andExpect(jsonPath("$.text").value("추출된 테스트"));
    }

    @Test
    void parseHwpReturnsExtractedText() throws Exception {
        String pblancId = "PBLN_TEST";
        HwpParseResponse response = new HwpParseResponse(
                pblancId,
                "테스트 공고.hwp",
                1,
                8,
                "추출된 테스트");

        when(documentParseService.parseHwp(pblancId)).thenReturn(response);

        mockMvc.perform(post("/api/documents/{pblancId}/parse/hwp", pblancId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pblancId").value(pblancId))
                .andExpect(jsonPath("$.sectionCount").value(1))
                .andExpect(jsonPath("$.characterCount").value(8))
                .andExpect(jsonPath("$.text").value("추출된 테스트"));
    }

    @Test
    void parseHwpxReturnsExtractedText() throws Exception {
        String pblancId = "PBLN_TEST";
        HwpxParseResponse response = new HwpxParseResponse(
                pblancId,
                "테스트 공고.hwpx",
                1,
                8,
                "추출된 테스트");

        when(documentParseService.parseHwpx(pblancId)).thenReturn(response);

        mockMvc.perform(post("/api/documents/{pblancId}/parse/hwpx", pblancId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pblancId").value(pblancId))
                .andExpect(jsonPath("$.sectionCount").value(1))
                .andExpect(jsonPath("$.characterCount").value(8))
                .andExpect(jsonPath("$.text").value("추출된 테스트"));
    }
}

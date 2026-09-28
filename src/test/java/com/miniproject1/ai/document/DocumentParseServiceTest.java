package com.miniproject1.ai.document;

import com.miniproject1.ai.document.parser.hwp.HwpDocumentParser;
import com.miniproject1.ai.document.parser.hwp.HwpParseResponse;
import com.miniproject1.ai.document.parser.hwpx.HwpxDocumentParser;
import com.miniproject1.ai.document.parser.hwpx.HwpxParseResponse;
import com.miniproject1.ai.document.parser.pdf.PdfDocumentParser;
import com.miniproject1.ai.document.parser.pdf.PdfParseResponse;
import com.miniproject1.ai.document.summary.DocumentSummaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentParseServiceTest {

    @Mock
    private PdfDocumentParser pdfDocumentParser;

    @Mock
    private HwpDocumentParser hwpDocumentParser;

    @Mock
    private HwpxDocumentParser hwpxDocumentParser;

    @Mock
    private ProgramDocumentRepository programDocumentRepository;

    @Mock
    private DocumentSummaryService documentSummaryService;

    @InjectMocks
    private DocumentParseService documentParseService;

    @Test
    void parsePdfSavesExtractedText() {
        String pblancId = "PBLN_TEST";
        PdfParseResponse response = new PdfParseResponse(
                pblancId,
                "테스트 공고.pdf",
                2,
                8,
                "추출된 테스트");

        when(pdfDocumentParser.parse(pblancId)).thenReturn(response);

        documentParseService.parsePdf(pblancId);

        verify(programDocumentRepository).saveOriginalText(pblancId, response.text());
        verify(documentSummaryService).summarizeIfMissing(pblancId, response.text());
    }

    @Test
    void parseHwpSavesExtractedText() {
        String pblancId = "PBLN_TEST";
        HwpParseResponse response = new HwpParseResponse(
                pblancId,
                "테스트 공고.hwp",
                1,
                8,
                "추출된 테스트");

        when(hwpDocumentParser.parse(pblancId)).thenReturn(response);

        documentParseService.parseHwp(pblancId);

        verify(programDocumentRepository).saveOriginalText(pblancId, response.text());
        verify(documentSummaryService).summarizeIfMissing(pblancId, response.text());
    }

    @Test
    void parseHwpxSavesExtractedText() {
        String pblancId = "PBLN_TEST";
        HwpxParseResponse response = new HwpxParseResponse(
                pblancId,
                "테스트 공고.hwpx",
                1,
                8,
                "추출된 테스트");

        when(hwpxDocumentParser.parse(pblancId)).thenReturn(response);

        documentParseService.parseHwpx(pblancId);

        verify(programDocumentRepository).saveOriginalText(pblancId, response.text());
        verify(documentSummaryService).summarizeIfMissing(pblancId, response.text());
    }
}

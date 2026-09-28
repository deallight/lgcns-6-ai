package com.miniproject1.ai.document;

import com.miniproject1.ai.document.parser.hwp.HwpDocumentParser;
import com.miniproject1.ai.document.parser.hwp.HwpParseResponse;
import com.miniproject1.ai.document.parser.hwpx.HwpxDocumentParser;
import com.miniproject1.ai.document.parser.hwpx.HwpxParseResponse;
import com.miniproject1.ai.document.parser.pdf.PdfDocumentParser;
import com.miniproject1.ai.document.parser.pdf.PdfParseResponse;
import com.miniproject1.ai.document.summary.DocumentSummaryService;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 파일 형식별 파싱과 파싱 결과 저장을 연결하는 서비스입니다.
 */
@Service
public class DocumentParseService {

    private final PdfDocumentParser pdfDocumentParser;
    private final HwpDocumentParser hwpDocumentParser;
    private final HwpxDocumentParser hwpxDocumentParser;
    private final ProgramDocumentRepository programDocumentRepository;
    private final DocumentSummaryService documentSummaryService;

    public DocumentParseService(
            PdfDocumentParser pdfDocumentParser,
            HwpDocumentParser hwpDocumentParser,
            HwpxDocumentParser hwpxDocumentParser,
            ProgramDocumentRepository programDocumentRepository,
            DocumentSummaryService documentSummaryService) {
        this.pdfDocumentParser = pdfDocumentParser;
        this.hwpDocumentParser = hwpDocumentParser;
        this.hwpxDocumentParser = hwpxDocumentParser;
        this.programDocumentRepository = programDocumentRepository;
        this.documentSummaryService = documentSummaryService;
    }

    /**
     * PDF를 파싱하고 추출한 원문을 저장합니다.
     */
    public PdfParseResponse parsePdf(String pblancId) {
        PdfParseResponse response = pdfDocumentParser.parse(pblancId);
        saveOriginalTextAndSummary(pblancId, response.text());
        return response;
    }

    /**
     * HWP를 파싱하고 추출한 원문을 저장합니다.
     */
    public HwpParseResponse parseHwp(String pblancId) {
        HwpParseResponse response = hwpDocumentParser.parse(pblancId);
        saveOriginalTextAndSummary(pblancId, response.text());
        return response;
    }

    /**
     * HWPX를 파싱하고 추출한 원문을 저장합니다.
     */
    public HwpxParseResponse parseHwpx(String pblancId) {
        HwpxParseResponse response = hwpxDocumentParser.parse(pblancId);
        saveOriginalTextAndSummary(pblancId, response.text());
        return response;
    }

    /**
     * DB 오류가 발생하면 파싱 성공으로 잘못 응답하지 않도록 실패를 반환합니다.
     */
    private void saveOriginalTextAndSummary(String pblancId, String originalText) {
        try {
            programDocumentRepository.saveOriginalText(pblancId, originalText);
        } catch (DataAccessException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "파싱 결과를 데이터베이스에 저장할 수 없습니다.",
                    exception);
        }

        documentSummaryService.summarizeIfMissing(pblancId, originalText);
    }
}

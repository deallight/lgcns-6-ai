package com.miniproject1.ai.document;

import com.miniproject1.ai.document.parser.hwp.HwpParseResponse;
import com.miniproject1.ai.document.parser.pdf.PdfParseResponse;
import com.miniproject1.ai.document.parser.hwpx.HwpxParseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공고 첨부파일 다운로드 API입니다.
 */
@Tag(name = "Document", description = "공고 첨부파일 관리 API")
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentParseService documentParseService;

    public DocumentController(
            DocumentService documentService,
            DocumentParseService documentParseService) {
        this.documentService = documentService;
        this.documentParseService = documentParseService;
    }

    /**
     * 공고 ID에 연결된 첨부파일 한 개를 AI 서버에 저장합니다.
     */
    @Operation(summary = "공고 첨부파일 한 개 다운로드")
    @PostMapping("/{pblancId}/download")
    public ResponseEntity<DocumentDownloadResponse> download(
            @Parameter(description = "기업마당 공고 ID", example = "PBLN_000000000126818", required = true)
            @PathVariable("pblancId") String pblancId) {
        return ResponseEntity.ok(documentService.download(pblancId));
    }

    /**
     * 다운로드한 PDF 한 개에서 텍스트를 추출하고 원문과 AI 요약을 저장합니다.
     */
    @Operation(summary = "PDF 한 개 파싱, 원문 및 AI 요약 저장")
    @PostMapping("/{pblancId}/parse/pdf")
    public ResponseEntity<PdfParseResponse> parsePdf(
            @Parameter(description = "기업마당 공고 ID", example = "PBLN_000000000117016", required = true)
            @PathVariable("pblancId") String pblancId) {
        return ResponseEntity.ok(documentParseService.parsePdf(pblancId));
    }

    /**
     * 다운로드한 HWP 한 개에서 본문과 표를 추출하고 원문과 AI 요약을 저장합니다.
     */
    @Operation(summary = "HWP 한 개 파싱, 원문 및 AI 요약 저장")
    @PostMapping("/{pblancId}/parse/hwp")
    public ResponseEntity<HwpParseResponse> parseHwp(
            @Parameter(description = "기업마당 공고 ID", example = "PBLN_000000000118395", required = true)
            @PathVariable("pblancId") String pblancId) {
        return ResponseEntity.ok(documentParseService.parseHwp(pblancId));
    }

    /**
     * 다운로드한 HWPX 한 개에서 문단과 표를 추출하고 원문과 AI 요약을 저장합니다.
     */
    @Operation(summary = "HWPX 한 개 파싱, 원문 및 AI 요약 저장")
    @PostMapping("/{pblancId}/parse/hwpx")
    public ResponseEntity<HwpxParseResponse> parseHwpx(
            @Parameter(description = "기업마당 공고 ID", example = "PBLN_000000000126818", required = true)
            @PathVariable("pblancId") String pblancId) {
        return ResponseEntity.ok(documentParseService.parseHwpx(pblancId));
    }
}

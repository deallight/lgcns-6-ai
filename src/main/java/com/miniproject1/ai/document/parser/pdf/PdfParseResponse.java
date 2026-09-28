package com.miniproject1.ai.document.parser.pdf;

/**
 * PDF 파일에서 추출한 텍스트 결과입니다.
 */
public record PdfParseResponse(
        String pblancId,
        String fileName,
        int pageCount,
        int characterCount,
        String text) {
}

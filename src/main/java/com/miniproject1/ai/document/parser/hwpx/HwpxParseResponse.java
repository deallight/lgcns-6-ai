package com.miniproject1.ai.document.parser.hwpx;

/**
 * HWPX 파일에서 추출한 텍스트 결과입니다.
 */
public record HwpxParseResponse(
        String pblancId,
        String fileName,
        int sectionCount,
        int characterCount,
        String text) {
}

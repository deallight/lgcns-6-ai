package com.miniproject1.ai.program;

/**
 * 공고 단건 조회 결과입니다.
 * 다음 단계에서 첨부파일을 내려받을 수 있도록 파일 URL을 포함합니다.
 */
public record ProgramResponse(
        String pblancId,
        String title,
        String printFilePathUrl) {
}

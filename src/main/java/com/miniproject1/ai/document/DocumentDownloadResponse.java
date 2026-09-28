package com.miniproject1.ai.document;

/**
 * 첨부파일 다운로드 결과입니다.
 */
public record DocumentDownloadResponse(
        String pblancId,
        String fileName,
        String extension,
        String savedPath,
        long fileSize) {
}

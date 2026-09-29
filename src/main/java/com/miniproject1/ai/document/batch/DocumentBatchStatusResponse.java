package com.miniproject1.ai.document.batch;

/**
 * 최대 1500건 일괄 처리의 현재 진행 상태입니다.
 */
public record DocumentBatchStatusResponse(
        String status,
        int limit,
        int total,
        int processed,
        int success,
        int failed,
        String currentPblancId) {
}

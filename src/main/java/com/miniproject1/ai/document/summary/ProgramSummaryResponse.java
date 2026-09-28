package com.miniproject1.ai.document.summary;

import java.util.Optional;

/**
 * 새 ai_summary 테이블의 6개 요약 컬럼과 같은 구조입니다.
 */
public class ProgramSummaryResponse {

    public Optional<String> bizSummary = Optional.empty();
    public Optional<String> targetDescription = Optional.empty();
    public Optional<String> supportContent = Optional.empty();
    public Optional<String> applyMethod = Optional.empty();
    public Optional<String> requiredDocuments = Optional.empty();
    public Optional<String> contactInfo = Optional.empty();
}

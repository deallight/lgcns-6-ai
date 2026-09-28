package com.miniproject1.ai.document.summary;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 새 ai_summary 테이블의 조회와 저장을 담당합니다.
 */
@Repository
public class AiSummaryRepository {

    private static final String HAS_SUMMARY_SQL = """
            SELECT EXISTS(
                SELECT 1
                FROM ai_summary
                WHERE pblanc_id = ?
            )
            """;

    private static final String SAVE_SUMMARY_SQL = """
            INSERT INTO ai_summary (
                pblanc_id,
                biz_summary,
                target_description,
                support_content,
                apply_method,
                required_documents,
                contact_info
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE pblanc_id = ai_summary.pblanc_id
            """;

    private final JdbcTemplate jdbcTemplate;

    public AiSummaryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 같은 공고의 요약이 이미 저장되어 있는지 확인합니다. */
    public boolean existsByPblancId(String pblancId) {
        Boolean exists = jdbcTemplate.queryForObject(
                HAS_SUMMARY_SQL,
                Boolean.class,
                pblancId);
        return Boolean.TRUE.equals(exists);
    }

    /** GPT가 만든 6개 요약 값을 각각 대응하는 컬럼에 저장합니다. */
    public void save(String pblancId, ProgramSummaryResponse summary) {
        jdbcTemplate.update(
                SAVE_SUMMARY_SQL,
                pblancId,
                valueOf(summary.bizSummary),
                valueOf(summary.targetDescription),
                valueOf(summary.supportContent),
                valueOf(summary.applyMethod),
                valueOf(summary.requiredDocuments),
                valueOf(summary.contactInfo));
    }

    /** 공고문에 없는 항목은 빈 문자열 대신 DB의 NULL로 저장합니다. */
    private String valueOf(Optional<String> value) {
        return value == null ? null : value.orElse(null);
    }
}

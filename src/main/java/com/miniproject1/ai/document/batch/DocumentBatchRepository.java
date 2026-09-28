package com.miniproject1.ai.document.batch;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 아직 AI 요약이 없는 공고를 일괄 처리 대상으로 조회합니다.
 */
@Repository
public class DocumentBatchRepository {

    private static final String FIND_PENDING_IDS_SQL = """
            SELECT p.pblanc_id
            FROM programs p
            LEFT JOIN ai_summary a ON a.pblanc_id = p.pblanc_id
            WHERE a.pblanc_id IS NULL
              AND p.print_file_path_url IS NOT NULL
              AND TRIM(p.print_file_path_url) <> ''
            ORDER BY p.pblanc_id
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public DocumentBatchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** AI 요약이 없는 공고 ID를 지정한 개수만큼 조회합니다. */
    public List<String> findPendingPblancIds(int limit) {
        return jdbcTemplate.query(
                FIND_PENDING_IDS_SQL,
                (resultSet, rowNumber) -> resultSet.getString("pblanc_id"),
                limit);
    }
}

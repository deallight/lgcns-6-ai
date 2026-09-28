package com.miniproject1.ai.document;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * AI가 사용하는 공고문 원문 저장 기능입니다.
 */
@Repository
public class ProgramDocumentRepository {

    private static final String SAVE_ORIGINAL_TEXT_SQL = """
            INSERT INTO program_documents (pblanc_id, original_text)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE pblanc_id = program_documents.pblanc_id
            """;

    private static final String FIND_ORIGINAL_TEXT_SQL = """
            SELECT original_text
            FROM program_documents
            WHERE pblanc_id = ?
              AND original_text IS NOT NULL
              AND TRIM(original_text) <> ''
            """;

    private final JdbcTemplate jdbcTemplate;

    public ProgramDocumentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 처음 파싱한 공고문만 추가하고, 같은 공고 ID가 있으면 기존 원문을 유지합니다.
     */
    public void saveOriginalText(String pblancId, String originalText) {
        jdbcTemplate.update(
                SAVE_ORIGINAL_TEXT_SQL,
                pblancId,
                originalText);
    }

    /** 이미 파싱한 원문이 있으면 다시 다운로드하지 않고 재사용합니다. */
    public Optional<String> findOriginalText(String pblancId) {
        List<String> originalTexts = jdbcTemplate.query(
                FIND_ORIGINAL_TEXT_SQL,
                (resultSet, rowNumber) -> resultSet.getString("original_text"),
                pblancId);

        return originalTexts.stream().findFirst();
    }

}

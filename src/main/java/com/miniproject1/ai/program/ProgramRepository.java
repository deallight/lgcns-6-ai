package com.miniproject1.ai.program;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * programs 테이블을 조회하는 클래스입니다.
 */
@Repository
public class ProgramRepository {

    private static final String FIND_BY_ID_SQL = """
            SELECT pblanc_id, title, print_file_path_url
            FROM programs
            WHERE pblanc_id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public ProgramRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 공고 ID가 일치하는 데이터 한 건을 조회합니다.
     */
    public Optional<ProgramResponse> findByPblancId(String pblancId) {
        List<ProgramResponse> programs = jdbcTemplate.query(
                FIND_BY_ID_SQL,
                (resultSet, rowNumber) -> new ProgramResponse(
                        resultSet.getString("pblanc_id"),
                        resultSet.getString("title"),
                        resultSet.getString("print_file_path_url")),
                pblancId);

        return programs.stream().findFirst();
    }
}

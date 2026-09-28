package com.miniproject1.ai.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * MariaDB 연결 상태를 확인하는 API입니다.
 */
@Tag(name = "Health", description = "AI 서버 상태 확인 API")
@RestController
@RequestMapping("/api/health/database")
public class DatabaseHealthController {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 실제 쿼리를 실행해 MariaDB 연결 여부만 확인합니다.
     */
    @Operation(summary = "MariaDB 연결 상태 확인")
    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

            if (Integer.valueOf(1).equals(result)) {
                return ResponseEntity.ok(Map.of(
                        "database", "MariaDB",
                        "status", "UP"));
            }
        } catch (DataAccessException exception) {
            // 접속 정보나 DB 서버에 문제가 있으면 상세 오류 대신 상태만 반환합니다.
        }

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "database", "MariaDB",
                "status", "DOWN"));
    }
}

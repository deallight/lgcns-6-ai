package com.miniproject1.ai.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Health", description = "AI 서버 상태 확인 API")
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Operation(summary = "AI 서버 상태 확인")
    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "ai",
                "status", "UP"));
    }
}


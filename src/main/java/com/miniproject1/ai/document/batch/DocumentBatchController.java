package com.miniproject1.ai.document.batch;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공고문 일괄 파싱 및 요약 API입니다.
 */
@Validated
@Tag(name = "Document Batch", description = "공고문 최대 1500건 일괄 처리 API")
@RestController
@RequestMapping("/api/documents/batch")
public class DocumentBatchController {

    private final DocumentBatchService documentBatchService;

    public DocumentBatchController(DocumentBatchService documentBatchService) {
        this.documentBatchService = documentBatchService;
    }

    /** 처리되지 않은 공고를 최대 1500건까지 백그라운드에서 처리합니다. */
    @Operation(summary = "공고문 최대 1500건 일괄 처리 시작")
    @PostMapping("/start")
    public ResponseEntity<DocumentBatchStatusResponse> start(
            @Parameter(description = "처리할 공고 수", example = "1500")
            @RequestParam(defaultValue = "1500") @Min(1) @Max(1500) int limit) {
        return ResponseEntity.accepted().body(documentBatchService.start(limit));
    }

    /** 실행 중인 일괄 처리의 진행 상태를 확인합니다. */
    @Operation(summary = "공고문 일괄 처리 상태 조회")
    @GetMapping("/status")
    public ResponseEntity<DocumentBatchStatusResponse> status() {
        return ResponseEntity.ok(documentBatchService.getStatus());
    }
}

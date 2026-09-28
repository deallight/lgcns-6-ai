package com.miniproject1.ai.program;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 서버에서 사용할 공고 조회 API입니다.
 */
@Tag(name = "Program", description = "지원사업 공고 조회 API")
@RestController
@RequestMapping("/api/programs")
public class ProgramController {

    private final ProgramService programService;

    public ProgramController(ProgramService programService) {
        this.programService = programService;
    }

    /**
     * 공고 ID로 공고 한 건을 조회합니다.
     */
    @Operation(summary = "지원사업 공고 단건 조회")
    @GetMapping("/{pblancId}")
    public ResponseEntity<ProgramResponse> getProgram(
            @Parameter(description = "기업마당 공고 ID", example = "PBLN_000000000126818", required = true)
            @PathVariable("pblancId") String pblancId) {
        return programService.findByPblancId(pblancId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

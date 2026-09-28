package com.miniproject1.ai.document.batch;

import com.miniproject1.ai.document.DocumentDownloadResponse;
import com.miniproject1.ai.document.DocumentParseService;
import com.miniproject1.ai.document.DocumentService;
import com.miniproject1.ai.document.ProgramDocumentRepository;
import com.miniproject1.ai.document.summary.DocumentSummaryService;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 공고 다운로드, 형식별 파싱, AI 요약 저장을 순서대로 실행합니다.
 */
@Service
public class DocumentBatchService {

    private static final int MAX_BATCH_LIMIT = 1554;

    private final DocumentBatchRepository documentBatchRepository;
    private final ProgramDocumentRepository programDocumentRepository;
    private final DocumentService documentService;
    private final DocumentParseService documentParseService;
    private final DocumentSummaryService documentSummaryService;
    private final TaskExecutor taskExecutor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile DocumentBatchStatusResponse status =
            new DocumentBatchStatusResponse("IDLE", 0, 0, 0, 0, 0, null);

    public DocumentBatchService(
            DocumentBatchRepository documentBatchRepository,
            ProgramDocumentRepository programDocumentRepository,
            DocumentService documentService,
            DocumentParseService documentParseService,
            DocumentSummaryService documentSummaryService,
            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.documentBatchRepository = documentBatchRepository;
        this.programDocumentRepository = programDocumentRepository;
        this.documentService = documentService;
        this.documentParseService = documentParseService;
        this.documentSummaryService = documentSummaryService;
        this.taskExecutor = taskExecutor;
    }

    /** 최대 1554건을 조회한 뒤 백그라운드에서 한 건씩 처리합니다. */
    public DocumentBatchStatusResponse start(int limit) {
        validateLimit(limit);
        if (!running.compareAndSet(false, true)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 일괄 처리가 실행 중입니다.");
        }

        try {
            List<String> pblancIds = documentBatchRepository.findPendingPblancIds(limit);
            status = new DocumentBatchStatusResponse(
                    "RUNNING", limit, pblancIds.size(), 0, 0, 0, null);

            taskExecutor.execute(() -> process(pblancIds, limit));
            return status;
        } catch (RuntimeException exception) {
            running.set(false);
            status = new DocumentBatchStatusResponse("FAILED", limit, 0, 0, 0, 0, null);
            throw exception;
        }
    }

    /** 현재 메모리에 기록된 진행 상태를 반환합니다. */
    public DocumentBatchStatusResponse getStatus() {
        return status;
    }

    /** 한 건이 실패해도 다음 공고 처리를 계속합니다. */
    private void process(List<String> pblancIds, int limit) {
        int processed = 0;
        int success = 0;
        int failed = 0;

        try {
            for (String pblancId : pblancIds) {
                status = new DocumentBatchStatusResponse(
                        "RUNNING", limit, pblancIds.size(), processed, success, failed, pblancId);

                try {
                    processOne(pblancId);
                    success++;
                } catch (RuntimeException exception) {
                    failed++;
                }

                processed++;
                status = new DocumentBatchStatusResponse(
                        "RUNNING", limit, pblancIds.size(), processed, success, failed, null);
            }

            status = new DocumentBatchStatusResponse(
                    "COMPLETED", limit, pblancIds.size(), processed, success, failed, null);
        } finally {
            running.set(false);
        }
    }

    /** 기존 원문이 없을 때만 다운로드하고 확장자에 맞는 파서를 호출합니다. */
    private void processOne(String pblancId) {
        Optional<String> originalText = programDocumentRepository.findOriginalText(pblancId);
        if (originalText.isPresent()) {
            documentSummaryService.summarizeIfMissing(pblancId, originalText.get());
            return;
        }

        DocumentDownloadResponse download = documentService.download(pblancId);
        switch (download.extension()) {
            case "pdf" -> documentParseService.parsePdf(pblancId);
            case "hwp" -> documentParseService.parseHwp(pblancId);
            case "hwpx" -> documentParseService.parseHwpx(pblancId);
            default -> throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "지원하지 않는 첨부파일 형식입니다.");
        }
    }

    /** 현재 DB의 전체 공고 수인 1554건을 넘지 못하도록 제한합니다. */
    private void validateLimit(int limit) {
        if (limit < 1 || limit > MAX_BATCH_LIMIT) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "limit은 1 이상 1554 이하만 사용할 수 있습니다.");
        }
    }
}

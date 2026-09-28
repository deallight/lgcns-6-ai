package com.miniproject1.ai.document.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.miniproject1.ai.document.DocumentDownloadResponse;
import com.miniproject1.ai.document.DocumentParseService;
import com.miniproject1.ai.document.DocumentService;
import com.miniproject1.ai.document.ProgramDocumentRepository;
import com.miniproject1.ai.document.summary.DocumentSummaryService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;

@ExtendWith(MockitoExtension.class)
class DocumentBatchServiceTest {

    @Mock
    private DocumentBatchRepository documentBatchRepository;

    @Mock
    private ProgramDocumentRepository programDocumentRepository;

    @Mock
    private DocumentService documentService;

    @Mock
    private DocumentParseService documentParseService;

    @Mock
    private DocumentSummaryService documentSummaryService;

    private DocumentBatchService documentBatchService;

    @BeforeEach
    void setUp() {
        // 테스트에서는 별도 스레드 없이 즉시 실행하여 결과를 확인합니다.
        TaskExecutor directExecutor = Runnable::run;
        documentBatchService = new DocumentBatchService(
                documentBatchRepository,
                programDocumentRepository,
                documentService,
                documentParseService,
                documentSummaryService,
                directExecutor);
    }

    @Test
    void routesDownloadedFilesToMatchingParser() {
        List<String> pblancIds = List.of("PBLN_PDF", "PBLN_HWP", "PBLN_HWPX");
        when(documentBatchRepository.findPendingPblancIds(5)).thenReturn(pblancIds);

        for (String pblancId : pblancIds) {
            when(programDocumentRepository.findOriginalText(pblancId)).thenReturn(Optional.empty());
        }
        when(documentService.download("PBLN_PDF"))
                .thenReturn(downloadResponse("PBLN_PDF", "pdf"));
        when(documentService.download("PBLN_HWP"))
                .thenReturn(downloadResponse("PBLN_HWP", "hwp"));
        when(documentService.download("PBLN_HWPX"))
                .thenReturn(downloadResponse("PBLN_HWPX", "hwpx"));

        documentBatchService.start(5);

        verify(documentParseService).parsePdf("PBLN_PDF");
        verify(documentParseService).parseHwp("PBLN_HWP");
        verify(documentParseService).parseHwpx("PBLN_HWPX");
        assertThat(documentBatchService.getStatus().status()).isEqualTo("COMPLETED");
        assertThat(documentBatchService.getStatus().success()).isEqualTo(3);
        assertThat(documentBatchService.getStatus().failed()).isZero();
    }

    @Test
    void reusesOriginalTextWithoutDownloadingAgain() {
        String pblancId = "PBLN_PARSED";
        when(documentBatchRepository.findPendingPblancIds(5)).thenReturn(List.of(pblancId));
        when(programDocumentRepository.findOriginalText(pblancId))
                .thenReturn(Optional.of("이미 파싱한 공고문"));

        documentBatchService.start(5);

        verify(documentSummaryService).summarizeIfMissing(pblancId, "이미 파싱한 공고문");
        verify(documentService, never()).download(pblancId);
        assertThat(documentBatchService.getStatus().success()).isEqualTo(1);
    }

    private DocumentDownloadResponse downloadResponse(String pblancId, String extension) {
        return new DocumentDownloadResponse(
                pblancId,
                "공고문." + extension,
                extension,
                "downloads/" + extension + "/" + pblancId + "/공고문." + extension,
                100L);
    }
}

package com.miniproject1.ai.document.summary;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiSummaryRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AiSummaryRepository aiSummaryRepository;

    @Test
    void saveMapsSixSummaryColumns() {
        ProgramSummaryResponse summary = new ProgramSummaryResponse();
        summary.bizSummary = Optional.of("사업개요");
        summary.targetDescription = Optional.of("지원대상");
        summary.supportContent = Optional.of("지원내용");
        summary.applyMethod = Optional.of("신청방법");
        summary.requiredDocuments = Optional.of("준비서류");
        summary.contactInfo = Optional.empty();

        aiSummaryRepository.save("PBLN_TEST", summary);

        // 6개 요약값이 정해진 순서로 저장되고, 없는 문의처는 NULL이 됩니다.
        verify(jdbcTemplate).update(
                anyString(),
                eq("PBLN_TEST"),
                eq("사업개요"),
                eq("지원대상"),
                eq("지원내용"),
                eq("신청방법"),
                eq("준비서류"),
                isNull());
    }
}

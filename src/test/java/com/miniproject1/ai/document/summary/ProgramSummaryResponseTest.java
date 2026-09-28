package com.miniproject1.ai.document.summary;

import com.openai.models.responses.ResponseCreateParams;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ProgramSummaryResponseTest {

    @Test
    void structuredOutputSchemaCanBeBuilt() {
        // API를 호출하지 않고 Java DTO에서 strict JSON 스키마가 생성되는지만 확인합니다.
        assertDoesNotThrow(() -> ResponseCreateParams.builder()
                .model("gpt-4o-mini")
                .input("테스트 공고문")
                .text(ProgramSummaryResponse.class)
                .build());
    }
}

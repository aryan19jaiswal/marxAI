package com.marxAI.model.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Unit tests for {@link AgentIntent#toDocumentType()} mapping.
 * Verifies that domain-specific intents map to the correct {@link DocumentType} and that
 * cross-cutting intents return empty, falling back to unscoped Qdrant retrieval.
 */
class AgentIntentTest {

    @Test
    void dsa_mapsTo_documentTypeDsa() {
        assertThat(AgentIntent.DSA.toDocumentType()).contains(DocumentType.DSA);
    }

    @Test
    void systemDesign_mapsTo_documentTypeSystemDesign() {
        assertThat(AgentIntent.SYSTEM_DESIGN.toDocumentType()).contains(DocumentType.SYSTEM_DESIGN);
    }

    @Test
    void resume_mapsTo_documentTypeResume() {
        assertThat(AgentIntent.RESUME.toDocumentType()).contains(DocumentType.RESUME);
    }

    @ParameterizedTest(name = "{0} has no dedicated document type")
    @EnumSource(value = AgentIntent.class, names = {"MOCK_INTERVIEW", "STUDY_PLAN", "GENERAL"})
    void crossCuttingIntents_returnEmpty(AgentIntent intent) {
        Optional<DocumentType> result = intent.toDocumentType();
        assertThat(result).isEmpty();
    }

    @Test
    void allDocumentTypesAreCoveredByAtLeastOneIntent() {
        // Guard: if a new DocumentType is added but AgentIntent.toDocumentType() isn't updated,
        // this test catches the gap by ensuring all DocumentType values appear in at least one mapping.
        long mappedCount = java.util.Arrays.stream(AgentIntent.values())
                .map(AgentIntent::toDocumentType)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .count();
        assertThat(mappedCount).isEqualTo(DocumentType.values().length);
    }
}

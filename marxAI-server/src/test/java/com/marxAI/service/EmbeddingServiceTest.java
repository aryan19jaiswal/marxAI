package com.marxAI.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link EmbeddingService}, with the OpenAI {@link EmbeddingModel} mocked. */
@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingService(embeddingModel);
    }

    @Test
    void embed_returnsModelsEmbedding_forGivenText() {
        Embedding embedding = Embedding.from(new float[] {0.1f, 0.2f, 0.3f});
        when(embeddingModel.embed("binary search")).thenReturn(Response.from(embedding));

        Embedding result = embeddingService.embed("binary search");

        assertThat(result).isSameAs(embedding);
    }

    @Test
    void embedAll_returnsEmptyList_withoutCallingModel_whenSegmentsEmpty() {
        List<Embedding> result = embeddingService.embedAll(List.of());

        assertThat(result).isEmpty();
        verify(embeddingModel, never()).embedAll(anyList());
    }

    @Test
    void embedAll_returnsOneEmbeddingPerSegment_inOrder() {
        List<TextSegment> segments = List.of(TextSegment.from("alpha"), TextSegment.from("beta"));
        Embedding first = Embedding.from(new float[] {1f, 0f});
        Embedding second = Embedding.from(new float[] {0f, 1f});
        when(embeddingModel.embedAll(segments)).thenReturn(Response.from(List.of(first, second)));

        List<Embedding> result = embeddingService.embedAll(segments);

        assertThat(result).containsExactly(first, second);
    }
}

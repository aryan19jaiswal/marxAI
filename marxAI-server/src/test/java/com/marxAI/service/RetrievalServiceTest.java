package com.marxAI.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marxAI.model.dto.AssembledContext;
import com.marxAI.model.enums.DocumentType;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link RetrievalService}, with {@link QdrantService} and
 * {@link ContextAssembler} mocked to verify filter construction and delegation behaviour.
 */
@ExtendWith(MockitoExtension.class)
class RetrievalServiceTest {

    @Mock
    private QdrantService qdrantService;

    @Mock
    private ContextAssembler contextAssembler;

    private RetrievalService retrievalService;

    @BeforeEach
    void setUp() {
        retrievalService = new RetrievalService(qdrantService, contextAssembler);
    }

    private static EmbeddingMatch<TextSegment> match(String text) {
        return new EmbeddingMatch<>(0.9, "id", Embedding.from(new float[]{1f}),
                TextSegment.from(text));
    }

    @Test
    void retrieve_queryOnly_usesDefaultTopKAndNullFilter() {
        List<EmbeddingMatch<TextSegment>> matches = List.of(match("binary search is O(log n)"));
        AssembledContext expected = new AssembledContext(
                "### Source 1:\nbinary search is O(log n)\n", 1);
        when(qdrantService.similaritySearch(
                eq("binary search"), eq(RetrievalService.DEFAULT_TOP_K), isNull()))
                .thenReturn(matches);
        when(contextAssembler.assemble(matches)).thenReturn(expected);

        AssembledContext result = retrievalService.retrieve("binary search");

        assertThat(result).isSameAs(expected);
        verify(qdrantService)
                .similaritySearch("binary search", RetrievalService.DEFAULT_TOP_K, null);
    }

    @Test
    void retrieve_withDocType_appliesDsaFilter() {
        Filter expectedFilter = QdrantService.byDocumentType(DocumentType.DSA);
        when(qdrantService.similaritySearch(eq("binary search"),
                eq(RetrievalService.DEFAULT_TOP_K), any()))
                .thenReturn(List.of());
        when(contextAssembler.assemble(List.of())).thenReturn(AssembledContext.empty());

        retrievalService.retrieve("binary search", DocumentType.DSA);

        ArgumentCaptor<Filter> filterCaptor = ArgumentCaptor.forClass(Filter.class);
        verify(qdrantService).similaritySearch(
                eq("binary search"), eq(RetrievalService.DEFAULT_TOP_K), filterCaptor.capture());
        assertThat(filterCaptor.getValue()).isEqualTo(expectedFilter);
    }

    @Test
    void retrieve_withSystemDesignDocType_appliesSystemDesignFilter() {
        when(qdrantService.similaritySearch(any(), any(Integer.class), any()))
                .thenReturn(List.of());
        when(contextAssembler.assemble(any())).thenReturn(AssembledContext.empty());

        retrievalService.retrieve("design a URL shortener", DocumentType.SYSTEM_DESIGN);

        ArgumentCaptor<Filter> filterCaptor = ArgumentCaptor.forClass(Filter.class);
        verify(qdrantService).similaritySearch(any(), any(Integer.class), filterCaptor.capture());
        assertThat(filterCaptor.getValue())
                .isEqualTo(new IsEqualTo("docType", "SYSTEM_DESIGN"));
    }

    @Test
    void retrieve_withNullDocType_passesNullFilter() {
        when(qdrantService.similaritySearch(any(), any(Integer.class), isNull()))
                .thenReturn(List.of());
        when(contextAssembler.assemble(any())).thenReturn(AssembledContext.empty());

        retrievalService.retrieve("some query", (DocumentType) null);

        verify(qdrantService).similaritySearch(any(), any(Integer.class), isNull());
    }

    @Test
    void retrieve_fullSignature_forwardsExplicitTopKAndDocTypeFilter() {
        when(qdrantService.similaritySearch(eq("heap sort"), eq(3), any()))
                .thenReturn(List.of());
        when(contextAssembler.assemble(any())).thenReturn(AssembledContext.empty());

        retrievalService.retrieve("heap sort", 3, DocumentType.DSA);

        verify(qdrantService).similaritySearch(eq("heap sort"), eq(3), any(Filter.class));
    }

    @Test
    void retrieve_passesQdrantMatchesDirectlyToContextAssembler() {
        List<EmbeddingMatch<TextSegment>> matches = List.of(match("quick sort partitions input"));
        AssembledContext expected = new AssembledContext("### Source 1:\nquick sort...\n", 1);
        when(qdrantService.similaritySearch(any(), any(Integer.class), any()))
                .thenReturn(matches);
        when(contextAssembler.assemble(matches)).thenReturn(expected);

        AssembledContext result = retrievalService.retrieve("quick sort", 5, DocumentType.DSA);

        verify(contextAssembler).assemble(matches);
        assertThat(result).isSameAs(expected);
    }

    @Test
    void retrieve_returnsAssemblerResult_evenWhenNoMatchesFound() {
        when(qdrantService.similaritySearch(any(), any(Integer.class), any()))
                .thenReturn(List.of());
        when(contextAssembler.assemble(List.of())).thenReturn(AssembledContext.empty());

        AssembledContext result = retrievalService.retrieve("unknown topic", DocumentType.RESUME);

        assertThat(result.context()).isEmpty();
        assertThat(result.sourceCount()).isZero();
    }
}

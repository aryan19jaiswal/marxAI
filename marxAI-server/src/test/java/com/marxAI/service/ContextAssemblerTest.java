package com.marxAI.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marxAI.model.dto.AssembledContext;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ContextAssembler}'s deduplication, source formatting, and
 * character-limit truncation logic.
 */
class ContextAssemblerTest {

    private final ContextAssembler assembler = new ContextAssembler();

    private static EmbeddingMatch<TextSegment> match(String text) {
        return new EmbeddingMatch<>(0.9, "id-" + text.hashCode(), Embedding.from(new float[]{1f}),
                TextSegment.from(text));
    }

    @Test
    void assemble_returnsEmptyContext_whenMatchesListIsEmpty() {
        AssembledContext result = assembler.assemble(List.of());

        assertThat(result.context()).isEmpty();
        assertThat(result.sourceCount()).isZero();
    }

    @Test
    void assemble_formatsSingleMatch_withCorrectHeaderAndTrailingNewline() {
        AssembledContext result = assembler.assemble(
                List.of(match("Binary search runs in O(log n) time.")));

        assertThat(result.context())
                .isEqualTo("### Source 1:\nBinary search runs in O(log n) time.\n");
        assertThat(result.sourceCount()).isEqualTo(1);
    }

    @Test
    void assemble_numbersSourcesSequentially_forMultipleUniqueMatches() {
        AssembledContext result = assembler.assemble(List.of(
                match("Binary search halves the search space each step."),
                match("Merge sort is O(n log n) stable sort.")));

        assertThat(result.context()).contains("### Source 1:");
        assertThat(result.context()).contains("### Source 2:");
        assertThat(result.context()).contains("Binary search halves");
        assertThat(result.context()).contains("Merge sort is");
        assertThat(result.sourceCount()).isEqualTo(2);
    }

    @Test
    void assemble_deduplicatesIdenticalChunks_keepingHigherScoredOccurrence() {
        String text = "Binary search is O(log n).";
        EmbeddingMatch<TextSegment> first = new EmbeddingMatch<>(0.95, "id-1",
                Embedding.from(new float[]{1f}), TextSegment.from(text));
        EmbeddingMatch<TextSegment> duplicate = new EmbeddingMatch<>(0.80, "id-2",
                Embedding.from(new float[]{1f}), TextSegment.from(text));

        AssembledContext result = assembler.assemble(List.of(first, duplicate));

        assertThat(result.sourceCount()).isEqualTo(1);
        assertThat(result.context()).startsWith("### Source 1:\nBinary search is O(log n).");
        assertThat(result.context()).doesNotContain("### Source 2:");
    }

    @Test
    void assemble_deduplicatesBasedOnTextContent_notOnMatchId() {
        TextSegment segment = TextSegment.from("shared chunk content");
        EmbeddingMatch<TextSegment> a = new EmbeddingMatch<>(0.9, "id-a",
                Embedding.from(new float[]{1f}), segment);
        EmbeddingMatch<TextSegment> b = new EmbeddingMatch<>(0.8, "id-b",
                Embedding.from(new float[]{2f}), segment);

        AssembledContext result = assembler.assemble(List.of(a, b));

        assertThat(result.sourceCount()).isEqualTo(1);
    }

    @Test
    void assemble_keepsAllThreeUnique_whenNoTextIsShared() {
        AssembledContext result = assembler.assemble(List.of(
                match("Heap sort uses a max-heap."),
                match("Quick sort partitions around a pivot."),
                match("Bubble sort swaps adjacent elements.")));

        assertThat(result.sourceCount()).isEqualTo(3);
        assertThat(result.context()).contains("### Source 3:");
    }

    @Test
    void assemble_stopsBeforeChunkThatWouldExceedMaxContextChars() {
        // Two chunks each ~70% of MAX_CONTEXT_CHARS: combined they exceed the limit
        int size = (int) (ContextAssembler.MAX_CONTEXT_CHARS * 0.70);
        String text1 = "a".repeat(size);
        String text2 = "b".repeat(size);

        AssembledContext result = assembler.assemble(List.of(match(text1), match(text2)));

        assertThat(result.sourceCount()).isEqualTo(1);
        assertThat(result.context().length()).isLessThanOrEqualTo(ContextAssembler.MAX_CONTEXT_CHARS);
    }

    @Test
    void assemble_includesAllSources_whenTotalLengthStaysUnderLimit() {
        // Short chunks — all three should fit easily
        AssembledContext result = assembler.assemble(List.of(
                match("DSA chunk one about arrays."),
                match("DSA chunk two about trees."),
                match("DSA chunk three about graphs.")));

        assertThat(result.sourceCount()).isEqualTo(3);
        assertThat(result.context().length()).isLessThan(ContextAssembler.MAX_CONTEXT_CHARS);
    }

    @Test
    void assemble_returnsZeroSources_whenSingleChunkAloneExceedsMaxContextChars() {
        // A chunk whose formatted block is longer than MAX_CONTEXT_CHARS cannot be included
        String oversized = "z".repeat(ContextAssembler.MAX_CONTEXT_CHARS + 1);

        AssembledContext result = assembler.assemble(List.of(match(oversized)));

        assertThat(result.sourceCount()).isZero();
        assertThat(result.context()).isEmpty();
    }

    @Test
    void assemble_contextNeverExceedsMaxContextChars_withManyLargeChunks() {
        String largeChunk = "w".repeat(3_000);
        List<EmbeddingMatch<TextSegment>> many = List.of(
                match(largeChunk + "1"),
                match(largeChunk + "2"),
                match(largeChunk + "3"),
                match(largeChunk + "4"),
                match(largeChunk + "5"));

        AssembledContext result = assembler.assemble(many);

        assertThat(result.context().length()).isLessThanOrEqualTo(ContextAssembler.MAX_CONTEXT_CHARS);
    }
}

package com.marxAI.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import com.marxAI.model.chunking.ParsedDocument;
import com.marxAI.model.chunking.TextChunk;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ChunkingService}'s token-aware splitting, focused on edge cases. */
class ChunkingServiceTest {

    private final ChunkingService chunkingService = new ChunkingService();
    private final Encoding encoding =
            Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);

    @Test
    void chunk_returnsEmptyList_whenDocumentHasNoPages() {
        List<TextChunk> chunks = chunkingService.chunk(new ParsedDocument("", List.of()));

        assertThat(chunks).isEmpty();
    }

    @Test
    void chunk_returnsEmptyList_whenAllPagesAreBlank() {
        ParsedDocument document = new ParsedDocument("", List.of("", "   ", "\n\t"));

        List<TextChunk> chunks = chunkingService.chunk(document);

        assertThat(chunks).isEmpty();
    }

    @Test
    void chunk_returnsSingleChunk_whenDocumentIsShorterThanChunkSize() {
        String text = "Binary search runs in O(log n) time on a sorted array.";
        ParsedDocument document = new ParsedDocument(text, List.of(text));

        List<TextChunk> chunks = chunkingService.chunk(document);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).chunkIndex()).isZero();
        assertThat(chunks.get(0).pageNumber()).isEqualTo(1);
        assertThat(chunks.get(0).text()).isEqualTo(text);
    }

    @Test
    void chunk_skipsBlankPages_butKeepsTrueSourcePageNumberForNonBlankPages() {
        ParsedDocument document =
                new ParsedDocument("", List.of("Page one content.", "", "Page three content."));

        List<TextChunk> chunks = chunkingService.chunk(document);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).pageNumber()).isEqualTo(1);
        assertThat(chunks.get(0).chunkIndex()).isZero();
        assertThat(chunks.get(0).text()).isEqualTo("Page one content.");
        assertThat(chunks.get(1).pageNumber()).isEqualTo(3);
        assertThat(chunks.get(1).chunkIndex()).isEqualTo(1);
        assertThat(chunks.get(1).text()).isEqualTo("Page three content.");
    }

    @Test
    void chunk_assignsContinuousChunkIndex_acrossMultiplePages() {
        String longPage = repeatWords("alpha bravo charlie delta echo foxtrot golf hotel ", 60);
        ParsedDocument document = new ParsedDocument("", List.of(longPage, longPage));

        List<TextChunk> chunks = chunkingService.chunk(document);

        assertThat(chunks).isNotEmpty();
        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).chunkIndex()).isEqualTo(i);
        }
        long page1Chunks = chunks.stream().filter(c -> c.pageNumber() == 1).count();
        long page2Chunks = chunks.stream().filter(c -> c.pageNumber() == 2).count();
        assertThat(page1Chunks).isEqualTo(page2Chunks).isGreaterThan(0);
    }

    @Test
    void chunk_splitsLongPageIntoMultipleChunks_eachAtMostChunkSizeTokens() {
        String longText = repeatWords("the quick brown fox jumps over the lazy dog. ", 200);
        ParsedDocument document = new ParsedDocument(longText, List.of(longText));
        int totalTokens = encoding.countTokensOrdinary(longText);
        assertThat(totalTokens).isGreaterThan(ChunkingService.CHUNK_SIZE_TOKENS);

        List<TextChunk> chunks = chunkingService.chunk(document);

        assertThat(chunks.size()).isGreaterThan(1);
        assertThat(tokenCount(chunks.get(0).text())).isEqualTo(ChunkingService.CHUNK_SIZE_TOKENS);
        TextChunk last = chunks.get(chunks.size() - 1);
        assertThat(tokenCount(last.text())).isBetween(1, ChunkingService.CHUNK_SIZE_TOKENS);
        assertThat(chunks).allSatisfy(c -> assertThat(c.pageNumber()).isEqualTo(1));
    }

    @Test
    void chunk_overlapsConsecutiveChunksByOverlapTokens_withinTheSamePage() {
        String longText = repeatWords("the quick brown fox jumps over the lazy dog. ", 200);
        ParsedDocument document = new ParsedDocument(longText, List.of(longText));

        List<TextChunk> chunks = chunkingService.chunk(document);
        assertThat(chunks.size()).isGreaterThanOrEqualTo(2);

        TextChunk first = chunks.get(0);
        TextChunk second = chunks.get(1);
        String overlapText = lastNTokensAsText(first.text(), ChunkingService.OVERLAP_TOKENS);

        assertThat(first.text()).endsWith(overlapText);
        assertThat(second.text()).startsWith(overlapText);
    }

    private int tokenCount(String text) {
        return encoding.countTokensOrdinary(text);
    }

    private String lastNTokensAsText(String text, int n) {
        IntArrayList tokens = encoding.encodeOrdinary(text);
        IntArrayList tail = new IntArrayList(n);
        for (int i = tokens.size() - n; i < tokens.size(); i++) {
            tail.add(tokens.get(i));
        }
        return encoding.decode(tail);
    }

    private String repeatWords(String phrase, int times) {
        return phrase.repeat(times);
    }
}

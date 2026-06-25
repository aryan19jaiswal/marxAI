package com.marxAI.service;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import com.marxAI.model.chunking.ParsedDocument;
import com.marxAI.model.chunking.TextChunk;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Splits a {@link ParsedDocument} into token-bounded {@link TextChunk}s ready for embedding.
 * Chunks are sized against the {@code cl100k_base} encoding (the tokenizer used by OpenAI's
 * {@code text-embedding-3-small}, the model {@code EmbeddingService} calls), so a chunk's token
 * count maps directly to what the embedding model will actually see. Splitting happens per page so
 * a chunk never straddles two PDF pages, and consecutive chunks within a page overlap by
 * {@value #OVERLAP_TOKENS} tokens so context isn't lost at a chunk boundary.
 */
@Service
public class ChunkingService {

    static final int CHUNK_SIZE_TOKENS = 512;
    static final int OVERLAP_TOKENS = 50;

    private final Encoding encoding;

    public ChunkingService() {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        this.encoding = registry.getEncoding(EncodingType.CL100K_BASE);
    }

    /**
     * @param document parsed text to chunk, page by page
     * @return chunks in reading order, with a document-wide {@code chunkIndex} and the 1-indexed
     *     {@code pageNumber} each chunk's text came from; empty if every page is blank
     */
    public List<TextChunk> chunk(ParsedDocument document) {
        List<TextChunk> chunks = new ArrayList<>();
        int chunkIndex = 0;
        List<String> pages = document.pages();
        for (int i = 0; i < pages.size(); i++) {
            String pageText = pages.get(i);
            if (pageText.isBlank()) {
                continue;
            }
            int pageNumber = i + 1;
            IntArrayList tokens = encoding.encodeOrdinary(pageText);
            int start = 0;
            while (start < tokens.size()) {
                int end = Math.min(start + CHUNK_SIZE_TOKENS, tokens.size());
                chunks.add(new TextChunk(chunkIndex++, pageNumber, decodeRange(tokens, start, end)));
                if (end == tokens.size()) {
                    break;
                }
                start = end - OVERLAP_TOKENS;
            }
        }
        return chunks;
    }

    private String decodeRange(IntArrayList tokens, int start, int end) {
        IntArrayList slice = new IntArrayList(end - start);
        for (int i = start; i < end; i++) {
            slice.add(tokens.get(i));
        }
        return encoding.decode(slice);
    }
}

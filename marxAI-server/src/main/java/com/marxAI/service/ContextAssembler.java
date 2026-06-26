package com.marxAI.service;

import com.marxAI.model.dto.AssembledContext;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Assembles a ranked list of {@link EmbeddingMatch} results from Qdrant into a single,
 * prompt-ready context string.
 *
 * <p>Assembly pipeline:
 * <ol>
 *   <li><b>Deduplicate</b> — matches whose text hashes to the same SHA-256 digest are dropped,
 *       keeping the first (highest-score) occurrence. This prevents overlapping chunks from
 *       inflating the context with repeated text.</li>
 *   <li><b>Format</b> — each unique match is rendered as
 *       {@code ### Source N:\n{chunk text}\n} where N is its 1-based position in the
 *       deduplicated list.</li>
 *   <li><b>Truncate</b> — formatted blocks are appended until adding the next would push the
 *       total past {@value #MAX_CONTEXT_CHARS} characters; sources beyond the limit are
 *       dropped to keep the assembled context within the LLM context window.</li>
 * </ol>
 */
@Service
public class ContextAssembler {

    /** Maximum character length of the assembled context string. */
    static final int MAX_CONTEXT_CHARS = 12_000;

    /**
     * Assembles {@code matches} into a deduplicated, truncated context string.
     *
     * @param matches ranked matches from a Qdrant similarity search, highest-score first
     * @return assembled context; {@link AssembledContext#empty()} if {@code matches} is empty
     */
    public AssembledContext assemble(List<EmbeddingMatch<TextSegment>> matches) {
        if (matches.isEmpty()) {
            return AssembledContext.empty();
        }

        List<EmbeddingMatch<TextSegment>> unique = deduplicate(matches);
        StringBuilder sb = new StringBuilder();
        int sourceCount = 0;

        for (EmbeddingMatch<TextSegment> match : unique) {
            String block = "### Source " + (sourceCount + 1) + ":\n" + match.embedded().text() + "\n";
            if (sb.length() + block.length() > MAX_CONTEXT_CHARS) {
                break;
            }
            sb.append(block);
            sourceCount++;
        }

        return new AssembledContext(sb.toString(), sourceCount);
    }

    private List<EmbeddingMatch<TextSegment>> deduplicate(List<EmbeddingMatch<TextSegment>> matches) {
        Set<String> seen = new LinkedHashSet<>();
        List<EmbeddingMatch<TextSegment>> unique = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : matches) {
            if (seen.add(sha256(match.embedded().text()))) {
                unique.add(match);
            }
        }
        return unique;
    }

    private static String sha256(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the JVM spec; this branch is unreachable in practice
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}

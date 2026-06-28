package com.marxAI.agent.tool;

import com.marxAI.model.dto.AssembledContext;
import com.marxAI.model.enums.DocumentType;
import com.marxAI.service.RetrievalService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * LangChain4J tool that gives the {@link com.marxAI.agent.DsaAgent} access to the
 * user's personal DSA study notes via semantic search in Qdrant.
 *
 * <p>The agent calls this before explaining a concept so that its response draws on
 * the user's own uploaded materials rather than generic knowledge alone.
 */
@Component
@RequiredArgsConstructor
public class NoteSearchTool {

    private final RetrievalService retrievalService;

    /**
     * Searches the user's uploaded DSA notes for content related to {@code topic}.
     *
     * @param topic DSA concept or problem type to search for (e.g. "binary search", "DP on trees")
     * @return formatted note excerpts ready for the agent to reference, or a "no notes found" string
     */
    @Tool("Search the user's personal DSA study notes for a topic. "
            + "Always call this before explaining a DSA concept to see if the user has their own notes on it.")
    public String searchNotes(
            @P("the DSA concept or problem type to search for, e.g. 'binary search' or 'dynamic programming'")
            String topic) {
        AssembledContext ctx = retrievalService.retrieve(topic, DocumentType.DSA);
        if (ctx.sourceCount() == 0) {
            return "No DSA notes found for topic: " + topic;
        }
        return ctx.context();
    }
}

package com.marxAI.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marxAI.model.dto.AssembledContext;
import com.marxAI.model.enums.DocumentType;
import com.marxAI.service.RetrievalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link NoteSearchTool} with a mocked {@link RetrievalService}.
 *
 * <p>Tests verify:
 * <ul>
 *   <li>The tool delegates to {@code RetrievalService.retrieve(topic, DSA)}.</li>
 *   <li>When notes are found, the assembled context string is returned.</li>
 *   <li>When no notes are found (sourceCount == 0), a "no notes" message is returned.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class NoteSearchToolTest {

    @Mock
    private RetrievalService retrievalService;

    private NoteSearchTool tool;

    @BeforeEach
    void setUp() {
        tool = new NoteSearchTool(retrievalService);
    }

    @Test
    void searchNotes_withResults_returnsAssembledContext() {
        String contextText = "### Source 1:\nBinary search requires a sorted array...\n";
        when(retrievalService.retrieve("binary search", DocumentType.DSA))
                .thenReturn(new AssembledContext(contextText, 1));

        String result = tool.searchNotes("binary search");

        assertThat(result).isEqualTo(contextText);
    }

    @Test
    void searchNotes_withMultipleSources_returnsAllSourcesInContext() {
        String multiSource = "### Source 1:\nnote one\n### Source 2:\nnote two\n";
        when(retrievalService.retrieve("dynamic programming", DocumentType.DSA))
                .thenReturn(new AssembledContext(multiSource, 2));

        String result = tool.searchNotes("dynamic programming");

        assertThat(result).contains("Source 1");
        assertThat(result).contains("Source 2");
    }

    @Test
    void searchNotes_noResults_returnsNoNotesMessage() {
        when(retrievalService.retrieve("suffix array", DocumentType.DSA))
                .thenReturn(AssembledContext.empty());

        String result = tool.searchNotes("suffix array");

        assertThat(result).contains("No DSA notes found");
        assertThat(result).contains("suffix array");
    }

    @Test
    void searchNotes_alwaysScopesRetrievalToDsaDocType() {
        when(retrievalService.retrieve("heap", DocumentType.DSA))
                .thenReturn(AssembledContext.empty());

        tool.searchNotes("heap");

        verify(retrievalService).retrieve("heap", DocumentType.DSA);
    }
}

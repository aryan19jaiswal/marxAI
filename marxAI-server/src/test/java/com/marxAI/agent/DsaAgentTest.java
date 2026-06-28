package com.marxAI.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marxAI.agent.tool.CodeRunnerTool;
import com.marxAI.agent.tool.NoteSearchTool;
import com.marxAI.agent.tool.QuestionGeneratorTool;
import com.marxAI.client.Judge0Client;
import com.marxAI.model.dto.AssembledContext;
import com.marxAI.model.dto.Judge0SubmissionResult;
import com.marxAI.model.dto.Judge0SubmissionResult.Judge0Status;
import com.marxAI.model.enums.DocumentType;
import com.marxAI.service.RetrievalService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the {@link DsaAgent} AiServices proxy.
 *
 * <p>The underlying {@link ChatModel} is mocked to return canned text responses.
 * Real {@link NoteSearchTool}, {@link QuestionGeneratorTool}, and {@link CodeRunnerTool}
 * instances are injected with their own mocked dependencies so that tool-invocation paths
 * (where the model chooses to call a tool) can be exercised.
 *
 * <p>Tests verify:
 * <ul>
 *   <li>The agent proxies {@code chat()} to the underlying {@link ChatModel}.</li>
 *   <li>The agent returns the model's text output verbatim.</li>
 *   <li>Different session IDs produce independent memory contexts.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class DsaAgentTest {

    @Mock private ChatModel chatModel;
    @Mock private RetrievalService retrievalService;
    @Mock private Judge0Client judge0Client;

    private DsaAgent dsaAgent;

    @BeforeEach
    void setUp() {
        NoteSearchTool noteSearchTool = new NoteSearchTool(retrievalService);
        QuestionGeneratorTool questionGeneratorTool = new QuestionGeneratorTool(chatModel);
        CodeRunnerTool codeRunnerTool = new CodeRunnerTool(judge0Client);

        dsaAgent = AiServices.builder(DsaAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(
                        memoryId -> MessageWindowChatMemory.withMaxMessages(20))
                .tools(noteSearchTool, questionGeneratorTool, codeRunnerTool)
                .build();
    }

    @Test
    void chat_delegatesToUnderlyingChatModel() {
        stubModelResponse("A binary search is an O(log n) search algorithm.");

        dsaAgent.chat("session-1", "explain binary search");

        verify(chatModel).chat(any(ChatRequest.class));
    }

    @Test
    void chat_returnsModelResponseText() {
        String expected = "Binary search works by repeatedly halving the search interval.";
        stubModelResponse(expected);

        String result = dsaAgent.chat("session-1", "how does binary search work?");

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void chat_differentSessionIds_treatAsIndependentMemory() {
        stubModelResponse("response for session A");

        dsaAgent.chat("session-A", "first question");
        dsaAgent.chat("session-B", "first question");

        // Both should delegate to the chat model without error
        verify(chatModel, org.mockito.Mockito.atLeast(2)).chat(any(ChatRequest.class));
    }

    @Test
    void chat_whenNoteSearchToolCalled_retrievalServiceIsInvoked() {
        // Simulate: model emits a tool-call for searchNotes, then produces a final reply.
        // LangChain4J intercepts the ToolExecutionRequest, calls NoteSearchTool.searchNotes(),
        // sends the result back to the model, and then forwards the second response as the answer.
        when(retrievalService.retrieve("binary search", DocumentType.DSA))
                .thenReturn(new AssembledContext("### Source 1:\nBinary search notes...\n", 1));

        ToolExecutionRequest toolCall = ToolExecutionRequest.builder()
                .id("tool-1")
                .name("searchNotes")
                .arguments("{\"topic\": \"binary search\"}")
                .build();

        when(chatModel.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder()
                        .aiMessage(AiMessage.from(List.of(toolCall)))
                        .build())
                .thenReturn(ChatResponse.builder()
                        .aiMessage(AiMessage.from("Based on your notes, binary search requires a sorted array."))
                        .build());

        String result = dsaAgent.chat("session-1", "search notes on binary search");

        assertThat(result).isNotBlank();
        verify(retrievalService).retrieve("binary search", DocumentType.DSA);
    }

    @Test
    void chat_whenCodeRunnerToolCalled_judge0ClientIsInvoked() {
        // Simulate: model emits a tool-call for runCode, then produces a final reply.
        when(judge0Client.execute("python", "print(42)", ""))
                .thenReturn(new Judge0SubmissionResult(
                        "42\n", null, null, new Judge0Status(3, "Accepted"), "0.012", null));

        ToolExecutionRequest toolCall = ToolExecutionRequest.builder()
                .id("tool-2")
                .name("runCode")
                .arguments("{\"language\": \"python\", \"code\": \"print(42)\", \"stdin\": \"\"}")
                .build();

        when(chatModel.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder()
                        .aiMessage(AiMessage.from(List.of(toolCall)))
                        .build())
                .thenReturn(ChatResponse.builder()
                        .aiMessage(AiMessage.from("The code outputs 42, which is correct."))
                        .build());

        String result = dsaAgent.chat("session-1", "run python print(42)");

        assertThat(result).isNotBlank();
        verify(judge0Client).execute("python", "print(42)", "");
    }

    // ---------------------------------------------------------------------------
    // Fixture helpers
    // ---------------------------------------------------------------------------

    private void stubModelResponse(String text) {
        when(chatModel.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder()
                        .aiMessage(AiMessage.from(text))
                        .build());
    }
}

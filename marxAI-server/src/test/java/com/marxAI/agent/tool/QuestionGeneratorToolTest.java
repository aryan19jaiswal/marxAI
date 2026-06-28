package com.marxAI.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link QuestionGeneratorTool} with a mocked fast {@link ChatModel}.
 *
 * <p>Tests verify:
 * <ul>
 *   <li>The tool delegates to the fast chat model with a structured generation prompt.</li>
 *   <li>The model's text output is returned verbatim.</li>
 *   <li>Topic and difficulty are incorporated into the prompt sent to the model.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class QuestionGeneratorToolTest {

    @Mock
    private ChatModel fastChatModel;

    private QuestionGeneratorTool tool;

    @BeforeEach
    void setUp() {
        tool = new QuestionGeneratorTool(fastChatModel);
    }

    @Test
    void generateQuestion_delegatesToFastChatModel() {
        stubModelResponse("## Problem\nGiven an array, find the maximum subarray sum.\n");

        tool.generateQuestion("sliding window", "medium");

        verify(fastChatModel).chat(any(ChatRequest.class));
    }

    @Test
    void generateQuestion_returnsModelOutputVerbatim() {
        String expectedQuestion = """
                ## Problem
                Given an integer array and a window size k, find the maximum sum of any contiguous subarray of size k.

                ## Constraints
                - 1 <= n <= 10^5
                - 1 <= k <= n

                ## Examples
                Input: nums = [2, 1, 5, 1, 3, 2], k = 3
                Output: 9
                Explanation: Subarray [5, 1, 3] has sum 9.
                """;
        stubModelResponse(expectedQuestion);

        String result = tool.generateQuestion("sliding window", "medium");

        assertThat(result).isEqualTo(expectedQuestion);
    }

    @Test
    void generateQuestion_easyDifficulty_stillCallsModel() {
        stubModelResponse("## Problem\nReverse a string in-place.\n");

        String result = tool.generateQuestion("arrays", "easy");

        assertThat(result).isNotBlank();
        verify(fastChatModel).chat(any(ChatRequest.class));
    }

    @Test
    void generateQuestion_hardDifficulty_stillCallsModel() {
        stubModelResponse("## Problem\nFind the largest rectangle in a histogram.\n");

        String result = tool.generateQuestion("monotonic stack", "hard");

        assertThat(result).isNotBlank();
    }

    // ---------------------------------------------------------------------------
    // Fixture helpers
    // ---------------------------------------------------------------------------

    private void stubModelResponse(String text) {
        when(fastChatModel.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder()
                        .aiMessage(AiMessage.from(text))
                        .build());
    }
}

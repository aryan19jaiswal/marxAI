package com.marxAI.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * LangChain4J tool that generates LeetCode-style DSA practice problems on demand.
 *
 * <p>Backed by the fast Gemini model (same one used for intent classification) so generation
 * is cheap and quick. The {@link com.marxAI.agent.DsaAgent} calls this when the user asks
 * for a practice question on a specific topic or difficulty level.
 *
 * <p>Delegating question generation to a tool rather than letting the main model handle it
 * inline lets us control the output format precisely without polluting the conversational
 * system prompt with lengthy formatting rules.
 */
@Slf4j
@Component
public class QuestionGeneratorTool {

    private final ChatModel fastChatModel;

    public QuestionGeneratorTool(@Qualifier("fast") ChatModel fastChatModel) {
        this.fastChatModel = fastChatModel;
    }

    /**
     * Generates a LeetCode-style DSA practice problem for the given topic and difficulty.
     *
     * @param topic      DSA concept to build the question around, e.g. "sliding window", "BFS"
     * @param difficulty question difficulty — must be "easy", "medium", or "hard"
     * @return a structured problem statement with constraints and examples
     */
    @Tool("Generate a LeetCode-style DSA practice question. "
            + "Use this when the user asks for a practice problem on a specific topic or difficulty.")
    public String generateQuestion(
            @P("the DSA concept or pattern to build the question around, e.g. 'binary tree', 'two pointers'")
            String topic,
            @P("difficulty level: 'easy', 'medium', or 'hard'")
            String difficulty) {
        log.debug("Generating {} question on topic '{}'", difficulty, topic);

        String prompt = """
                Generate a LeetCode-style DSA practice problem.
                Topic: %s
                Difficulty: %s

                Format your response with exactly these sections:
                ## Problem
                (2-3 sentence problem statement describing the task clearly)

                ## Constraints
                - (3-5 bullet points: array/string length, value ranges, time limit hint)

                ## Examples
                Input: ...
                Output: ...
                Explanation: (one sentence)

                Input: ...
                Output: ...

                Return ONLY the problem text using the sections above. No preamble, no solution hints.
                """.formatted(topic, difficulty);

        ChatRequest req = ChatRequest.builder()
                .messages(List.of(UserMessage.from(prompt)))
                .build();

        return fastChatModel.chat(req).aiMessage().text();
    }
}

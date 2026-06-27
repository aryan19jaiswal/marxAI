package com.marxAI.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marxAI.model.dto.IntentClassificationResult;
import com.marxAI.model.enums.AgentIntent;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the {@link IntentClassifier} AiServices proxy.
 *
 * <p>The underlying {@link ChatModel} is mocked to return canned JSON responses, isolating the
 * LangChain4J JSON-schema generation and response deserialisation into
 * {@link IntentClassificationResult} from network calls and API key requirements.
 *
 * <p>Tests verify:
 * <ul>
 *   <li>The proxy delegates to {@link ChatModel#chat(ChatRequest)}.</li>
 *   <li>Each supported {@link AgentIntent} value round-trips through JSON parsing correctly.</li>
 *   <li>Scalar fields (confidence, topic, difficulty, entities) are correctly deserialised.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class IntentClassifierTest {

    @Mock
    private ChatModel chatModel;

    private IntentClassifier intentClassifier;

    @BeforeEach
    void setUp() {
        intentClassifier = AiServices.builder(IntentClassifier.class)
                .chatModel(chatModel)
                .build();
    }

    @Test
    void classify_dsaMessage_returnsDsaIntent() {
        stubModelResponse("""
                {"intent":"DSA","confidence":0.97,"topic":"binary search",\
                "difficulty":"medium","entities":"array, pointer, sorted input"}
                """);

        IntentClassificationResult result = intentClassifier.classify(
                "Can you explain how binary search works on a sorted array?");

        assertThat(result.intent()).isEqualTo(AgentIntent.DSA);
        assertThat(result.confidence()).isGreaterThan(0.9);
        assertThat(result.topic()).isEqualTo("binary search");
        assertThat(result.difficulty()).isEqualTo("medium");
        assertThat(result.entities()).contains("array");
    }

    @Test
    void classify_systemDesignMessage_returnsSystemDesignIntent() {
        stubModelResponse("""
                {"intent":"SYSTEM_DESIGN","confidence":0.95,"topic":"URL shortener",\
                "difficulty":"medium","entities":"hashing, database, redirection"}
                """);

        IntentClassificationResult result = intentClassifier.classify("Design a URL shortener");

        assertThat(result.intent()).isEqualTo(AgentIntent.SYSTEM_DESIGN);
        assertThat(result.topic()).isEqualTo("URL shortener");
    }

    @Test
    void classify_resumeMessage_returnsResumeIntent() {
        stubModelResponse("""
                {"intent":"RESUME","confidence":0.92,"topic":"resume review",\
                "difficulty":"n/a","entities":"ATS, bullet points, FAANG"}
                """);

        IntentClassificationResult result = intentClassifier.classify(
                "Can you review my resume for a Google SWE role?");

        assertThat(result.intent()).isEqualTo(AgentIntent.RESUME);
        assertThat(result.difficulty()).isEqualTo("n/a");
    }

    @Test
    void classify_mockInterviewMessage_returnsMockInterviewIntent() {
        stubModelResponse("""
                {"intent":"MOCK_INTERVIEW","confidence":0.90,"topic":"interview simulation",\
                "difficulty":"n/a","entities":"coding, behavioral"}
                """);

        IntentClassificationResult result = intentClassifier.classify("I want to do a mock interview");

        assertThat(result.intent()).isEqualTo(AgentIntent.MOCK_INTERVIEW);
    }

    @Test
    void classify_studyPlanMessage_returnsStudyPlanIntent() {
        stubModelResponse("""
                {"intent":"STUDY_PLAN","confidence":0.88,"topic":"study schedule",\
                "difficulty":"n/a","entities":"graphs, DP, weak topics"}
                """);

        IntentClassificationResult result = intentClassifier.classify(
                "Create a 30-day study plan focusing on my weak topics");

        assertThat(result.intent()).isEqualTo(AgentIntent.STUDY_PLAN);
    }

    @Test
    void classify_generalMessage_returnsGeneralIntent() {
        stubModelResponse("""
                {"intent":"GENERAL","confidence":0.75,"topic":"greeting",\
                "difficulty":"n/a","entities":""}
                """);

        IntentClassificationResult result = intentClassifier.classify("Hello, how are you?");

        assertThat(result.intent()).isEqualTo(AgentIntent.GENERAL);
    }

    @Test
    void classify_delegatesToUnderlyingChatModel() {
        stubModelResponse("""
                {"intent":"DSA","confidence":0.80,"topic":"sorting","difficulty":"easy","entities":"array"}
                """);

        intentClassifier.classify("explain bubble sort");

        // Verify AiServices calls the underlying chat model (via ChatRequest)
        verify(chatModel).chat(any(ChatRequest.class));
    }

    // ---------------------------------------------------------------------------
    // Fixture helpers
    // ---------------------------------------------------------------------------

    private void stubModelResponse(String json) {
        when(chatModel.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder()
                        .aiMessage(AiMessage.from(json))
                        .build());
    }
}

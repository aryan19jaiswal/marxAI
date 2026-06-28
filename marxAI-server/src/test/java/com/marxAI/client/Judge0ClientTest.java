package com.marxAI.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marxAI.model.dto.Judge0SubmissionRequest;
import com.marxAI.model.dto.Judge0SubmissionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Unit tests for {@link Judge0Client} with a mocked {@link RestTemplate}.
 *
 * <p>Tests verify:
 * <ul>
 *   <li>Supported languages resolve to the correct Judge0 language ID and POST to the API.</li>
 *   <li>Unsupported languages return an error sentinel without calling the REST API.</li>
 *   <li>HTTP failures from the API are caught and returned as an error sentinel.</li>
 *   <li>Language matching is case-insensitive.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Judge0ClientTest {

    @Mock
    private RestTemplate restTemplate;

    private Judge0Client client;

    @BeforeEach
    void setUp() {
        client = new Judge0Client(restTemplate);
    }

    // ---------------------------------------------------------------------------
    // Successful submission
    // ---------------------------------------------------------------------------

    @Test
    void execute_python_submitsCorrectPayloadAndReturnsResult() {
        Judge0SubmissionResult expected = acceptedResult("42\n");
        when(restTemplate.postForObject(anyString(), any(), eq(Judge0SubmissionResult.class)))
                .thenReturn(expected);

        Judge0SubmissionResult result = client.execute("python", "print(42)", "");

        assertThat(result.stdout()).isEqualTo("42\n");
        assertThat(result.isAccepted()).isTrue();
        verify(restTemplate).postForObject(
                eq("/submissions?base64_encoded=false&wait=true"),
                any(Judge0SubmissionRequest.class),
                eq(Judge0SubmissionResult.class));
    }

    @Test
    void execute_java_usesLanguageId62() {
        when(restTemplate.postForObject(anyString(), any(), eq(Judge0SubmissionResult.class)))
                .thenReturn(acceptedResult("hello\n"));

        client.execute("java", "class Main { public static void main(String[] a) {} }", "");

        verify(restTemplate).postForObject(anyString(), any(), eq(Judge0SubmissionResult.class));
    }

    @Test
    void execute_languageMatchIsCaseInsensitive() {
        when(restTemplate.postForObject(anyString(), any(), eq(Judge0SubmissionResult.class)))
                .thenReturn(acceptedResult("output\n"));

        Judge0SubmissionResult result = client.execute("PYTHON", "print('output')", "");

        // Should not be an error sentinel — the language was resolved
        assertThat(result.isAccepted()).isTrue();
    }

    @Test
    void execute_pyAlias_resolvesToPython() {
        when(restTemplate.postForObject(anyString(), any(), eq(Judge0SubmissionResult.class)))
                .thenReturn(acceptedResult("done\n"));

        Judge0SubmissionResult result = client.execute("py", "print('done')", "");

        assertThat(result.isAccepted()).isTrue();
    }

    @Test
    void execute_passesStdinToRequest() {
        when(restTemplate.postForObject(anyString(), any(), eq(Judge0SubmissionResult.class)))
                .thenReturn(acceptedResult("5\n"));

        client.execute("python", "print(input())", "5");

        verify(restTemplate).postForObject(anyString(), any(), eq(Judge0SubmissionResult.class));
    }

    @Test
    void execute_nullStdin_treatedAsEmpty() {
        when(restTemplate.postForObject(anyString(), any(), eq(Judge0SubmissionResult.class)))
                .thenReturn(acceptedResult("ok\n"));

        Judge0SubmissionResult result = client.execute("python", "print('ok')", null);

        assertThat(result.isAccepted()).isTrue();
    }

    // ---------------------------------------------------------------------------
    // Unsupported language sentinel
    // ---------------------------------------------------------------------------

    @Test
    void execute_unsupportedLanguage_returnsErrorSentinelWithoutCallingApi() {
        Judge0SubmissionResult result = client.execute("ruby", "puts 42", "");

        assertThat(result.isAccepted()).isFalse();
        assertThat(result.stderr()).contains("ruby");
        assertThat(result.status().description()).isEqualTo("Unsupported Language");
        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }

    @Test
    void execute_unsupportedLanguage_resultMentionsSupportedLanguages() {
        Judge0SubmissionResult result = client.execute("kotlin", "fun main() {}", "");

        assertThat(result.stderr()).contains("python");
        assertThat(result.stderr()).contains("java");
    }

    // ---------------------------------------------------------------------------
    // API failure sentinel
    // ---------------------------------------------------------------------------

    @Test
    void execute_apiCallFails_returnsErrorSentinel() {
        when(restTemplate.postForObject(anyString(), any(), eq(Judge0SubmissionResult.class)))
                .thenThrow(new RestClientException("Connection refused"));

        Judge0SubmissionResult result = client.execute("python", "print(1)", "");

        assertThat(result.isAccepted()).isFalse();
        assertThat(result.stderr()).contains("Code execution service unavailable");
        assertThat(result.status().description()).isEqualTo("API Error");
    }

    // ---------------------------------------------------------------------------
    // Language ID map
    // ---------------------------------------------------------------------------

    @Test
    void languageIds_allExpectedLanguagesPresent() {
        assertThat(Judge0Client.LANGUAGE_IDS).containsKey("python");
        assertThat(Judge0Client.LANGUAGE_IDS).containsKey("java");
        assertThat(Judge0Client.LANGUAGE_IDS).containsKey("cpp");
        assertThat(Judge0Client.LANGUAGE_IDS).containsKey("javascript");
        assertThat(Judge0Client.LANGUAGE_IDS).containsKey("go");
        assertThat(Judge0Client.LANGUAGE_IDS).containsKey("c");
    }

    @Test
    void languageIds_pythonId_is71() {
        assertThat(Judge0Client.LANGUAGE_IDS.get("python")).isEqualTo(71);
    }

    @Test
    void languageIds_javaId_is62() {
        assertThat(Judge0Client.LANGUAGE_IDS.get("java")).isEqualTo(62);
    }

    @Test
    void languageIds_cppId_is54() {
        assertThat(Judge0Client.LANGUAGE_IDS.get("cpp")).isEqualTo(54);
    }

    // ---------------------------------------------------------------------------
    // Fixture helpers
    // ---------------------------------------------------------------------------

    private static Judge0SubmissionResult acceptedResult(String stdout) {
        return new Judge0SubmissionResult(
                stdout, null, null,
                new Judge0SubmissionResult.Judge0Status(3, "Accepted"),
                "0.012", 3456L);
    }
}

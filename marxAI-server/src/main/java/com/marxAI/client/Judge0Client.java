package com.marxAI.client;

import com.marxAI.model.dto.Judge0SubmissionRequest;
import com.marxAI.model.dto.Judge0SubmissionResult;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client for the Judge0 code-execution REST API.
 *
 * <p>Uses the {@code wait=true} query flag on the submission endpoint so Judge0 blocks until
 * execution completes and returns the result inline — no polling loop is needed.
 *
 * <p>Language support is limited to the languages most common in coding interviews.
 * Unknown language identifiers return an error sentinel rather than throwing, so the
 * {@link com.marxAI.agent.tool.CodeRunnerTool} can relay a meaningful message to the model.
 */
@Slf4j
@Service
public class Judge0Client {

    /**
     * Mapping from human-friendly language names (lower-case) to Judge0 integer language IDs.
     * Only languages relevant to coding interviews are included.
     */
    static final Map<String, Integer> LANGUAGE_IDS = Map.of(
            "python",     71,
            "py",         71,
            "java",       62,
            "cpp",        54,
            "c++",        54,
            "c",          50,
            "javascript", 63,
            "js",         63,
            "go",         60);

    private final RestTemplate restTemplate;

    public Judge0Client(@Qualifier("judge0RestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Submits {@code code} in the given {@code language} for execution and waits for the result.
     *
     * @param language case-insensitive language identifier (e.g. "python", "java", "cpp")
     * @param code     the source code to execute
     * @param stdin    standard input passed to the program; empty string if none required
     * @return execution result with stdout/stderr/status, or an error sentinel when the language
     *         is unsupported or the API call fails
     */
    public Judge0SubmissionResult execute(String language, String code, String stdin) {
        Integer langId = LANGUAGE_IDS.get(language.toLowerCase());
        if (langId == null) {
            log.warn("Unsupported language requested: '{}'", language);
            return unsupportedLanguageResult(language);
        }

        Judge0SubmissionRequest request = new Judge0SubmissionRequest(code, langId,
                stdin == null ? "" : stdin);
        try {
            log.debug("Submitting {} (lang_id={}) to Judge0", language, langId);
            return restTemplate.postForObject(
                    "/submissions?base64_encoded=false&wait=true",
                    request,
                    Judge0SubmissionResult.class);
        } catch (RestClientException e) {
            log.error("Judge0 API call failed", e);
            return errorResult("Code execution service unavailable: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------------------
    // Error sentinels — return structured results instead of throwing so the agent
    // receives a message it can relay to the user rather than crashing.
    // ---------------------------------------------------------------------------

    private static Judge0SubmissionResult unsupportedLanguageResult(String language) {
        String msg = "Unsupported language '" + language
                + "'. Supported: python, java, cpp, c, javascript, go.";
        return new Judge0SubmissionResult(
                null, msg, null,
                new Judge0SubmissionResult.Judge0Status(400, "Unsupported Language"),
                null, null);
    }

    private static Judge0SubmissionResult errorResult(String message) {
        return new Judge0SubmissionResult(
                null, message, null,
                new Judge0SubmissionResult.Judge0Status(500, "API Error"),
                null, null);
    }
}

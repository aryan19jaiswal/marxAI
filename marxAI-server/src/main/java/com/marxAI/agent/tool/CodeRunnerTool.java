package com.marxAI.agent.tool;

import com.marxAI.client.Judge0Client;
import com.marxAI.model.dto.Judge0SubmissionResult;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * LangChain4J tool that executes code via Judge0 and returns a human-readable verdict.
 *
 * <p>Enables the {@link com.marxAI.agent.DsaAgent} to run the user's submitted solution
 * against custom test inputs and give concrete feedback on output correctness. The agent
 * can also use this to demonstrate a working solution after guiding the user to it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeRunnerTool {

    private final Judge0Client judge0Client;

    /**
     * Submits code to Judge0 for execution and returns a formatted result string.
     *
     * <p>Supported languages: {@code python}, {@code java}, {@code cpp}, {@code c},
     * {@code javascript}, {@code go} (case-insensitive).
     *
     * @param language programming language identifier
     * @param code     source code to execute
     * @param stdin    standard input for the program; pass empty string if none required
     * @return formatted result containing status, stdout, stderr, and execution time
     */
    @Tool("Execute code using a sandboxed remote code runner and return the output. "
            + "Supported languages: python, java, cpp, c, javascript, go.")
    public String runCode(
            @P("programming language, e.g. 'python', 'java', 'cpp'")
            String language,
            @P("the complete source code to execute")
            String code,
            @P("standard input for the program; use empty string '' if the program reads no input")
            String stdin) {
        log.debug("Executing {} code via Judge0", language);
        Judge0SubmissionResult result = judge0Client.execute(language, code, stdin);
        return formatResult(result);
    }

    // ---------------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------------

    /**
     * Converts a Judge0 result into a compact, agent-readable string.
     * On success, leads with stdout. On failure, leads with stderr or compile output.
     */
    private String formatResult(Judge0SubmissionResult result) {
        if (result == null) {
            return "Code execution returned no result.";
        }

        StringBuilder sb = new StringBuilder();
        if (result.status() != null) {
            sb.append("Status: ").append(result.status().description()).append('\n');
        }

        if (hasText(result.stdout())) {
            sb.append("Output:\n").append(result.stdout().stripTrailing()).append('\n');
        }
        if (hasText(result.compileOutput())) {
            sb.append("Compile output:\n").append(result.compileOutput().stripTrailing()).append('\n');
        }
        if (hasText(result.stderr())) {
            sb.append("Error:\n").append(result.stderr().stripTrailing()).append('\n');
        }
        if (result.time() != null) {
            sb.append("Time: ").append(result.time()).append("s");
        }

        return sb.toString().stripTrailing();
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}

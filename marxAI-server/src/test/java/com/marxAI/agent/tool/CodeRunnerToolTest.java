package com.marxAI.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marxAI.client.Judge0Client;
import com.marxAI.model.dto.Judge0SubmissionResult;
import com.marxAI.model.dto.Judge0SubmissionResult.Judge0Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link CodeRunnerTool} with a mocked {@link Judge0Client}.
 *
 * <p>Tests verify:
 * <ul>
 *   <li>Successful execution includes status, stdout, and time in the output.</li>
 *   <li>Runtime errors include stderr in the output.</li>
 *   <li>Compile errors include compile output in the formatted result.</li>
 *   <li>The tool delegates to {@code Judge0Client} with the correct arguments.</li>
 *   <li>A {@code null} result from the client returns a graceful fallback message.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class CodeRunnerToolTest {

    @Mock
    private Judge0Client judge0Client;

    private CodeRunnerTool tool;

    @BeforeEach
    void setUp() {
        tool = new CodeRunnerTool(judge0Client);
    }

    // ---------------------------------------------------------------------------
    // Successful execution
    // ---------------------------------------------------------------------------

    @Test
    void runCode_accepted_includesStatusAndStdout() {
        when(judge0Client.execute("python", "print(42)", ""))
                .thenReturn(result("42\n", null, null, 3, "Accepted", "0.012"));

        String output = tool.runCode("python", "print(42)", "");

        assertThat(output).contains("Accepted");
        assertThat(output).contains("42");
        assertThat(output).contains("0.012s");
    }

    @Test
    void runCode_accepted_tripsStdoutWhitespace() {
        when(judge0Client.execute("java", "class M{}", ""))
                .thenReturn(result("  hello  \n", null, null, 3, "Accepted", "0.100"));

        String output = tool.runCode("java", "class M{}", "");

        assertThat(output).contains("hello");
    }

    // ---------------------------------------------------------------------------
    // Runtime error
    // ---------------------------------------------------------------------------

    @Test
    void runCode_runtimeError_includesStderr() {
        when(judge0Client.execute("python", "1/0", ""))
                .thenReturn(result(null, "ZeroDivisionError: division by zero\n", null,
                        11, "Runtime Error", "0.005"));

        String output = tool.runCode("python", "1/0", "");

        assertThat(output).contains("Runtime Error");
        assertThat(output).contains("ZeroDivisionError");
    }

    // ---------------------------------------------------------------------------
    // Compile error
    // ---------------------------------------------------------------------------

    @Test
    void runCode_compileError_includesCompileOutput() {
        when(judge0Client.execute("cpp", "int main(", ""))
                .thenReturn(result(null, null, "expected ')' before EOF", 6, "Compilation Error", null));

        String output = tool.runCode("cpp", "int main(", "");

        assertThat(output).contains("Compilation Error");
        assertThat(output).contains("expected ')'");
    }

    // ---------------------------------------------------------------------------
    // Delegation and edge cases
    // ---------------------------------------------------------------------------

    @Test
    void runCode_delegatesToJudge0ClientWithCorrectArgs() {
        when(judge0Client.execute("go", "package main\nfunc main() {}", "input"))
                .thenReturn(result("", null, null, 3, "Accepted", "0.008"));

        tool.runCode("go", "package main\nfunc main() {}", "input");

        verify(judge0Client).execute("go", "package main\nfunc main() {}", "input");
    }

    @Test
    void runCode_nullResultFromClient_returnsGracefulMessage() {
        when(judge0Client.execute("python", "pass", "")).thenReturn(null);

        String output = tool.runCode("python", "pass", "");

        assertThat(output).isEqualTo("Code execution returned no result.");
    }

    @Test
    void runCode_blankStdout_doesNotIncludeOutputSection() {
        when(judge0Client.execute("python", "pass", ""))
                .thenReturn(result("", null, null, 3, "Accepted", "0.003"));

        String output = tool.runCode("python", "pass", "");

        // No "Output:" header when stdout is blank
        assertThat(output).doesNotContain("Output:");
    }

    @Test
    void runCode_noExecutionTime_omitsTimeLine() {
        when(judge0Client.execute("python", "pass", ""))
                .thenReturn(result("ok\n", null, null, 3, "Accepted", null));

        String output = tool.runCode("python", "pass", "");

        assertThat(output).doesNotContain("Time:");
    }

    // ---------------------------------------------------------------------------
    // Fixture helpers
    // ---------------------------------------------------------------------------

    private static Judge0SubmissionResult result(
            String stdout, String stderr, String compileOutput,
            int statusId, String statusDesc, String time) {
        return new Judge0SubmissionResult(
                stdout, stderr, compileOutput,
                new Judge0Status(statusId, statusDesc),
                time, null);
    }
}

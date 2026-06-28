package com.marxAI.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON payload returned by Judge0 for a completed submission.
 * All fields except {@code status} are nullable — Judge0 omits fields irrelevant to the outcome.
 *
 * @param stdout        program's standard output; null if none
 * @param stderr        program's standard error; null if none
 * @param compileOutput compiler output for compile-error submissions; null otherwise
 * @param status        execution status descriptor (always present)
 * @param time          wall-clock execution time in seconds as a string (e.g. "0.012")
 * @param memory        peak memory usage in kilobytes; null when not reported
 */
public record Judge0SubmissionResult(
        String stdout,
        String stderr,
        @JsonProperty("compile_output") String compileOutput,
        Judge0Status status,
        String time,
        Long memory) {

    /**
     * Nested Judge0 status object — id drives routing logic, description is human-readable.
     *
     * <p>Notable status IDs:
     * <ul>
     *   <li>1 = In Queue</li>
     *   <li>2 = Processing</li>
     *   <li>3 = Accepted</li>
     *   <li>4 = Wrong Answer</li>
     *   <li>5 = Time Limit Exceeded</li>
     *   <li>6 = Compilation Error</li>
     *   <li>11 = Runtime Error</li>
     * </ul>
     */
    public record Judge0Status(int id, String description) {}

    /** Returns {@code true} when execution completed successfully (status id == 3). */
    public boolean isAccepted() {
        return status != null && status.id() == 3;
    }
}

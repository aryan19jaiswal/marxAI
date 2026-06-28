package com.marxAI.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON payload sent to Judge0 {@code POST /submissions}.
 *
 * @param sourceCode the source code to compile and execute
 * @param languageId Judge0 integer language ID (e.g. 71 = Python 3, 62 = Java, 54 = C++)
 * @param stdin      standard input to pass to the program; empty string if none
 */
public record Judge0SubmissionRequest(
        @JsonProperty("source_code") String sourceCode,
        @JsonProperty("language_id") int languageId,
        @JsonProperty("stdin") String stdin) {}

package com.marxAI.agent;

import com.marxAI.model.dto.IntentClassificationResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * LangChain4J AiServices interface for lightweight intent classification.
 *
 * <p>Backed by {@code gemini-2.0-flash-lite} (fast, low-cost) via {@code LangChainConfig}.
 * Called on every incoming user message before routing and RAG retrieval so it must be quick.
 *
 * <p>LangChain4J generates a JSON schema from {@link IntentClassificationResult}, appends
 * instructions to respond in that schema, and automatically deserialises the model's JSON reply
 * into the record. No manual JSON parsing is required.
 */
public interface IntentClassifier {

    @SystemMessage("""
            You are an intent classifier for MarxAI, a software engineering interview-prep platform.
            Classify the user's message into exactly one of these intents:
              DSA          – data structures, algorithms, LeetCode problems, complexity, coding challenges
              SYSTEM_DESIGN – distributed systems, architecture, scalability, databases, APIs, design patterns
              RESUME       – resume review, ATS scoring, job applications, cover letters
              MOCK_INTERVIEW – interview simulation, practice sessions, behavioral questions
              STUDY_PLAN   – study schedules, learning plans, weak-topic identification, progress tracking
              GENERAL      – anything that does not fit the above categories

            Rules:
            • topic: the primary subject of the message (e.g. "binary search", "URL shortener", "Python experience").
            • difficulty: "easy", "medium", "hard", or "n/a" if difficulty cannot be inferred.
            • entities: comma-separated key technical terms found in the message (e.g. "binary tree, DFS, memoisation").
            • confidence: your confidence in the chosen intent, from 0.0 to 1.0.
            """)
    IntentClassificationResult classify(@UserMessage String message);
}

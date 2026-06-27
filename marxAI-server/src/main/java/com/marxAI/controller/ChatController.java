package com.marxAI.controller;

import com.marxAI.model.dto.ChatRequest;
import com.marxAI.model.dto.ChatResponse;
import com.marxAI.security.UserPrincipal;
import com.marxAI.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Chat endpoints for the Planner agent.
 *
 * <ul>
 *   <li>{@code POST /api/chat} — blocking; waits for the full model response. Suitable for
 *       programmatic callers that need the complete reply before continuing.</li>
 *   <li>{@code POST /api/chat/stream} — SSE; streams tokens as they arrive. Suitable for chat
 *       UI components that render responses token-by-token for a more interactive feel.</li>
 * </ul>
 *
 * <p>Both endpoints require a valid JWT bearer token. The {@code sessionId} field in the request
 * is optional: omit it to start a new session, or include a previous session's ID to resume that
 * conversation's in-memory history.
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * Blocking chat endpoint. Waits for the model to produce its full reply.
     * Prefer {@link #streamChat} for conversational UI to avoid stalling the client.
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(chatService.chat(principal.getId(), request));
    }

    /**
     * SSE streaming endpoint. Returns a {@code text/event-stream} response; each event's
     * {@code data} field contains a single text token as it arrives from Gemini.
     * The stream closes when the model finishes or an error occurs.
     *
     * <p>Timeout is set to 2 minutes ({@code 120 000 ms}) to accommodate slow or long responses
     * without the server-side emitter expiring prematurely.
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        SseEmitter emitter = new SseEmitter(120_000L);
        chatService.streamChat(principal.getId(), request, emitter);
        return emitter;
    }
}

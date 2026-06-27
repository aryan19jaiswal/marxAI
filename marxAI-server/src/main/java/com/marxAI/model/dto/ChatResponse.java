package com.marxAI.model.dto;

import com.marxAI.model.enums.AgentIntent;
import java.util.UUID;

/**
 * Response payload for {@code POST /api/chat}.
 *
 * @param sessionId   the session this turn belongs to — pass back in the next request to
 *                    resume the conversation with in-memory history intact
 * @param response    the assistant's full reply text
 * @param intent      the classified intent that drove agent routing and RAG scoping
 * @param sourceCount number of personal note chunks injected as RAG context (0 when no relevant
 *                    notes were found)
 */
public record ChatResponse(UUID sessionId, String response, AgentIntent intent, int sourceCount) {}

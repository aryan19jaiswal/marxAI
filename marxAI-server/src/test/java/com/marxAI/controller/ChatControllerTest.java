package com.marxAI.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marxAI.model.dto.ChatRequest;
import com.marxAI.model.dto.ChatResponse;
import com.marxAI.model.entity.User;
import com.marxAI.model.enums.AgentIntent;
import com.marxAI.security.CustomUserDetailsService;
import com.marxAI.security.JwtService;
import com.marxAI.security.RestAuthenticationEntryPoint;
import com.marxAI.security.UserPrincipal;
import com.marxAI.service.ChatService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Slice tests for {@link ChatController} using {@code @WebMvcTest}.
 *
 * <p>{@link ChatService} is mocked so tests only exercise the HTTP contract: request
 * deserialisation, response serialisation, validation, and security gate.
 *
 * <p>Security dependencies are mocked so {@code SecurityConfig} can be fully instantiated:
 * <ul>
 *   <li>{@link JwtService} and {@link CustomUserDetailsService} are {@code @Service} beans
 *       that {@code @WebMvcTest} does not include automatically.</li>
 *   <li>{@link RestAuthenticationEntryPoint} is a {@code @Component} that {@code @WebMvcTest}
 *       may not scan; mocking it prevents a missing-bean failure and allows us to stub a
 *       proper 401 response for unauthenticated-request tests.</li>
 * </ul>
 *
 * <p>All POST requests include a CSRF token via {@code .with(csrf())} to satisfy both the
 * project's custom security config (which disables CSRF) and Spring Boot's default test security
 * config (which enables it) — making the tests robust regardless of which config is active.
 */
@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        // Stub the entry point to write a 401 status so unauthenticated-request tests can assert it
        doAnswer(inv -> {
            HttpServletResponse response = inv.getArgument(1);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }).when(restAuthenticationEntryPoint).commence(any(), any(), any());
    }

    private UserPrincipal principal() {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail("test@example.com");
        user.setName("Test User");
        user.setPasswordHash("x");
        return new UserPrincipal(user);
    }

    // ---------------------------------------------------------------------------
    // POST /api/chat
    // ---------------------------------------------------------------------------

    @Test
    void chat_validRequest_returns200WithChatResponse() throws Exception {
        ChatRequest request = new ChatRequest(null, "explain binary search");
        ChatResponse response = new ChatResponse(
                SESSION_ID, "Binary search is O(log n)...", AgentIntent.DSA, 1);
        when(chatService.chat(eq(USER_ID), any(ChatRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/chat")
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(SESSION_ID.toString()))
                .andExpect(jsonPath("$.response").value("Binary search is O(log n)..."))
                .andExpect(jsonPath("$.intent").value("DSA"))
                .andExpect(jsonPath("$.sourceCount").value(1));
    }

    @Test
    void chat_blankMessage_returns400() throws Exception {
        ChatRequest request = new ChatRequest(null, "");

        mockMvc.perform(post("/api/chat")
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_missingBody_returns400() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_withoutAuthentication_returns401() throws Exception {
        ChatRequest request = new ChatRequest(null, "explain binary search");

        mockMvc.perform(post("/api/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void chat_withSessionId_forwardsSessionIdToService() throws Exception {
        ChatRequest request = new ChatRequest(SESSION_ID, "explain merge sort");
        ChatResponse response = new ChatResponse(SESSION_ID, "Merge sort is...", AgentIntent.DSA, 0);
        when(chatService.chat(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/chat")
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(chatService).chat(eq(USER_ID), captor.capture());
        assertThat(captor.getValue().sessionId()).isEqualTo(SESSION_ID);
    }

    // ---------------------------------------------------------------------------
    // POST /api/chat/stream (SSE)
    // ---------------------------------------------------------------------------

    @Test
    void streamChat_validRequest_returns200() throws Exception {
        ChatRequest request = new ChatRequest(null, "explain quick sort");

        mockMvc.perform(post("/api/chat/stream")
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void streamChat_blankMessage_returns400() throws Exception {
        ChatRequest request = new ChatRequest(null, "   ");

        mockMvc.perform(post("/api/chat/stream")
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void streamChat_withoutAuthentication_returns401() throws Exception {
        ChatRequest request = new ChatRequest(null, "explain quick sort");

        mockMvc.perform(post("/api/chat/stream")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}

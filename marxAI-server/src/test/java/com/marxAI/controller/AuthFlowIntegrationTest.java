package com.marxAI.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.marxAI.model.dto.AuthResponse;
import com.marxAI.model.dto.ErrorResponse;
import com.marxAI.model.dto.LoginRequest;
import com.marxAI.model.dto.RegisterRequest;
import com.marxAI.model.dto.UserResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * End-to-end exercise of the register → login → call-protected-endpoint flow against the real
 * Postgres-backed application context, mirroring the manual Postman walkthrough from the roadmap.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static RegisterRequest uniqueRegisterRequest() {
        String email = "auth-flow-" + UUID.randomUUID() + "@example.com";
        return new RegisterRequest("Integration Tester", email, "password123");
    }

    @Test
    void register_thenLogin_thenAccessProtectedEndpoint_succeeds() {
        RegisterRequest registerRequest = uniqueRegisterRequest();

        ResponseEntity<AuthResponse> registerResponse =
                restTemplate.postForEntity("/api/users/register", registerRequest, AuthResponse.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResponse.getBody()).isNotNull();
        assertThat(registerResponse.getBody().user().email()).isEqualTo(registerRequest.email());

        var loginRequest = new LoginRequest(registerRequest.email(), registerRequest.password());
        ResponseEntity<AuthResponse> loginResponse =
                restTemplate.postForEntity("/api/users/login", loginRequest, AuthResponse.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = loginResponse.getBody().token();
        assertThat(token).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<UserResponse> meResponse = restTemplate.exchange(
                "/api/users/me", HttpMethod.GET, new HttpEntity<>(headers), UserResponse.class);
        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResponse.getBody()).isNotNull();
        assertThat(meResponse.getBody().email()).isEqualTo(registerRequest.email());
    }

    @Test
    void register_duplicateEmail_returnsConflict() {
        RegisterRequest registerRequest = uniqueRegisterRequest();
        restTemplate.postForEntity("/api/users/register", registerRequest, AuthResponse.class);

        ResponseEntity<ErrorResponse> secondAttempt =
                restTemplate.postForEntity("/api/users/register", registerRequest, ErrorResponse.class);

        assertThat(secondAttempt.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(secondAttempt.getBody()).isNotNull();
        assertThat(secondAttempt.getBody().message()).contains(registerRequest.email());
    }

    @Test
    void register_invalidPayload_returnsBadRequestWithFieldErrors() {
        RegisterRequest invalidRequest = new RegisterRequest("", "not-an-email", "short");

        ResponseEntity<ErrorResponse> response =
                restTemplate.postForEntity("/api/users/register", invalidRequest, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().fieldErrors()).containsKeys("name", "email", "password");
    }

    @Test
    void login_wrongPassword_returnsUnauthorized() {
        RegisterRequest registerRequest = uniqueRegisterRequest();
        restTemplate.postForEntity("/api/users/register", registerRequest, AuthResponse.class);

        var loginRequest = new LoginRequest(registerRequest.email(), "wrong-password");
        ResponseEntity<ErrorResponse> response =
                restTemplate.postForEntity("/api/users/login", loginRequest, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void accessProtectedEndpoint_withoutToken_returnsUnauthorized() {
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity("/api/users/me", ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void accessProtectedEndpoint_withInvalidToken_returnsUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("this.is.not.a.valid.jwt");

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/users/me", HttpMethod.GET, new HttpEntity<>(headers), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}

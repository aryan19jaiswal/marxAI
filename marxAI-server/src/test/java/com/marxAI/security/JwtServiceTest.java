package com.marxAI.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.marxAI.model.entity.User;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

/** Unit tests for {@link JwtService} token issuance and validation. */
class JwtServiceTest {

    private static final String SECRET = "unit-test-signing-secret-needs-to-be-at-least-32-bytes-long";

    private final JwtService jwtService = new JwtService(SECRET, 60_000L);

    private UserPrincipal principal(String email) {
        User user = User.builder().id(UUID.randomUUID()).email(email).name("Test").passwordHash("hash").build();
        return new UserPrincipal(user);
    }

    @Test
    void generateToken_thenExtractEmail_roundTrips() {
        UserPrincipal principal = principal("jane@example.com");
        String token = jwtService.generateToken(principal);

        assertThat(jwtService.extractEmail(token)).isEqualTo("jane@example.com");
    }

    @Test
    void generateToken_thenExtractUserId_roundTrips() {
        UserPrincipal principal = principal("jane@example.com");
        String token = jwtService.generateToken(principal);

        assertThat(jwtService.extractUserId(token)).isEqualTo(principal.getId());
    }

    @Test
    void isTokenValid_returnsTrue_forMatchingUser() {
        UserPrincipal principal = principal("jane@example.com");
        String token = jwtService.generateToken(principal);

        assertThat(jwtService.isTokenValid(token, principal)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalse_whenSubjectDoesNotMatchUser() {
        String token = jwtService.generateToken(principal("jane@example.com"));
        UserDetails differentUser = principal("john@example.com");

        assertThat(jwtService.isTokenValid(token, differentUser)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_forExpiredToken() {
        JwtService shortLived = new JwtService(SECRET, -1_000L);
        UserPrincipal principal = principal("jane@example.com");
        String expiredToken = shortLived.generateToken(principal);

        assertThat(shortLived.isTokenValid(expiredToken, principal)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_forMalformedToken() {
        UserPrincipal principal = principal("jane@example.com");

        assertThat(jwtService.isTokenValid("not-a-real-jwt", principal)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_forTokenSignedWithDifferentSecret() {
        JwtService otherService = new JwtService("a-completely-different-signing-secret-of-32+-bytes", 60_000L);
        UserPrincipal principal = principal("jane@example.com");
        String token = otherService.generateToken(principal);

        assertThat(jwtService.isTokenValid(token, principal)).isFalse();
    }
}

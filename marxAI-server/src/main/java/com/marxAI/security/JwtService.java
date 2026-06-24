package com.marxAI.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/** Issues and validates the HS256 JWTs used for stateless API authentication. */
@Service
public class JwtService {

    private static final String CLAIM_USER_ID = "userId";

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret, @Value("${jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    /** Builds a signed token whose subject is the user's email and carries their id as a claim. */
    public String generateToken(UserPrincipal principal) {
        Date issuedAt = new Date();
        Date expiry = new Date(issuedAt.getTime() + expirationMs);
        return Jwts.builder()
                .subject(principal.getUsername())
                .claim(CLAIM_USER_ID, principal.getId().toString())
                .issuedAt(issuedAt)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(extractClaims(token).get(CLAIM_USER_ID, String.class));
    }

    /** True only if the token is well-formed, unexpired, and belongs to the given user. */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = extractClaims(token);
            return claims.getSubject().equals(userDetails.getUsername())
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }
}

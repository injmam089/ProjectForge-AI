package com.projectforge.security;

import com.projectforge.entity.Role;
import com.projectforge.entity.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    private JwtService jwtService;
    private SecurityUser securityUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, EXPIRATION_MS);

        User user = new User("Alice Tester", "alice@example.com", "$2a$10$hashedpw", Role.USER);
        user.setId(42L);
        securityUser = new SecurityUser(user);
    }

    @Test
    @DisplayName("generateToken should create valid token and extract username and claims")
    void generateToken_shouldCreateValidToken() {
        String token = jwtService.generateToken(securityUser, 42L, Role.USER);

        assertNotNull(token);
        assertEquals("alice@example.com", jwtService.extractUsername(token));
        assertEquals(42L, ((Number) jwtService.extractClaim(token, claims -> claims.get("userId"))).longValue());
        assertEquals("USER", jwtService.extractClaim(token, claims -> claims.get("role")));
        assertTrue(jwtService.isTokenValid(token, securityUser));
        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    @DisplayName("isTokenValid should return false if username does not match")
    void isTokenValid_shouldReturnFalseForDifferentUser() {
        String token = jwtService.generateToken(securityUser, 42L, Role.USER);

        User anotherUser = new User("Bob", "bob@example.com", "hash", Role.USER);
        SecurityUser differentSecurityUser = new SecurityUser(anotherUser);

        assertFalse(jwtService.isTokenValid(token, differentSecurityUser));
    }

    @Test
    @DisplayName("Expired token should be recognized as expired and invalid")
    void expiredToken_shouldBeInvalid() {
        // Create a JwtService with negative expiration (already expired)
        JwtService expiredJwtService = new JwtService(TEST_SECRET, -5000);
        String expiredToken = expiredJwtService.generateToken(securityUser, 42L, Role.USER);

        assertTrue(expiredJwtService.isTokenExpired(expiredToken));
        assertFalse(expiredJwtService.isTokenValid(expiredToken, securityUser));
    }

    @Test
    @DisplayName("Malformed or tampered token should fail validation")
    void malformedToken_shouldFailValidation() {
        String token = jwtService.generateToken(securityUser, 42L, Role.USER);
        String tamperedToken = token + "corrupted";

        assertFalse(jwtService.isTokenValid(tamperedToken, securityUser));
        assertThrows(JwtException.class, () -> jwtService.extractAllClaims(tamperedToken));
    }

    @Test
    @DisplayName("Token signed with different secret should be rejected")
    void differentSecretToken_shouldBeRejected() {
        JwtService anotherService = new JwtService("DifferentSecretKeyWithSufficientLengthForHmacSha512BitsOver64BytesLong!!!", EXPIRATION_MS);
        String foreignToken = anotherService.generateToken(securityUser, 42L, Role.USER);

        assertFalse(jwtService.isTokenValid(foreignToken, securityUser));
        assertThrows(JwtException.class, () -> jwtService.extractAllClaims(foreignToken));
    }

    @Test
    @DisplayName("Short JWT secret (< 64 bytes) should throw IllegalArgumentException")
    void shortSecret_shouldThrowIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new JwtService("TooShortSecretForHS512", EXPIRATION_MS)
        );
        assertTrue(ex.getMessage().contains("at least 64 bytes"));
    }

    @Test
    @DisplayName("Null or blank JWT secret should throw IllegalArgumentException")
    void blankSecret_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService(null, EXPIRATION_MS));
        assertThrows(IllegalArgumentException.class, () -> new JwtService("   ", EXPIRATION_MS));
    }
}

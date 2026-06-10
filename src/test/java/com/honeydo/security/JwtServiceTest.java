package com.honeydo.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "bZiUSJmRtqZMmLUlpwsAEKczXFEkMqt8BUl0K1dFQNA=";

    private final JwtService jwtService = new JwtService(SECRET, 60_000);

    @Test
    void generatesAndValidatesToken() {
        String token = jwtService.generateToken("user@example.com");

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractSubject(token)).isEqualTo("user@example.com");
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.generateToken("user@example.com");
        char lastChar = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (lastChar == 'a' ? 'b' : 'a');

        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    void rejectsExpiredToken() {
        JwtService expiredTokenService = new JwtService(SECRET, -60_000);
        String token = expiredTokenService.generateToken("user@example.com");

        assertThat(expiredTokenService.isTokenValid(token)).isFalse();
    }
}

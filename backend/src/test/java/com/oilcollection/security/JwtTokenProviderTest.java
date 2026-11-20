package com.oilcollection.security;

import io.jsonwebtoken.io.DecodingException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenProviderTest {

    @Test
    void init_WithValidBase64SecretInitializesProvider() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(Base64.getEncoder().encodeToString(
                "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        JwtTokenProvider provider = new JwtTokenProvider(properties);

        assertDoesNotThrow(provider::init);
    }

    @Test
    void init_WithInvalidBase64SecretThrowsDecodingException() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("invalid_secret_with_underscore");
        JwtTokenProvider provider = new JwtTokenProvider(properties);

        assertThrows(DecodingException.class, provider::init);
    }

    @Test
    void init_WithMissingSecretThrowsIllegalArgumentException() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(null);
        JwtTokenProvider provider = new JwtTokenProvider(properties);

        assertThrows(IllegalArgumentException.class, provider::init);
    }
}

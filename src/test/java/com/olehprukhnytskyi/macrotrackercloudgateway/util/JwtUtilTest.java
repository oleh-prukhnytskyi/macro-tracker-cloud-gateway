package com.olehprukhnytskyi.macrotrackercloudgateway.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtUtilTest {
    private JwtUtil jwtUtil;
    private Key key;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        String secret = "test_secret_key_test_secret_key_test_secret_key_test_secret_"
                + "key_test_secret_key_test_secret_key_test_secret_key_test_secret_key";
        key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),
                SignatureAlgorithm.HS256.getJcaName());

        Field field = JwtUtil.class.getDeclaredFields()[0];
        field.setAccessible(true);
        try {
            field.set(jwtUtil, secret);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void validateToken_withValidToken_returnsTrue() {
        String token = Jwts.builder()
                .setSubject("test-user")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_withInvalidToken_returnsFalse() {
        String token = "some.invalid.token";
        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_withExpiredToken_returnsFalse() {
        String token = Jwts.builder()
                .setSubject("expired-user")
                .setIssuedAt(new Date(System.currentTimeMillis() - 100000))
                .setExpiration(new Date(System.currentTimeMillis() - 10000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_withNull_returnsFalse() {
        assertFalse(jwtUtil.validateToken(null));
    }

    @Test
    void validateToken_withEmptyString_returnsFalse() {
        assertFalse(jwtUtil.validateToken(""));
    }
}

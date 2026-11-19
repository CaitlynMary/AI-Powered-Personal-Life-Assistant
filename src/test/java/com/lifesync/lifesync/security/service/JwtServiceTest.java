package com.lifesync.lifesync.security.service;

import com.lifesync.lifesync.security.principal.CustomUserDetails;
import com.lifesync.lifesync.user.entity.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private static final String KEY = "VGhpcy1pcy1hLXRlc3Qta2V5LXdpdGgtMzItYnl0ZXMtbG9uZw==";

    @Test void generatesAndValidatesSignedToken() {
        JwtService service = service(60_000);
        User user = user();
        String token = service.generateAccessToken(new CustomUserDetails(user));
        assertEquals("test@example.com", service.extractSubject(token));
        assertTrue(service.isTokenValid(token, new CustomUserDetails(user)));
    }

    @Test void rejectsTamperedAndExpiredTokens() throws InterruptedException {
        JwtService service = service(1);
        User user = user();
        String token = service.generateAccessToken(new CustomUserDetails(user));
        assertThrows(JwtException.class, () -> service.extractSubject(token.substring(0, token.length() - 2) + "aa"));
        Thread.sleep(10);
        assertFalse(service.isTokenValid(token, new CustomUserDetails(user)));
    }

    private JwtService service(long expiration) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "encodedSecret", KEY);
        ReflectionTestUtils.setField(service, "expirationMillis", expiration);
        service.initializeKey();
        return service;
    }
    private User user() { User user = new User(); user.setId(42L); user.setEmail("test@example.com"); user.setPassword("hash"); user.setActive(true); return user; }
}
